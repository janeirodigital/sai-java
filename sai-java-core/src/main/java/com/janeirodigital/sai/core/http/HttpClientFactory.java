package com.janeirodigital.sai.core.http;

import com.janeirodigital.sai.core.annotations.ExcludeFromGeneratedCoverage;
import com.janeirodigital.sai.core.authorization.AccessTokenRefresher;
import com.janeirodigital.sai.core.authorization.AuthorizedSessionAccessor;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.shapetrees.client.okhttp.OkHttpClientFactory;
import com.janeirodigital.shapetrees.client.okhttp.OkHttpClientFactoryManager;
import com.janeirodigital.shapetrees.client.okhttp.OkHttpValidatingClientFactory;
import com.janeirodigital.shapetrees.core.exceptions.ShapeTreeException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import okhttp3.OkHttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory to get, cache, and clear OkHttp HTTP clients matching provided configurations.
 * <br>Can enable / disable SSL validation and/or Shape Tree validation
 * @see <a href="https://square.github.io/okhttp/">OkHttp</a> - HTTP client library from Square
 * @see <a href="https://github.com/janeirodigital/shapetrees-java">shapetrees-java</a> - Shape Trees client library implementation
 */
public class HttpClientFactory implements OkHttpClientFactory {

    private final ConcurrentHashMap<HttpClientConfiguration, OkHttpClient> okHttpClients;

    // Default values for new clients
    private final boolean validateSsl;
    private final boolean validateShapeTrees;
    private final boolean refreshTokens;

    private final AuthorizedSessionAccessor sessionAccessor;

    public HttpClientFactory(boolean validateSsl, boolean validateShapeTrees, boolean refreshTokens, AuthorizedSessionAccessor sessionAccessor) throws SaiException {
        this.validateSsl = validateSsl;
        this.validateShapeTrees = validateShapeTrees;
        if (refreshTokens && sessionAccessor == null) { throw new SaiException("Must provide an authorized session accessor if when configured to refresh tokens"); }
        this.refreshTokens = refreshTokens;
        this.sessionAccessor = sessionAccessor;
        this.okHttpClients = new ConcurrentHashMap<>();
    }

    public HttpClientFactory(boolean validateSsl, boolean validateShapeTrees, boolean refreshTokens) throws SaiException {
        this(validateSsl, validateShapeTrees, refreshTokens, null);
    }

    public OkHttpClient
    get() throws SaiException {
        return get(this.validateSsl, this.validateShapeTrees, this.refreshTokens);
    }

    /**
     * Factory to provide an OkHttpClient configured to enable or disable SSL and/or
     * Shape Tree validation based on the provided options. A small array of clients is
     * maintained such that there is a given client for each unique configuration.
     * <br><b>SSL Validation should never be disabled in production!</b><br>
     * @see <a href="https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/#okhttpclients-should-be-shared">OkHttpClients should be shared</a>
     * @param validateSsl Disables client/server SSL validation when false.
     * @param validateShapeTrees Disables client-side shape tree validation when false.
     * @param refreshTokens Disables automatic attempt to refresh access tokens on HTTP 401 responses when false
     * @return Configured OkHttpClient
     * @throws SaiException
     */
    public OkHttpClient
    get(boolean validateSsl, boolean validateShapeTrees, boolean refreshTokens) throws SaiException {

        // If there is already a client initialized matching this configuration return it
        HttpClientConfiguration configuration = new HttpClientConfiguration(validateSsl, validateShapeTrees, refreshTokens);
        if (this.okHttpClients.containsKey(configuration)) { return this.okHttpClients.get(configuration); }

        // Otherwise create a new client matching the configuration
        try {
            OkHttpClient client = getClientForConfiguration(validateSsl, validateShapeTrees, refreshTokens);
            this.okHttpClients.put(configuration, client);
            return client;
        } catch (NoSuchAlgorithmException|KeyManagementException ex) {
            throw new SaiException(ex.getMessage());
        }
    }

    /**
     * Build and return an OkHttpClient based on the provided options for
     * SSL Validation, Shape Tree Validation, and Token Refresh. SSL
     * is disabled by installing an all-trusting certificate manager.
     * Shape tree validation is enabled by injecting an OkHttp
     * <a href="https://square.github.io/okhttp/interceptors/">interceptor</a>.
     * @param validateSsl Disables client/server SSL validation when false.
     * @param validateShapeTrees Disables client-side shape tree validation when false.
     * @param refreshTokens Disables automatic attempt to refresh access tokens on HTTP 401 responses when false
     * @return Configured OkHttpClient
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    public OkHttpClient
    getClientForConfiguration(boolean validateSsl, boolean validateShapeTrees, boolean refreshTokens) throws NoSuchAlgorithmException, KeyManagementException, SaiException {

        okhttp3.OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        // Invoke the shapetrees-java-client-okhttp library to give us an OkHttpClient that
        // has been configured to intercept requests and perform client-side shape tree
        // validation.
        if (validateShapeTrees) {
            // Tell the shapetrees-java-client-okhttp library to use this client factory
            // for basic okhttp clients. This ensures that validating clients handed back
            // from shapetrees-java-client-okhttp will include any other configuration
            // options we set.
            OkHttpClientFactoryManager.setFactory(this);
            try {
                return OkHttpValidatingClientFactory.get();
            } catch (ShapeTreeException ex) {
                throw new SaiException(ex.getMessage());
            }
        }

        if (!validateSsl) {
            // DEVELOPMENT USE ONLY - Configure an all-trusted certificate manager because SSL Validation is disabled
            final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            TrustManager[] trustAllCerts = getTrustAllCertsManager();
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            clientBuilder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]);
            clientBuilder.hostnameVerifier(new NoopHostnameVerifier());
        }

        // If an AccessTokenProvider is available, an OkHttp authenticator that automatically attempts
        // to refresh tokens when needed (e.g. when a 401 response is received)
        if (refreshTokens) { clientBuilder.authenticator(new AccessTokenRefresher(this.sessionAccessor)); }

        return clientBuilder.build();
    }

    /**
     * Implementation of the {@link OkHttpClientFactory} interface provided from
     * shapetrees-java-client-okhttp. Allows this factory to be used as the source
     * of okhttp clients by that library.
     *
     * <br>This call explicitly sets <code>validateShapeTrees</code> to false because
     * it is the responsibility of {@link OkHttpClientFactory} enable validation, and
     * would create an infinite loop if it were set to true.
     *
     * @return OkHttpClient
     * @throws ShapeTreeException
     */
    @Override
    public OkHttpClient getOkHttpClient() throws ShapeTreeException {
        try {
            // the OkHttpClientFactory
            return get(this.validateSsl, false, this.refreshTokens);
        } catch (SaiException ex) {
            throw new ShapeTreeException(500, ex.getMessage());
        }
    }

    /**
     * Shuts down each initialized OkHttp client in the local cache, and then
     * empties them from it.
     */
    public void
    resetClients() {
        for (var entry : this.okHttpClients.entrySet()) {
            OkHttpClient client = this.okHttpClients.get(entry.getKey());
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
            this.okHttpClients.remove(entry.getKey());
        }
    }

    /**
     * Identifies whether the local OkHttp client cache is empty or not
     * @return true when the cache is empty
     */
    public boolean
    isEmpty() { return this.okHttpClients.isEmpty(); }

    /**
     * Construct an all-trusting certificate manager to use when SSL validation has
     * been disabled. <b>SSL Validation should never be disabled in production!</b>
     * @return Fully permissive TrustManager
     */
    private TrustManager[]
    getTrustAllCertsManager() {
        // Create a trust manager that does not validate certificate chains
        // NOT FOR PRODUCTION USE
        return new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    @ExcludeFromGeneratedCoverage
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        // Trust all clients to by skipping SSL validation
                    }

                    @Override
                    @ExcludeFromGeneratedCoverage
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        // Trust all servers by skipping SSL validation
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };
    }

    /**
     * Internal class to manage configuration options for http clients
     */
    @Getter @AllArgsConstructor
    protected static class HttpClientConfiguration {
        private final boolean validateSsl;
        private final boolean validateShapeTrees;
        private final boolean refreshTokens;

        @Override
        public boolean equals(Object object) {
            if (object == this) { return true; }
            if (!(object instanceof HttpClientConfiguration)) { return false; }
            HttpClientConfiguration configuration = (HttpClientConfiguration) object;
            if (this.validateSsl != configuration.isValidateSsl()) { return false; }
            if (this.validateShapeTrees != configuration.isValidateShapeTrees()) { return false; }
            if (this.refreshTokens != configuration.isRefreshTokens()) { return false; }
            return true;
        }

        @Override
        public int hashCode() {
            int result = Boolean.hashCode(this.validateSsl);
            result = 31 * result + Boolean.hashCode(this.validateShapeTrees);
            result = 31 * result + Boolean.hashCode(this.refreshTokens);
            return result;
        }
    }

}

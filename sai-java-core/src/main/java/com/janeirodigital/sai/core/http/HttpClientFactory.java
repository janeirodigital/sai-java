package com.janeirodigital.sai.core.http;

import com.janeirodigital.sai.core.annotations.ExcludeFromGeneratedCoverage;
import com.janeirodigital.sai.core.authorization.AccessTokenAuthenticator;
import com.janeirodigital.sai.core.authorization.AccessTokenProvider;
import com.janeirodigital.sai.core.authorization.AccessTokenProviderManager;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.shapetrees.core.exceptions.ShapeTreeException;
import com.janeirodigital.shapetrees.client.okhttp.OkHttpClientFactory;
import com.janeirodigital.shapetrees.client.okhttp.OkHttpClientFactoryManager;
import com.janeirodigital.shapetrees.client.okhttp.OkHttpValidatingClientFactory;
import okhttp3.OkHttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Factory to get, cache, and clear OkHttp HTTP clients matching provided configurations.
 * <br>Can enable / disable SSL validation and/or Shape Tree validation
 * @see <a href="https://square.github.io/okhttp/">OkHttp</a> - HTTP client library from Square
 * @see <a href="https://github.com/janeirodigital/shapetrees-java">shapetrees-java</a> - Shape Trees client library implementation
 */
public class HttpClientFactory implements OkHttpClientFactory {

    static final int NO_VALIDATION = 0;
    static final int VALIDATE = 1;

    private final OkHttpClient[][] okHttpClients = {{null, null}, {null, null}};
    private final boolean validateSsl;
    private final boolean validateShapeTrees;

    public HttpClientFactory(boolean validateSsl, boolean validateShapeTrees) {
        this.validateSsl = validateSsl;
        this.validateShapeTrees = validateShapeTrees;
    }

    public OkHttpClient
    get() throws SaiException {
        return get(this.validateSsl, this.validateShapeTrees);
    }

    /**
     * Factory to provide an OkHttpClient configured to enable or disable SSL and/or
     * Shape Tree validation based on the provided options. A small array of clients is
     * maintained such that there is a given client for each unique configuration.
     * <br><b>SSL Validation should never be disabled in production!</b><br>
     * @see <a href="https://square.github.io/okhttp/4.x/okhttp/okhttp3/-ok-http-client/#okhttpclients-should-be-shared">OkHttpClients should be shared</a>
     * @param validateSsl Disables client/server SSL validation when false.
     * @param validateShapeTrees Disables client-side shape tree validation when false.
     * @return Configured OkHttpClient
     * @throws SaiException
     */
    public OkHttpClient
    get(boolean validateSsl, boolean validateShapeTrees) throws SaiException {

        int ssl = validateSsl ? VALIDATE : NO_VALIDATION;
        int shapeTrees = validateShapeTrees ? VALIDATE : NO_VALIDATION;

        if (this.okHttpClients[ssl][shapeTrees] != null) { return this.okHttpClients[ssl][shapeTrees]; }
        try {
            // Just call an internal factory here to produce the appropriate client configuration
            OkHttpClient client = getClientForConfiguration(validateSsl, validateShapeTrees);
            this.okHttpClients[ssl][shapeTrees] = client;
            return client;
        } catch (NoSuchAlgorithmException|KeyManagementException ex) {
            throw new SaiException(ex.getMessage());
        }
    }

    /**
     * Build and return an OkHttpClient based on the provided options for
     * <code>validateSsl</code> and <code>validateShapeTrees</code>. SSL
     * is disabled by installing an all-trusting certificate manager.
     * Shape tree validation is enabled by injecting an OkHttp
     * <a href="https://square.github.io/okhttp/interceptors/">interceptor</a>.
     * @param validateSsl
     * @param validateShapeTrees
     * @return Configured OkHttpClient
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    public OkHttpClient
    getClientForConfiguration(boolean validateSsl, boolean validateShapeTrees) throws NoSuchAlgorithmException, KeyManagementException, SaiException {

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

        // If an AccessTokenProvider is available, and OkHttp authenticator that automatically attempts
        // to refresh tokens when needed (e.g. when a 401 response is received)
        AccessTokenProvider tokenProvider = AccessTokenProviderManager.getProvider();
        if (tokenProvider != null) { clientBuilder.authenticator(new AccessTokenAuthenticator(tokenProvider)); }

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
            return get(this.validateSsl, false);
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
        for (OkHttpClient[] row : this.okHttpClients) {
            for (OkHttpClient client : row) {
                if (client != null) {
                    client.dispatcher().executorService().shutdown();
                    client.connectionPool().evictAll();
                }
            }
            row[VALIDATE] = null;
            row[NO_VALIDATION] = null;
        }
    }

    /**
     * Identifies whether the local OkHttp client cache is empty or not
     * @return true when the cache is empty
     */
    public boolean
    isEmpty() {
        for (OkHttpClient[] row : this.okHttpClients) {
            for (OkHttpClient client : row) {
                if (client != null) {
                    return false;
                }
            }
        }
        return true;
    }

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

}

package com.janeirodigital.sai.core.http;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.shapetrees.okhttp.OkHttpValidatingShapeTreeInterceptor;
import okhttp3.OkHttpClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class HttpClientFactory {

    static final int NO_VALIDATION = 0;
    static final int VALIDATE = 1;

    private static final OkHttpClient[][] okHttpClients = {{null, null}, {null, null}};

    private HttpClientFactory() { }

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
    public static synchronized OkHttpClient
    get(boolean validateSsl, boolean validateShapeTrees) throws SaiException {

        int ssl = validateSsl ? VALIDATE : NO_VALIDATION;
        int shapeTrees = validateShapeTrees ? VALIDATE : NO_VALIDATION;

        if (okHttpClients[ssl][shapeTrees] != null) {
            return okHttpClients[ssl][shapeTrees];
        }
        try {
            // Just call an internal factory here to produce the appropriate client configuration
            OkHttpClient client = getClientForConfiguration(validateSsl, validateShapeTrees);
            okHttpClients[ssl][shapeTrees] = client;
            return client;
        } catch (Exception ex) {
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
    private static OkHttpClient
    getClientForConfiguration(boolean validateSsl, boolean validateShapeTrees) throws NoSuchAlgorithmException, KeyManagementException {

        okhttp3.OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();

        if (validateShapeTrees) {
            clientBuilder.interceptors().add(new OkHttpValidatingShapeTreeInterceptor());
        }

        if (!validateSsl) {
            // DEVELOPMENT USE ONLY - Configure an all-trusted certificate manager because SSL Validation is disabled
            final SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
            TrustManager[] trustAllCerts = getTrustAllCertsManager();
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            // Create an ssl socket factory with our all-trusting manager
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            clientBuilder.sslSocketFactory(sslSocketFactory, (X509TrustManager)trustAllCerts[0]).hostnameVerifier((hostname, session) -> true);
        }

        return clientBuilder.build();

    }

    /**
     * Construct an all-trusting certificate manager to use when SSL validation has
     * been disabled. <b><b>SSL Validation should never be disabled in production!</b>
     * @return Fully permissive TrustManager
     */
    private static TrustManager[]
    getTrustAllCertsManager() {
        // Create a trust manager that does not validate certificate chains
        // NOT FOR PRODUCTION USE
        return new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        // All clients are trusted when SSL validation is skipped
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                        // All servers are trusted when SSL validation is skipped
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };
    }

}

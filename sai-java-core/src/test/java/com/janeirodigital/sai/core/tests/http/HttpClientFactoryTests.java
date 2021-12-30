package com.janeirodigital.sai.core.tests.http;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;


class HttpClientFactoryTests {

    @Test
    @DisplayName("Initialize an HTTP factory and confirm defaults")
    void initializeHttpFactory() throws SaiException {
        HttpClientFactory factory = new HttpClientFactory(true, true);
        OkHttpClient httpClient = factory.get();
        assertNotNull(httpClient);
        assertTrue(httpClient.equals(factory.get(true, true)));
    }


    @Test
    @DisplayName("Get an HTTP client with SSL enabled : validation enabled")
    void getHttpClientSslOnValidationOn() throws SaiException {
        HttpClientFactory factory = new HttpClientFactory(true, true);
        OkHttpClient httpClient = factory.get();
        assertNotNull(httpClient);
        assertTrue(httpClient.equals(factory.get(true, true)));
    }

    @Test
    @DisplayName("Get an HTTP client with SSL disabled : validation enabled")
    void getHttpClientSslOffValidationOn() throws SaiException {
        HttpClientFactory factory = new HttpClientFactory(false, true);
        OkHttpClient httpClient = factory.get();
        assertNotNull(httpClient);
        assertTrue(httpClient.equals(factory.get(false, true)));
    }

    @Test
    @DisplayName("Get an HTTP client with SSL enabled : validation disabled")
    void getHttpClientSslOnValidationOff() throws SaiException {
        HttpClientFactory factory = new HttpClientFactory(true, false);
        OkHttpClient httpClient = factory.get();
        assertNotNull(httpClient);
        assertTrue(httpClient.equals(factory.get(true, false)));
    }

    @Test
    @DisplayName("Get an HTTP client with SSL disabled : validation disabled")
    void getHttpClientSslOffValidationOff() throws SaiException {
        HttpClientFactory factory = new HttpClientFactory(false, false);
        OkHttpClient httpClient = factory.get();
        assertNotNull(httpClient);
        assertTrue(httpClient.equals(factory.get(false, false)));
    }

    @Test
    @DisplayName("Reset the factory client cache")
    void resetClientCache() throws SaiException {
        HttpClientFactory factory = new HttpClientFactory(false, false);
        factory.get(true, false);
        factory.get(true, true);
        factory.get(false, true);
        assertFalse(factory.isEmpty());
        factory.resetClients();
        assertTrue(factory.isEmpty());
    }

    @Test
    @DisplayName("Fail to get an HTTP client with an invalid configuration")
    void failToGetHttpClientWithInvalidConfig() throws NoSuchAlgorithmException, KeyManagementException, SaiException {
        HttpClientFactory mockFactory = Mockito.mock(HttpClientFactory.class, withSettings().useConstructor(false, false).defaultAnswer(CALLS_REAL_METHODS));
        when(mockFactory.getClientForConfiguration(anyBoolean(), anyBoolean())).thenThrow(new NoSuchAlgorithmException("Invalid algorithm!"));
        assertThrows(SaiException.class, () -> { mockFactory.get(false, false); });
    }
}

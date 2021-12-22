package com.janeirodigital.sai.core.tests.http;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;


class HttpClientFactoryTests {

    @BeforeEach
    void beforeEach() {
        HttpClientFactory.resetClients();
    }

    @Test
    @DisplayName("Get an HTTP client with SSL enabled : validation enabled")
    void getHttpClientSslOnValidationOn() throws SaiException {
        OkHttpClient httpClient = HttpClientFactory.get(true, true);
        assertNotNull(httpClient);
    }

    @Test
    @DisplayName("Get an HTTP client with SSL disabled : validation enabled")
    void getHttpClientSslOffValidationOn() throws SaiException {
        OkHttpClient httpClient = HttpClientFactory.get(false, true);
        assertNotNull(httpClient);
    }

    @Test
    @DisplayName("Get an HTTP client with SSL enabled : validation disabled")
    void getHttpClientSslOnValidationOff() throws SaiException {
        OkHttpClient httpClient = HttpClientFactory.get(true, false);
        assertNotNull(httpClient);
    }

    @Test
    @DisplayName("Get an HTTP client with SSL disabled : validation disabled")
    void getHttpClientSslOffValidationOff() throws SaiException {
        OkHttpClient httpClient = HttpClientFactory.get(false, false);
        assertNotNull(httpClient);
    }

    @Test
    @DisplayName("Reset the factory client cache")
    void resetClientCache() throws SaiException {
        HttpClientFactory.get(false, false);
        HttpClientFactory.get(true, true);
        HttpClientFactory.resetClients();
    }

    @Test
    @DisplayName("Fail to get an HTTP client with an invalid configuration")
    void failToGetHttpClientWithInvalidConfig() {

        try (MockedStatic<HttpClientFactory> mockFactory = Mockito.mockStatic(HttpClientFactory.class, Mockito.CALLS_REAL_METHODS)) {

            mockFactory.when(() -> HttpClientFactory.getClientForConfiguration(anyBoolean(),anyBoolean()) ).thenThrow(new NoSuchAlgorithmException("Invalid algorithm!"));

            assertThrows(SaiException.class, () -> { HttpClientFactory.get(false, false); });

            mockFactory.reset();

        }



    }
}

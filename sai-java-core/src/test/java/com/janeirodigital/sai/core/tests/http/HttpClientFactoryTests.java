package com.janeirodigital.sai.core.tests.http;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.shapetrees.core.exceptions.ShapeTreeException;
import com.janeirodigital.shapetrees.okhttp.OkHttpValidatingClientFactory;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
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
        assertEquals(httpClient, factory.get(true, true));
    }


    @Test
    @DisplayName("Get an HTTP client with SSL enabled : validation enabled")
    void getHttpClientSslOnValidationOn() throws SaiException {
        HttpClientFactory factory = new HttpClientFactory(true, true);
        OkHttpClient httpClient = factory.get();
        assertNotNull(httpClient);
        assertEquals(httpClient, factory.get(true, true));
    }

    @Test
    @DisplayName("Get an HTTP client with SSL disabled : validation enabled")
    void getHttpClientSslOffValidationOn() throws SaiException {
        HttpClientFactory factory = new HttpClientFactory(false, true);
        OkHttpClient httpClient = factory.get();
        assertNotNull(httpClient);
        assertEquals(httpClient, factory.get(false, true));
    }

    @Test
    @DisplayName("Fail to get an HTTP client with validation enabled")
    void failToGetHttpClientValidationOn() {
        HttpClientFactory factory = new HttpClientFactory(false, true);
        try (MockedStatic<OkHttpValidatingClientFactory> mockValidatingFactory = Mockito.mockStatic(OkHttpValidatingClientFactory.class)) {
            mockValidatingFactory.when(() -> OkHttpValidatingClientFactory.get()).thenThrow(ShapeTreeException.class);
            assertThrows(SaiException.class, () -> { factory.get(); });
        }
    }

    @Test
    @DisplayName("Fail to get an HTTP client from shape tree client interface provider")
    void failToGetHttpClientFromShapeTreeInterface() throws ShapeTreeException, SaiException {
        HttpClientFactory mockFactory = Mockito.mock(HttpClientFactory.class, withSettings().useConstructor(false, true).defaultAnswer(CALLS_REAL_METHODS));
        when(mockFactory.get(anyBoolean(), anyBoolean())).thenThrow(SaiException.class);
        assertThrows(ShapeTreeException.class, () -> { mockFactory.getOkHttpClient(); });
    }

    @Test
    @DisplayName("Get an HTTP client with SSL enabled : validation disabled")
    void getHttpClientSslOnValidationOff() throws SaiException {
        HttpClientFactory factory = new HttpClientFactory(true, false);
        OkHttpClient httpClient = factory.get();
        assertNotNull(httpClient);
        assertEquals(httpClient, factory.get(true, false));
    }

    @Test
    @DisplayName("Get an HTTP client with SSL disabled : validation disabled")
    void getHttpClientSslOffValidationOff() throws SaiException {
        HttpClientFactory factory = new HttpClientFactory(false, false);
        OkHttpClient httpClient = factory.get();
        assertNotNull(httpClient);
        assertEquals(httpClient, factory.get(false, false));
    }

    @Test
    @DisplayName("Reset the factory client cache")
    void resetClientCache() throws SaiException {
        HttpClientFactory factory = new HttpClientFactory(false, false);
        factory.get(true, false);
        factory.get(true, true);
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

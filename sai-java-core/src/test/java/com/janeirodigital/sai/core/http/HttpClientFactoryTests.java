package com.janeirodigital.sai.core.http;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.shapetrees.client.okhttp.OkHttpValidatingClientFactory;
import com.janeirodigital.shapetrees.core.exceptions.ShapeTreeException;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnGet;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;


class HttpClientFactoryTests {

    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;

    @BeforeAll
    static void beforeAll() {
        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
        mockOnGet(dispatcher, "/op/.well-known/openid-configuration", "authorization/op-configuration-json");
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
    }

    @Test
    @DisplayName("Initialize an HTTP factory and confirm defaults")
    void initializeHttpFactory() throws SaiException {
        HttpClientFactory factory = new HttpClientFactory(true, true, false);
        OkHttpClient httpClient = factory.get();
        assertNotNull(httpClient);
        assertEquals(httpClient, factory.get(true, true, false));
    }

    @Test
    @DisplayName("Get an HTTP client with SSL enabled : validation enabled")
    void getHttpClientSslOnValidationOn() throws SaiException {
        HttpClientFactory factory = new HttpClientFactory(true, true, false);
        OkHttpClient httpClient = factory.get();
        assertNotNull(httpClient);
        assertEquals(httpClient, factory.get(true, true, false));
    }

    @Test
    @DisplayName("Get an HTTP client with SSL disabled : validation enabled")
    void getHttpClientSslOffValidationOn() throws SaiException {
        HttpClientFactory factory = new HttpClientFactory(false, true, false);
        OkHttpClient httpClient = factory.get();
        assertNotNull(httpClient);
        assertEquals(httpClient, factory.get(false, true, false));
    }

    @Test
    @DisplayName("Fail to get an HTTP client with validation enabled")
    void failToGetHttpClientValidationOn() throws SaiException {
        HttpClientFactory factory = new HttpClientFactory(false, true, false);
        try (MockedStatic<OkHttpValidatingClientFactory> mockValidatingFactory = Mockito.mockStatic(OkHttpValidatingClientFactory.class)) {
            mockValidatingFactory.when(() -> OkHttpValidatingClientFactory.get()).thenThrow(ShapeTreeException.class);
            assertThrows(SaiException.class, () -> { factory.get(); });
        }
    }

    @Test
    @DisplayName("Fail to get an HTTP client from shape tree client interface provider")
    void failToGetHttpClientFromShapeTreeInterface() throws SaiException {
        HttpClientFactory mockFactory = Mockito.mock(HttpClientFactory.class, withSettings().useConstructor(false, true, false).defaultAnswer(CALLS_REAL_METHODS));
        when(mockFactory.get(anyBoolean(), anyBoolean(), anyBoolean())).thenThrow(SaiException.class);
        assertThrows(ShapeTreeException.class, () -> { mockFactory.getOkHttpClient(); });
    }

    @Test
    @DisplayName("Get an HTTP client with SSL enabled : validation disabled")
    void getHttpClientSslOnValidationOff() throws SaiException {
        HttpClientFactory factory = new HttpClientFactory(true, false, false);
        OkHttpClient httpClient = factory.get();
        assertNotNull(httpClient);
        assertEquals(httpClient, factory.get(true, false, false));
    }

    @Test
    @DisplayName("Get an HTTP client with SSL disabled : validation disabled")
    void getHttpClientSslOffValidationOff() throws SaiException {
        HttpClientFactory factory = new HttpClientFactory(false, false, false);
        OkHttpClient httpClient = factory.get();
        assertNotNull(httpClient);
        assertEquals(httpClient, factory.get(false, false, false));
    }

    @Test
    @DisplayName("Reset the factory client cache")
    void resetClientCache() throws SaiException {
        HttpClientFactory factory = new HttpClientFactory(false, false, false);
        factory.get(true, false, false);
        factory.get(true, true, false);
        assertFalse(factory.isEmpty());
        factory.resetClients();
        assertTrue(factory.isEmpty());
    }

    @Test
    @DisplayName("Fail to get an HTTP client with an invalid configuration")
    void failToGetHttpClientWithInvalidConfig() throws NoSuchAlgorithmException, KeyManagementException, SaiException {
        HttpClientFactory mockFactory = Mockito.mock(HttpClientFactory.class, withSettings().useConstructor(false, false, false, null).defaultAnswer(CALLS_REAL_METHODS));
        when(mockFactory.getClientForConfiguration(anyBoolean(), anyBoolean(), anyBoolean())).thenThrow(new NoSuchAlgorithmException("Invalid algorithm!"));
        assertThrows(SaiException.class, () -> { mockFactory.get(false, false, false); });
    }
}

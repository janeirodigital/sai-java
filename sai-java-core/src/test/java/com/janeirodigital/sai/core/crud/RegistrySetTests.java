package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.sai.core.enums.ContentType.LD_JSON;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.*;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static com.janeirodigital.sai.core.helpers.HttpHelper.stringToUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RegistrySetTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;

    private static URL aliceAgentRegistry;
    private static URL aliceAccessConsentRegistry;
    private static URL aliceAgentRegistryJsonLd;
    private static URL aliceAccessConsentRegistryJsonLd;
    private static List<URL> aliceDataRegistries;

    @BeforeAll
    static void beforeAll() throws SaiException {

        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
        // GET crud registry set in Turtle
        mockOnGet(dispatcher, "/ttl/registries", "crud/registry-set-ttl");
        mockOnPut(dispatcher, "/new/ttl/registries", "http/201");  // create new
        mockOnPut(dispatcher, "/ttl/registries", "http/204");  // update existing
        mockOnDelete(dispatcher, "/ttl/registries", "http/204");  // delete
        // GET crud registry set in Turtle with missing fields
        mockOnGet(dispatcher, "/missing-fields/ttl/registries", "crud/registry-set-missing-fields-ttl");
        // GET crud registry set in JSON-LD
        mockOnGet(dispatcher, "/jsonld/registries", "crud/registry-set-jsonld");
        mockOnPut(dispatcher, "/new/jsonld/registries", "http/201");  // create new
        mockOnPut(dispatcher, "/jsonld/registries", "http/204");  // update existing or delete
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        aliceAgentRegistry = toUrl(server,"/ttl/agents/");
        aliceAccessConsentRegistry = toUrl(server, "/ttl/consents/");
        aliceAgentRegistryJsonLd = toUrl(server,"/jsonld/agents/");
        aliceAccessConsentRegistryJsonLd = toUrl(server, "/jsonld/consents/");
        aliceDataRegistries = Arrays.asList(stringToUrl("https://work.alice.example/data/"), stringToUrl("https://personal.alice.example/data/"));
    }

    @Test
    @DisplayName("Create new crud registry set")
    void createNewCrudRegistrySet() throws SaiException {
        URL url = toUrl(server, "/new/ttl/registries");
        RegistrySet.Builder builder = new RegistrySet.Builder(url, saiSession);
        RegistrySet registrySet = builder.setAgentRegistry(aliceAgentRegistry).setAccessConsentRegistry(aliceAccessConsentRegistry)
                                         .setDataRegistries(aliceDataRegistries).build();
        assertDoesNotThrow(() -> registrySet.update());
        assertNotNull(registrySet);
    }

    @Test
    @DisplayName("Read crud registry set")
    void readRegistrySet() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/registries");
        RegistrySet registrySet = RegistrySet.get(url, saiSession);
        checkRegistrySet(registrySet);
    }

    @Test
    @DisplayName("Fail to read existing crud registry set in turtle - missing required fields")
    void failToReadRegistrySet() {
        URL url = toUrl(server, "/missing-fields/ttl/registries");
        assertThrows(SaiException.class, () -> RegistrySet.get(url, saiSession));
    }

    @Test
    @DisplayName("Update existing crud registry set")
    void updateRegistrySet() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/registries");
        RegistrySet existing = RegistrySet.get(url, saiSession);
        existing.setAgentRegistryUrl(stringToUrl("https://alice.example/otheragents/"));
        assertDoesNotThrow(() -> existing.update());
        assertNotNull(existing);
    }

    @Test
    @DisplayName("Read existing registry set in JSON-LD")
    void readRegistrySetJsonLd() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/jsonld/registries");
        RegistrySet registrySet = RegistrySet.get(url, saiSession, LD_JSON);
        checkRegistrySet(registrySet);
    }

    @Test
    @DisplayName("Create new crud registry set in JSON-LD")
    void createNewCrudRegistrySetJsonLd() throws SaiException {
        URL url = toUrl(server, "/new/jsonld/registries");
        RegistrySet.Builder builder = new RegistrySet.Builder(url, saiSession);
        RegistrySet registrySet = builder.setContentType(LD_JSON).setAgentRegistry(aliceAgentRegistry)
                                          .setAccessConsentRegistry(aliceAccessConsentRegistry)
                                          .setDataRegistries(aliceDataRegistries).build();
        assertDoesNotThrow(() -> registrySet.update());
        assertNotNull(registrySet);
    }

    @Test
    @DisplayName("Delete crud registry set")
    void deleteRegistrySet() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/registries");
        RegistrySet registrySet = RegistrySet.get(url, saiSession);
        assertDoesNotThrow(() -> registrySet.delete());
        assertFalse(registrySet.isExists());
    }

    private void checkRegistrySet(RegistrySet registrySet) {
        assertNotNull(registrySet);
        assertEquals(aliceAgentRegistryJsonLd, registrySet.getAgentRegistryUrl());
        assertEquals(aliceAccessConsentRegistryJsonLd, registrySet.getAccessConsentRegistryUrl());
        assertTrue(aliceDataRegistries.containsAll(registrySet.getDataRegistryUrls()));
    }

}

package com.janeirodigital.sai.core.agents;

import com.janeirodigital.mockwebserver.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpException;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.mockwebserver.DispatcherHelper.*;
import static com.janeirodigital.mockwebserver.MockWebServerHelper.toMockUri;
import static com.janeirodigital.sai.httputils.ContentType.LD_JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RegistrySetTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;

    private static URI aliceAgentRegistry;
    private static URI aliceAuthorizationRegistry;
    private static URI aliceAgentRegistryJsonLd;
    private static URI aliceAuthorizationRegistryJsonLd;
    private static List<URI> aliceDataRegistries;

    @BeforeAll
    static void beforeAll() throws SaiException, SaiHttpException {

        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
        // GET crud registry set in Turtle
        mockOnGet(dispatcher, "/ttl/registries", "agents/registry-set-ttl");
        mockOnPut(dispatcher, "/new/ttl/registries", "http/201");  // create new
        mockOnPut(dispatcher, "/ttl/registries", "http/204");  // update existing
        mockOnDelete(dispatcher, "/ttl/registries", "http/204");  // delete
        // GET crud registry set in Turtle with missing fields
        mockOnGet(dispatcher, "/missing-fields/ttl/registries", "agents/registry-set-missing-fields-ttl");
        // GET crud registry set in JSON-LD
        mockOnGet(dispatcher, "/jsonld/registries", "agents/registry-set-jsonld");
        mockOnPut(dispatcher, "/new/jsonld/registries", "http/201");  // create new
        mockOnPut(dispatcher, "/jsonld/registries", "http/204");  // update existing or delete
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        aliceAgentRegistry = toMockUri(server,"/ttl/agents/");
        aliceAuthorizationRegistry = toMockUri(server, "/ttl/authorization/");
        aliceAgentRegistryJsonLd = toMockUri(server,"/jsonld/agents/");
        aliceAuthorizationRegistryJsonLd = toMockUri(server, "/jsonld/authorization/");
        aliceDataRegistries = Arrays.asList(URI.create("https://work.alice.example/data/"), URI.create("https://personal.alice.example/data/"));
    }

    @Test
    @DisplayName("Create new crud registry set")
    void createNewCrudRegistrySet() throws SaiException {
        URI url = toMockUri(server, "/new/ttl/registries");
        RegistrySet.Builder builder = new RegistrySet.Builder(url, saiSession);
        RegistrySet registrySet = builder.setAgentRegistry(aliceAgentRegistry).setAuthorizationRegistry(aliceAuthorizationRegistry)
                                         .setDataRegistries(aliceDataRegistries).build();
        assertDoesNotThrow(() -> registrySet.update());
        assertNotNull(registrySet);
    }

    @Test
    @DisplayName("Read crud registry set")
    void readRegistrySet() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/ttl/registries");
        RegistrySet registrySet = RegistrySet.get(url, saiSession);
        checkRegistrySet(registrySet);
    }

    @Test
    @DisplayName("Reload crud registry set")
    void reloadRegistrySet() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/ttl/registries");
        RegistrySet registrySet = RegistrySet.get(url, saiSession);
        RegistrySet reloaded = registrySet.reload();
        checkRegistrySet(reloaded);
    }

    @Test
    @DisplayName("Fail to read existing crud registry set in turtle - missing required fields")
    void failToReadRegistrySet() {
        URI url = toMockUri(server, "/missing-fields/ttl/registries");
        assertThrows(SaiException.class, () -> RegistrySet.get(url, saiSession));
    }

    @Test
    @DisplayName("Update existing crud registry set")
    void updateRegistrySet() throws SaiException, SaiHttpNotFoundException, SaiHttpException {
        URI url = toMockUri(server, "/ttl/registries");
        RegistrySet existing = RegistrySet.get(url, saiSession);
        existing.setAgentRegistryUri(URI.create("https://alice.example/otheragents/"));
        assertDoesNotThrow(() -> existing.update());
        assertNotNull(existing);
    }

    @Test
    @DisplayName("Read existing registry set in JSON-LD")
    void readRegistrySetJsonLd() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/jsonld/registries");
        RegistrySet registrySet = RegistrySet.get(url, saiSession, LD_JSON);
        checkRegistrySetJsonLd(registrySet);
    }

    @Test
    @DisplayName("Create new crud registry set in JSON-LD")
    void createNewCrudRegistrySetJsonLd() throws SaiException {
        URI url = toMockUri(server, "/new/jsonld/registries");
        RegistrySet.Builder builder = new RegistrySet.Builder(url, saiSession);
        RegistrySet registrySet = builder.setContentType(LD_JSON).setAgentRegistry(aliceAgentRegistry)
                                          .setAuthorizationRegistry(aliceAuthorizationRegistry)
                                          .setDataRegistries(aliceDataRegistries).build();
        assertDoesNotThrow(() -> registrySet.update());
        assertNotNull(registrySet);
    }

    @Test
    @DisplayName("Delete crud registry set")
    void deleteRegistrySet() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/ttl/registries");
        RegistrySet registrySet = RegistrySet.get(url, saiSession);
        assertDoesNotThrow(() -> registrySet.delete());
        assertFalse(registrySet.isExists());
    }

    private void checkRegistrySet(RegistrySet registrySet) {
        assertNotNull(registrySet);
        assertEquals(aliceAgentRegistry, registrySet.getAgentRegistryUri());
        assertEquals(aliceAuthorizationRegistry, registrySet.getAuthorizationRegistryUri());
        assertTrue(aliceDataRegistries.containsAll(registrySet.getDataRegistryUris()));
    }

    private void checkRegistrySetJsonLd(RegistrySet registrySet) {
        assertNotNull(registrySet);
        assertEquals(aliceAgentRegistryJsonLd, registrySet.getAgentRegistryUri());
        assertEquals(aliceAuthorizationRegistryJsonLd, registrySet.getAuthorizationRegistryUri());
        assertTrue(aliceDataRegistries.containsAll(registrySet.getDataRegistryUris()));
    }

}

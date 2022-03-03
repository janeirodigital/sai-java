package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.exceptions.SaiRuntimeException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.janeirodigital.sai.core.enums.ContentType.LD_JSON;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.*;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static com.janeirodigital.sai.core.helpers.HttpHelper.stringToUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AgentRegistryTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static List<URL> aliceSocialAgents;
    private static List<URL> aliceApplications;

    @BeforeAll
    static void beforeAll() throws SaiException {

        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        // GET agent registry in Turtle
        mockOnGet(dispatcher, "/ttl/agents/", "crud/agent-registry-ttl");
        mockOnGet(dispatcher, "/ttl/empty/agents/", "crud/agent-registry-empty-ttl");
        mockOnGet(dispatcher, "/ttl/agents/sa-1/", "crud/social-agent-registration-1-ttl");
        mockOnGet(dispatcher, "/ttl/agents/sa-2/", "crud/social-agent-registration-2-ttl");
        mockOnGet(dispatcher, "/ttl/agents/sa-3/", "crud/social-agent-registration-3-ttl");
        mockOnGet(dispatcher, "/ttl/agents/sa-4/", "crud/social-agent-registration-4-ttl");
        mockOnGet(dispatcher, "/ttl/agents/app-1/", "crud/application-registration-1-ttl");
        mockOnGet(dispatcher, "/ttl/agents/app-2/", "crud/application-registration-2-ttl");
        mockOnGet(dispatcher, "/ttl/agents/app-3/", "crud/application-registration-3-ttl");
        mockOnPut(dispatcher, "/new/ttl/agents/", "http/201");  // create new
        mockOnPut(dispatcher, "/ttl/agents/", "http/204");  // update existing
        mockOnDelete(dispatcher, "/ttl/agents/", "http/204");  // delete
        // GET agent registry in Turtle with invalid fields
        mockOnGet(dispatcher, "/invalid-fields/ttl/agents/", "crud/agent-registry-invalid-ttl");
        // GET agent registry in Turtle with links to registrations that don't exist
        mockOnGet(dispatcher, "/missing-registrations/ttl/agents/", "crud/agent-registry-missing-registrations-ttl");
        // GET agent registry in JSON-LD
        mockOnGet(dispatcher, "/jsonld/agents/", "crud/agent-registry-jsonld");
        mockOnPut(dispatcher, "/new/jsonld/agents/", "http/201");  // create new
        mockOnPut(dispatcher, "/jsonld/agents/", "http/204");  // update existing or delete
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        aliceSocialAgents = Arrays.asList(toUrl(server, "/ttl/agents/sa-1/"),
                                          toUrl(server, "/ttl/agents/sa-2/"),
                                          toUrl(server, "/ttl/agents/sa-3/"),
                                          toUrl(server, "/ttl/agents/sa-4/"));
        aliceApplications = Arrays.asList(toUrl(server, "/ttl/agents/app-1/"),
                                          toUrl(server, "/ttl/agents/app-2/"),
                                          toUrl(server, "/ttl/agents/app-3/"));
    }

    @Test
    @DisplayName("Create an agent registry")
    void createNewAgentRegistry() throws SaiException {
        URL url = toUrl(server, "/new/ttl/agents/");
        AgentRegistry agentRegistry = new AgentRegistry.Builder(url, saiSession).build();
        assertDoesNotThrow(() -> agentRegistry.update());
        assertNotNull(agentRegistry);
    }

    @Test
    @DisplayName("Get an agent registry")
    void readAgentRegistry() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(url, saiSession);
        checkRegistry(agentRegistry, false);
    }

    @Test
    @DisplayName("Get an empty agent registry")
    void readEmptyAgentRegistry() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/empty/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(url, saiSession);
        checkRegistry(agentRegistry, true);
    }

    @Test
    @DisplayName("Fail to get agent registry - invalid fields")
    void failToGetAgentRegistry() {
        URL url = toUrl(server, "/invalid-fields/ttl/agents/");
        assertThrows(SaiException.class, () -> AgentRegistry.get(url, saiSession));
    }

    @Test
    @DisplayName("Reload agent registry")
    void reloadAgentRegistry() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(url, saiSession);
        AgentRegistry reloaded = agentRegistry.reload();
        checkRegistry(reloaded, false);
    }

    @Test
    @DisplayName("Find a social agent registration")
    void findSocialAgentRegistration() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/agents/");
        URL toFind = stringToUrl("https://bob.example/id#me");
        URL toFail = stringToUrl("https://who.example/id#nobody");
        AgentRegistry agentRegistry = AgentRegistry.get(url, saiSession);
        SocialAgentRegistration found = agentRegistry.getSocialAgentRegistrations().find(toFind);
        assertEquals(toFind, found.getRegisteredAgent());
        SocialAgentRegistration fail = agentRegistry.getSocialAgentRegistrations().find(toFail);
        assertNull(fail);
    }

    @Test
    @DisplayName("Fail to iterate social agent registrations - missing registration")
    void failToFindSocialAgentRegistrationMissing() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/missing-registrations/ttl/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(url, saiSession);
        Iterator<SocialAgentRegistration> iterator = agentRegistry.getSocialAgentRegistrations().iterator();
        assertThrows(SaiRuntimeException.class, () -> iterator.next());
    }

    @Test
    @DisplayName("Find an application registration")
    void findApplicationRegistration() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/agents/");
        URL toFind = stringToUrl("https://projectron.example/id#app");
        URL toFail = stringToUrl("https://app.example/id#nothing");
        AgentRegistry agentRegistry = AgentRegistry.get(url, saiSession);
        ApplicationRegistration found = agentRegistry.getApplicationRegistrations().find(toFind);
        assertEquals(toFind, found.getRegisteredAgent());
        ApplicationRegistration fail = agentRegistry.getApplicationRegistrations().find(toFail);
        assertNull(fail);
    }

    @Test
    @DisplayName("Fail to iterate application registrations - missing registration")
    void failToFindApplicationRegistrationMissing() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/missing-registrations/ttl/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(url, saiSession);
        Iterator<ApplicationRegistration> iterator = agentRegistry.getApplicationRegistrations().iterator();
        assertThrows(SaiRuntimeException.class, () -> iterator.next());
    }
    
    @Test
    @DisplayName("Update registration in a crud agent registry")
    void updateAgentRegistry() throws SaiException, SaiAlreadyExistsException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(url, saiSession);
        agentRegistry.getSocialAgentRegistrations().remove(toUrl(server, "/ttl/agents/sa-1/"));
        agentRegistry.getSocialAgentRegistrations().add(toUrl(server, "/ttl/agents/sa-5/"));
        agentRegistry.getApplicationRegistrations().remove(toUrl(server, "/ttl/agents/app-1/"));
        agentRegistry.getApplicationRegistrations().add(toUrl(server, "/ttl/agents/app-5/"));
        assertDoesNotThrow(() -> agentRegistry.update());
    }

    @Test
    @DisplayName("Read existing agent registry in JSON-LD")
    void readAgentRegistryJsonLd() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/jsonld/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(url, saiSession, LD_JSON);
        checkRegistry(agentRegistry, false);
    }

    @Test
    @DisplayName("Create new crud agent registry in JSON-LD")
    void createNewAgentRegistryJsonLd() throws SaiException {
        URL url = toUrl(server, "/new/jsonld/agents/");
        AgentRegistry agentRegistry = new AgentRegistry.Builder(url, saiSession).setContentType(LD_JSON).build();
        assertDoesNotThrow(() -> agentRegistry.update());
        assertNotNull(agentRegistry);
    }

    @Test
    @DisplayName("Delete crud agent registry")
    void deleteAgentRegistry() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(url, saiSession);
        assertDoesNotThrow(() -> agentRegistry.delete());
        assertFalse(agentRegistry.isExists());
    }

    private void checkRegistry(AgentRegistry agentRegistry, boolean requiredOnly) {
        assertNotNull(agentRegistry);
        assertTrue(aliceSocialAgents.containsAll(agentRegistry.getSocialAgentRegistrations().getRegistrationUrls()));
        assertTrue(aliceApplications.containsAll(agentRegistry.getApplicationRegistrations().getRegistrationUrls()));
        if (!requiredOnly) {
            for (SocialAgentRegistration registration : agentRegistry.getSocialAgentRegistrations()) {
                assertTrue(aliceSocialAgents.contains(registration.getUrl()));
            }
            for (ApplicationRegistration registration : agentRegistry.getApplicationRegistrations()) {
                assertTrue(aliceApplications.contains(registration.getUrl()));
            }
        }
    }
}

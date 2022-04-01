package com.janeirodigital.sai.core.agents;

import com.janeirodigital.mockwebserver.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.agents.AgentRegistry;
import com.janeirodigital.sai.core.agents.ApplicationRegistration;
import com.janeirodigital.sai.core.agents.SocialAgentRegistration;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiRuntimeException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.janeirodigital.mockwebserver.DispatcherHelper.*;
import static com.janeirodigital.mockwebserver.MockWebServerHelper.toMockUri;
import static com.janeirodigital.sai.httputils.ContentType.LD_JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentRegistryTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static List<URI> aliceSocialAgents;
    private static List<URI> aliceApplications;

    @BeforeAll
    static void beforeAll() throws SaiException {

        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        // GET agent registry in Turtle
        mockOnGet(dispatcher, "/ttl/agents/", "agents/agent-registry-ttl");
        mockOnGet(dispatcher, "/ttl/empty/agents/", "agents/agent-registry-empty-ttl");
        mockOnGet(dispatcher, "/ttl/agents/sa-1/", "agents/social-agent-registration-1-ttl");
        mockOnGet(dispatcher, "/ttl/agents/sa-2/", "agents/social-agent-registration-2-ttl");
        mockOnGet(dispatcher, "/ttl/agents/sa-3/", "agents/social-agent-registration-3-ttl");
        mockOnGet(dispatcher, "/ttl/agents/sa-4/", "agents/social-agent-registration-4-ttl");
        mockOnGet(dispatcher, "/ttl/agents/app-1/", "agents/application-registration-1-ttl");
        mockOnGet(dispatcher, "/ttl/agents/app-2/", "agents/application-registration-2-ttl");
        mockOnGet(dispatcher, "/ttl/agents/app-3/", "agents/application-registration-3-ttl");
        mockOnPut(dispatcher, "/new/ttl/agents/", "http/201");  // create new
        mockOnPut(dispatcher, "/ttl/agents/", "http/204");  // update existing
        mockOnDelete(dispatcher, "/ttl/agents/", "http/204");  // delete
        // GET agent registry in Turtle with invalid fields
        mockOnGet(dispatcher, "/invalid-fields/ttl/agents/", "agents/agent-registry-invalid-ttl");
        // GET agent registry in Turtle with links to registrations that don't exist
        mockOnGet(dispatcher, "/missing-registrations/ttl/agents/", "agents/agent-registry-missing-registrations-ttl");
        // GET agent registry in JSON-LD
        mockOnGet(dispatcher, "/jsonld/agents/", "agents/agent-registry-jsonld");
        mockOnPut(dispatcher, "/new/jsonld/agents/", "http/201");  // create new
        mockOnPut(dispatcher, "/jsonld/agents/", "http/204");  // update existing or delete
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        aliceSocialAgents = Arrays.asList(toMockUri(server, "/ttl/agents/sa-1/"),
                                          toMockUri(server, "/ttl/agents/sa-2/"),
                                          toMockUri(server, "/ttl/agents/sa-3/"),
                                          toMockUri(server, "/ttl/agents/sa-4/"));
        aliceApplications = Arrays.asList(toMockUri(server, "/ttl/agents/app-1/"),
                                          toMockUri(server, "/ttl/agents/app-2/"),
                                          toMockUri(server, "/ttl/agents/app-3/"));
    }

    @Test
    @DisplayName("Create an agent registry")
    void createNewAgentRegistry() throws SaiException {
        URI uri = toMockUri(server, "/new/ttl/agents/");
        AgentRegistry agentRegistry = new AgentRegistry.Builder(uri, saiSession).build();
        assertDoesNotThrow(() -> agentRegistry.update());
        assertNotNull(agentRegistry);
    }

    @Test
    @DisplayName("Get an agent registry")
    void readAgentRegistry() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/ttl/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(uri, saiSession);
        checkRegistry(agentRegistry, false);
        assertFalse(agentRegistry.isEmpty());
    }

    @Test
    @DisplayName("Get an empty agent registry")
    void readEmptyAgentRegistry() throws SaiException, SaiHttpNotFoundException, SaiAlreadyExistsException {
        URI uri = toMockUri(server, "/ttl/empty/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(uri, saiSession);
        checkRegistry(agentRegistry, true);
        assertTrue(agentRegistry.isEmpty());
        agentRegistry.getApplicationRegistrations().add(toMockUri(server, "/ttl/empty/agents/app-555"));
        assertFalse(agentRegistry.isEmpty());
    }

    @Test
    @DisplayName("Fail to get agent registry - invalid fields")
    void failToGetAgentRegistry() {
        URI uri = toMockUri(server, "/invalid-fields/ttl/agents/");
        assertThrows(SaiException.class, () -> AgentRegistry.get(uri, saiSession));
    }

    @Test
    @DisplayName("Reload agent registry")
    void reloadAgentRegistry() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/ttl/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(uri, saiSession);
        AgentRegistry reloaded = agentRegistry.reload();
        checkRegistry(reloaded, false);
    }

    @Test
    @DisplayName("Find a social agent registration")
    void findSocialAgentRegistration() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/ttl/agents/");
        URI toFind = URI.create("https://bob.example/id#me");
        URI toFail = URI.create("https://who.example/id#nobody");
        AgentRegistry agentRegistry = AgentRegistry.get(uri, saiSession);
        SocialAgentRegistration found = agentRegistry.getSocialAgentRegistrations().find(toFind);
        assertEquals(toFind, found.getRegisteredAgent());
        SocialAgentRegistration fail = agentRegistry.getSocialAgentRegistrations().find(toFail);
        assertNull(fail);
    }

    @Test
    @DisplayName("Fail to iterate social agent registrations - missing registration")
    void failToFindSocialAgentRegistrationMissing() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/missing-registrations/ttl/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(uri, saiSession);
        Iterator<SocialAgentRegistration> iterator = agentRegistry.getSocialAgentRegistrations().iterator();
        assertThrows(SaiRuntimeException.class, () -> iterator.next());
    }

    @Test
    @DisplayName("Find an application registration")
    void findApplicationRegistration() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/ttl/agents/");
        URI toFind = URI.create("https://projectron.example/id#app");
        URI toFail = URI.create("https://app.example/id#nothing");
        AgentRegistry agentRegistry = AgentRegistry.get(uri, saiSession);
        ApplicationRegistration found = agentRegistry.getApplicationRegistrations().find(toFind);
        assertEquals(toFind, found.getRegisteredAgent());
        ApplicationRegistration fail = agentRegistry.getApplicationRegistrations().find(toFail);
        assertNull(fail);
    }

    @Test
    @DisplayName("Fail to iterate application registrations - missing registration")
    void failToFindApplicationRegistrationMissing() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/missing-registrations/ttl/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(uri, saiSession);
        Iterator<ApplicationRegistration> iterator = agentRegistry.getApplicationRegistrations().iterator();
        assertThrows(SaiRuntimeException.class, () -> iterator.next());
    }
    
    @Test
    @DisplayName("Update registration in a crud agent registry")
    void updateAgentRegistry() throws SaiException, SaiAlreadyExistsException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/ttl/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(uri, saiSession);
        agentRegistry.getSocialAgentRegistrations().remove(toMockUri(server, "/ttl/agents/sa-1/"));
        agentRegistry.getSocialAgentRegistrations().add(toMockUri(server, "/ttl/agents/sa-5/"));
        agentRegistry.getApplicationRegistrations().remove(toMockUri(server, "/ttl/agents/app-1/"));
        agentRegistry.getApplicationRegistrations().add(toMockUri(server, "/ttl/agents/app-5/"));
        assertDoesNotThrow(() -> agentRegistry.update());
    }

    @Test
    @DisplayName("Read existing agent registry in JSON-LD")
    void readAgentRegistryJsonLd() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/jsonld/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(uri, saiSession, LD_JSON);
        checkRegistry(agentRegistry, false);
    }

    @Test
    @DisplayName("Create new crud agent registry in JSON-LD")
    void createNewAgentRegistryJsonLd() throws SaiException {
        URI uri = toMockUri(server, "/new/jsonld/agents/");
        AgentRegistry agentRegistry = new AgentRegistry.Builder(uri, saiSession).setContentType(LD_JSON).build();
        assertDoesNotThrow(() -> agentRegistry.update());
        assertNotNull(agentRegistry);
    }

    @Test
    @DisplayName("Delete crud agent registry")
    void deleteAgentRegistry() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/ttl/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(uri, saiSession);
        assertDoesNotThrow(() -> agentRegistry.delete());
        assertFalse(agentRegistry.isExists());
    }

    @Test
    @DisplayName("Add agent registrations to agent registry")
    void addAgentRegistrations() throws SaiException, SaiHttpNotFoundException, SaiAlreadyExistsException {
        URI uri = toMockUri(server, "/ttl/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(uri, saiSession);

        URI saUri = toMockUri(server, "/ttl/agents/sa-786/");
        URI saAgent = URI.create("https://peter.example/id#me");
        SocialAgentRegistration sa = mock(SocialAgentRegistration.class);
        when(sa.getUri()).thenReturn(saUri);
        when(sa.getRegisteredAgent()).thenReturn(saAgent);
        agentRegistry.add(sa);

        URI appUri = toMockUri(server, "/ttl/agents/app-776/");
        URI appAgent = URI.create("https://superapp.example/id#app");
        ApplicationRegistration app = mock(ApplicationRegistration.class);
        when(app.getUri()).thenReturn(appUri);
        when(app.getRegisteredAgent()).thenReturn(appAgent);
        assertDoesNotThrow(() -> agentRegistry.add(app));
    }

    @Test
    @DisplayName("Remove agent registrations from agent registry")
    void removeAgentRegistrations() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/ttl/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(uri, saiSession);

        URI saUri = toMockUri(server, "/ttl/agents/sa-1/");
        SocialAgentRegistration sa = mock(SocialAgentRegistration.class);
        when(sa.getUri()).thenReturn(saUri);
        agentRegistry.remove(sa);

        URI appUri = toMockUri(server, "/ttl/agents/app-1/");
        ApplicationRegistration app = mock(ApplicationRegistration.class);
        when(app.getUri()).thenReturn(appUri);
        assertDoesNotThrow(() -> agentRegistry.remove(app));
    }

    @Test
    @DisplayName("Fail to add agent registrations to agent registry - already exists")
    void failToAddSocialAgentRegistration() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/ttl/agents/");
        AgentRegistry agentRegistry = AgentRegistry.get(uri, saiSession);

        URI saUri = toMockUri(server, "/ttl/agents/sa-1/");
        URI saAgent = URI.create("https://bob.example/id#me");
        SocialAgentRegistration sa = mock(SocialAgentRegistration.class);
        when(sa.getUri()).thenReturn(saUri);
        when(sa.getRegisteredAgent()).thenReturn(saAgent);
        assertThrows(SaiAlreadyExistsException.class, () -> agentRegistry.add(sa));

        URI appUri = toMockUri(server, "/ttl/agents/app-1/");
        URI appAgent = URI.create("https://projectron.example/id#app");
        ApplicationRegistration app = mock(ApplicationRegistration.class);
        when(app.getUri()).thenReturn(appUri);
        when(app.getRegisteredAgent()).thenReturn(appAgent);
        assertThrows(SaiAlreadyExistsException.class, () -> agentRegistry.add(app));
    }

    private void checkRegistry(AgentRegistry agentRegistry, boolean requiredOnly) {
        assertNotNull(agentRegistry);
        assertTrue(aliceSocialAgents.containsAll(agentRegistry.getSocialAgentRegistrations().getRegistrationUris()));
        assertTrue(aliceApplications.containsAll(agentRegistry.getApplicationRegistrations().getRegistrationUris()));
        if (!requiredOnly) {
            for (SocialAgentRegistration registration : agentRegistry.getSocialAgentRegistrations()) {
                assertTrue(aliceSocialAgents.contains(registration.getUri()));
            }
            for (ApplicationRegistration registration : agentRegistry.getApplicationRegistrations()) {
                assertTrue(aliceApplications.contains(registration.getUri()));
            }
        }
    }
}

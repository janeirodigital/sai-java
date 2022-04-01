package com.janeirodigital.sai.core.utils;

import com.janeirodigital.mockwebserver.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.agents.AgentRegistry;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.mockwebserver.DispatcherHelper.mockOnGet;
import static com.janeirodigital.mockwebserver.MockWebServerHelper.toMockUri;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class RegistrationListTests {

    private static SaiSession saiSession;
    private static MockWebServer server;

    @BeforeAll
    static void beforeAll() throws SaiException {
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        mockOnGet(dispatcher, "/ttl/agents/", "agents/agent-registry-ttl");
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
    }

    @Test
    @DisplayName("Add registrations to a registration list")
    void addAgentRegistrations() throws SaiException, SaiHttpNotFoundException, SaiAlreadyExistsException {
        URI url = toMockUri(server, "/ttl/agents/");
        URI saNewUri = toMockUri(server, "/ttl/agents/sa-66/");
        List<URI> newSaUris = Arrays.asList(toMockUri(server, "/ttl/agents/sa-67/"), toMockUri(server, "/ttl/agents/sa-68/"));
        AgentRegistry agentRegistry = AgentRegistry.get(url, saiSession);
        agentRegistry.getSocialAgentRegistrations().add(saNewUri);
        assertTrue(agentRegistry.getSocialAgentRegistrations().isPresent(saNewUri));
        agentRegistry.getSocialAgentRegistrations().addAll(newSaUris);
        for (URI added : newSaUris) { assertTrue(agentRegistry.getSocialAgentRegistrations().isPresent(added)); }
    }

    @Test
    @DisplayName("Fail to add registrations - already exists")
    void failToAddExistingRegistrations() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/ttl/agents/");
        URI saExistsUri = toMockUri(server, "/ttl/agents/sa-4/");
        List<URI> existingSaUris = Arrays.asList(saExistsUri, toMockUri(server, "/ttl/agents/sa-2/"));
        AgentRegistry agentRegistry = AgentRegistry.get(url, saiSession);
        assertThrows(SaiAlreadyExistsException.class, () -> agentRegistry.getSocialAgentRegistrations().add(saExistsUri));
        assertThrows(SaiAlreadyExistsException.class, () -> agentRegistry.getSocialAgentRegistrations().addAll(existingSaUris));
    }

}

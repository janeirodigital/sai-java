package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
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

import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnGet;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
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
        mockOnGet(dispatcher, "/ttl/agents/", "crud/agent-registry-ttl");
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
    }

    @Test
    @DisplayName("Add registrations to a registration list")
    void addAgentRegistrations() throws SaiException, SaiNotFoundException, SaiAlreadyExistsException {
        URL url = toUrl(server, "/ttl/agents/");
        URL saNewUrl = toUrl(server, "/ttl/agents/sa-66/");
        List<URL> newSaUrls = Arrays.asList(toUrl(server, "/ttl/agents/sa-67/"), toUrl(server, "/ttl/agents/sa-68/"));
        AgentRegistry agentRegistry = AgentRegistry.get(url, saiSession);
        agentRegistry.getSocialAgentRegistrations().add(saNewUrl);
        assertTrue(agentRegistry.getSocialAgentRegistrations().isPresent(saNewUrl));
        agentRegistry.getSocialAgentRegistrations().addAll(newSaUrls);
        for (URL added : newSaUrls) { assertTrue(agentRegistry.getSocialAgentRegistrations().isPresent(added)); }
    }

    @Test
    @DisplayName("Fail to add registrations - already exists")
    void failToAddExistingRegistrations() throws SaiException, SaiNotFoundException, SaiAlreadyExistsException {
        URL url = toUrl(server, "/ttl/agents/");
        URL saExistsUrl = toUrl(server, "/ttl/agents/sa-4/");
        List<URL> existingSaUrls = Arrays.asList(saExistsUrl, toUrl(server, "/ttl/agents/sa-2/"));
        AgentRegistry agentRegistry = AgentRegistry.get(url, saiSession);
        assertThrows(SaiAlreadyExistsException.class, () -> agentRegistry.getSocialAgentRegistrations().add(saExistsUrl));
        assertThrows(SaiAlreadyExistsException.class, () -> agentRegistry.getSocialAgentRegistrations().addAll(existingSaUrls));
    }

}

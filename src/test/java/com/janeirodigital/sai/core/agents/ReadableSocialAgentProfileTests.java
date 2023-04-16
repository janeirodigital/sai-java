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

import static com.janeirodigital.mockwebserver.DispatcherHelper.mockOnGet;
import static com.janeirodigital.mockwebserver.MockWebServerHelper.toMockUri;
import static com.janeirodigital.sai.httputils.ContentType.LD_JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ReadableSocialAgentProfileTests {

    private static SaiSession saiSession;
    private static MockWebServer server;

    @BeforeAll
    static void beforeAll() throws SaiException {
        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        // GET Readable social agent profile in Turtle
        mockOnGet(dispatcher, "/ttl/id", "agents/social-agent-profile-ttl");
        // GET Readable social agent profile in Turtle
        mockOnGet(dispatcher, "/missing/ttl/id", "agents/social-agent-profile-missing-fields-ttl");
        // GET Readable social agent profile in JSON-LD
        mockOnGet(dispatcher, "/jsonld/id", "agents/social-agent-profile-jsonld");
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));
    }

    @Test
    @DisplayName("Get readable social agent profile document")
    void getReadableSocialAgentProfileTurtle() throws SaiException, SaiHttpNotFoundException, SaiHttpException {
        ReadableSocialAgentProfile profile = ReadableSocialAgentProfile.get(toMockUri(server, "/ttl/id"), saiSession);
        checkProfile(profile);
    }

    @Test
    @DisplayName("Fail to get readable social agent profile document - missing required fields")
    void failToGetReadableSocialAgentProfileRequired() {
        assertThrows(SaiException.class, () -> ReadableSocialAgentProfile.get(toMockUri(server, "/missing/ttl/id"), saiSession));
    }

    @Test
    @DisplayName("Reload social agent profile document")
    void reloadReadableSocialAgentProfileTurtle() throws SaiException, SaiHttpNotFoundException, SaiHttpException {
        ReadableSocialAgentProfile profile = ReadableSocialAgentProfile.get(toMockUri(server, "/ttl/id"), saiSession);
        ReadableSocialAgentProfile reloaded = profile.reload();
        assertNotEquals(profile, reloaded);
        checkProfile(profile);
        checkProfile(reloaded);
    }

    @Test
    @DisplayName("Get readable social agent profile document as json-ld")
    void getReadableSocialAgentProfileJsonLd() throws SaiException, SaiHttpNotFoundException, SaiHttpException {
        ReadableSocialAgentProfile profile = ReadableSocialAgentProfile.get(toMockUri(server, "/jsonld/id"), saiSession, LD_JSON);
        checkProfile(profile);
    }

    private void checkProfile(ReadableSocialAgentProfile profile) throws SaiHttpException {
        assertNotNull(profile);
        assertEquals(URI.create("https://trusted.example/alice/"), profile.getAuthorizationAgentUri());
        assertEquals(toMockUri(server, "/registry_set"), profile.getRegistrySetUri());
        assertTrue(profile.getOidcIssuerUris().contains(URI.create("https://idp.alice.example")));
    }

}

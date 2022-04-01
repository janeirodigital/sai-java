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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReadableApplicationProfileTests {

    private static SaiSession saiSession;
    private static MockWebServer server;

    @BeforeAll
    static void beforeAll() throws SaiException  {
        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        // Provide a social agent endpoint for the authorized session
        mockOnGet(dispatcher, "/ttl/id", "agents/social-agent-profile-ttl");
        // In a given test, the first request to this endpoint will return provider-response, the second will return provider-refresh (a different token)
        mockOnGet(dispatcher, "/jsonld/projectron/id", "agents/application-profile-jsonld");
        mockOnGet(dispatcher, "/missing-fields/jsonld/projectron/id", "agents/application-profile-missing-fields-jsonld");
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        when(mockSession.getSocialAgentId()).thenReturn(toMockUri(server, "/ttl/id"));
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));
    }

    @Test
    @DisplayName("Get readable application profile document as json-ld")
    void getReadableApplicationProfileAsJsonLd() throws SaiException, SaiHttpNotFoundException, SaiHttpException {
        ReadableApplicationProfile profile = ReadableApplicationProfile.get(toMockUri(server, "/jsonld/projectron/id"), saiSession);
        checkProfile(profile);
    }

    @Test
    @DisplayName("Reload readable application profile document as json-ld")
    void reloadReadableApplicationProfileAsJsonLd() throws SaiException, SaiHttpNotFoundException, SaiHttpException {
        ReadableApplicationProfile profile = ReadableApplicationProfile.get(toMockUri(server, "/jsonld/projectron/id"), saiSession);
        ReadableApplicationProfile reloaded = profile.reload();
        assertNotEquals(profile, reloaded);
        checkProfile(profile);
        checkProfile(reloaded);
    }

    @Test
    @DisplayName("Fail to get readable application profile document - missing fields")
    void failToGetReadableApplicationProfileMissingFields() {
        assertThrows(SaiException.class, () -> ReadableApplicationProfile.get(toMockUri(server, "/missing-fields/jsonld/projectron/id"), saiSession));
    }

    private void checkProfile(ReadableApplicationProfile profile) throws SaiHttpException {
        assertEquals("Projectron", profile.getName());
        assertEquals(URI.create("http://projectron.example/logo.png"), profile.getLogoUri());
        assertEquals("Best project management ever", profile.getDescription());
        assertEquals(URI.create("http://acme.example/id"), profile.getAuthorUri());
        assertTrue(profile.getAccessNeedGroupUris().contains(URI.create("http://localhost/projectron/access#group1")));
        assertTrue(profile.getAccessNeedGroupUris().contains(URI.create("http://localhost/projectron/access#group2")));
        assertEquals(URI.create("http://projectron.example/"), profile.getClientUri());
        assertTrue((profile.getRedirectUris().contains(toMockUri(server, "/redirect"))));
        assertEquals(URI.create("http://projectron.example/tos.html"), profile.getTosUri());
        assertTrue(profile.getScopes().contains("openid"));
        assertTrue(profile.getScopes().contains("offline_access"));
        assertTrue(profile.getScopes().contains("profile"));
        assertTrue(profile.getGrantTypes().contains("refresh_token"));
        assertTrue(profile.getGrantTypes().contains("authorization_code"));
        assertTrue(profile.getResponseTypes().contains("code"));
        assertEquals(3600, profile.getDefaultMaxAge());
        assertTrue(profile.isRequireAuthTime());
    }

}

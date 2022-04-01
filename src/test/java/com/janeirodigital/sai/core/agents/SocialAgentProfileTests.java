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

import static com.janeirodigital.mockwebserver.DispatcherHelper.*;
import static com.janeirodigital.mockwebserver.MockWebServerHelper.toMockUri;
import static com.janeirodigital.sai.httputils.ContentType.LD_JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SocialAgentProfileTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static URI aliceAuthzAgent;
    private static URI aliceAccessInbox;
    private static URI aliceRegistrySet;
    private static URI aliceOidcIssuer;

    @BeforeAll
    static void beforeAll() throws SaiException, SaiHttpException {

        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        // GET crud social agent profile in Turtle
        mockOnGet(dispatcher, "/ttl/id", "agents/social-agent-profile-ttl");
        mockOnPut(dispatcher, "/new/ttl/id", "http/201");  // create new
        mockOnPut(dispatcher, "/ttl/id", "http/204");  // update existing
        mockOnDelete(dispatcher, "/ttl/id", "http/204");  // delete
        // GET crud social agent profile in Turtle with missing fields
        mockOnGet(dispatcher, "/missing-fields/ttl/id", "agents/social-agent-profile-missing-fields-ttl");
        // GET crud social agent profile in JSON-LD
        mockOnGet(dispatcher, "/jsonld/id", "agents/social-agent-profile-jsonld");
        mockOnPut(dispatcher, "/new/jsonld/id", "http/201");  // create new
        mockOnPut(dispatcher, "/jsonld/id", "http/204");  // update existing or delete
        // GET crud social agent profile in JSON-LD with missing fields
        mockOnGet(dispatcher, "/missing-fields/jsonld/id", "agents/social-agent-profile-missing-fields-jsonld");
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        aliceAuthzAgent = URI.create("https://trusted.example/alice/");
        aliceAccessInbox = toMockUri(server, "/access/inbox/");
        aliceRegistrySet = toMockUri(server, "/registry_set");
        aliceOidcIssuer = URI.create("https://idp.alice.example");
    }

    @Test
    @DisplayName("Create new crud social agent profile")
    void createNewCrudSocialAgentProfile() throws SaiException {
        URI url = toMockUri(server, "/new/ttl/id");
        SocialAgentProfile.Builder builder = new SocialAgentProfile.Builder(url, saiSession);
        SocialAgentProfile profile = builder.setAuthorizationAgent(aliceAuthzAgent).setAccessInbox(aliceAccessInbox)
                                            .setRegistrySet(aliceRegistrySet).setOidcIssuerUris(Arrays.asList(aliceOidcIssuer)).build();
        assertDoesNotThrow(() -> profile.update());
        assertNotNull(profile);
    }

    @Test
    @DisplayName("Read existing crud social agent profile in turtle")
    void readSocialAgentProfile() throws SaiException, SaiHttpNotFoundException, SaiHttpException {
        URI url = toMockUri(server, "/ttl/id");
        SocialAgentProfile profile = SocialAgentProfile.get(url, saiSession);
        checkProfile(profile);
    }

    @Test
    @DisplayName("Reload crud social agent profile")
    void reloadSocialAgentProfile() throws SaiException, SaiHttpNotFoundException, SaiHttpException {
        URI url = toMockUri(server, "/ttl/id");
        SocialAgentProfile profile = SocialAgentProfile.get(url, saiSession);
        SocialAgentProfile reloaded = profile.reload();
        checkProfile(reloaded);
    }

    @Test
    @DisplayName("Fail to read existing crud social agent profile in turtle - missing required fields")
    void failToReadSocialAgentProfile() {
        URI url = toMockUri(server, "/missing-fields/ttl/id");
        assertThrows(SaiException.class, () -> SocialAgentProfile.get(url, saiSession));
    }

    @Test
    @DisplayName("Update existing crud social agent profile in turtle")
    void updateSocialAgentProfile() throws SaiException, SaiHttpNotFoundException, SaiHttpException {
        URI url = toMockUri(server, "/ttl/id");
        SocialAgentProfile profile = SocialAgentProfile.get(url, saiSession);
        profile.setAuthorizationAgentUri(URI.create("https://other.example/alice/"));
        assertDoesNotThrow(() -> profile.update());
        assertNotNull(profile);
    }

    @Test
    @DisplayName("Read existing social agent profile in JSON-LD")
    void readSocialAgentProfileJsonLd() throws SaiException, SaiHttpNotFoundException, SaiHttpException {
        URI url = toMockUri(server, "/jsonld/id");
        SocialAgentProfile profile = SocialAgentProfile.get(url, saiSession, LD_JSON);
        checkProfile(profile);
    }

    @Test
    @DisplayName("Create new crud social agent profile in JSON-LD")
    void createNewCrudSocialAgentProfileJsonLd() throws SaiException {
        URI url = toMockUri(server, "/new/jsonld/id");
        SocialAgentProfile.Builder builder = new SocialAgentProfile.Builder(url, saiSession);
        SocialAgentProfile profile = builder.setContentType(LD_JSON).setAuthorizationAgent(aliceAuthzAgent)
                                            .setAccessInbox(aliceAccessInbox).setRegistrySet(aliceRegistrySet)
                                            .setOidcIssuerUris(Arrays.asList(aliceOidcIssuer)).build();
        assertDoesNotThrow(() -> profile.update());
        assertNotNull(profile);
    }

    @Test
    @DisplayName("Delete crud social agent profile")
    void deleteSocialAgentProfile() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/ttl/id");
        SocialAgentProfile profile = SocialAgentProfile.get(url, saiSession);
        assertDoesNotThrow(() -> profile.delete());
        assertFalse(profile.isExists());
    }

    private void checkProfile(SocialAgentProfile profile) throws SaiException, SaiHttpException {
        assertNotNull(profile);
        assertEquals(URI.create("https://trusted.example/alice/"), profile.getAuthorizationAgentUri());
        assertEquals(aliceAccessInbox, profile.getAccessInboxUri());
        assertEquals(aliceRegistrySet, profile.getRegistrySetUri());
        assertTrue(profile.getOidcIssuerUris().contains(URI.create("https://idp.alice.example")));
    }

}

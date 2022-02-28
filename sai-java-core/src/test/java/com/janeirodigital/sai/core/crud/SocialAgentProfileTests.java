package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Arrays;

import static com.janeirodigital.sai.core.enums.ContentType.LD_JSON;
import static com.janeirodigital.sai.core.enums.ContentType.TEXT_TURTLE;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.*;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static com.janeirodigital.sai.core.helpers.HttpHelper.stringToUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class SocialAgentProfileTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;

    private static URL aliceAuthzAgent;
    private static URL aliceAccessInbox;
    private static URL aliceRegistrySet;
    private static URL aliceOidcIssuer;

    @BeforeAll
    static void beforeAll() throws SaiException, SaiNotFoundException {

        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
        // GET crud social agent profile in Turtle
        mockOnGet(dispatcher, "/ttl/id", "crud/social-agent-profile-ttl");
        mockOnPut(dispatcher, "/new/ttl/id", "http/201");  // create new
        mockOnPut(dispatcher, "/ttl/id", "http/204");  // update existing
        mockOnDelete(dispatcher, "/ttl/id", "http/204");  // delete
        // GET crud social agent profile in Turtle with missing fields
        mockOnGet(dispatcher, "/missing-fields/ttl/id", "crud/social-agent-profile-missing-fields-ttl");
        // GET crud social agent profile in JSON-LD
        mockOnGet(dispatcher, "/jsonld/id", "crud/social-agent-profile-jsonld");
        mockOnPut(dispatcher, "/new/jsonld/id", "http/201");  // create new
        mockOnPut(dispatcher, "/jsonld/id", "http/204");  // update existing or delete
        // GET crud social agent profile in JSON-LD with missing fields
        mockOnGet(dispatcher, "/missing-fields/jsonld/id", "crud/social-agent-profile-missing-fields-jsonld");
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        aliceAuthzAgent = stringToUrl("https://trusted.example/alice/");
        aliceAccessInbox = toUrl(server, "/access/inbox/");
        aliceRegistrySet = toUrl(server, "/registry_set");
        aliceOidcIssuer = stringToUrl("https://idp.alice.example");
    }

    @Test
    @DisplayName("Create new crud social agent profile in turtle")
    void createNewCrudSocialAgentProfile() throws SaiException {
        URL url = toUrl(server, "/new/ttl/id");
        SocialAgentProfile.Builder builder = new SocialAgentProfile.Builder(url, saiSession, TEXT_TURTLE);
        SocialAgentProfile profile = builder.setAuthorizationAgent(aliceAuthzAgent).setAccessInbox(aliceAccessInbox)
                                            .setRegistrySet(aliceRegistrySet).setOidcIssuerUrls(Arrays.asList(aliceOidcIssuer)).build();
        assertDoesNotThrow(() -> profile.update());
        assertNotNull(profile);
    }

    @Test
    @DisplayName("Read existing crud social agent profile in turtle")
    void readSocialAgentProfile() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/id");
        SocialAgentProfile profile = SocialAgentProfile.get(url, saiSession);
        assertNotNull(profile);
        assertEquals(aliceAuthzAgent, profile.getAuthorizationAgentUrl());
        assertEquals(aliceRegistrySet, profile.getRegistrySetUrl());
        assertEquals(aliceAccessInbox, profile.getAccessInboxUrl());
        assertTrue(profile.getOidcIssuerUrls().contains(aliceOidcIssuer));
    }

    @Test
    @DisplayName("Fail to read existing crud social agent profile in turtle - missing required fields")
    void failToReadSocialAgentProfile() throws SaiException {
        URL url = toUrl(server, "/missing-fields/ttl/id");
        assertThrows(SaiException.class, () -> SocialAgentProfile.get(url, saiSession));
    }

    @Test
    @DisplayName("Update existing crud social agent profile in turtle")
    void updateSocialAgentProfile() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/id");
        SocialAgentProfile profile = SocialAgentProfile.get(url, saiSession);
        SocialAgentProfile.Builder builder = new SocialAgentProfile.Builder(url, saiSession, TEXT_TURTLE);
        SocialAgentProfile updated = builder.setDataset(profile.getDataset())
                                            .setAuthorizationAgent(stringToUrl("https://other.example/alice/"))
                                            .build();
        assertDoesNotThrow(() -> updated.update());
        assertNotNull(profile);
    }

    @Test
    @DisplayName("Read existing social agent profile in JSON-LD")
    void readSocialAgentProfileJsonLd() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/jsonld/id");
        SocialAgentProfile profile = SocialAgentProfile.get(url, saiSession, LD_JSON);
        assertNotNull(profile);
        assertEquals(aliceAuthzAgent, profile.getAuthorizationAgentUrl());
        assertEquals(aliceRegistrySet, profile.getRegistrySetUrl());
        assertEquals(aliceAccessInbox, profile.getAccessInboxUrl());
        assertTrue(profile.getOidcIssuerUrls().contains(aliceOidcIssuer));
    }

    @Test
    @DisplayName("Create new crud social agent profile in JSON-LD")
    void createNewCrudSocialAgentProfileJsonLd() throws SaiException {
        URL url = toUrl(server, "/new/jsonld/id");
        SocialAgentProfile.Builder builder = new SocialAgentProfile.Builder(url, saiSession, LD_JSON);
        SocialAgentProfile profile = builder.setAuthorizationAgent(aliceAuthzAgent).setAccessInbox(aliceAccessInbox)
                .setRegistrySet(aliceRegistrySet).setOidcIssuerUrls(Arrays.asList(aliceOidcIssuer)).build();
        assertDoesNotThrow(() -> profile.update());
        assertNotNull(profile);
    }

    @Test
    @DisplayName("Delete crud social agent profile")
    void deleteSocialAgentProfile() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/id");
        SocialAgentProfile profile = SocialAgentProfile.get(url, saiSession);
        assertDoesNotThrow(() -> profile.delete());
        assertFalse(profile.isExists());
    }

}

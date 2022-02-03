package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import com.janeirodigital.sai.core.fixtures.MockWebServerHelper;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnGet;
import static com.janeirodigital.sai.core.helpers.HttpHelper.stringToUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ReadableSocialAgentProfileTests {

    private static DataFactory dataFactory;
    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;

    @BeforeAll
    static void beforeAll() throws SaiException {
        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
        // GET Readable social agent profile in Turtle
        mockOnGet(dispatcher, "/ttl/id", "readable/social-agent-profile-ttl");
        // GET Readable social agent profile in JSON-LD
        mockOnGet(dispatcher, "/jsonld/id", "readable/social-agent-profile-jsonld");
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        dataFactory = new DataFactory(mockSession, new HttpClientFactory(false, false, false));
    }

    @Test
    @DisplayName("Get readable social agent profile document as turtle")
    void getReadableSocialAgentProfileTurtle() throws SaiException, SaiNotFoundException {
        ReadableSocialAgentProfile socialProfile = dataFactory.getReadableSocialAgentProfile(MockWebServerHelper.toUrl(server, "/ttl/id"));
        assertNotNull(socialProfile);
        assertEquals(stringToUrl("https://trusted.example/alice/"), socialProfile.getAuthorizationAgentUrl());
        assertEquals(stringToUrl("https://alice.example/access/inbox/"), socialProfile.getAccessInboxUrl());
        assertEquals(stringToUrl("https://alice.example/registry_set"), socialProfile.getRegistrySetUrl());
        assertTrue(socialProfile.getOidcIssuerUrls().contains(stringToUrl("https://idp.alice.example")));
    }

    // TODO - Include solid-oidc fields as well
    @Test
    @DisplayName("Get readable social agent profile document as json-ld")
    void getReadableSocialAgentProfileJsonLd() throws SaiException, SaiNotFoundException {
        ReadableSocialAgentProfile socialProfile = dataFactory.getReadableSocialAgentProfile(MockWebServerHelper.toUrl(server, "/jsonld/id"));
        assertNotNull(socialProfile);
        assertEquals(stringToUrl("https://trusted.example/alice/"), socialProfile.getAuthorizationAgentUrl());
        assertEquals(stringToUrl("https://alice.example/access/inbox/"), socialProfile.getAccessInboxUrl());
        assertEquals(stringToUrl("https://alice.example/registry_set"), socialProfile.getRegistrySetUrl());
        assertTrue(socialProfile.getOidcIssuerUrls().contains(stringToUrl("https://idp.alice.example")));
    }

}

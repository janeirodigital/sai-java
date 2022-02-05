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
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static com.janeirodigital.sai.core.helpers.HttpHelper.stringToUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ReadableApplicationProfileTests {

    private static DataFactory dataFactory;
    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;

    @BeforeAll
    static void beforeAll() throws SaiException {
        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
        // In a given test, the first request to this endpoint will return provider-response, the second will return provider-refresh (a different token)
        mockOnGet(dispatcher, "/jsonld/projectron/id", "readable/application-profile-jsonld");
        mockOnGet(dispatcher, "/missing-fields/jsonld/projectron/id", "readable/application-profile-missing-fields-jsonld");
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        dataFactory = new DataFactory(mockSession, new HttpClientFactory(false, false, false));
    }

    @Test
    @DisplayName("Get readable application profile document as json-ld")
    void getReadableApplicationProfileAsJsonLd() throws SaiException, SaiNotFoundException {
        ReadableApplicationProfile profile = dataFactory.getReadableApplicationProfile(MockWebServerHelper.toUrl(server, "/jsonld/projectron/id"));
        assertEquals("Projectron", profile.getName());
        assertEquals(stringToUrl("http://projectron.example/logo.png"), profile.getLogoUrl());
        assertEquals("Best project management ever", profile.getDescription());
        assertEquals(stringToUrl("http://acme.example/id"), profile.getAuthorUrl());
        assertTrue(profile.getAccessNeedGroupUrls().contains(stringToUrl("http://localhost/projectron/access#group1")));
        assertTrue(profile.getAccessNeedGroupUrls().contains(stringToUrl("http://localhost/projectron/access#group2")));
        assertEquals(stringToUrl("http://projectron.example/"), profile.getClientUrl());
        assertTrue((profile.getRedirectUrls().contains(toUrl(server, "/redirect"))));
        assertEquals(stringToUrl("http://projectron.example/tos.html"), profile.getTosUrl());
        assertTrue(profile.getScopes().contains("openid"));
        assertTrue(profile.getScopes().contains("offline_access"));
        assertTrue(profile.getScopes().contains("profile"));
        assertTrue(profile.getGrantTypes().contains("refresh_token"));
        assertTrue(profile.getGrantTypes().contains("authorization_code"));
        assertTrue(profile.getResponseTypes().contains("code"));
        assertEquals(3600, profile.getDefaultMaxAge());
        assertEquals(true, profile.isRequireAuthTime());
    }

    @Test
    @DisplayName("Fail to get readable application profile document - missing fields")
    void failToGetReadableApplicationProfileMissingFields() {
        assertThrows(SaiException.class, () -> dataFactory.getReadableApplicationProfile(MockWebServerHelper.toUrl(server, "/missing-fields/jsonld/projectron/id")));
    }


}

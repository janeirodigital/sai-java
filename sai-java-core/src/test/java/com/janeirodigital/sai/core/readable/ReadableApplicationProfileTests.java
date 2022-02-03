package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import com.janeirodigital.sai.core.fixtures.MockWebServerHelper;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnGet;
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
        mockOnGet(dispatcher, "/projectron/ttl/id", "readable/application-profile-ttl");
        mockOnGet(dispatcher, "/projectron/jsonld/id", "readable/application-profile-jsonld");
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        dataFactory = new DataFactory(mockSession, new HttpClientFactory(false, false, false));
    }

    @Test
    @DisplayName("Get readable application profile document as turtle")
    void getReadableApplicationProfile() throws SaiException, SaiNotFoundException {
        ReadableApplicationProfile applicationProfile = dataFactory.getReadableApplicationProfile(MockWebServerHelper.toUrl(server, "/projectron/ttl/id"));
        assertNotNull(applicationProfile);
        assertEquals("Projectron", applicationProfile.getName());
        assertEquals("Manage projects with ease", applicationProfile.getDescription());
        assertEquals("https://acme.example/#id", applicationProfile.getAuthorUrl().toString());
        assertEquals("https://acme.example/thumb.svg", applicationProfile.getThumbnailUrl().toString());
        List<URL> needGroups = Arrays.asList(MockWebServerHelper.toUrl(server, "/projectron/needs#need-group-pm"));
        assertTrue(CollectionUtils.isEqualCollection(needGroups, applicationProfile.getAccessNeedGroupUrls()));
    }

    @Test
    @DisplayName("Get readable application profile document as json-ld")
    void getReadableApplicationProfileAsJsonLd() throws SaiException, SaiNotFoundException {
        ReadableApplicationProfile applicationProfile = dataFactory.getReadableApplicationProfile(MockWebServerHelper.toUrl(server, "/projectron/jsonld/id"));
        assertNotNull(applicationProfile);
        assertEquals("Projectron", applicationProfile.getName());
        assertEquals("Manage projects with ease", applicationProfile.getDescription());
        assertEquals("https://acme.example/#id", applicationProfile.getAuthorUrl().toString());
        assertEquals("https://acme.example/thumb.svg", applicationProfile.getThumbnailUrl().toString());
        List<URL> needGroups = Arrays.asList(MockWebServerHelper.toUrl(server, "/projectron/needs#need-group-pm"));
        assertTrue(CollectionUtils.isEqualCollection(needGroups, applicationProfile.getAccessNeedGroupUrls()));
    }

    // TODO - Include solid-oidc fields as well

}

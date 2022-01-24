package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import com.janeirodigital.sai.core.fixtures.DispatcherEntry;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ReadableApplicationProfileTests {

    private static DataFactory dataFactory;
    private static MockWebServer server;

    @BeforeAll
    static void beforeAll() throws SaiException {
        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        dataFactory = new DataFactory(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher(List.of(
                new DispatcherEntry(List.of("readable/application-profile-ttl"), "GET", "/projectron/id", null),
                new DispatcherEntry(List.of("readable/application-profile-fragment-ttl"), "GET", "/projectron/idf", null)
        ));

        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
    }

    @Test
    @DisplayName("Get a Readable Application Profile Document")
    void getReadableApplicationProfile() throws SaiException, SaiNotFoundException {
        ReadableApplicationProfile applicationProfile = dataFactory.getReadableApplicationProfile(MockWebServerHelper.toUrl(server, "/projectron/id"));
        assertNotNull(applicationProfile);
        assertEquals("Projectron", applicationProfile.getName());
        assertEquals("Manage projects with ease", applicationProfile.getDescription());
        assertEquals("https://acme.example/#id", applicationProfile.getAuthorUrl().toString());
        assertEquals("https://acme.example/thumb.svg", applicationProfile.getThumbnailUrl().toString());
        List<URL> needGroups = Arrays.asList(MockWebServerHelper.toUrl(server, "/projectron/needs#need-group-pm"));
        assertTrue(CollectionUtils.isEqualCollection(needGroups, applicationProfile.getAccessNeedGroupUrls()));
    }

    @Test
    @DisplayName("Get a Readable Application Profile Document as Fragment")
    void getReadableApplicationProfileAsFragment() throws SaiException, SaiNotFoundException {
        ReadableApplicationProfile applicationProfile = dataFactory.getReadableApplicationProfile(MockWebServerHelper.toUrl(server, "/projectron/idf#profile"));
        assertNotNull(applicationProfile);
    }

    // Get a readable resource
    // Fail to get a readable resource that doesn't exist
    // Get a single node from a readable resource
    // Get many nodes from a readable resource
    // Get a graph from a readable resource (GraphReadOnly)

}

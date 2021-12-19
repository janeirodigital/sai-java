package com.janeirodigital.sai.core.tests.readable;

import com.janeirodigital.sai.core.DataFactory;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.readable.ReadableApplicationProfile;
import com.janeirodigital.sai.core.tests.fixtures.DispatcherEntry;
import com.janeirodigital.sai.core.tests.fixtures.RequestMatchingFixtureDispatcher;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.janeirodigital.sai.core.tests.fixtures.MockWebServerHelper.toUrl;

class ReadableApplicationProfileTests {

    private static DataFactory dataFactory;
    private static MockWebServer server;

    @BeforeAll
    static void beforeAll() throws SaiException {
        // Initialize the Data Factory
        dataFactory = new DataFactory(HttpClientFactory.get(false, false));

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
        ReadableApplicationProfile applicationProfile = dataFactory.getReadableApplicationProfile(toUrl(server, "/projectron/id"));
        Assertions.assertNotNull(applicationProfile);
    }

    @Test
    @DisplayName("Get a Readable Application Profile Document as Fragment")
    void getReadableApplicationProfileAsFragment() throws SaiException, SaiNotFoundException {
        ReadableApplicationProfile applicationProfile = dataFactory.getReadableApplicationProfile(toUrl(server, "/projectron/idf#profile"));
        Assertions.assertNotNull(applicationProfile);
    }

    // Get a readable resource
    // Fail to get a readable resource that doesn't exist
    // Get a single node from a readable resource
    // Get many nodes from a readable resource
    // Get a graph from a readable resource (GraphReadOnly)

}

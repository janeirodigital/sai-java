package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import com.janeirodigital.sai.core.fixtures.DispatcherEntry;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ReadableResourceTests {

    private static DataFactory dataFactory;
    private static MockWebServer server;

    @BeforeAll
    static void beforeAll() throws SaiException {
        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        dataFactory = new DataFactory(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher(List.of(
                new DispatcherEntry(List.of("readable/readable-resource-ttl"), "GET", "/readable/readable-resource", null)
        ));

        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
    }

    @Test
    @DisplayName("Initialize a Readable resource")
    void initializeReadableResource() throws SaiException {
        URL url = toUrl(server, "/readable/readable-resource");
        ReadableResource readable = new ReadableResource(url, dataFactory);
        assertNotNull(readable);
        assertEquals(url, readable.getUrl());
        assertEquals(dataFactory, readable.getDataFactory());
        assertNull(readable.getDataset());
    }

    @Test
    @DisplayName("Bootstrap a Readable resource")
    void bootstrapReadableResource() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/readable/readable-resource#project");
        TestableReadableResource testable = TestableReadableResource.build(url, dataFactory, true);

        assertNotNull(testable);
        assertEquals(6, testable.getId());
        assertEquals("Great Validations", testable.getName());
        assertEquals(OffsetDateTime.parse("2021-04-04T20:15:47.000Z", DateTimeFormatter.ISO_DATE_TIME), testable.getCreatedAt());
        assertTrue(testable.isActive());
        assertEquals(toUrl(server, "/data/projects/project-1/milestone-3/#milestone"), testable.getMilestone());

        List<URL> tags = Arrays.asList(toUrl(server, "/tags/tag-1"), toUrl(server, "/tags/tag-2"), toUrl(server, "/tags/tag-3"));
        assertTrue(CollectionUtils.isEqualCollection(tags, testable.getTags()));

        List<String> comments = Arrays.asList("First original comment", "Second original comment", "Third original comment");
        assertTrue(CollectionUtils.isEqualCollection(comments, testable.getComments()));
    }

    @Test
    @DisplayName("Bootstrap a protected Readable resource")
    void bootstrapProtectedReadableResource() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/readable/readable-resource#project");
        TestableReadableResource testable = TestableReadableResource.build(url, dataFactory, false);
        // No need to test all of the accessors again
        assertNotNull(testable);
        assertEquals("Great Validations", testable.getName());
    }

}
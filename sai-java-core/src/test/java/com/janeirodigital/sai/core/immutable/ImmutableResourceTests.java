package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import com.janeirodigital.sai.core.fixtures.DispatcherEntry;
import com.janeirodigital.sai.core.fixtures.MockWebServerHelper;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.readable.TestableReadableResource;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.sai.core.helpers.HttpHelper.urlToUri;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getModelFromFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ImmutableResourceTests {

    private static DataFactory dataFactory;
    private static MockWebServer server;

    @BeforeAll
    static void beforeAll() throws SaiException {
        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        dataFactory = new DataFactory(mockSession, new HttpClientFactory(false, false, false));
        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher(List.of(
                new DispatcherEntry(List.of("immutable/immutable-resource-ttl"), "GET", "/immutable/immutable-resource", null),
                new DispatcherEntry(List.of("immutable/immutable-resource-ttl"), "PUT", "/immutable/immutable-resource", null)
        ));
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
    }

    @Test
    @DisplayName("Store an Immutable resource")
    void storeImmutableResource() throws SaiException, SaiNotFoundException {
        URL url = MockWebServerHelper.toUrl(server, "/immutable/immutable-resource#project");
        Model model = loadModel(url, "fixtures/immutable/immutable-resource.ttl", ContentType.TEXT_TURTLE);
        TestableImmutableResource testable = new TestableImmutableResource(url, dataFactory, model.getResource(url.toString()), true);
        TestableReadableResource readable = testable.store();
        assertNotNull(readable);
        assertEquals(6, readable.getId());
        assertEquals("Great Validations", readable.getName());
        assertEquals(OffsetDateTime.parse("2021-04-04T20:15:47.000Z", DateTimeFormatter.ISO_DATE_TIME), readable.getCreatedAt());
        assertTrue(readable.isActive());
        assertEquals(MockWebServerHelper.toUrl(server, "/data/projects/project-1/milestone-3/#milestone"), readable.getMilestone());

        List<URL> tags = Arrays.asList(MockWebServerHelper.toUrl(server, "/tags/tag-1"), MockWebServerHelper.toUrl(server, "/tags/tag-2"), MockWebServerHelper.toUrl(server, "/tags/tag-3"));
        assertTrue(CollectionUtils.isEqualCollection(tags, readable.getTags()));

        List<String> comments = Arrays.asList("First original comment", "Second original comment", "Third original comment");
        assertTrue(CollectionUtils.isEqualCollection(comments, readable.getComments()));
    }

    @Test
    @DisplayName("Store a protected Immutable resource")
    void storeProtectedImmutableResource() throws SaiNotFoundException, SaiException {
        URL url = MockWebServerHelper.toUrl(server, "/immutable/immutable-resource#project");
        Model model = loadModel(url, "fixtures/immutable/immutable-resource.ttl", ContentType.TEXT_TURTLE);
        TestableImmutableResource testable = new TestableImmutableResource(url, dataFactory, model.getResource(url.toString()), false);
        TestableReadableResource readable = testable.store();
        assertNotNull(readable);
        assertEquals("Great Validations", readable.getName());
    }

    private Model loadModel(URL url, String filePath, ContentType contentType) throws SaiException {
    try {
        return getModelFromFile(urlToUri(url), "fixtures/immutable/immutable-resource.ttl", contentType);
    } catch (SaiException | IOException ex) {
        throw new SaiException("Failed too load test model from file: " + filePath);
    }
}

}

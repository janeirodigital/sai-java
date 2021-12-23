package com.janeirodigital.sai.core.tests.immutable;

import com.janeirodigital.sai.core.DataFactory;
import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.tests.fixtures.DispatcherEntry;
import com.janeirodigital.sai.core.tests.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.tests.readable.TestableReadableResource;
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
import static com.janeirodigital.sai.core.tests.fixtures.MockWebServerHelper.toUrl;
import static org.junit.jupiter.api.Assertions.*;

class ImmutableResourceTests {

    private static DataFactory dataFactory;
    private static MockWebServer server;

    @BeforeAll
    static void beforeAll() throws SaiException {
        // Initialize the Data Factory
        dataFactory = new DataFactory(HttpClientFactory.get(false, false));
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
        URL url = toUrl(server, "/immutable/immutable-resource#project");
        Model model = loadModel(url, "fixtures/immutable/immutable-resource.ttl", ContentType.TEXT_TURTLE);
        TestableImmutableResource testable = new TestableImmutableResource(url, dataFactory, model.getResource(url.toString()));
        TestableReadableResource readable = testable.store();
        assertNotNull(readable);
        assertEquals(6, readable.getId());
        assertEquals("Great Validations", readable.getName());
        assertEquals(OffsetDateTime.parse("2021-04-04T20:15:47.000Z", DateTimeFormatter.ISO_DATE_TIME), readable.getCreatedAt());
        assertTrue(readable.isActive());
        assertEquals(toUrl(server, "/data/projects/project-1/milestone-3/#milestone"), readable.getMilestone());

        List<URL> tags = Arrays.asList(toUrl(server, "/tags/tag-1"), toUrl(server, "/tags/tag-2"), toUrl(server, "/tags/tag-3"));
        assertTrue(CollectionUtils.isEqualCollection(tags, readable.getTags()));

        List<String> comments = Arrays.asList("First original comment", "Second original comment", "Third original comment");
        assertTrue(CollectionUtils.isEqualCollection(comments, readable.getComments()));
    }

    private Model loadModel(URL url, String filePath, ContentType contentType) throws SaiException {
        try {
            return getModelFromFile(urlToUri(url), "fixtures/immutable/immutable-resource.ttl", contentType);
        } catch (SaiException | IOException ex) {
            throw new SaiException("Failed too load test model from file: " + filePath);
        }
    }

}

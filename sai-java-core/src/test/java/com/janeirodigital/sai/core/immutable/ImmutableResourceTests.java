package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.fixtures.MockWebServerHelper;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.*;
import static com.janeirodigital.sai.httputils.ContentType.TEXT_TURTLE;
import static com.janeirodigital.sai.httputils.HttpUtils.urlToUri;
import static com.janeirodigital.sai.rdfutils.RdfUtils.getModelFromFile;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ImmutableResourceTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;

    @BeforeAll
    static void beforeAll() throws SaiException {
        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));
        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
        // GET and PUT (create) immutable resource
        mockOnGet(dispatcher, "/immutable/immutable-resource", "immutable/immutable-resource-ttl");
        mockOnPut(dispatcher, "/immutable/immutable-resource", "http/201");
        mockOnDelete(dispatcher, "/immutable/immutable-resource", "http/204");
        // Fail to delete a resource
        mockOnGet(dispatcher, "/delete-fails/immutable/immutable-resource", "immutable/immutable-resource-ttl");
        mockOnDelete(dispatcher, "/delete-fails/immutable/immutable-resource", "http/500");
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
    }

    @Test
    @DisplayName("Read an Immutable resource")
    void readImmutableResource() throws SaiHttpNotFoundException, SaiException {
        URL url = MockWebServerHelper.toUrl(server, "/immutable/immutable-resource#project");
        TestableImmutableResource testable = TestableImmutableResource.get(url, saiSession, true);
        checkTestableResource(testable);
    }

    @Test
    @DisplayName("Reload an Immutable resource")
    void reloadImmutableResource() throws SaiHttpNotFoundException, SaiException {
        URL url = MockWebServerHelper.toUrl(server, "/immutable/immutable-resource#project");
        TestableImmutableResource testable = TestableImmutableResource.get(url, saiSession, true);
        TestableImmutableResource reloaded = testable.reload();
        checkTestableResource(reloaded);
    }

    @Test
    @DisplayName("Store an Immutable resource")
    void storeImmutableResource() throws SaiException {
        URL url = MockWebServerHelper.toUrl(server, "/immutable/immutable-resource#project");
        Model model = loadModel(url, "fixtures/immutable/immutable-resource.ttl", TEXT_TURTLE);
        TestableImmutableResource.Builder builder = new TestableImmutableResource.Builder(url, saiSession);
        TestableImmutableResource testable = builder.setDataset(model).setUnprotected().build();
        testable.create();
        checkTestableResource(testable);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Fail to create an immutable resource - endpoint missing")
    void failToCreateImmutableResource(boolean unprotected) throws SaiException {
        URL url = MockWebServerHelper.toUrl(server, "/missing/immutable/immutable-resource#project");
        Model model = loadModel(url, "fixtures/immutable/immutable-resource.ttl", TEXT_TURTLE);
        TestableImmutableResource.Builder builder = new TestableImmutableResource.Builder(url, saiSession);
        TestableImmutableResource testable = builder.setDataset(model).build();
        assertThrows(SaiException.class, () -> testable.create());
    }

    @Test
    @DisplayName("Store a protected Immutable resource")
    void storeProtectedImmutableResource() throws SaiException {
        URL url = MockWebServerHelper.toUrl(server, "/immutable/immutable-resource#project");
        Model model = loadModel(url, "fixtures/immutable/immutable-resource.ttl", TEXT_TURTLE);
        TestableImmutableResource.Builder builder = new TestableImmutableResource.Builder(url, saiSession);
        TestableImmutableResource testable = builder.setDataset(model).build();
        testable.create();
        assertEquals("Great Validations", testable.getName());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Delete an Immutable resource")
    void deleteImmutableResource(boolean unprotected) throws SaiException, SaiHttpNotFoundException {
        URL url = MockWebServerHelper.toUrl(server, "/immutable/immutable-resource#project");
        TestableImmutableResource testable = TestableImmutableResource.get(url, saiSession, unprotected);
        testable.delete();
        assertFalse(testable.isExists());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Fail to delete an immutable resource - endpoint missing")
    void failToDeleteImmutableResource(boolean unprotected) throws SaiException, SaiHttpNotFoundException {
        URL url = MockWebServerHelper.toUrl(server, "/delete-fails/immutable/immutable-resource#project");
        TestableImmutableResource testable = TestableImmutableResource.get(url, saiSession, unprotected);
        assertThrows(SaiException.class, () -> testable.delete());
    }

    private void checkTestableResource(TestableImmutableResource testable) {
        assertEquals(6, testable.getId());
        assertEquals("Great Validations", testable.getName());
        assertEquals(OffsetDateTime.parse("2021-04-04T20:15:47.000Z", DateTimeFormatter.ISO_DATE_TIME), testable.getCreatedAt());
        assertTrue(testable.isActive());
        assertEquals(MockWebServerHelper.toUrl(server, "/data/projects/project-1/milestone-3/#milestone"), testable.getMilestone());
        List<URL> tags = Arrays.asList(MockWebServerHelper.toUrl(server, "/tags/tag-1"), MockWebServerHelper.toUrl(server, "/tags/tag-2"), MockWebServerHelper.toUrl(server, "/tags/tag-3"));
        assertTrue(CollectionUtils.isEqualCollection(tags, testable.getTags()));
        List<String> comments = Arrays.asList("First original comment", "Second original comment", "Third original comment");
        assertTrue(CollectionUtils.isEqualCollection(comments, testable.getComments()));
    }

    private Model loadModel(URL url, String filePath, ContentType contentType) throws SaiException {
        try {
            return getModelFromFile(urlToUri(url), "fixtures/immutable/immutable-resource.ttl", contentType.getValue());
        } catch (IOException | SaiRdfException ex) {
            throw new SaiException("Failed too load test model from file: " + filePath, ex);
        }
    }

}

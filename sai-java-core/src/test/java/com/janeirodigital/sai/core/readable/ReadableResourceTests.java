package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.sai.core.enums.ContentType.TEXT_TURTLE;
import static com.janeirodigital.sai.core.enums.HttpHeader.CONTENT_TYPE;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnGet;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ReadableResourceTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static MockWebServer queuingServer;
    private static RequestMatchingFixtureDispatcher dispatcher;

    @BeforeAll
    static void beforeAll() throws SaiException, SaiNotFoundException {
        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
        mockOnGet(dispatcher, "/readable/readable-resource", "readable/readable-resource-ttl");
        mockOnGet(dispatcher, "/missing-fields/readable/readable-resource", "readable/readable-resource-missing-fields-ttl");
        mockOnGet(dispatcher, "/malformed/readable/readable-resource", "readable/readable-resource-malformed-ttl");

        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
    }

    @BeforeEach
    void beforeEach() throws IOException {
        queuingServer = new MockWebServer();
        queuingServer.start();
    }

    @AfterEach
    void afterEach() throws IOException {
        queuingServer.shutdown();
    }

    @Test
    @DisplayName("Get a readable resource")
    void getReadableResource() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/readable/readable-resource#project");
        TestableReadableResource testable = TestableReadableResource.get(url, saiSession, true);

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
    @DisplayName("Get a protected readable resource")
    void bootstrapProtectedReadableResource() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/readable/readable-resource#project");
        TestableReadableResource testable = TestableReadableResource.get(url, saiSession, false);
        // No need to test all of the accessors again
        assertNotNull(testable);
        assertEquals("Great Validations", testable.getName());
    }

    @Test
    @DisplayName("Fail to get a protected readable resource - missing fields")
    void failToGetReadableResourceMissingFields() {
        URL url = toUrl(server, "/missing-fields/readable/readable-resource#project");
        assertThrows(SaiException.class, () -> TestableReadableResource.get(url, saiSession, false));
    }

    @Test
    @DisplayName("Fail to get a protected readable resource - io failure")
    void failToGetReadableResourceMalformedDocuments() throws SaiException {
        URL url = toUrl(queuingServer, "/io/readable/protected-readable");
        queuingServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(CONTENT_TYPE.getValue(), TEXT_TURTLE.getValue())
                .setBody("body")
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY));
        assertThrows(SaiException.class, () -> TestableReadableResource.get(url, saiSession, false));
    }

    @Test
    @DisplayName("Fail to get unprotected readable resource - io failure")
    void failToGetReadableResourceIO() throws SaiException {
        URL url = toUrl(queuingServer, "/io/readable/unprotected-readable");
        queuingServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(CONTENT_TYPE.getValue(), TEXT_TURTLE.getValue())
                .setBody("body")
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY));
        assertThrows(SaiException.class, () -> TestableReadableResource.get(url, saiSession, true));
    }

    @Test
    @DisplayName("Test readable response check - unsuccessful - resource not found")
    void testReadableResponseCheckNotFound() {
        URL missingUrl = toUrl(server, "/missing/readable/resource");
        assertThrows(SaiNotFoundException.class, () -> TestableReadableResource.get(missingUrl, saiSession, false));
    }

    @Test
    @DisplayName("Test readable response check - unsuccessful - server error")
    void testReadableResponseCheckError() throws SaiException {
        URL errorUrl = toUrl(queuingServer, "/io/readable/server-error-resource");
        queuingServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader(CONTENT_TYPE.getValue(), TEXT_TURTLE.getValue())
                .setBody("BAD"));
        assertThrows(SaiException.class, () -> TestableReadableResource.get(errorUrl, saiSession, true));
    }
}


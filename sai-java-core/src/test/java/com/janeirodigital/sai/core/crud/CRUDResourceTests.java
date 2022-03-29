package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
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

import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.*;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CRUDResourceTests {

    private static SaiSession saiSession;
    private static MockWebServer server;

    @BeforeAll
    static void beforeAll() throws SaiException, SaiHttpNotFoundException {

        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        // Get, Update, Delete for an existing CRUD resource
        mockOnGet(dispatcher, "/crud/crud-resource", "crud/crud-resource-ttl");
        mockOnPut(dispatcher, "/crud/crud-resource", "crud/crud-resource-ttl");
        mockOnDelete(dispatcher, "/crud/crud-resource", "crud/crud-resource-ttl");
        // Build a CRUD resource that doesn't exist and create it
        mockOnGet(dispatcher, "/new/crud/crud-resource", "http/404");
        mockOnPut(dispatcher, "/new/crud/crud-resource", "http/201");
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
    }

    @Test
    @DisplayName("Get a CRUD resource")
    void buildExistingCRUDResource() throws SaiException, SaiHttpNotFoundException {
        URL url = toUrl(server, "/crud/crud-resource#project");
        TestableCRUDResource testable = TestableCRUDResource.get(url, saiSession,true);
        checkTestableGraph(testable);
    }

    @Test
    @DisplayName("Create a CRUD resource")
    void createCRUDResource() throws SaiException {
        URL url = toUrl(server, "/new/crud/crud-resource");
        List<URL> tags = Arrays.asList(toUrl(server, "/tags/tag-111"), toUrl(server, "/tags/tag-222"), toUrl(server, "/tags/tag-333"));
        List<String> comments = Arrays.asList("First updated comment", "Second updated comment", "Third updated comment");
        TestableCRUDResource.Builder builder = new TestableCRUDResource.Builder(url, saiSession);
        TestableCRUDResource testable = builder.setId(42).setName("Interoperability").setCreatedAt(OffsetDateTime.MAX.now()).setActive(false)
               .setMilestone(toUrl(server, "/crud/project/milestone-1#milestone")).setTags(tags).setUnprotected()
               .setComments(comments).build();
        assertDoesNotThrow(() -> testable.update());
    }

    @Test
    @DisplayName("Update a CRUD resource")
    void updateCRUDResource() throws SaiException, SaiHttpNotFoundException {
        URL url = toUrl(server, "/crud/crud-resource#project");
        TestableCRUDResource testable = TestableCRUDResource.get(url, saiSession,true);
        testable.setActive(false);
        assertDoesNotThrow(() -> testable.update());
    }

    @Test
    @DisplayName("Fail to update a CRUD resource - missing")
    void failToUpdateCRUDResource() throws SaiException {
        URL url = toUrl(server, "/crud/missing");
        List<URL> tags = Arrays.asList(toUrl(server, "/tags/tag-111"), toUrl(server, "/tags/tag-222"), toUrl(server, "/tags/tag-333"));
        List<String> comments = Arrays.asList("First updated comment", "Second updated comment", "Third updated comment");
        TestableCRUDResource.Builder builder = new TestableCRUDResource.Builder(url, saiSession);
        TestableCRUDResource testable = builder.setId(42).setName("Interoperability").setCreatedAt(OffsetDateTime.MAX.now()).setActive(false)
                .setMilestone(toUrl(server, "/crud/project/milestone-1#milestone")).setTags(tags)
                .setComments(comments).build();
        assertThrows(SaiException.class, () -> testable.update());
    }

    @Test
    @DisplayName("Delete a CRUD resource")
    void deleteCRUDResource() throws SaiException, SaiHttpNotFoundException {
        URL url = toUrl(server, "/crud/crud-resource#project");
        TestableCRUDResource testable = TestableCRUDResource.get(url, saiSession, true);
        assertDoesNotThrow(() -> testable.delete());
    }

    @Test
    @DisplayName("Delete a protected CRUD resource")
    void deleteProtectedCRUDResource() throws SaiException, SaiHttpNotFoundException {
        URL url = toUrl(server, "/crud/crud-resource#project");
        TestableCRUDResource testable = TestableCRUDResource.get(url, saiSession, false);
        assertDoesNotThrow(() -> testable.delete());
    }

    private void checkTestableGraph(TestableCRUDResource testable) {
        assertNotNull(testable);
        assertNotNull(testable.getDataset());
        assertNotNull(testable.getResource());

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

}

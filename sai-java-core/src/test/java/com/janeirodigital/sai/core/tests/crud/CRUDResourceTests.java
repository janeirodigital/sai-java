package com.janeirodigital.sai.core.tests.crud;

import com.janeirodigital.sai.core.DataFactory;
import com.janeirodigital.sai.core.crud.CRUDResource;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.tests.fixtures.DispatcherEntry;
import com.janeirodigital.sai.core.tests.fixtures.RequestMatchingFixtureDispatcher;
import okhttp3.Response;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.sai.core.tests.fixtures.MockWebServerHelper.toUrl;
import static org.junit.jupiter.api.Assertions.*;

class CRUDResourceTests {

    private static DataFactory dataFactory;
    private static MockWebServer server;

    @BeforeAll
    static void beforeAll() throws SaiException {
        // Initialize the Data Factory
        dataFactory = new DataFactory(HttpClientFactory.get(false, false));

        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher(List.of(
                new DispatcherEntry(List.of("crud/crud-resource-ttl"), "GET", "/crud/crud-resource", null),
                new DispatcherEntry(List.of("crud/crud-resource-ttl"), "PUT", "/crud/crud-resource", null),
                new DispatcherEntry(List.of("crud/crud-resource-ttl"), "DELETE", "/crud/crud-resource", null)
        ));

        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
    }

    @Test
    @DisplayName("Initialize a CRUD resource")
    void initializeCRUDResource() throws SaiException {
        URL url = toUrl(server, "/crud/crud-resource");
        CRUDResource crud = new CRUDResource(url, dataFactory);
        assertNotNull(crud);
        assertEquals(url, crud.getUrl());
        assertEquals(dataFactory, crud.getDataFactory());
        assertNull(crud.getDataset());
    }

    @Test
    @DisplayName("Bootstrap a CRUD resource")
    void bootstrapCRUDResource() throws SaiException {
        URL url = toUrl(server, "/crud/crud-resource");
        TestableCRUDResource testable = new TestableCRUDResource(url, dataFactory);
        assertNotNull(testable);
        assertEquals(url, testable.getUrl());
        assertEquals(dataFactory, testable.getDataFactory());
        assertNull(testable.getDataset());
    }

    @Test
    @DisplayName("Update a CRUD resource")
    void updateCRUDResource() throws SaiException, SaiNotFoundException {

        URL url = toUrl(server, "/crud/crud-resource#project");
        List<URL> tags = Arrays.asList(toUrl(server, "/tags/tag-111"), toUrl(server, "/tags/tag-222"), toUrl(server, "/tags/tag-333"));
        List<String> comments = Arrays.asList("First updated comment", "Second updated comment", "Third updated comment");

        TestableCRUDResource testable = TestableCRUDResource.build(url, dataFactory);
        testable.setId(42);
        testable.setName("Interoperability");
        testable.setCreatedAt(OffsetDateTime.MAX.now());
        testable.setActive(false);
        testable.setMilestone(toUrl(server, "/crud/project/milestone-1#milestone"));
        testable.setTags(tags);
        testable.setComments(comments);
        Response response = testable.update();
        assertTrue(response.isSuccessful());

    }

    @Test
    @DisplayName("Delete a CRUD resource")
    void deleteCRUDResource() throws SaiNotFoundException, SaiException {
        URL url = toUrl(server, "/crud/crud-resource#project");
        TestableCRUDResource testable = TestableCRUDResource.build(url, dataFactory);
        Response response = testable.delete();
        assertTrue(response.isSuccessful());
    }

}
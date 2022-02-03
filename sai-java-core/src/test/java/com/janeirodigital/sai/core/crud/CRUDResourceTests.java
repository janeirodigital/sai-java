package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.factories.DataFactory;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.*;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static com.janeirodigital.sai.core.helpers.HttpHelper.urlToUri;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CRUDResourceTests {

    private static DataFactory dataFactory;
    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;

    @BeforeAll
    static void beforeAll() throws SaiException {

        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        dataFactory = new DataFactory(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
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
    @DisplayName("Initialize a Testable CRUD resource")
    void bootstrapCRUDResource() throws SaiException {
        URL url = toUrl(server, "/crud/crud-resource");
        TestableCRUDResource testable = new TestableCRUDResource(url, dataFactory, true);
        assertNotNull(testable);
        assertEquals(url, testable.getUrl());
        assertEquals(dataFactory, testable.getDataFactory());
        assertNull(testable.getDataset());
    }

    @Test
    @DisplayName("Initialize a protected CRUD resource")
    void initializeProtectedCRUDResource() throws SaiException {
        URL url = toUrl(server, "/crud/crud-resource");
        TestableCRUDResource testable = new TestableCRUDResource(url, dataFactory, false);
        assertNotNull(testable);
        assertEquals(url, testable.getUrl());
        assertEquals(dataFactory, testable.getDataFactory());
        assertNull(testable.getDataset());
    }

    @Test
    @DisplayName("Build a new CRUD resource that doesn't exist")
    void buildNewCrudResource() throws SaiException {
        URL url = toUrl(server, "/new/crud/crud-resource");
        TestableCRUDResource crudResource = TestableCRUDResource.build(url, dataFactory, true);
        assertNotNull(crudResource);
        assertNotNull(crudResource.getResource());
        assertNotNull(crudResource.getDataset());
        assertNotNull(getObject(crudResource.getResource(), RDF.type));
    }

    @Test
    @DisplayName("Build a new CRUD resource that doesn't exist with jena model")
    void buildCRUDResourceWithDataset() throws SaiException {
        URL url = toUrl(server, "/new/crud/crud-resource#project");
        Model model = loadModel(url, "fixtures/crud/crud-resource.ttl", ContentType.TEXT_TURTLE);
        Resource resource = getResourceFromModel(model, url);
        TestableCRUDResource testable = TestableCRUDResource.build(url, dataFactory, resource, true);
        checkTestableGraph(testable);
    }

    @Test
    @DisplayName("Build an existing CRUD resource")
    void buildExistingCRUDResource() throws SaiException {
        URL url = toUrl(server, "/crud/crud-resource#project");
        TestableCRUDResource testable = TestableCRUDResource.build(url, dataFactory,true);
        checkTestableGraph(testable);
    }

    @Test
    @DisplayName("Build an existing CRUD resource and override with jena model")
    void buildExistingCRUDResourceAndOverride() throws SaiException {
        URL url = toUrl(server, "/crud/crud-resource#project");
        Model model = loadModel(url, "fixtures/crud/crud-resource.ttl", ContentType.TEXT_TURTLE);
        Resource resource = getResourceFromModel(model, url);
        TestableCRUDResource testable = TestableCRUDResource.build(url, dataFactory, resource,true);
        checkTestableGraph(testable);
    }

    @Test
    @DisplayName("Update a CRUD resource")
    void updateCRUDResource() throws SaiException {

        URL url = toUrl(server, "/crud/crud-resource#project");
        List<URL> tags = Arrays.asList(toUrl(server, "/tags/tag-111"), toUrl(server, "/tags/tag-222"), toUrl(server, "/tags/tag-333"));
        List<String> comments = Arrays.asList("First updated comment", "Second updated comment", "Third updated comment");

        TestableCRUDResource testable = TestableCRUDResource.build(url, dataFactory, true);
        testable.setId(42);
        testable.setName("Interoperability");
        testable.setCreatedAt(OffsetDateTime.MAX.now());
        testable.setActive(false);
        testable.setMilestone(toUrl(server, "/crud/project/milestone-1#milestone"));
        testable.setTags(tags);
        testable.setComments(comments);
        assertDoesNotThrow(() -> testable.update());
    }

    @Test
    @DisplayName("Update a protected CRUD resource")
    void updateProtectedCRUDResource() throws SaiException {

        URL url = toUrl(server, "/crud/crud-resource#project");
        List<URL> tags = Arrays.asList(toUrl(server, "/tags/tag-111"), toUrl(server, "/tags/tag-222"), toUrl(server, "/tags/tag-333"));
        List<String> comments = Arrays.asList("First updated comment", "Second updated comment", "Third updated comment");

        TestableCRUDResource testable = TestableCRUDResource.build(url, dataFactory, false);
        testable.setId(42);
        testable.setName("Interoperability");
        testable.setCreatedAt(OffsetDateTime.MAX.now());
        testable.setActive(false);
        testable.setMilestone(toUrl(server, "/crud/project/milestone-1#milestone"));
        testable.setTags(tags);
        testable.setComments(comments);
        assertDoesNotThrow(() -> testable.update());
    }

    @Test
    @DisplayName("Delete a CRUD resource")
    void deleteCRUDResource() throws SaiException {
        URL url = toUrl(server, "/crud/crud-resource#project");
        TestableCRUDResource testable = TestableCRUDResource.build(url, dataFactory, true);
        assertDoesNotThrow(() -> testable.delete());
    }

    @Test
    @DisplayName("Delete a protected CRUD resource")
    void deleteProtectedCRUDResource() throws SaiException {
        URL url = toUrl(server, "/crud/crud-resource#project");
        TestableCRUDResource testable = TestableCRUDResource.build(url, dataFactory, false);
        assertDoesNotThrow(() -> testable.delete());
    }

    private Model loadModel(URL url, String filePath, ContentType contentType) throws SaiException {
        try {
            return getModelFromFile(urlToUri(url), filePath, contentType);
        } catch (SaiException | IOException ex) {
            throw new SaiException("Failed too load test model from file: " + filePath);
        }
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

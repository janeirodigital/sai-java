package com.janeirodigital.sai.core.helpers;

import com.janeirodigital.sai.core.TestableVocabulary;
import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.sai.core.helpers.HttpHelper.urlToUri;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RdfHelperTests {

    private static URL resourceUrl;
    private static URI resourceUri;
    private static String resourcePath;
    private static String invalidResourcePath;
    private static Model readableModel;
    private static Resource readableResource;
    private static Model updatableModel;
    private static Resource updatableResource;

    private static List<URL> READABLE_TAGS;
    private static List<String> READABLE_COMMENTS;
    private static URL READABLE_MILESTONE;
    private static boolean READABLE_ACTIVE;
    private static OffsetDateTime READABLE_CREATED_AT;
    private static String READABLE_NAME;
    private static int READABLE_ID;

    @BeforeAll
    static void beforeAll() throws MalformedURLException, SaiException {
        resourceUrl = new URL("https://data.example/resource#project");
        resourceUri = urlToUri(resourceUrl);
        resourcePath = "fixtures/rdf/rdf-resource.ttl";
        invalidResourcePath = "fixtures/rdf/invalid-rdf-resource.ttl";
        readableModel = getModelFromString(resourceUri, getRdfResourceBody(), ContentType.TEXT_TURTLE);
        readableResource = getResourceFromModel(readableModel, resourceUrl);

        READABLE_TAGS = Arrays.asList(new URL("https://data.example/tags/tag-1"),
                                      new URL("https://data.example/tags/tag-2"),
                                      new URL("https://data.example/tags/tag-3"));
        READABLE_COMMENTS = Arrays.asList("First original comment" ,
                                          "Second original comment" ,
                                          "Third original comment");
        READABLE_MILESTONE = new URL("https://data.example/data/projects/project-1/milestone-3/#milestone");
        READABLE_ACTIVE = true;
        READABLE_CREATED_AT = OffsetDateTime.parse("2021-04-04T20:15:47.000Z", DateTimeFormatter.ISO_DATE_TIME);
        READABLE_NAME = "Great Validations";
        READABLE_ID = 6;
    }

    @BeforeEach
    void beforeEach() throws SaiException {
        updatableModel = getModelFromString(resourceUri, getRdfResourceBody(), ContentType.TEXT_TURTLE);
        updatableResource = getResourceFromModel(updatableModel, resourceUrl);
    }

    @Test
    @DisplayName("Get RDF model from string")
    void checkGetModelFromString() throws SaiException {
        Model model = getModelFromString(resourceUri, getRdfResourceBody(), ContentType.TEXT_TURTLE);
        assertNotNull(model);
        assertNotNull(model.getResource(resourceUrl.toString()));
    }

    @Test
    @DisplayName("Fail to get RDF model from invalid string")
    void failToGetModelFromInvalidString() {
        assertThrows(SaiException.class, () -> {
            getModelFromString(resourceUri, getInvalidRdfResourceBody(), ContentType.TEXT_TURTLE);
        });
    }

    @Test
    @DisplayName("Get RDF model from file")
    void checkGetModelFromFile() throws SaiException, IOException {
        Model model = getModelFromFile(resourceUri, resourcePath, ContentType.TEXT_TURTLE);
        assertNotNull(model);
        assertNotNull(model.getResource(resourceUrl.toString()));
    }

    @Test
    @DisplayName("Fail to get RDF model from invalid file")
    void failToGetModelFromInvalidFile() {
        assertThrows(SaiException.class, () -> {
            getModelFromFile(resourceUri, invalidResourcePath, ContentType.TEXT_TURTLE);
        });
    }

    @Test
    @DisplayName("Fail to get RDF model from invalid file with null stream")
    void failToGetModelFromInvalidFileNull() {
        try (MockedStatic<RDFDataMgr> mockMgr = Mockito.mockStatic(RDFDataMgr.class)) {
            mockMgr.when(() -> RDFDataMgr.open(anyString())).thenReturn(null);
            Model mockModel = mock(Model.class);
            when(mockModel.read(any(InputStream.class), anyString(), anyString())).thenThrow(RiotException.class);
            assertDoesNotThrow(() -> getModelFromFile(resourceUri, invalidResourcePath, ContentType.TEXT_TURTLE));
        }
    }

    @Test
    @DisplayName("Get resource from RDF model")
    void checkGetResourceFromModel() {
        Resource resource = getResourceFromModel(readableModel, resourceUrl);
        assertNotNull(resource);
        assertEquals(resourceUrl.toString(), resource.getURI());
    }

    @Test
    @DisplayName("Get statement from resource by property")
    void checkGetStatement() {
        Statement statement = getStatement(readableResource, TestableVocabulary.TESTABLE_NAME);
        assertNotNull(statement);
        assertEquals(READABLE_NAME, statement.getObject().asLiteral().getString());

        Statement missing = getStatement(readableResource, TestableVocabulary.TESTABLE_MISSING);
        assertNull(missing);
    }

    @Test
    @DisplayName("Get required statement from resource by property")
    void checkGetRequiredStatement() throws SaiNotFoundException {
        Statement statement = getRequiredStatement(readableResource, TestableVocabulary.TESTABLE_NAME);
        assertNotNull(statement);
        assertEquals(READABLE_NAME, statement.getObject().asLiteral().getString());

        assertThrows(SaiNotFoundException.class, () -> {
            getRequiredStatement(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });
    }

    @Test
    @DisplayName("Serialize RDF model to string")
    void checkGetStringFromRdfModel() throws SaiException {
        String serialized = getStringFromRdfModel(readableModel, getLangForContentType(ContentType.TEXT_TURTLE));
        Model comparableModel = getModelFromString(resourceUri, serialized, ContentType.TEXT_TURTLE);
        Model difference = comparableModel.difference(readableModel);
        assertTrue(difference.isEmpty());
    }

    @Test
    @DisplayName("Get an object from resource by property")
    void checkGetObject() {
        RDFNode object = getObject(readableResource, TestableVocabulary.TESTABLE_ID);
        assertNotNull(object);
        assertEquals(6, object.asLiteral().getInt());

        RDFNode missing = getObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        assertNull(missing);
    }

    @Test
    @DisplayName("Get a required object from resource by property")
    void checkGetRequiredObject() throws SaiNotFoundException {
        RDFNode object = getRequiredObject(readableResource, TestableVocabulary.TESTABLE_ID);
        assertNotNull(object);
        assertEquals(6, object.asLiteral().getInt());

        assertThrows(SaiNotFoundException.class, () -> {
            getRequiredObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });

    }

    @Test
    @DisplayName("Get list of objects from resource by property")
    void checkGetObjects() {
        List<RDFNode> objects = getObjects(readableResource, TestableVocabulary.TESTABLE_HAS_COMMENT);
        assertNotNull(objects);
        assertEquals(3, objects.size());

        List<RDFNode> missing = getObjects(readableResource, TestableVocabulary.TESTABLE_MISSING);
        assertTrue(missing.isEmpty());
    }

    @Test
    @DisplayName("Get list of required objects from resource by property")
    void checkGetRequiredObjects() throws SaiNotFoundException {
        List<RDFNode> objects = getRequiredObjects(readableResource, TestableVocabulary.TESTABLE_HAS_COMMENT);
        assertNotNull(objects);
        assertEquals(3, objects.size());

        assertThrows(SaiNotFoundException.class, () -> {
            getRequiredObjects(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });
    }

    @Test
    @DisplayName("Get list of URL objects from resource by property")
    void checkGetUrlObjects() throws SaiException {
        List<URL> objects = getUrlObjects(readableResource, TestableVocabulary.TESTABLE_HAS_TAG);
        assertNotNull(objects);
        assertEquals(3, objects.size());
        assertTrue(objects.containsAll(READABLE_TAGS));

        assertThrows(SaiException.class, () -> {
            getRequiredUrlObjects(readableResource, TestableVocabulary.TESTABLE_HAS_COMMENT);
        });

        List<URL> missing = getUrlObjects(readableResource, TestableVocabulary.TESTABLE_MISSING);
        assertTrue(missing.isEmpty());
    }

    @Test
    @DisplayName("Get list of required URL objects from resource by property")
    void checkGetRequiredUrlObjects() throws SaiNotFoundException, SaiException {
        List<URL> objects = getRequiredUrlObjects(readableResource, TestableVocabulary.TESTABLE_HAS_TAG);
        assertNotNull(objects);
        assertEquals(3, objects.size());
        assertTrue(objects.containsAll(READABLE_TAGS));

        assertThrows(SaiNotFoundException.class, () -> {
            getRequiredUrlObjects(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });
    }

    @Test
    @DisplayName("Get list of String objects from resource by property")
    void checkGetStringObjects() throws SaiException {
        List<String> objects = getStringObjects(readableResource, TestableVocabulary.TESTABLE_HAS_COMMENT);
        assertNotNull(objects);
        assertEquals(3, objects.size());
        assertTrue(objects.containsAll(READABLE_COMMENTS));

        assertThrows(SaiException.class, () -> {
            getRequiredStringObjects(readableResource, TestableVocabulary.TESTABLE_HAS_TAG);
        });

        assertThrows(SaiException.class, () -> {
            getRequiredStringObjects(readableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        });

        assertThrows(SaiException.class, () -> {
            getRequiredStringObjects(readableResource, TestableVocabulary.TESTABLE_CREATED_AT);
        });

        List<String> missing = getStringObjects(readableResource, TestableVocabulary.TESTABLE_MISSING);
        assertTrue(missing.isEmpty());
    }

    @Test
    @DisplayName("Get list of required String objects from resource by property")
    void checkGetRequiredStringObjects() throws SaiNotFoundException, SaiException {
        List<String> objects = getRequiredStringObjects(readableResource, TestableVocabulary.TESTABLE_HAS_COMMENT);
        assertNotNull(objects);
        assertEquals(3, objects.size());
        assertTrue(objects.containsAll(READABLE_COMMENTS));

        assertThrows(SaiNotFoundException.class, () -> {
            getRequiredStringObjects(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });
    }
    
    @Test
    @DisplayName("Get URL object from resource by property")
    void checkGetUrlObject() throws SaiException {
        URL object = getUrlObject(readableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        assertNotNull(object);
        assertEquals(READABLE_MILESTONE, object);

        assertThrows(SaiException.class, () -> {
            getUrlObject(readableResource, TestableVocabulary.TESTABLE_CREATED_AT);
        });

        URL missing = getUrlObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        assertNull(missing);
    }

    @Test
    @DisplayName("Get required URL object from resource by property")
    void checkGetRequiredUrlObject() throws SaiNotFoundException, SaiException {
        URL object = getRequiredUrlObject(readableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        assertNotNull(object);
        assertEquals(READABLE_MILESTONE, object);

        assertThrows(SaiNotFoundException.class, () -> {
            getRequiredUrlObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });
    }

    @Test
    @DisplayName("Get String object from resource by property")
    void checkGetStringObject() throws SaiException {
        String object = getStringObject(readableResource, TestableVocabulary.TESTABLE_NAME);
        assertNotNull(object);
        assertEquals(READABLE_NAME, object);

        assertThrows(SaiException.class, () -> {
            getStringObject(readableResource, TestableVocabulary.TESTABLE_CREATED_AT);
        });

        assertThrows(SaiException.class, () -> {
            getStringObject(readableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        });

        String missing = getStringObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        assertNull(missing);
    }

    @Test
    @DisplayName("Get required String object from resource by property")
    void checkGetRequiredStringObject() throws SaiNotFoundException, SaiException {
        String object = getRequiredStringObject(readableResource, TestableVocabulary.TESTABLE_NAME);
        assertNotNull(object);
        assertEquals(READABLE_NAME, object);

        assertThrows(SaiNotFoundException.class, () -> {
            getRequiredStringObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });
    }

    @Test
    @DisplayName("Get Integer object from resource by property")
    void checkGetIntegerObject() throws SaiException {
        Integer object = getIntegerObject(readableResource, TestableVocabulary.TESTABLE_ID);
        assertNotNull(object);
        assertEquals(READABLE_ID, object);

        assertThrows(SaiException.class, () -> {
            getIntegerObject(readableResource, TestableVocabulary.TESTABLE_CREATED_AT);
        });

        assertThrows(SaiException.class, () -> {
            getIntegerObject(readableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        });

        Integer missing = getIntegerObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        assertNull(missing);
    }

    @Test
    @DisplayName("Get required Integer object from resource by property")
    void checkGetRequiredIntegerObject() throws SaiNotFoundException, SaiException {
        Integer object = getRequiredIntegerObject(readableResource, TestableVocabulary.TESTABLE_ID);
        assertNotNull(object);
        assertEquals(READABLE_ID, object);

        assertThrows(SaiNotFoundException.class, () -> {
            getRequiredIntegerObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });
    }

    @Test
    @DisplayName("Get DateTime object from resource by property")
    void checkGetDateTimeObject() throws SaiException {
        OffsetDateTime object = getDateTimeObject(readableResource, TestableVocabulary.TESTABLE_CREATED_AT);
        assertNotNull(object);
        assertEquals(READABLE_CREATED_AT, object);

        assertThrows(SaiException.class, () -> {
            getDateTimeObject(readableResource, TestableVocabulary.TESTABLE_ID);
        });

        assertThrows(SaiException.class, () -> {
            getDateTimeObject(readableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        });

        OffsetDateTime missing = getDateTimeObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        assertNull(missing);
    }

    @Test
    @DisplayName("Get required DateTime object from resource by property")
    void checkGetRequiredDateTimeObject() throws SaiNotFoundException, SaiException {
        OffsetDateTime object = getRequiredDateTimeObject(readableResource, TestableVocabulary.TESTABLE_CREATED_AT);
        assertNotNull(object);
        assertEquals(READABLE_CREATED_AT, object);

        assertThrows(SaiNotFoundException.class, () -> {
            getRequiredDateTimeObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });
    }

    @Test
    @DisplayName("Get Boolean object from resource by property")
    void checkGetBooleanObject() throws SaiNotFoundException, SaiException {
        boolean object = getBooleanObject(readableResource, TestableVocabulary.TESTABLE_ACTIVE);
        assertEquals(READABLE_ACTIVE, object);

        assertThrows(SaiException.class, () -> {
            getBooleanObject(readableResource, TestableVocabulary.TESTABLE_CREATED_AT);
        });

        assertThrows(SaiException.class, () -> {
            getBooleanObject(readableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        });

        assertThrows(SaiNotFoundException.class, () -> {
            getBooleanObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });
    }

    @Test
    @DisplayName("Get required Boolean object from resource by property")
    void checkGetRequiredBooleanObject() throws SaiNotFoundException, SaiException {
        boolean object = getRequiredBooleanObject(readableResource, TestableVocabulary.TESTABLE_ACTIVE);
        assertEquals(READABLE_ACTIVE, object);

        assertThrows(SaiException.class, () -> {
            getRequiredBooleanObject(readableResource, TestableVocabulary.TESTABLE_CREATED_AT);
        });

        assertThrows(SaiException.class, () -> {
            getRequiredBooleanObject(readableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        });

        assertThrows(SaiNotFoundException.class, () -> {
            getRequiredBooleanObject(readableResource, TestableVocabulary.TESTABLE_MISSING);
        });
    }
    
    @Test
    @DisplayName("Update RDF node object by property")
    void checkUpdateNodeObject() throws MalformedURLException, SaiException {
        URL url = new URL("https://solidproject.org");
        Node node = NodeFactory.createURI(url.toString());
        RdfHelper.updateObject(updatableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE, updatableModel.asRDFNode(node));
        assertEquals(url, getUrlObject(updatableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE));
    }

    @Test
    @DisplayName("Update string object by property")
    void checkUpdateStringObject() throws SaiException {
        String name = "Updated name";
        RdfHelper.updateObject(updatableResource, TestableVocabulary.TESTABLE_NAME, name);
        assertEquals(name, getStringObject(updatableResource, TestableVocabulary.TESTABLE_NAME));
    }

    @Test
    @DisplayName("Update URL object by property")
    void checkUpdateUrlObject() throws SaiException, MalformedURLException {
        URL milestone = new URL("https://solidproject.org/roadmap#milestone");
        RdfHelper.updateObject(updatableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE, milestone);
        assertEquals(milestone, getUrlObject(updatableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE));
    }

    @Test
    @DisplayName("Update date time object by property")
    void checkUpdateDateTimeObject() throws SaiException {
        OffsetDateTime dateTime = OffsetDateTime.parse("2021-12-25T06:00:00.000Z", DateTimeFormatter.ISO_DATE_TIME);
        RdfHelper.updateObject(updatableResource, TestableVocabulary.TESTABLE_CREATED_AT, dateTime);
        assertEquals(dateTime, getDateTimeObject(updatableResource, TestableVocabulary.TESTABLE_CREATED_AT));
    }

    @Test
    @DisplayName("Update Integer object by property")
    void checkUpdateIntegerObject() throws SaiException {
        int id = 777;
        RdfHelper.updateObject(updatableResource, TestableVocabulary.TESTABLE_ID, id);
        assertEquals(id, getIntegerObject(updatableResource, TestableVocabulary.TESTABLE_ID));
    }

    @Test
    @DisplayName("Update boolean object by property")
    void checkUpdateBooleanObject() throws SaiNotFoundException, SaiException {
        RdfHelper.updateObject(updatableResource, TestableVocabulary.TESTABLE_ACTIVE, false);
        assertFalse(getBooleanObject(updatableResource, TestableVocabulary.TESTABLE_ACTIVE));
    }

    @Test
    @DisplayName("Update list of URL objects by property")
    void checkUpdateUrlObjects() throws MalformedURLException, SaiException {

        List<URL> tags = Arrays.asList(new URL("https://data.example/tags/tag-11111"),
                                       new URL("https://data.example/tags/tag-22222"),
                                       new URL("https://data.example/tags/tag-33333"));
        updateUrlObjects(updatableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE, tags);
        assertTrue(CollectionUtils.isEqualCollection(tags, getUrlObjects(updatableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE)));
    }

    @Test
    @DisplayName("Update list of string objects by property")
    void checkUpdateStringObjects() throws SaiException {
        List<String> comments = Arrays.asList("First UPDATED comment" ,
                                              "Second UPDATED comment" ,
                                              "Third UPDATED comment");
        updateStringObjects(updatableResource, TestableVocabulary.TESTABLE_HAS_COMMENT, comments);
        assertTrue(CollectionUtils.isEqualCollection(comments, getStringObjects(updatableResource, TestableVocabulary.TESTABLE_HAS_COMMENT)));
    }

    @Test
    @DisplayName("Get URL value from RDF node")
    void checkNodeToUrl() throws SaiException {
        RDFNode object = getObject(readableResource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        assertEquals(READABLE_MILESTONE, nodeToUrl(object));
        // Fail when URL is not a resource
        RDFNode notResource = getObject(readableResource, TestableVocabulary.TESTABLE_NAME);
        assertThrows(SaiException.class, () -> { nodeToUrl(notResource); });
    }

    @Test
    @DisplayName("Fail to get malformed URL value from RDF node")
    void failNodeToUrlMalformed() throws MalformedURLException {
        // Fail when URL is not a resource
        RDFNode mockNode = mock(RDFNode.class);
        Resource mockResource = mock(Resource.class);
        when(mockNode.isResource()).thenReturn(true);
        when(mockNode.asResource()).thenReturn(mockResource);
        when(mockResource.getURI()).thenReturn("cool:web:times");
        assertThrows(SaiException.class, () -> { nodeToUrl(mockNode); });
    }

    @Test
    @DisplayName("Get Jena Lang for content-type")
    void checkLangForContentType() {
        assertEquals(Lang.TURTLE, getLangForContentType(null));
        assertEquals(Lang.TURTLE, getLangForContentType(ContentType.TEXT_TURTLE));
        assertEquals(Lang.JSONLD, getLangForContentType(ContentType.LD_JSON));
        assertEquals(Lang.RDFXML, getLangForContentType(ContentType.RDF_XML));
        assertEquals(Lang.NTRIPLES, getLangForContentType(ContentType.N_TRIPLES));
    }

    private static String getRdfResourceBody() {
        return "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "PREFIX test: <http://testable.example/ns/testable#>\n" +
                "\n" +
                "<> ldp:contains </data/projects/project-1/milestone-3/> .\n" +
                "\n" +
                "<#project>\n" +
                "  test:id 6 ;\n" +
                "  test:name \"Great Validations\" ;\n" +
                "  test:createdAt \"2021-04-04T20:15:47.000Z\"^^xsd:dateTime ;\n" +
                "  test:active true ;\n" +
                "  test:hasMilestone </data/projects/project-1/milestone-3/#milestone> ;\n" +
                "  test:hasTag\n" +
                "    </tags/tag-1> ,\n" +
                "    </tags/tag-2> ,\n" +
                "    </tags/tag-3> ;\n" +
                "  test:hasComment\n" +
                "    \"First original comment\" ,\n" +
                "    \"Second original comment\" ,\n" +
                "    \"Third original comment\" .";
    }

    private static String getInvalidRdfResourceBody() {
        return "PRE rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "PR rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "PR xml: <http://www.w3.org/XML/1998/namespace>\n" +
                "PRE xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "PREFI ldp: <http://www.w3.org/ns/ldp#>\n" +
                "X test: <http://testable.example/ns/testable#>\n" +
                "\n" +
                "<#project>\n" +
                "  test:id 6 ;\n" +
                "  test:name \"Great Validations\" .\n" +
                "  test:createdAt \"2021-04-04T20:15:47.000Z\"^^xsd:dateTime .\n" +
                "  test:active true ;\n" +
                "  test:hasMilestone </data/projects/project-1/milestone-3/#milestone> ;\n" +
                "  test:hasTag\n" +
                "    </tags/tag-1> ,\n" +
                "    </tags/tag-2> .\n" +
                "    </tags/tag-3> ;\n" +
                "  test:hasComment\n" +
                "    \"First original comment\" ,\n" +
                "    \"Second original comment\" ,\n" +
                "    \"Third original comment\" .";
    }

}

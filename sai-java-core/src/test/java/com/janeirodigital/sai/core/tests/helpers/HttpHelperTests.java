package com.janeirodigital.sai.core.tests.helpers;

import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.enums.LinkRelation;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.tests.fixtures.DispatcherEntry;
import com.janeirodigital.sai.core.tests.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.vocabularies.LdpVocabulary;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import okio.Buffer;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getModelFromString;
import static com.janeirodigital.sai.core.tests.fixtures.MockWebServerHelper.toUrl;
import static org.junit.jupiter.api.Assertions.*;

class HttpHelperTests {

    private static MockWebServer server;
    private static MockWebServer queuingServer;
    private static OkHttpClient httpClient;

    @BeforeAll
    static void beforeAll() throws SaiException {
        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher(List.of(
                new DispatcherEntry(List.of("http/delete-resource"), "DELETE", "/http/delete-resource", null),
                new DispatcherEntry(List.of("http/get-document-html"), "GET", "/http/get-document-html", null),
                new DispatcherEntry(List.of("http/get-image-png"), "GET", "/http/get-image-png", null),
                new DispatcherEntry(List.of("http/get-rdf-container-ttl"), "GET", "/http/get-rdf-container-ttl", null),
                new DispatcherEntry(List.of("http/get-rdf-resource-ttl"), "GET", "/http/get-rdf-resource-ttl", null),
                new DispatcherEntry(List.of("http/not-found"), "GET", "/http/not-found", null),
                new DispatcherEntry(List.of("http/put-create-resource"), "PUT", "/http/put-create-resource", null),
                new DispatcherEntry(List.of("http/put-update-resource"), "PUT", "/http/put-update-resource", null)
        ));
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
        // Initialize another Mock Web Server used specifically for queuing exceptions and assign a queue dispatcher
        // Initialize HTTP client
        httpClient = HttpClientFactory.get(true, false);
        Logger.getLogger(OkHttpClient.class.getName()).setLevel(Level.FINE);
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
    @DisplayName("Get a resource")
    void getHTTPResource() throws SaiException {
        Response response = getResource(httpClient, toUrl(server, "/http/get-document-html"));
        assertTrue(response.isSuccessful());
        response.close();
    }

    @Test
    @DisplayName("Get a required resource")
    void getRequiredHTTPResource() throws SaiException, SaiNotFoundException {
        Response response = getRequiredResource(httpClient, toUrl(server, "/http/get-document-html"));
        assertTrue(response.isSuccessful());
        response.close();
    }

    @Test
    @DisplayName("Get a missing resource")
    void GetMissingHTTPResource() throws SaiException {
        Response response = getResource(httpClient, toUrl(server, "/http/not-found"));
        assertFalse(response.isSuccessful());
        assertEquals(404, response.code());
        response.close();
    }

    @Test
    @DisplayName("Get an RDF resource")
    void getRdfHttpResource() throws SaiException {
        URL url = toUrl(server, "/http/get-rdf-resource-ttl");
        Response response = getRdfResource(httpClient, url);
        assertTrue(response.isSuccessful());
        Model model = getRdfModelFromResponse(response);
        assertNotNull(model);
        assertNotNull(model.getResource(url.toString()));
        response.close();
    }

    @Test
    @DisplayName("Fail to get a resource without content-type")
    void FailToGetHttpResourceNoContentType() throws IOException {
        URL url = toUrl(queuingServer, "/http/get-rdf-resource-ttl-io");
        queuingServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(getRdfBody()));
        assertThrows(SaiException.class, () -> { getRdfResource(httpClient, url); });
    }

    @Test
    @DisplayName("Fail to get an RDF resource with bad content-type")
    void failToGetRdfHttpResourceBadContentType() throws SaiException {
        URL url = toUrl(queuingServer, "/http/get-rdf-resource-ttl-io");
        queuingServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "cool/web")
                .setBody(getRdfBody()));
        assertThrows(SaiException.class, () -> { getRdfResource(httpClient, url); });
    }

    @Test
    @DisplayName("Fail to get an RDF resource due to IO issue")
    void failToGetRdfHttpResourceIO() throws SaiException, IOException {
        URL url = toUrl(queuingServer, "/http/get-rdf-resource-ttl-io");
        queuingServer.enqueue(new MockResponse()
                              .setResponseCode(200)
                              .addHeader("Content-Type", "text/turtle")
                              .setBody(getRdfBody())
                              .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY));
        Response response = getResource(httpClient, url);
        assertTrue(response.isSuccessful());
        assertThrows(SaiException.class, () -> { getRdfModelFromResponse(response); });
        response.close();
    }

    @Test
    @DisplayName("Fail to get a required resource")
    void FailToGetRequiredMissingHttpResource() throws IOException {
        queuingServer.enqueue(new MockResponse().setResponseCode(404).setBody(""));
        assertThrows(SaiNotFoundException.class, () -> {
            getRequiredResource(httpClient, toUrl(queuingServer, "/http/no-resource"));
        });
        queuingServer.enqueue(new MockResponse().setResponseCode(401).setBody(""));
        assertThrows(SaiException.class, () -> {
            getRequiredResource(httpClient, toUrl(queuingServer, "/http/not-authorized"));
        });
    }

    @Test
    @DisplayName("Fail to get a resource due to IO issue")
    void FailToGetHttpResourceIO() throws IOException {
        queuingServer.enqueue(new MockResponse()
                .setBody(new Buffer().write(new byte[4096]))
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        assertThrows(SaiException.class, () -> {
            getResource(httpClient, toUrl(queuingServer, "/http/get-document-io"));
        });
    }

    @Test
    @DisplayName("Update a resource")
    void updateHttpResource() throws SaiException {
        Response response = putResource(httpClient, toUrl(server, "/http/put-update-resource"), null, getHtmlBody(), "text/html");
        assertTrue(response.isSuccessful());
    }

    @Test
    @DisplayName("Update an RDF resource")
    void updateRdfHttpResource() throws SaiException {
        URL url = toUrl(server, "/http/put-update-resource");
        Model model = getModelFromString(urlToUri(url), getRdfBody(), "text/turtle");
        Resource resource = model.getResource(url.toString() + "#project");
        // Update with resource content
        Response response = putRdfResource(httpClient, url, resource);
        assertTrue(response.isSuccessful());
        response.close();
        // Update with no resource content (treated as empty body)
        response = putRdfResource(httpClient, url, null);
        assertTrue(response.isSuccessful());
        response.close();
    }

    @Test
    @DisplayName("Create an RDF container")
    void createRdfContainerHttpResource() throws SaiException {
        URL url = toUrl(server, "/http/put-create-resource");
        Model model = getModelFromString(urlToUri(url), getRdfContainerBody(), "text/turtle");
        Resource resource = model.getResource(url.toString() + "#project");
        Response response = putRdfContainer(httpClient, url, resource);
        assertTrue(response.isSuccessful());
        response.close();
    }

    @Test
    @DisplayName("Fail to update a resource due to IO issue")
    void failToUpdateHttpResourceIO() throws SaiException, IOException {
        queuingServer.enqueue(new MockResponse()
                .setBody(new Buffer().write(new byte[4096]))
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        assertThrows(SaiException.class, () -> {
            putResource(httpClient, toUrl(queuingServer, "/http/put-update-resource-io"), null, getHtmlBody(), "text/html");
        });
    }

    @Test
    @DisplayName("Create a resource")
    void createHttpResource() throws SaiException {
        Response response = putResource(httpClient, toUrl(server, "/http/put-create-resource"), null, getHtmlBody(), "text/html");
        assertTrue(response.isSuccessful());
    }

    @Test
    @DisplayName("Delete a resource")
    void deleteHttpResource() throws SaiException {
        Response response = deleteResource(httpClient, toUrl(server, "/http/delete-resource"));
        assertTrue(response.isSuccessful());
    }

    @Test
    @DisplayName("Fail to delete a resource due to IO issue")
    void FailToDeleteHttpResourceIO() {
        queuingServer.enqueue(new MockResponse()
                .setBody(new Buffer().write(new byte[4096]))
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        assertThrows(SaiException.class, () -> {
            deleteResource(httpClient, toUrl(queuingServer, "/http/delete-resource-io"));
        });
    }

    @Test
    @DisplayName("Set various HTTP headers")
    void confirmSetHttpHeader() {

        // Add to an empty list of headers
        Headers solo = setHttpHeader(HttpHeader.IF_NONE_MATCH, "*");
        assertEquals(1, solo.size());
        assertEquals("*", solo.get("If-None-Match"));

        // Append to an existing list of headers
        Headers.Builder builder = new Headers.Builder();
        builder.add("Content-Type", "text/html");
        Headers appended = builder.build();
        appended = setHttpHeader(HttpHeader.IF_NONE_MATCH, "*", appended);
        assertEquals(2, appended.size());
        assertEquals("*", appended.get("If-None-Match"));
        assertEquals("text/html", appended.get("Content-Type"));

        // Ensure duplicates aren't added
        appended = setHttpHeader(HttpHeader.IF_NONE_MATCH, "*", appended);
        assertEquals(2, appended.size());

    }

    @Test
    @DisplayName("Add various HTTP headers")
    void confirmAddHttpHeader() {

        // Add to an empty list of headers
        Headers solo = addHttpHeader(HttpHeader.LINK, LinkRelation.ACL.getValue());
        assertEquals(1, solo.size());
        assertEquals(LinkRelation.ACL.getValue(), solo.get(HttpHeader.LINK.getValue()));

        // Append to an existing list of headers
        Headers.Builder builder = new Headers.Builder();
        builder.add(HttpHeader.CONTENT_TYPE.getValue(), "text/html");
        Headers appended = builder.build();
        appended = setHttpHeader(HttpHeader.IF_NONE_MATCH, "*", appended);
        assertEquals(2, appended.size());
        assertEquals("*", appended.get(HttpHeader.IF_NONE_MATCH.getValue()));
        assertEquals("text/html", appended.get(HttpHeader.CONTENT_TYPE.getValue()));

        Headers added = addHttpHeader(HttpHeader.LINK, LinkRelation.DESCRIBED_BY.getValue());
        added = addHttpHeader(HttpHeader.LINK, LinkRelation.MANAGED_BY.getValue(), added);
        assertEquals(2, added.size());

    }

    @Test
    @DisplayName("Add Link relation headers")
    void confirmManageLinkRelationHttpHeaders() {

        // Add to an empty list of headers
        Headers relations = addLinkRelationHeader(LinkRelation.TYPE, LdpVocabulary.BASIC_CONTAINER.getURI());
        relations = addLinkRelationHeader(LinkRelation.TYPE, LdpVocabulary.CONTAINER.getURI(), relations);
        relations = addLinkRelationHeader(LinkRelation.ACL, "https://some.pod.example/resource.acl", relations);
        assertEquals(3, relations.size());

        Map<String, List<String>> headerMap = relations.toMultimap();
        List<String> linkHeaders = headerMap.get(HttpHeader.LINK.getValue());
        assertTrue(linkHeaders.contains(getLinkRelationString(LinkRelation.TYPE, LdpVocabulary.BASIC_CONTAINER.getURI())));
        assertTrue(linkHeaders.contains(getLinkRelationString(LinkRelation.TYPE, LdpVocabulary.CONTAINER.getURI())));
        assertTrue(linkHeaders.contains(getLinkRelationString(LinkRelation.ACL, "https://some.pod.example/resource.acl")));

    }

    @Test
    @DisplayName("Convert URL to URI")
    void convertUrlToUri() throws MalformedURLException {
        URL url = new URL("http://www.solidproject.org/");
        URI uri = urlToUri(url);
        assertEquals(url, uri.toURL());
    }

    @Test
    @DisplayName("Fail to convert URL to URI")
    void failToConvertUrlToUri() throws MalformedURLException {
        URL url = new URL("http://www.solidproject.org?q=something&something=<something+else>");
        assertThrows(IllegalStateException.class, () -> { urlToUri(url); });
    }

    private String getHtmlBody() {
         return "<!DOCTYPE html><html><body><h1>Regular HTML Resource</h1></body></html>";
    }

    private String getRdfBody() {
        return "  PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
               "  PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
               "  PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
               "  PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
               "  PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
               "  PREFIX ex: <http://www.example.com/ns/ex#>\n" +
               "\n" +
               "  <#project>\n" +
               "    ex:uri </data/projects/project-1/#project> ;\n" +
               "    ex:id 6 ;\n" +
               "    ex:name \"Great Validations\" ;\n" +
               "    ex:created_at \"2021-04-04T20:15:47.000Z\"^^xsd:dateTime ;\n" +
               "    ex:hasMilestone </data/projects/project-1/milestone-3/#milestone> .";
    }

    private String getRdfContainerBody() {
        return "  PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "  PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "  PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
                "  PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "  PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "  PREFIX ex: <http://www.example.com/ns/ex#>\n" +
                "\n" +
                "  <> ldp:contains </data/projects/project-1/milestone-3/> .\n" +
                "\n" +
                "  <#project>\n" +
                "    ex:uri </data/projects/project-1/#project> ;\n" +
                "    ex:id 6 ;\n" +
                "    ex:name \"Great Validations\" ;\n" +
                "    ex:created_at \"2021-04-04T20:15:47.000Z\"^^xsd:dateTime ;\n" +
                "    ex:hasMilestone </data/projects/project-1/milestone-3/#milestone> .";
    }

}

package com.janeirodigital.sai.core.utils;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.enums.LinkRelation;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.fixtures.DispatcherEntry;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
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

import static com.janeirodigital.sai.core.enums.ContentType.TEXT_HTML;
import static com.janeirodigital.sai.core.enums.ContentType.TEXT_TURTLE;
import static com.janeirodigital.sai.core.enums.HttpHeader.AUTHORIZATION;
import static com.janeirodigital.sai.core.enums.HttpHeader.CONTENT_TYPE;
import static com.janeirodigital.sai.core.utils.HttpUtils.*;
import static com.janeirodigital.sai.core.utils.RdfUtils.getModelFromString;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static org.junit.jupiter.api.Assertions.*;

class HttpUtilsTests {

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
        HttpClientFactory clientFactory = new HttpClientFactory(true, false, false);
        httpClient = clientFactory.get();
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
    @DisplayName("Get an RDF resource with headers")
    void getRdfHttpResourceHeaders() throws SaiException {
        URL url = toUrl(server, "/http/get-rdf-resource-ttl");
        Headers headers = setHttpHeader(AUTHORIZATION, "some-token-value");
        Response response = getRdfResource(httpClient, url, headers);
        assertTrue(response.isSuccessful());
        Model model = getRdfModelFromResponse(response);
        assertNotNull(model);
        assertNotNull(model.getResource(url.toString()));
        response.close();
    }

    @Test
    @DisplayName("Fail to get a resource without content-type")
    void FailToGetHttpResourceNoContentType() {
        URL url = toUrl(queuingServer, "/http/get-rdf-resource-ttl-no-ct");
        queuingServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody(getRdfBody()));
        assertThrows(SaiException.class, () -> { getRdfResource(httpClient, url); });
    }

    @Test
    @DisplayName("Fail to get an RDF resource with bad content-type")
    void failToGetRdfHttpResourceBadContentType() {
        URL url = toUrl(queuingServer, "/http/get-rdf-resource-ttl-coolweb");
        queuingServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "cool/web")
                .setBody(getRdfBody()));
        assertThrows(SaiException.class, () -> { getRdfResource(httpClient, url); });
    }

    @Test
    @DisplayName("Fail to get an RDF resource with non-rdf content-type")
    void failToGetRdfHttpResourceNonRdfContentType() {
        URL url = toUrl(queuingServer, "/http/get-rdf-resource-ttl-nonrdf");
        queuingServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/octet-stream")
                .setBody(getRdfBody()));
        assertThrows(SaiException.class, () -> { getRdfResource(httpClient, url); });
    }

    @Test
    @DisplayName("Fail to get an RDF resource due to IO issue")
    void failToGetRdfHttpResourceIO() throws SaiException {
        URL url = toUrl(queuingServer, "/http/get-rdf-resource-ttl-io");
        queuingServer.enqueue(new MockResponse()
                              .setResponseCode(200)
                              .addHeader(CONTENT_TYPE.getValue(), TEXT_TURTLE.getValue())
                              .setBody(getRdfBody())
                              .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY));
        Response response = getResource(httpClient, url);
        assertTrue(response.isSuccessful());
        assertThrows(SaiException.class, () -> { getRdfModelFromResponse(response); });
        response.close();
    }

    @Test
    @DisplayName("Fail to get a required resource")
    void FailToGetRequiredMissingHttpResource() {
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
    @DisplayName("Fail to get a resource and log details")
    void FailToGetResourceAndLog() throws SaiException {
        queuingServer.enqueue(new MockResponse().setResponseCode(404).setBody(""));
        try(Response response = getResource(httpClient, toUrl(queuingServer, "/http/no-resource"))) {
            assertNotNull(getResponseFailureMessage(response));
        }
        queuingServer.enqueue(new MockResponse().setResponseCode(404).setBody(""));
        Response response = deleteResource(httpClient, toUrl(queuingServer, "/path/no-resource"));
    }

    @Test
    @DisplayName("Fail to get a resource due to IO issue")
    void FailToGetHttpResourceIO() {
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
        Response response = putResource(httpClient, toUrl(server, "/http/put-update-resource"), null, getHtmlBody(), ContentType.TEXT_HTML);
        assertTrue(response.isSuccessful());
        response = putResource(httpClient, toUrl(server, "/http/put-update-resource"), null, null, ContentType.TEXT_HTML);
        assertTrue(response.isSuccessful());
    }

    @Test
    @DisplayName("Update an RDF resource")
    void updateRdfHttpResource() throws SaiException {
        URL url = toUrl(server, "/http/put-update-resource");
        Model model = getModelFromString(urlToUri(url), getRdfBody(), TEXT_TURTLE);
        Resource resource = model.getResource(url + "#project");
        // Update with resource content
        Response response = putRdfResource(httpClient, url, resource, TEXT_TURTLE);
        assertTrue(response.isSuccessful());
        response.close();
        // Update with no resource content (treated as empty body)
        Headers headers = null;
        response = putRdfResource(httpClient, url, null, TEXT_TURTLE, headers);
        assertTrue(response.isSuccessful());
        response.close();
    }

    @Test
    @DisplayName("Create an RDF container")
    void createRdfContainerHttpResource() throws SaiException {
        URL url = toUrl(server, "/http/put-create-resource");
        Model model = getModelFromString(urlToUri(url), getRdfContainerBody(), TEXT_TURTLE);
        Resource resource = model.getResource(url + "#project");
        Response response = putRdfContainer(httpClient, url, resource, TEXT_TURTLE);
        assertTrue(response.isSuccessful());
        response.close();
    }

    @Test
    @DisplayName("Fail to update a resource due to IO issue")
    void failToUpdateHttpResourceIO() {
        queuingServer.enqueue(new MockResponse()
                .setBody(new Buffer().write(new byte[4096]))
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));
        assertThrows(SaiException.class, () -> {
            putResource(httpClient, toUrl(queuingServer, "/http/put-update-resource-io"), null, getHtmlBody(), TEXT_TURTLE);
        });
    }

    @Test
    @DisplayName("Create a resource")
    void createHttpResource() throws SaiException {
        Response response = putResource(httpClient, toUrl(server, "/http/put-create-resource"), null, getHtmlBody(), TEXT_TURTLE);
        assertTrue(response.isSuccessful());
    }

    @Test
    @DisplayName("Delete a resource")
    void deleteHttpResource() throws SaiException {
        Headers headers = setHttpHeader(AUTHORIZATION, "some-token-value");
        Response response = deleteResource(httpClient, toUrl(server, "/http/delete-resource"), headers);
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
        builder.add(CONTENT_TYPE.getValue(), TEXT_HTML.getValue());
        Headers appended = builder.build();
        appended = setHttpHeader(HttpHeader.IF_NONE_MATCH, "*", appended);
        assertEquals(2, appended.size());
        assertEquals("*", appended.get("If-None-Match"));
        assertEquals(TEXT_HTML.getValue(), appended.get("Content-Type"));

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
        builder.add(CONTENT_TYPE.getValue(), TEXT_HTML.getValue());
        Headers appended = builder.build();
        appended = setHttpHeader(HttpHeader.IF_NONE_MATCH, "*", appended);
        assertEquals(2, appended.size());
        assertEquals("*", appended.get(HttpHeader.IF_NONE_MATCH.getValue()));
        assertEquals(TEXT_HTML.getValue(), appended.get(CONTENT_TYPE.getValue()));

        Headers added = addHttpHeader(HttpHeader.LINK, LinkRelation.DESCRIBED_BY.getValue());
        added = addHttpHeader(HttpHeader.LINK, LinkRelation.MANAGED_BY.getValue(), added);
        assertEquals(2, added.size());

    }

    @Test
    @DisplayName("Add Link relation headers")
    void confirmManageLinkRelationHttpHeaders() {

        // Add to an empty list of headers
        Headers relations = addLinkRelationHeader(LinkRelation.TYPE, LdpVocabulary.LDP_BASIC_CONTAINER.getURI());
        relations = addLinkRelationHeader(LinkRelation.TYPE, LdpVocabulary.LDP_CONTAINER.getURI(), relations);
        relations = addLinkRelationHeader(LinkRelation.ACL, "https://some.pod.example/resource.acl", relations);
        assertEquals(3, relations.size());

        Map<String, List<String>> headerMap = relations.toMultimap();
        List<String> linkHeaders = headerMap.get(HttpHeader.LINK.getValue());
        assertTrue(linkHeaders.contains(getLinkRelationString(LinkRelation.TYPE, LdpVocabulary.LDP_BASIC_CONTAINER.getURI())));
        assertTrue(linkHeaders.contains(getLinkRelationString(LinkRelation.TYPE, LdpVocabulary.LDP_CONTAINER.getURI())));
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
    @DisplayName("Fail to convert URL to URI - malformed URL")
    void failToConvertUrlToUri() throws MalformedURLException {
        URL url = new URL("http://www.solidproject.org?q=something&something=<something+else>");
        assertThrows(IllegalStateException.class, () -> { urlToUri(url); });
    }

    @Test
    @DisplayName("Get the base of a URL")
    void convertUrlToBase() throws MalformedURLException, SaiException {
        URL onlyQuery = new URL("http://www.solidproject.org/folder/resource?something=value&other=othervalue");
        URL onlyFragment = new URL("http://www.solidproject.org/folder/resource#somefragment");
        URL both = new URL("http://www.solidproject.org/folder/resource#somefragment?something=value");
        URL expected = new URL("http://www.solidproject.org/folder/resource");
        assertEquals(expected, urlToBase(onlyQuery));
        assertEquals(expected, urlToBase(onlyFragment));
        assertEquals(expected, urlToBase(both));
        assertEquals(expected, urlToBase(expected));
    }

    @Test
    @DisplayName("Fail to get the base of a URL - malformed URL")
    void failToConvertUrlToBase() throws MalformedURLException {
        URL malformed = new URL("http://www.solidproject.org?q=something&something=<something+else>");
        assertThrows(SaiException.class, () -> urlToBase(malformed));
    }


    @Test
    @DisplayName("Convert string to URL")
    void convertStringToUrl() throws SaiException, MalformedURLException {
        URL expected = new URL("http://www.solidproject.org");
        assertEquals(expected, stringToUrl("http://www.solidproject.org"));
    }

    @Test
    @DisplayName("Fail to convert string to URL - malformed URL")
    void failToConvertStringToUrl() {
        assertThrows(SaiException.class, () -> stringToUrl("ddd:\\--solidproject_orgZq=something&something=<something+else>"));
    }

    @Test
    @DisplayName("Fail to convert URI to URL - malformed URL")
    void failToConvertUriToUrl() {
        assertThrows(SaiException.class, () -> uriToUrl(URI.create("somescheme://what/path")));
    }

    @Test
    @DisplayName("Add child to URL Path")
    void testAddChildToUrlPath() throws SaiException, MalformedURLException {
        URL base = new URL("http://www.solidproject.org/");
        URL added = addChildToUrlPath(base, "child");
        assertEquals("http://www.solidproject.org/child", added.toString());
    }

    @Test
    @DisplayName("Fail to add child to URL path - malformed URL")
    void failToAddChildToUrlPath() throws MalformedURLException {
        URL base = new URL("http://www.solidproject.org/");
        assertThrows(SaiException.class, () -> addChildToUrlPath(base, "somescheme://what/"));
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

package com.janeirodigital.sai.core.http;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.fixtures.DispatcherEntry;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.List;

import static com.janeirodigital.sai.core.enums.ContentType.TEXT_TURTLE;
import static com.janeirodigital.sai.core.helpers.HttpHelper.putRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.urlToUri;
import static com.janeirodigital.sai.core.helpers.RdfUtils.getModelFromString;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpClientValidationTests {

    private static HttpClientFactory factory;
    private static MockWebServer server;
    private static URL validatingUrl;
    private static Resource validResource;
    private static Resource invalidResource;

    @BeforeAll
    static void beforeAll() throws SaiException {
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher(List.of(
                new DispatcherEntry(List.of("http/validating-resource-ttl"), "GET", "/http/validating-resource", null),
                new DispatcherEntry(List.of("http/put-update-resource"), "PUT", "/http/validating-resource", null),
                new DispatcherEntry(List.of("http/validating-resource-manager-ttl"), "GET", "/http/validating-resource.shapetree", null),
                new DispatcherEntry(List.of("http/shapetree-ttl"), "GET", "/http/shapetree", null),
                new DispatcherEntry(List.of("http/shape-shex"), "GET", "/http/shape", null)
        ));
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        validatingUrl = toUrl(server, "/http/validating-resource");
        // Valid dataset
        Model validModel = getModelFromString(urlToUri(validatingUrl), getValidResourceBody(), TEXT_TURTLE);
        validResource = validModel.getResource(validatingUrl.toString() + "#testable");
        // Invalid dataset
        Model invalidModel = getModelFromString(urlToUri(validatingUrl), getInvalidResourceBody(), TEXT_TURTLE);
        invalidResource = invalidModel.getResource(validatingUrl.toString() + "#testable");
    }

    @Test
    @DisplayName("Client shape tree validation allows valid data")
    void clientValidatesValidData() throws SaiException {
        factory = new HttpClientFactory(false, true, false);
        OkHttpClient validatingClient = factory.get();
        Response response = putRdfResource(validatingClient, validatingUrl, validResource, TEXT_TURTLE);
        assertTrue(response.isSuccessful());
    }

    @Test
    @DisplayName("Client shape tree validation fails invalid data")
    void clientFailsInvalidData() throws SaiException {
        factory = new HttpClientFactory(false, true, false);
        OkHttpClient validatingClient = factory.get();
        Response response = putRdfResource(validatingClient, validatingUrl, invalidResource, TEXT_TURTLE);
        assertFalse(response.isSuccessful());
    }

    @Test
    @DisplayName("Disabled client shape tree validation allows invalid data")
    void disabledClientAllowInvalidData() throws SaiException {
        factory = new HttpClientFactory(false, false, false);
        OkHttpClient validatingClient = factory.get();
        Response response = putRdfResource(validatingClient, validatingUrl, invalidResource, TEXT_TURTLE);
        assertTrue(response.isSuccessful());
    }

    private static String getValidResourceBody() {
        return "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
               "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
               "PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
               "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
               "PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
               "PREFIX ex: <http://www.example.com/ns/ex#>\n" +
               "\n" +
               "<#testable>\n" +
               "    ex:uri <#testable> ;\n" +
               "    ex:id 6 ;\n" +
               "    ex:name \"Testable Data!\" ;\n" +
               "    ex:created_at \"2021-04-04T20:15:47.000Z\"^^xsd:dateTime .";
    }

    private static String getInvalidResourceBody() {
        // Missing ex:name which is required by the associated shape
        return  "  PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                "  PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
                "  PREFIX xml: <http://www.w3.org/XML/1998/namespace>\n" +
                "  PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
                "  PREFIX ldp: <http://www.w3.org/ns/ldp#>\n" +
                "  PREFIX ex: <http://www.example.com/ns/ex#>\n" +
                "\n" +
                "  <#testable>\n" +
                "    ex:id 6 ;\n" +
                "    ex:created_at \"2021-04-04T20:15:47.000Z\"^^xsd:dateTime .";
    }

}

package com.janeirodigital.sai.core.http;

import com.janeirodigital.mockwebserver.DispatcherEntry;
import com.janeirodigital.mockwebserver.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.httputils.SaiHttpException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static com.janeirodigital.mockwebserver.MockWebServerHelper.toMockUri;
import static com.janeirodigital.sai.httputils.ContentType.TEXT_TURTLE;
import static com.janeirodigital.sai.httputils.HttpUtils.putRdfResource;
import static com.janeirodigital.sai.rdfutils.RdfUtils.getModelFromString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpClientValidationTests {

    private static HttpClientFactory factory;
    private static MockWebServer server;
    private static URI validatingUri;
    private static Resource validResource;
    private static Resource invalidResource;

    @BeforeAll
    static void beforeAll() throws SaiRdfException {
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher(List.of(
                new DispatcherEntry(List.of("http/validating-resource-ttl"), "GET", "/http/validating-resource", null),
                new DispatcherEntry(List.of("http/put-update-resource"), "PUT", "/http/validating-resource", null),
                new DispatcherEntry(List.of("http/validating-resource-manager-ttl"), "GET", "/http/validating-resource.shapetree", null),
                new DispatcherEntry(List.of("http/shapetree-ttl"), "GET", "/http/shapetree", null),
                new DispatcherEntry(List.of("http/shape-shex"), "GET", "/http/shape", null)
        ));
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        validatingUri = toMockUri(server, "/http/validating-resource");
        // Valid dataset
        Model validModel = getModelFromString(validatingUri, getValidResourceBody(), TEXT_TURTLE.getValue());
        validResource = validModel.getResource(validatingUri.toString() + "#testable");
        // Invalid dataset
        Model invalidModel = getModelFromString(validatingUri, getInvalidResourceBody(), TEXT_TURTLE.getValue());
        invalidResource = invalidModel.getResource(validatingUri.toString() + "#testable");
    }

    @Test
    @DisplayName("Client shape tree validation allows valid data")
    void clientValidatesValidData() throws SaiException, SaiHttpException {
        factory = new HttpClientFactory(false, true, false);
        OkHttpClient validatingClient = factory.get();
        Response response = putRdfResource(validatingClient, validatingUri, validResource, TEXT_TURTLE);
        assertTrue(response.isSuccessful());
    }

    @Test
    @DisplayName("Client shape tree validation fails invalid data")
    void clientFailsInvalidData() throws SaiException, SaiHttpException {
        factory = new HttpClientFactory(false, true, false);
        OkHttpClient validatingClient = factory.get();
        Response response = putRdfResource(validatingClient, validatingUri, invalidResource, TEXT_TURTLE);
        assertFalse(response.isSuccessful());
    }

    @Test
    @DisplayName("Disabled client shape tree validation allows invalid data")
    void disabledClientAllowInvalidData() throws SaiException, SaiHttpException {
        factory = new HttpClientFactory(false, false, false);
        OkHttpClient validatingClient = factory.get();
        Response response = putRdfResource(validatingClient, validatingUri, invalidResource, TEXT_TURTLE);
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

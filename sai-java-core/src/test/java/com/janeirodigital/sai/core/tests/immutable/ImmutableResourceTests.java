package com.janeirodigital.sai.core.tests.immutable;

import com.janeirodigital.sai.core.DataFactory;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.tests.fixtures.DispatcherEntry;
import com.janeirodigital.sai.core.tests.fixtures.RequestMatchingFixtureDispatcher;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.jena.rdf.model.Model;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static com.janeirodigital.sai.core.helpers.HttpHelper.urlToUri;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getModelFromFile;
import static com.janeirodigital.sai.core.tests.fixtures.MockWebServerHelper.toUrl;

class ImmutableResourceTests {

    private static DataFactory dataFactory;
    private static MockWebServer server;

    @BeforeAll
    static void beforeAll() throws SaiException {
        // Initialize the Data Factory
        dataFactory = new DataFactory(HttpClientFactory.get(false, false));

        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher(List.of(
                new DispatcherEntry(List.of("immutable/immutable-resource-ttl"), "GET", "/immutable/immutable-resource", null),
                new DispatcherEntry(List.of("immutable/immutable-resource-ttl"), "PUT", "/immutable/immutable-resource", null)
        ));

        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
    }

    @Test
    @DisplayName("Store an Immutable resource")
    void storeImmutableResource() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/immutable/immutable-resource#project");
        Model model = loadModel(url, "fixtures/immutable/immutable-resource.ttl", "text/turtle");
        TestableImmutableResource testable = new TestableImmutableResource(url, dataFactory, model.getResource(url.toString()));
        testable.store();
    }

    private Model loadModel(URL url, String filePath, String contentType) throws SaiException {
        try {
            return getModelFromFile(urlToUri(url), "fixtures/immutable/immutable-resource.ttl", contentType);
        } catch (SaiException | IOException ex) {
            throw new SaiException("Failed too load test model from file: " + filePath);
        }
    }

}

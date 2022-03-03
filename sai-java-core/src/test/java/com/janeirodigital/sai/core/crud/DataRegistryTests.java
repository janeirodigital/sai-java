package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.exceptions.SaiRuntimeException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.janeirodigital.sai.core.enums.ContentType.LD_JSON;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.*;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static com.janeirodigital.sai.core.helpers.HttpHelper.stringToUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataRegistryTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static List<URL> dataRegistrationUrls;

    @BeforeAll
    static void beforeAll() throws SaiException {

        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        // GET data registry in Turtle
        mockOnGet(dispatcher, "/ttl/data/", "crud/data-registry-ttl");
        mockOnGet(dispatcher, "/ttl/empty/data/", "crud/data-registry-empty-ttl");
        mockOnGet(dispatcher, "/ttl/data/dr-1/", "crud/data-registration-1-ttl");
        mockOnGet(dispatcher, "/ttl/data/dr-2/", "crud/data-registration-2-ttl");
        mockOnGet(dispatcher, "/ttl/data/dr-3/", "crud/data-registration-3-ttl");
        mockOnPut(dispatcher, "/new/ttl/data/", "http/201");  // create new
        mockOnPut(dispatcher, "/ttl/data/", "http/204");  // update existing
        mockOnDelete(dispatcher, "/ttl/data/", "http/204");  // delete
        // GET agent registry in Turtle with invalid fields
        mockOnGet(dispatcher, "/invalid-fields/ttl/data/", "crud/data-registry-invalid-ttl");
        // GET data registry in Turtle with links to registrations that don't exist
        mockOnGet(dispatcher, "/missing-registrations/ttl/data/", "crud/data-registry-missing-registrations-ttl");
        // GET data registry in JSON-LD
        mockOnGet(dispatcher, "/jsonld/data/", "crud/data-registry-jsonld");
        mockOnPut(dispatcher, "/new/jsonld/data/", "http/201");  // create new
        mockOnPut(dispatcher, "/jsonld/data/", "http/204");  // update existing or delete
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
        dataRegistrationUrls = Arrays.asList(toUrl(server, "/ttl/data/dr-1/"),
                                              toUrl(server, "/ttl/data/dr-2/"),
                                              toUrl(server, "/ttl/data/dr-3/"));
    }

    @Test
    @DisplayName("Create a data registry")
    void createNewDataRegistry() throws SaiException {
        URL url = toUrl(server, "/new/ttl/data/");
        DataRegistry dataRegistry = new DataRegistry.Builder(url, saiSession).build();
        assertDoesNotThrow(() -> dataRegistry.update());
        assertNotNull(dataRegistry);
    }

    @Test
    @DisplayName("Get a data registry")
    void readDataRegistry() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/data/");
        DataRegistry dataRegistry = DataRegistry.get(url, saiSession);
        checkRegistry(dataRegistry);
        assertFalse(dataRegistry.isEmpty());
    }

    @Test
    @DisplayName("Get an empty data registry")
    void readEmptyDataRegistry() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/empty/data/");
        DataRegistry dataRegistry = DataRegistry.get(url, saiSession);
        assertTrue(dataRegistry.isEmpty());
    }

    @Test
    @DisplayName("Fail to get data registry - invalid fields")
    void failToGetDataRegistry() {
        URL url = toUrl(server, "/invalid-fields/ttl/data/");
        assertThrows(SaiException.class, () -> DataRegistry.get(url, saiSession));
    }

    @Test
    @DisplayName("Reload data registry")
    void reloadDataRegistry() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/data/");
        DataRegistry dataRegistry = DataRegistry.get(url, saiSession);
        DataRegistry reloaded = dataRegistry.reload();
        checkRegistry(reloaded);
    }

    @Test
    @DisplayName("Find a data registration")
    void findDataRegistration() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/data/");
        URL toFind = stringToUrl("http://data.example/shapetrees/pm#ProjectTree");
        URL toFail = stringToUrl("http://data.example/shapetrees/pm#MissingTree");
        DataRegistry dataRegistry = DataRegistry.get(url, saiSession);
        DataRegistration found = dataRegistry.getDataRegistrations().find(toFind);
        assertEquals(toFind, found.getRegisteredShapeTree());
        DataRegistration fail = dataRegistry.getDataRegistrations().find(toFail);
        assertNull(fail);
    }

    @Test
    @DisplayName("Fail to iterate data registrations - missing registration")
    void failToFindDataRegistrationMissing() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/missing-registrations/ttl/data/");
        DataRegistry dataRegistry = DataRegistry.get(url, saiSession);
        Iterator<DataRegistration> iterator = dataRegistry.getDataRegistrations().iterator();
        assertThrows(SaiRuntimeException.class, () -> iterator.next());
    }
    
    @Test
    @DisplayName("Read existing data registry in JSON-LD")
    void readDataRegistryJsonLd() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/jsonld/data/");
        DataRegistry dataRegistry = DataRegistry.get(url, saiSession, LD_JSON);
        checkRegistry(dataRegistry);
    }

    @Test
    @DisplayName("Create new crud data registry in JSON-LD")
    void createNewDataRegistryJsonLd() throws SaiException {
        URL url = toUrl(server, "/new/jsonld/data/");
        DataRegistry dataRegistry = new DataRegistry.Builder(url, saiSession).setContentType(LD_JSON).build();
        assertDoesNotThrow(() -> dataRegistry.update());
        assertNotNull(dataRegistry);
    }

    @Test
    @DisplayName("Delete crud data registry")
    void deleteDataRegistry() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/data/");
        DataRegistry dataRegistry = DataRegistry.get(url, saiSession);
        assertDoesNotThrow(() -> dataRegistry.delete());
        assertFalse(dataRegistry.isExists());
    }

    @Test
    @DisplayName("Add data registration to data registry")
    void addAgentRegistrations() throws SaiException, SaiNotFoundException, SaiAlreadyExistsException {
        URL url = toUrl(server, "/ttl/data/");
        DataRegistry dataRegistry = DataRegistry.get(url, saiSession);

        URL drUrl = toUrl(server, "/ttl/data/dr-5/");
        URL drTree = stringToUrl("http://data.example/shapetrees/pm#StatusTree");
        DataRegistration registration = mock(DataRegistration.class);
        when(registration.getUrl()).thenReturn(drUrl);
        when(registration.getRegisteredShapeTree()).thenReturn(drTree);
        dataRegistry.add(registration);
    }

    @Test
    @DisplayName("Remove data registrations from data registry")
    void removeAgentRegistrations() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/data/");
        DataRegistry dataRegistry = DataRegistry.get(url, saiSession);

        URL drUrl = toUrl(server, "/ttl/data/dr-1/");
        DataRegistration registration = mock(DataRegistration.class);
        when(registration.getUrl()).thenReturn(drUrl);
        dataRegistry.remove(registration);
    }

    @Test
    @DisplayName("Fail to add data registrations to data registry - already exists")
    void failToAddDataRegistration() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/data/");
        DataRegistry dataRegistry = DataRegistry.get(url, saiSession);

        URL drUrl = toUrl(server, "/ttl/data/registration-1/");
        URL drTree = stringToUrl("http://data.example/shapetrees/pm#ProjectTree");
        DataRegistration registration = mock(DataRegistration.class);
        when(registration.getUrl()).thenReturn(drUrl);
        when(registration.getRegisteredShapeTree()).thenReturn(drTree);
        assertThrows(SaiAlreadyExistsException.class, () -> dataRegistry.add(registration));
    }

    private void checkRegistry(DataRegistry dataRegistry) {
        assertNotNull(dataRegistry);
        assertTrue(dataRegistrationUrls.containsAll(dataRegistry.getDataRegistrations().getRegistrationUrls()));
    }
}

package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.readable.ReadableDataRegistration;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpException;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.*;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static com.janeirodigital.sai.httputils.ContentType.LD_JSON;
import static com.janeirodigital.sai.httputils.HttpUtils.stringToUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DataRegistrationTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static URL dr1RegisteredBy;
    private static URL dr1RegisteredWith;
    private static OffsetDateTime dr1RegisteredAt;
    private static OffsetDateTime dr1UpdatedAt;
    private static URL dr1RegisteredShapeTree;

    @BeforeAll
    static void beforeAll() throws SaiException, SaiHttpException {

        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        // GET data registration in Turtle
        mockOnGet(dispatcher, "/ttl/data/dr-1/", "crud/data-registration-1-ttl");
        mockOnPut(dispatcher, "/new/ttl/data/dr-1/", "http/201");  // create new
        mockOnPut(dispatcher, "/ttl/data/dr-1/", "http/204");  // update existing
        mockOnDelete(dispatcher, "/ttl/data/dr-1/", "http/204");  // delete
        // GET crud data registration in Turtle with missing fields
        mockOnGet(dispatcher, "/missing-fields/ttl/data/dr-1/", "crud/data-registration-1-missing-fields-ttl");
        // GET crud data registration in JSON-LD
        mockOnGet(dispatcher, "/jsonld/data/dr-1/", "crud/data-registration-1-jsonld");
        mockOnPut(dispatcher, "/new/jsonld/data/dr-1/", "http/201");  // create new
        mockOnPut(dispatcher, "/jsonld/data/dr-1/", "http/204");  // update existing or delete
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        dr1RegisteredBy = stringToUrl("https://alice.example/id#me");
        dr1RegisteredWith = toUrl(server, "https://trusted.example/id#app");
        dr1RegisteredAt = OffsetDateTime.parse("2021-04-04T20:15:47.000Z", DateTimeFormatter.ISO_DATE_TIME);
        dr1UpdatedAt = OffsetDateTime.parse("2021-04-04T20:15:47.000Z", DateTimeFormatter.ISO_DATE_TIME);
        dr1RegisteredShapeTree = toUrl(server, "/shapetrees/pm#ProjectTree");
    }

    @Test
    @DisplayName("Create new crud data registration")
    void createNewCrudDataRegistration() throws SaiException {
        URL url = toUrl(server, "/new/ttl/data/dr-1/");
        DataRegistration.Builder builder = new DataRegistration.Builder(url, saiSession);
        DataRegistration registration = builder.setRegisteredBy(dr1RegisteredBy).setRegisteredWith(dr1RegisteredWith)
                                                      .setRegisteredAt(dr1RegisteredAt).setUpdatedAt(dr1UpdatedAt)
                                                      .setRegisteredShapeTree(dr1RegisteredShapeTree).build();
        assertDoesNotThrow(() -> registration.update());
    }
    
    @Test
    @DisplayName("Get crud data registration")
    void readDataRegistration() throws SaiException, SaiHttpNotFoundException {
        URL url = toUrl(server, "/ttl/data/dr-1/");
        DataRegistration registration = DataRegistration.get(url, saiSession);
        checkRegistration(registration);
    }

    @Test
    @DisplayName("Reload crud data registration")
    void reloadDataRegistration() throws SaiException, SaiHttpNotFoundException {
        URL url = toUrl(server, "/ttl/data/dr-1/");
        DataRegistration registration = DataRegistration.get(url, saiSession);
        DataRegistration reloaded = registration.reload();
        checkRegistration(reloaded);
    }

    @Test
    @DisplayName("Get readable data registration")
    void getReadableDataRegistration() throws SaiException, SaiHttpNotFoundException {
        URL url = toUrl(server, "/ttl/data/dr-1/");
        ReadableDataRegistration readable = ReadableDataRegistration.get(url, saiSession);
        checkReadableRegistration(readable);
    }

    @Test
    @DisplayName("Reload readable data registration")
    void reloadReadableDataRegistration() throws SaiException, SaiHttpNotFoundException {
        URL url = toUrl(server, "/ttl/data/dr-1/");
        ReadableDataRegistration readable = ReadableDataRegistration.get(url, saiSession);
        ReadableDataRegistration reloaded = readable.reload();
        checkReadableRegistration(reloaded);
    }

    @Test
    @DisplayName("Fail to get existing crud data registration in turtle - missing required fields")
    void failToReadDataRegistration() {
        URL url = toUrl(server, "/missing-fields/ttl/data/dr-1/");
        assertThrows(SaiException.class, () -> DataRegistration.get(url, saiSession));
    }

    @Test
    @DisplayName("Fail to get readable data registration in turtle - missing required fields")
    void failToGetReadableDataRegistrationRequired() {
        URL url = toUrl(server, "/missing-fields/ttl/data/dr-1/");
        assertThrows(SaiException.class, () -> ReadableDataRegistration.get(url, saiSession));
    }

    @Test
    @DisplayName("Update crud data registration")
    void updateDataRegistration() throws SaiException, SaiHttpNotFoundException {
        URL url = toUrl(server, "/ttl/data/dr-1/");
        DataRegistration registration = DataRegistration.get(url, saiSession);
        registration.setRegisteredShapeTree(toUrl(server, "/shapetrees/pm#OtherTree"));
        assertDoesNotThrow(() -> registration.update());
    }

    @Test
    @DisplayName("Read existing data registration in JSON-LD")
    void readDataRegistrationJsonLd() throws SaiException, SaiHttpNotFoundException {
        URL url = toUrl(server, "/jsonld/data/dr-1/");
        DataRegistration registration = DataRegistration.get(url, saiSession, LD_JSON);
        checkRegistration(registration);
    }

    @Test
    @DisplayName("Create new crud data registration in JSON-LD")
    void createNewCrudDataRegistrationJsonLd() throws SaiException {
        URL url = toUrl(server, "/new/jsonld/data/dr-1/");
        DataRegistration.Builder builder = new DataRegistration.Builder(url, saiSession);
        DataRegistration registration = builder.setContentType(LD_JSON).setRegisteredBy(dr1RegisteredBy)
                                                      .setRegisteredWith(dr1RegisteredWith)
                                                      .setRegisteredAt(dr1RegisteredAt).setUpdatedAt(dr1UpdatedAt)
                                                      .setRegisteredShapeTree(dr1RegisteredShapeTree).build();
        assertDoesNotThrow(() -> registration.update());
    }

    @Test
    @DisplayName("Delete crud data registration")
    void deleteDataRegistration() throws SaiException, SaiHttpNotFoundException {
        URL url = toUrl(server, "/ttl/data/dr-1/");
        DataRegistration registration = DataRegistration.get(url, saiSession);
        assertDoesNotThrow(() -> registration.delete());
        assertFalse(registration.isExists());
    }

    private void checkRegistration(DataRegistration registration) {
        assertNotNull(registration);
        assertEquals(dr1RegisteredBy, registration.getRegisteredBy());
        assertEquals(dr1RegisteredWith, registration.getRegisteredWith());
        assertEquals(dr1RegisteredAt, registration.getRegisteredAt());
        assertEquals(dr1UpdatedAt, registration.getUpdatedAt());
        assertEquals(dr1RegisteredShapeTree, registration.getRegisteredShapeTree());
    }

    private void checkReadableRegistration(ReadableDataRegistration registration) {
        assertNotNull(registration);
        assertEquals(dr1RegisteredBy, registration.getRegisteredBy());
        assertEquals(dr1RegisteredWith, registration.getRegisteredWith());
        assertEquals(dr1RegisteredAt, registration.getRegisteredAt());
        assertEquals(dr1UpdatedAt, registration.getUpdatedAt());
        assertEquals(dr1RegisteredShapeTree, registration.getRegisteredShapeTree());
    }

}

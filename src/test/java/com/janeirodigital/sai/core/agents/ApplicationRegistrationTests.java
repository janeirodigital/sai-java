package com.janeirodigital.sai.core.agents;

import com.janeirodigital.mockwebserver.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpException;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static com.janeirodigital.mockwebserver.DispatcherHelper.*;
import static com.janeirodigital.mockwebserver.MockWebServerHelper.toMockUri;
import static com.janeirodigital.sai.httputils.ContentType.LD_JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ApplicationRegistrationTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static URI app1RegisteredBy;
    private static URI app1RegisteredWith;
    private static OffsetDateTime app1RegisteredAt;
    private static OffsetDateTime app1UpdatedAt;
    private static URI app1RegisteredAgent;
    private static URI app1AccessGrant;

    @BeforeAll
    static void beforeAll() throws SaiException, SaiHttpException {

        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        // GET application registration in Turtle
        mockOnGet(dispatcher, "/ttl/agents/app-1/", "agents/application-registration-ttl");
        mockOnPut(dispatcher, "/new/ttl/agents/app-1/", "http/201");  // create new
        mockOnPut(dispatcher, "/ttl/agents/app-1/", "http/204");  // update existing
        mockOnDelete(dispatcher, "/ttl/agents/app-1/", "http/204");  // delete
        // GET crud application registration in Turtle with missing fields
        mockOnGet(dispatcher, "/missing-fields/ttl/agents/app-1/", "agents/application-registration-missing-fields-ttl");
        // GET crud application registration in JSON-LD
        mockOnGet(dispatcher, "/jsonld/agents/app-1/", "agents/application-registration-jsonld");
        mockOnPut(dispatcher, "/new/jsonld/agents/app-1/", "http/201");  // create new
        mockOnPut(dispatcher, "/jsonld/agents/app-1/", "http/204");  // update existing or delete
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        app1RegisteredBy = URI.create("https://alice.example/id#me");
        app1RegisteredWith = toMockUri(server, "https://trusted.example/id#app");
        app1RegisteredAt = OffsetDateTime.parse("2021-04-04T20:15:47.000Z", DateTimeFormatter.ISO_DATE_TIME);
        app1UpdatedAt = OffsetDateTime.parse("2021-04-04T20:15:47.000Z", DateTimeFormatter.ISO_DATE_TIME);
        app1RegisteredAgent = URI.create("https://projectron.example/id#app");
        app1AccessGrant = toMockUri(server, "/ttl/agents/app-1/access-grant");
    }

    @Test
    @DisplayName("Create new crud application registration")
    void createNewCrudApplicationRegistration() throws SaiException {
        URI url = toMockUri(server, "/new/ttl/agents/app-1/");
        ApplicationRegistration.Builder builder = new ApplicationRegistration.Builder(url, saiSession);
        ApplicationRegistration registration = builder.setRegisteredBy(app1RegisteredBy).setRegisteredWith(app1RegisteredWith)
                                                      .setRegisteredAt(app1RegisteredAt).setUpdatedAt(app1UpdatedAt)
                                                      .setRegisteredAgent(app1RegisteredAgent)
                                                      .setAccessGrant(app1AccessGrant).build();
        assertDoesNotThrow(() -> registration.update());
        assertTrue(registration.hasAccessGrant());
    }

    @Test
    @DisplayName("Create new crud application registration - only required fields")
    void createNewCrudApplicationRegistrationRequired() throws SaiException {
        URI url = toMockUri(server, "/new/ttl/agents/app-1/");
        ApplicationRegistration.Builder builder = new ApplicationRegistration.Builder(url, saiSession);
        ApplicationRegistration registration = builder.setRegisteredBy(app1RegisteredBy).setRegisteredWith(app1RegisteredWith)
                .setRegisteredAgent(app1RegisteredAgent).build();
        assertDoesNotThrow(() -> registration.update());
        assertFalse(registration.hasAccessGrant());
    }

    @Test
    @DisplayName("Read crud application registration")
    void readApplicationRegistration() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/ttl/agents/app-1/");
        ApplicationRegistration registration = ApplicationRegistration.get(url, saiSession);
        checkRegistration(registration);
    }

    @Test
    @DisplayName("Reload crud application registration")
    void reloadApplicationRegistration() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/ttl/agents/app-1/");
        ApplicationRegistration registration = ApplicationRegistration.get(url, saiSession);
        ApplicationRegistration reloaded = registration.reload();
        checkRegistration(reloaded);
    }

    @Test
    @DisplayName("Fail to read existing crud application registration in turtle - missing required fields")
    void failToReadApplicationRegistration() {
        URI url = toMockUri(server, "/missing-fields/ttl/agents/app-1/");
        assertThrows(SaiException.class, () -> ApplicationRegistration.get(url, saiSession));
    }

    @Test
    @DisplayName("Update crud application registration")
    void updateApplicationRegistration() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/ttl/agents/app-1/");
        ApplicationRegistration registration = ApplicationRegistration.get(url, saiSession);
        registration.setAccessGrantUri(toMockUri(server, "/ttl/agents/app-1/access-granted"));
        assertDoesNotThrow(() -> registration.update());
    }

    @Test
    @DisplayName("Read existing application registration in JSON-LD")
    void readApplicationRegistrationJsonLd() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/jsonld/agents/app-1/");
        ApplicationRegistration registration = ApplicationRegistration.get(url, saiSession, LD_JSON);
        checkRegistration(registration);
    }

    @Test
    @DisplayName("Create new crud application registration in JSON-LD")
    void createNewCrudApplicationRegistrationJsonLd() throws SaiException {
        URI url = toMockUri(server, "/new/jsonld/agents/app-1/");
        ApplicationRegistration.Builder builder = new ApplicationRegistration.Builder(url, saiSession);
        ApplicationRegistration registration = builder.setContentType(LD_JSON).setRegisteredBy(app1RegisteredBy)
                                                      .setRegisteredWith(app1RegisteredWith)
                                                      .setRegisteredAt(app1RegisteredAt).setUpdatedAt(app1UpdatedAt)
                                                      .setRegisteredAgent(app1RegisteredAgent)
                                                      .setAccessGrant(app1AccessGrant).build();
        assertDoesNotThrow(() -> registration.update());
    }

    @Test
    @DisplayName("Delete crud application registration")
    void deleteApplicationRegistration() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/ttl/agents/app-1/");
        ApplicationRegistration registration = ApplicationRegistration.get(url, saiSession);
        assertDoesNotThrow(() -> registration.delete());
        assertFalse(registration.isExists());
    }

    @Test
    @DisplayName("Generate URI for contained resource")
    void generateUriForContained() throws SaiHttpNotFoundException, SaiException {
        URI url = toMockUri(server, "/ttl/agents/app-1/");
        ApplicationRegistration registration = ApplicationRegistration.get(url, saiSession);
        assertDoesNotThrow(() -> registration.generateContainedUri());
    }

    private void checkRegistration(ApplicationRegistration registration) {
        assertNotNull(registration);
        assertEquals(app1RegisteredBy, registration.getRegisteredBy());
        assertEquals(app1RegisteredWith, registration.getRegisteredWith());
        assertEquals(app1RegisteredAt, registration.getRegisteredAt());
        assertEquals(app1UpdatedAt, registration.getUpdatedAt());
        assertEquals(app1RegisteredAgent, registration.getRegisteredAgent());
        assertEquals(app1AccessGrant, registration.getAccessGrantUri());
    }

}

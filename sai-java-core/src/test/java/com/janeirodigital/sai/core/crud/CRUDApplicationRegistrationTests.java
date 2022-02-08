package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.factories.TrustedDataFactory;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static com.janeirodigital.sai.core.enums.ContentType.LD_JSON;
import static com.janeirodigital.sai.core.enums.ContentType.TEXT_TURTLE;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.*;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static com.janeirodigital.sai.core.helpers.HttpHelper.stringToUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CRUDApplicationRegistrationTests {

    private static TrustedDataFactory trustedDataFactory;
    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;

    private static URL app1RegisteredBy;
    private static URL app1RegisteredWith;
    private static OffsetDateTime app1RegisteredAt;
    private static OffsetDateTime app1UpdatedAt;
    private static URL app1RegisteredAgent;
    private static URL app1AccessGrant;

    @BeforeAll
    static void beforeAll() throws SaiException {

        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        trustedDataFactory = new TrustedDataFactory(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
        // GET application registration in Turtle
        mockOnGet(dispatcher, "/ttl/agents/app-1/", "crud/application-registration-ttl");
        mockOnPut(dispatcher, "/new/ttl/agents/app-1/", "http/201");  // create new
        mockOnPut(dispatcher, "/ttl/agents/app-1/", "http/204");  // update existing
        mockOnDelete(dispatcher, "/ttl/agents/app-1/", "http/204");  // delete
        // GET crud application registration in Turtle with missing fields
        mockOnGet(dispatcher, "/missing-fields/ttl/agents/app-1/", "crud/application-registration-missing-fields-ttl");
        // GET crud application registration in JSON-LD
        mockOnGet(dispatcher, "/jsonld/agents/app-1/", "crud/application-registration-jsonld");
        mockOnPut(dispatcher, "/new/jsonld/agents/app-1/", "http/201");  // create new
        mockOnPut(dispatcher, "/jsonld/agents/app-1/", "http/204");  // update existing or delete
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        app1RegisteredBy = stringToUrl("https://alice.example/id#me");
        app1RegisteredWith = toUrl(server, "https://trusted.example/id#app");
        app1RegisteredAt = OffsetDateTime.parse("2021-04-04T20:15:47.000Z", DateTimeFormatter.ISO_DATE_TIME);
        app1UpdatedAt = OffsetDateTime.parse("2021-04-04T20:15:47.000Z", DateTimeFormatter.ISO_DATE_TIME);
        app1RegisteredAgent = stringToUrl("https://projectron.example/id#app");
        app1AccessGrant = toUrl(server, "/ttl/agents/app-1/access-grant");
    }

    @Test
    @DisplayName("Create new crud application registration in turtle")
    void createNewCrudApplicationRegistration() throws SaiException {
        URL url = toUrl(server, "/new/ttl/agents/app-1/");
        CRUDApplicationRegistration registration = trustedDataFactory.getCRUDApplicationRegistration(url);
        registration.setRegisteredBy(app1RegisteredBy);
        registration.setRegisteredWith(app1RegisteredWith);
        registration.setRegisteredAt(app1RegisteredAt);
        registration.setUpdatedAt(app1UpdatedAt);
        registration.setRegisteredAgent(app1RegisteredAgent);
        registration.setAccessGrant(app1AccessGrant);
        assertDoesNotThrow(() -> registration.update());
        assertNotNull(registration);
    }

    @Test
    @DisplayName("Create new crud application registration in turtle with jena resource")
    void createCrudApplicationRegistrationWithJenaResource() throws SaiException {
        URL existingUrl = toUrl(server, "/ttl/agents/app-1/");
        CRUDApplicationRegistration existingRegistration = trustedDataFactory.getCRUDApplicationRegistration(existingUrl);

        URL newUrl = toUrl(server, "/new/ttl/agents/app-1/");
        CRUDApplicationRegistration resourceRegistration = trustedDataFactory.getCRUDApplicationRegistration(newUrl, TEXT_TURTLE, existingRegistration.getResource());
        assertDoesNotThrow(() -> resourceRegistration.update());
        assertNotNull(resourceRegistration);
    }

    @Test
    @DisplayName("Read existing crud application registration in turtle")
    void readApplicationRegistration() throws SaiException {
        URL url = toUrl(server, "/ttl/agents/app-1/");
        CRUDApplicationRegistration registration = trustedDataFactory.getCRUDApplicationRegistration(url);
        assertNotNull(registration);
        assertEquals(app1RegisteredBy, registration.getRegisteredBy());
        assertEquals(app1RegisteredWith, registration.getRegisteredWith());
        assertEquals(app1RegisteredAt, registration.getRegisteredAt());
        assertEquals(app1UpdatedAt, registration.getUpdatedAt());
        assertEquals(app1RegisteredAgent, registration.getRegisteredAgent());
        assertEquals(app1AccessGrant, registration.getAccessGrantUrl());
    }

    @Test
    @DisplayName("Fail to read existing crud application registration in turtle - missing required fields")
    void failToReadApplicationRegistration() throws SaiException {
        URL url = toUrl(server, "/missing-fields/ttl/agents/app-1/");
        assertThrows(SaiException.class, () -> CRUDApplicationRegistration.build(url, trustedDataFactory));
    }

    @Test
    @DisplayName("Update existing crud application registration in turtle")
    void updateApplicationRegistration() throws SaiException {
        URL url = toUrl(server, "/ttl/agents/app-1/");
        CRUDApplicationRegistration registration = trustedDataFactory.getCRUDApplicationRegistration(url);
        registration.setAccessGrant(toUrl(server, "/ttl/agents/app-1/access-granted"));
        assertDoesNotThrow(() -> registration.update());
    }

    @Test
    @DisplayName("Read existing application registration in JSON-LD")
    void readApplicationRegistrationJsonLd() throws SaiException {
        URL url = toUrl(server, "/jsonld/agents/app-1/");
        CRUDApplicationRegistration registration = trustedDataFactory.getCRUDApplicationRegistration(url, LD_JSON);
        assertNotNull(registration);
        assertEquals(app1RegisteredBy, registration.getRegisteredBy());
        assertEquals(app1RegisteredWith, registration.getRegisteredWith());
        assertEquals(app1RegisteredAt, registration.getRegisteredAt());
        assertEquals(app1UpdatedAt, registration.getUpdatedAt());
        assertEquals(app1RegisteredAgent, registration.getRegisteredAgent());
        assertEquals(app1AccessGrant, registration.getAccessGrantUrl());
    }

    @Test
    @DisplayName("Create new crud application registration in JSON-LD")
    void createNewCrudApplicationRegistrationJsonLd() throws SaiException {
        URL url = toUrl(server, "/new/jsonld/agents/app-1/");
        CRUDApplicationRegistration registration = trustedDataFactory.getCRUDApplicationRegistration(url, LD_JSON);
        registration.setRegisteredBy(app1RegisteredBy);
        registration.setRegisteredWith(app1RegisteredWith);
        registration.setRegisteredAt(app1RegisteredAt);
        registration.setUpdatedAt(app1UpdatedAt);
        registration.setRegisteredAgent(app1RegisteredAgent);
        registration.setAccessGrant(app1AccessGrant);
        assertDoesNotThrow(() -> registration.update());
    }

    @Test
    @DisplayName("Delete crud application registration")
    void deleteApplicationRegistration() throws SaiException {
        URL url = toUrl(server, "/ttl/agents/app-1/");
        CRUDApplicationRegistration registration = trustedDataFactory.getCRUDApplicationRegistration(url);
        assertDoesNotThrow(() -> registration.delete());
        assertFalse(registration.isExists());
    }

}

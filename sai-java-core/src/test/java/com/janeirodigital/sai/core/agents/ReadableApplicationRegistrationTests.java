package com.janeirodigital.sai.core.agents;

import com.janeirodigital.mockwebserver.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.agents.ReadableApplicationRegistration;
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

class ReadableApplicationRegistrationTests {

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
        // GET social agent registration in Turtle
        mockOnGet(dispatcher, "/ttl/agents/app-1/", "agents/application-registration-ttl");
        mockOnPut(dispatcher, "/new/ttl/agents/app-1/", "http/201");  // create new
        mockOnPut(dispatcher, "/ttl/agents/app-1/", "http/204");  // update existing
        mockOnDelete(dispatcher, "/ttl/agents/app-1/", "http/204");  // delete
        // GET crud social agent registration in Turtle with missing fields
        mockOnGet(dispatcher, "/missing-fields/ttl/agents/app-1/", "agents/social-agent-registration-missing-fields-ttl");
        // GET crud social agent registration in JSON-LD
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
    @DisplayName("Get readable social agent registration")
    void readSocialAgentRegistration() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/ttl/agents/app-1/");
        ReadableApplicationRegistration registration = ReadableApplicationRegistration.get(url, saiSession);
        checkRegistration(registration);
    }

    @Test
    @DisplayName("Reload readable social agent registration")
    void reloadSocialAgentRegistration() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/ttl/agents/app-1/");
        ReadableApplicationRegistration registration = ReadableApplicationRegistration.get(url, saiSession);
        ReadableApplicationRegistration reloaded = registration.reload();
        assertNotEquals(registration, reloaded);
        checkRegistration(registration);
        checkRegistration(reloaded);
    }

    @Test
    @DisplayName("Fail to get readable social agent registration - missing required fields")
    void failToReadSocialAgentRegistration() {
        URI url = toMockUri(server, "/missing-fields/ttl/agents/app-1/");
        assertThrows(SaiException.class, () -> ReadableApplicationRegistration.get(url, saiSession));
    }

    @Test
    @DisplayName("Read existing social agent registration in JSON-LD")
    void readSocialAgentRegistrationJsonLd() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/jsonld/agents/app-1/");
        ReadableApplicationRegistration registration = ReadableApplicationRegistration.get(url, saiSession, LD_JSON);
        checkRegistration(registration);
    }

    private void checkRegistration(ReadableApplicationRegistration registration) {
        assertNotNull(registration);
        assertEquals(app1RegisteredBy, registration.getRegisteredBy());
        assertEquals(app1RegisteredWith, registration.getRegisteredWith());
        assertEquals(app1RegisteredAt, registration.getRegisteredAt());
        assertEquals(app1UpdatedAt, registration.getUpdatedAt());
        assertEquals(app1RegisteredAgent, registration.getRegisteredAgent());
        assertEquals(app1AccessGrant, registration.getAccessGrantUri());
    }

}

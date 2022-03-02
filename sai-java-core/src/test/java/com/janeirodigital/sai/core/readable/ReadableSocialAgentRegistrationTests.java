package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static com.janeirodigital.sai.core.enums.ContentType.LD_JSON;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.*;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static com.janeirodigital.sai.core.helpers.HttpHelper.stringToUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ReadableSocialAgentRegistrationTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static URL sa1RegisteredBy;
    private static URL sa1RegisteredWith;
    private static OffsetDateTime sa1RegisteredAt;
    private static OffsetDateTime sa1UpdatedAt;
    private static URL sa1ReciprocalRegistration;
    private static URL sa1RegisteredAgent;
    private static URL sa1AccessGrant;

    @BeforeAll
    static void beforeAll() throws SaiException {

        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        // GET social agent registration in Turtle
        mockOnGet(dispatcher, "/ttl/agents/sa-1/", "crud/social-agent-registration-ttl");
        mockOnPut(dispatcher, "/new/ttl/agents/sa-1/", "http/201");  // create new
        mockOnPut(dispatcher, "/ttl/agents/sa-1/", "http/204");  // update existing
        mockOnDelete(dispatcher, "/ttl/agents/sa-1/", "http/204");  // delete
        // GET crud social agent registration in Turtle with missing fields
        mockOnGet(dispatcher, "/missing-fields/ttl/agents/sa-1/", "crud/social-agent-registration-missing-fields-ttl");
        // GET crud social agent registration in JSON-LD
        mockOnGet(dispatcher, "/jsonld/agents/sa-1/", "crud/social-agent-registration-jsonld");
        mockOnPut(dispatcher, "/new/jsonld/agents/sa-1/", "http/201");  // create new
        mockOnPut(dispatcher, "/jsonld/agents/sa-1/", "http/204");  // update existing or delete
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        sa1RegisteredBy = stringToUrl("https://alice.example/id#me");
        sa1RegisteredWith = toUrl(server, "https://trusted.example/id#app");
        sa1RegisteredAt = OffsetDateTime.parse("2021-04-04T20:15:47.000Z", DateTimeFormatter.ISO_DATE_TIME);
        sa1UpdatedAt = OffsetDateTime.parse("2021-04-04T20:15:47.000Z", DateTimeFormatter.ISO_DATE_TIME);
        sa1RegisteredAgent = stringToUrl("https://bob.example/id#me");
        sa1ReciprocalRegistration = stringToUrl("https://bob.example/agents/sa-7/");
        sa1AccessGrant = toUrl(server, "/ttl/agents/sa-1/access-grant");
    }

    @Test
    @DisplayName("Get readable social agent registration")
    void readSocialAgentRegistration() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/agents/sa-1/");
        ReadableSocialAgentRegistration registration = ReadableSocialAgentRegistration.get(url, saiSession);
        checkRegistration(registration);
    }

    @Test
    @DisplayName("Reload readable social agent registration")
    void reloadSocialAgentRegistration() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/ttl/agents/sa-1/");
        ReadableSocialAgentRegistration registration = ReadableSocialAgentRegistration.get(url, saiSession);
        ReadableSocialAgentRegistration reloaded = registration.reload();
        assertNotEquals(registration, reloaded);
        checkRegistration(registration);
        checkRegistration(reloaded);
    }

    @Test
    @DisplayName("Fail to get readable social agent registration - missing required fields")
    void failToReadSocialAgentRegistration() {
        URL url = toUrl(server, "/missing-fields/ttl/agents/sa-1/");
        assertThrows(SaiException.class, () -> ReadableSocialAgentRegistration.get(url, saiSession));
    }

    @Test
    @DisplayName("Read existing social agent registration in JSON-LD")
    void readSocialAgentRegistrationJsonLd() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/jsonld/agents/sa-1/");
        ReadableSocialAgentRegistration registration = ReadableSocialAgentRegistration.get(url, saiSession, LD_JSON);
        checkRegistration(registration);
    }

    private void checkRegistration(ReadableSocialAgentRegistration registration) {
        assertNotNull(registration);
        assertEquals(sa1RegisteredBy, registration.getRegisteredBy());
        assertEquals(sa1RegisteredWith, registration.getRegisteredWith());
        assertEquals(sa1RegisteredAt, registration.getRegisteredAt());
        assertEquals(sa1UpdatedAt, registration.getUpdatedAt());
        assertEquals(sa1RegisteredAgent, registration.getRegisteredAgent());
        assertEquals(sa1ReciprocalRegistration, registration.getReciprocalRegistration());
        assertEquals(sa1AccessGrant, registration.getAccessGrantUrl());
    }

}

package com.janeirodigital.sai.core.agents;

import com.janeirodigital.mockwebserver.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.HttpUtils;
import com.janeirodigital.sai.httputils.SaiHttpException;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static com.janeirodigital.mockwebserver.DispatcherHelper.*;
import static com.janeirodigital.mockwebserver.MockWebServerHelper.toMockUri;
import static com.janeirodigital.sai.httputils.ContentType.LD_JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

class SocialAgentRegistrationTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static URI sa1RegisteredBy;
    private static URI sa1RegisteredWith;
    private static OffsetDateTime sa1RegisteredAt;
    private static OffsetDateTime sa1UpdatedAt;
    private static URI sa1ReciprocalRegistration;
    private static URI sa1RegisteredAgent;
    private static URI sa1AccessGrant;

    @BeforeAll
    static void beforeAll() throws SaiException, SaiHttpException {

        // Initialize a mock sai session we can use for protected requests
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        // GET social agent registration in Turtle
        mockOnGet(dispatcher, "/ttl/agents/sa-1/", "agents/social-agent-registration-ttl");
        mockOnGet(dispatcher, "/ttl/required/agents/sa-1/", "agents/social-agent-registration-required-ttl");
        mockOnPut(dispatcher, "/new/ttl/agents/sa-1/", "http/201");  // create new
        mockOnPut(dispatcher, "/ttl/agents/sa-1/", "http/204");  // update existing
        mockOnDelete(dispatcher, "/ttl/agents/sa-1/", "http/204");  // delete
        // GET crud social agent registration in Turtle with missing fields
        mockOnGet(dispatcher, "/missing-fields/ttl/agents/sa-1/", "agents/social-agent-registration-missing-fields-ttl");
        // GET crud social agent registration in Turtle with invalid fields
        mockOnGet(dispatcher, "/invalid-fields/ttl/agents/sa-1/", "agents/social-agent-registration-invalid-fields-ttl");
        // GET crud social agent registration in JSON-LD
        mockOnGet(dispatcher, "/jsonld/agents/sa-1/", "agents/social-agent-registration-jsonld");
        mockOnPut(dispatcher, "/new/jsonld/agents/sa-1/", "http/201");  // create new
        mockOnPut(dispatcher, "/jsonld/agents/sa-1/", "http/204");  // update existing or delete
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        sa1RegisteredBy = URI.create("https://alice.example/id#me");
        sa1RegisteredWith = toMockUri(server, "https://trusted.example/id#app");
        sa1RegisteredAt = OffsetDateTime.parse("2021-04-04T20:15:47.000Z", DateTimeFormatter.ISO_DATE_TIME);
        sa1UpdatedAt = OffsetDateTime.parse("2021-04-04T20:15:47.000Z", DateTimeFormatter.ISO_DATE_TIME);
        sa1RegisteredAgent = URI.create("https://bob.example/id#me");
        sa1ReciprocalRegistration = URI.create("https://bob.example/agents/sa-7/");
        sa1AccessGrant = toMockUri(server, "/ttl/agents/sa-1/access-grant");
    }

    @Test
    @DisplayName("Create new crud social agent registration")
    void createNewCrudSocialAgentRegistration() throws SaiException {
        URI url = toMockUri(server, "/new/ttl/agents/sa-1/");
        SocialAgentRegistration.Builder builder = new SocialAgentRegistration.Builder(url, saiSession);
        SocialAgentRegistration registration = builder.setRegisteredBy(sa1RegisteredBy).setRegisteredWith(sa1RegisteredWith)
                .setRegisteredAt(sa1RegisteredAt).setUpdatedAt(sa1UpdatedAt)
                .setRegisteredAgent(sa1RegisteredAgent)
                .setAccessGrant(sa1AccessGrant).setReciprocalRegistration(sa1ReciprocalRegistration).build();
        assertDoesNotThrow(() -> registration.update());
        assertTrue(registration.hasAccessGrant());
    }

    @Test
    @DisplayName("Create new crud social agent registration - only required fields")
    void createNewCrudSocialAgentRegistrationRequired() throws SaiException {
        URI url = toMockUri(server, "/new/ttl/agents/sa-1/");
        SocialAgentRegistration.Builder builder = new SocialAgentRegistration.Builder(url, saiSession);
        SocialAgentRegistration registration = builder.setRegisteredBy(sa1RegisteredBy).setRegisteredWith(sa1RegisteredWith)
                .setRegisteredAgent(sa1RegisteredAgent).build();
        assertDoesNotThrow(() -> registration.update());
        assertFalse(registration.hasAccessGrant());
    }
    
    @Test
    @DisplayName("Read crud social agent registration")
    void readSocialAgentRegistration() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/ttl/agents/sa-1/");
        SocialAgentRegistration registration = SocialAgentRegistration.get(url, saiSession);
        checkRegistration(registration, false);
    }

    @Test
    @DisplayName("Read crud social agent registration - only required fields")
    void readSocialAgentRegistrationRequired() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/ttl/required/agents/sa-1/");
        SocialAgentRegistration registration = SocialAgentRegistration.get(url, saiSession);
        checkRegistration(registration, true);
    }

    @Test
    @DisplayName("Reload crud social agent registration")
    void reloadSocialAgentRegistration() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/ttl/agents/sa-1/");
        SocialAgentRegistration registration = SocialAgentRegistration.get(url, saiSession);
        SocialAgentRegistration reloaded = registration.reload();
        checkRegistration(reloaded, false);
    }

    @Test
    @DisplayName("Fail to read existing crud social agent registration - missing required fields")
    void failToReadSocialAgentRegistration() {
        URI url = toMockUri(server, "/missing-fields/ttl/agents/sa-1/");
        assertThrows(SaiException.class, () -> SocialAgentRegistration.get(url, saiSession));
    }

    @Test
    @DisplayName("Fail to read existing crud social agent registration - invalid fields")
    void failToReadSocialAgentRegistrationBadReciprocal() {
        URI url = toMockUri(server, "/invalid-fields/ttl/agents/sa-1/");
        assertThrows(SaiException.class, () -> SocialAgentRegistration.get(url, saiSession));
    }

    @Test
    @DisplayName("Update existing crud social agent registration")
    void updateSocialAgentRegistration() throws SaiException, SaiHttpNotFoundException, SaiHttpException {
        URI url = toMockUri(server, "/ttl/agents/sa-1/");

        SocialAgentRegistration registration = SocialAgentRegistration.get(url, saiSession);
        registration.setReciprocalRegistration(URI.create("https://bob.example/agents/sa-222/"));
        assertDoesNotThrow(() -> registration.update());
    }

    @Test
    @DisplayName("Read existing social agent registration in JSON-LD")
    void readSocialAgentRegistrationJsonLd() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/jsonld/agents/sa-1/");
        SocialAgentRegistration registration = SocialAgentRegistration.get(url, saiSession, LD_JSON);
        checkRegistration(registration, false);
    }

    @Test
    @DisplayName("Create new crud social agent registration in JSON-LD")
    void createNewCrudSocialAgentRegistrationJsonLd() throws SaiException {
        URI url = toMockUri(server, "/new/jsonld/agents/sa-1/");
        SocialAgentRegistration.Builder builder = new SocialAgentRegistration.Builder(url, saiSession);
        SocialAgentRegistration registration = builder.setContentType(LD_JSON).setRegisteredBy(sa1RegisteredBy)
                                                      .setRegisteredWith(sa1RegisteredWith)
                                                      .setRegisteredAt(sa1RegisteredAt).setUpdatedAt(sa1UpdatedAt)
                                                      .setRegisteredAgent(sa1RegisteredAgent)
                                                      .setAccessGrant(sa1AccessGrant)
                                                      .setReciprocalRegistration(sa1ReciprocalRegistration).build();
        assertDoesNotThrow(() -> registration.update());
    }

    @Test
    @DisplayName("Delete crud social agent registration")
    void deleteSocialAgentRegistration() throws SaiException, SaiHttpNotFoundException {
        URI url = toMockUri(server, "/ttl/agents/sa-1/");
        SocialAgentRegistration registration = SocialAgentRegistration.get(url, saiSession);
        assertDoesNotThrow(() -> registration.delete());
        assertFalse(registration.isExists());
    }

    @Test
    @DisplayName("Generate URI for contained resource")
    void generateUriForContained() throws SaiHttpNotFoundException, SaiException {
        URI url = toMockUri(server, "/ttl/agents/sa-1/");
        SocialAgentRegistration registration = SocialAgentRegistration.get(url, saiSession);
        assertDoesNotThrow(() -> registration.generateContainedUri());
    }

    @Test
    @DisplayName("Fail to generate URI for contained resource - Invalid path")
    void failToGenerateUriForContained() throws SaiHttpNotFoundException, SaiException {
        URI url = toMockUri(server, "/ttl/agents/sa-1/");
        SocialAgentRegistration registration = SocialAgentRegistration.get(url, saiSession);
        try (MockedStatic<HttpUtils> mockedStaticUtils = Mockito.mockStatic(HttpUtils.class)) {
            mockedStaticUtils.when(() -> HttpUtils.addChildToUriPath(any(URI.class), anyString())).thenThrow(SaiHttpException.class);
            assertThrows(SaiException.class, () -> registration.generateContainedUri());
        }
    }

    private void checkRegistration(SocialAgentRegistration registration, boolean required) {
        assertNotNull(registration);
        assertEquals(sa1RegisteredBy, registration.getRegisteredBy());
        assertEquals(sa1RegisteredWith, registration.getRegisteredWith());
        assertEquals(sa1RegisteredAt, registration.getRegisteredAt());
        assertEquals(sa1UpdatedAt, registration.getUpdatedAt());
        assertEquals(sa1RegisteredAgent, registration.getRegisteredAgent());
        if (!required) {
            assertEquals(sa1ReciprocalRegistration, registration.getReciprocalRegistration());
            assertTrue(registration.hasAccessGrant());
            assertEquals(sa1AccessGrant, registration.getAccessGrantUri());
        }
    }

}

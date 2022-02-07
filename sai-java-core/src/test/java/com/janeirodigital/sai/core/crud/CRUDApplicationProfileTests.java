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

import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.*;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static com.janeirodigital.sai.core.helpers.HttpHelper.stringToUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CRUDApplicationProfileTests {

    private static TrustedDataFactory trustedDataFactory;
    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;

    @BeforeAll
    static void beforeAll() throws SaiException {

        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        trustedDataFactory = new TrustedDataFactory(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
        mockOnGet(dispatcher, "/contexts/interop", "crud/interop-context-jsonld");
        // Get, Update, Delete for an existing CRUD resource
        mockOnGet(dispatcher, "/crud/application", "crud/application-profile-jsonld");
        mockOnPut(dispatcher, "/crud/application", "http/204");
        mockOnDelete(dispatcher, "/crud/application", "http/204");
        // Build a CRUD resource that doesn't exist and create it
        mockOnGet(dispatcher, "/new/crud/application", "http/404");
        mockOnPut(dispatcher, "/new/crud/application", "http/201");
        // Get and application profile that is missing required fields
        mockOnGet(dispatcher, "/missing-fields/crud/application", "crud/application-profile-missing-fields-jsonld");
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

    }

    @Test
    @DisplayName("Create new crud application profile")
    void createNewCrudApplicationProfile() throws SaiException {
        URL url = toUrl(server, "/new/crud/application");
        CRUDApplicationProfile profile = trustedDataFactory.getCRUDApplicationProfile(url);
        profile.setName("Projectron");
        profile.setLogoUrl(stringToUrl("https://logo.example/logo.png"));
        profile.setDescription("What a great application");
        profile.setAuthorUrl(stringToUrl("https://justin.bingham.id"));
        profile.addAccessNeedGroupUrl(stringToUrl("https://projectron.example/needs#group1"));
        profile.addAccessNeedGroupUrl(stringToUrl("https://projectron.example/needs#group2"));
        // Solid-OIDC Specific
        profile.setClientUrl(stringToUrl("https://projectron.example"));
        profile.addRedirectUrl(toUrl(server, "/redirect"));
        profile.setTosUrl(stringToUrl("https://projectron.example/tos"));
        profile.addScope("openid");
        profile.addScope("offline_access");
        profile.addScope("profile");
        profile.addGrantType("refresh_token");
        profile.addGrantType("authorization_code");
        profile.addResponseType("code");
        profile.setDefaultMaxAge(3600);
        profile.setRequireAuthTime(true);
        assertDoesNotThrow(() -> profile.update());
        assertNotNull(profile);
    }

    @Test
    @DisplayName("Create new crud application profile with jena resource")
    void createCrudApplicationProfileWithResource() throws SaiException {
        URL url = toUrl(server, "/crud/application");
        CRUDApplicationProfile existingProfile = trustedDataFactory.getCRUDApplicationProfile(url);

        CRUDApplicationProfile resourceProfile = trustedDataFactory.getCRUDApplicationProfile(url, existingProfile.getResource());
        assertDoesNotThrow(() -> resourceProfile.update());
        assertEquals("Projectron", resourceProfile.getName());
    }

    @Test
    @DisplayName("Read existing crud application profile")
    void readCrudApplicationProfile() throws SaiException {
        URL url = toUrl(server, "/crud/application");
        CRUDApplicationProfile profile = trustedDataFactory.getCRUDApplicationProfile(url);
        assertEquals("Projectron", profile.getName());
        assertEquals(stringToUrl("http://projectron.example/logo.png"), profile.getLogoUrl());
        assertEquals("Best project management ever", profile.getDescription());
        assertEquals(stringToUrl("http://acme.example/id"), profile.getAuthorUrl());
        assertTrue(profile.getAccessNeedGroupUrls().contains(stringToUrl("http://localhost/projectron/access#group1")));
        assertTrue(profile.getAccessNeedGroupUrls().contains(stringToUrl("http://localhost/projectron/access#group2")));
        assertEquals(stringToUrl("http://projectron.example/"), profile.getClientUrl());
        assertTrue((profile.getRedirectUrls().contains(toUrl(server, "/redirect"))));
        assertEquals(stringToUrl("http://projectron.example/tos.html"), profile.getTosUrl());
        assertTrue(profile.getScopes().contains("openid"));
        assertTrue(profile.getScopes().contains("offline_access"));
        assertTrue(profile.getScopes().contains("profile"));
        assertTrue(profile.getGrantTypes().contains("refresh_token"));
        assertTrue(profile.getGrantTypes().contains("authorization_code"));
        assertTrue(profile.getResponseTypes().contains("code"));
        assertEquals(3600, profile.getDefaultMaxAge());
        assertEquals(true, profile.isRequireAuthTime());
    }

    @Test
    @DisplayName("Fail to read existing crud application profile - missing required fields")
    void failToReadCrudApplicationProfileMissingFields() {
        URL url = toUrl(server, "/missing-fields/crud/application");
        assertThrows(SaiException.class, () -> trustedDataFactory.getCRUDApplicationProfile(url));
    }

    @Test
    @DisplayName("Update existing crud application profile")
    void updateCrudApplicationProfile() throws SaiException {
        URL url = toUrl(server, "/crud/application");
        CRUDApplicationProfile profile = trustedDataFactory.getCRUDApplicationProfile(url);;
        assertEquals("Projectron", profile.getName());
        profile.setName("Projectimus Prime");
        assertDoesNotThrow(() -> profile.update());
        assertEquals("Projectimus Prime", profile.getName());
    }

    @Test
    @DisplayName("Delete existing crud application profile")
    void deleteCrudApplicationProfile() throws SaiException {
        URL url = toUrl(server, "/crud/application");
        CRUDApplicationProfile profile = trustedDataFactory.getCRUDApplicationProfile(url);
        assertDoesNotThrow(() -> profile.delete());
        assertFalse(profile.isExists());
    }

}

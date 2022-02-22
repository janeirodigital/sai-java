package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.TrustedDataFactory;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.*;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static com.janeirodigital.sai.core.helpers.HttpHelper.stringToUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ApplicationProfileTests {

    private static TrustedDataFactory trustedDataFactory;
    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;

    private static final String PROJECTRON_NAME = "Projectron";
    private static final String PROJECTRON_DESCRIPTION = "Project management done right";
    private static final List<String> PROJECTRON_SCOPES = Arrays.asList("openid", "offline_access", "profile");
    private static final List<String> PROJECTRON_GRANT_TYPES = Arrays.asList("refresh_token", "authorization_code");
    private static final List<String> PROJECTRON_RESPONSE_TYPES = Arrays.asList("code");
    private static URL PROJECTRON_LOGO;
    private static URL PROJECTRON_AUTHOR;
    private static URL PROJECTRON_CLIENT_URL;
    private static URL PROJECTRON_TOS;
    private static List<URL> PROJECTRON_NEED_GROUPS;
    private static List<URL> PROJECTRON_REDIRECTS;
    private static final int PROJECTRON_MAX_AGE = 3600;
    private static final boolean PROJECTRON_REQUIRE_AUTH_TIME = true;

    @BeforeAll
    static void beforeAll() throws SaiException, SaiNotFoundException {

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

        PROJECTRON_LOGO = stringToUrl("http://projectron.example/logo.png");
        PROJECTRON_AUTHOR = stringToUrl("http://acme.example/id");
        PROJECTRON_NEED_GROUPS = Arrays.asList(stringToUrl("http://localhost/projectron/access#group1"), stringToUrl("http://localhost/projectron/access#group2"));
        PROJECTRON_CLIENT_URL = stringToUrl("http://projectron.example/");
        PROJECTRON_REDIRECTS = Arrays.asList(toUrl(server, "/redirect"));
        PROJECTRON_TOS = stringToUrl("http://projectron.example/tos.html");
    }

    @Test
    @DisplayName("Create new crud application profile")
    void createNewCrudApplicationProfile() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/new/crud/application");

        ApplicationProfile.Builder builder = new ApplicationProfile.Builder(url, trustedDataFactory);
        ApplicationProfile profile = builder.setName(PROJECTRON_NAME).setDescription(PROJECTRON_DESCRIPTION)
                                            .setAuthorUrl(PROJECTRON_AUTHOR).setAccessNeedGroupUrls(PROJECTRON_NEED_GROUPS)
                                            .setClientUrl(PROJECTRON_CLIENT_URL).setRedirectUrls(PROJECTRON_REDIRECTS)
                                            .setTosUrl(PROJECTRON_TOS).setLogoUrl(PROJECTRON_LOGO).setScopes(PROJECTRON_SCOPES)
                                            .setGrantType(PROJECTRON_GRANT_TYPES).setResponseTypes(PROJECTRON_RESPONSE_TYPES)
                                            .setDefaultMaxAge(PROJECTRON_MAX_AGE).setRequireAuthTime(PROJECTRON_REQUIRE_AUTH_TIME)
                                            .build();
        assertDoesNotThrow(() -> profile.update());
        assertNotNull(profile);
    }

    @Test
    @DisplayName("Read existing crud application profile")
    void readCrudApplicationProfile() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/crud/application");
        ApplicationProfile profile = trustedDataFactory.getApplicationProfile(url);
        assertEquals(PROJECTRON_NAME, profile.getName());
        assertEquals(PROJECTRON_LOGO, profile.getLogoUrl());
        assertEquals(PROJECTRON_DESCRIPTION, profile.getDescription());
        assertEquals(PROJECTRON_AUTHOR, profile.getAuthorUrl());
        for (URL groupUrl : profile.getAccessNeedGroupUrls()) { assertTrue(PROJECTRON_NEED_GROUPS.contains(groupUrl)); }
        assertEquals(PROJECTRON_CLIENT_URL, profile.getClientUrl());
        for (URL redirectUrl : profile.getRedirectUrls()) { assertTrue(PROJECTRON_REDIRECTS.contains(redirectUrl)); }
        assertEquals(PROJECTRON_TOS, profile.getTosUrl());
        for (String scope : profile.getScopes()) { assertTrue(PROJECTRON_SCOPES.contains(scope)); }
        for (String grantType : profile.getGrantTypes()) { assertTrue(PROJECTRON_GRANT_TYPES.contains(grantType)); }
        for (String responseType : profile.getResponseTypes()) { assertTrue(PROJECTRON_RESPONSE_TYPES.contains(responseType)); }
        assertEquals(PROJECTRON_MAX_AGE, profile.getDefaultMaxAge());
        assertEquals(PROJECTRON_REQUIRE_AUTH_TIME, profile.isRequireAuthTime());
    }

    @Test
    @DisplayName("Fail to read existing crud application profile - missing required fields")
    void failToReadCrudApplicationProfileMissingFields() {
        URL url = toUrl(server, "/missing-fields/crud/application");
        assertThrows(SaiException.class, () -> trustedDataFactory.getApplicationProfile(url));
    }

    @Test
    @DisplayName("Update existing crud application profile")
    void updateCrudApplicationProfile() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/crud/application");
        ApplicationProfile profile = trustedDataFactory.getApplicationProfile(url);;
        ApplicationProfile.Builder builder = new ApplicationProfile.Builder(url, trustedDataFactory);
        ApplicationProfile updatedProfile = builder.setDataset(profile.getDataset()).setName("Projectimus Prime").build();
        assertDoesNotThrow(() -> updatedProfile.update());
        assertEquals("Projectimus Prime", updatedProfile.getName());
    }

    @Test
    @DisplayName("Delete existing crud application profile")
    void deleteCrudApplicationProfile() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/crud/application");
        ApplicationProfile profile = trustedDataFactory.getApplicationProfile(url);
        assertDoesNotThrow(() -> profile.delete());
        assertFalse(profile.isExists());
    }

}

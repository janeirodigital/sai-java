package com.janeirodigital.sai.core.crud;

import com.janeirodigital.mockwebserver.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpException;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.RdfUtils;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.mockwebserver.DispatcherHelper.*;
import static com.janeirodigital.mockwebserver.MockWebServerHelper.toMockUri;
import static com.janeirodigital.sai.core.contexts.InteropContext.INTEROP_CONTEXT;
import static com.janeirodigital.sai.core.contexts.SolidOidcContext.SOLID_OIDC_CONTEXT;
import static com.janeirodigital.sai.httputils.ContentType.LD_JSON;
import static com.janeirodigital.sai.rdfutils.RdfUtils.buildRemoteJsonLdContexts;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

class ApplicationProfileTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static final String PROJECTRON_NAME = "Projectron";
    private static final String PROJECTRON_DESCRIPTION = "Project management done right";
    private static final List<String> PROJECTRON_SCOPES = Arrays.asList("openid", "offline_access", "profile");
    private static final List<String> PROJECTRON_GRANT_TYPES = Arrays.asList("refresh_token", "authorization_code");
    private static final List<String> PROJECTRON_RESPONSE_TYPES = Arrays.asList("code");
    private static URI PROJECTRON_LOGO;
    private static URI PROJECTRON_AUTHOR;
    private static URI PROJECTRON_CLIENT_URI;
    private static URI PROJECTRON_TOS;
    private static String APPLICATION_CONTEXT;
    private static List<URI> PROJECTRON_NEED_GROUPS;
    private static List<URI> PROJECTRON_REDIRECTS;
    private static final int PROJECTRON_MAX_AGE = 3600;
    private static final boolean PROJECTRON_REQUIRE_AUTH_TIME = true;

    @BeforeAll
    static void beforeAll() throws SaiException, SaiHttpException, SaiRdfException {

        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        mockOnGet(dispatcher, "/contexts/interop", "crud/interop-context-jsonld");
        // Get, Update, Delete for an existing CRUD resource
        mockOnGet(dispatcher, "/crud/application", "crud/application-profile-jsonld");
        mockOnGet(dispatcher, "/crud/required/application", "crud/application-profile-required-jsonld");
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

        PROJECTRON_LOGO = URI.create("http://projectron.example/logo.png");
        PROJECTRON_AUTHOR = URI.create("http://acme.example/id");
        PROJECTRON_NEED_GROUPS = Arrays.asList(URI.create("http://localhost/projectron/access#group1"), URI.create("http://localhost/projectron/access#group2"));
        PROJECTRON_CLIENT_URI = URI.create("http://projectron.example/");
        PROJECTRON_REDIRECTS = Arrays.asList(toMockUri(server, "/redirect"));
        PROJECTRON_TOS = URI.create("http://projectron.example/tos.html");
        APPLICATION_CONTEXT = buildRemoteJsonLdContexts(Arrays.asList(INTEROP_CONTEXT, SOLID_OIDC_CONTEXT));
    }

    @Test
    @DisplayName("Create new crud application profile")
    void createNewCrudApplicationProfile() throws SaiException {
        URI uri = toMockUri(server, "/new/crud/application");

        ApplicationProfile.Builder builder = new ApplicationProfile.Builder(uri, saiSession);
        // Note - the application profile constructor automatically sets the right contexts but setting it here
        // explicitly to to test the underlying support of json ld context assignment.
        ApplicationProfile profile = builder.setContentType(LD_JSON).setJsonLdContext(APPLICATION_CONTEXT).setName(PROJECTRON_NAME).setDescription(PROJECTRON_DESCRIPTION)
                                            .setAuthorUri(PROJECTRON_AUTHOR).setAccessNeedGroupUris(PROJECTRON_NEED_GROUPS)
                                            .setClientUri(PROJECTRON_CLIENT_URI).setRedirectUris(PROJECTRON_REDIRECTS)
                                            .setTosUri(PROJECTRON_TOS).setLogoUri(PROJECTRON_LOGO).setScopes(PROJECTRON_SCOPES)
                                            .setGrantType(PROJECTRON_GRANT_TYPES).setResponseTypes(PROJECTRON_RESPONSE_TYPES)
                                            .setDefaultMaxAge(PROJECTRON_MAX_AGE).setRequireAuthTime(PROJECTRON_REQUIRE_AUTH_TIME)
                                            .build();
        assertDoesNotThrow(() -> profile.update());
        assertNotNull(profile);
    }

    @Test
    @DisplayName("Create new crud application profile - only required fields")
    void createNewCrudApplicationProfileRequired() throws SaiException {
        URI uri = toMockUri(server, "/new/crud/application");
        ApplicationProfile.Builder builder = new ApplicationProfile.Builder(uri, saiSession);
        ApplicationProfile profile = builder.setName(PROJECTRON_NAME).setDescription(PROJECTRON_DESCRIPTION)
                .setAuthorUri(PROJECTRON_AUTHOR).setAccessNeedGroupUris(PROJECTRON_NEED_GROUPS)
                .setRedirectUris(PROJECTRON_REDIRECTS).setLogoUri(PROJECTRON_LOGO).setScopes(PROJECTRON_SCOPES)
                .setGrantType(PROJECTRON_GRANT_TYPES).setResponseTypes(PROJECTRON_RESPONSE_TYPES).build();
        assertDoesNotThrow(() -> profile.update());
        assertNotNull(profile);
    }

    @Test
    @DisplayName("Read crud application profile")
    void readCrudApplicationProfile() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/crud/application");
        ApplicationProfile profile = ApplicationProfile.get(uri, saiSession);
        checkProfile(profile, false);
    }

    @Test
    @DisplayName("Read crud application profile - only required fields")
    void readCrudApplicationProfileRequired() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/crud/required/application");
        ApplicationProfile profile = ApplicationProfile.get(uri, saiSession);
        checkProfile(profile, true);
    }

    @Test
    @DisplayName("Reload crud application profile")
    void reloadCrudApplicationProfile() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/crud/application");
        ApplicationProfile profile = ApplicationProfile.get(uri, saiSession);
        ApplicationProfile reloaded = profile.reload();
        checkProfile(reloaded, false);
    }

    @Test
    @DisplayName("Fail to read crud application profile - missing required fields")
    void failToReadCrudApplicationProfileMissingFields() {
        URI uri = toMockUri(server, "/missing-fields/crud/application");
        assertThrows(SaiException.class, () -> ApplicationProfile.get(uri, saiSession));
    }

    @Test
    @DisplayName("Update crud application profile")
    void updateCrudApplicationProfile() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/crud/application");
        ApplicationProfile profile = ApplicationProfile.get(uri, saiSession);
        ApplicationProfile.Builder builder = new ApplicationProfile.Builder(uri, saiSession);
        ApplicationProfile updatedProfile = builder.setDataset(profile.getDataset()).setName("Projectimus Prime").build();
        assertDoesNotThrow(() -> updatedProfile.update());
        assertEquals("Projectimus Prime", updatedProfile.getName());
    }

    @Test
    @DisplayName("Delete crud application profile")
    void deleteCrudApplicationProfile() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/crud/application");
        ApplicationProfile profile = ApplicationProfile.get(uri, saiSession);
        assertDoesNotThrow(() -> profile.delete());
        assertFalse(profile.isExists());
    }

    @Test
    @DisplayName("Fail to read crud application profile - invalid context")
    void failToReadCrudApplicationProfileContexts() {
        URI uri = toMockUri(server, "/crud/application");
        try (MockedStatic<RdfUtils> mockRdfUtils = Mockito.mockStatic(RdfUtils.class, CALLS_REAL_METHODS)) {
            mockRdfUtils.when(() -> RdfUtils.buildRemoteJsonLdContexts(any(List.class))).thenThrow(SaiRdfException.class);
            assertThrows(SaiException.class, () -> ApplicationProfile.get(uri, saiSession));
        }
    }

    void checkProfile(ApplicationProfile profile, boolean requiredOnly) {
        assertEquals(PROJECTRON_NAME, profile.getName());
        assertEquals(PROJECTRON_LOGO, profile.getLogoUri());
        assertEquals(PROJECTRON_DESCRIPTION, profile.getDescription());
        assertEquals(PROJECTRON_AUTHOR, profile.getAuthorUri());
        for (URI groupUri : profile.getAccessNeedGroupUris()) { assertTrue(PROJECTRON_NEED_GROUPS.contains(groupUri)); }
        for (URI redirectUri : profile.getRedirectUris()) { assertTrue(PROJECTRON_REDIRECTS.contains(redirectUri)); }
        for (String scope : profile.getScopes()) { assertTrue(PROJECTRON_SCOPES.contains(scope)); }
        for (String grantType : profile.getGrantTypes()) { assertTrue(PROJECTRON_GRANT_TYPES.contains(grantType)); }
        for (String responseType : profile.getResponseTypes()) { assertTrue(PROJECTRON_RESPONSE_TYPES.contains(responseType)); }
        if (!requiredOnly) {
            assertEquals(PROJECTRON_MAX_AGE, profile.getDefaultMaxAge());
            assertEquals(PROJECTRON_REQUIRE_AUTH_TIME, profile.getRequireAuthTime());
            assertEquals(PROJECTRON_CLIENT_URI, profile.getClientUri());
            assertEquals(PROJECTRON_TOS, profile.getTosUri());
        }
    }
}

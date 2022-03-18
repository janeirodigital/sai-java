package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.exceptions.SaiRuntimeException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.immutable.AccessConsent;
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

class AuthorizationRegistryTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static List<URL> accessConsentUrls;

    @BeforeAll
    static void beforeAll() throws SaiException {

        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));

        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        // GET authorization registry in Turtle
        mockOnGet(dispatcher, "/authorization/", "authorization/authorization-registry-ttl");
        mockOnGet(dispatcher, "/empty/authorization/", "authorization/authorization-registry-empty-ttl");
        
        mockOnGet(dispatcher, "/authorization/all-1", "authorization/all/all-1-ttl");
        mockOnGet(dispatcher, "/authorization/all-1-project", "authorization/all/all-1-project-ttl");
        mockOnGet(dispatcher, "/authorization/all-1-milestone", "authorization/all/all-1-milestone-ttl");
        mockOnGet(dispatcher, "/authorization/all-1-issue", "authorization/all/all-1-issue-ttl");
        mockOnGet(dispatcher, "/authorization/all-1-task", "authorization/all/all-1-task-ttl");

        mockOnGet(dispatcher, "/authorization/all-2", "authorization/all/all-2-ttl");
        mockOnGet(dispatcher, "/authorization/all-2-note", "authorization/all/all-2-note-ttl");


        mockOnGet(dispatcher, "/authorization/registry-1", "authorization/all-from-registry/registry-1-ttl");
        mockOnGet(dispatcher, "/authorization/registry-1-project", "authorization/all-from-registry/registry-1-project-ttl");
        mockOnGet(dispatcher, "/authorization/registry-1-milestone", "authorization/all-from-registry/registry-1-milestone-ttl");
        mockOnGet(dispatcher, "/authorization/registry-1-issue", "authorization/all-from-registry/registry-1-issue-ttl");
        mockOnGet(dispatcher, "/authorization/registry-1-task", "authorization/all-from-registry/registry-1-task-ttl");

        mockOnGet(dispatcher, "/authorization/agent-1", "authorization/all-from-agent/agent-1-ttl");
        mockOnGet(dispatcher, "/authorization/agent-1-project", "authorization/all-from-agent/agent-1-project-ttl");
        mockOnGet(dispatcher, "/authorization/agent-1-milestone", "authorization/all-from-agent/agent-1-milestone-ttl");
        mockOnGet(dispatcher, "/authorization/agent-1-issue", "authorization/all-from-agent/agent-1-issue-ttl");
        mockOnGet(dispatcher, "/authorization/agent-1-task", "authorization/all-from-agent/agent-1-task-ttl");

        mockOnGet(dispatcher, "/authorization/selected-1", "authorization/selected-from-registry/selected-1-ttl");
        mockOnGet(dispatcher, "/authorization/selected-1-project", "authorization/selected-from-registry/selected-1-project-ttl");
        mockOnGet(dispatcher, "/authorization/selected-1-milestone", "authorization/selected-from-registry/selected-1-milestone-ttl");
        mockOnGet(dispatcher, "/authorization/selected-1-issue", "authorization/selected-from-registry/selected-1-issue-ttl");
        mockOnGet(dispatcher, "/authorization/selected-1-task", "authorization/selected-from-registry/selected-1-task-ttl");
        
        mockOnPut(dispatcher, "/new/authorization/", "http/201");  // create new
        mockOnPut(dispatcher, "/authorization/", "http/204");  // update existing
        mockOnDelete(dispatcher, "/authorization/", "http/204");  // delete
        // GET authorization registry in Turtle with invalid fields
        mockOnGet(dispatcher, "/invalid-fields/authorization/", "authorization/authorization-registry-invalid-ttl");
        // GET authorization registry in Turtle with links to consents that don't exist
        mockOnGet(dispatcher, "/missing-consents/authorization/", "authorization/authorization-registry-missing-consents-ttl");
        // GET authorization registry in JSON-LD
        mockOnGet(dispatcher, "/jsonld/authorization/", "authorization/authorization-registry-jsonld");
        mockOnPut(dispatcher, "/new/jsonld/authorization/", "http/201");  // create new
        mockOnPut(dispatcher, "/jsonld/authorization/", "http/204");  // update existing or delete
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
        accessConsentUrls = Arrays.asList(toUrl(server, "/authorization/all-1"),
                                          toUrl(server, "/authorization/registry-1"),
                                          toUrl(server, "/authorization/agent-1"),
                                          toUrl(server, "/authorization/selected-1"));
    }

    @Test
    @DisplayName("Create an authorization registry")
    void createNewAuthorizationRegistry() throws SaiException {
        URL url = toUrl(server, "/new/authorization/");
        AuthorizationRegistry authzRegistry = new AuthorizationRegistry.Builder(url, saiSession).build();
        assertDoesNotThrow(() -> authzRegistry.update());
        assertNotNull(authzRegistry);
    }

    @Test
    @DisplayName("Get an authorization registry")
    void readAuthorizationRegistry() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(url, saiSession);
        checkRegistry(authzRegistry);
        assertFalse(authzRegistry.isEmpty());
    }

    @Test
    @DisplayName("Get an empty authorization registry")
    void readEmptyAuthorizationRegistry() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/empty/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(url, saiSession);
        assertTrue(authzRegistry.isEmpty());
    }

    @Test
    @DisplayName("Fail to get authorization registry - invalid fields")
    void failToGetAuthorizationRegistry() {
        URL url = toUrl(server, "/invalid-fields/authorization/");
        assertThrows(SaiException.class, () -> AuthorizationRegistry.get(url, saiSession));
    }

    @Test
    @DisplayName("Reload authorization registry")
    void reloadAuthorizationRegistry() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(url, saiSession);
        AuthorizationRegistry reloaded = authzRegistry.reload();
        checkRegistry(reloaded);
    }

    @Test
    @DisplayName("Find an access consent")
    void findAccessConsent() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/authorization/");
        URL toFind = stringToUrl("https://projectron.example/id");
        URL toFail = stringToUrl("https://ghost.example/id");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(url, saiSession);
        AccessConsent found = authzRegistry.getAccessConsents().find(toFind);
        assertEquals(toFind, found.getGrantee());
        AccessConsent fail = authzRegistry.getAccessConsents().find(toFail);
        assertNull(fail);
    }

    @Test
    @DisplayName("Fail to iterate access consents - missing consent")
    void failToFindAccessConsentMissing() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/missing-consents/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(url, saiSession);
        Iterator<AccessConsent> iterator = authzRegistry.getAccessConsents().iterator();
        assertThrows(SaiRuntimeException.class, () -> iterator.next());
    }
    
    @Test
    @DisplayName("Read existing authorization registry in JSON-LD")
    void readAuthorizationRegistryJsonLd() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/jsonld/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(url, saiSession, LD_JSON);
        checkRegistry(authzRegistry);
    }

    @Test
    @DisplayName("Create new crud authorization registry in JSON-LD")
    void createNewAuthorizationRegistryJsonLd() throws SaiException {
        URL url = toUrl(server, "/new/jsonld/authorization/");
        AuthorizationRegistry authzRegistry = new AuthorizationRegistry.Builder(url, saiSession).setContentType(LD_JSON).build();
        assertDoesNotThrow(() -> authzRegistry.update());
        assertNotNull(authzRegistry);
    }

    @Test
    @DisplayName("Delete crud authorization registry")
    void deleteAuthorizationRegistry() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(url, saiSession);
        assertDoesNotThrow(() -> authzRegistry.delete());
        assertFalse(authzRegistry.isExists());
    }

    @Test
    @DisplayName("Add access consent to authorization registry")
    void addAccessConsents() throws SaiException, SaiNotFoundException, SaiAlreadyExistsException {
        URL url = toUrl(server, "/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(url, saiSession);

        URL consentUrl = toUrl(server, "/authorization/all-2");
        URL grantee = stringToUrl("https://nevernote.example/id");

        AccessConsent consent = mock(AccessConsent.class);
        when(consent.getUrl()).thenReturn(consentUrl);
        when(consent.getGrantee()).thenReturn(grantee);
        authzRegistry.add(consent);
        assertTrue(authzRegistry.getAccessConsents().isPresent(consentUrl));
    }

    @Test
    @DisplayName("Replace and remove access consent from authorization registry")
    void replaceAccessConsent() throws SaiException, SaiNotFoundException, SaiAlreadyExistsException {
        URL registryUrl = toUrl(server, "/authorization/");
        URL originalUrl = toUrl(server, "/authorization/all-2");
        URL replacedUrl = toUrl(server, "/authorization/all-replaced-2");

        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(registryUrl, saiSession);
        AccessConsent original = AccessConsent.get(originalUrl, saiSession);
        AccessConsent.Builder builder = new AccessConsent.Builder(replacedUrl, saiSession);
        AccessConsent replaced = builder.setGrantedBy(original.getGrantedBy()).setGrantedWith(original.getGrantedWith())
                                        .setGrantedAt(original.getGrantedAt()).setGrantee(original.getGrantee())
                                        .setAccessNeedGroup(original.getAccessNeedGroup()).setReplaces(original.getUrl())
                                        .setDataConsents(original.getDataConsents()).build();
        authzRegistry.add(replaced);
        assertTrue(authzRegistry.getAccessConsents().isPresent(replacedUrl));
        assertFalse(authzRegistry.getAccessConsents().isPresent(originalUrl));
    }

    @Test
    @DisplayName("Remove access consents from authorization registry")
    void removeAccessConsents() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(url, saiSession);

        URL consentUrl = toUrl(server, "/authorization/all-1");
        AccessConsent consent = mock(AccessConsent.class);
        when(consent.getUrl()).thenReturn(consentUrl);
        authzRegistry.remove(consent);
        assertFalse(authzRegistry.getAccessConsents().isPresent(consentUrl));
    }

    @Test
    @DisplayName("Fail to add access consents to authorization registry - already exists")
    void failToAddAccessConsent() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(url, saiSession);

        URL consentUrl = toUrl(server, "/authorization/all-1");
        URL grantee = stringToUrl("https://projectron.example/id");
        AccessConsent consent = mock(AccessConsent.class);
        when(consent.getUrl()).thenReturn(consentUrl);
        when(consent.getGrantee()).thenReturn(grantee);
        assertThrows(SaiAlreadyExistsException.class, () -> authzRegistry.add(consent));
    }

    private void checkRegistry(AuthorizationRegistry authzRegistry) {
        assertNotNull(authzRegistry);
        assertTrue(accessConsentUrls.containsAll(authzRegistry.getAccessConsents().getRegistrationUrls()));
    }
}

package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.exceptions.SaiRuntimeException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.immutable.AccessAuthorization;
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
    private static List<URL> accessAuthorizationUrls;

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
        // GET authorization registry in Turtle with links to authorizations that don't exist
        mockOnGet(dispatcher, "/missing-authorizations/authorization/", "authorization/authorization-registry-missing-authorizations-ttl");
        // GET authorization registry in JSON-LD
        mockOnGet(dispatcher, "/jsonld/authorization/", "authorization/authorization-registry-jsonld");
        mockOnPut(dispatcher, "/new/jsonld/authorization/", "http/201");  // create new
        mockOnPut(dispatcher, "/jsonld/authorization/", "http/204");  // update existing or delete
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
        accessAuthorizationUrls = Arrays.asList(toUrl(server, "/authorization/all-1"),
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
    @DisplayName("Find an access authorization")
    void findAccessAuthorization() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/authorization/");
        URL toFind = stringToUrl("https://projectron.example/id");
        URL toFail = stringToUrl("https://ghost.example/id");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(url, saiSession);
        AccessAuthorization found = authzRegistry.getAccessAuthorizations().find(toFind);
        assertEquals(toFind, found.getGrantee());
        AccessAuthorization fail = authzRegistry.getAccessAuthorizations().find(toFail);
        assertNull(fail);
    }

    @Test
    @DisplayName("Fail to iterate access authorizations - missing authorization")
    void failToFindAccessAuthorizationMissing() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/missing-authorizations/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(url, saiSession);
        Iterator<AccessAuthorization> iterator = authzRegistry.getAccessAuthorizations().iterator();
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
    @DisplayName("Add access authorization to authorization registry")
    void addAccessAuthorizations() throws SaiException, SaiNotFoundException, SaiAlreadyExistsException {
        URL url = toUrl(server, "/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(url, saiSession);

        URL authorizationUrl = toUrl(server, "/authorization/all-2");
        URL grantee = stringToUrl("https://nevernote.example/id");

        AccessAuthorization authorization = mock(AccessAuthorization.class);
        when(authorization.getUrl()).thenReturn(authorizationUrl);
        when(authorization.getGrantee()).thenReturn(grantee);
        authzRegistry.add(authorization);
        assertTrue(authzRegistry.getAccessAuthorizations().isPresent(authorizationUrl));
    }

    @Test
    @DisplayName("Replace and remove access authorization from authorization registry")
    void replaceAccessAuthorization() throws SaiException, SaiNotFoundException, SaiAlreadyExistsException {
        URL registryUrl = toUrl(server, "/authorization/");
        URL originalUrl = toUrl(server, "/authorization/all-2");
        URL replacedUrl = toUrl(server, "/authorization/all-replaced-2");

        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(registryUrl, saiSession);
        AccessAuthorization original = AccessAuthorization.get(originalUrl, saiSession);
        AccessAuthorization.Builder builder = new AccessAuthorization.Builder(replacedUrl, saiSession);
        AccessAuthorization replaced = builder.setGrantedBy(original.getGrantedBy()).setGrantedWith(original.getGrantedWith())
                                        .setGrantedAt(original.getGrantedAt()).setGrantee(original.getGrantee())
                                        .setAccessNeedGroup(original.getAccessNeedGroup()).setReplaces(original.getUrl())
                                        .setDataAuthorizations(original.getDataAuthorizations()).build();
        authzRegistry.add(replaced);
        assertTrue(authzRegistry.getAccessAuthorizations().isPresent(replacedUrl));
        assertFalse(authzRegistry.getAccessAuthorizations().isPresent(originalUrl));
    }

    @Test
    @DisplayName("Remove access authorizations from authorization registry")
    void removeAccessAuthorizations() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(url, saiSession);

        URL authorizationUrl = toUrl(server, "/authorization/all-1");
        AccessAuthorization authorization = mock(AccessAuthorization.class);
        when(authorization.getUrl()).thenReturn(authorizationUrl);
        authzRegistry.remove(authorization);
        assertFalse(authzRegistry.getAccessAuthorizations().isPresent(authorizationUrl));
    }

    @Test
    @DisplayName("Fail to add access authorizations to authorization registry - already exists")
    void failToAddAccessAuthorization() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(url, saiSession);

        URL authorizationUrl = toUrl(server, "/authorization/all-1");
        URL grantee = stringToUrl("https://projectron.example/id");
        AccessAuthorization authorization = mock(AccessAuthorization.class);
        when(authorization.getUrl()).thenReturn(authorizationUrl);
        when(authorization.getGrantee()).thenReturn(grantee);
        assertThrows(SaiAlreadyExistsException.class, () -> authzRegistry.add(authorization));
    }

    private void checkRegistry(AuthorizationRegistry authzRegistry) {
        assertNotNull(authzRegistry);
        assertTrue(accessAuthorizationUrls.containsAll(authzRegistry.getAccessAuthorizations().getRegistrationUrls()));
    }
}

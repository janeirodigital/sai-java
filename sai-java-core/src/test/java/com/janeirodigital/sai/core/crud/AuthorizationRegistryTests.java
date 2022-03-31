package com.janeirodigital.sai.core.crud;

import com.janeirodigital.mockwebserver.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiRuntimeException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.immutable.AccessAuthorization;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpException;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.janeirodigital.mockwebserver.DispatcherHelper.*;
import static com.janeirodigital.mockwebserver.MockWebServerHelper.toMockUri;
import static com.janeirodigital.sai.httputils.ContentType.LD_JSON;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationRegistryTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static List<URI> accessAuthorizationUris;

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
        accessAuthorizationUris = Arrays.asList(toMockUri(server, "/authorization/all-1"),
                                          toMockUri(server, "/authorization/registry-1"),
                                          toMockUri(server, "/authorization/agent-1"),
                                          toMockUri(server, "/authorization/selected-1"));
    }

    @Test
    @DisplayName("Create an authorization registry")
    void createNewAuthorizationRegistry() throws SaiException {
        URI uri = toMockUri(server, "/new/authorization/");
        AuthorizationRegistry authzRegistry = new AuthorizationRegistry.Builder(uri, saiSession).build();
        assertDoesNotThrow(() -> authzRegistry.update());
        assertNotNull(authzRegistry);
    }

    @Test
    @DisplayName("Get an authorization registry")
    void readAuthorizationRegistry() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(uri, saiSession);
        checkRegistry(authzRegistry);
        assertFalse(authzRegistry.isEmpty());
    }

    @Test
    @DisplayName("Get an empty authorization registry")
    void readEmptyAuthorizationRegistry() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/empty/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(uri, saiSession);
        assertTrue(authzRegistry.isEmpty());
    }

    @Test
    @DisplayName("Fail to get authorization registry - invalid fields")
    void failToGetAuthorizationRegistry() {
        URI uri = toMockUri(server, "/invalid-fields/authorization/");
        assertThrows(SaiException.class, () -> AuthorizationRegistry.get(uri, saiSession));
    }

    @Test
    @DisplayName("Reload authorization registry")
    void reloadAuthorizationRegistry() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(uri, saiSession);
        AuthorizationRegistry reloaded = authzRegistry.reload();
        checkRegistry(reloaded);
    }

    @Test
    @DisplayName("Find an access authorization")
    void findAccessAuthorization() throws SaiException, SaiHttpNotFoundException, SaiHttpException {
        URI uri = toMockUri(server, "/authorization/");
        URI toFind = URI.create("https://projectron.example/id");
        URI toFail = URI.create("https://ghost.example/id");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(uri, saiSession);
        AccessAuthorization found = authzRegistry.getAccessAuthorizations().find(toFind);
        assertEquals(toFind, found.getGrantee());
        AccessAuthorization fail = authzRegistry.getAccessAuthorizations().find(toFail);
        assertNull(fail);
    }

    @Test
    @DisplayName("Fail to iterate access authorizations - missing authorization")
    void failToFindAccessAuthorizationMissing() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/missing-authorizations/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(uri, saiSession);
        Iterator<AccessAuthorization> iterator = authzRegistry.getAccessAuthorizations().iterator();
        assertThrows(SaiRuntimeException.class, () -> iterator.next());
    }
    
    @Test
    @DisplayName("Read existing authorization registry in JSON-LD")
    void readAuthorizationRegistryJsonLd() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/jsonld/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(uri, saiSession, LD_JSON);
        checkRegistry(authzRegistry);
    }

    @Test
    @DisplayName("Create new crud authorization registry in JSON-LD")
    void createNewAuthorizationRegistryJsonLd() throws SaiException {
        URI uri = toMockUri(server, "/new/jsonld/authorization/");
        AuthorizationRegistry authzRegistry = new AuthorizationRegistry.Builder(uri, saiSession).setContentType(LD_JSON).build();
        assertDoesNotThrow(() -> authzRegistry.update());
        assertNotNull(authzRegistry);
    }

    @Test
    @DisplayName("Delete crud authorization registry")
    void deleteAuthorizationRegistry() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(uri, saiSession);
        assertDoesNotThrow(() -> authzRegistry.delete());
        assertFalse(authzRegistry.isExists());
    }

    @Test
    @DisplayName("Add access authorization to authorization registry")
    void addAccessAuthorizations() throws SaiException, SaiHttpNotFoundException, SaiAlreadyExistsException, SaiHttpException {
        URI uri = toMockUri(server, "/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(uri, saiSession);

        URI authorizationUri = toMockUri(server, "/authorization/all-2");
        URI grantee = URI.create("https://nevernote.example/id");

        AccessAuthorization authorization = mock(AccessAuthorization.class);
        when(authorization.getUri()).thenReturn(authorizationUri);
        when(authorization.getGrantee()).thenReturn(grantee);
        authzRegistry.add(authorization);
        assertTrue(authzRegistry.getAccessAuthorizations().isPresent(authorizationUri));
    }

    @Test
    @DisplayName("Replace and remove access authorization from authorization registry")
    void replaceAccessAuthorization() throws SaiException, SaiHttpNotFoundException, SaiAlreadyExistsException {
        URI registryUri = toMockUri(server, "/authorization/");
        URI originalUri = toMockUri(server, "/authorization/all-2");
        URI replacedUri = toMockUri(server, "/authorization/all-replaced-2");

        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(registryUri, saiSession);
        AccessAuthorization original = AccessAuthorization.get(originalUri, saiSession);
        AccessAuthorization.Builder builder = new AccessAuthorization.Builder(replacedUri, saiSession);
        AccessAuthorization replaced = builder.setGrantedBy(original.getGrantedBy()).setGrantedWith(original.getGrantedWith())
                                        .setGrantedAt(original.getGrantedAt()).setGrantee(original.getGrantee())
                                        .setAccessNeedGroup(original.getAccessNeedGroup()).setReplaces(original.getUri())
                                        .setDataAuthorizations(original.getDataAuthorizations()).build();
        authzRegistry.add(replaced);
        assertTrue(authzRegistry.getAccessAuthorizations().isPresent(replacedUri));
        assertFalse(authzRegistry.getAccessAuthorizations().isPresent(originalUri));
    }

    @Test
    @DisplayName("Remove access authorizations from authorization registry")
    void removeAccessAuthorizations() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(uri, saiSession);

        URI authorizationUri = toMockUri(server, "/authorization/all-1");
        AccessAuthorization authorization = mock(AccessAuthorization.class);
        when(authorization.getUri()).thenReturn(authorizationUri);
        authzRegistry.remove(authorization);
        assertFalse(authzRegistry.getAccessAuthorizations().isPresent(authorizationUri));
    }

    @Test
    @DisplayName("Fail to add access authorizations to authorization registry - already exists")
    void failToAddAccessAuthorization() throws SaiException, SaiHttpNotFoundException, SaiHttpException {
        URI uri = toMockUri(server, "/authorization/");
        AuthorizationRegistry authzRegistry = AuthorizationRegistry.get(uri, saiSession);

        URI authorizationUri = toMockUri(server, "/authorization/all-1");
        URI grantee = URI.create("https://projectron.example/id");
        AccessAuthorization authorization = mock(AccessAuthorization.class);
        when(authorization.getUri()).thenReturn(authorizationUri);
        when(authorization.getGrantee()).thenReturn(grantee);
        assertThrows(SaiAlreadyExistsException.class, () -> authzRegistry.add(authorization));
    }

    private void checkRegistry(AuthorizationRegistry authzRegistry) {
        assertNotNull(authzRegistry);
        assertTrue(accessAuthorizationUris.containsAll(authzRegistry.getAccessAuthorizations().getRegistrationUris()));
    }
}

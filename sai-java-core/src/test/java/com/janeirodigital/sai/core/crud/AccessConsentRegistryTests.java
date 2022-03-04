package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
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

class AccessConsentRegistryTests {

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
        // GET access consent registry in Turtle
        mockOnGet(dispatcher, "/access/", "access/access-registry-ttl");
        mockOnGet(dispatcher, "/empty/access/", "access/access-registry-empty-ttl");
        
        mockOnGet(dispatcher, "/access/all-1", "access/all/all-1-ttl");
        mockOnGet(dispatcher, "/access/all-1-project", "access/all/all-1-project-ttl");
        mockOnGet(dispatcher, "/access/all-1-milestone", "access/all/all-1-milestone-ttl");
        mockOnGet(dispatcher, "/access/all-1-issue", "access/all/all-1-issue-ttl");
        mockOnGet(dispatcher, "/access/all-1-task", "access/all/all-1-task-ttl");

        mockOnGet(dispatcher, "/access/all-2", "access/all/all-2-ttl");
        mockOnGet(dispatcher, "/access/all-2-note", "access/all/all-2-note-ttl");


        mockOnGet(dispatcher, "/access/registry-1", "access/all-from-registry/registry-1-ttl");
        mockOnGet(dispatcher, "/access/registry-1-project", "access/all-from-registry/registry-1-project-ttl");
        mockOnGet(dispatcher, "/access/registry-1-milestone", "access/all-from-registry/registry-1-milestone-ttl");
        mockOnGet(dispatcher, "/access/registry-1-issue", "access/all-from-registry/registry-1-issue-ttl");
        mockOnGet(dispatcher, "/access/registry-1-task", "access/all-from-registry/registry-1-task-ttl");

        mockOnGet(dispatcher, "/access/agent-1", "access/all-from-agent/agent-1-ttl");
        mockOnGet(dispatcher, "/access/agent-1-project", "access/all-from-agent/agent-1-project-ttl");
        mockOnGet(dispatcher, "/access/agent-1-milestone", "access/all-from-agent/agent-1-milestone-ttl");
        mockOnGet(dispatcher, "/access/agent-1-issue", "access/all-from-agent/agent-1-issue-ttl");
        mockOnGet(dispatcher, "/access/agent-1-task", "access/all-from-agent/agent-1-task-ttl");

        mockOnGet(dispatcher, "/access/selected-1", "access/selected-from-registry/selected-1-ttl");
        mockOnGet(dispatcher, "/access/selected-1-project", "access/selected-from-registry/selected-1-project-ttl");
        mockOnGet(dispatcher, "/access/selected-1-milestone", "access/selected-from-registry/selected-1-milestone-ttl");
        mockOnGet(dispatcher, "/access/selected-1-issue", "access/selected-from-registry/selected-1-issue-ttl");
        mockOnGet(dispatcher, "/access/selected-1-task", "access/selected-from-registry/selected-1-task-ttl");
        
        mockOnPut(dispatcher, "/new/access/", "http/201");  // create new
        mockOnPut(dispatcher, "/access/", "http/204");  // update existing
        mockOnDelete(dispatcher, "/access/", "http/204");  // delete
        // GET access consent registry in Turtle with invalid fields
        mockOnGet(dispatcher, "/invalid-fields/access/", "access/access-registry-invalid-ttl");
        // GET access consent registry in Turtle with links to consents that don't exist
        mockOnGet(dispatcher, "/missing-consents/access/", "access/access-registry-missing-consents-ttl");
        // GET access consent registry in JSON-LD
        mockOnGet(dispatcher, "/jsonld/access/", "access/access-registry-jsonld");
        mockOnPut(dispatcher, "/new/jsonld/access/", "http/201");  // create new
        mockOnPut(dispatcher, "/jsonld/access/", "http/204");  // update existing or delete
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
        accessConsentUrls = Arrays.asList(toUrl(server, "/access/all-1"),
                                          toUrl(server, "/access/registry-1"),
                                          toUrl(server, "/access/agent-1"),
                                          toUrl(server, "/access/selected-1"));
    }

    @Test
    @DisplayName("Create an access consent registry")
    void createNewAccessConsentRegistry() throws SaiException {
        URL url = toUrl(server, "/new/access/");
        AccessConsentRegistry accessRegistry = new AccessConsentRegistry.Builder(url, saiSession).build();
        assertDoesNotThrow(() -> accessRegistry.update());
        assertNotNull(accessRegistry);
    }

    @Test
    @DisplayName("Get an access consent registry")
    void readAccessConsentRegistry() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/access/");
        AccessConsentRegistry accessRegistry = AccessConsentRegistry.get(url, saiSession);
        checkRegistry(accessRegistry);
        assertFalse(accessRegistry.isEmpty());
    }

    @Test
    @DisplayName("Get an empty access consent registry")
    void readEmptyAccessConsentRegistry() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/empty/access/");
        AccessConsentRegistry accessRegistry = AccessConsentRegistry.get(url, saiSession);
        assertTrue(accessRegistry.isEmpty());
    }

    @Test
    @DisplayName("Fail to get access consent registry - invalid fields")
    void failToGetAccessConsentRegistry() {
        URL url = toUrl(server, "/invalid-fields/access/");
        assertThrows(SaiException.class, () -> AccessConsentRegistry.get(url, saiSession));
    }

    @Test
    @DisplayName("Reload access consent registry")
    void reloadAccessConsentRegistry() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/access/");
        AccessConsentRegistry accessRegistry = AccessConsentRegistry.get(url, saiSession);
        AccessConsentRegistry reloaded = accessRegistry.reload();
        checkRegistry(reloaded);
    }

    @Test
    @DisplayName("Find an access consent")
    void findAccessConsent() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/access/");
        URL toFind = stringToUrl("https://projectron.example/id");
        URL toFail = stringToUrl("https://ghost.example/id");
        AccessConsentRegistry accessRegistry = AccessConsentRegistry.get(url, saiSession);
        AccessConsent found = accessRegistry.getAccessConsents().find(toFind);
        assertEquals(toFind, found.getGrantee());
        AccessConsent fail = accessRegistry.getAccessConsents().find(toFail);
        assertNull(fail);
    }

    @Test
    @DisplayName("Fail to iterate access consents - missing consent")
    void failToFindAccessConsentMissing() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/missing-consents/access/");
        AccessConsentRegistry accessRegistry = AccessConsentRegistry.get(url, saiSession);
        Iterator<AccessConsent> iterator = accessRegistry.getAccessConsents().iterator();
        assertThrows(SaiRuntimeException.class, () -> iterator.next());
    }
    
    @Test
    @DisplayName("Read existing access consent registry in JSON-LD")
    void readAccessConsentRegistryJsonLd() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/jsonld/access/");
        AccessConsentRegistry accessRegistry = AccessConsentRegistry.get(url, saiSession, LD_JSON);
        checkRegistry(accessRegistry);
    }

    @Test
    @DisplayName("Create new crud access consent registry in JSON-LD")
    void createNewAccessConsentRegistryJsonLd() throws SaiException {
        URL url = toUrl(server, "/new/jsonld/access/");
        AccessConsentRegistry accessRegistry = new AccessConsentRegistry.Builder(url, saiSession).setContentType(LD_JSON).build();
        assertDoesNotThrow(() -> accessRegistry.update());
        assertNotNull(accessRegistry);
    }

    @Test
    @DisplayName("Delete crud access consent registry")
    void deleteAccessConsentRegistry() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/access/");
        AccessConsentRegistry accessRegistry = AccessConsentRegistry.get(url, saiSession);
        assertDoesNotThrow(() -> accessRegistry.delete());
        assertFalse(accessRegistry.isExists());
    }

    @Test
    @DisplayName("Add access consent to access consent registry")
    void addAgentRegistrations() throws SaiException, SaiNotFoundException, SaiAlreadyExistsException {
        URL url = toUrl(server, "/access/");
        AccessConsentRegistry accessRegistry = AccessConsentRegistry.get(url, saiSession);

        URL consentUrl = toUrl(server, "/access/all-2");
        URL grantee = stringToUrl("https://nevernote.example/id");

        AccessConsent consent = mock(AccessConsent.class);
        when(consent.getUrl()).thenReturn(consentUrl);
        when(consent.getGrantee()).thenReturn(grantee);
        accessRegistry.add(consent);
    }

    @Test
    @DisplayName("Remove access consents from access consent registry")
    void removeAgentRegistrations() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/access/");
        AccessConsentRegistry accessRegistry = AccessConsentRegistry.get(url, saiSession);

        URL consentUrl = toUrl(server, "/access/all-1");
        AccessConsent consent = mock(AccessConsent.class);
        when(consent.getUrl()).thenReturn(consentUrl);
        accessRegistry.remove(consent);
    }

    @Test
    @DisplayName("Fail to add access consents to access consent registry - already exists")
    void failToAddAccessConsent() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/access/");
        AccessConsentRegistry accessRegistry = AccessConsentRegistry.get(url, saiSession);

        URL consentUrl = toUrl(server, "/access/all-1");
        URL grantee = stringToUrl("https://projectron.example/id");
        AccessConsent consent = mock(AccessConsent.class);
        when(consent.getUrl()).thenReturn(consentUrl);
        when(consent.getGrantee()).thenReturn(grantee);
        assertThrows(SaiAlreadyExistsException.class, () -> accessRegistry.add(consent));
    }

    private void checkRegistry(AccessConsentRegistry accessRegistry) {
        assertNotNull(accessRegistry);
        assertTrue(accessConsentUrls.containsAll(accessRegistry.getAccessConsents().getRegistrationUrls()));
    }
}

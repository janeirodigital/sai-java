package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnGet;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnPut;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static com.janeirodigital.sai.core.helpers.HttpHelper.stringToUrl;
import static com.janeirodigital.sai.core.vocabularies.AclVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.SCOPE_ALL;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.SCOPE_INHERITED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AccessConsentTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static URL ALICE_ID;
    private static URL JARVIS_ID;
    private static URL PROJECTRON_ID;
    private static URL PROJECTRON_NEED_GROUP;
    private static URL PROJECT_TREE, MILESTONE_TREE, ISSUE_TREE, TASK_TREE;
    private static URL PROJECTRON_PROJECT_NEED, PROJECTRON_MILESTONE_NEED, PROJECTRON_ISSUE_NEED, PROJECTRON_TASK_NEED;
    private static OffsetDateTime GRANT_TIME;
    private static List<URL> ALL_DATA_CONSENT_URLS;
    private static List<RDFNode> ACCESS_MODES, CREATOR_ACCESS_MODES;

    @BeforeAll
    static void beforeAll() throws SaiException, SaiNotFoundException {
        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));
        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        // GET agent registry in Turtle
        mockOnGet(dispatcher, "/access/all-1", "access/all/all-1-ttl");
        mockOnPut(dispatcher, "/access/all-1", "http/201");
        mockOnGet(dispatcher, "/access/all-1-project", "access/all/all-1-project-ttl");
        mockOnPut(dispatcher, "/access/all-1-project", "http/201");
        mockOnGet(dispatcher, "/access/all-1-milestone", "access/all/all-1-milestone-ttl");
        mockOnPut(dispatcher, "/access/all-1-milestone", "http/201");
        mockOnGet(dispatcher, "/access/all-1-task", "access/all/all-1-task-ttl");
        mockOnPut(dispatcher, "/access/all-1-task", "http/201");
        mockOnGet(dispatcher, "/access/all-1-issue", "access/all/all-1-issue-ttl");
        mockOnPut(dispatcher, "/access/all-1-issue", "http/201");
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        ALICE_ID = stringToUrl("https://alice.example/id");
        JARVIS_ID = stringToUrl("https://jarvis.example/id");
        PROJECTRON_ID = stringToUrl("https://projectron.example/id");
        GRANT_TIME = OffsetDateTime.parse("2020-09-05T06:15:01Z", DateTimeFormatter.ISO_DATE_TIME);
        PROJECTRON_NEED_GROUP = stringToUrl("https://projectron.example/#d8219b1f");
        PROJECTRON_PROJECT_NEED = stringToUrl("https://projectron.example/#ac54ff1e");
        PROJECTRON_MILESTONE_NEED = stringToUrl("https://projectron.example/#bd66ee2b");
        PROJECTRON_ISSUE_NEED = stringToUrl("https://projectron.example/#aa123a1b");
        PROJECTRON_TASK_NEED = stringToUrl("https://projectron.example/#ce22cc1a");
        ALL_DATA_CONSENT_URLS = Arrays.asList(toUrl(server, "/access/all-1-project"), toUrl(server, "/access/all-1-milestone"),
                                              toUrl(server, "/access/all-1-issue"), toUrl(server, "/access/all-1-task"));
        PROJECT_TREE = stringToUrl("http://data.example/shapetrees/pm#ProjectTree");
        MILESTONE_TREE = stringToUrl("http://data.example/shapetrees/pm#MilestoneTree");
        ISSUE_TREE = stringToUrl("http://data.example/shapetrees/pm#IssueTree");
        TASK_TREE = stringToUrl("http://data.example/shapetrees/pm#TaskTree");
        ACCESS_MODES = Arrays.asList(ACL_READ, ACL_CREATE);
        CREATOR_ACCESS_MODES = Arrays.asList(ACL_UPDATE, ACL_DELETE);
         
    }

    @Test
    @DisplayName("Create an access consent and data consents - Scope: All")
    void createAccessConsentScopeAll() throws SaiException {
        URL accessUrl = toUrl(server, "/access/all-1");
        URL projectUrl = toUrl(server, "/access/all-1-project");
        URL milestoneUrl = toUrl(server, "/access/all-1-milestone");
        URL issueUrl = toUrl(server, "/access/all-1-issue");
        URL taskUrl = toUrl(server, "/access/all-1-task");
        
        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setDataOwner(ALICE_ID).setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                                                   .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                                                   .setScopeOfConsent(SCOPE_ALL).setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        DataConsent.Builder milestoneBuilder = new DataConsent.Builder(milestoneUrl, saiSession);
        DataConsent milestoneConsent = milestoneBuilder.setDataOwner(ALICE_ID).setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(MILESTONE_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfConsent(SCOPE_INHERITED).setAccessNeed(PROJECTRON_MILESTONE_NEED).setInheritsFrom(projectConsent.getUrl()).build();

        DataConsent.Builder issueBuilder = new DataConsent.Builder(issueUrl, saiSession);
        DataConsent issueConsent = issueBuilder.setDataOwner(ALICE_ID).setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(ISSUE_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfConsent(SCOPE_INHERITED).setAccessNeed(PROJECTRON_ISSUE_NEED).setInheritsFrom(milestoneConsent.getUrl()).build();

        DataConsent.Builder taskBuilder = new DataConsent.Builder(taskUrl, saiSession);
        DataConsent taskConsent = taskBuilder.setDataOwner(ALICE_ID).setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(TASK_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfConsent(SCOPE_INHERITED).setAccessNeed(PROJECTRON_TASK_NEED).setInheritsFrom(milestoneConsent.getUrl()).build();

        List<DataConsent> dataConsents = Arrays.asList(projectConsent, milestoneConsent, issueConsent, taskConsent);

        AccessConsent.Builder accessBuilder = new AccessConsent.Builder(accessUrl, saiSession);
        AccessConsent accessConsent = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID).setGrantedAt(GRANT_TIME)
                                                   .setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                                                   .setDataConsents(dataConsents).build();

        accessConsent.create();
        assertNotNull(accessConsent);
    }

    @Test
    @DisplayName("Create an access grant and data grants - Scope: All")
    void createAccessGrantScopeAll() { }


    @Test
    @DisplayName("Read an access consent and data consents - Scope: All")
    void readAccessConsentScopeAll() throws SaiNotFoundException, SaiException {
        URL url = toUrl(server, "/access/all-1");
        AccessConsent consent = AccessConsent.get(url, saiSession);
        assertNotNull(consent);
        assertEquals(ALICE_ID, consent.getGrantedBy());
        assertEquals(JARVIS_ID, consent.getGrantedWith());
        assertEquals(PROJECTRON_ID, consent.getGrantee());
        assertEquals(GRANT_TIME, consent.getGrantedAt());
        assertEquals(PROJECTRON_NEED_GROUP, consent.getAccessNeedGroup());
        for (DataConsent dataConsent : consent.getDataConsents()) { assertTrue(ALL_DATA_CONSENT_URLS.contains(dataConsent.getUrl())); }
    }

    @Test
    @DisplayName("Read an access grant and data grants - Scope: All")
    void readAccessGrantScopeAll() {
        // This should be testing the more extensive readable access grant
    }

    // Create an access consent and data consents
    // Add it to the registry (replace if necessary)
    // Regenerate the access grant and data grants
    // Let a consuming application use them (potentially have integration tests that work across modules?)

}

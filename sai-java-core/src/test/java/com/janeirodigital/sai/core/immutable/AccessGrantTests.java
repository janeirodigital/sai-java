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
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AccessGrantTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static URL ALICE_ID;
    private static URL JARVIS_ID;
    private static URL PROJECTRON_ID;
    private static URL PROJECTRON_NEED_GROUP;
    private static URL PROJECTS_DATA_REGISTRATION, MILESTONES_DATA_REGISTRATION, ISSUES_DATA_REGISTRATION, TASKS_DATA_REGISTRATION;
    private static URL PROJECT_TREE, MILESTONE_TREE, ISSUE_TREE, TASK_TREE;
    private static URL PROJECTRON_PROJECT_NEED, PROJECTRON_MILESTONE_NEED, PROJECTRON_ISSUE_NEED, PROJECTRON_TASK_NEED;
    private static OffsetDateTime GRANT_TIME;
    private static List<URL> ALL_DATA_GRANT_URLS;
    private static List<RDFNode> ACCESS_MODES, CREATOR_ACCESS_MODES;

    @BeforeAll
    static void beforeAll() throws SaiException, SaiNotFoundException {
        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));
        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        // GET access grant in Turtle with missing fields
        mockOnGet(dispatcher, "/missing-fields/agents/projectron/all-1-grant", "agents/alice/projectron-all/all-1-grant-missing-fields-ttl");
        // GET / PUT access grant and data grants from the agent registry
        mockOnGet(dispatcher, "/agents/projectron/all-1-grant", "agents/alice/projectron-all/all-1-grant-ttl");
        mockOnPut(dispatcher, "/agents/projectron/all-1-grant", "http/201");
        mockOnGet(dispatcher, "/agents/projectron/all-1-grant-project", "agents/alice/projectron-all/all-1-grant-project-ttl");
        mockOnPut(dispatcher, "/agents/projectron/all-1-grant-project", "http/201");
        mockOnGet(dispatcher, "/agents/projectron/all-1-grant-milestone", "agents/alice/projectron-all/all-1-grant-milestone-ttl");
        mockOnPut(dispatcher, "/agents/projectron/all-1-grant-milestone", "http/201");
        mockOnGet(dispatcher, "/agents/projectron/all-1-grant-issue", "agents/alice/projectron-all/all-1-grant-issue-ttl");
        mockOnPut(dispatcher, "/agents/projectron/all-1-grant-issue", "http/201");
        mockOnGet(dispatcher, "/agents/projectron/all-1-grant-task", "agents/alice/projectron-all/all-1-grant-task-ttl");
        mockOnPut(dispatcher, "/agents/projectron/all-1-grant-task", "http/201");
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
        ALL_DATA_GRANT_URLS = Arrays.asList(toUrl(server, "/agents/projectron/all-1-grant-project"),
                                            toUrl(server, "/agents/projectron/all-1-grant-milestone"),
                                            toUrl(server, "/agents/projectron/all-1-grant-issue"),
                                            toUrl(server, "/agents/projectron/all-1-grant-task"));
        PROJECTS_DATA_REGISTRATION = toUrl(server, "/personal/data/projects/");
        MILESTONES_DATA_REGISTRATION = toUrl(server, "/personal/data/milestones/");
        ISSUES_DATA_REGISTRATION = toUrl(server, "/personal/data/issues/");
        TASKS_DATA_REGISTRATION = toUrl(server, "/personal/data/tasks/");
        PROJECT_TREE = stringToUrl("http://data.example/shapetrees/pm#ProjectTree");
        MILESTONE_TREE = stringToUrl("http://data.example/shapetrees/pm#MilestoneTree");
        ISSUE_TREE = stringToUrl("http://data.example/shapetrees/pm#IssueTree");
        TASK_TREE = stringToUrl("http://data.example/shapetrees/pm#TaskTree");
        ACCESS_MODES = Arrays.asList(ACL_READ, ACL_CREATE);
        CREATOR_ACCESS_MODES = Arrays.asList(ACL_UPDATE, ACL_DELETE);
         
    }

    @Test
    @DisplayName("Create new access grant and linked data grants - scope: all")
    void createAccessGrantScopeAll() throws SaiException {
        URL accessUrl = toUrl(server, "/agents/projectron/all-1-grant");
        URL projectUrl = toUrl(server, "/agents/projectron/all-1-grant-project");
        URL milestoneUrl = toUrl(server, "/agents/projectron/all-1-grant-milestone");
        URL issueUrl = toUrl(server, "/agents/projectron/all-1-grant-issue");
        URL taskUrl = toUrl(server, "/agents/projectron/all-1-grant-task");
        
        DataGrant.Builder projectBuilder = new DataGrant.Builder(projectUrl, saiSession);
        DataGrant projectGrant = projectBuilder.setDataOwner(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                                                   .setDataRegistration(PROJECTS_DATA_REGISTRATION).setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                                                   .setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY).setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        DataGrant.Builder milestoneBuilder = new DataGrant.Builder(milestoneUrl, saiSession);
        DataGrant milestoneGrant = milestoneBuilder.setDataOwner(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(MILESTONES_DATA_REGISTRATION).setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY).setAccessNeed(PROJECTRON_MILESTONE_NEED).build();

        DataGrant.Builder issueBuilder = new DataGrant.Builder(issueUrl, saiSession);
        DataGrant issueGrant = issueBuilder.setDataOwner(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(ISSUES_DATA_REGISTRATION).setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY).setAccessNeed(PROJECTRON_ISSUE_NEED).build();

        DataGrant.Builder taskBuilder = new DataGrant.Builder(taskUrl, saiSession);
        DataGrant taskGrant = taskBuilder.setDataOwner(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(TASKS_DATA_REGISTRATION).setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY).setAccessNeed(PROJECTRON_TASK_NEED).build();

        List<DataGrant> dataGrants = Arrays.asList(projectGrant, milestoneGrant, issueGrant, taskGrant);

        AccessGrant.Builder accessBuilder = new AccessGrant.Builder(accessUrl, saiSession);
        AccessGrant accessGrant = accessBuilder.setGrantedBy(ALICE_ID).setGrantedAt(GRANT_TIME)
                                                   .setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                                                   .setDataGrants(dataGrants).build();
        assertDoesNotThrow(() -> accessGrant.create());
    }

    @Test
    @DisplayName("Get an access grant and linked data grants - scope: all")
    void getAccessGrant() throws SaiNotFoundException, SaiException {
        URL url = toUrl(server, "/agents/projectron/all-1-grant");
        AccessGrant accessGrant = AccessGrant.get(url, saiSession);
        checkAccessGrant(accessGrant);
    }

    @Test
    @DisplayName("Reload an access grant and linked data grants - scope: all")
    void reloadAccessGrant() throws SaiNotFoundException, SaiException {
        URL url = toUrl(server, "/agents/projectron/all-1-grant");
        AccessGrant accessGrant = AccessGrant.get(url, saiSession);
        AccessGrant reloaded = accessGrant.reload();
        checkAccessGrant(reloaded);
    }

    @Test
    @DisplayName("Fail to get access grant - missing required fields")
    void failToGetAccessGrantRequired() {
        URL url = toUrl(server, "/missing-fields/agents/projectron/all-1-grant");
        assertThrows(SaiException.class, () -> AccessGrant.get(url, saiSession));
    }

    private void checkAccessGrant(AccessGrant accessGrant) {
        assertNotNull(accessGrant);
        assertEquals(ALICE_ID, accessGrant.getGrantedBy());
        assertEquals(PROJECTRON_ID, accessGrant.getGrantee());
        assertEquals(GRANT_TIME, accessGrant.getGrantedAt());
        assertEquals(PROJECTRON_NEED_GROUP, accessGrant.getAccessNeedGroup());
        for (DataGrant dataGrant : accessGrant.getDataGrants()) { assertTrue(ALL_DATA_GRANT_URLS.contains(dataGrant.getUrl())); }

    }
}

package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.readable.*;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.shapetrees.core.contentloaders.DocumentLoaderManager;
import com.janeirodigital.shapetrees.core.contentloaders.HttpExternalDocumentLoader;
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

import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.*;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static com.janeirodigital.sai.core.helpers.HttpHelper.stringToUrl;
import static com.janeirodigital.sai.core.vocabularies.AclVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.SCOPE_ALL_FROM_REGISTRY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AccessGrantTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static URL ALICE_ID;
    private static URL BOB_ID;
    private static URL JARVIS_ID;
    private static URL PROJECTRON_ID;
    private static URL PROJECTRON_NEED_GROUP;
    private static URL PROJECTS_DATA_REGISTRATION, MILESTONES_DATA_REGISTRATION, ISSUES_DATA_REGISTRATION, TASKS_DATA_REGISTRATION;
    private static URL PROJECT_1, PROJECT_2, PROJECT_3;
    private static URL PROJECT_1_MILESTONE_1, PROJECT_1_MILESTONE_2, PROJECT_2_MILESTONE_3;
    private static URL PROJECT_1_TASK_1, PROJECT_1_TASK_2, PROJECT_1_ISSUE_1;
    private static URL PROJECT_TREE, MILESTONE_TREE, ISSUE_TREE, TASK_TREE, MISSING_TREE;
    private static URL PROJECTRON_PROJECT_NEED, PROJECTRON_MILESTONE_NEED, PROJECTRON_ISSUE_NEED, PROJECTRON_TASK_NEED;
    private static OffsetDateTime GRANT_TIME;
    private static List<URL> ALL_DATA_GRANT_URLS;
    private static List<RDFNode> ACCESS_MODES, CREATOR_ACCESS_MODES;

    @BeforeAll
    static void beforeAll() throws SaiException, SaiNotFoundException {
        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));
        DocumentLoaderManager.setLoader(new HttpExternalDocumentLoader());
        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        // GET access grant in Turtle with missing fields
        mockOnGet(dispatcher, "/missing-fields/all-1-agents/all-1-projectron/all-1-grant", "agents/alice/projectron-all/all-1-grant-missing-fields-ttl");
        // GET / PUT access grant and data grants from the agent registry
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant", "agents/alice/projectron-all/all-1-grant-ttl");
        mockOnPut(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant", "http/201");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-personal-project", "agents/alice/projectron-all/all-1-grant-personal-project-ttl");
        mockOnPut(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-personal-project", "http/201");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-personal-milestone", "agents/alice/projectron-all/all-1-grant-personal-milestone-ttl");
        mockOnPut(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-personal-milestone", "http/201");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-personal-issue", "agents/alice/projectron-all/all-1-grant-personal-issue-ttl");
        mockOnPut(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-personal-issue", "http/201");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-personal-task", "agents/alice/projectron-all/all-1-grant-personal-task-ttl");
        mockOnPut(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-personal-task", "http/201");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-work-project", "agents/alice/projectron-all/all-1-grant-work-project-ttl");
        mockOnPut(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-work-project", "http/201");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-work-milestone", "agents/alice/projectron-all/all-1-grant-work-milestone-ttl");
        mockOnPut(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-work-milestone", "http/201");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-work-issue", "agents/alice/projectron-all/all-1-grant-work-issue-ttl");
        mockOnPut(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-work-issue", "http/201");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-work-task", "agents/alice/projectron-all/all-1-grant-work-task-ttl");
        mockOnPut(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-work-task", "http/201");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-project", "agents/alice/projectron-all/all-1-delegated-grant-bob-project-ttl");
        mockOnPut(dispatcher, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-project", "http/201");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-milestone", "agents/alice/projectron-all/all-1-delegated-grant-bob-milestone-ttl");
        mockOnPut(dispatcher, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-milestone", "http/201");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-issue", "agents/alice/projectron-all/all-1-delegated-grant-bob-issue-ttl");
        mockOnPut(dispatcher, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-issue", "http/201");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-task", "agents/alice/projectron-all/all-1-delegated-grant-bob-task-ttl");
        mockOnPut(dispatcher, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-task", "http/201");

        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/", "agents/alice/projectron-all-from-registry/registry-1-projectron-registration-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant", "agents/alice/projectron-all-from-registry/registry-1-grant-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-project", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-project-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-milestone", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-milestone-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-issue", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-issue-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-task", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-task-ttl");

        mockOnGet(dispatcher, "/selected-1-agents/selected-1-projectron/", "agents/alice/projectron-selected-from-registry/selected-1-projectron-registration-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/selected-1-projectron/selected-1-grant", "agents/alice/projectron-selected-from-registry/selected-1-grant-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/selected-1-projectron/selected-1-grant-personal-project", "agents/alice/projectron-selected-from-registry/selected-1-grant-personal-project-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/selected-1-projectron/selected-1-grant-personal-milestone", "agents/alice/projectron-selected-from-registry/selected-1-grant-personal-milestone-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/selected-1-projectron/selected-1-grant-personal-issue", "agents/alice/projectron-selected-from-registry/selected-1-grant-personal-issue-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/selected-1-projectron/selected-1-grant-personal-task", "agents/alice/projectron-selected-from-registry/selected-1-grant-personal-task-ttl");

        mockOnGet(dispatcher, "/agent-1-agents/agent-1-projectron/", "agents/alice/projectron-all-from-agent/agent-1-projectron-registration-ttl");
        mockOnGet(dispatcher, "/agent-1-agents/agent-1-projectron/agent-1-grant", "agents/alice/projectron-all-from-agent/agent-1-grant-ttl");
        mockOnGet(dispatcher, "/agent-1-agents/agent-1-projectron/agent-1-delegated-grant-bob-project", "agents/alice/projectron-all-from-agent/agent-1-delegated-grant-bob-project-ttl");
        mockOnGet(dispatcher, "/agent-1-agents/agent-1-projectron/agent-1-delegated-grant-bob-milestone", "agents/alice/projectron-all-from-agent/agent-1-delegated-grant-bob-milestone-ttl");
        mockOnGet(dispatcher, "/agent-1-agents/agent-1-projectron/agent-1-delegated-grant-bob-issue", "agents/alice/projectron-all-from-agent/agent-1-delegated-grant-bob-issue-ttl");
        mockOnGet(dispatcher, "/agent-1-agents/agent-1-projectron/agent-1-delegated-grant-bob-task", "agents/alice/projectron-all-from-agent/agent-1-delegated-grant-bob-task-ttl");
        mockOnGet(dispatcher, "/agent-1-agents/agent-1-bob/", "agents/alice/projectron-all-from-agent/agent-1-bob-registration-ttl");
        mockOnGet(dispatcher, "/agent-1-bob-agents/agent-1-alice/", "agents/alice/projectron-all-from-agent/agent-1-bob-alice-registration-ttl");
        mockOnGet(dispatcher, "/agent-1-bob-agents/agent-1-alice/agent-1-grant", "agents/alice/projectron-all-from-agent/agent-1-bob-grant-ttl");
        mockOnGet(dispatcher, "/agent-1-bob-agents/agent-1-alice/agent-1-grant-project", "agents/alice/projectron-all-from-agent/agent-1-bob-grant-project-ttl");
        mockOnGet(dispatcher, "/agent-1-bob-agents/agent-1-alice/agent-1-grant-milestone", "agents/alice/projectron-all-from-agent/agent-1-bob-grant-milestone-ttl");
        mockOnGet(dispatcher, "/agent-1-bob-agents/agent-1-alice/agent-1-grant-issue", "agents/alice/projectron-all-from-agent/agent-1-bob-grant-issue-ttl");
        mockOnGet(dispatcher, "/agent-1-bob-agents/agent-1-alice/agent-1-grant-task", "agents/alice/projectron-all-from-agent/agent-1-bob-grant-task-ttl");

        // Shape trees and shapes
        mockOnGet(dispatcher, "/shapetrees/pm", "schemas/pm-shapetrees-ttl");

        // Get Alice and Bob's data registries - doesn't change across use cases
        mockOnGet(dispatcher, "/personal/data/", "data/alice/personal-data-registry-ttl");
        mockOnGet(dispatcher, "/personal/data/projects/", "data/alice/personal-data-registration-projects-ttl");
        mockOnGet(dispatcher, "/personal/data/milestones/", "data/alice/personal-data-registration-milestones-ttl");
        mockOnGet(dispatcher, "/personal/data/issues/", "data/alice/personal-data-registration-issues-ttl");
        mockOnGet(dispatcher, "/personal/data/tasks/", "data/alice/personal-data-registration-tasks-ttl");
        mockOnGet(dispatcher, "/personal/data/calendars/", "data/alice/personal-data-registration-calendars-ttl");
        mockOnGet(dispatcher, "/personal/data/appointments/", "data/alice/personal-data-registration-appointments-ttl");

        mockOnGet(dispatcher, "/personal/data/projects/p1", "data/alice/personal-data-projects-p1-ttl");
        mockOnPut(dispatcher, "/personal/data/projects/p1", "http/204");
        mockOnGet(dispatcher, "/personal/data/projects/p2", "data/alice/personal-data-projects-p2-ttl");
        mockOnPut(dispatcher, "/personal/data/projects/p2", "http/204");
        mockOnGet(dispatcher, "/personal/data/projects/p3", "data/alice/personal-data-projects-p3-ttl");
        mockOnPut(dispatcher, "/personal/data/projects/p3", "http/204");
        mockOnPut(dispatcher, "/personal/data/projects/new-project", "http/201");

        mockOnGet(dispatcher, "/personal/data/milestones/p1m1", "data/alice/personal-data-milestones-p1m1-ttl");
        mockOnGet(dispatcher, "/personal/data/milestones/p1m2", "data/alice/personal-data-milestones-p1m2-ttl");
        mockOnGet(dispatcher, "/personal/data/milestones/p2m3", "data/alice/personal-data-milestones-p2m3-ttl");
        mockOnDelete(dispatcher, "/personal/data/milestones/p2m3", "http/204");
        mockOnPut(dispatcher, "/personal/data/milestones/new-milestone", "http/201");

        mockOnGet(dispatcher, "/personal/data/tasks/p1t1", "data/alice/personal-data-tasks-p1t1-ttl");
        mockOnGet(dispatcher, "/personal/data/tasks/p1t2", "data/alice/personal-data-tasks-p1t2-ttl");

        mockOnGet(dispatcher, "/personal/data/issues/p1i1", "data/alice/personal-data-issues-p1i1-ttl");
        mockOnDelete(dispatcher, "/personal/data/issues/p1i1", "http/204");

        mockOnGet(dispatcher, "/work/data/", "data/alice/work-data-registry-ttl");
        mockOnGet(dispatcher, "/work/data/projects/", "data/alice/work-data-registration-projects-ttl");
        mockOnGet(dispatcher, "/work/data/milestones/", "data/alice/work-data-registration-milestones-ttl");
        mockOnGet(dispatcher, "/work/data/issues/", "data/alice/work-data-registration-issues-ttl");
        mockOnGet(dispatcher, "/work/data/tasks/", "data/alice/work-data-registration-tasks-ttl");

        mockOnGet(dispatcher, "/bob/data/", "data/bob/bob-data-registry-ttl");
        mockOnGet(dispatcher, "/bob/data/projects/", "data/bob/bob-data-registration-projects-ttl");
        mockOnGet(dispatcher, "/bob/data/milestones/", "data/bob/bob-data-registration-milestones-ttl");
        mockOnGet(dispatcher, "/bob/data/issues/", "data/bob/bob-data-registration-issues-ttl");
        mockOnGet(dispatcher, "/bob/data/tasks/", "data/bob/bob-data-registration-tasks-ttl");


        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
        ALICE_ID = stringToUrl("https://alice.example/id");
        BOB_ID = stringToUrl("https://bob.example/id");
        JARVIS_ID = stringToUrl("https://jarvis.example/id");
        PROJECTRON_ID = stringToUrl("https://projectron.example/id");
        GRANT_TIME = OffsetDateTime.parse("2020-09-05T06:15:01Z", DateTimeFormatter.ISO_DATE_TIME);
        PROJECTRON_NEED_GROUP = stringToUrl("https://projectron.example/#d8219b1f");
        PROJECTRON_PROJECT_NEED = stringToUrl("https://projectron.example/#ac54ff1e");
        PROJECTRON_MILESTONE_NEED = stringToUrl("https://projectron.example/#bd66ee2b");
        PROJECTRON_ISSUE_NEED = stringToUrl("https://projectron.example/#aa123a1b");
        PROJECTRON_TASK_NEED = stringToUrl("https://projectron.example/#ce22cc1a");
        ALL_DATA_GRANT_URLS = Arrays.asList(toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project"),
                                            toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-milestone"),
                                            toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-issue"),
                                            toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-task"),
                                            toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-work-project"),
                                            toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-work-milestone"),
                                            toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-work-issue"),
                                            toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-work-task"),
                                            toUrl(server, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-project"),
                                            toUrl(server, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-milestone"),
                                            toUrl(server, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-issue"),
                                            toUrl(server, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-task"));
        PROJECTS_DATA_REGISTRATION = toUrl(server, "/personal/data/projects/");
        MILESTONES_DATA_REGISTRATION = toUrl(server, "/personal/data/milestones/");
        ISSUES_DATA_REGISTRATION = toUrl(server, "/personal/data/issues/");
        TASKS_DATA_REGISTRATION = toUrl(server, "/personal/data/tasks/");
        PROJECT_1 = toUrl(server, "/personal/data/projects/p1");
        PROJECT_2 = toUrl(server, "/personal/data/projects/p2");
        PROJECT_3 = toUrl(server, "/personal/data/projects/p3");
        PROJECT_1_MILESTONE_1 = toUrl(server, "/personal/data/milestones/p1m1");
        PROJECT_1_MILESTONE_2 = toUrl(server, "/personal/data/milestones/p1m2");
        PROJECT_2_MILESTONE_3 = toUrl(server, "/personal/data/milestones/p2m3");
        PROJECT_1_TASK_1 = toUrl(server, "/personal/data/tasks/p1t1");
        PROJECT_1_TASK_2 = toUrl(server, "/personal/data/tasks/p1t2");
        PROJECT_1_ISSUE_1 = toUrl(server, "/personal/data/issues/p1i1");
        PROJECT_TREE = toUrl(server, "/shapetrees/pm#ProjectTree");
        MILESTONE_TREE = toUrl(server, "/shapetrees/pm#MilestoneTree");
        ISSUE_TREE = toUrl(server, "/shapetrees/pm#IssueTree");
        TASK_TREE = toUrl(server, "/shapetrees/pm#TaskTree");
        MISSING_TREE = toUrl(server, "/shapetrees/pm#MissingTree");
        ACCESS_MODES = Arrays.asList(ACL_READ, ACL_CREATE);
        CREATOR_ACCESS_MODES = Arrays.asList(ACL_UPDATE, ACL_DELETE);
         
    }

    @Test
    @DisplayName("Create new access grant and linked data grants - scope: all")
    void createAccessGrantScopeAll() throws SaiException {
        URL accessUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant");
        URL projectUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        URL milestoneUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-milestone");
        URL issueUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-issue");
        URL taskUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-task");
        
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
    @DisplayName("Create new access grant and linked data grants - only required fields")
    void createAccessGrantRequiredOnly() throws SaiException {
        URL accessUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant");
        URL projectUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        URL milestoneUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-milestone");

        DataGrant.Builder projectBuilder = new DataGrant.Builder(projectUrl, saiSession);
        DataGrant projectGrant = projectBuilder.setDataOwner(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(PROJECTS_DATA_REGISTRATION).setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY).setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        DataGrant.Builder milestoneBuilder = new DataGrant.Builder(milestoneUrl, saiSession);
        DataGrant milestoneGrant = milestoneBuilder.setDataOwner(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(MILESTONES_DATA_REGISTRATION).setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY).setAccessNeed(PROJECTRON_MILESTONE_NEED).build();

        List<DataGrant> dataGrants = Arrays.asList(projectGrant, milestoneGrant);

        AccessGrant.Builder accessBuilder = new AccessGrant.Builder(accessUrl, saiSession);
        AccessGrant accessGrant = accessBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataGrants(dataGrants).build();
        assertDoesNotThrow(() -> accessGrant.create());
    }

    @Test
    @DisplayName("Get an access grant and linked data grants - scope: all")
    void getAccessGrant() throws SaiNotFoundException, SaiException {
        URL url = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant");
        AccessGrant accessGrant = AccessGrant.get(url, saiSession);
        checkAccessGrant(accessGrant);
    }

    @Test
    @DisplayName("Reload an access grant and linked data grants - scope: all")
    void reloadAccessGrant() throws SaiNotFoundException, SaiException {
        URL url = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant");
        AccessGrant accessGrant = AccessGrant.get(url, saiSession);
        AccessGrant reloaded = accessGrant.reload();
        checkAccessGrant(reloaded);
    }

    @Test
    @DisplayName("Get a readable access grant and linked data grants - scope: all")
    void getReadableAccessGrant() throws SaiNotFoundException, SaiException {
        URL url = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant");
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(url, saiSession);
        checkReadableAccessGrant(accessGrant);
    }

    @Test
    @DisplayName("Reload a readable access grant and linked data grants - scope: all")
    void reloadReadableAccessGrant() throws SaiNotFoundException, SaiException {
        URL url = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant");
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(url, saiSession);
        ReadableAccessGrant reloaded = accessGrant.reload();
        checkReadableAccessGrant(accessGrant);
    }

    @Test
    @DisplayName("Fail to get access grant - missing required fields")
    void failToGetAccessGrantRequired() {
        URL url = toUrl(server, "/missing-fields/all-1-agents/all-1-projectron/all-1-grant");
        assertThrows(SaiException.class, () -> AccessGrant.get(url, saiSession));
    }

    @Test
    @DisplayName("Fail to get readable access grant - missing required fields")
    void failToGetReadableAccessGrantRequired() {
        URL url = toUrl(server, "/missing-fields/all-1-agents/all-1-projectron/all-1-grant");
        assertThrows(SaiException.class, () -> ReadableAccessGrant.get(url, saiSession));
    }

    @Test
    @DisplayName("Get readable access grant and associated readable data grants - Scope: All")
    void testGetAccessGrantAll() throws SaiNotFoundException, SaiException {
        URL grantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant");
        URL bobProjectGrantUrl = toUrl(server, "/all-1-bob-agents/all-1-alice/all-1-grant-project");
        URL delegatedProjectUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-project");
        List<URL> aliceProjectUrls = Arrays.asList(toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project"),
                toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-work-project"));
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(grantUrl, saiSession);
        assertNotNull(accessGrant);
        assertEquals(ALICE_ID, accessGrant.getGrantedBy());
        assertEquals(GRANT_TIME, accessGrant.getGrantedAt());
        assertEquals(PROJECTRON_ID, accessGrant.getGrantee());
        assertEquals(PROJECTRON_NEED_GROUP, accessGrant.getAccessNeedGroup());
        assertEquals(12, accessGrant.getDataGrants().size());
        assertEquals(2, accessGrant.getDataOwners().size());
        assertEquals(3, accessGrant.findDataGrants(PROJECT_TREE).size());
        for (ReadableDataGrant readableDataGrant : accessGrant.getDataGrants()) {
            if (readableDataGrant instanceof AllFromRegistryDataGrant) {
                AllFromRegistryDataGrant specificGrant = (AllFromRegistryDataGrant) readableDataGrant;
                assertEquals(PROJECT_TREE, specificGrant.getRegisteredShapeTree());
                assertFalse(specificGrant.getInheritingGrants().isEmpty());
                if (specificGrant.isDelegated()) { assertEquals(bobProjectGrantUrl, specificGrant.getDelegationOf()); }
            } else if (readableDataGrant instanceof InheritedDataGrant) {
                InheritedDataGrant inheritedGrant = (InheritedDataGrant) readableDataGrant;
                if (inheritedGrant.isDelegated()) { assertEquals(delegatedProjectUrl, inheritedGrant.getInheritsFrom());
                } else { assertTrue(aliceProjectUrls.contains(inheritedGrant.getInheritsFrom())); }
            }
        }
        List<ReadableDataGrant> projectGrants = accessGrant.findDataGrants(ALICE_ID, PROJECT_TREE);
        assertFalse(projectGrants.isEmpty());
        assertEquals(2, projectGrants.size());
        for (ReadableDataGrant readableDataGrant : projectGrants) {
            AllFromRegistryDataGrant projectGrant = (AllFromRegistryDataGrant) readableDataGrant;
            assertFalse(projectGrant.getDataInstances().isEmpty());
            assertEquals(3, projectGrant.getDataInstances().size());
        }
    }

    @Test
    @DisplayName("Get readable access grant and associated readable data grants - Scope: AllFromRegistry")
    void testGetAccessGrantAllFromRegistry() throws SaiNotFoundException, SaiException {
        URL grantUrl = toUrl(server, "/registry-1-agents/registry-1-projectron/registry-1-grant");
        URL aliceProjectUrl = toUrl(server, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-project");
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(grantUrl, saiSession);
        assertNotNull(accessGrant);
        assertEquals(ALICE_ID, accessGrant.getGrantedBy());
        assertEquals(GRANT_TIME, accessGrant.getGrantedAt());
        assertEquals(PROJECTRON_ID, accessGrant.getGrantee());
        assertEquals(PROJECTRON_NEED_GROUP, accessGrant.getAccessNeedGroup());
        assertEquals(4, accessGrant.getDataGrants().size());
        assertEquals(1, accessGrant.getDataOwners().size());
        assertEquals(1, accessGrant.findDataGrants(PROJECT_TREE).size());
        for (ReadableDataGrant readableDataGrant : accessGrant.getDataGrants()) {
            if (readableDataGrant instanceof AllFromRegistryDataGrant) {
                AllFromRegistryDataGrant specificGrant = (AllFromRegistryDataGrant) readableDataGrant;
                assertEquals(PROJECT_TREE, specificGrant.getRegisteredShapeTree());
                assertFalse(specificGrant.getInheritingGrants().isEmpty());
                assertFalse(specificGrant.isDelegated());
            } else if (readableDataGrant instanceof InheritedDataGrant) {
                InheritedDataGrant inheritedGrant = (InheritedDataGrant) readableDataGrant;
                assertFalse(inheritedGrant.isDelegated());
                assertEquals(aliceProjectUrl, inheritedGrant.getInheritsFrom());
            }
        }
        List<ReadableDataGrant> projectGrants = accessGrant.findDataGrants(ALICE_ID, PROJECT_TREE);
        assertFalse(projectGrants.isEmpty());
        assertEquals(1, projectGrants.size());
        AllFromRegistryDataGrant projectGrant = (AllFromRegistryDataGrant) projectGrants.get(0);
        assertFalse(projectGrant.getDataInstances().isEmpty());
        assertEquals(3, projectGrant.getDataInstances().size());
    }

    @Test
    @DisplayName("Get readable access grant and associated readable data grants - Scope: SelectedFromRegistry")
    void testGetAccessGrantSelectedFromRegistry() throws SaiNotFoundException, SaiException {
        URL grantUrl = toUrl(server, "/selected-1-agents/selected-1-projectron/selected-1-grant");
        URL aliceProjectUrl = toUrl(server, "/selected-1-agents/selected-1-projectron/selected-1-grant-personal-project");
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(grantUrl, saiSession);
        assertNotNull(accessGrant);
        assertEquals(ALICE_ID, accessGrant.getGrantedBy());
        assertEquals(GRANT_TIME, accessGrant.getGrantedAt());
        assertEquals(PROJECTRON_ID, accessGrant.getGrantee());
        assertEquals(PROJECTRON_NEED_GROUP, accessGrant.getAccessNeedGroup());
        assertEquals(4, accessGrant.getDataGrants().size());
        assertEquals(1, accessGrant.getDataOwners().size());
        assertEquals(1, accessGrant.findDataGrants(PROJECT_TREE).size());
        for (ReadableDataGrant readableDataGrant : accessGrant.getDataGrants()) {
            if (readableDataGrant instanceof SelectedFromRegistryDataGrant) {
                SelectedFromRegistryDataGrant specificGrant = (SelectedFromRegistryDataGrant) readableDataGrant;
                assertEquals(PROJECT_TREE, specificGrant.getRegisteredShapeTree());
                assertFalse(specificGrant.getInheritingGrants().isEmpty());
                assertFalse(specificGrant.isDelegated());
            } else if (readableDataGrant instanceof InheritedDataGrant) {
                InheritedDataGrant inheritedGrant = (InheritedDataGrant) readableDataGrant;
                assertFalse(inheritedGrant.isDelegated());
                assertEquals(aliceProjectUrl, inheritedGrant.getInheritsFrom());
            }
        }
        List<ReadableDataGrant> projectGrants = accessGrant.findDataGrants(ALICE_ID, PROJECT_TREE);
        assertFalse(projectGrants.isEmpty());
        assertEquals(1, projectGrants.size());
        SelectedFromRegistryDataGrant projectGrant = (SelectedFromRegistryDataGrant) projectGrants.get(0);
        assertFalse(projectGrant.getDataInstances().isEmpty());
        assertEquals(3, projectGrant.getDataInstances().size());
    }

    // Get readable access grant and data grants for scope: all from agent
    @Test
    @DisplayName("Get readable access grant and associated readable data grants - Scope: AllFromAgent")
    void testGetAccessGrantAllFromAgent() throws SaiNotFoundException, SaiException {
        URL grantUrl = toUrl(server, "/agent-1-agents/agent-1-projectron/agent-1-grant");
        URL bobProjectGrantUrl = toUrl(server, "/agent-1-bob-agents/agent-1-alice/agent-1-grant-project");
        URL delegatedProjectUrl = toUrl(server, "/agent-1-agents/agent-1-projectron/agent-1-delegated-grant-bob-project");
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(grantUrl, saiSession);
        assertNotNull(accessGrant);
        assertEquals(ALICE_ID, accessGrant.getGrantedBy());
        assertEquals(GRANT_TIME, accessGrant.getGrantedAt());
        assertEquals(PROJECTRON_ID, accessGrant.getGrantee());
        assertEquals(PROJECTRON_NEED_GROUP, accessGrant.getAccessNeedGroup());
        assertEquals(4, accessGrant.getDataGrants().size());
        assertEquals(1, accessGrant.getDataOwners().size());
        assertEquals(1, accessGrant.findDataGrants(PROJECT_TREE).size());
        for (ReadableDataGrant readableDataGrant : accessGrant.getDataGrants()) {
            assertTrue(readableDataGrant.isDelegated());
            if (readableDataGrant instanceof AllFromRegistryDataGrant) {
                AllFromRegistryDataGrant specificGrant = (AllFromRegistryDataGrant) readableDataGrant;
                assertEquals(PROJECT_TREE, specificGrant.getRegisteredShapeTree());
                assertFalse(specificGrant.getInheritingGrants().isEmpty());
                assertEquals(bobProjectGrantUrl, specificGrant.getDelegationOf());
            } else if (readableDataGrant instanceof InheritedDataGrant) {
                InheritedDataGrant inheritedGrant = (InheritedDataGrant) readableDataGrant;
                assertEquals(delegatedProjectUrl, inheritedGrant.getInheritsFrom());
            }
        }
        List<ReadableDataGrant> projectGrants = accessGrant.findDataGrants(BOB_ID, PROJECT_TREE);
        assertFalse(projectGrants.isEmpty());
        assertEquals(1, projectGrants.size());
        for (ReadableDataGrant readableDataGrant : projectGrants) {
            AllFromRegistryDataGrant projectGrant = (AllFromRegistryDataGrant) readableDataGrant;
            assertFalse(projectGrant.getDataInstances().isEmpty());
            assertEquals(3, projectGrant.getDataInstances().size());
        }
    }

    @Test
    @DisplayName("Get data instances from readable data grants - Scope: AllFromRegistry")
    void testGetDataInstancesAllFromRegistry() throws SaiNotFoundException, SaiException {
        URL grantUrl = toUrl(server, "/registry-1-agents/registry-1-projectron/registry-1-grant");
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(grantUrl, saiSession);
        // Grant provides access to three projects in Alice's personal data registry (/personal/data/projects/)
        List<ReadableDataGrant> projectGrants = accessGrant.findDataGrants(ALICE_ID, PROJECT_TREE);
        AllFromRegistryDataGrant projectGrant = (AllFromRegistryDataGrant) projectGrants.get(0);
        assertEquals(3, projectGrant.getDataInstances().size());

        List<TestableProject> projects = TestableProject.getAccessible(projectGrant, saiSession);

        for (TestableProject project : projects) {
            if (project.getUrl().equals(PROJECT_1)) { checkProject1(project);
            } else if (project.getUrl().equals(PROJECT_2)) { checkProject2(project);
            } else if (project.getUrl().equals(PROJECT_3)) { checkProject3(project);
            }
        }

        List<ReadableDataGrant> milestoneGrants = accessGrant.findDataGrants(ALICE_ID, MILESTONE_TREE);
        InheritedDataGrant milestoneGrant = (InheritedDataGrant) milestoneGrants.get(0);
        assertEquals(3, projectGrant.getDataInstances().size());

        List<TestableMilestone> milestones = TestableMilestone.getAccessible(milestoneGrant, saiSession);

        for (TestableMilestone milestone : milestones) {
            if (milestone.getUrl().equals(PROJECT_1_MILESTONE_1)) { checkMilestone1(milestone); }
        }
    }

    @Test
    @DisplayName("Create a new data instance from readable data grant")
    void createNewDataInstanceAllFromRegistry() throws SaiNotFoundException, SaiException {
        URL grantUrl = toUrl(server, "/registry-1-agents/registry-1-projectron/registry-1-grant");
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(grantUrl, saiSession);
        List<ReadableDataGrant> projectGrants = accessGrant.findDataGrants(ALICE_ID, PROJECT_TREE);
        AllFromRegistryDataGrant projectGrant = (AllFromRegistryDataGrant) projectGrants.get(0);
        assertTrue(projectGrant.canCreate());
        URL projectUrl = DataInstance.generateUrl(projectGrant, "new-project");
        TestableProject project = new TestableProject.Builder(projectUrl, saiSession)
                                                     .setDataGrant(projectGrant)
                                                     .setName("New project")
                                                     .setDescription("New project instance")
                                                     .build();
        assertNull(project.getParent());
        assertTrue(project.hasAccessible(MILESTONE_TREE));
        assertTrue(project.hasAccessible(ISSUE_TREE));
        assertTrue(project.hasAccessible(TASK_TREE));
        assertFalse(project.hasAccessible(MISSING_TREE));
        assertTrue(project.getMilestones(MILESTONE_TREE).isEmpty());
        assertTrue(project.getIssues(ISSUE_TREE).isEmpty());
        assertTrue(project.getTasks(TASK_TREE).isEmpty());
        assertEquals(PROJECT_TREE, project.getShapeTree().getId());
        assertDoesNotThrow(() -> project.update());
    }

    @Test
    @DisplayName("Create a new child data instance from readable data grant")
    void createNewDataInstanceInherited() throws SaiNotFoundException, SaiException {
        URL grantUrl = toUrl(server, "/registry-1-agents/registry-1-projectron/registry-1-grant");
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(grantUrl, saiSession);
        List<ReadableDataGrant> projectGrants = accessGrant.findDataGrants(ALICE_ID, PROJECT_TREE);
        AllFromRegistryDataGrant projectGrant = (AllFromRegistryDataGrant) projectGrants.get(0);

        List<TestableProject> projects = TestableProject.getAccessible(projectGrant, saiSession);
        TestableProject project = projects.get(0);  // grab any project from the accessible projects
        InheritedDataGrant milestoneGrant = (InheritedDataGrant) project.findChildGrant(MILESTONE_TREE);

        URL milestoneUrl = TestableMilestone.generateUrl(milestoneGrant, "new-milestone");
        TestableMilestone milestone = new TestableMilestone.Builder(milestoneUrl, saiSession)
                                                           .setDataGrant(milestoneGrant)
                                                           .setParent(project)
                                                           .setName("New milestone")
                                                           .setDescription("New milestone instance")
                                                           .build();
        assertEquals(project, milestone.getParent());
        assertEquals(MILESTONE_TREE, milestone.getShapeTree().getId());
        assertDoesNotThrow(() -> milestone.update());
        assertTrue(project.getChildReferences(MILESTONE_TREE).contains(milestone.getUrl()));
        assertThrows(SaiException.class, () -> milestone.findChildGrant(ISSUE_TREE));
    }

    @Test
    @DisplayName("Delete a child data instance from a readable data grant")
    void deleteChildDataInstance() throws SaiNotFoundException, SaiException {
        URL grantUrl = toUrl(server, "/registry-1-agents/registry-1-projectron/registry-1-grant");
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(grantUrl, saiSession);
        List<ReadableDataGrant> projectGrants = accessGrant.findDataGrants(ALICE_ID, PROJECT_TREE);
        AllFromRegistryDataGrant projectGrant = (AllFromRegistryDataGrant) projectGrants.get(0);

        List<TestableProject> projects = TestableProject.getAccessible(projectGrant, saiSession);

        for (TestableProject project : projects) {
            if (project.getUrl().equals(PROJECT_1)) {
                for (TestableIssue issue : project.getIssues(ISSUE_TREE)) {
                    if (issue.getUrl().equals(PROJECT_1_ISSUE_1)) { issue.delete(); }
                }
            }
        }
    }

    private void checkProject1(TestableProject project) throws SaiException {
        List<URL> P1_MILESTONES = Arrays.asList(PROJECT_1_MILESTONE_1, PROJECT_1_MILESTONE_2);
        List<URL> P1_TASKS = Arrays.asList(PROJECT_1_TASK_1, PROJECT_1_TASK_2);
        List<URL> P1_ISSUES = Arrays.asList(PROJECT_1_ISSUE_1);
        // Ensure that the URLs are referencing the right instances
        assertEquals(PROJECT_TREE, project.getShapeTree().getId());
        assertTrue(P1_MILESTONES.containsAll(project.getChildReferences(MILESTONE_TREE)));
        assertTrue(P1_TASKS.containsAll(project.getChildReferences(TASK_TREE)));
        assertTrue(P1_ISSUES.containsAll(project.getChildReferences(ISSUE_TREE)));

        for (TestableMilestone milestone : project.getMilestones(MILESTONE_TREE)) {
            assertTrue(P1_MILESTONES.contains(milestone.getUrl()));
            assertEquals(MILESTONE_TREE, milestone.getShapeTree().getId());
            assertEquals(project, milestone.getParent());
        }

        for (TestableTask task : project.getTasks(TASK_TREE)) {
            assertTrue(P1_TASKS.contains(task.getUrl()));
            assertEquals(TASK_TREE, task.getShapeTree().getId());
            assertEquals(project, task.getParent());
        }

        for (TestableIssue issue : project.getIssues(ISSUE_TREE)) {
            assertTrue(P1_ISSUES.contains(issue.getUrl()));
            assertEquals(ISSUE_TREE, issue.getShapeTree().getId());
            assertEquals(project, issue.getParent());
        }
    }

    private void checkProject2(TestableProject project) throws SaiException {
        List<URL> P2_MILESTONES = Arrays.asList(PROJECT_1_MILESTONE_1, PROJECT_1_MILESTONE_2);
        assertEquals(PROJECT_TREE, project.getShapeTree().getId());
        assertTrue(P2_MILESTONES.containsAll(project.getChildReferences(MILESTONE_TREE)));
        for (TestableMilestone milestone : project.getMilestones(MILESTONE_TREE)) {
            assertTrue(P2_MILESTONES.contains(milestone.getUrl()));
            assertEquals(MILESTONE_TREE, milestone.getShapeTree().getId());
            assertEquals(project, milestone.getParent());
        }
    }

    private void checkProject3(TestableProject project) throws SaiException {
        assertEquals(PROJECT_TREE, project.getShapeTree().getId());
        assertTrue(project.getChildReferences(MILESTONE_TREE).isEmpty());
        assertTrue(project.getChildInstances(MILESTONE_TREE).isEmpty());
        assertTrue(project.getMilestones(MILESTONE_TREE).isEmpty());
        assertTrue(project.getIssues(ISSUE_TREE).isEmpty());
        assertTrue(project.getTasks(TASK_TREE).isEmpty());
    }

    private void checkMilestone1(TestableMilestone milestone) throws SaiException {
        assertEquals(MILESTONE_TREE, milestone.getShapeTree().getId());
        milestone.getParent().getUrl().equals(PROJECT_1);
    }

    private void checkAccessGrant(AccessGrant accessGrant) {
        assertNotNull(accessGrant);
        assertEquals(ALICE_ID, accessGrant.getGrantedBy());
        assertEquals(PROJECTRON_ID, accessGrant.getGrantee());
        assertEquals(GRANT_TIME, accessGrant.getGrantedAt());
        assertEquals(PROJECTRON_NEED_GROUP, accessGrant.getAccessNeedGroup());
        for (DataGrant dataGrant : accessGrant.getDataGrants()) {
            assertTrue(ALL_DATA_GRANT_URLS.contains(dataGrant.getUrl()));
            if (dataGrant.getRegisteredShapeTree().equals(PROJECT_TREE)) { assertEquals(3, dataGrant.getInheritingGrants().size()); }
        }
    }

    private void checkReadableAccessGrant(ReadableAccessGrant accessGrant) {
        assertNotNull(accessGrant);
        assertEquals(ALICE_ID, accessGrant.getGrantedBy());
        assertEquals(PROJECTRON_ID, accessGrant.getGrantee());
        assertEquals(GRANT_TIME, accessGrant.getGrantedAt());
        assertEquals(PROJECTRON_NEED_GROUP, accessGrant.getAccessNeedGroup());
        for (ReadableDataGrant dataGrant : accessGrant.getDataGrants()) {
            assertTrue(ALL_DATA_GRANT_URLS.contains(dataGrant.getUrl()));
            if (dataGrant.getRegisteredShapeTree().equals(PROJECT_TREE)) {
                InheritableDataGrant inheritableDataGrant = (InheritableDataGrant) dataGrant;
                assertEquals(3, inheritableDataGrant.getInheritingGrants().size());
            }
        }
    }

}

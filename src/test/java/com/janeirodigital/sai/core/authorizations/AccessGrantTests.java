package com.janeirodigital.sai.core.authorizations;

import com.janeirodigital.mockwebserver.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.data.*;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpException;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.shapetrees.core.contentloaders.DocumentLoaderManager;
import com.janeirodigital.shapetrees.core.contentloaders.HttpExternalDocumentLoader;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.mockwebserver.DispatcherHelper.*;
import static com.janeirodigital.mockwebserver.MockWebServerHelper.toMockUri;
import static com.janeirodigital.sai.core.http.UrlUtils.uriToUrl;
import static com.janeirodigital.sai.core.vocabularies.AclVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.SCOPE_ALL_FROM_REGISTRY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AccessGrantTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static URI ALICE_ID;
    private static URI BOB_ID;
    private static URI JARVIS_ID;
    private static URI PROJECTRON_ID;
    private static URI PROJECTRON_NEED_GROUP;
    private static URI PROJECTS_DATA_REGISTRATION, MILESTONES_DATA_REGISTRATION, ISSUES_DATA_REGISTRATION, TASKS_DATA_REGISTRATION;
    private static URI PROJECT_1, PROJECT_2, PROJECT_3;
    private static URI PROJECT_1_MILESTONE_1, PROJECT_1_MILESTONE_2, PROJECT_2_MILESTONE_3;
    private static URI PROJECT_1_TASK_1, PROJECT_1_TASK_2, PROJECT_1_ISSUE_1;
    private static URI PROJECT_TREE, MILESTONE_TREE, ISSUE_TREE, TASK_TREE, MISSING_TREE;
    private static URI PROJECTRON_PROJECT_NEED, PROJECTRON_MILESTONE_NEED, PROJECTRON_ISSUE_NEED, PROJECTRON_TASK_NEED;
    private static OffsetDateTime GRANT_TIME;
    private static List<URI> ALL_DATA_GRANT_URIS;
    private static List<RDFNode> ACCESS_MODES, CREATOR_ACCESS_MODES;

    @BeforeAll
    static void beforeAll() throws SaiException, SaiHttpNotFoundException, SaiHttpException {
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
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-project-missing", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-project-missing-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-project-badscope", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-project-badscope-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-project-milestone-missing", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-project-milestone-missing-ttl");

        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-milestone", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-milestone-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-milestone-missing", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-milestone-missing-ttl");
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
        mockOnGet(dispatcher, "/personal/data/projects-milestone-missing/", "data/alice/personal-data-registration-projects-milestone-missing-ttl");
        mockOnGet(dispatcher, "/personal/data/projects-missing/", "http/404");
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
        mockOnGet(dispatcher, "/personal/data/projects/p20-milestone-missing", "data/alice/personal-data-projects-p20-milestone-missing-ttl");

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
        ALICE_ID = URI.create("https://alice.example/id");
        BOB_ID = URI.create("https://bob.example/id");
        JARVIS_ID = URI.create("https://jarvis.example/id");
        PROJECTRON_ID = URI.create("https://projectron.example/id");
        GRANT_TIME = OffsetDateTime.parse("2020-09-05T06:15:01Z", DateTimeFormatter.ISO_DATE_TIME);
        PROJECTRON_NEED_GROUP = URI.create("https://projectron.example/#d8219b1f");
        PROJECTRON_PROJECT_NEED = URI.create("https://projectron.example/#ac54ff1e");
        PROJECTRON_MILESTONE_NEED = URI.create("https://projectron.example/#bd66ee2b");
        PROJECTRON_ISSUE_NEED = URI.create("https://projectron.example/#aa123a1b");
        PROJECTRON_TASK_NEED = URI.create("https://projectron.example/#ce22cc1a");
        ALL_DATA_GRANT_URIS = Arrays.asList(toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project"),
                                            toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-milestone"),
                                            toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-issue"),
                                            toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-task"),
                                            toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-work-project"),
                                            toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-work-milestone"),
                                            toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-work-issue"),
                                            toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-work-task"),
                                            toMockUri(server, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-project"),
                                            toMockUri(server, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-milestone"),
                                            toMockUri(server, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-issue"),
                                            toMockUri(server, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-task"));
        PROJECTS_DATA_REGISTRATION = toMockUri(server, "/personal/data/projects/");
        MILESTONES_DATA_REGISTRATION = toMockUri(server, "/personal/data/milestones/");
        ISSUES_DATA_REGISTRATION = toMockUri(server, "/personal/data/issues/");
        TASKS_DATA_REGISTRATION = toMockUri(server, "/personal/data/tasks/");
        PROJECT_1 = toMockUri(server, "/personal/data/projects/p1");
        PROJECT_2 = toMockUri(server, "/personal/data/projects/p2");
        PROJECT_3 = toMockUri(server, "/personal/data/projects/p3");
        PROJECT_1_MILESTONE_1 = toMockUri(server, "/personal/data/milestones/p1m1");
        PROJECT_1_MILESTONE_2 = toMockUri(server, "/personal/data/milestones/p1m2");
        PROJECT_2_MILESTONE_3 = toMockUri(server, "/personal/data/milestones/p2m3");
        PROJECT_1_TASK_1 = toMockUri(server, "/personal/data/tasks/p1t1");
        PROJECT_1_TASK_2 = toMockUri(server, "/personal/data/tasks/p1t2");
        PROJECT_1_ISSUE_1 = toMockUri(server, "/personal/data/issues/p1i1");
        PROJECT_TREE = toMockUri(server, "/shapetrees/pm#ProjectTree");
        MILESTONE_TREE = toMockUri(server, "/shapetrees/pm#MilestoneTree");
        ISSUE_TREE = toMockUri(server, "/shapetrees/pm#IssueTree");
        TASK_TREE = toMockUri(server, "/shapetrees/pm#TaskTree");
        MISSING_TREE = toMockUri(server, "/shapetrees/pm#MissingTree");
        ACCESS_MODES = Arrays.asList(ACL_READ, ACL_CREATE);
        CREATOR_ACCESS_MODES = Arrays.asList(ACL_UPDATE, ACL_DELETE);
         
    }

    @Test
    @DisplayName("Create new access grant and linked data grants - scope: all")
    void createAccessGrantScopeAll() throws SaiException {
        URI accessUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant");
        URI projectUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        URI milestoneUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-milestone");
        URI issueUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-issue");
        URI taskUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-task");
        
        DataGrant.Builder projectBuilder = new DataGrant.Builder(projectUri, saiSession);
        DataGrant projectGrant = projectBuilder.setDataOwner(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                                                   .setDataRegistration(PROJECTS_DATA_REGISTRATION).setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                                                   .setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY).setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        DataGrant.Builder milestoneBuilder = new DataGrant.Builder(milestoneUri, saiSession);
        DataGrant milestoneGrant = milestoneBuilder.setDataOwner(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(MILESTONES_DATA_REGISTRATION).setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY).setAccessNeed(PROJECTRON_MILESTONE_NEED).build();

        DataGrant.Builder issueBuilder = new DataGrant.Builder(issueUri, saiSession);
        DataGrant issueGrant = issueBuilder.setDataOwner(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(ISSUES_DATA_REGISTRATION).setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY).setAccessNeed(PROJECTRON_ISSUE_NEED).build();

        DataGrant.Builder taskBuilder = new DataGrant.Builder(taskUri, saiSession);
        DataGrant taskGrant = taskBuilder.setDataOwner(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(TASKS_DATA_REGISTRATION).setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY).setAccessNeed(PROJECTRON_TASK_NEED).build();

        List<DataGrant> dataGrants = Arrays.asList(projectGrant, milestoneGrant, issueGrant, taskGrant);

        AccessGrant.Builder accessBuilder = new AccessGrant.Builder(accessUri, saiSession);
        AccessGrant accessGrant = accessBuilder.setGrantedBy(ALICE_ID).setGrantedAt(GRANT_TIME)
                                                   .setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                                                   .setDataGrants(dataGrants).build();
        assertDoesNotThrow(() -> accessGrant.create());
    }

    @Test
    @DisplayName("Create new access grant and linked data grants - only required fields")
    void createAccessGrantRequiredOnly() throws SaiException {
        URI accessUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant");
        URI projectUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        URI milestoneUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-milestone");

        DataGrant.Builder projectBuilder = new DataGrant.Builder(projectUri, saiSession);
        DataGrant projectGrant = projectBuilder.setDataOwner(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(PROJECTS_DATA_REGISTRATION).setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY).setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        DataGrant.Builder milestoneBuilder = new DataGrant.Builder(milestoneUri, saiSession);
        DataGrant milestoneGrant = milestoneBuilder.setDataOwner(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(MILESTONES_DATA_REGISTRATION).setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY).setAccessNeed(PROJECTRON_MILESTONE_NEED).build();

        List<DataGrant> dataGrants = Arrays.asList(projectGrant, milestoneGrant);

        AccessGrant.Builder accessBuilder = new AccessGrant.Builder(accessUri, saiSession);
        AccessGrant accessGrant = accessBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataGrants(dataGrants).build();
        assertDoesNotThrow(() -> accessGrant.create());
    }

    @Test
    @DisplayName("Get an access grant and linked data grants - scope: all")
    void getAccessGrant() throws SaiHttpNotFoundException, SaiException {
        URI uri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant");
        AccessGrant accessGrant = AccessGrant.get(uri, saiSession);
        checkAccessGrant(accessGrant);
    }

    @Test
    @DisplayName("Reload an access grant and linked data grants - scope: all")
    void reloadAccessGrant() throws SaiHttpNotFoundException, SaiException {
        URI uri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant");
        AccessGrant accessGrant = AccessGrant.get(uri, saiSession);
        AccessGrant reloaded = accessGrant.reload();
        checkAccessGrant(reloaded);
    }

    @Test
    @DisplayName("Get a readable access grant and linked data grants - scope: all")
    void getReadableAccessGrant() throws SaiHttpNotFoundException, SaiException {
        URI uri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant");
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(uri, saiSession);
        checkReadableAccessGrant(accessGrant);
    }

    @Test
    @DisplayName("Fail to get readable data grant - invalid scope")
    void createReadableDataGrant() throws SaiException {
        URI projectUri = toMockUri(server, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-project-badscope");
        assertThrows(SaiException.class, () -> ReadableDataGrant.get(projectUri, saiSession));
    }

    @Test
    @DisplayName("Reload a readable access grant and linked data grants - scope: all")
    void reloadReadableAccessGrant() throws SaiHttpNotFoundException, SaiException {
        URI uri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant");
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(uri, saiSession);
        ReadableAccessGrant reloaded = accessGrant.reload();
        checkReadableAccessGrant(accessGrant);
    }

    @Test
    @DisplayName("Fail to get access grant - missing required fields")
    void failToGetAccessGrantRequired() {
        URI uri = toMockUri(server, "/missing-fields/all-1-agents/all-1-projectron/all-1-grant");
        assertThrows(SaiException.class, () -> AccessGrant.get(uri, saiSession));
    }

    @Test
    @DisplayName("Fail to get readable access grant - missing required fields")
    void failToGetReadableAccessGrantRequired() {
        URI uri = toMockUri(server, "/missing-fields/all-1-agents/all-1-projectron/all-1-grant");
        assertThrows(SaiException.class, () -> ReadableAccessGrant.get(uri, saiSession));
    }

    @Test
    @DisplayName("Get readable access grant and associated readable data grants - Scope: All")
    void testGetAccessGrantAll() throws SaiHttpNotFoundException, SaiException {
        URI grantUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant");
        URI bobProjectGrantUri = toMockUri(server, "/all-1-bob-agents/all-1-alice/all-1-grant-project");
        URI delegatedProjectUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-project");
        List<URI> aliceProjectUris = Arrays.asList(toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project"),
                toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-work-project"));
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(grantUri, saiSession);
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
                if (specificGrant.isDelegated()) { assertEquals(bobProjectGrantUri, specificGrant.getDelegationOf()); }
            } else if (readableDataGrant instanceof InheritedDataGrant) {
                InheritedDataGrant inheritedGrant = (InheritedDataGrant) readableDataGrant;
                if (inheritedGrant.isDelegated()) { assertEquals(delegatedProjectUri, inheritedGrant.getInheritsFrom());
                } else { assertTrue(aliceProjectUris.contains(inheritedGrant.getInheritsFrom())); }
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
    void testGetAccessGrantAllFromRegistry() throws SaiHttpNotFoundException, SaiException {
        URI grantUri = toMockUri(server, "/registry-1-agents/registry-1-projectron/registry-1-grant");
        URI aliceProjectUri = toMockUri(server, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-project");
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(grantUri, saiSession);
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
                assertEquals(aliceProjectUri, inheritedGrant.getInheritsFrom());
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
    void testGetAccessGrantSelectedFromRegistry() throws SaiHttpNotFoundException, SaiException {
        URI grantUri = toMockUri(server, "/selected-1-agents/selected-1-projectron/selected-1-grant");
        URI aliceProjectUri = toMockUri(server, "/selected-1-agents/selected-1-projectron/selected-1-grant-personal-project");
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(grantUri, saiSession);
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
                assertEquals(aliceProjectUri, inheritedGrant.getInheritsFrom());
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
    void testGetAccessGrantAllFromAgent() throws SaiHttpNotFoundException, SaiException {
        URI grantUri = toMockUri(server, "/agent-1-agents/agent-1-projectron/agent-1-grant");
        URI bobProjectGrantUri = toMockUri(server, "/agent-1-bob-agents/agent-1-alice/agent-1-grant-project");
        URI delegatedProjectUri = toMockUri(server, "/agent-1-agents/agent-1-projectron/agent-1-delegated-grant-bob-project");
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(grantUri, saiSession);
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
                assertEquals(bobProjectGrantUri, specificGrant.getDelegationOf());
            } else if (readableDataGrant instanceof InheritedDataGrant) {
                InheritedDataGrant inheritedGrant = (InheritedDataGrant) readableDataGrant;
                assertEquals(delegatedProjectUri, inheritedGrant.getInheritsFrom());
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
    void testGetDataInstancesAllFromRegistry() throws SaiHttpNotFoundException, SaiException {
        URI grantUri = toMockUri(server, "/registry-1-agents/registry-1-projectron/registry-1-grant");
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(grantUri, saiSession);
        // Grant provides access to three projects in Alice's personal data registry (/personal/data/projects/)
        List<ReadableDataGrant> projectGrants = accessGrant.findDataGrants(ALICE_ID, PROJECT_TREE);
        AllFromRegistryDataGrant projectGrant = (AllFromRegistryDataGrant) projectGrants.get(0);
        assertEquals(3, projectGrant.getDataInstances().size());

        List<TestableProject> projects = TestableProject.getAccessible(projectGrant, saiSession);

        for (TestableProject project : projects) {
            if (project.getUri().equals(PROJECT_1)) { checkProject1(project);
            } else if (project.getUri().equals(PROJECT_2)) { checkProject2(project);
            } else if (project.getUri().equals(PROJECT_3)) { checkProject3(project);
            }
        }

        List<ReadableDataGrant> milestoneGrants = accessGrant.findDataGrants(ALICE_ID, MILESTONE_TREE);
        InheritedDataGrant milestoneGrant = (InheritedDataGrant) milestoneGrants.get(0);
        assertEquals(3, projectGrant.getDataInstances().size());

        List<TestableMilestone> milestones = TestableMilestone.getAccessible(milestoneGrant, saiSession);

        for (TestableMilestone milestone : milestones) {
            if (milestone.getUri().equals(PROJECT_1_MILESTONE_1)) { checkMilestone1(milestone); }
        }
    }

    @Test
    @DisplayName("Fail to get data instances from readable data grant scoped with AllFromRegistry - instance missing")
    void failToGetDataInstancesAllFromRegistryMissing() throws SaiHttpNotFoundException, SaiException {
        URI grantUri = toMockUri(server, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-project-missing");
        ReadableDataGrant dataGrant = ReadableDataGrant.get(grantUri, saiSession);
        AllFromRegistryDataGrant projectGrant = (AllFromRegistryDataGrant) dataGrant;
        assertThrows(SaiException.class, () -> projectGrant.getDataInstances());
    }

    @Test
    @DisplayName("Get data instances from readable data grant - Scope: Inherited")
    void testGetDataInstancesAllFromRegistryInherited() throws SaiHttpNotFoundException, SaiException {
        URI grantUri = toMockUri(server, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-milestone");
        List<URI> parents = Arrays.asList(PROJECT_1, PROJECT_2);
        ReadableDataGrant dataGrant = ReadableDataGrant.get(grantUri, saiSession);
        InheritedDataGrant milestoneGrant = (InheritedDataGrant) dataGrant;
        assertEquals(3, milestoneGrant.getDataInstances().size());
        for (DataInstance dataInstance : milestoneGrant.getDataInstances()) {
            assertEquals(milestoneGrant, dataInstance.getDataGrant());
            assertEquals(uriToUrl(MILESTONE_TREE), dataInstance.getShapeTree().getId());
            if (dataInstance.getParent() != null) assertTrue(parents.contains(dataInstance.getParent().getUri()));
        }
    }

    @Test
    @DisplayName("Fail to get data instances from readable data grant scoped with Inherited - instance missing")
    void failToGetDataInstancesInheritedMissing() throws SaiHttpNotFoundException, SaiException {
        URI projectGrantUri = toMockUri(server, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-project-missing");
        URI milestoneGrantUri = toMockUri(server, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-milestone-missing");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(projectGrantUri, saiSession);

        ReadableDataGrant dataGrant = ReadableDataGrant.get(milestoneGrantUri, saiSession);
        InheritedDataGrant milestoneGrant = (InheritedDataGrant) dataGrant;
        assertThrows(SaiException.class, () -> milestoneGrant.getDataInstances());
    }

    @Test
    @DisplayName("Create a new data instance from readable data grant")
    void createNewDataInstanceAllFromRegistry() throws SaiHttpNotFoundException, SaiException {
        URI grantUri = toMockUri(server, "/registry-1-agents/registry-1-projectron/registry-1-grant");
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(grantUri, saiSession);
        List<ReadableDataGrant> projectGrants = accessGrant.findDataGrants(ALICE_ID, PROJECT_TREE);
        AllFromRegistryDataGrant projectGrant = (AllFromRegistryDataGrant) projectGrants.get(0);
        assertTrue(projectGrant.canCreate());
        URI projectUri = DataInstance.generateUri(projectGrant, "new-project");
        TestableProject project = new TestableProject.Builder(projectUri, saiSession)
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
        assertEquals(uriToUrl(PROJECT_TREE), project.getShapeTree().getId());
        assertDoesNotThrow(() -> project.update());
    }

    @Test
    @DisplayName("Create a new child data instance from readable data grant")
    void createNewDataInstanceInherited() throws SaiHttpNotFoundException, SaiException {
        URI grantUri = toMockUri(server, "/registry-1-agents/registry-1-projectron/registry-1-grant");
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(grantUri, saiSession);
        List<ReadableDataGrant> projectGrants = accessGrant.findDataGrants(ALICE_ID, PROJECT_TREE);
        AllFromRegistryDataGrant projectGrant = (AllFromRegistryDataGrant) projectGrants.get(0);

        List<TestableProject> projects = TestableProject.getAccessible(projectGrant, saiSession);
        TestableProject project = projects.get(0);  // grab any project from the accessible projects
        InheritedDataGrant milestoneGrant = (InheritedDataGrant) project.findChildGrant(MILESTONE_TREE);

        URI milestoneUri = TestableMilestone.generateUri(milestoneGrant, "new-milestone");
        TestableMilestone milestone = new TestableMilestone.Builder(milestoneUri, saiSession)
                                                           .setDataGrant(milestoneGrant)
                                                           .setParent(project)
                                                           .setName("New milestone")
                                                           .setDescription("New milestone instance")
                                                           .build();
        assertEquals(project, milestone.getParent());
        assertEquals(uriToUrl(MILESTONE_TREE), milestone.getShapeTree().getId());
        assertDoesNotThrow(() -> milestone.update());
        assertTrue(project.getChildReferences(MILESTONE_TREE).contains(milestone.getUri()));
        assertThrows(SaiException.class, () -> milestone.findChildGrant(ISSUE_TREE));
    }

    @Test
    @DisplayName("Delete a child data instance from a readable data grant")
    void deleteChildDataInstance() throws SaiHttpNotFoundException, SaiException {
        URI grantUri = toMockUri(server, "/registry-1-agents/registry-1-projectron/registry-1-grant");
        ReadableAccessGrant accessGrant = ReadableAccessGrant.get(grantUri, saiSession);
        List<ReadableDataGrant> projectGrants = accessGrant.findDataGrants(ALICE_ID, PROJECT_TREE);
        AllFromRegistryDataGrant projectGrant = (AllFromRegistryDataGrant) projectGrants.get(0);

        List<TestableProject> projects = TestableProject.getAccessible(projectGrant, saiSession);

        for (TestableProject project : projects) {
            if (project.getUri().equals(PROJECT_1)) {
                for (TestableIssue issue : project.getIssues(ISSUE_TREE)) {
                    if (issue.getUri().equals(PROJECT_1_ISSUE_1)) { assertDoesNotThrow(() -> issue.delete()); }
                }
            }
        }
    }

    private void checkProject1(TestableProject project) throws SaiException {
        List<URI> P1_MILESTONES = Arrays.asList(PROJECT_1_MILESTONE_1, PROJECT_1_MILESTONE_2);
        List<URI> P1_TASKS = Arrays.asList(PROJECT_1_TASK_1, PROJECT_1_TASK_2);
        List<URI> P1_ISSUES = Arrays.asList(PROJECT_1_ISSUE_1);
        // Ensure that the URIs are referencing the right instances
        assertEquals(uriToUrl(PROJECT_TREE), project.getShapeTree().getId());
        assertTrue(P1_MILESTONES.containsAll(project.getChildReferences(MILESTONE_TREE)));
        assertTrue(P1_TASKS.containsAll(project.getChildReferences(TASK_TREE)));
        assertTrue(P1_ISSUES.containsAll(project.getChildReferences(ISSUE_TREE)));

        for (TestableMilestone milestone : project.getMilestones(MILESTONE_TREE)) {
            assertTrue(P1_MILESTONES.contains(milestone.getUri()));
            assertEquals(uriToUrl(MILESTONE_TREE), milestone.getShapeTree().getId());
            assertEquals(project, milestone.getParent());
        }

        for (TestableTask task : project.getTasks(TASK_TREE)) {
            assertTrue(P1_TASKS.contains(task.getUri()));
            assertEquals(uriToUrl(TASK_TREE), task.getShapeTree().getId());
            assertEquals(project, task.getParent());
        }

        for (TestableIssue issue : project.getIssues(ISSUE_TREE)) {
            assertTrue(P1_ISSUES.contains(issue.getUri()));
            assertEquals(uriToUrl(ISSUE_TREE), issue.getShapeTree().getId());
            assertEquals(project, issue.getParent());
        }
    }

    private void checkProject2(TestableProject project) throws SaiException {
        List<URI> P2_MILESTONES = Arrays.asList(PROJECT_2_MILESTONE_3);
        assertEquals(uriToUrl(PROJECT_TREE), project.getShapeTree().getId());
        assertTrue(P2_MILESTONES.containsAll(project.getChildReferences(MILESTONE_TREE)));
        for (TestableMilestone milestone : project.getMilestones(MILESTONE_TREE)) {
            assertTrue(P2_MILESTONES.contains(milestone.getUri()));
            assertEquals(uriToUrl(MILESTONE_TREE), milestone.getShapeTree().getId());
            assertEquals(project, milestone.getParent());
        }
    }

    private void checkProject3(TestableProject project) throws SaiException {
        assertEquals(uriToUrl(PROJECT_TREE), project.getShapeTree().getId());
        assertTrue(project.getChildReferences(MILESTONE_TREE).isEmpty());
        assertTrue(project.getChildInstances(MILESTONE_TREE).isEmpty());
        assertTrue(project.getMilestones(MILESTONE_TREE).isEmpty());
        assertTrue(project.getIssues(ISSUE_TREE).isEmpty());
        assertTrue(project.getTasks(TASK_TREE).isEmpty());
    }

    private void checkMilestone1(TestableMilestone milestone) throws SaiException {
        assertEquals(uriToUrl(MILESTONE_TREE), milestone.getShapeTree().getId());
        milestone.getParent().getUri().equals(PROJECT_1);
    }

    private void checkAccessGrant(AccessGrant accessGrant) {
        assertNotNull(accessGrant);
        assertEquals(ALICE_ID, accessGrant.getGrantedBy());
        assertEquals(PROJECTRON_ID, accessGrant.getGrantee());
        assertEquals(GRANT_TIME, accessGrant.getGrantedAt());
        assertEquals(PROJECTRON_NEED_GROUP, accessGrant.getAccessNeedGroup());
        for (DataGrant dataGrant : accessGrant.getDataGrants()) {
            assertTrue(ALL_DATA_GRANT_URIS.contains(dataGrant.getUri()));
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
            assertTrue(ALL_DATA_GRANT_URIS.contains(dataGrant.getUri()));
            if (dataGrant.getRegisteredShapeTree().equals(PROJECT_TREE)) {
                InheritableDataGrant inheritableDataGrant = (InheritableDataGrant) dataGrant;
                assertEquals(3, inheritableDataGrant.getInheritingGrants().size());
            }
        }
    }

}

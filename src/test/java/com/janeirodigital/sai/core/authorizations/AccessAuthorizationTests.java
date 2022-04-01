package com.janeirodigital.sai.core.authorizations;

import com.janeirodigital.mockwebserver.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.agents.AgentRegistry;
import com.janeirodigital.sai.core.agents.ApplicationRegistration;
import com.janeirodigital.sai.core.data.DataRegistry;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.mockwebserver.DispatcherHelper.mockOnGet;
import static com.janeirodigital.mockwebserver.DispatcherHelper.mockOnPut;
import static com.janeirodigital.mockwebserver.MockWebServerHelper.toMockUri;
import static com.janeirodigital.sai.core.vocabularies.AclVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
class AccessAuthorizationTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;
    private static URI ALICE_ID, BOB_ID, CAROL_ID, JUAN_ID, TARA_ID;
    private static URI JARVIS_ID;
    private static URI PROJECTRON_ID;
    private static URI PROJECTRON_NEED_GROUP;
    private static URI PROJECT_TREE, MILESTONE_TREE, ISSUE_TREE, TASK_TREE, CALENDAR_TREE, APPOINTMENT_TREE;
    private static URI PROJECTRON_PROJECT_NEED, PROJECTRON_MILESTONE_NEED, PROJECTRON_ISSUE_NEED, PROJECTRON_TASK_NEED;
    private static URI PROJECTRON_CALENDAR_NEED, PROJECTRON_APPOINTMENT_NEED;
    private static OffsetDateTime GRANT_TIME;
    private static List<URI> ALL_DATA_AUTHORIZATION_URIS;
    private static List<RDFNode> ACCESS_MODES, CREATOR_ACCESS_MODES, READ_MODES;

    @BeforeAll
    static void beforeAll() throws SaiException {
        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));
        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();

        // Scope: interop:All - GET all necessary resources across registries (used in basic crud tests as well as grant generation)
        mockOnGet(dispatcher, "/authorization/all-1", "authorization/all/all-1-ttl");
        mockOnGet(dispatcher, "/missing-fields/authorization/all-1", "authorization/all/all-1-missing-fields-ttl");
        mockOnPut(dispatcher, "/authorization/all-1", "http/201");
        mockOnGet(dispatcher, "/authorization/all-1-project", "authorization/all/all-1-project-ttl");
        mockOnPut(dispatcher, "/authorization/all-1-project", "http/201");
        mockOnGet(dispatcher, "/authorization/all-1-milestone", "authorization/all/all-1-milestone-ttl");
        mockOnPut(dispatcher, "/authorization/all-1-milestone", "http/201");
        mockOnGet(dispatcher, "/authorization/all-1-task", "authorization/all/all-1-task-ttl");
        mockOnPut(dispatcher, "/authorization/all-1-task", "http/201");
        mockOnGet(dispatcher, "/authorization/all-1-issue", "authorization/all/all-1-issue-ttl");
        mockOnPut(dispatcher, "/authorization/all-1-issue", "http/201");
        mockOnGet(dispatcher, "/all-1-agents/", "agents/alice/projectron-all/all-1-agent-registry-ttl");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/", "agents/alice/projectron-all/all-1-projectron-registration-ttl");
        mockOnGet(dispatcher, "/all-1-agents/all-1-bob/", "agents/alice/projectron-all/all-1-bob-registration-ttl");
        mockOnGet(dispatcher, "/all-1-bob-agents/all-1-alice/", "agents/alice/projectron-all/all-1-bob-alice-registration-ttl");
        mockOnGet(dispatcher, "/all-1-bob-agents/all-1-alice/all-1-grant", "agents/alice/projectron-all/all-1-bob-grant-ttl");
        mockOnGet(dispatcher, "/all-1-bob-agents/all-1-alice/all-1-grant-project", "agents/alice/projectron-all/all-1-bob-grant-project-ttl");
        mockOnGet(dispatcher, "/all-1-bob-agents/all-1-alice/all-1-grant-milestone", "agents/alice/projectron-all/all-1-bob-grant-milestone-ttl");
        mockOnGet(dispatcher, "/all-1-bob-agents/all-1-alice/all-1-grant-issue", "agents/alice/projectron-all/all-1-bob-grant-issue-ttl");
        mockOnGet(dispatcher, "/all-1-bob-agents/all-1-alice/all-1-grant-task", "agents/alice/projectron-all/all-1-bob-grant-task-ttl");
        // // Known good baseline grants used to compare against generated grants for interop:All
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant", "agents/alice/projectron-all/all-1-grant-ttl");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-personal-project", "agents/alice/projectron-all/all-1-grant-personal-project-ttl");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-personal-milestone", "agents/alice/projectron-all/all-1-grant-personal-milestone-ttl");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-personal-issue", "agents/alice/projectron-all/all-1-grant-personal-issue-ttl");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-personal-task", "agents/alice/projectron-all/all-1-grant-personal-task-ttl");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-work-project", "agents/alice/projectron-all/all-1-grant-work-project-ttl");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-work-milestone", "agents/alice/projectron-all/all-1-grant-work-milestone-ttl");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-work-issue", "agents/alice/projectron-all/all-1-grant-work-issue-ttl");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-work-task", "agents/alice/projectron-all/all-1-grant-work-task-ttl");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-project", "agents/alice/projectron-all/all-1-delegated-grant-bob-project-ttl");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-milestone", "agents/alice/projectron-all/all-1-delegated-grant-bob-milestone-ttl");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-issue", "agents/alice/projectron-all/all-1-delegated-grant-bob-issue-ttl");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-delegated-grant-bob-task", "agents/alice/projectron-all/all-1-delegated-grant-bob-task-ttl");

        // Scope: interop:AllFromRegistry - GET all necessary resources across registries (for testing grant generation)
        mockOnGet(dispatcher, "/authorization/registry-1", "authorization/all-from-registry/registry-1-ttl");
        mockOnGet(dispatcher, "/authorization/registry-1-project", "authorization/all-from-registry/registry-1-project-ttl");
        mockOnGet(dispatcher, "/authorization/registry-1-milestone", "authorization/all-from-registry/registry-1-milestone-ttl");
        mockOnGet(dispatcher, "/authorization/registry-1-task", "authorization/all-from-registry/registry-1-task-ttl");
        mockOnGet(dispatcher, "/authorization/registry-1-issue", "authorization/all-from-registry/registry-1-issue-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/", "agents/alice/projectron-all-from-registry/registry-1-agent-registry-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/", "agents/alice/projectron-all-from-registry/registry-1-projectron-registration-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant", "agents/alice/projectron-all-from-registry/registry-1-grant-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-project", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-project-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-milestone", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-milestone-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-issue", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-issue-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-task", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-task-ttl");

        // Scope: interop:SelectedFromRegistry - GET all necessary resources across registries (for testing grant generation)
        mockOnGet(dispatcher, "/authorization/selected-1", "authorization/selected-from-registry/selected-1-ttl");
        mockOnGet(dispatcher, "/authorization/selected-1-project", "authorization/selected-from-registry/selected-1-project-ttl");
        mockOnGet(dispatcher, "/authorization/selected-1-milestone", "authorization/selected-from-registry/selected-1-milestone-ttl");
        mockOnGet(dispatcher, "/authorization/selected-1-task", "authorization/selected-from-registry/selected-1-task-ttl");
        mockOnGet(dispatcher, "/authorization/selected-1-issue", "authorization/selected-from-registry/selected-1-issue-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/", "agents/alice/projectron-selected-from-registry/selected-1-agent-registry-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/selected-1-projectron/", "agents/alice/projectron-selected-from-registry/selected-1-projectron-registration-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/selected-1-projectron/selected-1-grant", "agents/alice/projectron-selected-from-registry/selected-1-grant-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/selected-1-projectron/selected-1-grant-personal-project", "agents/alice/projectron-selected-from-registry/selected-1-grant-personal-project-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/selected-1-projectron/selected-1-grant-personal-milestone", "agents/alice/projectron-selected-from-registry/selected-1-grant-personal-milestone-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/selected-1-projectron/selected-1-grant-personal-issue", "agents/alice/projectron-selected-from-registry/selected-1-grant-personal-issue-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/selected-1-projectron/selected-1-grant-personal-task", "agents/alice/projectron-selected-from-registry/selected-1-grant-personal-task-ttl");
        
        // Scope: interop:AllFromAgent - GET all necessary resources across registries (for testing grant generation)
        mockOnGet(dispatcher, "/authorization/agent-1", "authorization/all-from-agent/agent-1-ttl");
        mockOnGet(dispatcher, "/authorization/agent-1-project", "authorization/all-from-agent/agent-1-project-ttl");
        mockOnGet(dispatcher, "/authorization/agent-1-milestone", "authorization/all-from-agent/agent-1-milestone-ttl");
        mockOnGet(dispatcher, "/authorization/agent-1-task", "authorization/all-from-agent/agent-1-task-ttl");
        mockOnGet(dispatcher, "/authorization/agent-1-issue", "authorization/all-from-agent/agent-1-issue-ttl");
        mockOnGet(dispatcher, "/agent-1-agents/", "agents/alice/projectron-all-from-agent/agent-1-agent-registry-ttl");
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

        // MISC TEST SCENARIOS - Useful fixtures for various success and failure test scenarios related to grant generation
        mockOnGet(dispatcher, "/scenario-agents/", "authorization/scenario/scenario-agent-registry-ttl");
        mockOnGet(dispatcher, "/scenario-agents/scenario-projectron/", "authorization/scenario/scenario-projectron-registration-ttl");
        mockOnGet(dispatcher, "/delegated-agents/", "authorization/scenario/delegated-agent-registry-ttl");
        mockOnGet(dispatcher, "/delegated-agents/delegated-projectron/", "authorization/scenario/delegated-projectron-registration-ttl");
        mockOnGet(dispatcher, "/delegated-agents/delegated-performchart/", "authorization/scenario/delegated-performchart-registration-ttl");
        mockOnGet(dispatcher, "/delegated-agents/delegated-bob/", "authorization/scenario/delegated-bob-registration-ttl");
        mockOnGet(dispatcher, "/delegated-agents/delegated-juan/", "authorization/scenario/delegated-juan-registration-ttl");
        mockOnGet(dispatcher, "/delegated-agents/delegated-carol/", "authorization/scenario/delegated-carol-registration-ttl");
        mockOnGet(dispatcher, "/delegated-agents/delegated-tara/", "authorization/scenario/delegated-tara-registration-ttl");
        mockOnGet(dispatcher, "/delegated-bob-agents/delegated-alice/", "authorization/scenario/delegated-bob-alice-registration-ttl");
        mockOnGet(dispatcher, "/delegated-bob-agents/delegated-alice/delegated-grant", "authorization/scenario/delegated-bob-alice-grant-ttl");
        mockOnGet(dispatcher, "/delegated-bob-agents/delegated-alice/delegated-grant-project", "authorization/scenario/delegated-bob-alice-grant-project-ttl");
        mockOnGet(dispatcher, "/delegated-bob-agents/delegated-alice/delegated-grant-milestone", "authorization/scenario/delegated-bob-alice-grant-milestone-ttl");
        mockOnGet(dispatcher, "/delegated-tara-agents/delegated-alice/", "authorization/scenario/delegated-tara-alice-registration-ttl");

        // Get Alice and Bob's data registries - doesn't change across use cases
        mockOnGet(dispatcher, "/personal/data/", "data/alice/personal-data-registry-ttl");
        mockOnGet(dispatcher, "/personal/data/projects/", "data/alice/personal-data-registration-projects-ttl");
        mockOnGet(dispatcher, "/personal/data/milestones/", "data/alice/personal-data-registration-milestones-ttl");
        mockOnGet(dispatcher, "/personal/data/issues/", "data/alice/personal-data-registration-issues-ttl");
        mockOnGet(dispatcher, "/personal/data/tasks/", "data/alice/personal-data-registration-tasks-ttl");
        mockOnGet(dispatcher, "/personal/data/calendars/", "data/alice/personal-data-registration-calendars-ttl");
        mockOnGet(dispatcher, "/personal/data/appointments/", "data/alice/personal-data-registration-appointments-ttl");
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
        JARVIS_ID = URI.create("https://jarvis.example/id");
        PROJECTRON_ID = URI.create("https://projectron.example/id");
        BOB_ID = URI.create("https://bob.example/id");
        CAROL_ID = URI.create("https://carol.example/id");
        JUAN_ID = URI.create("https://juan.example/id");
        TARA_ID = URI.create("https://tara.example/id");
        GRANT_TIME = OffsetDateTime.parse("2020-09-05T06:15:01Z", DateTimeFormatter.ISO_DATE_TIME);

        PROJECTRON_NEED_GROUP = URI.create("https://projectron.example/#d8219b1f");
        PROJECTRON_PROJECT_NEED = URI.create("https://projectron.example/#ac54ff1e");
        PROJECTRON_MILESTONE_NEED = URI.create("https://projectron.example/#bd66ee2b");
        PROJECTRON_ISSUE_NEED = URI.create("https://projectron.example/#aa123a1b");
        PROJECTRON_TASK_NEED = URI.create("https://projectron.example/#ce22cc1a");
        PROJECTRON_CALENDAR_NEED = URI.create("https://projectron.example/#ba66ff1e");
        PROJECTRON_APPOINTMENT_NEED = URI.create("https://projectron.example/#aa11aa1b");
        ALL_DATA_AUTHORIZATION_URIS = Arrays.asList(toMockUri(server, "/authorization/all-1-project"), toMockUri(server, "/authorization/all-1-milestone"),
                                              toMockUri(server, "/authorization/all-1-issue"), toMockUri(server, "/authorization/all-1-task"));
        PROJECT_TREE = toMockUri(server, "/shapetrees/pm#ProjectTree");
        MILESTONE_TREE = toMockUri(server, "/shapetrees/pm#MilestoneTree");
        ISSUE_TREE = toMockUri(server, "/shapetrees/pm#IssueTree");
        TASK_TREE = toMockUri(server, "/shapetrees/pm#TaskTree");
        CALENDAR_TREE = toMockUri(server, "/shapetrees/pm#CalendarTree");
        APPOINTMENT_TREE = toMockUri(server, "/shapetrees/pm#AppointmentTree");
        READ_MODES = Arrays.asList(ACL_READ);
        ACCESS_MODES = Arrays.asList(ACL_READ, ACL_CREATE);
        CREATOR_ACCESS_MODES = Arrays.asList(ACL_UPDATE, ACL_DELETE);
         
    }

    @Test
    @DisplayName("Create new access authorization and linked data authorizations - scope: all")
    void createAccessAuthorizationScopeAll() throws SaiException {
        URI accessUri = toMockUri(server, "/authorization/all-1");
        URI projectUri = toMockUri(server, "/authorization/all-1-project");
        URI milestoneUri = toMockUri(server, "/authorization/all-1-milestone");
        URI issueUri = toMockUri(server, "/authorization/all-1-issue");
        URI taskUri = toMockUri(server, "/authorization/all-1-task");
        
        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectUri, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                                                   .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                                                   .setScopeOfAuthorization(SCOPE_ALL).setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        DataAuthorization.Builder milestoneBuilder = new DataAuthorization.Builder(milestoneUri, saiSession);
        DataAuthorization milestoneAuthorization = milestoneBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(MILESTONE_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfAuthorization(SCOPE_INHERITED).setAccessNeed(PROJECTRON_MILESTONE_NEED).setInheritsFrom(projectAuthorization.getUri()).build();

        DataAuthorization.Builder issueBuilder = new DataAuthorization.Builder(issueUri, saiSession);
        DataAuthorization issueAuthorization = issueBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(ISSUE_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfAuthorization(SCOPE_INHERITED).setAccessNeed(PROJECTRON_ISSUE_NEED).setInheritsFrom(milestoneAuthorization.getUri()).build();

        DataAuthorization.Builder taskBuilder = new DataAuthorization.Builder(taskUri, saiSession);
        DataAuthorization taskAuthorization = taskBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(TASK_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfAuthorization(SCOPE_INHERITED).setAccessNeed(PROJECTRON_TASK_NEED).setInheritsFrom(milestoneAuthorization.getUri()).build();

        List<DataAuthorization> dataAuthorizations = Arrays.asList(projectAuthorization, milestoneAuthorization, issueAuthorization, taskAuthorization);

        AccessAuthorization.Builder accessBuilder = new AccessAuthorization.Builder(accessUri, saiSession);
        AccessAuthorization accessAuthorization = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID).setGrantedAt(GRANT_TIME)
                                                   .setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                                                   .setDataAuthorizations(dataAuthorizations).build();
        assertDoesNotThrow(() -> accessAuthorization.create());
    }

    @Test
    @DisplayName("Generate access grant and associated data grants - Scope: All")
    void testGenerateAccessGrantAll() throws SaiHttpNotFoundException, SaiException {
        // Note that in typical use we wouldn't be getting an existing acccess authorization, but would instead
        // be generating the grants right after generating the authorizations
        URI accessUri = toMockUri(server, "/authorization/all-1");
        URI agentRegistryUri = toMockUri(server, "/all-1-agents/");
        URI personalDataUri = toMockUri(server, "/personal/data/");
        URI workDataUri = toMockUri(server, "/personal/data/");
        URI registrationUri = toMockUri(server, "/all-1-agents/all-1-projectron/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUri, saiSession);
        DataRegistry personalData = DataRegistry.get(personalDataUri, saiSession);
        DataRegistry workData = DataRegistry.get(workDataUri, saiSession);
        AccessAuthorization accessAuthorization = AccessAuthorization.get(accessUri, saiSession);
        AccessGrant accessGrant = accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(personalData, workData));
        checkAccessGrantAll(accessGrant);
    }

    @Test
    @DisplayName("Generate access grant and associated data grants - Scope: AllFromRegistry")
    void testGenerateAccessGrantAllFromRegistry() throws SaiHttpNotFoundException, SaiException {
        // Note that in typical use we wouldn't be getting an existing acccess authorization, but would instead
        // be generating the grants right after generating the authorizations
        URI accessUri = toMockUri(server, "/authorization/registry-1");
        URI agentRegistryUri = toMockUri(server, "/registry-1-agents/");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI registrationUri = toMockUri(server, "/registry-1-agents/registry-1-projectron/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);
        AccessAuthorization accessAuthorization = AccessAuthorization.get(accessUri, saiSession);
        AccessGrant accessGrant = accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        checkAccessGrantAllFromRegistry(accessGrant);
    }

    @Test
    @DisplayName("Generate access grant and associated data grants - Scope: SelectedFromRegistry")
    void testGenerateAccessGrantSelectedFromRegistry() throws SaiHttpNotFoundException, SaiException {
        // Note that in typical use we wouldn't be getting an existing acccess authorization, but would instead
        // be generating the grants right after generating the authorizations
        URI accessUri = toMockUri(server, "/authorization/selected-1");
        URI agentRegistryUri = toMockUri(server, "/selected-1-agents/");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI registrationUri = toMockUri(server, "/selected-1-agents/selected-1-projectron/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);
        AccessAuthorization accessAuthorization = AccessAuthorization.get(accessUri, saiSession);
        AccessGrant accessGrant = accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        checkAccessGrantSelectedFromRegistry(accessGrant);
    }

    @Test
    @DisplayName("Generate access grant and associated data grants - Scope: AllFromAgent")
    void testGenerateAccessGrantAllFromAgent() throws SaiHttpNotFoundException, SaiException {
        // Note that in typical use we wouldn't be getting an existing acccess authorization, but would instead
        // be generating the grants right after generating the authorizations
        URI accessUri = toMockUri(server, "/authorization/agent-1");
        URI agentRegistryUri = toMockUri(server, "/agent-1-agents/");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI registrationUri = toMockUri(server, "/agent-1-agents/agent-1-projectron/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);
        AccessAuthorization accessAuthorization = AccessAuthorization.get(accessUri, saiSession);
        AccessGrant accessGrant = accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        checkAccessGrantAllFromAgent(accessGrant);
    }

    @Test
    @DisplayName("Generate access grant and associated data grants - no matching data registrations")
    void generateDataGrantsNoMatchingRegistrations() throws SaiHttpNotFoundException, SaiException {
        URI accessUri = toMockUri(server, "/authorization/no-matches");
        URI dataAuthorizationUri = toMockUri(server, "/authorization/no-matches-project");
        URI EVENT_TREE = toMockUri(server, "/shapetrees/pm#EventTree");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI agentRegistryUri = toMockUri(server, "/scenario-agents/");
        URI registrationUri = toMockUri(server, "/scenario-agents/scenario-projectron/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);

        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(dataAuthorizationUri, saiSession);
        DataAuthorization eventAuthorization = projectBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(EVENT_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfAuthorization(SCOPE_ALL).setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        AccessAuthorization.Builder accessBuilder = new AccessAuthorization.Builder(accessUri, saiSession);
        AccessAuthorization accessAuthorization = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID).setGrantedAt(GRANT_TIME)
                .setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataAuthorizations(Arrays.asList(eventAuthorization)).build();

        AccessGrant accessGrant = accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        assertTrue(accessGrant.getDataGrants().isEmpty());
    }

    @Test
    @DisplayName("Generate access grant and associated data grants - read-only access modes")
    void generateDataGrantsReadOnly() throws SaiHttpNotFoundException, SaiException {
        URI accessUri = toMockUri(server, "/authorization/read-only");
        URI dataAuthorizationUri = toMockUri(server, "/authorization/read-only-project");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI agentRegistryUri = toMockUri(server, "/scenario-agents/");
        URI registrationUri = toMockUri(server, "/scenario-agents/scenario-projectron/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);

        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(dataAuthorizationUri, saiSession);
        DataAuthorization eventAuthorization = projectBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(READ_MODES).setScopeOfAuthorization(SCOPE_ALL).setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        AccessAuthorization.Builder accessBuilder = new AccessAuthorization.Builder(accessUri, saiSession);
        AccessAuthorization accessAuthorization = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID).setGrantedAt(GRANT_TIME)
                .setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataAuthorizations(Arrays.asList(eventAuthorization)).build();

        AccessGrant accessGrant = accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        assertFalse(accessGrant.getDataGrants().get(0).canCreate());
    }

    @Test
    @DisplayName("Generate access grant and delegated data grants - multiple social agents in registry")
    void generateDelegatedGrantsMultipleSocials() throws SaiHttpNotFoundException, SaiException {
        URI accessUri = toMockUri(server, "/authorization/delegated-agents");
        URI projectAuthorizationUri = toMockUri(server, "/authorization/delegated-project");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI agentRegistryUri = toMockUri(server, "/delegated-agents/");
        URI juanRegistrationUri = toMockUri(server, "/delegated-agents/delegated-juan/");
        URI bobProjectsRegistration = toMockUri(server, "/bob/data/projects/");
        
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);

        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectAuthorizationUri, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(READ_MODES).setScopeOfAuthorization(SCOPE_ALL_FROM_AGENT).setAccessNeed(PROJECTRON_PROJECT_NEED)
                .setDataRegistration(bobProjectsRegistration).build();

        AccessAuthorization.Builder accessBuilder = new AccessAuthorization.Builder(accessUri, saiSession);
        AccessAuthorization accessAuthorization = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataAuthorizations(Arrays.asList(projectAuthorization)).build();

        AccessGrant accessGrant = accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        assertEquals(1, accessGrant.getDataGrants().size());
    }

    @Test
    @DisplayName("Generate access grant and delegated data grants - no match for remote data registration")
    void generateDelegatedGrantsNoMatchRemote() throws SaiHttpNotFoundException, SaiException {
        URI accessUri = toMockUri(server, "/authorization/delegated-agents");
        URI projectAuthorizationUri = toMockUri(server, "/authorization/delegated-project");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI agentRegistryUri = toMockUri(server, "/delegated-agents/");
        URI juanRegistrationUri = toMockUri(server, "/delegated-agents/delegated-juan/");
        URI bobProjectsRegistration = toMockUri(server, "/bob/data/nomatch/");

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);

        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectAuthorizationUri, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(READ_MODES).setScopeOfAuthorization(SCOPE_ALL_FROM_AGENT).setAccessNeed(PROJECTRON_PROJECT_NEED)
                .setDataRegistration(bobProjectsRegistration) // SPECIFIES NON-MATCHING REGISTRATION
                .build();

        AccessAuthorization.Builder accessBuilder = new AccessAuthorization.Builder(accessUri, saiSession);
        AccessAuthorization accessAuthorization = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataAuthorizations(Arrays.asList(projectAuthorization)).build();

        AccessGrant accessGrant = accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        assertTrue(accessGrant.getDataGrants().isEmpty());
    }

    @Test
    @DisplayName("Fail to generate access grant and delegated data grants - data authorization includes access modes not originally granted")
    void failToGenerateDelegatedGrantsExpandedModes() throws SaiHttpNotFoundException, SaiException {
        URI accessUri = toMockUri(server, "/authorization/delegated-agents");
        URI projectAuthorizationUri = toMockUri(server, "/authorization/delegated-project");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI agentRegistryUri = toMockUri(server, "/delegated-agents/");
        URI juanRegistrationUri = toMockUri(server, "/delegated-agents/delegated-juan/");

        List<RDFNode> EXPANDED_ACCESS_MODES = Arrays.asList(ACL_READ, ACL_CREATE, ACL_UPDATE);

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);

        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectAuthorizationUri, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(EXPANDED_ACCESS_MODES)
                .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfAuthorization(SCOPE_ALL_FROM_AGENT)
                .setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        AccessAuthorization.Builder accessBuilder = new AccessAuthorization.Builder(accessUri, saiSession);
        AccessAuthorization accessAuthorization = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataAuthorizations(Arrays.asList(projectAuthorization)).build();

        assertThrows(SaiException.class, () -> accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry)));
    }

    @Test
    @DisplayName("Generate access grant and delegated data grants - read only")
    void testGenerateDelegatedGrantsReadOnly() throws SaiHttpNotFoundException, SaiException {
        URI accessUri = toMockUri(server, "/authorization/delegated-agents");
        URI projectAuthorizationUri = toMockUri(server, "/authorization/delegated-project");
        URI milestoneAuthorizationUri = toMockUri(server, "/authorization/delegated-milestone");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI agentRegistryUri = toMockUri(server, "/delegated-agents/");
        URI juanRegistrationUri = toMockUri(server, "/delegated-agents/delegated-juan/");

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);

        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectAuthorizationUri, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(READ_MODES)
                .setScopeOfAuthorization(SCOPE_ALL_FROM_AGENT)
                .setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        DataAuthorization.Builder milestoneBuilder = new DataAuthorization.Builder(milestoneAuthorizationUri, saiSession);
        DataAuthorization milestoneAuthorization = milestoneBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID).setRegisteredShapeTree(MILESTONE_TREE)
                .setAccessModes(READ_MODES)
                .setScopeOfAuthorization(SCOPE_INHERITED).setInheritsFrom(projectAuthorization.getUri())
                .setAccessNeed(PROJECTRON_PROJECT_NEED).build();


        AccessAuthorization.Builder accessBuilder = new AccessAuthorization.Builder(accessUri, saiSession);
        AccessAuthorization accessAuthorization = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataAuthorizations(Arrays.asList(projectAuthorization, milestoneAuthorization)).build();

        AccessGrant accessGrant = accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        for (DataGrant dataGrant : accessGrant.getDataGrants()) { assertFalse(dataGrant.canCreate()); }
    }

    @Test
    @DisplayName("Fail to generate access grant and delegated data grants - child data authorization includes access modes not originally granted")
    void failToGenerateDelegatedGrantsChildExpandedModes() throws SaiHttpNotFoundException, SaiException {
        URI accessUri = toMockUri(server, "/authorization/delegated-agents");
        URI projectAuthorizationUri = toMockUri(server, "/authorization/delegated-project");
        URI milestoneAuthorizationUri = toMockUri(server, "/authorization/delegated-milestone");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI agentRegistryUri = toMockUri(server, "/delegated-agents/");
        URI juanRegistrationUri = toMockUri(server, "/delegated-agents/delegated-juan/");

        List<RDFNode> EXPANDED_ACCESS_MODES = Arrays.asList(ACL_READ, ACL_CREATE, ACL_UPDATE);

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);

        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectAuthorizationUri, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(ACCESS_MODES)
                .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfAuthorization(SCOPE_ALL_FROM_AGENT)
                .setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        DataAuthorization.Builder milestoneBuilder = new DataAuthorization.Builder(milestoneAuthorizationUri, saiSession);
        DataAuthorization milestoneAuthorization = milestoneBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID).setRegisteredShapeTree(MILESTONE_TREE)
                .setAccessModes(EXPANDED_ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfAuthorization(SCOPE_INHERITED).setInheritsFrom(projectAuthorization.getUri())
                .setAccessNeed(PROJECTRON_PROJECT_NEED).build();


        AccessAuthorization.Builder accessBuilder = new AccessAuthorization.Builder(accessUri, saiSession);
        AccessAuthorization accessAuthorization = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataAuthorizations(Arrays.asList(projectAuthorization, milestoneAuthorization)).build();

        assertThrows(SaiException.class, () -> accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry)));
    }

    @Test
    @DisplayName("Fail to generate access grant and delegated data grants - data authorization includes creator access modes not originally granted")
    void failToGenerateDelegatedGrantsExpandedCreatorModes() throws SaiHttpNotFoundException, SaiException {
        URI accessUri = toMockUri(server, "/authorization/delegated-agents");
        URI projectAuthorizationUri = toMockUri(server, "/authorization/delegated-project");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI agentRegistryUri = toMockUri(server, "/delegated-agents/");
        URI juanRegistrationUri = toMockUri(server, "/delegated-agents/delegated-juan/");

        List<RDFNode> EXPANDED_CREATOR_MODES = Arrays.asList(ACL_UPDATE, ACL_DELETE, ACL_CREATE);

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);

        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectAuthorizationUri, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(ACCESS_MODES)
                .setCreatorAccessModes(EXPANDED_CREATOR_MODES)
                .setScopeOfAuthorization(SCOPE_ALL_FROM_AGENT)
                .setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        AccessAuthorization.Builder accessBuilder = new AccessAuthorization.Builder(accessUri, saiSession);
        AccessAuthorization accessAuthorization = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataAuthorizations(Arrays.asList(projectAuthorization)).build();

        assertThrows(SaiException.class, () -> accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry)));
    }

    @Test
    @DisplayName("Fail to generate access grant and delegated data grants - child data authorization includes creator access modes not originally granted")
    void failToGenerateDelegatedGrantsChildExpandedCreatorModes() throws SaiHttpNotFoundException, SaiException {
        URI accessUri = toMockUri(server, "/authorization/delegated-agents");
        URI projectAuthorizationUri = toMockUri(server, "/authorization/delegated-project");
        URI milestoneAuthorizationUri = toMockUri(server, "/authorization/delegated-milestone");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI agentRegistryUri = toMockUri(server, "/delegated-agents/");
        URI juanRegistrationUri = toMockUri(server, "/delegated-agents/delegated-juan/");

        List<RDFNode> EXPANDED_CREATOR_MODES = Arrays.asList(ACL_UPDATE, ACL_DELETE, ACL_CREATE);

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);

        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectAuthorizationUri, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(ACCESS_MODES)
                .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfAuthorization(SCOPE_ALL_FROM_AGENT)
                .setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        DataAuthorization.Builder milestoneBuilder = new DataAuthorization.Builder(milestoneAuthorizationUri, saiSession);
        DataAuthorization milestoneAuthorization = milestoneBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID).setRegisteredShapeTree(MILESTONE_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(EXPANDED_CREATOR_MODES)
                .setScopeOfAuthorization(SCOPE_INHERITED).setInheritsFrom(projectAuthorization.getUri())
                .setAccessNeed(PROJECTRON_PROJECT_NEED).build();


        AccessAuthorization.Builder accessBuilder = new AccessAuthorization.Builder(accessUri, saiSession);
        AccessAuthorization accessAuthorization = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataAuthorizations(Arrays.asList(projectAuthorization, milestoneAuthorization)).build();

        assertThrows(SaiException.class, () -> accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry)));
    }

    @Test
    @DisplayName("Generate access grant and delegated data grants - no reciprocal registration")
    void generateDelegatedGrantsNoReciprocal() throws SaiHttpNotFoundException, SaiException {
        URI accessUri = toMockUri(server, "/authorization/delegated-agents");
        URI projectAuthorizationUri = toMockUri(server, "/authorization/delegated-project");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI agentRegistryUri = toMockUri(server, "/delegated-agents/");
        URI juanRegistrationUri = toMockUri(server, "/delegated-agents/delegated-juan/");

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);

        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectAuthorizationUri, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setDataOwner(CAROL_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(READ_MODES).setScopeOfAuthorization(SCOPE_ALL_FROM_AGENT).setAccessNeed(PROJECTRON_PROJECT_NEED)
                .build();

        AccessAuthorization.Builder accessBuilder = new AccessAuthorization.Builder(accessUri, saiSession);
        AccessAuthorization accessAuthorization = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataAuthorizations(Arrays.asList(projectAuthorization)).build();

        AccessGrant accessGrant = accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        assertTrue(accessGrant.getDataGrants().isEmpty());
    }

    @Test
    @DisplayName("Fail to generate access grant and delegated data grants - invalid scope")
    void failToGenerateDelegatedGrantsInvalidScope() throws SaiHttpNotFoundException, SaiException {
        URI accessUri = toMockUri(server, "/authorization/delegated-agents");
        URI projectAuthorizationUri = toMockUri(server, "/authorization/delegated-project");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI agentRegistryUri = toMockUri(server, "/delegated-agents/");
        URI juanRegistrationUri = toMockUri(server, "/delegated-agents/delegated-juan/");

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);

        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectAuthorizationUri, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setDataOwner(CAROL_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(READ_MODES).setScopeOfAuthorization(SCOPE_ALL_FROM_AGENT).setAccessNeed(PROJECTRON_PROJECT_NEED)
                .build();

        DataAuthorization spyProject = Mockito.spy(projectAuthorization);
        when(spyProject.getScopeOfAuthorization()).thenReturn(ACCESS_GRANT); // NOT A VALID SCOPE TYPE

        AccessAuthorization.Builder accessBuilder = new AccessAuthorization.Builder(accessUri, saiSession);
        AccessAuthorization accessAuthorization = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataAuthorizations(Arrays.asList(spyProject)).build();

        assertThrows(SaiException.class, () -> accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry)));
    }

    @Test
    @DisplayName("Generate access grant and delegated data grants - no access grant at reciprocal")
    void generateDelegatedGrantsNoGrantAtReciprocal() throws SaiHttpNotFoundException, SaiException {
        URI accessUri = toMockUri(server, "/authorization/delegated-agents");
        URI projectAuthorizationUri = toMockUri(server, "/authorization/delegated-project");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI agentRegistryUri = toMockUri(server, "/delegated-agents/");
        URI juanRegistrationUri = toMockUri(server, "/delegated-agents/delegated-juan/");

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);

        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectAuthorizationUri, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setDataOwner(TARA_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(READ_MODES).setScopeOfAuthorization(SCOPE_ALL_FROM_AGENT).setAccessNeed(PROJECTRON_PROJECT_NEED)
                .build();

        AccessAuthorization.Builder accessBuilder = new AccessAuthorization.Builder(accessUri, saiSession);
        AccessAuthorization accessAuthorization = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataAuthorizations(Arrays.asList(projectAuthorization)).build();

        AccessGrant accessGrant = accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        assertTrue(accessGrant.getDataGrants().isEmpty());
    }

    @Test
    @DisplayName("Generate access grant and associated data grants - multiple parents and children")
    void generateDataGrantsMultipleParents() throws SaiHttpNotFoundException, SaiException {
        URI accessUri = toMockUri(server, "/authorization/multiple-parents");
        URI dataAuthorizationUri = toMockUri(server, "/authorization/multiple-parents-project");
        URI milestoneAuthorizationUri = toMockUri(server, "/authorization/multiple-parents-milestone");
        URI calendarAuthorizationUri = toMockUri(server, "/authorization/multiple-parents-calendar");
        URI appointmentAuthorizationUri = toMockUri(server, "/authorization/multiple-parents-appointment");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI agentRegistryUri = toMockUri(server, "/scenario-agents/");
        URI registrationUri = toMockUri(server, "/scenario-agents/scenario-projectron/");
        URI PROJECT_REGISTRATION = toMockUri(server, "/personal/data/projects/");
        URI MILESTONE_REGISTRATION = toMockUri(server, "/personal/data/milestones/");
        URI CALENDAR_REGISTRATION = toMockUri(server, "/personal/data/calendars/");
        URI APPOINTMENT_REGISTRATION = toMockUri(server, "/personal/data/appointments/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);

        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(dataAuthorizationUri, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setDataOwner(ALICE_ID).setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(READ_MODES).setScopeOfAuthorization(SCOPE_ALL_FROM_REGISTRY)
                .setAccessNeed(PROJECTRON_PROJECT_NEED).setDataRegistration(PROJECT_REGISTRATION).build();

        DataAuthorization.Builder milestoneBuilder = new DataAuthorization.Builder(milestoneAuthorizationUri, saiSession);
        DataAuthorization milestoneAuthorization = milestoneBuilder.setDataOwner(ALICE_ID).setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(MILESTONE_TREE)
                .setAccessModes(READ_MODES).setScopeOfAuthorization(SCOPE_INHERITED).setInheritsFrom(projectAuthorization.getUri())
                .setAccessNeed(PROJECTRON_PROJECT_NEED).setDataRegistration(MILESTONE_REGISTRATION).build();

        DataAuthorization.Builder calendarBuilder = new DataAuthorization.Builder(calendarAuthorizationUri, saiSession);
        DataAuthorization calendarAuthorization = calendarBuilder.setDataOwner(ALICE_ID).setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(CALENDAR_TREE)
                .setAccessModes(READ_MODES).setScopeOfAuthorization(SCOPE_ALL_FROM_REGISTRY)
                .setAccessNeed(PROJECTRON_CALENDAR_NEED).setDataRegistration(CALENDAR_REGISTRATION).build();

        DataAuthorization.Builder appointmentBuilder = new DataAuthorization.Builder(appointmentAuthorizationUri, saiSession);
        DataAuthorization appointmentAuthorization = appointmentBuilder.setDataOwner(ALICE_ID).setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(APPOINTMENT_TREE)
                .setAccessModes(READ_MODES).setScopeOfAuthorization(SCOPE_INHERITED).setInheritsFrom(calendarAuthorization.getUri())
                .setAccessNeed(PROJECTRON_APPOINTMENT_NEED).setDataRegistration(APPOINTMENT_REGISTRATION).build();
        
        AccessAuthorization.Builder accessBuilder = new AccessAuthorization.Builder(accessUri, saiSession);
        AccessAuthorization accessAuthorization = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID).setGrantedAt(GRANT_TIME)
                .setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataAuthorizations(Arrays.asList(projectAuthorization, milestoneAuthorization, calendarAuthorization, appointmentAuthorization)).build();

        AccessGrant accessGrant = accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        assertEquals(4, accessGrant.getDataGrants().size());
    }

    @Test
    @DisplayName("Fail to generate data grants - data authorization has inherited scope")
    void failToGenerateDataGrantsAccessScopeInherited() throws SaiHttpNotFoundException, SaiException {
        URI accessUri = toMockUri(server, "/authorization/registry-1");
        URI agentRegistryUri = toMockUri(server, "/registry-1-agents/");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI registrationUri = toMockUri(server, "/registry-1-agents/registry-1-projectron/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);
        AccessAuthorization accessAuthorization = AccessAuthorization.get(accessUri, saiSession);
        for (DataAuthorization dataAuthorization : accessAuthorization.getDataAuthorizations()) {
            if (dataAuthorization.getScopeOfAuthorization().equals(SCOPE_INHERITED)) {
                assertThrows(SaiException.class, () -> dataAuthorization.generateGrants(accessAuthorization, registration, agentRegistry, Arrays.asList(dataRegistry)));
            }
        }
    }

    @Test
    @DisplayName("Fail to generate data grants - data authorization has invalid scope")
    void failToGenerateDataGrantsInvalidDataAuthorizationScope() throws SaiHttpNotFoundException, SaiException {
        URI accessUri = toMockUri(server, "/authorization/invalid-scope");
        URI dataAuthorizationUri = toMockUri(server, "/authorization/invalid-scope-project");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI agentRegistryUri = toMockUri(server, "/scenario-agents/");
        URI registrationUri = toMockUri(server, "/scenario-agents/scenario-projectron/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);

        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(dataAuthorizationUri, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfAuthorization(SCOPE_ALL).setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        DataAuthorization spyProject = Mockito.spy(projectAuthorization);
        when(spyProject.getScopeOfAuthorization()).thenReturn(ACCESS_GRANT); // NOT A VALID SCOPE TYPE

        AccessAuthorization.Builder accessBuilder = new AccessAuthorization.Builder(accessUri, saiSession);
        AccessAuthorization accessAuthorization = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID).setGrantedAt(GRANT_TIME)
                .setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataAuthorizations(Arrays.asList(spyProject)).build();

        assertThrows(SaiException.class, () -> accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry)));
    }

    @Test
    @DisplayName("Fail to generate data grants - specified data registration doesn't exist")
    void failToGenerateDataGrantsInvalidDataRegistration() throws SaiHttpNotFoundException, SaiException {
        URI accessUri = toMockUri(server, "/authorization/invalid-registration");
        URI dataAuthorizationUri = toMockUri(server, "/authorization/invalid-registration-project");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI agentRegistryUri = toMockUri(server, "/scenario-agents/");
        URI registrationUri = toMockUri(server, "/scenario-agents/scenario-projectron/");
        URI MISSING_REGISTRATION = toMockUri(server, "/personal/data/noprojects/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);

        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(dataAuthorizationUri, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES).setDataRegistration(MISSING_REGISTRATION)
                .setScopeOfAuthorization(SCOPE_ALL_FROM_REGISTRY).setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        AccessAuthorization.Builder accessBuilder = new AccessAuthorization.Builder(accessUri, saiSession);
        AccessAuthorization accessAuthorization = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID).setGrantedAt(GRANT_TIME)
                .setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataAuthorizations(Arrays.asList(projectAuthorization)).build();

        assertThrows(SaiException.class, () -> accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry)));
    }

    @Test
    @DisplayName("Fail to generate data grants - specified child data registration doesn't exist")
    void failToGenerateDataGrantsInvalidChildDataRegistration() throws SaiHttpNotFoundException, SaiException {
        URI accessUri = toMockUri(server, "/authorization/invalid-registration");
        URI dataAuthorizationUri = toMockUri(server, "/authorization/invalid-registration-project");
        URI eventAuthorizationUri = toMockUri(server, "/authorization/invalid-registration-event");
        URI dataRegistryUri = toMockUri(server, "/personal/data/");
        URI agentRegistryUri = toMockUri(server, "/scenario-agents/");
        URI registrationUri = toMockUri(server, "/scenario-agents/scenario-projectron/");
        URI PROJECT_REGISTRATION = toMockUri(server, "/personal/data/projects/");
        URI EVENT_TREE = toMockUri(server, "/shapetrees/pm#EventTree");

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUri, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUri, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUri, saiSession);

        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(dataAuthorizationUri, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES).setDataRegistration(PROJECT_REGISTRATION)
                .setScopeOfAuthorization(SCOPE_ALL_FROM_REGISTRY).setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        DataAuthorization.Builder eventBuilder = new DataAuthorization.Builder(eventAuthorizationUri, saiSession);
        DataAuthorization eventAuthorization = eventBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(EVENT_TREE)  // UNKNOWN
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfAuthorization(SCOPE_INHERITED).setInheritsFrom(projectAuthorization.getUri()).setAccessNeed(PROJECTRON_MILESTONE_NEED).build();

        AccessAuthorization.Builder accessBuilder = new AccessAuthorization.Builder(accessUri, saiSession);
        AccessAuthorization accessAuthorization = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID).setGrantedAt(GRANT_TIME)
                .setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataAuthorizations(Arrays.asList(projectAuthorization, eventAuthorization)).build();

        assertThrows(SaiException.class, () -> accessAuthorization.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry)));
    }


    @Test
    @DisplayName("Get an access authorization and linked data authorizations - scope: all")
    void getAccessAuthorization() throws SaiHttpNotFoundException, SaiException {
        URI uri = toMockUri(server, "/authorization/all-1");
        AccessAuthorization accessAuthorization = AccessAuthorization.get(uri, saiSession);
        checkAccessAuthorization(accessAuthorization);
    }

    @Test
    @DisplayName("Reload an access authorization and linked data authorizations - scope: all")
    void reloadAccessAuthorization() throws SaiHttpNotFoundException, SaiException {
        URI uri = toMockUri(server, "/authorization/all-1");
        AccessAuthorization accessAuthorization = AccessAuthorization.get(uri, saiSession);
        AccessAuthorization reloaded = accessAuthorization.reload();
        checkAccessAuthorization(reloaded);
    }

    @Test
    @DisplayName("Fail to get access authorization - missing required fields")
    void failToGetAccessAuthorizationRequired() {
        URI uri = toMockUri(server, "/missing-fields/authorization/all-1");
        assertThrows(SaiException.class, () -> AccessAuthorization.get(uri, saiSession));
    }

    private void checkAccessAuthorization(AccessAuthorization accessAuthorization) {
        assertNotNull(accessAuthorization);
        assertEquals(ALICE_ID, accessAuthorization.getGrantedBy());
        assertEquals(JARVIS_ID, accessAuthorization.getGrantedWith());
        assertEquals(PROJECTRON_ID, accessAuthorization.getGrantee());
        assertEquals(GRANT_TIME, accessAuthorization.getGrantedAt());
        assertEquals(PROJECTRON_NEED_GROUP, accessAuthorization.getAccessNeedGroup());
        for (DataAuthorization dataAuthorization : accessAuthorization.getDataAuthorizations()) { assertTrue(ALL_DATA_AUTHORIZATION_URIS.contains(dataAuthorization.getUri())); }
    }

    // The most efficient way to ensure that the access grants and data grants generated are
    // correct is to create fixtures that are known to be correct, load them, and then compare.
    private void checkAccessGrantAll(AccessGrant accessGrant) throws SaiHttpNotFoundException, SaiException {

        // Load a known-good "baseline" access grant and data grants from test fixtures to compare against
        URI baselineUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant");
        AccessGrant baselineGrant = AccessGrant.get(baselineUri, saiSession);

        assertNotNull(accessGrant);
        assertEquals(ALICE_ID, accessGrant.getGrantedBy());
        assertEquals(GRANT_TIME, accessGrant.getGrantedAt());
        assertEquals(PROJECTRON_ID, accessGrant.getGrantee());
        assertEquals(PROJECTRON_NEED_GROUP, accessGrant.getAccessNeedGroup());
        assertEquals(baselineGrant.getDataGrants().size(), accessGrant.getDataGrants().size());
        for (DataGrant dataGrant : accessGrant.getDataGrants()) {
            assertNotNull(findMatchingGrant(dataGrant, accessGrant.getDataGrants()));
        }
    }

    private DataGrant findMatchingGrant(DataGrant grantToMatch, List<DataGrant> dataGrants) {
        for (DataGrant dataGrant : dataGrants) {
            if (dataGrant.getRegisteredShapeTree() != grantToMatch.getRegisteredShapeTree()) continue;
            if (dataGrant.getScopeOfGrant() != grantToMatch.getScopeOfGrant()) continue;
            if (dataGrant.getDataOwner() != grantToMatch.getDataOwner()) continue;
            if (dataGrant.getInheritsFrom() != grantToMatch.getInheritsFrom()) continue;
            if (dataGrant.getDelegationOf() != grantToMatch.getDelegationOf()) continue;
            return dataGrant;
        }
        return null;
    }

    private void checkAccessGrantAllFromRegistry(AccessGrant accessGrant) throws SaiHttpNotFoundException, SaiException {

        // Load a known-good "baseline" access grant and data grants from test fixtures to compare against
        URI baselineUri = toMockUri(server, "/registry-1-agents/registry-1-projectron/registry-1-grant");
        AccessGrant baselineGrant = AccessGrant.get(baselineUri, saiSession);

        assertNotNull(accessGrant);
        assertEquals(ALICE_ID, accessGrant.getGrantedBy());
        assertEquals(GRANT_TIME, accessGrant.getGrantedAt());
        assertEquals(PROJECTRON_ID, accessGrant.getGrantee());
        assertEquals(PROJECTRON_NEED_GROUP, accessGrant.getAccessNeedGroup());
        assertEquals(baselineGrant.getDataGrants().size(), accessGrant.getDataGrants().size());
        for (DataGrant dataGrant : accessGrant.getDataGrants()) {
            assertNotNull(findMatchingGrant(dataGrant, accessGrant.getDataGrants()));
        }

    }

    private void checkAccessGrantSelectedFromRegistry(AccessGrant accessGrant) throws SaiHttpNotFoundException, SaiException {

        // Load a known-good "baseline" access grant and data grants from test fixtures to compare against
        URI baselineUri = toMockUri(server, "/selected-1-agents/selected-1-projectron/selected-1-grant");
        AccessGrant baselineGrant = AccessGrant.get(baselineUri, saiSession);

        assertNotNull(accessGrant);
        assertEquals(ALICE_ID, accessGrant.getGrantedBy());
        assertEquals(GRANT_TIME, accessGrant.getGrantedAt());
        assertEquals(PROJECTRON_ID, accessGrant.getGrantee());
        assertEquals(PROJECTRON_NEED_GROUP, accessGrant.getAccessNeedGroup());
        assertEquals(baselineGrant.getDataGrants().size(), accessGrant.getDataGrants().size());
        for (DataGrant dataGrant : accessGrant.getDataGrants()) {
            assertNotNull(findMatchingGrant(dataGrant, accessGrant.getDataGrants()));
        }
        
    }

    private void checkAccessGrantAllFromAgent(AccessGrant accessGrant) throws SaiHttpNotFoundException, SaiException {

        // Load a known-good "baseline" access grant and data grants from test fixtures to compare against
        URI baselineUri = toMockUri(server, "/agent-1-agents/agent-1-projectron/agent-1-grant");
        AccessGrant baselineGrant = AccessGrant.get(baselineUri, saiSession);

        assertNotNull(accessGrant);
        assertEquals(ALICE_ID, accessGrant.getGrantedBy());
        assertEquals(GRANT_TIME, accessGrant.getGrantedAt());
        assertEquals(PROJECTRON_ID, accessGrant.getGrantee());
        assertEquals(PROJECTRON_NEED_GROUP, accessGrant.getAccessNeedGroup());
        assertEquals(baselineGrant.getDataGrants().size(), accessGrant.getDataGrants().size());
        for (DataGrant dataGrant : accessGrant.getDataGrants()) {
            assertNotNull(findMatchingGrant(dataGrant, accessGrant.getDataGrants()));
        }

    }

}

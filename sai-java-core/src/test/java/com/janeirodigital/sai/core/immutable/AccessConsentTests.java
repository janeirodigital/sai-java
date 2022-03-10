package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.crud.AgentRegistry;
import com.janeirodigital.sai.core.crud.ApplicationRegistration;
import com.janeirodigital.sai.core.crud.DataRegistry;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.readable.*;
import com.janeirodigital.sai.core.sessions.SaiSession;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

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
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
class AccessConsentTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;
    private static URL ALICE_ID, BOB_ID, CAROL_ID, JUAN_ID, TARA_ID;
    private static URL JARVIS_ID;
    private static URL PROJECTRON_ID;
    private static URL PROJECTRON_NEED_GROUP;
    private static URL PROJECT_TREE, MILESTONE_TREE, ISSUE_TREE, TASK_TREE, CALENDAR_TREE, APPOINTMENT_TREE;
    private static URL PROJECTRON_PROJECT_NEED, PROJECTRON_MILESTONE_NEED, PROJECTRON_ISSUE_NEED, PROJECTRON_TASK_NEED;
    private static URL PROJECTRON_CALENDAR_NEED, PROJECTRON_APPOINTMENT_NEED;
    private static OffsetDateTime GRANT_TIME;
    private static List<URL> ALL_DATA_CONSENT_URLS;
    private static List<RDFNode> ACCESS_MODES, CREATOR_ACCESS_MODES, READ_MODES;

    @BeforeAll
    static void beforeAll() throws SaiException, SaiNotFoundException {
        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));
        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();

        // Scope: interop:All - GET all necessary resources across registries (used in basic crud tests as well as grant generation)
        mockOnGet(dispatcher, "/access/all-1", "access/all/all-1-ttl");
        mockOnGet(dispatcher, "/missing-fields/access/all-1", "access/all/all-1-missing-fields-ttl");
        mockOnPut(dispatcher, "/access/all-1", "http/201");
        mockOnGet(dispatcher, "/access/all-1-project", "access/all/all-1-project-ttl");
        mockOnPut(dispatcher, "/access/all-1-project", "http/201");
        mockOnGet(dispatcher, "/access/all-1-milestone", "access/all/all-1-milestone-ttl");
        mockOnPut(dispatcher, "/access/all-1-milestone", "http/201");
        mockOnGet(dispatcher, "/access/all-1-task", "access/all/all-1-task-ttl");
        mockOnPut(dispatcher, "/access/all-1-task", "http/201");
        mockOnGet(dispatcher, "/access/all-1-issue", "access/all/all-1-issue-ttl");
        mockOnPut(dispatcher, "/access/all-1-issue", "http/201");
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
        mockOnGet(dispatcher, "/access/registry-1", "access/all-from-registry/registry-1-ttl");
        mockOnGet(dispatcher, "/access/registry-1-project", "access/all-from-registry/registry-1-project-ttl");
        mockOnGet(dispatcher, "/access/registry-1-milestone", "access/all-from-registry/registry-1-milestone-ttl");
        mockOnGet(dispatcher, "/access/registry-1-task", "access/all-from-registry/registry-1-task-ttl");
        mockOnGet(dispatcher, "/access/registry-1-issue", "access/all-from-registry/registry-1-issue-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/", "agents/alice/projectron-all-from-registry/registry-1-agent-registry-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/", "agents/alice/projectron-all-from-registry/registry-1-projectron-registration-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant", "agents/alice/projectron-all-from-registry/registry-1-grant-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-project", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-project-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-milestone", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-milestone-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-issue", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-issue-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-task", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-task-ttl");

        // Scope: interop:SelectedFromRegistry - GET all necessary resources across registries (for testing grant generation)
        mockOnGet(dispatcher, "/access/selected-1", "access/selected-from-registry/selected-1-ttl");
        mockOnGet(dispatcher, "/access/selected-1-project", "access/selected-from-registry/selected-1-project-ttl");
        mockOnGet(dispatcher, "/access/selected-1-milestone", "access/selected-from-registry/selected-1-milestone-ttl");
        mockOnGet(dispatcher, "/access/selected-1-task", "access/selected-from-registry/selected-1-task-ttl");
        mockOnGet(dispatcher, "/access/selected-1-issue", "access/selected-from-registry/selected-1-issue-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/", "agents/alice/projectron-selected-from-registry/selected-1-agent-registry-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/selected-1-projectron/", "agents/alice/projectron-selected-from-registry/selected-1-projectron-registration-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/selected-1-projectron/selected-1-grant", "agents/alice/projectron-selected-from-registry/selected-1-grant-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/selected-1-projectron/selected-1-grant-personal-project", "agents/alice/projectron-selected-from-registry/selected-1-grant-personal-project-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/selected-1-projectron/selected-1-grant-personal-milestone", "agents/alice/projectron-selected-from-registry/selected-1-grant-personal-milestone-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/selected-1-projectron/selected-1-grant-personal-issue", "agents/alice/projectron-selected-from-registry/selected-1-grant-personal-issue-ttl");
        mockOnGet(dispatcher, "/selected-1-agents/selected-1-projectron/selected-1-grant-personal-task", "agents/alice/projectron-selected-from-registry/selected-1-grant-personal-task-ttl");
        
        // Scope: interop:AllFromAgent - GET all necessary resources across registries (for testing grant generation)
        mockOnGet(dispatcher, "/access/agent-1", "access/all-from-agent/agent-1-ttl");
        mockOnGet(dispatcher, "/access/agent-1-project", "access/all-from-agent/agent-1-project-ttl");
        mockOnGet(dispatcher, "/access/agent-1-milestone", "access/all-from-agent/agent-1-milestone-ttl");
        mockOnGet(dispatcher, "/access/agent-1-task", "access/all-from-agent/agent-1-task-ttl");
        mockOnGet(dispatcher, "/access/agent-1-issue", "access/all-from-agent/agent-1-issue-ttl");
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
        mockOnGet(dispatcher, "/scenario-agents/", "access/scenario/scenario-agent-registry-ttl");
        mockOnGet(dispatcher, "/scenario-agents/scenario-projectron/", "access/scenario/scenario-projectron-registration-ttl");
        mockOnGet(dispatcher, "/delegated-agents/", "access/scenario/delegated-agent-registry-ttl");
        mockOnGet(dispatcher, "/delegated-agents/delegated-projectron/", "access/scenario/delegated-projectron-registration-ttl");
        mockOnGet(dispatcher, "/delegated-agents/delegated-performchart/", "access/scenario/delegated-performchart-registration-ttl");
        mockOnGet(dispatcher, "/delegated-agents/delegated-bob/", "access/scenario/delegated-bob-registration-ttl");
        mockOnGet(dispatcher, "/delegated-agents/delegated-juan/", "access/scenario/delegated-juan-registration-ttl");
        mockOnGet(dispatcher, "/delegated-agents/delegated-carol/", "access/scenario/delegated-carol-registration-ttl");
        mockOnGet(dispatcher, "/delegated-agents/delegated-tara/", "access/scenario/delegated-tara-registration-ttl");
        mockOnGet(dispatcher, "/delegated-bob-agents/delegated-alice/", "access/scenario/delegated-bob-alice-registration-ttl");
        mockOnGet(dispatcher, "/delegated-bob-agents/delegated-alice/delegated-grant", "access/scenario/delegated-bob-alice-grant-ttl");
        mockOnGet(dispatcher, "/delegated-bob-agents/delegated-alice/delegated-grant-project", "access/scenario/delegated-bob-alice-grant-project-ttl");
        mockOnGet(dispatcher, "/delegated-bob-agents/delegated-alice/delegated-grant-milestone", "access/scenario/delegated-bob-alice-grant-milestone-ttl");
        mockOnGet(dispatcher, "/delegated-tara-agents/delegated-alice/", "access/scenario/delegated-tara-alice-registration-ttl");

        // Get Alice's data registries - doesn't change across use cases
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

        ALICE_ID = stringToUrl("https://alice.example/id");
        JARVIS_ID = stringToUrl("https://jarvis.example/id");
        PROJECTRON_ID = stringToUrl("https://projectron.example/id");
        BOB_ID = stringToUrl("https://bob.example/id");
        CAROL_ID = stringToUrl("https://carol.example/id");
        JUAN_ID = stringToUrl("https://juan.example/id");
        TARA_ID = stringToUrl("https://tara.example/id");
        GRANT_TIME = OffsetDateTime.parse("2020-09-05T06:15:01Z", DateTimeFormatter.ISO_DATE_TIME);

        PROJECTRON_NEED_GROUP = stringToUrl("https://projectron.example/#d8219b1f");
        PROJECTRON_PROJECT_NEED = stringToUrl("https://projectron.example/#ac54ff1e");
        PROJECTRON_MILESTONE_NEED = stringToUrl("https://projectron.example/#bd66ee2b");
        PROJECTRON_ISSUE_NEED = stringToUrl("https://projectron.example/#aa123a1b");
        PROJECTRON_TASK_NEED = stringToUrl("https://projectron.example/#ce22cc1a");
        PROJECTRON_CALENDAR_NEED = stringToUrl("https://projectron.example/#ba66ff1e");
        PROJECTRON_APPOINTMENT_NEED = stringToUrl("https://projectron.example/#aa11aa1b");
        ALL_DATA_CONSENT_URLS = Arrays.asList(toUrl(server, "/access/all-1-project"), toUrl(server, "/access/all-1-milestone"),
                                              toUrl(server, "/access/all-1-issue"), toUrl(server, "/access/all-1-task"));
        PROJECT_TREE = stringToUrl("http://data.example/shapetrees/pm#ProjectTree");
        MILESTONE_TREE = stringToUrl("http://data.example/shapetrees/pm#MilestoneTree");
        ISSUE_TREE = stringToUrl("http://data.example/shapetrees/pm#IssueTree");
        TASK_TREE = stringToUrl("http://data.example/shapetrees/pm#TaskTree");
        CALENDAR_TREE = stringToUrl("http://data.example/shapetrees/pm#CalendarTree");
        APPOINTMENT_TREE = stringToUrl("http://data.example/shapetrees/pm#AppointmentTree");
        READ_MODES = Arrays.asList(ACL_READ);
        ACCESS_MODES = Arrays.asList(ACL_READ, ACL_CREATE);
        CREATOR_ACCESS_MODES = Arrays.asList(ACL_UPDATE, ACL_DELETE);
         
    }

    @Test
    @DisplayName("Create new access consent and linked data consents - scope: all")
    void createAccessConsentScopeAll() throws SaiException {
        URL accessUrl = toUrl(server, "/access/all-1");
        URL projectUrl = toUrl(server, "/access/all-1-project");
        URL milestoneUrl = toUrl(server, "/access/all-1-milestone");
        URL issueUrl = toUrl(server, "/access/all-1-issue");
        URL taskUrl = toUrl(server, "/access/all-1-task");
        
        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                                                   .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                                                   .setScopeOfConsent(SCOPE_ALL).setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        DataConsent.Builder milestoneBuilder = new DataConsent.Builder(milestoneUrl, saiSession);
        DataConsent milestoneConsent = milestoneBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(MILESTONE_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfConsent(SCOPE_INHERITED).setAccessNeed(PROJECTRON_MILESTONE_NEED).setInheritsFrom(projectConsent.getUrl()).build();

        DataConsent.Builder issueBuilder = new DataConsent.Builder(issueUrl, saiSession);
        DataConsent issueConsent = issueBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(ISSUE_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfConsent(SCOPE_INHERITED).setAccessNeed(PROJECTRON_ISSUE_NEED).setInheritsFrom(milestoneConsent.getUrl()).build();

        DataConsent.Builder taskBuilder = new DataConsent.Builder(taskUrl, saiSession);
        DataConsent taskConsent = taskBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(TASK_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfConsent(SCOPE_INHERITED).setAccessNeed(PROJECTRON_TASK_NEED).setInheritsFrom(milestoneConsent.getUrl()).build();

        List<DataConsent> dataConsents = Arrays.asList(projectConsent, milestoneConsent, issueConsent, taskConsent);

        AccessConsent.Builder accessBuilder = new AccessConsent.Builder(accessUrl, saiSession);
        AccessConsent accessConsent = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID).setGrantedAt(GRANT_TIME)
                                                   .setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                                                   .setDataConsents(dataConsents).build();
        assertDoesNotThrow(() -> accessConsent.create());
    }

    @Test
    @DisplayName("Generate access grant and associated data grants - Scope: All")
    void testGenerateAccessGrantAll() throws SaiNotFoundException, SaiException {
        // Note that in typical use we wouldn't be getting an existing acccess consent, but would instead
        // be generating the grants right after generating the consents
        URL accessUrl = toUrl(server, "/access/all-1");
        URL agentRegistryUrl = toUrl(server, "/all-1-agents/");
        URL personalDataUrl = toUrl(server, "/personal/data/");
        URL workDataUrl = toUrl(server, "/personal/data/");
        URL registrationUrl = toUrl(server, "/all-1-agents/all-1-projectron/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUrl, saiSession);
        DataRegistry personalData = DataRegistry.get(personalDataUrl, saiSession);
        DataRegistry workData = DataRegistry.get(workDataUrl, saiSession);
        AccessConsent accessConsent = AccessConsent.get(accessUrl, saiSession);
        AccessGrant accessGrant = accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(personalData, workData));
        checkAccessGrantAll(accessGrant);
    }

    @Test
    @DisplayName("Generate access grant and associated data grants - Scope: AllFromRegistry")
    void testGenerateAccessGrantAllFromRegistry() throws SaiNotFoundException, SaiException {
        // Note that in typical use we wouldn't be getting an existing acccess consent, but would instead
        // be generating the grants right after generating the consents
        URL accessUrl = toUrl(server, "/access/registry-1");
        URL agentRegistryUrl = toUrl(server, "/registry-1-agents/");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL registrationUrl = toUrl(server, "/registry-1-agents/registry-1-projectron/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);
        AccessConsent accessConsent = AccessConsent.get(accessUrl, saiSession);
        AccessGrant accessGrant = accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        checkAccessGrantAllFromRegistry(accessGrant);
    }

    @Test
    @DisplayName("Generate access grant and associated data grants - Scope: SelectedFromRegistry")
    void testGenerateAccessGrantSelectedFromRegistry() throws SaiNotFoundException, SaiException {
        // Note that in typical use we wouldn't be getting an existing acccess consent, but would instead
        // be generating the grants right after generating the consents
        URL accessUrl = toUrl(server, "/access/selected-1");
        URL agentRegistryUrl = toUrl(server, "/selected-1-agents/");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL registrationUrl = toUrl(server, "/selected-1-agents/selected-1-projectron/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);
        AccessConsent accessConsent = AccessConsent.get(accessUrl, saiSession);
        AccessGrant accessGrant = accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        checkAccessGrantSelectedFromRegistry(accessGrant);
    }

    @Test
    @DisplayName("Generate access grant and associated data grants - Scope: AllFromAgent")
    void testGenerateAccessGrantAllFromAgent() throws SaiNotFoundException, SaiException {
        // Note that in typical use we wouldn't be getting an existing acccess consent, but would instead
        // be generating the grants right after generating the consents
        URL accessUrl = toUrl(server, "/access/agent-1");
        URL agentRegistryUrl = toUrl(server, "/agent-1-agents/");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL registrationUrl = toUrl(server, "/agent-1-agents/agent-1-projectron/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);
        AccessConsent accessConsent = AccessConsent.get(accessUrl, saiSession);
        AccessGrant accessGrant = accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        checkAccessGrantAllFromAgent(accessGrant);
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
    @DisplayName("Generate access grant and associated data grants - no matching data registrations")
    void generateDataGrantsNoMatchingRegistrations() throws SaiNotFoundException, SaiException {
        URL accessUrl = toUrl(server, "/access/no-matches");
        URL dataConsentUrl = toUrl(server, "/access/no-matches-project");
        URL EVENT_TREE = stringToUrl("http://data.example/shapetrees/pm#EventTree");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL agentRegistryUrl = toUrl(server, "/scenario-agents/");
        URL registrationUrl = toUrl(server, "/scenario-agents/scenario-projectron/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);

        DataConsent.Builder projectBuilder = new DataConsent.Builder(dataConsentUrl, saiSession);
        DataConsent eventConsent = projectBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(EVENT_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfConsent(SCOPE_ALL).setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        AccessConsent.Builder accessBuilder = new AccessConsent.Builder(accessUrl, saiSession);
        AccessConsent accessConsent = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID).setGrantedAt(GRANT_TIME)
                .setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataConsents(Arrays.asList(eventConsent)).build();

        AccessGrant accessGrant = accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        assertTrue(accessGrant.getDataGrants().isEmpty());
    }

    @Test
    @DisplayName("Generate access grant and associated data grants - read-only access modes")
    void generateDataGrantsReadOnly() throws SaiNotFoundException, SaiException {
        URL accessUrl = toUrl(server, "/access/read-only");
        URL dataConsentUrl = toUrl(server, "/access/read-only-project");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL agentRegistryUrl = toUrl(server, "/scenario-agents/");
        URL registrationUrl = toUrl(server, "/scenario-agents/scenario-projectron/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);

        DataConsent.Builder projectBuilder = new DataConsent.Builder(dataConsentUrl, saiSession);
        DataConsent eventConsent = projectBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(READ_MODES).setScopeOfConsent(SCOPE_ALL).setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        AccessConsent.Builder accessBuilder = new AccessConsent.Builder(accessUrl, saiSession);
        AccessConsent accessConsent = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID).setGrantedAt(GRANT_TIME)
                .setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataConsents(Arrays.asList(eventConsent)).build();

        AccessGrant accessGrant = accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        assertFalse(accessGrant.getDataGrants().get(0).canCreate());
    }

    @Test
    @DisplayName("Generate access grant and delegated data grants - multiple social agents in registry")
    void generateDelegatedGrantsMultipleSocials() throws SaiNotFoundException, SaiException, SaiAlreadyExistsException {
        URL accessUrl = toUrl(server, "/access/delegated-agents");
        URL projectConsentUrl = toUrl(server, "/access/delegated-project");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL agentRegistryUrl = toUrl(server, "/delegated-agents/");
        URL juanRegistrationUrl = toUrl(server, "/delegated-agents/delegated-juan/");
        URL bobProjectsRegistration = toUrl(server, "/bob/data/projects/");
        
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);

        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectConsentUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(READ_MODES).setScopeOfConsent(SCOPE_ALL_FROM_AGENT).setAccessNeed(PROJECTRON_PROJECT_NEED)
                .setDataRegistration(bobProjectsRegistration).build();

        AccessConsent.Builder accessBuilder = new AccessConsent.Builder(accessUrl, saiSession);
        AccessConsent accessConsent = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataConsents(Arrays.asList(projectConsent)).build();

        AccessGrant accessGrant = accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        assertEquals(1, accessGrant.getDataGrants().size());
    }

    @Test
    @DisplayName("Generate access grant and delegated data grants - no match for remote data registration")
    void generateDelegatedGrantsNoMatchRemote() throws SaiNotFoundException, SaiException, SaiAlreadyExistsException {
        URL accessUrl = toUrl(server, "/access/delegated-agents");
        URL projectConsentUrl = toUrl(server, "/access/delegated-project");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL agentRegistryUrl = toUrl(server, "/delegated-agents/");
        URL juanRegistrationUrl = toUrl(server, "/delegated-agents/delegated-juan/");
        URL bobProjectsRegistration = toUrl(server, "/bob/data/nomatch/");

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);

        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectConsentUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(READ_MODES).setScopeOfConsent(SCOPE_ALL_FROM_AGENT).setAccessNeed(PROJECTRON_PROJECT_NEED)
                .setDataRegistration(bobProjectsRegistration) // SPECIFIES NON-MATCHING REGISTRATION
                .build();

        AccessConsent.Builder accessBuilder = new AccessConsent.Builder(accessUrl, saiSession);
        AccessConsent accessConsent = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataConsents(Arrays.asList(projectConsent)).build();

        AccessGrant accessGrant = accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        assertTrue(accessGrant.getDataGrants().isEmpty());
    }

    @Test
    @DisplayName("Fail to generate access grant and delegated data grants - data consent includes access modes not originally granted")
    void failToGenerateDelegatedGrantsExpandedModes() throws SaiNotFoundException, SaiException, SaiAlreadyExistsException {
        URL accessUrl = toUrl(server, "/access/delegated-agents");
        URL projectConsentUrl = toUrl(server, "/access/delegated-project");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL agentRegistryUrl = toUrl(server, "/delegated-agents/");
        URL juanRegistrationUrl = toUrl(server, "/delegated-agents/delegated-juan/");

        List<RDFNode> EXPANDED_ACCESS_MODES = Arrays.asList(ACL_READ, ACL_CREATE, ACL_UPDATE);

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);

        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectConsentUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(EXPANDED_ACCESS_MODES)
                .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfConsent(SCOPE_ALL_FROM_AGENT)
                .setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        AccessConsent.Builder accessBuilder = new AccessConsent.Builder(accessUrl, saiSession);
        AccessConsent accessConsent = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataConsents(Arrays.asList(projectConsent)).build();

        assertThrows(SaiException.class, () -> accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry)));
    }

    @Test
    @DisplayName("Generate access grant and delegated data grants - read only")
    void testGenerateDelegatedGrantsReadOnly() throws SaiNotFoundException, SaiException {
        URL accessUrl = toUrl(server, "/access/delegated-agents");
        URL projectConsentUrl = toUrl(server, "/access/delegated-project");
        URL milestoneConsentUrl = toUrl(server, "/access/delegated-milestone");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL agentRegistryUrl = toUrl(server, "/delegated-agents/");
        URL juanRegistrationUrl = toUrl(server, "/delegated-agents/delegated-juan/");

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);

        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectConsentUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(READ_MODES)
                .setScopeOfConsent(SCOPE_ALL_FROM_AGENT)
                .setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        DataConsent.Builder milestoneBuilder = new DataConsent.Builder(milestoneConsentUrl, saiSession);
        DataConsent milestoneConsent = milestoneBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID).setRegisteredShapeTree(MILESTONE_TREE)
                .setAccessModes(READ_MODES)
                .setScopeOfConsent(SCOPE_INHERITED).setInheritsFrom(projectConsent.getUrl())
                .setAccessNeed(PROJECTRON_PROJECT_NEED).build();


        AccessConsent.Builder accessBuilder = new AccessConsent.Builder(accessUrl, saiSession);
        AccessConsent accessConsent = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataConsents(Arrays.asList(projectConsent, milestoneConsent)).build();

        AccessGrant accessGrant = accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        for (DataGrant dataGrant : accessGrant.getDataGrants()) { assertFalse(dataGrant.canCreate()); }
    }

    @Test
    @DisplayName("Fail to generate access grant and delegated data grants - child data consent includes access modes not originally granted")
    void failToGenerateDelegatedGrantsChildExpandedModes() throws SaiNotFoundException, SaiException, SaiAlreadyExistsException {
        URL accessUrl = toUrl(server, "/access/delegated-agents");
        URL projectConsentUrl = toUrl(server, "/access/delegated-project");
        URL milestoneConsentUrl = toUrl(server, "/access/delegated-milestone");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL agentRegistryUrl = toUrl(server, "/delegated-agents/");
        URL juanRegistrationUrl = toUrl(server, "/delegated-agents/delegated-juan/");

        List<RDFNode> EXPANDED_ACCESS_MODES = Arrays.asList(ACL_READ, ACL_CREATE, ACL_UPDATE);

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);

        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectConsentUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(ACCESS_MODES)
                .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfConsent(SCOPE_ALL_FROM_AGENT)
                .setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        DataConsent.Builder milestoneBuilder = new DataConsent.Builder(milestoneConsentUrl, saiSession);
        DataConsent milestoneConsent = milestoneBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID).setRegisteredShapeTree(MILESTONE_TREE)
                .setAccessModes(EXPANDED_ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfConsent(SCOPE_INHERITED).setInheritsFrom(projectConsent.getUrl())
                .setAccessNeed(PROJECTRON_PROJECT_NEED).build();


        AccessConsent.Builder accessBuilder = new AccessConsent.Builder(accessUrl, saiSession);
        AccessConsent accessConsent = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataConsents(Arrays.asList(projectConsent, milestoneConsent)).build();

        assertThrows(SaiException.class, () -> accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry)));
    }

    @Test
    @DisplayName("Fail to generate access grant and delegated data grants - data consent includes creator access modes not originally granted")
    void failToGenerateDelegatedGrantsExpandedCreatorModes() throws SaiNotFoundException, SaiException, SaiAlreadyExistsException {
        URL accessUrl = toUrl(server, "/access/delegated-agents");
        URL projectConsentUrl = toUrl(server, "/access/delegated-project");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL agentRegistryUrl = toUrl(server, "/delegated-agents/");
        URL juanRegistrationUrl = toUrl(server, "/delegated-agents/delegated-juan/");

        List<RDFNode> EXPANDED_CREATOR_MODES = Arrays.asList(ACL_UPDATE, ACL_DELETE, ACL_CREATE);

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);

        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectConsentUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(ACCESS_MODES)
                .setCreatorAccessModes(EXPANDED_CREATOR_MODES)
                .setScopeOfConsent(SCOPE_ALL_FROM_AGENT)
                .setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        AccessConsent.Builder accessBuilder = new AccessConsent.Builder(accessUrl, saiSession);
        AccessConsent accessConsent = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataConsents(Arrays.asList(projectConsent)).build();

        assertThrows(SaiException.class, () -> accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry)));
    }

    @Test
    @DisplayName("Fail to generate access grant and delegated data grants - child data consent includes creator access modes not originally granted")
    void failToGenerateDelegatedGrantsChildExpandedCreatorModes() throws SaiNotFoundException, SaiException {
        URL accessUrl = toUrl(server, "/access/delegated-agents");
        URL projectConsentUrl = toUrl(server, "/access/delegated-project");
        URL milestoneConsentUrl = toUrl(server, "/access/delegated-milestone");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL agentRegistryUrl = toUrl(server, "/delegated-agents/");
        URL juanRegistrationUrl = toUrl(server, "/delegated-agents/delegated-juan/");

        List<RDFNode> EXPANDED_CREATOR_MODES = Arrays.asList(ACL_UPDATE, ACL_DELETE, ACL_CREATE);

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);

        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectConsentUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(ACCESS_MODES)
                .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfConsent(SCOPE_ALL_FROM_AGENT)
                .setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        DataConsent.Builder milestoneBuilder = new DataConsent.Builder(milestoneConsentUrl, saiSession);
        DataConsent milestoneConsent = milestoneBuilder.setDataOwner(BOB_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID).setRegisteredShapeTree(MILESTONE_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(EXPANDED_CREATOR_MODES)
                .setScopeOfConsent(SCOPE_INHERITED).setInheritsFrom(projectConsent.getUrl())
                .setAccessNeed(PROJECTRON_PROJECT_NEED).build();


        AccessConsent.Builder accessBuilder = new AccessConsent.Builder(accessUrl, saiSession);
        AccessConsent accessConsent = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataConsents(Arrays.asList(projectConsent, milestoneConsent)).build();

        assertThrows(SaiException.class, () -> accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry)));
    }

    @Test
    @DisplayName("Generate access grant and delegated data grants - no reciprocal registration")
    void generateDelegatedGrantsNoReciprocal() throws SaiNotFoundException, SaiException, SaiAlreadyExistsException {
        URL accessUrl = toUrl(server, "/access/delegated-agents");
        URL projectConsentUrl = toUrl(server, "/access/delegated-project");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL agentRegistryUrl = toUrl(server, "/delegated-agents/");
        URL juanRegistrationUrl = toUrl(server, "/delegated-agents/delegated-juan/");

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);

        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectConsentUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setDataOwner(CAROL_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(READ_MODES).setScopeOfConsent(SCOPE_ALL_FROM_AGENT).setAccessNeed(PROJECTRON_PROJECT_NEED)
                .build();

        AccessConsent.Builder accessBuilder = new AccessConsent.Builder(accessUrl, saiSession);
        AccessConsent accessConsent = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataConsents(Arrays.asList(projectConsent)).build();

        AccessGrant accessGrant = accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        assertTrue(accessGrant.getDataGrants().isEmpty());
    }

    @Test
    @DisplayName("Fail to generate access grant and delegated data grants - invalid scope")
    void failToGenerateDelegatedGrantsInvalidScope() throws SaiNotFoundException, SaiException {
        URL accessUrl = toUrl(server, "/access/delegated-agents");
        URL projectConsentUrl = toUrl(server, "/access/delegated-project");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL agentRegistryUrl = toUrl(server, "/delegated-agents/");
        URL juanRegistrationUrl = toUrl(server, "/delegated-agents/delegated-juan/");

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);

        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectConsentUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setDataOwner(CAROL_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(READ_MODES).setScopeOfConsent(SCOPE_ALL_FROM_AGENT).setAccessNeed(PROJECTRON_PROJECT_NEED)
                .build();

        DataConsent spyProject = Mockito.spy(projectConsent);
        when(spyProject.getScopeOfConsent()).thenReturn(ACCESS_GRANT); // NOT A VALID SCOPE TYPE

        AccessConsent.Builder accessBuilder = new AccessConsent.Builder(accessUrl, saiSession);
        AccessConsent accessConsent = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataConsents(Arrays.asList(spyProject)).build();

        assertThrows(SaiException.class, () -> accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry)));
    }

    @Test
    @DisplayName("Generate access grant and delegated data grants - no access grant at reciprocal")
    void generateDelegatedGrantsNoGrantAtReciprocal() throws SaiNotFoundException, SaiException, SaiAlreadyExistsException {
        URL accessUrl = toUrl(server, "/access/delegated-agents");
        URL projectConsentUrl = toUrl(server, "/access/delegated-project");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL agentRegistryUrl = toUrl(server, "/delegated-agents/");
        URL juanRegistrationUrl = toUrl(server, "/delegated-agents/delegated-juan/");

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(juanRegistrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);

        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectConsentUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setDataOwner(TARA_ID).setGrantedBy(ALICE_ID).setGrantee(JUAN_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(READ_MODES).setScopeOfConsent(SCOPE_ALL_FROM_AGENT).setAccessNeed(PROJECTRON_PROJECT_NEED)
                .build();

        AccessConsent.Builder accessBuilder = new AccessConsent.Builder(accessUrl, saiSession);
        AccessConsent accessConsent = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID)
                .setGrantee(JUAN_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataConsents(Arrays.asList(projectConsent)).build();

        AccessGrant accessGrant = accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        assertTrue(accessGrant.getDataGrants().isEmpty());
    }

    @Test
    @DisplayName("Generate access grant and associated data grants - multiple parents and children")
    void generateDataGrantsMultipleParents() throws SaiNotFoundException, SaiException {
        URL accessUrl = toUrl(server, "/access/multiple-parents");
        URL dataConsentUrl = toUrl(server, "/access/multiple-parents-project");
        URL milestoneConsentUrl = toUrl(server, "/access/multiple-parents-milestone");
        URL calendarConsentUrl = toUrl(server, "/access/multiple-parents-calendar");
        URL appointmentConsentUrl = toUrl(server, "/access/multiple-parents-appointment");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL agentRegistryUrl = toUrl(server, "/scenario-agents/");
        URL registrationUrl = toUrl(server, "/scenario-agents/scenario-projectron/");
        URL PROJECT_REGISTRATION = toUrl(server, "/personal/data/projects/");
        URL MILESTONE_REGISTRATION = toUrl(server, "/personal/data/milestones/");
        URL CALENDAR_REGISTRATION = toUrl(server, "/personal/data/calendars/");
        URL APPOINTMENT_REGISTRATION = toUrl(server, "/personal/data/appointments/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);

        DataConsent.Builder projectBuilder = new DataConsent.Builder(dataConsentUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setDataOwner(ALICE_ID).setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(READ_MODES).setScopeOfConsent(SCOPE_ALL_FROM_REGISTRY)
                .setAccessNeed(PROJECTRON_PROJECT_NEED).setDataRegistration(PROJECT_REGISTRATION).build();

        DataConsent.Builder milestoneBuilder = new DataConsent.Builder(milestoneConsentUrl, saiSession);
        DataConsent milestoneConsent = milestoneBuilder.setDataOwner(ALICE_ID).setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(MILESTONE_TREE)
                .setAccessModes(READ_MODES).setScopeOfConsent(SCOPE_INHERITED).setInheritsFrom(projectConsent.getUrl())
                .setAccessNeed(PROJECTRON_PROJECT_NEED).setDataRegistration(MILESTONE_REGISTRATION).build();

        DataConsent.Builder calendarBuilder = new DataConsent.Builder(calendarConsentUrl, saiSession);
        DataConsent calendarConsent = calendarBuilder.setDataOwner(ALICE_ID).setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(CALENDAR_TREE)
                .setAccessModes(READ_MODES).setScopeOfConsent(SCOPE_ALL_FROM_REGISTRY)
                .setAccessNeed(PROJECTRON_CALENDAR_NEED).setDataRegistration(CALENDAR_REGISTRATION).build();

        DataConsent.Builder appointmentBuilder = new DataConsent.Builder(appointmentConsentUrl, saiSession);
        DataConsent appointmentConsent = appointmentBuilder.setDataOwner(ALICE_ID).setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(APPOINTMENT_TREE)
                .setAccessModes(READ_MODES).setScopeOfConsent(SCOPE_INHERITED).setInheritsFrom(calendarConsent.getUrl())
                .setAccessNeed(PROJECTRON_APPOINTMENT_NEED).setDataRegistration(APPOINTMENT_REGISTRATION).build();
        
        AccessConsent.Builder accessBuilder = new AccessConsent.Builder(accessUrl, saiSession);
        AccessConsent accessConsent = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID).setGrantedAt(GRANT_TIME)
                .setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataConsents(Arrays.asList(projectConsent, milestoneConsent, calendarConsent, appointmentConsent)).build();

        AccessGrant accessGrant = accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry));
        assertEquals(4, accessGrant.getDataGrants().size());
    }

    @Test
    @DisplayName("Fail to generate data grants - data consent has inherited scope")
    void failToGenerateDataGrantsAccessScopeInherited() throws SaiNotFoundException, SaiException {
        URL accessUrl = toUrl(server, "/access/registry-1");
        URL agentRegistryUrl = toUrl(server, "/registry-1-agents/");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL registrationUrl = toUrl(server, "/registry-1-agents/registry-1-projectron/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);
        AccessConsent accessConsent = AccessConsent.get(accessUrl, saiSession);
        for (DataConsent dataConsent : accessConsent.getDataConsents()) {
            if (dataConsent.getScopeOfConsent().equals(SCOPE_INHERITED)) {
                assertThrows(SaiException.class, () -> dataConsent.generateGrants(accessConsent, registration, agentRegistry, Arrays.asList(dataRegistry)));
            }
        }
    }

    @Test
    @DisplayName("Fail to generate data grants - data consent has invalid scope")
    void failToGenerateDataGrantsInvalidDataConsentScope() throws SaiNotFoundException, SaiException {
        URL accessUrl = toUrl(server, "/access/invalid-scope");
        URL dataConsentUrl = toUrl(server, "/access/invalid-scope-project");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL agentRegistryUrl = toUrl(server, "/scenario-agents/");
        URL registrationUrl = toUrl(server, "/scenario-agents/scenario-projectron/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);

        DataConsent.Builder projectBuilder = new DataConsent.Builder(dataConsentUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfConsent(SCOPE_ALL).setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        DataConsent spyProject = Mockito.spy(projectConsent);
        when(spyProject.getScopeOfConsent()).thenReturn(ACCESS_GRANT); // NOT A VALID SCOPE TYPE

        AccessConsent.Builder accessBuilder = new AccessConsent.Builder(accessUrl, saiSession);
        AccessConsent accessConsent = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID).setGrantedAt(GRANT_TIME)
                .setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataConsents(Arrays.asList(spyProject)).build();

        assertThrows(SaiException.class, () -> accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry)));
    }

    @Test
    @DisplayName("Fail to generate data grants - specified data registration doesn't exist")
    void failToGenerateDataGrantsInvalidDataRegistration() throws SaiNotFoundException, SaiException {
        URL accessUrl = toUrl(server, "/access/invalid-registration");
        URL dataConsentUrl = toUrl(server, "/access/invalid-registration-project");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL agentRegistryUrl = toUrl(server, "/scenario-agents/");
        URL registrationUrl = toUrl(server, "/scenario-agents/scenario-projectron/");
        URL MISSING_REGISTRATION = toUrl(server, "/personal/data/noprojects/");
        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);

        DataConsent.Builder projectBuilder = new DataConsent.Builder(dataConsentUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES).setDataRegistration(MISSING_REGISTRATION)
                .setScopeOfConsent(SCOPE_ALL_FROM_REGISTRY).setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        AccessConsent.Builder accessBuilder = new AccessConsent.Builder(accessUrl, saiSession);
        AccessConsent accessConsent = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID).setGrantedAt(GRANT_TIME)
                .setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataConsents(Arrays.asList(projectConsent)).build();

        assertThrows(SaiException.class, () -> accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry)));
    }

    @Test
    @DisplayName("Fail to generate data grants - specified child data registration doesn't exist")
    void failToGenerateDataGrantsInvalidChildDataRegistration() throws SaiNotFoundException, SaiException {
        URL accessUrl = toUrl(server, "/access/invalid-registration");
        URL dataConsentUrl = toUrl(server, "/access/invalid-registration-project");
        URL eventConsentUrl = toUrl(server, "/access/invalid-registration-event");
        URL dataRegistryUrl = toUrl(server, "/personal/data/");
        URL agentRegistryUrl = toUrl(server, "/scenario-agents/");
        URL registrationUrl = toUrl(server, "/scenario-agents/scenario-projectron/");
        URL PROJECT_REGISTRATION = toUrl(server, "/personal/data/projects/");
        URL EVENT_TREE = stringToUrl("http://data.example/shapetrees/pm#EventTree");

        AgentRegistry agentRegistry = AgentRegistry.get(agentRegistryUrl, saiSession);
        ApplicationRegistration registration = ApplicationRegistration.get(registrationUrl, saiSession);
        DataRegistry dataRegistry = DataRegistry.get(dataRegistryUrl, saiSession);

        DataConsent.Builder projectBuilder = new DataConsent.Builder(dataConsentUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(PROJECT_TREE)
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES).setDataRegistration(PROJECT_REGISTRATION)
                .setScopeOfConsent(SCOPE_ALL_FROM_REGISTRY).setAccessNeed(PROJECTRON_PROJECT_NEED).build();

        DataConsent.Builder eventBuilder = new DataConsent.Builder(eventConsentUrl, saiSession);
        DataConsent eventConsent = eventBuilder.setGrantedBy(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(EVENT_TREE)  // UNKNOWN
                .setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfConsent(SCOPE_INHERITED).setInheritsFrom(projectConsent.getUrl()).setAccessNeed(PROJECTRON_MILESTONE_NEED).build();

        AccessConsent.Builder accessBuilder = new AccessConsent.Builder(accessUrl, saiSession);
        AccessConsent accessConsent = accessBuilder.setGrantedBy(ALICE_ID).setGrantedWith(JARVIS_ID).setGrantedAt(GRANT_TIME)
                .setGrantee(PROJECTRON_ID).setAccessNeedGroup(PROJECTRON_NEED_GROUP)
                .setDataConsents(Arrays.asList(projectConsent, eventConsent)).build();

        assertThrows(SaiException.class, () -> accessConsent.generateGrant(registration, agentRegistry, Arrays.asList(dataRegistry)));
    }


    @Test
    @DisplayName("Get an access consent and linked data consents - scope: all")
    void getAccessConsent() throws SaiNotFoundException, SaiException {
        URL url = toUrl(server, "/access/all-1");
        AccessConsent accessConsent = AccessConsent.get(url, saiSession);
        checkAccessConsent(accessConsent);
    }

    @Test
    @DisplayName("Reload an access consent and linked data consents - scope: all")
    void reloadAccessConsent() throws SaiNotFoundException, SaiException {
        URL url = toUrl(server, "/access/all-1");
        AccessConsent accessConsent = AccessConsent.get(url, saiSession);
        AccessConsent reloaded = accessConsent.reload();
        checkAccessConsent(reloaded);
    }

    @Test
    @DisplayName("Fail to get access consent - missing required fields")
    void failToGetAccessConsentRequired() {
        URL url = toUrl(server, "/missing-fields/access/all-1");
        assertThrows(SaiException.class, () -> AccessConsent.get(url, saiSession));
    }

    private void checkAccessConsent(AccessConsent accessConsent) {
        assertNotNull(accessConsent);
        assertEquals(ALICE_ID, accessConsent.getGrantedBy());
        assertEquals(JARVIS_ID, accessConsent.getGrantedWith());
        assertEquals(PROJECTRON_ID, accessConsent.getGrantee());
        assertEquals(GRANT_TIME, accessConsent.getGrantedAt());
        assertEquals(PROJECTRON_NEED_GROUP, accessConsent.getAccessNeedGroup());
        for (DataConsent dataConsent : accessConsent.getDataConsents()) { assertTrue(ALL_DATA_CONSENT_URLS.contains(dataConsent.getUrl())); }
    }

    // The most efficient way to ensure that the access grants and data grants generated are
    // correct is to create fixtures that are known to be correct, load them, and then compare.
    private void checkAccessGrantAll(AccessGrant accessGrant) throws SaiNotFoundException, SaiException {

        // Load a known-good "baseline" access grant and data grants from test fixtures to compare against
        URL baselineUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant");
        AccessGrant baselineGrant = AccessGrant.get(baselineUrl, saiSession);

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

    private void checkAccessGrantAllFromRegistry(AccessGrant accessGrant) throws SaiNotFoundException, SaiException {

        // Load a known-good "baseline" access grant and data grants from test fixtures to compare against
        URL baselineUrl = toUrl(server, "/registry-1-agents/registry-1-projectron/registry-1-grant");
        AccessGrant baselineGrant = AccessGrant.get(baselineUrl, saiSession);

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

    private void checkAccessGrantSelectedFromRegistry(AccessGrant accessGrant) throws SaiNotFoundException, SaiException {

        // Load a known-good "baseline" access grant and data grants from test fixtures to compare against
        URL baselineUrl = toUrl(server, "/selected-1-agents/selected-1-projectron/selected-1-grant");
        AccessGrant baselineGrant = AccessGrant.get(baselineUrl, saiSession);

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

    private void checkAccessGrantAllFromAgent(AccessGrant accessGrant) throws SaiNotFoundException, SaiException {

        // Load a known-good "baseline" access grant and data grants from test fixtures to compare against
        URL baselineUrl = toUrl(server, "/agent-1-agents/agent-1-projectron/agent-1-grant");
        AccessGrant baselineGrant = AccessGrant.get(baselineUrl, saiSession);

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

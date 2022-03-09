package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.crud.AgentRegistry;
import com.janeirodigital.sai.core.crud.ApplicationRegistration;
import com.janeirodigital.sai.core.crud.DataRegistry;
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
        
        // Get Alice's data registries - doesn't change across use cases
        mockOnGet(dispatcher, "/personal/data/", "data/alice/personal-data-registry-ttl");
        mockOnGet(dispatcher, "/personal/data/projects/", "data/alice/personal-data-registration-projects-ttl");
        mockOnGet(dispatcher, "/personal/data/milestones/", "data/alice/personal-data-registration-milestones-ttl");
        mockOnGet(dispatcher, "/personal/data/issues/", "data/alice/personal-data-registration-issues-ttl");
        mockOnGet(dispatcher, "/personal/data/tasks/", "data/alice/personal-data-registration-tasks-ttl");
        mockOnGet(dispatcher, "/work/data/", "data/alice/work-data-registry-ttl");
        mockOnGet(dispatcher, "/work/data/projects/", "data/alice/work-data-registration-projects-ttl");
        mockOnGet(dispatcher, "/work/data/milestones/", "data/alice/work-data-registration-milestones-ttl");
        mockOnGet(dispatcher, "/work/data/issues/", "data/alice/work-data-registration-issues-ttl");
        mockOnGet(dispatcher, "/work/data/tasks/", "data/alice/work-data-registration-tasks-ttl");

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

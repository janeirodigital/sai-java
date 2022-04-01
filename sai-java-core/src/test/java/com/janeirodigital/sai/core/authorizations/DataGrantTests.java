package com.janeirodigital.sai.core.authorizations;

import com.janeirodigital.mockwebserver.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpException;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
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

import static com.janeirodigital.mockwebserver.DispatcherHelper.mockOnGet;
import static com.janeirodigital.mockwebserver.DispatcherHelper.mockOnPut;
import static com.janeirodigital.mockwebserver.MockWebServerHelper.toMockUri;
import static com.janeirodigital.sai.core.vocabularies.AclVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DataGrantTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static URI ALICE_ID;
    private static URI BOB_ID;
    private static URI JARVIS_ID;
    private static URI PROJECTRON_ID;
    private static URI PROJECTRON_NEED_GROUP;
    private static URI PROJECTS_DATA_REGISTRATION, MILESTONES_DATA_REGISTRATION, ISSUES_DATA_REGISTRATION, TASKS_DATA_REGISTRATION;
    private static URI BOB_PROJECTS_DATA_REGISTRATION, BOB_PROJECTS_DATA_GRANT;
    private static URI PROJECT_TREE, MILESTONE_TREE, ISSUE_TREE, TASK_TREE;
    private static URI PROJECTRON_PROJECT_NEED, PROJECTRON_MILESTONE_NEED, PROJECTRON_ISSUE_NEED, PROJECTRON_TASK_NEED;
    private static OffsetDateTime GRANT_TIME;
    private static List<URI> ALL_DATA_GRANT_URIS;
    private static List<RDFNode> ACCESS_MODES, CREATOR_ACCESS_MODES, READ_MODES, WRITE_MODES;

    @BeforeAll
    static void beforeAll() throws SaiException, SaiHttpException {
        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));
        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        // GET data grant in Turtle with missing fields
        mockOnGet(dispatcher, "/missing-fields/all-1-agents/all-1-projectron/all-1-grant-personal-project", "agents/alice/projectron-all/all-1-grant-personal-project-missing-fields-ttl");
        // GET / PUT access grant and data grants from the agent registry
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-personal-project", "agents/alice/projectron-all/all-1-grant-personal-project-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-project-readonly", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-project-readonly-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-project-cancreate", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-project-cancreate-ttl");
        mockOnGet(dispatcher, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-project-canwrite", "agents/alice/projectron-all-from-registry/registry-1-grant-personal-project-canwrite-ttl");
        mockOnPut(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-personal-project", "http/201");
        mockOnGet(dispatcher, "/agents/projectron/selected-1-grant-project", "agents/alice/projectron-selected/selected-1-grant-project-ttl");
        mockOnPut(dispatcher, "/agents/projectron/selected-1-grant-project", "http/201");
        mockOnPut(dispatcher, "/agents/projectron/selected-1-grant-milestone", "http/201");
        mockOnGet(dispatcher, "/agents/projectron/agent-1-grant-project", "agents/alice/projectron-all-from-agent/agent-1-grant-project-ttl");
        mockOnPut(dispatcher, "/agents/projectron/agent-1-grant-project", "http/201");

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
                                            toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-task"));
        BOB_PROJECTS_DATA_GRANT = toMockUri(server, "/bob/agents/alice/selected-1-grant-project");
        PROJECTS_DATA_REGISTRATION = toMockUri(server, "/personal/data/projects/");
        BOB_PROJECTS_DATA_REGISTRATION = toMockUri(server, "/bob/data/projects/");
        MILESTONES_DATA_REGISTRATION = toMockUri(server, "/personal/data/milestones/");
        ISSUES_DATA_REGISTRATION = toMockUri(server, "/personal/data/issues/");
        TASKS_DATA_REGISTRATION = toMockUri(server, "/personal/data/tasks/");
        PROJECT_TREE = toMockUri(server, "/shapetrees/pm#ProjectTree");
        MILESTONE_TREE = toMockUri(server, "/shapetrees/pm#MilestoneTree");
        ISSUE_TREE = toMockUri(server, "/shapetrees/pm#IssueTree");
        TASK_TREE = toMockUri(server, "/shapetrees/pm#TaskTree");
        ACCESS_MODES = Arrays.asList(ACL_READ, ACL_CREATE);
        READ_MODES = Arrays.asList(ACL_READ);
        WRITE_MODES = Arrays.asList(ACL_WRITE);
        CREATOR_ACCESS_MODES = Arrays.asList(ACL_UPDATE, ACL_DELETE);
         
    }

    @Test
    @DisplayName("Create new data grant - scope: all from registry")
    void createDataGrantAllFromRegistry() throws SaiException {
        URI projectUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        DataGrant.Builder projectBuilder = new DataGrant.Builder(projectUri, saiSession);
        DataGrant projectGrant = projectBuilder.setDataOwner(ALICE_ID)
                                               .setGrantee(PROJECTRON_ID)
                                               .setRegisteredShapeTree(PROJECT_TREE)
                                               .setDataRegistration(PROJECTS_DATA_REGISTRATION)
                                               .setAccessModes(WRITE_MODES)
                                               .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                                               .setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY)
                                               .setAccessNeed(PROJECTRON_PROJECT_NEED)
                                               .build();
        assertDoesNotThrow(() -> projectGrant.create());
        assertTrue(projectGrant.canCreate());
    }

    @Test
    @DisplayName("Create new data grant - scope: selected from registry")
    void createDataGrantSelected() throws SaiException {
        URI projectUri = toMockUri(server, "/agents/projectron/selected-1-grant-project");
        List<URI> dataInstances = Arrays.asList(toMockUri(server, "/personal/data/projects/project-1"),
                toMockUri(server, "/personal/data/projects/project-2"),
                toMockUri(server, "/personal/data/projects/project-3"));
        DataGrant.Builder projectBuilder = new DataGrant.Builder(projectUri, saiSession);
        DataGrant projectGrant = projectBuilder.setDataOwner(ALICE_ID)
                                                .setGrantee(PROJECTRON_ID)
                                                .setRegisteredShapeTree(PROJECT_TREE)
                                                .setDataRegistration(PROJECTS_DATA_REGISTRATION)
                                                .setAccessModes(READ_MODES)
                                                .setScopeOfGrant(SCOPE_SELECTED_FROM_REGISTRY)
                                                .setAccessNeed(PROJECTRON_PROJECT_NEED)
                                                .setDataInstances(dataInstances)
                                                .build();
        assertDoesNotThrow(() -> projectGrant.create());
        assertFalse(projectGrant.canCreate());
    }

    @Test
    @DisplayName("Create new data grant - scope: delegated")
    void createDataGrantDelegated() throws SaiException {
        URI projectUri = toMockUri(server, "/agents/projectron/agent-1-grant-project");
        List<URI> dataInstances = Arrays.asList(toMockUri(server, "/bob/data/projects/project-1"),
                                                toMockUri(server, "/bob/data/projects/project-2"),
                                                toMockUri(server, "/bob/data/projects/project-3"));
        DataGrant.Builder projectBuilder = new DataGrant.Builder(projectUri, saiSession);
        DataGrant projectGrant = projectBuilder.setDataOwner(BOB_ID)
                                               .setGrantee(PROJECTRON_ID)
                                               .setRegisteredShapeTree(PROJECT_TREE)
                                               .setDataRegistration(BOB_PROJECTS_DATA_REGISTRATION)
                                               .setAccessModes(READ_MODES)
                                               .setScopeOfGrant(SCOPE_SELECTED_FROM_REGISTRY)
                                               .setAccessNeed(PROJECTRON_PROJECT_NEED)
                                               .setDataInstances(dataInstances)
                                               .setDelegationOf(BOB_PROJECTS_DATA_GRANT)
                                               .build();
        assertDoesNotThrow(() -> projectGrant.create());
    }

    @Test
    @DisplayName("Create new data grant - scope: inherited")
    void createDataGrantInherited() throws SaiException {
        URI projectUri = toMockUri(server, "/agents/projectron/selected-1-grant-project");
        URI milestoneUri = toMockUri(server, "/agents/projectron/selected-1-grant-milestone");
        DataGrant.Builder projectBuilder = new DataGrant.Builder(milestoneUri, saiSession);
        DataGrant milestoneGrant = projectBuilder.setDataOwner(ALICE_ID)
                                                 .setGrantee(PROJECTRON_ID)
                                                 .setRegisteredShapeTree(MILESTONE_TREE)
                                                 .setDataRegistration(MILESTONES_DATA_REGISTRATION)
                                                 .setAccessModes(ACCESS_MODES)
                                                 .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                                                 .setScopeOfGrant(SCOPE_INHERITED)
                                                 .setAccessNeed(PROJECTRON_PROJECT_NEED)
                                                 .setInheritsFrom(projectUri)
                                                 .build();
        assertDoesNotThrow(() -> milestoneGrant.create());
    }

    @Test
    @DisplayName("Create new data grant - scope: no access")
    void createDataGrantNoAccess() throws SaiException {
        URI projectUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        DataGrant.Builder projectBuilder = new DataGrant.Builder(projectUri, saiSession);
        DataGrant projectGrant = projectBuilder.setDataOwner(ALICE_ID)
                .setGrantee(PROJECTRON_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(PROJECTS_DATA_REGISTRATION)
                .setAccessModes(ACCESS_MODES)
                .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfGrant(SCOPE_NO_ACCESS)
                .setAccessNeed(PROJECTRON_PROJECT_NEED)
                .build();
        assertDoesNotThrow(() -> projectGrant.create());
    }

    @Test
    @DisplayName("Fail to create new data grant - invalid scope")
    void failToCreateDataGrantBadScope() throws SaiException {
        URI projectUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        DataGrant.Builder projectBuilder = new DataGrant.Builder(projectUri, saiSession);
        assertThrows(SaiException.class, () -> projectBuilder.setDataOwner(ALICE_ID)
                                                              .setGrantee(PROJECTRON_ID)
                                                             .setRegisteredShapeTree(PROJECT_TREE)
                                                             .setDataRegistration(PROJECTS_DATA_REGISTRATION)
                                                             .setAccessModes(ACCESS_MODES)
                                                             .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                                                             .setScopeOfGrant(ACCESS_GRANT) // INVALID
                                                             .setAccessNeed(PROJECTRON_PROJECT_NEED)
                                                             .build());
    }

    @Test
    @DisplayName("Fail to create new data grant - missing creator modes")
    void failToCreateDataGrantMissingCreatorModes() {
        URI projectUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        DataGrant.Builder projectBuilder = new DataGrant.Builder(projectUri, saiSession);
        assertThrows(SaiException.class, () -> projectBuilder.setDataOwner(ALICE_ID)
                .setGrantee(PROJECTRON_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(PROJECTS_DATA_REGISTRATION)
                .setAccessModes(ACCESS_MODES)
                .setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY)
                .setAccessNeed(PROJECTRON_PROJECT_NEED)
                .build());
    }

    @Test
    @DisplayName("Fail to create new data grant - inherits from without inherited scope")
    void failToCreateDataGrantInvalidInheritsFrom() {
        URI projectUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        DataGrant.Builder projectBuilder = new DataGrant.Builder(projectUri, saiSession);
        assertThrows(SaiException.class, () -> projectBuilder.setDataOwner(ALICE_ID)
                .setGrantee(PROJECTRON_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(PROJECTS_DATA_REGISTRATION)
                .setAccessModes(ACCESS_MODES)
                .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY)
                .setAccessNeed(PROJECTRON_PROJECT_NEED)
                .setInheritsFrom(projectUri)  // Only allowed when scope is inherited
                .build());
    }

    @Test
    @DisplayName("Fail to create new data grant - inherited scope without inherits from")
    void failToCreateDataGrantInheritsFromMissing() {
        URI projectUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        DataGrant.Builder projectBuilder = new DataGrant.Builder(projectUri, saiSession);
        assertThrows(SaiException.class, () -> projectBuilder.setDataOwner(ALICE_ID)
                .setGrantee(PROJECTRON_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(PROJECTS_DATA_REGISTRATION)
                .setAccessModes(ACCESS_MODES)
                .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfGrant(SCOPE_INHERITED)
                .setAccessNeed(PROJECTRON_PROJECT_NEED)
                .build());
    }

    @Test
    @DisplayName("Fail to create new data grant - selected instances without selected scope")
    void failToCreateDataGrantInstancesNoSelected() {
        URI projectUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        List<URI> dataInstances = Arrays.asList(toMockUri(server, "/personal/data/projects/project-1"),
                toMockUri(server, "/personal/data/projects/project-2"),
                toMockUri(server, "/personal/data/projects/project-3"));
        DataGrant.Builder projectBuilder = new DataGrant.Builder(projectUri, saiSession);
        assertThrows(SaiException.class, () -> projectBuilder.setDataOwner(ALICE_ID)
                .setGrantee(PROJECTRON_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(PROJECTS_DATA_REGISTRATION)
                .setAccessModes(READ_MODES)
                .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY)
                .setAccessNeed(PROJECTRON_PROJECT_NEED)
                .setDataInstances(dataInstances)
                .build());
    }

    @Test
    @DisplayName("Fail to create new data grant - selected scope without selected instances")
    void failToCreateDataGrantSelectedNoInstances() {
        URI projectUri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        List<URI> dataInstances = Arrays.asList(toMockUri(server, "/personal/data/projects/project-1"),
                toMockUri(server, "/personal/data/projects/project-2"),
                toMockUri(server, "/personal/data/projects/project-3"));
        DataGrant.Builder projectBuilder = new DataGrant.Builder(projectUri, saiSession);
        assertThrows(SaiException.class, () -> projectBuilder.setDataOwner(ALICE_ID)
                .setGrantee(PROJECTRON_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(PROJECTS_DATA_REGISTRATION)
                .setAccessModes(READ_MODES)
                .setScopeOfGrant(SCOPE_SELECTED_FROM_REGISTRY)
                .setAccessNeed(PROJECTRON_PROJECT_NEED)
                .build());
    }

    @Test
    @DisplayName("Get data grant - scope: all from registry")
    void getDataGrant() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        DataGrant dataGrant = DataGrant.get(uri, saiSession);
        assertEquals(ALICE_ID, dataGrant.getDataOwner());
        assertEquals(PROJECTRON_ID, dataGrant.getGrantee());
        assertEquals(PROJECT_TREE, dataGrant.getRegisteredShapeTree());
        assertEquals(PROJECTS_DATA_REGISTRATION, dataGrant.getDataRegistration());
        for (RDFNode mode : dataGrant.getAccessModes()) { assertTrue(ACCESS_MODES.contains(mode)); }
        for (RDFNode mode : dataGrant.getCreatorAccessModes()) { assertTrue(CREATOR_ACCESS_MODES.contains(mode)); }
        assertEquals(SCOPE_ALL_FROM_REGISTRY, dataGrant.getScopeOfGrant());
        assertEquals(PROJECTRON_PROJECT_NEED, dataGrant.getAccessNeed());
    }

    @Test
    @DisplayName("Get readable read-only data grant - scope: all from registry")
    void getReadOnlyDataGrant() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-project-readonly");
        ReadableDataGrant dataGrant = ReadableDataGrant.get(uri, saiSession);
        assertFalse(dataGrant.canCreate());
    }

    @Test
    @DisplayName("Get readable data grant with create privileges - scope: all from registry")
    void getReadOnlyDataGrantCreate() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-project-cancreate");
        ReadableDataGrant dataGrant = ReadableDataGrant.get(uri, saiSession);
        assertTrue(dataGrant.canCreate());
    }

    @Test
    @DisplayName("Get readable data grant with write privileges - scope: all from registry")
    void getReadOnlyDataGrantWrite() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/registry-1-agents/registry-1-projectron/registry-1-grant-personal-project-canwrite");
        ReadableDataGrant dataGrant = ReadableDataGrant.get(uri, saiSession);
        assertTrue(dataGrant.canCreate());
    }

    @Test
    @DisplayName("Reload data grant - scope: all from registry")
    void reloadDataGrant() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        DataGrant dataGrant = DataGrant.get(uri, saiSession);
        DataGrant reloaded = dataGrant.reload();
        assertEquals(ALICE_ID, reloaded.getDataOwner());
        assertEquals(PROJECTRON_ID, reloaded.getGrantee());
        assertEquals(PROJECT_TREE, reloaded.getRegisteredShapeTree());
        assertEquals(PROJECTS_DATA_REGISTRATION, reloaded.getDataRegistration());
        for (RDFNode mode : reloaded.getAccessModes()) { assertTrue(ACCESS_MODES.contains(mode)); }
        for (RDFNode mode : reloaded.getCreatorAccessModes()) { assertTrue(CREATOR_ACCESS_MODES.contains(mode)); }
        assertEquals(SCOPE_ALL_FROM_REGISTRY, reloaded.getScopeOfGrant());
        assertEquals(PROJECTRON_PROJECT_NEED, reloaded.getAccessNeed());
    }

    @Test
    @DisplayName("Reload readable data grant - scope: all from registry")
    void reloadReadableDataGrant() throws SaiException, SaiHttpNotFoundException {
        URI uri = toMockUri(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        ReadableDataGrant dataGrant = ReadableDataGrant.get(uri, saiSession);
        ReadableDataGrant reloaded = dataGrant.reload();
        assertEquals(ALICE_ID, reloaded.getDataOwner());
        assertEquals(PROJECTRON_ID, reloaded.getGrantee());
        assertEquals(PROJECT_TREE, reloaded.getRegisteredShapeTree());
        assertEquals(PROJECTS_DATA_REGISTRATION, reloaded.getDataRegistration());
        for (RDFNode mode : reloaded.getAccessModes()) { assertTrue(ACCESS_MODES.contains(mode)); }
        for (RDFNode mode : reloaded.getCreatorAccessModes()) { assertTrue(CREATOR_ACCESS_MODES.contains(mode)); }
        assertEquals(SCOPE_ALL_FROM_REGISTRY, reloaded.getScopeOfGrant());
        assertEquals(PROJECTRON_PROJECT_NEED, reloaded.getAccessNeed());
    }

    @Test
    @DisplayName("Fail to get data grant - missing required fields")
    void failToGetDataGrantRequired() {
        URI uri = toMockUri(server, "/missing-fields/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        assertThrows(SaiException.class, () -> DataGrant.get(uri, saiSession));
    }

    @Test
    @DisplayName("Fail to get readable data grant - missing required fields")
    void failToGetReadableDataGrantRequired() {
        URI uri = toMockUri(server, "/missing-fields/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        assertThrows(SaiException.class, () -> ReadableDataGrant.get(uri, saiSession));
    }

}

package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpException;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
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
import static com.janeirodigital.sai.core.vocabularies.AclVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.httputils.HttpUtils.stringToUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class DataAuthorizationTests {

    private static SaiSession saiSession;
    private static MockWebServer server;
    private static URL ALICE_ID;
    private static URL BOB_ID;
    private static URL JARVIS_ID;
    private static URL PROJECTRON_ID;
    private static URL PROJECTRON_NEED_GROUP;
    private static URL PROJECT_TREE, MILESTONE_TREE, ISSUE_TREE, TASK_TREE;
    private static URL PROJECTRON_PROJECT_NEED, PROJECTRON_MILESTONE_NEED, PROJECTRON_ISSUE_NEED, PROJECTRON_TASK_NEED;
    private static OffsetDateTime GRANT_TIME;
    private static List<URL> ALL_DATA_AUTHORIZATION_URLS;
    private static List<RDFNode> ACCESS_MODES, CREATOR_ACCESS_MODES, READ_MODES, WRITE_MODES;

    @BeforeAll
    static void beforeAll() throws SaiException, SaiHttpException {
        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));
        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        // GET agent registry in Turtle
        mockOnGet(dispatcher, "/authorization/all-1-project", "authorization/all/all-1-project-ttl");
        mockOnPut(dispatcher, "/authorization/all-1-project", "http/201");
        mockOnGet(dispatcher, "/authorization/registry-1-project", "authorization/all-from-registry/registry-1-project-ttl");
        mockOnPut(dispatcher, "/authorization/registry-1-project", "http/201");
        mockOnGet(dispatcher, "/authorization/registry-1-milestone", "authorization/all-from-registry/registry-1-milestone-ttl");
        mockOnPut(dispatcher, "/authorization/registry-1-milestone", "http/201");
        mockOnGet(dispatcher, "/authorization/agent-1-project", "authorization/all-from-agent/agent-1-project-ttl");
        mockOnPut(dispatcher, "/authorization/agent-1-project", "http/201");
        mockOnGet(dispatcher, "/authorization/selected-1-project", "authorization/selected-from-registry/selected-1-project-ttl");
        mockOnPut(dispatcher, "/authorization/selected-1-project", "http/201");

        // GET data authorization in Turtle with missing fields
        mockOnGet(dispatcher, "/missing-fields/authorization/all-1-project", "authorization/all/all-1-project-missing-fields-ttl");
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
        ALL_DATA_AUTHORIZATION_URLS = Arrays.asList(toUrl(server, "/authorization/all-1-project"), toUrl(server, "/authorization/all-1-milestone"),
                                              toUrl(server, "/authorization/all-1-issue"), toUrl(server, "/authorization/all-1-task"));
        PROJECT_TREE = toUrl(server, "/shapetrees/pm#ProjectTree");
        MILESTONE_TREE = toUrl(server, "/shapetrees/pm#MilestoneTree");
        ISSUE_TREE = toUrl(server, "/shapetrees/pm#IssueTree");
        TASK_TREE = toUrl(server, "/shapetrees/pm#TaskTree");
        ACCESS_MODES = Arrays.asList(ACL_READ, ACL_CREATE);
        READ_MODES = Arrays.asList(ACL_READ);
        WRITE_MODES = Arrays.asList(ACL_WRITE);
        CREATOR_ACCESS_MODES = Arrays.asList(ACL_UPDATE, ACL_DELETE);
         
    }

    @Test
    @DisplayName("Create new data authorization - scope: all")
    void createAll() throws SaiException {
        URL projectUrl = toUrl(server, "/authorization/all-1-project");
        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectUrl, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setGrantedBy(ALICE_ID)
                                                   .setGrantee(PROJECTRON_ID)
                                                   .setRegisteredShapeTree(PROJECT_TREE)
                                                   .setAccessModes(ACCESS_MODES)
                                                   .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                                                   .setScopeOfAuthorization(SCOPE_ALL)
                                                   .setAccessNeed(PROJECTRON_PROJECT_NEED)
                                                   .build();
        assertDoesNotThrow(() -> projectAuthorization.create());
        assertTrue(projectAuthorization.canCreate());
    }

    @Test
    @DisplayName("Create new data authorization - scope: all from registry")
    void createAllFromRegistry() throws SaiException {
        URL projectUrl = toUrl(server, "/authorization/registry-1-project");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectUrl, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setDataOwner(ALICE_ID)
                                                   .setGrantedBy(ALICE_ID)
                                                   .setGrantee(PROJECTRON_ID)
                                                   .setRegisteredShapeTree(PROJECT_TREE)
                                                   .setDataRegistration(drUrl)
                                                   .setAccessModes(WRITE_MODES)
                                                   .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                                                   .setScopeOfAuthorization(SCOPE_ALL_FROM_REGISTRY)
                                                   .setAccessNeed(PROJECTRON_PROJECT_NEED)
                                                   .build();
        assertDoesNotThrow(() -> projectAuthorization.create());
        assertTrue(projectAuthorization.canCreate());
    }

    @Test
    @DisplayName("Create new data authorization - scope: selected from registry")
    void createSelectedFromRegistry() throws SaiException {
        URL projectUrl = toUrl(server, "/authorization/selected-1-project");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        List<URL> dataInstances = Arrays.asList(toUrl(server, "/personal/data/projects/project-1"),
                                                toUrl(server, "/personal/data/projects/project-2"),
                                                toUrl(server, "/personal/data/projects/project-3"));
        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectUrl, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setDataOwner(ALICE_ID)
                                                    .setGrantedBy(ALICE_ID)
                                                    .setGrantee(PROJECTRON_ID)
                                                    .setRegisteredShapeTree(PROJECT_TREE)
                                                    .setDataRegistration(drUrl)
                                                    .setAccessModes(READ_MODES)
                                                    .setScopeOfAuthorization(SCOPE_SELECTED_FROM_REGISTRY)
                                                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                                                    .setDataInstances(dataInstances)
                                                    .build();
        assertDoesNotThrow(() -> projectAuthorization.create());
        assertFalse(projectAuthorization.canCreate());
    }

    @Test
    @DisplayName("Create new data authorization - scope: all from agent")
    void createAllFromAgent() throws SaiException {
        URL projectUrl = toUrl(server, "/authorization/agent-1-project");
        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectUrl, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setDataOwner(BOB_ID)
                                                   .setGrantedBy(ALICE_ID)
                                                   .setGrantee(PROJECTRON_ID)
                                                   .setRegisteredShapeTree(PROJECT_TREE)
                                                   .setAccessModes(ACCESS_MODES)
                                                   .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                                                   .setScopeOfAuthorization(SCOPE_ALL_FROM_AGENT)
                                                   .setAccessNeed(PROJECTRON_PROJECT_NEED)
                                                   .build();
        assertDoesNotThrow(() -> projectAuthorization.create());
    }

    @Test
    @DisplayName("Create new data authorization - scope: inherited")
    void createInherited() throws SaiException {
        URL projectUrl = toUrl(server, "/authorization/project-1-milestone");
        URL milestoneUrl = toUrl(server, "/authorization/registry-1-milestone");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataAuthorization.Builder milestoneBuilder = new DataAuthorization.Builder(milestoneUrl, saiSession);
        DataAuthorization milestoneAuthorization = milestoneBuilder.setDataOwner(ALICE_ID)
                .setGrantedBy(ALICE_ID)
                .setGrantee(PROJECTRON_ID)
                .setRegisteredShapeTree(MILESTONE_TREE)
                .setDataRegistration(drUrl)
                .setAccessModes(ACCESS_MODES)
                .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfAuthorization(SCOPE_INHERITED)
                .setAccessNeed(PROJECTRON_MILESTONE_NEED)
                .setInheritsFrom(projectUrl)
                .build();
        assertDoesNotThrow(() -> milestoneAuthorization.create());
    }

    @Test
    @DisplayName("Create new data authorization - scope: no access")
    void createNoAccess() throws SaiException {
        URL projectUrl = toUrl(server, "/authorization/selected-1-project");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectUrl, saiSession);
        DataAuthorization projectAuthorization = projectBuilder.setDataOwner(ALICE_ID)
                .setGrantedBy(ALICE_ID)
                .setGrantee(PROJECTRON_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(drUrl)
                .setAccessModes(ACCESS_MODES)
                .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfAuthorization(SCOPE_NO_ACCESS)
                .setAccessNeed(PROJECTRON_PROJECT_NEED)
                .build();
        assertDoesNotThrow(() -> projectAuthorization.create());
    }

    // Generate data grants

    @Test
    @DisplayName("Fail to create new data authorization - scope: invalid")
    void failToCreateInvalidScope() throws SaiException {
        URL projectUrl = toUrl(server, "/authorization/selected-1-project");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
        DataAuthorization projectAuthorization = projectBuilder.setDataOwner(ALICE_ID)
                .setGrantedBy(ALICE_ID)
                .setGrantee(PROJECTRON_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(drUrl)
                .setAccessModes(ACCESS_MODES)
                .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfAuthorization(ACCESS_GRANT)  // INVALID
                .setAccessNeed(PROJECTRON_PROJECT_NEED)
                .build();
        });
    }

    @Test
    @DisplayName("Fail to create new data authorization - create privileges and no creator modes")
    void failToCreateInvalidCreator() throws SaiException {
        URL projectUrl = toUrl(server, "/authorization/selected-1-project");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
            DataAuthorization projectAuthorization = projectBuilder.setDataOwner(ALICE_ID)
                    .setGrantedBy(ALICE_ID)
                    .setGrantee(PROJECTRON_ID)
                    .setRegisteredShapeTree(PROJECT_TREE)
                    .setDataRegistration(drUrl)
                    .setAccessModes(ACCESS_MODES)  // can create but no creator modes are set
                    .setScopeOfAuthorization(SCOPE_ALL_FROM_REGISTRY)
                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                    .build();
        });
    }

    @Test
    @DisplayName("Fail to create new data authorization - not inherited and inherits from data authorization")
    void failToCreateInvalidInheritsFrom() {
        URL projectUrl = toUrl(server, "/authorization/all-1-project");
        URL milestoneUrl = toUrl(server, "/authorization/all-1-milestone");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
            DataAuthorization projectAuthorization = projectBuilder.setDataOwner(ALICE_ID)
                    .setGrantedBy(ALICE_ID)
                    .setGrantee(PROJECTRON_ID)
                    .setRegisteredShapeTree(PROJECT_TREE)
                    .setDataRegistration(drUrl)
                    .setAccessModes(ACCESS_MODES)
                    .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                    .setScopeOfAuthorization(SCOPE_ALL_FROM_REGISTRY)
                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                    .setInheritsFrom(milestoneUrl)
                    .build();
        });
    }

    @Test
    @DisplayName("Fail to create new data authorization - selected with no instances")
    void failToCreateSelectedNoInstances() throws SaiException {
        URL projectUrl = toUrl(server, "/authorization/selected-1-project");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
            DataAuthorization projectAuthorization = projectBuilder.setDataOwner(ALICE_ID)
                    .setGrantedBy(ALICE_ID)
                    .setGrantee(PROJECTRON_ID)
                    .setRegisteredShapeTree(PROJECT_TREE)
                    .setDataRegistration(drUrl)
                    .setAccessModes(READ_MODES)
                    .setScopeOfAuthorization(SCOPE_SELECTED_FROM_REGISTRY)
                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                    .build();
            assertDoesNotThrow(() -> projectAuthorization.create());
            assertFalse(projectAuthorization.canCreate());
        });
    }

    @Test
    @DisplayName("Fail to create new data authorization - selected with no instances")
    void failToCreateSelectedNoRegistration() {
        URL projectUrl = toUrl(server, "/authorization/selected-1-project");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        List<URL> dataInstances = Arrays.asList(toUrl(server, "/personal/data/projects/project-1"),
                toUrl(server, "/personal/data/projects/project-2"),
                toUrl(server, "/personal/data/projects/project-3"));
        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
            DataAuthorization projectAuthorization = projectBuilder.setDataOwner(ALICE_ID)
                    .setGrantedBy(ALICE_ID)
                    .setGrantee(PROJECTRON_ID)
                    .setRegisteredShapeTree(PROJECT_TREE)
                    .setAccessModes(READ_MODES)
                    .setScopeOfAuthorization(SCOPE_SELECTED_FROM_REGISTRY)
                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                    .setDataInstances(dataInstances)
                    .build();
            assertDoesNotThrow(() -> projectAuthorization.create());
            assertFalse(projectAuthorization.canCreate());
        });
    }

    @Test
    @DisplayName("Fail to create new data authorization - has instances and no selected scope")
    void failToCreateInstancesNoSelected() {
        URL projectUrl = toUrl(server, "/authorization/selected-1-project");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        List<URL> dataInstances = Arrays.asList(toUrl(server, "/personal/data/projects/project-1"),
                toUrl(server, "/personal/data/projects/project-2"),
                toUrl(server, "/personal/data/projects/project-3"));
        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
            DataAuthorization projectAuthorization = projectBuilder.setDataOwner(ALICE_ID)
                    .setGrantedBy(ALICE_ID)
                    .setGrantee(PROJECTRON_ID)
                    .setRegisteredShapeTree(PROJECT_TREE)
                    .setDataRegistration(drUrl)
                    .setAccessModes(READ_MODES)
                    .setScopeOfAuthorization(SCOPE_ALL_FROM_REGISTRY)
                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                    .setDataInstances(dataInstances)
                    .build();
            assertDoesNotThrow(() -> projectAuthorization.create());
            assertFalse(projectAuthorization.canCreate());
        });
    }

    @Test
    @DisplayName("Fail to create new data authorization - scope of all but data registration provided")
    void failToCreateAllAndDataReg() {
        URL projectUrl = toUrl(server, "/authorization/all-1-project");
        URL milestoneUrl = toUrl(server, "/authorization/all-1-milestone");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
            DataAuthorization projectAuthorization = projectBuilder.setGrantedBy(ALICE_ID)
                    .setGrantee(PROJECTRON_ID)
                    .setRegisteredShapeTree(PROJECT_TREE)
                    .setDataRegistration(drUrl)
                    .setAccessModes(ACCESS_MODES)
                    .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                    .setScopeOfAuthorization(SCOPE_ALL)
                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                    .build();
        });
    }

    @Test
    @DisplayName("Fail to create new data authorization - scope of all but data owner is set")
    void failToCreateAllAndDataOwner() {
        URL projectUrl = toUrl(server, "/authorization/all-1-project");
        URL milestoneUrl = toUrl(server, "/authorization/all-1-milestone");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
            DataAuthorization projectAuthorization = projectBuilder.setDataOwner(ALICE_ID)
                    .setGrantedBy(ALICE_ID)
                    .setGrantee(PROJECTRON_ID)
                    .setRegisteredShapeTree(PROJECT_TREE)
                    .setAccessModes(ACCESS_MODES)
                    .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                    .setScopeOfAuthorization(SCOPE_ALL)
                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                    .build();
        });
    }

    @Test
    @DisplayName("Fail to create new data authorization - all from registry scope but no data registration")
    void failToCreateAllFromRegNoDataReg() {
        URL projectUrl = toUrl(server, "/authorization/all-1-project");
        URL milestoneUrl = toUrl(server, "/authorization/all-1-milestone");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
            DataAuthorization projectAuthorization = projectBuilder.setDataOwner(ALICE_ID)
                    .setGrantedBy(ALICE_ID)
                    .setGrantee(PROJECTRON_ID)
                    .setRegisteredShapeTree(PROJECT_TREE)
                    .setAccessModes(ACCESS_MODES)
                    .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                    .setScopeOfAuthorization(SCOPE_ALL_FROM_REGISTRY)
                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                    .build();
        });
    }

    @Test
    @DisplayName("Fail to create new data authorization - inherited but no inherits from")
    void failToCreateInheritedNoInheritsFrom() {
        URL projectUrl = toUrl(server, "/authorization/all-1-project");
        URL milestoneUrl = toUrl(server, "/authorization/all-1-milestone");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataAuthorization.Builder projectBuilder = new DataAuthorization.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
            DataAuthorization projectAuthorization = projectBuilder.setDataOwner(BOB_ID)
                    .setGrantedBy(ALICE_ID)
                    .setGrantee(PROJECTRON_ID)
                    .setRegisteredShapeTree(PROJECT_TREE)
                    .setDataRegistration(drUrl)
                    .setAccessModes(ACCESS_MODES)
                    .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                    .setScopeOfAuthorization(SCOPE_INHERITED)
                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                    .build();
        });
    }

    @Test
    @DisplayName("Get data authorization - scope: all")
    void getDataAuthorization() throws SaiException, SaiHttpNotFoundException {
        URL url = toUrl(server, "/authorization/all-1-project");
        DataAuthorization dataAuthorization = DataAuthorization.get(url, saiSession);
        assertEquals(ALICE_ID, dataAuthorization.getGrantedBy());
        assertEquals(PROJECTRON_ID, dataAuthorization.getGrantee());
        assertEquals(PROJECT_TREE, dataAuthorization.getRegisteredShapeTree());
        for (RDFNode mode : dataAuthorization.getAccessModes()) { assertTrue(ACCESS_MODES.contains(mode)); }
        for (RDFNode mode : dataAuthorization.getCreatorAccessModes()) { assertTrue(CREATOR_ACCESS_MODES.contains(mode)); }
        assertEquals(SCOPE_ALL, dataAuthorization.getScopeOfAuthorization());
        assertEquals(PROJECTRON_PROJECT_NEED, dataAuthorization.getAccessNeed());
    }

    @Test
    @DisplayName("Reload data authorization - scope: all")
    void reloadDataAuthorization() throws SaiException, SaiHttpNotFoundException {
        URL url = toUrl(server, "/authorization/all-1-project");
        DataAuthorization dataAuthorization = DataAuthorization.get(url, saiSession);
        DataAuthorization reloaded = dataAuthorization.reload();
        assertEquals(ALICE_ID, reloaded.getGrantedBy());
        assertEquals(PROJECTRON_ID, reloaded.getGrantee());
        assertEquals(PROJECT_TREE, reloaded.getRegisteredShapeTree());
        for (RDFNode mode : reloaded.getAccessModes()) { assertTrue(ACCESS_MODES.contains(mode)); }
        for (RDFNode mode : reloaded.getCreatorAccessModes()) { assertTrue(CREATOR_ACCESS_MODES.contains(mode)); }
        assertEquals(SCOPE_ALL, reloaded.getScopeOfAuthorization());
        assertEquals(PROJECTRON_PROJECT_NEED, reloaded.getAccessNeed());
    }

    @Test
    @DisplayName("Fail to get data authorization - missing required fields")
    void failToReadDataRegistration() {
        URL url = toUrl(server, "/missing-fields/authorization/all-1-project");
        assertThrows(SaiException.class, () -> DataAuthorization.get(url, saiSession));
    }

}

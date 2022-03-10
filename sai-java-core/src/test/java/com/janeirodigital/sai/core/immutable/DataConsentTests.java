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

class DataConsentTests {

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
    private static List<URL> ALL_DATA_CONSENT_URLS;
    private static List<RDFNode> ACCESS_MODES, CREATOR_ACCESS_MODES, READ_MODES, WRITE_MODES;

    @BeforeAll
    static void beforeAll() throws SaiException, SaiNotFoundException {
        // Initialize the Data Factory
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));
        // Initialize request fixtures for the MockWebServer
        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();
        // GET agent registry in Turtle
        mockOnGet(dispatcher, "/access/all-1-project", "access/all/all-1-project-ttl");
        mockOnPut(dispatcher, "/access/all-1-project", "http/201");
        mockOnGet(dispatcher, "/access/registry-1-project", "access/all-from-registry/registry-1-project-ttl");
        mockOnPut(dispatcher, "/access/registry-1-project", "http/201");
        mockOnGet(dispatcher, "/access/registry-1-milestone", "access/all-from-registry/registry-1-milestone-ttl");
        mockOnPut(dispatcher, "/access/registry-1-milestone", "http/201");
        mockOnGet(dispatcher, "/access/agent-1-project", "access/all-from-agent/agent-1-project-ttl");
        mockOnPut(dispatcher, "/access/agent-1-project", "http/201");
        mockOnGet(dispatcher, "/access/selected-1-project", "access/selected-from-registry/selected-1-project-ttl");
        mockOnPut(dispatcher, "/access/selected-1-project", "http/201");

        // GET data consent in Turtle with missing fields
        mockOnGet(dispatcher, "/missing-fields/access/all-1-project", "access/all/all-1-project-missing-fields-ttl");
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
        ALL_DATA_CONSENT_URLS = Arrays.asList(toUrl(server, "/access/all-1-project"), toUrl(server, "/access/all-1-milestone"),
                                              toUrl(server, "/access/all-1-issue"), toUrl(server, "/access/all-1-task"));
        PROJECT_TREE = stringToUrl("http://data.example/shapetrees/pm#ProjectTree");
        MILESTONE_TREE = stringToUrl("http://data.example/shapetrees/pm#MilestoneTree");
        ISSUE_TREE = stringToUrl("http://data.example/shapetrees/pm#IssueTree");
        TASK_TREE = stringToUrl("http://data.example/shapetrees/pm#TaskTree");
        ACCESS_MODES = Arrays.asList(ACL_READ, ACL_CREATE);
        READ_MODES = Arrays.asList(ACL_READ);
        WRITE_MODES = Arrays.asList(ACL_WRITE);
        CREATOR_ACCESS_MODES = Arrays.asList(ACL_UPDATE, ACL_DELETE);
         
    }

    @Test
    @DisplayName("Create new data consent - scope: all")
    void createAll() throws SaiException {
        URL projectUrl = toUrl(server, "/access/all-1-project");
        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setGrantedBy(ALICE_ID)
                                                   .setGrantee(PROJECTRON_ID)
                                                   .setRegisteredShapeTree(PROJECT_TREE)
                                                   .setAccessModes(ACCESS_MODES)
                                                   .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                                                   .setScopeOfConsent(SCOPE_ALL)
                                                   .setAccessNeed(PROJECTRON_PROJECT_NEED)
                                                   .build();
        assertDoesNotThrow(() -> projectConsent.create());
        assertTrue(projectConsent.canCreate());
    }

    @Test
    @DisplayName("Create new data consent - scope: all from registry")
    void createAllFromRegistry() throws SaiException {
        URL projectUrl = toUrl(server, "/access/registry-1-project");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setDataOwner(ALICE_ID)
                                                   .setGrantedBy(ALICE_ID)
                                                   .setGrantee(PROJECTRON_ID)
                                                   .setRegisteredShapeTree(PROJECT_TREE)
                                                   .setDataRegistration(drUrl)
                                                   .setAccessModes(WRITE_MODES)
                                                   .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                                                   .setScopeOfConsent(SCOPE_ALL_FROM_REGISTRY)
                                                   .setAccessNeed(PROJECTRON_PROJECT_NEED)
                                                   .build();
        assertDoesNotThrow(() -> projectConsent.create());
        assertTrue(projectConsent.canCreate());
    }

    @Test
    @DisplayName("Create new data consent - scope: selected from registry")
    void createSelectedFromRegistry() throws SaiException {
        URL projectUrl = toUrl(server, "/access/selected-1-project");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        List<URL> dataInstances = Arrays.asList(toUrl(server, "/personal/data/projects/project-1"),
                                                toUrl(server, "/personal/data/projects/project-2"),
                                                toUrl(server, "/personal/data/projects/project-3"));
        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setDataOwner(ALICE_ID)
                                                    .setGrantedBy(ALICE_ID)
                                                    .setGrantee(PROJECTRON_ID)
                                                    .setRegisteredShapeTree(PROJECT_TREE)
                                                    .setDataRegistration(drUrl)
                                                    .setAccessModes(READ_MODES)
                                                    .setScopeOfConsent(SCOPE_SELECTED_FROM_REGISTRY)
                                                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                                                    .setDataInstances(dataInstances)
                                                    .build();
        assertDoesNotThrow(() -> projectConsent.create());
        assertFalse(projectConsent.canCreate());
    }

    @Test
    @DisplayName("Create new data consent - scope: all from agent")
    void createAllFromAgent() throws SaiException {
        URL projectUrl = toUrl(server, "/access/agent-1-project");
        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setDataOwner(BOB_ID)
                                                   .setGrantedBy(ALICE_ID)
                                                   .setGrantee(PROJECTRON_ID)
                                                   .setRegisteredShapeTree(PROJECT_TREE)
                                                   .setAccessModes(ACCESS_MODES)
                                                   .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                                                   .setScopeOfConsent(SCOPE_ALL_FROM_AGENT)
                                                   .setAccessNeed(PROJECTRON_PROJECT_NEED)
                                                   .build();
        assertDoesNotThrow(() -> projectConsent.create());
    }

    @Test
    @DisplayName("Create new data consent - scope: inherited")
    void createInherited() throws SaiException {
        URL projectUrl = toUrl(server, "/access/project-1-milestone");
        URL milestoneUrl = toUrl(server, "/access/registry-1-milestone");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataConsent.Builder milestoneBuilder = new DataConsent.Builder(milestoneUrl, saiSession);
        DataConsent milestoneConsent = milestoneBuilder.setDataOwner(ALICE_ID)
                .setGrantedBy(ALICE_ID)
                .setGrantee(PROJECTRON_ID)
                .setRegisteredShapeTree(MILESTONE_TREE)
                .setDataRegistration(drUrl)
                .setAccessModes(ACCESS_MODES)
                .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfConsent(SCOPE_INHERITED)
                .setAccessNeed(PROJECTRON_MILESTONE_NEED)
                .setInheritsFrom(projectUrl)
                .build();
        assertDoesNotThrow(() -> milestoneConsent.create());
    }

    @Test
    @DisplayName("Create new data consent - scope: no access")
    void createNoAccess() throws SaiException {
        URL projectUrl = toUrl(server, "/access/selected-1-project");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectUrl, saiSession);
        DataConsent projectConsent = projectBuilder.setDataOwner(ALICE_ID)
                .setGrantedBy(ALICE_ID)
                .setGrantee(PROJECTRON_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(drUrl)
                .setAccessModes(ACCESS_MODES)
                .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfConsent(SCOPE_NO_ACCESS)
                .setAccessNeed(PROJECTRON_PROJECT_NEED)
                .build();
        assertDoesNotThrow(() -> projectConsent.create());
    }

    // Generate data grants

    @Test
    @DisplayName("Fail to create new data consent - scope: invalid")
    void failToCreateInvalidScope() throws SaiException {
        URL projectUrl = toUrl(server, "/access/selected-1-project");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
        DataConsent projectConsent = projectBuilder.setDataOwner(ALICE_ID)
                .setGrantedBy(ALICE_ID)
                .setGrantee(PROJECTRON_ID)
                .setRegisteredShapeTree(PROJECT_TREE)
                .setDataRegistration(drUrl)
                .setAccessModes(ACCESS_MODES)
                .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfConsent(ACCESS_GRANT)  // INVALID
                .setAccessNeed(PROJECTRON_PROJECT_NEED)
                .build();
        });
    }

    @Test
    @DisplayName("Fail to create new data consent - create privileges and no creator modes")
    void failToCreateInvalidCreator() throws SaiException {
        URL projectUrl = toUrl(server, "/access/selected-1-project");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
            DataConsent projectConsent = projectBuilder.setDataOwner(ALICE_ID)
                    .setGrantedBy(ALICE_ID)
                    .setGrantee(PROJECTRON_ID)
                    .setRegisteredShapeTree(PROJECT_TREE)
                    .setDataRegistration(drUrl)
                    .setAccessModes(ACCESS_MODES)  // can create but no creator modes are set
                    .setScopeOfConsent(SCOPE_ALL_FROM_REGISTRY)
                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                    .build();
        });
    }

    @Test
    @DisplayName("Fail to create new data consent - not inherited and inherits from data consent")
    void failToCreateInvalidInheritsFrom() {
        URL projectUrl = toUrl(server, "/access/all-1-project");
        URL milestoneUrl = toUrl(server, "/access/all-1-milestone");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
            DataConsent projectConsent = projectBuilder.setDataOwner(ALICE_ID)
                    .setGrantedBy(ALICE_ID)
                    .setGrantee(PROJECTRON_ID)
                    .setRegisteredShapeTree(PROJECT_TREE)
                    .setDataRegistration(drUrl)
                    .setAccessModes(ACCESS_MODES)
                    .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                    .setScopeOfConsent(SCOPE_ALL_FROM_REGISTRY)
                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                    .setInheritsFrom(milestoneUrl)
                    .build();
        });
    }

    @Test
    @DisplayName("Fail to create new data consent - selected with no instances")
    void failToCreateSelectedNoInstances() throws SaiException {
        URL projectUrl = toUrl(server, "/access/selected-1-project");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
            DataConsent projectConsent = projectBuilder.setDataOwner(ALICE_ID)
                    .setGrantedBy(ALICE_ID)
                    .setGrantee(PROJECTRON_ID)
                    .setRegisteredShapeTree(PROJECT_TREE)
                    .setDataRegistration(drUrl)
                    .setAccessModes(READ_MODES)
                    .setScopeOfConsent(SCOPE_SELECTED_FROM_REGISTRY)
                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                    .build();
            assertDoesNotThrow(() -> projectConsent.create());
            assertFalse(projectConsent.canCreate());
        });
    }

    @Test
    @DisplayName("Fail to create new data consent - selected with no instances")
    void failToCreateSelectedNoRegistration() {
        URL projectUrl = toUrl(server, "/access/selected-1-project");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        List<URL> dataInstances = Arrays.asList(toUrl(server, "/personal/data/projects/project-1"),
                toUrl(server, "/personal/data/projects/project-2"),
                toUrl(server, "/personal/data/projects/project-3"));
        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
            DataConsent projectConsent = projectBuilder.setDataOwner(ALICE_ID)
                    .setGrantedBy(ALICE_ID)
                    .setGrantee(PROJECTRON_ID)
                    .setRegisteredShapeTree(PROJECT_TREE)
                    .setAccessModes(READ_MODES)
                    .setScopeOfConsent(SCOPE_SELECTED_FROM_REGISTRY)
                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                    .setDataInstances(dataInstances)
                    .build();
            assertDoesNotThrow(() -> projectConsent.create());
            assertFalse(projectConsent.canCreate());
        });
    }

    @Test
    @DisplayName("Fail to create new data consent - has instances and no selected scope")
    void failToCreateInstancesNoSelected() {
        URL projectUrl = toUrl(server, "/access/selected-1-project");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        List<URL> dataInstances = Arrays.asList(toUrl(server, "/personal/data/projects/project-1"),
                toUrl(server, "/personal/data/projects/project-2"),
                toUrl(server, "/personal/data/projects/project-3"));
        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
            DataConsent projectConsent = projectBuilder.setDataOwner(ALICE_ID)
                    .setGrantedBy(ALICE_ID)
                    .setGrantee(PROJECTRON_ID)
                    .setRegisteredShapeTree(PROJECT_TREE)
                    .setDataRegistration(drUrl)
                    .setAccessModes(READ_MODES)
                    .setScopeOfConsent(SCOPE_ALL_FROM_REGISTRY)
                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                    .setDataInstances(dataInstances)
                    .build();
            assertDoesNotThrow(() -> projectConsent.create());
            assertFalse(projectConsent.canCreate());
        });
    }

    @Test
    @DisplayName("Fail to create new data consent - scope of all but data registration provided")
    void failToCreateAllAndDataReg() {
        URL projectUrl = toUrl(server, "/access/all-1-project");
        URL milestoneUrl = toUrl(server, "/access/all-1-milestone");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
            DataConsent projectConsent = projectBuilder.setGrantedBy(ALICE_ID)
                    .setGrantee(PROJECTRON_ID)
                    .setRegisteredShapeTree(PROJECT_TREE)
                    .setDataRegistration(drUrl)
                    .setAccessModes(ACCESS_MODES)
                    .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                    .setScopeOfConsent(SCOPE_ALL)
                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                    .build();
        });
    }

    @Test
    @DisplayName("Fail to create new data consent - scope of all but data owner is set")
    void failToCreateAllAndDataOwner() {
        URL projectUrl = toUrl(server, "/access/all-1-project");
        URL milestoneUrl = toUrl(server, "/access/all-1-milestone");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
            DataConsent projectConsent = projectBuilder.setDataOwner(ALICE_ID)
                    .setGrantedBy(ALICE_ID)
                    .setGrantee(PROJECTRON_ID)
                    .setRegisteredShapeTree(PROJECT_TREE)
                    .setAccessModes(ACCESS_MODES)
                    .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                    .setScopeOfConsent(SCOPE_ALL)
                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                    .build();
        });
    }

    @Test
    @DisplayName("Fail to create new data consent - all from registry scope but no data registration")
    void failToCreateAllFromRegNoDataReg() {
        URL projectUrl = toUrl(server, "/access/all-1-project");
        URL milestoneUrl = toUrl(server, "/access/all-1-milestone");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
            DataConsent projectConsent = projectBuilder.setDataOwner(ALICE_ID)
                    .setGrantedBy(ALICE_ID)
                    .setGrantee(PROJECTRON_ID)
                    .setRegisteredShapeTree(PROJECT_TREE)
                    .setAccessModes(ACCESS_MODES)
                    .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                    .setScopeOfConsent(SCOPE_ALL_FROM_REGISTRY)
                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                    .build();
        });
    }

    @Test
    @DisplayName("Fail to create new data consent - inherited but no inherits from")
    void failToCreateInheritedNoInheritsFrom() {
        URL projectUrl = toUrl(server, "/access/all-1-project");
        URL milestoneUrl = toUrl(server, "/access/all-1-milestone");
        URL drUrl = toUrl(server, "/personal/data/projects/");
        DataConsent.Builder projectBuilder = new DataConsent.Builder(projectUrl, saiSession);
        assertThrows(SaiException.class, () -> {
            DataConsent projectConsent = projectBuilder.setDataOwner(BOB_ID)
                    .setGrantedBy(ALICE_ID)
                    .setGrantee(PROJECTRON_ID)
                    .setRegisteredShapeTree(PROJECT_TREE)
                    .setDataRegistration(drUrl)
                    .setAccessModes(ACCESS_MODES)
                    .setCreatorAccessModes(CREATOR_ACCESS_MODES)
                    .setScopeOfConsent(SCOPE_INHERITED)
                    .setAccessNeed(PROJECTRON_PROJECT_NEED)
                    .build();
        });
    }

    @Test
    @DisplayName("Get data consent - scope: all")
    void getDataConsent() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/access/all-1-project");
        DataConsent dataConsent = DataConsent.get(url, saiSession);
        assertEquals(ALICE_ID, dataConsent.getGrantedBy());
        assertEquals(PROJECTRON_ID, dataConsent.getGrantee());
        assertEquals(PROJECT_TREE, dataConsent.getRegisteredShapeTree());
        for (RDFNode mode : dataConsent.getAccessModes()) { assertTrue(ACCESS_MODES.contains(mode)); }
        for (RDFNode mode : dataConsent.getCreatorAccessModes()) { assertTrue(CREATOR_ACCESS_MODES.contains(mode)); }
        assertEquals(SCOPE_ALL, dataConsent.getScopeOfConsent());
        assertEquals(PROJECTRON_PROJECT_NEED, dataConsent.getAccessNeed());
    }

    @Test
    @DisplayName("Reload data consent - scope: all")
    void reloadDataConsent() throws SaiException, SaiNotFoundException {
        URL url = toUrl(server, "/access/all-1-project");
        DataConsent dataConsent = DataConsent.get(url, saiSession);
        DataConsent reloaded = dataConsent.reload();
        assertEquals(ALICE_ID, reloaded.getGrantedBy());
        assertEquals(PROJECTRON_ID, reloaded.getGrantee());
        assertEquals(PROJECT_TREE, reloaded.getRegisteredShapeTree());
        for (RDFNode mode : reloaded.getAccessModes()) { assertTrue(ACCESS_MODES.contains(mode)); }
        for (RDFNode mode : reloaded.getCreatorAccessModes()) { assertTrue(CREATOR_ACCESS_MODES.contains(mode)); }
        assertEquals(SCOPE_ALL, reloaded.getScopeOfConsent());
        assertEquals(PROJECTRON_PROJECT_NEED, reloaded.getAccessNeed());
    }

    @Test
    @DisplayName("Fail to get data consent - missing required fields")
    void failToReadDataRegistration() {
        URL url = toUrl(server, "/missing-fields/access/all-1-project");
        assertThrows(SaiException.class, () -> DataConsent.get(url, saiSession));
    }

}

package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.immutable.DataGrant;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.shapetrees.core.contentloaders.DocumentLoaderManager;
import com.janeirodigital.shapetrees.core.contentloaders.HttpExternalDocumentLoader;
import com.janeirodigital.shapetrees.core.exceptions.ShapeTreeException;
import com.janeirodigital.shapetrees.core.validation.ShapeTree;
import com.janeirodigital.shapetrees.core.validation.ShapeTreeReference;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.RDFNode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.*;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static com.janeirodigital.sai.core.helpers.HttpHelper.stringToUrl;
import static com.janeirodigital.sai.core.vocabularies.AclVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.SCOPE_ALL_FROM_REGISTRY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataInstanceTests {

    private static SaiSession saiSession;
    private static MockWebServer server;

    private static URL ALICE_ID;
    private static URL PROJECTRON_ID;
    private static URL PROJECTS_DATA_REGISTRATION;
    private static URL PROJECT_TREE, MILESTONE_TREE, MISSING_TREE;
    private static URL PROJECTRON_PROJECT_NEED;
    private static List<RDFNode> ACCESS_MODES, CREATOR_ACCESS_MODES;


    @BeforeAll
    static void beforeAll() throws SaiException {
        // Initialize a mock sai session we can use for protected requests
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        saiSession = new SaiSession(mockSession, new HttpClientFactory(false, false, false));
        DocumentLoaderManager.setLoader(new HttpExternalDocumentLoader());

        RequestMatchingFixtureDispatcher dispatcher = new RequestMatchingFixtureDispatcher();

        // Shape trees and shapes
        mockOnGet(dispatcher, "/shapetrees/pm", "schemas/pm-shapetrees-ttl");

        // Access grants
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-personal-project", "agents/alice/projectron-all/all-1-grant-personal-project-ttl");
        mockOnGet(dispatcher, "/all-1-agents/all-1-projectron/all-1-grant-personal-milestone", "agents/alice/projectron-all/all-1-grant-personal-milestone-ttl");

        // Data Registrations and Data Instances
        mockOnGet(dispatcher, "/personal/data/projects/", "data/alice/personal-data-registration-projects-ttl");
        mockOnGet(dispatcher, "/personal/data/projects/p1", "data/alice/personal-data-projects-p1-ttl");
        mockOnPut(dispatcher, "/personal/data/projects/p1", "http/204");
        mockOnGet(dispatcher, "/personal/data/projects/p2", "data/alice/personal-data-projects-p2-ttl");
        mockOnPut(dispatcher, "/personal/data/projects/p2", "http/204");
        mockOnGet(dispatcher, "/personal/data/projects/p3", "data/alice/personal-data-projects-p3-ttl");
        mockOnPut(dispatcher, "/personal/data/projects/p3", "http/204");

        mockOnPut(dispatcher, "/personal/data/projects/new-project", "http/201");
        mockOnPut(dispatcher, "/personal/data/projects/new-milestone", "http/201");
        mockOnPut(dispatcher, "/personal/data/projects/new-project", "http/204");
        mockOnPut(dispatcher, "/personal/data/projects/new-milestone", "http/204");
        mockOnDelete(dispatcher, "/personal/data/projects/new-project", "http/204");
        mockOnDelete(dispatcher, "/personal/data/projects/new-milestone", "http/204");

        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        ALICE_ID = stringToUrl("https://alice.example/id");
        PROJECTRON_ID = stringToUrl("https://projectron.example/id");
        PROJECT_TREE = toUrl(server, "/shapetrees/pm#ProjectTree");
        MILESTONE_TREE = toUrl(server, "/shapetrees/pm#MilestoneTree");
        MISSING_TREE = toUrl(server, "/shapetrees/pm#MissingTree");
        PROJECTS_DATA_REGISTRATION = toUrl(server, "/personal/data/projects/");
        ACCESS_MODES = Arrays.asList(ACL_READ, ACL_CREATE);
        CREATOR_ACCESS_MODES = Arrays.asList(ACL_UPDATE, ACL_DELETE);
        PROJECTRON_PROJECT_NEED = stringToUrl("https://projectron.example/#ac54ff1e");

    }

    @Test
    @DisplayName("Create a basic data instance - no dataset (empty)")
    void createBasicDataInstance() throws SaiNotFoundException, SaiException {
        URL url = toUrl(server, "/personal/data/projects/new-project");
        URL grantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(grantUrl, saiSession);
        BasicDataInstance.Builder builder = new BasicDataInstance.Builder(url, saiSession);
        BasicDataInstance dataInstance = builder.setDataGrant(projectGrant).build();
        assertFalse(dataInstance.isExists());
        assertTrue(dataInstance.isDraft());
        assertDoesNotThrow(() -> dataInstance.update());
        assertTrue(dataInstance.isExists());
        assertFalse(dataInstance.isDraft());
    }

    @Test
    @DisplayName("Fail to build a basic data instance - inherited grant with no parent")
    void failToCreateBasicDataInstanceNoParent() throws SaiNotFoundException, SaiException {
        URL url = toUrl(server, "/personal/data/projects/new-project");
        URL grantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-milestone");
        ReadableDataGrant readableDataGrant = ReadableDataGrant.get(grantUrl, saiSession);
        InheritedDataGrant milestoneGrant = (InheritedDataGrant) readableDataGrant;
        BasicDataInstance.Builder builder = new BasicDataInstance.Builder(url, saiSession).setDataGrant(milestoneGrant);
        assertThrows(SaiException.class, () -> builder.build());
    }

    @Test
    @DisplayName("Fail to build a basic data instance - cannot lookup associated shape tree")
    void failToGetBasicDataInstanceShapeTree() throws SaiException {
        URL url = toUrl(server, "/personal/data/projects/new-project");
        URL grantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project-invalid");

        DataGrant.Builder grantBuilder = new DataGrant.Builder(grantUrl, saiSession);
        DataGrant dataGrant = grantBuilder.setDataOwner(ALICE_ID).setGrantee(PROJECTRON_ID).setRegisteredShapeTree(MISSING_TREE)
                .setDataRegistration(PROJECTS_DATA_REGISTRATION).setAccessModes(ACCESS_MODES).setCreatorAccessModes(CREATOR_ACCESS_MODES)
                .setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY).setAccessNeed(PROJECTRON_PROJECT_NEED).build();
        ReadableDataGrant projectGrant = new ReadableDataGrant.Builder(grantUrl, saiSession).setDataset(dataGrant.getDataset()).build();
        BasicDataInstance.Builder builder = new BasicDataInstance.Builder(url, saiSession);
        assertThrows(SaiException.class, () -> builder.setDataGrant(projectGrant));
    }

    @Test
    @DisplayName("Get a basic data instance")
    void getBasicDataInstance() throws SaiNotFoundException, SaiException {
        URL url = toUrl(server, "/personal/data/projects/p1");
        URL grantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(grantUrl, saiSession);
        BasicDataInstance dataInstance = BasicDataInstance.get(url, saiSession, projectGrant, null);
        assertTrue(dataInstance.isExists());
        assertFalse(dataInstance.isDraft());
        assertEquals(projectGrant, dataInstance.getDataGrant());
    }

    @Test
    @DisplayName("Generate data instance URL - UUID")
    void testGenerateInstanceUrlUUID() throws SaiNotFoundException, SaiException {
        URL grantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(grantUrl, saiSession);
        URL instanceUrl = DataInstance.generateUrl(projectGrant);
        String generatedName = FilenameUtils.getName(instanceUrl.getPath());
        assertDoesNotThrow(() -> { UUID.fromString(generatedName); });
    }

    @Test
    @DisplayName("Generate data instance URL - Resource name provided")
    void testGenerateInstanceUrlString() throws SaiNotFoundException, SaiException {
        URL grantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        String RESOURCE_NAME = "generated-resource";
        ReadableDataGrant projectGrant = ReadableDataGrant.get(grantUrl, saiSession);
        URL instanceUrl = DataInstance.generateUrl(projectGrant, RESOURCE_NAME);
        String generatedName = FilenameUtils.getName(instanceUrl.getPath());
        assertEquals(generatedName, RESOURCE_NAME);
    }

    @Test
    @DisplayName("Create, update, and delete child data instance")
    void createChildDataInstance() throws SaiNotFoundException, SaiException {
        URL projectUrl = toUrl(server, "/personal/data/projects/new-project");
        URL milestoneUrl = toUrl(server, "/personal/data/projects/new-milestone");
        URL projectGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        URL milestoneGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-milestone");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(projectGrantUrl, saiSession);
        ReadableDataGrant milestoneGrant = ReadableDataGrant.get(milestoneGrantUrl, saiSession);
        BasicDataInstance.Builder parentBuilder = new BasicDataInstance.Builder(projectUrl, saiSession);
        BasicDataInstance parentInstance = parentBuilder.setDataGrant(projectGrant).build();
        BasicDataInstance.Builder childBuilder = new BasicDataInstance.Builder(milestoneUrl, saiSession);
        BasicDataInstance childInstance = childBuilder.setDataGrant(milestoneGrant).setParent(parentInstance).build();
        assertFalse(parentInstance.isExists() && childInstance.isExists());
        assertTrue(parentInstance.isDraft() && childInstance.isDraft());
        assertDoesNotThrow(() -> childInstance.update());
        assertDoesNotThrow(() -> parentInstance.update());
        // resources are created - so they should exist, not be drafts, and there should be a single child reference for the child
        assertTrue(parentInstance.isExists() && childInstance.isExists());
        assertFalse(parentInstance.isDraft() && childInstance.isDraft());
        assertEquals(childInstance.getUrl(), parentInstance.getChildReferences(MILESTONE_TREE).get(0));
        // child resource is updated again - should not create another child reference
        assertDoesNotThrow(() -> childInstance.update());
        assertEquals(childInstance.getUrl(), parentInstance.getChildReferences(MILESTONE_TREE).get(0));
        assertEquals(1, parentInstance.getChildReferences(MILESTONE_TREE).size());
        assertDoesNotThrow(() -> childInstance.delete());
        assertTrue(parentInstance.getChildReferences(MILESTONE_TREE).isEmpty());
        assertFalse(childInstance.isExists());
        assertDoesNotThrow(() -> parentInstance.delete());
        assertFalse(parentInstance.isExists());
    }

    @Test
    @DisplayName("Fail to add child reference to parent instance - no child reference in shape tree")
    void failToAddChildReferenceNull() throws SaiNotFoundException, SaiException {
        URL projectUrl = toUrl(server, "/personal/data/projects/new-project");
        URL projectGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        URL milestoneGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-milestone");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(projectGrantUrl, saiSession);
        ReadableDataGrant milestoneGrant = ReadableDataGrant.get(milestoneGrantUrl, saiSession);
        BasicDataInstance.Builder parentBuilder = new BasicDataInstance.Builder(projectUrl, saiSession);
        BasicDataInstance parentInstance = parentBuilder.setDataGrant(projectGrant).build();

        ShapeTree mockShapeTree = mock(ShapeTree.class);
        when(mockShapeTree.getId()).thenReturn(MISSING_TREE);
        BasicDataInstance mockChildInstance = mock(BasicDataInstance.class);
        when(mockChildInstance.getShapeTree()).thenReturn(mockShapeTree);

        assertThrows(SaiException.class, () -> parentInstance.addChildReference(mockChildInstance));
    }

    @Test
    @DisplayName("Fail to add child reference to parent instance - failure in shape tree reference lookup")
    void failToAddChildReferenceLookup() throws SaiNotFoundException, SaiException {
        URL projectUrl = toUrl(server, "/personal/data/projects/new-project");
        URL projectGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        URL milestoneGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-milestone");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(projectGrantUrl, saiSession);
        ReadableDataGrant milestoneGrant = ReadableDataGrant.get(milestoneGrantUrl, saiSession);
        BasicDataInstance.Builder parentBuilder = new BasicDataInstance.Builder(projectUrl, saiSession);
        BasicDataInstance parentInstance = parentBuilder.setDataGrant(projectGrant).build();

        ShapeTree mockShapeTree = mock(ShapeTree.class);
        when(mockShapeTree.getId()).thenReturn(MISSING_TREE);
        BasicDataInstance mockChildInstance = mock(BasicDataInstance.class);
        when(mockChildInstance.getShapeTree()).thenReturn(mockShapeTree);

        try (MockedStatic<ShapeTreeReference> mockStaticReference = Mockito.mockStatic(ShapeTreeReference.class)) {
            mockStaticReference.when(() -> ShapeTreeReference.findChildReference(any(ShapeTree.class), any(URL.class))).thenThrow(ShapeTreeException.class);
            assertThrows(SaiException.class, () -> parentInstance.addChildReference(mockChildInstance));
        }
    }

    @Test
    @DisplayName("Fail to remove child reference from parent instance - no child reference in shape tree")
    void failToRemoveChildReferenceNull() throws SaiNotFoundException, SaiException {
        URL projectUrl = toUrl(server, "/personal/data/projects/new-project");
        URL projectGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        URL milestoneGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-milestone");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(projectGrantUrl, saiSession);
        ReadableDataGrant milestoneGrant = ReadableDataGrant.get(milestoneGrantUrl, saiSession);
        BasicDataInstance.Builder parentBuilder = new BasicDataInstance.Builder(projectUrl, saiSession);
        BasicDataInstance parentInstance = parentBuilder.setDataGrant(projectGrant).build();

        ShapeTree mockShapeTree = mock(ShapeTree.class);
        when(mockShapeTree.getId()).thenReturn(MISSING_TREE);
        BasicDataInstance mockChildInstance = mock(BasicDataInstance.class);
        when(mockChildInstance.getShapeTree()).thenReturn(mockShapeTree);

        assertThrows(SaiException.class, () -> parentInstance.removeChildReference(mockChildInstance));
    }

    @Test
    @DisplayName("Fail to remove child reference from parent instance - failure in shape tree reference lookup")
    void failToRemoveChildReferenceLookup() throws SaiNotFoundException, SaiException {
        URL projectUrl = toUrl(server, "/personal/data/projects/new-project");
        URL projectGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        URL milestoneGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-milestone");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(projectGrantUrl, saiSession);
        ReadableDataGrant milestoneGrant = ReadableDataGrant.get(milestoneGrantUrl, saiSession);
        BasicDataInstance.Builder parentBuilder = new BasicDataInstance.Builder(projectUrl, saiSession);
        BasicDataInstance parentInstance = parentBuilder.setDataGrant(projectGrant).build();

        ShapeTree mockShapeTree = mock(ShapeTree.class);
        when(mockShapeTree.getId()).thenReturn(MISSING_TREE);
        BasicDataInstance mockChildInstance = mock(BasicDataInstance.class);
        when(mockChildInstance.getShapeTree()).thenReturn(mockShapeTree);

        try (MockedStatic<ShapeTreeReference> mockStaticReference = Mockito.mockStatic(ShapeTreeReference.class)) {
            mockStaticReference.when(() -> ShapeTreeReference.findChildReference(any(ShapeTree.class), any(URL.class))).thenThrow(ShapeTreeException.class);
            assertThrows(SaiException.class, () -> parentInstance.removeChildReference(mockChildInstance));
        }
    }

    @Test
    @DisplayName("Fail to get child references from parent instance - failure in shape tree reference lookup")
    void failToGetChildReferencesLookup() throws SaiNotFoundException, SaiException {
        URL projectUrl = toUrl(server, "/personal/data/projects/new-project");
        URL projectGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(projectGrantUrl, saiSession);
        BasicDataInstance.Builder parentBuilder = new BasicDataInstance.Builder(projectUrl, saiSession);
        BasicDataInstance parentInstance = parentBuilder.setDataGrant(projectGrant).build();

        try (MockedStatic<ShapeTreeReference> mockStaticReference = Mockito.mockStatic(ShapeTreeReference.class)) {
            mockStaticReference.when(() -> ShapeTreeReference.findChildReference(any(ShapeTree.class), any(URL.class))).thenThrow(ShapeTreeException.class);
            assertThrows(SaiException.class, () -> parentInstance.getChildReferences(MILESTONE_TREE));
        }
    }

    @Test
    @DisplayName("Fail to get child instances from parent instance - failure in shape tree reference lookup")
    void failToGetChildInstancesLookup() throws SaiNotFoundException, SaiException {
        URL projectUrl = toUrl(server, "/personal/data/projects/new-project");
        URL projectGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(projectGrantUrl, saiSession);
        BasicDataInstance.Builder parentBuilder = new BasicDataInstance.Builder(projectUrl, saiSession);
        BasicDataInstance parentInstance = parentBuilder.setDataGrant(projectGrant).build();

        try (MockedStatic<ShapeTreeReference> mockStaticReference = Mockito.mockStatic(ShapeTreeReference.class)) {
            ShapeTreeReference mockReference = mock(ShapeTreeReference.class);
            mockStaticReference.when(() -> ShapeTreeReference.getPropertyFromReference(any(ShapeTreeReference.class))).thenThrow(ShapeTreeException.class);
            assertThrows(SaiException.class, () -> parentInstance.findChildInstances(mockReference));
        }
    }

}

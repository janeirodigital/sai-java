package com.janeirodigital.sai.core.readable;

import com.janeirodigital.mockwebserver.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiRuntimeException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.immutable.DataGrant;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpException;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.RdfUtils;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.shapetrees.core.contentloaders.DocumentLoaderManager;
import com.janeirodigital.shapetrees.core.contentloaders.HttpExternalDocumentLoader;
import com.janeirodigital.shapetrees.core.exceptions.ShapeTreeException;
import com.janeirodigital.shapetrees.core.validation.ShapeTree;
import com.janeirodigital.shapetrees.core.validation.ShapeTreeReference;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.URL;
import java.util.*;

import static com.janeirodigital.mockwebserver.DispatcherHelper.*;
import static com.janeirodigital.mockwebserver.MockWebServerHelper.toUrl;
import static com.janeirodigital.sai.core.vocabularies.AclVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.SCOPE_ALL_FROM_REGISTRY;
import static com.janeirodigital.sai.httputils.HttpUtils.stringToUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataInstanceTests {

    private static SaiSession saiSession;
    private static MockWebServer server;

    private static URL ALICE_ID;
    private static URL PROJECTRON_ID;
    private static URL PROJECTS_DATA_REGISTRATION;
    private static URL MILESTONE_TREE, MISSING_TREE;
    private static URL PROJECTRON_PROJECT_NEED;
    private static List<URL> PROJECT_DATA_INSTANCES;
    private static List<RDFNode> ACCESS_MODES, CREATOR_ACCESS_MODES;


    @BeforeAll
    static void beforeAll() throws SaiException, SaiHttpException {
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
        MILESTONE_TREE = toUrl(server, "/shapetrees/pm#MilestoneTree");
        MISSING_TREE = toUrl(server, "/shapetrees/pm#MissingTree");
        PROJECTS_DATA_REGISTRATION = toUrl(server, "/personal/data/projects/");
        ACCESS_MODES = Arrays.asList(ACL_READ, ACL_CREATE);
        CREATOR_ACCESS_MODES = Arrays.asList(ACL_UPDATE, ACL_DELETE);
        PROJECTRON_PROJECT_NEED = stringToUrl("https://projectron.example/#ac54ff1e");
        PROJECT_DATA_INSTANCES = Arrays.asList(toUrl(server, "/personal/data/projects/p1"),
                                               toUrl(server, "/personal/data/projects/p2"),
                                               toUrl(server, "/personal/data/projects/p3"));
    }

    @Test
    @DisplayName("Create a basic data instance - no dataset (empty)")
    void createBasicDataInstance() throws SaiHttpNotFoundException, SaiException {
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
    void failToCreateBasicDataInstanceNoParent() throws SaiHttpNotFoundException, SaiException {
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
    void getBasicDataInstance() throws SaiHttpNotFoundException, SaiException {
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
    void testGenerateInstanceUrlUUID() throws SaiHttpNotFoundException, SaiException {
        URL grantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(grantUrl, saiSession);
        URL instanceUrl = DataInstance.generateUrl(projectGrant);
        String generatedName = FilenameUtils.getName(instanceUrl.getPath());
        assertDoesNotThrow(() -> { UUID.fromString(generatedName); });
    }

    @Test
    @DisplayName("Generate data instance URL - Resource name provided")
    void testGenerateInstanceUrlString() throws SaiHttpNotFoundException, SaiException {
        URL grantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        String RESOURCE_NAME = "generated-resource";
        ReadableDataGrant projectGrant = ReadableDataGrant.get(grantUrl, saiSession);
        URL instanceUrl = DataInstance.generateUrl(projectGrant, RESOURCE_NAME);
        String generatedName = FilenameUtils.getName(instanceUrl.getPath());
        assertEquals(generatedName, RESOURCE_NAME);
    }

    @Test
    @DisplayName("Fail to generate data instance URL - Invalid path")
    void failToGenerateInstanceUrlInvalidPath() throws SaiHttpNotFoundException, SaiException {
        URL grantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        String RESOURCE_NAME = "somescheme://what/"; // INVALID
        ReadableDataGrant projectGrant = ReadableDataGrant.get(grantUrl, saiSession);
        assertThrows(SaiException.class, () -> DataInstance.generateUrl(projectGrant, RESOURCE_NAME));
    }

    @Test
    @DisplayName("Create, update, and delete child data instance")
    void createChildDataInstance() throws SaiHttpNotFoundException, SaiException {
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
    void failToAddChildReferenceNull() throws SaiHttpNotFoundException, SaiException {
        URL projectUrl = toUrl(server, "/personal/data/projects/new-project");
        URL projectGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(projectGrantUrl, saiSession);
        BasicDataInstance.Builder parentBuilder = new BasicDataInstance.Builder(projectUrl, saiSession);
        BasicDataInstance parentInstance = parentBuilder.setDataGrant(projectGrant).build();

        ShapeTree mockShapeTree = mock(ShapeTree.class);
        when(mockShapeTree.getId()).thenReturn(MISSING_TREE);
        BasicDataInstance mockChildInstance = mock(BasicDataInstance.class);
        when(mockChildInstance.getShapeTree()).thenReturn(mockShapeTree);

        assertThrows(SaiException.class, () -> parentInstance.addChildInstance(mockChildInstance));
    }

    @Test
    @DisplayName("Fail to add child reference to parent instance - failure in shape tree reference lookup")
    void failToAddChildReferenceLookup() throws SaiHttpNotFoundException, SaiException {
        URL projectUrl = toUrl(server, "/personal/data/projects/new-project");
        URL projectGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(projectGrantUrl, saiSession);
        BasicDataInstance.Builder parentBuilder = new BasicDataInstance.Builder(projectUrl, saiSession);
        BasicDataInstance parentInstance = parentBuilder.setDataGrant(projectGrant).build();

        ShapeTree mockShapeTree = mock(ShapeTree.class);
        when(mockShapeTree.getId()).thenReturn(MISSING_TREE);
        BasicDataInstance mockChildInstance = mock(BasicDataInstance.class);
        when(mockChildInstance.getShapeTree()).thenReturn(mockShapeTree);

        try (MockedStatic<ShapeTreeReference> mockStaticReference = Mockito.mockStatic(ShapeTreeReference.class)) {
            mockStaticReference.when(() -> ShapeTreeReference.findChildReference(any(ShapeTree.class), any(URL.class))).thenThrow(ShapeTreeException.class);
            assertThrows(SaiException.class, () -> parentInstance.addChildInstance(mockChildInstance));
        }
    }

    @Test
    @DisplayName("Fail to remove child reference from parent instance - no child reference in shape tree")
    void failToRemoveChildReferenceNull() throws SaiHttpNotFoundException, SaiException {
        URL projectUrl = toUrl(server, "/personal/data/projects/new-project");
        URL projectGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(projectGrantUrl, saiSession);
        BasicDataInstance.Builder parentBuilder = new BasicDataInstance.Builder(projectUrl, saiSession);
        BasicDataInstance parentInstance = parentBuilder.setDataGrant(projectGrant).build();

        ShapeTree mockShapeTree = mock(ShapeTree.class);
        when(mockShapeTree.getId()).thenReturn(MISSING_TREE);
        BasicDataInstance mockChildInstance = mock(BasicDataInstance.class);
        when(mockChildInstance.getShapeTree()).thenReturn(mockShapeTree);

        assertThrows(SaiException.class, () -> parentInstance.removeChildInstance(mockChildInstance));
    }

    @Test
    @DisplayName("Fail to remove child reference from parent instance - invalid rdf graph")
    void failToRemoveChildReferenceInvalidGraph() throws SaiHttpNotFoundException, SaiException, SaiRdfException {
        URL projectUrl = toUrl(server, "/personal/data/projects/new-project");
        URL projectGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(projectGrantUrl, saiSession);
        BasicDataInstance.Builder parentBuilder = new BasicDataInstance.Builder(projectUrl, saiSession);
        BasicDataInstance parentInstance = parentBuilder.setDataGrant(projectGrant).build();

        ShapeTree mockShapeTree = mock(ShapeTree.class);
        when(mockShapeTree.getId()).thenReturn(MILESTONE_TREE);
        BasicDataInstance mockChildInstance = mock(BasicDataInstance.class);
        when(mockChildInstance.getShapeTree()).thenReturn(mockShapeTree);

        try (MockedStatic<RdfUtils> mockRdfUtils = Mockito.mockStatic(RdfUtils.class)) {
            mockRdfUtils.when(() -> RdfUtils.getUrlObjects(any(Resource.class), any(Property.class))).thenThrow(SaiRdfException.class);
            assertThrows(SaiException.class, () -> parentInstance.removeChildInstance(mockChildInstance));
        }
    }

    @Test
    @DisplayName("Fail to get child instances from parent instance - failure in shape tree reference lookup")
    void failToGetChildInstancesLookup() throws SaiHttpNotFoundException, SaiException, ShapeTreeException {
        URL projectUrl = toUrl(server, "/personal/data/projects/new-project");
        URL projectGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(projectGrantUrl, saiSession);
        BasicDataInstance.Builder parentBuilder = new BasicDataInstance.Builder(projectUrl, saiSession);
        BasicDataInstance parentInstance = parentBuilder.setDataGrant(projectGrant).build();

        ShapeTreeReference reference = new ShapeTreeReference(toUrl(server, "/references/test-reference"), null, MILESTONE_TREE);

        try (MockedStatic<RdfUtils> mockRdfUtils = Mockito.mockStatic(RdfUtils.class)) {
            mockRdfUtils.when(() -> RdfUtils.getUrlObjects(any(Resource.class), any(Property.class))).thenThrow(SaiRdfException.class);
            assertThrows(SaiException.class, () -> parentInstance.findChildReferences(reference));
        }

    }

    @Test
    @DisplayName("Fail to get child instances from parent instance - invalid rdf graph")
    void failToGetChildInstancesLookupRdfInvalid() throws SaiHttpNotFoundException, SaiException {
        URL projectUrl = toUrl(server, "/personal/data/projects/new-project");
        URL projectGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(projectGrantUrl, saiSession);
        BasicDataInstance.Builder parentBuilder = new BasicDataInstance.Builder(projectUrl, saiSession);
        BasicDataInstance parentInstance = parentBuilder.setDataGrant(projectGrant).build();

        try (MockedStatic<ShapeTreeReference> mockStaticReference = Mockito.mockStatic(ShapeTreeReference.class)) {
            ShapeTreeReference mockReference = mock(ShapeTreeReference.class);
            mockStaticReference.when(() -> ShapeTreeReference.getPropertyFromReference(any(ShapeTreeReference.class))).thenThrow(ShapeTreeException.class);
            assertThrows(SaiException.class, () -> parentInstance.findChildReferences(mockReference));
        }
    }

    @Test
    @DisplayName("Get a list of basic data instances from a list of instance URLs")
    void testGetDataInstanceList() throws SaiHttpNotFoundException, SaiException {
        URL projectGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(projectGrantUrl, saiSession);
        Map<URL, DataInstance> instanceMap = new HashMap<>();
        for (URL instanceUrl : PROJECT_DATA_INSTANCES) { instanceMap.put(instanceUrl, null); }
        DataInstanceList list = new DataInstanceList(saiSession, projectGrant, instanceMap);
        assertFalse(list.isEmpty());
        assertEquals(3, list.size());
        for (DataInstance instance : list) { PROJECT_DATA_INSTANCES.contains(instance.getUrl()); }
    }

    @Test
    @DisplayName("Fail to get a list of basic data instances - instance not found")
    void failToGetDataInstanceListMissing() throws SaiHttpNotFoundException, SaiException {
        URL projectGrantUrl = toUrl(server, "/all-1-agents/all-1-projectron/all-1-grant-personal-project");
        URL MISSING_PROJECT_INSTANCE = toUrl(server, "/personal/data/projects/missing-project");
        ReadableDataGrant projectGrant = ReadableDataGrant.get(projectGrantUrl, saiSession);
        Map<URL, DataInstance> instanceMap = new HashMap<>();
        instanceMap.put(MISSING_PROJECT_INSTANCE, null);  // This instance doesn't exist, so we expect to fail when iterating to get it
        DataInstanceList list = new DataInstanceList(saiSession, projectGrant, instanceMap);
        assertEquals(1, list.size());
        Iterator<DataInstance> iterator = list.iterator();
        assertTrue(iterator.hasNext());
        assertThrows(SaiRuntimeException.class, () -> iterator.next());
    }

}

package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.TestableVocabulary;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import com.janeirodigital.sai.core.helpers.RdfHelper;
import lombok.Getter;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.List;

import static com.janeirodigital.sai.core.helpers.RdfHelper.*;

@Getter
public class TestableCRUDResource extends CRUDResource {

    private final String TESTABLE_RDF_TYPE = "https://graph.example/ns/terms#testable";

    private int id;
    private String name;
    private OffsetDateTime createdAt;
    private URL milestone;
    private boolean active;
    private List<URL> tags;
    private List<String> comments;

    public TestableCRUDResource(URL resourceUrl, DataFactory dataFactory, boolean unprotected) throws SaiException {
        super(resourceUrl, dataFactory, unprotected);
    }

    /**
     * Build a TestableCRUDResource. If a Jena Resource is supplied, it will be override any existing graph that
     * may exist remotely. If it is not, the remote graph will be loaded. If it doesn't exist remotely, it will
     * be initialized to an empty resource of TESTABLE_RDF_TYPE
     */
    public static TestableCRUDResource build(URL url, DataFactory dataFactory, Resource resource, boolean unprotected) throws SaiException {
        TestableCRUDResource testable = new TestableCRUDResource(url, dataFactory, unprotected);
        if (resource != null) {
            testable.resource = resource;
            testable.dataset = resource.getModel();
        }
        testable.bootstrap();
        return testable;
    }

    /**
     * Build a TestableCRUDResource without supplying a Jena resource to populate it with
     */
    public static TestableCRUDResource build(URL url, DataFactory dataFactory, boolean unprotected) throws SaiException {
        return build(url, dataFactory, null, unprotected);
    }

    public void setId(int id) throws SaiException {
        this.id = id;
        RdfHelper.updateObject(this.resource, TestableVocabulary.TESTABLE_ID, id);
    }

    public void setName(String name) throws SaiException {
        this.name = name;
        RdfHelper.updateObject(this.resource, TestableVocabulary.TESTABLE_NAME, name);
    }

    public void setCreatedAt(OffsetDateTime createdAt) throws SaiException {
        this.createdAt = createdAt;
        RdfHelper.updateObject(this.resource, TestableVocabulary.TESTABLE_CREATED_AT, createdAt);
    }

    public void setActive(boolean isActive) throws SaiException {
        this.active = isActive;
        RdfHelper.updateObject(this.resource, TestableVocabulary.TESTABLE_ACTIVE, isActive);
    }

    public void setMilestone(URL milestone) throws SaiException {
        this.milestone = milestone;
        RdfHelper.updateObject(this.resource, TestableVocabulary.TESTABLE_HAS_MILESTONE, milestone);
    }

    public void setTags(List<URL> tags) throws SaiException {
        this.tags = tags;
        updateUrlObjects(this.resource, TestableVocabulary.TESTABLE_HAS_TAG, tags);
    }

    public void setComments(List<String> comments) throws SaiException {
        this.comments = comments;
        updateStringObjects(this.resource, TestableVocabulary.TESTABLE_HAS_COMMENT, comments);
    }

    private void bootstrap() throws SaiException {
        // A Jena resource provided to the factory overrides any remote graph so no need to fetch it
        if (this.resource != null) { populate(); } else {
            try {
                // Fetch the remote resource and populate
                this.fetchData();
                populate();
            } catch (SaiNotFoundException ex) {
                // Remote resource didn't exist, initialize one
                this.resource = getNewResourceForType(this.url, TESTABLE_RDF_TYPE);
                this.dataset = resource.getModel();
            }
        }
    }

    private void populate() throws SaiException {
        try {
            this.id = getRequiredIntegerObject(this.resource, TestableVocabulary.TESTABLE_ID);
            this.name = getRequiredStringObject(this.resource, TestableVocabulary.TESTABLE_NAME);
            this.createdAt = getRequiredDateTimeObject(this.resource, TestableVocabulary.TESTABLE_CREATED_AT);
            this.milestone = getRequiredUrlObject(this.resource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
            this.active = getBooleanObject(this.resource, TestableVocabulary.TESTABLE_ACTIVE);
            this.tags = getUrlObjects(this.resource, TestableVocabulary.TESTABLE_HAS_TAG);
            this.comments = getStringObjects(this.resource, TestableVocabulary.TESTABLE_HAS_COMMENT);
        } catch (SaiNotFoundException ex) {
            throw new SaiException("Unable to bootstrap testable crud resource. Missing required fields: " + ex.getMessage());
        }
    }

}

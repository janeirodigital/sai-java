package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.factories.DataFactory;
import com.janeirodigital.sai.core.TestableVocabulary;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.helpers.RdfHelper;
import lombok.Getter;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.List;

import static com.janeirodigital.sai.core.helpers.RdfHelper.*;

@Getter
public class TestableCRUDResource extends CRUDResource {

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

    public TestableCRUDResource(URL resourceUrl, DataFactory dataFactory, Resource resource, boolean unprotected) throws SaiException {
        super(resourceUrl, dataFactory, resource, unprotected);
    }

    public TestableCRUDResource(URL resourceUrl, DataFactory dataFactory, Resource resource) throws SaiException {
        super(resourceUrl, dataFactory, resource);
    }

    private void bootstrap() throws SaiException, SaiNotFoundException {
        this.fetchData();
        // populate the application profile fields
        this.id = getRequiredIntegerObject(this.resource, TestableVocabulary.TESTABLE_ID);
        this.name = getRequiredStringObject(this.resource, TestableVocabulary.TESTABLE_NAME);
        this.createdAt = getRequiredDateTimeObject(this.resource, TestableVocabulary.TESTABLE_CREATED_AT);
        this.milestone = getRequiredUrlObject(this.resource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        this.active = getBooleanObject(this.resource, TestableVocabulary.TESTABLE_ACTIVE);
        this.tags = getUrlObjects(this.resource, TestableVocabulary.TESTABLE_HAS_TAG);
        this.comments = getStringObjects(this.resource, TestableVocabulary.TESTABLE_HAS_COMMENT);
    }

    public static TestableCRUDResource build(URL url, DataFactory dataFactory, boolean unprotected) throws SaiException, SaiNotFoundException {
        TestableCRUDResource testable = new TestableCRUDResource(url, dataFactory, unprotected);
        testable.bootstrap();
        return testable;
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

}

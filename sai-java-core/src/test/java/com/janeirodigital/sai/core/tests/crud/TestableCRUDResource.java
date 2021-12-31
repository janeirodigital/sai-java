package com.janeirodigital.sai.core.tests.crud;

import com.janeirodigital.sai.core.DataFactory;
import com.janeirodigital.sai.core.crud.CRUDResource;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import lombok.Getter;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.List;

import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.tests.TestableVocabulary.*;

@Getter
public class TestableCRUDResource extends CRUDResource {

    private int id;
    private String name;
    private OffsetDateTime createdAt;
    private URL milestone;
    private boolean active;
    private List<URL> tags;
    private List<String> comments;

    public TestableCRUDResource(URL resourceUrl, DataFactory dataFactory) throws SaiException {
        super(resourceUrl, dataFactory);
    }

    public TestableCRUDResource(URL resourceUrl, DataFactory dataFactory, Resource resource) throws SaiException {
        super(resourceUrl, dataFactory, resource);
    }

    private void bootstrap() throws SaiException, SaiNotFoundException {
        this.fetchData();
        // populate the application profile fields
        this.id = getRequiredIntegerObject(this.resource, TESTABLE_ID);
        this.name = getRequiredStringObject(this.resource, TESTABLE_NAME);
        this.createdAt = getRequiredDateTimeObject(this.resource, TESTABLE_CREATED_AT);
        this.milestone = getRequiredUrlObject(this.resource, TESTABLE_HAS_MILESTONE);
        this.active = getBooleanObject(this.resource, TESTABLE_ACTIVE);
        this.tags = getUrlObjects(this.resource, TESTABLE_HAS_TAG);
        this.comments = getStringObjects(this.resource, TESTABLE_HAS_COMMENT);
    }

    public static TestableCRUDResource build(URL url, DataFactory dataFactory) throws SaiException, SaiNotFoundException {
        TestableCRUDResource testable = new TestableCRUDResource(url, dataFactory);
        testable.bootstrap();
        return testable;
    }

    public void setId(int id) throws SaiException {
        this.id = id;
        updateObject(this.resource, TESTABLE_ID, id);
    }

    public void setName(String name) throws SaiException {
        this.name = name;
        updateObject(this.resource, TESTABLE_NAME, name);
    }

    public void setCreatedAt(OffsetDateTime createdAt) throws SaiException {
        this.createdAt = createdAt;
        updateObject(this.resource, TESTABLE_CREATED_AT, createdAt);
    }

    public void setActive(boolean isActive) throws SaiException {
        this.active = isActive;
        updateObject(this.resource, TESTABLE_ACTIVE, isActive);
    }

    public void setMilestone(URL milestone) throws SaiException {
        this.milestone = milestone;
        updateObject(this.resource, TESTABLE_HAS_MILESTONE, milestone);
    }

    public void setTags(List<URL> tags) throws SaiException {
        this.tags = tags;
        updateUrlObjects(this.resource, TESTABLE_HAS_TAG, tags);
    }

    public void setComments(List<String> comments) throws SaiException {
        this.comments = comments;
        updateStringObjects(this.resource, TESTABLE_HAS_COMMENT, comments);
    }

}

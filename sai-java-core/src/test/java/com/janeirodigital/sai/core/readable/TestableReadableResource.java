package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.DataFactory;
import com.janeirodigital.sai.core.TestableVocabulary;
import com.janeirodigital.sai.core.crud.CRUDResource;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import lombok.Getter;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.List;

import static com.janeirodigital.sai.core.helpers.RdfHelper.*;

@Getter
public class TestableReadableResource extends CRUDResource {

    private int id;
    private String name;
    private OffsetDateTime createdAt;
    private URL milestone;
    private boolean active;
    private List<URL> tags;
    private List<String> comments;

    public TestableReadableResource(URL resourceUrl, DataFactory dataFactory) throws SaiException {
        super(resourceUrl, dataFactory);
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

    public static TestableReadableResource build(URL url, DataFactory dataFactory) throws SaiException, SaiNotFoundException {
        TestableReadableResource testable = new TestableReadableResource(url, dataFactory);
        testable.bootstrap();
        return testable;
    }

}

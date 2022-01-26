package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.factories.DataFactory;
import com.janeirodigital.sai.core.TestableVocabulary;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.readable.TestableReadableResource;
import lombok.Getter;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.List;

import static com.janeirodigital.sai.core.helpers.RdfHelper.*;

@Getter
public class TestableImmutableResource extends ImmutableResource {

    private final int id;
    private final String name;
    private final OffsetDateTime createdAt;
    private final URL milestone;
    private final boolean active;
    private final List<URL> tags;
    private final List<String> comments;

    public TestableImmutableResource(URL resourceUrl, DataFactory dataFactory, Resource resource, boolean unprotected) throws SaiNotFoundException, SaiException {
        super(resourceUrl, dataFactory, resource, unprotected);
        this.id = getRequiredIntegerObject(this.resource, TestableVocabulary.TESTABLE_ID);
        this.name = getRequiredStringObject(this.resource, TestableVocabulary.TESTABLE_NAME);
        this.createdAt = getRequiredDateTimeObject(this.resource, TestableVocabulary.TESTABLE_CREATED_AT);
        this.milestone = getRequiredUrlObject(this.resource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
        this.active = getBooleanObject(this.resource, TestableVocabulary.TESTABLE_ACTIVE);
        this.tags = getUrlObjects(this.resource, TestableVocabulary.TESTABLE_HAS_TAG);
        this.comments = getStringObjects(this.resource, TestableVocabulary.TESTABLE_HAS_COMMENT);
    }

    public TestableReadableResource store() throws SaiException, SaiNotFoundException {
        this.create();
        return TestableReadableResource.build(this.url, this.dataFactory, this.unprotected);
    }

}

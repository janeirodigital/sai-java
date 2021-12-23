package com.janeirodigital.sai.core.tests.immutable;

import com.janeirodigital.sai.core.DataFactory;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.immutable.ImmutableResource;
import com.janeirodigital.sai.core.tests.readable.TestableReadableResource;
import lombok.Getter;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.List;

import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.tests.TestableVocabulary.*;

@Getter
public class TestableImmutableResource extends ImmutableResource {

    private final int id;
    private final String name;
    private final OffsetDateTime createdAt;
    private final URL milestone;
    private final boolean active;
    private final List<URL> tags;
    private final List<String> comments;

    public TestableImmutableResource(URL resourceUrl, DataFactory dataFactory, Resource resource) throws SaiNotFoundException, SaiException {
        super(resourceUrl, dataFactory, resource);
        this.id = getRequiredIntegerObject(this.resource, TESTABLE_ID);
        this.name = getRequiredStringObject(this.resource, TESTABLE_NAME);
        this.createdAt = getRequiredDateTimeObject(this.resource, TESTABLE_CREATED_AT);
        this.milestone = getRequiredUrlObject(this.resource, TESTABLE_HAS_MILESTONE);
        this.active = getBooleanObject(this.resource, TESTABLE_ACTIVE);
        this.tags = getUrlObjects(this.resource, TESTABLE_HAS_TAG);
        this.comments = getStringObjects(this.resource, TESTABLE_HAS_COMMENT);
    }

    public TestableReadableResource store() throws SaiException, SaiNotFoundException {
        this.create();
        return TestableReadableResource.build(this.url, this.dataFactory);
    }

}

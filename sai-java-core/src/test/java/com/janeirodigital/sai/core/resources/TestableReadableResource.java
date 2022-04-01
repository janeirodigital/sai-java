package com.janeirodigital.sai.core.resources;

import com.janeirodigital.sai.core.vocabularies.TestableVocabulary;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.sai.rdfutils.SaiRdfNotFoundException;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.httputils.ContentType.TEXT_TURTLE;
import static com.janeirodigital.sai.rdfutils.RdfUtils.*;

@Getter
public class TestableReadableResource extends ReadableResource {

    private final int id;
    private final String name;
    private final OffsetDateTime createdAt;
    private final URI milestone;
    private final boolean active;
    private final List<URI> tags;
    private final List<String> comments;

    private TestableReadableResource(Builder builder) throws SaiException {
        super(builder);
        this.id = builder.id;
        this.name = builder.name;
        this.createdAt = builder.createdAt;
        this.milestone = builder.milestone;
        this.active = builder.active;
        this.tags = builder.tags;
        this.comments = builder.comments;
    }

    public static TestableReadableResource get(URI uri, SaiSession saiSession, boolean unprotected) throws SaiException, SaiHttpNotFoundException {
        Objects.requireNonNull(uri, "Must provide a URI to get");
        Objects.requireNonNull(saiSession, "Must provide a sai session to assign");
        TestableReadableResource.Builder builder = new TestableReadableResource.Builder(uri, saiSession);
        if (unprotected) builder.setUnprotected();
        try (Response response = read(uri, saiSession, TEXT_TURTLE, unprotected)) {
            return builder.setDataset(response).setContentType(TEXT_TURTLE).build();
        }
    }

    public TestableReadableResource reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.uri, this.saiSession, this.unprotected);
    }

    public static class Builder extends ReadableResource.Builder<Builder> {

        private int id;
        private String name;
        private OffsetDateTime createdAt;
        private URI milestone;
        private boolean active;
        private List<URI> tags;
        private List<String> comments;

        public Builder(URI uri, SaiSession saiSession) {
            super(uri, saiSession);
            this.tags = new ArrayList<>();
            this.comments = new ArrayList<>();
        }

        @Override
        public Builder getThis() { return this; }

        @Override
        public Builder setDataset(Model dataset) throws SaiException {
            super.setDataset(dataset);
            populateFromDataset();
            this.exists = true;
            return this;
        }

        private void populateFromDataset() throws SaiException {
            try {
                this.id = getRequiredIntegerObject(this.resource, TestableVocabulary.TESTABLE_ID);
                this.name = getRequiredStringObject(this.resource, TestableVocabulary.TESTABLE_NAME);
                this.createdAt = getRequiredDateTimeObject(this.resource, TestableVocabulary.TESTABLE_CREATED_AT);
                this.milestone = getRequiredUriObject(this.resource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
                this.active = getBooleanObject(this.resource, TestableVocabulary.TESTABLE_ACTIVE);
                this.tags = getUriObjects(this.resource, TestableVocabulary.TESTABLE_HAS_TAG);
                this.comments = getStringObjects(this.resource, TestableVocabulary.TESTABLE_HAS_COMMENT);
            } catch (SaiRdfException | SaiRdfNotFoundException ex) {
                throw new SaiException("Unable to populate readable resource", ex);
            }
        }

        public TestableReadableResource build() throws SaiException {
            return new TestableReadableResource(this);
        }
    }

}

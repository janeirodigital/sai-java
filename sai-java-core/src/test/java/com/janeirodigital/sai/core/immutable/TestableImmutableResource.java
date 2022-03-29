package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.TestableVocabulary;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.sai.rdfutils.SaiRdfNotFoundException;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.TestableVocabulary.*;
import static com.janeirodigital.sai.httputils.ContentType.TEXT_TURTLE;
import static com.janeirodigital.sai.rdfutils.RdfUtils.*;

@Getter
public class TestableImmutableResource extends ImmutableResource {

    private static final String TESTABLE_RDF_TYPE = "https://graph.example/ns/terms#testable";

    private final int id;
    private final String name;
    private final OffsetDateTime createdAt;
    private final URL milestone;
    private final boolean active;
    private final List<URL> tags;
    private final List<String> comments;

    public TestableImmutableResource(Builder builder) throws SaiException {
        super(builder);
        this.id = builder.id;
        this.name = builder.name;
        this.createdAt = builder.createdAt;
        this.milestone = builder.milestone;
        this.active = builder.active;
        this.tags = builder.tags;
        this.comments = builder.comments;
    }

    public static TestableImmutableResource get(URL url, SaiSession saiSession, boolean unprotected) throws SaiHttpNotFoundException, SaiException {
        Objects.requireNonNull(url, "Must provide a URL to get");
        Objects.requireNonNull(saiSession, "Must provide a sai session to assign");
        TestableImmutableResource.Builder builder = new TestableImmutableResource.Builder(url, saiSession);
        if (unprotected) builder.setUnprotected();
        try (Response response = read(url, saiSession, TEXT_TURTLE, unprotected)) {
            return builder.setDataset(response).setContentType(TEXT_TURTLE).build();
        }
    }

    public TestableImmutableResource reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.unprotected);
    }

    public static class Builder extends ImmutableResource.Builder<Builder> {

        private int id;
        private String name;
        private OffsetDateTime createdAt;
        private URL milestone;
        private boolean active;
        private List<URL> tags;
        private List<String> comments;

        public Builder(URL url, SaiSession saiSession) {
            super(url, saiSession);
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

        public Builder setId(int id) {
            Objects.requireNonNull(id, "Must provide an id");
            this.id = id;
            return this;
        }

        public Builder setName(String name) {
            Objects.requireNonNull(name, "Must provide a name");
            this.name = name;
            return this;
        }

        public Builder setCreatedAt(OffsetDateTime createdAt) {
            Objects.requireNonNull(createdAt, "Must provide created at");
            this.createdAt = createdAt;
            return this;
        }

        public Builder setMilestone(URL milestone) {
            Objects.requireNonNull(milestone, "Must provide milestone");
            this.milestone = milestone;
            return this;
        }

        public Builder setActive(boolean active) {
            this.active = active;
            return this;
        }

        public Builder setTags(List<URL> tags) {
            Objects.requireNonNull(tags, "Must provide tags");
            this.tags = tags;
            return this;
        }

        public Builder setComments(List<String> comments) {
            Objects.requireNonNull(comments, "Must provide comments");
            this.comments = comments;
            return this;
        }

        private void populateFromDataset() throws SaiException {
            try {
                this.id = getRequiredIntegerObject(this.resource, TESTABLE_ID);
                this.name = getRequiredStringObject(this.resource, TESTABLE_NAME);
                this.createdAt = getRequiredDateTimeObject(this.resource, TestableVocabulary.TESTABLE_CREATED_AT);
                this.milestone = getRequiredUrlObject(this.resource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
                this.active = getBooleanObject(this.resource, TestableVocabulary.TESTABLE_ACTIVE);
                this.tags = getUrlObjects(this.resource, TestableVocabulary.TESTABLE_HAS_TAG);
                this.comments = getStringObjects(this.resource, TestableVocabulary.TESTABLE_HAS_COMMENT);
            } catch (SaiRdfException | SaiRdfNotFoundException ex) {
                throw new SaiException("Unable to bootstrap testable crud resource. Missing required fields", ex);
            }
        }

        private void populateDataset() {
            this.resource = getNewResourceForType(this.url, TESTABLE_RDF_TYPE);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, TESTABLE_ID, this.id);
            updateObject(this.resource, TESTABLE_NAME, this.name);
            updateObject(this.resource, TESTABLE_CREATED_AT, this.createdAt);
            updateObject(this.resource, TESTABLE_HAS_MILESTONE, this.milestone);
            updateObject(this.resource, TESTABLE_ACTIVE, this.active);
            updateUrlObjects(this.resource, TESTABLE_HAS_TAG, this.tags);
            updateStringObjects(this.resource, TESTABLE_HAS_COMMENT, this.comments);
        }

        public TestableImmutableResource build() throws SaiException {
            if (this.dataset == null) { populateDataset(); }
            return new TestableImmutableResource(this);
        }
    }
}

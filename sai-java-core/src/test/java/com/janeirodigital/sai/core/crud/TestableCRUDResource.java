package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.TestableVocabulary;
import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.Getter;
import okhttp3.Headers;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.TestableVocabulary.*;
import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.enums.ContentType.TEXT_TURTLE;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;

@Getter
public class TestableCRUDResource extends CRUDResource {

    private static final String TESTABLE_RDF_TYPE = "https://graph.example/ns/terms#testable";

    private final int id;
    private final String name;
    private final OffsetDateTime createdAt;
    private final URL milestone;
    private final boolean active;
    private final List<URL> tags;
    private final List<String> comments;

    public TestableCRUDResource(URL resourceUrl, DataFactory dataFactory, boolean unprotected, Model dataset,
                                Resource resource, ContentType contentType, int id, String name, OffsetDateTime createdAt,
                                URL milestone, boolean active, List<URL> tags, List<String> comments) throws SaiException {
        super(resourceUrl, dataFactory, unprotected);
        this.dataset = dataset;
        this.resource = resource;
        this.contentType = contentType;
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.milestone = milestone;
        this.active = active;
        this.tags = tags;
        this.comments = comments;
    }

    public static TestableCRUDResource get(URL url, DataFactory dataFactory, boolean unprotected) throws SaiNotFoundException, SaiException {
        Objects.requireNonNull(url, "Must provide a URL to get");
        Objects.requireNonNull(dataFactory, "Must provide a data factory to assign");
        if (unprotected) { return getProtected(url, dataFactory); } else { return getUnprotected(url, dataFactory); }
    }

    private static TestableCRUDResource getProtected(URL url, DataFactory dataFactory) throws SaiException, SaiNotFoundException {
        TestableCRUDResource.Builder builder = new TestableCRUDResource.Builder(url, dataFactory, false, TEXT_TURTLE);
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, TEXT_TURTLE.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(dataFactory.getAuthorizedSession(), dataFactory.getHttpClient(), url, headers))) {
            builder.setDataset(getRdfModelFromResponse(response));
        }
        return builder.build();
    }

    private static TestableCRUDResource getUnprotected(URL url, DataFactory dataFactory) throws SaiException, SaiNotFoundException {
        TestableCRUDResource.Builder builder = new TestableCRUDResource.Builder(url, dataFactory, false, TEXT_TURTLE);
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, TEXT_TURTLE.getValue());
        try (Response response = checkReadableResponse(getRdfResource(dataFactory.getHttpClient(), url, headers))) {
            builder.setDataset(getRdfModelFromResponse(response));
        }
        return builder.build();
    }

    public static class Builder {

        private final URL url;
        private final DataFactory dataFactory;
        private final boolean unprotected;
        private final ContentType contentType;
        private Model dataset;
        private Resource resource;
        private int id;
        private String name;
        private OffsetDateTime createdAt;
        private URL milestone;
        private boolean active;
        private List<URL> tags;
        private List<String> comments;

        public Builder(URL url, DataFactory dataFactory, boolean unprotected, ContentType contentType) {
            Objects.requireNonNull(url, "Must provide a URL");
            Objects.requireNonNull(dataFactory, "Must provide a data factory");
            this.url = url;
            this.dataFactory = dataFactory;
            this.unprotected = unprotected;
            this.contentType = contentType;
            this.tags = new ArrayList<>();
            this.comments = new ArrayList<>();
        }

        public Builder setDataset(Model dataset) throws SaiException {
            Objects.requireNonNull(dataset, "Must provide a Jena model");
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            populateFromDataset();
            return this;
        }

        public Builder setId(int id) {
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

        public TestableCRUDResource build() throws SaiException {
            if (this.dataset == null) { populateDataset(); }
            return new TestableCRUDResource(this.url, this.dataFactory, this.unprotected, this.dataset, this.resource,
                    this.contentType, this.id, this.name, this.createdAt, this.milestone, this.active, this.tags, this.comments);
        }

    }

}

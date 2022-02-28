package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.TestableVocabulary;
import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import okhttp3.Headers;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.enums.ContentType.TEXT_TURTLE;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;

@Getter
public class TestableReadableResource extends ReadableResource {

    private final int id;
    private final String name;
    private final OffsetDateTime createdAt;
    private final URL milestone;
    private final boolean active;
    private final List<URL> tags;
    private final List<String> comments;

    private TestableReadableResource(URL resourceUrl, SaiSession saiSession, boolean unprotected, Model dataset,
                                     Resource resource, ContentType contentType, int id, String name, OffsetDateTime createdAt,
                                     URL milestone, boolean active, List<URL> tags, List<String> comments) throws SaiException {
        super(resourceUrl, saiSession, unprotected);
        this.dataset =  dataset;
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

    public static TestableReadableResource get(URL url, SaiSession saiSession, boolean unprotected) throws SaiException, SaiNotFoundException {
        if (unprotected) { return getProtected(url, saiSession); } else { return getUnprotected(url, saiSession); }
    }

    private static TestableReadableResource getProtected(URL url, SaiSession saiSession) throws SaiException, SaiNotFoundException {
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, TEXT_TURTLE.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(saiSession.getAuthorizedSession(), saiSession.getHttpClient(), url, headers))) {
            return new Builder(url, saiSession, false, TEXT_TURTLE, getRdfModelFromResponse(response)).build();
        }
    }

    private static TestableReadableResource getUnprotected(URL url, SaiSession saiSession) throws SaiException, SaiNotFoundException {
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, TEXT_TURTLE.getValue());
        try (Response response = checkReadableResponse(getRdfResource(saiSession.getHttpClient(), url, headers))) {
            return new Builder(url, saiSession, true, TEXT_TURTLE, getRdfModelFromResponse(response)).build();
        }
    }

    public static class Builder {

        private final URL url;
        private final SaiSession saiSession;
        private final boolean unprotected;
        private final ContentType contentType;
        private final Model dataset;
        private final Resource resource;
        private int id;
        private String name;
        private OffsetDateTime createdAt;
        private URL milestone;
        private boolean active;
        private List<URL> tags;
        private List<String> comments;

        public Builder(URL url, SaiSession saiSession, boolean unprotected, ContentType contentType, Model dataset) throws SaiException, SaiNotFoundException {
            Objects.requireNonNull(url, "Must provide a URL for the readable social agent profile builder");
            Objects.requireNonNull(saiSession, "Must provide a sai session for the readable social agent profile builder");
            Objects.requireNonNull(contentType, "Must provide a content type to use for retrieval of readable social agent profile ");
            Objects.requireNonNull(dataset, "Must provide a dateset to use to populate the readable social agent profile ");
            this.url = url;
            this.saiSession = saiSession;
            this.unprotected = unprotected;
            this.contentType = contentType;
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            populateFromDataset();
        }

        private void populateFromDataset() throws SaiException, SaiNotFoundException {
            this.id = getRequiredIntegerObject(this.resource, TestableVocabulary.TESTABLE_ID);
            this.name = getRequiredStringObject(this.resource, TestableVocabulary.TESTABLE_NAME);
            this.createdAt = getRequiredDateTimeObject(this.resource, TestableVocabulary.TESTABLE_CREATED_AT);
            this.milestone = getRequiredUrlObject(this.resource, TestableVocabulary.TESTABLE_HAS_MILESTONE);
            this.active = getBooleanObject(this.resource, TestableVocabulary.TESTABLE_ACTIVE);
            this.tags = getUrlObjects(this.resource, TestableVocabulary.TESTABLE_HAS_TAG);
            this.comments = getStringObjects(this.resource, TestableVocabulary.TESTABLE_HAS_COMMENT);
        }

        public TestableReadableResource build() throws SaiException {
            return new TestableReadableResource(this.url, this.saiSession, this.unprotected, this.dataset, this.resource,
                                                this.contentType, this.id, this.name, this.createdAt, this.milestone,
                                                this.active, this.tags, this.comments);
        }
    }

}

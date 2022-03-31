package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.sai.rdfutils.SaiRdfNotFoundException;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URI;
import java.time.OffsetDateTime;

import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.httputils.HttpUtils.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.rdfutils.RdfUtils.getRequiredDateTimeObject;
import static com.janeirodigital.sai.rdfutils.RdfUtils.getRequiredUriObject;

/**
 * Readable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-registration">Data Registration</a>.
 */
@Getter
public class ReadableDataRegistration extends ReadableResource {

    private final URI registeredBy;
    private final URI registeredWith;
    private final OffsetDateTime registeredAt;
    private final OffsetDateTime updatedAt;
    private final URI registeredShapeTree;

    /**
     * Construct a {@link ReadableDataRegistration} instance from the provided {@link Builder}.
     * @param builder {@link ReadableAccessGrant.Builder} to construct with
     * @throws SaiException
     */
    private ReadableDataRegistration(Builder builder) throws SaiException {
        super(builder);
        this.registeredBy = builder.registeredBy;
        this.registeredWith = builder.registeredWith;
        this.registeredAt = builder.registeredAt;
        this.updatedAt = builder.updatedAt;
        this.registeredShapeTree = builder.registeredShapeTree;
    }

    /**
     * Get a {@link ReadableDataRegistration} at the provided <code>uri</code>
     * @param uri URI of the {@link ReadableDataRegistration} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link ReadableDataRegistration}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static ReadableDataRegistration get(URI uri, SaiSession saiSession, ContentType contentType) throws SaiException, SaiHttpNotFoundException {
        ReadableDataRegistration.Builder builder = new ReadableDataRegistration.Builder(uri, saiSession);
        try (Response response = read(uri, saiSession, contentType, false)) {
            return builder.setDataset(response).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URI, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param uri URI of the {@link ReadableDataRegistration} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link ReadableDataRegistration}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static ReadableDataRegistration get(URI uri, SaiSession saiSession) throws SaiException, SaiHttpNotFoundException {
        return get(uri, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link ReadableDataRegistration} using the attributes of the current instance
     * @return Reloaded {@link ReadableDataRegistration}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public ReadableDataRegistration reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.uri, this.saiSession, this.contentType);
    }

    /**
     * Builder for {@link ReadableDataRegistration} instances.
     */
    private static class Builder extends ReadableResource.Builder<Builder> {

        private URI registeredBy;
        private URI registeredWith;
        private OffsetDateTime registeredAt;
        private OffsetDateTime updatedAt;
        private URI registeredShapeTree;

        /**
         * Initialize builder with <code>uri</code> and <code>saiSession</code>
         * @param uri URI of the {@link ReadableApplicationProfile} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URI uri, SaiSession saiSession) { super(uri, saiSession); }

        /**
         * Ensures that don't get an unchecked cast warning when returning from setters
         * @return {@link Builder}
         */
        @Override
        public Builder getThis() { return this; }

        /**
         * Set the Jena model and use it to populate attributes of the {@link Builder}. Assumption
         * is made that the corresponding resource exists.
         * @param dataset Jena model to populate the Builder attributes with
         * @return {@link Builder}
         * @throws SaiException
         */
        @Override
        public Builder setDataset(Model dataset) throws SaiException {
            super.setDataset(dataset);
            populateFromDataset();
            this.exists = true;
            return this;
        }

        /**
         * Populates the fields of the {@link Builder} based on the associated Jena resource.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException {
            try {
                this.registeredBy = getRequiredUriObject(this.resource, REGISTERED_BY);
                this.registeredWith = getRequiredUriObject(this.resource, REGISTERED_WITH);
                this.registeredAt = getRequiredDateTimeObject(this.resource, REGISTERED_AT);
                this.updatedAt = getRequiredDateTimeObject(this.resource, UPDATED_AT);
                this.registeredShapeTree = getRequiredUriObject(this.resource, REGISTERED_SHAPE_TREE);
            } catch (SaiRdfException | SaiRdfNotFoundException ex) {
                throw new SaiException("Unable to populate readable data registration resource", ex);
            }
        }

        /**
         * Build the {@link ReadableDataRegistration} using attributes from the Builder.
         * @return {@link ReadableDataRegistration}
         * @throws SaiException
         */
        public ReadableDataRegistration build() throws SaiException {
            return new ReadableDataRegistration(this);
        }
    }
}

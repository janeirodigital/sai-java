package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URL;
import java.time.OffsetDateTime;

import static com.janeirodigital.sai.core.helpers.HttpHelper.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.core.helpers.HttpHelper.getRdfModelFromResponse;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getRequiredDateTimeObject;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getRequiredUrlObject;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

/**
 * Readable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-registration">Data Registration</a>.
 */
@Getter
public class ReadableDataRegistration extends ReadableResource {

    private final URL registeredBy;
    private final URL registeredWith;
    private final OffsetDateTime registeredAt;
    private final OffsetDateTime updatedAt;
    private final URL registeredShapeTree;

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
     * Get a {@link ReadableDataRegistration} at the provided <code>url</code>
     * @param url URL of the {@link ReadableDataRegistration} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link ReadableDataRegistration}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ReadableDataRegistration get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        ReadableDataRegistration.Builder builder = new ReadableDataRegistration.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link ReadableDataRegistration} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link ReadableDataRegistration}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ReadableDataRegistration get(URL url, SaiSession saiSession) throws SaiException, SaiNotFoundException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link ReadableDataRegistration} using the attributes of the current instance
     * @return Reloaded {@link ReadableDataRegistration}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public ReadableDataRegistration reload() throws SaiNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    /**
     * Builder for {@link ReadableDataRegistration} instances.
     */
    private static class Builder extends ReadableResource.Builder<Builder> {

        private URL registeredBy;
        private URL registeredWith;
        private OffsetDateTime registeredAt;
        private OffsetDateTime updatedAt;
        private URL registeredShapeTree;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link ReadableApplicationProfile} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URL url, SaiSession saiSession) { super(url, saiSession); }

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
                this.registeredBy = getRequiredUrlObject(this.resource, REGISTERED_BY);
                this.registeredWith = getRequiredUrlObject(this.resource, REGISTERED_WITH);
                this.registeredAt = getRequiredDateTimeObject(this.resource, REGISTERED_AT);
                this.updatedAt = getRequiredDateTimeObject(this.resource, UPDATED_AT);
                this.registeredShapeTree = getRequiredUrlObject(this.resource, REGISTERED_SHAPE_TREE);
            } catch (SaiNotFoundException ex) {
                throw new SaiException("Unable to populate readable data registration resource: " + ex.getMessage());
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

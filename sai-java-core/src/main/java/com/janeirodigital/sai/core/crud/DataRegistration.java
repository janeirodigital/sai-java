package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import lombok.Setter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.utils.HttpUtils.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.core.utils.HttpUtils.getRdfModelFromResponse;
import static com.janeirodigital.sai.core.utils.RdfUtils.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.LdpVocabulary.LDP_CONTAINS;

/**
 * Modifiable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-registration">Data Registration</a>
 */
@Getter @Setter
public class DataRegistration extends CRUDResource {

    private URL registeredBy;
    private URL registeredWith;
    private OffsetDateTime registeredAt;
    private OffsetDateTime updatedAt;
    private URL registeredShapeTree;
    private List<URL> dataInstances;

    /**
     * Construct a {@link DataRegistration} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private DataRegistration(Builder builder) throws SaiException {
        super(builder);
        this.registeredBy = builder.registeredBy;
        this.registeredWith = builder.registeredWith;
        this.registeredAt = builder.registeredAt;
        this.updatedAt = builder.updatedAt;
        this.registeredShapeTree = builder.registeredShapeTree;
        this.dataInstances = builder.dataInstances;
    }

    /**
     * Get a {@link DataRegistration} at the provided <code>url</code>
     * @param url URL of the {@link DataRegistration} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link DataRegistration}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static DataRegistration get(URL url, SaiSession saiSession, ContentType contentType) throws SaiNotFoundException, SaiException {
        DataRegistration.Builder builder = new DataRegistration.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link DataRegistration} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link DataRegistration}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public static DataRegistration get(URL url, SaiSession saiSession) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link DataRegistration} using the attributes of the current instance
     * @return Reloaded {@link DataRegistration}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public DataRegistration reload() throws SaiNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    /**
     * Builder for {@link DataRegistration} instances.
     */
    public static class Builder extends CRUDResource.Builder<Builder> {

        private URL registeredBy;
        private URL registeredWith;
        private OffsetDateTime registeredAt;
        private OffsetDateTime updatedAt;
        private URL registeredShapeTree;
        private List<URL> dataInstances;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link DataRegistration} to build
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
         * Set the URL of the social agent that registered the data registration
         * @param registeredBy URL of the registering social agent
         * @return {@link Builder}
         */
        public Builder setRegisteredBy(URL registeredBy) {
            Objects.requireNonNull(registeredBy, "Must provide a URL for the social agent that registered the data registration");
            this.registeredBy = registeredBy;
            return this;
        }

        /**
         * Set the URL of the application used to register the data registration
         * @param registeredWith URL of the registering application
         * @return {@link Builder}
         */
        public Builder setRegisteredWith(URL registeredWith) {
            Objects.requireNonNull(registeredWith, "Must provide a URL for the application that registered the data registration");
            this.registeredWith = registeredWith;
            return this;
        }

        /**
         * Set the time the data registration was registered
         * @param registeredAt time the registration was updated
         * @return {@link Builder}
         */
        public Builder setRegisteredAt(OffsetDateTime registeredAt) {
            Objects.requireNonNull(registeredAt, "Must provide the time the data registration was registered");
            this.registeredAt = registeredAt;
            return this;
        }

        /**
         * Set the time the data registration was updated
         * @param updatedAt time the registration was updated
         * @return {@link Builder}
         */
        public Builder setUpdatedAt(OffsetDateTime updatedAt) {
            Objects.requireNonNull(updatedAt, "Must provide the time the data registration was updated");
            this.updatedAt = updatedAt;
            return this;
        }

        /**
         * Set the registered shape tree for the data registration
         * @param registeredShapeTree URL of the registered shape tree
         * @return {@link Builder}
         */
        public Builder setRegisteredShapeTree(URL registeredShapeTree) {
            Objects.requireNonNull(registeredShapeTree, "Must provide the registered shape tree for the data registration");
            this.registeredShapeTree = registeredShapeTree;
            return this;
        }

        /**
         * Populates the fields of the {@link DataRegistration} based on the associated Jena resource.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException {
            try {
                this.registeredBy = getRequiredUrlObject(this.resource, REGISTERED_BY);
                this.registeredWith = getRequiredUrlObject(this.resource, REGISTERED_WITH);
                this.registeredAt = getRequiredDateTimeObject(this.resource, REGISTERED_AT);
                this.updatedAt = getRequiredDateTimeObject(this.resource, UPDATED_AT);
                this.registeredShapeTree = getRequiredUrlObject(this.resource, REGISTERED_SHAPE_TREE);
                this.dataInstances = getUrlObjects(this.resource, LDP_CONTAINS);
            } catch (SaiNotFoundException | SaiException ex) {
                throw new SaiException("Unable to populate data registration: " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         * @throws SaiException
         */
        private void populateDataset() {
            this.resource = getNewResourceForType(this.url, DATA_REGISTRATION);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, REGISTERED_BY, this.registeredBy);
            updateObject(this.resource, REGISTERED_WITH, this.registeredWith);
            updateObject(this.resource, REGISTERED_AT, this.registeredAt);
            updateObject(this.resource, UPDATED_AT, this.updatedAt);
            updateObject(this.resource, REGISTERED_SHAPE_TREE, this.registeredShapeTree);
        }

        /**
         * Build the {@link DataRegistration} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}.
         * @return {@link DataRegistration}
         * @throws SaiException
         */
        public DataRegistration build() throws SaiException {
            Objects.requireNonNull(registeredBy, "Must provide a URL for the social agent that registered the data registration");
            Objects.requireNonNull(registeredWith, "Must provide a URL for the application that registered the data registration");
            Objects.requireNonNull(registeredAt, "Must provide the time the data registration was registered");
            Objects.requireNonNull(updatedAt, "Must provide the time the data registration was updated");
            Objects.requireNonNull(registeredShapeTree, "Must provide the registered shape tree for the data registration");
            if (this.dataset == null) { populateDataset(); }
            return new DataRegistration(this);
        }
    }

}

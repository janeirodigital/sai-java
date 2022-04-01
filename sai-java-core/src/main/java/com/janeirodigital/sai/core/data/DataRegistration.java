package com.janeirodigital.sai.core.data;

import com.janeirodigital.sai.core.resources.CRUDResource;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.sai.rdfutils.SaiRdfNotFoundException;
import lombok.Getter;
import lombok.Setter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.LdpVocabulary.LDP_CONTAINS;
import static com.janeirodigital.sai.httputils.HttpUtils.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.rdfutils.RdfUtils.*;

/**
 * Modifiable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-registration">Data Registration</a>
 */
@Getter @Setter
public class DataRegistration extends CRUDResource {

    private URI registeredBy;
    private URI registeredWith;
    private OffsetDateTime registeredAt;
    private OffsetDateTime updatedAt;
    private URI registeredShapeTree;
    private List<URI> dataInstances;

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
     * Get a {@link DataRegistration} at the provided <code>uri</code>
     * @param uri URI of the {@link DataRegistration} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link DataRegistration}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static DataRegistration get(URI uri, SaiSession saiSession, ContentType contentType) throws SaiHttpNotFoundException, SaiException {
        DataRegistration.Builder builder = new DataRegistration.Builder(uri, saiSession);
        try (Response response = read(uri, saiSession, contentType, false)) {
            return builder.setDataset(response).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URI, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param uri URI of the {@link DataRegistration} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link DataRegistration}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public static DataRegistration get(URI uri, SaiSession saiSession) throws SaiHttpNotFoundException, SaiException {
        return get(uri, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link DataRegistration} using the attributes of the current instance
     * @return Reloaded {@link DataRegistration}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public DataRegistration reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.uri, this.saiSession, this.contentType);
    }

    /**
     * Builder for {@link DataRegistration} instances.
     */
    public static class Builder extends CRUDResource.Builder<Builder> {

        private URI registeredBy;
        private URI registeredWith;
        private OffsetDateTime registeredAt;
        private OffsetDateTime updatedAt;
        private URI registeredShapeTree;
        private List<URI> dataInstances;

        /**
         * Initialize builder with <code>uri</code> and <code>saiSession</code>
         * @param uri URI of the {@link DataRegistration} to build
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
         * Set the URI of the social agent that registered the data registration
         * @param registeredBy URI of the registering social agent
         * @return {@link Builder}
         */
        public Builder setRegisteredBy(URI registeredBy) {
            Objects.requireNonNull(registeredBy, "Must provide a URI for the social agent that registered the data registration");
            this.registeredBy = registeredBy;
            return this;
        }

        /**
         * Set the URI of the application used to register the data registration
         * @param registeredWith URI of the registering application
         * @return {@link Builder}
         */
        public Builder setRegisteredWith(URI registeredWith) {
            Objects.requireNonNull(registeredWith, "Must provide a URI for the application that registered the data registration");
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
         * @param registeredShapeTree URI of the registered shape tree
         * @return {@link Builder}
         */
        public Builder setRegisteredShapeTree(URI registeredShapeTree) {
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
                this.registeredBy = getRequiredUriObject(this.resource, REGISTERED_BY);
                this.registeredWith = getRequiredUriObject(this.resource, REGISTERED_WITH);
                this.registeredAt = getRequiredDateTimeObject(this.resource, REGISTERED_AT);
                this.updatedAt = getRequiredDateTimeObject(this.resource, UPDATED_AT);
                this.registeredShapeTree = getRequiredUriObject(this.resource, REGISTERED_SHAPE_TREE);
                this.dataInstances = getUriObjects(this.resource, LDP_CONTAINS);
            } catch (SaiRdfException | SaiRdfNotFoundException ex) {
                throw new SaiException("Unable to populate data registration", ex);
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        private void populateDataset() {
            this.resource = getNewResourceForType(this.uri, DATA_REGISTRATION);
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
            Objects.requireNonNull(registeredBy, "Must provide a URI for the social agent that registered the data registration");
            Objects.requireNonNull(registeredWith, "Must provide a URI for the application that registered the data registration");
            Objects.requireNonNull(registeredAt, "Must provide the time the data registration was registered");
            Objects.requireNonNull(updatedAt, "Must provide the time the data registration was updated");
            Objects.requireNonNull(registeredShapeTree, "Must provide the registered shape tree for the data registration");
            if (this.dataset == null) { populateDataset(); }
            return new DataRegistration(this);
        }
    }

}

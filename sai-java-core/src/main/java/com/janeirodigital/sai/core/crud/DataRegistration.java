package com.janeirodigital.sai.core.crud;

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
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

/**
 * Modifiable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-registration">Data Registration</a>
 */
@Getter
public class DataRegistration extends CRUDResource {

    private URL registeredBy;
    private URL registeredWith;
    private OffsetDateTime registeredAt;
    private OffsetDateTime updatedAt;
    private URL registeredShapeTree;

    /**
     * Construct a new {@link DataRegistration}
     * @param url URL of the {@link DataRegistration}
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    private DataRegistration(URL url, DataFactory dataFactory, Model dataset, Resource resource, ContentType contentType,
                             URL registeredBy, URL registeredWith, OffsetDateTime registeredAt, OffsetDateTime updatedAt,
                             URL registeredShapeTree) throws SaiException {
        super(url, dataFactory, false);
        this.dataset = dataset;
        this.resource = resource;
        this.contentType = contentType;
        this.registeredBy = registeredBy;
        this.registeredWith = registeredWith;
        this.registeredAt = registeredAt;
        this.updatedAt = updatedAt;
        this.registeredShapeTree = registeredShapeTree;
    }

    /**
     * Get a {@link DataRegistration} at the provided <code>url</code>
     * @param url URL of the {@link DataRegistration} to get
     * @param dataFactory {@link DataFactory} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link DataRegistration}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static DataRegistration get(URL url, DataFactory dataFactory, ContentType contentType) throws SaiNotFoundException, SaiException {
        Objects.requireNonNull(url, "Must provide the URL of the data registration to get");
        Objects.requireNonNull(dataFactory, "Must provide a data factory to assign to the data registration");
        Objects.requireNonNull(contentType, "Must provide a content type for the data registration");
        Builder builder = new Builder(url, dataFactory, contentType);
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(dataFactory.getAuthorizedSession(), dataFactory.getHttpClient(), url, headers))) {
            builder.setDataset(getRdfModelFromResponse(response));
        }
        return builder.build();
    }

    /**
     * Call {@link #get(URL, DataFactory, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link DataRegistration} to get
     * @param dataFactory {@link DataFactory} to assign
     * @return Retrieved {@link DataRegistration}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public static DataRegistration get(URL url, DataFactory dataFactory) throws SaiNotFoundException, SaiException {
        return get(url, dataFactory, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Builder for {@link DataRegistration} instances.
     */
    public static class Builder {

        private final URL url;
        private final DataFactory dataFactory;
        private final ContentType contentType;
        private Model dataset;
        private Resource resource;
        private URL registeredBy;
        private URL registeredWith;
        private OffsetDateTime registeredAt;
        private OffsetDateTime updatedAt;
        private URL registeredShapeTree;

        /**
         * Initialize builder with <code>url</code>, <code>dataFactory</code>, and desired <code>contentType</code>
         * @param url URL of the {@link DataRegistration} to build
         * @param dataFactory {@link DataFactory} to assign
         * @param contentType {@link ContentType} to assign
         */
        public Builder(URL url, DataFactory dataFactory, ContentType contentType) {
            Objects.requireNonNull(url, "Must provide a URL for the data registration builder");
            Objects.requireNonNull(dataFactory, "Must provide a data factory for the data registration builder");
            Objects.requireNonNull(contentType, "Must provide a content type for the data registration builder");
            this.url = url;
            this.dataFactory = dataFactory;
            this.contentType = contentType;
        }

        /**
         * Optional Jena Model that will initialize the attributes of the Builder rather than set
         * them manually. Typically used in read scenarios when populating the Builder from
         * the contents of a remote resource.
         * @param dataset Jena model to populate the Builder attributes with
         * @return {@link Builder}
         */
        public Builder setDataset(Model dataset) throws SaiException {
            Objects.requireNonNull(dataset, "Must provide a Jena model for the data registration builder");
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            populateFromDataset();
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
         * {@link #populateDataset()}. Conversely, if a dataset was provided, the attributes of the
         * Builder will be populated from it.
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
            return new DataRegistration(this.url, this.dataFactory, this.dataset, this.resource, this.contentType,
                                        this.registeredBy, this.registeredWith, this.registeredAt, this.updatedAt,
                                        this.registeredShapeTree);
        }


    }

}

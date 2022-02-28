package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.Getter;
import okhttp3.Headers;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

/**
 * Readable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-consent">Data Grant</a>
 */
@Getter
public abstract class ReadableDataGrant extends ReadableResource {

    private final URL dataOwner;
    private final URL grantee;
    private final URL registeredShapeTree;
    private final List<RDFNode> accessModes;
    private final List<RDFNode> creatorAccessModes;
    private final RDFNode scopeOfGrant;
    private final URL dataRegistration;
    private final URL accessNeed;
    private final URL delegationOf;

    protected ReadableDataGrant(URL url, DataFactory dataFactory, Model dataset, Resource resource, ContentType contentType, URL dataOwner,
                                URL grantee, URL registeredShapeTree, List<RDFNode> accessModes, List<RDFNode> creatorAccessModes,
                                RDFNode scopeOfGrant, URL dataRegistration, URL accessNeed, URL delegationOf) throws SaiException {
        super(url, dataFactory, false);
        this.dataset = dataset;
        this.resource = resource;
        this.contentType = contentType;
        this.dataOwner = dataOwner;
        this.grantee = grantee;
        this.registeredShapeTree = registeredShapeTree;
        this.accessModes = accessModes;
        this.creatorAccessModes = creatorAccessModes;
        this.scopeOfGrant = scopeOfGrant;
        this.dataRegistration = dataRegistration;
        this.accessNeed = accessNeed;
        this.delegationOf = delegationOf;
    }

    /**
     * Get a {@link ReadableDataGrant} at the provided <code>url</code>
     * @param url URL of the {@link ReadableDataGrant} to get
     * @param dataFactory {@link DataFactory} to assign
     * @return Retrieved {@link ReadableDataGrant}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ReadableDataGrant get(URL url, DataFactory dataFactory, ContentType contentType) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(url, "Must provide the URL of the readable data grant to get");
        Objects.requireNonNull(dataFactory, "Must provide a data factory to assign to the readable data grant");
        Objects.requireNonNull(contentType, "Must provide a content type for the readable data grant");
        ReadableDataGrant.Builder builder = new ReadableDataGrant.Builder(url, dataFactory, contentType);
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(dataFactory.getAuthorizedSession(), dataFactory.getHttpClient(), url, headers))) {
            builder.setDataset(getRdfModelFromResponse(response));
        }
        ReadableDataGrant dataGrant = builder.build();
        return dataGrant;
    }

    /**
     * Call {@link #get(URL, DataFactory, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link ReadableDataGrant} to get
     * @param dataFactory {@link DataFactory} to assign
     * @return Retrieved {@link ReadableDataGrant}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public static ReadableDataGrant get(URL url, DataFactory dataFactory) throws SaiNotFoundException, SaiException {
        return get(url, dataFactory, DEFAULT_RDF_CONTENT_TYPE);
    }

    protected abstract DataInstanceList getDataInstances() throws SaiNotFoundException, SaiException;

    protected abstract DataInstance newDataInstance(DataInstance parent) throws SaiException;

    // Static helper called from grant specific new data instance instantiations
    public static DataInstance newDataInstance(ReadableDataGrant dataGrant, DataInstance parent) throws SaiException {
        // Get a URL for the data instance to add (built from the data registration)
        URL instanceUrl = addChildToUrlPath(dataGrant.dataRegistration, UUID.randomUUID().toString());
        DataInstance.Builder builder = new DataInstance.Builder(instanceUrl, dataGrant.dataFactory, dataGrant.contentType);
        builder.setDataGrant(dataGrant).setDraft(true);
        if (parent != null) { builder.setParent(parent); }  // if this is a child instance set the parent
        return builder.build();
    }

    /**
     * Builder for {@link ReadableDataGrant} instances.
     */
    public static class Builder {

        private final URL url;
        private final DataFactory dataFactory;
        private final ContentType contentType;
        private Model dataset;
        private Resource resource;
        private URL dataOwner;
        private URL grantee;
        private URL registeredShapeTree;
        private List<RDFNode> accessModes;
        private List<RDFNode> creatorAccessModes;
        private RDFNode scopeOfGrant;
        private URL dataRegistration;
        private List<URL> dataInstances;
        private URL accessNeed;
        private URL inheritsFrom;
        private URL delegationOf;

        /**
         * Initialize builder with <code>url</code>, <code>dataFactory</code>, and desired <code>contentType</code>
         * @param url URL of the {@link ReadableDataGrant} to build
         * @param dataFactory {@link DataFactory} to assign
         * @param contentType {@link ContentType} to assign
         */
        public Builder(URL url, DataFactory dataFactory, ContentType contentType) {
            Objects.requireNonNull(url, "Must provide a URL for the data grant builder");
            Objects.requireNonNull(dataFactory, "Must provide a data factory for the data grant builder");
            Objects.requireNonNull(contentType, "Must provide a content type for the data grant builder");
            this.url = url;
            this.dataFactory = dataFactory;
            this.contentType = contentType;
            this.accessModes = new ArrayList<>();
            this.creatorAccessModes = new ArrayList<>();
            this.dataInstances = new ArrayList<>();
        }

        /**
         * Optional Jena Model that will initialize the attributes of the Builder rather than set
         * them manually. Typically used in read scenarios when populating the Builder from
         * the contents of a remote resource.
         *
         * @param dataset Jena model to populate the Builder attributes with
         * @return {@link Builder}
         */
        public Builder setDataset(Model dataset) throws SaiException {
            Objects.requireNonNull(dataset, "Must provide a Jena model for the data grant builder");
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            populateFromDataset();
            return this;
        }

        /**
         * Populates the fields of the {@link ReadableDataGrant} based on the associated Jena resource.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException {
            try {
                this.dataOwner = getRequiredUrlObject(this.resource, DATA_OWNER);
                this.grantee = getRequiredUrlObject(this.resource, GRANTEE);
                this.registeredShapeTree = getRequiredUrlObject(this.resource, REGISTERED_SHAPE_TREE);
                this.accessModes = getRequiredObjects(this.resource, ACCESS_MODE);
                this.creatorAccessModes = getRequiredObjects(this.resource, CREATOR_ACCESS_MODE);
                this.scopeOfGrant = getRequiredObject(this.resource, SCOPE_OF_GRANT);
                this.dataRegistration = getUrlObject(this.resource, HAS_DATA_REGISTRATION);
                this.dataInstances = getUrlObjects(this.resource, HAS_DATA_INSTANCE);
                this.accessNeed = getRequiredUrlObject(this.resource, SATISFIES_ACCESS_NEED);
                this.inheritsFrom = getUrlObject(this.resource, INHERITS_FROM_GRANT);
                this.delegationOf = getUrlObject(this.resource, DELEGATION_OF_GRANT);
            } catch (SaiNotFoundException ex) {
                throw new SaiException("Unable to populate immutable data grant. Missing required fields: " + ex.getMessage());
            }
        }

        /**
         * Build the {@link ReadableDataGrant} using attributes from the Builder, populated by {@link #populateFromDataset()}
         * @return {@link ReadableDataGrant}
         * @throws SaiException
         */
        public ReadableDataGrant build() throws SaiException {
            if (this.scopeOfGrant.equals(SCOPE_ALL_FROM_REGISTRY)) {
                return new AllFromRegistryDataGrant(this.url, this.dataFactory, this.dataset, this.resource, this.contentType, this.dataOwner,
                        this.grantee, this.registeredShapeTree, this.accessModes, this.creatorAccessModes,
                        this.dataRegistration, this.accessNeed, this.delegationOf);
            } else if (this.scopeOfGrant.equals(SCOPE_SELECTED_FROM_REGISTRY)) {
                return new SelectedFromRegistryDataGrant(this.url, this.dataFactory, this.dataset, this.resource, this.contentType, this.dataOwner,
                        this.grantee, this.registeredShapeTree, this.accessModes, this.creatorAccessModes, this.dataRegistration,
                        this.dataInstances, this.accessNeed, this.delegationOf);
            } else if (this.scopeOfGrant.equals(SCOPE_INHERITED)) {
                return new InheritedDataGrant(this.url, this.dataFactory, this.dataset, this.resource, this.contentType, this.dataOwner,
                        this.grantee, this.registeredShapeTree, this.accessModes, this.creatorAccessModes,
                        this.dataRegistration, this.accessNeed, this.inheritsFrom, this.delegationOf);
            }
            throw new SaiException("Invalid scope for readable data grant: " + this.scopeOfGrant);
        }

    }

}

package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.data.DataInstance;
import com.janeirodigital.sai.core.data.DataInstanceList;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.sai.rdfutils.SaiRdfNotFoundException;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static com.janeirodigital.sai.core.vocabularies.AclVocabulary.ACL_CREATE;
import static com.janeirodigital.sai.core.vocabularies.AclVocabulary.ACL_WRITE;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.httputils.HttpUtils.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.rdfutils.RdfUtils.*;

/**
 * Readable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-grant">Data Grant</a>
 */
@Getter
public abstract class ReadableDataGrant extends ReadableResource {

    private final URI dataOwner;
    private final URI grantee;
    private final URI registeredShapeTree;
    private final List<RDFNode> accessModes;
    private final List<RDFNode> creatorAccessModes;
    private final RDFNode scopeOfGrant;
    private final URI dataRegistration;
    private final URI accessNeed;
    private final URI delegationOf;

    /**
     * Construct a {@link ReadableDataGrant} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    protected ReadableDataGrant(Builder builder) throws SaiException {
        super(builder);
        this.dataOwner = builder.dataOwner;
        this.grantee = builder.grantee;
        this.registeredShapeTree = builder.registeredShapeTree;
        this.accessModes = builder.accessModes;
        this.creatorAccessModes = builder.creatorAccessModes;
        this.scopeOfGrant = builder.scopeOfGrant;
        this.dataRegistration = builder.dataRegistration;
        this.accessNeed = builder.accessNeed;
        this.delegationOf = builder.delegationOf;
    }

    /**
     * Get a {@link ReadableDataGrant} at the provided <code>uri</code>
     * @param uri URI of the {@link ReadableDataGrant} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link ReadableDataGrant}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static ReadableDataGrant get(URI uri, SaiSession saiSession, ContentType contentType) throws SaiException, SaiHttpNotFoundException {
        ReadableDataGrant.Builder builder = new ReadableDataGrant.Builder(uri, saiSession);
        try (Response response = read(uri, saiSession, contentType, false)) {
            return builder.setDataset(response).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URI, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param uri URI of the {@link ReadableDataGrant} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link ReadableDataGrant}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public static ReadableDataGrant get(URI uri, SaiSession saiSession) throws SaiHttpNotFoundException, SaiException {
        return get(uri, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link ReadableDataGrant} using the attributes of the current instance
     * @return Reloaded {@link ReadableDataGrant}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public ReadableDataGrant reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.uri, this.saiSession, this.contentType);
    }

    /**
     * Indicates whether the {@link ReadableDataGrant} is a delegated data grant
     * @return true when the data grant is delegated
     */
    public boolean isDelegated() {
        return this.delegationOf != null;
    }

    /**
     * Denotes whether the grantee can create new resources based on the assigned permission modes
     * @return true when grantee can create
     */
    public boolean canCreate() {
        return (this.accessModes.contains(ACL_CREATE) || this.accessModes.contains(ACL_WRITE));
    }

    /**
     * Abstract method implemented by specific types of data grants, that allow the {@link DataInstance}s
     * permitted by that grant to be iterated.
     * @return Map of DataInstance URI, Parent DataInstance URI
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public abstract DataInstanceList getDataInstances() throws SaiHttpNotFoundException, SaiException;

    /**
     * Builder for {@link ReadableDataGrant} instances.
     */
    public static class Builder extends ReadableResource.Builder<Builder> {

        protected URI dataOwner;
        protected URI grantee;
        protected URI registeredShapeTree;
        protected List<RDFNode> accessModes;
        protected List<RDFNode> creatorAccessModes;
        protected RDFNode scopeOfGrant;
        protected URI dataRegistration;
        protected List<URI> dataInstances;
        protected URI accessNeed;
        protected URI inheritsFrom;
        protected URI delegationOf;

        /**
         * Initialize builder with <code>uri</code> and <code>saiSession</code>
         * @param uri URI of the {@link ReadableDataGrant} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URI uri, SaiSession saiSession) {
            super(uri, saiSession);
            this.accessModes = new ArrayList<>();
            this.creatorAccessModes = new ArrayList<>();
            this.dataInstances = new ArrayList<>();
        }

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
         * Populates the fields of the {@link ReadableDataGrant} based on the associated Jena resource.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException {
            try {
                this.dataOwner = getRequiredUriObject(this.resource, DATA_OWNER);
                this.grantee = getRequiredUriObject(this.resource, GRANTEE);
                this.registeredShapeTree = getRequiredUriObject(this.resource, REGISTERED_SHAPE_TREE);
                this.accessModes = getRequiredObjects(this.resource, ACCESS_MODE);
                this.creatorAccessModes = getObjects(this.resource, CREATOR_ACCESS_MODE);
                this.scopeOfGrant = getRequiredObject(this.resource, SCOPE_OF_GRANT);
                this.dataRegistration = getUriObject(this.resource, HAS_DATA_REGISTRATION);
                this.dataInstances = getUriObjects(this.resource, HAS_DATA_INSTANCE);
                this.accessNeed = getRequiredUriObject(this.resource, SATISFIES_ACCESS_NEED);
                this.inheritsFrom = getUriObject(this.resource, INHERITS_FROM_GRANT);
                this.delegationOf = getUriObject(this.resource, DELEGATION_OF_GRANT);
            } catch (SaiRdfException | SaiRdfNotFoundException ex) {
                throw new SaiException("Unable to populate immutable data grant. Missing required fields", ex);
            }
        }

        /**
         * Build the {@link ReadableDataGrant} using attributes from the Builder. This builder returns
         * type-specific sub-classes representing the three distinct types of data grants.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#access-scopes">Data Access Scopes</a>
         * @return {@link ReadableDataGrant}
         * @throws SaiException
         */
        public ReadableDataGrant build() throws SaiException {
            if (this.scopeOfGrant.equals(SCOPE_ALL_FROM_REGISTRY)) {
                return new AllFromRegistryDataGrant(this);
            } else if (this.scopeOfGrant.equals(SCOPE_SELECTED_FROM_REGISTRY)) {
                return new SelectedFromRegistryDataGrant(this);
            } else if (this.scopeOfGrant.equals(SCOPE_INHERITED)) {
                return new InheritedDataGrant(this);
            }
            throw new SaiException("Invalid scope for readable data grant: " + this.scopeOfGrant);
        }

    }

}

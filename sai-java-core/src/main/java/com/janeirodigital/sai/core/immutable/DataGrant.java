package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.utils.HttpUtils.*;
import static com.janeirodigital.sai.core.utils.RdfUtils.*;
import static com.janeirodigital.sai.core.vocabularies.AclVocabulary.ACL_CREATE;
import static com.janeirodigital.sai.core.vocabularies.AclVocabulary.ACL_WRITE;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

/**
 * Immutable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-grant">Data Grant</a>
 */
@Getter
public class DataGrant extends ImmutableResource {

    private final URL dataOwner;
    private final URL grantee;
    private final URL registeredShapeTree;
    private final List<RDFNode> accessModes;
    private final List<RDFNode> creatorAccessModes;
    private final RDFNode scopeOfGrant;
    private final URL dataRegistration;
    private final List<URL> dataInstances;
    private final URL accessNeed;
    private final URL inheritsFrom;
    private final URL delegationOf;
    private final List<DataGrant> inheritingGrants;

    /**
     * Construct a {@link DataGrant} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private DataGrant(Builder builder) throws SaiException {
        super(builder);
        this.dataOwner = builder.dataOwner;
        this.grantee = builder.grantee;
        this.registeredShapeTree = builder.registeredShapeTree;
        this.accessModes = builder.accessModes;
        this.creatorAccessModes = builder.creatorAccessModes;
        this.scopeOfGrant = builder.scopeOfGrant;
        this.dataRegistration = builder.dataRegistration;
        this.dataInstances = builder.dataInstances;
        this.accessNeed = builder.accessNeed;
        this.inheritsFrom = builder.inheritsFrom;
        this.delegationOf = builder.delegationOf;
        this.inheritingGrants = new ArrayList<>();
    }

    /**
     * Get a {@link DataGrant} at the provided <code>url</code>
     * @param url URL of the {@link DataGrant} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link DataGrant}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static DataGrant get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        DataGrant.Builder builder = new DataGrant.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link DataGrant} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link DataGrant}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public static DataGrant get(URL url, SaiSession saiSession) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link DataGrant} using the attributes of the current instance
     * @return Reloaded {@link DataGrant}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public DataGrant reload() throws SaiNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    /**
     * Denotes whether the grantee can create new resources based on the assigned permission modes
     * @return true when grantee can create
     */
    protected boolean canCreate() {
        return (this.accessModes.contains(ACL_CREATE) || this.accessModes.contains(ACL_WRITE));
    }

    /**
     * Basic structural validations of the {@link DataGrant}
     * @throws SaiException
     */
    private static DataGrant validate(DataGrant dataGrant) throws SaiException {
        Objects.requireNonNull(dataGrant.scopeOfGrant, "Cannot validate an unscoped data grant");
        validateGeneral(dataGrant);
        if (dataGrant.scopeOfGrant.equals(SCOPE_ALL_FROM_REGISTRY)) { validateAllFromRegistry(dataGrant); }
        else if (dataGrant.scopeOfGrant.equals(SCOPE_SELECTED_FROM_REGISTRY)) { validateSelectedFromRegistry(dataGrant); }
        else if (dataGrant.scopeOfGrant.equals(SCOPE_INHERITED)) { validateInherited(dataGrant); }
        else if (dataGrant.scopeOfGrant.equals(SCOPE_NO_ACCESS)) { validateNoAccess(dataGrant); }
        else { throw new SaiException("Unsupported data grant scope: " + dataGrant.scopeOfGrant); }
        return dataGrant;
    }

    /**
     * Validate the data grant with criteria that isn't specific to a given scope
     * @throws SaiException
     */
    private static void validateGeneral(DataGrant dataGrant) throws SaiException {
        if (dataGrant.canCreate() && dataGrant.creatorAccessModes.isEmpty()) {
            throw new SaiException(buildInvalidMessage(dataGrant, "Must provide creator access modes when grant includes the ability to create resources"));
        }
        if (!dataGrant.scopeOfGrant.equals(SCOPE_INHERITED) && dataGrant.inheritsFrom != null) { throw new SaiException(buildInvalidMessage(dataGrant, "Cannot inherit from another data grant without a scope of interop:Inherited")); }
        if (!dataGrant.scopeOfGrant.equals(SCOPE_SELECTED_FROM_REGISTRY) && !dataGrant.dataInstances.isEmpty()) { throw new SaiException(buildInvalidMessage(dataGrant, "Cannot target specific data instances without a scope of interop:SelectedFromRegistry")); }
    }

    /**
     * Validate a data grant with scope of interop:AllFromRegistry
     */
    private static void validateAllFromRegistry(DataGrant dataGrant) { }

    /**
     * Validate a data grant with scope of interop:SelectedFromRegistry
     */
    private static void validateSelectedFromRegistry(DataGrant dataGrant) throws SaiException {
        if (dataGrant.dataInstances.isEmpty()) { throw new SaiException(buildInvalidMessage(dataGrant, "Must provide selected data instances with a scope of interop:SelectedFromRegistry")); }
    }

    /**
     * Validate a data grant with scope of interop:Inherited
     */
    private static void validateInherited(DataGrant dataGrant) throws SaiException {
        if (dataGrant.inheritsFrom == null) { throw new SaiException(buildInvalidMessage(dataGrant, "Must provide a data grant to inherit from with a scope of interop:Inherited")); }
    }

    /**
     * Validate a data grant with scope of interop:NoAccess
     */
    private static void validateNoAccess(DataGrant dataGrant) { }

    /**
     * Provide context for a validation failure in string form
     * @param reason reason for the validation failure
     * @return Stringified failure message
     */
    private static String buildInvalidMessage(DataGrant dataGrant, String reason) {
        StringBuilder message = new StringBuilder();
        message.append("Invalid data grant " + dataGrant.url);
        message.append(" - Scope: " + dataGrant.scopeOfGrant);
        message.append(" - Shape Tree: " + dataGrant.registeredShapeTree);
        message.append(" - Grantee: " + dataGrant.grantee);
        message.append(" - Reason: " + reason);
        return message.toString();
    }

    /**
     * Builder for {@link DataGrant} instances.
     */
    public static class Builder extends ImmutableResource.Builder<Builder> {

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
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link DataGrant} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URL url, SaiSession saiSession) {
            super(url, saiSession);
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
         * Set the URL of the data owner
         * @param dataOwner URL of the data owner to set
         * @return {@link Builder}
         */
        public Builder setDataOwner(URL dataOwner) {
            Objects.requireNonNull(dataOwner, "Must provide a URL for the data owner");
            this.dataOwner = dataOwner;
            return this;
        }

        /**
         * Set the URL of the grantee
         * @param grantee URL of the grantee to set
         * @return {@link Builder}
         */
        public Builder setGrantee(URL grantee) {
            Objects.requireNonNull(grantee, "Must provide a URL for the grantee of the data grant");
            this.grantee = grantee;
            return this;
        }

        /**
         * Set the URL of the registered shape tree
         * @param registeredShapeTree URL of the registered shape tree to set
         * @return {@link Builder}
         */
        public Builder setRegisteredShapeTree(URL registeredShapeTree) {
            Objects.requireNonNull(registeredShapeTree, "Must provide a URL for the registered shape tree of the data grant");
            this.registeredShapeTree = registeredShapeTree;
            return this;
        }

        /**
         * Set the list of assigned access modes
         * @param accessModes List of access modes to set
         * @return {@link Builder}
         */
        public Builder setAccessModes(List<RDFNode> accessModes) {
            Objects.requireNonNull(accessModes, "Must provide a list of access modes for the data grant");
            this.accessModes = accessModes;
            return this;
        }

        /**
         * Set the list of creator access modes
         * @param creatorAccessModes List of creator access modes to set
         * @return {@link Builder}
         */
        public Builder setCreatorAccessModes(List<RDFNode> creatorAccessModes) {
            Objects.requireNonNull(creatorAccessModes, "Must provide a list of creator access modes for the data grant");
            this.creatorAccessModes = creatorAccessModes;
            return this;
        }

        /**
         * Set the scope of the {@link DataGrant}
         * @param scopeOfGrant scope to assign
         * @return {@link Builder}
         */
        public Builder setScopeOfGrant(RDFNode scopeOfGrant) {
            Objects.requireNonNull(scopeOfGrant, "Must provide a scope for the data grant");
            this.scopeOfGrant = scopeOfGrant;
            return this;
        }

        /**
         * Set the assigned data registration
         * @param dataRegistration data registration to assign
         * @return {@link Builder}
         */
        public Builder setDataRegistration(URL dataRegistration) {
            Objects.requireNonNull(dataRegistration, "Must provide a URL for the data registration associated with the data grant");
            this.dataRegistration = dataRegistration;
            return this;
        }

        /**
         * Set the list of assigned data instances
         * @param dataInstances List of data instances to assign
         * @return {@link Builder}
         */
        public Builder setDataInstances(List<URL> dataInstances) {
            Objects.requireNonNull(dataInstances, "Must provide a URL for the data instances associated with the data grant");
            this.dataInstances = dataInstances;
            return this;
        }

        /**
         * Set the assigned access need
         * @param accessNeed access need to assign
         * @return {@link Builder}
         */
        public Builder setAccessNeed(URL accessNeed) {
            Objects.requireNonNull(accessNeed, "Must provide a URL for the access need associated with the data grant");
            this.accessNeed = accessNeed;
            return this;
        }

        /**
         * Set the {@link DataGrant} that is being inherited from
         * @param inheritsFrom URL of the data grant to inherit from
         * @return {@link Builder}
         */
        public Builder setInheritsFrom(URL inheritsFrom) {
            Objects.requireNonNull(inheritsFrom, "Must provide a URL for the data grant being inherited from");
            this.inheritsFrom = inheritsFrom;
            return this;
        }

        /**
         * Set the {@link DataGrant} that is being delegated
         * @param delegationOf URL of the data grant to delegate
         * @return {@link Builder}
         */
        public Builder setDelegationOf(URL delegationOf) {
            Objects.requireNonNull(delegationOf, "Must provide a URL for the data grant being delegated");
            this.delegationOf = delegationOf;
            return this;
        }

        /**
         * Populates the fields of the {@link DataGrant} based on the associated Jena resource.
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
                this.dataRegistration = getRequiredUrlObject(this.resource, HAS_DATA_REGISTRATION);
                this.dataInstances = getUrlObjects(this.resource, HAS_DATA_INSTANCE);
                this.accessNeed = getRequiredUrlObject(this.resource, SATISFIES_ACCESS_NEED);
                this.inheritsFrom = getUrlObject(this.resource, INHERITS_FROM_GRANT);
                this.delegationOf = getUrlObject(this.resource, DELEGATION_OF_GRANT);
            } catch (SaiNotFoundException ex) {
                throw new SaiException("Unable to populate immutable data grant. Missing required fields: " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         * @throws SaiException
         */
        private void populateDataset() throws SaiException {

            if (this.delegationOf == null) { this.resource = getNewResourceForType(this.url, DATA_GRANT); } else {
                this.resource = getNewResourceForType(this.url, DELEGATED_DATA_GRANT);
            }
            this.dataset = this.resource.getModel();
            updateObject(this.resource, DATA_OWNER, this.dataOwner);
            updateObject(this.resource, GRANTEE, this.grantee);
            updateObject(this.resource, REGISTERED_SHAPE_TREE, this.registeredShapeTree);
            updateObject(this.resource, SCOPE_OF_GRANT, this.scopeOfGrant);
            updateObject(this.resource, SATISFIES_ACCESS_NEED, this.accessNeed);
            updateObject(this.resource, HAS_DATA_REGISTRATION, this.dataRegistration);

            if (!this.dataInstances.isEmpty()) { updateUrlObjects(this.resource, HAS_DATA_INSTANCE, this.dataInstances); }
            if (this.inheritsFrom != null) { updateObject(this.resource, INHERITS_FROM_GRANT, this.inheritsFrom); }
            if (this.delegationOf != null) { updateObject(this.resource, DELEGATION_OF_GRANT, this.delegationOf); }

            final List<URL> accessModeUrls = new ArrayList<>();
            for(RDFNode mode : this.accessModes) { accessModeUrls.add(stringToUrl(mode.asResource().getURI())); }
            updateUrlObjects(this.resource, ACCESS_MODE, accessModeUrls);

            if (!this.creatorAccessModes.isEmpty()) {
                final List<URL> creatorAccessModeUrls = new ArrayList<>();
                for(RDFNode creatorMode : this.creatorAccessModes) { creatorAccessModeUrls.add(stringToUrl(creatorMode.asResource().getURI())); }
                updateUrlObjects(this.resource, CREATOR_ACCESS_MODE, creatorAccessModeUrls);
            }
        }

        /**
         * Build the {@link DataGrant} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}. Conversely, if a dataset was provided, the attributes of the
         * Builder will be populated from it.
         * @return {@link DataGrant}
         * @throws SaiException
         */
        public DataGrant build() throws SaiException {
            Objects.requireNonNull(dataOwner, "Must provide a URL for the data owner");
            Objects.requireNonNull(grantee, "Must provide a URL for the grantee of the data grant");
            Objects.requireNonNull(registeredShapeTree, "Must provide a URL for the registered shape tree of the data grant");
            Objects.requireNonNull(accessModes, "Must provide a list of access modes for the data grant");
            Objects.requireNonNull(scopeOfGrant, "Must provide a scope for the data grant");
            Objects.requireNonNull(dataRegistration, "Must provide a URL for the data registration associated with the data grant");
            Objects.requireNonNull(accessNeed, "Must provide a URL for the access need associated with the data grant");
            if (this.dataset == null) { populateDataset(); }
            return validate(new DataGrant(this));
        }
    }

}

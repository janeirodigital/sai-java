package com.janeirodigital.sai.core.immutable;

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

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.AclVocabulary.ACL_CREATE;
import static com.janeirodigital.sai.core.vocabularies.AclVocabulary.ACL_WRITE;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

/**
 * Immutable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-consent">Data Consent</a>
 */
@Getter
public class DataConsent extends ImmutableResource {

    private final URL dataOwner;
    private final URL grantedBy;
    private final URL grantee;
    private final URL registeredShapeTree;
    private final List<RDFNode> accessModes;
    private final List<RDFNode> creatorAccessModes;
    private final RDFNode scopeOfConsent;
    private final URL dataRegistration;
    private final List<URL> dataInstances;
    private final URL accessNeed;
    private final URL inheritsFrom;

    /**
     * Construct a new {@link DataConsent}
     * @param url URL of the {@link DataConsent}
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    private DataConsent(URL url, DataFactory dataFactory, Model dataset, Resource resource, ContentType contentType,
                        URL dataOwner, URL grantedBy, URL grantee, URL registeredShapeTree, List<RDFNode> accessModes,
                        List<RDFNode> creatorAccessModes, RDFNode scopeOfConsent, URL dataRegistration,
                        List<URL> dataInstances, URL accessNeed, URL inheritsFrom) throws SaiException {
        super(url, dataFactory, false);
        this.dataset = dataset;
        this.resource = resource;
        this.contentType = contentType;
        this.dataOwner = dataOwner;
        this.grantedBy = grantedBy;
        this.grantee = grantee;
        this.registeredShapeTree = registeredShapeTree;
        this.accessModes = accessModes;
        this.creatorAccessModes = creatorAccessModes;
        this.scopeOfConsent = scopeOfConsent;
        this.dataRegistration = dataRegistration;
        this.dataInstances = dataInstances;
        this.accessNeed = accessNeed;
        this.inheritsFrom = inheritsFrom;
    }

    /**
     * Get an {@link DataConsent} at the provided <code>url</code>
     * @param url URL of the {@link DataConsent} to get
     * @param dataFactory {@link DataFactory} to assign
     * @return Retrieved {@link DataConsent}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static DataConsent get(URL url, DataFactory dataFactory, ContentType contentType) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(url, "Must provide the URL of the data consent to get");
        Objects.requireNonNull(dataFactory, "Must provide a data factory to assign to the data consent");
        Objects.requireNonNull(contentType, "Must provide a content type for the data consent");
        Builder builder = new Builder(url, dataFactory, contentType);
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(dataFactory.getAuthorizedSession(), dataFactory.getHttpClient(), url, headers))) {
            builder.setDataset(getRdfModelFromResponse(response));
        }
        DataConsent dataConsent = builder.build();
        dataConsent.validate();
        return dataConsent;
    }

    /**
     * Call {@link #get(URL, DataFactory, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link DataConsent} to get
     * @param dataFactory {@link DataFactory} to assign
     * @return Retrieved {@link DataConsent}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public static DataConsent get(URL url, DataFactory dataFactory) throws SaiNotFoundException, SaiException {
        return get(url, dataFactory, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Generate one or more {@link DataGrant}s for this {@link DataConsent}.
     * @return List of generated {@link DataGrant}s
     */
    public List<DataGrant> generateGrants() throws SaiException {
        if (this.scopeOfConsent.equals(SCOPE_INHERITED)) { throw new SaiException("A data consent with an inherited scope cannot generate data grants"); }
        List<DataGrant> dataGrants = new ArrayList<>();
        if (this.dataOwner == null || this.dataOwner.equals(this.grantedBy)) {
            // if the consent was granted by the data owner it is a source grant || if there's no owner (e.g. with All)
            return generateSourceGrants();
        }

        if (this.dataOwner == null || !this.dataOwner.equals(this.grantedBy)) {
            // if the consent wasn't granted by the data owner it is a delegated grant || if there's no owner (e.g. with All)
            return generateDelegatedGrants();
        }
        return dataGrants;
    }

    private List<DataGrant> generateSourceGrants() {
        // get data registrations from all registries that match the registered shape tree
        // filter down to a specifically matched data registration if hasDataRegistration was set
        // for each matching registration
            // create children if needed (generate child source grants)
            // let scope of grant = AllFromRegistry (default value)
            // if scope of consent is selected then set scope of grant to selected
            // start building the data grant
                // dataowner = grantedBy
                // registeredShapeTree
                // hasDataRegistration = current matching registration
                // scope of grant
                // access modes
                // creator access modes
            // if any data instances were set with hasDataInstance set those on the data grant
            // if any child data grants were generated add those to hasInheritingGrant list (need to add this to data grant)
            // finish building the data grant with builder and add to list
        // return list of data grants
        return null;
    }

    private List<DataGrant> generateChildSourceGrants(DataGrant parentGrant) {
        // generate the child (inheriting) grants for a given parent source grant
        // for each child data consent that inherits from the current one (e.g. specifies it with inheritsFrom)
            // generate an iri for the data grant in the agent registration
            // find the data registration for the child data consent (must be same registry as parent)
            // build the child registration with data grant builder
                // data owner = granted by
                // registered shape tree
                // hasDataRegistration = matched registration from above
                // scope must be inherited
                // access mode / creator access mode
                // inheritsFromGrant (parent grant iri)
                // add to list
        // return the list
        return null;
    }

    private List<DataGrant> generateDelegatedGrants() {
        // for each social agent registration in the agent registry
            // continue if the data owner is set but the agent registration is not theirs (registeredAgent) ... if data owner is not set, that could mean we have scope of all, and we're sharing all data shared from everyone
            // continue if the grantee of the data consent is the registered agent of the agent registration (don't delegate to themselves)
            // set access grant iri to hasAccessGrant of the reciprocal registration
            // continue if there's no access grant iri in the reciprocal (which would mean they haven't shared anything so there's nothing to delegate)
            // in the remote access grant, find data grants matching the the registered shape tree of the data consent
            // filter to a given data registration if specified
            // for each matching data grant from the list above
                // get the iri for the delegated data grant
                // generate child delegated data grants if necessary
                // build the delegated data grant
                    // dataowner = source grant data owner
                    // registered shape tree = source grant shape tree
                    // data registration = source grant data registration
                    // scope of grant = source grant scope
                    // delegationOfGrant = source grant iri
                    // access mode = filter granted modes by source grant modes (subset)
                // if child grants were generated - add to hasInheritingGrants
                // build the delegated grant
                // add to the list
        // return the created list
        return null;
    }

    private List<DataGrant> generateChildDelegatedGrants() {
        // for each inheriting consent of the data consent
            // make the iri for the child grant
            // find the child source grant - look at the parent source grant and find the child grant with same shape tree
            // build the child source grant
                // data owner = child source grant data owner
                // registered shape tree = child consent registered shape tree
                // data registration = child source grant data registration
                // scope of grant = inherited
                // access mode = filter the subset of modes
                // inherits from grant = parent grant iri
                // delegation of grant = child source grant iri
            // build and add to the list
        // return the list
        return null;
    }

    /**
     * Denotes whether the grantee can create new resources based on the assigned permission modes
     * @return true when grantee can create
     */
    protected boolean canCreate() {
        return (this.accessModes.contains(ACL_CREATE) || this.accessModes.contains(ACL_WRITE));
    }

    /**
     * Basic structural validations of the {@link DataConsent}
     * @throws SaiException
     */
    private void validate() throws SaiException {
        Objects.requireNonNull(this.scopeOfConsent, "Cannot validate an unscoped data consent");
        validateGeneral();
        if (this.scopeOfConsent.equals(SCOPE_ALL)) { validateAll(); }
        else if (this.scopeOfConsent.equals(SCOPE_ALL_FROM_REGISTRY)) { validateAllFromRegistry(); }
        else if (this.scopeOfConsent.equals(SCOPE_ALL_FROM_AGENT)) { validateAllFromAgent(); }
        else if (this.scopeOfConsent.equals(SCOPE_SELECTED_FROM_REGISTRY)) { validateSelectedFromRegistry(); }
        else if (this.scopeOfConsent.equals(SCOPE_INHERITED)) { validateInherited(); }
        else if (this.scopeOfConsent.equals(SCOPE_NO_ACCESS)) { validateNoAccess(); }
        else { throw new SaiException("Unsupported data consent scope: " + this.scopeOfConsent); }
    }

    /**
     * Validate the data consent with criteria that isn't specific to a given scope
     * @throws SaiException
     */
    private void validateGeneral() throws SaiException {
        if (this.canCreate() && this.creatorAccessModes == null) {
            throw new SaiException(buildInvalidMessage("Must provide creator access modes when consent includes the ability to create resources"));
        }
        if (!this.scopeOfConsent.equals(SCOPE_INHERITED) && this.inheritsFrom != null) { throw new SaiException(buildInvalidMessage("Cannot inherit from another data consent without a scope of interop:Inherited")); }
        if (!this.scopeOfConsent.equals(SCOPE_SELECTED_FROM_REGISTRY) && !this.dataInstances.isEmpty()) { throw new SaiException(buildInvalidMessage("Cannot target specific data instances without a scope of interop:SelectedFromRegistry")); }
    }

    /**
     * Validate a data consent with scope of interop:All
     */
    private void validateAll() throws SaiException {
        if (this.dataRegistration != null) { throw new SaiException(buildInvalidMessage("Cannot target a specific data registration with scope of interop:All")); }
    }

    /**
     * Validate a data consent with scope of interop:AllFromRegistry
     */
    private void validateAllFromRegistry() throws SaiException {
        if (this.dataRegistration == null) { throw new SaiException(buildInvalidMessage("Must provide a specific data registration with a scope of interop:AllFromRegistry")); }
    }

    /**
     * Validate a data consent with scope of interop:AllFromAgent
     */
    private void validateAllFromAgent() throws SaiException {
        if (this.dataRegistration != null) { throw new SaiException(buildInvalidMessage("Cannot target a specific data registration with scope of interop:AllFromAgent")); }
    }

    /**
     * Validate a data consent with scope of interop:SelectedFromRegistry
     */
    private void validateSelectedFromRegistry() throws SaiException {
        if (this.dataRegistration == null) { throw new SaiException(buildInvalidMessage("Must provide a specific data registration with a scope of interop:SelectedFromRegistry")); }
    }

    /**
     * Validate a data consent with scope of interop:Inherited
     */
    private void validateInherited() throws SaiException {
        if (this.inheritsFrom == null) { throw new SaiException(buildInvalidMessage("Must provide a data consent to inherit from with a scope of interop:Inherited")); }
    }

    /**
     * Validate a data consent with scope of interop:NoAccess
     */
    private void validateNoAccess() { }

    /**
     * Provide context for a validation failure in string form
     * @param reason reason for the validation failure
     * @return Stringified failure message
     */
    private String buildInvalidMessage(String reason) {
        StringBuilder message = new StringBuilder();
        message.append("Invalid data consent " + this.url);
        message.append(" - Scope: " + this.scopeOfConsent);
        message.append(" - Shape Tree: " + this.registeredShapeTree);
        message.append(" - Grantee: " + this.grantee);
        message.append(" - Reason: " + reason);
        return message.toString();
    }

    /**
     * Builder for {@link DataConsent} instances.
     */
    public static class Builder {

        private final URL url;
        private final DataFactory dataFactory;
        private final ContentType contentType;
        private Model dataset;
        private Resource resource;
        private URL dataOwner;
        private URL grantedBy;
        private URL grantee;
        private URL registeredShapeTree;
        private List<RDFNode> accessModes;
        private List<RDFNode> creatorAccessModes;
        private RDFNode scopeOfConsent;
        private URL dataRegistration;
        private List<URL> dataInstances;
        private URL accessNeed;
        private URL inheritsFrom;

        /**
         * Initialize builder with <code>url</code>, <code>dataFactory</code>, and desired <code>contentType</code>
         * @param url URL of the {@link DataGrant} to build
         * @param dataFactory {@link DataFactory} to assign
         * @param contentType {@link ContentType} to assign
         */
        public Builder(URL url, DataFactory dataFactory, ContentType contentType) {
            Objects.requireNonNull(url, "Must provide a URL for the data consent builder");
            Objects.requireNonNull(dataFactory, "Must provide a data factory for the data consent builder");
            Objects.requireNonNull(contentType, "Must provide a content type for the data consent builder");
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
         * @param dataset Jena model to populate the Builder attributes with
         * @return {@link Builder}
         */
        public Builder setDataset(Model dataset) throws SaiException {
            Objects.requireNonNull(dataset, "Must provide a Jena model for the data consent builder");
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            populateFromDataset();
            return this;
        }

        public Builder setDataOwner(URL dataOwner) {
            Objects.requireNonNull(dataOwner, "Must provide a URL for the data owner");
            this.dataOwner = dataOwner;
            return this;
        }

        public Builder setGrantedBy(URL grantedBy) {
            Objects.requireNonNull(grantedBy, "Must provide a URL for the social agent that granted the consent");
            this.grantedBy = grantedBy;
            return this;
        }

        public Builder setGrantee(URL grantee) {
            Objects.requireNonNull(grantee, "Must provide a URL for the grantee of the data consent");
            this.grantee = grantee;
            return this;
        }

        public Builder setRegisteredShapeTree(URL registeredShapeTree) {
            Objects.requireNonNull(registeredShapeTree, "Must provide a URL for the registered shape tree of the data consent");
            this.registeredShapeTree = registeredShapeTree;
            return this;
        }
        
        public Builder setAccessModes(List<RDFNode> accessModes) {
            Objects.requireNonNull(accessModes, "Must provide a list of access modes for the data consent");
            this.accessModes = accessModes;
            return this;
        }

        public Builder setCreatorAccessModes(List<RDFNode> creatorAccessModes) {
            Objects.requireNonNull(creatorAccessModes, "Must provide a list of creator access modes for the data consent");
            this.creatorAccessModes = creatorAccessModes;
            return this;
        }

        public Builder setScopeOfConsent(RDFNode scopeOfConsent) {
            Objects.requireNonNull(scopeOfConsent, "Must provide a scope of consent for the data consent");
            this.scopeOfConsent = scopeOfConsent;
            return this;
        }

        public Builder setDataRegistration(URL dataRegistration) {
            Objects.requireNonNull(dataRegistration, "Must provide a URL for the data registration associated with the data consent");
            this.dataRegistration = dataRegistration;
            return this;
        }

        public Builder setDataInstances(List<URL> dataInstances) {
            Objects.requireNonNull(dataInstances, "Must provide a URL for the data instances associated with the data consent");
            this.dataInstances = dataInstances;
            return this;
        }

        public Builder setAccessNeed(URL accessNeed) {
            Objects.requireNonNull(accessNeed, "Must provide a URL for the access need associated with the data consent");
            this.accessNeed = accessNeed;
            return this;
        }

        public Builder setInheritsFrom(URL inheritsFrom) {
            Objects.requireNonNull(inheritsFrom, "Must provide a URL for the data consent being inherited from");
            this.inheritsFrom = inheritsFrom;
            return this;
        }

        private void populateFromDataset() throws SaiException {
            try {
                this.dataOwner = getRequiredUrlObject(this.resource, DATA_OWNER);
                this.grantedBy = getRequiredUrlObject(this.resource, GRANTED_BY);
                this.grantee = getRequiredUrlObject(this.resource, GRANTEE);
                this.registeredShapeTree = getRequiredUrlObject(this.resource, REGISTERED_SHAPE_TREE);
                this.accessModes = getRequiredObjects(this.resource, ACCESS_MODE);
                this.creatorAccessModes = getRequiredObjects(this.resource, CREATOR_ACCESS_MODE);
                this.scopeOfConsent = getRequiredObject(this.resource, SCOPE_OF_CONSENT);
                this.dataRegistration = getUrlObject(this.resource, HAS_DATA_REGISTRATION);
                this.dataInstances = getUrlObjects(this.resource, HAS_DATA_INSTANCE);
                this.accessNeed = getRequiredUrlObject(this.resource, SATISFIES_ACCESS_NEED);
                this.inheritsFrom = getUrlObject(this.resource, INHERITS_FROM_CONSENT);
            } catch (SaiNotFoundException ex) {
                throw new SaiException("Unable to populate immutable data consent. Missing required fields: " + ex.getMessage());
            }
        }

        private void populateDataset() throws SaiException {
            this.resource = getNewResourceForType(this.url, DATA_CONSENT);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, DATA_OWNER, this.dataOwner);
            updateObject(this.resource, GRANTED_BY, this.grantedBy);
            updateObject(this.resource, GRANTEE, this.grantee);
            updateObject(this.resource, REGISTERED_SHAPE_TREE, this.registeredShapeTree);
            updateObject(this.resource, SCOPE_OF_CONSENT, this.scopeOfConsent);
            updateObject(this.resource, SATISFIES_ACCESS_NEED, this.accessNeed);

            if (this.dataRegistration != null) { updateObject(this.resource, HAS_DATA_REGISTRATION, this.dataRegistration); }
            if (!this.dataInstances.isEmpty()) { updateUrlObjects(this.resource, HAS_DATA_INSTANCE, this.dataInstances); }
            if (this.inheritsFrom != null) { updateObject(this.resource, INHERITS_FROM_CONSENT, this.inheritsFrom); }

            final List<URL> accessModeUrls = new ArrayList<>();
            for(RDFNode mode : this.accessModes) { accessModeUrls.add(stringToUrl(mode.asResource().getURI())); }
            updateUrlObjects(this.resource, ACCESS_MODE, accessModeUrls);

            if (!this.creatorAccessModes.isEmpty()) {
                final List<URL> creatorAccessModeUrls = new ArrayList<>();
                for(RDFNode creatorMode : this.creatorAccessModes) { creatorAccessModeUrls.add(stringToUrl(creatorMode.asResource().getURI())); }
                updateUrlObjects(this.resource, ACCESS_MODE, creatorAccessModeUrls);
            }
        }

        public DataConsent build() throws SaiException {
            Objects.requireNonNull(this.dataOwner, "Must provide a URL for the data owner");
            Objects.requireNonNull(this.grantedBy, "Must provide a URL for the grantee of the data consent");
            Objects.requireNonNull(this.grantee, "Must provide a URL for the grantee of the data consent");
            Objects.requireNonNull(this.registeredShapeTree, "Must provide a URL for the registered shape tree of the data consent");
            Objects.requireNonNull(this.accessModes, "Must provide a list of access modes for the data consent");
            Objects.requireNonNull(this.scopeOfConsent, "Must provide a scope of consent for the data consent");
            Objects.requireNonNull(this.accessNeed, "Must provide a URL for the access need associated with the data consent");
            if (this.dataset == null) { populateDataset(); }
            return new DataConsent(this.url, this.dataFactory, this.dataset, this.resource, this.contentType, this.dataOwner,
                                   this.grantee, this.grantedBy, this.registeredShapeTree, this.accessModes, this.creatorAccessModes,
                                   this.scopeOfConsent, this.dataRegistration, this.dataInstances, this.accessNeed,
                                   this.inheritsFrom);
        }

    }

}

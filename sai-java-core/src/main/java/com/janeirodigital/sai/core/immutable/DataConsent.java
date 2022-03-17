package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.crud.*;
import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.readable.InheritableDataGrant;
import com.janeirodigital.sai.core.readable.ReadableAccessGrant;
import com.janeirodigital.sai.core.readable.ReadableDataGrant;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;

import java.net.URL;
import java.util.*;

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
    private final List<DataConsent> inheritingConsents;

    /**
     * Construct a {@link DataConsent} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private DataConsent(Builder builder) throws SaiException {
        super(builder);
        this.dataOwner = builder.dataOwner;
        this.grantedBy = builder.grantedBy;
        this.grantee = builder.grantee;
        this.registeredShapeTree = builder.registeredShapeTree;
        this.accessModes = builder.accessModes;
        this.creatorAccessModes = builder.creatorAccessModes;
        this.scopeOfConsent = builder.scopeOfConsent;
        this.dataRegistration = builder.dataRegistration;
        this.dataInstances = builder.dataInstances;
        this.accessNeed = builder.accessNeed;
        this.inheritsFrom = builder.inheritsFrom;
        this.inheritingConsents = new ArrayList<>();
    }

    /**
     * Get a {@link DataConsent} at the provided <code>url</code>
     * @param url URL of the {@link DataConsent} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link DataConsent}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static DataConsent get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        DataConsent.Builder builder = new DataConsent.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link DataConsent} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link DataConsent}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public static DataConsent get(URL url, SaiSession saiSession) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link DataConsent} using the attributes of the current instance
     * @return Reloaded {@link DataConsent}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public DataConsent reload() throws SaiNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }
    
    /**
     * Generate one or more {@link DataGrant}s for this {@link DataConsent}.
     * @return List of generated {@link DataGrant}s
     */
    public List<DataGrant> generateGrants(AccessConsent accessConsent, AgentRegistration granteeRegistration,
                                          AgentRegistry agentRegistry, List<DataRegistry> dataRegistries) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(granteeRegistration, "Must provide a grantee agent registration to generate data grants");
        Objects.requireNonNull(agentRegistry, "Must provide an agent registry to generate data grants");
        Objects.requireNonNull(dataRegistries, "Must provide data registries to generate data grants");
        if (this.getScopeOfConsent().equals(SCOPE_INHERITED)) { throw new SaiException("A data consent with an inherited scope cannot generate data grants"); }
        List<DataGrant> dataGrants = new ArrayList<>();
        if (this.getDataOwner() == null || this.getDataOwner().equals(this.getGrantedBy())) {
            // Scope: All - Data owner is sharing across their data and data shared with them (dataOwner == null)
            // Scope: AllFromRegistry - Data owner sharing all data of a type from a data registry they own (dataOwner == grantedBy)
            // Scope: SelectedFromRegistry - Data owner sharing data instances of a type from a data registry they own (dataOwner == grantedBy)
            dataGrants.addAll(generateSourceGrants(accessConsent, granteeRegistration, agentRegistry, dataRegistries));
        }

        if (this.getDataOwner() == null || !this.getDataOwner().equals(this.getGrantedBy())) {
            // Scope: All - Data owner is sharing across their data and data shared with them (dataOwner == null)
            // Scope: AllFromAgent - Data owner sharing all data of a type shared with them (dataOwner != grantedBy)
            dataGrants.addAll(generateDelegatedGrants(accessConsent, granteeRegistration, agentRegistry, dataRegistries));
        }
        return dataGrants;
    }

    /**
     * Generate grants for a {@link DataConsent} where the data owner is sharing data they own directly.
     * <br>Applies to scopes: All, AllFromRegistry, SelectedFromRegistry
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#access-scopes">Data Access Scopes</a>
     * @param accessConsent {@link AccessConsent} that this {@link DataConsent} is associated with
     * @param granteeRegistration {@link AgentRegistration} of the grantee in data owner's {@link AgentRegistry}
     * @param agentRegistry {@link AgentRegistry} of the data owner
     * @param dataRegistries List of {@link DataRegistry} instances of the data owner
     * @return List of generated {@link DataGrant}s
     * @throws SaiException
     */
    private List<DataGrant> generateSourceGrants(AccessConsent accessConsent, AgentRegistration granteeRegistration,
                                                 AgentRegistry agentRegistry, List<DataRegistry> dataRegistries) throws SaiException {

        if (!this.getScopeOfConsent().equals(SCOPE_ALL) && !this.getScopeOfConsent().equals(SCOPE_ALL_FROM_REGISTRY) && !this.getScopeOfConsent().equals(SCOPE_SELECTED_FROM_REGISTRY)) {
            throw new SaiException("Cannot generate a regular (non-delegated) data grant for a data consent with scope: " + this.getScopeOfConsent());
        }
        // get data registrations from all registries that match the registered shape tree
        Map<DataRegistration, DataRegistry> dataRegistrations = new HashMap<>();
        for (DataRegistry dataRegistry : dataRegistries) {
            DataRegistration matchingRegistration = dataRegistry.getDataRegistrations().find(this.registeredShapeTree);
            if (matchingRegistration != null) { dataRegistrations.put(matchingRegistration, dataRegistry); }
        }

        if (this.getDataRegistration() != null) {
            // filter down to a specifically matched data registration if hasDataRegistration was set
            Map.Entry<DataRegistration,DataRegistry> filtered = dataRegistrations.entrySet().stream()
                                                         .filter(e -> this.getDataRegistration().equals(e.getKey().getUrl()))
                                                         .findAny().orElse(null);
            if (filtered == null) { throw new SaiException("Data registration " + this.getDataRegistration() + "not found in data registries: " + dataRegistries); }
            dataRegistrations.clear();
            dataRegistrations.put(filtered.getKey(), filtered.getValue());
        }

        List<DataGrant> dataGrants = new ArrayList<>();
        for (Map.Entry<DataRegistration, DataRegistry> entry : dataRegistrations.entrySet()) {
            DataRegistration dataRegistration = entry.getKey();
            URL dataGrantUrl = granteeRegistration.generateContainedUrl();
            DataGrant.Builder grantBuilder = new DataGrant.Builder(dataGrantUrl, this.saiSession);
            // create children if needed (generate child source grants)
            List<DataGrant> childDataGrants = generateChildSourceGrants(accessConsent, dataGrantUrl, entry.getKey(), entry.getValue(), granteeRegistration);
            // build the data grant
            grantBuilder.setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY); // default value in this context
            if (this.getScopeOfConsent().equals(SCOPE_SELECTED_FROM_REGISTRY)) { grantBuilder.setScopeOfGrant(SCOPE_SELECTED_FROM_REGISTRY); }
            grantBuilder.setDataOwner(this.grantedBy);
            grantBuilder.setGrantee(this.grantee);
            grantBuilder.setRegisteredShapeTree(this.registeredShapeTree);
            grantBuilder.setAccessModes(this.accessModes);
            grantBuilder.setAccessNeed(this.accessNeed);
            grantBuilder.setDataRegistration(dataRegistration.getUrl());
            if (!this.getCreatorAccessModes().isEmpty()) { grantBuilder.setCreatorAccessModes(this.getCreatorAccessModes()); }
            if (!this.getDataInstances().isEmpty()) { grantBuilder.setDataInstances(this.getDataInstances()); }
            // add the data grant (and child grants if they exist) to the list
            dataGrants.add(grantBuilder.build());
            if (!childDataGrants.isEmpty()) { dataGrants.addAll(childDataGrants); }
        }
        return dataGrants;
    }

    /**
     * Generate inherited "child" grants for a parent {@link DataGrant}, where the data being granted is
     * being shared by the data owner directly. Called from {@link #generateSourceGrants(AccessConsent, AgentRegistration, AgentRegistry, List)}.
     * <br>Applies to scopes: Inherited
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#access-scopes">Data Access Scopes</a>
     * @param accessConsent {@link AccessConsent} that the {@link DataConsent} is associated with
     * @param dataGrantUrl URL of the parent {@link DataGrant} being inherited from
     * @param dataRegistration {@link DataRegistration} the data resides in
     * @param dataRegistry {@link DataRegistry} the <code>dataRegistration</code> belongs to
     * @param granteeRegistration {@link AgentRegistration} of the grantee in data owner's {@link AgentRegistry}
     * @return List of generated {@link DataGrant}s
     * @throws SaiException
     */
    private List<DataGrant> generateChildSourceGrants(AccessConsent accessConsent, URL dataGrantUrl, DataRegistration dataRegistration,
                                                      DataRegistry dataRegistry, AgentRegistration granteeRegistration) throws SaiException {
        List<DataGrant> childDataGrants = new ArrayList<>();
        for (DataConsent childConsent : accessConsent.getDataConsents()) {
            // for each child data consent that inherits from the current one (e.g. specifies it with inheritsFrom)
            if (childConsent.getScopeOfConsent().equals(SCOPE_INHERITED) && childConsent.getInheritsFrom().equals(this.getUrl())) {
                URL childGrantUrl = granteeRegistration.generateContainedUrl();
                // find the data registration for the child data consent (must be same registry as parent)
                DataRegistration childRegistration = dataRegistry.getDataRegistrations().find(childConsent.registeredShapeTree);
                if (childRegistration == null) { throw new SaiException("Could not find data registration " + dataRegistration.getUrl() + " in registry " + dataRegistry.getUrl()); }
                DataGrant.Builder childBuilder = new DataGrant.Builder(childGrantUrl, this.saiSession);
                childBuilder.setDataOwner(childConsent.grantedBy);
                childBuilder.setGrantee(childConsent.grantee);
                childBuilder.setRegisteredShapeTree(childConsent.registeredShapeTree);
                childBuilder.setDataRegistration(childRegistration.getUrl());
                childBuilder.setScopeOfGrant(SCOPE_INHERITED);
                childBuilder.setAccessModes(childConsent.accessModes);
                childBuilder.setCreatorAccessModes(childConsent.creatorAccessModes);
                childBuilder.setAccessNeed(childConsent.accessNeed);
                childBuilder.setInheritsFrom(dataGrantUrl);
                childDataGrants.add(childBuilder.build());
            }
        }
        return childDataGrants;
    }

    /**
     * Generate {@link DataGrant}s that "delegate" access to data that was granted from another
     * social agent. It is delegated because the access is being shared by the grantee
     * of the original grant. A simple example of delegation is a social agent who was
     * given access to another social agent's data "delegating" access to an application
     * that they would like to use to access that data.
     * <br>Applies to scopes: All, AllFromAgent
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#delegated-data-grant">Delegated Data Grant</a>
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#access-scopes">Data Access Scopes</a>
     * @param accessConsent {@link AccessConsent} that the {@link DataConsent} is associated with
     * @param granteeRegistration {@link AgentRegistration} of the grantee receiving delegated permissions
     * @param agentRegistry {@link AgentRegistry} of the social agent delegating permission
     * @param dataRegistries {@link DataRegistry} list of the social agent delegating permission
     * @return
     */
    private List<DataGrant> generateDelegatedGrants(AccessConsent accessConsent, AgentRegistration granteeRegistration,
                                                    AgentRegistry agentRegistry, List<DataRegistry> dataRegistries) throws SaiException, SaiNotFoundException {
        if (!this.getScopeOfConsent().equals(SCOPE_ALL) && !this.getScopeOfConsent().equals(SCOPE_ALL_FROM_AGENT)) {
            throw new SaiException("Cannot generate a delegated data grant for a data consent with scope: " + this.getScopeOfConsent());
        }

        List<DataGrant> delegatedGrants = new ArrayList<>();
        for (SocialAgentRegistration agentRegistration : agentRegistry.getSocialAgentRegistrations()) {
            // continue if the grantee of the data consent is the registered agent of the agent registration (don't delegate to themselves)
            if (this.getGrantee().equals(agentRegistration.getRegisteredAgent())) { continue; }
            // Continue if the data owner is set (AllFromAgent) but the agent registration is not theirs (registeredAgent)
            if (this.getDataOwner() != null && !agentRegistration.getRegisteredAgent().equals(this.getDataOwner())) { continue; }
            // continue if there's no reciprocal registration
            if (agentRegistration.getReciprocalRegistration() == null) { continue; }
            // Lookup the remote agent registration
            SocialAgentRegistration remoteRegistration = SocialAgentRegistration.get(agentRegistration.getReciprocalRegistration(), this.saiSession);
            // continue if there's no access grant iri in the reciprocal (which would mean they haven't shared anything so there's nothing to delegate)
            if (remoteRegistration.getAccessGrantUrl() == null) { continue; }
            ReadableAccessGrant remoteGrant = ReadableAccessGrant.get(remoteRegistration.getAccessGrantUrl(), this.saiSession);
            for (ReadableDataGrant remoteDataGrant : remoteGrant.getDataGrants()) {
                // skip data grants that don't match the shape tree of this data consent
                if (!remoteDataGrant.getRegisteredShapeTree().equals(this.registeredShapeTree)) { continue; }
                // filter to a given data registration if specified
                if (this.getDataRegistration() != null) { if (!remoteDataGrant.getDataRegistration().equals(this.getDataRegistration())) { continue; } }
                // Build the delegated data grant based on this data consent and the remote data grant
                URL grantUrl = granteeRegistration.generateContainedUrl();
                DataGrant.Builder grantBuilder = new DataGrant.Builder(grantUrl, this.saiSession);
                // generate child delegated data grants if necessary
                List<DataGrant> childDataGrants = generateChildDelegatedGrants(grantUrl, remoteDataGrant, granteeRegistration);
                // build the delegated data grant
                grantBuilder.setDataOwner(remoteDataGrant.getDataOwner());
                grantBuilder.setGrantee(this.grantee);
                grantBuilder.setRegisteredShapeTree(remoteDataGrant.getRegisteredShapeTree());
                grantBuilder.setScopeOfGrant(remoteDataGrant.getScopeOfGrant());
                grantBuilder.setAccessNeed(remoteDataGrant.getAccessNeed());
                grantBuilder.setDelegationOf(remoteDataGrant.getUrl());
                if (!remoteDataGrant.getAccessModes().containsAll(this.accessModes)) { throw new SaiException("Data consent issues access modes that were not granted by remote social agent"); }
                grantBuilder.setAccessModes(this.accessModes);
                if (this.canCreate()) {
                    if (!remoteDataGrant.getCreatorAccessModes().containsAll(this.creatorAccessModes)) { throw new SaiException("Data consent issues creator access modes that were not granted by remote social agent"); }
                    grantBuilder.setCreatorAccessModes(this.creatorAccessModes);
                }
                grantBuilder.setDataRegistration(remoteDataGrant.getDataRegistration());
                delegatedGrants.add(grantBuilder.build());
                if (!childDataGrants.isEmpty()) { delegatedGrants.addAll(childDataGrants); }
            }
        }
        return delegatedGrants;
    }

    /**
     * Generate inherited "child" delegated {@link DataGrant} for a parent delegated {@link DataGrant}.
     * <br>Applies to scopes: Inherited
     * @param dataGrantUrl URL of the parent delegated {@link DataGrant}
     * @param remoteDataGrant URL of the remote {@link DataGrant} that is being delegated
     * @param granteeRegistration {@link AgentRegistration} of the grantee
     * @return List of child delegated {@link DataGrant}s
     * @throws SaiException
     */
    private List<DataGrant> generateChildDelegatedGrants(URL dataGrantUrl, ReadableDataGrant remoteDataGrant, AgentRegistration granteeRegistration) throws SaiException {
        List<DataGrant> childDataGrants = new ArrayList<>();
        for (DataConsent childConsent : this.getInheritingConsents()) {
            InheritableDataGrant remoteInheritableGrant = (InheritableDataGrant) remoteDataGrant;
            for (ReadableDataGrant remoteChildGrant: remoteInheritableGrant.getInheritingGrants()) {
                // continue if the remote inheriting grant isn't the same shape tree as the child consent
                if (!remoteChildGrant.getRegisteredShapeTree().equals(childConsent.getRegisteredShapeTree())) { continue; }
                URL childGrantUrl = granteeRegistration.generateContainedUrl();
                DataGrant.Builder childBuilder = new DataGrant.Builder(childGrantUrl, this.saiSession);
                childBuilder.setDataOwner(remoteChildGrant.getDataOwner());
                childBuilder.setGrantee(this.grantee);
                childBuilder.setRegisteredShapeTree(remoteChildGrant.getRegisteredShapeTree());
                childBuilder.setScopeOfGrant(SCOPE_INHERITED);
                childBuilder.setAccessNeed(remoteChildGrant.getAccessNeed());
                childBuilder.setDataRegistration(remoteChildGrant.getDataRegistration());
                if (!remoteChildGrant.getAccessModes().containsAll(childConsent.accessModes)) { throw new SaiException("Data consent issues access modes that were not granted by remote social agent"); }
                childBuilder.setAccessModes(childConsent.accessModes);
                if (childConsent.canCreate()) {
                    if (!remoteChildGrant.getCreatorAccessModes().containsAll(childConsent.creatorAccessModes)) { throw new SaiException("Data consent issues creator access modes that were not granted by remote social agent"); }
                    childBuilder.setCreatorAccessModes(this.creatorAccessModes);
                }
                childBuilder.setInheritsFrom(dataGrantUrl);
                childBuilder.setDelegationOf(remoteChildGrant.getUrl());
                childDataGrants.add(childBuilder.build());
            }
        }
        return childDataGrants;
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
    private static DataConsent validate(DataConsent dataConsent) throws SaiException {
        Objects.requireNonNull(dataConsent, "Must provide a data consent to validate");
        validateGeneral(dataConsent);
        if (dataConsent.scopeOfConsent.equals(SCOPE_ALL)) { validateAll(dataConsent); }
        else if (dataConsent.scopeOfConsent.equals(SCOPE_ALL_FROM_REGISTRY)) { validateAllFromRegistry(dataConsent); }
        else if (dataConsent.scopeOfConsent.equals(SCOPE_ALL_FROM_AGENT)) { validateAllFromAgent(dataConsent); }
        else if (dataConsent.scopeOfConsent.equals(SCOPE_SELECTED_FROM_REGISTRY)) { validateSelectedFromRegistry(dataConsent); }
        else if (dataConsent.scopeOfConsent.equals(SCOPE_INHERITED)) { validateInherited(dataConsent); }
        else if (dataConsent.scopeOfConsent.equals(SCOPE_NO_ACCESS)) { validateNoAccess(dataConsent); }
        else { throw new SaiException("Unsupported data consent scope: " + dataConsent.scopeOfConsent); }
        return dataConsent;
    }

    /**
     * Validate the data consent with criteria that isn't specific to a given scope
     * @throws SaiException
     */
    private static void validateGeneral(DataConsent dataConsent) throws SaiException {
        if (dataConsent.canCreate() && dataConsent.creatorAccessModes.isEmpty()) {
            throw new SaiException(buildInvalidMessage(dataConsent, "Must provide creator access modes when consent includes the ability to create resources"));
        }
        if (!dataConsent.scopeOfConsent.equals(SCOPE_INHERITED) && dataConsent.inheritsFrom != null) { throw new SaiException(buildInvalidMessage(dataConsent, "Cannot inherit from another data consent without a scope of interop:Inherited")); }
        if (!dataConsent.scopeOfConsent.equals(SCOPE_SELECTED_FROM_REGISTRY) && !dataConsent.dataInstances.isEmpty()) { throw new SaiException(buildInvalidMessage(dataConsent, "Cannot target specific data instances without a scope of interop:SelectedFromRegistry")); }
    }

    /**
     * Validate a data consent with scope of interop:All
     */
    private static void validateAll(DataConsent dataConsent) throws SaiException {
        if (dataConsent.dataOwner != null) { throw new SaiException(buildInvalidMessage(dataConsent, "Cannot provide a data owner with scope of interop:All")); }
        if (dataConsent.dataRegistration != null) { throw new SaiException(buildInvalidMessage(dataConsent, "Cannot target a specific data registration with scope of interop:All")); }
    }

    /**
     * Validate a data consent with scope of interop:AllFromRegistry
     */
    private static void validateAllFromRegistry(DataConsent dataConsent) throws SaiException {
        if (dataConsent.dataRegistration == null) { throw new SaiException(buildInvalidMessage(dataConsent, "Must provide a specific data registration with a scope of interop:AllFromRegistry")); }
    }

    /**
     * Validate a data consent with scope of interop:AllFromAgent
     */
    private static void validateAllFromAgent(DataConsent dataConsent) {
        // Placeholder for future logic to validate all from agent scope
    }

    /**
     * Validate a data consent with scope of interop:SelectedFromRegistry
     */
    private static void validateSelectedFromRegistry(DataConsent dataConsent) throws SaiException {
        if (dataConsent.dataRegistration == null) { throw new SaiException(buildInvalidMessage(dataConsent, "Must provide a specific data registration with a scope of interop:SelectedFromRegistry")); }
        if (dataConsent.getDataInstances().isEmpty()) { throw new SaiException(buildInvalidMessage(dataConsent, "Must provide specific data instances with a scope of interop:SelectedFromRegistry")); }
    }

    /**
     * Validate a data consent with scope of interop:Inherited
     */
    private static void validateInherited(DataConsent dataConsent) throws SaiException {
        if (dataConsent.inheritsFrom == null) { throw new SaiException(buildInvalidMessage(dataConsent, "Must provide a data consent to inherit from with a scope of interop:Inherited")); }
    }

    /**
     * Validate a data consent with scope of interop:NoAccess
     */
    private static void validateNoAccess(DataConsent dataConsent) { 
        // Placeholder for future logic to validate no access scope
    }

    /**
     * Provide context for a validation failure in string form
     * @param reason reason for the validation failure
     * @return Stringified failure message
     */
    private static String buildInvalidMessage(DataConsent dataConsent, String reason) {
        StringBuilder message = new StringBuilder();
        message.append("Invalid data consent " + dataConsent.url);
        message.append(" - Scope: " + dataConsent.scopeOfConsent);
        message.append(" - Shape Tree: " + dataConsent.registeredShapeTree);
        message.append(" - Grantee: " + dataConsent.grantee);
        message.append(" - Reason: " + reason);
        return message.toString();
    }

    /**
     * Builder for {@link DataConsent} instances.
     */
    public static class Builder extends ImmutableResource.Builder<Builder> {

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
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link AccessConsent} to build
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
                this.dataOwner = getUrlObject(this.resource, DATA_OWNER);
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

            updateObject(this.resource, GRANTED_BY, this.grantedBy);
            updateObject(this.resource, GRANTEE, this.grantee);
            updateObject(this.resource, REGISTERED_SHAPE_TREE, this.registeredShapeTree);
            updateObject(this.resource, SCOPE_OF_CONSENT, this.scopeOfConsent);
            updateObject(this.resource, SATISFIES_ACCESS_NEED, this.accessNeed);

            if (this.dataOwner != null) { updateObject(this.resource, DATA_OWNER, this.dataOwner); }
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
            Objects.requireNonNull(this.grantedBy, "Must provide a URL for the grantee of the data consent");
            Objects.requireNonNull(this.grantee, "Must provide a URL for the grantee of the data consent");
            Objects.requireNonNull(this.registeredShapeTree, "Must provide a URL for the registered shape tree of the data consent");
            Objects.requireNonNull(this.accessModes, "Must provide a list of access modes for the data consent");
            Objects.requireNonNull(this.scopeOfConsent, "Must provide a scope of consent for the data consent");
            Objects.requireNonNull(this.accessNeed, "Must provide a URL for the access need associated with the data consent");
            if (this.dataset == null) { populateDataset(); }
            return validate(new DataConsent(this));
        }
    }
}

package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.agents.AgentRegistration;
import com.janeirodigital.sai.core.agents.AgentRegistry;
import com.janeirodigital.sai.core.agents.SocialAgentRegistration;
import com.janeirodigital.sai.core.data.DataRegistration;
import com.janeirodigital.sai.core.data.DataRegistry;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.readable.InheritableDataGrant;
import com.janeirodigital.sai.core.readable.ReadableAccessGrant;
import com.janeirodigital.sai.core.readable.ReadableDataGrant;
import com.janeirodigital.sai.core.resources.ImmutableResource;
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
import java.util.*;

import static com.janeirodigital.sai.core.vocabularies.AclVocabulary.ACL_CREATE;
import static com.janeirodigital.sai.core.vocabularies.AclVocabulary.ACL_WRITE;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.httputils.HttpUtils.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.rdfutils.RdfUtils.*;

/**
 * Immutable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-authorization">Data Authorization</a>
 */
@Getter
public class DataAuthorization extends ImmutableResource {

    private final URI dataOwner;
    private final URI grantedBy;
    private final URI grantee;
    private final URI registeredShapeTree;
    private final List<RDFNode> accessModes;
    private final List<RDFNode> creatorAccessModes;
    private final RDFNode scopeOfAuthorization;
    private final URI dataRegistration;
    private final List<URI> dataInstances;
    private final URI accessNeed;
    private final URI inheritsFrom;
    private final List<DataAuthorization> inheritingAuthorizations;

    /**
     * Construct a {@link DataAuthorization} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private DataAuthorization(Builder builder) throws SaiException {
        super(builder);
        this.dataOwner = builder.dataOwner;
        this.grantedBy = builder.grantedBy;
        this.grantee = builder.grantee;
        this.registeredShapeTree = builder.registeredShapeTree;
        this.accessModes = builder.accessModes;
        this.creatorAccessModes = builder.creatorAccessModes;
        this.scopeOfAuthorization = builder.scopeOfAuthorization;
        this.dataRegistration = builder.dataRegistration;
        this.dataInstances = builder.dataInstances;
        this.accessNeed = builder.accessNeed;
        this.inheritsFrom = builder.inheritsFrom;
        this.inheritingAuthorizations = new ArrayList<>();
    }

    /**
     * Get a {@link DataAuthorization} at the provided <code>uri</code>
     * @param uri URI of the {@link DataAuthorization} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link DataAuthorization}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static DataAuthorization get(URI uri, SaiSession saiSession, ContentType contentType) throws SaiException, SaiHttpNotFoundException {
        DataAuthorization.Builder builder = new DataAuthorization.Builder(uri, saiSession);
        try (Response response = read(uri, saiSession, contentType, false)) {
            return builder.setDataset(response).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URI, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param uri URI of the {@link DataAuthorization} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link DataAuthorization}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public static DataAuthorization get(URI uri, SaiSession saiSession) throws SaiException, SaiHttpNotFoundException {
        return get(uri, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link DataAuthorization} using the attributes of the current instance
     * @return Reloaded {@link DataAuthorization}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public DataAuthorization reload() throws SaiException, SaiHttpNotFoundException {
        return get(this.uri, this.saiSession, this.contentType);
    }
    
    /**
     * Generate one or more {@link DataGrant}s for this {@link DataAuthorization}.
     * @return List of generated {@link DataGrant}s
     */
    public List<DataGrant> generateGrants(AccessAuthorization accessAuthorization, AgentRegistration granteeRegistration,
                                          AgentRegistry agentRegistry, List<DataRegistry> dataRegistries) throws SaiException, SaiHttpNotFoundException {
        Objects.requireNonNull(granteeRegistration, "Must provide a grantee agent registration to generate data grants");
        Objects.requireNonNull(agentRegistry, "Must provide an agent registry to generate data grants");
        Objects.requireNonNull(dataRegistries, "Must provide data registries to generate data grants");
        if (this.getScopeOfAuthorization().equals(SCOPE_INHERITED)) { throw new SaiException("A data authorization with an inherited scope cannot generate data grants"); }
        List<DataGrant> dataGrants = new ArrayList<>();
        if (this.getDataOwner() == null || this.getDataOwner().equals(this.getGrantedBy())) {
            // Scope: All - Data owner is sharing across their data and data shared with them (dataOwner == null)
            // Scope: AllFromRegistry - Data owner sharing all data of a type from a data registry they own (dataOwner == grantedBy)
            // Scope: SelectedFromRegistry - Data owner sharing data instances of a type from a data registry they own (dataOwner == grantedBy)
            dataGrants.addAll(generateSourceGrants(accessAuthorization, granteeRegistration, dataRegistries));
        }

        if (this.getDataOwner() == null || !this.getDataOwner().equals(this.getGrantedBy())) {
            // Scope: All - Data owner is sharing across their data and data shared with them (dataOwner == null)
            // Scope: AllFromAgent - Data owner sharing all data of a type shared with them (dataOwner != grantedBy)
            dataGrants.addAll(generateDelegatedGrants(granteeRegistration, agentRegistry));
        }
        return dataGrants;
    }

    /**
     * Generate grants for a {@link DataAuthorization} where the data owner is sharing data they own directly.
     * <br>Applies to scopes: All, AllFromRegistry, SelectedFromRegistry
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#access-scopes">Data Access Scopes</a>
     * @param accessAuthorization {@link AccessAuthorization} that this {@link DataAuthorization} is associated with
     * @param granteeRegistration {@link AgentRegistration} of the grantee in data owner's {@link AgentRegistry}
     * @param dataRegistries List of {@link DataRegistry} instances of the data owner
     * @return List of generated {@link DataGrant}s
     * @throws SaiException
     */
    private List<DataGrant> generateSourceGrants(AccessAuthorization accessAuthorization, AgentRegistration granteeRegistration,
                                                 List<DataRegistry> dataRegistries) throws SaiException {

        if (!this.getScopeOfAuthorization().equals(SCOPE_ALL) && !this.getScopeOfAuthorization().equals(SCOPE_ALL_FROM_REGISTRY) && !this.getScopeOfAuthorization().equals(SCOPE_SELECTED_FROM_REGISTRY)) {
            throw new SaiException("Cannot generate a regular (non-delegated) data grant for a data authorization with scope: " + this.getScopeOfAuthorization());
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
                                                         .filter(e -> this.getDataRegistration().equals(e.getKey().getUri()))
                                                         .findAny().orElse(null);
            if (filtered == null) { throw new SaiException("Data registration " + this.getDataRegistration() + "not found in data registries: " + dataRegistries); }
            dataRegistrations.clear();
            dataRegistrations.put(filtered.getKey(), filtered.getValue());
        }

        List<DataGrant> dataGrants = new ArrayList<>();
        for (Map.Entry<DataRegistration, DataRegistry> entry : dataRegistrations.entrySet()) {
            DataRegistration dataRegistration = entry.getKey();
            URI dataGrantUri = granteeRegistration.generateContainedUri();
            DataGrant.Builder grantBuilder = new DataGrant.Builder(dataGrantUri, this.saiSession);
            // create children if needed (generate child source grants)
            List<DataGrant> childDataGrants = generateChildSourceGrants(accessAuthorization, dataGrantUri, entry.getKey(), entry.getValue(), granteeRegistration);
            // build the data grant
            grantBuilder.setScopeOfGrant(SCOPE_ALL_FROM_REGISTRY); // default value in this context
            if (this.getScopeOfAuthorization().equals(SCOPE_SELECTED_FROM_REGISTRY)) { grantBuilder.setScopeOfGrant(SCOPE_SELECTED_FROM_REGISTRY); }
            grantBuilder.setDataOwner(this.grantedBy);
            grantBuilder.setGrantee(this.grantee);
            grantBuilder.setRegisteredShapeTree(this.registeredShapeTree);
            grantBuilder.setAccessModes(this.accessModes);
            grantBuilder.setAccessNeed(this.accessNeed);
            grantBuilder.setDataRegistration(dataRegistration.getUri());
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
     * being shared by the data owner directly. Called from {@link #generateSourceGrants(AccessAuthorization, AgentRegistration, List)}.
     * <br>Applies to scopes: Inherited
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#access-scopes">Data Access Scopes</a>
     * @param accessAuthorization {@link AccessAuthorization} that the {@link DataAuthorization} is associated with
     * @param dataGrantUri URI of the parent {@link DataGrant} being inherited from
     * @param dataRegistration {@link DataRegistration} the data resides in
     * @param dataRegistry {@link DataRegistry} the <code>dataRegistration</code> belongs to
     * @param granteeRegistration {@link AgentRegistration} of the grantee in data owner's {@link AgentRegistry}
     * @return List of generated {@link DataGrant}s
     * @throws SaiException
     */
    private List<DataGrant> generateChildSourceGrants(AccessAuthorization accessAuthorization, URI dataGrantUri, DataRegistration dataRegistration,
                                                      DataRegistry dataRegistry, AgentRegistration granteeRegistration) throws SaiException {
        List<DataGrant> childDataGrants = new ArrayList<>();
        for (DataAuthorization childAuthorization : accessAuthorization.getDataAuthorizations()) {
            // for each child data authorization that inherits from the current one (e.g. specifies it with inheritsFrom)
            if (childAuthorization.getScopeOfAuthorization().equals(SCOPE_INHERITED) && childAuthorization.getInheritsFrom().equals(this.getUri())) {
                URI childGrantUri = granteeRegistration.generateContainedUri();
                // find the data registration for the child data authorization (must be same registry as parent)
                DataRegistration childRegistration = dataRegistry.getDataRegistrations().find(childAuthorization.registeredShapeTree);
                if (childRegistration == null) { throw new SaiException("Could not find data registration " + dataRegistration.getUri() + " in registry " + dataRegistry.getUri()); }
                DataGrant.Builder childBuilder = new DataGrant.Builder(childGrantUri, this.saiSession);
                childBuilder.setDataOwner(childAuthorization.grantedBy);
                childBuilder.setGrantee(childAuthorization.grantee);
                childBuilder.setRegisteredShapeTree(childAuthorization.registeredShapeTree);
                childBuilder.setDataRegistration(childRegistration.getUri());
                childBuilder.setScopeOfGrant(SCOPE_INHERITED);
                childBuilder.setAccessModes(childAuthorization.accessModes);
                childBuilder.setCreatorAccessModes(childAuthorization.creatorAccessModes);
                childBuilder.setAccessNeed(childAuthorization.accessNeed);
                childBuilder.setInheritsFrom(dataGrantUri);
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
     * @param granteeRegistration {@link AgentRegistration} of the grantee receiving delegated permissions
     * @param agentRegistry {@link AgentRegistry} of the social agent delegating permission
     * @return
     */
    private List<DataGrant> generateDelegatedGrants(AgentRegistration granteeRegistration, AgentRegistry agentRegistry) throws SaiException, SaiHttpNotFoundException {
        if (!this.getScopeOfAuthorization().equals(SCOPE_ALL) && !this.getScopeOfAuthorization().equals(SCOPE_ALL_FROM_AGENT)) {
            throw new SaiException("Cannot generate a delegated data grant for a data authorization with scope: " + this.getScopeOfAuthorization());
        }

        List<DataGrant> delegatedGrants = new ArrayList<>();
        for (SocialAgentRegistration agentRegistration : agentRegistry.getSocialAgentRegistrations()) {
            // continue if the grantee of the data authorization is the registered agent of the agent registration (don't delegate to themselves)
            if (this.getGrantee().equals(agentRegistration.getRegisteredAgent())) { continue; }
            // Continue if the data owner is set (AllFromAgent) but the agent registration is not theirs (registeredAgent)
            if (this.getDataOwner() != null && !agentRegistration.getRegisteredAgent().equals(this.getDataOwner())) { continue; }
            // continue if there's no reciprocal registration
            if (agentRegistration.getReciprocalRegistration() == null) { continue; }
            // Lookup the remote agent registration
            SocialAgentRegistration remoteRegistration = SocialAgentRegistration.get(agentRegistration.getReciprocalRegistration(), this.saiSession);
            // continue if there's no access grant iri in the reciprocal (which would mean they haven't shared anything so there's nothing to delegate)
            if (remoteRegistration.getAccessGrantUri() == null) { continue; }
            ReadableAccessGrant remoteGrant = ReadableAccessGrant.get(remoteRegistration.getAccessGrantUri(), this.saiSession);
            for (ReadableDataGrant remoteDataGrant : remoteGrant.getDataGrants()) {
                // skip data grants that don't match the shape tree of this data authorization
                if (!remoteDataGrant.getRegisteredShapeTree().equals(this.registeredShapeTree)) { continue; }
                // filter to a given data registration if specified
                if (this.getDataRegistration() != null && !remoteDataGrant.getDataRegistration().equals(this.getDataRegistration())) { continue; }
                // Build the delegated data grant based on this data authorization and the remote data grant
                URI grantUri = granteeRegistration.generateContainedUri();
                DataGrant.Builder grantBuilder = new DataGrant.Builder(grantUri, this.saiSession);
                // generate child delegated data grants if necessary
                List<DataGrant> childDataGrants = generateChildDelegatedGrants(grantUri, remoteDataGrant, granteeRegistration);
                // build the delegated data grant
                grantBuilder.setDataOwner(remoteDataGrant.getDataOwner());
                grantBuilder.setGrantee(this.grantee);
                grantBuilder.setRegisteredShapeTree(remoteDataGrant.getRegisteredShapeTree());
                grantBuilder.setScopeOfGrant(remoteDataGrant.getScopeOfGrant());
                grantBuilder.setAccessNeed(remoteDataGrant.getAccessNeed());
                grantBuilder.setDelegationOf(remoteDataGrant.getUri());
                if (!remoteDataGrant.getAccessModes().containsAll(this.accessModes)) { throw new SaiException("Data authorization issues access modes that were not granted by remote social agent"); }
                grantBuilder.setAccessModes(this.accessModes);
                if (this.canCreate()) {
                    if (!remoteDataGrant.getCreatorAccessModes().containsAll(this.creatorAccessModes)) { throw new SaiException("Data authorization issues creator access modes that were not granted by remote social agent"); }
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
     * @param dataGrantUri URI of the parent delegated {@link DataGrant}
     * @param remoteDataGrant URI of the remote {@link DataGrant} that is being delegated
     * @param granteeRegistration {@link AgentRegistration} of the grantee
     * @return List of child delegated {@link DataGrant}s
     * @throws SaiException
     */
    private List<DataGrant> generateChildDelegatedGrants(URI dataGrantUri, ReadableDataGrant remoteDataGrant, AgentRegistration granteeRegistration) throws SaiException {
        List<DataGrant> childDataGrants = new ArrayList<>();
        for (DataAuthorization childAuthorization : this.getInheritingAuthorizations()) {
            InheritableDataGrant remoteInheritableGrant = (InheritableDataGrant) remoteDataGrant;
            for (ReadableDataGrant remoteChildGrant: remoteInheritableGrant.getInheritingGrants()) {
                // continue if the remote inheriting grant isn't the same shape tree as the child authorization
                if (!remoteChildGrant.getRegisteredShapeTree().equals(childAuthorization.getRegisteredShapeTree())) { continue; }
                URI childGrantUri = granteeRegistration.generateContainedUri();
                DataGrant.Builder childBuilder = new DataGrant.Builder(childGrantUri, this.saiSession);
                childBuilder.setDataOwner(remoteChildGrant.getDataOwner());
                childBuilder.setGrantee(this.grantee);
                childBuilder.setRegisteredShapeTree(remoteChildGrant.getRegisteredShapeTree());
                childBuilder.setScopeOfGrant(SCOPE_INHERITED);
                childBuilder.setAccessNeed(remoteChildGrant.getAccessNeed());
                childBuilder.setDataRegistration(remoteChildGrant.getDataRegistration());
                if (!remoteChildGrant.getAccessModes().containsAll(childAuthorization.accessModes)) { throw new SaiException("Data authorization issues access modes that were not granted by remote social agent"); }
                childBuilder.setAccessModes(childAuthorization.accessModes);
                if (childAuthorization.canCreate()) {
                    if (!remoteChildGrant.getCreatorAccessModes().containsAll(childAuthorization.creatorAccessModes)) { throw new SaiException("Data authorization issues creator access modes that were not granted by remote social agent"); }
                    childBuilder.setCreatorAccessModes(this.creatorAccessModes);
                }
                childBuilder.setInheritsFrom(dataGrantUri);
                childBuilder.setDelegationOf(remoteChildGrant.getUri());
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
     * Basic structural validations of the {@link DataAuthorization}
     * @throws SaiException
     */
    private static DataAuthorization validate(DataAuthorization dataAuthorization) throws SaiException {
        Objects.requireNonNull(dataAuthorization, "Must provide a data authorization to validate");
        validateGeneral(dataAuthorization);
        if (dataAuthorization.scopeOfAuthorization.equals(SCOPE_ALL)) { validateAll(dataAuthorization); }
        else if (dataAuthorization.scopeOfAuthorization.equals(SCOPE_ALL_FROM_REGISTRY)) { validateAllFromRegistry(dataAuthorization); }
        else if (dataAuthorization.scopeOfAuthorization.equals(SCOPE_ALL_FROM_AGENT)) { validateAllFromAgent(dataAuthorization); }
        else if (dataAuthorization.scopeOfAuthorization.equals(SCOPE_SELECTED_FROM_REGISTRY)) { validateSelectedFromRegistry(dataAuthorization); }
        else if (dataAuthorization.scopeOfAuthorization.equals(SCOPE_INHERITED)) { validateInherited(dataAuthorization); }
        else if (dataAuthorization.scopeOfAuthorization.equals(SCOPE_NO_ACCESS)) { validateNoAccess(dataAuthorization); }
        else { throw new SaiException("Unsupported data authorization scope: " + dataAuthorization.scopeOfAuthorization); }
        return dataAuthorization;
    }

    /**
     * Validate the data authorization with criteria that isn't specific to a given scope
     * @throws SaiException
     */
    private static void validateGeneral(DataAuthorization dataAuthorization) throws SaiException {
        if (dataAuthorization.canCreate() && dataAuthorization.creatorAccessModes.isEmpty()) {
            throw new SaiException(buildInvalidMessage(dataAuthorization, "Must provide creator access modes when authorization includes the ability to create resources"));
        }
        if (!dataAuthorization.scopeOfAuthorization.equals(SCOPE_INHERITED) && dataAuthorization.inheritsFrom != null) { throw new SaiException(buildInvalidMessage(dataAuthorization, "Cannot inherit from another data authorization without a scope of interop:Inherited")); }
        if (!dataAuthorization.scopeOfAuthorization.equals(SCOPE_SELECTED_FROM_REGISTRY) && !dataAuthorization.dataInstances.isEmpty()) { throw new SaiException(buildInvalidMessage(dataAuthorization, "Cannot target specific data instances without a scope of interop:SelectedFromRegistry")); }
    }

    /**
     * Validate a data authorization with scope of interop:All
     */
    private static void validateAll(DataAuthorization dataAuthorization) throws SaiException {
        if (dataAuthorization.dataOwner != null) { throw new SaiException(buildInvalidMessage(dataAuthorization, "Cannot provide a data owner with scope of interop:All")); }
        if (dataAuthorization.dataRegistration != null) { throw new SaiException(buildInvalidMessage(dataAuthorization, "Cannot target a specific data registration with scope of interop:All")); }
    }

    /**
     * Validate a data authorization with scope of interop:AllFromRegistry
     */
    private static void validateAllFromRegistry(DataAuthorization dataAuthorization) throws SaiException {
        if (dataAuthorization.dataRegistration == null) { throw new SaiException(buildInvalidMessage(dataAuthorization, "Must provide a specific data registration with a scope of interop:AllFromRegistry")); }
    }

    /**
     * Validate a data authorization with scope of interop:AllFromAgent
     */
    private static void validateAllFromAgent(DataAuthorization dataAuthorization) {
        // Placeholder for future logic to validate all from agent scope
    }

    /**
     * Validate a data authorization with scope of interop:SelectedFromRegistry
     */
    private static void validateSelectedFromRegistry(DataAuthorization dataAuthorization) throws SaiException {
        if (dataAuthorization.dataRegistration == null) { throw new SaiException(buildInvalidMessage(dataAuthorization, "Must provide a specific data registration with a scope of interop:SelectedFromRegistry")); }
        if (dataAuthorization.getDataInstances().isEmpty()) { throw new SaiException(buildInvalidMessage(dataAuthorization, "Must provide specific data instances with a scope of interop:SelectedFromRegistry")); }
    }

    /**
     * Validate a data authorization with scope of interop:Inherited
     */
    private static void validateInherited(DataAuthorization dataAuthorization) throws SaiException {
        if (dataAuthorization.inheritsFrom == null) { throw new SaiException(buildInvalidMessage(dataAuthorization, "Must provide a data authorization to inherit from with a scope of interop:Inherited")); }
    }

    /**
     * Validate a data authorization with scope of interop:NoAccess
     */
    private static void validateNoAccess(DataAuthorization dataAuthorization) {
        // Placeholder for future logic to validate no access scope
    }

    /**
     * Provide context for a validation failure in string form
     * @param reason reason for the validation failure
     * @return Stringified failure message
     */
    private static String buildInvalidMessage(DataAuthorization dataAuthorization, String reason) {
        StringBuilder message = new StringBuilder();
        message.append("Invalid data authorization " + dataAuthorization.uri);
        message.append(" - Scope: " + dataAuthorization.scopeOfAuthorization);
        message.append(" - Shape Tree: " + dataAuthorization.registeredShapeTree);
        message.append(" - Grantee: " + dataAuthorization.grantee);
        message.append(" - Reason: " + reason);
        return message.toString();
    }

    /**
     * Builder for {@link DataAuthorization} instances.
     */
    public static class Builder extends ImmutableResource.Builder<Builder> {

        private URI dataOwner;
        private URI grantedBy;
        private URI grantee;
        private URI registeredShapeTree;
        private List<RDFNode> accessModes;
        private List<RDFNode> creatorAccessModes;
        private RDFNode scopeOfAuthorization;
        private URI dataRegistration;
        private List<URI> dataInstances;
        private URI accessNeed;
        private URI inheritsFrom;

        /**
         * Initialize builder with <code>uri</code> and <code>saiSession</code>
         * @param uri URI of the {@link AccessAuthorization} to build
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

        public Builder setDataOwner(URI dataOwner) {
            Objects.requireNonNull(dataOwner, "Must provide a URI for the data owner");
            this.dataOwner = dataOwner;
            return this;
        }

        public Builder setGrantedBy(URI grantedBy) {
            Objects.requireNonNull(grantedBy, "Must provide a URI for the social agent that granted the authorization");
            this.grantedBy = grantedBy;
            return this;
        }

        public Builder setGrantee(URI grantee) {
            Objects.requireNonNull(grantee, "Must provide a URI for the grantee of the data authorization");
            this.grantee = grantee;
            return this;
        }

        public Builder setRegisteredShapeTree(URI registeredShapeTree) {
            Objects.requireNonNull(registeredShapeTree, "Must provide a URI for the registered shape tree of the data authorization");
            this.registeredShapeTree = registeredShapeTree;
            return this;
        }
        
        public Builder setAccessModes(List<RDFNode> accessModes) {
            Objects.requireNonNull(accessModes, "Must provide a list of access modes for the data authorization");
            this.accessModes = accessModes;
            return this;
        }

        public Builder setCreatorAccessModes(List<RDFNode> creatorAccessModes) {
            Objects.requireNonNull(creatorAccessModes, "Must provide a list of creator access modes for the data authorization");
            this.creatorAccessModes = creatorAccessModes;
            return this;
        }

        public Builder setScopeOfAuthorization(RDFNode scopeOfAuthorization) {
            Objects.requireNonNull(scopeOfAuthorization, "Must provide a scope of authorization for the data authorization");
            this.scopeOfAuthorization = scopeOfAuthorization;
            return this;
        }

        public Builder setDataRegistration(URI dataRegistration) {
            Objects.requireNonNull(dataRegistration, "Must provide a URI for the data registration associated with the data authorization");
            this.dataRegistration = dataRegistration;
            return this;
        }

        public Builder setDataInstances(List<URI> dataInstances) {
            Objects.requireNonNull(dataInstances, "Must provide a URI for the data instances associated with the data authorization");
            this.dataInstances = dataInstances;
            return this;
        }

        public Builder setAccessNeed(URI accessNeed) {
            Objects.requireNonNull(accessNeed, "Must provide a URI for the access need associated with the data authorization");
            this.accessNeed = accessNeed;
            return this;
        }

        public Builder setInheritsFrom(URI inheritsFrom) {
            Objects.requireNonNull(inheritsFrom, "Must provide a URI for the data authorization being inherited from");
            this.inheritsFrom = inheritsFrom;
            return this;
        }

        private void populateFromDataset() throws SaiException {
            try {
                this.dataOwner = getUriObject(this.resource, DATA_OWNER);
                this.grantedBy = getRequiredUriObject(this.resource, GRANTED_BY);
                this.grantee = getRequiredUriObject(this.resource, GRANTEE);
                this.registeredShapeTree = getRequiredUriObject(this.resource, REGISTERED_SHAPE_TREE);
                this.accessModes = getRequiredObjects(this.resource, ACCESS_MODE);
                this.creatorAccessModes = getRequiredObjects(this.resource, CREATOR_ACCESS_MODE);
                this.scopeOfAuthorization = getRequiredObject(this.resource, SCOPE_OF_AUTHORIZATION);
                this.dataRegistration = getUriObject(this.resource, HAS_DATA_REGISTRATION);
                this.dataInstances = getUriObjects(this.resource, HAS_DATA_INSTANCE);
                this.accessNeed = getRequiredUriObject(this.resource, SATISFIES_ACCESS_NEED);
                this.inheritsFrom = getUriObject(this.resource, INHERITS_FROM_AUTHORIZATION);
            } catch (SaiRdfException | SaiRdfNotFoundException ex) {
                throw new SaiException("Unable to populate immutable data authorization. Missing required fields", ex);
            }
        }

        private void populateDataset() {
            this.resource = getNewResourceForType(this.uri, DATA_AUTHORIZATION);
            this.dataset = this.resource.getModel();

            updateObject(this.resource, GRANTED_BY, this.grantedBy);
            updateObject(this.resource, GRANTEE, this.grantee);
            updateObject(this.resource, REGISTERED_SHAPE_TREE, this.registeredShapeTree);
            updateObject(this.resource, SCOPE_OF_AUTHORIZATION, this.scopeOfAuthorization);
            updateObject(this.resource, SATISFIES_ACCESS_NEED, this.accessNeed);

            if (this.dataOwner != null) { updateObject(this.resource, DATA_OWNER, this.dataOwner); }
            if (this.dataRegistration != null) { updateObject(this.resource, HAS_DATA_REGISTRATION, this.dataRegistration); }
            if (!this.dataInstances.isEmpty()) { updateUriObjects(this.resource, HAS_DATA_INSTANCE, this.dataInstances); }
            if (this.inheritsFrom != null) { updateObject(this.resource, INHERITS_FROM_AUTHORIZATION, this.inheritsFrom); }

            final List<URI> accessModeUris = new ArrayList<>();
            for(RDFNode mode : this.accessModes) { accessModeUris.add(URI.create(mode.asResource().getURI())); }
            updateUriObjects(this.resource, ACCESS_MODE, accessModeUris);

            if (!this.creatorAccessModes.isEmpty()) {
                final List<URI> creatorAccessModeUris = new ArrayList<>();
                for(RDFNode creatorMode : this.creatorAccessModes) { creatorAccessModeUris.add(URI.create(creatorMode.asResource().getURI())); }
                updateUriObjects(this.resource, ACCESS_MODE, creatorAccessModeUris);
            }
        }

        public DataAuthorization build() throws SaiException {
            Objects.requireNonNull(this.grantedBy, "Must provide a URI for the grantee of the data authorization");
            Objects.requireNonNull(this.grantee, "Must provide a URI for the grantee of the data authorization");
            Objects.requireNonNull(this.registeredShapeTree, "Must provide a URI for the registered shape tree of the data authorization");
            Objects.requireNonNull(this.accessModes, "Must provide a list of access modes for the data authorization");
            Objects.requireNonNull(this.scopeOfAuthorization, "Must provide a scope of authorization for the data authorization");
            Objects.requireNonNull(this.accessNeed, "Must provide a URI for the access need associated with the data authorization");
            if (this.dataset == null) { populateDataset(); }
            return validate(new DataAuthorization(this));
        }
    }
}

package com.janeirodigital.sai.core.authorizations;

import com.janeirodigital.sai.core.agents.AgentRegistration;
import com.janeirodigital.sai.core.agents.AgentRegistry;
import com.janeirodigital.sai.core.data.DataRegistry;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.resources.ImmutableResource;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.sai.rdfutils.SaiRdfNotFoundException;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.httputils.HttpUtils.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.rdfutils.RdfUtils.*;

/**
 * Immutable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#access-authorization">Access Authorization</a>
 */
@Getter
public class AccessAuthorization extends ImmutableResource {

    private final URI grantedBy;
    private final URI grantedWith;
    private final OffsetDateTime grantedAt;
    private final URI grantee;
    private final URI accessNeedGroup;
    private final URI replaces;
    private final List<DataAuthorization> dataAuthorizations;

    /**
     * Construct an {@link AccessAuthorization} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private AccessAuthorization(Builder builder) throws SaiException {
        super(builder);
        this.grantedBy = builder.grantedBy;
        this.grantedWith = builder.grantedWith;
        this.grantedAt = builder.grantedAt;
        this.grantee = builder.grantee;
        this.accessNeedGroup = builder.accessNeedGroup;
        this.replaces = builder.replaces;
        this.dataAuthorizations = builder.dataAuthorizations;
    }

    /**
     * Get an {@link AccessAuthorization} at the provided <code>uri</code>
     * @param uri URI of the {@link AccessAuthorization} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link AccessAuthorization}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static AccessAuthorization get(URI uri, SaiSession saiSession, ContentType contentType) throws SaiException, SaiHttpNotFoundException {
        AccessAuthorization.Builder builder = new AccessAuthorization.Builder(uri, saiSession);
        try (Response response = read(uri, saiSession, contentType, false)) {
            return builder.setDataset(response).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URI, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param uri URI of the {@link AccessAuthorization} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link AccessAuthorization}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public static AccessAuthorization get(URI uri, SaiSession saiSession) throws SaiHttpNotFoundException, SaiException {
        return get(uri, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link AccessAuthorization} using the attributes of the current instance
     * @return Reloaded {@link AccessAuthorization}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public AccessAuthorization reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.uri, this.saiSession, this.contentType);
    }

    /**
     * Generate an {@link AccessGrant} and its associated {@link DataGrant}s based on this {@link AccessAuthorization}.
     * @param granteeRegistration {@link AgentRegistration} for the grantee
     * @param agentRegistry {@link AgentRegistry} for the social agent performing the grant
     * @param dataRegistries List of {@link DataRegistry} instances in scope of the authorization
     * @return Generated {@link AccessGrant}
     * @throws SaiException
     */
    public AccessGrant generateGrant(AgentRegistration granteeRegistration, AgentRegistry agentRegistry, List<DataRegistry> dataRegistries) throws SaiException, SaiHttpNotFoundException {
        Objects.requireNonNull(granteeRegistration, "Must provide a grantee agent registration to generate an access grant");
        Objects.requireNonNull(agentRegistry, "Must provide an agent registry to generate an access grant");
        Objects.requireNonNull(dataRegistries, "Must provide data registries to generate an access grant");
        List<DataAuthorization> primaryDataAuthorizations = new ArrayList<>();
        this.dataAuthorizations.forEach(dataAuthorization -> {
            if (!dataAuthorization.getScopeOfAuthorization().equals(SCOPE_INHERITED)) { primaryDataAuthorizations.add(dataAuthorization); }
        });
        List<DataGrant> dataGrants = new ArrayList<>();
        for (DataAuthorization dataAuthorization : primaryDataAuthorizations) { dataGrants.addAll(dataAuthorization.generateGrants(this, granteeRegistration, agentRegistry, dataRegistries)); }
        // TODO - If there was a prior access grant, look at reusing some data grants
        URI accessGrantUri = granteeRegistration.generateContainedUri();
        AccessGrant.Builder grantBuilder = new AccessGrant.Builder(accessGrantUri, this.saiSession);
        return grantBuilder.setGrantedBy(this.grantedBy).setGrantedAt(this.grantedAt).setGrantee(this.grantee)
                           .setAccessNeedGroup(this.accessNeedGroup).setDataGrants(dataGrants).build();
    }

    /**
     * Builder for {@link AccessAuthorization} instances.
     */
    public static class Builder extends ImmutableResource.Builder<Builder> {

        private URI grantedBy;
        private URI grantedWith;
        private OffsetDateTime grantedAt;
        private URI grantee;
        private URI accessNeedGroup;
        private URI replaces;
        private List<DataAuthorization> dataAuthorizations;

        /**
         * Initialize builder with <code>uri</code> and <code>saiSession</code>
         * @param uri URI of the {@link AccessAuthorization} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URI uri, SaiSession saiSession) {
            super(uri, saiSession);
            this.dataAuthorizations = new ArrayList<>();
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
         * Set the URI of the social agent that granted the access authorization
         * @param grantedBy URI of the social agent grantor
         * @return {@link Builder}
         */
        public Builder setGrantedBy(URI grantedBy) {
            Objects.requireNonNull(grantedBy, "Must provide a URI for the social agent that granted the access authorization");
            this.grantedBy = grantedBy;
            return this;
        }

        public Builder setGrantedWith(URI grantedWith) {
            Objects.requireNonNull(grantedWith, "Must provide a URI for the application that was used to grant the access authorization");
            this.grantedWith = grantedWith;
            return this;
        }

        public Builder setGrantedAt(OffsetDateTime grantedAt) {
            Objects.requireNonNull(grantedAt, "Must provide the time the access authorization was granted at");
            this.grantedAt = grantedAt;
            return this;
        }

        public Builder setGrantee(URI grantee) {
            Objects.requireNonNull(grantee, "Must provide a URI for the grantee of the access authorization");
            this.grantee = grantee;
            return this;
        }

        public Builder setAccessNeedGroup(URI accessNeedGroup) {
            Objects.requireNonNull(accessNeedGroup, "Must provide a URI for the access need group of the access authorization");
            this.accessNeedGroup = accessNeedGroup;
            return this;
        }

        public Builder setReplaces(URI replaces) {
            Objects.requireNonNull(replaces, "Must provide a URI for the access authorization that is being replaced");
            this.replaces = replaces;
            return this;
        }

        public Builder setDataAuthorizations(List<DataAuthorization> dataAuthorizations) {
            Objects.requireNonNull(dataAuthorizations, "Must provide a list of data authorizations for the access authorization");
            this.dataAuthorizations = dataAuthorizations;
            return this;
        }

        /**
         * Populates "parent" data authorizations with the "child" data authorizations that inherit from them
         */
        private void organizeInheritance() {
            for (DataAuthorization dataAuthorization : this.dataAuthorizations) {
                if (!dataAuthorization.getScopeOfAuthorization().equals(SCOPE_INHERITED)) {
                    for (DataAuthorization childAuthorization : this.dataAuthorizations) {
                        if (childAuthorization.getScopeOfAuthorization().equals(SCOPE_INHERITED) && childAuthorization.getInheritsFrom().equals(dataAuthorization.getUri())) {
                            dataAuthorization.getInheritingAuthorizations().add(childAuthorization);
                        }
                    }
                }
            }
        }

        /**
         * Populates the fields of the {@link AccessAuthorization} based on the associated Jena resource.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException {
            try {
                this.grantedBy = getRequiredUriObject(this.resource, GRANTED_BY);
                this.grantedWith = getRequiredUriObject(this.resource, GRANTED_WITH);
                this.grantedAt = getRequiredDateTimeObject(this.resource, GRANTED_AT);
                this.grantee = getRequiredUriObject(this.resource, GRANTEE);
                this.accessNeedGroup = getRequiredUriObject(this.resource, HAS_ACCESS_NEED_GROUP);
                this.replaces = getUriObject(this.resource, REPLACES);
                List<URI> dataAuthorizationUris = getRequiredUriObjects(this.resource, HAS_DATA_AUTHORIZATION);
                for (URI dataAuthorizationUri : dataAuthorizationUris) { this.dataAuthorizations.add(DataAuthorization.get(dataAuthorizationUri, this.saiSession)); }
                organizeInheritance();
            } catch (SaiRdfException | SaiRdfNotFoundException | SaiHttpNotFoundException ex) {
                throw new SaiException("Unable to populate immutable access authorization resource", ex);
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        private void populateDataset() {
            this.resource = getNewResourceForType(this.uri, ACCESS_AUTHORIZATION);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, GRANTED_BY, this.grantedBy);
            updateObject(this.resource, GRANTED_WITH, this.grantedWith);
            if (this.grantedAt == null) { this.grantedAt = OffsetDateTime.now(); }
            updateObject(this.resource, GRANTED_AT, this.grantedAt);
            updateObject(this.resource, GRANTEE, this.grantee);
            updateObject(this.resource, HAS_ACCESS_NEED_GROUP, this.accessNeedGroup);
            if (this.replaces != null) { updateObject(this.resource, REPLACES, this.replaces); }
            List<URI> dataAuthorizationUris = new ArrayList<>();
            for (DataAuthorization dataAuthorization : this.dataAuthorizations) { dataAuthorizationUris.add(dataAuthorization.getUri()); }
            organizeInheritance();
            updateUriObjects(this.resource, HAS_DATA_AUTHORIZATION, dataAuthorizationUris);
        }

        /**
         * Build the {@link AccessAuthorization} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}.
         * @return {@link AccessAuthorization}
         * @throws SaiException
         */
        public AccessAuthorization build() throws SaiException {
            Objects.requireNonNull(this.grantedBy, "Must provide a URI for the social agent that granted the access authorization");
            Objects.requireNonNull(this.grantedWith, "Must provide a URI for the application that was used to grant the access authorization");
            Objects.requireNonNull(this.grantee, "Must provide a URI for the grantee of the access authorization");
            Objects.requireNonNull(this.accessNeedGroup, "Must provide a URI for the access need group of the access authorization");
            Objects.requireNonNull(this.dataAuthorizations, "Must provide a list of data authorizations for the access authorization");
            if (this.dataset == null) { populateDataset(); }
            return new AccessAuthorization(this);
        }
    }

}

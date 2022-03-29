package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.crud.AgentRegistration;
import com.janeirodigital.sai.core.crud.AgentRegistry;
import com.janeirodigital.sai.core.crud.DataRegistry;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.sai.rdfutils.SaiRdfNotFoundException;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URL;
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

    private final URL grantedBy;
    private final URL grantedWith;
    private final OffsetDateTime grantedAt;
    private final URL grantee;
    private final URL accessNeedGroup;
    private final URL replaces;
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
     * Get an {@link AccessAuthorization} at the provided <code>url</code>
     * @param url URL of the {@link AccessAuthorization} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link AccessAuthorization}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static AccessAuthorization get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiHttpNotFoundException {
        AccessAuthorization.Builder builder = new AccessAuthorization.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(response).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link AccessAuthorization} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link AccessAuthorization}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public static AccessAuthorization get(URL url, SaiSession saiSession) throws SaiHttpNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link AccessAuthorization} using the attributes of the current instance
     * @return Reloaded {@link AccessAuthorization}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public AccessAuthorization reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
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
        URL accessGrantUrl = granteeRegistration.generateContainedUrl();
        AccessGrant.Builder grantBuilder = new AccessGrant.Builder(accessGrantUrl, this.saiSession);
        return grantBuilder.setGrantedBy(this.grantedBy).setGrantedAt(this.grantedAt).setGrantee(this.grantee)
                           .setAccessNeedGroup(this.accessNeedGroup).setDataGrants(dataGrants).build();
    }

    /**
     * Builder for {@link AccessAuthorization} instances.
     */
    public static class Builder extends ImmutableResource.Builder<Builder> {

        private URL grantedBy;
        private URL grantedWith;
        private OffsetDateTime grantedAt;
        private URL grantee;
        private URL accessNeedGroup;
        private URL replaces;
        private List<DataAuthorization> dataAuthorizations;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link AccessAuthorization} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URL url, SaiSession saiSession) {
            super(url, saiSession);
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
         * Set the URL of the social agent that granted the access authorization
         * @param grantedBy URL of the social agent grantor
         * @return {@link Builder}
         */
        public Builder setGrantedBy(URL grantedBy) {
            Objects.requireNonNull(grantedBy, "Must provide a URL for the social agent that granted the access authorization");
            this.grantedBy = grantedBy;
            return this;
        }

        public Builder setGrantedWith(URL grantedWith) {
            Objects.requireNonNull(grantedWith, "Must provide a URL for the application that was used to grant the access authorization");
            this.grantedWith = grantedWith;
            return this;
        }

        public Builder setGrantedAt(OffsetDateTime grantedAt) {
            Objects.requireNonNull(grantedAt, "Must provide the time the access authorization was granted at");
            this.grantedAt = grantedAt;
            return this;
        }

        public Builder setGrantee(URL grantee) {
            Objects.requireNonNull(grantee, "Must provide a URL for the grantee of the access authorization");
            this.grantee = grantee;
            return this;
        }

        public Builder setAccessNeedGroup(URL accessNeedGroup) {
            Objects.requireNonNull(accessNeedGroup, "Must provide a URL for the access need group of the access authorization");
            this.accessNeedGroup = accessNeedGroup;
            return this;
        }

        public Builder setReplaces(URL replaces) {
            Objects.requireNonNull(replaces, "Must provide a URL for the access authorization that is being replaced");
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
                        if (childAuthorization.getScopeOfAuthorization().equals(SCOPE_INHERITED) && childAuthorization.getInheritsFrom().equals(dataAuthorization.getUrl())) {
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
                this.grantedBy = getRequiredUrlObject(this.resource, GRANTED_BY);
                this.grantedWith = getRequiredUrlObject(this.resource, GRANTED_WITH);
                this.grantedAt = getRequiredDateTimeObject(this.resource, GRANTED_AT);
                this.grantee = getRequiredUrlObject(this.resource, GRANTEE);
                this.accessNeedGroup = getRequiredUrlObject(this.resource, HAS_ACCESS_NEED_GROUP);
                this.replaces = getUrlObject(this.resource, REPLACES);
                List<URL> dataAuthorizationUrls = getRequiredUrlObjects(this.resource, HAS_DATA_AUTHORIZATION);
                for (URL dataAuthorizationUrl : dataAuthorizationUrls) { this.dataAuthorizations.add(DataAuthorization.get(dataAuthorizationUrl, this.saiSession)); }
                organizeInheritance();
            } catch (SaiRdfException | SaiRdfNotFoundException | SaiHttpNotFoundException ex) {
                throw new SaiException("Unable to populate immutable access authorization resource", ex);
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        private void populateDataset() {
            this.resource = getNewResourceForType(this.url, ACCESS_AUTHORIZATION);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, GRANTED_BY, this.grantedBy);
            updateObject(this.resource, GRANTED_WITH, this.grantedWith);
            if (this.grantedAt == null) { this.grantedAt = OffsetDateTime.now(); }
            updateObject(this.resource, GRANTED_AT, this.grantedAt);
            updateObject(this.resource, GRANTEE, this.grantee);
            updateObject(this.resource, HAS_ACCESS_NEED_GROUP, this.accessNeedGroup);
            if (this.replaces != null) { updateObject(this.resource, REPLACES, this.replaces); }
            List<URL> dataAuthorizationUrls = new ArrayList<>();
            for (DataAuthorization dataAuthorization : this.dataAuthorizations) { dataAuthorizationUrls.add(dataAuthorization.getUrl()); }
            organizeInheritance();
            updateUrlObjects(this.resource, HAS_DATA_AUTHORIZATION, dataAuthorizationUrls);
        }

        /**
         * Build the {@link AccessAuthorization} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}.
         * @return {@link AccessAuthorization}
         * @throws SaiException
         */
        public AccessAuthorization build() throws SaiException {
            Objects.requireNonNull(this.grantedBy, "Must provide a URL for the social agent that granted the access authorization");
            Objects.requireNonNull(this.grantedWith, "Must provide a URL for the application that was used to grant the access authorization");
            Objects.requireNonNull(this.grantee, "Must provide a URL for the grantee of the access authorization");
            Objects.requireNonNull(this.accessNeedGroup, "Must provide a URL for the access need group of the access authorization");
            Objects.requireNonNull(this.dataAuthorizations, "Must provide a list of data authorizations for the access authorization");
            if (this.dataset == null) { populateDataset(); }
            return new AccessAuthorization(this);
        }
    }

}

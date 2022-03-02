package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.crud.AgentRegistration;
import com.janeirodigital.sai.core.crud.AgentRegistry;
import com.janeirodigital.sai.core.crud.DataRegistry;
import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.helpers.HttpHelper.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.core.helpers.HttpHelper.getRdfModelFromResponse;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

/**
 * Immutable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#access-consent">Access Consent</a>
 */
@Getter
public class AccessConsent extends ImmutableResource {

    private final URL grantedBy;
    private final URL grantedWith;
    private final OffsetDateTime grantedAt;
    private final URL grantee;
    private final URL accessNeedGroup;
    private final URL replaces;
    private final List<DataConsent> dataConsents;

    /**
     * Construct an {@link AccessConsent} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private AccessConsent(Builder builder) throws SaiException {
        super(builder);
        this.grantedBy = builder.grantedBy;
        this.grantedWith = builder.grantedWith;
        this.grantedAt = builder.grantedAt;
        this.grantee = builder.grantee;
        this.accessNeedGroup = builder.accessNeedGroup;
        this.replaces = builder.replaces;
        this.dataConsents = builder.dataConsents;
    }

    /**
     * Get an {@link AccessConsent} at the provided <code>url</code>
     * @param url URL of the {@link AccessConsent} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link AccessConsent}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static AccessConsent get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        AccessConsent.Builder builder = new AccessConsent.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link AccessConsent} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link AccessConsent}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public static AccessConsent get(URL url, SaiSession saiSession) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link AccessConsent} using the attributes of the current instance
     * @return Reloaded {@link AccessConsent}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public AccessConsent reload() throws SaiNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    /**
     * Generate an {@link AccessGrant} and its associated {@link DataGrant}s based on this {@link AccessConsent}.
     * @param granteeRegistration {@link AgentRegistration} for the grantee
     * @param agentRegistry {@link AgentRegistry} for the social agent performing the grant
     * @param dataRegistries List of {@link DataRegistry} instances in scope of the consent
     * @return Generated {@link AccessGrant}
     * @throws SaiException
     */
    public AccessGrant generateGrant(AgentRegistration granteeRegistration, AgentRegistry agentRegistry, List<DataRegistry> dataRegistries) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(granteeRegistration, "Must provide a grantee agent registration to generate an access grant");
        Objects.requireNonNull(agentRegistry, "Must provide an agent registry to generate an access grant");
        Objects.requireNonNull(dataRegistries, "Must provide data registries to generate an access grant");
        List<DataConsent> primaryDataConsents = new ArrayList<>();
        this.dataConsents.forEach(dataConsent -> {
            if (!dataConsent.getScopeOfConsent().equals(SCOPE_INHERITED)) { primaryDataConsents.add(dataConsent); }
        });
        List<DataGrant> dataGrants = new ArrayList<>();
        for (DataConsent dataConsent : primaryDataConsents) { dataGrants.addAll(dataConsent.generateGrants(this, granteeRegistration, agentRegistry, dataRegistries)); }
        // TODO - If there was a prior access grant, look at reusing some data grants
        URL accessGrantUrl = granteeRegistration.generateContainedUrl();
        AccessGrant.Builder grantBuilder = new AccessGrant.Builder(accessGrantUrl, this.saiSession);
        return grantBuilder.setGrantedBy(this.grantedBy).setGrantedAt(this.grantedAt).setGrantee(this.grantee)
                           .setAccessNeedGroup(this.accessNeedGroup).setDataGrants(dataGrants).build();
    }

    /**
     * Builder for {@link AccessConsent} instances.
     */
    public static class Builder extends ImmutableResource.Builder<Builder> {

        private URL grantedBy;
        private URL grantedWith;
        private OffsetDateTime grantedAt;
        private URL grantee;
        private URL accessNeedGroup;
        private URL replaces;
        private List<DataConsent> dataConsents;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link AccessConsent} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URL url, SaiSession saiSession) {
            super(url, saiSession);
            this.dataConsents = new ArrayList<>();
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
         * Set the URL of the social agent that granted the access consent
         * @param grantedBy URL of the social agent grantor
         * @return {@link Builder}
         */
        public Builder setGrantedBy(URL grantedBy) {
            Objects.requireNonNull(grantedBy, "Must provide a URL for the social agent that granted the access consent");
            this.grantedBy = grantedBy;
            return this;
        }

        public Builder setGrantedWith(URL grantedWith) {
            Objects.requireNonNull(grantedWith, "Must provide a URL for the application that was used to grant the access consent");
            this.grantedWith = grantedWith;
            return this;
        }

        public Builder setGrantedAt(OffsetDateTime grantedAt) {
            Objects.requireNonNull(grantedAt, "Must provide the time the access consent was granted at");
            this.grantedAt = grantedAt;
            return this;
        }

        public Builder setGrantee(URL grantee) {
            Objects.requireNonNull(grantee, "Must provide a URL for the grantee of the access consent");
            this.grantee = grantee;
            return this;
        }

        public Builder setAccessNeedGroup(URL accessNeedGroup) {
            Objects.requireNonNull(accessNeedGroup, "Must provide a URL for the access need group of the access consent");
            this.accessNeedGroup = accessNeedGroup;
            return this;
        }

        public Builder setReplaces(URL replaces) {
            Objects.requireNonNull(replaces, "Must provide a URL for the access consent that is being replaced");
            this.replaces = replaces;
            return this;
        }

        public Builder setDataConsents(List<DataConsent> dataConsents) {
            Objects.requireNonNull(dataConsents, "Must provide a list of data consents for the access consent");
            this.dataConsents = dataConsents;
            return this;
        }

        /**
         * Populates "parent" data consents with the "child" data consents that inherit from them
         */
        private void organizeInheritance() {
            for (DataConsent dataConsent : this.dataConsents) {
                if (!dataConsent.getScopeOfConsent().equals(SCOPE_INHERITED)) {
                    for (DataConsent childConsent : this.dataConsents) {
                        if (childConsent.getScopeOfConsent().equals(SCOPE_INHERITED) && childConsent.getInheritsFrom().equals(dataConsent.getUrl())) {
                            dataConsent.getInheritingConsents().add(childConsent);
                        }
                    }
                }
            }
        }

        /**
         * Populates the fields of the {@link AccessConsent} based on the associated Jena resource.
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
                List<URL> dataConsentUrls = getRequiredUrlObjects(this.resource, HAS_DATA_CONSENT);
                for (URL dataConsentUrl : dataConsentUrls) { this.dataConsents.add(DataConsent.get(dataConsentUrl, this.saiSession)); }
                organizeInheritance();
            } catch (SaiNotFoundException | SaiException ex) {
                throw new SaiException("Unable to populate immutable access consent resource: " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        private void populateDataset() {
            this.resource = getNewResourceForType(this.url, ACCESS_CONSENT);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, GRANTED_BY, this.grantedBy);
            updateObject(this.resource, GRANTED_WITH, this.grantedWith);
            updateObject(this.resource, GRANTED_AT, this.grantedAt);
            updateObject(this.resource, GRANTEE, this.grantee);
            updateObject(this.resource, HAS_ACCESS_NEED_GROUP, this.accessNeedGroup);
            if (this.replaces != null) { updateObject(this.resource, REPLACES, this.replaces); }
            List<URL> dataConsentUrls = new ArrayList<>();
            for (DataConsent dataConsent : this.dataConsents) { dataConsentUrls.add(dataConsent.getUrl()); }
            updateUrlObjects(this.resource, HAS_DATA_CONSENT, dataConsentUrls);
        }

        /**
         * Build the {@link AccessConsent} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}.
         * @return {@link AccessConsent}
         * @throws SaiException
         */
        public AccessConsent build() throws SaiException {
            Objects.requireNonNull(this.grantedBy, "Must provide a URL for the social agent that granted the access consent");
            Objects.requireNonNull(this.grantedWith, "Must provide a URL for the application that was used to grant the access consent");
            Objects.requireNonNull(this.grantedAt, "Must provide the time the access consent was granted at");
            Objects.requireNonNull(this.grantee, "Must provide a URL for the grantee of the access consent");
            Objects.requireNonNull(this.accessNeedGroup, "Must provide a URL for the access need group of the access consent");
            Objects.requireNonNull(this.dataConsents, "Must provide a list of data consents for the access consent");
            if (this.dataset == null) { populateDataset(); }
            return new AccessConsent(this);
        }
    }

}

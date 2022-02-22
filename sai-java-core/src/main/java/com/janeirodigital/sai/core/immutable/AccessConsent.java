package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.crud.AgentRegistration;
import com.janeirodigital.sai.core.crud.AgentRegistry;
import com.janeirodigital.sai.core.crud.DataRegistry;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
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
     * Construct a new {@link AccessConsent}
     * @param url URL of the {@link AccessConsent}
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    private AccessConsent(URL url, DataFactory dataFactory, Model dataset, Resource resource, ContentType contentType,
                          URL grantedBy, URL grantedWith, OffsetDateTime grantedAt, URL grantee, URL accessNeedGroup,
                          URL replaces, List<DataConsent> dataConsents) throws SaiException {
        super(url, dataFactory, false);
        this.dataset = dataset;
        this.resource = resource;
        this.contentType = contentType;
        this.grantedBy = grantedBy;
        this.grantedWith = grantedWith;
        this.grantedAt = grantedAt;
        this.grantee = grantee;
        this.accessNeedGroup = accessNeedGroup;
        this.replaces = replaces;
        this.dataConsents = dataConsents;
    }

    /**
     * Get an {@link AccessConsent} at the provided <code>url</code>
     * @param url URL of the {@link AccessConsent} to get
     * @param dataFactory {@link DataFactory} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link AccessConsent}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static AccessConsent get(URL url, DataFactory dataFactory, ContentType contentType) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(url, "Must provide the URL of the access consent to get");
        Objects.requireNonNull(dataFactory, "Must provide a data factory to assign to the access consent");
        Objects.requireNonNull(contentType, "Must provide a content type for the access consent");
        Builder builder = new Builder(url, dataFactory, contentType);
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(dataFactory.getAuthorizedSession(), dataFactory.getHttpClient(), url, headers))) {
            builder.setDataset(getRdfModelFromResponse(response));
        }
        return builder.build();
    }

    /**
     * Call {@link #get(URL, DataFactory, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link AccessConsent} to get
     * @param dataFactory {@link DataFactory} to assign
     * @return Retrieved {@link AccessConsent}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public static AccessConsent get(URL url, DataFactory dataFactory) throws SaiNotFoundException, SaiException {
        return get(url, dataFactory, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Generate an {@link AccessGrant} and its associated {@link DataGrant}s based on this {@link AccessConsent}.
     * @param granteeRegistration {@link AgentRegistration} for the grantee
     * @param agentRegistry {@link AgentRegistry} for the social agent performing the grant
     * @param dataRegistries List of {@link DataRegistry} instances in scope of the consent
     * @return Generated {@link AccessGrant}
     * @throws SaiException
     */
    public AccessGrant generateGrant(AgentRegistration granteeRegistration, AgentRegistry agentRegistry, List<DataRegistry> dataRegistries) throws SaiException {
        List<DataConsent> primaryDataConsents = new ArrayList<>();
        this.dataConsents.forEach((dataConsent) -> {
            if (!dataConsent.getScopeOfConsent().equals(SCOPE_INHERITED)) { primaryDataConsents.add(dataConsent); }
        });
        List<DataGrant> dataGrants = new ArrayList<>();
        for (DataConsent dataConsent : primaryDataConsents) { dataGrants.addAll(dataConsent.generateGrants()); }
        if (granteeRegistration.hasAccessGrant()) {
            // TODO - If there was a prior access grant, look at reusing some data grants
        }
        URL accessGrantUrl = granteeRegistration.generateAccessGrantUrl();
        AccessGrant.Builder grantBuilder = new AccessGrant.Builder(accessGrantUrl, this.dataFactory, DEFAULT_RDF_CONTENT_TYPE);
        return grantBuilder.setGrantedBy(this.grantedBy).setGrantedAt(this.grantedAt).setGrantee(this.grantee)
                           .setAccessNeedGroup(this.accessNeedGroup).setDataGrants(dataGrants).build();
    }

    /**
     * Builder for {@link AccessConsent} instances.
     */
    public static class Builder {
        private final URL url;
        private final DataFactory dataFactory;
        private final ContentType contentType;
        private Model dataset;
        private Resource resource;
        private URL grantedBy;
        private URL grantedWith;
        private OffsetDateTime grantedAt;
        private URL grantee;
        private URL accessNeedGroup;
        private URL replaces;
        private List<DataConsent> dataConsents;

        /**
         * Initialize builder with <code>url</code>, <code>dataFactory</code>, and desired <code>contentType</code>
         * @param url URL of the {@link AccessConsent} to build
         * @param dataFactory {@link DataFactory} to assign
         * @param contentType {@link ContentType} to assign
         */
        public Builder(URL url, DataFactory dataFactory, ContentType contentType) {
            Objects.requireNonNull(url, "Must provide a URL for the access consent builder");
            Objects.requireNonNull(dataFactory, "Must provide a data factory for the access consent builder");
            Objects.requireNonNull(contentType, "Must provide a content type for the access consent builder");
            this.url = url;
            this.dataFactory = dataFactory;
            this.contentType = contentType;
            this.dataConsents = new ArrayList<>();
        }

        /**
         * Optional Jena Model that will initialize the attributes of the Builder rather than set
         * them manually. Typically used in read scenarios when populating the Builder from
         * the contents of a remote resource.
         * @param dataset Jena model to populate the Builder attributes with
         * @return {@link Builder}
         */
        public Builder setDataset(Model dataset) throws SaiException {
            Objects.requireNonNull(dataset, "Must provide a Jena model for the access consent builder");
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            populateFromDataset();
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
                for (URL dataConsentUrl : dataConsentUrls) { this.dataConsents.add(DataConsent.get(dataConsentUrl, this.dataFactory)); }
            } catch (SaiNotFoundException | SaiException ex) {
                throw new SaiException("Unable to populate immutable access consent resource: " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         * @throws SaiException
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
         * {@link #populateDataset()}. Conversely, if a dataset was provided, the attributes of the
         * Builder will be populated from it.
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
            return new AccessConsent(this.url, this.dataFactory, this.dataset, this.resource, this.contentType, this.grantedBy,
                                    this.grantedWith, this.grantedAt, this.grantee, this.accessNeedGroup,
                                    this.replaces, this.dataConsents);
        }

    }

}

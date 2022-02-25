package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import com.janeirodigital.sai.core.immutable.AccessConsent;
import lombok.Getter;
import lombok.SneakyThrows;
import okhttp3.Headers;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.ACCESS_CONSENT_REGISTRY;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.HAS_ACCESS_CONSENT;

/**
 * Modifiable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#access-consent-registry">Access Consent Registry</a>
 */
@Getter
public class AccessConsentRegistry extends CRUDResource {

    private final AccessConsentList<AccessConsent> accessConsents;

    /**
     * Construct a new {@link AccessConsentRegistry}
     * @throws SaiException
     */
    private AccessConsentRegistry(URL url, DataFactory dataFactory, Model dataset, Resource resource, 
                                  ContentType contentType, AccessConsentList<AccessConsent> accessConsents) throws SaiException {
        super(url, dataFactory, false);
        this.dataset = dataset;
        this.resource = resource;
        this.contentType = contentType;
        this.accessConsents = accessConsents;
    }

    /**
     * Get a {@link AccessConsentRegistry} at the provided <code>url</code>
     * @param url URL of the {@link AccessConsentRegistry} to get
     * @param dataFactory {@link DataFactory} to assign
     * @return Retrieved {@link AccessConsentRegistry}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static AccessConsentRegistry get(URL url, DataFactory dataFactory, ContentType contentType) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(url, "Must provide the URL of the access consent registry to get");
        Objects.requireNonNull(dataFactory, "Must provide a data factory to assign to the access consent registry");
        Objects.requireNonNull(contentType, "Must provide a content type for the access consent registry");
        AccessConsentRegistry.Builder builder = new AccessConsentRegistry.Builder(url, dataFactory, contentType);
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(dataFactory.getAuthorizedSession(), dataFactory.getHttpClient(), url, headers))) {
            builder.setDataset(getRdfModelFromResponse(response));
        }
        return builder.build();
    }

    /**
     * Call {@link #get(URL, DataFactory, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link AccessConsentRegistry}
     * @param dataFactory {@link DataFactory} to assign
     * @return
     */
    public static AccessConsentRegistry get(URL url, DataFactory dataFactory) throws SaiNotFoundException, SaiException {
        return get(url, dataFactory, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Builder for {@link AccessConsentRegistry} instances.
     */
    public static class Builder {

        private final URL url;
        private final DataFactory dataFactory;
        private final ContentType contentType;
        private Model dataset;
        private Resource resource;
        private AccessConsentList<AccessConsent> accessConsents;

        /**
         * Initialize builder with <code>url</code> and <code>dataFactory</code>
         * @param url URL of the {@link AccessConsentRegistry} to build
         * @param dataFactory {@link DataFactory} to assign
         * @param contentType {@link ContentType} to assign
         */
        public Builder(URL url, DataFactory dataFactory, ContentType contentType) {
            Objects.requireNonNull(url, "Must provide a URL for the access consent registry builder");
            Objects.requireNonNull(dataFactory, "Must provide a data factory for the access consent registry builder");
            Objects.requireNonNull(contentType, "Must provide a content type for the access consent registry builder");
            this.url = url;
            this.dataFactory = dataFactory;
            this.contentType = contentType;
            this.accessConsents = new AccessConsentList<>(this.dataFactory, this.resource);
        }

        /**
         * Optional Jena Model that will initialize the attributes of the Builder rather than set
         * them manually. Typically used in read scenarios when populating the Builder from
         * the contents of a remote resource.
         * @param dataset Jena model to populate the Builder attributes with
         * @return {@link Builder}
         */
        public Builder setDataset(Model dataset) throws SaiException {
            Objects.requireNonNull(dataset, "Must provide a Jena model for the access consent registry builder");
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            populateFromDataset();
            return this;
        }

        /**
         * Set the URLs of access consents in the Access Consent Registry (which must have already been created)
         * @param accessConsentUrls List of URLs to {@link AccessConsent} instances
         * @return {@link Builder}
         */
        private Builder setAccessConsentUrls(List<URL> accessConsentUrls) throws SaiAlreadyExistsException {
            Objects.requireNonNull(accessConsentUrls, "Must provide a list of access consent urls to the access consent registry builder");
            this.accessConsents.addAll(accessConsentUrls);
            return this;
        }

        /**
         * Populates the fields of the {@link AccessConsentRegistry} based on the associated Jena resource.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException {
            try {
                this.accessConsents.populate();
            } catch (SaiException ex) {
                throw new SaiException("Failed to load access consent registry " + this.url + ": " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         * @throws SaiException
         */
        private void populateDataset() throws SaiException {
            this.resource = getNewResourceForType(this.url, ACCESS_CONSENT_REGISTRY);
            this.dataset = this.resource.getModel();
            // Note that access consent URLs added via setDataRegistrationUrls are automatically added to the
            // dataset graph, so they don't have to be added here again
        }

        /**
         * Build the {@link AccessConsentRegistry} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}. Conversely, if a dataset was provided, the attributes of the
         * Builder will be populated from it.
         * @return {@link AccessConsentRegistry}
         * @throws SaiException
         */
        public AccessConsentRegistry build() throws SaiException {
            if (this.dataset == null) { populateDataset(); }
            return new AccessConsentRegistry(this.url, this.dataFactory, this.dataset, this.resource, this.contentType, this.accessConsents);
        }

    }

    /**
     * Class for access and iteration of {@link AccessConsent}s. Most of the capability is provided
     * through extension of {@link RegistrationList}, which requires select overrides to ensure the correct
     * types are built and returned by the iterator.
     */
    public static class AccessConsentList<T> extends RegistrationList<T> {

        public AccessConsentList(DataFactory dataFactory, Resource resource) { super(dataFactory, resource, HAS_ACCESS_CONSENT); }

        @Override
        public void add(URL consentUrl) throws SaiException {
            Objects.requireNonNull(consentUrl, "Must provide the URL of the access consent to add to registry");
            // Get the consent to add
            AccessConsent consent;
            try { consent = AccessConsent.get(consentUrl, this.dataFactory); } catch (SaiNotFoundException ex) {
                throw new SaiException("Failed to get access consent at " + consentUrl + ": " + ex.getMessage());
            }
            // Check to see if an access consent for that grantee already exists
            AccessConsent existing = (AccessConsent) find(consent.getGrantee());
            if (existing != null) {
                registrationUrls.remove(existing.getUrl());
                // TODO - this should be handled by access consent builder
                //consent.setReplaces(existing.getUrl());
            }
            registrationUrls.add(consentUrl);  // add the consent to the registry list
            updateUrlObjects(this.resource, this.linkedVia, this.registrationUrls);
        }

        @Override
        public T find(URL granteeUrl) {
            for (T registration : this) {
                AccessConsent consent = (AccessConsent) registration;
                if (granteeUrl.equals(consent.getGrantee())) { return (T) consent; }
            }
            return null;
        }

        @Override
        public Iterator<T> iterator() { return new AccessConsentListIterator(this.getDataFactory(), this.getRegistrationUrls()); }

        private class AccessConsentListIterator<T> extends RegistrationListIterator<T> {
            public AccessConsentListIterator(DataFactory dataFactory, List<URL> registrationUrls) { super(dataFactory, registrationUrls); }
            @SneakyThrows
            @Override
            public T next() {
                URL registrationUrl = current.next();
                return (T) AccessConsent.get(registrationUrl, dataFactory);
            }
        }
    }

}

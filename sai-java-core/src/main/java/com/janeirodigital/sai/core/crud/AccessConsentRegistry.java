package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.exceptions.SaiRuntimeException;
import com.janeirodigital.sai.core.immutable.AccessConsent;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.helpers.HttpHelper.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.core.helpers.HttpHelper.getRdfModelFromResponse;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getNewResourceForType;
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
     * Construct an {@link AccessConsentRegistry} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private AccessConsentRegistry(Builder builder) throws SaiException {
        super(builder);
        this.accessConsents = builder.accessConsents;
    }

    /**
     * Get a {@link AccessConsentRegistry} at the provided <code>url</code>
     * @param url URL of the {@link AccessConsentRegistry} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link AccessConsentRegistry}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static AccessConsentRegistry get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        AccessConsentRegistry.Builder builder = new AccessConsentRegistry.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link AccessConsentRegistry}
     * @param saiSession {@link SaiSession} to assign
     * @return
     */
    public static AccessConsentRegistry get(URL url, SaiSession saiSession) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link AccessConsentRegistry} using the attributes of the current instance
     * @return Reloaded {@link AccessConsentRegistry}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public AccessConsentRegistry reload() throws SaiNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    /**
     * Indicate whether the {@link AccessConsentRegistry} has any {@link AccessConsent}s
     * @return true if there are no access consents
     */
    public boolean isEmpty() {
        return this.accessConsents.isEmpty();
    }

    /**
     * Add an {@link AccessConsent} to the {@link AccessConsentRegistry}. In the event that the
     * {@link AccessConsent} replaces another, the replaced one will be removed first (as it is
     * linked by the one that is replacing it).
     * @param accessConsent {@link AccessConsent} to add
     * @throws SaiException
     * @throws SaiAlreadyExistsException
     */
    public void add(AccessConsent accessConsent) throws SaiException, SaiAlreadyExistsException {
        Objects.requireNonNull(accessConsent, "Cannot add a null access consent to access consent registry");
        if (accessConsent.getReplaces() != null) { this.accessConsents.remove(accessConsent.getReplaces()); }
        AccessConsent found = this.getAccessConsents().find(accessConsent.getGrantee());
        if (found != null) {
            throw new SaiAlreadyExistsException("Access Consent already exists for grantee " + accessConsent.getGrantee() +
                                                " at " + found.getUrl() + " and added consent does not replace it");
        }
        this.getAccessConsents().add(accessConsent.getUrl());
    }

    /**
     * Remove an {@link AccessConsent} from the {@link AccessConsentRegistry}
     * @param accessConsent {@link AccessConsent} to remove
     */
    public void remove(AccessConsent accessConsent) {
        Objects.requireNonNull(accessConsent, "Cannot remove a null access consent from access consent registry");
        this.accessConsents.remove(accessConsent.getUrl());
    }

    /**
     * Builder for {@link AccessConsentRegistry} instances.
     */
    public static class Builder extends CRUDResource.Builder<Builder> {

        private AccessConsentList<AccessConsent> accessConsents;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link AccessConsentRegistry} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URL url, SaiSession saiSession) {
            super(url, saiSession);
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
         * Populates the fields of the {@link AccessConsentRegistry} based on the associated Jena resource.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException {
            try {
                this.accessConsents = new AccessConsentList<>(this.saiSession, this.resource);
                this.accessConsents.populate();
            } catch (SaiException ex) {
                throw new SaiException("Failed to load access consent registry " + this.url + ": " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         * @throws SaiException
         */
        private void populateDataset() {
            this.resource = getNewResourceForType(this.url, ACCESS_CONSENT_REGISTRY);
            this.dataset = this.resource.getModel();
            // Note that access consent URLs added via setDataRegistrationUrls are automatically added to the
            // dataset graph, so they don't have to be added here again
        }

        /**
         * Build the {@link AccessConsentRegistry} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}.
         * @return {@link AccessConsentRegistry}
         * @throws SaiException
         */
        public AccessConsentRegistry build() throws SaiException {
            if (this.dataset == null) { populateDataset(); }
            return new AccessConsentRegistry(this);
        }
    }

    /**
     * Class for access and iteration of {@link AccessConsent}s. Most of the capability is provided
     * through extension of {@link RegistrationList}, which requires select overrides to ensure the correct
     * types are built and returned by the iterator.
     */
    public static class AccessConsentList<T> extends RegistrationList<T> {

        public AccessConsentList(SaiSession saiSession, Resource resource) { super(saiSession, resource, HAS_ACCESS_CONSENT); }

        /**
         * Override the default find in {@link RegistrationList} to lookup based on the grantee of an {@link AccessConsent}
         * @param granteeUrl URL of the grantee to find
         * @return {@link SocialAgentRegistration}
         */
        @Override
        public T find(URL granteeUrl) {
            for (T registration : this) {
                AccessConsent consent = (AccessConsent) registration;
                if (granteeUrl.equals(consent.getGrantee())) { return (T) consent; }
            }
            return null;
        }

        /**
         * Return an iterator for {@link AccessConsent} instances
         * @return {@link AccessConsent} Iterator
         */
        @Override
        public Iterator<T> iterator() { return new AccessConsentListIterator(this.getSaiSession(), this.getRegistrationUrls()); }

        /**
         * Custom iterator that iterates over {@link AccessConsent} URLs and gets actual instances of them
         */
        private class AccessConsentListIterator<T> extends RegistrationListIterator<T> {

            public AccessConsentListIterator(SaiSession saiSession, List<URL> registrationUrls) { super(saiSession, registrationUrls); }

            /**
             * Get the {@link AccessConsent} for the next URL in the iterator
             * @return {@link AccessConsent}
             */
            @Override
            public T next() {
                try {
                    URL registrationUrl = current.next();
                    return (T) AccessConsent.get(registrationUrl, saiSession);
                } catch (SaiException|SaiNotFoundException ex) {
                    throw new SaiRuntimeException("Failed to get access consent while iterating list: " + ex.getMessage());
                }
            }
        }
    }

}

package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.exceptions.SaiRuntimeException;
import com.janeirodigital.sai.core.immutable.AccessAuthorization;
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
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.AUTHORIZATION_REGISTRY;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.HAS_ACCESS_AUTHORIZATION;

/**
 * Modifiable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#authorization-registry">Authorization Registry</a>
 */
@Getter
public class AuthorizationRegistry extends CRUDResource {

    private final AccessAuthorizationList<AccessAuthorization> accessAuthorizations;

    /**
     * Construct an {@link AuthorizationRegistry} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private AuthorizationRegistry(Builder builder) throws SaiException {
        super(builder);
        this.accessAuthorizations = builder.accessAuthorizations;
    }

    /**
     * Get a {@link AuthorizationRegistry} at the provided <code>url</code>
     * @param url URL of the {@link AuthorizationRegistry} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link AuthorizationRegistry}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static AuthorizationRegistry get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        AuthorizationRegistry.Builder builder = new AuthorizationRegistry.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link AuthorizationRegistry}
     * @param saiSession {@link SaiSession} to assign
     * @return
     */
    public static AuthorizationRegistry get(URL url, SaiSession saiSession) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link AuthorizationRegistry} using the attributes of the current instance
     * @return Reloaded {@link AuthorizationRegistry}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public AuthorizationRegistry reload() throws SaiNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    /**
     * Indicate whether the {@link AuthorizationRegistry} has any {@link AccessAuthorization}s
     * @return true if there are no access authorizations
     */
    public boolean isEmpty() {
        return this.accessAuthorizations.isEmpty();
    }

    /**
     * Add an {@link AccessAuthorization} to the {@link AuthorizationRegistry}. In the event that the
     * {@link AccessAuthorization} replaces another, the replaced one will be removed first (as it is
     * linked by the one that is replacing it).
     * @param accessAuthorization {@link AccessAuthorization} to add
     * @throws SaiException
     * @throws SaiAlreadyExistsException
     */
    public void add(AccessAuthorization accessAuthorization) throws SaiException, SaiAlreadyExistsException {
        Objects.requireNonNull(accessAuthorization, "Cannot add a null access authorization to authorization registry");
        if (accessAuthorization.getReplaces() != null) { this.accessAuthorizations.remove(accessAuthorization.getReplaces()); }
        AccessAuthorization found = this.getAccessAuthorizations().find(accessAuthorization.getGrantee());
        if (found != null) {
            throw new SaiAlreadyExistsException("Access Authorization already exists for grantee " + accessAuthorization.getGrantee() +
                                                " at " + found.getUrl() + " and added authorization does not replace it");
        }
        this.getAccessAuthorizations().add(accessAuthorization.getUrl());
    }

    /**
     * Remove an {@link AccessAuthorization} from the {@link AuthorizationRegistry}
     * @param accessAuthorization {@link AccessAuthorization} to remove
     */
    public void remove(AccessAuthorization accessAuthorization) {
        Objects.requireNonNull(accessAuthorization, "Cannot remove a null access authorization from authorization registry");
        this.accessAuthorizations.remove(accessAuthorization.getUrl());
    }

    /**
     * Builder for {@link AuthorizationRegistry} instances.
     */
    public static class Builder extends CRUDResource.Builder<Builder> {

        private AccessAuthorizationList<AccessAuthorization> accessAuthorizations;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link AuthorizationRegistry} to build
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
         * Populates the fields of the {@link AuthorizationRegistry} based on the associated Jena resource.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException {
            try {
                this.accessAuthorizations = new AccessAuthorizationList<>(this.saiSession, this.resource);
                this.accessAuthorizations.populate();
            } catch (SaiException ex) {
                throw new SaiException("Failed to load authorization registry " + this.url + ": " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         * @throws SaiException
         */
        private void populateDataset() {
            this.resource = getNewResourceForType(this.url, AUTHORIZATION_REGISTRY);
            this.dataset = this.resource.getModel();
            // Note that access authorization URLs added via setDataRegistrationUrls are automatically added to the
            // dataset graph, so they don't have to be added here again
        }

        /**
         * Build the {@link AuthorizationRegistry} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}.
         * @return {@link AuthorizationRegistry}
         * @throws SaiException
         */
        public AuthorizationRegistry build() throws SaiException {
            if (this.dataset == null) { populateDataset(); }
            return new AuthorizationRegistry(this);
        }
    }

    /**
     * Class for access and iteration of {@link AccessAuthorization}s. Most of the capability is provided
     * through extension of {@link RegistrationList}, which requires select overrides to ensure the correct
     * types are built and returned by the iterator.
     */
    public static class AccessAuthorizationList<T> extends RegistrationList<T> {

        public AccessAuthorizationList(SaiSession saiSession, Resource resource) { super(saiSession, resource, HAS_ACCESS_AUTHORIZATION); }

        /**
         * Override the default find in {@link RegistrationList} to lookup based on the grantee of an {@link AccessAuthorization}
         * @param granteeUrl URL of the grantee to find
         * @return {@link SocialAgentRegistration}
         */
        @Override
        public T find(URL granteeUrl) {
            for (T registration : this) {
                AccessAuthorization authorization = (AccessAuthorization) registration;
                if (granteeUrl.equals(authorization.getGrantee())) { return (T) authorization; }
            }
            return null;
        }

        /**
         * Return an iterator for {@link AccessAuthorization} instances
         * @return {@link AccessAuthorization} Iterator
         */
        @Override
        public Iterator<T> iterator() { return new AccessAuthorizationListIterator(this.getSaiSession(), this.getRegistrationUrls()); }

        /**
         * Custom iterator that iterates over {@link AccessAuthorization} URLs and gets actual instances of them
         */
        private class AccessAuthorizationListIterator<T> extends RegistrationListIterator<T> {

            public AccessAuthorizationListIterator(SaiSession saiSession, List<URL> registrationUrls) { super(saiSession, registrationUrls); }

            /**
             * Get the {@link AccessAuthorization} for the next URL in the iterator
             * @return {@link AccessAuthorization}
             */
            @Override
            public T next() {
                try {
                    URL registrationUrl = current.next();
                    return (T) AccessAuthorization.get(registrationUrl, saiSession);
                } catch (SaiException|SaiNotFoundException ex) {
                    throw new SaiRuntimeException("Failed to get access authorization while iterating list: " + ex.getMessage());
                }
            }
        }
    }

}

package com.janeirodigital.sai.core.authorizations;

import com.janeirodigital.sai.core.agents.SocialAgentRegistration;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiRuntimeException;
import com.janeirodigital.sai.core.resources.CRUDResource;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.core.utils.RegistrationList;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.AUTHORIZATION_REGISTRY;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.HAS_ACCESS_AUTHORIZATION;
import static com.janeirodigital.sai.httputils.HttpUtils.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.rdfutils.RdfUtils.getNewResourceForType;

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
     * Get a {@link AuthorizationRegistry} at the provided <code>uri</code>
     * @param uri URI of the {@link AuthorizationRegistry} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link AuthorizationRegistry}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static AuthorizationRegistry get(URI uri, SaiSession saiSession, ContentType contentType) throws SaiException, SaiHttpNotFoundException {
        Builder builder = new Builder(uri, saiSession);
        try (Response response = read(uri, saiSession, contentType, false)) {
            return builder.setDataset(response).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URI, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param uri URI of the {@link AuthorizationRegistry}
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link AuthorizationRegistry}
     */
    public static AuthorizationRegistry get(URI uri, SaiSession saiSession) throws SaiHttpNotFoundException, SaiException {
        return get(uri, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link AuthorizationRegistry} using the attributes of the current instance
     * @return Reloaded {@link AuthorizationRegistry}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public AuthorizationRegistry reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.uri, this.saiSession, this.contentType);
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
                                                " at " + found.getUri() + " and added authorization does not replace it");
        }
        this.getAccessAuthorizations().add(accessAuthorization.getUri());
    }

    /**
     * Remove an {@link AccessAuthorization} from the {@link AuthorizationRegistry}
     * @param accessAuthorization {@link AccessAuthorization} to remove
     */
    public void remove(AccessAuthorization accessAuthorization) {
        Objects.requireNonNull(accessAuthorization, "Cannot remove a null access authorization from authorization registry");
        this.accessAuthorizations.remove(accessAuthorization.getUri());
    }

    /**
     * Builder for {@link AuthorizationRegistry} instances.
     */
    public static class Builder extends CRUDResource.Builder<Builder> {

        private AccessAuthorizationList<AccessAuthorization> accessAuthorizations;

        /**
         * Initialize builder with <code>uri</code> and <code>saiSession</code>
         * @param uri URI of the {@link AuthorizationRegistry} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URI uri, SaiSession saiSession) {
            super(uri, saiSession);
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
                throw new SaiException("Failed to load authorization registry " + this.uri, ex);
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        private void populateDataset() {
            this.resource = getNewResourceForType(this.uri, AUTHORIZATION_REGISTRY);
            this.dataset = this.resource.getModel();
            // Note that access authorization URIs added via setDataRegistrationUris are automatically added to the
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
         * @param granteeUri URI of the grantee to find
         * @return {@link SocialAgentRegistration}
         */
        @Override
        public T find(URI granteeUri) {
            for (T registration : this) {
                AccessAuthorization authorization = (AccessAuthorization) registration;
                if (granteeUri.equals(authorization.getGrantee())) { return registration; }
            }
            return null;
        }

        /**
         * Return an iterator for {@link AccessAuthorization} instances
         * @return {@link AccessAuthorization} Iterator
         */
        @Override
        public Iterator<T> iterator() { return new AccessAuthorizationListIterator<>(this.getSaiSession(), this.getRegistrationUris()); }

        /**
         * Custom iterator that iterates over {@link AccessAuthorization} URIs and gets actual instances of them
         */
        private static class AccessAuthorizationListIterator<T> extends RegistrationListIterator<T> {

            public AccessAuthorizationListIterator(SaiSession saiSession, List<URI> registrationUris) { super(saiSession, registrationUris); }

            /**
             * Get the {@link AccessAuthorization} for the next URI in the iterator
             * @return {@link AccessAuthorization}
             */
            @Override
            public T next() {
                try {
                    URI registrationUri = current.next();
                    return (T) AccessAuthorization.get(registrationUri, saiSession);
                } catch (SaiException | SaiHttpNotFoundException ex) {
                    throw new SaiRuntimeException("Failed to get access authorization while iterating list", ex);
                }
            }
        }
    }

}

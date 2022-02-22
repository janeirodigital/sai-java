package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import com.janeirodigital.sai.core.immutable.AccessConsent;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.contexts.InteropContext.INTEROP_CONTEXT;
import static com.janeirodigital.sai.core.helpers.HttpHelper.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

/**
 * Modifiable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#access-consent-registry">Access Consent Registry</a>
 */
@Getter
public class AccessConsentRegistry extends CRUDResource {

    private AccessConsentList<AccessConsent> accessConsents;

    /**
     * Construct a new {@link AccessConsentRegistry}
     * @param url URL of the {@link AccessConsentRegistry}
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    public AccessConsentRegistry(URL url, DataFactory dataFactory) throws SaiException {
        super(url, dataFactory, false);
        this.jsonLdContext = buildRemoteJsonLdContext(INTEROP_CONTEXT);
    }

    /**
     * Builder used by a {@link DataFactory} to construct and initialize a {@link AccessConsentRegistry}.
     * If a Jena <code>resource</code> is provided and there is already a {@link AccessConsentRegistry}
     * at the provided <code>url</code>, the graph of the provided resource will be used. The remote graph
     * will not be updated until {@link #update()} is called.
     * @param url URL of the {@link AccessConsentRegistry} to build
     * @param dataFactory {@link DataFactory} to assign
     * @param contentType {@link ContentType} to use for read / write
     * @param resource Optional Jena Resource to populate the resource graph
     * @return {@link AccessConsentRegistry}
     * @throws SaiException
     */
    public static AccessConsentRegistry build(URL url, DataFactory dataFactory, ContentType contentType, Resource resource) throws SaiException {
        AccessConsentRegistry consentRegistry = new AccessConsentRegistry(url, dataFactory);
        consentRegistry.contentType = contentType;
        if (resource != null) {
            consentRegistry.resource = resource;
            consentRegistry.dataset = resource.getModel();
        }
        consentRegistry.bootstrap();
        return consentRegistry;
    }

    /**
     * Calls {@link #build(URL, DataFactory, ContentType, Resource)} to construct a {@link AccessConsentRegistry} with
     * no Jena resource provided and the specified content type (e.g. JSON-LD).
     * @param url URL of the {@link AccessConsentRegistry} to build
     * @param dataFactory {@link DataFactory} to assign
     * @return {@link AccessConsentRegistry}
     * @throws SaiException
     */
    public static AccessConsentRegistry build(URL url, DataFactory dataFactory, ContentType contentType) throws SaiException {
        return build(url, dataFactory, contentType, null);
    }

    /**
     * Calls {@link #build(URL, DataFactory, ContentType, Resource)} to construct a {@link AccessConsentRegistry} with
     * no Jena resource provided and the default content type.
     * @param url URL of the {@link AccessConsentRegistry} to build
     * @param dataFactory {@link DataFactory} to assign
     * @return {@link AccessConsentRegistry}
     * @throws SaiException
     */
    public static AccessConsentRegistry build(URL url, DataFactory dataFactory) throws SaiException {
        return build(url, dataFactory, DEFAULT_RDF_CONTENT_TYPE, null);
    }

    /**
     * Bootstraps the {@link AccessConsentRegistry}. If a Jena Resource was provided, it will
     * be used to populate the instance. If not, the remote resource will be fetched and
     * populated. If the remote resource doesn't exist, a local graph will be created for it,
     * which includes initializing the access consent list.
     * @throws SaiException
     */
    private void bootstrap() throws SaiException {
        if (this.resource != null) { populate(); } else {
            try {
                // Fetch the remote resource and populate
                this.fetchData();
                populate();
            } catch (SaiNotFoundException ex) {
                // Remote resource didn't exist, initialize one
                this.resource = getNewResourceForType(this.url, ACCESS_CONSENT_REGISTRY);
                this.dataset = resource.getModel();
                this.accessConsents = new AccessConsentList<>(this.getDataFactory(), this.getResource());
            }
        }
    }

    /**
     * Populates the {@link AccessConsentRegistry} instance with required and optional fields
     * @throws SaiException If any required fields cannot be populated, or other exceptional conditions
     */
    private void populate() throws SaiException {
        this.accessConsents = new AccessConsentList<>(this.getDataFactory(), this.getResource());
        this.accessConsents.populate();
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

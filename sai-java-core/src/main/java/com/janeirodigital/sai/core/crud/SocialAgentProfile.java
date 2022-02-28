package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import okhttp3.Headers;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.SolidTermsVocabulary.SOLID_OIDC_ISSUER;

/**
 * Modifiable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Social Agent</a>
 * profile, which is also cross-pollinated with other terms from the Solid ecosystem.
 */
@Getter
public class SocialAgentProfile extends CRUDResource {

    private final URL registrySetUrl;
    private final URL authorizationAgentUrl;
    private final URL accessInboxUrl;
    private final List<URL> oidcIssuerUrls;

    /**
     * Construct a new {@link SocialAgentProfile}
     * @param url URL of the {@link SocialAgentProfile}
     * @param saiSession {@link SaiSession} to assign
     * @throws SaiException
     */
    public SocialAgentProfile(URL url, SaiSession saiSession, Model dataset, Resource resource, ContentType contentType,
                              URL registrySetUrl, URL authorizationAgentUrl, URL accessInboxUrl, List<URL> oidcIssuerUrls) throws SaiException {
        super(url, saiSession, false);
        this.dataset = dataset;
        this.resource = resource;
        this.contentType = contentType;
        this.registrySetUrl = registrySetUrl;
        this.authorizationAgentUrl = authorizationAgentUrl;
        this.accessInboxUrl = accessInboxUrl;
        this.oidcIssuerUrls = oidcIssuerUrls;
    }

    /**
     * Get a {@link SocialAgentProfile} at the provided <code>url</code>
     * @param url URL of the {@link SocialAgentProfile} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link SocialAgentProfile}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static SocialAgentProfile get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(url, "Must provide the URL of the social agent profile to get");
        Objects.requireNonNull(saiSession, "Must provide a sai session to assign to the social agent profile");
        Objects.requireNonNull(contentType, "Must provide a content type for the social agent profile");
        SocialAgentProfile.Builder builder = new SocialAgentProfile.Builder(url, saiSession, contentType);
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(saiSession.getAuthorizedSession(), saiSession.getHttpClient(), url, headers))) {
            builder.setDataset(getRdfModelFromResponse(response));
        }
        return builder.build();
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link SocialAgentProfile}
     * @param saiSession {@link SaiSession} to assign
     * @return
     */
    public static SocialAgentProfile get(URL url, SaiSession saiSession) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }
    
    /**
     * Builder for {@link SocialAgentProfile} instances.
     */
    public static class Builder {

        private final URL url;
        private final SaiSession saiSession;
        private final ContentType contentType;
        private Model dataset;
        private Resource resource;
        URL registrySetUrl;
        URL authorizationAgentUrl;
        URL accessInboxUrl;
        List<URL> oidcIssuerUrls;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link SocialAgentProfile} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URL url, SaiSession saiSession, ContentType contentType) {
            Objects.requireNonNull(url, "Must provide a URL for the social agent profile builder");
            Objects.requireNonNull(saiSession, "Must provide a sai session for the social agent profile builder");
            this.url = url;
            this.saiSession = saiSession;
            this.contentType = contentType;
        }

        /**
         * Optional Jena Model that will initialize the attributes of the Builder rather than set
         * them manually. Typically used in read scenarios when populating the Builder from
         * the contents of a remote resource.
         * @param dataset Jena model to populate the Builder attributes with
         * @return {@link Builder}
         */
        public Builder setDataset(Model dataset) throws SaiException {
            Objects.requireNonNull(dataset, "Must provide a Jena model for the social agent profile builder");
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            populateFromDataset();
            return this;
        }

        /**
         * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Registry Set</a>
         * for the Social Agent.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#datamodel-registry-set">Registry Set</a>
         * @param registrySetUrl URL of the social agent's registry set resource
         */
        public Builder setRegistrySet(URL registrySetUrl) {
            Objects.requireNonNull(registrySetUrl, "Must provide a registry set for the social agent");
            this.registrySetUrl = registrySetUrl;
            return this;
        }

        /**
         * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Authorization Agent</a>
         * for the Social Agent.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#authorization-agent">Authorization Agent</a>
         * @param authorizationAgentUrl URL of the social agent's authorization agent
         */
        public Builder setAuthorizationAgent(URL authorizationAgentUrl) {
            Objects.requireNonNull(authorizationAgentUrl, "Must provide an authorization agent for the social agent");
            this.authorizationAgentUrl = authorizationAgentUrl;
            return this;
        }

        /**
         * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Access Inbox</a>
         * for the Social Agent.
         * @param accessInboxUrl URL of the social agent's access inbox
         */
        public Builder setAccessInbox(URL accessInboxUrl) {
            Objects.requireNonNull(accessInboxUrl, "Must provide an access inbox for the social agent");
            this.accessInboxUrl = accessInboxUrl;
            return this;
        }

        /**
         * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">OpenID Connect Issuers</a>
         * for the Social Agent
         * @see <a href="https://solid.github.io/solid-oidc/#resource-access-validation">Solid-OIDC - ID Token Validation</a>
         * @param oidcIssuerUrls List of URLs of OIDC Issuers
         */
        public Builder setOidcIssuerUrls(List<URL> oidcIssuerUrls) {
            Objects.requireNonNull(oidcIssuerUrls, "Must provide oidc issuer urls for the social agent");
            this.oidcIssuerUrls = oidcIssuerUrls;
            return this;
        }

        /**
         * Populates the fields of the {@link SocialAgentProfile} based on the associated Jena resource.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException {
            try {
                this.registrySetUrl = getRequiredUrlObject(this.resource, HAS_REGISTRY_SET);
                this.authorizationAgentUrl = getRequiredUrlObject(this.resource, HAS_AUTHORIZATION_AGENT);
                this.accessInboxUrl = getRequiredUrlObject(this.resource, HAS_ACCESS_INBOX);
                this.oidcIssuerUrls = getRequiredUrlObjects(this.resource, SOLID_OIDC_ISSUER);
            } catch (SaiNotFoundException ex) {
                throw new SaiException("Failed to load social agent profile " + this.url + ": " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         * @throws SaiException
         */
        private void populateDataset() throws SaiException {
            this.resource = getNewResourceForType(this.url, SOCIAL_AGENT);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, HAS_REGISTRY_SET, registrySetUrl);
            updateObject(this.resource, HAS_AUTHORIZATION_AGENT, authorizationAgentUrl);
            updateObject(this.resource, HAS_ACCESS_INBOX, accessInboxUrl);
            updateUrlObjects(this.resource, SOLID_OIDC_ISSUER, this.oidcIssuerUrls);
        }

        /**
         * Build the {@link ApplicationProfile} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}. Conversely, if a dataset was provided, the attributes of the
         * Builder will be populated from it.
         * @return {@link ApplicationProfile}
         * @throws SaiException
         */
        public SocialAgentProfile build() throws SaiException {
            Objects.requireNonNull(registrySetUrl, "Must provide a registry set for the social agent");
            Objects.requireNonNull(authorizationAgentUrl, "Must provide an authorization agent for the social agent");
            Objects.requireNonNull(accessInboxUrl, "Must provide an access inbox for the social agent");
            Objects.requireNonNull(oidcIssuerUrls, "Must provide oidc issuer urls for the social agent");
            if (this.dataset == null) { populateDataset(); }
            return new SocialAgentProfile(this.url, this.saiSession, this.dataset, this.resource, this.contentType,
                                          this.registrySetUrl, this.authorizationAgentUrl, this.accessInboxUrl, this.oidcIssuerUrls);                   
        }

    }

}

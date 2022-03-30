package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.sai.rdfutils.SaiRdfNotFoundException;
import lombok.Getter;
import lombok.Setter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URL;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.SolidTermsVocabulary.SOLID_OIDC_ISSUER;
import static com.janeirodigital.sai.httputils.HttpUtils.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.rdfutils.RdfUtils.*;

/**
 * Modifiable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Social Agent</a>
 * profile, which is also cross-pollinated with other terms from the Solid ecosystem.
 */
@Getter @Setter
public class SocialAgentProfile extends CRUDResource {

    private URL registrySetUrl;
    private URL authorizationAgentUrl;
    private URL accessInboxUrl;
    private List<URL> oidcIssuerUrls;

    /**
     * Construct a {@link SocialAgentProfile} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private SocialAgentProfile(Builder builder) throws SaiException {
        super(builder);
        this.registrySetUrl = builder.registrySetUrl;
        this.authorizationAgentUrl = builder.authorizationAgentUrl;
        this.accessInboxUrl = builder.accessInboxUrl;
        this.oidcIssuerUrls = builder.oidcIssuerUrls;
    }

    /**
     * Get a {@link SocialAgentProfile} at the provided <code>url</code>
     * @param url URL of the {@link SocialAgentProfile} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link SocialAgentProfile}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static SocialAgentProfile get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiHttpNotFoundException {
        SocialAgentProfile.Builder builder = new SocialAgentProfile.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(response).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link SocialAgentProfile}
     * @param saiSession {@link SaiSession} to assign
     * @return
     */
    public static SocialAgentProfile get(URL url, SaiSession saiSession) throws SaiHttpNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link SocialAgentProfile} using the attributes of the current instance
     * @return Reloaded {@link SocialAgentProfile}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public SocialAgentProfile reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    /**
     * Builder for {@link SocialAgentProfile} instances.
     */
    public static class Builder extends CRUDResource.Builder<Builder> {

        URL registrySetUrl;
        URL authorizationAgentUrl;
        URL accessInboxUrl;
        List<URL> oidcIssuerUrls;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link SocialAgentProfile} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URL url, SaiSession saiSession) { super(url, saiSession); }

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
            } catch (SaiRdfException | SaiRdfNotFoundException ex) {
                throw new SaiException("Failed to load social agent profile " + this.url, ex);
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        private void populateDataset() {
            this.resource = getNewResourceForType(this.url, SOCIAL_AGENT);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, HAS_REGISTRY_SET, registrySetUrl);
            updateObject(this.resource, HAS_AUTHORIZATION_AGENT, authorizationAgentUrl);
            updateObject(this.resource, HAS_ACCESS_INBOX, accessInboxUrl);
            updateUrlObjects(this.resource, SOLID_OIDC_ISSUER, this.oidcIssuerUrls);
        }

        /**
         * Build the {@link SocialAgentProfile} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}.
         * @return {@link SocialAgentProfile}
         * @throws SaiException
         */
        public SocialAgentProfile build() throws SaiException {
            Objects.requireNonNull(registrySetUrl, "Must provide a registry set for the social agent");
            Objects.requireNonNull(authorizationAgentUrl, "Must provide an authorization agent for the social agent");
            Objects.requireNonNull(accessInboxUrl, "Must provide an access inbox for the social agent");
            Objects.requireNonNull(oidcIssuerUrls, "Must provide oidc issuer urls for the social agent");
            if (this.dataset == null) { populateDataset(); }
            return new SocialAgentProfile(this);
        }

    }

}

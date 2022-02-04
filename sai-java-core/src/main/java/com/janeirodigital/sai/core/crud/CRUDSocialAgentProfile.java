package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.Getter;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.contexts.InteropContexts.SOCIAL_AGENT_PROFILE_CONTEXT;
import static com.janeirodigital.sai.core.enums.ContentType.TEXT_TURTLE;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.SolidTermsVocabulary.SOLID_OIDC_ISSUER;

/**
 * Modifiable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Social Agent</a>
 * profile, which is also cross-pollinated with other terms from the Solid ecosystem.
 */
@Getter
public class CRUDSocialAgentProfile extends CRUDResource {

    URL registrySetUrl;
    URL authorizationAgentUrl;
    URL accessInboxUrl;
    List<URL> oidcIssuerUrls;

    /**
     * Construct a new {@link CRUDSocialAgentProfile}
     * @param url URL of the {@link CRUDSocialAgentProfile}
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    public CRUDSocialAgentProfile(URL url, DataFactory dataFactory) throws SaiException {
        super(url, dataFactory);
        this.oidcIssuerUrls = new ArrayList<>();
        this.jsonLdContext = buildRemoteJsonLdContext(SOCIAL_AGENT_PROFILE_CONTEXT);
    }

    /**
     * Builder used by a {@link DataFactory} to construct and initialize a {@link CRUDSocialAgentProfile}.
     * If a Jena <code>resource</code> is provided and there is already a {@link CRUDSocialAgentProfile}
     * at the provided <code>url</code>, the graph of the provided resource will be used. The remote graph
     * will not be updated until {@link #update()} is called.
     * @param url URL of the {@link CRUDSocialAgentProfile} to build
     * @param dataFactory {@link DataFactory} to assign
     * @param resource Optional Jena Resource to populate the resource graph
     * @return {@link CRUDSocialAgentProfile}
     * @throws SaiException
     */
    public static CRUDSocialAgentProfile build(URL url, DataFactory dataFactory, ContentType contentType, Resource resource) throws SaiException {
        CRUDSocialAgentProfile profile = new CRUDSocialAgentProfile(url, dataFactory);
        profile.contentType = contentType;
        if (resource != null) {
            profile.resource = resource;
            profile.dataset = resource.getModel();
        }
        profile.bootstrap();
        return profile;
    }

    /**
     * Calls {@link #build(URL, DataFactory, ContentType, Resource)} to construct a {@link CRUDSocialAgentProfile} with
     * no Jena resource provided and the specified content type (e.g. JSON-LD).
     * @param url URL of the {@link CRUDSocialAgentProfile} to build
     * @param dataFactory {@link DataFactory} to assign
     * @return {@link CRUDSocialAgentProfile}
     * @throws SaiException
     */
    public static CRUDSocialAgentProfile build(URL url, DataFactory dataFactory, ContentType contentType) throws SaiException {
        return build(url, dataFactory, contentType, null);
    }

    /**
     * Calls {@link #build(URL, DataFactory, ContentType, Resource)} to construct a {@link CRUDSocialAgentProfile} with
     * no Jena resource provided and the default content type.
     * @param url URL of the {@link CRUDSocialAgentProfile} to build
     * @param dataFactory {@link DataFactory} to assign
     * @return {@link CRUDSocialAgentProfile}
     * @throws SaiException
     */
    public static CRUDSocialAgentProfile build(URL url, DataFactory dataFactory) throws SaiException {
        return build(url, dataFactory, TEXT_TURTLE, null);
    }

    /**
     * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Registry Set</a>
     * for the Social Agent.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#datamodel-registry-set">Registry Set</a>
     * @param registrySetUrl URL of the social agent's registry set resource
     */
    public void setRegistrySet(URL registrySetUrl) {
        Objects.requireNonNull(registrySetUrl, "Must provide a registry set for the social agent");
        this.registrySetUrl = registrySetUrl;
        updateObject(this.resource, HAS_REGISTRY_SET, registrySetUrl);
    }

    /**
     * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Authorization Agent</a>
     * for the Social Agent.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#authorization-agent">Authorization Agent</a>
     * @param authorizationAgentUrl URL of the social agent's authorization agent
     */
    public void setAuthorizationAgent(URL authorizationAgentUrl) {
        Objects.requireNonNull(authorizationAgentUrl, "Must provide an authorization agent for the social agent");
        this.authorizationAgentUrl = authorizationAgentUrl;
        updateObject(this.resource, HAS_AUTHORIZATION_AGENT, authorizationAgentUrl);
    }

    /**
     * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Access Inbox</a>
     * for the Social Agent.
     * @param accessInboxUrl URL of the social agent's access inbox
     */
    public void setAccessInbox(URL accessInboxUrl) {
        Objects.requireNonNull(accessInboxUrl, "Must provide an access inbox for the social agent");
        this.accessInboxUrl = accessInboxUrl;
        updateObject(this.resource, HAS_ACCESS_INBOX, accessInboxUrl);
    }

    /**
     * Add an <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">OpenID Connect Issuer</a>
     * for the Social Agent
     * @see <a href="https://solid.github.io/solid-oidc/#resource-access-validation">Solid-OIDC - ID Token Validation</a>
     * @param oidcIssuerUrl URL of OIDC Issuer
     */
    public void addOidcIssuerUrl(URL oidcIssuerUrl) {
        Objects.requireNonNull(oidcIssuerUrl, "Must provide an oidc issuer for the social agent");
        this.oidcIssuerUrls.add(oidcIssuerUrl);
        updateUrlObjects(this.resource, SOLID_OIDC_ISSUER, this.oidcIssuerUrls);
    }

    /**
     * Bootstraps the {@link CRUDSocialAgentProfile}. If a Jena Resource was provided, it will
     * be used to populate the instance. If not, the remote resource will be fetched and
     * populated. If the remote resource doesn't exist, a local graph will be created for it.
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
                this.resource = getNewResourceForType(this.url, SOCIAL_AGENT);
                this.dataset = resource.getModel();
            }
        }
    }

    /**
     * Populates the {@link CRUDSocialAgentProfile} instance with required and optional fields
     * @throws SaiException If any required fields cannot be populated, or other exceptional conditions
     */
    private void populate() throws SaiException {
        try {
            this.registrySetUrl = getRequiredUrlObject(this.resource, HAS_REGISTRY_SET);
            this.authorizationAgentUrl = getRequiredUrlObject(this.resource, HAS_AUTHORIZATION_AGENT);
            this.accessInboxUrl = getRequiredUrlObject(this.resource, HAS_ACCESS_INBOX);
            this.oidcIssuerUrls = getRequiredUrlObjects(this.resource, SOLID_OIDC_ISSUER);
        } catch (SaiNotFoundException ex) {
            throw new SaiException("Failed to load social agent profile " + this.url + ": " + ex.getMessage());
        }
    }

}

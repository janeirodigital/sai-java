package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.Getter;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.contexts.InteropContexts.APPLICATION_PROFILE_CONTEXT;
import static com.janeirodigital.sai.core.contexts.SolidOidcContexts.SOLID_OIDC_CONTEXT;
import static com.janeirodigital.sai.core.enums.ContentType.LD_JSON;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.SolidOidcVocabulary.*;

/**
 * Modifiable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#app">Application</a>,
 * which is also cross-pollinated with the
 * <a href="https://solid.github.io/solid-oidc/#clientids-document">Client Identifier Document</a>
 * from Solid-OIDC.
 */
@Getter
public class CRUDApplicationProfile extends CRUDResource {

    private static final List<String> contexts = Arrays.asList(SOLID_OIDC_CONTEXT, APPLICATION_PROFILE_CONTEXT);

    private String name;
    private String description;
    private URL authorUrl;
    private URL logoUrl;
    private List<URL> accessNeedGroupUrls;
    // Solid-OIDC specific
    private List<URL> redirectUrls;
    private URL clientUrl;
    private URL tosUrl;
    private List<String> scopes;
    private List<String> grantTypes;
    private List<String> responseTypes;
    private int defaultMaxAge;
    private boolean requireAuthTime;

    /**
     * Construct a new {@link CRUDApplicationProfile}
     * @param url URL of the {@link CRUDApplicationProfile}
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    public CRUDApplicationProfile(URL url, DataFactory dataFactory) throws SaiException {
        super(url, dataFactory);
        this.accessNeedGroupUrls = new ArrayList<>();
        this.redirectUrls = new ArrayList<>();
        this.scopes = new ArrayList<>();
        this.grantTypes = new ArrayList<>();
        this.responseTypes = new ArrayList<>();
        // By default the application profile document is JSON-LD
        this.setContentType(LD_JSON);
        this.setJsonLdContext(buildRemoteJsonLdContexts(contexts));
    }

    /**
     * Builder used by a {@link DataFactory} to construct and initialize a {@link CRUDApplicationProfile}.
     * If a Jena <code>resource</code> is provided and there is already a {@link CRUDApplicationProfile}
     * at the provided <code>url</code>, the graph of the provided resource will be used. The remote graph
     * will not be updated until {@link #update()} is called.
     * @param url URL of the {@link CRUDApplicationProfile} to build
     * @param dataFactory {@link DataFactory} to assign
     * @param resource Optional Jena Resource to populate the resource graph
     * @return {@link CRUDApplicationProfile}
     * @throws SaiException
     */
    public static CRUDApplicationProfile build(URL url, DataFactory dataFactory, Resource resource) throws SaiException {
        CRUDApplicationProfile profile = new CRUDApplicationProfile(url, dataFactory);
        if (resource != null) {
            profile.resource = resource;
            profile.dataset = resource.getModel();
        }
        profile.bootstrap();
        return profile;
    }

    /**
     * Calls {@link #build(URL, DataFactory, Resource)} to construct a {@link CRUDApplicationProfile} with
     * no Jena resource provided.
     * @param url URL of the {@link CRUDApplicationProfile} to build
     * @param dataFactory {@link DataFactory} to assign
     * @return {@link CRUDApplicationProfile}
     * @throws SaiException
     */
    public static CRUDApplicationProfile build(URL url, DataFactory dataFactory) throws SaiException {
        return build(url, dataFactory, null);
    }

    /**
     * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#app">name</a>
     * of the application in the local resource graph
     * @param name name of application
     */
    public void setName(String name) {
        Objects.requireNonNull(name, "Must provide a name of the application");
        this.name = name;
        updateObject(this.resource, SOLID_OIDC_CLIENT_NAME, name);
    }

    /**
     * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#app">description</a>
     * of the application in the local resource graph
     * @param description description of application
     */
    public void setDescription(String description) {
        Objects.requireNonNull(description, "Must provide a description of the application");
        this.description = description;
        updateObject(this.resource, APPLICATION_DESCRIPTION, description);
    }

    /**
     * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#app">author</a>
     * of the application in the local resource graph
     * @param authorUrl URL of application author
     */
    public void setAuthorUrl(URL authorUrl) {
        Objects.requireNonNull(authorUrl, "Must provide an author of the application");
        this.authorUrl = authorUrl;
        updateObject(this.resource, APPLICATION_AUTHOR, authorUrl);
    }

    /**
     * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#app">thumbnail</a>
     * of the application in the local resource graph
     * @param logoUrl URL of application thumbnail
     */
    public void setLogoUrl(URL logoUrl) {
        Objects.requireNonNull(logoUrl, "Must provide a logo for the application");
        this.logoUrl = logoUrl;
        updateObject(this.resource, SOLID_OIDC_LOGO_URI, logoUrl);
    }

    /**
     * Add an <a href="https://solid.github.io/data-interoperability-panel/specification/#app">access need group</a>
     * requested by the application in the local resource graph
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#access-need-group">Access Need Group</a>
     * @param accessNeedGroupUrl URL of access need group
     */
    public void addAccessNeedGroupUrl(URL accessNeedGroupUrl) {
        Objects.requireNonNull(accessNeedGroupUrl, "Must provide an access need group for the application");
        this.accessNeedGroupUrls.add(accessNeedGroupUrl);
        updateUrlObjects(this.resource, HAS_ACCESS_NEED_GROUP, this.accessNeedGroupUrls);
    }

    /**
     * Add to Solid-OIDC <a href="https://solid.github.io/solid-oidc/#clientids-document">redirect_uris</a> with
     * the provided URI;
     * @param redirectUrl URI of redirect
     */
    public void addRedirectUrl(URL redirectUrl) {
        Objects.requireNonNull(redirectUrl, "Must provide a redirect uri for solid-oidc");
        this.redirectUrls.add(redirectUrl);
        updateUrlObjects(this.resource, SOLID_OIDC_REDIRECT_URIS, this.redirectUrls);
    }

    /**
     * Set the <a href="https://solid.github.io/solid-oidc/#clientids-document">client_uri</a>
     * of the Solid-OIDC client identifier document
     * @param clientUrl client URI
     */
    public void setClientUrl(URL clientUrl) {
        Objects.requireNonNull(clientUrl, "Must provide a client uri for solid-oidc");
        this.clientUrl = clientUrl;
        updateObject(this.resource, SOLID_OIDC_CLIENT_URI, clientUrl);
    }

    /**
     * Set the <a href="https://solid.github.io/solid-oidc/#clientids-document">tos_url</a>
     * of the Solid-OIDC client identifier document
     * @param tosUrl Terms of service URL
     */
    public void setTosUrl(URL tosUrl) {
        Objects.requireNonNull(tosUrl, "Must provide a terms of service uri for solid-oidc");
        this.tosUrl = tosUrl;
        updateObject(this.resource, SOLID_OIDC_TOS_URI, tosUrl);
    }

    /**
     * Add to Solid-OIDC <a href="https://solid.github.io/solid-oidc/#clientids-document">scopes</a> with
     * the provided scope
     * @param scope Scope to add
     */
    public void addScope(String scope) {
        Objects.requireNonNull(scope, "Must provide a scope for solid-oidc");
        this.scopes.add(scope);
        updateObject(this.resource, SOLID_OIDC_SCOPE, String.join(" ", this.scopes));
    }

    /**
     * Add to Solid-OIDC <a href="https://solid.github.io/solid-oidc/#clientids-document">grant_types</a> with
     * the provided grant type;
     * @param grantType Grant type to add
     */
    public void addGrantType(String grantType) {
        Objects.requireNonNull(grantType, "Must provide a grant type for solid-oidc");
        this.grantTypes.add(grantType);
        updateStringObjects(this.resource, SOLID_OIDC_GRANT_TYPES, this.grantTypes);
    }

    /**
     * Add to Solid-OIDC <a href="https://solid.github.io/solid-oidc/#clientids-document">response_types</a> with
     * the provided response type;
     * @param responseType Response type to add
     */
    public void addResponseType(String responseType) {
        Objects.requireNonNull(responseType, "Must provide a response type for solid-oidc");
        this.responseTypes.add(responseType);
        updateStringObjects(this.resource, SOLID_OIDC_RESPONSE_TYPES, this.responseTypes);
    }

    /**
     * Set the <a href="https://solid.github.io/solid-oidc/#clientids-document">default_max_age</a>
     * for the Solid-OIDC client identifier document
     * @param defaultMaxAge Default max age
     */
    public void setDefaultMaxAge(int defaultMaxAge) {
        Objects.requireNonNull(defaultMaxAge, "Must provide a default max age for solid-oidc");
        this.defaultMaxAge = defaultMaxAge;
        updateObject(this.resource, SOLID_OIDC_DEFAULT_MAX_AGE, defaultMaxAge);
    }

    /**
     * Set <a href="https://solid.github.io/solid-oidc/#clientids-document">require_auth_time</a>
     * for the Solid-OIDC client identifier document
     * @param requireAuthTime Require auth time
     */
    public void setRequireAuthTime(boolean requireAuthTime) {
        Objects.requireNonNull(requireAuthTime, "Must provide an auth time requirement for solid-oidc");
        this.requireAuthTime = requireAuthTime;
        updateObject(this.resource, SOLID_OIDC_REQUIRE_AUTH_TIME, requireAuthTime);
    }

    /**
     * Bootstraps the {@link CRUDApplicationProfile}. If a Jena Resource was provided, it will
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
                this.resource = getNewResourceForType(this.url, APPLICATION);
                this.dataset = resource.getModel();
            }
        }
    }

    /**
     * Populates the {@link CRUDApplicationProfile} instance with required and optional
     * fields for the application profile.
     * @throws SaiException If any required fields cannot be populated, or other exceptional conditions
     */
    private void populate() throws SaiException {
        try {
            this.name = getRequiredStringObject(this.resource, SOLID_OIDC_CLIENT_NAME);
            this.description = getRequiredStringObject(this.resource, APPLICATION_DESCRIPTION);
            this.authorUrl = getRequiredUrlObject(this.resource, APPLICATION_AUTHOR);
            this.logoUrl = getRequiredUrlObject(this.resource, SOLID_OIDC_LOGO_URI);
            this.accessNeedGroupUrls = getRequiredUrlObjects(this.resource, HAS_ACCESS_NEED_GROUP);
            // Solid-OIDC specific
            this.redirectUrls = getRequiredUrlObjects(this.resource, SOLID_OIDC_REDIRECT_URIS);
            this.clientUrl = getUrlObject(this.resource, SOLID_OIDC_CLIENT_URI);
            this.tosUrl = getUrlObject(this.resource, SOLID_OIDC_TOS_URI);
            this.scopes.addAll(Arrays.asList(getRequiredStringObject(this.resource, SOLID_OIDC_SCOPE).split(" ")));
            this.grantTypes = getRequiredStringObjects(this.resource, SOLID_OIDC_GRANT_TYPES);
            this.responseTypes = getRequiredStringObjects(this.resource, SOLID_OIDC_RESPONSE_TYPES);
            this.defaultMaxAge = getIntegerObject(this.resource, SOLID_OIDC_DEFAULT_MAX_AGE);
            this.requireAuthTime = getBooleanObject(this.resource, SOLID_OIDC_REQUIRE_AUTH_TIME);
        } catch (SaiNotFoundException ex) {
            throw new SaiException("Failed to load application profile " + this.url + ": " + ex.getMessage());
        }
    }

}

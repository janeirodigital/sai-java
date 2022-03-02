package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import lombok.Setter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.contexts.InteropContext.INTEROP_CONTEXT;
import static com.janeirodigital.sai.core.contexts.SolidOidcContext.SOLID_OIDC_CONTEXT;
import static com.janeirodigital.sai.core.enums.ContentType.LD_JSON;
import static com.janeirodigital.sai.core.helpers.HttpHelper.getRdfModelFromResponse;
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
@Getter @Setter
public class ApplicationProfile extends CRUDResource {

    private static final List<String> contexts = Arrays.asList(SOLID_OIDC_CONTEXT, INTEROP_CONTEXT);

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
    private Integer defaultMaxAge;
    private boolean requireAuthTime;

    /**
     * Construct an {@link ApplicationProfile} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private ApplicationProfile(Builder builder) throws SaiException {
        super(builder);
        this.name = builder.name;
        this.description = builder.description;
        this.authorUrl = builder.authorUrl;
        this.logoUrl = builder.logoUrl;
        this.accessNeedGroupUrls = builder.accessNeedGroupUrls;
        this.redirectUrls = builder.redirectUrls;
        this.clientUrl = builder.clientUrl;
        this.tosUrl = builder.tosUrl;
        this.scopes = builder.scopes;
        this.grantTypes = builder.grantTypes;
        this.responseTypes = builder.responseTypes;
        this.defaultMaxAge = builder.defaultMaxAge;
        this.requireAuthTime = builder.requireAuthTime;
    }

    /**
     * Get a {@link ApplicationProfile} from the provided <code>url</code>
     * @param url URL of the {@link RegistrySet} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link ApplicationProfile}
     * @throws SaiException
     */
    public static ApplicationProfile get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        ApplicationProfile.Builder builder = new ApplicationProfile.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).setContentType(LD_JSON).build();
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} using the application profile default content-type of JSON-LD
     * @param url URL of the {@link ApplicationProfile}
     * @param saiSession {@link SaiSession} to assign
     * @return
     */
    public static ApplicationProfile get(URL url, SaiSession saiSession) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, LD_JSON);
    }

    /**
     * Reload a new instance of {@link ApplicationProfile} using the attributes of the current instance
     * @return Reloaded {@link ApplicationProfile}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public ApplicationProfile reload() throws SaiNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    /**
     * Builder for {@link ApplicationProfile} instances.
     */
    public static class Builder extends CRUDResource.Builder<Builder> {

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
        private Integer defaultMaxAge;
        private Boolean requireAuthTime;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link ApplicationProfile} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URL url, SaiSession saiSession) { super(url, saiSession); }

        /**
         * Ensures that don't get an unchecked cast warning when returning from setters
         * @return {@link RegistrySet.Builder}
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
         * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#app">name</a>
         * of the application
         * @param name name of application
         */
        public Builder setName(String name) {
            Objects.requireNonNull(name, "Must provide a name of the application");
            this.name = name;
            return this;
        }

        /**
         * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#app">description</a>
         * of the application
         * @param description description of application
         */
        public Builder setDescription(String description) {
            Objects.requireNonNull(description, "Must provide a description of the application");
            this.description = description;
            return this;
        }

        /**
         * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#app">author</a>
         * of the application
         * @param authorUrl URL of application author
         */
        public Builder setAuthorUrl(URL authorUrl) {
            Objects.requireNonNull(authorUrl, "Must provide an author of the application");
            this.authorUrl = authorUrl;
            return this;
        }

        /**
         * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#app">thumbnail</a>
         * of the application
         * @param logoUrl URL of application thumbnail
         */
        public Builder setLogoUrl(URL logoUrl) {
            Objects.requireNonNull(logoUrl, "Must provide a logo for the application");
            this.logoUrl = logoUrl;
            return this;
        }

        /**
         * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#app">access need groups</a>
         * requested by the application
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#access-need-group">Access Need Group</a>
         * @param accessNeedGroupUrls List of access need group URLs
         */
        public Builder setAccessNeedGroupUrls(List<URL> accessNeedGroupUrls) {
            Objects.requireNonNull(accessNeedGroupUrls, "Must provide access need groups for the application");
            this.accessNeedGroupUrls = accessNeedGroupUrls;
            return this;
        }

        /**
         * Set the Solid-OIDC <a href="https://solid.github.io/solid-oidc/#clientids-document">redirect_uris</a> with
         * the provided URI list;
         * @param redirectUrls List of redirect URIs
         */
        public Builder setRedirectUrls(List<URL> redirectUrls) {
            Objects.requireNonNull(redirectUrls, "Must provide redirect uris for solid-oidc");
            this.redirectUrls = redirectUrls;
            return this;
        }

        /**
         * Set the <a href="https://solid.github.io/solid-oidc/#clientids-document">client_uri</a>
         * of the Solid-OIDC client identifier document
         * @param clientUrl client URI
         */
        public Builder setClientUrl(URL clientUrl) {
            Objects.requireNonNull(clientUrl, "Must provide a client uri for solid-oidc");
            this.clientUrl = clientUrl;
            return this;
        }

        /**
         * Set the <a href="https://solid.github.io/solid-oidc/#clientids-document">tos_url</a>
         * of the Solid-OIDC client identifier document
         * @param tosUrl Terms of service URL
         */
        public Builder setTosUrl(URL tosUrl) {
            Objects.requireNonNull(tosUrl, "Must provide a terms of service uri for solid-oidc");
            this.tosUrl = tosUrl;
            return this;
        }

        /**
         * Set the Solid-OIDC <a href="https://solid.github.io/solid-oidc/#clientids-document">scopes</a> with
         * the provided list of scopes
         * @param scopes Scopes to add
         */
        public Builder setScopes(List<String> scopes) {
            Objects.requireNonNull(scopes, "Must provide scopes for solid-oidc");
            this.scopes = scopes;
            return this;
        }

        /**
         * Set the Solid-OIDC <a href="https://solid.github.io/solid-oidc/#clientids-document">grant_types</a> with
         * the provided grant types
         * @param grantTypes Grant types to add
         */
        public Builder setGrantType(List<String> grantTypes) {
            Objects.requireNonNull(grantTypes, "Must provide grant types for solid-oidc");
            this.grantTypes = grantTypes;
            return this;
        }

        /**
         * Set the Solid-OIDC <a href="https://solid.github.io/solid-oidc/#clientids-document">response_types</a> with
         * the provided response types
         * @param responseTypes Response types to add
         */
        public Builder setResponseTypes(List<String> responseTypes) {
            Objects.requireNonNull(responseTypes, "Must provide response types for solid-oidc");
            this.responseTypes = responseTypes;
            return this;
        }

        /**
         * Set the <a href="https://solid.github.io/solid-oidc/#clientids-document">default_max_age</a>
         * for the Solid-OIDC client identifier document
         * @param defaultMaxAge Default max age
         */
        public Builder setDefaultMaxAge(int defaultMaxAge) {
            this.defaultMaxAge = defaultMaxAge;
            return this;
        }

        /**
         * Set <a href="https://solid.github.io/solid-oidc/#clientids-document">require_auth_time</a>
         * for the Solid-OIDC client identifier document
         * @param requireAuthTime Require auth time
         */
        public Builder setRequireAuthTime(boolean requireAuthTime) {
            this.requireAuthTime = requireAuthTime;
            return this;
        }
        
        /**
         * Populates the fields of the {@link RegistrySet} based on the associated Jena resource.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException {
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
                this.scopes = new ArrayList<>();
                this.scopes.addAll(Arrays.asList(getRequiredStringObject(this.resource, SOLID_OIDC_SCOPE).split(" ")));
                this.grantTypes = getRequiredStringObjects(this.resource, SOLID_OIDC_GRANT_TYPES);
                this.responseTypes = getRequiredStringObjects(this.resource, SOLID_OIDC_RESPONSE_TYPES);
                this.defaultMaxAge = getIntegerObject(this.resource, SOLID_OIDC_DEFAULT_MAX_AGE);
                this.requireAuthTime = getBooleanObject(this.resource, SOLID_OIDC_REQUIRE_AUTH_TIME);
            } catch (SaiNotFoundException ex) {
                throw new SaiException("Failed to load application profile " + this.url + ": " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        private void populateDataset() {
            this.resource = getNewResourceForType(this.url, APPLICATION);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, SOLID_OIDC_CLIENT_NAME, this.name);
            updateObject(this.resource, APPLICATION_DESCRIPTION, this.description);
            updateObject(this.resource, APPLICATION_AUTHOR, this.authorUrl);
            updateObject(this.resource, SOLID_OIDC_LOGO_URI, this.logoUrl);
            updateUrlObjects(this.resource, HAS_ACCESS_NEED_GROUP, this.accessNeedGroupUrls);
            updateUrlObjects(this.resource, SOLID_OIDC_REDIRECT_URIS, this.redirectUrls);
            if (this.clientUrl != null) { updateObject(this.resource, SOLID_OIDC_CLIENT_URI, this.clientUrl); }
            if (this.tosUrl != null) { updateObject(this.resource, SOLID_OIDC_TOS_URI, this.tosUrl); }
            updateObject(this.resource, SOLID_OIDC_SCOPE, String.join(" ", this.scopes));
            updateStringObjects(this.resource, SOLID_OIDC_GRANT_TYPES, this.grantTypes);
            updateStringObjects(this.resource, SOLID_OIDC_RESPONSE_TYPES, this.responseTypes);
            if (this.defaultMaxAge != null) { updateObject(this.resource, SOLID_OIDC_DEFAULT_MAX_AGE, this.defaultMaxAge); }
            if (this.requireAuthTime != null) { updateObject(this.resource, SOLID_OIDC_REQUIRE_AUTH_TIME, this.requireAuthTime); }
        }

        /**
         * Build the {@link ApplicationProfile} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}.
         * @return {@link ApplicationProfile}
         * @throws SaiException
         */
        public ApplicationProfile build() throws SaiException {
            Objects.requireNonNull(name, "Must provide a name of the application");
            Objects.requireNonNull(description, "Must provide a description of the application");
            Objects.requireNonNull(authorUrl, "Must provide an author of the application");
            Objects.requireNonNull(logoUrl, "Must provide a logo for the application");
            Objects.requireNonNull(accessNeedGroupUrls, "Must provide access need groups for the application");
            Objects.requireNonNull(redirectUrls, "Must provide redirect uris for solid-oidc");
            Objects.requireNonNull(scopes, "Must provide scopes for solid-oidc");
            Objects.requireNonNull(grantTypes, "Must provide grant types for solid-oidc");
            Objects.requireNonNull(responseTypes, "Must provide response types for solid-oidc");
            if (this.dataset == null) { populateDataset(); }
            return new ApplicationProfile(this);
        }
        
    }
    

}
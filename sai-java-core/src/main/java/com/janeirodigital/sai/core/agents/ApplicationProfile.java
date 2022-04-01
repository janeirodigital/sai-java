package com.janeirodigital.sai.core.agents;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.resources.CRUDResource;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.sai.rdfutils.SaiRdfNotFoundException;
import lombok.Getter;
import lombok.Setter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.contexts.InteropContext.INTEROP_CONTEXT;
import static com.janeirodigital.sai.core.contexts.SolidOidcContext.SOLID_OIDC_CONTEXT;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.SolidOidcVocabulary.*;
import static com.janeirodigital.sai.rdfutils.RdfUtils.*;

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
    private URI authorUri;
    private URI logoUri;
    private List<URI> accessNeedGroupUris;
    // Solid-OIDC specific
    private List<URI> redirectUris;
    private URI clientUri;
    private URI tosUri;
    private List<String> scopes;
    private List<String> grantTypes;
    private List<String> responseTypes;
    private Integer defaultMaxAge;
    private Boolean requireAuthTime;

    /**
     * Construct an {@link ApplicationProfile} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private ApplicationProfile(Builder builder) throws SaiException {
        super(builder);
        this.name = builder.name;
        this.description = builder.description;
        this.authorUri = builder.authorUri;
        this.logoUri = builder.logoUri;
        this.accessNeedGroupUris = builder.accessNeedGroupUris;
        this.redirectUris = builder.redirectUris;
        this.clientUri = builder.clientUri;
        this.tosUri = builder.tosUri;
        this.scopes = builder.scopes;
        this.grantTypes = builder.grantTypes;
        this.responseTypes = builder.responseTypes;
        this.defaultMaxAge = builder.defaultMaxAge;
        this.requireAuthTime = builder.requireAuthTime;
        try { this.jsonLdContext = buildRemoteJsonLdContexts(contexts); } catch (SaiRdfException ex) {
            throw new SaiException("Failed to build remote JSON-LD context", ex);
        }
    }

    /**
     * Get a {@link ApplicationProfile} from the provided <code>uri</code>
     * @param uri URI of the {@link RegistrySet} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link ApplicationProfile}
     * @throws SaiException
     */
    public static ApplicationProfile get(URI uri, SaiSession saiSession, ContentType contentType) throws SaiException, SaiHttpNotFoundException {
        ApplicationProfile.Builder builder = new ApplicationProfile.Builder(uri, saiSession);
        try (Response response = read(uri, saiSession, contentType, false)) {
            return builder.setDataset(response).setContentType(ContentType.LD_JSON).build();
        }
    }

    /**
     * Call {@link #get(URI, SaiSession, ContentType)} using the application profile default content-type of JSON-LD
     * @param uri URI of the {@link ApplicationProfile}
     * @param saiSession {@link SaiSession} to assign
     * @return
     */
    public static ApplicationProfile get(URI uri, SaiSession saiSession) throws SaiHttpNotFoundException, SaiException {
        return get(uri, saiSession, ContentType.LD_JSON);
    }

    /**
     * Reload a new instance of {@link ApplicationProfile} using the attributes of the current instance
     * @return Reloaded {@link ApplicationProfile}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public ApplicationProfile reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.uri, this.saiSession, this.contentType);
    }

    /**
     * Builder for {@link ApplicationProfile} instances.
     */
    public static class Builder extends CRUDResource.Builder<Builder> {

        private String name;
        private String description;
        private URI authorUri;
        private URI logoUri;
        private List<URI> accessNeedGroupUris;
        // Solid-OIDC specific
        private List<URI> redirectUris;
        private URI clientUri;
        private URI tosUri;
        private List<String> scopes;
        private List<String> grantTypes;
        private List<String> responseTypes;
        private Integer defaultMaxAge;
        private Boolean requireAuthTime;

        /**
         * Initialize builder with <code>uri</code> and <code>saiSession</code>
         * @param uri URI of the {@link ApplicationProfile} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URI uri, SaiSession saiSession) { super(uri, saiSession); }

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
         * @param authorUri URI of application author
         */
        public Builder setAuthorUri(URI authorUri) {
            Objects.requireNonNull(authorUri, "Must provide an author of the application");
            this.authorUri = authorUri;
            return this;
        }

        /**
         * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#app">thumbnail</a>
         * of the application
         * @param logoUri URI of application thumbnail
         */
        public Builder setLogoUri(URI logoUri) {
            Objects.requireNonNull(logoUri, "Must provide a logo for the application");
            this.logoUri = logoUri;
            return this;
        }

        /**
         * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#app">access need groups</a>
         * requested by the application
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#access-need-group">Access Need Group</a>
         * @param accessNeedGroupUris List of access need group URIs
         */
        public Builder setAccessNeedGroupUris(List<URI> accessNeedGroupUris) {
            Objects.requireNonNull(accessNeedGroupUris, "Must provide access need groups for the application");
            this.accessNeedGroupUris = accessNeedGroupUris;
            return this;
        }

        /**
         * Set the Solid-OIDC <a href="https://solid.github.io/solid-oidc/#clientids-document">redirect_uris</a> with
         * the provided URI list;
         * @param redirectUris List of redirect URIs
         */
        public Builder setRedirectUris(List<URI> redirectUris) {
            Objects.requireNonNull(redirectUris, "Must provide redirect uris for solid-oidc");
            this.redirectUris = redirectUris;
            return this;
        }

        /**
         * Set the <a href="https://solid.github.io/solid-oidc/#clientids-document">client_uri</a>
         * of the Solid-OIDC client identifier document
         * @param clientUri client URI
         */
        public Builder setClientUri(URI clientUri) {
            Objects.requireNonNull(clientUri, "Must provide a client uri for solid-oidc");
            this.clientUri = clientUri;
            return this;
        }

        /**
         * Set the <a href="https://solid.github.io/solid-oidc/#clientids-document">tos_uri</a>
         * of the Solid-OIDC client identifier document
         * @param tosUri Terms of service URI
         */
        public Builder setTosUri(URI tosUri) {
            Objects.requireNonNull(tosUri, "Must provide a terms of service uri for solid-oidc");
            this.tosUri = tosUri;
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
                this.authorUri = getRequiredUriObject(this.resource, APPLICATION_AUTHOR);
                this.logoUri = getRequiredUriObject(this.resource, SOLID_OIDC_LOGO_URI);
                this.accessNeedGroupUris = getRequiredUriObjects(this.resource, HAS_ACCESS_NEED_GROUP);
                // Solid-OIDC specific
                this.redirectUris = getRequiredUriObjects(this.resource, SOLID_OIDC_REDIRECT_URIS);
                this.clientUri = getUriObject(this.resource, SOLID_OIDC_CLIENT_URI);
                this.tosUri = getUriObject(this.resource, SOLID_OIDC_TOS_URI);
                this.scopes = new ArrayList<>();
                this.scopes.addAll(Arrays.asList(getRequiredStringObject(this.resource, SOLID_OIDC_SCOPE).split(" ")));
                this.grantTypes = getRequiredStringObjects(this.resource, SOLID_OIDC_GRANT_TYPES);
                this.responseTypes = getRequiredStringObjects(this.resource, SOLID_OIDC_RESPONSE_TYPES);
                this.defaultMaxAge = getIntegerObject(this.resource, SOLID_OIDC_DEFAULT_MAX_AGE);
                this.requireAuthTime = getBooleanObject(this.resource, SOLID_OIDC_REQUIRE_AUTH_TIME);
            } catch (SaiRdfException | SaiRdfNotFoundException ex) {
                throw new SaiException("Failed to load application profile " + this.uri, ex);
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        private void populateDataset() {
            this.resource = getNewResourceForType(this.uri, APPLICATION);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, SOLID_OIDC_CLIENT_NAME, this.name);
            updateObject(this.resource, APPLICATION_DESCRIPTION, this.description);
            updateObject(this.resource, APPLICATION_AUTHOR, this.authorUri);
            updateObject(this.resource, SOLID_OIDC_LOGO_URI, this.logoUri);
            updateUriObjects(this.resource, HAS_ACCESS_NEED_GROUP, this.accessNeedGroupUris);
            updateUriObjects(this.resource, SOLID_OIDC_REDIRECT_URIS, this.redirectUris);
            if (this.clientUri != null) { updateObject(this.resource, SOLID_OIDC_CLIENT_URI, this.clientUri); }
            if (this.tosUri != null) { updateObject(this.resource, SOLID_OIDC_TOS_URI, this.tosUri); }
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
            Objects.requireNonNull(authorUri, "Must provide an author of the application");
            Objects.requireNonNull(logoUri, "Must provide a logo for the application");
            Objects.requireNonNull(accessNeedGroupUris, "Must provide access need groups for the application");
            Objects.requireNonNull(redirectUris, "Must provide redirect uris for solid-oidc");
            Objects.requireNonNull(scopes, "Must provide scopes for solid-oidc");
            Objects.requireNonNull(grantTypes, "Must provide grant types for solid-oidc");
            Objects.requireNonNull(responseTypes, "Must provide response types for solid-oidc");
            if (this.dataset == null) { populateDataset(); }
            return new ApplicationProfile(this);
        }
        
    }
    

}
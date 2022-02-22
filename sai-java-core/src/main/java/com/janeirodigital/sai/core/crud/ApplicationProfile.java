package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.Getter;
import okhttp3.Headers;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.contexts.InteropContext.INTEROP_CONTEXT;
import static com.janeirodigital.sai.core.contexts.SolidOidcContext.SOLID_OIDC_CONTEXT;
import static com.janeirodigital.sai.core.enums.ContentType.LD_JSON;
import static com.janeirodigital.sai.core.helpers.HttpHelper.addHttpHeader;
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
@Getter
public class ApplicationProfile extends CRUDResource {

    private static final List<String> contexts = Arrays.asList(SOLID_OIDC_CONTEXT, INTEROP_CONTEXT);

    private final String name;
    private final String description;
    private final URL authorUrl;
    private final URL logoUrl;
    private final List<URL> accessNeedGroupUrls;
    // Solid-OIDC specific
    private final List<URL> redirectUrls;
    private final URL clientUrl;
    private final URL tosUrl;
    private final List<String> scopes;
    private final List<String> grantTypes;
    private final List<String> responseTypes;
    private final Integer defaultMaxAge;
    private boolean requireAuthTime;

    /**
     * Construct a new {@link ApplicationProfile}
     * @param url URL of the {@link ApplicationProfile}
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    private ApplicationProfile(URL url, DataFactory dataFactory, Model dataset, Resource resource, ContentType contentType,
                              String name, String description, URL authorUrl, URL logoUrl, List<URL>accessNeedGroupUrls,
                              List<URL> redirectUrls, URL clientUrl, URL tosUrl, List<String> scopes, List<String> grantTypes,
                              List<String> responseTypes, int defaultMaxAge, boolean requireAuthTime) throws SaiException {
        super(url, dataFactory, false);
        // By default the application profile document is JSON-LD
        this.dataset = dataset;
        this.resource = resource;
        this.contentType = contentType;
        this.jsonLdContext = buildRemoteJsonLdContexts(contexts);
        this.name = name;
        this.description = description;
        this.authorUrl = authorUrl;
        this.logoUrl = logoUrl;
        this.accessNeedGroupUrls = accessNeedGroupUrls;
        this.redirectUrls = redirectUrls;
        this.clientUrl = clientUrl;
        this.tosUrl = tosUrl;
        this.scopes = scopes;
        this.grantTypes = grantTypes;
        this.responseTypes = responseTypes;
        this.defaultMaxAge = defaultMaxAge;
        this.requireAuthTime = requireAuthTime;
    }

    /**
     * Get a {@link ApplicationProfile} from the provided <code>url</code>
     * @param url URL of the {@link RegistrySet} to get
     * @param dataFactory {@link DataFactory} to assign
     * @return Retrieved {@link ApplicationProfile}
     * @throws SaiException
     */
    public static ApplicationProfile get(URL url, DataFactory dataFactory) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(url, "Must provide the URL of the data grant to get");
        Objects.requireNonNull(dataFactory, "Must provide a data factory to assign to the data grant");
        Builder builder = new Builder(url, dataFactory);
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, LD_JSON.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(dataFactory.getAuthorizedSession(), dataFactory.getHttpClient(), url, headers))) {
            builder.setDataset(getRdfModelFromResponse(response));
        }
        return builder.build();
    }

    /**
     * Builder for {@link ApplicationProfile} instances.
     */
    public static class Builder {

        private final URL url;
        private final DataFactory dataFactory;
        private final ContentType contentType;
        private Model dataset;
        private Resource resource;
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
         * Initialize builder with <code>url</code> and <code>dataFactory</code> 
         * @param url URL of the {@link RegistrySet} to build
         * @param dataFactory {@link DataFactory} to assign
         */
        public Builder(URL url, DataFactory dataFactory) {
            Objects.requireNonNull(url, "Must provide a URL for the application profile builder");
            Objects.requireNonNull(dataFactory, "Must provide a data factory for the application profile builder");
            this.url = url;
            this.dataFactory = dataFactory;
            this.contentType = LD_JSON; // Client Identifier documents are JSON-LD
        }

        /**
         * Optional Jena Model that will initialize the attributes of the Builder rather than set
         * them manually. Typically used in read scenarios when populating the Builder from
         * the contents of a remote resource.
         * @param dataset Jena model to populate the Builder attributes with
         * @return {@link Builder}
         */
        public Builder setDataset(Model dataset) throws SaiException {
            Objects.requireNonNull(dataset, "Must provide a Jena model for the application profile builder");
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            populateFromDataset();
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
            Objects.requireNonNull(defaultMaxAge, "Must provide a default max age for solid-oidc");
            this.defaultMaxAge = defaultMaxAge;
            return this;
        }

        /**
         * Set <a href="https://solid.github.io/solid-oidc/#clientids-document">require_auth_time</a>
         * for the Solid-OIDC client identifier document
         * @param requireAuthTime Require auth time
         */
        public Builder setRequireAuthTime(boolean requireAuthTime) {
            Objects.requireNonNull(requireAuthTime, "Must provide an auth time requirement for solid-oidc");
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
         * @throws SaiException
         */
        private void populateDataset() throws SaiException {
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
         * Build the {@link RegistrySet} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}. Conversely, if a dataset was provided, the attributes of the
         * Builder will be populated from it.
         * @return {@link RegistrySet}
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
            // TODO - fill this out
            return new ApplicationProfile(this.url, this.dataFactory, this.dataset, this.resource, this.contentType,
                                          this.name, this.description, this.authorUrl, this.logoUrl, this.accessNeedGroupUrls,
                                          this.redirectUrls, this.clientUrl, this.tosUrl, this.scopes, this.grantTypes,
                                          this.responseTypes, this.defaultMaxAge, this.requireAuthTime);
        }
        
    }
    

}

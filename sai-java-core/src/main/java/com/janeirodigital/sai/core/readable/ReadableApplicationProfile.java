package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.sai.rdfutils.SaiRdfNotFoundException;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.SolidOidcVocabulary.*;
import static com.janeirodigital.sai.httputils.HttpUtils.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.rdfutils.RdfUtils.*;

/**
 * Publicly readable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#app">Application</a>,
 * profile which is also cross-pollinated with the
 * <a href="https://solid.github.io/solid-oidc/#clientids-document">Client Identifier Document</a>
 * from Solid-OIDC.
 */
@Getter
public class ReadableApplicationProfile extends ReadableResource {

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
    private final boolean requireAuthTime;

    /**
     * Construct a {@link ReadableApplicationProfile} from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private ReadableApplicationProfile(Builder builder) throws SaiException {
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
     * Get a {@link ReadableApplicationProfile} from the provided <code>url</code>.
     * @param url URL to get the {@link ReadableApplicationProfile} from
     * @param saiSession {@link SaiSession} to use
     * @param contentType {@link ContentType} to use for retrieval
     * @return {@link ReadableApplicationProfile}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static ReadableApplicationProfile get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiHttpNotFoundException {
        ReadableApplicationProfile.Builder builder = new ReadableApplicationProfile.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, true)) {
            return builder.setDataset(response).setContentType(contentType).setUnprotected().build();
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link ReadableApplicationProfile} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link ReadableApplicationProfile}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static ReadableApplicationProfile get(URL url, SaiSession saiSession) throws SaiException, SaiHttpNotFoundException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link ReadableApplicationProfile} using the attributes of the
     * current instance
     * @return Reloaded {@link ReadableApplicationProfile}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public ReadableApplicationProfile reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    /**
     * Builder for {@link ReadableApplicationProfile} instances.
     */
    private static class Builder extends ReadableResource.Builder<Builder> {

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
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link ReadableApplicationProfile} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URL url, SaiSession saiSession) {
            super(url, saiSession);
            this.contentType = ContentType.LD_JSON;  // Solid Application Profile documents are always JSON-LD
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
         * Populates the fields of the {@link ReadableApplicationProfile} based on the associated Jena resource.
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
                this.scopes = Arrays.asList(getRequiredStringObject(this.resource, SOLID_OIDC_SCOPE).split(" "));
                this.grantTypes = getRequiredStringObjects(this.resource, SOLID_OIDC_GRANT_TYPES);
                this.responseTypes = getRequiredStringObjects(this.resource, SOLID_OIDC_RESPONSE_TYPES);
                this.defaultMaxAge = getIntegerObject(this.resource, SOLID_OIDC_DEFAULT_MAX_AGE);
                this.requireAuthTime = getBooleanObject(this.resource, SOLID_OIDC_REQUIRE_AUTH_TIME);
            } catch (SaiRdfException | SaiRdfNotFoundException ex) {
                throw new SaiException("Failed to load application profile " + this.url, ex);
            }
        }

        /**
         * Build the {@link ReadableApplicationProfile} using attributes from the Builder.
         * @return {@link ReadableApplicationProfile}
         * @throws SaiException
         */
        public ReadableApplicationProfile build() throws SaiException {
            return new ReadableApplicationProfile(this);             
        }
        
    }
}

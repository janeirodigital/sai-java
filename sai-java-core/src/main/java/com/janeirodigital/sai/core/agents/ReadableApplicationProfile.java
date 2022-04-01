package com.janeirodigital.sai.core.agents;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.resources.ReadableResource;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.sai.rdfutils.SaiRdfNotFoundException;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URI;
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
    private final URI authorUri;
    private final URI logoUri;
    private final List<URI> accessNeedGroupUris;
    // Solid-OIDC specific
    private final List<URI> redirectUris;
    private final URI clientUri;
    private final URI tosUri;
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
    }

    /**
     * Get a {@link ReadableApplicationProfile} from the provided <code>uri</code>.
     * @param uri URI to get the {@link ReadableApplicationProfile} from
     * @param saiSession {@link SaiSession} to use
     * @param contentType {@link ContentType} to use for retrieval
     * @return {@link ReadableApplicationProfile}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static ReadableApplicationProfile get(URI uri, SaiSession saiSession, ContentType contentType) throws SaiException, SaiHttpNotFoundException {
        ReadableApplicationProfile.Builder builder = new ReadableApplicationProfile.Builder(uri, saiSession);
        try (Response response = read(uri, saiSession, contentType, true)) {
            return builder.setDataset(response).setContentType(contentType).setUnprotected().build();
        }
    }

    /**
     * Call {@link #get(URI, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param uri URI of the {@link ReadableApplicationProfile} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link ReadableApplicationProfile}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static ReadableApplicationProfile get(URI uri, SaiSession saiSession) throws SaiException, SaiHttpNotFoundException {
        return get(uri, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link ReadableApplicationProfile} using the attributes of the
     * current instance
     * @return Reloaded {@link ReadableApplicationProfile}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public ReadableApplicationProfile reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.uri, this.saiSession, this.contentType);
    }

    /**
     * Builder for {@link ReadableApplicationProfile} instances.
     */
    private static class Builder extends ReadableResource.Builder<Builder> {

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
        private boolean requireAuthTime;
        
        /**
         * Initialize builder with <code>uri</code> and <code>saiSession</code>
         * @param uri URI of the {@link ReadableApplicationProfile} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URI uri, SaiSession saiSession) {
            super(uri, saiSession);
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
                this.authorUri = getRequiredUriObject(this.resource, APPLICATION_AUTHOR);
                this.logoUri = getRequiredUriObject(this.resource, SOLID_OIDC_LOGO_URI);
                this.accessNeedGroupUris = getRequiredUriObjects(this.resource, HAS_ACCESS_NEED_GROUP);
                // Solid-OIDC specific
                this.redirectUris = getRequiredUriObjects(this.resource, SOLID_OIDC_REDIRECT_URIS);
                this.clientUri = getUriObject(this.resource, SOLID_OIDC_CLIENT_URI);
                this.tosUri = getUriObject(this.resource, SOLID_OIDC_TOS_URI);
                this.scopes = Arrays.asList(getRequiredStringObject(this.resource, SOLID_OIDC_SCOPE).split(" "));
                this.grantTypes = getRequiredStringObjects(this.resource, SOLID_OIDC_GRANT_TYPES);
                this.responseTypes = getRequiredStringObjects(this.resource, SOLID_OIDC_RESPONSE_TYPES);
                this.defaultMaxAge = getIntegerObject(this.resource, SOLID_OIDC_DEFAULT_MAX_AGE);
                this.requireAuthTime = getBooleanObject(this.resource, SOLID_OIDC_REQUIRE_AUTH_TIME);
            } catch (SaiRdfException | SaiRdfNotFoundException ex) {
                throw new SaiException("Failed to load application profile " + this.uri, ex);
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

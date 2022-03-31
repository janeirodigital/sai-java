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

import java.net.URI;
import java.util.List;

import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.SolidTermsVocabulary.SOLID_OIDC_ISSUER;
import static com.janeirodigital.sai.httputils.HttpUtils.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.rdfutils.RdfUtils.getRequiredUriObject;
import static com.janeirodigital.sai.rdfutils.RdfUtils.getRequiredUriObjects;

/**
 * Publicly readable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Social Agent</a>
 * profile which is also cross-pollinated with other terms from the Solid ecosystem
 */
@Getter
public class ReadableSocialAgentProfile extends ReadableResource {

    private final URI registrySetUri;
    private final URI authorizationAgentUri;
    private final URI accessInboxUri;
    private final List<URI> oidcIssuerUris;

    /**
     * Construct a {@link ReadableSocialAgentProfile} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private ReadableSocialAgentProfile(Builder builder) throws SaiException {
        super(builder);
        this.registrySetUri = builder.registrySetUri;
        this.authorizationAgentUri = builder.authorizationAgentUri;
        this.accessInboxUri = builder.accessInboxUri;
        this.oidcIssuerUris = builder.oidcIssuerUris;
    }

    /**
     * Get a {@link ReadableSocialAgentProfile} from the provided <code>uri</code>.
     * @param uri URI to get the {@link ReadableSocialAgentProfile} from
     * @param saiSession {@link SaiSession} to use
     * @param contentType {@link ContentType} to use for retrieval
     * @return {@link ReadableSocialAgentProfile}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static ReadableSocialAgentProfile get(URI uri, SaiSession saiSession, ContentType contentType) throws SaiException, SaiHttpNotFoundException {
        ReadableSocialAgentProfile.Builder builder = new ReadableSocialAgentProfile.Builder(uri, saiSession);
        try (Response response = read(uri, saiSession, contentType, true)) {
            return builder.setDataset(response).setContentType(contentType).setUnprotected().build();
        }
    }

    /**
     * Call {@link #get(URI, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param uri URI of the {@link ReadableSocialAgentProfile} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link ReadableSocialAgentProfile}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static ReadableSocialAgentProfile get(URI uri, SaiSession saiSession) throws SaiException, SaiHttpNotFoundException {
        return get(uri, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link ReadableSocialAgentProfile} using the attributes of the current instance
     * @return Reloaded {@link ReadableSocialAgentProfile}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public ReadableSocialAgentProfile reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.uri, this.saiSession, this.contentType);
    }

    /**
     * Builder for {@link ReadableApplicationProfile} instances.
     */
    public static class Builder extends ReadableResource.Builder<Builder> {

        private URI registrySetUri;
        private URI authorizationAgentUri;
        private URI accessInboxUri;
        private List<URI> oidcIssuerUris;

        /**
         * Initialize builder with <code>uri</code> and <code>saiSession</code>
         * @param uri URI of the {@link ReadableApplicationProfile} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URI uri, SaiSession saiSession) { super(uri, saiSession); }

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
                this.oidcIssuerUris = getRequiredUriObjects(this.resource, SOLID_OIDC_ISSUER);
                this.authorizationAgentUri = getRequiredUriObject(this.resource, HAS_AUTHORIZATION_AGENT);
                this.registrySetUri = getRequiredUriObject(this.resource, HAS_REGISTRY_SET);
                this.accessInboxUri = getRequiredUriObject(this.resource, HAS_ACCESS_INBOX);
            } catch (SaiRdfException | SaiRdfNotFoundException ex) {
                throw new SaiException("Failed to load social agent profile " + this.uri, ex);
            }
        }

        /**
         * Build the {@link ReadableApplicationProfile} using attributes from the Builder.
         * @return {@link ReadableApplicationProfile}
         * @throws SaiException
         */
        public ReadableSocialAgentProfile build() throws SaiException {
            return new ReadableSocialAgentProfile(this);
        }
    }
}

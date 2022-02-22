package com.janeirodigital.sai.core.readable;

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
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.SolidTermsVocabulary.SOLID_OIDC_ISSUER;

/**
 * Publicly readable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Social Agent</a>
 * profile which is also cross-pollinated with other terms from the Solid ecosystem
 */
@Getter
public class ReadableSocialAgentProfile extends ReadableResource {

    private final URL registrySetUrl;
    private final URL authorizationAgentUrl;
    private final URL accessInboxUrl;
    private final List<URL> oidcIssuerUrls;

    /**
     * Construct a {@link ReadableSocialAgentProfile} instance from the provided <code>url</code>.
     * @param url URL to generate the {@link ReadableSocialAgentProfile} from
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    private ReadableSocialAgentProfile(URL url, DataFactory dataFactory, Model dataset, Resource resource, ContentType contentType,
                                      URL registrySetUrl, URL authorizationAgentUrl, URL accessInboxUrl, List<URL> oidcIssuerUrls) throws SaiException {
        super(url, dataFactory, true);
        this.dataset = dataset;
        this.resource = resource;
        this.contentType = contentType;
        this.registrySetUrl = registrySetUrl;
        this.authorizationAgentUrl = authorizationAgentUrl;
        this.accessInboxUrl = accessInboxUrl;
        this.oidcIssuerUrls = oidcIssuerUrls;
    }

    /**
     * Primary mechanism used to construct and bootstrap a {@link ReadableSocialAgentProfile} from
     * the provided <code>url</code>.
     * @param url URL to generate the {@link ReadableSocialAgentProfile} from
     * @param dataFactory {@link DataFactory} to assign
     * @param contentType {@link ContentType} to use for retrieval
     * @return {@link ReadableSocialAgentProfile}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ReadableSocialAgentProfile get(URL url, DataFactory dataFactory, ContentType contentType) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(url, "Must provide the URL of the readable social agent profile to get");
        Objects.requireNonNull(dataFactory, "Must provide a data factory to assign to the readable social agent profile");
        Objects.requireNonNull(contentType, "Must provide a content type to assign to the readable social agent profile");
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(dataFactory.getAuthorizedSession(), dataFactory.getHttpClient(), url, headers))) {
            return new ReadableSocialAgentProfile.Builder(url, dataFactory, contentType, getRdfModelFromResponse(response)).build();
        }
    }

    /**
     * Call {@link #get(URL, DataFactory, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link ReadableSocialAgentProfile} to get
     * @param dataFactory {@link DataFactory} to assign
     * @return Retrieved {@link ReadableSocialAgentProfile}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ReadableSocialAgentProfile get(URL url, DataFactory dataFactory) throws SaiException, SaiNotFoundException {
        return get(url, dataFactory, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Builder for {@link ReadableApplicationProfile} instances.
     */
    private static class Builder {

        private final URL url;
        private final DataFactory dataFactory;
        private final ContentType contentType;
        private final Model dataset;
        private final Resource resource;
        private URL registrySetUrl;
        private URL authorizationAgentUrl;
        private URL accessInboxUrl;
        private List<URL> oidcIssuerUrls;

        /**
         * Initialize builder with <code>url</code> and <code>dataFactory</code>
         * @param url URL of the {@link ReadableApplicationProfile} to build
         * @param dataFactory {@link DataFactory} to assign
         * @param contentType {@link ContentType} to assign
         * @param dataset Jena model to populate the readable social agent profile with
         */
        public Builder(URL url, DataFactory dataFactory, ContentType contentType, Model dataset) throws SaiException, SaiNotFoundException {
            Objects.requireNonNull(url, "Must provide a URL for the readable social agent profile builder");
            Objects.requireNonNull(dataFactory, "Must provide a data factory for the readable social agent profile builder");
            Objects.requireNonNull(contentType, "Must provide a content type to use for retrieval of readable social agent profile ");
            Objects.requireNonNull(dataset, "Must provide a dateset to use to populate the readable social agent profile ");
            this.url = url;
            this.dataFactory = dataFactory;
            this.contentType = contentType;
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            populateFromDataset();
        }

        /**
         * Populates the fields of the {@link ReadableApplicationProfile} based on the associated Jena resource.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException, SaiNotFoundException {
            this.oidcIssuerUrls = getRequiredUrlObjects(this.resource, SOLID_OIDC_ISSUER);
            this.authorizationAgentUrl = getRequiredUrlObject(this.resource, HAS_AUTHORIZATION_AGENT);
            this.registrySetUrl = getRequiredUrlObject(this.resource, HAS_REGISTRY_SET);
            this.accessInboxUrl = getRequiredUrlObject(this.resource, HAS_ACCESS_INBOX);
        }

        /**
         * Build the {@link ReadableApplicationProfile} using attributes from the Builder.
         * @return {@link ReadableApplicationProfile}
         * @throws SaiException
         */
        public ReadableSocialAgentProfile build() throws SaiException {
            return new ReadableSocialAgentProfile(this.url, this.dataFactory, this.dataset, this.resource, this.contentType,
                                                  this.registrySetUrl, this.authorizationAgentUrl, this.accessInboxUrl,
                                                  this.oidcIssuerUrls);
        }
    }
}

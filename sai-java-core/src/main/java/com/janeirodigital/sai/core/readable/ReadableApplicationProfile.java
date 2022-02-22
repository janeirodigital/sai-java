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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.SolidOidcVocabulary.*;

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
     * Construct a {@link ReadableApplicationProfile}. Should only be called from {@link Builder}.
     * @throws SaiException
     */
    private ReadableApplicationProfile(URL url, DataFactory dataFactory, Model dataset, Resource resource, ContentType contentType,
                                       String name, String description, URL authorUrl, URL logoUrl, List<URL> accessNeedGroupUrls,
                                      List<URL> redirectUrls, URL clientUrl, URL tosUrl, List<String> scopes, 
                                      List<String> grantTypes, List<String> responseTypes, Integer defaultMaxAge,
                                      boolean requireAuthTime) throws SaiException {
        super(url, dataFactory, true);
        this.dataset = dataset;
        this.resource = resource;
        this.contentType = contentType;
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
     * Primary mechanism used to construct and bootstrap a {@link ReadableApplicationProfile} from
     * the provided <code>url</code>.
     * @param url URL to generate the {@link ReadableApplicationProfile} from
     * @param dataFactory {@link DataFactory} to assign
     * @param contentType {@link ContentType} to use for retrieval
     * @return {@link ReadableApplicationProfile}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ReadableApplicationProfile get(URL url, DataFactory dataFactory, ContentType contentType) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(url, "Must provide the URL of the readable application profile to get");
        Objects.requireNonNull(dataFactory, "Must provide a data factory to assign to the readable application profile");
        Objects.requireNonNull(contentType, "Must provide a content type to assign to the readable application profile");
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(dataFactory.getAuthorizedSession(), dataFactory.getHttpClient(), url, headers))) {
            return new ReadableApplicationProfile.Builder(url, dataFactory, contentType, getRdfModelFromResponse(response)).build();
        }
    }

    /**
     * Call {@link #get(URL, DataFactory, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link ReadableApplicationProfile} to get
     * @param dataFactory {@link DataFactory} to assign
     * @return Retrieved {@link ReadableApplicationProfile}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ReadableApplicationProfile get(URL url, DataFactory dataFactory) throws SaiException, SaiNotFoundException {
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
         * Initialize builder with <code>url</code> and <code>dataFactory</code>
         * @param url URL of the {@link ReadableApplicationProfile} to build
         * @param dataFactory {@link DataFactory} to assign
         * @param contentType {@link ContentType} to assign
         * @param dataset Jena model to populate the readable application profile with                                       
         */
        public Builder(URL url, DataFactory dataFactory, ContentType contentType, Model dataset) throws SaiException {
            Objects.requireNonNull(url, "Must provide a URL for the readable application profile builder");
            Objects.requireNonNull(dataFactory, "Must provide a data factory for the readable application profile builder");
            Objects.requireNonNull(contentType, "Must provide a content type to use for retrieval of readable application profile ");
            Objects.requireNonNull(dataset, "Must provide a dateset to use to populate the readable application profile ");
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
            } catch (SaiNotFoundException ex) {
                throw new SaiException("Failed to load application profile " + this.url + ": " + ex.getMessage());
            }
        }

        /**
         * Build the {@link ReadableApplicationProfile} using attributes from the Builder.
         * @return {@link ReadableApplicationProfile}
         * @throws SaiException
         */
        public ReadableApplicationProfile build() throws SaiException {
            return new ReadableApplicationProfile(this.url, this.dataFactory, this.dataset, this.resource, this.contentType, this.name,
                                                  this.description, this.authorUrl, this.logoUrl, this.accessNeedGroupUrls,
                                                  this.redirectUrls, this.clientUrl, this.tosUrl, this.scopes, this.grantTypes,
                                                  this.responseTypes, this.defaultMaxAge, this.requireAuthTime);             
        }
        
    }
}

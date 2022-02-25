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
import java.time.OffsetDateTime;
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

/**
 * Readable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agent-registration">Social Agent Registration</a>.
 */
@Getter
public class ReadableSocialAgentRegistration extends ReadableAgentRegistration {

    private final URL reciprocalRegistration;
    
    /**
     * Construct a {@link ReadableSocialAgentRegistration} instance from the provided <code>url</code>.
     * @param url URL to generate the {@link ReadableSocialAgentRegistration} from
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    private ReadableSocialAgentRegistration(URL url, DataFactory dataFactory, Model dataset, Resource resource, ContentType contentType,
                                            URL registeredBy, URL registeredWith, OffsetDateTime registeredAt, OffsetDateTime updatedAt,
                                            URL registeredAgent, URL accessGrantUrl, URL reciprocalRegistration) throws SaiException {
        super(url, dataFactory, dataset, resource, contentType, registeredBy, registeredWith, registeredAt, updatedAt, registeredAgent, accessGrantUrl);
        this.reciprocalRegistration = reciprocalRegistration;
    }

    /**
     * Primary mechanism used to construct and bootstrap a {@link ReadableSocialAgentRegistration} from
     * the provided <code>url</code>.
     * @param url URL to generate the {@link ReadableSocialAgentRegistration} from
     * @param dataFactory {@link DataFactory} to assign
     * @param contentType {@link ContentType} to use for retrieval
     * @return {@link ReadableSocialAgentRegistration}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ReadableSocialAgentRegistration get(URL url, DataFactory dataFactory, ContentType contentType) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(url, "Must provide the URL of the readable social agent registration to get");
        Objects.requireNonNull(dataFactory, "Must provide a data factory to assign to the readable social agent registration");
        Objects.requireNonNull(contentType, "Must provide a content type to assign to the readable social agent registration");
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(dataFactory.getAuthorizedSession(), dataFactory.getHttpClient(), url, headers))) {
            return new ReadableSocialAgentRegistration.Builder(url, dataFactory, contentType, getRdfModelFromResponse(response)).build();
        }
    }

    /**
     * Call {@link #get(URL, DataFactory, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link ReadableSocialAgentRegistration} to get
     * @param dataFactory {@link DataFactory} to assign
     * @return Retrieved {@link ReadableSocialAgentRegistration}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ReadableSocialAgentRegistration get(URL url, DataFactory dataFactory) throws SaiException, SaiNotFoundException {
        return get(url, dataFactory, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Builder for {@link ReadableSocialAgentRegistration} instances.
     */
    private static class Builder {

        private final URL url;
        private final DataFactory dataFactory;
        private final ContentType contentType;
        private final Model dataset;
        private final Resource resource;
        private URL registeredBy;
        private URL registeredWith;
        private OffsetDateTime registeredAt;
        private OffsetDateTime updatedAt;
        private URL registeredAgent;
        private URL accessGrantUrl;
        URL reciprocalRegistration;

        /**
         * Initialize builder with <code>url</code> and <code>dataFactory</code>
         * @param url URL of the {@link ReadableSocialAgentRegistration} to build
         * @param dataFactory {@link DataFactory} to assign
         * @param contentType {@link ContentType} to assign
         * @param dataset Jena model to populate the readable social agent registration with
         */
        public Builder(URL url, DataFactory dataFactory, ContentType contentType, Model dataset) throws SaiException, SaiNotFoundException {
            Objects.requireNonNull(url, "Must provide a URL for the readable social agent registration builder");
            Objects.requireNonNull(dataFactory, "Must provide a data factory for the readable social agent registration builder");
            Objects.requireNonNull(contentType, "Must provide a content type to use for retrieval of readable social agent registration ");
            Objects.requireNonNull(dataset, "Must provide a dateset to use to populate the readable social agent registration ");
            this.url = url;
            this.dataFactory = dataFactory;
            this.contentType = contentType;
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            populateFromDataset();
        }

        /**
         * Populates the fields of the {@link ReadableSocialAgentRegistration} based on the associated Jena resource.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException, SaiNotFoundException {
            try {
                this.registeredBy = getRequiredUrlObject(this.resource, REGISTERED_BY);
                this.registeredWith = getRequiredUrlObject(this.resource, REGISTERED_WITH);
                this.registeredAt = getRequiredDateTimeObject(this.resource, REGISTERED_AT);
                this.updatedAt = getRequiredDateTimeObject(this.resource, UPDATED_AT);
                this.registeredAgent = getRequiredUrlObject(this.resource, REGISTERED_AGENT);
                this.reciprocalRegistration = getUrlObject(this.resource, RECIPROCAL_REGISTRATION);
                this.accessGrantUrl = getUrlObject(this.resource, HAS_ACCESS_GRANT);
            } catch (SaiNotFoundException | SaiException ex) {
                throw new SaiException("Failed to load readable social agent registration " + this.url + ": " + ex.getMessage());
            }
        }

        /**
         * Build the {@link ReadableSocialAgentRegistration} using attributes from the Builder.
         * @return {@link ReadableSocialAgentRegistration}
         * @throws SaiException
         */
        public ReadableSocialAgentRegistration build() throws SaiException {
            return new ReadableSocialAgentRegistration(this.url, this.dataFactory, this.dataset, this.resource, this.contentType,
                                                       this.registeredBy, this.registeredWith, this.registeredAt, this.updatedAt,
                                                       this.registeredAgent, this.accessGrantUrl, this.reciprocalRegistration);
        }
    }
}

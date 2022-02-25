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
 * Readable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#application-registration">Application Registration</a>.
 */
@Getter
public class ReadableApplicationRegistration extends ReadableAgentRegistration {
    
    /**
     * Construct a {@link ReadableApplicationRegistration} instance from the provided <code>url</code>.
     * @param url URL to generate the {@link ReadableApplicationRegistration} from
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    private ReadableApplicationRegistration(URL url, DataFactory dataFactory, Model dataset, Resource resource, ContentType contentType,
                                            URL registeredBy, URL registeredWith, OffsetDateTime registeredAt, OffsetDateTime updatedAt,
                                            URL registeredAgent, URL accessGrantUrl) throws SaiException {
        super(url, dataFactory, dataset, resource, contentType, registeredBy, registeredWith, registeredAt, updatedAt, registeredAgent, accessGrantUrl);
    }

    /**
     * Primary mechanism used to construct and bootstrap a {@link ReadableApplicationRegistration} from
     * the provided <code>url</code>.
     * @param url URL to generate the {@link ReadableApplicationRegistration} from
     * @param dataFactory {@link DataFactory} to assign
     * @param contentType {@link ContentType} to use for retrieval
     * @return {@link ReadableApplicationRegistration}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ReadableApplicationRegistration get(URL url, DataFactory dataFactory, ContentType contentType) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(url, "Must provide the URL of the readable application registration to get");
        Objects.requireNonNull(dataFactory, "Must provide a data factory to assign to the readable application registration");
        Objects.requireNonNull(contentType, "Must provide a content type to assign to the readable application registration");
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(dataFactory.getAuthorizedSession(), dataFactory.getHttpClient(), url, headers))) {
            return new ReadableApplicationRegistration.Builder(url, dataFactory, contentType, getRdfModelFromResponse(response)).build();
        }
    }

    /**
     * Call {@link #get(URL, DataFactory, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link ReadableApplicationRegistration} to get
     * @param dataFactory {@link DataFactory} to assign
     * @return Retrieved {@link ReadableApplicationRegistration}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ReadableApplicationRegistration get(URL url, DataFactory dataFactory) throws SaiException, SaiNotFoundException {
        return get(url, dataFactory, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Builder for {@link ReadableApplicationRegistration} instances.
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

        /**
         * Initialize builder with <code>url</code> and <code>dataFactory</code>
         * @param url URL of the {@link ReadableApplicationRegistration} to build
         * @param dataFactory {@link DataFactory} to assign
         * @param contentType {@link ContentType} to assign
         * @param dataset Jena model to populate the readable application registration with
         */
        public Builder(URL url, DataFactory dataFactory, ContentType contentType, Model dataset) throws SaiException, SaiNotFoundException {
            Objects.requireNonNull(url, "Must provide a URL for the readable application registration builder");
            Objects.requireNonNull(dataFactory, "Must provide a data factory for the readable application registration builder");
            Objects.requireNonNull(contentType, "Must provide a content type to use for retrieval of readable application registration ");
            Objects.requireNonNull(dataset, "Must provide a dateset to use to populate the readable application registration ");
            this.url = url;
            this.dataFactory = dataFactory;
            this.contentType = contentType;
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            populateFromDataset();
        }

        /**
         * Populates the fields of the {@link ReadableApplicationRegistration} based on the associated Jena resource.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException, SaiNotFoundException {
            try {
                this.registeredBy = getRequiredUrlObject(this.resource, REGISTERED_BY);
                this.registeredWith = getRequiredUrlObject(this.resource, REGISTERED_WITH);
                this.registeredAt = getRequiredDateTimeObject(this.resource, REGISTERED_AT);
                this.updatedAt = getRequiredDateTimeObject(this.resource, UPDATED_AT);
                this.registeredAgent = getRequiredUrlObject(this.resource, REGISTERED_AGENT);
                this.accessGrantUrl = getUrlObject(this.resource, HAS_ACCESS_GRANT);
            } catch (SaiNotFoundException | SaiException ex) {
                throw new SaiException("Failed to load readable application registration " + this.url + ": " + ex.getMessage());
            }
        }

        /**
         * Build the {@link ReadableApplicationRegistration} using attributes from the Builder.
         * @return {@link ReadableApplicationRegistration}
         * @throws SaiException
         */
        public ReadableApplicationRegistration build() throws SaiException {
            return new ReadableApplicationRegistration(this.url, this.dataFactory, this.dataset, this.resource, this.contentType,
                                                       this.registeredBy, this.registeredWith, this.registeredAt, this.updatedAt,
                                                       this.registeredAgent, this.accessGrantUrl);
        }
    }
}

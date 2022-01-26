package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.factories.DataFactory;
import com.janeirodigital.sai.core.exceptions.SaiException;
import lombok.Getter;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.getRdfModelFromResponse;
import static com.janeirodigital.sai.core.helpers.HttpHelper.getRdfResource;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getResourceFromModel;

/**
 * Represents a corresponding RDF Resource and provides read-only capabilities.
 */
@Getter
public class ReadableResource {

    protected final URL url;
    protected final DataFactory dataFactory;
    protected final OkHttpClient httpClient;
    protected Model dataset;
    protected Resource resource;
    protected boolean unprotected;

    /**
     * Construct a Readable resource for <code>resourceUrl</code>, assigning the provided
     * <code>dataFactory</code> for subsequent operations, along with an HTTP client to
     * facilitate them, and the associated credentials to use if the resource is protected
     * @param resourceUrl URL of the Readable resource
     * @param dataFactory Data factory to assign
     * @param unprotected When true no authorization credentials will be sent in requests to this resource
     */
    public ReadableResource(URL resourceUrl, DataFactory dataFactory, boolean unprotected) throws SaiException {
        Objects.requireNonNull(resourceUrl, "Must provide a URL for the target resource");
        Objects.requireNonNull(dataFactory, "Must provide a data factory");
        Objects.requireNonNull(dataFactory.getHttpClient(), "Must provide a valid HTTP client");
        this.url = resourceUrl;
        this.dataFactory = dataFactory;
        this.httpClient = dataFactory.getHttpClient();
        this.dataset = null;
        this.unprotected = unprotected;
    }

    /**
     * Calls {@link #ReadableResource(URL, DataFactory, boolean)} for a protected resource that
     * requires authorization.
     * @param resourceUrl URL of the Readable resource
     * @param dataFactory Data factory to assign
     */
    public ReadableResource(URL resourceUrl, DataFactory dataFactory) throws SaiException {
        this(resourceUrl, dataFactory, false);
    }

    /**
     * Populates the <code>dataset</code> based on the contents of the corresponding
     * resource requested and returned over HTTP.
     * @throws SaiException
     */
    protected void fetchData() throws SaiException {
        if (this.isUnprotected()) { this.fetchUnprotectedData(); } else {
            try (Response response = getProtectedRdfResource(this.dataFactory.getAuthorizedSession(), this.httpClient, this.url)) {
                // wrapping the call in try-with-resources automatically closes the response
                this.dataset = getRdfModelFromResponse(response);
                this.resource = getResourceFromModel(this.dataset, this.url);
            }
        }
    }

    /**
     * Fetches data and populates the dataset without sending any authorization headers
     * @throws SaiException
     */
    private void fetchUnprotectedData() throws SaiException {
        try (Response response = getRdfResource(this.httpClient, this.url)) {
            // wrapping the call in try-with-resources automatically closes the response
            this.dataset = getRdfModelFromResponse(response);
            this.resource = getResourceFromModel(this.dataset, this.url);
        }
    }

}

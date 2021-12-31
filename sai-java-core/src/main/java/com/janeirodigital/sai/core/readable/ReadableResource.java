package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.DataFactory;
import com.janeirodigital.sai.core.exceptions.SaiException;
import lombok.Getter;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.Objects;

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

    /**
     * Construct a Readable resource for <code>resourceUrl</code>, assigning the provided
     * <code>dataFactory</code> for subsequent operations, along with an HTTP client to
     * facilitate them.
     * @param resourceUrl URL of the Readable resource
     * @param dataFactory Data factory to assign
     */
    public ReadableResource(URL resourceUrl, DataFactory dataFactory) throws SaiException {
        Objects.requireNonNull(resourceUrl, "Must provide a URL for the target resource");
        Objects.requireNonNull(dataFactory, "Must provide a data factory");
        Objects.requireNonNull(dataFactory.getHttpClient(), "Must provide a valid HTTP client");
        this.url = resourceUrl;
        this.dataFactory = dataFactory;
        this.httpClient = dataFactory.getHttpClient();
        this.dataset = null;
    }

    /**
     * Populates the <code>dataset</code> based on the contents of the corresponding
     * resource requested and returned over HTTP.
     * @throws SaiException
     */
    protected void fetchData() throws SaiException {
        try (Response response = getRdfResource(this.httpClient, this.url)) {
            // wrapping the call in try-with-resources automatically closes the response
            this.dataset = getRdfModelFromResponse(response);
            this.resource = getResourceFromModel(this.dataset, this.url);
        }
    }

}

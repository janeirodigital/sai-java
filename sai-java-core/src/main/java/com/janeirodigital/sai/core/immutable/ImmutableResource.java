package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.DataFactory;
import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.exceptions.SaiException;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.Objects;

import static com.janeirodigital.sai.core.helpers.HttpHelper.*;

/**
 * Represents a corresponding RDF Resource and provides create, read, and
 * delete capabilities. Immutable resources don't provide an update capability.
 */
public class ImmutableResource {

    protected final URL url;
    protected final DataFactory dataFactory;
    protected final OkHttpClient httpClient;
    protected Model dataset;
    protected Resource resource;

    /**
     * Construct an immutable resource for <code>resourceUrl</code> based on the dataset
     * from the provided Jena <code>resource</code>.
     * @param resourceUrl URL of the immutable resource
     * @param dataFactory Data factory to assign
     * @param resource Jena resource to populate with
     */
    public ImmutableResource(URL resourceUrl, DataFactory dataFactory, Resource resource) {
        Objects.requireNonNull(resourceUrl, "Must provide a URL for the target resource");
        Objects.requireNonNull(dataFactory, "Must provide a data factory");
        Objects.requireNonNull(dataFactory.getHttpClient(), "Must provide a valid HTTP client");
        Objects.requireNonNull(resource, "Must provide a resource and model to populate an immutable resource");
        this.url = resourceUrl;
        this.dataFactory = dataFactory;
        this.httpClient = dataFactory.getHttpClient();
        this.dataset = resource.getModel();
        this.resource = resource;
    }

    /**
     * Create the corresponding resource over HTTP with the current contents
     * of <code>dataset</code>. If-None-Match header is used to ensure another
     * resource at <code>url</code> doesn't already exist.
     *
     * @throws SaiException
     */
    public void create() throws SaiException {
        Headers headers = setHttpHeader(HttpHeader.IF_NONE_MATCH, "*");
        putRdfResource(this.httpClient, this.url, this.resource, headers);
    }

}

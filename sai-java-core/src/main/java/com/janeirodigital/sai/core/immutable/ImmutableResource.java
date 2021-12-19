package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.DataFactory;
import com.janeirodigital.sai.core.exceptions.SaiException;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.Objects;

import static com.janeirodigital.sai.core.helpers.HttpHelper.addHeaderIfNoneMatch;
import static com.janeirodigital.sai.core.helpers.HttpHelper.putRdfResource;

public class ImmutableResource {

    protected final URL url;
    protected final DataFactory dataFactory;
    protected final OkHttpClient httpClient;
    protected Model dataset;
    protected Resource resource;

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

    public Response create() throws SaiException {
        Headers headers = addHeaderIfNoneMatch(null);
        return putRdfResource(this.httpClient, this.url, this.resource, headers);
    }

}

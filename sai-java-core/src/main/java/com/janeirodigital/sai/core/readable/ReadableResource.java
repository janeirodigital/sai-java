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

@Getter
public class ReadableResource {

    protected final URL url;
    protected final DataFactory dataFactory;
    protected final OkHttpClient httpClient;
    protected Model dataset;
    protected Resource resource;
    protected boolean container;

    public ReadableResource(URL resourceUrl, DataFactory dataFactory) {
        Objects.requireNonNull(resourceUrl, "Must provide a URL for the target resource");
        Objects.requireNonNull(dataFactory, "Must provide a data factory");
        Objects.requireNonNull(dataFactory.getHttpClient(), "Must provide a valid HTTP client");
        this.url = resourceUrl;
        this.dataFactory = dataFactory;
        this.httpClient = dataFactory.getHttpClient();
        this.dataset = null;
        this.container = false;
    }

    protected Response fetchData() throws SaiException {
        Response response = getRdfResource(this.httpClient, this.url);
        this.dataset = getRdfModelFromResponse(response);
        this.resource = getResourceFromModel(this.dataset, this.url);
        return response;
    }

}

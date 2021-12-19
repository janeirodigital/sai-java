package com.janeirodigital.sai.core.helpers;

import com.janeirodigital.sai.core.exceptions.SaiException;
import okhttp3.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Set;

import static com.janeirodigital.sai.core.enums.HttpHeaders.LINK;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getModelFromString;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getStringFromRdfModel;
import static com.janeirodigital.sai.core.vocabularies.LdpVocabulary.LDP_NS;

/**
 * Assorted helper methods related to working with HTTP requests and responses
 */
public class HttpHelper {

    private static final String PUT = "PUT";
    private static final String POST = "POST";
    private static final String GET = "GET";
    private static final String DELETE = "DELETE";
    private static final String PATCH = "PATCH";

    private static final String TEXT_TURTLE = "text/turtle";
    private static final String RDF_XML = "application/rdf+xml";
    private static final String N_TRIPLES = "application/n-triples";
    private static final String LD_JSON = "application/ld+json";
    private static final Set<String> RDF_CONTENT_TYPES = Set.of(TEXT_TURTLE, RDF_XML, N_TRIPLES, LD_JSON);

    public static Response getResource(OkHttpClient httpClient, URL url) throws SaiException {
        Response response;
        try {
            Request.Builder requestBuilder = new Request.Builder();
            requestBuilder.url(url);
            requestBuilder.method("GET", null);
            response = checkResponse(httpClient.newCall(requestBuilder.build()).execute());
        } catch (IOException ex) {
            throw new SaiException("Failed to lookup remote resource: " + ex.getMessage());
        }
        return response;
    }

    public static Response putResource(OkHttpClient httpClient, URL url, Headers headers, String body, String contentType) throws SaiException {
        Response response;
        try {
            Request.Builder requestBuilder = new Request.Builder();
            requestBuilder.url(url);
            RequestBody requestBody = RequestBody.create(body, MediaType.get(contentType));
            requestBuilder.method("PUT", requestBody);
            if (headers != null) { requestBuilder.headers(headers); }
            response = checkResponse(httpClient.newCall(requestBuilder.build()).execute());
        } catch (IOException | SaiException ex) {
            throw new SaiException("Failed to put remote resource: " + ex.getMessage());
        }
        return response;
    }

    public static Response deleteResource(OkHttpClient httpClient, URL url) throws SaiException {
        Response response;
        try {
            Request.Builder requestBuilder = new Request.Builder();
            requestBuilder.url(url);
            requestBuilder.method("DELETE", null);
            response = checkResponse(httpClient.newCall(requestBuilder.build()).execute());
        } catch (IOException | SaiException ex) {
            throw new SaiException("Failed to delete remote resource: " + ex.getMessage());
        }
        return response;
    }

    public static Response getRdfResource(OkHttpClient httpClient, URL url) throws SaiException {
        return checkRdfResponse(getResource(httpClient, url));
    }

    public static Model getRdfModelFromResponse(Response response) throws SaiException {
        checkRdfResponse(response);
        String body;
        HttpUrl requestUrl = response.request().url();
        try { body = response.body().string(); } catch (IOException ex) {
            throw new SaiException("Failed to access response body");
        }
        return getModelFromString(requestUrlToUri(requestUrl), body, getContentType(response));
    }

    public static Response putRdfResource(OkHttpClient httpClient, URL url, Resource resource) throws SaiException {
        return putRdfResource(httpClient, url, resource, null);
    }

    public static Response putRdfResource(OkHttpClient httpClient, URL url, Resource resource, Headers headers) throws SaiException {
        // TODO - should content type of turtle be hardcoded here?
        String body = "";
        // Treat a null resource as an empty body
        if (resource != null) { body = getStringFromRdfModel(resource.getModel(), Lang.TURTLE); }
        return checkResponse(putResource(httpClient, url, headers, body, TEXT_TURTLE));
    }

    public static Response putRdfContainer(OkHttpClient httpClient, URL url, Resource resource) throws SaiException {
        Headers.Builder builder = new Headers.Builder();
        builder.add(LINK + ":<" + LDP_NS + ">", "rel=type");
        Headers headers = builder.build();
        return putRdfResource(httpClient, url, resource, headers);
    }

    public static Headers addHeaderIfNoneMatch(Headers headers) {
        Headers.Builder builder = new Headers.Builder();
        if (headers != null) { builder.addAll(headers); }
        builder.add("If-None-Match", "*");
        return builder.build();
    }

    protected static Response checkResponse(Response response) throws SaiException {
        if (response == null || !response.isSuccessful()) {
            throw new SaiException("Failed to lookup remote resource: " + response.request().url());
        }
        return response;
    }

    protected static Response checkRdfResponse(Response response) throws SaiException {
        checkResponse(response);
        String contentType = getContentType(response);
        if (!RDF_CONTENT_TYPES.contains(contentType)) {
            throw new SaiException("Invalid Content-Type for RDF resource: " + contentType);
        }
        return response;
    }

    protected static String getContentType(Response response) throws SaiException {
        if (response.header("Content-Type") == null) {
            throw new SaiException("Content-type header is missing");
        }
        return response.header("Content-Type");
    }

    /**
     * Wrap conversion from URL to URI which should never fail on a well-formed URL.
     * @param url covert this URL to a URI
     * @return IRI java native object for a URI (useful for Jena graph operations)
     */
    public static URI urlToUri(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException ex) {
            throw new IllegalStateException("can't convert URL <" + url + "> to IRI: " + ex);
        }
    }

    /**
     * Convenience wrapper around urlToUri which takes the OkHttp HttpUrl type
     * as input.
     * @param url HttpUrl from OkHttp
     * @return IRI java native object for a URI (useful for Jena graph operations)
     */
    public static URI requestUrlToUri(HttpUrl url) {
        return urlToUri(url.url());
    }

}

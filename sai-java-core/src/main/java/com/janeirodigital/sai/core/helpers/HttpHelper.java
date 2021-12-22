package com.janeirodigital.sai.core.helpers;

import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.enums.LinkRelation;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.vocabularies.LdpVocabulary;
import okhttp3.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.Set;

import static com.janeirodigital.sai.core.enums.HttpHeader.LINK;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getModelFromString;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getStringFromRdfModel;

/**
 * Assorted helper methods related to working with HTTP requests and responses
 */
public class HttpHelper {

    private static final String PUT = "PUT";
    private static final String GET = "GET";
    private static final String DELETE = "DELETE";

    private static final String TEXT_TURTLE = "text/turtle";
    private static final String RDF_XML = "application/rdf+xml";
    private static final String N_TRIPLES = "application/n-triples";
    private static final String LD_JSON = "application/ld+json";
    private static final Set<String> RDF_CONTENT_TYPES = Set.of(TEXT_TURTLE, RDF_XML, N_TRIPLES, LD_JSON);

    private HttpHelper() { }

    // The response MUST be closed outside of this call
    public static Response getResource(OkHttpClient httpClient, URL url) throws SaiException {
        try {
            Request.Builder requestBuilder = new Request.Builder();
            requestBuilder.url(url);
            requestBuilder.method(GET, null);
            return checkResponse(httpClient.newCall(requestBuilder.build()).execute());
        } catch (IOException ex) {
            throw new SaiException("Failed to lookup remote resource: " + ex.getMessage());
        }
    }

    public static Response getRequiredResource(OkHttpClient httpClient, URL url) throws SaiException, SaiNotFoundException {
        Response response = getResource(httpClient, url);
        if (!response.isSuccessful()) {
            if (response.code() == 404) {
                throw new SaiNotFoundException("No resource found at " + response.request().url());
            } else {
                throw new SaiException("HTTP " + response.request().method() + "operation failed on " + response.request().url());
            }
        }
        return response;
    }

    public static Response putResource(OkHttpClient httpClient, URL url, Headers headers, String body, String contentType) throws SaiException {

        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(url);
        RequestBody requestBody = RequestBody.create(body, MediaType.get(contentType));
        requestBuilder.method(PUT, requestBody);
        if (headers != null) { requestBuilder.headers(headers); }

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            // wrapping the call in try-with-resources automatically closes the response
            return checkResponse(response);
        } catch (IOException | SaiException ex) {
            throw new SaiException("Failed to put remote resource: " + ex.getMessage());
        }
    }

    public static Response deleteResource(OkHttpClient httpClient, URL url) throws SaiException {

        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(url);
        requestBuilder.method(DELETE, null);

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            // wrapping the call in try-with-resources automatically closes the response
            return checkResponse(response);
        } catch (IOException | SaiException ex) {
            throw new SaiException("Failed to delete remote resource: " + ex.getMessage());
        }

    }

    public static Response getRdfResource(OkHttpClient httpClient, URL url) throws SaiException {
        return checkRdfResponse(getResource(httpClient, url));
    }

    public static Model getRdfModelFromResponse(Response response) throws SaiException {
        checkRdfResponse(response);
        String body;
        HttpUrl requestUrl = response.request().url();
        try { body = response.peekBody(Long.MAX_VALUE).string(); } catch (IOException ex) {
            throw new SaiException("Failed to access response body");
        }
        return getModelFromString(requestUrlToUri(requestUrl), body, getContentType(response));
    }

    public static Response putRdfResource(OkHttpClient httpClient, URL url, Resource resource) throws SaiException {
        return putRdfResource(httpClient, url, resource, null);
    }

    public static Response putRdfResource(OkHttpClient httpClient, URL url, Resource resource, Headers headers) throws SaiException {
        // TODO - May not want to hard-code a content type of turtle here
        String body = "";
        // Treat a null resource as an empty body
        if (resource != null) { body = getStringFromRdfModel(resource.getModel(), Lang.TURTLE); }
        return checkResponse(putResource(httpClient, url, headers, body, TEXT_TURTLE));
    }

    public static Response putRdfContainer(OkHttpClient httpClient, URL url, Resource resource) throws SaiException {
        Headers headers = addLinkRelationHeader(LinkRelation.TYPE, LdpVocabulary.BASIC_CONTAINER.getURI());
        return putRdfResource(httpClient, url, resource, headers);
    }

    public static Headers setHttpHeader(HttpHeader name, String value, Headers headers) {
        Headers.Builder builder = new Headers.Builder();
        if (headers != null) { builder.addAll(headers); }
        builder.set(name.getValue(), value);
        return builder.build();
    }

    public static Headers setHttpHeader(HttpHeader name, String value) {
        return setHttpHeader(name, value, null);
    }

    public static Headers addHttpHeader(HttpHeader name, String value, Headers headers) {
        Headers.Builder builder = new Headers.Builder();
        if (headers != null) { builder.addAll(headers); }
        builder.add(name.getValue(), value);
        return builder.build();
    }

    public static Headers addHttpHeader(HttpHeader name, String value) {
        return addHttpHeader(name, value, null);
    }

    public static Headers addLinkRelationHeader(LinkRelation type, String target, Headers headers) {
        return addHttpHeader(LINK, getLinkRelationString(type, target), headers);
    }

    public static Headers addLinkRelationHeader(LinkRelation type, String target) {
        return addLinkRelationHeader(type, target, null);
    }

    public static String getLinkRelationString(LinkRelation type, String target) {
        return "<"+target+">;"+" rel=\""+type.getValue()+"\"";
    }

    protected static Response checkResponse(Response response) throws SaiException {
        Objects.requireNonNull(response, "Do not expect to receive a null response to an HTTP client request");
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

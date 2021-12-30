package com.janeirodigital.sai.core.helpers;

import com.janeirodigital.sai.core.enums.ContentType;
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

import static com.janeirodigital.sai.core.enums.ContentType.*;
import static com.janeirodigital.sai.core.enums.HttpHeader.LINK;
import static com.janeirodigital.sai.core.enums.HttpMethod.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getModelFromString;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getStringFromRdfModel;

/**
 * Assorted helper methods related to working with HTTP requests and responses
 * @see <a href="https://square.github.io/okhttp/4.x/okhttp/okhttp3/">OkHttp</a>
 * @see <a href="https://square.github.io/okhttp/4.x/okhttp/okhttp3/-request/">OkHttp - Request</a>
 * @see <a href="https://square.github.io/okhttp/4.x/okhttp/okhttp3/-response/">OkHttp - Response</a>
 * @see <a href="https://square.github.io/okhttp/4.x/okhttp/okhttp3/-headers/">OkHttp - Headers</a>
 * @see <a href="https://square.github.io/okhttp/4.x/okhttp/okhttp3/-response-body/">OkHttp - ResponseBody</a>
 */
public class HttpHelper {

    private static final Set<ContentType> RDF_CONTENT_TYPES = Set.of(TEXT_TURTLE, RDF_XML, N_TRIPLES, LD_JSON);

    private HttpHelper() { }

    /**
     * Perform an HTTP GET on the resource at <code>url</code>.
     * The response MUST be closed outside of this call. The body of the response
     * is returned as a one-shot stream, and <b>MUST BE CLOSED</b> separately
     * by the caller.
     * @see <a href="https://square.github.io/okhttp/4.x/okhttp/okhttp3/-response-body/#the-response-body-must-be-closed">OkHttp - Closing the Response Body</a>
     * @param httpClient OkHttpClient to perform the GET with
     * @param url URL of the resource to GET
     * @return OkHttp Response
     * @throws SaiException
     */
    public static Response getResource(OkHttpClient httpClient, URL url) throws SaiException {
        try {
            Request.Builder requestBuilder = new Request.Builder();
            requestBuilder.url(url);
            requestBuilder.method(GET.getValue(), null);
            return checkResponse(httpClient.newCall(requestBuilder.build()).execute());
        } catch (IOException ex) {
            throw new SaiException("Failed to lookup remote resource: " + ex.getMessage());
        }
    }

    /**
     * Perform an HTTP GET on the resource at <code>url</code>, and throws an exception if the resource
     * cannot be found or the response is otherwise unsuccessful. The body of the response
     * is returned as a one-shot stream, and <b>MUST BE CLOSED</b> separately
     * by the caller.
     * @see <a href="https://square.github.io/okhttp/4.x/okhttp/okhttp3/-response-body/#the-response-body-must-be-closed">OkHttp - Closing the Response Body</a>
     * @param httpClient OkHttpClient to perform the GET with
     * @param url URL of the resource to GET
     * @return OkHttp Response
     * @throws SaiException
     * @throws SaiNotFoundException
     */
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

    /**
     * Perform an HTTP PUT on the resource at <code>url</code>.
     * <i>ResponseBody is closed automatically</i>.
     * @param httpClient OkHttpClient to perform the PUT with
     * @param url URL of the resource to PUT
     * @param headers Optional OkHttp Headers to include
     * @param body Body of the PUT request
     * @param contentType {@link ContentType} of the PUT request
     * @return OkHttp Response
     * @throws SaiException
     */
    public static Response putResource(OkHttpClient httpClient, URL url, Headers headers, String body, ContentType contentType) throws SaiException {

        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(url);
        if (body == null) { body = ""; }
        RequestBody requestBody = RequestBody.create(body, MediaType.get(contentType.getValue()));
        requestBuilder.method(PUT.getValue(), requestBody);
        if (headers != null) { requestBuilder.headers(headers); }

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            // wrapping the call in try-with-resources automatically closes the response
            return checkResponse(response);
        } catch (IOException ex) {
            throw new SaiException("Failed to put remote resource: " + ex.getMessage());
        }
    }

    /**
     * Perform an HTTP DELETE on the resource at <code>url</code>.
     * <i>ResponseBody is closed automatically</i>.
     * @param httpClient OkHttpClient to perform the DELETE with
     * @param url URL of the resource to DELETE
     * @return OkHttp Response
     * @throws SaiException
     */
    public static Response deleteResource(OkHttpClient httpClient, URL url) throws SaiException {

        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(url);
        requestBuilder.method( DELETE.getValue(), null);

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            // wrapping the call in try-with-resources automatically closes the response
            return checkResponse(response);
        } catch (IOException ex) {
            throw new SaiException("Failed to delete remote resource: " + ex.getMessage());
        }

    }

    /**
     * Perform an HTTP GET on an RDF resource at <code>url</code>. Checks that the
     * response is representative of an RDF resource.
     * @param httpClient OkHttpClient to perform the GET with
     * @param url URL of the resource to GET
     * @return OkHttp Response
     * @throws SaiException
     */
    public static Response getRdfResource(OkHttpClient httpClient, URL url) throws SaiException {
        return checkRdfResponse(getResource(httpClient, url));
    }

    /**
     * Get a Jena RDF Model from the body of the OkHttp Response.
     * @param response OkHttp Response
     * @return Jena Model
     * @throws SaiException
     */
    public static Model getRdfModelFromResponse(Response response) throws SaiException {
        checkRdfResponse(response);
        String body;
        HttpUrl requestUrl = response.request().url();
        try { body = response.peekBody(Long.MAX_VALUE).string(); } catch (IOException ex) {
            throw new SaiException("Failed to access response body");
        }
        return getModelFromString(requestUrlToUri(requestUrl), body, getContentType(response));
    }

    /**
     * Perform an HTTP PUT on the resource at <code>url</code> using a serialized Jena
     * Resource as the request body.
     * @param httpClient OkHttpClient to perform the PUT with
     * @param url URL of the resource to PUT
     * @param resource Jena Resource for the request body
     * @return OkHttp Response
     * @throws SaiException
     */
    public static Response putRdfResource(OkHttpClient httpClient, URL url, Resource resource) throws SaiException {
        return putRdfResource(httpClient, url, resource, null);
    }

    /**
     * Perform an HTTP PUT with optional headers on the resource at <code>url</code> using a serialized Jena
     * Resource as the request body.
     * @param httpClient OkHttpClient to perform the PUT with
     * @param url URL of the resource to PUT
     * @param resource Jena Resource for the request body
     * @param headers Optional OkHttp Headers
     * @return OkHttp Response
     * @throws SaiException
     */
    public static Response putRdfResource(OkHttpClient httpClient, URL url, Resource resource, Headers headers) throws SaiException {
        String body = "";
        // Treat a null resource as an empty body
        // REVISIT - May not want to hard-code a content type of turtle here
        if (resource != null) { body = getStringFromRdfModel(resource.getModel(), Lang.TURTLE); }
        return checkResponse(putResource(httpClient, url, headers, body, TEXT_TURTLE));
    }

    /**
     * Perform an HTTP PUT on the resource at <code>url</code> treated as a Basic Container,
     * with a serialized Jena Resource as the request body.
     * @param httpClient OkHttpClient to perform the PUT with
     * @param url URL of the resource to PUT
     * @param resource Jena Resource for the request body
     * @return OkHttp Response
     * @throws SaiException
     */
    public static Response putRdfContainer(OkHttpClient httpClient, URL url, Resource resource) throws SaiException {
        Headers headers = addLinkRelationHeader(LinkRelation.TYPE, LdpVocabulary.BASIC_CONTAINER.getURI());
        return putRdfResource(httpClient, url, resource, headers);
    }

    /**
     * Set the HTTP header identified by <code>name</code> with the provided <code>value</code>. If
     * <code>headers</code> are provided, they will be included in the Headers returned. If
     * the Header to set already exists in that set, it will be updated.
     * @param name {@link HttpHeader} to set
     * @param value Value to use for the header
     * @param headers Optional OkHttp Headers to include
     * @return Updated OkHttp Headers
     */
    public static Headers setHttpHeader(HttpHeader name, String value, Headers headers) {
        Headers.Builder builder = new Headers.Builder();
        if (headers != null) { builder.addAll(headers); }
        builder.set(name.getValue(), value);
        return builder.build();
    }

    /**
     * Set the HTTP header identified by <code>name</code> with the provided <code>value</code>.
     * @param name {@link HttpHeader} to set
     * @param value Value to use for the header
     * @return Updated OkHttp Headers
     */
    public static Headers setHttpHeader(HttpHeader name, String value) {
        return setHttpHeader(name, value, null);
    }

    /**
     * Add the HTTP header identified by <code>name</code> with the provided <code>value</code>.
     * If <code>headers</code> are provided, they will be included in the Headers returned.
     * @param name {@link HttpHeader} to add
     * @param value Value to use for the added header
     * @param headers Optional OkHttp Headers to include
     * @return Populated OkHttp Headers
     */
    public static Headers addHttpHeader(HttpHeader name, String value, Headers headers) {
        Headers.Builder builder = new Headers.Builder();
        if (headers != null) { builder.addAll(headers); }
        builder.add(name.getValue(), value);
        return builder.build();
    }

    /**
     * Add the HTTP header identified by <code>name</code> with the provided <code>value</code>.
     * @param name {@link HttpHeader} to add
     * @param value Value to use for the added header
     * @return Populated OkHttp Headers
     */
    public static Headers addHttpHeader(HttpHeader name, String value) {
        return addHttpHeader(name, value, null);
    }

    /**
     * Add an HTTP Link Relation header of <code>type</code> with the provided <code>target</code>.
     * If <code>headers</code> are provided, they will be included in the Headers returned.
     * @see <a href="https://www.rfc-editor.org/rfc/rfc8288.html">RFC 8288 - Web Linking</a>
     * @param type Link relation type
     * @param target Link relation target
     * @param headers Optional OkHttp Headers to include
     * @return Populated OkHttp Headers
     */
    public static Headers addLinkRelationHeader(LinkRelation type, String target, Headers headers) {
        return addHttpHeader(LINK, getLinkRelationString(type, target), headers);
    }

    /**
     * Add an HTTP Link Relation header of <code>type</code> with the provided <code>target</code>.
     * @see <a href="https://www.rfc-editor.org/rfc/rfc8288.html">RFC 8288 - Web Linking</a>
     * @param type Link relation type
     * @param target Link relation target
     * @return Populated OkHttp Headers
     */
    public static Headers addLinkRelationHeader(LinkRelation type, String target) {
        return addLinkRelationHeader(type, target, null);
    }

    /**
     * Get a formatted HTTP Link Relation string in compliance with RFC 8288
     * @param type Link relation type
     * @param target Link relation target
     * @return Formatted Link Relation string
     */
    public static String getLinkRelationString(LinkRelation type, String target) {
        return "<"+target+">;"+" rel=\""+type.getValue()+"\"";
    }

    /**
     * Check the provided <code>response</code> for viability
     * @param response Response to check
     * @return Checked response
     */
    protected static Response checkResponse(Response response) {
        Objects.requireNonNull(response, "Do not expect to receive a null response to an HTTP client request");
        return response;
    }

    /**
     * Check the provided <code>response</code> for viable for an RDF resource.
     * @param response Response to check
     * @return Checked response
     * @throws SaiException
     */
    protected static Response checkRdfResponse(Response response) throws SaiException {
        checkResponse(response);
        ContentType contentType = getContentType(response);
        if (!RDF_CONTENT_TYPES.contains(contentType)) {
            throw new SaiException("Invalid Content-Type for RDF resource: " + contentType);
        }
        return response;
    }

    /**
     * Get the HTTP Content-Type of the response
     * @param response Response to get Content-Type from
     * @return {@link ContentType} of response
     * @throws SaiException
     */
    protected static ContentType getContentType(Response response) throws SaiException {
        if (response.header(HttpHeader.CONTENT_TYPE.getValue()) == null) {
            throw new SaiException("Content-type header is missing");
        }
        String responseType = response.header(HttpHeader.CONTENT_TYPE.getValue());
        ContentType contentType = ContentType.get(responseType);
        if (contentType == null) { throw new SaiException("Unrecognized content-type"); }
        return contentType;
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

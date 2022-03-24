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
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;
import java.util.Set;

import static com.janeirodigital.sai.core.enums.ContentType.*;
import static com.janeirodigital.sai.core.enums.HttpHeader.CONTENT_TYPE;
import static com.janeirodigital.sai.core.enums.HttpHeader.LINK;
import static com.janeirodigital.sai.core.enums.HttpMethod.*;
import static com.janeirodigital.sai.core.helpers.RdfUtils.*;

/**
 * Assorted helper methods related to working with HTTP requests and responses
 * @see <a href="https://square.github.io/okhttp/4.x/okhttp/okhttp3/">OkHttp</a>
 * @see <a href="https://square.github.io/okhttp/4.x/okhttp/okhttp3/-request/">OkHttp - Request</a>
 * @see <a href="https://square.github.io/okhttp/4.x/okhttp/okhttp3/-response/">OkHttp - Response</a>
 * @see <a href="https://square.github.io/okhttp/4.x/okhttp/okhttp3/-headers/">OkHttp - Headers</a>
 * @see <a href="https://square.github.io/okhttp/4.x/okhttp/okhttp3/-response-body/">OkHttp - ResponseBody</a>
 */
public class HttpHelper {

    public static final Set<ContentType> RDF_CONTENT_TYPES = Set.of(TEXT_TURTLE, RDF_XML, N_TRIPLES, LD_JSON);
    public static final ContentType DEFAULT_RDF_CONTENT_TYPE = TEXT_TURTLE;

    private HttpHelper() { }

    /**
     * Perform an HTTP GET on the resource at <code>url</code>.
     * The response MUST be closed outside of this call. The body of the response
     * is returned as a one-shot stream, and <b>MUST BE CLOSED</b> separately
     * by the caller.
     * @see <a href="https://square.github.io/okhttp/4.x/okhttp/okhttp3/-response-body/#the-response-body-must-be-closed">OkHttp - Closing the Response Body</a>
     * @param httpClient OkHttpClient to perform the GET with
     * @param url URL of the resource to GET
     * @param headers Optional OkHttp Headers to include
     * @return OkHttp Response
     * @throws SaiException
     */
    public static Response getResource(OkHttpClient httpClient, URL url, Headers headers) throws SaiException {
        Objects.requireNonNull(httpClient, "Must provide an http client to access resource");
        Objects.requireNonNull(url, "Must provide a target URL to access resource");
        try {
            Request.Builder requestBuilder = new Request.Builder();
            requestBuilder.url(url);
            requestBuilder.method(GET.getValue(), null);
            if (headers != null) { requestBuilder.headers(headers); }
            return checkResponse(httpClient.newCall(requestBuilder.build()).execute());
        } catch (IOException ex) {
            throw new SaiException("Failed to get remote resource: " + ex.getMessage());
        }
    }

    /**
     * Calls {@link #getResource(OkHttpClient, URL, Headers)} without any additional
     * headers supplied.
     * @param httpClient OkHttpClient to perform the GET with
     * @param url URL of the resource to GET
     * @return OkHttp Response
     * @throws SaiException
     */
    public static Response getResource(OkHttpClient httpClient, URL url) throws SaiException {
        return getResource(httpClient, url, null);
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

        Objects.requireNonNull(httpClient, "Must provide an http client to access resource");
        Objects.requireNonNull(url, "Must provide a target URL to access resource");
        Objects.requireNonNull(contentType, "Must provide a content type to create resource");

        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(url);
        if (body == null) { body = ""; }
        RequestBody requestBody = RequestBody.create(body, MediaType.get(contentType.getValue()));
        requestBuilder.method(PUT.getValue(), requestBody);
        headers = setHttpHeader(CONTENT_TYPE, contentType.getValue(), headers);
        requestBuilder.headers(headers);
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
     * @param headers Optional OkHttp headers to include
     * @return OkHttp Response
     * @throws SaiException
     */
    public static Response deleteResource(OkHttpClient httpClient, URL url, Headers headers) throws SaiException {

        Objects.requireNonNull(httpClient, "Must provide an http client to access resource");
        Objects.requireNonNull(url, "Must provide a target URL to access resource");

        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(url);
        requestBuilder.method( DELETE.getValue(), null);
        if (headers != null) { requestBuilder.headers(headers); }

        try (Response response = httpClient.newCall(requestBuilder.build()).execute()) {
            // wrapping the call in try-with-resources automatically closes the response
            return checkResponse(response);
        } catch (IOException ex) {
            throw new SaiException("Failed to delete remote resource: " + ex.getMessage());
        }

    }

    /**
     * Calls {@link #deleteResource(OkHttpClient, URL, Headers)} without any additional headers supplied.
     * @param httpClient OkHttpClient to perform the DELETE with
     * @param url URL of the resource to DELETE
     * @return OkHttp Response
     * @throws SaiException
     */
    public static Response deleteResource(OkHttpClient httpClient, URL url) throws SaiException {
        return deleteResource(httpClient, url, null);
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
     * Perform an HTTP GET on an RDF resource at <code>url</code>. Checks that the
     * response is representative of an RDF resource. Requires the resource to be found,
     * or an exception is thrown.
     * @param httpClient OkHttpClient to perform the GET with
     * @param url URL of the resource to GET
     * @return OkHttp Response
     * @throws SaiException
     * @throws SaiNotFoundException when no resource is found
     */
    public static Response getRequiredRdfResource(OkHttpClient httpClient, URL url) throws SaiException, SaiNotFoundException {
        return checkRdfResponse(getRequiredResource(httpClient, url));
    }

    /**
     * Perform an HTTP GET on an RDF resource at <code>url</code>. Checks that the
     * response is representative of an RDF resource.
     * @param httpClient OkHttpClient to perform the GET with
     * @param url URL of the resource to GET
     * @param headers Optional OkHttp headers to include
     * @return OkHttp Response
     * @throws SaiException
     */
    public static Response getRdfResource(OkHttpClient httpClient, URL url, Headers headers) throws SaiException {
        return checkRdfResponse(getResource(httpClient, url, headers));
    }

    /**
     * Get a Jena RDF Model from the body of the OkHttp Response.
     * @param response OkHttp Response
     * @return Jena Model
     * @throws SaiException
     */
    public static Model getRdfModelFromResponse(Response response) throws SaiException {
        Objects.requireNonNull(response, "Must provide a response to get RDF model from");
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
     * @param contentType ContentType of the request
     * @return OkHttp Response
     * @throws SaiException
     */
    public static Response putRdfResource(OkHttpClient httpClient, URL url, Resource resource, ContentType contentType) throws SaiException {
        return putRdfResource(httpClient, url, resource, contentType, null, null);
    }

    /**
     * Perform an HTTP PUT on the resource at <code>url</code> using a serialized Jena
     * Resource as the request body with a jsonLdContext
     * @param httpClient OkHttpClient to perform the PUT with
     * @param url URL of the resource to PUT
     * @param resource Jena Resource for the request body
     * @param contentType ContentType of the request
     * @param jsonLdContext JSON-LD context string to include
     * @return OkHttp Response
     * @throws SaiException
     */
    public static Response putRdfResource(OkHttpClient httpClient, URL url, Resource resource, ContentType contentType, String jsonLdContext) throws SaiException {
        return putRdfResource(httpClient, url, resource, contentType, jsonLdContext, null);
    }

    /**
     * Perform an HTTP PUT with optional headers on the resource at <code>url</code> using a serialized Jena
     * Resource as the request body.
     * @param httpClient OkHttpClient to perform the PUT with
     * @param url URL of the resource to PUT
     * @param resource Jena Resource for the request body
     * @param contentType ContentType of the request
     * @param headers Optional OkHttp Headers
     * @return OkHttp Response
     * @throws SaiException
     */
    public static Response putRdfResource(OkHttpClient httpClient, URL url, Resource resource, ContentType contentType, Headers headers) throws SaiException {
        return putRdfResource(httpClient, url, resource, contentType, null, headers);
    }

    /**
     * Perform an HTTP PUT with optional headers on the resource at <code>url</code> using a serialized Jena
     * Resource as the request body.
     * @param httpClient OkHttpClient to perform the PUT with
     * @param url URL of the resource to PUT
     * @param resource Jena Resource for the request body
     * @param contentType ContentType of the request
     * @param jsonLdContext Optional JSON-LD context string to include
     * @param headers Optional OkHttp Headers
     * @return OkHttp Response
     * @throws SaiException
     */
    public static Response putRdfResource(OkHttpClient httpClient, URL url, Resource resource, ContentType contentType, String jsonLdContext, Headers headers) throws SaiException {
        Objects.requireNonNull(contentType, "Must provide a content-type for the PUT request on an RDF document");
        String body = "";
        Lang lang = getLangForContentType(contentType);
        if (resource != null) {
            if (lang.equals(Lang.JSONLD11)) {
                body = getJsonLdStringFromModel(resource.getModel(), jsonLdContext);
            } else {
                body = getStringFromRdfModel(resource.getModel(), lang);
            }
        }
        return checkResponse(putResource(httpClient, url, headers, body, contentType));
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
    public static Response putRdfContainer(OkHttpClient httpClient, URL url, Resource resource, ContentType contentType, String jsonLdContext) throws SaiException {
        Headers headers = addLinkRelationHeader(LinkRelation.TYPE, LdpVocabulary.LDP_BASIC_CONTAINER.getURI());
        return putRdfResource(httpClient, url, resource, contentType, jsonLdContext, headers);
    }

    public static Response putRdfContainer(OkHttpClient httpClient, URL url, Resource resource, ContentType contentType) throws SaiException {
        return putRdfContainer(httpClient, url, resource, contentType, null);
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
        Objects.requireNonNull(name, "Must provide an http header to set");
        Objects.requireNonNull(value, "Must provide a value for http header");
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
        Objects.requireNonNull(name, "Must provide an http header to add");
        Objects.requireNonNull(value, "Must provide a value for http header");
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
        Objects.requireNonNull(type, "Must provide a link relation type");
        Objects.requireNonNull(target, "Must provide a link relation target");
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
        // if it isn't a successful response there's nothing to check, but leave it to higher
        // levels of the stack to determine whether that is an exceptional condition
        if (!response.isSuccessful()) { return response; }
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
        Objects.requireNonNull(response, "Must provide a response to get content type for");
        if (response.header(HttpHeader.CONTENT_TYPE.getValue()) == null) {
            throw new SaiException("Content-type header is missing");
        }
        String responseType = response.header(HttpHeader.CONTENT_TYPE.getValue());
        ContentType contentType = ContentType.get(responseType);
        if (contentType == null) { contentType = OCTET_STREAM; }
        return contentType;
    }

    /**
     * Wrap conversion from URL to URI which should never fail on a well-formed URL.
     * @param url covert this URL to a URI
     * @return IRI java native object for a URI (useful for Jena graph operations)
     */
    public static URI urlToUri(URL url) {
        Objects.requireNonNull(url, "Must provide a URL to convert");
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
        Objects.requireNonNull(url, "Must provide a URL to convert");
        return urlToUri(url.url());
    }

    /**
     * Returns the scheme, domain name, port, and path of a URL, removing
     * any query parameters or fragments.
     * @param url to trim
     * @return Base URL
     */
    public static URL urlToBase(URL url) throws SaiException {
        Objects.requireNonNull(url, "Must provide a URL to convert");
        try {
            URI uri = urlToUri(url);
            if (uri.getFragment() == null && uri.getQuery() == null) { return url; }
            URI trimmed = new URI(uri.getScheme(), uri.getHost(), uri.getPath(), null);
            return trimmed.toURL();
        } catch(MalformedURLException|URISyntaxException|IllegalStateException ex) {
            throw new SaiException("Unable to convert URL to Base URL: " + ex.getMessage());
        }
    }

    /**
     * Converts a string to a URL
     * @param urlString String to convert to URL
     * @return Converted URL
     * @throws SaiException
     */
    public static URL stringToUrl(String urlString) throws SaiException {
        Objects.requireNonNull(urlString, "Must provide a string to convert");
        try {
            return new URL(urlString);
        } catch (MalformedURLException ex) {
            throw new SaiException("Unable to convert String to URL: " + ex.getMessage());
        }
    }

    /**
     * Coverts a URI to a URL
     * @param uri URI to convert
     * @return Converted URL
     * @throws SaiException
     */
    public static URL uriToUrl(URI uri) throws SaiException {
        Objects.requireNonNull(uri, "Must provide a URI to convert");
        try {
            return uri.toURL();
        } catch (MalformedURLException ex) {
            throw new SaiException("Unable to convert URI to URL: " + ex.getMessage());
        }
    }

    /**
     * Adds a child to the end of the path of <code>baseUrl</code>
     * @param baseUrl Base URL to append to
     * @param child Child to add to the path
     * @return URL with <code>child</code> appended
     * @throws SaiException
     */
    public static URL addChildToUrlPath(URL baseUrl, String child) throws SaiException {
        try {
            return new URL(baseUrl, child);
        } catch (MalformedURLException ex) {
            throw new SaiException("Unable to append child " + child + "to URL path " + baseUrl + ": " + ex.getMessage());
        }
    }

    /**
     * Generate a failure message based on an OkHttp Response
     * @param response OkHttp Response
     * @return Failure message string
     */
    public static String getResponseFailureMessage(Response response) {
        return "HTTP " + response.code() + " " + response.message() ;
    }

}
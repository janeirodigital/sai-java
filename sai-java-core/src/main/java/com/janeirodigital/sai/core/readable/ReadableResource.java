package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.contexts.InteropContext.INTEROP_CONTEXT;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.buildRemoteJsonLdContext;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getResourceFromModel;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

/**
 * Represents a corresponding RDF Resource and provides read-only capabilities.
 */
@Getter
public class ReadableResource {

    protected final URL url;
    protected final SaiSession saiSession;
    protected final OkHttpClient httpClient;
    protected Model dataset;
    protected Resource resource;
    protected ContentType contentType;
    protected String jsonLdContext;
    protected boolean unprotected;
    protected boolean exists;

    /**
     * Construct a Readable resource for <code>resourceUrl</code>, assigning the provided
     * <code>saiSession</code> for subsequent operations, along with an HTTP client to
     * facilitate them, and the associated credentials to use if the resource is protected
     * @param resourceUrl URL of the Readable resource
     * @param saiSession Data factory to assign
     * @param unprotected When true no authorization credentials will be sent in requests to this resource
     */
    public ReadableResource(URL resourceUrl, SaiSession saiSession, boolean unprotected) throws SaiException {
        Objects.requireNonNull(resourceUrl, "Must provide a URL for the target resource");
        Objects.requireNonNull(saiSession, "Must provide a sai session");
        Objects.requireNonNull(saiSession.getHttpClient(), "Must provide a valid HTTP client");
        this.url = resourceUrl;
        this.saiSession = saiSession;
        this.httpClient = saiSession.getHttpClient();
        this.dataset = null;
        this.resource = null;
        this.unprotected = unprotected;
        this.contentType = DEFAULT_RDF_CONTENT_TYPE;
        this.jsonLdContext = buildRemoteJsonLdContext(INTEROP_CONTEXT);
    }

    /**
     * Populates the <code>dataset</code> based on the contents of the corresponding
     * resource requested and returned over HTTP.
     * @throws SaiException
     */
    protected void fetchData() throws SaiException, SaiNotFoundException {
        if (this.isUnprotected()) { this.fetchUnprotectedData(); } else {
            // wrapping the call in try-with-resources automatically closes the response
            Headers headers = addHttpHeader(HttpHeader.ACCEPT, this.contentType.getValue());
            try (Response response = checkReadableResponse(getProtectedRdfResource(this.saiSession.getAuthorizedSession(), this.httpClient, this.url, headers))) {
                this.dataset = getRdfModelFromResponse(response);
                this.resource = getResourceFromModel(this.dataset, this.url);
            }
        }
        this.exists = true;
    }

    /**
     * Fetches data and populates the dataset without sending any authorization headers
     * @throws SaiNotFoundException when the request failed
     * @throws SaiException when the request failed
     */
    private void fetchUnprotectedData() throws SaiException, SaiNotFoundException {
        // wrapping the call in try-with-resources automatically closes the response
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, this.contentType.getValue());
        try (Response response = checkReadableResponse(getRdfResource(this.httpClient, this.url, headers))) {
            this.dataset = getRdfModelFromResponse(response);
            this.resource = getResourceFromModel(this.dataset, this.url);
        }
    }

    /**
     * Checks the response when fetching data for a readable resource
     * @param response Response to check
     * @return Checked Response
     * @throws SaiNotFoundException when the resource cannot be found (HTTP 404)
     * @throws SaiException when the response code is unsuccessful for any other reason
     */
    public static Response checkReadableResponse(Response response) throws SaiNotFoundException, SaiException {
        if (response.code() == HTTP_NOT_FOUND) { throw new SaiNotFoundException("Resource " + response.request().url() + " doesn't exist"); }
        if (!response.isSuccessful()) {
            throw new SaiException("Unable to fetch data for " + response.request().url() + ": " + response.code() + " " + response.message());
        }
        return response;
    }

}

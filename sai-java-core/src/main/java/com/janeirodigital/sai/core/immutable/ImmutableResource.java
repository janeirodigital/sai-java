package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.core.readable.ReadableResource;
import lombok.Getter;
import okhttp3.Headers;
import okhttp3.Response;

import java.net.URL;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.deleteProtectedResource;
import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.putProtectedRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;

/**
 * Represents a corresponding RDF Resource and provides create, read, and
 * delete capabilities. Immutable resources don't provide an update capability.
 */
@Getter
public class ImmutableResource extends ReadableResource {

    /**
     * Construct an immutable resource for <code>resourceUrl</code>. Calls the parent
     * {@link ReadableResource} constructor which assigns the provided
     * <code>saiSession</code>, and gets an HTTP client.
     * @param resourceUrl URL of the immutable resource
     * @param saiSession Data factory to assign
     * @param unprotected When true no authorization credentials will be sent in requests to this resource
     */
    public ImmutableResource(URL resourceUrl, SaiSession saiSession, boolean unprotected) throws SaiException {
        super(resourceUrl, saiSession, unprotected);
        this.exists = false; // assume the resource doesn't exist until it's bootstrapped
    }

    /**
     * Create the corresponding resource over HTTP with the current contents
     * of <code>dataset</code>. If-None-Match header is used to ensure another
     * resource at <code>url</code> doesn't already exist.
     * @throws SaiException
     */
    public void create() throws SaiException {
        Headers headers = setHttpHeader(HttpHeader.IF_NONE_MATCH, "*");
        if (this.isUnprotected()) { this.createUnprotected(headers); } else {
            Response response = putProtectedRdfResource(this.saiSession.getAuthorizedSession(), this.httpClient, this.url, this.resource, this.contentType, this.jsonLdContext, headers);
            if (!response.isSuccessful()) { throw new SaiException("Failed to create " + this.url + ": " + getResponseFailureMessage(response)); }
        }
        this.exists = true;
    }

    /**
     * Create the corresponding resource without sending any authorization headers
     */
    private void createUnprotected(Headers headers) throws SaiException {
        Response response = putRdfResource(this.httpClient, this.url, this.resource, this.contentType, this.jsonLdContext, headers);
        if (!response.isSuccessful()) { throw new SaiException("Failed to create " + this.url + ": " + getResponseFailureMessage(response)); }
    }

    /**
     * Deletes the corresponding resource over HTTP
     * @throws SaiException
     */
    public void delete() throws SaiException {
        if (this.isUnprotected()) { this.deleteUnprotected(); } else {
            Response response = deleteProtectedResource(this.getSaiSession().getAuthorizedSession(), this.httpClient, this.url);
            if (!response.isSuccessful()) { throw new SaiException("Failed to delete " + this.url + ": " + getResponseFailureMessage(response)); }
        }
        this.exists = false;
    }

    /**
     * Deletes the corresponding resource over HTTP without sending any
     * authorization headers
     * @throws SaiException
     */
    private void deleteUnprotected() throws SaiException {
        Response response = deleteResource(this.httpClient, this.url);
        if (!response.isSuccessful()) { throw new SaiException("Failed to create " + this.url + ": " + getResponseFailureMessage(response)); }
    }

}

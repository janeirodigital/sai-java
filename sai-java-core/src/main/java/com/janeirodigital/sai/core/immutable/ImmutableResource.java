package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.authentication.SaiAuthenticationException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.readable.ReadableResource;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.HttpHeader;
import com.janeirodigital.sai.httputils.SaiHttpException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import lombok.Getter;
import okhttp3.Headers;
import okhttp3.Response;

import java.net.URL;

import static com.janeirodigital.sai.authentication.AuthorizedSessionHelper.*;
import static com.janeirodigital.sai.httputils.HttpUtils.*;

/**
 * Represents a corresponding RDF Resource and provides create, read, and
 * delete capabilities. Immutable resources don't provide an update capability.
 */
@Getter
public class ImmutableResource extends ReadableResource {

    /**
     * Construct an Immutable resource using the provided {@link Builder}.
     * @param builder {@link Builder} or an instance of an inheriting subclass
     */
    public ImmutableResource(Builder<?> builder) throws SaiException {
        super(builder);
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
        try {
            if (this.isUnprotected()) { this.createUnprotected(headers); } else {
                checkResponse(putProtectedRdfResource(this.saiSession.getAuthorizedSession(), this.httpClient, this.url, this.resource, this.contentType, this.jsonLdContext, headers));
            }
        } catch (SaiRdfException | SaiAuthenticationException | SaiHttpException ex) {
            throw new SaiException("Failed to create immutable resource " + this.url, ex);
        }
        this.exists = true;
    }

    /**
     * Create the corresponding resource without sending any authorization headers
     */
    private void createUnprotected(Headers headers) throws SaiRdfException, SaiHttpException {
        checkResponse(putRdfResource(this.httpClient, this.url, this.resource, this.contentType, this.jsonLdContext, headers));
    }

    /**
     * Deletes the corresponding resource over HTTP
     * @throws SaiException
     */
    public void delete() throws SaiException {
        try {
            if (this.isUnprotected()) { this.deleteUnprotected(); } else {
                checkResponse(deleteProtectedResource(this.getSaiSession().getAuthorizedSession(), this.httpClient, this.url));
            }
        } catch (SaiHttpException | SaiAuthenticationException ex) {
            throw new SaiException("Failed to delete immutable resource " + this.url, ex);
        }
        this.exists = false;
    }

    /**
     * Deletes the corresponding resource over HTTP without sending any
     * authorization headers
     * @throws SaiHttpException
     */
    private void deleteUnprotected() throws SaiHttpException {
        checkResponse(deleteResource(this.httpClient, this.url));
    }

    /**
     * Ensure the response to a create or delete operation is successful
     * @param response OkHttp Response to check
     * @return Response
     * @throws SaiHttpException
     */
    private Response checkResponse(Response response) throws SaiHttpException {
        if (!response.isSuccessful()) { throw new SaiHttpException("Failed to " + response.request().method() + " " + this.url + ": " + getResponseFailureMessage(response)); }
        return response;
    }

    /**
     * Generic builder which is extended by Immutable resource builders. Extends and incorporates the
     * {@link ReadableResource.Builder} as a base.
     * @param <T> Parameterized type of an inheriting builder
     */
    protected abstract static class Builder<T extends ReadableResource.Builder<T>> extends ReadableResource.Builder<T> {

        /**
         * Base builder for Immutable resource types. Use setters for all further configuration
         * @param url URL of the resource to build
         * @param saiSession {@link SaiSession} to use
         */
        protected Builder(URL url, SaiSession saiSession) { super(url, saiSession); }

    }
}

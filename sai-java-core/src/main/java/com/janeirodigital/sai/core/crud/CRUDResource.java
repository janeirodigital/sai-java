package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.authentication.SaiAuthenticationException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.readable.ReadableResource;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpException;
import lombok.Getter;
import okhttp3.Response;

import java.net.URI;

import static com.janeirodigital.sai.authentication.AuthorizedSessionHelper.deleteProtectedResource;
import static com.janeirodigital.sai.authentication.AuthorizedSessionHelper.putProtectedRdfResource;
import static com.janeirodigital.sai.httputils.HttpUtils.*;

/**
 * Represents a corresponding RDF Resource and provides create, read, update,
 * and delete capabilities.
 */
@Getter
public class CRUDResource extends ReadableResource {

    /**
     * Construct a CRUD resource using the provided {@link Builder}.
     * @param builder {@link Builder} or an instance of an inheriting subclass
     */
    public CRUDResource(Builder<?> builder) throws SaiException {
        super(builder);
    }

    /**
     * Updates the corresponding resource over HTTP with the current contents of
     * <code>dataset</code>.
     * @throws SaiException
     */
    public void update() throws SaiException {
        try {
            if (this.isUnprotected()) { this.updateUnprotected(); } else {
                checkResponse(putProtectedRdfResource(this.getSaiSession().getAuthorizedSession(), this.httpClient, this.uri, this.resource, this.contentType, this.jsonLdContext));
            }
        } catch (SaiHttpException | SaiAuthenticationException ex) {
            throw new SaiException("Failed to update resource " + this.uri, ex);
        }
        this.exists = true;
    }

    /**
     * Deletes the corresponding resource over HTTP
     * @throws SaiException
     */
    public void delete() throws SaiException {
        try {
            if (this.isUnprotected()) { this.deleteUnprotected(); } else {
                checkResponse(deleteProtectedResource(this.getSaiSession().getAuthorizedSession(), this.httpClient, this.uri));
            }
        } catch (SaiHttpException | SaiAuthenticationException ex ) {
            throw new SaiException("Failed to delete resource " + this.uri, ex);
        }
        this.exists = false;
    }

    /**
     * Updates the corresponding resource over HTTP with the current contents of
     * <code>dataset</code> without sending any authorization headers.
     * @throws SaiException
     */
    private void updateUnprotected() throws SaiHttpException, SaiException {
        checkResponse(putRdfResource(this.httpClient, this.uri, this.resource, this.contentType, this.jsonLdContext));
    }

    /**
     * Deletes the corresponding resource over HTTP without sending any
     * authorization headers
     * @throws SaiException
     */
    private void deleteUnprotected() throws SaiException, SaiHttpException {
        checkResponse(deleteResource(this.httpClient, this.uri));
    }

    /**
     * Ensure the response to a CRUD operation is successful
     * @param response OkHttp Response to check
     * @return Response
     * @throws SaiException
     */
    private Response checkResponse(Response response) throws SaiException {
        if (!response.isSuccessful()) { throw new SaiException("Failed to " + response.request().method() + " " + this.uri + ": " + getResponseFailureMessage(response)); }
        return response;
    }

    /**
     * Generic builder which is extended by CRUD resource builders. Extends and incorporates the
     * {@link ReadableResource.Builder} as a base.
     * @param <T> Parameterized type of an inheriting builder
     */
    protected abstract static class Builder<T extends ReadableResource.Builder<T>> extends ReadableResource.Builder<T> {

        /**
         * Base builder for CRUD resource types. Use setters for all further configuration
         * @param uri URI of the resource to build
         * @param saiSession {@link SaiSession} to use
         */
        protected Builder(URI uri, SaiSession saiSession) { super(uri, saiSession); }

    }
}

package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.readable.ReadableResource;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import okhttp3.Response;

import java.net.URL;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.deleteProtectedResource;
import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.putProtectedRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;

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
        if (this.isUnprotected()) { this.updateUnprotected(); } else {
            checkResponse(putProtectedRdfResource(this.getSaiSession().getAuthorizedSession(), this.httpClient, this.url, this.resource, this.contentType, this.jsonLdContext));
        }
        this.exists = true;
    }

    /**
     * Deletes the corresponding resource over HTTP
     * @throws SaiException
     */
    public void delete() throws SaiException {
        if (this.isUnprotected()) { this.deleteUnprotected(); } else {
            checkResponse(deleteProtectedResource(this.getSaiSession().getAuthorizedSession(), this.httpClient, this.url));
        }
        this.exists = false;
    }

    /**
     * Updates the corresponding resource over HTTP with the current contents of
     * <code>dataset</code> without sending any authorization headers.
     * @throws SaiException
     */
    private void updateUnprotected() throws SaiException {
        checkResponse(putRdfResource(this.httpClient, this.url, this.resource, this.contentType, this.jsonLdContext));
    }

    /**
     * Deletes the corresponding resource over HTTP without sending any
     * authorization headers
     * @throws SaiException
     */
    private void deleteUnprotected() throws SaiException {
        checkResponse(deleteResource(this.httpClient, this.url));
    }

    /**
     * Ensure the response to a CRUD operation is successful
     * @param response OkHttp Response to check
     * @return Response
     * @throws SaiException
     */
    private Response checkResponse(Response response) throws SaiException {
        if (!response.isSuccessful()) { throw new SaiException("Failed to " + response.request().method() + " " + this.url + ": " + getResponseFailureMessage(response)); }
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
         * @param url URL of the resource to build
         * @param saiSession {@link SaiSession} to use
         */
        protected Builder(URL url, SaiSession saiSession) { super(url, saiSession); }

    }
}

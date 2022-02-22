package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.factories.DataFactory;
import com.janeirodigital.sai.core.readable.ReadableResource;
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
     * Construct a CRUD resource for <code>resourceUrl</code>. Calls the parent
     * {@link ReadableResource} constructor which assigns the provided
     * <code>dataFactory</code>, and gets an HTTP client.
     * @param resourceUrl URL of the CRUD resource
     * @param dataFactory Data factory to assign
     * @param unprotected When true no authorization credentials will be sent in requests to this resource
     */
    public CRUDResource(URL resourceUrl, DataFactory dataFactory, boolean unprotected) throws SaiException {
        super(resourceUrl, dataFactory, unprotected);
        this.exists = false; // assume the resource doesn't exist until it's bootstrapped
    }

    /**
     * Updates the corresponding resource over HTTP with the current contents of
     * <code>dataset</code>.
     * @throws SaiException
     */
    public void update() throws SaiException {
        if (this.isUnprotected()) { this.updateUnprotected(); } else {
            Response response = putProtectedRdfResource(this.getDataFactory().getAuthorizedSession(), this.httpClient, this.url, this.resource, this.contentType, this.jsonLdContext);
            if (!response.isSuccessful()) { throw new SaiException("Failed to update " + this.url + ": " + getResponseFailureMessage(response)); }
        }
        this.exists = true;
    }

    /**
     * Deletes the corresponding resource over HTTP
     * @throws SaiException
     */
    public void delete() throws SaiException {
        if (this.isUnprotected()) { this.deleteUnprotected(); } else {
            Response response = deleteProtectedResource(this.getDataFactory().getAuthorizedSession(), this.httpClient, this.url);
            if (!response.isSuccessful()) { throw new SaiException("Failed to delete " + this.url + ": " + getResponseFailureMessage(response)); }
        }
        this.exists = false;
    }

    /**
     * Updates the corresponding resource over HTTP with the current contents of
     * <code>dataset</code> without sending any authorization headers.
     * @throws SaiException
     */
    private void updateUnprotected() throws SaiException {
        putRdfResource(this.httpClient, this.url, this.resource, this.contentType, this.jsonLdContext);
    }

    /**
     * Deletes the corresponding resource over HTTP without sending any
     * authorization headers
     * @throws SaiException
     */
    private void deleteUnprotected() throws SaiException {
        deleteResource(this.httpClient, this.url);
    }

}

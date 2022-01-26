package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.factories.DataFactory;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.readable.ReadableResource;
import lombok.Getter;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.Objects;

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
    }

    /**
     * Construct a protected CRUD resource for <code>resourceUrl</code>.
     * @param resourceUrl URL of the CRUD resource
     * @param dataFactory Data factory to assign
     * @throws SaiException
     */
    public CRUDResource(URL resourceUrl, DataFactory dataFactory) throws SaiException {
        super(resourceUrl, dataFactory, false);
    }

    /**
     * Additional constructor which initializes the RDF dataset with the provided
     * Jena <code>resource</code> and its corresponding Jena Model.
     * @param resourceUrl URL of the CRUD resource
     * @param dataFactory Data factory to assign
     * @param resource Jena Resource to initialize with
     * @param unprotected When true no authorization credentials will be sent in requests to this resource
     */
    public CRUDResource(URL resourceUrl, DataFactory dataFactory, Resource resource, boolean unprotected) throws SaiException {
        super(resourceUrl, dataFactory);
        Objects.requireNonNull(resource, "Cannot provide a null resource when initializing a crud resource with a dataset");
        this.resource = resource;
        this.dataset = resource.getModel();
    }

    /**
     * Initialize the RDF dataset for a protected resource with the provided
     * Jena <code>resource</code> and its corresponding Jena Model.
     * @param resourceUrl URL of the CRUD resource
     * @param dataFactory Data factory to assign
     * @param resource Jena Resource to initialize with
     * @throws SaiException
     */
    public CRUDResource(URL resourceUrl, DataFactory dataFactory, Resource resource) throws SaiException {
        this(resourceUrl, dataFactory, resource, false);
    }

    /**
     * Updates the corresponding resource over HTTP with the current contents of
     * <code>dataset</code>.
     * @throws SaiException
     */
    public void update() throws SaiException {
        if (this.isUnprotected()) { this.updateUnprotected(); } else {
            putProtectedRdfResource(this.getDataFactory().getAuthorizedSession(), this.httpClient, this.url, this.resource);
        }
    }

    /**
     * Updates the corresponding resource over HTTP with the current contents of
     * <code>dataset</code> without sending any authorization headers.
     * @throws SaiException
     */
    private void updateUnprotected() throws SaiException {
        putRdfResource(this.httpClient, this.url, this.resource);
    }

    /**
     * Deletes the corresponding resource over HTTP
     * @throws SaiException
     */
    public void delete() throws SaiException {
        if (this.isUnprotected()) { this.deleteUnprotected(); } else {
            deleteProtectedResource(this.getDataFactory().getAuthorizedSession(), this.httpClient, this.url);
        }
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

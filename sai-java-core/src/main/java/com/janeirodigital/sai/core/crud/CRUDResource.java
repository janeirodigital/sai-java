package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.DataFactory;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.readable.ReadableResource;
import lombok.Getter;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.Objects;

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
     */
    public CRUDResource(URL resourceUrl, DataFactory dataFactory) {
        super(resourceUrl, dataFactory);
    }

    /**
     * Additional constructor which initializes the RDF dataset with the provided
     * Jena <code>resource</code> and its corresponding Jena Model.
     * @param resourceUrl URL of the CRUD resource
     * @param dataFactory Data factory to assign
     * @param resource Jena Resource to initialize with
     */
    public CRUDResource(URL resourceUrl, DataFactory dataFactory, Resource resource) {
        super(resourceUrl, dataFactory);
        Objects.requireNonNull(resource, "Cannot provide a null resource when initializing a crud resource with a dataset");
        this.resource = resource;
        this.dataset = resource.getModel();
    }

    /**
     * Updates the corresponding resource over HTTP with the current contents of
     * <code>dataset</code>.
     * @throws SaiException
     */
    public void update() throws SaiException {
        putRdfResource(this.httpClient, this.url, this.resource);
    }

    /**
     * Deletes the corresponding resource over HTTP
     * @throws SaiException
     */
    public void delete() throws SaiException {
        deleteResource(this.httpClient, this.url);
    }

}

package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.DataFactory;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.readable.ReadableResource;
import lombok.Getter;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.Objects;

import static com.janeirodigital.sai.core.helpers.HttpHelper.*;

@Getter
public class CRUDResource extends ReadableResource {

    public CRUDResource(URL resourceUrl, DataFactory dataFactory) {
        super(resourceUrl, dataFactory);
    }

    public CRUDResource(URL resourceUrl, DataFactory dataFactory, Resource resource) {
        super(resourceUrl, dataFactory);
        Objects.requireNonNull(resource, "Cannot provide a null resource when initializing a crud resource with a dataset");
        this.resource = resource;
        this.dataset = resource.getModel();
    }

    // update
    public void update() throws SaiException {
        putRdfResource(this.httpClient, this.url, this.resource);
    }

    // delete
    public void delete() throws SaiException {
        deleteResource(this.httpClient, this.url);
    }

}

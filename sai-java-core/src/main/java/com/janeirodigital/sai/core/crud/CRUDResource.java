package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.DataFactory;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.readable.ReadableResource;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;

import static com.janeirodigital.sai.core.helpers.HttpHelper.*;

@Getter
public class CRUDResource extends ReadableResource {

    public CRUDResource(URL resourceUrl, DataFactory dataFactory) {
        super(resourceUrl, dataFactory);
    }

    public CRUDResource(URL resourceUrl, DataFactory dataFactory, Resource resource) {
        super(resourceUrl, dataFactory);
        if (resource != null) {
            this.resource = resource;
            this.dataset = resource.getModel();
        }
    }

    // update
    public Response update() throws SaiException {
        if (this.isContainer()) {
            return putRdfContainer(this.httpClient, this.url, this.resource);
        }
        return putRdfResource(this.httpClient, this.url, this.resource);
    }

    // delete
    public Response delete() throws SaiException {
        return deleteResource(this.httpClient, this.url);
    }

}

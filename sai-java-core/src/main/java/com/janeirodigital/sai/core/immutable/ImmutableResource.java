package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.Getter;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.putProtectedRdfResource;
import static com.janeirodigital.sai.core.enums.ContentType.LD_JSON;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;

/**
 * Represents a corresponding RDF Resource and provides create, read, and
 * delete capabilities. Immutable resources don't provide an update capability.
 */
@Getter
public class ImmutableResource {

    protected final URL url;
    protected final DataFactory dataFactory;
    protected final OkHttpClient httpClient;
    protected Model dataset;
    protected Resource resource;
    protected ContentType contentType;
    protected String jsonLdContext;
    protected boolean unprotected;

    /**
     * Construct an immutable resource for <code>resourceUrl</code> based on the dataset
     * from the provided Jena <code>resource</code>.
     * @param resourceUrl URL of the immutable resource
     * @param dataFactory Data factory to assign
     * @param resource Jena resource to populate with
     * @param unprotected When true no authorization credentials will be sent in requests to this resource
     */
    public ImmutableResource(URL resourceUrl, DataFactory dataFactory, Resource resource, boolean unprotected) throws SaiException {
        Objects.requireNonNull(resourceUrl, "Must provide a URL for the target resource");
        Objects.requireNonNull(dataFactory, "Must provide a data factory");
        Objects.requireNonNull(dataFactory.getHttpClient(), "Must provide a valid HTTP client");
        Objects.requireNonNull(resource, "Must provide a resource and model to populate an immutable resource");
        this.url = resourceUrl;
        this.dataFactory = dataFactory;
        this.httpClient = dataFactory.getHttpClient();
        this.dataset = resource.getModel();
        this.resource = resource;
        this.unprotected = unprotected;
        // Turtle is the default content type for read and writes
        this.contentType = ContentType.TEXT_TURTLE;
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
            putProtectedRdfResource(this.dataFactory.getAuthorizedSession(), this.httpClient, this.url, this.resource, this.contentType, this.jsonLdContext, headers);
        }
    }

    /**
     * Set the preferred RDF content type for writes. Will be supplied in HTTP
     * Content-Type headers.
     * @param contentType RDF content type
     * @throws SaiException on invalid content type
     */
    public void setContentType(ContentType contentType) throws SaiException {
        if (!RDF_CONTENT_TYPES.contains(contentType)) { throw new SaiException("Must provide a supported RDF content-type"); }
        this.contentType = contentType;
    }

    /**
     * Set the JSON-LD context to use on writes when the content-type is JSON-LD.
     * @param jsonLdContext JSON-LD context as string
     * @throws SaiException on invalid content type
     */
    public void setJsonLdContext(String jsonLdContext) throws SaiException {
        if (!this.contentType.equals(LD_JSON)) { throw new SaiException("JSON-LD contexts only apply to the " + LD_JSON.getValue() + " content-type"); }
        this.jsonLdContext = jsonLdContext;
    }

    /**
     * Create the corresponding resource without sending any authorization headers
     */
    private void createUnprotected(Headers headers) throws SaiException {
        putRdfResource(this.httpClient, this.url, this.resource, this.contentType, this.jsonLdContext, headers);
    }

}

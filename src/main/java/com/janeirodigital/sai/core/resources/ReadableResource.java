package com.janeirodigital.sai.core.resources;

import com.janeirodigital.sai.authentication.SaiAuthenticationException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.HttpHeader;
import com.janeirodigital.sai.httputils.SaiHttpException;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import lombok.Getter;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URI;
import java.util.Objects;

import static com.janeirodigital.sai.authentication.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.contexts.InteropContext.INTEROP_CONTEXT;
import static com.janeirodigital.sai.httputils.HttpUtils.*;
import static com.janeirodigital.sai.rdfutils.RdfUtils.buildRemoteJsonLdContext;
import static com.janeirodigital.sai.rdfutils.RdfUtils.getResourceFromModel;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;

/**
 * Represents a corresponding RDF Resource and provides read-only capabilities.
 */
@Getter
public class ReadableResource {

    protected final URI uri;
    protected final SaiSession saiSession;
    protected final OkHttpClient httpClient;
    protected Model dataset;
    protected Resource resource;
    protected ContentType contentType;
    protected String jsonLdContext;
    protected boolean unprotected;
    protected boolean exists;

    /**
     * Construct a Readable resource using the provided {@link Builder}.
     * @param builder {@link Builder} or an instance of an inheriting subclass
     */
    protected ReadableResource(Builder<?> builder) throws SaiException {
        Objects.requireNonNull(builder, "Must provide a builder for the readable resource");
        this.uri = builder.uri;
        this.saiSession = builder.saiSession;
        this.httpClient = this.saiSession.getHttpClient();
        this.dataset = builder.dataset;
        this.resource = builder.resource;
        this.unprotected = builder.unprotected;
        this.contentType = builder.contentType;
        this.jsonLdContext = builder.jsonLdContext;
        this.exists = builder.exists;
    }

    /**
     * Reads the remote RDF resource at <code>uri</code>, providing credentials from the {@link SaiSession} when
     * <code>unprotected</code> is not true.
     * @param uri URI to GET
     * @param saiSession {@link SaiSession} to use
     * @param contentType {@link ContentType} to accept
     * @param unprotected When true, does not send authorization headers
     * @return OkHttp Response
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    protected static Response read(URI uri, SaiSession saiSession, ContentType contentType, boolean unprotected) throws SaiException, SaiHttpNotFoundException {
        Objects.requireNonNull(uri, "Must provide the URI of the readable social agent profile to get");
        Objects.requireNonNull(saiSession, "Must provide a sai session to assign to the readable social agent profile");
        Objects.requireNonNull(contentType, "Must provide a content type to assign to the readable social agent profile");
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        Response response;
        try {
            if (unprotected) { response = getRdfResource(saiSession.getHttpClient(), uri, headers); } else {
                response = getProtectedRdfResource(saiSession.getAuthorizedSession(), saiSession.getHttpClient(), uri, headers);
            }
        } catch (SaiHttpException | SaiAuthenticationException ex) {
            throw new SaiException("Unable to read resource " + uri, ex);
        }
        return checkReadableResponse(response);
    }

    /**
     * Checks the response when fetching data for a readable resource
     * @param response Response to check
     * @return Checked Response
     * @throws SaiHttpNotFoundException when the resource cannot be found (HTTP 404)
     * @throws SaiException when the response code is unsuccessful for any other reason
     */
    public static Response checkReadableResponse(Response response) throws SaiHttpNotFoundException, SaiException {
        if (response.code() == HTTP_NOT_FOUND) { throw new SaiHttpNotFoundException("Resource " + response.request().url() + " doesn't exist"); }
        if (!response.isSuccessful()) {
            throw new SaiException("Unable to fetch data for " + response.request().url() + ": " + response.code() + " " + response.message());
        }
        return response;
    }

    /**
     * Generic builder which is extended by readable resource builders, as well as builders for the other
     * base resource types {@link CRUDResource} and
     * {@link ImmutableResource}, respectively.
     * @param <T> Parameterized type of an inheriting builder
     */
    public abstract static class Builder<T extends Builder<T>> {

        protected final URI uri;
        protected final SaiSession saiSession;
        protected boolean unprotected;
        protected ContentType contentType;
        protected Model dataset;
        protected Resource resource;
        protected String jsonLdContext;
        protected boolean exists;

        /**
         * Base builder for all resource types. Use setters for all further configuration
         * @param uri URI of the resource to build
         * @param saiSession {@link SaiSession} to use
         */
        protected Builder(URI uri, SaiSession saiSession) {
            Objects.requireNonNull(uri, "Must provide a URI to the resource builder");
            Objects.requireNonNull(saiSession, "Must provide a sai session to the resource builder");
            this.uri = uri;
            this.saiSession = saiSession;
            // resource defaults (can override with setters)
            this.unprotected = false;                                       // default to protected resources
            this.exists = false;                                            // default to non-existent
            this.contentType = DEFAULT_RDF_CONTENT_TYPE;                    // default to rdf default constant
            this.jsonLdContext = buildRemoteJsonLdContext(INTEROP_CONTEXT); // default to interop context
        }

        /**
         * Necessary to so that we don't get an unchecked cast warning when casting the return value of setters
         * in this and any inheriting builders.
         * @return T generic - on implementations this is the resource type that is being built
         */
        public abstract T getThis();

        /**
         * Set the resource type as unprotected. Keeps any credentials from being sent along
         * as part of any requests issued for this resource
         * @return {@link Builder}
         */
        public T setUnprotected() {
            this.unprotected = true;
            return getThis();
        }

        /**
         * Set the content-type for the resource (as opposed to the default set by the builder constructor).
         * @param contentType {@link ContentType} to set
         * @return {@link Builder}
         */
        public T setContentType(ContentType contentType) {
            Objects.requireNonNull(contentType, "Must provide a content type to the resource builder");
            this.contentType = contentType;
            return getThis();
        }

        /**
         * Jena Model that is used to initialize attributes of inheriting builders rather than set
         * them manually. Used in all read scenarios when populating the Builder from
         * the contents of a remote resource.
         * @param dataset Jena model to populate the Builder attributes with
         * @return {@link Builder}
         */
        public T setDataset(Model dataset) throws SaiException {
            Objects.requireNonNull(dataset, "Must provide a Jena model to the resource builder");
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.uri);
            return getThis();
        }

        /**
         * Extracts a Jena Model from an HTTP response and calls
         * {@link #setDataset(Model)} after extraction
         * @param response OkHttp Response to extract from
         * @return {@link Builder}
         * @throws SaiException
         */
        public T setDataset(Response response) throws SaiException {
            try {
                return setDataset(getRdfModelFromResponse(response));
            } catch (SaiHttpException ex) {
                throw new SaiException("Unable to get rdf graph from http response", ex);
            }
        }

        /**
         * Set the JSON-LD context to use for the resource (as opposed to the default context by the builder
         * constructor). Note that this setting won't matter unless the {@link ContentType} of the resource is LD_JSON.
         * @param jsonLdContext JSON-LD context to set
         * @return {@link Builder}
         */
        public T setJsonLdContext(String jsonLdContext) {
            Objects.requireNonNull(jsonLdContext, "Must provide a json-ld context to the resource builder");
            this.jsonLdContext = jsonLdContext;
            return getThis();
        }

    }

}

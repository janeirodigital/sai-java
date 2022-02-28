package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import lombok.SneakyThrows;
import okhttp3.Headers;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getNewResourceForType;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getResourceFromModel;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.DATA_REGISTRY;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.HAS_DATA_REGISTRATION;

/**
 * Modifiable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-registry">Data Registry</a>
 */
@Getter
public class DataRegistry extends CRUDResource {

    private final DataRegistrationList<DataRegistration> dataRegistrations;

    /**
     * Construct a new {@link DataRegistry}. Should only be called from {@link Builder}
     */
    private DataRegistry(URL url, SaiSession saiSession, Model dataset, Resource resource, ContentType contentType,
                         DataRegistrationList<DataRegistration> dataRegistrations) throws SaiException {
        super(url, saiSession, false);
        this.dataset = dataset;
        this.resource = resource;
        this.contentType = contentType;
        this.dataRegistrations = dataRegistrations;
    }

    /**
     * Get a {@link DataRegistry} at the provided <code>url</code>
     * @param url URL of the {@link DataRegistry} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link DataRegistry}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static DataRegistry get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(url, "Must provide the URL of the data registry to get");
        Objects.requireNonNull(saiSession, "Must provide a sai session to assign to the data registry");
        Objects.requireNonNull(contentType, "Must provide a content type for the data registry");
        DataRegistry.Builder builder = new DataRegistry.Builder(url, saiSession, contentType);
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(saiSession.getAuthorizedSession(), saiSession.getHttpClient(), url, headers))) {
            builder.setDataset(getRdfModelFromResponse(response));
        }
        return builder.build();
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link DataRegistry}
     * @param saiSession {@link SaiSession} to assign
     * @return
     */
    public static DataRegistry get(URL url, SaiSession saiSession) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }
    
    /**
     * Builder for {@link DataRegistry} instances.
     */
    public static class Builder {
        private final URL url;
        private final SaiSession saiSession;
        private final ContentType contentType;
        private Model dataset;
        private Resource resource;
        private DataRegistrationList<DataRegistration> dataRegistrations;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link DataRegistry} to build
         * @param saiSession {@link SaiSession} to assign
         * @param contentType {@link ContentType} to assign
         */
        public Builder(URL url, SaiSession saiSession, ContentType contentType) {
            Objects.requireNonNull(url, "Must provide a URL for the data registry builder");
            Objects.requireNonNull(saiSession, "Must provide a sai session for the data registry builder");
            Objects.requireNonNull(contentType, "Must provide a content type for the data registry builder");
            this.url = url;
            this.saiSession = saiSession;
            this.contentType = contentType;
            this.dataRegistrations = new DataRegistrationList<>(this.saiSession, this.resource);
        }

        /**
         * Optional Jena Model that will initialize the attributes of the Builder rather than set
         * them manually. Typically used in read scenarios when populating the Builder from
         * the contents of a remote resource.
         * @param dataset Jena model to populate the Builder attributes with
         * @return {@link Builder}
         */
        public Builder setDataset(Model dataset) throws SaiException {
            Objects.requireNonNull(dataset, "Must provide a Jena model for the data registry builder");
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            populateFromDataset();
            return this;
        }

        /**
         * Set the URLs of data registrations in the Data Registry (which must have already been created)
         * @param dataRegistrationUrls List of URLs to {@link DataRegistration} instances
         * @return {@link Builder}
         */
        public Builder setDataRegistrationUrls(List<URL> dataRegistrationUrls) throws SaiAlreadyExistsException {
            Objects.requireNonNull(dataRegistrations, "Must provide a list of data registration urls to the data registry builder");
            this.dataRegistrations.addAll(dataRegistrationUrls);
            return this;
        }

        /**
         * Populates the fields of the {@link DataRegistry} based on the associated Jena resource.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException {
            try {
                this.dataRegistrations.populate();
            } catch (SaiException ex) {
                throw new SaiException("Failed to load data registry " + this.url + ": " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         * @throws SaiException
         */
        private void populateDataset() throws SaiException {
            this.resource = getNewResourceForType(this.url, DATA_REGISTRY);
            this.dataset = this.resource.getModel();
            // Note that data registration URLs added via setDataRegistrationUrls are automatically added to the
            // dataset graph, so they don't have to be added here again
        }

        /**
         * Build the {@link DataRegistry} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}. Conversely, if a dataset was provided, the attributes of the
         * Builder will be populated from it.
         * @return {@link DataRegistry}
         * @throws SaiException
         */
         public DataRegistry build() throws SaiException {
             if (this.dataset == null) { populateDataset(); }
             return new DataRegistry(this.url, this.saiSession, this.dataset, this.resource, this.contentType, this.dataRegistrations);
         }
    }

    /**
     * Class for access and iteration of {@link DataRegistration}s. Most of the capability is provided
     * through extension of {@link RegistrationList}, which requires select overrides to ensure the correct
     * types are built and returned by the iterator.
     */
    public static class DataRegistrationList<T> extends RegistrationList<T> {
        public DataRegistrationList(SaiSession saiSession, Resource resource) { super(saiSession, resource, HAS_DATA_REGISTRATION); }

        @Override
        public T find(URL shapeTreeUrl) {
            for (T registration : this) {
                DataRegistration dataRegistration = (DataRegistration) registration;
                if (shapeTreeUrl.equals(dataRegistration.getRegisteredShapeTree())) { return (T) dataRegistration; }
            }
            return null;
        }

        @Override
        public Iterator<T> iterator() { return new DataRegistrationListIterator(this.getSaiSession(), this.getRegistrationUrls()); }

        private class DataRegistrationListIterator<T> extends RegistrationListIterator<T> {
            public DataRegistrationListIterator(SaiSession saiSession, List<URL> registrationUrls) { super(saiSession, registrationUrls); }
            @SneakyThrows
            @Override
            public T next() {
                URL registrationUrl = (URL) current.next();
                return (T) DataRegistration.get(registrationUrl, saiSession);
            }
        }

    }

}

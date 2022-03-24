package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.exceptions.SaiRuntimeException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.helpers.HttpHelper.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.core.helpers.HttpHelper.getRdfModelFromResponse;
import static com.janeirodigital.sai.core.helpers.RdfUtils.getNewResourceForType;
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
     * Construct a {@link DataRegistry} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private DataRegistry(Builder builder) throws SaiException {
        super(builder);
        this.dataRegistrations = builder.dataRegistrations;
    }

    /**
     * Get a {@link DataRegistry} at the provided <code>url</code>
     * @param url URL of the {@link DataRegistry} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link DataRegistry}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static DataRegistry get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        DataRegistry.Builder builder = new DataRegistry.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).setContentType(contentType).build();
        }
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
     * Reload a new instance of {@link DataRegistry} using the attributes of the current instance
     * @return Reloaded {@link DataRegistry}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public DataRegistry reload() throws SaiNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    /**
     * Indicate whether the {@link DataRegistry} has any {@link DataRegistration}s
     * @return true if there are no registrations
     */
    public boolean isEmpty() {
        return dataRegistrations.isEmpty();
    }

    /**
     * Add a {@link DataRegistration} to the {@link DataRegistry}
     * @param registration {@link DataRegistration} to add
     * @throws SaiException
     * @throws SaiAlreadyExistsException
     */
    public void add(DataRegistration registration) throws SaiException, SaiAlreadyExistsException {
        Objects.requireNonNull(registration, "Cannot add a null data registration to agent registry");
        DataRegistration found = this.getDataRegistrations().find(registration.getRegisteredShapeTree());
        if (found != null) { throw new SaiAlreadyExistsException("Data registration already exists for shape tree " + registration.getRegisteredShapeTree() + " at " + found.getUrl()); }
        this.getDataRegistrations().add(registration.getUrl());
    }

    /**
     * Remove a {@link DataRegistration} from the {@link DataRegistry}
     * @param registration {@link DataRegistration} to remove
     */
    public void remove(DataRegistration registration) {
        Objects.requireNonNull(registration, "Cannot remove a null data registration to agent registry");
        this.dataRegistrations.remove(registration.getUrl());
    }
    
    /**
     * Builder for {@link DataRegistry} instances.
     */
    public static class Builder extends CRUDResource.Builder<Builder> {

        private DataRegistrationList<DataRegistration> dataRegistrations;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link DataRegistry} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URL url, SaiSession saiSession) {
            super(url, saiSession);
        }

        /**
         * Ensures that don't get an unchecked cast warning when returning from setters
         * @return {@link Builder}
         */
        @Override
        public Builder getThis() { return this; }

        /**
         * Set the Jena model and use it to populate attributes of the {@link Builder}. Assumption
         * is made that the corresponding resource exists.
         * @param dataset Jena model to populate the Builder attributes with
         * @return {@link Builder}
         * @throws SaiException
         */
        @Override
        public Builder setDataset(Model dataset) throws SaiException {
            super.setDataset(dataset);
            populateFromDataset();
            this.exists = true;
            return this;
        }

        /**
         * Populates the fields of the {@link DataRegistry} based on the associated Jena resource.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException {
            try {
                this.dataRegistrations = new DataRegistrationList<>(this.saiSession, this.resource);
                this.dataRegistrations.populate();
            } catch (SaiException ex) {
                throw new SaiException("Failed to load data registry " + this.url + ": " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        private void populateDataset() {
            this.resource = getNewResourceForType(this.url, DATA_REGISTRY);
            this.dataset = this.resource.getModel();
            // Note that data registration URLs added via setDataRegistrationUrls are automatically added to the
            // dataset graph, so they don't have to be added here again
        }

        /**
         * Build the {@link DataRegistry} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}.
         * @return {@link DataRegistry}
         * @throws SaiException
         */
         public DataRegistry build() throws SaiException {
             if (this.dataset == null) { populateDataset(); }
             return new DataRegistry(this);
         }
    }

    /**
     * Class for access and iteration of {@link DataRegistration}s. Most of the capability is provided
     * through extension of {@link RegistrationList}, which requires select overrides to ensure the correct
     * types are built and returned by the iterator.
     */
    public static class DataRegistrationList<T> extends RegistrationList<T> {
        public DataRegistrationList(SaiSession saiSession, Resource resource) { super(saiSession, resource, HAS_DATA_REGISTRATION); }

        /**
         * Override the default find in {@link RegistrationList} to lookup based on the registered shape tree of
         * a {@link DataRegistration}
         * @param shapeTreeUrl URL of the registeredShapeTree to find
         * @return {@link DataRegistration}
         */
        @Override
        public T find(URL shapeTreeUrl) {
            for (T registration : this) {
                DataRegistration dataRegistration = (DataRegistration) registration;
                if (shapeTreeUrl.equals(dataRegistration.getRegisteredShapeTree())) { return (T) dataRegistration; }
            }
            return null;
        }

        /**
         * Return an iterator for {@link DataRegistration} instances
         * @return {@link DataRegistration} Iterator
         */
        @Override
        public Iterator<T> iterator() { return new DataRegistrationListIterator(this.getSaiSession(), this.getRegistrationUrls()); }

        /**
         * Custom iterator that iterates over {@link DataRegistration} URLs and gets actual instances of them
         */
        private static class DataRegistrationListIterator<T> extends RegistrationListIterator<T> {
            public DataRegistrationListIterator(SaiSession saiSession, List<URL> registrationUrls) { super(saiSession, registrationUrls); }
            /**
             * Get the {@link DataRegistration} for the next URL in the iterator
             * @return {@link DataRegistration}
             */
            @Override
            public T next() {
                try {
                    URL registrationUrl = current.next();
                    return (T) DataRegistration.get(registrationUrl, saiSession);
                } catch (SaiException|SaiNotFoundException ex) {
                    throw new SaiRuntimeException("Failed to get data registration while iterating list: " + ex.getMessage());
                }
            }
        }

    }

}

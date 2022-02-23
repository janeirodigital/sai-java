package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.Getter;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.helpers.RdfHelper.getUrlObjects;
import static com.janeirodigital.sai.core.helpers.RdfHelper.updateUrlObjects;

/**
 * Used by Registries as a base class to model their associated registrations. For example,
 * {@link AgentRegistry} extends {@link RegistrationList} application and social agent registrations.
 * @param <T>
 */
@Getter
public abstract class RegistrationList<T> implements Iterable<T> {

    protected List<URL> registrationUrls;
    protected DataFactory dataFactory;
    protected Resource resource;
    protected Property linkedVia;

    /**
     * Construct a {@link RegistrationList} (called by sub-classes)
     * @param dataFactory {@link DataFactory} to assign
     * @param resource Jena resource for the associated registry
     * @param linkedVia Jena property that links the registry to the registry set
     */
    public RegistrationList(DataFactory dataFactory, Resource resource, Property linkedVia) {
        Objects.requireNonNull(dataFactory, "Must provide a data factory for the registration list");
        Objects.requireNonNull(resource, "Must provide a Jena resource for the registry the registration list is associated with");
        Objects.requireNonNull(linkedVia, "Must provide a property to link the registration to the registry");
        this.dataFactory = dataFactory;
        this.resource = resource;
        this.linkedVia = linkedVia;
        this.registrationUrls = new ArrayList<>();
    }

    /**
     * Populate the internal list of registration URLs based on links to registrations in the graph of the registry
     * @throws SaiException
     */
    public void populate() throws SaiException {
        this.registrationUrls = getUrlObjects(this.resource, this.linkedVia);
    }

    /**
     * Add a registration to the internal list of registration URLs, and add to the graph of the registry
     * @param registrationUrl URL of the registration to add
     * @throws SaiAlreadyExistsException if the registration already exists
     */
    public void add(URL registrationUrl) throws SaiException, SaiAlreadyExistsException {
        Objects.requireNonNull(registrationUrl, "Must provide the URL of the registration to add to registry");
        T found = find(registrationUrl); // Check first to see if it already exists
        if (found != null) { throw new SaiAlreadyExistsException("Cannot add " + registrationUrl + "because a record already exists"); }
        this.registrationUrls.add(registrationUrl);
        updateUrlObjects(this.resource, this.linkedVia, this.registrationUrls);
    }

    public void addAll(List<URL> registrationUrls) throws SaiAlreadyExistsException {
        Objects.requireNonNull(registrationUrls, "Must provide a list of URLs of the registrations to add to registry");
        for (URL registrationUrl: registrationUrls) {
            T found = find(registrationUrl); // Check first to see if it already exists
            if (found != null) { throw new SaiAlreadyExistsException("Cannot add " + registrationUrl + "because a record already exists"); }
        }
        this.registrationUrls.addAll(registrationUrls);
        updateUrlObjects(this.resource, this.linkedVia, this.registrationUrls);
    }

    public boolean isEmpty() { return this.registrationUrls.isEmpty(); }

    public abstract T find(URL targetUrl);

    /**
     * Remove a registration from the internal list of registration URLs, and remove from the graph of the registry
     * @param registrationUrl URL of the registration to remove
     */
    public void remove(URL registrationUrl) {
        Objects.requireNonNull(registrationUrl, "Must provide the URL of the registration to remove from registry");
        this.registrationUrls.remove(registrationUrl);
        updateUrlObjects(this.resource, this.linkedVia, this.registrationUrls);
    }

    /**
     * Return an iterator for the {@link RegistrationList}
     * @return
     */
    public Iterator<T> iterator() {
        return new RegistrationListIterator<T>(this.dataFactory, this.registrationUrls);
    }

    /**
     * Custom iterator for the {@link RegistrationList}. Will not function unless {@link #hasNext()}
     * is overriden.
     */
    public class RegistrationListIterator<T> implements Iterator<T> {
        public Iterator<URL> current;
        public DataFactory dataFactory;
        public RegistrationListIterator(DataFactory dataFactory, List<URL> registrationUrls) {

            this.dataFactory = dataFactory;
            this.current = registrationUrls.iterator();
        }
        public boolean hasNext() { return current.hasNext(); }
        public T next() { throw new UnsupportedOperationException("Must override registration list iterator"); }
    }

}
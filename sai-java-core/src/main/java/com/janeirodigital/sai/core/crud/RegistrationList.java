package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.annotations.ExcludeFromGeneratedCoverage;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.helpers.RdfUtils.getUrlObjects;
import static com.janeirodigital.sai.core.helpers.RdfUtils.updateUrlObjects;

/**
 * Used by Registries as a base class to model their associated registrations. For example,
 * {@link AgentRegistry} extends {@link RegistrationList} application and social agent registrations.
 * @param <T>
 */
@Getter
public abstract class RegistrationList<T> implements Iterable<T> {

    protected List<URL> registrationUrls;
    protected SaiSession saiSession;
    protected Resource resource;
    protected Property linkedVia;

    /**
     * Construct a {@link RegistrationList} (called by sub-classes)
     * @param saiSession {@link SaiSession} to assign
     * @param resource Jena resource for the associated registry
     * @param linkedVia Jena property that links the registry to the registry set
     */
    protected RegistrationList(SaiSession saiSession, Resource resource, Property linkedVia) {
        Objects.requireNonNull(saiSession, "Must provide a sai session for the registration list");
        Objects.requireNonNull(resource, "Must provide a Jena resource for the registry the registration list is associated with");
        Objects.requireNonNull(linkedVia, "Must provide a property to link the registration to the registry");
        this.saiSession = saiSession;
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
     * Add a registration URL to the internal list of registration URLs, and add to the graph of the registry
     * @param registrationUrl URL of the registration to add
     * @throws SaiAlreadyExistsException if the registration already exists
     */
    public void add(URL registrationUrl) throws SaiException, SaiAlreadyExistsException {
        Objects.requireNonNull(registrationUrl, "Must provide the URL of the registration to add to registry");
        if (this.isPresent(registrationUrl)) {
            throw new SaiAlreadyExistsException("Cannot add " + registrationUrl + "because a record already exists");
        }
        this.registrationUrls.add(registrationUrl);
        updateUrlObjects(this.resource, this.linkedVia, this.registrationUrls);
    }

    /**
     * Add a list of registration URLs to the internal list, and add to the graph of the registry
     * @param registrationUrls List of registration URLs to add
     * @throws SaiAlreadyExistsException if any of the registration URLs already exist
     */
    public void addAll(List<URL> registrationUrls) throws SaiAlreadyExistsException {
        Objects.requireNonNull(registrationUrls, "Must provide a list of URLs of the registrations to add to registry");
        for (URL registrationUrl: registrationUrls) {
            if (this.isPresent(registrationUrl)) {
                throw new SaiAlreadyExistsException("Cannot add " + registrationUrl + "because a record already exists");
            }
        }
        this.registrationUrls.addAll(registrationUrls);
        updateUrlObjects(this.resource, this.linkedVia, this.registrationUrls);
    }

    /**
     * Check to see if the provided URL <code>checkUrl</code> is already part of the registration list
     * @param checkUrl true if <code>checkUrl</code> is already part of the list
     * @return
     */
    public boolean isPresent(URL checkUrl) {
        Objects.requireNonNull(checkUrl, "Must provide the URL of the registration to check for");
        return this.registrationUrls.contains(checkUrl);
    }

    /**
     * Check if the registration list is empty
     * @return true if empty
     */
    public boolean isEmpty() { return this.registrationUrls.isEmpty(); }

    /**
     * Abstract find method implemented by sub-classes
     * @param targetUrl Target URL to find with
     * @return found type
     */
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
    @ExcludeFromGeneratedCoverage
    public Iterator<T> iterator() {
        return new RegistrationListIterator<>(this.saiSession, this.registrationUrls);
    }

    /**
     * Custom iterator for the {@link RegistrationList}. Will not function unless {@link #hasNext()}
     * is overriden.
     */
    public static class RegistrationListIterator<T> implements Iterator<T> {
        protected Iterator<URL> current;
        protected SaiSession saiSession;
        public RegistrationListIterator(SaiSession saiSession, List<URL> registrationUrls) {
            this.saiSession = saiSession;
            this.current = registrationUrls.iterator();
        }
        public boolean hasNext() { return current.hasNext(); }
        @ExcludeFromGeneratedCoverage
        public T next() { throw new UnsupportedOperationException("Must override registration list iterator"); }
    }

}
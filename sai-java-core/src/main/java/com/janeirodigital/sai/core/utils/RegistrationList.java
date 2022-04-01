package com.janeirodigital.sai.core.utils;

import com.janeirodigital.sai.core.agents.AgentRegistry;
import com.janeirodigital.sai.core.annotations.ExcludeFromGeneratedCoverage;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import lombok.Getter;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.rdfutils.RdfUtils.getUriObjects;
import static com.janeirodigital.sai.rdfutils.RdfUtils.updateUriObjects;

/**
 * Used by Registries as a base class to model their associated registrations. For example,
 * {@link AgentRegistry} extends {@link RegistrationList} application and social agent registrations.
 * @param <T>
 */
@Getter
public abstract class RegistrationList<T> implements Iterable<T> {

    protected List<URI> registrationUris;
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
        this.registrationUris = new ArrayList<>();
    }

    /**
     * Populate the internal list of registration URIs based on links to registrations in the graph of the registry
     * @throws SaiException
     */
    public void populate() throws SaiException {
        try {
            this.registrationUris = getUriObjects(this.resource, this.linkedVia);
        } catch (SaiRdfException ex) {
            throw new SaiException("Unable to populate graph", ex);
        }
    }

    /**
     * Add a registration URI to the internal list of registration URIs, and add to the graph of the registry
     * @param registrationUri URI of the registration to add
     * @throws SaiAlreadyExistsException if the registration already exists
     */
    public void add(URI registrationUri) throws SaiAlreadyExistsException {
        Objects.requireNonNull(registrationUri, "Must provide the URI of the registration to add to registry");
        if (this.isPresent(registrationUri)) {
            throw new SaiAlreadyExistsException("Cannot add " + registrationUri + "because a record already exists");
        }
        this.registrationUris.add(registrationUri);
        updateUriObjects(this.resource, this.linkedVia, this.registrationUris);
    }

    /**
     * Add a list of registration URIs to the internal list, and add to the graph of the registry
     * @param registrationUris List of registration URIs to add
     * @throws SaiAlreadyExistsException if any of the registration URIs already exist
     */
    public void addAll(List<URI> registrationUris) throws SaiAlreadyExistsException {
        Objects.requireNonNull(registrationUris, "Must provide a list of URIs of the registrations to add to registry");
        for (URI registrationUri: registrationUris) {
            if (this.isPresent(registrationUri)) {
                throw new SaiAlreadyExistsException("Cannot add " + registrationUri + "because a record already exists");
            }
        }
        this.registrationUris.addAll(registrationUris);
        updateUriObjects(this.resource, this.linkedVia, this.registrationUris);
    }

    /**
     * Check to see if the provided URI <code>checkUri</code> is already part of the registration list
     * @param checkUri true if <code>checkUri</code> is already part of the list
     * @return
     */
    public boolean isPresent(URI checkUri) {
        Objects.requireNonNull(checkUri, "Must provide the URI of the registration to check for");
        return this.registrationUris.contains(checkUri);
    }

    /**
     * Check if the registration list is empty
     * @return true if empty
     */
    public boolean isEmpty() { return this.registrationUris.isEmpty(); }

    /**
     * Abstract find method implemented by sub-classes
     * @param targetUri Target URI to find with
     * @return found type
     */
    public abstract T find(URI targetUri);

    /**
     * Remove a registration from the internal list of registration URIs, and remove from the graph of the registry
     * @param registrationUri URI of the registration to remove
     */
    public void remove(URI registrationUri) {
        Objects.requireNonNull(registrationUri, "Must provide the URI of the registration to remove from registry");
        this.registrationUris.remove(registrationUri);
        updateUriObjects(this.resource, this.linkedVia, this.registrationUris);
    }

    /**
     * Return an iterator for the {@link RegistrationList}
     * @return
     */
    @ExcludeFromGeneratedCoverage
    public Iterator<T> iterator() {
        return new RegistrationListIterator<>(this.saiSession, this.registrationUris);
    }

    /**
     * Custom iterator for the {@link RegistrationList}. Will not function unless {@link #hasNext()}
     * is overriden.
     */
    public static class RegistrationListIterator<T> implements Iterator<T> {
        protected Iterator<URI> current;
        protected SaiSession saiSession;
        public RegistrationListIterator(SaiSession saiSession, List<URI> registrationUris) {
            this.saiSession = saiSession;
            this.current = registrationUris.iterator();
        }
        public boolean hasNext() { return current.hasNext(); }
        @ExcludeFromGeneratedCoverage
        public T next() { throw new UnsupportedOperationException("Must override registration list iterator"); }
    }

}
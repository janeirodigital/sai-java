package com.janeirodigital.sai.core.data;

import com.janeirodigital.sai.core.crud.CRUDResource;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.http.UrlUtils;
import com.janeirodigital.sai.core.readable.InheritableDataGrant;
import com.janeirodigital.sai.core.readable.InheritedDataGrant;
import com.janeirodigital.sai.core.readable.ReadableDataGrant;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.shapetrees.core.exceptions.ShapeTreeException;
import com.janeirodigital.shapetrees.core.validation.ShapeTree;
import com.janeirodigital.shapetrees.core.validation.ShapeTreeFactory;
import com.janeirodigital.shapetrees.core.validation.ShapeTreeReference;
import lombok.Getter;
import org.apache.jena.rdf.model.Property;

import java.net.URI;
import java.util.*;

import static com.janeirodigital.sai.httputils.HttpUtils.*;
import static com.janeirodigital.sai.rdfutils.RdfUtils.*;
import static com.janeirodigital.shapetrees.core.validation.ShapeTreeReference.findChildReference;
import static com.janeirodigital.shapetrees.core.validation.ShapeTreeReference.getPropertyFromReference;

/**
 * General instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-instance">Data Instance</a>.
 * This class should either be extended (through inheritance) or included through composition
 * in a class specific to a given data domain. For a specific instantiation, see {@link BasicDataInstance}.
 */
@Getter
public abstract class DataInstance extends CRUDResource {

    private final ReadableDataGrant dataGrant;
    private final DataInstance parent;
    private final ShapeTree shapeTree;
    private boolean draft;

    protected DataInstance(Builder<?> builder) throws SaiException {
        super(builder);
        Objects.requireNonNull(builder.dataGrant, "Must provide a data grant to construct a data instance");
        Objects.requireNonNull(builder.shapeTree, "Must provide a shape tree to construct a data instance");
        this.dataGrant = builder.dataGrant;
        this.parent = builder.parent;
        this.shapeTree = builder.shapeTree;
        this.draft = builder.draft;
    }

    /**
     * Update the corresponding {@link DataInstance} resource over HTTP with the current
     * contents of the <code>dataset</code>. In the event that an inherited instance is
     * being created, the parent instance is updated to add a reference to the
     * created child instance.
     * @throws SaiException
     */
    @Override
    public void update() throws SaiException {
        if (this.parent != null && this.draft) { this.parent.addChildInstance(this); }
        super.update();
        this.draft = false;
    }

    /**
     * Delete the corresponding {@link DataInstance} resource over HTTP . In the
     * event that an inherited instance is being deleted, the parent instance
     * is updated to remove the reference to the deleted child instance.
     * @throws SaiException
     */
    @Override
    public void delete() throws SaiException {
        if (!this.draft) {
            super.delete();
            if (this.parent != null) parent.removeChildInstance(this);
        }
    }

    /**
     * Gets a list of "child" data instances that are associated with
     * the current instance via
     * <a href="https://shapetrees.org/TR/specification/index.html#shape-tree-reference">shape tree reference</a>
     * as part of an
     * <a href="https://solid.github.io/data-interoperability-panel/specification/#scope-inherited">inherited data grant</a>.
     * @param shapeTreeUri URI of the shape tree type to get children for
     * @return List of data instances for the provided shape tree
     * @throws SaiException
     */
    public DataInstanceList getChildInstances(URI shapeTreeUri) throws SaiException {
        // Lookup the inherited child grant based on the shape tree type
        ReadableDataGrant childGrant = findChildGrant(shapeTreeUri);
        // get "child references" for shape tree - gets the shape path for a referenced shape tree // looks in the graph for instances
        Map<URI, DataInstance> childInstanceUris = new HashMap<>();
        for (URI childReference : getChildReferences(shapeTreeUri)) { childInstanceUris.put(childReference, this); }
        return new DataInstanceList(this.saiSession, childGrant, childInstanceUris);
    }

    /**
     * Add a child data instance by adding a
     * <a href="https://shapetrees.org/TR/specification/index.html#shape-tree-reference">shape tree reference</a>
     * relationship to the instance graph, as authorized by an
     * <a href="https://solid.github.io/data-interoperability-panel/specification/#scope-inherited">inherited data grant</a>.
     * @param childInstance Child {@link DataInstance} to add
     * @throws SaiException
     */
    public void addChildInstance(DataInstance childInstance) throws SaiException {
        // Lookup the shape tree reference for the child instance
        ShapeTreeReference reference = findChildShapeTreeReference(urlToUri(childInstance.getShapeTree().getId()));
        if (reference == null) { throw new SaiException("Cannot find a child reference to shape tree " + this.getShapeTree().getId() + " to add to parent data instance: " + this.getUri()); }
        // Get the property use from the shape tree reference
        Property property = getPropertyFromShapeTreeReference(reference);
        // add to the instance graph
        updateObject(this.resource, property, childInstance.getUri());
        // update the instance graph
        this.update();
    }

    /**
     * Remove a child data instance by removing a
     * <a href="https://shapetrees.org/TR/specification/index.html#shape-tree-reference">shape tree reference</a>
     * relationship from the instance graph.
     * @param childInstance Child {@link DataInstance} to remove
     * @throws SaiException
     */
    public void removeChildInstance(DataInstance childInstance) throws SaiException {
        // Lookup the shape tree reference for the child instance
        ShapeTreeReference reference = findChildShapeTreeReference(urlToUri(childInstance.getShapeTree().getId()));
        if (reference == null) { throw new SaiException("Cannot find a child reference to shape tree " + this.getShapeTree().getId() + " to remove from parent data instance: " + this.getUri()); }
        // Get the property use from the shape tree reference
        Property property = getPropertyFromShapeTreeReference(reference);
        // Get the existing references from the graph and remove the child reference
        List<URI> uriReferences;
        try { uriReferences = getUriObjects(this.resource, property); } catch (SaiRdfException ex) {
            throw new SaiException("Unable to get existing references from graph", ex);
        }
        uriReferences.remove(childInstance.getUri());
        updateUriObjects(this.resource, property, uriReferences);
        // update the instance graph
        this.update();
    }

    /**
     * Gets a list of references to "child" data instances based on a
     * <a href="https://shapetrees.org/TR/specification/index.html#shape-tree-reference">shape tree reference</a>
     * in the shape tree identified by the provided <code>shapeTreeUri</code>.
     * @param shapeTreeUri URI of the shape tree type to get children for
     * @return List of child data instance URIs
     * @throws SaiException
     */
    public List<URI> getChildReferences(URI shapeTreeUri) throws SaiException {
        List<URI> childUris = new ArrayList<>();
        ShapeTreeReference reference = findChildShapeTreeReference(shapeTreeUri);
        List<URI> foundUris = findChildReferences(reference);
        if (!foundUris.isEmpty()) { childUris.addAll(foundUris); }
        return childUris;
    }

    /**
     * Generate the URI for a new {@link DataInstance} as permitted by the
     * provided <code>dataGrant</code>. UUID will be generated and used for <code>resourceName</code>
     * if it is null.
     * @param dataGrant {@link ReadableDataGrant} allowing the instance to be created
     * @param resourceName Name of the instance to create
     * @return URI for new instance
     * @throws SaiException
     */
    public static URI generateUri(ReadableDataGrant dataGrant, String resourceName) throws SaiException {
        if (resourceName == null) resourceName = UUID.randomUUID().toString();
        try { return addChildToUriPath(dataGrant.getDataRegistration(), resourceName); } catch (SaiHttpException ex) {
            throw new SaiException("Unable to add child to uri path", ex);
        }
    }

    /**
     * Generate the URI for a new {@link DataInstance} as permitted by the
     * provided <code>dataGrant</code>, with a generated UUID as resource name.
     * @param dataGrant {@link ReadableDataGrant} allowing the instance to be created
     * @return URI for new instance
     * @throws SaiException
     */
    public static URI generateUri(ReadableDataGrant dataGrant) throws SaiException {
        return generateUri(dataGrant, null);
    }

    public ReadableDataGrant findChildGrant(URI shapeTreeUri) throws SaiException {
        if (this.dataGrant instanceof InheritedDataGrant) { throw new SaiException("Cannot lookup child grant - child instance cannot have other child instances"); }
        InheritableDataGrant inheritableGrant = (InheritableDataGrant) this.dataGrant;
        for (ReadableDataGrant childGrant : inheritableGrant.getInheritingGrants()) {
            if (childGrant.getRegisteredShapeTree().equals(shapeTreeUri)) { return childGrant; }
        }
        return null;
    }

    public boolean hasAccessible(URI shapeTreeUri) throws SaiException {
        return findChildGrant(shapeTreeUri) != null;
    }

    /**
     * Searches the instance graph for instances of the provided
     * <a href="https://shapetrees.org/TR/specification/index.html#shape-tree-reference">shape tree reference</a>
     * and returns the associated targets (objects) they link to
     * @param reference shape tree references search with
     * @return List of referenced URIs
     * @throws SaiException
     */
    protected List<URI> findChildReferences(ShapeTreeReference reference) throws SaiException {
        Property lookupVia = getPropertyFromShapeTreeReference(reference);
        try { return getUriObjects(this.resource, lookupVia); } catch (SaiRdfException ex) {
            throw new SaiException("Unable to lookup child references from graph", ex);
        }
    }

    /**
     * Wrapper around shape tree operation to find child reference.
     */
    private ShapeTreeReference findChildShapeTreeReference(URI shapeTreeUri) throws SaiException {
        try { return findChildReference(this.getShapeTree(), UrlUtils.uriToUrl(shapeTreeUri)); } catch (ShapeTreeException ex) {
            throw new SaiException("Failed to lookup child shape tree reference", ex);
        }
    }

    /**
     * Wrapper around shape tree operation to get property from shape tree reference
     */
    private Property getPropertyFromShapeTreeReference(ShapeTreeReference shapeTreeReference) throws SaiException {
        try { return getPropertyFromReference(shapeTreeReference); } catch (ShapeTreeException ex) {
            throw new SaiException("Failed to get property from shape tree reference", ex);
        }
    }

    /**
     * Builder for {@link DataInstance} instances.
     */
    public abstract static class Builder <T extends CRUDResource.Builder<T>> extends CRUDResource.Builder<T> {

        protected ReadableDataGrant dataGrant;
        protected DataInstance parent;
        protected ShapeTree shapeTree;
        protected boolean draft;

        /**
         * Initialize builder with <code>uri</code> and <code>saiSession</code>
         * @param uri URI of the {@link DataInstance} to build
         * @param saiSession {@link SaiSession} to assign
         */
        protected Builder(URI uri, SaiSession saiSession) {
            super(uri, saiSession);
            this.unprotected = false;
            this.draft = true;
        }

        /**
         * Initialize builder with a {@link DataInstance}
         * @param dataInstance {@link DataInstance} to initialize from
         */
        public Builder(DataInstance dataInstance) throws SaiException {
            this(dataInstance.getUri(), dataInstance.getSaiSession());
            this.setDataGrant(dataInstance.getDataGrant());
            if (dataInstance.getParent() != null) { this.setParent(dataInstance.getParent()); }
            setDataset(dataInstance.getDataset());
        }

        /**
         * Set the {@link ReadableDataGrant} associated with the {@link DataInstance}. This is the
         * grant that the {@link DataInstance} is being accessed through. Also looks up the
         * Shape Tree associated with the {@link ReadableDataGrant} and stores it.
         * @param dataGrant {@link ReadableDataGrant} to set
         * @return {@link Builder}
         * @throws SaiException
         */
        public T setDataGrant(ReadableDataGrant dataGrant) throws SaiException {
            Objects.requireNonNull(dataGrant, "Must provide a data grant for the data instance builder");
            this.dataGrant = dataGrant;
            try { this.shapeTree = ShapeTreeFactory.getShapeTree(UrlUtils.uriToUrl(dataGrant.getRegisteredShapeTree())); } catch (ShapeTreeException ex) {
                throw new SaiException("Failed to get shape tree " + dataGrant.getRegisteredShapeTree() + " associated with data grant " + dataGrant.getUri(), ex);
            }
            return getThis();
        }

        /**
         * Set the parent associated with a "child" {@link DataInstance}. Only applicable in cases
         * where there is an inherited parent / child relationship.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#scope-inherited">Inherited Data Acecss Scope</a>
         * @param parent Parent {@link DataInstance} to set
         * @return {@link Builder}
         */
        public T setParent(DataInstance parent) {
            Objects.requireNonNull(parent, "Must provide a parent data instance for the data instance builder");
            this.parent = parent;
            return getThis();
        }

        /**
         * Set whether or not the {@link DataInstance} is a draft. A draft means that
         * it has not been updated on the resource server yet.
         * @param status boolean draft status
         * @return {@link Builder}
         */
        public T setDraft(boolean status) {
            this.draft = status;
            return getThis();
        }

    }

}

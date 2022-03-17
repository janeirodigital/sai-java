package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.crud.CRUDResource;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.shapetrees.core.exceptions.ShapeTreeException;
import com.janeirodigital.shapetrees.core.validation.ShapeTree;
import com.janeirodigital.shapetrees.core.validation.ShapeTreeFactory;
import com.janeirodigital.shapetrees.core.validation.ShapeTreeReference;
import lombok.Getter;
import org.apache.jena.rdf.model.Property;

import java.net.URL;
import java.util.*;

import static com.janeirodigital.sai.core.helpers.HttpHelper.addChildToUrlPath;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.shapetrees.core.validation.ShapeTreeReference.findChildReference;
import static com.janeirodigital.shapetrees.core.validation.ShapeTreeReference.getPropertyFromReference;

/**
 * General instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-instance">Data Instance</a>
 */
@Getter
public class DataInstance extends CRUDResource {

    private final ReadableDataGrant dataGrant;
    private final DataInstance parent;
    private final ShapeTree shapeTree;
    private boolean draft;


    protected DataInstance(Builder builder) throws SaiException {
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
        if (this.parent != null && this.draft) { this.parent.addChildReference(this); }
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
            if (this.parent != null) parent.removeChildReference(this);
        }
    }

    public DataInstanceList getChildInstances(URL shapeTreeUrl) throws SaiException {
        // Lookup the inherited child grant based on the shape tree type
        ReadableDataGrant childGrant = findChildGrant(shapeTreeUrl);
        // get "child references" for shape tree - gets the shape path for a referenced shape tree // looks in the graph for instances
        Map<URL, DataInstance> childInstanceUrls = new HashMap<>();
        for (URL childReference : getChildReferences(shapeTreeUrl)) { childInstanceUrls.put(childReference, this); }
        return new DataInstanceList(this.saiSession, childGrant, childInstanceUrls);
    }

    public void addChildReference(DataInstance childInstance) throws SaiException {
        // Lookup the shape tree reference for the child instance
        ShapeTreeReference reference = findChildShapeTreeReference(this.getShapeTree(), childInstance.getShapeTree().getId());
        if (reference == null) { throw new SaiException("Cannot find a child reference to shape tree " + this.getShapeTree().getId() + " to add to parent data instance: " + this.getUrl()); }
        // Get the property use from the shape tree reference
        Property property = getPropertyFromShapeTreeReference(reference);
        // add to the instance graph
        updateObject(this.resource, property, childInstance.getUrl());
        // update the instance graph
        this.update();
    }

    public void removeChildReference(DataInstance childInstance) throws SaiException {
        // Lookup the shape tree reference for the child instance
        ShapeTreeReference reference = findChildShapeTreeReference(this.getShapeTree(), childInstance.getShapeTree().getId());
        if (reference == null) { throw new SaiException("Cannot find a child reference to shape tree " + this.getShapeTree().getId() + " to remove from parent data instance: " + this.getUrl()); }
        // Get the property use from the shape tree reference
        Property property = getPropertyFromShapeTreeReference(reference);
        // Get the existing references from the graph and remove the child reference
        List<URL> urlReferences = getUrlObjects(this.resource, property);
        urlReferences.remove(childInstance.getUrl());
        updateUrlObjects(this.resource, property, urlReferences);
        // update the instance graph
        this.update();
    }

    public List<URL> getChildReferences(URL shapeTreeUrl) throws SaiException {
        List<URL> childUrls = new ArrayList<>();
        ShapeTreeReference reference = findChildShapeTreeReference(this.getShapeTree(), shapeTreeUrl);
        List<URL> foundUrls = findChildInstances(reference);
        if (!foundUrls.isEmpty()) { childUrls.addAll(foundUrls); }
        return childUrls;
    }

    public static URL generateUrl(ReadableDataGrant dataGrant, String resourceName) throws SaiException {
        if (resourceName == null) resourceName = UUID.randomUUID().toString();
        return addChildToUrlPath(dataGrant.getDataRegistration(), resourceName);
    }

    public static URL generateUrl(ReadableDataGrant dataGrant) throws SaiException {
        return generateUrl(dataGrant, null);
    }

    public List<URL> findChildInstances(ShapeTreeReference reference) throws SaiException {
        Property lookupVia = getPropertyFromShapeTreeReference(reference);
        return getUrlObjects(this.resource, lookupVia);
    }

    public ReadableDataGrant findChildGrant(URL shapeTreeUrl) throws SaiException {
        if (this.dataGrant instanceof InheritedDataGrant) { throw new SaiException("Cannot lookup child grant - child instance cannot have other child instances"); }
        InheritableDataGrant inheritableGrant = (InheritableDataGrant) this.dataGrant;
        for (ReadableDataGrant childGrant : inheritableGrant.getInheritingGrants()) {
            if (childGrant.getRegisteredShapeTree().equals(shapeTreeUrl)) { return childGrant; }
        }
        return null;
    }

    public boolean hasAccessible(URL shapeTreeUrl) throws SaiException {
        return findChildGrant(shapeTreeUrl) != null;
    }

    private ShapeTreeReference findChildShapeTreeReference(ShapeTree shapeTree, URL shapeTreeUrl) throws SaiException {
        try { return findChildReference(this.getShapeTree(), shapeTreeUrl); } catch (ShapeTreeException ex) {
            throw new SaiException("Failed to lookup child shape tree reference: " + ex.getMessage());
        }
    }

    private Property getPropertyFromShapeTreeReference(ShapeTreeReference shapeTreeReference) throws SaiException {
        try { return getPropertyFromReference(shapeTreeReference); } catch (ShapeTreeException ex) {
            throw new SaiException("Failed to get property from shape tree reference: " + ex.getMessage());
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
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link DataInstance} to build
         * @param saiSession {@link SaiSession} to assign
         */
        protected Builder(URL url, SaiSession saiSession) {
            super(url, saiSession);
            this.unprotected = false;
            this.draft = true;
        }

        /**
         * Initialize builder with a {@DataInstance}
         * @param dataInstance {@link DataInstance} to initialize from
         */
        public Builder(DataInstance dataInstance) throws SaiException {
            this(dataInstance.getUrl(), dataInstance.getSaiSession());
            this.setDataGrant(dataInstance.getDataGrant());
            if (dataInstance.getParent() != null) { this.setParent(dataInstance.getParent()); }
            setDataset(dataInstance.getDataset());
        }

        public T setDataGrant(ReadableDataGrant dataGrant) throws SaiException {
            Objects.requireNonNull(dataGrant, "Must provide a data grant for the data instance builder");
            this.dataGrant = dataGrant;
            try { this.shapeTree = ShapeTreeFactory.getShapeTree(dataGrant.getRegisteredShapeTree()); } catch (ShapeTreeException ex) {
                throw new SaiException("Failed to get shape tree " + dataGrant.getRegisteredShapeTree() + " associated with data grant " + dataGrant.getUrl());
            }
            return getThis();
        }

        public T setParent(DataInstance parent) {
            Objects.requireNonNull(parent, "Must provide a parent data instance for the data instance builder");
            this.parent = parent;
            return getThis();
        }

        public T setDraft(boolean status) {
            this.draft = status;
            return getThis();
        }

    }

}

package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.crud.CRUDResource;
import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.shapetrees.core.exceptions.ShapeTreeException;
import com.janeirodigital.shapetrees.core.validation.ShapeTree;
import com.janeirodigital.shapetrees.core.validation.ShapeTreeFactory;
import com.janeirodigital.shapetrees.core.validation.ShapeTreeReference;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.janeirodigital.sai.core.helpers.HttpHelper.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.core.helpers.HttpHelper.getRdfModelFromResponse;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;

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


    private DataInstance(Builder builder) throws SaiException {
        super(builder);
        this.dataGrant = builder.dataGrant;
        this.parent = builder.parent;
        this.shapeTree = builder.shapeTree;
        this.draft = builder.draft;
    }

    /**
     * Get a {@link DataInstance} at the provided <code>url</code>
     * @param url URL of the {@link DataInstance} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @param unprotected when true no credentials are sent in subsequent requests
     * @param dataGrant {@link ReadableDataGrant} furnishing instance access
     * @return Retrieved {@link DataInstance}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static DataInstance get(URL url, SaiSession saiSession, ContentType contentType, boolean unprotected, ReadableDataGrant dataGrant) throws SaiException, SaiNotFoundException {
        DataInstance.Builder builder = new DataInstance.Builder(url, saiSession);
        if (unprotected) builder.setUnprotected();
        try (Response response = read(url, saiSession, contentType, unprotected)) {
            builder.setDataset(getRdfModelFromResponse(response)).setContentType(contentType);
        }
        return builder.setDataGrant(dataGrant).build();
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType, boolean, ReadableDataGrant)}
     * without specifying a desired content type for retrieval
     * @param url URL of the {@link DataInstance} to get
     * @param saiSession {@link SaiSession} to assign
     * @param unprotected when true no credentials are sent in subsequent requests
     * @param dataGrant {@link ReadableDataGrant} associated with {@link DataInstance} access
     * @return Retrieved {@link DataInstance}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public static DataInstance get(URL url, SaiSession saiSession, boolean unprotected, ReadableDataGrant dataGrant) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE, unprotected, dataGrant);
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
            this.parent.removeChildReference(this);
        }
    }

    public DataInstanceList getChildInstances(URL shapeTreeUrl) throws SaiException {
        // Lookup the inherited child grant based on the shape tree type
        ReadableDataGrant childGrant = findChildGrant(shapeTreeUrl);
        // get "child references" for shape tree - gets the shape path for a referenced shape tree // looks in the graph for instances
        List<URL> childUrls = getChildReferences(shapeTreeUrl);
        return new DataInstanceList(this.saiSession, childGrant, childUrls);
    }

    public DataInstance newChildInstance(URL shapeTreeUrl) throws SaiException, SaiNotFoundException {
        ReadableDataGrant childGrant = findChildGrant(shapeTreeUrl);
        if (childGrant == null) { throw new SaiNotFoundException("Cannot find a child grant associated with shape tree " + shapeTreeUrl + " for parent instance " + this.getUrl()); }
        return childGrant.newDataInstance(this);
    }

    public void addChildReference(DataInstance childInstance) throws SaiException {
        // Lookup the shape tree reference for the child instance
        ShapeTreeReference reference = findChildReference(childInstance.shapeTree.getId());
        if (reference == null) { throw new SaiException("Cannot find a child reference to shape tree " + this.shapeTree.getId() + " to add to parent data instance: " + this.getUrl()); }
        // Get the property use from the shape tree reference
        Property property = getPropertyFromReference(reference);
        if (property == null) { throw new SaiException("Unable to find a property to add child instance to parent instance with: " + this.getUrl()); }
        // add to the instance graph
        updateObject(this.resource, property, childInstance.getUrl());
        // update the instance graph
        this.update();
    }

    public void removeChildReference(DataInstance childInstance) throws SaiException {
        // Lookup the shape tree reference for the child instance
        ShapeTreeReference reference = findChildReference(childInstance.shapeTree.getId());
        if (reference == null) { throw new SaiException("Cannot find a child reference to shape tree " + this.shapeTree.getId() + " to remove from parent data instance: " + this.getUrl()); }
        // Get the property use from the shape tree reference
        Property property = getPropertyFromReference(reference);
        if (property == null) { throw new SaiException("Unable to find a property to remove child instance from parent instance with: " + this.getUrl()); }
        // Get the existing references from the graph and remove the child reference
        List<URL> urlReferences = getUrlObjects(this.resource, property);
        urlReferences.remove(childInstance.getUrl());
        updateUrlObjects(this.resource, property, urlReferences);
        // update the instance graph
        this.update();
    }

    public List<URL> getChildReferences(URL shapeTreeUrl) throws SaiException {
        List<URL> childUrls = new ArrayList<>();
        ShapeTreeReference reference = findChildReference(shapeTreeUrl);
        List<URL> foundUrls = findChildInstances(reference);
        if (foundUrls.isEmpty()) { childUrls.addAll(foundUrls); }
        return childUrls;
    }

    private ShapeTreeReference findChildReference(URL shapeTreeUrl) throws SaiException {
        try {
            Iterator<ShapeTreeReference> iterator = this.shapeTree.getReferencedShapeTrees();
            while (iterator.hasNext()) {
                ShapeTreeReference reference = iterator.next();
                if (reference.getReferenceUrl().equals(shapeTreeUrl)) { return reference; }
            }
        } catch (ShapeTreeException ex) {
            throw new SaiException("Failed to lookup shape tree references for shape tree: " + this.shapeTree.getId());
        }
        return null;
    }

    private List<URL> findChildInstances(ShapeTreeReference reference) throws SaiException {
        Property lookupVia = getPropertyFromReference(reference);
        if (lookupVia == null) return Arrays.asList();
        return getUrlObjects(this.resource, lookupVia);
    }

    private Property getPropertyFromReference(ShapeTreeReference reference) {
        Property property = null;
        if (reference.viaPredicate()) { property = ResourceFactory.createProperty(reference.getPredicate().toString()); }
        if (reference.viaShapePath()) {
            // TODO - this is a bit of a workaround the extract the target property from the shape path, given the
            // TODO - (cont'd) current lack of a shape path parser in java. It is functionally equivalent to viaPredicate
            Pattern pattern = Pattern.compile("@\\S+~(\\S*$)");
            Matcher matcher = pattern.matcher(reference.getShapePath());
            String parsed = matcher.group(1);
            if (parsed == null) return null;
            property = ResourceFactory.createProperty(parsed);
        }
        return property;
    }

    private ReadableDataGrant findChildGrant(URL shapeTreeUrl) throws SaiException {
        if (this.dataGrant instanceof InheritedDataGrant) { throw new SaiException("Cannot lookup child grant - child instance cannot have other child instances"); }
        InheritableDataGrant inheritableGrant = (InheritableDataGrant) this.dataGrant;
        for (ReadableDataGrant childGrant : inheritableGrant.getInheritingGrants()) {
            if (childGrant.getRegisteredShapeTree().equals(shapeTreeUrl)) { return childGrant; }
        }
        return null;
    }

    /**
     * Builder for {@link DataInstance} instances.
     */
    public static class Builder extends CRUDResource.Builder<Builder> {

        private ReadableDataGrant dataGrant;
        private DataInstance parent;
        private ShapeTree shapeTree;
        private boolean draft;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link DataInstance} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URL url, SaiSession saiSession) {
            super(url, saiSession);
            this.unprotected = false;
            this.draft = true;
        }

        /**
         * Ensures that don't get an unchecked cast warning when returning from setters
         * @return {@link Builder}
         */
        @Override
        public Builder getThis() { return this; }

        public Builder setDataGrant(ReadableDataGrant dataGrant) throws SaiException {
            Objects.requireNonNull(dataGrant, "Must provide a data grant for the data instance builder");
            this.dataGrant = dataGrant;
            try { this.shapeTree = ShapeTreeFactory.getShapeTree(dataGrant.getRegisteredShapeTree()); } catch (ShapeTreeException ex) {
                throw new SaiException("Failed to get shape tree " + dataGrant.getRegisteredShapeTree() + " associated with data grant " + dataGrant.getUrl());
            }
            return this;
        }

        public Builder setParent(DataInstance parent) {
            Objects.requireNonNull(parent, "Must provide a parent data instance for the data instance builder");
            this.parent = parent;
            return this;
        }

        public Builder setDraft(boolean status) {
            this.draft = status;
            return this;
        }

        public DataInstance build() throws SaiException {
            Objects.requireNonNull(dataGrant, "Must provide a data grant for the data instance builder");
            if (this.dataset == null) {
                // In cases where a Data Instance is being created, initialize an empty graph resource that
                // the application code can populate.
                this.resource = getNewResource(this.url);
                this.dataset = this.resource.getModel();
            }
            return new DataInstance(this);
        }
    }
}

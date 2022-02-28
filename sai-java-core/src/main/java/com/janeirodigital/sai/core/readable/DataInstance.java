package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.crud.CRUDResource;
import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.shapetrees.core.exceptions.ShapeTreeException;
import com.janeirodigital.shapetrees.core.validation.ShapeTree;
import com.janeirodigital.shapetrees.core.validation.ShapeTreeFactory;
import com.janeirodigital.shapetrees.core.validation.ShapeTreeReference;
import okhttp3.Headers;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;

/**
 * General instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-instance">Data Instance</a>
 */
public class DataInstance extends CRUDResource {

    private final ReadableDataGrant dataGrant;
    private final DataInstance parent;
    private final ShapeTree shapeTree;
    private boolean draft;


    private DataInstance(URL url, SaiSession saiSession, boolean unprotected, Model dataset, Resource resource, ContentType contentType,
                         ReadableDataGrant dataGrant, DataInstance parent, ShapeTree shapeTree, boolean draft) throws SaiException {
        super(url, saiSession, unprotected);
        this.dataset = dataset;
        this.resource = resource;
        this.contentType = contentType;
        this.dataGrant = dataGrant;
        this.parent = parent;
        this.shapeTree = shapeTree;
        this.draft = draft;
    }

    /**
     * Get a {@link DataInstance} at the provided <code>url</code>
     * @param url URL of the {@link DataInstance} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link DataInstance}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static DataInstance get(URL url, SaiSession saiSession, boolean unprotected, ContentType contentType, ReadableDataGrant dataGrant) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(url, "Must provide the URL of the readable data instance to get");
        Objects.requireNonNull(saiSession, "Must provide a sai session to assign to the readable data instance");
        Objects.requireNonNull(contentType, "Must provide a content type for the readable data instance");
        DataInstance.Builder builder = new DataInstance.Builder(url, saiSession, contentType);
        if (!unprotected) { return getProtected(url, saiSession, contentType, dataGrant, builder); } else { return getUnprotected(url, saiSession, contentType, dataGrant, builder); }
    }

    /**
     * Call {@link #get(URL, SaiSession, boolean, ContentType, ReadableDataGrant)}
     * without specifying a desired content type for retrieval
     * @param url URL of the {@link DataInstance} to get
     * @param saiSession {@link SaiSession} to assign
     * @param dataGrant {@link ReadableDataGrant} associated with {@link DataInstance} access
     * @return Retrieved {@link DataInstance}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public static DataInstance get(URL url, SaiSession saiSession, ReadableDataGrant dataGrant) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, false, DEFAULT_RDF_CONTENT_TYPE, dataGrant);
    }
    
    private static DataInstance getProtected(URL url, SaiSession saiSession, ContentType contentType, ReadableDataGrant dataGrant, DataInstance.Builder builder) throws SaiException, SaiNotFoundException {
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(saiSession.getAuthorizedSession(), saiSession.getHttpClient(), url, headers))) {
            builder.setDataset(getRdfModelFromResponse(response));
        }
        builder.setDataGrant(dataGrant);
        return builder.build();
    }

    private static DataInstance getUnprotected(URL url, SaiSession saiSession, ContentType contentType, ReadableDataGrant dataGrant, DataInstance.Builder builder) throws SaiException, SaiNotFoundException {
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getRdfResource(saiSession.getHttpClient(), url, headers))) {
            builder.setDataset(getRdfModelFromResponse(response));
        }
        builder.setDataGrant(dataGrant);
        builder.setUnprotected();
        return builder.build();
    }

    @Override
    public void update() throws SaiException {
        if (this.parent != null && this.draft) { this.parent.addChildReference(this); }
        super.update();
        this.draft = false;
    }

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
    public static class Builder {

        private final URL url;
        private final SaiSession saiSession;
        private final ContentType contentType;
        private Model dataset;
        private Resource resource;
        private ReadableDataGrant dataGrant;
        private DataInstance parent;
        private ShapeTree shapeTree;
        private boolean unprotected;
        private boolean draft;


        public Builder(URL url, SaiSession saiSession, ContentType contentType) {
            Objects.requireNonNull(url, "Must provide a URL for the data instance builder");
            Objects.requireNonNull(saiSession, "Must provide a sai session for the data instance builder");
            Objects.requireNonNull(contentType, "Must provide a content type for the data instance builder");
            this.url = url;
            this.saiSession = saiSession;
            this.contentType = contentType;
            this.unprotected = false;
            this.draft = true;
        }

        /**
         * Optional Jena Model that will initialize the attributes of the Builder rather than set
         * them manually. Typically used in read scenarios when populating the Builder from
         * the contents of a remote resource.
         *
         * @param dataset Jena model to populate the Builder attributes with
         * @return {@link Builder}
         */
        public Builder setDataset(Model dataset) {
            Objects.requireNonNull(dataset, "Must provide a Jena model for the data instance builder");
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            return this;
        }

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

        public Builder setUnprotected() {
            this.unprotected = true;
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
            return new DataInstance(this.url, this.saiSession, this.unprotected, this.dataset, this.resource,
                                    this.contentType, this.dataGrant, this.parent, this.shapeTree, this.draft);
        }

    }

}

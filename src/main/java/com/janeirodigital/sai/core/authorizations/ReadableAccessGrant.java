package com.janeirodigital.sai.core.authorizations;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.resources.ReadableResource;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.sai.rdfutils.SaiRdfNotFoundException;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.httputils.HttpUtils.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.rdfutils.RdfUtils.*;

/**
 * Readable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#access-grant">Access Grant</a>
 */
@Getter
public class ReadableAccessGrant extends ReadableResource {

    private final URI grantedBy;
    private final OffsetDateTime grantedAt;
    private final URI grantee;
    private final URI accessNeedGroup;
    private final List<ReadableDataGrant> dataGrants;

    /**
     * Construct a {@link ReadableAccessGrant} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private ReadableAccessGrant(Builder builder) throws SaiException {
        super(builder);
        this.grantedBy = builder.grantedBy;
        this.grantedAt = builder.grantedAt;
        this.grantee = builder.grantee;
        this.accessNeedGroup = builder.accessNeedGroup;
        this.dataGrants = builder.dataGrants;
    }

    /**
     * Get a {@link ReadableAccessGrant} at the provided <code>uri</code>
     * @param uri URI of the {@link ReadableAccessGrant} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link ReadableAccessGrant}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static ReadableAccessGrant get(URI uri, SaiSession saiSession, ContentType contentType) throws SaiException, SaiHttpNotFoundException {
        Builder builder = new Builder(uri, saiSession);
        try (Response response = read(uri, saiSession, contentType, false)) {
            return builder.setDataset(response).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URI, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param uri URI of the {@link ReadableAccessGrant} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link ReadableAccessGrant}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public static ReadableAccessGrant get(URI uri, SaiSession saiSession) throws SaiHttpNotFoundException, SaiException {
        return get(uri, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link ReadableAccessGrant} using the attributes of the current instance
     * @return Reloaded {@link ReadableAccessGrant}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public ReadableAccessGrant reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.uri, this.saiSession, this.contentType);
    }

    /**
     * Lookup {@link ReadableDataGrant}s linked to the {@link ReadableAccessGrant} by data owner and
     * shape tree
     * @param dataOwnerUri URI of the data owner that granted access
     * @param shapeTreeUri URI of the shape tree associated with the data
     * @return List of matching {@link ReadableDataGrant}
     */
    public List<ReadableDataGrant> findDataGrants(URI dataOwnerUri, URI shapeTreeUri) {
        Objects.requireNonNull(dataOwnerUri, "Must provide the URI of the data owner to find data grant");
        Objects.requireNonNull(shapeTreeUri, "Must provide the URI of the shape tree to find data grant");
        List<ReadableDataGrant> dataGrants = new ArrayList<>();
        for (ReadableDataGrant dataGrant : this.getDataGrants()) {
            if (!dataGrant.getDataOwner().equals(dataOwnerUri)) continue;
            if (!dataGrant.getRegisteredShapeTree().equals(shapeTreeUri)) continue;
            dataGrants.add(dataGrant);
        }
        return dataGrants;
    }

    /**
     * Lookup {@link ReadableDataGrant}s linked to the {@link ReadableAccessGrant} by shape tree
     * @param shapeTreeUri URI of the shape tree associated with the data
     * @return List of matching {@link ReadableDataGrant}
     */
    public List<ReadableDataGrant> findDataGrants(URI shapeTreeUri) {
        Objects.requireNonNull(shapeTreeUri, "Must provide the URI of the shape tree to find data grant");
        List<ReadableDataGrant> dataGrants = new ArrayList<>();
        for (ReadableDataGrant dataGrant : this.getDataGrants()) {
            if (dataGrant.getRegisteredShapeTree().equals(shapeTreeUri)) dataGrants.add(dataGrant);
        }
        return dataGrants;
    }

    /**
     * Lookup the data owners represented by the {@link ReadableDataGrant}s linked to the {@link ReadableAccessGrant}
     * @return List of data owner identifiers
     */
    public List<URI> getDataOwners() {
        List<URI> dataOwners = new ArrayList<>();
        for (ReadableDataGrant dataGrant : this.getDataGrants()) {
            if (!dataOwners.contains(dataGrant.getDataOwner())) dataOwners.add(dataGrant.getDataOwner());
        }
        return dataOwners;
    }

    /**
     * Builder for {@link ReadableAccessGrant} instances.
     */
    public static class Builder extends ReadableResource.Builder<Builder> {

        private URI grantedBy;
        private OffsetDateTime grantedAt;
        private URI grantee;
        private URI accessNeedGroup;
        private final List<ReadableDataGrant> dataGrants;

        /**
         * Initialize builder with <code>uri</code> and <code>saiSession</code>
         * @param uri URI of the {@link ReadableAccessGrant} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URI uri, SaiSession saiSession) {
            super(uri, saiSession);
            this.dataGrants = new ArrayList<>();
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
         * Populates "parent" inheritable data grants with the "child" inherited data grants
         * that inherit from them
         */
        private void organizeInheritance() {
            for (ReadableDataGrant dataGrant : this.dataGrants) {
                if (dataGrant instanceof InheritableDataGrant) {
                    InheritableDataGrant parentDataGrant = (InheritableDataGrant) dataGrant;
                    for (ReadableDataGrant childGrant : this.dataGrants) {
                        if (childGrant instanceof InheritedDataGrant) {
                            InheritedDataGrant inheritedChildGrant = (InheritedDataGrant) childGrant;
                            if (inheritedChildGrant.getInheritsFrom().equals(dataGrant.getUri())) {
                                parentDataGrant.getInheritingGrants().add(inheritedChildGrant);
                            }
                        }
                    }
                }
            }
        }

        /**
         * Populates the fields of the {@link Builder} based on the associated Jena resource.
         * Also retrieves and populates the associated {@link ReadableDataGrant}s.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException {
            try {
                this.grantedBy = getRequiredUriObject(this.resource, GRANTED_BY);
                this.grantedAt = getRequiredDateTimeObject(this.resource, GRANTED_AT);
                this.grantee = getRequiredUriObject(this.resource, GRANTEE);
                this.accessNeedGroup = getRequiredUriObject(this.resource, HAS_ACCESS_NEED_GROUP);
                List<URI> dataGrantUris = getRequiredUriObjects(this.resource, HAS_DATA_GRANT);
                for (URI uri : dataGrantUris) { this.dataGrants.add(ReadableDataGrant.get(uri, this.saiSession)); }
                organizeInheritance();
            } catch (SaiHttpNotFoundException | SaiException | SaiRdfException | SaiRdfNotFoundException ex) {
                throw new SaiException("Unable to populate immutable access grant resource", ex);
            }
        }

        /**
         * Build the {@link ReadableAccessGrant} using attributes from the Builder.
         * @return {@link ReadableAccessGrant}
         * @throws SaiException
         */
        public ReadableAccessGrant build() throws SaiException {
            return new ReadableAccessGrant(this);
        }
    }

}

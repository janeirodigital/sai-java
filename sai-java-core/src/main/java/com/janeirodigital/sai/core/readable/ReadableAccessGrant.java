package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.helpers.HttpHelper.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.core.helpers.HttpHelper.getRdfModelFromResponse;
import static com.janeirodigital.sai.core.helpers.RdfUtils.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

/**
 * Readable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#access-grant">Access Grant</a>
 */
@Getter
public class ReadableAccessGrant extends ReadableResource {

    private final URL grantedBy;
    private final OffsetDateTime grantedAt;
    private final URL grantee;
    private final URL accessNeedGroup;
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
     * Get a {@link ReadableAccessGrant} at the provided <code>url</code>
     * @param url URL of the {@link ReadableAccessGrant} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link ReadableAccessGrant}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ReadableAccessGrant get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        ReadableAccessGrant.Builder builder = new ReadableAccessGrant.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link ReadableAccessGrant} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link ReadableAccessGrant}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public static ReadableAccessGrant get(URL url, SaiSession saiSession) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link ReadableAccessGrant} using the attributes of the current instance
     * @return Reloaded {@link ReadableAccessGrant}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public ReadableAccessGrant reload() throws SaiNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    /**
     * Lookup {@link ReadableDataGrant}s linked to the {@link ReadableAccessGrant} by data owner and
     * shape tree
     * @param dataOwnerUrl URL of the data owner that granted access
     * @param shapeTreeUrl URL of the shape tree associated with the data
     * @return List of matching {@link ReadableDataGrant}
     */
    public List<ReadableDataGrant> findDataGrants(URL dataOwnerUrl, URL shapeTreeUrl) {
        Objects.requireNonNull(dataOwnerUrl, "Must provide the URL of the data owner to find data grant");
        Objects.requireNonNull(shapeTreeUrl, "Must provide the URL of the shape tree to find data grant");
        List<ReadableDataGrant> dataGrants = new ArrayList<>();
        for (ReadableDataGrant dataGrant : this.getDataGrants()) {
            if (!dataGrant.getDataOwner().equals(dataOwnerUrl)) continue;
            if (!dataGrant.getRegisteredShapeTree().equals(shapeTreeUrl)) continue;
            dataGrants.add(dataGrant);
        }
        return dataGrants;
    }

    /**
     * Lookup {@link ReadableDataGrant}s linked to the {@link ReadableAccessGrant} by shape tree
     * @param shapeTreeUrl URL of the shape tree associated with the data
     * @return List of matching {@link ReadableDataGrant}
     */
    public List<ReadableDataGrant> findDataGrants(URL shapeTreeUrl) {
        Objects.requireNonNull(shapeTreeUrl, "Must provide the URL of the shape tree to find data grant");
        List<ReadableDataGrant> dataGrants = new ArrayList<>();
        for (ReadableDataGrant dataGrant : this.getDataGrants()) {
            if (dataGrant.getRegisteredShapeTree().equals(shapeTreeUrl)) dataGrants.add(dataGrant);
        }
        return dataGrants;
    }

    /**
     * Lookup the data owners represented by the {@link ReadableDataGrant}s linked to the {@link ReadableAccessGrant}
     * @return List of data owner identifiers
     */
    public List<URL> getDataOwners() {
        List<URL> dataOwners = new ArrayList<>();
        for (ReadableDataGrant dataGrant : this.getDataGrants()) {
            if (!dataOwners.contains(dataGrant.getDataOwner())) dataOwners.add(dataGrant.getDataOwner());
        }
        return dataOwners;
    }

    /**
     * Builder for {@link ReadableAccessGrant} instances.
     */
    public static class Builder extends ReadableResource.Builder<Builder> {

        private URL grantedBy;
        private OffsetDateTime grantedAt;
        private URL grantee;
        private URL accessNeedGroup;
        private final List<ReadableDataGrant> dataGrants;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link ReadableAccessGrant} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URL url, SaiSession saiSession) {
            super(url, saiSession);
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
                            if (inheritedChildGrant.getInheritsFrom().equals(dataGrant.getUrl())) {
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
                this.grantedBy = getRequiredUrlObject(this.resource, GRANTED_BY);
                this.grantedAt = getRequiredDateTimeObject(this.resource, GRANTED_AT);
                this.grantee = getRequiredUrlObject(this.resource, GRANTEE);
                this.accessNeedGroup = getRequiredUrlObject(this.resource, HAS_ACCESS_NEED_GROUP);
                List<URL> dataGrantUrls = getRequiredUrlObjects(this.resource, HAS_DATA_GRANT);
                for (URL url : dataGrantUrls) { this.dataGrants.add(ReadableDataGrant.get(url, this.saiSession)); }
                organizeInheritance();
            } catch (SaiNotFoundException | SaiException ex) {
                throw new SaiException("Unable to populate immutable access grant resource: " + ex.getMessage());
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

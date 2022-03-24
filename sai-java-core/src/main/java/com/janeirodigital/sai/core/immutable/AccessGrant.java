package com.janeirodigital.sai.core.immutable;

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
 * Immutable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#access-grant">Access Grant</a>
 */
@Getter
public class AccessGrant extends ImmutableResource {

    private final URL grantedBy;
    private final OffsetDateTime grantedAt;
    private final URL grantee;
    private final URL accessNeedGroup;
    private final List<DataGrant> dataGrants;

    /**
     * Construct an {@link AccessGrant} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private AccessGrant(Builder builder) throws SaiException {
        super(builder);
        this.grantedBy = builder.grantedBy;
        this.grantedAt = builder.grantedAt;
        this.grantee = builder.grantee;
        this.accessNeedGroup = builder.accessNeedGroup;
        this.dataGrants = builder.dataGrants;
    }

    /**
     * Get an {@link AccessGrant} at the provided <code>url</code>
     * @param url URL of the {@link AccessGrant} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link AccessGrant}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static AccessGrant get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        AccessGrant.Builder builder = new AccessGrant.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link AccessGrant} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link AccessGrant}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public static AccessGrant get(URL url, SaiSession saiSession) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link AccessGrant} using the attributes of the current instance
     * @return Reloaded {@link AccessGrant}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public AccessGrant reload() throws SaiNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    /**
     * Builder for {@link AccessGrant} instances.
     */
    public static class Builder extends ImmutableResource.Builder<Builder> {

        private URL grantedBy;
        private OffsetDateTime grantedAt;
        private URL grantee;
        private URL accessNeedGroup;
        private List<DataGrant> dataGrants;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link AccessAuthorization} to build
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

        public Builder setGrantedBy(URL grantedBy) {
            Objects.requireNonNull(grantedBy, "Must provide a URL for the social agent that granted the access grant");
            this.grantedBy = grantedBy;
            return this;
        }

        public Builder setGrantedAt(OffsetDateTime grantedAt) {
            Objects.requireNonNull(grantedAt, "Must provide the time the access grant was granted at");
            this.grantedAt = grantedAt;
            return this;
        }

        public Builder setGrantee(URL grantee) {
            Objects.requireNonNull(grantee, "Must provide a URL for the grantee of the access grant");
            this.grantee = grantee;
            return this;
        }

        public Builder setAccessNeedGroup(URL accessNeedGroup) {
            Objects.requireNonNull(accessNeedGroup, "Must provide a URL for the access need group of the access grant");
            this.accessNeedGroup = accessNeedGroup;
            return this;
        }

        public Builder setDataGrants(List<DataGrant> dataGrants) {
            Objects.requireNonNull(dataGrants, "Must provide a list of data grants for the access grant");
            this.dataGrants = dataGrants;
            return this;
        }

        /**
         * Populates "parent" data grants with the "child" data grants that inherit from them
         */
        private void organizeInheritance() {
            for (DataGrant dataGrant : this.dataGrants) {
                if (!dataGrant.getScopeOfGrant().equals(SCOPE_INHERITED)) {
                    for (DataGrant childGrant : this.dataGrants) {
                        if (childGrant.getScopeOfGrant().equals(SCOPE_INHERITED) && childGrant.getInheritsFrom().equals(dataGrant.getUrl())) {
                            dataGrant.getInheritingGrants().add(childGrant);
                        }
                    }
                }
            }
        }
        
        /**
         * Populates the fields of the {@link Builder} based on the associated Jena resource.
         * Also retrieves and populates the associated {@link DataAuthorization}s.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException {
            try {
                this.grantedBy = getRequiredUrlObject(this.resource, GRANTED_BY);
                this.grantedAt = getRequiredDateTimeObject(this.resource, GRANTED_AT);
                this.grantee = getRequiredUrlObject(this.resource, GRANTEE);
                this.accessNeedGroup = getRequiredUrlObject(this.resource, HAS_ACCESS_NEED_GROUP);
                List<URL> dataGrantUrls = getRequiredUrlObjects(this.resource, HAS_DATA_GRANT);
                for (URL url : dataGrantUrls) { this.dataGrants.add(DataGrant.get(url, this.saiSession)); }
                organizeInheritance();
            } catch (SaiNotFoundException | SaiException ex) {
                throw new SaiException("Unable to populate immutable access grant resource: " + ex.getMessage());
            }
        }

        private void populateDataset() {
            this.resource = getNewResourceForType(this.url, ACCESS_GRANT);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, GRANTED_BY, this.grantedBy);
            if (this.grantedAt == null) { this.grantedAt = OffsetDateTime.now(); }
            updateObject(this.resource, GRANTED_AT, this.grantedAt);
            updateObject(this.resource, GRANTEE, this.grantee);
            updateObject(this.resource, HAS_ACCESS_NEED_GROUP, this.accessNeedGroup);
            List<URL> dataGrantUrls = new ArrayList<>();
            for (DataGrant dataGrant : this.dataGrants) { dataGrantUrls.add(dataGrant.getUrl()); }
            updateUrlObjects(this.resource, HAS_DATA_GRANT, dataGrantUrls);
        }
        
        public AccessGrant build() throws SaiException {
            Objects.requireNonNull(this.grantedBy, "Must provide a URL for the social agent that granted the access grant");
            Objects.requireNonNull(this.grantee, "Must provide a URL for the grantee of the access grant");
            Objects.requireNonNull(this.accessNeedGroup, "Must provide a URL for the access need group of the access grant");
            Objects.requireNonNull(this.dataGrants, "Must provide a list of data authorizations for the access grant");
            if (this.dataset == null) { populateDataset(); }
            return new AccessGrant(this);
        }
    }

}

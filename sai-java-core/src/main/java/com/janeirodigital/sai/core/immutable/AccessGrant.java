package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import okhttp3.Headers;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
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
     * Construct a new {@link AccessGrant}
     * @param url URL of the {@link AccessGrant}
     * @param saiSession {@link SaiSession} to assign
     * @throws SaiException
     */
    private AccessGrant(URL url, SaiSession saiSession, Model dataset, Resource resource, ContentType contentType, URL grantedBy,
                        OffsetDateTime grantedAt, URL grantee, URL accessNeedGroup, List<DataGrant> dataGrants) throws SaiException {
        super(url, saiSession, false);
        this.dataset = dataset;
        this.resource = resource;
        this.contentType = contentType;
        this.grantedBy = grantedBy;
        this.grantedAt = grantedAt;
        this.grantee = grantee;
        this.accessNeedGroup = accessNeedGroup;
        this.dataGrants = dataGrants;
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
        Objects.requireNonNull(url, "Must provide the URL of the access grants to get");
        Objects.requireNonNull(saiSession, "Must provide a sai session to assign to the access grants");
        Objects.requireNonNull(contentType, "Must provide a content type for the access grant");
        Builder builder = new Builder(url, saiSession, contentType);
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(saiSession.getAuthorizedSession(), saiSession.getHttpClient(), url, headers))) {
            builder.setDataset(getRdfModelFromResponse(response));
        }
        return builder.build();
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
     * Builder for {@link AccessGrant} instances.
     */
    public static class Builder {
        private final URL url;
        private final SaiSession saiSession;
        private final ContentType contentType;
        private Model dataset;
        private Resource resource;
        private URL grantedBy;
        private OffsetDateTime grantedAt;
        private URL grantee;
        private URL accessNeedGroup;
        private List<DataGrant> dataGrants;

        /**
         * Initialize builder with <code>url</code>, <code>saiSession</code>, and desired <code>contentType</code>
         * @param url URL of the {@link AccessGrant} to build
         * @param saiSession {@link SaiSession} to assign
         * @param contentType {@link ContentType} to assign
         */
        public Builder(URL url, SaiSession saiSession, ContentType contentType) {
            Objects.requireNonNull(url, "Must provide a URL for the access grant builder");
            Objects.requireNonNull(saiSession, "Must provide a sai session for the access grant builder");
            Objects.requireNonNull(contentType, "Must provide a content type for the access grant builder");
            this.url = url;
            this.saiSession = saiSession;
            this.contentType = contentType;
            this.dataGrants = new ArrayList<>();
        }

        /**
         * Optional Jena Model that will initialize the attributes of the Builder rather than set
         * them manually. Typically used in read scenarios when populating the Builder from
         * the contents of a remote resource.
         * @param dataset Jena model to populate the Builder attributes with
         * @return {@link Builder}
         */
        public Builder setDataset(Model dataset) throws SaiException {
            Objects.requireNonNull(dataset, "Must provide a Jena model for the access grant builder");
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            populateFromDataset();
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
         * Also retrieves and populates the associated {@link DataConsent}s.
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
            updateObject(this.resource, GRANTED_AT, this.grantedAt);
            updateObject(this.resource, GRANTEE, this.grantee);
            updateObject(this.resource, HAS_ACCESS_NEED_GROUP, this.accessNeedGroup);
            List<URL> dataGrantUrls = new ArrayList<>();
            for (DataGrant dataGrant : this.dataGrants) { dataGrantUrls.add(dataGrant.getUrl()); }
            updateUrlObjects(this.resource, HAS_DATA_GRANT, dataGrantUrls);
        }
        
        public AccessGrant build() throws SaiException {
            Objects.requireNonNull(this.grantedBy, "Must provide a URL for the social agent that granted the access grant");
            Objects.requireNonNull(this.grantedAt, "Must provide the time the access grant was granted at");
            Objects.requireNonNull(this.grantee, "Must provide a URL for the grantee of the access grant");
            Objects.requireNonNull(this.accessNeedGroup, "Must provide a URL for the access need group of the access grant");
            Objects.requireNonNull(this.dataGrants, "Must provide a list of data consents for the access grant");
            if (this.dataset == null) { populateDataset(); }
            return new AccessGrant(this.url, this.saiSession, this.dataset, this.resource, this.contentType, this.grantedBy,
                                   this.grantedAt, this.grantee, this.accessNeedGroup, this.dataGrants);
        }
    }

}

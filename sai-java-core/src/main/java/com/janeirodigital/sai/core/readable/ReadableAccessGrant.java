package com.janeirodigital.sai.core.readable;

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
     * Construct a {@link ReadableAccessGrant} instance from the provided <code>url</code>.
     * @param url URL to generate the {@link ReadableAccessGrant} from
     * @param saiSession {@link SaiSession} to assign
     * @throws SaiException
     */
    private ReadableAccessGrant(URL url, SaiSession saiSession, Model dataset, Resource resource, ContentType contentType, URL grantedBy,
                                OffsetDateTime grantedAt, URL grantee, URL accessNeedGroup, List<ReadableDataGrant> dataGrants) throws SaiException {
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
     * Get an {@link ReadableAccessGrant} at the provided <code>url</code>
     * @param url URL of the {@link ReadableAccessGrant} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link ReadableAccessGrant}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ReadableAccessGrant get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(url, "Must provide the URL of the access grants to get");
        Objects.requireNonNull(saiSession, "Must provide a sai session to assign to the access grants");
        Objects.requireNonNull(contentType, "Must provide a content type for the access grant");
        ReadableAccessGrant.Builder builder = new ReadableAccessGrant.Builder(url, saiSession, contentType);
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(saiSession.getAuthorizedSession(), saiSession.getHttpClient(), url, headers))) {
            builder.setDataset(getRdfModelFromResponse(response));
        }
        return builder.build();
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
     * Builder for {@link ReadableAccessGrant} instances.
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
        private List<ReadableDataGrant> dataGrants;

        /**
         * Initialize builder with <code>url</code>, <code>saiSession</code>, and desired <code>contentType</code>
         *
         * @param url URL of the {@link ReadableAccessGrant} to build
         * @param saiSession {@link SaiSession} to assign
         * @param contentType {@link ContentType} to assign
         */
        public Builder(URL url, SaiSession saiSession, ContentType contentType) {
            Objects.requireNonNull(url, "Must provide a URL for the readable access grant builder");
            Objects.requireNonNull(saiSession, "Must provide a sai session for the readable access grant builder");
            Objects.requireNonNull(contentType, "Must provide a content type for the readable access grant builder");
            this.url = url;
            this.saiSession = saiSession;
            this.contentType = contentType;
            this.dataGrants = new ArrayList<>();
        }

        /**
         * Optional Jena Model that will initialize the attributes of the Builder rather than set
         * them manually. Typically used in read scenarios when populating the Builder from
         * the contents of a remote resource.
         *
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
            return new ReadableAccessGrant(this.url, this.saiSession, this.dataset, this.resource, this.contentType,
                                           this.grantedBy, this.grantedAt, this.grantee, this.accessNeedGroup, this.dataGrants);
        }
    }

}

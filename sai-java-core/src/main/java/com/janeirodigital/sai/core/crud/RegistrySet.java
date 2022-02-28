package com.janeirodigital.sai.core.crud;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

/**
 * Modifiable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#datamodel-registry-set">Registry Set</a>
 */
@Getter
public class RegistrySet extends CRUDResource {

    private final URL agentRegistryUrl;
    private final URL accessConsentRegistryUrl;
    private final List<URL> dataRegistryUrls;

    /**
     * Construct a new {@link RegistrySet}. Should only be called from {@link Builder}
     */
    private RegistrySet(URL url, SaiSession saiSession, Model dataset, Resource resource, ContentType contentType,
                        URL agentRegistryUrl, URL accessConsentRegistryUrl, List<URL> dataRegistryUrls) throws SaiException {
        super(url, saiSession, false);
        this.dataset = dataset;
        this.resource = resource;
        this.contentType = contentType;
        this.agentRegistryUrl = agentRegistryUrl;
        this.accessConsentRegistryUrl = accessConsentRegistryUrl;
        this.dataRegistryUrls = dataRegistryUrls;
    }

    /**
     * Get a {@link RegistrySet} at the provided <code>url</code>
     * @param url URL of the {@link RegistrySet} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link RegistrySet}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static RegistrySet get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(url, "Must provide the URL of the registry set to get");
        Objects.requireNonNull(saiSession, "Must provide a sai session to assign to the registry set");
        Objects.requireNonNull(contentType, "Must provide a content type for the registry set");
        RegistrySet.Builder builder = new RegistrySet.Builder(url, saiSession, contentType);
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(saiSession.getAuthorizedSession(), saiSession.getHttpClient(), url, headers))) {
            builder.setDataset(getRdfModelFromResponse(response));
        }
        return builder.build();
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link RegistrySet}
     * @param saiSession {@link SaiSession} to assign
     * @return
     */
    public static RegistrySet get(URL url, SaiSession saiSession) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Builder for {@link RegistrySet} instances.
     */
    public static class Builder {
        
        private final URL url;
        private final SaiSession saiSession;
        private final ContentType contentType;
        private Model dataset;
        private Resource resource;
        private URL agentRegistryUrl;
        private URL accessConsentRegistryUrl;
        private List<URL> dataRegistryUrls;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link RegistrySet} to build
         * @param saiSession {@link SaiSession} to assign
         * @param contentType {@link ContentType} to assign
         */
        public Builder(URL url, SaiSession saiSession, ContentType contentType) {
            Objects.requireNonNull(url, "Must provide a URL for the registry set builder");
            Objects.requireNonNull(saiSession, "Must provide a sai session for the registry set builder");
            Objects.requireNonNull(contentType, "Must provide a content type for the registry set builder");
            this.url = url;
            this.saiSession = saiSession;
            this.contentType = contentType;
            this.dataRegistryUrls = new ArrayList<>();
        }

        /**
         * Optional Jena Model that will initialize the attributes of the Builder rather than set
         * them manually. Typically used in read scenarios when populating the Builder from
         * the contents of a remote resource.
         * @param dataset Jena model to populate the Builder attributes with
         * @return {@link Builder}
         */
        public Builder setDataset(Model dataset) throws SaiException {
            Objects.requireNonNull(dataset, "Must provide a Jena model for the registry set builder");
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            populateFromDataset();
            return this;
        }

        /**
         * Set the URL of the {@link AgentRegistry}
         * @param agentRegistryUrl URL of the {@link AgentRegistry} to set
         * @return {@link Builder}
         */
        public Builder setAgentRegistry(URL agentRegistryUrl) {
            Objects.requireNonNull(agentRegistryUrl, "Must provide the URL of an agent registry to the registry set builder");
            this.agentRegistryUrl = agentRegistryUrl;
            return this;
        }

        /**
         * Set the URL of the {@link AccessConsentRegistry}
         * @param accessConsentRegistryUrl URL of the {@link AccessConsentRegistry} to set
         * @return {@link Builder}
         */
        public Builder setAccessConsentRegistry(URL accessConsentRegistryUrl) {
            Objects.requireNonNull(accessConsentRegistryUrl, "Must provide the URL of an access consent registry to the registry set builder");
            this.accessConsentRegistryUrl = accessConsentRegistryUrl;
            return this;
        }

        /**
         * Set the URLs of associated Data Registries
         * @param dataRegistryUrls List of Data Registry URLs
         * @return {@link Builder}
         */
        public Builder setDataRegistries(List<URL> dataRegistryUrls) {
            Objects.requireNonNull(dataRegistryUrls, "Must provide the URLs of associated data registries to the registry set builder");
            this.dataRegistryUrls = dataRegistryUrls;
            return this;
        }

        /**
         * Populates the fields of the {@link RegistrySet} based on the associated Jena resource.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException {
            try {
                this.agentRegistryUrl = getRequiredUrlObject(this.resource, HAS_AGENT_REGISTRY);
                this.accessConsentRegistryUrl = getRequiredUrlObject(this.resource, HAS_ACCESS_CONSENT_REGISTRY);
                this.dataRegistryUrls = getRequiredUrlObjects(this.resource, HAS_DATA_REGISTRY);
            } catch (SaiNotFoundException ex) {
                throw new SaiException("Failed to load registry set " + this.url + ": " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         * @throws SaiException
         */
        private void populateDataset() throws SaiException {
            this.resource = getNewResourceForType(this.url, REGISTRY_SET);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, HAS_AGENT_REGISTRY, agentRegistryUrl);
            updateObject(this.resource, HAS_ACCESS_CONSENT_REGISTRY, accessConsentRegistryUrl);
            updateUrlObjects(this.resource, HAS_DATA_REGISTRY, this.dataRegistryUrls);
        }

        /**
         * Build the {@link RegistrySet} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}. Conversely, if a dataset was provided, the attributes of the
         * Builder will be populated from it.
         * @return {@link RegistrySet}
         * @throws SaiException
         */
        public RegistrySet build() throws SaiException {
            Objects.requireNonNull(this.agentRegistryUrl, "Must provide the URL of an agent registry to the registry set builder");
            Objects.requireNonNull(this.accessConsentRegistryUrl, "Must provide the URL of an access consent registry to the registry set builder");
            Objects.requireNonNull(this.dataRegistryUrls, "Must provide the URLs of associated data registries to the registry set builder");
            if (this.dataset == null) { populateDataset(); }
            return new RegistrySet(this.url, this.saiSession, this.dataset, this.resource, this.contentType,
                                   this.agentRegistryUrl, this.accessConsentRegistryUrl, this.dataRegistryUrls);
        }
    }

}

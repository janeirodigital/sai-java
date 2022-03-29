package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.SaiHttpException;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.sai.rdfutils.SaiRdfNotFoundException;
import lombok.Getter;
import lombok.Setter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.httputils.HttpUtils.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.httputils.HttpUtils.getRdfModelFromResponse;
import static com.janeirodigital.sai.rdfutils.RdfUtils.*;

/**
 * Modifiable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#datamodel-registry-set">Registry Set</a>
 */
@Getter @Setter
public class RegistrySet extends CRUDResource {

    private URL agentRegistryUrl;
    private URL authorizationRegistryUrl;
    private List<URL> dataRegistryUrls;

    /**
     * Construct a {@link RegistrySet} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private RegistrySet(Builder builder) throws SaiException {
        super(builder);
        this.agentRegistryUrl = builder.agentRegistryUrl;
        this.authorizationRegistryUrl = builder.authorizationRegistryUrl;
        this.dataRegistryUrls = builder.dataRegistryUrls;
    }

    /**
     * Get a {@link RegistrySet} at the provided <code>url</code>
     * @param url URL of the {@link RegistrySet} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link RegistrySet}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static RegistrySet get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiHttpNotFoundException {
        RegistrySet.Builder builder = new RegistrySet.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).setContentType(contentType).build();
        } catch (SaiRdfException | SaiHttpException ex) {
            throw new SaiException("Unable to read registry set " + url, ex);
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link RegistrySet}
     * @param saiSession {@link SaiSession} to assign
     * @return
     */
    public static RegistrySet get(URL url, SaiSession saiSession) throws SaiHttpNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link RegistrySet} using the attributes of the current instance
     * @return Reloaded {@link RegistrySet}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public RegistrySet reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    /**
     * Builder for {@link RegistrySet} instances.
     */
    public static class Builder extends CRUDResource.Builder<Builder> {
        
        private URL agentRegistryUrl;
        private URL authorizationRegistryUrl;
        private List<URL> dataRegistryUrls;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link RegistrySet} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URL url, SaiSession saiSession) {
            super(url, saiSession);
            this.dataRegistryUrls = new ArrayList<>();
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
         * Set the URL of the {@link AuthorizationRegistry}
         * @param authorizationRegistryUrl URL of the {@link AuthorizationRegistry} to set
         * @return {@link Builder}
         */
        public Builder setAuthorizationRegistry(URL authorizationRegistryUrl) {
            Objects.requireNonNull(authorizationRegistryUrl, "Must provide the URL of an authorization registry to the registry set builder");
            this.authorizationRegistryUrl = authorizationRegistryUrl;
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
                this.authorizationRegistryUrl = getRequiredUrlObject(this.resource, HAS_AUTHORIZATION_REGISTRY);
                this.dataRegistryUrls = getRequiredUrlObjects(this.resource, HAS_DATA_REGISTRY);
            } catch (SaiRdfException | SaiRdfNotFoundException ex) {
                throw new SaiException("Failed to load registry set " + this.url, ex);
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        private void populateDataset() {
            this.resource = getNewResourceForType(this.url, REGISTRY_SET);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, HAS_AGENT_REGISTRY, agentRegistryUrl);
            updateObject(this.resource, HAS_AUTHORIZATION_REGISTRY, authorizationRegistryUrl);
            updateUrlObjects(this.resource, HAS_DATA_REGISTRY, this.dataRegistryUrls);
        }

        /**
         * Build the {@link RegistrySet} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}.
         * @return {@link RegistrySet}
         * @throws SaiException
         */
        public RegistrySet build() throws SaiException {
            Objects.requireNonNull(this.agentRegistryUrl, "Must provide the URL of an agent registry to the registry set builder");
            Objects.requireNonNull(this.authorizationRegistryUrl, "Must provide the URL of an authorization registry to the registry set builder");
            Objects.requireNonNull(this.dataRegistryUrls, "Must provide the URLs of associated data registries to the registry set builder");
            if (this.dataset == null) { populateDataset(); }
            return new RegistrySet(this);
        }
    }

}

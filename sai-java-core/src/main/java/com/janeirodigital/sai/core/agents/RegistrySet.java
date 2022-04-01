package com.janeirodigital.sai.core.agents;

import com.janeirodigital.sai.core.authorizations.AuthorizationRegistry;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.resources.CRUDResource;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.sai.rdfutils.SaiRdfNotFoundException;
import lombok.Getter;
import lombok.Setter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.httputils.HttpUtils.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.rdfutils.RdfUtils.*;

/**
 * Modifiable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#datamodel-registry-set">Registry Set</a>
 */
@Getter @Setter
public class RegistrySet extends CRUDResource {

    private URI agentRegistryUri;
    private URI authorizationRegistryUri;
    private List<URI> dataRegistryUris;

    /**
     * Construct a {@link RegistrySet} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private RegistrySet(Builder builder) throws SaiException {
        super(builder);
        this.agentRegistryUri = builder.agentRegistryUri;
        this.authorizationRegistryUri = builder.authorizationRegistryUri;
        this.dataRegistryUris = builder.dataRegistryUris;
    }

    /**
     * Get a {@link RegistrySet} at the provided <code>uri</code>
     * @param uri URI of the {@link RegistrySet} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link RegistrySet}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static RegistrySet get(URI uri, SaiSession saiSession, ContentType contentType) throws SaiException, SaiHttpNotFoundException {
        RegistrySet.Builder builder = new RegistrySet.Builder(uri, saiSession);
        try (Response response = read(uri, saiSession, contentType, false)) {
            return builder.setDataset(response).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URI, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param uri URI of the {@link RegistrySet}
     * @param saiSession {@link SaiSession} to assign
     * @return
     */
    public static RegistrySet get(URI uri, SaiSession saiSession) throws SaiHttpNotFoundException, SaiException {
        return get(uri, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link RegistrySet} using the attributes of the current instance
     * @return Reloaded {@link RegistrySet}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public RegistrySet reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.uri, this.saiSession, this.contentType);
    }

    /**
     * Builder for {@link RegistrySet} instances.
     */
    public static class Builder extends CRUDResource.Builder<Builder> {
        
        private URI agentRegistryUri;
        private URI authorizationRegistryUri;
        private List<URI> dataRegistryUris;

        /**
         * Initialize builder with <code>uri</code> and <code>saiSession</code>
         * @param uri URI of the {@link RegistrySet} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URI uri, SaiSession saiSession) {
            super(uri, saiSession);
            this.dataRegistryUris = new ArrayList<>();
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
         * Set the URI of the {@link AgentRegistry}
         * @param agentRegistryUri URI of the {@link AgentRegistry} to set
         * @return {@link Builder}
         */
        public Builder setAgentRegistry(URI agentRegistryUri) {
            Objects.requireNonNull(agentRegistryUri, "Must provide the URI of an agent registry to the registry set builder");
            this.agentRegistryUri = agentRegistryUri;
            return this;
        }

        /**
         * Set the URI of the {@link AuthorizationRegistry}
         * @param authorizationRegistryUri URI of the {@link AuthorizationRegistry} to set
         * @return {@link Builder}
         */
        public Builder setAuthorizationRegistry(URI authorizationRegistryUri) {
            Objects.requireNonNull(authorizationRegistryUri, "Must provide the URI of an authorization registry to the registry set builder");
            this.authorizationRegistryUri = authorizationRegistryUri;
            return this;
        }

        /**
         * Set the URIs of associated Data Registries
         * @param dataRegistryUris List of Data Registry URIs
         * @return {@link Builder}
         */
        public Builder setDataRegistries(List<URI> dataRegistryUris) {
            Objects.requireNonNull(dataRegistryUris, "Must provide the URIs of associated data registries to the registry set builder");
            this.dataRegistryUris = dataRegistryUris;
            return this;
        }

        /**
         * Populates the fields of the {@link RegistrySet} based on the associated Jena resource.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException {
            try {
                this.agentRegistryUri = getRequiredUriObject(this.resource, HAS_AGENT_REGISTRY);
                this.authorizationRegistryUri = getRequiredUriObject(this.resource, HAS_AUTHORIZATION_REGISTRY);
                this.dataRegistryUris = getRequiredUriObjects(this.resource, HAS_DATA_REGISTRY);
            } catch (SaiRdfException | SaiRdfNotFoundException ex) {
                throw new SaiException("Failed to load registry set " + this.uri, ex);
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        private void populateDataset() {
            this.resource = getNewResourceForType(this.uri, REGISTRY_SET);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, HAS_AGENT_REGISTRY, agentRegistryUri);
            updateObject(this.resource, HAS_AUTHORIZATION_REGISTRY, authorizationRegistryUri);
            updateUriObjects(this.resource, HAS_DATA_REGISTRY, this.dataRegistryUris);
        }

        /**
         * Build the {@link RegistrySet} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}.
         * @return {@link RegistrySet}
         * @throws SaiException
         */
        public RegistrySet build() throws SaiException {
            Objects.requireNonNull(this.agentRegistryUri, "Must provide the URI of an agent registry to the registry set builder");
            Objects.requireNonNull(this.authorizationRegistryUri, "Must provide the URI of an authorization registry to the registry set builder");
            Objects.requireNonNull(this.dataRegistryUris, "Must provide the URIs of associated data registries to the registry set builder");
            if (this.dataset == null) { populateDataset(); }
            return new RegistrySet(this);
        }
    }

}

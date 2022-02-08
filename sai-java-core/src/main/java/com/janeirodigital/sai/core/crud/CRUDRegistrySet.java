package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.Getter;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.contexts.InteropContexts.REGISTRY_SET_CONTEXT;
import static com.janeirodigital.sai.core.enums.ContentType.TEXT_TURTLE;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

/**
 * Modifiable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#datamodel-registry-set">Registry Set</a>
 */
@Getter
public class CRUDRegistrySet extends CRUDResource {

    private URL agentRegistryUrl;
    private URL accessConsentRegistryUrl;
    private List<URL> dataRegistryUrls;

    /**
     * Construct a new {@link CRUDRegistrySet}
     * @param url URL of the {@link CRUDRegistrySet}
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    public CRUDRegistrySet(URL url, DataFactory dataFactory) throws SaiException {
        super(url, dataFactory, false);
        this.dataRegistryUrls = new ArrayList<>();
        this.jsonLdContext = buildRemoteJsonLdContext(REGISTRY_SET_CONTEXT);
    }

    /**
     * Builder used by a {@link DataFactory} to construct and initialize a {@link CRUDRegistrySet}.
     * If a Jena <code>resource</code> is provided and there is already a {@link CRUDRegistrySet}
     * at the provided <code>url</code>, the graph of the provided resource will be used. The remote graph
     * will not be updated until {@link #update()} is called.
     * @param url URL of the {@link CRUDRegistrySet} to build
     * @param dataFactory {@link DataFactory} to assign
     * @param contentType {@link ContentType} to use for read / write
     * @param resource Optional Jena Resource to populate the resource graph
     * @return {@link CRUDRegistrySet}
     * @throws SaiException
     */
    public static CRUDRegistrySet build(URL url, DataFactory dataFactory, ContentType contentType, Resource resource) throws SaiException {
        CRUDRegistrySet registrySet = new CRUDRegistrySet(url, dataFactory);
        registrySet.contentType = contentType;
        if (resource != null) {
            registrySet.resource = resource;
            registrySet.dataset = resource.getModel();
        }
        registrySet.bootstrap();
        return registrySet;
    }

    /**
     * Calls {@link #build(URL, DataFactory, ContentType, Resource)} to construct a {@link CRUDRegistrySet} with
     * no Jena resource provided and the specified content type (e.g. JSON-LD).
     * @param url URL of the {@link CRUDRegistrySet} to build
     * @param dataFactory {@link DataFactory} to assign
     * @return {@link CRUDRegistrySet}
     * @throws SaiException
     */
    public static CRUDRegistrySet build(URL url, DataFactory dataFactory, ContentType contentType) throws SaiException {
        return build(url, dataFactory, contentType, null);
    }

    /**
     * Calls {@link #build(URL, DataFactory, ContentType, Resource)} to construct a {@link CRUDRegistrySet} with
     * no Jena resource provided and the default content type.
     * @param url URL of the {@link CRUDRegistrySet} to build
     * @param dataFactory {@link DataFactory} to assign
     * @return {@link CRUDRegistrySet}
     * @throws SaiException
     */
    public static CRUDRegistrySet build(URL url, DataFactory dataFactory) throws SaiException {
        return build(url, dataFactory, TEXT_TURTLE, null);
    }

    /**
     * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#datamodel-registry-set">Agent Registry</a>
     * for the Registry Set of the Social Agent.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar-registry">Agent Registry</a>
     * @param agentRegistryUrl URL of the agent registry for the social agent
     */
    public void setAgentRegistry(URL agentRegistryUrl) {
        Objects.requireNonNull(agentRegistryUrl, "Must provide an agent registry for the registry set");
        this.agentRegistryUrl = agentRegistryUrl;
        updateObject(this.resource, HAS_AGENT_REGISTRY, agentRegistryUrl);
    }

    /**
     * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#datamodel-registry-set">Access Consent Registry</a>
     * for the Registry Set of the Social Agent.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#datamodel-registry-set">Registry Set</a>
     * @param accessConsentRegistryUrl URL of the access consent registry for the social agent
     */
    public void setAccessConsentRegistry(URL accessConsentRegistryUrl) {
        Objects.requireNonNull(accessConsentRegistryUrl, "Must provide an access consent registry for the registry set");
        this.accessConsentRegistryUrl = accessConsentRegistryUrl;
        updateObject(this.resource, HAS_ACCESS_CONSENT_REGISTRY, accessConsentRegistryUrl);
    }

    /**
     * Add a <a href="https://solid.github.io/data-interoperability-panel/specification/#datamodel-registry-set">Data Registry</a>
     * to the Registry Set of the Social Agent.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#data-registry">Data Registry</a>
     * @param dataRegistryUrl URL a data registry to add to the registry set
     */
    public void addDataRegistry(URL dataRegistryUrl) {
        Objects.requireNonNull(dataRegistryUrl, "Must provide a data registry to add to the registry set");
        this.dataRegistryUrls.add(dataRegistryUrl);
        updateUrlObjects(this.resource, HAS_DATA_REGISTRY, this.dataRegistryUrls);
    }

    /**
     * Bootstraps the {@link CRUDRegistrySet}. If a Jena Resource was provided, it will
     * be used to populate the instance. If not, the remote resource will be fetched and
     * populated. If the remote resource doesn't exist, a local graph will be created for it.
     * @throws SaiException
     */
    private void bootstrap() throws SaiException {
        if (this.resource != null) { populate(); } else {
            try {
                // Fetch the remote resource and populate
                this.fetchData();
                populate();
            } catch (SaiNotFoundException ex) {
                // Remote resource didn't exist, initialize one
                this.resource = getNewResourceForType(this.url, REGISTRY_SET);
                this.dataset = resource.getModel();
            }
        }
    }

    /**
     * Populates the {@link CRUDRegistrySet} instance with required and optional fields
     * @throws SaiException If any required fields cannot be populated, or other exceptional conditions
     */
    private void populate() throws SaiException {
        try {
            this.agentRegistryUrl = getRequiredUrlObject(this.resource, HAS_AGENT_REGISTRY);
            this.accessConsentRegistryUrl = getRequiredUrlObject(this.resource, HAS_ACCESS_CONSENT_REGISTRY);
            this.dataRegistryUrls = getRequiredUrlObjects(this.resource, HAS_DATA_REGISTRY);
        } catch (SaiNotFoundException ex) {
            throw new SaiException("Failed to load registry set " + this.url + ": " + ex.getMessage());
        }
    }

}

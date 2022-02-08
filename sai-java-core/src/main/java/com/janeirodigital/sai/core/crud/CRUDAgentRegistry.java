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

import static com.janeirodigital.sai.core.contexts.InteropContexts.AGENT_REGISTRY_CONTEXT;
import static com.janeirodigital.sai.core.enums.ContentType.TEXT_TURTLE;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

/**
 * Modifiable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#ar-registry">Agent Registry</a>
 */
@Getter
public class CRUDAgentRegistry extends CRUDResource {

    List<URL> socialAgentRegistrations;
    List<URL> applicationRegistrations;

    /**
     * Construct a new {@link CRUDAgentRegistry}
     * @param url URL of the {@link CRUDAgentRegistry}
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    public CRUDAgentRegistry(URL url, DataFactory dataFactory) throws SaiException {
        super(url, dataFactory, false);
        this.socialAgentRegistrations = new ArrayList<>();
        this.applicationRegistrations = new ArrayList<>();
        this.jsonLdContext = buildRemoteJsonLdContext(AGENT_REGISTRY_CONTEXT);
    }

    /**
     * Builder used by a {@link DataFactory} to construct and initialize a {@link CRUDAgentRegistry}.
     * If a Jena <code>resource</code> is provided and there is already a {@link CRUDAgentRegistry}
     * at the provided <code>url</code>, the graph of the provided resource will be used. The remote graph
     * will not be updated until {@link #update()} is called.
     * @param url URL of the {@link CRUDAgentRegistry} to build
     * @param dataFactory {@link DataFactory} to assign
     * @param contentType {@link ContentType} to use for read / write
     * @param resource Optional Jena Resource to populate the resource graph
     * @return {@link CRUDAgentRegistry}
     * @throws SaiException
     */
    public static CRUDAgentRegistry build(URL url, DataFactory dataFactory, ContentType contentType, Resource resource) throws SaiException {
        CRUDAgentRegistry agentRegistry = new CRUDAgentRegistry(url, dataFactory);
        agentRegistry.contentType = contentType;
        if (resource != null) {
            agentRegistry.resource = resource;
            agentRegistry.dataset = resource.getModel();
        }
        agentRegistry.bootstrap();
        return agentRegistry;
    }

    /**
     * Calls {@link #build(URL, DataFactory, ContentType, Resource)} to construct a {@link CRUDAgentRegistry} with
     * no Jena resource provided and the specified content type (e.g. JSON-LD).
     * @param url URL of the {@link CRUDAgentRegistry} to build
     * @param dataFactory {@link DataFactory} to assign
     * @return {@link CRUDAgentRegistry}
     * @throws SaiException
     */
    public static CRUDAgentRegistry build(URL url, DataFactory dataFactory, ContentType contentType) throws SaiException {
        return build(url, dataFactory, contentType, null);
    }

    /**
     * Calls {@link #build(URL, DataFactory, ContentType, Resource)} to construct a {@link CRUDAgentRegistry} with
     * no Jena resource provided and the default content type.
     * @param url URL of the {@link CRUDAgentRegistry} to build
     * @param dataFactory {@link DataFactory} to assign
     * @return {@link CRUDAgentRegistry}
     * @throws SaiException
     */
    public static CRUDAgentRegistry build(URL url, DataFactory dataFactory) throws SaiException {
        return build(url, dataFactory, TEXT_TURTLE, null);
    }

    /**
     * Add a <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agent-registration">Social Agent Registration</a>
     * to the Agent Registry.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar-registry">Agent Registry</a>
     * @param registrationUrl URL of social agent registration to add to the agent registry
     */
    public void addSocialAgentRegistration(URL registrationUrl) {
        Objects.requireNonNull(registrationUrl, "Must provide a social agent registration to add to the agent registry");
        this.socialAgentRegistrations.add(registrationUrl);
        updateUrlObjects(this.resource, HAS_SOCIAL_AGENT_REGISTRATION, this.socialAgentRegistrations);
    }

    /**
     * Remove a <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agent-registration">Social Agent Registration</a>
     * from the Agent Registry.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar-registry">Agent Registry</a>
     * @param registrationUrl URL of social agent registration to remove from the agent registry
     */
    public void removeSocialAgentRegistration(URL registrationUrl) {
        Objects.requireNonNull(registrationUrl, "Must provide a social agent registration to remove from the agent registry");
        this.socialAgentRegistrations.remove(registrationUrl);
        updateUrlObjects(this.resource, HAS_SOCIAL_AGENT_REGISTRATION, this.socialAgentRegistrations);
    }

    /**
     * Add a <a href="https://solid.github.io/data-interoperability-panel/specification/#application-registration">Application Registration</a>
     * to the Agent Registry.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar-registry">Agent Registry</a>
     * @param registrationUrl URL of application registration to add to the agent registry
     */
    public void addApplicationRegistration(URL registrationUrl) {
        Objects.requireNonNull(registrationUrl, "Must provide an application registration to add to the agent registry");
        this.applicationRegistrations.add(registrationUrl);
        updateUrlObjects(this.resource, HAS_APPLICATION_REGISTRATION, this.applicationRegistrations);
    }

    /**
     * Remove a <a href="https://solid.github.io/data-interoperability-panel/specification/#application-registration">Application Registration</a>
     * from the Agent Registry.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar-registry">Agent Registry</a>
     * @param registrationUrl URL of application registration to remove from the agent registry
     */
    public void removeApplicationRegistration(URL registrationUrl) {
        Objects.requireNonNull(registrationUrl, "Must provide an application registration to remove from the agent registry");
        this.applicationRegistrations.remove(registrationUrl);
        updateUrlObjects(this.resource, HAS_APPLICATION_REGISTRATION, this.applicationRegistrations);
    }

    /**
     * Bootstraps the {@link CRUDAgentRegistry}. If a Jena Resource was provided, it will
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
                this.resource = getNewResourceForType(this.url, AGENT_REGISTRY);
                this.dataset = resource.getModel();
            }
        }
    }

    /**
     * Populates the {@link CRUDAgentRegistry} instance with required and optional fields
     * @throws SaiException If any required fields cannot be populated, or other exceptional conditions
     */
    private void populate() throws SaiException {
        this.socialAgentRegistrations = getUrlObjects(this.resource, HAS_SOCIAL_AGENT_REGISTRATION);
        this.applicationRegistrations = getUrlObjects(this.resource, HAS_APPLICATION_REGISTRATION);
    }
}

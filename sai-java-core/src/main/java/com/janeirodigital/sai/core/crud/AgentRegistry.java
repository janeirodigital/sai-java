package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

import static com.janeirodigital.sai.core.contexts.InteropContext.INTEROP_CONTEXT;
import static com.janeirodigital.sai.core.helpers.HttpHelper.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.core.helpers.RdfHelper.buildRemoteJsonLdContext;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getNewResourceForType;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

/**
 * Modifiable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#ar-registry">Agent Registry</a>
 */
@Getter
public class AgentRegistry extends CRUDResource {

    SocialAgentRegistrationList<SocialAgentRegistration> socialAgentRegistrations;
    ApplicationRegistrationList<ApplicationRegistration> applicationRegistrations;

    /**
     * Construct a new {@link AgentRegistry}
     * @param url URL of the {@link AgentRegistry}
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    public AgentRegistry(URL url, DataFactory dataFactory) throws SaiException {
        super(url, dataFactory, false);
        this.jsonLdContext = buildRemoteJsonLdContext(INTEROP_CONTEXT);
    }

    /**
     * Builder used by a {@link DataFactory} to construct and initialize a {@link AgentRegistry}.
     * If a Jena <code>resource</code> is provided and there is already a {@link AgentRegistry}
     * at the provided <code>url</code>, the graph of the provided resource will be used. The remote graph
     * will not be updated until {@link #update()} is called.
     * @param url URL of the {@link AgentRegistry} to build
     * @param dataFactory {@link DataFactory} to assign
     * @param contentType {@link ContentType} to use for read / write
     * @param resource Optional Jena Resource to populate the resource graph
     * @return {@link AgentRegistry}
     * @throws SaiException
     */
    public static AgentRegistry build(URL url, DataFactory dataFactory, ContentType contentType, Resource resource) throws SaiException {
        AgentRegistry agentRegistry = new AgentRegistry(url, dataFactory);
        agentRegistry.contentType = contentType;
        if (resource != null) {
            agentRegistry.resource = resource;
            agentRegistry.dataset = resource.getModel();
        }
        agentRegistry.bootstrap();
        return agentRegistry;
    }

    /**
     * Calls {@link #build(URL, DataFactory, ContentType, Resource)} to construct a {@link AgentRegistry} with
     * no Jena resource provided and the specified content type (e.g. JSON-LD).
     * @param url URL of the {@link AgentRegistry} to build
     * @param dataFactory {@link DataFactory} to assign
     * @return {@link AgentRegistry}
     * @throws SaiException
     */
    public static AgentRegistry build(URL url, DataFactory dataFactory, ContentType contentType) throws SaiException {
        return build(url, dataFactory, contentType, null);
    }

    /**
     * Calls {@link #build(URL, DataFactory, ContentType, Resource)} to construct a {@link AgentRegistry} with
     * no Jena resource provided and the default content type.
     * @param url URL of the {@link AgentRegistry} to build
     * @param dataFactory {@link DataFactory} to assign
     * @return {@link AgentRegistry}
     * @throws SaiException
     */
    public static AgentRegistry build(URL url, DataFactory dataFactory) throws SaiException {
        return build(url, dataFactory, DEFAULT_RDF_CONTENT_TYPE, null);
    }

    /**
     * Bootstraps the {@link AgentRegistry}. If a Jena Resource was provided, it will
     * be used to populate the instance. If not, the remote resource will be fetched and
     * populated. If the remote resource doesn't exist, a local graph will be created for it,
     * which includes initializing social agent and application registration lists.
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
                this.socialAgentRegistrations = new SocialAgentRegistrationList<>(this.getDataFactory(), this.getResource());
                this.applicationRegistrations = new ApplicationRegistrationList<>(this.getDataFactory(), this.getResource());
            }
        }
    }

    /**
     * Populates the {@link AgentRegistry} instance with required and optional fields
     * @throws SaiException If any required fields cannot be populated, or other exceptional conditions
     */
    private void populate() throws SaiException {
        this.socialAgentRegistrations = new SocialAgentRegistrationList<>(this.getDataFactory(), this.getResource());
        this.applicationRegistrations = new ApplicationRegistrationList<>(this.getDataFactory(), this.getResource());
        this.socialAgentRegistrations.populate();
        this.applicationRegistrations.populate();
    }

    /**
     * Class for access and iteration of {@link SocialAgentRegistration}s. Most of the capability is provided
     * through extension of {@link RegistrationList}, which requires select overrides to ensure the correct
     * types are built and returned by the iterator.
     */
    public static class SocialAgentRegistrationList<T> extends RegistrationList<T> {
        public SocialAgentRegistrationList(DataFactory dataFactory, Resource resource) { super(dataFactory, resource, HAS_SOCIAL_AGENT_REGISTRATION); }
        @Override
        public T find(URL agentUrl) {
            for (T registration : this) {
                SocialAgentRegistration social = (SocialAgentRegistration) registration;
                if (agentUrl.equals(social.getRegisteredAgent())) { return (T) social; }
            }
            return null;
        }
        @Override
        public Iterator<T> iterator() { return new SocialAgentRegistrationListIterator<>(this.getDataFactory(), this.getRegistrationUrls()); }
        private class SocialAgentRegistrationListIterator<T> extends RegistrationListIterator<T> {
            public SocialAgentRegistrationListIterator(DataFactory dataFactory, List<URL> registrationUrls) { super(dataFactory, registrationUrls); }
            @SneakyThrows
            @Override
            public T next() {
                URL registrationUrl = current.next();
                return (T) SocialAgentRegistration.build(registrationUrl, dataFactory);
            }
        }
    }

    /**
     * Class for access and iteration of {@link ApplicationRegistration}s. Most of the capability is provided
     * through extension of {@link RegistrationList}, which requires select overrides to ensure the correct
     * types are built and returned by the iterator.
     */
    public static class ApplicationRegistrationList<T> extends RegistrationList<T> {
        public ApplicationRegistrationList(DataFactory dataFactory, Resource resource) { super(dataFactory, resource, HAS_APPLICATION_REGISTRATION); }
        @Override
        public T find(URL agentUrl) {
            for (T registration : this) {
                ApplicationRegistration application = (ApplicationRegistration) registration;
                if (agentUrl.equals(application.getRegisteredAgent())) { return registration; }
            }
            return null;
        }
        @Override
        public Iterator<T> iterator() { return new ApplicationRegistrationListIterator<>(this.getDataFactory(), this.getRegistrationUrls()); }
        private class ApplicationRegistrationListIterator<T> extends RegistrationListIterator<T> {
            public ApplicationRegistrationListIterator(DataFactory dataFactory, List<URL> registrationUrls) { super(dataFactory, registrationUrls); }
            @SneakyThrows
            @Override
            public T next() {
                URL registrationUrl = current.next();
                return (T) ApplicationRegistration.build(registrationUrl, dataFactory);
            }
        }
    }

}

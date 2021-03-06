package com.janeirodigital.sai.core.agents;

import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiRuntimeException;
import com.janeirodigital.sai.core.resources.CRUDResource;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.core.utils.RegistrationList;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.httputils.HttpUtils.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.rdfutils.RdfUtils.getNewResourceForType;

/**
 * Modifiable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#ar-registry">Agent Registry</a>
 */
@Getter
public class AgentRegistry extends CRUDResource {

    private final SocialAgentRegistrationList<SocialAgentRegistration> socialAgentRegistrations;
    private final ApplicationRegistrationList<ApplicationRegistration> applicationRegistrations;

    /**
     * Construct an {@link AgentRegistry} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    public AgentRegistry(Builder builder) throws SaiException {
        super(builder);
        this.socialAgentRegistrations = builder.socialAgentRegistrations;
        this.applicationRegistrations = builder.applicationRegistrations;
    }

    /**
     * Get an {@link AgentRegistry} at the provided <code>uri</code>
     * @param uri URI of the {@link AgentRegistry} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link AgentRegistry}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static AgentRegistry get(URI uri, SaiSession saiSession, ContentType contentType) throws SaiException, SaiHttpNotFoundException {
        Builder builder = new Builder(uri, saiSession);
        try (Response response = read(uri, saiSession, contentType, false)) {
            return builder.setDataset(response).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URI, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param uri URI of the {@link AgentRegistry}
     * @param saiSession {@link SaiSession} to assign
     * @return
     */
    public static AgentRegistry get(URI uri, SaiSession saiSession) throws SaiHttpNotFoundException, SaiException {
        return get(uri, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link AgentRegistry} using the attributes of the current instance
     * @return Reloaded {@link AgentRegistry}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public AgentRegistry reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.uri, this.saiSession, this.contentType);
    }

    /**
     * Indicate whether the {@link AgentRegistry} has any registrations
     * @return true if there are not registrations
     */
    public boolean isEmpty() {
        return socialAgentRegistrations.isEmpty() && applicationRegistrations.isEmpty();
    }

    /**
     * Add a {@link SocialAgentRegistration} or {@link ApplicationRegistration} to the {@link AgentRegistry}
     * @param registration {@link AgentRegistration} to add
     * @throws SaiAlreadyExistsException
     */
    public void add(AgentRegistration registration) throws SaiAlreadyExistsException {
        Objects.requireNonNull(registration, "Cannot add a null agent registration to agent registry");
        if (registration instanceof SocialAgentRegistration) { addSocialAgentRegistration((SocialAgentRegistration) registration); }
        if (registration instanceof ApplicationRegistration) { addApplicationRegistration((ApplicationRegistration) registration); }
    }

    /**
     * Add a {@link SocialAgentRegistration} to the {@link AgentRegistry}. Ensures a registration 
     * doesn't already exist for the registered agent.
     * @param registration {@link SocialAgentRegistration} to add
     * @throws SaiAlreadyExistsException
     */
    private void addSocialAgentRegistration(SocialAgentRegistration registration) throws SaiAlreadyExistsException {
        SocialAgentRegistration found = this.getSocialAgentRegistrations().find(registration.getRegisteredAgent());
        if (found != null) { throw new SaiAlreadyExistsException("Social agent registration already exists for " + registration.getRegisteredAgent()); }
        this.getSocialAgentRegistrations().add(registration.getUri());
    }

    /**
     * Add a {@link ApplicationRegistration} to the {@link AgentRegistry}. Ensures a registration 
     * doesn't already exist for the registered agent.
     * @param registration {@link ApplicationRegistration} to add
     * @throws SaiAlreadyExistsException
     */
    private void addApplicationRegistration(ApplicationRegistration registration) throws SaiAlreadyExistsException {
        ApplicationRegistration found = this.getApplicationRegistrations().find(registration.getRegisteredAgent());
        if (found != null) { throw new SaiAlreadyExistsException("Application registration already exists for " + registration.getRegisteredAgent()); }
        this.getSocialAgentRegistrations().add(registration.getUri());
    }

    /**
     * Remove an {@link AgentRegistration} from the {@link AgentRegistry}
     * @param registration {@link AgentRegistration} to remove
     */
    public void remove(AgentRegistration registration) {
        Objects.requireNonNull(registration, "Cannot remove a null agent registration to agent registry");
        if (registration instanceof SocialAgentRegistration) { this.socialAgentRegistrations.remove(registration.getUri()); }
        if (registration instanceof ApplicationRegistration) { this.applicationRegistrations.remove(registration.getUri()); }
    }

    /**
     * Builder for {@link AgentRegistry} instances.
     */
    public static class Builder extends CRUDResource.Builder<Builder> {

        private SocialAgentRegistrationList<SocialAgentRegistration> socialAgentRegistrations;
        private ApplicationRegistrationList<ApplicationRegistration> applicationRegistrations;

        /**
         * Initialize builder with <code>uri</code> and <code>saiSession</code>
         * @param uri URI of the {@link AgentRegistry} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URI uri, SaiSession saiSession) {
            super(uri, saiSession);
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
         * Populates the fields of the {@link AgentRegistry} based on the associated Jena resource.
         * @throws SaiException
         */
        private void populateFromDataset() throws SaiException {
            try {
                this.socialAgentRegistrations = new SocialAgentRegistrationList<>(this.saiSession, this.resource);
                this.applicationRegistrations = new ApplicationRegistrationList<>(this.saiSession, this.resource);
                this.socialAgentRegistrations.populate();
                this.applicationRegistrations.populate();
            } catch (SaiException ex) {
                throw new SaiException("Failed to load data registry " + this.uri, ex);
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        private void populateDataset() {
            this.resource = getNewResourceForType(this.uri, AGENT_REGISTRY);
            this.dataset = this.resource.getModel();
            // Note that agent registration URIs added via setSocialAgentRegistrationUri or
            // setApplicationRegistrationUris are automatically added to the
            // dataset graph, so they don't have to be added here again
        }

        /**
         * Build the {@link AgentRegistry} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}.
         * @return {@link AgentRegistry}
         * @throws SaiException
         */
        public AgentRegistry build() throws SaiException {
            if (this.dataset == null) { populateDataset(); }
            return new AgentRegistry(this);
        }
    }

    /**
     * Class for access and iteration of {@link SocialAgentRegistration}s. Most of the capability is provided
     * through extension of {@link RegistrationList}, which requires select overrides to ensure the correct
     * types are built and returned by the iterator.
     */
    public static class SocialAgentRegistrationList<T> extends RegistrationList<T> {

        public SocialAgentRegistrationList(SaiSession saiSession, Resource resource) { super(saiSession, resource, HAS_SOCIAL_AGENT_REGISTRATION); }

        /**
         * Override the default find in {@link RegistrationList} to lookup based on the registeredAgent of
         * the {@link SocialAgentRegistration}
         * @param agentUri URI of the registered agent
         * @return {@link SocialAgentRegistration}
         */
        @Override
        public T find(URI agentUri) {
            for (T registration : this) {
                SocialAgentRegistration social = (SocialAgentRegistration) registration;
                if (agentUri.equals(social.getRegisteredAgent())) { return registration; }
            }
            return null;
        }

        /**
         * Return an iterator for {@link SocialAgentRegistration} instances
         * @return {@link SocialAgentRegistration} Iterator
         */
        @Override
        public Iterator<T> iterator() { return new SocialAgentRegistrationListIterator<>(this.getSaiSession(), this.getRegistrationUris()); }

        /**
         * Custom iterator that iterates over {@link SocialAgentRegistration} URIs and gets actual instances of them
         */
        private static class SocialAgentRegistrationListIterator<T> extends RegistrationListIterator<T> {

            public SocialAgentRegistrationListIterator(SaiSession saiSession, List<URI> registrationUris) { super(saiSession, registrationUris); }

            /**
             * Get the {@link SocialAgentRegistration} for the next URI in the iterator
             * @return {@link SocialAgentRegistration}
             */
            @Override
            public T next() {
                try {
                    URI registrationUri = current.next();
                    return (T) SocialAgentRegistration.get(registrationUri, saiSession);
                } catch (SaiException | SaiHttpNotFoundException ex) {
                    throw new SaiRuntimeException("Failed to get social agent registration while iterating list", ex);
                }
            }
        }
    }

    /**
     * Class for access and iteration of {@link ApplicationRegistration}s. Most of the capability is provided
     * through extension of {@link RegistrationList}, which requires select overrides to ensure the correct
     * types are built and returned by the iterator.
     */
    public static class ApplicationRegistrationList<T> extends RegistrationList<T> {

        public ApplicationRegistrationList(SaiSession saiSession, Resource resource) { super(saiSession, resource, HAS_APPLICATION_REGISTRATION); }

        /**
         * Override the default find in {@link RegistrationList} to lookup based on the registeredAgent of
         * the {@link ApplicationRegistration}
         * @param agentUri URI of the registered agent
         * @return {@link ApplicationRegistration}
         */
        @Override
        public T find(URI agentUri) {
            for (T registration : this) {
                ApplicationRegistration application = (ApplicationRegistration) registration;
                if (agentUri.equals(application.getRegisteredAgent())) { return registration; }
            }
            return null;
        }

        /**
         * Return an iterator for {@link ApplicationRegistration} instances
         * @return {@link ApplicationRegistration} Iterator
         */
        @Override
        public Iterator<T> iterator() { return new ApplicationRegistrationListIterator<>(this.getSaiSession(), this.getRegistrationUris()); }

        /**
         * Custom iterator that iterates over {@link ApplicationRegistration} URIs and gets actual instances of them
         */
        private static class ApplicationRegistrationListIterator<T> extends RegistrationListIterator<T> {

            public ApplicationRegistrationListIterator(SaiSession saiSession, List<URI> registrationUris) { super(saiSession, registrationUris); }

            /**
             * Get the {@link ApplicationRegistration} for the next URI in the iterator
             * @return {@link ApplicationRegistration}
             */
            @Override
            public T next() {
                try {
                    URI registrationUri = current.next();
                    return (T) ApplicationRegistration.get(registrationUri, saiSession);
                } catch (SaiException | SaiHttpNotFoundException ex) {
                    throw new SaiRuntimeException("Failed to get application registration while iterating list", ex);
                }
            }
        }
    }
}

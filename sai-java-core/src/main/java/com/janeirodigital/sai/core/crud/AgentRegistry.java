package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import lombok.SneakyThrows;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.helpers.HttpHelper.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.core.helpers.HttpHelper.getRdfModelFromResponse;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getNewResourceForType;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

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
     * Get an {@link AgentRegistry} at the provided <code>url</code>
     * @param url URL of the {@link AgentRegistry} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use
     * @return Retrieved {@link AgentRegistry}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static AgentRegistry get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        AgentRegistry.Builder builder = new AgentRegistry.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link AgentRegistry}
     * @param saiSession {@link SaiSession} to assign
     * @return
     */
    public static AgentRegistry get(URL url, SaiSession saiSession) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link AgentRegistry} using the attributes of the current instance
     * @return Reloaded {@link AgentRegistry}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public AgentRegistry reload() throws SaiNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    /**
     * Builder for {@link AgentRegistry} instances.
     */
    public static class Builder extends CRUDResource.Builder<Builder> {

        private SocialAgentRegistrationList<SocialAgentRegistration> socialAgentRegistrations;
        private ApplicationRegistrationList<ApplicationRegistration> applicationRegistrations;
        private List<URL> socialAgentRegistrationUrls;
        private List<URL> applicationRegistrationUrls;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link AgentRegistry} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URL url, SaiSession saiSession) {
            super(url, saiSession);
            this.socialAgentRegistrationUrls = new ArrayList<>();
            this.applicationRegistrationUrls = new ArrayList<>();
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
         * Set the URLs of social agent registrations in the Agent Registry (which must have already been created)
         * @param socialAgentRegistrationUrls List of URLs to {@link SocialAgentRegistration} instances
         * @return {@link Builder}
         */
        public Builder setSocialAgentRegistrationUrls(List<URL> socialAgentRegistrationUrls) {
            Objects.requireNonNull(socialAgentRegistrationUrls, "Must provide a list of social agent registration urls to the agent registry builder");
            this.socialAgentRegistrationUrls.addAll(socialAgentRegistrationUrls);
            return this;
        }

        /**
         * Set the URLs of social agent registrations in the Agent Registry (which must have already been created)
         * @param applicationRegistrationUrls List of URLs to {@link ApplicationRegistration} instances
         * @return {@link Builder}
         */
        public Builder setApplicationRegistrationUrls(List<URL> applicationRegistrationUrls) {
            Objects.requireNonNull(applicationRegistrationUrls, "Must provide a list of application registration urls to the agent registry builder");
            this.applicationRegistrationUrls.addAll(applicationRegistrationUrls);
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
                if (!socialAgentRegistrationUrls.isEmpty()) { this.socialAgentRegistrations.addAll(socialAgentRegistrationUrls); }
                if (!applicationRegistrationUrls.isEmpty()) { this.applicationRegistrations.addAll(applicationRegistrationUrls); }
                this.socialAgentRegistrations.populate();
                this.applicationRegistrations.populate();
            } catch (SaiException | SaiAlreadyExistsException ex) {
                throw new SaiException("Failed to load data registry " + this.url + ": " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         * @throws SaiException
         */
        private void populateDataset() throws SaiException {
            this.resource = getNewResourceForType(this.url, AGENT_REGISTRY);
            this.dataset = this.resource.getModel();
            // Note that agent registration URLs added via setSocialAgentRegistrationUrl or
            // setApplicationRegistrationUrls are automatically added to the
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
         * @param agentUrl URL of the registered agent
         * @return {@link SocialAgentRegistration}
         */
        @Override
        public T find(URL agentUrl) {
            for (T registration : this) {
                SocialAgentRegistration social = (SocialAgentRegistration) registration;
                if (agentUrl.equals(social.getRegisteredAgent())) { return (T) social; }
            }
            return null;
        }

        /**
         * Return an iterator for {@link SocialAgentRegistration} instances
         * @return {@link SocialAgentRegistration} Iterator
         */
        @Override
        public Iterator<T> iterator() { return new SocialAgentRegistrationListIterator<>(this.getSaiSession(), this.getRegistrationUrls()); }

        /**
         * Custom iterator that iterates over {@link SocialAgentRegistration} URLs and gets actual instances of them
         */
        private class SocialAgentRegistrationListIterator<T> extends RegistrationListIterator<T> {

            public SocialAgentRegistrationListIterator(SaiSession saiSession, List<URL> registrationUrls) { super(saiSession, registrationUrls); }

            /**
             * Get the {@link SocialAgentRegistration} for the next URL in the iterator
             * @return {@link SocialAgentRegistration}
             */
            @SneakyThrows
            @Override
            public T next() {
                URL registrationUrl = current.next();
                return (T) SocialAgentRegistration.get(registrationUrl, saiSession);
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
         * @param agentUrl URL of the registered agent
         * @return {@link ApplicationRegistration}
         */
        @Override
        public T find(URL agentUrl) {
            for (T registration : this) {
                ApplicationRegistration application = (ApplicationRegistration) registration;
                if (agentUrl.equals(application.getRegisteredAgent())) { return registration; }
            }
            return null;
        }

        /**
         * Return an iterator for {@link ApplicationRegistration} instances
         * @return {@link ApplicationRegistration} Iterator
         */
        @Override
        public Iterator<T> iterator() { return new ApplicationRegistrationListIterator<>(this.getSaiSession(), this.getRegistrationUrls()); }

        /**
         * Custom iterator that iterates over {@link ApplicationRegistration} URLs and gets actual instances of them
         */
        private class ApplicationRegistrationListIterator<T> extends RegistrationListIterator<T> {

            public ApplicationRegistrationListIterator(SaiSession saiSession, List<URL> registrationUrls) { super(saiSession, registrationUrls); }

            /**
             * Get the {@link ApplicationRegistration} for the next URL in the iterator
             * @return {@link ApplicationRegistration}
             */
            @SneakyThrows
            @Override
            public T next() {
                URL registrationUrl = current.next();
                return (T) ApplicationRegistration.get(registrationUrl, saiSession);
            }
        }
    }
}

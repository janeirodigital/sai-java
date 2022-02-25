package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.exceptions.SaiAlreadyExistsException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.Getter;
import lombok.SneakyThrows;
import okhttp3.Headers;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getNewResourceForType;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getResourceFromModel;
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
     * Construct a new {@link AgentRegistry}
     * @param url URL of the {@link AgentRegistry}
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    public AgentRegistry(URL url, DataFactory dataFactory, Model dataset, Resource resource, ContentType contentType,
                         SocialAgentRegistrationList<SocialAgentRegistration> socialAgentRegistrations,
                         ApplicationRegistrationList<ApplicationRegistration> applicationRegistrations) throws SaiException {
        super(url, dataFactory, false);
        this.dataset = dataset;
        this.resource = resource;
        this.contentType = contentType;
        this.socialAgentRegistrations = socialAgentRegistrations;
        this.applicationRegistrations = applicationRegistrations;
    }

    /**
     * Get a {@link AgentRegistry} at the provided <code>url</code>
     * @param url URL of the {@link AgentRegistry} to get
     * @param dataFactory {@link DataFactory} to assign
     * @return Retrieved {@link AgentRegistry}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static AgentRegistry get(URL url, DataFactory dataFactory, ContentType contentType) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(url, "Must provide the URL of the agent registry to get");
        Objects.requireNonNull(dataFactory, "Must provide a data factory to assign to the agent registry");
        Objects.requireNonNull(contentType, "Must provide a content type for the agent registry");
        AgentRegistry.Builder builder = new AgentRegistry.Builder(url, dataFactory, contentType);
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(dataFactory.getAuthorizedSession(), dataFactory.getHttpClient(), url, headers))) {
            builder.setDataset(getRdfModelFromResponse(response));
        }
        return builder.build();
    }

    /**
     * Call {@link #get(URL, DataFactory, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link AgentRegistry}
     * @param dataFactory {@link DataFactory} to assign
     * @return
     */
    public static AgentRegistry get(URL url, DataFactory dataFactory) throws SaiNotFoundException, SaiException {
        return get(url, dataFactory, DEFAULT_RDF_CONTENT_TYPE);
    }
    
    /**
     * Builder for {@link AgentRegistry} instances.
     */
    public static class Builder {
        private final URL url;
        private final DataFactory dataFactory;
        private final ContentType contentType;
        private Model dataset;
        private Resource resource;
        private SocialAgentRegistrationList<SocialAgentRegistration> socialAgentRegistrations;
        private ApplicationRegistrationList<ApplicationRegistration> applicationRegistrations;
        private List<URL> socialAgentRegistrationUrls;
        private List<URL> applicationRegistrationUrls;

        /**
         * Initialize builder with <code>url</code> and <code>dataFactory</code>
         *
         * @param url URL of the {@link AgentRegistry} to build
         * @param dataFactory {@link DataFactory} to assign
         * @param contentType {@link ContentType} to assign
         */
        public Builder(URL url, DataFactory dataFactory, ContentType contentType) {
            Objects.requireNonNull(url, "Must provide a URL for the agent registry builder");
            Objects.requireNonNull(dataFactory, "Must provide a data factory for the agent registry builder");
            Objects.requireNonNull(contentType, "Must provide a content type for the agent registry builder");
            this.url = url;
            this.dataFactory = dataFactory;
            this.contentType = contentType;
            this.socialAgentRegistrationUrls = new ArrayList<>();
            this.applicationRegistrationUrls = new ArrayList<>();
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
            Objects.requireNonNull(dataset, "Must provide a Jena model for the agent registry builder");
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            populateFromDataset();
            return this;
        }

        /**
         * Set the URLs of social agent registrations in the Agent Registry (which must have already been created)
         * @param socialAgentRegistrationUrls List of URLs to {@link SocialAgentRegistration} instances
         * @return {@link Builder}
         */
        public Builder setSocialAgentRegistrationUrls(List<URL> socialAgentRegistrationUrls) throws SaiAlreadyExistsException {
            Objects.requireNonNull(socialAgentRegistrationUrls, "Must provide a list of social agent registration urls to the agent registry builder");
            this.socialAgentRegistrationUrls.addAll(socialAgentRegistrationUrls);
            return this;
        }

        /**
         * Set the URLs of social agent registrations in the Agent Registry (which must have already been created)
         * @param applicationRegistrationUrls List of URLs to {@link ApplicationRegistration} instances
         * @return {@link Builder}
         */
        public Builder setApplicationRegistrationUrls(List<URL> applicationRegistrationUrls) throws SaiAlreadyExistsException {
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
                this.socialAgentRegistrations = new SocialAgentRegistrationList<>(this.dataFactory, this.resource);
                this.applicationRegistrations = new ApplicationRegistrationList<>(this.dataFactory, this.resource);
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
         * {@link #populateDataset()}. Conversely, if a dataset was provided, the attributes of the
         * Builder will be populated from it.
         * @return {@link AgentRegistry}
         * @throws SaiException
         */
        public AgentRegistry build() throws SaiException {
            if (this.dataset == null) { populateDataset(); }
            return new AgentRegistry(this.url, this.dataFactory, this.dataset, this.resource, this.contentType, 
                                     this.socialAgentRegistrations, this.applicationRegistrations);
        }
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
                return (T) SocialAgentRegistration.get(registrationUrl, dataFactory);
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
                return (T) ApplicationRegistration.get(registrationUrl, dataFactory);
            }
        }
    }

}

package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.Getter;
import okhttp3.Headers;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedRdfResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

/**
 * Modifiable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#application-registration">Application Registration</a>.
 */
@Getter
public class ApplicationRegistration extends AgentRegistration {

    /**
     * Construct a new {@link ApplicationRegistration}
     * @param url URL of the {@link ApplicationRegistration}
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    public ApplicationRegistration(URL url, DataFactory dataFactory, Model dataset, Resource resource, ContentType contentType,
                                   URL registeredBy, URL registeredWith, OffsetDateTime registeredAt, OffsetDateTime updatedAt,
                                   URL registeredAgent, URL accessGrantUrl) throws SaiException {
        super(url, dataFactory, dataset, resource, contentType, registeredBy, registeredWith,
                registeredAt, updatedAt, registeredAgent, accessGrantUrl);
    }

    /**
     * Get a {@link ApplicationRegistration} at the provided <code>url</code>
     * @param url URL of the {@link ApplicationRegistration} to get
     * @param dataFactory {@link DataFactory} to assign
     * @return Retrieved {@link ApplicationRegistration}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ApplicationRegistration get(URL url, DataFactory dataFactory, ContentType contentType) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(url, "Must provide the URL of the social agent registration to get");
        Objects.requireNonNull(dataFactory, "Must provide a data factory to assign to the social agent registration");
        Objects.requireNonNull(contentType, "Must provide a content type for the social agent registration");
        ApplicationRegistration.Builder builder = new ApplicationRegistration.Builder(url, dataFactory, contentType);
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(dataFactory.getAuthorizedSession(), dataFactory.getHttpClient(), url, headers))) {
            builder.setDataset(getRdfModelFromResponse(response));
        }
        return builder.build();
    }

    /**
     * Call {@link #get(URL, DataFactory, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link ApplicationRegistration} to get
     * @param dataFactory {@link DataFactory} to assign
     * @return Retrieved {@link ApplicationRegistration}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public static ApplicationRegistration get(URL url, DataFactory dataFactory) throws SaiNotFoundException, SaiException {
        return get(url, dataFactory, DEFAULT_RDF_CONTENT_TYPE);
    }
    
    /**
     * Builder for {@link ApplicationRegistration} instances.
     */
    public static class Builder {

        private final URL url;
        private final DataFactory dataFactory;
        private final ContentType contentType;
        private Model dataset;
        private Resource resource;
        private URL registeredBy;
        private URL registeredWith;
        private OffsetDateTime registeredAt;
        private OffsetDateTime updatedAt;
        private URL registeredAgent;
        private URL accessGrantUrl;
        
        /**
         * Initialize builder with <code>url</code>, <code>dataFactory</code>, and desired <code>contentType</code>
         * @param url URL of the {@link ApplicationRegistration} to build
         * @param dataFactory {@link DataFactory} to assign
         * @param contentType {@link ContentType} to assign
         */
        public Builder(URL url, DataFactory dataFactory, ContentType contentType) {
            Objects.requireNonNull(url, "Must provide a URL for the application registration builder");
            Objects.requireNonNull(dataFactory, "Must provide a data factory for the application registration builder");
            Objects.requireNonNull(contentType, "Must provide a content type for the application registration builder");
            this.url = url;
            this.dataFactory = dataFactory;
            this.contentType = contentType;
        }

        /**
         * Optional Jena Model that will initialize the attributes of the Builder rather than set
         * them manually. Typically used in read scenarios when populating the Builder from
         * the contents of a remote resource.
         * @param dataset Jena model to populate the Builder attributes with
         * @return {@link Builder}
         */
        public Builder setDataset(Model dataset) throws SaiException {
            Objects.requireNonNull(dataset, "Must provide a Jena model for the application registration builder");
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            populateFromDataset();
            return this;
        }

        /**
         * Set the social agent that registered the application registration.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @param socialAgentUrl URL of the social agent that added the registration
         */
        public Builder setRegisteredBy(URL socialAgentUrl) {
            Objects.requireNonNull(socialAgentUrl, "Must provide the social agent who registered the application registration");
            this.registeredBy = socialAgentUrl;
            return this;
        }

        /**
         * Set the application that registered the application registration.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @param applicationUrl URL of the application that was use to add the registration
         */
        public Builder setRegisteredWith(URL applicationUrl) {
            Objects.requireNonNull(applicationUrl, "Must provide the application used to register the application registration");
            this.registeredWith = applicationUrl;
            return this;
        }

        /**
         * Set the time that the application registration was created.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @param registeredAt when the application registration was created
         */
        public Builder setRegisteredAt(OffsetDateTime registeredAt) {
            Objects.requireNonNull(registeredAt, "Must provide the time that the application registration was created");
            this.registeredAt = registeredAt;
            return this;
        }

        /**
         * Set the time that the application registration was updated.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @param updatedAt when the application registration was updated
         */
        public Builder setUpdatedAt(OffsetDateTime updatedAt) {
            Objects.requireNonNull(updatedAt, "Must provide the time that the application registration was updated");
            this.updatedAt = updatedAt;
            return this;
        }

        /**
         * Set the registered agent that is the subject of the application registration
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @param agentUrl URL of the agent that was registered
         */
        public Builder setRegisteredAgent(URL agentUrl) {
            Objects.requireNonNull(agentUrl, "Must provide the agent to register");
            this.registeredAgent = agentUrl;
            return this;
        }

        /**
         * Set the access grant for the application registration
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#access-grant">Access Grants</a>
         * @param accessGrantUrl URL of the access grant
         */
        public Builder setAccessGrant(URL accessGrantUrl) {
            Objects.requireNonNull(accessGrantUrl, "Must provide the access grant for the application registration");
            this.registeredAgent = accessGrantUrl;
            return this;
        }
        
        /**
         * Populates the fields of the {@link ApplicationRegistration.Builder} based on the associated Jena resource.
         * @throws SaiException
         */
        protected void populateFromDataset() throws SaiException {
            try {
                this.registeredBy = getRequiredUrlObject(this.resource, REGISTERED_BY);
                this.registeredWith = getRequiredUrlObject(this.resource, REGISTERED_WITH);
                this.registeredAt = getRequiredDateTimeObject(this.resource, REGISTERED_AT);
                this.updatedAt = getRequiredDateTimeObject(this.resource, UPDATED_AT);
                this.registeredAgent = getRequiredUrlObject(this.resource, REGISTERED_AGENT);
                this.accessGrantUrl = getUrlObject(this.resource, HAS_ACCESS_GRANT);
            } catch (SaiNotFoundException | SaiException ex) {
                throw new SaiException("Failed to load social agent registration " + this.url + ": " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        private void populateDataset() {
            this.resource = getNewResourceForType(this.url, SOCIAL_AGENT_REGISTRATION);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, REGISTERED_BY, this.registeredBy);
            updateObject(this.resource, REGISTERED_WITH, this.registeredWith);
            updateObject(this.resource, REGISTERED_AT, this.registeredAt);
            updateObject(this.resource, UPDATED_AT, this.updatedAt);
            updateObject(this.resource, REGISTERED_AGENT, this.registeredAgent);
            if (this.accessGrantUrl != null) { updateObject(this.resource, HAS_ACCESS_GRANT, this.accessGrantUrl); }
        }

        /**
         * Build the {@link ApplicationRegistration} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}. Conversely, if a dataset was provided, the attributes of the
         * Builder will be populated from it.
         * @return {@link ApplicationRegistration}
         * @throws SaiException
         */
        public ApplicationRegistration build() throws SaiException {
            Objects.requireNonNull(this.registeredBy, "Must provide the social agent who registered the agent registration");
            Objects.requireNonNull(this.registeredWith, "Must provide the application used to register the agent registration");
            Objects.requireNonNull(this.registeredAt, "Must provide the time that the agent registration was created");
            Objects.requireNonNull(this.updatedAt, "Must provide the time that the agent registration was updated");
            Objects.requireNonNull(this.registeredAgent, "Must provide the agent to register");
            if (this.dataset == null) { populateDataset(); }
            return new ApplicationRegistration(this.url, this.dataFactory, this.dataset, this.resource, this.contentType,
                    this.registeredBy, this.registeredWith, this.registeredAt, this.updatedAt,
                    this.registeredAgent, this.accessGrantUrl);
        }

    }

}

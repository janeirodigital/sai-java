package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.core.immutable.DataGrant;
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
 * Modifiable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agent-registration">Social Agent Registration</a>.
 */
@Getter
public class SocialAgentRegistration extends AgentRegistration {

    private final URL reciprocalRegistration;

    /**
     * Construct a new {@link SocialAgentRegistration}
     * @throws SaiException
     */
    private SocialAgentRegistration(URL url, SaiSession saiSession, Model dataset, Resource resource, ContentType contentType,
                                    URL registeredBy, URL registeredWith, OffsetDateTime registeredAt, OffsetDateTime updatedAt,
                                    URL registeredAgent, URL accessGrantUrl, URL reciprocalRegistration) throws SaiException {
        super(url, saiSession, dataset, resource, contentType, registeredBy, registeredWith,
              registeredAt, updatedAt, registeredAgent, accessGrantUrl);
        this.reciprocalRegistration = reciprocalRegistration;
    }

    /**
     * Get a {@link SocialAgentRegistration} at the provided <code>url</code>
     * @param url URL of the {@link SocialAgentRegistration} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link SocialAgentRegistration}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static SocialAgentRegistration get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        Objects.requireNonNull(url, "Must provide the URL of the social agent registration to get");
        Objects.requireNonNull(saiSession, "Must provide a sai session to assign to the social agent registration");
        Objects.requireNonNull(contentType, "Must provide a content type for the social agent registration");
        SocialAgentRegistration.Builder builder = new SocialAgentRegistration.Builder(url, saiSession, contentType);
        Headers headers = addHttpHeader(HttpHeader.ACCEPT, contentType.getValue());
        try (Response response = checkReadableResponse(getProtectedRdfResource(saiSession.getAuthorizedSession(), saiSession.getHttpClient(), url, headers))) {
            builder.setDataset(getRdfModelFromResponse(response));
        }
        return builder.build();
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link SocialAgentRegistration} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link SocialAgentRegistration}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public static SocialAgentRegistration get(URL url, SaiSession saiSession) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Builder for {@link SocialAgentRegistration} instances.
     */
    public static class Builder {

        private final URL url;
        private final SaiSession saiSession;
        private final ContentType contentType;
        private Model dataset;
        private Resource resource;
        private URL registeredBy;
        private URL registeredWith;
        private OffsetDateTime registeredAt;
        private OffsetDateTime updatedAt;
        private URL registeredAgent;
        private URL accessGrantUrl;
        URL reciprocalRegistration;

        /**
         * Initialize builder with <code>url</code>, <code>saiSession</code>, and desired <code>contentType</code>
         * @param url URL of the {@link SocialAgentRegistration} to build
         * @param saiSession {@link SaiSession} to assign
         * @param contentType {@link ContentType} to assign
         */
        public Builder(URL url, SaiSession saiSession, ContentType contentType) {
            Objects.requireNonNull(url, "Must provide a URL for the social agent registration builder");
            Objects.requireNonNull(saiSession, "Must provide a sai session for the social agent registration builder");
            Objects.requireNonNull(contentType, "Must provide a content type for the social agent registration builder");
            this.url = url;
            this.saiSession = saiSession;
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
            Objects.requireNonNull(dataset, "Must provide a Jena model for the social agent registration builder");
            this.dataset = dataset;
            this.resource = getResourceFromModel(this.dataset, this.url);
            populateFromDataset();
            return this;
        }

        /**
         * Set the social agent that registered the social agent registration.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @param socialAgentUrl URL of the social agent that added the registration
         */
        public Builder setRegisteredBy(URL socialAgentUrl) {
            Objects.requireNonNull(socialAgentUrl, "Must provide the social agent who registered the social agent registration");
            this.registeredBy = socialAgentUrl;
            return this;
        }

        /**
         * Set the application that registered the social agent registration.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @param applicationUrl URL of the application that was use to add the registration
         */
        public Builder setRegisteredWith(URL applicationUrl) {
            Objects.requireNonNull(applicationUrl, "Must provide the application used to register the social agent registration");
            this.registeredWith = applicationUrl;
            return this;
        }

        /**
         * Set the time that the social agent registration was created.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @param registeredAt when the social agent registration was created
         */
        public Builder setRegisteredAt(OffsetDateTime registeredAt) {
            Objects.requireNonNull(registeredAt, "Must provide the time that the social agent registration was created");
            this.registeredAt = registeredAt;
            return this;
        }

        /**
         * Set the time that the social agent registration was updated.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @param updatedAt when the social agent registration was updated
         */
        public Builder setUpdatedAt(OffsetDateTime updatedAt) {
            Objects.requireNonNull(updatedAt, "Must provide the time that the social agent registration was updated");
            this.updatedAt = updatedAt;
            return this;
        }

        /**
         * Set the registered agent that is the subject of the social agent registration
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @param agentUrl URL of the agent that was registered
         */
        public Builder setRegisteredAgent(URL agentUrl) {
            Objects.requireNonNull(agentUrl, "Must provide the agent to register");
            this.registeredAgent = agentUrl;
            return this;
        }

        /**
         * Set the access grant for the social agent registration
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#access-grant">Access Grants</a>
         * @param accessGrantUrl URL of the access grant
         */
        public Builder setAccessGrant(URL accessGrantUrl) {
            Objects.requireNonNull(accessGrantUrl, "Must provide the access grant for the social agent registration");
            this.registeredAgent = accessGrantUrl;
            return this;
        }
        
        /**
         * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agent-registration">Reciprocal Registration</a>
         * that <code>registeredAgent</code> maintains for the social agent that owns the agent registry.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar-registry">Agent Registry</a>
         * @param registrationUrl URL of the reciprocal social agent registration
         */
        public Builder setReciprocalRegistration(URL registrationUrl) {
            Objects.requireNonNull(registrationUrl, "Must provide a reciprocal registration for the social agent registration");
            this.reciprocalRegistration = registrationUrl;
            return this;
        }

        /**
         * Populates the fields of the {@link Builder} based on the associated Jena resource.
         * @throws SaiException
         */
        protected void populateFromDataset() throws SaiException {
            try {
                this.registeredBy = getRequiredUrlObject(this.resource, REGISTERED_BY);
                this.registeredWith = getRequiredUrlObject(this.resource, REGISTERED_WITH);
                this.registeredAt = getRequiredDateTimeObject(this.resource, REGISTERED_AT);
                this.updatedAt = getRequiredDateTimeObject(this.resource, UPDATED_AT);
                this.registeredAgent = getRequiredUrlObject(this.resource, REGISTERED_AGENT);
                this.reciprocalRegistration = getUrlObject(this.resource, RECIPROCAL_REGISTRATION);
                this.accessGrantUrl = getUrlObject(this.resource, HAS_ACCESS_GRANT);
            } catch (SaiNotFoundException | SaiException ex) {
                throw new SaiException("Failed to load social agent registration " + this.url + ": " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         * @throws SaiException
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
            if (this.reciprocalRegistration != null) { updateObject(this.resource, RECIPROCAL_REGISTRATION, this.reciprocalRegistration); }
        }

        /**
         * Build the {@link DataGrant} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}. Conversely, if a dataset was provided, the attributes of the
         * Builder will be populated from it.
         * @return {@link DataGrant}
         * @throws SaiException
         */
        public SocialAgentRegistration build() throws SaiException {
            Objects.requireNonNull(this.registeredBy, "Must provide the social agent who registered the agent registration");
            Objects.requireNonNull(this.registeredWith, "Must provide the application used to register the agent registration");
            Objects.requireNonNull(this.registeredAt, "Must provide the time that the agent registration was created");
            Objects.requireNonNull(this.updatedAt, "Must provide the time that the agent registration was updated");
            Objects.requireNonNull(this.registeredAgent, "Must provide the agent to register");
            if (this.dataset == null) { populateDataset(); }
            return new SocialAgentRegistration(this.url, this.saiSession, this.dataset, this.resource, this.contentType,
                                               this.registeredBy, this.registeredWith, this.registeredAt, this.updatedAt,
                                               this.registeredAgent, this.accessGrantUrl, this.reciprocalRegistration);
        }

    }

}

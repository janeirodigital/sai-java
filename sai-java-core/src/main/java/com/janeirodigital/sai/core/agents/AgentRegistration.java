package com.janeirodigital.sai.core.agents;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.resources.CRUDResource;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.sai.rdfutils.SaiRdfNotFoundException;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.httputils.HttpUtils.addChildToUriPath;
import static com.janeirodigital.sai.rdfutils.RdfUtils.*;

/**
 * Abstract base instantiation of a modifiable
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registration</a>, which
 * can be extended for type-specific implementations (i.e. Social Agent Registration, Application Registration).
 */
@Getter @Setter
public abstract class AgentRegistration extends CRUDResource {

    private URI registeredBy;
    private URI registeredWith;
    private OffsetDateTime registeredAt;
    private OffsetDateTime updatedAt;
    private URI registeredAgent;
    private URI accessGrantUri;

    /**
     * Construct an {@link AgentRegistration} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    protected AgentRegistration(Builder<?> builder) throws SaiException {
        super(builder);
        this.registeredBy = builder.registeredBy;
        this.registeredWith = builder.registeredWith;
        this.registeredAt = builder.registeredAt;
        this.updatedAt = builder.updatedAt;
        this.registeredAgent = builder.registeredAgent;
        this.accessGrantUri = builder.accessGrantUri;
    }
    
    /**
     * Generates the URI for a new contained "child" resource in the {@link AgentRegistration}
     * @return Generated URI
     * @throws SaiException
     */
    public URI generateContainedUri() throws SaiException {
        try { return addChildToUriPath(this.getUri(), UUID.randomUUID().toString()); } catch (SaiHttpException ex) {
            throw new SaiException("Unable to add child to uri path", ex);
        }
    }

    /**
     * Indicates whether or not there is an {@link com.janeirodigital.sai.core.immutable.AccessGrant} linked
     * to the registration
     * @return true when there is an access grant
     */
    public boolean hasAccessGrant() {
        return this.accessGrantUri != null;
    }

    /**
     * Abstract builder for {@link AgentRegistration} instances (used by subclasses). See
     * {@link SocialAgentRegistration} and {@link ApplicationRegistration}
     */
    protected abstract static class Builder <T extends CRUDResource.Builder<T>> extends CRUDResource.Builder<T> {

        protected URI registeredBy;
        protected URI registeredWith;
        protected OffsetDateTime registeredAt;
        protected OffsetDateTime updatedAt;
        protected URI registeredAgent;
        protected URI accessGrantUri;

        /**
         * Initialize builder with <code>uri</code> and <code>saiSession</code>
         * @param uri URI of the {@link AgentRegistration} to build
         * @param saiSession {@link SaiSession} to assign
         */
        protected Builder(URI uri, SaiSession saiSession) { super(uri, saiSession); }

        /**
         * Set the social agent that registered the agent registration.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @param socialAgentUri URI of the social agent that added the registration
         */
        public T setRegisteredBy(URI socialAgentUri) {
            Objects.requireNonNull(socialAgentUri, "Must provide the social agent who registered the agent registration");
            this.registeredBy = socialAgentUri;
            return getThis();
        }

        /**
         * Set the application that registered the agent registration.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @param applicationUri URI of the application that was use to add the registration
         */
        public T setRegisteredWith(URI applicationUri) {
            Objects.requireNonNull(applicationUri, "Must provide the application used to register the agent registration");
            this.registeredWith = applicationUri;
            return getThis();
        }

        /**
         * Set the time that the agent registration was created.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @param registeredAt when the agent registration was created
         */
        public T setRegisteredAt(OffsetDateTime registeredAt) {
            Objects.requireNonNull(registeredAt, "Must provide the time that the agent registration was created");
            this.registeredAt = registeredAt;
            return getThis();
        }

        /**
         * Set the time that the agent registration was updated.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @param updatedAt when the agent registration was updated
         */
        public T setUpdatedAt(OffsetDateTime updatedAt) {
            Objects.requireNonNull(updatedAt, "Must provide the time that the agent registration was updated");
            this.updatedAt = updatedAt;
            return getThis();
        }

        /**
         * Set the registered agent that is the subject of the agent registration
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @param agentUri URI of the agent that was registered
         */
        public T setRegisteredAgent(URI agentUri) {
            Objects.requireNonNull(agentUri, "Must provide the agent to register");
            this.registeredAgent = agentUri;
            return getThis();
        }

        /**
         * Set the access grant for the agent registration
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#access-grant">Access Grants</a>
         * @param accessGrantUri URI of the access grant
         */
        public T setAccessGrant(URI accessGrantUri) {
            Objects.requireNonNull(accessGrantUri, "Must provide the access grant for the agent registration");
            this.accessGrantUri = accessGrantUri;
            return getThis();
        }

        /**
         * Populates the common fields of the {@link AgentRegistration} based on the associated Jena resource.
         * @throws SaiException
         */
        protected void populateFromDataset() throws SaiException {
            Objects.requireNonNull(this.resource, "Must provide a Jena model to populate from dataset");
            try {
                this.registeredBy = getRequiredUriObject(this.resource, REGISTERED_BY);
                this.registeredWith = getRequiredUriObject(this.resource, REGISTERED_WITH);
                this.registeredAt = getRequiredDateTimeObject(this.resource, REGISTERED_AT);
                this.updatedAt = getRequiredDateTimeObject(this.resource, UPDATED_AT);
                this.registeredAgent = getRequiredUriObject(this.resource, REGISTERED_AGENT);
                this.accessGrantUri = getUriObject(this.resource, HAS_ACCESS_GRANT);
            } catch (SaiRdfException | SaiRdfNotFoundException ex) {
                throw new SaiException("Failed to load agent registration " + this.uri, ex);
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        protected void populateDataset() {
            updateObject(this.resource, REGISTERED_BY, this.registeredBy);
            updateObject(this.resource, REGISTERED_WITH, this.registeredWith);
            if (this.registeredAt == null) { this.registeredAt = OffsetDateTime.now(); }
            updateObject(this.resource, REGISTERED_AT, this.registeredAt);
            if (this.updatedAt == null) { this.updatedAt = OffsetDateTime.now(); }
            updateObject(this.resource, UPDATED_AT, this.updatedAt);
            updateObject(this.resource, REGISTERED_AGENT, this.registeredAgent);
            if (this.accessGrantUri != null) { updateObject(this.resource, HAS_ACCESS_GRANT, this.accessGrantUri); }
        }
    }

}

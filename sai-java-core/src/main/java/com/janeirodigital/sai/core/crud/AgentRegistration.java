package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import lombok.Setter;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.janeirodigital.sai.core.utils.HttpUtils.addChildToUrlPath;
import static com.janeirodigital.sai.core.utils.RdfUtils.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

/**
 * Abstract base instantiation of a modifiable
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registration</a>, which
 * can be extended for type-specific implementations (i.e. Social Agent Registration, Application Registration).
 */
@Getter @Setter
public abstract class AgentRegistration extends CRUDResource {

    private URL registeredBy;
    private URL registeredWith;
    private OffsetDateTime registeredAt;
    private OffsetDateTime updatedAt;
    private URL registeredAgent;
    private URL accessGrantUrl;

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
        this.accessGrantUrl = builder.accessGrantUrl;
    }
    
    /**
     * Generates the URL for a new contained "child" resource in the {@link AgentRegistration}
     * @return Generated URL
     * @throws SaiException
     */
    public URL generateContainedUrl() throws SaiException {
        return addChildToUrlPath(this.getUrl(), UUID.randomUUID().toString());
    }

    /**
     * Indicates whether or not there is an {@link com.janeirodigital.sai.core.immutable.AccessGrant} linked
     * to the registration
     * @return true when there is an access grant
     */
    public boolean hasAccessGrant() {
        return this.accessGrantUrl != null;
    }

    /**
     * Abstract builder for {@link AgentRegistration} instances (used by subclasses). See
     * {@link SocialAgentRegistration} and {@link ApplicationRegistration}
     */
    protected abstract static class Builder <T extends CRUDResource.Builder<T>> extends CRUDResource.Builder<T> {

        protected URL registeredBy;
        protected URL registeredWith;
        protected OffsetDateTime registeredAt;
        protected OffsetDateTime updatedAt;
        protected URL registeredAgent;
        protected URL accessGrantUrl;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link AgentRegistration} to build
         * @param saiSession {@link SaiSession} to assign
         */
        protected Builder(URL url, SaiSession saiSession) { super(url, saiSession); }

        /**
         * Set the social agent that registered the agent registration.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @param socialAgentUrl URL of the social agent that added the registration
         */
        public T setRegisteredBy(URL socialAgentUrl) {
            Objects.requireNonNull(socialAgentUrl, "Must provide the social agent who registered the agent registration");
            this.registeredBy = socialAgentUrl;
            return getThis();
        }

        /**
         * Set the application that registered the agent registration.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @param applicationUrl URL of the application that was use to add the registration
         */
        public T setRegisteredWith(URL applicationUrl) {
            Objects.requireNonNull(applicationUrl, "Must provide the application used to register the agent registration");
            this.registeredWith = applicationUrl;
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
         * @param agentUrl URL of the agent that was registered
         */
        public T setRegisteredAgent(URL agentUrl) {
            Objects.requireNonNull(agentUrl, "Must provide the agent to register");
            this.registeredAgent = agentUrl;
            return getThis();
        }

        /**
         * Set the access grant for the agent registration
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#access-grant">Access Grants</a>
         * @param accessGrantUrl URL of the access grant
         */
        public T setAccessGrant(URL accessGrantUrl) {
            Objects.requireNonNull(accessGrantUrl, "Must provide the access grant for the agent registration");
            this.accessGrantUrl = accessGrantUrl;
            return getThis();
        }

        /**
         * Populates the common fields of the {@link AgentRegistration} based on the associated Jena resource.
         * @throws SaiException
         */
        protected void populateFromDataset() throws SaiException {
            Objects.requireNonNull(this.resource, "Must provide a Jena model to populate from dataset");
            try {
                this.registeredBy = getRequiredUrlObject(this.resource, REGISTERED_BY);
                this.registeredWith = getRequiredUrlObject(this.resource, REGISTERED_WITH);
                this.registeredAt = getRequiredDateTimeObject(this.resource, REGISTERED_AT);
                this.updatedAt = getRequiredDateTimeObject(this.resource, UPDATED_AT);
                this.registeredAgent = getRequiredUrlObject(this.resource, REGISTERED_AGENT);
                this.accessGrantUrl = getUrlObject(this.resource, HAS_ACCESS_GRANT);
            } catch (SaiNotFoundException | SaiException ex) {
                throw new SaiException("Failed to load agent registration " + this.url + ": " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         * @throws SaiException
         */
        protected void populateDataset() {
            updateObject(this.resource, REGISTERED_BY, this.registeredBy);
            updateObject(this.resource, REGISTERED_WITH, this.registeredWith);
            if (this.registeredAt == null) { this.registeredAt = OffsetDateTime.now(); }
            updateObject(this.resource, REGISTERED_AT, this.registeredAt);
            if (this.updatedAt == null) { this.updatedAt = OffsetDateTime.now(); }
            updateObject(this.resource, UPDATED_AT, this.updatedAt);
            updateObject(this.resource, REGISTERED_AGENT, this.registeredAgent);
            if (this.accessGrantUrl != null) { updateObject(this.resource, HAS_ACCESS_GRANT, this.accessGrantUrl); }
        }
    }

}

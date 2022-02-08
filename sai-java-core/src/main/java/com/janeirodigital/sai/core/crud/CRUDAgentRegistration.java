package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.Getter;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Objects;

import static com.janeirodigital.sai.core.contexts.InteropContexts.AGENT_REGISTRATION_CONTEXT;
import static com.janeirodigital.sai.core.helpers.RdfHelper.buildRemoteJsonLdContext;
import static com.janeirodigital.sai.core.helpers.RdfHelper.updateObject;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

/**
 * Base instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registration</a>, which
 * can be extended for type-specific implementations (i.e. Social Agent Registration, Application Registration).
 */
@Getter
public abstract class CRUDAgentRegistration extends CRUDResource {

    URL registeredBy;
    URL registeredWith;
    OffsetDateTime registeredAt;
    OffsetDateTime updatedAt;
    URL registeredAgent;
    URL accessGrantUrl;

    /**
     * Construct a new {@link CRUDAgentRegistration}
     * @param url URL of the {@link CRUDAgentRegistration}
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    protected CRUDAgentRegistration(URL url, DataFactory dataFactory) throws SaiException {
        super(url, dataFactory, false);
        this.jsonLdContext = buildRemoteJsonLdContext(AGENT_REGISTRATION_CONTEXT);
    }

    /**
     * Set the social agent that registered the agent registration.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
     * @param socialAgentUrl URL of the social agent that added the registration
     */
    public void setRegisteredBy(URL socialAgentUrl) {
        Objects.requireNonNull(socialAgentUrl, "Must provide the social agent who registered the agent registration");
        this.registeredBy = socialAgentUrl;
        updateObject(this.resource, REGISTERED_BY, socialAgentUrl);
    }

    /**
     * Set the application that registered the agent registration.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
     * @param applicationUrl URL of the application that was use to add the registration
     */
    public void setRegisteredWith(URL applicationUrl) {
        Objects.requireNonNull(applicationUrl, "Must provide the application used to register the agent registration");
        this.registeredWith = applicationUrl;
        updateObject(this.resource, REGISTERED_WITH, applicationUrl);
    }

    /**
     * Set the time that the agent registration was created.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
     * @param registeredAt when the agent registration was created
     */
    public void setRegisteredAt(OffsetDateTime registeredAt) {
        Objects.requireNonNull(registeredAt, "Must provide the time that the agent registration was created");
        this.registeredAt = registeredAt;
        updateObject(this.resource, REGISTERED_AT, registeredAt);
    }

    /**
     * Set the time that the agent registration was updated.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
     * @param updatedAt when the agent registration was updated
     */
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        Objects.requireNonNull(updatedAt, "Must provide the time that the agent registration was updated");
        this.updatedAt = updatedAt;
        updateObject(this.resource, UPDATED_AT, updatedAt);
    }

    /**
     * Set the registered agent that is the subject of the agent registration
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
     * @param agentUrl URL of the agent that was registered
     */
    public void setRegisteredAgent(URL agentUrl) {
        Objects.requireNonNull(agentUrl, "Must provide the agent to register");
        this.registeredAgent = agentUrl;
        updateObject(this.resource, REGISTERED_AGENT, agentUrl);
    }

    /**
     * Set the access grant for the agent registration
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registrations</a>
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#access-grant">Access Grants</a>
     * @param accessGrantUrl URL of the access grant
     */
    public void setAccessGrant(URL accessGrantUrl) {
        Objects.requireNonNull(accessGrantUrl, "Must provide the access grant for the agent registration");
        this.registeredAgent = accessGrantUrl;
        updateObject(this.resource, HAS_ACCESS_GRANT, accessGrantUrl);
    }
}

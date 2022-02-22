package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.Getter;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.Objects;

import static com.janeirodigital.sai.core.enums.ContentType.TEXT_TURTLE;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

/**
 * Modifiable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agent-registration">Social Agent Registration</a>.
 */
@Getter
public class SocialAgentRegistration extends AgentRegistration {

    URL reciprocalRegistration;

    /**
     * Construct a new {@link SocialAgentRegistration}
     * @param url URL of the {@link SocialAgentRegistration}
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    public SocialAgentRegistration(URL url, DataFactory dataFactory) throws SaiException {
        super(url, dataFactory);
    }

    /**
     * Builder used by a {@link DataFactory} to construct and initialize a {@link SocialAgentRegistration}.
     * If a Jena <code>resource</code> is provided and there is already a {@link SocialAgentRegistration}
     * at the provided <code>url</code>, the graph of the provided resource will be used. The remote graph
     * will not be updated until {@link #update()} is called.
     * @param url URL of the {@link SocialAgentRegistration} to build
     * @param dataFactory {@link DataFactory} to assign
     * @param contentType {@link ContentType} to use for read / write
     * @param resource Optional Jena Resource to populate the resource graph
     * @return {@link SocialAgentRegistration}
     * @throws SaiException
     */
    public static SocialAgentRegistration build(URL url, DataFactory dataFactory, ContentType contentType, Resource resource) throws SaiException {
        SocialAgentRegistration registration = new SocialAgentRegistration(url, dataFactory);
        registration.contentType = contentType;
        if (resource != null) {
            registration.resource = resource;
            registration.dataset = resource.getModel();
        }
        registration.bootstrap();
        return registration;
    }

    /**
     * Calls {@link #build(URL, DataFactory, ContentType, Resource)} to construct a {@link SocialAgentRegistration} with
     * no Jena resource provided and the specified content type (e.g. JSON-LD).
     * @param url URL of the {@link SocialAgentRegistration} to build
     * @param dataFactory {@link DataFactory} to assign
     * @return {@link SocialAgentRegistration}
     * @throws SaiException
     */
    public static SocialAgentRegistration build(URL url, DataFactory dataFactory, ContentType contentType) throws SaiException {
        return build(url, dataFactory, contentType, null);
    }

    /**
     * Calls {@link #build(URL, DataFactory, ContentType, Resource)} to construct a {@link SocialAgentRegistration} with
     * no Jena resource provided and the default content type.
     * @param url URL of the {@link SocialAgentRegistration} to build
     * @param dataFactory {@link DataFactory} to assign
     * @return {@link SocialAgentRegistration}
     * @throws SaiException
     */
    public static SocialAgentRegistration build(URL url, DataFactory dataFactory) throws SaiException {
        return build(url, dataFactory, TEXT_TURTLE, null);
    }

    /**
     * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agent-registration">Reciprocal Registration</a>
     * that <code>registeredAgent</code> maintains for the social agent that owns the agent registry.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar-registry">Agent Registry</a>
     * @param registrationUrl URL of the reciprocal social agent registration
     */
    public void setReciprocalRegistration(URL registrationUrl) {
        Objects.requireNonNull(registrationUrl, "Must provide a reciprocal registration for the social agent registration");
        this.reciprocalRegistration = registrationUrl;
        updateObject(this.resource, RECIPROCAL_REGISTRATION, registrationUrl);
    }

    /**
     * Bootstraps the {@link SocialAgentRegistration}. If a Jena Resource was provided, it will
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
                this.resource = getNewResourceForType(this.url, SOCIAL_AGENT_REGISTRATION);
                this.dataset = resource.getModel();
            }
        }
    }

    /**
     * Populates the {@link SocialAgentRegistration} instance with required and optional fields
     * @throws SaiException If any required fields cannot be populated, or other exceptional conditions
     */
    private void populate() throws SaiException {
        try {
            this.registeredBy = getRequiredUrlObject(this.resource, REGISTERED_BY);
            this.registeredWith = getRequiredUrlObject(this.resource, REGISTERED_WITH);
            this.registeredAt = getRequiredDateTimeObject(this.resource, REGISTERED_AT);
            this.updatedAt = getRequiredDateTimeObject(this.resource, UPDATED_AT);
            this.registeredAgent = getRequiredUrlObject(this.resource, REGISTERED_AGENT);
            this.reciprocalRegistration = getUrlObject(this.resource, RECIPROCAL_REGISTRATION);
            this.accessGrantUrl = getUrlObject(this.resource, HAS_ACCESS_GRANT);
        } catch (SaiNotFoundException ex) {
            throw new SaiException("Failed to load social agent registration " + this.url + ": " + ex.getMessage());
        }
    }

}

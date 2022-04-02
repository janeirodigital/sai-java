package com.janeirodigital.sai.core.agents;

import com.janeirodigital.sai.core.authorizations.AccessGrant;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.resources.ReadableResource;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.sai.rdfutils.SaiRdfNotFoundException;
import lombok.Getter;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Objects;

import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.rdfutils.RdfUtils.*;

/**
 * Readable abstract base instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registration</a>, which
 * can be extended for type-specific readable implementations (i.e. Social Agent Registration, Application Registration).
 */
@Getter
public abstract class ReadableAgentRegistration extends ReadableResource {

    protected final URI registeredBy;
    protected final URI registeredWith;
    protected final OffsetDateTime registeredAt;
    protected final OffsetDateTime updatedAt;
    protected final URI registeredAgent;
    protected final URI accessGrantUri;
    /**
     * Construct a {@link ReadableAgentRegistration} instance from the provided <code>uri</code>.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    protected ReadableAgentRegistration(Builder<?> builder) throws SaiException {
        super(builder);
        this.registeredBy = builder.registeredBy;
        this.registeredWith = builder.registeredWith;
        this.registeredAt = builder.registeredAt;
        this.updatedAt = builder.updatedAt;
        this.registeredAgent = builder.registeredAgent;
        this.accessGrantUri = builder.accessGrantUri;
    }

    /**
     * Indicates whether or not there is an {@link AccessGrant} linked
     * to the registration
     * @return true when there is an access grant
     */
    public boolean hasAccessGrant() {
        return this.accessGrantUri != null;
    }

    /**
     * Abstract builder for {@link ReadableAgentRegistration} instances (used by subclasses). See
     * {@link ReadableApplicationRegistration} and {@link ReadableSocialAgentRegistration}
     */
    protected abstract static class Builder <T extends ReadableResource.Builder<T>> extends ReadableResource.Builder<T>  {

        protected URI registeredBy;
        protected URI registeredWith;
        protected OffsetDateTime registeredAt;
        protected OffsetDateTime updatedAt;
        protected URI registeredAgent;
        protected URI accessGrantUri;

        /**
         * Initialize builder with <code>uri</code> and <code>saiSession</code>
         * @param uri URI of the {@link ReadableAgentRegistration} to build
         * @param saiSession {@link SaiSession} to assign
         */
        protected Builder(URI uri, SaiSession saiSession) { super(uri, saiSession); }

        /**
         * Populates the common fields of the {@link ReadableAgentRegistration} based on the associated Jena resource.
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
    }
    
}

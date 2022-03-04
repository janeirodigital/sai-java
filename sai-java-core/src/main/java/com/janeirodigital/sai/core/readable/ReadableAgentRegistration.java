package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Objects;

import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

/**
 * Readable abstract base instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registration</a>, which
 * can be extended for type-specific readable implementations (i.e. Social Agent Registration, Application Registration).
 */
@Getter
public abstract class ReadableAgentRegistration extends ReadableResource {

    protected final URL registeredBy;
    protected final URL registeredWith;
    protected final OffsetDateTime registeredAt;
    protected final OffsetDateTime updatedAt;
    protected final URL registeredAgent;
    protected final URL accessGrantUrl;
    /**
     * Construct a {@link ReadableAgentRegistration} instance from the provided <code>url</code>.
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
        this.accessGrantUrl = builder.accessGrantUrl;
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
     * Abstract builder for {@link ReadableAgentRegistration} instances (used by subclasses). See
     * {@link ReadableApplicationRegistration} and {@link ReadableSocialAgentRegistration}
     */
    protected abstract static class Builder <T extends ReadableResource.Builder<T>> extends ReadableResource.Builder<T>  {

        protected URL registeredBy;
        protected URL registeredWith;
        protected OffsetDateTime registeredAt;
        protected OffsetDateTime updatedAt;
        protected URL registeredAgent;
        protected URL accessGrantUrl;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link ReadableAgentRegistration} to build
         * @param saiSession {@link SaiSession} to assign
         */
        protected Builder(URL url, SaiSession saiSession) { super(url, saiSession); }

        /**
         * Populates the common fields of the {@link ReadableAgentRegistration} based on the associated Jena resource.
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
    }
    
}

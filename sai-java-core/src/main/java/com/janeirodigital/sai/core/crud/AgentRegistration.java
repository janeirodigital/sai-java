package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.UUID;

import static com.janeirodigital.sai.core.helpers.HttpHelper.addChildToUrlPath;

/**
 * Abstract base instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#ar">Agent Registration</a>, which
 * can be extended for type-specific implementations (i.e. Social Agent Registration, Application Registration).
 */
@Getter
public abstract class AgentRegistration extends CRUDResource {

    private final URL registeredBy;
    private final URL registeredWith;
    private final OffsetDateTime registeredAt;
    private final OffsetDateTime updatedAt;
    private final URL registeredAgent;
    private final URL accessGrantUrl;

    /**
     * Construct a new {@link AgentRegistration}
     * @param url URL of the {@link AgentRegistration}
     * @param saiSession {@link SaiSession} to assign
     * @throws SaiException
     */
    protected AgentRegistration(URL url, SaiSession saiSession, Model dataset, Resource resource, ContentType contentType,
                                URL registeredBy, URL registeredWith, OffsetDateTime registeredAt, OffsetDateTime updatedAt,
                                URL registeredAgent, URL accessGrantUrl) throws SaiException {
        super(url, saiSession, false);
        this.dataset = dataset;
        this.resource = resource;
        this.contentType = contentType;
        this.registeredBy = registeredBy;
        this.registeredWith = registeredWith;
        this.registeredAt = registeredAt;
        this.updatedAt = updatedAt;
        this.registeredAgent = registeredAgent;
        this.accessGrantUrl = accessGrantUrl;
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
        return this.accessGrantUrl == null;
    }

}

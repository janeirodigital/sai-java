package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.Getter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.time.OffsetDateTime;

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
     * @param url URL to generate the {@link ReadableAgentRegistration} from
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    protected ReadableAgentRegistration(URL url, DataFactory dataFactory, Model dataset, Resource resource, ContentType contentType,
                                        URL registeredBy, URL registeredWith, OffsetDateTime registeredAt, OffsetDateTime updatedAt,
                                        URL registeredAgent, URL accessGrantUrl) throws SaiException {
        super(url, dataFactory, true);
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
     * Indicates whether or not there is an {@link com.janeirodigital.sai.core.immutable.AccessGrant} linked
     * to the registration
     * @return true when there is an access grant
     */
    public boolean hasAccessGrant() {
        return this.accessGrantUrl == null;
    }
    
}

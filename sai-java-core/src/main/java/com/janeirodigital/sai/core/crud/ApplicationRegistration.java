package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.Getter;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;

import static com.janeirodigital.sai.core.helpers.HttpHelper.DEFAULT_RDF_CONTENT_TYPE;
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
    public ApplicationRegistration(URL url, DataFactory dataFactory) throws SaiException {
        super(url, dataFactory);
    }

    /**
     * Builder used by a {@link DataFactory} to construct and initialize a {@link ApplicationRegistration}.
     * If a Jena <code>resource</code> is provided and there is already a {@link ApplicationRegistration}
     * at the provided <code>url</code>, the graph of the provided resource will be used. The remote graph
     * will not be updated until {@link #update()} is called.
     * @param url URL of the {@link ApplicationRegistration} to build
     * @param dataFactory {@link DataFactory} to assign
     * @param contentType {@link ContentType} to use for read / write
     * @param resource Optional Jena Resource to populate the resource graph
     * @return {@link ApplicationRegistration}
     * @throws SaiException
     */
    public static ApplicationRegistration build(URL url, DataFactory dataFactory, ContentType contentType, Resource resource) throws SaiException {
        ApplicationRegistration registration = new ApplicationRegistration(url, dataFactory);
        registration.contentType = contentType;
        if (resource != null) {
            registration.resource = resource;
            registration.dataset = resource.getModel();
        }
        registration.bootstrap();
        return registration;
    }

    /**
     * Calls {@link #build(URL, DataFactory, ContentType, Resource)} to construct a {@link ApplicationRegistration} with
     * no Jena resource provided and the specified content type (e.g. JSON-LD).
     * @param url URL of the {@link ApplicationRegistration} to build
     * @param dataFactory {@link DataFactory} to assign
     * @return {@link ApplicationRegistration}
     * @throws SaiException
     */
    public static ApplicationRegistration build(URL url, DataFactory dataFactory, ContentType contentType) throws SaiException {
        return build(url, dataFactory, contentType, null);
    }

    /**
     * Calls {@link #build(URL, DataFactory, ContentType, Resource)} to construct a {@link ApplicationRegistration} with
     * no Jena resource provided and the default content type.
     * @param url URL of the {@link ApplicationRegistration} to build
     * @param dataFactory {@link DataFactory} to assign
     * @return {@link ApplicationRegistration}
     * @throws SaiException
     */
    public static ApplicationRegistration build(URL url, DataFactory dataFactory) throws SaiException {
        return build(url, dataFactory, DEFAULT_RDF_CONTENT_TYPE, null);
    }

    /**
     * Bootstraps the {@link ApplicationRegistration}. If a Jena Resource was provided, it will
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
                this.resource = getNewResourceForType(this.url, APPLICATION_REGISTRATION);
                this.dataset = resource.getModel();
            }
        }
    }

    /**
     * Populates the {@link ApplicationRegistration} instance with required and optional fields
     * @throws SaiException If any required fields cannot be populated, or other exceptional conditions
     */
    private void populate() throws SaiException {
        try {
            this.registeredBy = getRequiredUrlObject(this.resource, REGISTERED_BY);
            this.registeredWith = getRequiredUrlObject(this.resource, REGISTERED_WITH);
            this.registeredAt = getRequiredDateTimeObject(this.resource, REGISTERED_AT);
            this.updatedAt = getRequiredDateTimeObject(this.resource, UPDATED_AT);
            this.registeredAgent = getRequiredUrlObject(this.resource, REGISTERED_AGENT);
            this.accessGrantUrl = getUrlObject(this.resource, HAS_ACCESS_GRANT);
        } catch (SaiNotFoundException ex) {
            throw new SaiException("Failed to load application registration " + this.url + ": " + ex.getMessage());
        }
    }

}

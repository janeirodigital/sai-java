package com.janeirodigital.sai.core.factories;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.crud.CRUDApplicationProfile;
import com.janeirodigital.sai.core.crud.CRUDRegistrySet;
import com.janeirodigital.sai.core.crud.CRUDSocialAgentProfile;
import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import lombok.Getter;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;

/**
 * Extends the base {@link DataFactory} with classes only relevant or allowed for use by
 * trusted applications (e.g. Authorization Agent, Provisioning Services, etc.)
 */
@Getter
public class TrustedDataFactory extends DataFactory {

    /**
     * Initialize a trusted data factory with the provided authorized session and
     * http client, which will be used for subsequent operations by the factory.
     * @param clientFactory Initialized {@link HttpClientFactory}
     * @param authorizedSession {@link AuthorizedSession} with credentials used for access to protected resources
     */
    public TrustedDataFactory(AuthorizedSession authorizedSession, HttpClientFactory clientFactory) {
        super(authorizedSession, clientFactory);
    }

    /**
     * Get a crud version of an Application Profile - {@link CRUDApplicationProfile}
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#app">Solid - Application Profile</a>
     * @param url URL of the {@link CRUDApplicationProfile}
     * @return {@link CRUDApplicationProfile}
     * @throws SaiException
     */
    public CRUDApplicationProfile getCRUDApplicationProfile(URL url) throws SaiException {
        return CRUDApplicationProfile.build(url, this);
    }

    /**
     * Get a crud version of an Application Profile - {@link CRUDApplicationProfile} - and populate it with the
     * provided Jena <code>resource</code>. If there is already a {@link CRUDApplicationProfile} at the provided
     * <code>url</code>, the graph of the provided resource will be used. The remote graph
     * will not be updated until update is called.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#app">Solid - Application Profile</a>
     * @param url URL of the {@link CRUDApplicationProfile}
     * @param resource Jena Resource to populate with
     * @return {@link CRUDApplicationProfile}
     * @throws SaiException
     */
    public CRUDApplicationProfile getCRUDApplicationProfile(URL url, Resource resource) throws SaiException {
        return CRUDApplicationProfile.build(url, this, resource);
    }

    /**
     * Get a crud version of a Social Agent Profile - {@link CRUDSocialAgentProfile}
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Solid - Social Agent Profile</a>
     * @param url URL of the {@link CRUDSocialAgentProfile}
     * @return {@link CRUDSocialAgentProfile}
     * @throws SaiException
     */
    public CRUDSocialAgentProfile getCRUDSocialAgentProfile(URL url) throws SaiException {
        return CRUDSocialAgentProfile.build(url, this);
    }

    /**
     * Get a crud version of a Social Agent Profile - {@link CRUDSocialAgentProfile} that will be remotely accessed
     * via the provided <code>contentType</code>.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Solid - Social Agent Profile</a>
     * @param url URL of the {@link CRUDSocialAgentProfile}
     * @param contentType {@link ContentType} to use
     * @return {@link CRUDSocialAgentProfile}
     * @throws SaiException
     */
    public CRUDSocialAgentProfile getCRUDSocialAgentProfile(URL url, ContentType contentType) throws SaiException {
        return CRUDSocialAgentProfile.build(url, this, contentType);
    }

    /**
     * Get a crud version of a Social Agent Profile - {@link CRUDSocialAgentProfile} that will be remotely accessed
     * via the provided <code>contentType</code>. If there is already a {@link CRUDSocialAgentProfile} at the provided
     * <code>url</code>, the graph of the provided resource will be used. The remote graph
     * will not be updated until update is called.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Solid - Social Agent Profile</a>
     * @param url URL of the {@link CRUDSocialAgentProfile}
     * @param contentType {@link ContentType} to use
     * @param resource Jena Resource to populate with
     * @return {@link CRUDSocialAgentProfile}
     * @throws SaiException
     */
    public CRUDSocialAgentProfile getCRUDSocialAgentProfile(URL url, ContentType contentType, Resource resource) throws SaiException {
        return CRUDSocialAgentProfile.build(url, this, contentType, resource);
    }

    /**
     * Get a crud version of a Registry Set for a Social Agent - {@link CRUDRegistrySet}
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#datamodel-registry-set">Solid - Registry Set</a>
     * @param url URL of the {@link CRUDRegistrySet}
     * @return {@link CRUDRegistrySet}
     * @throws SaiException
     */
    public CRUDRegistrySet getCRUDRegistrySet(URL url) throws SaiException {
        return CRUDRegistrySet.build(url, this);
    }

    /**
     * Get a crud version of a Registry Set - {@link CRUDRegistrySet} that will be remotely accessed
     * via the provided <code>contentType</code>.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#datamodel-registry-set">Solid - Registry Set</a>
     * @param url URL of the {@link CRUDRegistrySet}
     * @param contentType {@link ContentType} to use
     * @return {@link CRUDRegistrySet}
     * @throws SaiException
     */
    public CRUDRegistrySet getCRUDRegistrySet(URL url, ContentType contentType) throws SaiException {
        return CRUDRegistrySet.build(url, this, contentType);
    }

    /**
     * Get a crud version of a Registry Set - {@link CRUDRegistrySet} that will be remotely accessed
     * via the provided <code>contentType</code>. If there is already a {@link CRUDRegistrySet} at the provided
     * <code>url</code>, the graph of the provided resource will be used. The remote graph
     * will not be updated until update is called.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#datamodel-registry-set">Solid - Registry Set</a>
     * @param url URL of the {@link CRUDRegistrySet}
     * @param contentType {@link ContentType} to use
     * @param resource Jena Resource to populate with
     * @return {@link CRUDRegistrySet}
     * @throws SaiException
     */
    public CRUDRegistrySet getCRUDRegistrySet(URL url, ContentType contentType, Resource resource) throws SaiException {
        return CRUDRegistrySet.build(url, this, contentType, resource);
    }

}

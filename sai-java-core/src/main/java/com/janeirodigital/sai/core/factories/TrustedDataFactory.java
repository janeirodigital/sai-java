package com.janeirodigital.sai.core.factories;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.crud.*;
import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.immutable.AccessConsent;
import com.janeirodigital.sai.core.immutable.AccessGrant;
import lombok.Getter;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;

/**
 * Extends the base {@link DataFactory} with classes only relevant or allowed for use by
 * trusted applications (e.g. Authorization Agent, Provisioning Services, etc.)
 */
@Getter
public class TrustedDataFactory extends DataFactory {

    RegistrySet registrySet;

    /**
     * Initialize a trusted data factory with the provided authorized session and
     * http client, which will be used for subsequent operations by the factory.
     * @param clientFactory Initialized {@link HttpClientFactory}
     * @param authorizedSession {@link AuthorizedSession} with credentials used for access to protected resources
     */
    public TrustedDataFactory(AuthorizedSession authorizedSession, HttpClientFactory clientFactory) throws SaiNotFoundException, SaiException {
        super(authorizedSession, clientFactory);
    }

    /**
     * Get a crud version of an Application Profile - {@link ApplicationProfile}
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#app">Solid - Application Profile</a>
     * @param url URL of the {@link ApplicationProfile}
     * @return {@link ApplicationProfile}
     * @throws SaiException
     */
    public ApplicationProfile getApplicationProfile(URL url) throws SaiException, SaiNotFoundException {
        return ApplicationProfile.get(url, this);
    }

    /**
     * Get a crud version of a Social Agent Profile - {@link SocialAgentProfile}
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Solid - Social Agent Profile</a>
     * @param url URL of the {@link SocialAgentProfile}
     * @return {@link SocialAgentProfile}
     * @throws SaiException
     */
    public SocialAgentProfile getSocialAgentProfile(URL url) throws SaiException {
        return SocialAgentProfile.build(url, this);
    }

    /**
     * Get a crud version of a Social Agent Profile - {@link SocialAgentProfile} that will be remotely accessed
     * via the provided <code>contentType</code>.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Solid - Social Agent Profile</a>
     * @param url URL of the {@link SocialAgentProfile}
     * @param contentType {@link ContentType} to use
     * @return {@link SocialAgentProfile}
     * @throws SaiException
     */
    public SocialAgentProfile getSocialAgentProfile(URL url, ContentType contentType) throws SaiException {
        return SocialAgentProfile.build(url, this, contentType);
    }

    /**
     * Get a crud version of a Social Agent Profile - {@link SocialAgentProfile} that will be remotely accessed
     * via the provided <code>contentType</code>. If there is already a {@link SocialAgentProfile} at the provided
     * <code>url</code>, the graph of the provided resource will be used. The remote graph
     * will not be updated until update is called.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Solid - Social Agent Profile</a>
     * @param url URL of the {@link SocialAgentProfile}
     * @param contentType {@link ContentType} to use
     * @param resource Jena Resource to populate with
     * @return {@link SocialAgentProfile}
     * @throws SaiException
     */
    public SocialAgentProfile getSocialAgentProfile(URL url, ContentType contentType, Resource resource) throws SaiException {
        return SocialAgentProfile.build(url, this, contentType, resource);
    }

    /**
     * Get a crud version of a Registry Set for a Social Agent - {@link RegistrySet}
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#datamodel-registry-set">Solid - Registry Set</a>
     * @param url URL of the {@link RegistrySet}
     * @return {@link RegistrySet}
     * @throws SaiException
     */
    public RegistrySet getRegistrySet(URL url) throws SaiException, SaiNotFoundException {
        return RegistrySet.get(url, this);
    }

    /**
     * Get a crud version of a Registry Set - {@link RegistrySet} that will be remotely accessed
     * via the provided <code>contentType</code>.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#datamodel-registry-set">Solid - Registry Set</a>
     * @param url URL of the {@link RegistrySet}
     * @param contentType {@link ContentType} to use
     * @return {@link RegistrySet}
     * @throws SaiException
     */
    public RegistrySet getRegistrySet(URL url, ContentType contentType) throws SaiException, SaiNotFoundException {
        return RegistrySet.get(url, this, contentType);
    }

    /**
     * Get a crud version of a Agent Registry for a Social Agent - {@link AgentRegistry}
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar-registry">Solid - Agent Registry</a>
     * @param url URL of the {@link AgentRegistry}
     * @return {@link AgentRegistry}
     * @throws SaiException
     */
    public AgentRegistry getAgentRegistry(URL url) throws SaiException {
        return AgentRegistry.build(url, this);
    }

    /**
     * Get a crud version of a Agent Registry - {@link AgentRegistry} that will be remotely accessed
     * via the provided <code>contentType</code>.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar-registry">Solid - Agent Registry</a>
     * @param url URL of the {@link AgentRegistry}
     * @param contentType {@link ContentType} to use
     * @return {@link AgentRegistry}
     * @throws SaiException
     */
    public AgentRegistry getAgentRegistry(URL url, ContentType contentType) throws SaiException {
        return AgentRegistry.build(url, this, contentType);
    }

    /**
     * Get a crud version of a Agent Registry - {@link AgentRegistry} that will be remotely accessed
     * via the provided <code>contentType</code>. If there is already a {@link AgentRegistry} at the provided
     * <code>url</code>, the graph of the provided resource will be used. The remote graph
     * will not be updated until update is called.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar-registry">Solid - Agent Registry</a>
     * @param url URL of the {@link AgentRegistry}
     * @param contentType {@link ContentType} to use
     * @param resource Jena Resource to populate with
     * @return {@link AgentRegistry}
     * @throws SaiException
     */
    public AgentRegistry getAgentRegistry(URL url, ContentType contentType, Resource resource) throws SaiException {
        return AgentRegistry.build(url, this, contentType, resource);
    }

    /**
     * Get a crud version of a Social Agent Registration for a Social Agent - {@link SocialAgentRegistration}
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agent-registration">Solid - Social Agent Registration</a>
     * @param url URL of the {@link SocialAgentRegistration}
     * @return {@link SocialAgentRegistration}
     * @throws SaiException
     */
    public SocialAgentRegistration getSocialAgentRegistration(URL url) throws SaiException {
        return SocialAgentRegistration.build(url, this);
    }

    /**
     * Get a crud version of a Social Agent Registration - {@link SocialAgentRegistration} that will be remotely accessed
     * via the provided <code>contentType</code>.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agent-registration">Solid - Social Agent Registration</a>
     * @param url URL of the {@link SocialAgentRegistration}
     * @param contentType {@link ContentType} to use
     * @return {@link SocialAgentRegistration}
     * @throws SaiException
     */
    public SocialAgentRegistration getSocialAgentRegistration(URL url, ContentType contentType) throws SaiException {
        return SocialAgentRegistration.build(url, this, contentType);
    }

    /**
     * Get a crud version of a Social Agent Registration - {@link SocialAgentRegistration} that will be remotely accessed
     * via the provided <code>contentType</code>. If there is already a {@link SocialAgentRegistration} at the provided
     * <code>url</code>, the graph of the provided resource will be used. The remote graph
     * will not be updated until update is called.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agent-registration">Solid - Social Agent Registration</a>
     * @param url URL of the {@link SocialAgentRegistration}
     * @param contentType {@link ContentType} to use
     * @param resource Jena Resource to populate with
     * @return {@link SocialAgentRegistration}
     * @throws SaiException
     */
    public SocialAgentRegistration getSocialAgentRegistration(URL url, ContentType contentType, Resource resource) throws SaiException {
        return SocialAgentRegistration.build(url, this, contentType, resource);
    }

    /**
     * Get a crud version of a Application Registration for a Social Agent - {@link ApplicationRegistration}
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#application-registration">Solid - Application Registration</a>
     * @param url URL of the {@link ApplicationRegistration}
     * @return {@link ApplicationRegistration}
     * @throws SaiException
     */
    public ApplicationRegistration getApplicationRegistration(URL url) throws SaiException {
        return ApplicationRegistration.build(url, this);
    }

    /**
     * Get a crud version of a Application Registration - {@link ApplicationRegistration} that will be remotely accessed
     * via the provided <code>contentType</code>.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#application-registration">Solid - Application Registration</a>
     * @param url URL of the {@link ApplicationRegistration}
     * @param contentType {@link ContentType} to use
     * @return {@link ApplicationRegistration}
     * @throws SaiException
     */
    public ApplicationRegistration getApplicationRegistration(URL url, ContentType contentType) throws SaiException {
        return ApplicationRegistration.build(url, this, contentType);
    }

    /**
     * Get a crud version of a Application Registration - {@link ApplicationRegistration} that will be remotely accessed
     * via the provided <code>contentType</code>. If there is already a {@link ApplicationRegistration} at the provided
     * <code>url</code>, the graph of the provided resource will be used. The remote graph
     * will not be updated until update is called.
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#application-registration">Solid - Application Registration</a>
     * @param url URL of the {@link ApplicationRegistration}
     * @param contentType {@link ContentType} to use
     * @param resource Jena Resource to populate with
     * @return {@link ApplicationRegistration}
     * @throws SaiException
     */
    public ApplicationRegistration getApplicationRegistration(URL url, ContentType contentType, Resource resource) throws SaiException {
        return ApplicationRegistration.build(url, this, contentType, resource);
    }

    public AccessConsent getAccessConsent(URL url) throws SaiNotFoundException, SaiException {
        return AccessConsent.get(url, this);
    }

    public AccessGrant getAccessGrant(URL url) throws SaiNotFoundException, SaiException {
        return AccessGrant.get(url, this);
    }

}

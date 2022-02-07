package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.Getter;

import java.net.URL;
import java.util.List;

import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.SolidTermsVocabulary.SOLID_OIDC_ISSUER;

/**
 * Publicly readable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Social Agent</a>
 * profile which is also cross-pollinated with other terms from the Solid ecosystem
 */
@Getter
public class ReadableSocialAgentProfile extends ReadableResource {

    URL registrySetUrl;
    URL authorizationAgentUrl;
    URL accessInboxUrl;
    List<URL> oidcIssuerUrls;

    /**
     * Construct a {@link ReadableSocialAgentProfile} instance from the provided <code>url</code>.
     * @param url URL to generate the {@link ReadableSocialAgentProfile} from
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    public ReadableSocialAgentProfile(URL url, DataFactory dataFactory) throws SaiException {
        super(url, dataFactory, true);
    }

    /**
     * Primary mechanism used to construct and bootstrap a {@link ReadableSocialAgentProfile} from
     * the provided <code>url</code>.
     * @param url URL to generate the {@link ReadableSocialAgentProfile} from
     * @param dataFactory {@link DataFactory} to assign
     * @return {@link ReadableSocialAgentProfile}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ReadableSocialAgentProfile build(URL url, DataFactory dataFactory) throws SaiException, SaiNotFoundException {
        ReadableSocialAgentProfile profile = new ReadableSocialAgentProfile(url, dataFactory);
        profile.bootstrap();
        return profile;
    }

    /**
     * Bootstraps the {@link ReadableSocialAgentProfile} by fetching the resource
     * and populating the corresponding fields from the data contained in the
     * graph.
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    private void bootstrap() throws SaiException, SaiNotFoundException {
        this.fetchData();
        // populate the social agent profile fields
        this.oidcIssuerUrls = getRequiredUrlObjects(this.resource, SOLID_OIDC_ISSUER);
        this.authorizationAgentUrl = getRequiredUrlObject(this.resource, HAS_AUTHORIZATION_AGENT);
        this.registrySetUrl = getRequiredUrlObject(this.resource, HAS_REGISTRY_SET);
        this.accessInboxUrl = getRequiredUrlObject(this.resource, HAS_ACCESS_INBOX);
    }

}

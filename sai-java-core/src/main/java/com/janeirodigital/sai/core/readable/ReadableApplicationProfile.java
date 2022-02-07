package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.Getter;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;
import static com.janeirodigital.sai.core.vocabularies.SolidOidcVocabulary.*;

/**
 * Publicly readable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#app">Application</a>,
 * profile which is also cross-pollinated with the
 * <a href="https://solid.github.io/solid-oidc/#clientids-document">Client Identifier Document</a>
 * from Solid-OIDC.
 */
@Getter
public class ReadableApplicationProfile extends ReadableResource {

    private String name;
    private String description;
    private URL authorUrl;
    private URL logoUrl;
    private List<URL> accessNeedGroupUrls;
    // Solid-OIDC specific
    private List<URL> redirectUrls;
    private URL clientUrl;
    private URL tosUrl;
    private List<String> scopes;
    private List<String> grantTypes;
    private List<String> responseTypes;
    private Integer defaultMaxAge;
    private boolean requireAuthTime;

    /**
     * Construct a {@link ReadableApplicationProfile} instance from the provided <code>url</code>.
     * @param url URL to generate the {@link ReadableApplicationProfile} from
     * @param dataFactory {@link DataFactory} to assign
     * @throws SaiException
     */
    public ReadableApplicationProfile(URL url, DataFactory dataFactory) throws SaiException {
        super(url, dataFactory, true);
    }

    /**
     * Primary mechanism used to construct and bootstrap a {@link ReadableApplicationProfile} from
     * the provided <code>url</code>.
     * @param url URL to generate the {@link ReadableApplicationProfile} from
     * @param dataFactory {@link DataFactory} to assign
     * @return {@link ReadableApplicationProfile}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ReadableApplicationProfile build(URL url, DataFactory dataFactory) throws SaiException, SaiNotFoundException {
        ReadableApplicationProfile profile = new ReadableApplicationProfile(url, dataFactory);
        profile.bootstrap();
        return profile;
    }

    /**
     * Bootstraps the {@link ReadableApplicationProfile} by fetching the resource
     * and populating the corresponding fields from the data contained in the
     * graph.
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    private void bootstrap() throws SaiException, SaiNotFoundException {
        this.fetchData();
        try {
            this.name = getRequiredStringObject(this.resource, SOLID_OIDC_CLIENT_NAME);
            this.description = getRequiredStringObject(this.resource, APPLICATION_DESCRIPTION);
            this.authorUrl = getRequiredUrlObject(this.resource, APPLICATION_AUTHOR);
            this.logoUrl = getRequiredUrlObject(this.resource, SOLID_OIDC_LOGO_URI);
            this.accessNeedGroupUrls = getRequiredUrlObjects(this.resource, HAS_ACCESS_NEED_GROUP);
            // Solid-OIDC specific
            this.redirectUrls = getRequiredUrlObjects(this.resource, SOLID_OIDC_REDIRECT_URIS);
            this.clientUrl = getUrlObject(this.resource, SOLID_OIDC_CLIENT_URI);
            this.tosUrl = getUrlObject(this.resource, SOLID_OIDC_TOS_URI);
            this.scopes = Arrays.asList(getRequiredStringObject(this.resource, SOLID_OIDC_SCOPE).split(" "));
            this.grantTypes = getRequiredStringObjects(this.resource, SOLID_OIDC_GRANT_TYPES);
            this.responseTypes = getRequiredStringObjects(this.resource, SOLID_OIDC_RESPONSE_TYPES);
            this.defaultMaxAge = getIntegerObject(this.resource, SOLID_OIDC_DEFAULT_MAX_AGE);
            this.requireAuthTime = getBooleanObject(this.resource, SOLID_OIDC_REQUIRE_AUTH_TIME);
        } catch (SaiNotFoundException ex) {
            throw new SaiException("Failed to load application profile " + this.url + ": " + ex.getMessage());
        }
    }

}

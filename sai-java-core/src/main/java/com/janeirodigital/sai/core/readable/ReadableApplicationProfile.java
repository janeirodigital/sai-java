package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.factories.DataFactory;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import lombok.Getter;

import java.net.URL;
import java.util.List;

import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

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
    private URL thumbnailUrl;
    private List<URL> accessNeedGroupUrls;

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
        // populate the application profile fields
        this.name = getRequiredStringObject(this.resource, APPLICATION_NAME);
        this.description = getRequiredStringObject(this.resource, APPLICATION_DESCRIPTION);
        this.authorUrl = getRequiredUrlObject(this.resource, APPLICATION_AUTHOR);
        this.thumbnailUrl = getUrlObject(this.resource, APPLICATION_THUMBNAIL);
        this.accessNeedGroupUrls = getUrlObjects(this.resource, HAS_ACCESS_NEED_GROUP);
    }

}

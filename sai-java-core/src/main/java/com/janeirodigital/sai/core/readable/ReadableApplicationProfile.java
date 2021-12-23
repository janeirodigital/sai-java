package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.DataFactory;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import lombok.Getter;

import java.net.URL;
import java.util.List;

import static com.janeirodigital.sai.core.helpers.RdfHelper.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.*;

@Getter
public class ReadableApplicationProfile extends ReadableResource {

    private String name;
    private String description;
    private URL authorUrl;
    private URL thumbnailUrl;
    private List<URL> accessNeedGroupUrls;

    public ReadableApplicationProfile(URL url, DataFactory dataFactory) {
        super(url, dataFactory);
    }

    private void bootstrap() throws SaiException, SaiNotFoundException {
        this.fetchData();
        // populate the application profile fields
        this.name = getRequiredStringObject(this.resource, APPLICATION_NAME);
        this.description = getRequiredStringObject(this.resource, APPLICATION_DESCRIPTION);
        this.authorUrl = getRequiredUrlObject(this.resource, APPLICATION_AUTHOR);
        this.thumbnailUrl = getUrlObject(this.resource, APPLICATION_THUMBNAIL);
        this.accessNeedGroupUrls = getUrlObjects(this.resource, HAS_ACCESS_NEED_GROUP);
    }

    public static ReadableApplicationProfile build(URL url, DataFactory dataFactory) throws SaiException, SaiNotFoundException {
        ReadableApplicationProfile profile = new ReadableApplicationProfile(url, dataFactory);
        profile.bootstrap();
        return profile;
    }

}

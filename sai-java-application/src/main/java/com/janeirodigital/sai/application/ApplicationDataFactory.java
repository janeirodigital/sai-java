package com.janeirodigital.sai.application;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.factories.DataFactory;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import lombok.Getter;

import java.util.Objects;

/**
 * Data factory specifically for use by client applications
 * <a href="https://solid.github.io/data-interoperability-panel/specification/">as defined</a>
 * by the Solid Application Interoperability specification
 */
@Getter
public class ApplicationDataFactory extends DataFactory {

    private final Application application;

    public ApplicationDataFactory(Application application, AuthorizedSession authorizedSession, HttpClientFactory clientFactory) {
        super(authorizedSession, clientFactory);
        Objects.requireNonNull(application, "Must provide an application to initialize application data factory");
        this.application = application;
    }

}

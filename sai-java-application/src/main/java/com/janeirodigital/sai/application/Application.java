package com.janeirodigital.sai.application;

import com.janeirodigital.sai.core.authentication.AuthorizedSessionAccessor;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import lombok.Getter;

import java.net.URL;
import java.util.Objects;

/**
 * Represents an Application
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#app">as defined</a>
 * by the Solid Application Interoperability Specification.
 *
 * <blockquote>An Application is a software-based Agent that
 * a Social Agent uses to access, manipulate, and manage the data in their control, as
 * well as the data they've been granted access to.</blockquote>
 */
@Getter
public class Application {

    private final URL id ;
    private final HttpClientFactory clientFactory;
    private final boolean validateSsl;
    private final boolean validateShapeTrees;

    /**
     * Construct a SAI compatible Application
     * @param id <a href="https://solid.github.io/data-interoperability-panel/specification/#app">URL identifier</a> of the Application
     * @param validateSsl Ignores SSL validation errors when false
     * @param validateShapeTrees Intercept requests and perform client-side shape tree validation when true
     */
    public Application(URL id, boolean validateSsl, boolean validateShapeTrees, boolean refreshTokens, AuthorizedSessionAccessor sessionAccessor) throws SaiException {
        Objects.requireNonNull(id,"Must provide an application identifier to initialize an application");
        this.id = id;
        this.validateSsl = validateSsl;
        this.validateShapeTrees = validateShapeTrees;
        this.clientFactory = new HttpClientFactory(validateSsl, validateShapeTrees, refreshTokens, sessionAccessor);
    }

}

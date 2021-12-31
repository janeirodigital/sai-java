package com.janeirodigital.sai.application;

import com.janeirodigital.sai.core.ApplicationFactory;
import com.janeirodigital.sai.core.DataFactory;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.URL;

/**
 * Represents an Application
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#app">as defined</a>
 * by the Solid Application Interoperability Specification.
 *
 * <blockquote>An Application is a software-based Agent that
 * a Social Agent uses to access, manipulate, and manage the data in their control, as
 * well as the data they've been granted access to.</blockquote>
 */
@AllArgsConstructor @Getter
public class Application {

    private final URL id ;

    private DataFactory dataFactory;
    private HttpClientFactory clientFactory;

    public Application(URL id, boolean validateSsl, boolean validateShapeTrees) {
        this.id = id;
        this.clientFactory = new HttpClientFactory(validateSsl, validateShapeTrees);
        this.dataFactory = new ApplicationFactory(clientFactory);
    }

}

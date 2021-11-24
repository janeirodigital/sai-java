package com.janeirodigital.sai.application;

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

    private String name ;
    private String description ;
    private URL author ;

    /**
     * Temporary method to test test infrastructure
     * @return Concatenated string of name and description
     */
    String getFullDescription() {
        return this.name + " - " + this.description;
    }

}

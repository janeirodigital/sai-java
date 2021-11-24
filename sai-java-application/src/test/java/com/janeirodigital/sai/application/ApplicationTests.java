package com.janeirodigital.sai.application;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

class ApplicationTests {

    private final String PROJECTRON_NAME = "Projectron";
    private final String PROJECTRON_DESCRIPTION = "Manage applications with ease";
    private final URL PROJECTRON_URL = new URL("https://projectron.example");

    ApplicationTests() throws MalformedURLException { }

    @Test
    @DisplayName("Construct an Application")
    void testFullDescription() throws MalformedURLException {
        Application app = new Application(PROJECTRON_NAME, PROJECTRON_DESCRIPTION, PROJECTRON_URL);
        Assertions.assertEquals(PROJECTRON_NAME + " - " + PROJECTRON_DESCRIPTION, app.getFullDescription());
    }

}

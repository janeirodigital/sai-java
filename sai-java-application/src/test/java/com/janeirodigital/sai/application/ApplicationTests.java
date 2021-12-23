package com.janeirodigital.sai.application;

import com.janeirodigital.sai.core.DataFactory;
import com.janeirodigital.sai.core.exceptions.SaiException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

class ApplicationTests {

    private final String PROJECTRON_NAME = "Projectron";
    private final String PROJECTRON_DESCRIPTION = "Manage applications with ease";
    private final URL PROJECTRON_URL = new URL("https://projectron.example/id");

    ApplicationTests() throws MalformedURLException { }

    @Test
    @DisplayName("Initialize an Application")
    void initializeApplication() throws MalformedURLException, SaiException {
        Application app = new Application(PROJECTRON_URL, false, true);
        DataFactory factory = app.getDataFactory();
        Assertions.assertEquals(PROJECTRON_URL, app.getId());
    }

}

package com.janeirodigital.sai.application;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;

public class ApplicationTests {

    @Test
    @DisplayName("Construct an Application")
    void constructApplication() throws MalformedURLException {
        Application app = new Application("Projectron", "Manage applications with ease", new URL("https://projectron.example"));
        Assertions.assertEquals("Projectron", app.getName());
    }

}

package com.janeirodigital.sai.core.tests.fixtures;

import okhttp3.mockwebserver.MockWebServer;

import java.net.MalformedURLException;
import java.net.URL;

public class MockWebServerHelper {

    public static URL toUrl(MockWebServer server, String path) {
        try {
            return new URL(server.url(path).toString());
        } catch (MalformedURLException ex) {
            throw new IllegalStateException("Can't convert dispatcher request path to URL");
        }
    }

}

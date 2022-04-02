package com.janeirodigital.sai.core.http;

import com.janeirodigital.sai.core.exceptions.SaiException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

public class UrlUtils {

    private UrlUtils() { }

    /**
     * Converts a string to a URL
     * @param urlString String to convert to URL
     * @return Converted URL
     * @throws SaiException
     */
    public static URL stringToUrl(String urlString) throws SaiException {
        Objects.requireNonNull(urlString, "Must provide a string to convert");
        try {
            return new URL(urlString);
        } catch (MalformedURLException ex) {
            throw new SaiException("Unable to convert String to URL", ex);
        }
    }

    /**
     * Coverts a URI to a URL
     * @param uri URI to convert
     * @return Converted URL
     * @throws SaiException
     */
    public static URL uriToUrl(URI uri) throws SaiException {
        Objects.requireNonNull(uri, "Must provide a URI to convert");
        try {
            return uri.toURL();
        } catch (MalformedURLException ex) {
            throw new SaiException("Unable to convert URI to URL", ex);
        }
    }

}

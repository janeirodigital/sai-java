package com.janeirodigital.sai.core.http;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.httputils.SaiHttpException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class UrlUtils {

    private UrlUtils() { }

    /**
     * Converts a string to a URL
     * @param urlString String to convert to URL
     * @return Converted URL
     * @throws SaiHttpException
     */
    public static URL stringToUrl(String urlString) throws SaiException {
        Objects.requireNonNull(urlString, "Must provide a string to convert");
        try {
            return new URL(urlString);
        } catch (MalformedURLException ex) {
            throw new SaiException("Unable to convert String to URL", ex);
        }
    }

}

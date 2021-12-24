package com.janeirodigital.sai.core.enums;

/**
 * Enumerated list of applicable HTTP headers
 */
public enum HttpHeader {
    ACCEPT("Accept"),
    AUTHORIZATION("Authorization"),
    CONTENT_TYPE("Content-Type"),
    LINK("Link"),
    IF_NONE_MATCH("If-None-Match"),
    LOCATION("Location"),
    SLUG("Slug");

    public String getValue() {
        return this.value;
    }

    private final String value;

    HttpHeader(String value) {
        this.value = value;
    }
}

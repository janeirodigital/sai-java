package com.janeirodigital.sai.core.enums;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enumerated list of applicable HTTP headers
 */
public enum HttpHeader {
    ACCEPT("Accept"),
    AUTHORIZATION("Authorization"),
    DPOP("DPoP"),
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

    private static final Map<String,HttpHeader> ENUM_MAP;

    static {
        Map<String,HttpHeader> map = new ConcurrentHashMap<>();
        for (HttpHeader instance : HttpHeader.values()) {
            map.put(instance.getValue().toLowerCase(),instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    public static HttpHeader get(String name) {
        return ENUM_MAP.get(name.toLowerCase());
    }

}

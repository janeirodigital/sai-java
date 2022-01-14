package com.janeirodigital.sai.core.enums;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enumerated list of HTTP methods
 */
public enum HttpMethod {
    POST("POST"),
    PUT("PUT"),
    PATCH("PATCH"),
    DELETE("DELETE"),
    GET("GET"),
    HEAD("HEAD"),
    OPTIONS("OPTIONS"),
    TRACE("TRACE"),
    CONNECT("CONNECT");

    public String getValue() {
        return this.value;
    }

    private final String value;

    HttpMethod(String value) {
        this.value = value;
    }

    private static final Map<String,HttpMethod> ENUM_MAP;

    static {
        Map<String,HttpMethod> map = new ConcurrentHashMap<>();
        for (HttpMethod instance : HttpMethod.values()) {
            map.put(instance.getValue().toLowerCase(),instance);
        }
        ENUM_MAP = Collections.unmodifiableMap(map);
    }

    public static HttpMethod get(String name) {
        return ENUM_MAP.get(name.toLowerCase());
    }
    
}

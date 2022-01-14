package com.janeirodigital.sai.core.enums;

/**
 * Enumerated list of Access Token Types
 */
public enum AccessTokenType {
    BEARER("Bearer"),
    DPOP("DPoP");

    public String getValue() {
        return this.value;
    }

    private final String value;

    AccessTokenType(String value) {
        this.value = value;
    }
}

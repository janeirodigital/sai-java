package com.janeirodigital.sai.core.authorization;

import lombok.Getter;

import java.util.Objects;

/**
 * General representation of an AccessToken.
 */
@Getter
public class AccessToken {

    protected final String value;

    /**
     * Construct a new AccessToken
     * @param value Value of the token itself
     */
    protected AccessToken(String value) {
        Objects.requireNonNull(value, "Must provide an access token value");
        this.value = value;
    }

}

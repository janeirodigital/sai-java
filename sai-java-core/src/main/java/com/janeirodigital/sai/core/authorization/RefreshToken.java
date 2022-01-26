package com.janeirodigital.sai.core.authorization;

import lombok.Getter;

import java.util.Objects;

/**
 * General representation of a RefreshToken.
 */
@Getter
public class RefreshToken {

    protected final String value;

    /**
     * Construct a new RefreshToken
     * @param value Value of the token itself
     */
    protected RefreshToken(String value) {
        Objects.requireNonNull(value, "Must provide a refresh token value");
        this.value = value;
    }

}

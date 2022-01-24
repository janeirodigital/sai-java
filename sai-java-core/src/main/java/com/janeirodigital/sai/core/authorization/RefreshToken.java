package com.janeirodigital.sai.core.authorization;

import lombok.Getter;

import java.util.Objects;

/**
 * General representation of a RefreshToken used when an access token expires and a
 * new one is needed from a given {@link AccessTokenProvider}.
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

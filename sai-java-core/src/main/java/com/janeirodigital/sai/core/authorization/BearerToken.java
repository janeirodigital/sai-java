package com.janeirodigital.sai.core.authorization;

import java.util.Objects;

import static com.janeirodigital.sai.core.enums.AccessTokenType.BEARER;

public class BearerToken extends AccessToken {

    public BearerToken(String value, AccessTokenProvider provider) {
        super(BEARER, value, provider);
        Objects.requireNonNull(value, "Must provide a value for the Bearer token");
    }

}

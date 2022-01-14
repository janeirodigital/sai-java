package com.janeirodigital.sai.core.authorization;

import com.janeirodigital.sai.core.enums.AccessTokenType;
import lombok.Getter;

import java.util.Objects;

@Getter
public abstract class AccessToken {

    protected final AccessTokenProvider provider;
    protected final AccessTokenType type;
    protected final String value;

    protected AccessToken(AccessTokenType type, String value, AccessTokenProvider provider) {
        Objects.requireNonNull(type, "Must provide an access token type");
        Objects.requireNonNull(type, "Must provide an access token value");
        Objects.requireNonNull(type, "Must provide an access token provider");
        this.type = type;
        this.value = value;
        this.provider = provider;
    }

}

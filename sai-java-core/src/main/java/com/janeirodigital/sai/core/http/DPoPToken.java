package com.janeirodigital.sai.core.http;

import java.util.Objects;

import static com.janeirodigital.sai.core.enums.AccessTokenType.DPOP;

public class DPoPToken extends AccessToken {

    public DPoPToken(String value, AccessTokenProvider provider) {
        super(DPOP, value, provider);
        Objects.requireNonNull(value, "Must provide a value for the DPoP token");
    }

}

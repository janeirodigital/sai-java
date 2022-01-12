package com.janeirodigital.sai.core.http;

import com.janeirodigital.sai.core.exceptions.SaiException;
import lombok.Setter;
import lombok.Synchronized;

/**
 * Manage the {@link AccessTokenProvider} in use by sai-js, which provides
 * access tokens to use for requests to protected resources
 */
public class AccessTokenProviderManager {

    @Setter(onMethod_={@Synchronized})
    private static AccessTokenProvider provider;

    private AccessTokenProviderManager() { }

    /**
     * Get the {@link AccessTokenProvider} set via {@link #setProvider(AccessTokenProvider)}
     * @return AccessTokenProvider
     * @throws SaiException when no AccessTokenProvider has been set
     */
    @Synchronized
    public static AccessTokenProvider getProvider() throws SaiException {
        if (provider == null) { throw new SaiException("Must provide a valid access token provider for requests to protected resources"); }
        return provider;
    }

}

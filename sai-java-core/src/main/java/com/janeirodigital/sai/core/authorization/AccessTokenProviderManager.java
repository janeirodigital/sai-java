package com.janeirodigital.sai.core.authorization;

import com.janeirodigital.sai.core.exceptions.SaiException;
import lombok.Setter;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;

/**
 * Manage the {@link AccessTokenProvider} in use by sai-js, which provides
 * access tokens to use for requests to protected resources
 */
@Slf4j
public class AccessTokenProviderManager {

    /**
     * Set the access token provider that can be retrieved via {@link #getProvider()}
     */
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
        if (provider == null) { log.warn("Must provide a valid access token provider for requests to protected resources"); }
        return provider;
    }

}

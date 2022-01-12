package com.janeirodigital.sai.core.http;

import java.io.IOException;

/**
 * An interface for providers of access tokens and refresh tokens
 */
public interface AccessTokenProvider {
    /**
     * Get an access token that can be used in subsequent authorized HTTP requests
     * @return Access Token or null if the token cannot be obtained
     */
    public String getAccessToken() throws IOException;

    /**
     * Refreshes the access token and returns it. This call must be made synchronously.
     * @return Refreshed access token or null if the token cannot be refreshed
     */
    public String refreshAccessToken() throws IOException;
}

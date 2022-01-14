package com.janeirodigital.sai.core.http;

import com.janeirodigital.sai.core.enums.HttpMethod;
import com.janeirodigital.sai.core.exceptions.SaiException;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * An interface for providers of access tokens and refresh tokens
 */
public interface AccessTokenProvider {
    /**
     * Get an access token that can be used in subsequent authorized HTTP requests
     * @return Access Token or null if the token cannot be obtained
     */
    AccessToken getAccessToken() throws IOException;

    /**
     * Refreshes the access token and returns it. This call must be made synchronously.
     * @return Refreshed access token or null if the token cannot be refreshed
     */
    AccessToken refreshAccessToken() throws IOException;

    /**
     * Provides a map of HTTP Authorization headers that can be supplied in a subsequent
     * request via the provided <code>method</code> and <code>url</code>. Must include the
     * appropriate authentication scheme for the Authorization Header (e.g. Bearer, DPoP)
     * @param accessToken to get Authorization headers for
     * @return Map of HTTP headers
     */
    Map<String, String> getAuthorizationHeaders(AccessToken accessToken, HttpMethod method, URL url) throws SaiException;


}

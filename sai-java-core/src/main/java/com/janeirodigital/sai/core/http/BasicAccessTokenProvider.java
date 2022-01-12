package com.janeirodigital.sai.core.http;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation of {@link AccessTokenProvider} that can be used in the
 * absence of another option. Since this basic implementation uses the
 * client_credentials flow, a refresh_token isn't actually needed. Consequently,
 * calls to refresh the token simply make another client_credentials token request.
 */
@Slf4j
public class BasicAccessTokenProvider implements AccessTokenProvider {

    private final String clientIdentifier;
    private final String clientSecret;
    private final URI tokenEndpoint;
    private final List<String> scopes;
    private AccessToken accessToken;

    public BasicAccessTokenProvider(String clientIdentifier, String clientSecret, URI tokenEndpoint, List<String> scopes) {
        Objects.requireNonNull(clientIdentifier, "Must provide a client identifier for authentication");
        Objects.requireNonNull(clientSecret, "Must provide a client secret for authentication");
        Objects.requireNonNull(tokenEndpoint, "Must provide a token endpoint for authentication");
        this.clientIdentifier = clientIdentifier;
        this.clientSecret = clientSecret;
        this.tokenEndpoint = tokenEndpoint;
        if (scopes == null) { this.scopes = new ArrayList<>(); } else { this.scopes = scopes; }
        this.accessToken = null;
    }

    public BasicAccessTokenProvider(String clientIdentifier, String clientSecret, URI tokenEndpoint) {
        this(clientIdentifier, clientSecret, tokenEndpoint, null);
    }

    /**
     * Provide an existing access token if it has already been obtained, otherwise obtain one.
     * @return AccessToken
     */
    @Override
    public String getAccessToken() throws IOException {
        if (this.accessToken == null) { this.accessToken = obtainToken(); }
        return this.accessToken.toString();
    }

    /**
     * Refreshes an existing access token. This call should be made synchronously.
     * @return Refreshed AccessToken
     */
    @Override
    public String refreshAccessToken() throws IOException {
        this.accessToken = obtainToken();
        return this.accessToken.toString();
    }

    /**
     * Get an Access Token via a client_credentials grant flow, using the client identifier
     * and secret provided on construction of the BasicAccessTokenProvider.
     * @return AccessToken
     */
    private synchronized AccessToken obtainToken() throws IOException {

        AuthorizationGrant clientGrant = new ClientCredentialsGrant();

        ClientID clientID = new ClientID(this.clientIdentifier);
        Secret secret = new Secret(this.clientSecret);
        ClientAuthentication clientAuth = new ClientSecretBasic(clientID, secret);

        TokenRequest request;
        TokenResponse response;
        if (!this.scopes.isEmpty()) {
            String[] scopeArray = this.scopes.toArray(new String[0]);
            Scope scope = new Scope(scopeArray);
            request = new TokenRequest(tokenEndpoint, clientAuth, clientGrant, scope);
        } else {
            request = new TokenRequest(tokenEndpoint, clientAuth, clientGrant);
        }

        try {
            response = TokenResponse.parse(request.toHTTPRequest().send());
            if (!response.indicatesSuccess()) { throw new IOException(response.toErrorResponse().toString()); }
        } catch (IOException | ParseException ex) {
            throw new IOException("Request failed to token endpoint " + this.tokenEndpoint + ": " + ex.getMessage());
        }

        AccessTokenResponse successResponse = response.toSuccessResponse();
        AccessToken newToken = successResponse.getTokens().getAccessToken();
        log.debug("Access token received from {}", this.tokenEndpoint);

        return newToken;
    }

}

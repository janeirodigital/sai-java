package com.janeirodigital.sai.core.authorization;

import com.janeirodigital.sai.core.enums.HttpMethod;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.*;

import static com.janeirodigital.sai.core.enums.AccessTokenType.BEARER;
import static com.janeirodigital.sai.core.enums.HttpHeader.AUTHORIZATION;

/**
 * Default implementation of {@link AccessTokenProvider} that can be used in the
 * absence of another option. This provider uses Bearer tokens in a
 * client_credentials flow. A refresh_token isn't actually needed. Consequently,
 * calls to refresh the token simply make another client_credentials token request.
 */
@Slf4j @Getter
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
     * @return AccessToken (in generic sai-java format)
     */
    @Override
    public AccessToken getAccessToken() throws IOException {
        if (this.accessToken == null) { this.accessToken = translate(obtainToken()); }
        return this.accessToken;
    }

    /**
     * Refreshes an existing access token. This call should be made synchronously.
     * @return Refreshed AccessToken (in generic sai-java format)
     */
    @Override
    public AccessToken refreshAccessToken() throws IOException {
        this.accessToken = translate(obtainToken());
        return this.accessToken;
    }

    /**
     * Returns an immutable map with a single entry for the HTTP Authorization header
     * and the provided access token as a Bearer token
     * @param accessToken to get Authorization headers for
     * @param method Not used for Bearer tokens - can be null
     * @param url Not used for Bearer tokens - can be null
     * @return Map with single HTTP Authorization Header populated
     */
    @Override
    public Map<String, String> getAuthorizationHeaders(AccessToken accessToken, HttpMethod method, URL url) throws SaiException {
        Objects.requireNonNull(accessToken, "Must provide an access token to get authorization headers");
        return Map.of(AUTHORIZATION.getValue(), "Bearer " + accessToken.getValue());
    }

    /**
     * Get an Access Token via a client_credentials grant flow, using the client identifier
     * and secret provided on construction of the BasicAccessTokenProvider.
     * @return AccessToken (nimbus native format)
     */
    protected synchronized com.nimbusds.oauth2.sdk.token.AccessToken obtainToken() throws IOException {

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
        com.nimbusds.oauth2.sdk.token.AccessToken newToken = successResponse.getTokens().getAccessToken();
        log.debug("Access token received from {}", this.tokenEndpoint);

        return newToken;
    }

    /**
     * Translates a nimbus native AccessToken into the generic sai-java format
     * @param nimbusToken Nimbus AccessToken
     * @return AccessToken in sai-java format
     */
    private AccessToken translate(com.nimbusds.oauth2.sdk.token.AccessToken nimbusToken) {
        return new AccessToken(BEARER, nimbusToken.toString(), this);
    }

}

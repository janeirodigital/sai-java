package com.janeirodigital.sai.core.authorization;

import com.janeirodigital.sai.core.enums.HttpMethod;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getOIDCProviderConfiguration;
import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.translateAccessToken;
import static com.janeirodigital.sai.core.enums.HttpHeader.AUTHORIZATION;

/**
 * Implements of the client credentials flow to establish and use
 * a traditional Bearer access token by a given Application.
 */
@Getter
public class ClientCredentialsSession implements AuthorizedSession {

    private final String clientIdentifier;
    private final String clientSecret;
    private final URL oidcProviderId;
    private final OIDCProviderMetadata oidcProviderMetadata;
    private final Scope scope;
    private AccessToken accessToken;

    private ClientCredentialsSession(String clientIdentifier, String clientSecret, URL oidcProviderId,
                                     OIDCProviderMetadata oidcProviderMetadata, Scope scope, AccessToken accessToken) {
        Objects.requireNonNull(clientIdentifier, "Must provide an OIDC client identifier to construct a client credentials session");
        Objects.requireNonNull(clientSecret, "Must provide an OIDC client secret to construct a client credentials session");
        Objects.requireNonNull(oidcProviderId, "Must provide an OIDC provider identifier to construct a client credentials session");
        Objects.requireNonNull(oidcProviderMetadata, "Must provide OIDC provider metadata to construct a client credentials session");
        Objects.requireNonNull(scope, "Must provide scope to construct a client credentials session");
        Objects.requireNonNull(accessToken, "Must provide an access token to construct a client credentials session");
        this.clientIdentifier = clientIdentifier;
        this.clientSecret = clientSecret;
        this.oidcProviderId = oidcProviderId;
        this.oidcProviderMetadata = oidcProviderMetadata;
        this.scope = scope;
        this.accessToken = accessToken;
    }

    /**
     * Generates a map of HTTP Authorization headers that can be use to make authorized requests
     * using the session. Client credentials uses a Bearer token in a single authorization header.
     * @param method not needed - can be null for client credentials
     * @param url not needed - can be null for client credentials
     * @return Map of HTTP Authorization headers
     */
    @Override
    public Map<String, String> toHttpHeaders(HttpMethod method, URL url) {
        Objects.requireNonNull(this.accessToken, "Cannot generate authorization headers for an uninitialized access token");
        return Map.of(AUTHORIZATION.getValue(), "Bearer " + this.accessToken.getValue());
    }

    /**
     * "Refreshes" the session via another client credentials token request. A client credentials
     * flow doesn't require refresh tokens.
     * @throws SaiException
     */
    @Override
    public void refresh() throws SaiException {
        this.accessToken = obtainToken(this.clientIdentifier, this.clientSecret, this.oidcProviderMetadata, this.scope);
    }

    /**
     * The client credentials flows don't require refresh tokens
     * @return null
     */
    @Override
    public RefreshToken getRefreshToken() { return null; }

    /**
     * This implementation of client credentials flow doesn't incorporate a social agent identity
     * @return null
     */
    @Override
    public URL getSocialAgentId() { return null; }

    /**
     * This implementation of client credentials flow doesn't incorporate an application identity, only
     * the clientIdentifier registered with the oidc provider.
     * @return null
     */
    @Override
    public URL getApplicationId() { return null; }

    /**
     * POSTs a token request to the token endpoint of the oidcProvider using the provided
     * <code>clientIdentifier</code> and <code>clientSecret</code> to authenticate and request
     * the provided <code>scope</code>. Used for both initial token request and refresh (since
     * the client credentials flow doesn't require refresh tokens).
     * @param clientIdentifier client identifier that has been registered with the oidc provider
     * @param clientSecret client secret that has been registered with the oidc provider for the clientIdentifier
     * @param oidcProviderMetadata configure of the oidc provider obtained through discovery
     * @param scope scope of access being requested
     * @return AccessToken
     * @throws SaiException
     */
    protected static AccessToken obtainToken(String clientIdentifier, String clientSecret, OIDCProviderMetadata oidcProviderMetadata, Scope scope) throws SaiException {
        Objects.requireNonNull(clientIdentifier, "Must provide a client identifier to build client credentials session");
        Objects.requireNonNull(clientSecret, "Must provide a client secret to build client credentials session");
        Objects.requireNonNull(scope, "Must provide scope to build client credentials session");
        Objects.requireNonNull(oidcProviderMetadata, "Cannot build client credentials session without OIDC provider metadata");

        AuthorizationGrant clientGrant = new ClientCredentialsGrant();
        ClientID clientID = new ClientID(clientIdentifier);
        Secret secret = new Secret(clientSecret);
        ClientAuthentication clientAuth = new ClientSecretBasic(clientID, secret);
        TokenRequest request = new TokenRequest(oidcProviderMetadata.getTokenEndpointURI(), clientAuth, clientGrant, scope);

        TokenResponse response;
        try {
            response = TokenResponse.parse(request.toHTTPRequest().send());
            if (!response.indicatesSuccess()) { throw new IOException(response.toErrorResponse().toString()); }
        } catch (IOException | ParseException ex) {
            throw new SaiException("Request failed to token endpoint " + oidcProviderMetadata.getTokenEndpointURI() + ": " + ex.getMessage());
        }

        AccessTokenResponse successResponse = response.toSuccessResponse();
        com.nimbusds.oauth2.sdk.token.AccessToken newToken = successResponse.getTokens().getAccessToken();
        return translateAccessToken(newToken);
    }

    /**
     * Builder for {@link ClientCredentialsSession} instances. Requires methods to be called
     * in a particular order to establish the session successfully.<br>
     * <ol>
     *     <li>{@link #setOidcProvider(URL)}</li>
     *     <li>{@link #setClientIdentifier(String)}</li>
     *     <li>{@link #setClientSecret(String)}</li>
     *     <li>{@link #setScope(List)}</li>
     *     <li>{@link #requestToken()}</li>
     *     <li>{@link #build()}</li>
     * </ol>
     */
    @NoArgsConstructor @Getter
    public static class Builder {

        private String clientIdentifier;
        private String clientSecret;
        private URL oidcProviderId;
        private OIDCProviderMetadata oidcProviderMetadata;
        Scope scope;
        private AccessToken accessToken;

        /**
         * Sets the openid connect provider that the client is registered with. Will be
         * checked for validity via .well-known/openid-configuration discovery
         * @param oidcProviderId URL of the oidc provider
         * @return ClientCredentialsSession.Builder
         */
        public Builder setOidcProvider(URL oidcProviderId) throws SaiException {
            Objects.requireNonNull(oidcProviderId, "Must provide an oidc provider URL to build client credentials session");
            this.oidcProviderId = oidcProviderId;
            this.oidcProviderMetadata = getOIDCProviderConfiguration(this.oidcProviderId);
            return this;
        }

        /**
         * Sets the client identifier that will be used to authenticate with the oidc provider.
         * @param clientIdentifier client identifier that has been registered with the oidc provider
         * @return ClientCredentialsSession.Builder
         */
        public Builder setClientIdentifier(String clientIdentifier) {
            Objects.requireNonNull(clientIdentifier, "Must provide a client identifier to build client credentials session");
            this.clientIdentifier = clientIdentifier;
            return this;
        }

        /**
         * Sets the client secret that will be used to authenticate with the oidc provider.
         * @param clientSecret client secret that has been registered with the oidc provider for the clientIdentifier
         * @return ClientCredentialsSession.Builder
         */
        public Builder setClientSecret(String clientSecret) {
            Objects.requireNonNull(clientSecret, "Must provide a client secret to build client credentials session");
            this.clientSecret = clientSecret;
            return this;
        }
        
        /**
         * Sets the authorization scopes to use in the authorization request
         * @param scopes List of scopes to include in request
         * @return ClientCredentialsSession.Builder
         */
        public Builder setScope(List<String> scopes) {
            Objects.requireNonNull(scopes, "Must provide scope to build client credentials session");
            String[] scopeArray = scopes.toArray(new String[0]);
            this.scope = new Scope(scopeArray);
            return this;
        }

        /**
         * Request tokens from the token endpoint of the openid connect provider
         * @return SolidOidcSession.Builder
         * @throws SaiException
         */
        public ClientCredentialsSession.Builder requestToken() throws SaiException {
            Objects.requireNonNull(this.clientIdentifier, "Must provide a client identifier to build client credentials session");
            Objects.requireNonNull(this.clientSecret, "Must provide a client secret to build client credentials session");
            Objects.requireNonNull(this.scope, "Must provide scope to build client credentials session");
            Objects.requireNonNull(this.oidcProviderMetadata, "Cannot request tokens without OIDC provider metadata");
            this.accessToken = obtainToken(this.clientIdentifier, this.clientSecret, this.oidcProviderMetadata, this.scope);
            return this;
        }


        /**
         * Constructs a {@link ClientCredentialsSession} once all of the requisite operations have completed
         * successfully.
         * @return {@link ClientCredentialsSession}
         */
        public ClientCredentialsSession build() {
            Objects.requireNonNull(this.clientIdentifier, "Must provide an OIDC client identifier to build a client credentials session");
            Objects.requireNonNull(this.clientSecret, "Must provide an OIDC client secret to build a client credentials session");
            Objects.requireNonNull(this.oidcProviderId, "Must provide an OIDC provider id to build a client credentials session");
            Objects.requireNonNull(this.oidcProviderMetadata, "Cannot build a client credentials session without OIDC provider metadata");
            Objects.requireNonNull(this.scope, "Must provide scope to build client credentials session");
            Objects.requireNonNull(this.accessToken, "Cannot build a client credentials session without an access token");
            return new ClientCredentialsSession(this.clientIdentifier, this.clientSecret, this.oidcProviderId, this.oidcProviderMetadata, this.scope, this.accessToken);
        }

    }

}

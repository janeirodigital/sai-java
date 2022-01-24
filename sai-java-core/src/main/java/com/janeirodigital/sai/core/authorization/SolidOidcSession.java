package com.janeirodigital.sai.core.authorization;

import com.janeirodigital.sai.core.enums.HttpMethod;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.dpop.DPoPProofFactory;
import com.nimbusds.oauth2.sdk.dpop.DefaultDPoPProofFactory;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.id.State;
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.nimbusds.openid.connect.sdk.Prompt;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import lombok.Getter;
import lombok.NoArgsConstructor;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.*;
import static com.janeirodigital.sai.core.enums.HttpHeader.AUTHORIZATION;
import static com.janeirodigital.sai.core.enums.HttpHeader.DPOP;
import static com.janeirodigital.sai.core.enums.HttpMethod.POST;
import static com.janeirodigital.sai.core.helpers.HttpHelper.uriToUrl;
import static com.janeirodigital.sai.core.helpers.HttpHelper.urlToUri;

/**
 * Implementation of {@link AuthorizedSession} for
 * <a href="https://solid.github.io/solid-oidc/">Solid-OIDC</a>. Must use
 * {@link SolidOidcSession.Builder} for session creation.
 */
@Getter
public class SolidOidcSession implements AuthorizedSession {

    private final URL socialAgentId;
    private final URL applicationId;
    private final URL oidcProviderId;
    private final OIDCProviderMetadata oidcProviderMetadata;
    private AccessToken accessToken;
    private RefreshToken refreshToken;
    private final DPoPProofFactory proofFactory;

    private SolidOidcSession(URL socialAgentId, URL applicationId, URL oidcProviderId, OIDCProviderMetadata oidcProviderMetadata,
                             AccessToken accessToken, RefreshToken refreshToken, DPoPProofFactory proofFactory) {
        Objects.requireNonNull(socialAgentId, "Must provide a Social Agent identifier to construct a Solid OIDC session");
        Objects.requireNonNull(applicationId, "Must provide an application identifier to construct a Solid OIDC session");
        Objects.requireNonNull(oidcProviderId, "Must provide an OIDC provider identifier to construct a Solid OIDC session");
        Objects.requireNonNull(oidcProviderMetadata, "Must provide OIDC provider metadata to construct a Solid OIDC session");
        Objects.requireNonNull(accessToken, "Must provide an access token to construct a Solid OIDC session");
        Objects.requireNonNull(proofFactory, "Must provide a DPoP proof factory to construct a Solid OIDC session");
        this.socialAgentId = socialAgentId;
        this.applicationId = applicationId;
        this.oidcProviderId = oidcProviderId;
        this.oidcProviderMetadata = oidcProviderMetadata;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.proofFactory = proofFactory;
    }

    /**
     * Generates a map of HTTP Authorization headers that can be use to make authorized requests
     * using the session. DPoP requires a proof to be created for each request based on the
     * <code>method</code> and target <code>url</code>.
     * @param method HTTP method of the request
     * @param url Target URL of the request
     * @return Map of HTTP Authorization headers
     * @throws SaiException
     */
    @Override
    public Map<String, String> toHttpHeaders(HttpMethod method, URL url) throws SaiException {
        Objects.requireNonNull(method, "Must provide the HTTP method of the request to generate headers for");
        Objects.requireNonNull(url, "Must provide the target URL of the request to generate headers for");
        Objects.requireNonNull(this.accessToken, "Cannot generate authorization headers for an uninitialized access token");
        SignedJWT proof = getProof(this.proofFactory, method, url);
        return Map.of(AUTHORIZATION.getValue(), "DPoP " + this.accessToken.getValue(), DPOP.getValue(), proof.serialize());
    }

    /**
     * Refreshes the tokens associated with the session. Session must have been established as
     * refreshable upon creation.
     * @throws SaiException
     */
    @Override
    public void refresh() throws SaiException {
        Objects.requireNonNull(this.applicationId, "Must provide an application identifier to use as client id in session refresh");
        Objects.requireNonNull(this.oidcProviderMetadata, "Must provide openid connect provider configuration for session refresh");
        Objects.requireNonNull(this.proofFactory, "Must provide a dpop proof factory for session refresh");
        if (this.refreshToken == null) { throw new SaiException("Unable to refresh a session without a refresh token"); }
        // Construct the grant from the saved refresh token
        com.nimbusds.oauth2.sdk.token.RefreshToken nimbusRefreshToken = new com.nimbusds.oauth2.sdk.token.RefreshToken(this.refreshToken.getValue());
        AuthorizationGrant refreshTokenGrant = new RefreshTokenGrant(nimbusRefreshToken);
        ClientID clientId = new ClientID(this.applicationId.toString());

        // Make the token request
        TokenRequest request = new TokenRequest(this.oidcProviderMetadata.getTokenEndpointURI(), clientId, refreshTokenGrant);
        HTTPRequest httpRequest = request.toHTTPRequest();
        httpRequest.setAccept("*/*");
        SignedJWT proof = getProof(this.proofFactory, POST, httpRequest.getURL());
        httpRequest.setDPoP(proof);

        TokenResponse response;
        try {
            response = TokenResponse.parse(httpRequest.send());
            if (!response.indicatesSuccess()) { throw new SaiException(response.toErrorResponse().toString()); }
        } catch (IOException | ParseException ex) {
            throw new SaiException("Refresh request failed to token endpoint " + this.oidcProviderMetadata.getTokenEndpointURI() + ": " + ex.getMessage());
        }

        Tokens tokens = response.toSuccessResponse().getTokens();
        if (tokens.getDPoPAccessToken() == null) { throw new SaiException("Access token is not DPoP"); }
        this.accessToken = translateAccessToken(tokens.getDPoPAccessToken());
        if (tokens.getRefreshToken() != null) { this.refreshToken = translateRefreshToken(tokens.getRefreshToken()); }
    }

    /**
     * Gets the required DPoP proof that must be created for each request based on the
     * <code>method</code> and target <code>url</code>.
     * @param proofFactory DPoP proof factory
     * @param method HTTP method of the request
     * @param url Target URL of the request
     * @return DPoP proof
     * @throws SaiException
     */
    protected static SignedJWT getProof(DPoPProofFactory proofFactory, HttpMethod method, URL url) throws SaiException {
        Objects.requireNonNull(proofFactory, "Must provide a DPoP proof factory to get DPoP proof");
        Objects.requireNonNull(method, "Must provide the HTTP method of the request to generate DPoP proof");
        Objects.requireNonNull(url, "Must provide the target URL of the request to generate DPoP proof");
        try {
            return proofFactory.createDPoPJWT(method.getValue(), urlToUri(url));
        } catch (JOSEException ex) {
            throw new SaiException("Unable to create DPoP proof: " + ex.getMessage());
        }
    }

    /**
     * Builder for {@link SolidOidcSession} instances. Requires methods to be called
     * in a particular order to establish the Solid-OIDC session successfully.<br>
     * <ol>
     *     <li>{@link #setHttpClient(OkHttpClient)}</li>
     *     <li>{@link #setSocialAgent(URL)}</li>
     *     <li>{@link #setApplication(URL)}</lii>
     *     <li>{@link #setScope(List)}</li>
     *     <li>{@link #setPrompt(Prompt)}</li>
     *     <li>{@link #setRedirect(URL)}</li>
     *     <li>{@link #prepareCodeRequest()}</li>
     *     <li>{@link #getCodeRequestUrl()}</li>
     *     <li>{@link #processCodeResponse(URL)}</li>
     *     <li>{@link #requestTokens()}</li>
     *     <li>{@link #build()}</li>
     * </ol>
     */
    @NoArgsConstructor @Getter
    public static class Builder {

        private URL socialAgentId;
        private URL applicationId;
        private ClientID clientId;
        private URL oidcProviderId;
        private OIDCProviderMetadata oidcProviderMetadata;
        private OkHttpClient httpClient;
        private Scope scope;
        private Prompt prompt;
        private State requestState;
        private URL redirect;
        private CodeVerifier codeVerifier;
        private AuthorizationRequest authorizationRequest;
        private AuthorizationCode authorizationCode;
        private DPoPProofFactory proofFactory;
        private AccessToken accessToken;
        private RefreshToken refreshToken;

        /**
         * Sets an http client that can be used for various operations when building a Solid OIDC session
         * @param httpClient HTTP client to use for requests
         * @return SolidOidcSession.Builder
         */
        public Builder setHttpClient(OkHttpClient httpClient) {
            Objects.requireNonNull(httpClient, "Must provide an http client to build a Solid OIDC session");
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Sets the Social Agent that the Solid-OIDC session will be established on behalf of.
         * Looks up the provided <code>socialAgentId</code> and gets an OIDC Issuer(s) trusted
         * by the social agent, then ensures the issuer has a compatible configuration and stores
         * pertinent information about it.
         * @param socialAgentId URL of the SocialAgent Identity
         * @return SolidOidcSession.Builder
         */
        public Builder setSocialAgent(URL socialAgentId) throws SaiException {
            Objects.requireNonNull(this.httpClient, "Must provide an http client to build a Solid OIDC session");
            Objects.requireNonNull(socialAgentId, "Must provide a Social Agent identifier to build a Solid OIDC session");
            this.socialAgentId = socialAgentId;
            this.oidcProviderId = getOidcIssuerForSocialAgent(this.httpClient, this.socialAgentId);
            this.oidcProviderMetadata = getOIDCProviderConfiguration(this.oidcProviderId);
            // Ensure that the OIDC Provider supports DPoP
            if (this.oidcProviderMetadata.getDPoPJWSAlgs() == null) {
                throw new SaiException("OpenID Provider " + this.oidcProviderId.toString() + "does not support DPoP");
            }
            // Ensure that the OIDC Provider can issue webid and client_id claims
            if (!this.oidcProviderMetadata.getClaims().contains("webid") || !this.oidcProviderMetadata.getClaims().contains("client_id")) {
                throw new SaiException("OpenID Provider " + this.oidcProviderId.toString() + "does not support the necessary claims for solid-oidc");
            }
            return this;
        }

        /**
         * Sets the client Application that will use the Solid-OIDC session. Looks up the provided
         * <code>applicationId</code> to ensure it is available and well-formed.
         * @param applicationId URL of the Client Application Identity
         * @return SolidOidcSession.Builder
         */
        public Builder setApplication(URL applicationId) throws SaiException {
            Objects.requireNonNull(applicationId, "Must provide an application identifier to build a Solid OIDC session");
            Objects.requireNonNull(httpClient, "Must provide an http client to build a Solid OIDC session");
            // TODO - Need to disable this due to JSON-LD11 dependency issues - have to get Jena running latest version across deps
            // getClientIdDocument(this.httpClient, applicationId);
            this.applicationId = applicationId;
            this.clientId = new ClientID(this.applicationId.toString());
            // TODO - should be getting redirect_uris directly from the client id document and not taking as input
            return this;
        }

        /**
         * Sets the authorization scopes to use in the authorization request
         * @param scopes List of scopes to include in request
         * @return SolidOidcSession.Builder
         */
        public Builder setScope(List<String> scopes) {
            Objects.requireNonNull(scopes, "Must provide scopes to set authorization request scope");
            String[] scopeArray = scopes.toArray(new String[0]);
            this.scope = new Scope(scopeArray);
            return this;
        }

        /**
         * Sets the prompt to use in the authorization request
         * @param prompt prompt to use in the authorization request
         * @return SolidOidcSession.Builder
         */
        public Builder setPrompt(Prompt prompt) {
            Objects.requireNonNull(prompt, "Must provide prompt to set prompt for authorization request");
            this.prompt = prompt;
            return this;
        }

        /**
         * Sets the redirect URI to use in the authorization request
         * @param redirect redirection URI to use in the authorization request
         * @return SolidOidcSession.Builder
         */
        public Builder setRedirect(URL redirect) {
            Objects.requireNonNull(redirect, "Must provide redirection endpoint for authorization request");
            this.redirect = redirect;
            return this;
        }

        /**
         * Prepares an Authorization Code Request which should be provided to the Social Agent for review in-browser
         * @return SolidOidcSession.Builder
         */
        public Builder prepareCodeRequest() {
            Objects.requireNonNull(this.clientId, "Must provide a client application for the authorization request");
            Objects.requireNonNull(this.redirect, "Must provide a redirect for the authorization request");
            Objects.requireNonNull(this.scope, "Must provide a scope for the authorization request");
            Objects.requireNonNull(this.oidcProviderMetadata, "Cannot prepare authorization request without OIDC provider metadata");
            this.requestState = new State();
            this.codeVerifier = new CodeVerifier();  // Generate a new random 256 bit code verifier for PKCE
            AuthorizationRequest.Builder requestBuilder = new AuthorizationRequest.Builder(new ResponseType(ResponseType.Value.CODE), this.clientId);
            requestBuilder.scope(scope)
                          .state(this.requestState)
                          .codeChallenge(this.codeVerifier, CodeChallengeMethod.S256)
                          .redirectionURI(urlToUri(this.redirect))
                          .endpointURI(this.oidcProviderMetadata.getAuthorizationEndpointURI());
            if (this.prompt != null) { requestBuilder.prompt(this.prompt); }
            this.authorizationRequest = requestBuilder.build();
            return this;
        }

        /**
         * Returns the prepared authorization code request URL
         * @return URL of the generated authorization code request
         */
        public URL getCodeRequestUrl() throws SaiException {
            Objects.requireNonNull(this.authorizationRequest, "Cannot get code request URL before the code request is prepared");
            return uriToUrl(this.authorizationRequest.toURI());
        }

        /**
         * Process the response to the authorization code request. All of the information
         * needed is fully contained in the URL of the response.
         * @param redirectResponse URL response to the authorization code request
         * @return SolidOidcSession.Builder
         * @throws SaiException
         */
        public Builder processCodeResponse(URL redirectResponse) throws SaiException {
            Objects.requireNonNull(redirectResponse, "Must provide a response to process authorization code response");
            Objects.requireNonNull(this.requestState, "Must provide an original request state to process a valid code response");
            AuthorizationResponse response;
            try {
                // Parse the authorization response from the callback URI
                response = AuthorizationResponse.parse(urlToUri(redirectResponse));
            } catch (ParseException ex) {
                throw new SaiException("Failed to parse response to authorization code request: " + ex.getMessage());
            }
            // Check that the returned state parameter matches the original
            if (!this.requestState.equals(response.getState())) {
                throw new SaiException("Unexpected or tampered contents detected in authorization response");
            }
            if (!response.indicatesSuccess()) {
                // The request was denied or some error occurred
                AuthorizationErrorResponse errorResponse = response.toErrorResponse();
                throw new SaiException("Authorization requested failed: " + errorResponse.getErrorObject());
            }
            AuthorizationSuccessResponse successResponse = response.toSuccessResponse();
            // Retrieve the authorisation code, to be used later to exchange the code for
            // an access token at the token endpoint of the server
            this.authorizationCode = successResponse.getAuthorizationCode();
            return this;
        }

        /**
         * Request tokens from the token endpoint of the openid connect provider
         * @return SolidOidcSession.Builder
         * @throws SaiException
         */
        public Builder requestTokens() throws SaiException {
            Objects.requireNonNull(this.clientId, "Must provide a client application for the token request");
            Objects.requireNonNull(this.oidcProviderMetadata, "Cannot request tokens without OIDC provider metadata");
            Objects.requireNonNull(this.authorizationCode, "Cannot request tokens without authorization code");
            Objects.requireNonNull(this.redirect, "Must provide a redirect for the token request");
            Objects.requireNonNull(this.codeVerifier, "Must provide a code verifier for the token request");
            this.proofFactory = getProofFactory();
            TokenRequest request;
            request = new TokenRequest(this.oidcProviderMetadata.getTokenEndpointURI(),
                                       this.clientId,
                                       new AuthorizationCodeGrant(this.authorizationCode, urlToUri(this.redirect), this.codeVerifier));
            HTTPRequest httpRequest = request.toHTTPRequest();
            httpRequest.setAccept("*/*");
            SignedJWT proof = getProof(this.proofFactory, POST, httpRequest.getURL());
            httpRequest.setDPoP(proof);
            TokenResponse response;
            try {
                response = TokenResponse.parse(httpRequest.send());
                if (!response.indicatesSuccess()) { throw new SaiException(response.toErrorResponse().toString()); }
            } catch (IOException | ParseException ex) {
                throw new SaiException("Request failed to token endpoint " + this.oidcProviderMetadata.getTokenEndpointURI() + ": " + ex.getMessage());
            }
            Tokens tokens = response.toSuccessResponse().getTokens();
            // The access token is not of type DPoP
            if (tokens.getDPoPAccessToken() == null) { throw new SaiException("Access token is not DPoP"); }
            this.accessToken = translateAccessToken(tokens.getDPoPAccessToken());
            if (tokens.getRefreshToken() != null) { this.refreshToken = translateRefreshToken(tokens.getRefreshToken()); }
            return this;
        }

        /**
         * Constructs a {@link SolidOidcSession} once all of the requisite operations have completed
         * successfully.
         * @return {@link SolidOidcSession}
         */
        public SolidOidcSession build() {
            Objects.requireNonNull(this.socialAgentId, "Must provide a Social Agent identifier to build a Solid OIDC session");
            Objects.requireNonNull(this.applicationId, "Must provide an application identifier to build a Solid OIDC session");
            Objects.requireNonNull(this.oidcProviderId, "Must provide an OIDC provider id to build a Solid OIDC session");
            Objects.requireNonNull(this.oidcProviderMetadata, "Cannot build a Solid OIDC session without OIDC provider metadata");
            Objects.requireNonNull(this.accessToken, "Cannot build a Solid OIDC session without an access token");
            Objects.requireNonNull(this.proofFactory, "Cannot build a Solid OIDC session without a proof factory");
            return new SolidOidcSession(this.socialAgentId, this.applicationId, this.oidcProviderId, this.oidcProviderMetadata, this.accessToken, this.refreshToken, this.proofFactory);
        }

        /**
         * Gets a DPoP proof factory that can be used for generate DPoP proofs for requests
         * made by the session.
         * @return DPoPProofFactory
         * @throws SaiException
         */
        private DPoPProofFactory getProofFactory() throws SaiException {
            try {
                ECKey ecJwk = new ECKeyGenerator(Curve.P_256).keyID("1").generate();
                return new DefaultDPoPProofFactory(ecJwk, JWSAlgorithm.ES256);
            } catch (JOSEException ex) {
                throw new SaiException("Failed to initiate DPoP proof generation infrastructure: " + ex.getMessage());
            }
        }

    }

}

package com.janeirodigital.sai.core.http;

import com.janeirodigital.sai.core.enums.HttpMethod;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.dpop.DPoPProofFactory;
import com.nimbusds.oauth2.sdk.dpop.DefaultDPoPProofFactory;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.DPoPAccessToken;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.janeirodigital.sai.core.enums.AccessTokenType.DPOP;
import static com.janeirodigital.sai.core.enums.HttpHeader.AUTHORIZATION;
import static com.janeirodigital.sai.core.helpers.HttpHelper.urlToUri;

/**
 * Basic implementation of {@link AccessTokenProvider} that can be used to provide
 * DPoP access tokens and corresponding proofs per resource request. This provider uses DPoP tokens in a
 * client_credentials flow. A refresh_token isn't actually needed. Consequently,
 * calls to refresh the token simply make another client_credentials token request.
 */
@Slf4j @Getter
public class DPoPAccessTokenProvider implements AccessTokenProvider {

    private final String clientIdentifier;
    private final String clientSecret;
    private final URI tokenEndpoint;
    private final List<String> scopes;
    private AccessToken accessToken;  // This cached access token is in nimbus native format
    private final DPoPProofFactory proofFactory;

    public DPoPAccessTokenProvider(String clientIdentifier, String clientSecret, URI tokenEndpoint, List<String> scopes, Curve curve, DPoPProofFactory proofFactory) throws SaiException {
        Objects.requireNonNull(clientIdentifier, "Must provide a client identifier for authentication");
        Objects.requireNonNull(clientSecret, "Must provide a client secret for authentication");
        Objects.requireNonNull(tokenEndpoint, "Must provide a token endpoint for authentication");
        Objects.requireNonNull(tokenEndpoint, "Must provide a value for elliptic curve key generation");
        this.clientIdentifier = clientIdentifier;
        this.clientSecret = clientSecret;
        this.tokenEndpoint = tokenEndpoint;
        this.accessToken = null;
        if (scopes == null) { this.scopes = new ArrayList<>(); } else { this.scopes = scopes; }
        if (proofFactory == null) {
            try {
                ECKey ecJwk = new ECKeyGenerator(curve).keyID("1").generate();
                this.proofFactory = new DefaultDPoPProofFactory(ecJwk, JWSAlgorithm.ES256);
            } catch (JOSEException ex) {
                throw new SaiException("Failed to initiate DPoP proof generation infrastructure: " + ex.getMessage());
            }
        } else {
            this.proofFactory = proofFactory;
        }
    }

    /**
     * Construct provider with no scopes
     */
    public DPoPAccessTokenProvider(String clientIdentifier, String clientSecret, URI tokenEndpoint, Curve curve) throws SaiException {
        this(clientIdentifier, clientSecret, tokenEndpoint, null, curve, null);
    }

    /**
     * Construct provider with scopes and default curve
     */
    public DPoPAccessTokenProvider(String clientIdentifier, String clientSecret, URI tokenEndpoint, List<String> scopes) throws SaiException {
        this(clientIdentifier, clientSecret, tokenEndpoint, scopes, Curve.P_256, null);
    }

    /**
     * Construct provider with no scopes and default curve
     */
    public DPoPAccessTokenProvider(String clientIdentifier, String clientSecret, URI tokenEndpoint) throws SaiException {
        this(clientIdentifier, clientSecret, tokenEndpoint, null, Curve.P_256, null);
    }

    /**
     * Construct provider with no scopes and custom proof factory
     */
    public DPoPAccessTokenProvider(String clientIdentifier, String clientSecret, URI tokenEndpoint, DPoPProofFactory proofFactory) throws SaiException {
        this(clientIdentifier, clientSecret, tokenEndpoint, null, Curve.P_256, proofFactory);
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
     * Returns an immutable map with an entry for the HTTP Authorization header
     * and the provided access token as a DPoP token, and another for the DPoP proof.
     * @param accessToken to get Authorization headers for
     * @param method HTTP method to be used in request
     * @param url URL target of the request
     * @return Map with HTTP Authorization Headers populated
     */
    @Override
    public Map<String, String> getAuthorizationHeaders(AccessToken accessToken, HttpMethod method, URL url) throws SaiException {
        Objects.requireNonNull(accessToken, "Must provide an access token to get authorization headers");
        Objects.requireNonNull(method, "Must provide an HTTP method to get authorization headers for a DPoP request");
        Objects.requireNonNull(url, "Must provide an HTTP method to get authorization headers for a DPoP request");
        SignedJWT proof;
        try {
            proof = this.proofFactory.createDPoPJWT(method.getValue(), urlToUri(url));
        } catch (JOSEException ex) {
            throw new SaiException("Failed to generate DPoP proof for " + method.getValue() + " to " + url + ": " + ex.getMessage());
        }
        return Map.of(AUTHORIZATION.getValue(), "DPoP " + accessToken.getValue(), DPOP.getValue(), proof.toString());
    }

    /**
     * Get an Access Token via a client_credentials grant flow, using the client identifier
     * and secret provided on construction of the BasicAccessTokenProvider.
     * @return AccessToken (nimbus native format)
     */
    private synchronized com.nimbusds.oauth2.sdk.token.AccessToken obtainToken() throws IOException {

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

        HTTPRequest httpRequest = request.toHTTPRequest();

        try {
            SignedJWT proof = this.proofFactory.createDPoPJWT(httpRequest.getMethod().name(), httpRequest.getURI());
            httpRequest.setDPoP(proof);
        } catch(JOSEException ex) {
            throw new IOException("Unable to create DPoP request: " + ex.getMessage());
        }

        try {
            response = TokenResponse.parse(httpRequest.send());
            if (!response.indicatesSuccess()) { throw new IOException(response.toErrorResponse().toString()); }
        } catch (IOException | ParseException ex) {
            throw new IOException("Request failed to token endpoint " + this.tokenEndpoint + ": " + ex.getMessage());
        }

        AccessTokenResponse successResponse = response.toSuccessResponse();
        DPoPAccessToken dpopToken = successResponse.getTokens().getDPoPAccessToken();
        log.debug("DPoP access token received from {}", this.tokenEndpoint);

        return dpopToken;
    }

    /**
     * Translates a nimbus native AccessToken into the generic sai-java format
     * @param nimbusToken Nimbus AccessToken
     * @return AccessToken in sai-java format
     */
    private AccessToken translate(com.nimbusds.oauth2.sdk.token.AccessToken nimbusToken) {
        return new DPoPToken(nimbusToken.toString(), this);
    }

}

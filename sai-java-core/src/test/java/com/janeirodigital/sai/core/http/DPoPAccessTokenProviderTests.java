package com.janeirodigital.sai.core.http;

import com.janeirodigital.sai.core.enums.HttpMethod;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.oauth2.sdk.dpop.DefaultDPoPProofFactory;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.janeirodigital.sai.core.enums.HttpHeader.AUTHORIZATION;
import static com.janeirodigital.sai.core.enums.HttpHeader.DPOP;
import static com.janeirodigital.sai.core.helpers.HttpHelper.urlToUri;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnPost;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DPoPAccessTokenProviderTests {

    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;
    private static URI tokenEndpoint;
    private final String CLIENT_IDENTIFIER = "test--client";
    private final String CLIENT_SECRET = "test-secret";
    private final List<String> TOKEN_SCOPES = Arrays.asList("profile", "test");
    private final String TOKEN_STRING = "Kz~8mXK1EalYznwH-LC-1fBAo.4Ljp~zsPE_NeO.gxU";
    private final String REFRESH_STRING = "Kz~8mXK1EalYznwH-LC-1fBAo.4Ljp~zsPE_NeO.gxY";

    @BeforeEach
    void beforeEach() {
        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
        // In a given test, the first request to this endpoint will return provider-response, the second will return provider-refresh (a different token)
        mockOnPost(dispatcher, "/token", List.of("http/dpop-token-provider-response-json", "http/dpop-token-provider-refresh-json"));
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
        tokenEndpoint = urlToUri(toUrl(server, "/token"));
        AccessTokenProviderManager.setProvider(null);
    }

    @AfterEach
    void afterEach() { AccessTokenProviderManager.setProvider(null); }

    @Test
    @DisplayName("Initialize DPoP access token provider")
    void initializeDPoPTokenProvider() throws SaiException {
        DPoPAccessTokenProvider dpopProvider = new DPoPAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, tokenEndpoint, TOKEN_SCOPES, Curve.P_256, null);
        assertEquals(CLIENT_IDENTIFIER, dpopProvider.getClientIdentifier());
        assertEquals(CLIENT_SECRET, dpopProvider.getClientSecret());
        assertEquals(tokenEndpoint, dpopProvider.getTokenEndpoint());
        assertEquals(TOKEN_SCOPES, dpopProvider.getScopes());
    }

    @Test
    @DisplayName("Fail to initialize DPoP access token provider")
    void failToInitializeDPoPTokenProvider() {
        Curve badCurve = new Curve("this", "not", "good");
        assertThrows(SaiException.class, () -> { new DPoPAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, tokenEndpoint, badCurve); });
    }

    @Test
    @DisplayName("Supply and get token provider from token provider manager")
    void setAndGetTokenProvider() throws SaiException {
        AccessTokenProvider tokenProvider = new DPoPAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, tokenEndpoint);
        AccessTokenProviderManager.setProvider(tokenProvider);
        assertEquals(tokenProvider, AccessTokenProviderManager.getProvider());
    }

    @Test
    @DisplayName("Fail to get token provider from token provider manager")
    void failToGetTokenProvider() throws SaiException {
        AccessTokenProviderManager.setProvider(null);
        assertNull(AccessTokenProviderManager.getProvider());
    }

    @Test
    @DisplayName("Obtain taken with scopes")
    void getTokenWithScopes() throws IOException, SaiException {
        AccessTokenProvider tokenProvider = new DPoPAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, tokenEndpoint, TOKEN_SCOPES);
        AccessToken accessToken = tokenProvider.getAccessToken();
        assertEquals(TOKEN_STRING, accessToken.getValue());
        // Confirm that getting the token fetches the cached version
        assertEquals(accessToken, tokenProvider.getAccessToken());
    }

    @Test
    @DisplayName("Obtain taken without scopes")
    void getTokenWithoutScopes() throws IOException, SaiException {
        AccessTokenProvider tokenProvider = new DPoPAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, tokenEndpoint, Curve.P_256);
        AccessToken accessToken = tokenProvider.getAccessToken();
        assertEquals(TOKEN_STRING, accessToken.getValue());
    }

    @Test
    @DisplayName("Fail to obtain token from invalid endpoint")
    void failToGetTokenBadEndpoint() throws SaiException {
        AccessTokenProvider tokenProvider = new DPoPAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, URI.create("http://bad.com/token"));
        assertThrows(IOException.class, () -> { tokenProvider.getAccessToken(); });
    }

    @Test
    @DisplayName("Fail to get DPoP proof for token request")
    void failToGetProofForTokenRequest(@Mock DefaultDPoPProofFactory mockProofFactory) throws SaiException, JOSEException {
        AccessTokenProvider tokenProvider = new DPoPAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, tokenEndpoint, mockProofFactory);
        when(mockProofFactory.createDPoPJWT(anyString(), any(URI.class))).thenThrow(JOSEException.class);
        assertThrows(IOException.class, () -> { tokenProvider.getAccessToken(); });
    }

    @Test
    @DisplayName("Get authorization headers for token request")
    void getHeadersForTokenRequest() throws SaiException, IOException {
        AccessTokenProvider tokenProvider = new DPoPAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, tokenEndpoint);
        AccessToken accessToken = tokenProvider.getAccessToken();
        URL url = new URL("https://cool.com/protected");
        Map<String, String> headers = tokenProvider.getAuthorizationHeaders(accessToken, HttpMethod.GET, url);
        assertEquals(2, headers.keySet().size());
        assertEquals("DPoP " + accessToken.getValue(), headers.get(AUTHORIZATION.getValue()));
        assertTrue(headers.containsKey(DPOP.getValue()));
    }

    @Test
    @DisplayName("Fail to get authorization headers for token request")
    void failToGetHeadersForTokenRequest(@Mock DefaultDPoPProofFactory mockProofFactory) throws SaiException, JOSEException {
        AccessTokenProvider tokenProvider = new DPoPAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, tokenEndpoint, mockProofFactory);
        AccessToken token = mock(AccessToken.class);
        when(mockProofFactory.createDPoPJWT(anyString(), any(URI.class))).thenThrow(JOSEException.class);
        assertThrows(SaiException.class, () -> { tokenProvider.getAuthorizationHeaders(token, HttpMethod.PUT, tokenEndpoint.toURL()); });
    }

    @Test
    @DisplayName("Refresh token")
    void refreshToken() throws IOException, SaiException {
        AccessTokenProvider tokenProvider = new DPoPAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, tokenEndpoint);
        AccessToken accessToken = tokenProvider.getAccessToken();
        assertEquals(TOKEN_STRING, accessToken.getValue());
        AccessToken refreshToken = tokenProvider.refreshAccessToken();
        assertEquals(REFRESH_STRING, refreshToken.getValue());
    }

}

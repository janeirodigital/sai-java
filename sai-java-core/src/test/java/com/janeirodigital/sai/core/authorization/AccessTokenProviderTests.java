package com.janeirodigital.sai.core.authorization;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnGet;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnPost;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static org.junit.jupiter.api.Assertions.*;

class AccessTokenProviderTests {

    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;
    private static URL oidcProvider;
    private final String CLIENT_IDENTIFIER = "test-client";
    private final String CLIENT_SECRET = "test-secret";
    private final List<String> TOKEN_SCOPES = Arrays.asList("profile", "test");
    private final String TOKEN_STRING = "MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3";
    private final String REFRESH_STRING = "MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZjJi56";

    @BeforeEach
    void beforeEach() {
        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
        // In a given test, the first request to this endpoint will return provider-response, the second will return provider-refresh (a different token)
        mockOnGet(dispatcher, "/op/.well-known/openid-configuration", "authorization/op-configuration-json");
        mockOnGet(dispatcher, "/badop/.well-known/openid-configuration", "authorization/op-configuration-badtoken-json");
        mockOnPost(dispatcher, "/op/token", List.of("http/token-provider-response-json", "http/token-provider-refresh-json"));
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
        oidcProvider = toUrl(server, "/op/");
        AccessTokenProviderManager.setProvider(null);
    }

    @AfterEach
    void afterEach() { AccessTokenProviderManager.setProvider(null); }

    @Test
    @DisplayName("Initialize basic access token provider")
    void initializeBasicTokenProvider() throws SaiException {
        BasicAccessTokenProvider basicProvider = new BasicAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, oidcProvider, TOKEN_SCOPES);
        assertEquals(CLIENT_IDENTIFIER, basicProvider.getClientIdentifier());
        assertEquals(CLIENT_SECRET, basicProvider.getClientSecret());
        assertEquals(oidcProvider.toString(), basicProvider.getOidcProvider().getIssuer().getValue());
        assertEquals(TOKEN_SCOPES, basicProvider.getScopes());
    }

    @Test
    @DisplayName("Supply and get token provider from token provider manager")
    void setAndGetTokenProvider() throws SaiException {
        AccessTokenProvider tokenProvider = new BasicAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, oidcProvider, TOKEN_SCOPES);
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
        AccessTokenProvider tokenProvider = new BasicAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, oidcProvider, TOKEN_SCOPES);
        AccessToken accessToken = tokenProvider.getAccessToken();
        assertEquals(TOKEN_STRING, accessToken.getValue());
        // Confirm that getting the token fetches the cached version
        assertEquals(accessToken, tokenProvider.getAccessToken());
    }

    @Test
    @DisplayName("Obtain taken without scopes")
    void getTokenWithoutScopes() throws IOException, SaiException {
        AccessTokenProvider tokenProvider = new BasicAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, oidcProvider);
        AccessToken accessToken = tokenProvider.getAccessToken();
        assertEquals(TOKEN_STRING, accessToken.getValue());
    }

    @Test
    @DisplayName("Fail to obtain token from invalid endpoint")
    void failToGetTokenBadEndpoint() throws SaiException, MalformedURLException {
        AccessTokenProvider tokenProvider = new BasicAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, toUrl(server, "/badop/"));
        assertThrows(IOException.class, () -> { tokenProvider.getAccessToken(); });
    }

    @Test
    @DisplayName("Refresh token")
    void refreshToken() throws IOException, SaiException {
        AccessTokenProvider tokenProvider = new BasicAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, oidcProvider, TOKEN_SCOPES);
        AccessToken accessToken = tokenProvider.getAccessToken();
        assertEquals(TOKEN_STRING, accessToken.getValue());
        AccessToken refreshToken = tokenProvider.refreshAccessToken();
        assertEquals(REFRESH_STRING, refreshToken.getValue());
    }

}

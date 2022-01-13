package com.janeirodigital.sai.core.tests.http;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.http.AccessTokenProvider;
import com.janeirodigital.sai.core.http.AccessTokenProviderManager;
import com.janeirodigital.sai.core.http.BasicAccessTokenProvider;
import com.janeirodigital.sai.core.tests.fixtures.RequestMatchingFixtureDispatcher;
import com.nimbusds.oauth2.sdk.ParseException;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.sai.core.helpers.HttpHelper.urlToUri;
import static com.janeirodigital.sai.core.tests.fixtures.DispatcherHelper.mockOnPost;
import static com.janeirodigital.sai.core.tests.fixtures.MockWebServerHelper.toUrl;
import static org.junit.jupiter.api.Assertions.*;

class AccessTokenProviderTests {

    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;
    private static URI tokenEndpoint;
    private final String CLIENT_IDENTIFIER = "test--client";
    private final String CLIENT_SECRET = "test-secret";
    private final List<String> TOKEN_SCOPES = Arrays.asList("profile", "test");
    private final String TOKEN_STRING = "MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3";
    private final String REFRESH_STRING = "MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZjJi56";

    @BeforeEach
    void beforeEach() {
        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
        // In a given test, the first request to this endpoint will return provider-response, the second will return provider-refresh (a different token)
        mockOnPost(dispatcher, "/token", List.of("http/token-provider-response-json", "http/token-provider-refresh-json"));
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
        tokenEndpoint = urlToUri(toUrl(server, "/token"));
        AccessTokenProviderManager.setProvider(null);
    }

    @AfterEach
    void afterEach() { AccessTokenProviderManager.setProvider(null); }

    @Test
    @DisplayName("Initialize basic access token provider")
    void initializeBasicTokenProvider() {
        BasicAccessTokenProvider basicProvider = new BasicAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, tokenEndpoint, TOKEN_SCOPES);
        assertEquals(CLIENT_IDENTIFIER, basicProvider.getClientIdentifier());
        assertEquals(CLIENT_SECRET, basicProvider.getClientSecret());
        assertEquals(tokenEndpoint, basicProvider.getTokenEndpoint());
        assertEquals(TOKEN_SCOPES, basicProvider.getScopes());
    }

    @Test
    @DisplayName("Supply and get token provider from token provider manager")
    void setAndGetTokenProvider() throws SaiException {
        AccessTokenProvider tokenProvider = new BasicAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, tokenEndpoint, TOKEN_SCOPES);
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
    void getTokenWithScopes() throws IOException, ParseException {
        AccessTokenProvider tokenProvider = new BasicAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, tokenEndpoint, TOKEN_SCOPES);
        String accessToken = tokenProvider.getAccessToken();
        assertEquals(TOKEN_STRING, accessToken);
        // Confirm that getting the token fetches the cached version
        assertEquals(accessToken, tokenProvider.getAccessToken());
    }

    @Test
    @DisplayName("Obtain taken without scopes")
    void getTokenWithoutScopes() throws IOException {
        AccessTokenProvider tokenProvider = new BasicAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, tokenEndpoint);
        String accessToken = tokenProvider.getAccessToken();
        assertEquals(TOKEN_STRING, accessToken);
    }

    @Test
    @DisplayName("Fail to obtain token from invalid endpoint")
    void failToGetTokenBadEndpoint() {
        AccessTokenProvider tokenProvider = new BasicAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, URI.create("http://bad.com/token"));
        assertThrows(IOException.class, () -> { tokenProvider.getAccessToken(); });
    }


    @Test
    @DisplayName("Refresh token")
    void refreshToken() throws IOException {
        AccessTokenProvider tokenProvider = new BasicAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, tokenEndpoint, TOKEN_SCOPES);
        String accessToken = tokenProvider.getAccessToken();
        assertEquals(TOKEN_STRING, accessToken);
        String refreshToken = tokenProvider.refreshAccessToken();
        assertEquals(REFRESH_STRING, refreshToken);
    }

}

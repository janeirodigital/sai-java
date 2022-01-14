package com.janeirodigital.sai.core.authorization;

import com.janeirodigital.sai.core.authorization.*;
import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.enums.HttpMethod;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.fixtures.DispatcherHelper;
import com.janeirodigital.sai.core.fixtures.MockWebServerHelper;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.oauth2.sdk.dpop.DefaultDPoPProofFactory;
import okhttp3.*;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;

import static com.janeirodigital.sai.core.enums.HttpMethod.GET;
import static com.janeirodigital.sai.core.helpers.HttpHelper.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessTokenAuthenticatorTests {

    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;
    private static AccessTokenProvider tokenProvider;
    private static HttpClientFactory clientFactory;
    private static OkHttpClient httpClient;
    private static URI tokenEndpoint;
    private static final String CLIENT_IDENTIFIER = "test--client";
    private static final String CLIENT_SECRET = "test-secret";
    private final String TOKEN_STRING = "MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3";
    private final String REFRESH_STRING = "MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZjJi56";

    @BeforeEach
    void beforeEach() throws SaiException {
        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
        // In a given test, the first request to this endpoint will return provider-response, the second will return provider-refresh (a different token)
        DispatcherHelper.mockOnPost(dispatcher, "/token", List.of("http/token-provider-response-json", "http/token-provider-refresh-json"));
        DispatcherHelper.mockOnGet(dispatcher, "/protected", List.of("http/401", "http/protected-ttl"));
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        tokenEndpoint = urlToUri(MockWebServerHelper.toUrl(server, "/token"));
        tokenProvider = new BasicAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, tokenEndpoint);
        AccessTokenProviderManager.setProvider(tokenProvider);
        clientFactory = new HttpClientFactory(false, false);
        httpClient = clientFactory.get();
    }

    @Test
    @DisplayName("Automatically refresh token on 401")
    void refreshToken() throws IOException, SaiException {
        AccessToken accessToken = tokenProvider.getAccessToken();
        Headers headers = setAuthorizationHeaders(accessToken, HttpMethod.GET, MockWebServerHelper.toUrl(server, "/protected"), null);
        Response response = getResource(httpClient, MockWebServerHelper.toUrl(server, "/protected"), headers);
        assertEquals(200, response.code());
    }

    @Test
    @DisplayName("Fail to get original access token")
    void failToGetOriginalToken(@Mock DefaultDPoPProofFactory mockProofFactory) throws SaiException, JOSEException, MalformedURLException {
        AccessTokenProvider mockedProvider = new DPoPAccessTokenProvider(CLIENT_IDENTIFIER, CLIENT_SECRET, tokenEndpoint, mockProofFactory);
        URL url = new URL("http://cool.biz/protected");
        when(mockProofFactory.createDPoPJWT(anyString(), any(URI.class))).thenThrow(JOSEException.class);
        AccessTokenAuthenticator authenticator = new AccessTokenAuthenticator(mockedProvider);
        assertNull(authenticator.authenticate(null, createResponse(url, true)));
    }

    @Test
    @DisplayName("Do not proceed when original token is unavailable")
    void doNotProceedOriginalUnavailable() throws MalformedURLException {
        URL url = new URL("http://cool.biz/protected");
        AccessTokenAuthenticator mockAuthenticator = mock(AccessTokenAuthenticator.class, withSettings().useConstructor(tokenProvider).defaultAnswer(CALLS_REAL_METHODS));
        when(mockAuthenticator.getAccessToken(tokenProvider)).thenReturn(null);
        assertNull(mockAuthenticator.authenticate(null, createResponse(url, true)));
    }



    @Test
    @DisplayName("Do not proceed when recent token is unavailable")
    void doNotProceedRecentUnavailable() throws MalformedURLException {
        URL url = new URL("http://cool.biz/protected");
        AccessTokenAuthenticator mockAuthenticator = mock(AccessTokenAuthenticator.class, withSettings().useConstructor(tokenProvider).defaultAnswer(CALLS_REAL_METHODS));
        AccessToken original = new BearerToken("original-token", tokenProvider);
        AccessToken recent = null;
        AccessToken refreshed = new BearerToken("refreshed-token", tokenProvider);
        when(mockAuthenticator.getAccessToken(tokenProvider)).thenReturn(original,recent);
        assertNull(mockAuthenticator.authenticate(null, createResponse(url, true)));
    }

    @Test
    @DisplayName("Proceed when recent token has been updated")
    void proceedWithUpdatedRecentToken() throws MalformedURLException {
        URL url = new URL("http://cool.biz/protected");
        AccessTokenAuthenticator mockAuthenticator = mock(AccessTokenAuthenticator.class, withSettings().useConstructor(tokenProvider).defaultAnswer(CALLS_REAL_METHODS));
        AccessToken original = new BearerToken("original-token", tokenProvider);
        AccessToken recent = new BearerToken("recent-token", tokenProvider);
        when(mockAuthenticator.getAccessToken(tokenProvider)).thenReturn(original,recent);
        assertNotNull(mockAuthenticator.authenticate(null, createResponse(url, true)));
    }

    @Test
    @DisplayName("Do not proceed if original request didn't include authorization header")
    void doNotProceedWhenNoAuthorizationHeader() throws MalformedURLException {
        URL url = new URL("http://cool.biz/protected");
        AccessTokenAuthenticator mockAuthenticator = mock(AccessTokenAuthenticator.class, withSettings().useConstructor(tokenProvider).defaultAnswer(CALLS_REAL_METHODS));
        AccessToken original = new BearerToken("original-token", tokenProvider);
        AccessToken recent = new BearerToken("recent-token", tokenProvider);
        when(mockAuthenticator.getAccessToken(tokenProvider)).thenReturn(original,recent);
        assertNull(mockAuthenticator.authenticate(null, createResponse(url, false)));
    }

    @Test
    @DisplayName("Do not proceed when refresh token is unavailable")
    void doNotProceedRefreshUnavailable() throws MalformedURLException {
        URL url = new URL("http://cool.biz/protected");
        AccessTokenAuthenticator mockAuthenticator = mock(AccessTokenAuthenticator.class, withSettings().useConstructor(tokenProvider).defaultAnswer(CALLS_REAL_METHODS));
        AccessToken original = new BearerToken("original-token", tokenProvider);
        AccessToken recent = original;
        when(mockAuthenticator.getAccessToken(tokenProvider)).thenReturn(original,recent);
        when(mockAuthenticator.refreshAccessToken(tokenProvider)).thenReturn(null);
        assertNull(mockAuthenticator.authenticate(null, createResponse(url, true)));
    }

    @Test
    @DisplayName("Do not proceed when refresh token fails")
    void doNotProceedRefreshFails() throws IOException {
        URL url = new URL("http://cool.biz/protected");
        BasicAccessTokenProvider mockedProvider = mock(BasicAccessTokenProvider.class);
        AccessTokenAuthenticator mockAuthenticator = mock(AccessTokenAuthenticator.class, withSettings().useConstructor(mockedProvider).defaultAnswer(CALLS_REAL_METHODS));
        AccessToken original = new BearerToken("original-token", mockedProvider);
        AccessToken recent = original;
        when(mockAuthenticator.getAccessToken(mockedProvider)).thenReturn(original,recent);
        when(mockedProvider.refreshAccessToken()).thenThrow(IOException.class);
        assertNull(mockAuthenticator.authenticate(null, createResponse(url, true)));
    }

    @Test
    @DisplayName("Do not proceed when authorization headers cannot be replaced")
    void doNotProceedHeadersFails() throws IOException, SaiException {
        URL url = new URL("http://cool.biz/protected");
        BasicAccessTokenProvider mockedProvider = mock(BasicAccessTokenProvider.class);
        AccessTokenAuthenticator mockAuthenticator = mock(AccessTokenAuthenticator.class, withSettings().useConstructor(mockedProvider).defaultAnswer(CALLS_REAL_METHODS));
        AccessToken original = new BearerToken("original-token", mockedProvider);
        AccessToken recent = original;
        AccessToken refreshed = new BearerToken("refreshed-token", mockedProvider);
        when(mockAuthenticator.getAccessToken(mockedProvider)).thenReturn(original,recent);
        when(mockedProvider.refreshAccessToken()).thenReturn(refreshed);
        when(mockedProvider.getAuthorizationHeaders(any(AccessToken.class), any(HttpMethod.class), any(URL.class))).thenThrow(SaiException.class);
        assertNull(mockAuthenticator.authenticate(null, createResponse(url, true)));
    }

    private Response createResponse(URL url, boolean withHeaders) {
        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.url(url);
        requestBuilder.method(GET.getValue(), null);
        if (withHeaders) { requestBuilder.addHeader(HttpHeader.AUTHORIZATION.getValue(), "smoothjazz"); }
        Request request = requestBuilder.build();
        Response.Builder responseBuilder = new Response.Builder();
        return responseBuilder.code(200).protocol(Protocol.HTTP_2).message("success").request(request).build();
    }


}

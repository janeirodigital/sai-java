package com.janeirodigital.sai.core.authorization;

import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.nimbusds.jose.JOSEException;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getProtectedResource;
import static com.janeirodigital.sai.core.enums.HttpMethod.GET;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnGet;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnPost;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessTokenRefresherTests {

    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;
    private static HttpClientFactory clientFactory;
    private static OkHttpClient httpClient;
    private static URL oidcProvider;
    private static AuthorizedSession authorizedSession;
    private static AuthorizedSessionAccessor sessionAccessor;
    private final String socialAgentId = "https://alice.example/id#me";
    private final String applicationId = "https://projectron.example/id";
    private final String TOKEN_STRING = "MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3";
    private final String REFRESH_STRING = "MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZjJi56";

    @BeforeEach
    void beforeEach() throws SaiException {
        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
        // In a given test, the first request to this endpoint will return provider-response, the second will return provider-refresh (a different token)
        mockOnGet(dispatcher, "/op/.well-known/openid-configuration", "authorization/op-configuration-json");
        mockOnPost(dispatcher, "/op/token", List.of("http/token-provider-response-json", "http/token-provider-refresh-json"));
        mockOnGet(dispatcher, "/protected", List.of("http/401", "http/protected-ttl"));
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        oidcProvider = toUrl(server, "/op/");
        // TODO - may need to initialize an actual session here to refresh ?
        authorizedSession = mock(AuthorizedSession.class);
        sessionAccessor = new BasicAuthorizedSessionAccessor();  // TODO - probably need to come back to this to store session
        clientFactory = new HttpClientFactory(false, false, true, sessionAccessor);
        httpClient = clientFactory.get();
    }
    
    @Test
    @DisplayName("Automatically refresh token on 401")
    void refreshToken() throws SaiException {
        Response response = getProtectedResource(authorizedSession, httpClient, toUrl(server, "/protected"));
        assertEquals(200, response.code());
    }

    @Test
    @DisplayName("Fail to get original access token")
    void failToGetOriginalToken(@Mock AuthorizedSessionAccessor mockAccessor) throws SaiException, JOSEException, MalformedURLException {
        URL url = new URL("http://cool.biz/protected");
        AccessTokenRefresher authenticator = new AccessTokenRefresher(mockAccessor);
        assertNull(authenticator.authenticate(null, createResponse(url, true)));
    }

    // TODO - AUTH-REFACTOR
    @Test
    @DisplayName("Do not proceed when original token is unavailable")
    void doNotProceedOriginalUnavailable() throws MalformedURLException {
        URL url = new URL("http://cool.biz/protected");
        AccessTokenRefresher mockAuthenticator = mock(AccessTokenRefresher.class, withSettings().useConstructor(sessionAccessor).defaultAnswer(CALLS_REAL_METHODS));
        assertNull(mockAuthenticator.authenticate(null, createResponse(url, true)));
    }

    // TODO - AUTH-REFACTOR
    @Test
    @DisplayName("Do not proceed when recent token is unavailable")
    void doNotProceedRecentUnavailable() throws MalformedURLException {
        URL url = new URL("http://cool.biz/protected");
        AccessTokenRefresher mockAuthenticator = mock(AccessTokenRefresher.class, withSettings().useConstructor(sessionAccessor).defaultAnswer(CALLS_REAL_METHODS));
        AccessToken original = new AccessToken("original-token");
        AccessToken recent = null;
        AccessToken refreshed = new AccessToken("refreshed-token");
//        when(mockAuthenticator.getAccessToken(tokenProvider)).thenReturn(original,recent);
        assertNull(mockAuthenticator.authenticate(null, createResponse(url, true)));
    }

    // TODO - AUTH-REFACTOR
    @Test
    @DisplayName("Proceed when recent token has been updated")
    void proceedWithUpdatedRecentToken() throws MalformedURLException {
        URL url = new URL("http://cool.biz/protected");
        AccessTokenRefresher mockAuthenticator = mock(AccessTokenRefresher.class, withSettings().useConstructor(sessionAccessor).defaultAnswer(CALLS_REAL_METHODS));
        AccessToken original = new AccessToken("original-token");
        AccessToken recent = new AccessToken("recent-token");
        //when(mockAuthenticator.getAccessToken(tokenProvider)).thenReturn(original,recent);
        assertNotNull(mockAuthenticator.authenticate(null, createResponse(url, true)));
    }

    // TODO - AUTH-REFACTOR
    @Test
    @DisplayName("Do not proceed if original request didn't include authorization header")
    void doNotProceedWhenNoAuthorizationHeader() throws MalformedURLException {
        URL url = new URL("http://cool.biz/protected");
        AccessTokenRefresher mockAuthenticator = mock(AccessTokenRefresher.class, withSettings().useConstructor(sessionAccessor).defaultAnswer(CALLS_REAL_METHODS));
        AccessToken original = new AccessToken("original-token");
        AccessToken recent = new AccessToken("recent-token");
//        when(mockAuthenticator.getAccessToken(tokenProvider)).thenReturn(original,recent);
        assertNull(mockAuthenticator.authenticate(null, createResponse(url, false)));
    }

    // TODO - AUTH-REFACTOR
    @Test
    @DisplayName("Do not proceed when refresh token is unavailable")
    void doNotProceedRefreshUnavailable() throws MalformedURLException {
        URL url = new URL("http://cool.biz/protected");
        AccessTokenRefresher mockAuthenticator = mock(AccessTokenRefresher.class, withSettings().useConstructor(sessionAccessor).defaultAnswer(CALLS_REAL_METHODS));
        AccessToken original = new AccessToken("original-token");
        AccessToken recent = original;
//        when(mockAuthenticator.getAccessToken(tokenProvider)).thenReturn(original,recent);
//        when(mockAuthenticator.refreshAccessToken(tokenProvider)).thenReturn(null);
        assertNull(mockAuthenticator.authenticate(null, createResponse(url, true)));
    }

    /*
    // TODO - AUTH-REFACTOR
    @Test
    @DisplayName("Do not proceed when refresh token fails")
    void doNotProceedRefreshFails() throws IOException {
        URL url = new URL("http://cool.biz/protected");
        BasicAccessTokenProvider mockedProvider = mock(BasicAccessTokenProvider.class);
        AccessTokenRefresher mockAuthenticator = mock(AccessTokenRefresher.class, withSettings().useConstructor(mockedProvider).defaultAnswer(CALLS_REAL_METHODS));
        AccessToken original = new AccessToken("original-token");
        AccessToken recent = original;
//        when(mockAuthenticator.getAccessToken(mockedProvider)).thenReturn(original,recent);
//        when(mockedProvider.refreshAccessToken()).thenThrow(IOException.class);
        assertNull(mockAuthenticator.authenticate(null, createResponse(url, true)));
    }

     */

    // TODO - AUTH-REFACTOR
    /*
    @Test
    @DisplayName("Do not proceed when authorization headers cannot be replaced")
    void doNotProceedHeadersFails() throws IOException, SaiException {
        URL url = new URL("http://cool.biz/protected");
        BasicAccessTokenProvider mockedProvider = mock(BasicAccessTokenProvider.class);
        AccessTokenRefresher mockAuthenticator = mock(AccessTokenRefresher.class, withSettings().useConstructor(mockedProvider).defaultAnswer(CALLS_REAL_METHODS));
        AccessToken original = new AccessToken("original-token");
        AccessToken recent = original;
        AccessToken refreshed = new AccessToken("refreshed-token");
//        when(mockAuthenticator.getAccessToken(mockedProvider)).thenReturn(original,recent);
        when(mockedProvider.refreshAccessToken()).thenReturn(refreshed);
        when(mockedProvider.getAuthorizationHeaders(any(AccessToken.class), any(HttpMethod.class), any(URL.class))).thenThrow(SaiException.class);
        assertNull(mockAuthenticator.authenticate(null, createResponse(url, true)));
    }
    */

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

package com.janeirodigital.sai.core.authentication;

import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.enums.HttpMethod;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URL;
import java.util.List;
import java.util.Map;

import static com.janeirodigital.sai.core.authentication.AuthorizedSessionHelper.getProtectedResource;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnGet;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnPost;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static com.janeirodigital.sai.core.helpers.HttpHelper.getResource;
import static com.janeirodigital.sai.core.helpers.HttpHelper.stringToUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccessTokenRefresherTests {

    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;
    private static HttpClientFactory clientFactory;
    private static OkHttpClient httpClient;

    private static BasicAuthorizedSessionAccessor sessionAccessor;

    @Mock(answer = Answers.CALLS_REAL_METHODS, lenient = true)
    private AuthorizedSession authorizedSession;
    @Mock(answer = Answers.CALLS_REAL_METHODS, lenient = true)
    private AuthorizedSession updatedSession;
    private URL socialAgentId;
    private URL applicationId;
    private URL oidcProviderId;
    private AccessToken originalToken;
    private AccessToken updatedToken;
    private final String TOKEN_STRING = "MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3";
    private final String REFRESH_STRING = "MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZjJi56";

    @BeforeEach
    void beforeEach() throws SaiException {
        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
        // In a given test, the first request to this endpoint will return provider-response, the second will return provider-refresh (a different token)
        mockOnGet(dispatcher, "/op/.well-known/openid-configuration", "authentication/op-configuration-json");
        mockOnPost(dispatcher, "/op/token", List.of("authentication/op-token-response-json", "authentication/op-token-response-refresh-json"));
        mockOnGet(dispatcher, "/protected", List.of("http/401", "authentication/protected-ttl"));
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        sessionAccessor = new BasicAuthorizedSessionAccessor();
        clientFactory = new HttpClientFactory(false, false, true, sessionAccessor);
        httpClient = clientFactory.get();

        oidcProviderId = toUrl(server, "/op/");
        socialAgentId = stringToUrl("https://alice.example/id#me");
        applicationId = stringToUrl("https://projectron.example/id");

        originalToken = new AccessToken(TOKEN_STRING);
        when(authorizedSession.getSocialAgentId()).thenReturn(socialAgentId);
        when(authorizedSession.getApplicationId()).thenReturn(applicationId);
        when(authorizedSession.getOidcProviderId()).thenReturn(oidcProviderId);
        when(authorizedSession.getAccessToken()).thenReturn(originalToken);
        Map<String, String> authorizationHeader = Map.of(HttpHeader.AUTHORIZATION.getValue(), "Bearer " + originalToken.getValue());
        when(authorizedSession.toHttpHeaders(any(HttpMethod.class), any(URL.class))).thenReturn(authorizationHeader);
    }

    @Test
    @DisplayName("Automatically refresh token on 401")
    void refreshToken() throws SaiException {
        sessionAccessor.store(authorizedSession);
        Response response = getProtectedResource(authorizedSession, httpClient, toUrl(server, "/protected"));
        assertEquals(200, response.code());
        verify(authorizedSession).refresh();
    }

    @Test
    @DisplayName("Bypass on request with no authorization header")
    void bypassNoAuthorizationHeader() throws SaiException {
        Response response = getResource(httpClient, toUrl(server, "/protected"));
        assertEquals(401, response.code());
    }

    @Test
    @DisplayName("Fail to refresh when access token cannot be extracted from authorization header")
    void failToRefreshBadHeader() throws SaiException {
        AuthorizedSession badHeaderSession = mock(AuthorizedSession.class, CALLS_REAL_METHODS);
        Map<String, String> authorizationHeader = Map.of(HttpHeader.AUTHORIZATION.getValue(), "INVALIDVALUE");
        when(badHeaderSession.toHttpHeaders(any(HttpMethod.class), any(URL.class))).thenReturn(authorizationHeader);
        Response response = getProtectedResource(badHeaderSession, httpClient, toUrl(server, "/protected"));
        assertEquals(401, response.code());
    }

    @Test
    @DisplayName("Fail to refresh when session is missing from session storage")
    void failToRefreshNoSessionInStorage() throws SaiException {
        Response response = getProtectedResource(authorizedSession, httpClient, toUrl(server, "/protected"));
        assertEquals(401, response.code());
    }

    @Test
    @DisplayName("Fail to refresh when session is missing from storage on followup")
    void failToRefreshNoSessionInFollowUp() throws SaiException {
        AuthorizedSessionAccessor mockAccessor = mock(AuthorizedSessionAccessor.class);
        HttpClientFactory influencedFactory = new HttpClientFactory(false, false, true, mockAccessor);
        OkHttpClient influencedClient = influencedFactory.get();
        when(mockAccessor.get(any(AccessToken.class))).thenReturn(authorizedSession);
        when(mockAccessor.get(any(AuthorizedSession.class))).thenReturn(null);
        Response response = getProtectedResource(authorizedSession, influencedClient, toUrl(server, "/protected"));
        assertEquals(401, response.code());
    }

    @Test
    @DisplayName("Refresh with session updated in storage while waiting on another thread")
    void refreshWithSessionUpdatedOnWait() throws SaiException {
        AuthorizedSessionAccessor mockAccessor = mock(AuthorizedSessionAccessor.class);
        HttpClientFactory influencedFactory = new HttpClientFactory(false, false, true, mockAccessor);
        OkHttpClient influencedClient = influencedFactory.get();
        when(mockAccessor.get(any(AccessToken.class))).thenReturn(authorizedSession);
        when(mockAccessor.get(any(AuthorizedSession.class))).thenReturn(updatedSession);

        updatedToken = new AccessToken(REFRESH_STRING);
        when(updatedSession.getSocialAgentId()).thenReturn(stringToUrl("https://some.agent")); // ensures equality check will be false
        when(updatedSession.getApplicationId()).thenReturn(applicationId);
        when(updatedSession.getOidcProviderId()).thenReturn(oidcProviderId);
        when(updatedSession.getAccessToken()).thenReturn(updatedToken);
        Map<String, String> updatedHeader = Map.of(HttpHeader.AUTHORIZATION.getValue(), "Bearer " + updatedToken.getValue());
        when(updatedSession.toHttpHeaders(any(HttpMethod.class), any(URL.class))).thenReturn(updatedHeader);
        
        Response response = getProtectedResource(authorizedSession, influencedClient, toUrl(server, "/protected"));
        assertEquals(200, response.code());
    }

    @Test
    @DisplayName("Fail to refresh when session refresh fails")
    void failToRefreshOnRefreshFailure() throws SaiException {
        AuthorizedSessionAccessor mockAccessor = mock(AuthorizedSessionAccessor.class);
        HttpClientFactory influencedFactory = new HttpClientFactory(false, false, true, mockAccessor);
        OkHttpClient influencedClient = influencedFactory.get();
        when(mockAccessor.get(any(AccessToken.class))).thenReturn(authorizedSession);
        when(mockAccessor.get(any(AuthorizedSession.class))).thenReturn(authorizedSession);
        when(mockAccessor.refresh(any(AuthorizedSession.class))).thenReturn(null);
        Response response = getProtectedResource(authorizedSession, influencedClient, toUrl(server, "/protected"));
        assertEquals(401, response.code());
    }

    @Test
    @DisplayName("Fail to refresh when header generation fails")
    void failToRefreshOnHeaderFailure() throws SaiException {
        AuthorizedSessionAccessor mockAccessor = mock(AuthorizedSessionAccessor.class);
        HttpClientFactory influencedFactory = new HttpClientFactory(false, false, true, mockAccessor);
        OkHttpClient influencedClient = influencedFactory.get();
        when(mockAccessor.get(any(AccessToken.class))).thenReturn(authorizedSession);
        when(mockAccessor.get(any(AuthorizedSession.class))).thenReturn(authorizedSession);
        when(mockAccessor.refresh(any(AuthorizedSession.class))).thenReturn(updatedSession);

        updatedToken = new AccessToken(REFRESH_STRING);
        when(updatedSession.toHttpHeaders(any(HttpMethod.class), any(URL.class))).thenThrow(SaiException.class);

        Response response = getProtectedResource(authorizedSession, influencedClient, toUrl(server, "/protected"));
        assertEquals(401, response.code());
    }

    @Test
    @DisplayName("Fail to refresh - session storage exception")
    void failToRefreshStorageException() throws SaiException {
        AuthorizedSessionAccessor mockAccessor = mock(AuthorizedSessionAccessor.class);
        HttpClientFactory influencedFactory = new HttpClientFactory(false, false, true, mockAccessor);
        OkHttpClient influencedClient = influencedFactory.get();
        when(mockAccessor.get(any(AccessToken.class))).thenReturn(authorizedSession);
        when(mockAccessor.get(any(AuthorizedSession.class))).thenThrow(SaiException.class);
        Response response = getProtectedResource(authorizedSession, influencedClient, toUrl(server, "/protected"));
        assertEquals(401, response.code());
    }

    @Test
    @DisplayName("Fail to refresh - session storage refresh exception")
    void failToRefreshStorageRefreshException() throws SaiException {
        AuthorizedSessionAccessor mockAccessor = mock(AuthorizedSessionAccessor.class);
        HttpClientFactory influencedFactory = new HttpClientFactory(false, false, true, mockAccessor);
        OkHttpClient influencedClient = influencedFactory.get();
        when(mockAccessor.get(any(AccessToken.class))).thenReturn(authorizedSession);
        when(mockAccessor.get(any(AuthorizedSession.class))).thenReturn(authorizedSession);
        when(mockAccessor.refresh(any(AuthorizedSession.class))).thenThrow(SaiException.class);
        Response response = getProtectedResource(authorizedSession, influencedClient, toUrl(server, "/protected"));
        assertEquals(401, response.code());
    }

}

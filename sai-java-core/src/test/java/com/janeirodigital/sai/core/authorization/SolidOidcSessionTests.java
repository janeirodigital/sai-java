package com.janeirodigital.sai.core.authorization;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.dpop.DefaultDPoPProofFactory;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.openid.connect.sdk.Prompt;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static com.janeirodigital.sai.core.authorization.SolidOidcSession.getDPoPProofFactory;
import static com.janeirodigital.sai.core.authorization.SolidOidcSession.getProof;
import static com.janeirodigital.sai.core.enums.HttpHeader.AUTHORIZATION;
import static com.janeirodigital.sai.core.enums.HttpHeader.DPOP;
import static com.janeirodigital.sai.core.enums.HttpMethod.GET;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnGet;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnPost;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SolidOidcSessionTests {

    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;
    private static HttpClientFactory clientFactory;
    private static OkHttpClient httpClient;
    private static URL applicationId;
    private static URL socialAgentId;
    private static URL socialAgentNoDpopId;
    private static URL socialAgentNoWebId;
    private static URL socialAgentNoClientId;
    private static URL socialAgentBadIoId;
    private static URL socialAgentUnknownId;
    private static URL socialAgentNoRefreshId;
    private static URL socialAgentRefreshId;
    private static URL socialAgentRefreshUnknownId;
    private static URL socialAgentRefreshNoRefreshId;
    private static URL oidcProviderId;
    private static URL redirect;

    private static final String redirectPath = "/projectron/redirect";
    private static final String code = "gVhyP_MCzEFUbH5ygCWYfEBAMGrLdZLwcwAPwTg0AFv";
    private static final List<String> scopes = Arrays.asList("openid", "profile", "offline_access");
    private static final Prompt prompt = new Prompt(Prompt.Type.CONSENT);


    @BeforeAll
    static void beforeAll() throws SaiException {
        dispatcher = new RequestMatchingFixtureDispatcher();
        // Good webid and provider configuration
        mockOnGet(dispatcher, "/alice/id", "authorization/alice-webid-ttl");
        mockOnGet(dispatcher, "/op/.well-known/openid-configuration", "authorization/op-configuration-json");
        mockOnPost(dispatcher, "/op/token", "authorization/op-token-response-json");
        // Webid points to provider that doesn't have DPoP support
        mockOnGet(dispatcher, "/nodpop/alice/id", "authorization/alice-webid-nodpop-ttl");
        mockOnGet(dispatcher, "/nodpop/op/.well-known/openid-configuration", "authorization/op-configuration-nodpop-json");
        // Webid points to provider thaht doesn't support webid claims
        mockOnGet(dispatcher, "/nowebid/alice/id", "authorization/alice-webid-nowebid-ttl");
        mockOnGet(dispatcher, "/nowebid/op/.well-known/openid-configuration", "authorization/op-configuration-nowebid-json");
        // Webid points to provider that doesn't support client_id claims
        mockOnGet(dispatcher, "/noclientid/alice/id", "authorization/alice-webid-noclientid-ttl");
        mockOnGet(dispatcher, "/noclientid/op/.well-known/openid-configuration", "authorization/op-configuration-noclientid-json");
        // Webid points to provider with a token endpoint that gives a network IO error
        mockOnGet(dispatcher, "/badio/alice/id", "authorization/alice-webid-badio-ttl");
        mockOnGet(dispatcher, "/badio/op/.well-known/openid-configuration", "authorization/op-configuration-badio-json");
        mockOnPost(dispatcher, "/badio/op/token", "authorization/op-token-response-badio-json");
        // Webid points to provider with a token endpoint that gives an access token of unknown type
        mockOnGet(dispatcher, "/unknown/alice/id", "authorization/alice-webid-unknown-ttl");
        mockOnGet(dispatcher, "/unknown/op/.well-known/openid-configuration", "authorization/op-configuration-unknown-json");
        mockOnPost(dispatcher, "/unknown/op/token", "authorization/op-token-response-unknown-json");
        // Request is made without an offline access scope so the token endpoint doesn't give back a refresh token
        mockOnGet(dispatcher, "/norefresh/alice/id", "authorization/alice-webid-norefresh-ttl");
        mockOnGet(dispatcher, "/norefresh/op/.well-known/openid-configuration", "authorization/op-configuration-norefresh-json");
        mockOnPost(dispatcher, "/norefresh/op/token", "authorization/op-token-response-norefresh-json");
        // Request is made with an offline access scope and then a refresh is issued getting a different access token and refresh token
        mockOnGet(dispatcher, "/refresh/alice/id", "authorization/alice-webid-refresh-ttl");
        mockOnGet(dispatcher, "/refresh/op/.well-known/openid-configuration", "authorization/op-configuration-refresh-json");
        mockOnPost(dispatcher, "/refresh/op/token", "authorization/op-token-response-refresh-json");
        // Request is made with an offline access scope and then a refresh is issued getting a different access token of unknown type
        mockOnGet(dispatcher, "/refresh-unknown/alice/id", "authorization/alice-webid-refresh-unknown-ttl");
        mockOnGet(dispatcher, "/refresh-unknown/op/.well-known/openid-configuration", "authorization/op-configuration-refresh-unknown-json");
        mockOnPost(dispatcher, "/refresh-unknown/op/token", List.of("authorization/op-token-response-refresh-unknown-1-json", "authorization/op-token-response-refresh-unknown-2-json"));
        // Request is made with an offline access scope and then a refresh is issued but an additional refresh token isn't provided
        mockOnGet(dispatcher, "/refresh-norefresh/alice/id", "authorization/alice-webid-refresh-norefresh-ttl");
        mockOnGet(dispatcher, "/refresh-norefresh/op/.well-known/openid-configuration", "authorization/op-configuration-refresh-norefresh-json");
        mockOnPost(dispatcher, "/refresh-norefresh/op/token", List.of("authorization/op-token-response-refresh-norefresh-1-json", "authorization/op-token-response-refresh-norefresh-2-json"));
        
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
        clientFactory = new HttpClientFactory(false, false, false);
        httpClient = clientFactory.get();

        socialAgentId = toUrl(server, "/alice/id#me");
        socialAgentNoDpopId = toUrl(server, "/nodpop/alice/id#me");
        socialAgentNoWebId = toUrl(server, "/nowebid/alice/id#me");
        socialAgentNoClientId = toUrl(server, "/noclientid/alice/id#me");
        socialAgentBadIoId = toUrl(server, "/badio/alice/id#me");
        socialAgentUnknownId = toUrl(server, "/unknown/alice/id#me");
        socialAgentNoRefreshId = toUrl(server, "/norefresh/alice/id#me");
        socialAgentRefreshId = toUrl(server, "/refresh/alice/id#me");
        socialAgentRefreshUnknownId = toUrl(server, "/refresh-unknown/alice/id#me");
        socialAgentRefreshNoRefreshId = toUrl(server, "/refresh-norefresh/alice/id#me");

        applicationId = toUrl(server, "/projectron/id");
        oidcProviderId = toUrl(server, "/op/");

        redirect = toUrl(server, redirectPath);
    }

    @Test
    @DisplayName("Initialize solid-oidc builder - http client")
    void initBuilderHttp() throws SaiException {
        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient);
        assertEquals(httpClient, builder.getHttpClient());
    }

    @Test
    @DisplayName("Initialize solid-oidc builder - social agent")
    void initBuilderSocialAgent() throws SaiException {
        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentId);
        assertEquals(socialAgentId, builder.getSocialAgentId());
        assertNotNull(builder.getOidcProviderMetadata());
        assertEquals(oidcProviderId.toString(), builder.getOidcProviderMetadata().getIssuer().getValue());
    }

    @Test
    @DisplayName("Fail to initialize solid-oidc builder - no provider dpop support")
    void failToInitBuilderNoProviderDPoP() {
        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient);
        assertThrows(SaiException.class, () -> { builder.setSocialAgent(socialAgentNoDpopId); });
    }

    @Test
    @DisplayName("Fail to initialize solid-oidc builder - no provider webid claim support")
    void failToInitBuilderNoProviderWebId() {
        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient);
        assertThrows(SaiException.class, () -> { builder.setSocialAgent(socialAgentNoWebId); });
    }

    @Test
    @DisplayName("Fail to initialize solid-oidc builder - no provider client_id claim support")
    void failToInitBuilderNoProviderClientId() {
        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient);
        assertThrows(SaiException.class, () -> { builder.setSocialAgent(socialAgentNoClientId); });
    }

    @Test
    @DisplayName("Initialize solid-oidc builder - application")
    void initBuilderApplication() throws SaiException {
        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentId).setApplication(applicationId);
        assertEquals(applicationId, builder.getApplicationId());
    }

    @Test
    @DisplayName("Initialize solid-oidc builder - scope")
    void initBuilderScope() throws SaiException {
        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentId).setApplication(applicationId).setScope(scopes);
        for (String scope : scopes) { assertTrue(builder.getScope().contains(scope)); }
    }

    @Test
    @DisplayName("Initialize solid-oidc builder - prompt")
    void initBuilderPrompt() throws SaiException {
        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentId).setApplication(applicationId).setScope(scopes).setPrompt(prompt);
        assertEquals(prompt, builder.getPrompt());
    }

    @Test
    @DisplayName("Initialize solid-oidc builder - redirect")
    void initBuilderRedirect() throws SaiException {
        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentId).setApplication(applicationId).setScope(scopes)
                .setPrompt(prompt).setRedirect(redirect);
        assertEquals(redirect, builder.getRedirect());
    }

    @Test
    @DisplayName("Initialize solid-oidc builder - prepare code request")
    void initBuilderPrepareCode() throws SaiException {
        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentId).setApplication(applicationId).setScope(scopes)
                .setPrompt(prompt).setRedirect(redirect).prepareCodeRequest();
        assertNotNull(builder.getAuthorizationRequest());
    }

    @Test
    @DisplayName("Initialize solid-oidc builder - prepare code request no prompt")
    void initBuilderPrepareCodeNoPrompt() throws SaiException {
        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentId).setApplication(applicationId).setScope(scopes)
                .setRedirect(redirect).prepareCodeRequest();;
        assertNotNull(builder.getAuthorizationRequest());
        assertNotNull(builder.getCodeRequestUrl());
    }

    @Test
    @DisplayName("Initialize solid-oidc builder - process code response")
    void initBuilderProcessResponse() throws SaiException {
        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentId).setApplication(applicationId).setScope(scopes)
                .setPrompt(prompt).setRedirect(redirect).prepareCodeRequest();

        URL responseUrl = toUrl(server, redirectPath + "?code=" + code + "&state=" + builder.getAuthorizationRequest().getState());
        builder.processCodeResponse(responseUrl);
        assertNotNull(builder.getAuthorizationCode());
        assertEquals(code, builder.getAuthorizationCode().toString());
    }

    @Test
    @DisplayName("Fail to initialize solid-oidc builder - state mismatch in response")
    void failToInitBuilderStateMismatch() throws SaiException {
        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentId).setApplication(applicationId).setScope(scopes)
                .setPrompt(prompt).setRedirect(redirect).prepareCodeRequest();
        URL responseUrl = toUrl(server, redirectPath + "?code=" + code + "&state=ThisIsNotTheRequestState");
        assertThrows(SaiException.class, () -> builder.processCodeResponse(responseUrl) );
    }

    @Test
    @DisplayName("Fail to initialize solid-oidc builder - parse failure")
    void failToInitBuilderParseFailure() throws SaiException {

        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentId).setApplication(applicationId).setScope(scopes)
                .setPrompt(prompt).setRedirect(redirect).prepareCodeRequest();

        try (MockedStatic<AuthorizationResponse> mockResponse = Mockito.mockStatic(AuthorizationResponse.class)) {
            URL responseUrl = toUrl(server, redirectPath + "?codeeeeeoooooo=" + code + "&staaaattteeee=cantparsethisbro");
            mockResponse.when(() -> AuthorizationResponse.parse(any(URI.class))).thenThrow(ParseException.class);
            assertThrows(SaiException.class, () -> builder.processCodeResponse(responseUrl));
        }

    }

    @Test
    @DisplayName("Fail to initialize solid-oidc builder - misc code response failure")
    void failToInitBuilderResponseFailure() throws SaiException {

        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentId).setApplication(applicationId).setScope(scopes)
                .setPrompt(prompt).setRedirect(redirect).prepareCodeRequest();

        try (MockedStatic<AuthorizationResponse> mockStaticResponse = Mockito.mockStatic(AuthorizationResponse.class)) {
            AuthorizationResponse mockResponse = mock(AuthorizationResponse.class);
            AuthorizationErrorResponse mockErrorResponse = mock(AuthorizationErrorResponse.class);
            URL responseUrl = toUrl(server, redirectPath + "?code=" + code + "&state=" + builder.getAuthorizationRequest().getState());
            when(mockResponse.indicatesSuccess()).thenReturn(false);
            when(mockResponse.getState()).thenReturn(builder.getAuthorizationRequest().getState());
            when(mockErrorResponse.getErrorObject()).thenReturn(new ErrorObject("Problems!"));
            when(mockResponse.toErrorResponse()).thenReturn(mockErrorResponse);
            mockStaticResponse.when(() -> AuthorizationResponse.parse(any(URI.class))).thenReturn(mockResponse);
            assertThrows(SaiException.class, () -> builder.processCodeResponse(responseUrl));
        }

    }

    @Test
    @DisplayName("Initialize solid-oidc builder - request tokens")
    void initBuilderRequestTokens() throws SaiException {

        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentId).setApplication(applicationId).setScope(scopes)
                .setPrompt(prompt).setRedirect(redirect).prepareCodeRequest();
        URL responseUrl = toUrl(server, redirectPath + "?code=" + code + "&state=" + builder.getAuthorizationRequest().getState());
        builder.processCodeResponse(responseUrl).requestTokens();
        assertNotNull(builder.getAccessToken());
        assertNotNull(builder.getRefreshToken());
    }

    @Test
    @DisplayName("Fail to initialize solid-oidc builder - misc token response failure")
    void failToInitBuilderMiscTokenFailure() throws SaiException, ParseException {

        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentId).setApplication(applicationId).setScope(scopes)
                .setPrompt(prompt).setRedirect(redirect).prepareCodeRequest();

        try (MockedStatic<TokenResponse> mockStaticResponse = Mockito.mockStatic(TokenResponse.class)) {
            URL responseUrl = toUrl(server, redirectPath + "?code=" + code + "&state=" + builder.getAuthorizationRequest().getState());
            TokenResponse mockResponse = mock(TokenResponse.class);
            TokenErrorResponse mockErrorResponse = mock(TokenErrorResponse.class);
            when(mockResponse.indicatesSuccess()).thenReturn(false);
            when(mockErrorResponse.getErrorObject()).thenReturn(new ErrorObject("Problems!"));
            when(mockResponse.toErrorResponse()).thenReturn(mockErrorResponse);
            when(TokenResponse.parse(any(HTTPResponse.class))).thenReturn(mockResponse);
            builder.processCodeResponse(responseUrl);
            assertThrows(SaiException.class, () -> builder.requestTokens());
        }

    }

    @Test
    @DisplayName("Fail to initialize solid-oidc builder - token parse failure")
    void failToInitBuilderTokenParseFailure() throws SaiException, ParseException {

        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentId).setApplication(applicationId).setScope(scopes)
                .setPrompt(prompt).setRedirect(redirect).prepareCodeRequest();

        try (MockedStatic<TokenResponse> mockStaticResponse = Mockito.mockStatic(TokenResponse.class)) {
            URL responseUrl = toUrl(server, redirectPath + "?code=" + code + "&state=" + builder.getAuthorizationRequest().getState());
            when(TokenResponse.parse(any(HTTPResponse.class))).thenThrow(ParseException.class);
            builder.processCodeResponse(responseUrl);
            assertThrows(SaiException.class, () -> builder.requestTokens());
        }

    }

    @Test
    @DisplayName("Fail to initialize solid-oidc builder - token io failure")
    void failToInitBuilderTokenIOFailure() throws SaiException {

        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentBadIoId).setApplication(applicationId).setScope(scopes)
                .setPrompt(prompt).setRedirect(redirect).prepareCodeRequest();
        URL responseUrl = toUrl(server, redirectPath + "?code=" + code + "&state=" + builder.getAuthorizationRequest().getState());
        builder.processCodeResponse(responseUrl);
        assertThrows(SaiException.class, () -> builder.requestTokens());

    }

    @Test
    @DisplayName("Fail to initialize solid-oidc builder - unknown access token type")
    void failToInitBuilderTokenUnknown() throws SaiException {

        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentUnknownId).setApplication(applicationId).setScope(scopes)
                .setPrompt(prompt).setRedirect(redirect).prepareCodeRequest();
        URL responseUrl = toUrl(server, redirectPath + "?code=" + code + "&state=" + builder.getAuthorizationRequest().getState());
        builder.processCodeResponse(responseUrl);
        assertThrows(SaiException.class, () -> builder.requestTokens());

    }

    @Test
    @DisplayName("Initialize solid-oidc builder - request tokens no refresh")
    void initBuilderRequestTokensNoRefresh() throws SaiException {

        List<String> noRefreshScopes = Arrays.asList("openid", "profile");
        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentNoRefreshId).setApplication(applicationId).setScope(scopes)
                .setPrompt(prompt).setRedirect(redirect).prepareCodeRequest();
        URL responseUrl = toUrl(server, redirectPath + "?code=" + code + "&state=" + builder.getAuthorizationRequest().getState());
        builder.processCodeResponse(responseUrl).requestTokens();
        assertNotNull(builder.getAccessToken());
        assertNull(builder.getRefreshToken());
    }

    @Test
    @DisplayName("Initialize solid-oidc builder - build session")
    void initBuilderBuildSession() throws SaiException {

        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentId).setApplication(applicationId).setScope(scopes)
                .setPrompt(prompt).setRedirect(redirect).prepareCodeRequest();
        URL responseUrl = toUrl(server, redirectPath + "?code=" + code + "&state=" + builder.getAuthorizationRequest().getState());
        SolidOidcSession session = builder.processCodeResponse(responseUrl).requestTokens().build();
        assertNotNull(session);
        assertEquals(socialAgentId, session.getSocialAgentId());
        assertEquals(applicationId, session.getApplicationId());
        assertEquals(oidcProviderId.toString(), session.getOidcProviderMetadata().getIssuer().toString());
        assertNotNull(session.getAccessToken());
        assertNotNull(session.getRefreshToken());
        assertNotNull(session.getProofFactory());
        assertTrue(session.toHttpHeaders(GET, redirect).containsKey(AUTHORIZATION.getValue()));
        assertTrue(session.toHttpHeaders(GET, redirect).get(AUTHORIZATION.getValue()).startsWith("DPoP"));
        assertTrue(session.toHttpHeaders(GET, redirect).containsKey(DPOP.getValue()));
        assertNotNull(session.getId("SHA-512"));
    }

    @Test
    @DisplayName("Refresh solid-oidc session")
    void initBuilderRefreshSession() throws SaiException {

        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentRefreshId).setApplication(applicationId).setScope(scopes)
                .setPrompt(prompt).setRedirect(redirect).prepareCodeRequest();
        URL responseUrl = toUrl(server, redirectPath + "?code=" + code + "&state=" + builder.getAuthorizationRequest().getState());
        SolidOidcSession session = builder.processCodeResponse(responseUrl).requestTokens().build();
        assertNotNull(session);
        AccessToken originalAccessToken = session.getAccessToken();
        RefreshToken originalRefreshToken = session.getRefreshToken();
        session.refresh();
        assertNotEquals(originalAccessToken, session.getAccessToken());
        assertNotEquals(originalRefreshToken, session.getRefreshToken());
    }

    @Test
    @DisplayName("Fail to refresh session - no refresh token")
    void failToRefreshSessionNullRefresh() throws SaiException {
        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentNoRefreshId).setApplication(applicationId).setScope(scopes)
                .setPrompt(prompt).setRedirect(redirect).prepareCodeRequest();
        URL responseUrl = toUrl(server, redirectPath + "?code=" + code + "&state=" + builder.getAuthorizationRequest().getState());
        SolidOidcSession session = builder.processCodeResponse(responseUrl).requestTokens().build();
        assertThrows(SaiException.class, () -> session.refresh());
    }

    @Test
    @DisplayName("Fail to refresh session - unknown token type returned")
    void failToRefreshSessionUnknownToken() throws SaiException {
        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentRefreshUnknownId).setApplication(applicationId).setScope(scopes)
                .setPrompt(prompt).setRedirect(redirect).prepareCodeRequest();
        URL responseUrl = toUrl(server, redirectPath + "?code=" + code + "&state=" + builder.getAuthorizationRequest().getState());
        SolidOidcSession session = builder.processCodeResponse(responseUrl).requestTokens().build();
        assertThrows(SaiException.class, () -> session.refresh());
    }

    @Test
    @DisplayName("Refresh session - no refresh token returned")
    void failToRefreshSessionNoRefresh() throws SaiException {
        SolidOidcSession.Builder builder = new SolidOidcSession.Builder();
        builder.setHttpClient(httpClient).setSocialAgent(socialAgentRefreshNoRefreshId).setApplication(applicationId).setScope(scopes)
                .setPrompt(prompt).setRedirect(redirect).prepareCodeRequest();
        URL responseUrl = toUrl(server, redirectPath + "?code=" + code + "&state=" + builder.getAuthorizationRequest().getState());
        SolidOidcSession session = builder.processCodeResponse(responseUrl).requestTokens().build();
        assertNotNull(session);
        session.refresh();
        assertNotNull(session.getAccessToken());
        assertNull(session.getRefreshToken());
    }

    @Test
    @DisplayName("Fail to get dpop proof factory - invalid curve")
    void failToGetDpopProofFactory() {
        assertThrows(SaiException.class, () -> getDPoPProofFactory(new Curve("NOTREAL")));
    }

    @Test
    @DisplayName("Fail to get proof - jose error")
    void failToGetDpopProofJose() throws JOSEException {
        DefaultDPoPProofFactory mockProofFactory = mock(DefaultDPoPProofFactory.class);
        when(mockProofFactory.createDPoPJWT(anyString(), any(URI.class))).thenThrow(JOSEException.class);
        assertThrows(SaiException.class, () -> getProof(mockProofFactory, GET, redirect));
    }

}

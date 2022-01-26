package com.janeirodigital.sai.core.authorization;

import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.enums.HttpMethod;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockWebServer;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.*;
import static com.janeirodigital.sai.core.enums.HttpMethod.GET;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnGet;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnPost;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getIntegerObject;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getStringObject;
import static com.janeirodigital.sai.core.vocabularies.SolidOidcVocabulary.SOLID_OIDC_CLIENT_NAME;
import static com.janeirodigital.sai.core.vocabularies.SolidOidcVocabulary.SOLID_OIDC_DEFAULT_MAX_AGE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizedSessionHelperTests {

    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;
    private static HttpClientFactory clientFactory;
    private static OkHttpClient httpClient;
    private static URL socialAgentId;
    private static URL clientId;
    private static URL oidcProviderId;

    @BeforeAll
    static void beforeAll() throws SaiException {
        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
        // Good op configuration and webid pointing to that issuer
        mockOnGet(dispatcher, "/op/.well-known/openid-configuration", "authorization/op-configuration-json");
        mockOnGet(dispatcher, "/alice/id", "authorization/alice-webid-ttl");
        // Op configuration with mismatch on issuer
        mockOnGet(dispatcher, "/mismatch/.well-known/openid-configuration", "authorization/op-configuration-json");
        // In a given test, the first request to this endpoint will return provider-response, the second will return provider-refresh (a different token)
        mockOnPost(dispatcher, "/op/token", List.of("http/token-provider-response-json", "http/token-provider-refresh-json"));
        // Good client identity document for projectron
        mockOnGet(dispatcher, "/projectron", "authorization/projectron-clientid-jsonld");
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);

        clientFactory = new HttpClientFactory(false, false, false);
        httpClient = clientFactory.get();

        clientId = toUrl(server, "/projectron");
        socialAgentId = toUrl(server, "/alice/id#me");
        oidcProviderId = toUrl(server, "/op/");

    }

    @Test
    @DisplayName("Get oidc provider configuration")
    void getConfigurationForOIDCProvider() throws SaiException, MalformedURLException {
        OIDCProviderMetadata oidcProvider = getOIDCProviderConfiguration(toUrl(server, "/op/"));
        assertEquals(toUrl(server,"/op/token"), oidcProvider.getTokenEndpointURI().toURL());
        assertEquals(toUrl(server,"/op/auth"), oidcProvider.getAuthorizationEndpointURI().toURL());
        assertEquals(toUrl(server,"/op/reg"), oidcProvider.getRegistrationEndpointURI().toURL());
    }

    @Test
    @DisplayName("Fail to get oidc provider configuration - issuer mismatch")
    void failToGetConfigurationIssuerMismatch() {
        assertThrows(SaiException.class, () -> { getOIDCProviderConfiguration(toUrl(server, "/mismatch/")); });
    }

    @Test
    @DisplayName("Fail to get oidc provider configuration - missing configuration")
    void failToGetConfigurationNoConfiguration() {
        assertThrows(SaiException.class, () -> { getOIDCProviderConfiguration(toUrl(server, "/missing/")); });
    }

    @Test
    @DisplayName("Get oidc issuer for social agent")
    void getIssuer() throws SaiException {
        URL issuer = getOidcIssuerForSocialAgent(httpClient, socialAgentId);
        assertEquals(issuer, oidcProviderId);
    }

    @Test
    @DisplayName("Fail to get oidc issuer for social agent - missing id document")
    void failToGetIssuerMissingId() throws SaiException {
        URL missingId = toUrl(server, "/missing");
        assertThrows(SaiException.class, () -> getOidcIssuerForSocialAgent(httpClient, missingId));
    }

    @Test
    @DisplayName("Get client id document for application")
    void getClientId() throws SaiException {
        Resource clientIdDocument = getClientIdDocument(httpClient, clientId);
        assertNotNull(clientIdDocument);
        assertEquals(3600, getIntegerObject(clientIdDocument, SOLID_OIDC_DEFAULT_MAX_AGE));
        assertEquals("Projectron", getStringObject(clientIdDocument, SOLID_OIDC_CLIENT_NAME));
    }

    @Test
    @DisplayName("Fail to get client id document for application - missing document")
    void failToGetClientIdMissingDocument() throws SaiException {
        URL missingId = toUrl(server, "/missing");
        assertThrows(SaiException.class, () -> getClientIdDocument(httpClient, missingId));
    }

    @Test
    @DisplayName("Get access token from Request")
    void getRequestAccessToken() throws SaiException {
        String tokenValue = "MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3";
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        Map<String, String> authorizationHeader = Map.of(HttpHeader.AUTHORIZATION.getValue(), "Bearer " + tokenValue);
        when(mockSession.toHttpHeaders(any(HttpMethod.class), any(URL.class))).thenReturn(authorizationHeader);

        URL resourceUrl = toUrl(server, "/protected");
        Headers headers = setAuthorizationHeaders(mockSession, GET, resourceUrl, null);
        Request.Builder requestBuilder = new Request.Builder();
        Request request = requestBuilder.url(resourceUrl).method(GET.getValue(), null).headers(headers).build();
        AccessToken token = getAccessTokenFromRequest(request);
        assertEquals(tokenValue, token.getValue());
    }

    @Test
    @DisplayName("Fail to get token from Request - no authorization headers")
    void failToGetRequestTokenNoHeaders() {
        URL resourceUrl = toUrl(server, "/protected");
        Request.Builder requestBuilder = new Request.Builder();
        Request request = requestBuilder.url(resourceUrl).method(GET.getValue(), null).build();
        AccessToken token = getAccessTokenFromRequest(request);
        assertNull(token);
    }

    @Test
    @DisplayName("Fail to get token from Request - invalid authorization header")
    void failToGetRequestTokenInvalidHeader() throws SaiException {
        AuthorizedSession mockSession = mock(AuthorizedSession.class);
        Map<String, String> authorizationHeader = Map.of(HttpHeader.AUTHORIZATION.getValue(), "INVALIDVALUE");
        when(mockSession.toHttpHeaders(any(HttpMethod.class), any(URL.class))).thenReturn(authorizationHeader);

        URL resourceUrl = toUrl(server, "/protected");
        Headers headers = setAuthorizationHeaders(mockSession, GET, resourceUrl, null);
        Request.Builder requestBuilder = new Request.Builder();
        Request request = requestBuilder.url(resourceUrl).method(GET.getValue(), null).headers(headers).build();
        AccessToken token = getAccessTokenFromRequest(request);
        assertNull(token);
    }

    // TODO - add specific test for protected http helpers. They're covered well throughout other tests but for completeness.
    // getProtectedResource (with headers) -- success
    // getProtectedRdfResource (with headers) -- success
    // putProtectedRdfResource (with headers) -- success
    // deleteProtectedRdfResource (with headers) -- success


}

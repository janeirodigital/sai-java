package com.janeirodigital.sai.core.authorization;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.fixtures.RequestMatchingFixtureDispatcher;
import com.nimbusds.openid.connect.sdk.op.OIDCProviderMetadata;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.util.List;

import static com.janeirodigital.sai.core.authorization.AuthorizedSessionHelper.getOIDCProviderConfiguration;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnGet;
import static com.janeirodigital.sai.core.fixtures.DispatcherHelper.mockOnPost;
import static com.janeirodigital.sai.core.fixtures.MockWebServerHelper.toUrl;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AuthorizedSessionHelperTests {

    private static MockWebServer server;
    private static RequestMatchingFixtureDispatcher dispatcher;

    @BeforeAll
    static void beforeAll() {
        // Initialize request fixtures for the MockWebServer
        dispatcher = new RequestMatchingFixtureDispatcher();
        // In a given test, the first request to this endpoint will return provider-response, the second will return provider-refresh (a different token)
        mockOnGet(dispatcher, "/op/.well-known/openid-configuration", "authorization/op-configuration-json");
        mockOnGet(dispatcher, "/mismatch/.well-known/openid-configuration", "authorization/op-configuration-json");
        mockOnGet(dispatcher, "/badop/.well-known/openid-configuration", "authorization/op-configuration-badtoken-json");
        mockOnPost(dispatcher, "/op/token", List.of("http/token-provider-response-json", "http/token-provider-refresh-json"));
        // Initialize the Mock Web Server and assign the initialized dispatcher
        server = new MockWebServer();
        server.setDispatcher(dispatcher);
    }

    @Test
    @DisplayName("Get OIDC Provider configuration")
    void getConfigurationForOIDCProvider() throws SaiException, MalformedURLException {
        OIDCProviderMetadata oidcProvider = getOIDCProviderConfiguration(toUrl(server, "/op/"));
        assertEquals(toUrl(server,"/op/token"), oidcProvider.getTokenEndpointURI().toURL());
        assertEquals(toUrl(server,"/op/auth"), oidcProvider.getAuthorizationEndpointURI().toURL());
        assertEquals(toUrl(server,"/op/reg"), oidcProvider.getRegistrationEndpointURI().toURL());
    }

    @Test
    @DisplayName("Fail to get OIDC Provider configuration - issuer mismatch")
    void failToGetConfigurationIssuerMismatch() {
        assertThrows(SaiException.class, () -> { getOIDCProviderConfiguration(toUrl(server, "/mismatch/")); });
    }

    @Test
    @DisplayName("Fail to get OIDC Provider configuration - missing configuration")
    void failToGetConfigurationNoConfiguration() {
        assertThrows(SaiException.class, () -> { getOIDCProviderConfiguration(toUrl(server, "/missing/")); });
    }

}

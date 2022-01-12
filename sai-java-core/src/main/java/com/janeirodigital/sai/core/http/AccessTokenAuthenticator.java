package com.janeirodigital.sai.core.http;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Authenticator;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

import static com.janeirodigital.sai.core.enums.HttpHeader.AUTHORIZATION;

/**
 * Leverages the OkHttp
 * <a href="https://square.github.io/okhttp/3.x/okhttp/okhttp3/Authenticator.html">Authenticator API</a>
 * to react to HTTP 401 Not Authorized responses that may arise as a result of an expired or invalid
 * token. Tokens are obtained and refreshed through the {@link AccessTokenProvider}.
 */
@Slf4j
public class AccessTokenAuthenticator implements Authenticator {

    private final AccessTokenProvider tokenProvider;

    public AccessTokenAuthenticator(AccessTokenProvider tokenProvider) {
        Objects.requireNonNull(tokenProvider, "Must supply a token provider for the access token authenticator");
        this.tokenProvider = tokenProvider;
    }

    /**
     * In the event that a request receives a 401 Unauthorized, this method will be automatically called
     * by the OkHttp client (if added during client initialization), and will attempt to get a valid token,
     * refreshing if it must. This authenticator blocks all requests while an updated token is being obtained.
     * In-flight requests that fail with a 401 are automatically retried.
     * @param route Optional OkHttp Route
     * @param response OkHttp Response
     * @return OkHttp Request with updated token in Authorization header
     */
    @Override
    public Request authenticate(Route route, @NotNull Response response) {

        // Get current access token
        String originalToken = getAccessToken(this.tokenProvider);
        // If we can't get an access token, or the original request didn't include an authorization header
        if (originalToken == null || response.request().header(AUTHORIZATION.getValue()) == null) { return null; }

        // Only one thread at a time will go through this
        synchronized (this) {

            // Check and see if the token has been updated (e.g. by another thread that already went through this)
            String recentToken = getAccessToken(this.tokenProvider);
            if (recentToken == null) { return null; }

            // Check if the initial request (that the 401 was sent for) included an access token in the Authorization header
            if (response.request().header(AUTHORIZATION.getValue()) != null) {

                // Use the new token if the token has changed
                if (!recentToken.equals(originalToken)) { return replaceAuthorizationValue(response, recentToken); }

                // Otherwise refresh the token
                String refreshedToken = refreshAccessToken(this.tokenProvider);
                if (refreshedToken == null) { return null; }

                // Retry the request with the refreshed token
                return replaceAuthorizationValue(response, refreshedToken);
            }
        }
        return null;
    }

    private String getAccessToken(AccessTokenProvider tokenProvider) {
        try { return tokenProvider.getAccessToken(); } catch (IOException ex) {
            log.error("Failed to get access token from the token provider: {}", ex.getMessage());
            return null;
        }
    }

    private String refreshAccessToken(AccessTokenProvider tokenProvider) {
        try { return tokenProvider.refreshAccessToken(); } catch (IOException ex) {
            log.error("Failed to get refreshed access token: {}", ex.getMessage());
            return null;
        }
    }

    private Request replaceAuthorizationValue(Response response, String accessToken) {
        String authorizationValue = "Bearer " + accessToken;
        // Remove the previous authorization header, and replace it with the provided access token
        return response.request().newBuilder()
                .removeHeader(AUTHORIZATION.getValue())
                .addHeader(AUTHORIZATION.getValue(), authorizationValue)
                .build();
    }

}

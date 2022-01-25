package com.janeirodigital.sai.core.authorization;

import com.janeirodigital.sai.core.enums.HttpMethod;
import com.janeirodigital.sai.core.exceptions.SaiException;

import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

/**
 * Interface implemented by different types of authorized sessions, typically
 * different types of OAuth2/OIDC flows. Keeps sai-java classes that require
 * credentials to access protected resources from having to care about the specifics
 * of how those credentials are acquired and maintained. See {@link SolidOidcSession} and
 * {@link ClientCredentialsSession} for implementation examples.
 */
public interface AuthorizedSession {

    /**
     * Gets the URL of the SocialAgent identity associated with the {@link AuthorizedSession}
     * @return URL of SocialAgent identity
     */
    URL getSocialAgentId();

    /**
     * Gets the URL of the Application identity associated with the {@link AuthorizedSession}
     * @return URL of Application identity
     */
    URL getApplicationId();

    /**
     * Gets the URL of the OIDC Provider that issued the tokens for the {@link AuthorizedSession}
     * @return URL of Application identity
     */
    URL getOidcProviderId();

    /**
     * Gets the {@link AccessToken} associated with the {@link AuthorizedSession}
     * @return {@link AccessToken}
     */
    AccessToken getAccessToken();

    /**
     * Gets the {@link RefreshToken} associated with the {@link AuthorizedSession}
     * @return {@link RefreshToken}
     */
    RefreshToken getRefreshToken();

    /**
     * Generates a map of HTTP authorization headers that can be added to an HTTP request when
     * accessing protected resources. Some types of sessions (e.g. DPoP) need to know the
     * HTTP method and target URL of the request to generate the headers.
     * @param method HTTP method of the request
     * @param url Target URL of the request
     * @return Map of Authorization Headers
     */
    Map<String, String> toHttpHeaders(HttpMethod method, URL url) throws SaiException;

    /**
     * Refreshes the token(s) associated with the {@link AuthorizedSession}
     * @throws SaiException
     */
    void refresh() throws SaiException;

    /**
     * Default method that returns a consistent session identifier across implementations
     * for an authorized session scoped to the social agent, application id, and openid provider.
     * @param algorithm Message digest algorithm to use
     * @return String identifier of an authorized session
     */
    default String getId(String algorithm) throws SaiException {
        String combined = getSocialAgentId().toString() + getApplicationId().toString() + getOidcProviderId().toString();
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] messageDigest = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            BigInteger no = new BigInteger(1, messageDigest);
            return no.toString(16);
        } catch (NoSuchAlgorithmException ex) {
            throw new SaiException("Failed to generate identifier for authorized session: " + ex.getMessage());
        }
    }

}

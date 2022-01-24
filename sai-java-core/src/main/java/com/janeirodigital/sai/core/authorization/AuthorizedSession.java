package com.janeirodigital.sai.core.authorization;

import com.janeirodigital.sai.core.enums.HttpMethod;
import com.janeirodigital.sai.core.exceptions.SaiException;

import java.math.BigInteger;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

// TODO - AUTH REFACTOR
public interface AuthorizedSession {

    URL getSocialAgentId();
    URL getApplicationId();
    URL getOidcProviderId();
    AccessToken getAccessToken();
    RefreshToken getRefreshToken();
    Map<String, String> toHttpHeaders(HttpMethod method, URL url) throws SaiException;
    void refresh() throws SaiException;

    /**
     * Default method that returns a consistent session identifier across implementations
     * for an authorized session scoped to the social agent, application id, and openid provider.
     * @return String identifier of an authorized session
     */
    default String getId() throws SaiException {
        String combined = getSocialAgentId().toString() + getApplicationId().toString() + getOidcProviderId().toString();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            BigInteger no = new BigInteger(1, messageDigest);
            String hash = no.toString(16);
            while (hash.length() < 32) { hash = "0" + hash; }
            return hash;
        } catch (NoSuchAlgorithmException ex) {
            throw new SaiException("Failed to generate identifier for authorized session: " + ex.getMessage());
        }
    }

}

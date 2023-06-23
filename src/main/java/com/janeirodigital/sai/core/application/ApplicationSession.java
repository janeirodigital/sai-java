package com.janeirodigital.sai.core.application;

import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Representing an instance of the application scoped to a given
 * [Social Agent](https://solid.github.io/data-interoperability-panel/specification/#social-agents)
 * that has or is in the process of authorizing access to data under their control.
 */
@Getter @AllArgsConstructor
public class ApplicationSession {

    @NonNull
    private final Application application;
    @NonNull
    private final AuthorizedSession authorizedSession;
    @NonNull
    private final HttpClientFactory clientFactory;
    @NonNull
    private final SaiSession saiSession;

    private static final String DIGEST_ALGORITHM = "SHA-512";

    /**
     * Returns a consistent session identifier for an application session
     * @param socialAgentId social agent identifier
     * @param applicationId application identifier
     * @param oidcProviderId oidc provider identifier
     * @return String identifier of an application session
     */
    public static String
    generateId(URI socialAgentId, URI applicationId, URI oidcProviderId) throws SaiException {
        Objects.requireNonNull(socialAgentId, "Must provide a social agent identifier for session id generation");
        Objects.requireNonNull(applicationId, "Must provide an application identifier for session id generation");
        Objects.requireNonNull(oidcProviderId, "Must provide an oidc provider identifier for session id generation");
        String combined = socialAgentId.toString() + applicationId.toString() + oidcProviderId.toString();
        try {
            MessageDigest md = MessageDigest.getInstance(DIGEST_ALGORITHM);
            byte[] messageDigest = md.digest(combined.getBytes(StandardCharsets.UTF_8));
            BigInteger no = new BigInteger(1, messageDigest);
            return no.toString(16);
        } catch (NoSuchAlgorithmException ex) {
            throw new SaiException("Failed to generate identifier for application session", ex);
        }
    }

    public String getId() throws SaiException {
        return generateId(this.authorizedSession.getSocialAgentId(),
                          this.authorizedSession.getApplicationId(),
                          this.authorizedSession.getOidcProviderId());
    }

}

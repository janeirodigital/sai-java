package com.janeirodigital.sai.core.application;

import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.authentication.SaiAuthenticationException;
import com.janeirodigital.sai.core.exceptions.SaiException;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class
BasicApplicationSessionAccessor implements ApplicationSessionAccessor {

    private static final String DIGEST_ALGORITHM = "SHA-512";
    private final ConcurrentHashMap<String, ApplicationSession> sessions;

    /**
     * Initializes a new Concurrent (thread safe) Hash Map for storage and retrieval
     * of an {@link ApplicationSession}
     */
    public BasicApplicationSessionAccessor() { this.sessions = new ConcurrentHashMap<>(); }

    /**
     * Gets an {@link ApplicationSession} from the in-memory store based on the
     * provided {@link AuthorizedSession}
     * @param session {@link AuthorizedSession} to get
     * @return {@link ApplicationSession} matching the provided session or null
     */
    @Override
    public ApplicationSession get(AuthorizedSession session) throws SaiException {

        return this.sessions.searchValues(1, value -> {
            try {
                if (session.getId(DIGEST_ALGORITHM).equals(value.getAuthorizedSession().getId(DIGEST_ALGORITHM))) {
                    return value;
                } else {
                    return null;
                }
            } catch (SaiAuthenticationException ex) {
                return null;
            }
        });
    }

    @Override
    public ApplicationSession get(URI socialAgentId, URI applicationId, URI oidcProviderId) throws SaiException {
        String identifier = ApplicationSession.generateId(socialAgentId, applicationId, oidcProviderId);
        return this.sessions.get(identifier);
    }


    @Override
    public ApplicationSession get(ApplicationSession session) throws SaiException {
        return this.sessions.get(session.getId());
    }

    /**
     * Updates in-memory session store with the provided {@link ApplicationSession}
     * @param session session to store
     * @throws SaiException
     */
    @Override
    public void store(ApplicationSession session) throws SaiException { this.sessions.put(session.getId(), session); }

    /**
     * Returns the size of the in-memory session store
     * @return number of sessions
     */
    public int size() { return this.sessions.size(); }

}

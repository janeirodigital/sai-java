package com.janeirodigital.sai.core.authorization;

import com.janeirodigital.sai.core.exceptions.SaiException;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Basic in-memory implementation of {@link AuthorizedSessionAccessor} when the consumer
 * of sai-java doesn't provide their own implementation.
 */
@Slf4j
public class BasicAuthorizedSessionAccessor implements AuthorizedSessionAccessor {

    private final ConcurrentHashMap<String, AuthorizedSession> sessions;

    /**
     * Initializes a new Concurrent (thread safe) Hash Map for storage and retrieval
     * of an {@link AuthorizedSession}
     */
    public BasicAuthorizedSessionAccessor() { this.sessions = new ConcurrentHashMap<>(); }

    /**
     * Gets the provided {@link AuthorizedSession} from the in-memory store
     * @param session {@link AuthorizedSession} to get
     * @return
     */
    @Override
    public AuthorizedSession get(AuthorizedSession session) throws SaiException {
        return this.sessions.get(session.getId());
    }

    /**
     * Searches the in-memory session store for an {@link AuthorizedSession} with the same access token value
     * as the one in the provided <code>accessToken</code>.
     * @param accessToken Access token to lookup session with
     * @return {@link AuthorizedSession} matching the provided access token
     */
    @Override
    public AuthorizedSession get(AccessToken accessToken) {
        return this.sessions.searchValues(1, value -> {
            if (accessToken.getValue().equals(value.getAccessToken().getValue())) { return value; } else { return null; }
        });
    }

    /**
     * Refreshes the {@link AuthorizedSession} and updates the in-memory session store with the new values
     * @param session {@link AuthorizedSession} to refresh
     * @return Refreshed {@link AuthorizedSession}
     */
    @Override
    public AuthorizedSession refresh(AuthorizedSession session) throws SaiException {
        session.refresh();
        this.sessions.replace(session.getId(), session);
        return session;
    }

    public void store(AuthorizedSession session) throws SaiException { this.sessions.put(session.getId(), session); }

}

package com.janeirodigital.sai.core.authorization;

import com.janeirodigital.sai.core.exceptions.SaiException;

/**
 * Provides an interface for sai-java to lookup an {@link AuthorizedSession} based
 * on an AccessToken or the session itself, which is necessary for applications
 * that operate on behalf of multiple social agents.
 */
public interface AuthorizedSessionAccessor {

    /**
     * Get an {@link AuthorizedSession} based on the value of an {@link AccessToken}
     * @param accessToken {@link AccessToken} to lookup session for
     * @return {@link AuthorizedSession} or null if it can't be found
     */
    AuthorizedSession get(AccessToken accessToken);

    /**
     * Get the provided {@link AuthorizedSession}
     * @param session {@link AuthorizedSession} to lookup
     * @return {@link AuthorizedSession} or null if it can't be found
     */
    AuthorizedSession get(AuthorizedSession session) throws SaiException;

    /**
     * Refreshes and updates the stored version of the {@link AuthorizedSession}
     * @param session {@link AuthorizedSession} to refresh and update
     * @return Refreshed and updated {@link AuthorizedSession}
     * @throws SaiException
     */
    AuthorizedSession refresh(AuthorizedSession session) throws SaiException;

}

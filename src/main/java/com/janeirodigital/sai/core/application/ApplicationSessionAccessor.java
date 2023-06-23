package com.janeirodigital.sai.core.application;

import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;

import java.net.URI;

/**
 * Provides an interface for sai-java to lookup an {@link ApplicationSession} based
 * on an AccessToken, AuthorizedSession, or the ApplicationSession itself
 */
public interface ApplicationSessionAccessor {

    /**
     * Get the provided {@link ApplicationSession}
     * @param session {@link ApplicationSession} to lookup
     * @return {@link ApplicationSession} or null if it can't be found
     */
    ApplicationSession get(ApplicationSession session) throws SaiException;

    /**
     * Get an {@link ApplicationSession} based on the provided {@link AuthorizedSession}
     * @param session {@link AuthorizedSession} to lookup
     * @return {@link ApplicationSession} or null if it can't be found
     */
    ApplicationSession get(AuthorizedSession session) throws SaiException;

    /**
     * Get an {@link ApplicationSession} matching the provided <code>socialAgentId</code>,
     * <code>applicationId</code>, <code>oidcProviderId</code>
     * @param socialAgentId URI identifier of the session's social agent
     * @param applicationId URI identifier of the session's application
     * @param oidcProviderId URI identifier of the session's openid connect provider
     * @return {@link ApplicationSession} or null if it can't be found
     */
    ApplicationSession get(URI socialAgentId, URI applicationId, URI oidcProviderId) throws SaiException;

    /**
     * Store an {@link ApplicationSession}
     * @param session {@link ApplicationSession} to store
     * @return void
     */
    void store(ApplicationSession session) throws SaiException;

    /**
     * Get the number of {@link ApplicationSession} sessions
     * @return int number of sessions
     */
    int size();

}

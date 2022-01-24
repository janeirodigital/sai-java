package com.janeirodigital.sai.application;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;

import java.util.Objects;

/**
 * Factory for creation of authorized {@link ApplicationSession} instances
 */
public class ApplicationSessionFactory {

    /**
     * Gets an {@link ApplicationSession} instance for the provided {@link Application} scoped to the
     * provided {@link AuthorizedSession}. Will also initialize a similarly scoped {@link ApplicationDataFactory}.
     * @param application {@link Application} to initialize the session for
     * @param authorizedSession Established {@link AuthorizedSession} for access to protected resources
     * @return {@link ApplicationSession}
     */
    public static ApplicationSession get(Application application, AuthorizedSession authorizedSession) {
        Objects.requireNonNull(application, "Must provide an application to initialize an application session");
        Objects.requireNonNull(authorizedSession, "Must provide an authorized session to initialize an application session");
        Objects.requireNonNull(application.getClientFactory(), "Cannot initialize an application session when the provided application has no client factory");
        ApplicationDataFactory dataFactory = new ApplicationDataFactory(application, authorizedSession, application.getClientFactory());
        return new ApplicationSession(application, authorizedSession, application.getClientFactory(), dataFactory);
    }

}

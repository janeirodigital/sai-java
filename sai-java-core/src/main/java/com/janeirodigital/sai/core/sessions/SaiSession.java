package com.janeirodigital.sai.core.sessions;

import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import lombok.Getter;
import okhttp3.OkHttpClient;

import java.util.Objects;

/**
 * Base factory providing builders for many of the core data models
 * defined by the
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#app">Solid Application Interoperability specification</a>.
 */
@Getter
public class SaiSession {

    private final AuthorizedSession authorizedSession;
    private final HttpClientFactory clientFactory;

    /**
     * Initialize a sai session with the provided authorized session and
     * http client, which will be used for subsequent operations by the factory.
     * @param clientFactory Initialized {@link HttpClientFactory}
     * @param authorizedSession {@link AuthorizedSession} with credentials used for access to protected resources
     */
    public SaiSession(AuthorizedSession authorizedSession, HttpClientFactory clientFactory) {
        Objects.requireNonNull(authorizedSession, "Must provide an authorized session to initialize sai session");
        Objects.requireNonNull(clientFactory, "Must provide a client factory to initialize sai session");
        this.authorizedSession = authorizedSession;
        this.clientFactory = clientFactory;
    }

    /**
     * Get an OkHttpClient from the {@link HttpClientFactory} based on the
     * default configuration provided when the client factory was initialized.
     * @return OkHttpClient
     * @throws SaiException
     */
    public OkHttpClient getHttpClient() throws SaiException {
        return this.clientFactory.get();
    }

}

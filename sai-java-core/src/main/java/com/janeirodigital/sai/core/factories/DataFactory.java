package com.janeirodigital.sai.core.factories;

import com.janeirodigital.sai.core.authorization.AuthorizedSession;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.readable.ReadableApplicationProfile;
import com.janeirodigital.sai.core.readable.ReadableSocialAgentProfile;
import lombok.Getter;
import okhttp3.OkHttpClient;

import java.net.URL;
import java.util.Objects;

/**
 * Base factory providing builders for many of the core data models
 * defined by the
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#app">Solid Application Interoperability specification</a>.
 */
@Getter
public class DataFactory {

    private final AuthorizedSession authorizedSession;
    private final HttpClientFactory clientFactory;

    /**
     * Initialize a data factory with the provided authorized session and
     * http client, which will be used for subsequent operations by the factory.
     * @param clientFactory Initialized {@link HttpClientFactory}
     * @param authorizedSession {@link AuthorizedSession} with credentials used for access to protected resources
     */
    public DataFactory(AuthorizedSession authorizedSession, HttpClientFactory clientFactory) {
        Objects.requireNonNull(authorizedSession, "Must provide an authorized session to initialize data factory");
        Objects.requireNonNull(clientFactory, "Must provide a client factory to initialize data factory");
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

    /**
     * Get a read-only version of an Application Profile
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#app">Solid - Application Profile</a>
     * @param url URL of the {@link ReadableApplicationProfile} to read
     * @return {@link ReadableApplicationProfile}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public ReadableApplicationProfile getReadableApplicationProfile(URL url) throws SaiException, SaiNotFoundException {
        return ReadableApplicationProfile.get(url, this);
    }

    /**
     * Get a read-only version of a Social Agent Profile
     * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agents">Solid - Social Agent</a>
     * @param url URL of the {@link ReadableApplicationProfile} to read
     * @return {@link ReadableApplicationProfile}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public ReadableSocialAgentProfile getReadableSocialAgentProfile(URL url) throws SaiNotFoundException, SaiException {
        return ReadableSocialAgentProfile.get(url, this);
    }

}

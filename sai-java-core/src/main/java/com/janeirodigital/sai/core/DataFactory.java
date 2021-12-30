package com.janeirodigital.sai.core;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.readable.ReadableApplicationProfile;
import lombok.Getter;
import okhttp3.OkHttpClient;

import java.net.URL;

/**
 * Base factory providing builders for many of the core data models
 * defined by the
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#app">Solid Application Interoperability specification</a>.
 */
@Getter
public class DataFactory {

    private final HttpClientFactory clientFactory;

    /**
     * Initialize a data factory with the provided http client, which
     * will be used for subsequent operations by the factory.
     * @param clientFactory Initialized {@link HttpClientFactory}
     */
    public DataFactory(HttpClientFactory clientFactory) {
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
     * @param url
     * @return Readable Application Profile
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public ReadableApplicationProfile getReadableApplicationProfile(URL url) throws SaiException, SaiNotFoundException {
        return ReadableApplicationProfile.build(url, this);
    }

}

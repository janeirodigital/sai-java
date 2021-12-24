package com.janeirodigital.sai.core;

import okhttp3.OkHttpClient;

/**
 * Data factory specifically for use by client applications
 * <a href="https://solid.github.io/data-interoperability-panel/specification/">as defined</a>
 * by the Solid Application Interoperability specification
 */
public class ApplicationFactory extends DataFactory {

    public ApplicationFactory(OkHttpClient httpClient) {
        super(httpClient);
    }

}

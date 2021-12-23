package com.janeirodigital.sai.core;

import okhttp3.OkHttpClient;

public class ApplicationFactory extends DataFactory {

    public ApplicationFactory(OkHttpClient httpClient) {
        super(httpClient);
    }

}

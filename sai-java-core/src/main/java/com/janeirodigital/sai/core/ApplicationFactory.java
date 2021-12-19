package com.janeirodigital.sai.core;

import okhttp3.OkHttpClient;

public class ApplicationFactory extends DataFactory {

    public ApplicationFactory(OkHttpClient httpClient) {
        super(httpClient);
    }
    // TODO - Add get for Social Agent that author's a given application...
        // ... That may belong in Base because AuthorizationAgent is likely to want to use it too

}

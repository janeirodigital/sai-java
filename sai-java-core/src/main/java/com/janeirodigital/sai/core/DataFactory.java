package com.janeirodigital.sai.core;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.readable.ReadableApplicationProfile;
import com.janeirodigital.sai.core.readable.ReadableSocialAgentProfile;
import lombok.Getter;
import okhttp3.OkHttpClient;

import java.net.URL;

@Getter
public class DataFactory {

    private final OkHttpClient httpClient;

    // TODO - Add caching

    public DataFactory(OkHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public ReadableSocialAgentProfile getReadableSocialAgentProfile(URL url) {
        return new ReadableSocialAgentProfile(url, this);
    }

    public ReadableApplicationProfile getReadableApplicationProfile(URL url) throws SaiException, SaiNotFoundException {
        return ReadableApplicationProfile.build(url, this);
    }

}

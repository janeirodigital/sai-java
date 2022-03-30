package com.janeirodigital.sai.application;

import com.janeirodigital.sai.authentication.AuthorizedSession;
import com.janeirodigital.sai.core.http.HttpClientFactory;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

/**
 * Representing an instance of the application scoped to a given
 * [Social Agent](https://solid.github.io/data-interoperability-panel/specification/#social-agents)
 * that has or is in the process of authorizing access to data under their control.
 */
@Getter @AllArgsConstructor
public class ApplicationSession {

    @NonNull
    private final Application application;
    @NonNull
    private final AuthorizedSession authorizedSession;
    @NonNull
    private final HttpClientFactory clientFactory;
    @NonNull
    private final SaiSession saiSession;

}

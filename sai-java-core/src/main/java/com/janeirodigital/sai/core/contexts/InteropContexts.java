package com.janeirodigital.sai.core.contexts;

/**
 * Remote JSON-LD Contexts used by the
 * <a href="https://solid.github.io/data-interoperability-panel/specification/">Solid Application Interoperability specification</a>
 */
public class InteropContexts {

    private InteropContexts() { }

    private static final String NS = "https://solid.github.io/data-interoperability-panel/specification/contexts/";
    public static final String APPLICATION_PROFILE_CONTEXT = NS + "application-profile.jsonld";
    public static final String SOCIAL_AGENT_PROFILE_CONTEXT = NS + "social-agent-profile.jsonld";
    public static final String REGISTRY_SET_CONTEXT = NS + "registry-set.jsonld";
    // TODO - Update to use NS when PR https://github.com/solid/data-interoperability-panel/pull/236 is merged
    public static final String AGENT_REGISTRY_CONTEXT = "https://deploy-preview-236--data-interoperability-panel.netlify.app/specification/contexts/agent-registry.jsonld";
    public static final String AGENT_REGISTRATION_CONTEXT = "https://deploy-preview-236--data-interoperability-panel.netlify.app/specification/contexts/agent-registration.jsonld";
}

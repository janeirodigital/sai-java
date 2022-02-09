package com.janeirodigital.sai.core.vocabularies;

import org.apache.jena.rdf.model.*;

/**
 * Properties and classes of the
 * <a href="http://www.w3.org/ns/solid/interop">Solid Interoperability vocabulary</a>
 */
public class InteropVocabulary {

    private InteropVocabulary() { }

    private static Model model = ModelFactory.createDefaultModel();

    // Namespace
    private static final String NS = "http://www.w3.org/ns/solid/interop#";
    public static final Resource NAMESPACE = model.createResource(NS);

    // Application
    public static final String APPLICATION = NS + "Application";
    public static final Property APPLICATION_NAME = model.createProperty(NS + "applicationName");
    public static final Property APPLICATION_DESCRIPTION = model.createProperty(NS + "applicationDescription");
    public static final Property APPLICATION_AUTHOR = model.createProperty(NS + "applicationAuthor");
    public static final Property APPLICATION_THUMBNAIL = model.createProperty(NS + "applicationThumbnail");
    public static final Property HAS_ACCESS_NEED_GROUP = model.createProperty(NS + "hasAccessNeedGroup");

    // Social Agent
    public static final String SOCIAL_AGENT = NS + "SocialAgent";
    public static final Property HAS_AUTHORIZATION_AGENT = model.createProperty(NS + "hasAuthorizationAgent");
    public static final Property HAS_REGISTRY_SET = model.createProperty(NS + "hasRegistrySet");
    public static final Property HAS_ACCESS_INBOX = model.createProperty(NS + "hasAccessInbox");

    // Registry Set
    public static final String REGISTRY_SET = NS + "RegistrySet";
    public static final Property HAS_AGENT_REGISTRY = model.createProperty(NS + "hasAgentRegistry");
    public static final Property HAS_ACCESS_CONSENT_REGISTRY = model.createProperty(NS + "hasAccessConsentRegistry");
    public static final Property HAS_DATA_REGISTRY = model.createProperty(NS + "hasDataRegistry");

    // Agent Registry
    public static final String AGENT_REGISTRY = NS + "AgentRegistry";
    public static final Property HAS_SOCIAL_AGENT_REGISTRATION = model.createProperty(NS + "hasSocialAgentRegistration");
    public static final Property HAS_APPLICATION_REGISTRATION = model.createProperty(NS + "hasApplicationRegistration");

    // Agent Registration
    public static final String SOCIAL_AGENT_REGISTRATION = NS + "SocialAgentRegistration";
    public static final String APPLICATION_REGISTRATION = NS + "ApplicationRegistration";
    public static final Property RECIPROCAL_REGISTRATION = model.createProperty(NS + "reciprocalRegistration");
    public static final Property HAS_ACCESS_GRANT = model.createProperty(NS + "hasAccessGrant");

    // Registration Properties used by various classes
    public static final Property REGISTERED_BY = model.createProperty(NS + "registeredBy");
    public static final Property REGISTERED_WITH = model.createProperty(NS + "registeredWith");
    public static final Property REGISTERED_AT = model.createProperty(NS + "registeredAt");
    public static final Property UPDATED_AT = model.createProperty(NS + "updatedAt");
    public static final Property REGISTERED_AGENT = model.createProperty(NS + "registeredAgent");






}

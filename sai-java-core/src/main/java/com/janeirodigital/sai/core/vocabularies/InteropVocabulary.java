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
}

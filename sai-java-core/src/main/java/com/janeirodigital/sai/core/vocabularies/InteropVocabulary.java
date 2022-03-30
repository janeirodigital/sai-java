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
    public static final RDFNode APPLICATION = model.getResource(NS + "Application");
    public static final Property APPLICATION_NAME = model.createProperty(NS + "applicationName");
    public static final Property APPLICATION_DESCRIPTION = model.createProperty(NS + "applicationDescription");
    public static final Property APPLICATION_AUTHOR = model.createProperty(NS + "applicationAuthor");
    public static final Property APPLICATION_THUMBNAIL = model.createProperty(NS + "applicationThumbnail");
    public static final Property HAS_ACCESS_NEED_GROUP = model.createProperty(NS + "hasAccessNeedGroup");

    // Social Agent
    public static final RDFNode SOCIAL_AGENT = model.getResource(NS + "SocialAgent");
    public static final Property HAS_AUTHORIZATION_AGENT = model.createProperty(NS + "hasAuthorizationAgent");
    public static final Property HAS_REGISTRY_SET = model.createProperty(NS + "hasRegistrySet");
    public static final Property HAS_ACCESS_INBOX = model.createProperty(NS + "hasAccessInbox");

    // Registry Set
    public static final RDFNode REGISTRY_SET = model.getResource(NS + "RegistrySet");
    public static final Property HAS_AGENT_REGISTRY = model.createProperty(NS + "hasAgentRegistry");
    public static final Property HAS_AUTHORIZATION_REGISTRY = model.createProperty(NS + "hasAuthorizationRegistry");
    public static final Property HAS_DATA_REGISTRY = model.createProperty(NS + "hasDataRegistry");

    // Agent Registry
    public static final RDFNode AGENT_REGISTRY = model.getResource(NS + "AgentRegistry");
    public static final Property HAS_SOCIAL_AGENT_REGISTRATION = model.createProperty(NS + "hasSocialAgentRegistration");
    public static final Property HAS_APPLICATION_REGISTRATION = model.createProperty(NS + "hasApplicationRegistration");

    // Agent Registration
    public static final RDFNode SOCIAL_AGENT_REGISTRATION = model.getResource(NS + "SocialAgentRegistration");
    public static final RDFNode APPLICATION_REGISTRATION = model.getResource(NS + "ApplicationRegistration");
    public static final Property RECIPROCAL_REGISTRATION = model.createProperty(NS + "reciprocalRegistration");
    public static final Property HAS_ACCESS_GRANT = model.createProperty(NS + "hasAccessGrant");

    // Access Authorizations
    public static final RDFNode AUTHORIZATION_REGISTRY = model.getResource(NS + "AuthorizationRegistry");
    public static final RDFNode ACCESS_AUTHORIZATION = model.getResource(NS + "AccessAuthorization");
    public static final Property HAS_ACCESS_AUTHORIZATION = model.createProperty(NS + "hasAccessAuthorization");
    public static final Property GRANTED_BY = model.createProperty(NS + "grantedBy");
    public static final Property GRANTED_WITH = model.createProperty(NS + "grantedWith");
    public static final Property GRANTED_AT = model.createProperty(NS + "grantedAt");
    public static final Property GRANTEE = model.createProperty(NS + "grantee");
    public static final Property HAS_DATA_AUTHORIZATION = model.createProperty(NS + "hasDataAuthorization");
    public static final Property REPLACES = model.createProperty(NS + "replaces");

    // Data Authorizations
    public static final RDFNode DATA_AUTHORIZATION = model.getResource(NS + "DataAuthorization");
    public static final Property DATA_OWNER = model.createProperty(NS + "dataOwner");
    public static final Property REGISTERED_SHAPE_TREE = model.createProperty(NS + "registeredShapeTree");
    public static final Property ACCESS_MODE = model.createProperty(NS + "accessMode");
    public static final Property CREATOR_ACCESS_MODE = model.createProperty(NS + "creatorAccessMode");
    public static final Property SCOPE_OF_AUTHORIZATION = model.createProperty(NS + "scopeOfAuthorization");
    public static final Property SATISFIES_ACCESS_NEED = model.createProperty(NS + "satisfiesAccessNeed");
    public static final Property INHERITS_FROM_AUTHORIZATION = model.createProperty(NS + "inheritsFromAuthorization");

    // Authorization Scopes
    public static final RDFNode SCOPE_ALL = model.getResource(NS + "All");
    public static final RDFNode SCOPE_ALL_FROM_REGISTRY = model.getResource(NS + "AllFromRegistry");
    public static final RDFNode SCOPE_ALL_FROM_AGENT = model.getResource(NS + "AllFromAgent");
    public static final RDFNode SCOPE_SELECTED_FROM_REGISTRY = model.getResource(NS + "SelectedFromRegistry");
    public static final RDFNode SCOPE_INHERITED = model.getResource(NS + "Inherited");
    public static final RDFNode SCOPE_NO_ACCESS = model.getResource(NS + "NoAccess");

    // Access Grants
    public static final RDFNode ACCESS_GRANT = model.getResource(NS + "AccessGrant");
    public static final Property HAS_DATA_GRANT = model.createProperty(NS + "hasDataGrant");

    // Data Grants
    public static final RDFNode DATA_GRANT = model.getResource(NS + "DataGrant");
    public static final RDFNode DELEGATED_DATA_GRANT = model.getResource(NS + "DelegatedDataGrant");
    public static final Property SCOPE_OF_GRANT = model.createProperty(NS + "scopeOfGrant");
    public static final Property INHERITS_FROM_GRANT = model.createProperty(NS + "inheritsFromGrant");
    public static final Property DELEGATION_OF_GRANT = model.createProperty(NS + "delegationOfGrant");


    // Data Registry
    public static final RDFNode DATA_REGISTRY = model.getResource(NS + "DataRegistry");

    // Data Registrations
    public static final RDFNode DATA_REGISTRATION = model.getResource(NS + "DataRegistration");
    public static final Property HAS_DATA_REGISTRATION = model.createProperty(NS + "hasDataRegistration");
    public static final Property HAS_DATA_INSTANCE = model.createProperty(NS + "hasDataInstance");

    // Registration Properties used by various classes
    public static final Property HAS_REGISTRATION = model.createProperty(NS + "hasRegistration");
    public static final Property REGISTERED_BY = model.createProperty(NS + "registeredBy");
    public static final Property REGISTERED_WITH = model.createProperty(NS + "registeredWith");
    public static final Property REGISTERED_AT = model.createProperty(NS + "registeredAt");
    public static final Property UPDATED_AT = model.createProperty(NS + "updatedAt");
    public static final Property REGISTERED_AGENT = model.createProperty(NS + "registeredAgent");

}

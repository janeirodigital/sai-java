package com.janeirodigital.sai.core.vocabularies;

import org.apache.jena.rdf.model.*;

public final class TestableVocabulary {

    private TestableVocabulary() { }

    private static Model model = ModelFactory.createDefaultModel();

    // Namespace
    private static final String NS = "http://testable.example/ns/testable#";
    public static final Resource NAMESPACE = model.createResource(NS);

    public static final RDFNode TESTABLE_PROJECT = model.getResource(NS + "TestableProject");
    public static final RDFNode TESTABLE_MILESTONE = model.getResource(NS + "TestableMilestone");
    public static final RDFNode TESTABLE_TASK = model.getResource(NS + "TestableTask");
    public static final RDFNode TESTABLE_ISSUE = model.getResource(NS + "TestableIssue");

    // Properties and Classes
    public static final Property TESTABLE_ID = model.createProperty(NS + "id");
    public static final Property TESTABLE_NAME = model.createProperty(NS + "name");
    public static final Property TESTABLE_DESCRIPTION = model.createProperty(NS + "description");
    public static final Property TESTABLE_CREATED_AT = model.createProperty(NS + "createdAt");
    public static final Property TESTABLE_ACTIVE = model.createProperty(NS + "active");
    public static final Property TESTABLE_HAS_MILESTONE = model.createProperty(NS + "hasMilestone");
    public static final Property TESTABLE_HAS_ISSUE = model.createProperty(NS + "hasIssue");
    public static final Property TESTABLE_HAS_TASK = model.createProperty(NS + "hasTask");
    public static final Property TESTABLE_HAS_TAG = model.createProperty(NS + "hasTag");
    public static final Property TESTABLE_HAS_COMMENT = model.createProperty(NS + "hasComment");
    public static final Property TESTABLE_MISSING = model.createProperty(NS + "isMissing");

}

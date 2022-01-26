package com.janeirodigital.sai.core;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

public final class TestableVocabulary {

    private TestableVocabulary() { }

    private static Model model = ModelFactory.createDefaultModel();

    // Namespace
    private static final String NS = "http://testable.example/ns/testable#";
    public static final Resource NAMESPACE = model.createResource(NS);

    // Properties and Classes
    public static final Property TESTABLE_ID = model.createProperty(NS + "id");
    public static final Property TESTABLE_NAME = model.createProperty(NS + "name");
    public static final Property TESTABLE_CREATED_AT = model.createProperty(NS + "createdAt");
    public static final Property TESTABLE_ACTIVE = model.createProperty(NS + "active");
    public static final Property TESTABLE_HAS_MILESTONE = model.createProperty(NS + "hasMilestone");
    public static final Property TESTABLE_HAS_TAG = model.createProperty(NS + "hasTag");
    public static final Property TESTABLE_HAS_COMMENT = model.createProperty(NS + "hasComment");
    public static final Property TESTABLE_MISSING = model.createProperty(NS + "isMissing");

}

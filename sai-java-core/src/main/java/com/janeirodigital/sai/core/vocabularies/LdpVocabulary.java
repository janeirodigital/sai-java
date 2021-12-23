package com.janeirodigital.sai.core.vocabularies;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Properties and classes of the
 * <a href="http://www.w3.org/ns/ldp">Linked Data Platform vocabulary</a>
 */
public final class LdpVocabulary {

    private LdpVocabulary() { }

    private static Model model = ModelFactory.createDefaultModel();

    // Namespace
    public static final String LDP_NS = "http://www.w3.org/ns/ldp#";
    public static final Resource NAMESPACE = model.createResource(LDP_NS);

    // Properties and Classes
    public static final Property CONTAINER = model.createProperty(LDP_NS + "Container");
    public static final Property BASIC_CONTAINER = model.createProperty(LDP_NS + "BasicContainer");
    public static final Property CONTAINS = model.createProperty(LDP_NS + "contains");

}

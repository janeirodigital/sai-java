package com.janeirodigital.sai.core.vocabularies;

import org.apache.jena.rdf.model.*;

public class SkosVocabulary {

    private SkosVocabulary() { }
    private static Model model = ModelFactory.createDefaultModel();
    // Namespace
    public static final String NS = "http://www.w3.org/2004/02/skos/core#";
    public static final Resource NAMESPACE = model.createResource(NS);
    // Properties and Classes
    public static final Property SKOS_PREF_LABEL = model.createProperty(NS + "prefLabel");
    public static final Property SKOS_DEFINITION = model.createProperty(NS + "definition");

}

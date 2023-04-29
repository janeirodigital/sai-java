package com.janeirodigital.sai.core.vocabularies;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

public class SkosVocabulary {

    private SkosVocabulary() { }
    private static Model model = ModelFactory.createDefaultModel();
    // Namespace
    public static final String NS = "http://www.w3.org/2004/02/skos/core#";
    public static final Resource NAMESPACE = model.createResource(NS);
    // Properties and Classes
    public static final RDFNode SKOS_PREF_LABEL = model.getResource(NS + "prefLabel");
    public static final RDFNode SKOS_DEFINITION = model.getResource(NS + "definition");

}

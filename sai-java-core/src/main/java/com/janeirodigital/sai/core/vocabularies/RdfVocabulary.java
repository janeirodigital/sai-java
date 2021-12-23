package com.janeirodigital.sai.core.vocabularies;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

/**
 * Properties and classes of the
 * <a href="http://www.w3.org/1999/02/22-rdf-syntax-ns">RDF vocabulary</a>
 */
public final class RdfVocabulary {

    private RdfVocabulary() { }

    private static Model model = ModelFactory.createDefaultModel();

    // Namespace
    private static final String NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    public static final Resource NAMESPACE = model.createResource(NS);

    // Properties and Classes
    public static final Property RDF_TYPE = model.createProperty(NS + "type");

}

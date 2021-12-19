package com.janeirodigital.sai.core.vocabularies;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;

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
    public static final Property APPLICATION_NAME = model.createProperty(NS + "applicationName");
    public static final Property APPLICATION_DESCRIPTION = model.createProperty(NS + "applicationDescription");
    public static final Property APPLICATION_AUTHOR = model.createProperty(NS + "applicationAuthor");
    public static final Property APPLICATION_THUMBNAIL = model.createProperty(NS + "applicationThumbnail");

    // Access Needs
    public static final Property HAS_ACCESS_NEED_GROUP = model.createProperty(NS + "hasAccessNeedGroup");

}
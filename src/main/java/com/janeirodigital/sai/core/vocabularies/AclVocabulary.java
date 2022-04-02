package com.janeirodigital.sai.core.vocabularies;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

public class AclVocabulary {

    private AclVocabulary() { }
    private static Model model = ModelFactory.createDefaultModel();
    // Namespace
    public static final String NS = "http://www.w3.org/ns/auth/acl#";
    public static final Resource NAMESPACE = model.createResource(NS);
    // Properties and Classes
    public static final RDFNode ACL_CREATE = model.getResource(NS + "Create");
    public static final RDFNode ACL_READ = model.getResource(NS + "Read");
    public static final RDFNode ACL_UPDATE = model.getResource(NS + "Update");
    public static final RDFNode ACL_DELETE = model.getResource(NS + "Delete");
    public static final RDFNode ACL_APPEND = model.getResource(NS + "Append");
    public static final RDFNode ACL_WRITE = model.getResource(NS + "Write");
    public static final RDFNode ACL_CONTROL = model.getResource(NS + "Control");

}

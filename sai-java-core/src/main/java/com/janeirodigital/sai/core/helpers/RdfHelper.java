package com.janeirodigital.sai.core.helpers;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.apache.jena.datatypes.xsd.XSDDatatype.*;

/**
 * Assorted helper methods related to the Jena RDF Model
 * @see <a href="https://jena.apache.org/documentation/javadoc/jena/org/apache/jena/rdf/model/Model.html">Jena - Model</a>
 * @see <a href="https://jena.apache.org/documentation/javadoc/jena/org/apache/jena/rdf/model/Resource.html">Jena - Resource</a>
 * @see <a href="https://jena.apache.org/documentation/javadoc/jena/org/apache/jena/rdf/model/Statement.html">Jena - Statement</a>
 * @see <a href="https://jena.apache.org/documentation/javadoc/jena/org/apache/jena/rdf/model/Property.html">Jena - Property</a>
 * @see <a href="https://jena.apache.org/documentation/javadoc/jena/org/apache/jena/rdf/model/RDFNode.html">Jena - RDFNode</a>
 */
public class RdfHelper {

    private RdfHelper() { }

    /**
     * Deserializes the provided String <code>rawContent</code> into a Jena Model
     * @param baseURI Base URI to use for statements
     * @param rawContent String of RDF
     * @param contentType Content type of content
     * @return Deserialized Jean Model
     * @throws SaiException
     */
    public static Model getModelFromString(URI baseURI, String rawContent, ContentType contentType) throws SaiException {
        Objects.requireNonNull(baseURI, "Must provide a baseURI to generate a model");
        Objects.requireNonNull(rawContent, "Must provide content to generate a model from");
        Objects.requireNonNull(contentType, "Must provide content type for model generation");
        try {
            Model model = ModelFactory.createDefaultModel();
            StringReader reader = new StringReader(rawContent);
            RDFDataMgr.read(model.getGraph(), reader, baseURI.toString(), RdfHelper.getLangForContentType(contentType));
            return model;
        } catch (RiotException ex) {
            throw new SaiException("Error processing input - " + ex.getMessage());
        }
    }

    /**
     * Deserializes the contents of the provided <code>filePath</code> into a Jena Model.
     * @param baseURI Base URI to use for statements
     * @param filePath Path to file containing input data
     * @param contentType Content type of file data
     * @return Deserialized Jena Model
     * @throws SaiException
     * @throws IOException
     */
    public static Model getModelFromFile(URI baseURI, String filePath, ContentType contentType) throws SaiException, IOException {
        Objects.requireNonNull(baseURI, "Must provide a baseURI to generate a model");
        Objects.requireNonNull(filePath, "Must provide an input file path to provide data for the generated model");
        Objects.requireNonNull(contentType, "Must provide content type for model generation");
        InputStream in = null;
        try {
            Model model = ModelFactory.createDefaultModel();
            in = RDFDataMgr.open(filePath);
            model.read(in, baseURI.toString(), contentType.getValue());
            return model;
        } catch (RiotException ex) {
            throw new SaiException("Error processing input - " + ex.getMessage());
        } finally {
            if (in != null) { in.close(); }
        }
    }

    /**
     * Get a String of the provided <code>model</code> serialized in <code>lang</code>.
     * @param model Jena Model to serialize
     * @param lang Format to serialize into
     * @return Serialized string of the provided model
     */
    public static String getStringFromRdfModel(Model model, Lang lang) {
        Objects.requireNonNull(model, "Cannot serialize a null model");
        Objects.requireNonNull(lang, "Must provide a serialization format");
        StringWriter sw = new StringWriter();
        RDFDataMgr.write(sw, model, lang);
        return sw.toString();
    }

    /**
     * Returns a jena Resource at the specified <code>resourceUrl</code> from the provided jena Model
     * @param model Model to search
     * @param resourceUrl URL of the resource to search for
     * @return Jena Resource at resourceUrl
     */
    public static Resource getResourceFromModel(Model model, URL resourceUrl) {
        Objects.requireNonNull(model, "Must provide a model to get a resource from it");
        Objects.requireNonNull(resourceUrl, "Must provide resource to get from model");
        return model.getResource(resourceUrl.toString());
    }

    /**
     * Returns a single Jena Statement matching the provided <code>property</code> in
     * the provided <code>resource</code>. When nothing is found null is returned.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return Jena Statement matching the provided Property or null
     */
    public static Statement getStatement(Resource resource, Property property) {
        Objects.requireNonNull(resource, "Cannot get a statement from a null resource");
        Objects.requireNonNull(property, "Cannot get a statement from a resource with a null property");
        return resource.getProperty(property);
    }

    /**
     * Returns a single Jena Statement matching the provided <code>property</code> in
     * the provided <code>resource</code>. If nothing is found an exception is thrown.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return Jena Statement matching the provided Property
     * @throws SaiNotFoundException when nothing is found
     */
    public static Statement getRequiredStatement(Resource resource, Property property) throws SaiNotFoundException {
        Statement statement = getStatement(resource, property);
        if (statement == null) { throw new SaiNotFoundException(msgNothingFound(resource, property)); }
        return statement;
    }

    /**
     * Returns a single Jena RDFNode matching the provided <code>property</code> in the
     * provided <code>resource</code>. When nothing is found null is returned.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return Jena RDFNode matching the provided property or null
     */
    public static RDFNode getObject(Resource resource, Property property) {
        Statement statement = getStatement(resource, property);
        if (statement == null) { return null; }
        return statement.getObject();
    }

    /**
     * Returns a single Jena RDFNode matching the provided <code>property</code> in the
     * provided <code>resource</code>. When nothing is found an exception is thrown.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return Jena RDFNode matching the provided property
     * @throws SaiNotFoundException when nothing is found
     */
    public static RDFNode getRequiredObject(Resource resource, Property property) throws SaiNotFoundException {
        return getRequiredStatement(resource, property).getObject();
    }

    /**
     * Returns a list of Jena RDFNodes matching the provided <code>property</code> in the
     * provided <code>resource</code>. When nothing is found an empty list is returned.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return List of Jena RDFNodes matching the provided property (possibly empty)
     */
    public static List<RDFNode> getObjects(Resource resource, Property property) {
        Objects.requireNonNull(resource, "Cannot get objects from a null resource");
        Objects.requireNonNull(property, "Cannot get objects from a resource with a null property");
        StmtIterator it = resource.listProperties(property);
        ArrayList<RDFNode> objects = new ArrayList<>();
        while (it.hasNext()) {
            Statement statement = it.next();
            objects.add(statement.getObject());
        }
        return objects;
    }

    /**
     * Returns a list of Jena RDFNodes matching the provided <code>property</code> in the
     * provided <code>resource</code>. When nothing is found an exception is thrown.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return List of Jena RDFNodes matching the provided property
     */
    public static List<RDFNode> getRequiredObjects(Resource resource, Property property) throws SaiNotFoundException {
        List<RDFNode> objects = getObjects(resource, property);
        if (objects.isEmpty()) { throw new SaiNotFoundException(msgNothingFound(resource, property)); }
        return objects;
    }

    /**
     * Returns a list of URLs matching the provided <code>property</code> in the
     * provided <code>resource</code>. When nothing is found an empty list is returned.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return List of URL object values matching the provided property (possibly empty)
     */
    public static List<URL> getUrlObjects(Resource resource, Property property) throws SaiException {
        Objects.requireNonNull(resource, "Cannot get URLs from a null resource");
        Objects.requireNonNull(property, "Cannot get URLs from a resource with a null property");
        StmtIterator it = resource.listProperties(property);
        ArrayList<URL> urls = new ArrayList<>();
        while (it.hasNext()) {
            Statement statement = it.next();
            RDFNode object = statement.getObject();
            if (!object.isResource()) { throw new SaiException(msgNotUrlResource(resource, property, object)); }
            urls.add(nodeToUrl(object));
        }
        return urls;
    }

    /**
     * Returns a list of URLs matching the provided <code>property</code> in the
     * provided <code>resource</code>. When nothing is found an exception is thrown.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return List of URLs matching the provided property
     */
    public static List<URL> getRequiredUrlObjects(Resource resource, Property property) throws SaiException, SaiNotFoundException {
        List<URL> urls = getUrlObjects(resource, property);
        if (urls.isEmpty()) { throw new SaiNotFoundException(msgNothingFound(resource, property)); }
        return urls;
    }

    /**
     * Returns a list of Strings matching the provided <code>property</code> in the
     * provided <code>resource</code>. When nothing is found an empty list is returned.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return List of String object values matching the provided property (possibly empty)
     */
    public static List<String> getStringObjects(Resource resource, Property property) throws SaiException {
        Objects.requireNonNull(resource, "Cannot get strings from a null resource");
        Objects.requireNonNull(property, "Cannot get strings from a resource with a null property");
        StmtIterator it = resource.listProperties(property);
        ArrayList<String> strings = new ArrayList<>();
        while (it.hasNext()) {
            Statement statement = it.next();
            RDFNode object = statement.getObject();
            if (!object.isLiteral()) { throw new SaiException(msgInvalidDataType(resource, property, XSDstring)); }
            if (!object.asLiteral().getDatatype().equals(XSDstring)) { throw new SaiException(msgInvalidDataType(resource, property, XSDstring)); }
            strings.add(object.asLiteral().getString());
        }
        return strings;
    }

    /**
     * Returns a list of Strings matching the provided <code>property</code> in the
     * provided <code>resource</code>. When nothing is found an exception is thrown.
     * @param resource Jena Resource to navigate
     * @param property Jena Property to search for
     * @return List of Strings matching the provided property
     */
    public static List<String> getRequiredStringObjects(Resource resource, Property property) throws SaiException, SaiNotFoundException {
        List<String> strings = getStringObjects(resource, property);
        if (strings.isEmpty()) { throw new SaiNotFoundException(msgNothingFound(resource, property)); }
        return strings;
    }

    /**
     * Returns a single URL value from the object of the statement matching the provided
     * <code>property</code> in the provided <code>resource</code>. Returns null when
     * no match is found.
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return URL object value or null
     * @throws SaiException
     */
    public static URL getUrlObject(Resource resource, Property property) throws SaiException {
        RDFNode object = getObject(resource, property);
        if (object == null) { return null; }
        if (!object.isResource()) { throw new SaiException(msgNotUrlResource(resource, property, object)); }
        return nodeToUrl(object);
    }

    /**
     * Returns a single URL value from the object of the statement matching the provided
     * <code>property</code> in the provided <code>resource</code>. Throws an exception
     * when no match is found.
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return URL object value
     * @throws SaiException
     * @throws SaiNotFoundException when nothing is found
     */
    public static URL getRequiredUrlObject(Resource resource, Property property) throws SaiException, SaiNotFoundException {
        URL url = getUrlObject(resource, property);
        if (url == null) { throw new SaiNotFoundException(msgNothingFound(resource, property)); }
        return url;
    }

    /**
     * Returns a single literal value as String from the object of the statement matching
     * the provided <code>property</code> in the provided <code>resource</code>. Returns
     * null when no match is found.
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return Literal object value as String or null
     * @throws SaiException
     */
    public static String getStringObject(Resource resource, Property property) throws SaiException {
        RDFNode object = getObject(resource, property);
        if (object == null) { return null; }
        if (!object.isLiteral()) { throw new SaiException(msgInvalidDataType(resource, property, XSDstring)); }
        if (!object.asLiteral().getDatatype().equals(XSDstring)) { throw new SaiException(msgInvalidDataType(resource, property, XSDstring)); }
        return object.asLiteral().getString();
    }

    /**
     * Returns a single literal value as String from the object of the statement matching
     * the provided <code>property</code> in the provided <code>resource</code>. Throws an
     * exception when no match is found.
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return Literal object value as String
     * @throws SaiException
     * @throws SaiNotFoundException when nothing is found
     */
    public static String getRequiredStringObject(Resource resource, Property property) throws SaiException, SaiNotFoundException {
        String string = getStringObject(resource, property);
        if (string == null) { throw new SaiNotFoundException(msgNothingFound(resource, property, XSDstring)); }
        return string;
    }

    /**
     * Returns a single literal value as Integer from the object of the statement matching
     * the provided <code>property</code> in the provided <code>resource</code>. Returns
     * null when no match is found.
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return Literal object value as Integer or null
     * @throws SaiException
     */
    public static Integer getIntegerObject(Resource resource, Property property) throws SaiException {
        RDFNode object = getObject(resource, property);
        if (object == null) { return null; }
        if (!object.isLiteral()) { throw new SaiException(msgInvalidDataType(resource, property, XSDinteger)); }
        if (!object.asLiteral().getDatatype().equals(XSDinteger)) { throw new SaiException(msgInvalidDataType(resource, property, XSDinteger)); }
        return object.asLiteral().getInt();
    }

    /**
     * Returns a single literal value as Integer from the object of the statement matching
     * the provided <code>property</code> in the provided <code>resource</code>. Throws
     * an exception when no match is found
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return Literal object value as Integer
     * @throws SaiException
     * @throws SaiNotFoundException when nothing is found
     */
    public static Integer getRequiredIntegerObject(Resource resource, Property property) throws SaiException, SaiNotFoundException {
        Integer i = getIntegerObject(resource, property);
        if (i == null) { throw new SaiNotFoundException(msgNothingFound(resource, property, XSDinteger)); }
        return i;
    }

    /**
     * Returns a single literal value as OffsetDateTime from the object of the statement matching
     * the provided <code>property</code> in the provided <code>resource</code>. Returns null
     * when no match is found
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return Literal object value as OffsetDateTime or null
     * @throws SaiException
     */
    public static OffsetDateTime getDateTimeObject(Resource resource, Property property) throws SaiException {
        RDFNode object = getObject(resource, property);
        if (object == null) { return null; }
        if (!object.isLiteral()) { throw new SaiException(msgInvalidDataType(resource, property, XSDdateTime)); }
        if (!object.asLiteral().getDatatype().equals(XSDdateTime)) { throw new SaiException(msgInvalidDataType(resource, property, XSDdateTime)); }
        return OffsetDateTime.parse(object.asLiteral().getString(), DateTimeFormatter.ISO_DATE_TIME);
    }

    /**
     * Returns a single literal value as OffsetDateTime from the object of the statement matching
     * the provided <code>property</code> in the provided <code>resource</code>. Throws an
     * exception when no match is found.
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return Literal object value as OffsetDateTime
     * @throws SaiException
     * @throws SaiNotFoundException when nothing is found
     */
    public static OffsetDateTime getRequiredDateTimeObject(Resource resource, Property property) throws SaiException, SaiNotFoundException {
        OffsetDateTime dateTime = getDateTimeObject(resource, property);
        if (dateTime == null) { throw new SaiNotFoundException(msgNothingFound(resource, property, XSDdateTime)); }
        return dateTime;
    }

    /**
     * Returns a single literal value as Boolean from the object of the statement matching
     * the provided <code>property</code> in the provided <code>resource</code>. Returns an exception
     * when no match is found. <i>Note: This method and the corresponding getRequiredBooleanObject
     * both throw SaiNotFoundException when no data is found.</i>
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return Literal object value as Boolean
     * @throws SaiException
     * @throws SaiNotFoundException when nothing is found
     */
    public static boolean getBooleanObject(Resource resource, Property property) throws SaiException, SaiNotFoundException {
        RDFNode object = getObject(resource, property);
        if (object == null) { throw new SaiNotFoundException(msgNothingFound(resource, property, XSDboolean)); }
        if (!object.isLiteral()) { throw new SaiException(msgInvalidDataType(resource, property, XSDboolean)); }
        if (!object.asLiteral().getDatatype().equals(XSDboolean)) { throw new SaiException(msgInvalidDataType(resource, property, XSDboolean)); }
        return object.asLiteral().getBoolean();
    }

    /**
     * Returns a single literal value as Boolean from the object of the statement matching
     * the provided <code>property</code> in the provided <code>resource</code>. Throws an
     * exception when no match is found.
     * @param resource Jena resource to navigate
     * @param property Jena property to search for
     * @return Literal object value as Boolean
     * @throws SaiException
     * @throws SaiNotFoundException when nothing is found
     */
    public static Boolean getRequiredBooleanObject(Resource resource, Property property) throws SaiException, SaiNotFoundException {
        return getBooleanObject(resource, property);
    }

    /**
     * Updates the provided Jena Resource <code>resource</code> for the specified
     * <code>property</code> with the RDFNode <code>object</code>. This will remove
     * all existing statements of <code>property</code> in <code>resource</code> first.
     * @param resource Jena Resource to update
     * @param property Jena Property to update
     * @param object RDFNode to update with
     * @return This resource to allow cascading calls
     */
    public static Resource updateObject(Resource resource, Property property, RDFNode object) {
        Objects.requireNonNull(resource, "Cannot update a null resource");
        Objects.requireNonNull(property, "Cannot update a resource by passing a null property");
        Objects.requireNonNull(property, "Cannot update a resource by passing a null object");

        resource.removeAll(property);
        resource.addProperty(property, object);
        return resource;
    }

    /**
     * Updates the provided Jena Resource <code>resource</code> for the specified
     * <code>property</code> with the String literal <code>string</code>. This will remove
     * all existing statements of <code>property</code> in <code>resource</code> first.
     * @param resource Jena Resource to update
     * @param property Jena Property to update
     * @param string String literal to update with
     * @return This resource to allow cascading calls
     */
    public static Resource updateObject(Resource resource, Property property, String string) {
        Objects.requireNonNull(string, "Cannot update a resource by passing a null string");
        Node node = NodeFactory.createLiteral(string);
        updateObject(resource, property, resource.getModel().asRDFNode(node));
        return resource;
    }

    /**
     * Updates the provided Jena Resource <code>resource</code> for the specified
     * <code>property</code> with the URL <code>url</code>. This will remove
     * all existing statements of <code>property</code> in <code>resource</code> first.
     * @param resource Jena Resource to update
     * @param property Jena Property to update
     * @param url URL to update with
     * @return This resource to allow cascading calls
     */
    public static Resource updateObject(Resource resource, Property property, URL url) {
        Objects.requireNonNull(url, "Cannot update a resource by passing a null url");
        Node node = NodeFactory.createURI(url.toString());
        updateObject(resource, property, resource.getModel().asRDFNode(node));
        return resource;
    }

    /**
     * Updates the provided Jena Resource <code>resource</code> for the specified
     * <code>property</code> with the xsd:dateTime provided via <code>dateTime</code>. This will remove
     * all existing statements of <code>property</code> in <code>resource</code> first.
     * @param resource Jena Resource to update
     * @param property Jena Property to update
     * @param dateTime String literal to update with
     * @return This resource to allow cascading calls
     */
    public static Resource updateObject(Resource resource, Property property, OffsetDateTime dateTime) {
        Objects.requireNonNull(dateTime, "Cannot update a resource by passing a null date time value");
        Node node = NodeFactory.createLiteralByValue(dateTime, XSDdateTime);
        updateObject(resource, property, resource.getModel().asRDFNode(node));
        return resource;
    }

    /**
     * Updates the provided Jena Resource <code>resource</code> for the specified
     * <code>property</code> with the integer provided via <code>integer</code>. This will remove
     * all existing statements of <code>property</code> in <code>resource</code> first.
     * @param resource Jena Resource to update
     * @param property Jena Property to update
     * @param integer integer to update with
     * @return This resource to allow cascading calls
     */
    public static Resource updateObject(Resource resource, Property property, int integer) {
        Node node = NodeFactory.createLiteralByValue(integer, XSDinteger);
        updateObject(resource, property, resource.getModel().asRDFNode(node));
        return resource;
    }

    /**
     * Updates the provided Jena Resource <code>resource</code> for the specified
     * <code>property</code> with the boolean provided via <code>bool</code>. This will remove
     * all existing statements of <code>property</code> in <code>resource</code> first.
     * @param resource Jena Resource to update
     * @param property Jena Property to update
     * @param bool boolean literal to update with
     * @return This resource to allow cascading calls
     */
    public static Resource updateObject(Resource resource, Property property, boolean bool) {
        Node node = NodeFactory.createLiteralByValue(bool, XSDboolean);
        updateObject(resource, property, resource.getModel().asRDFNode(node));
        return resource;
    }

    /**
     * Updates the provided Jena Resource <code>resource</code> for the specified
     * <code>property</code> with the list of URLs provided via <code>bool</code>. This will remove
     * all existing statements of <code>property</code> in <code>resource</code> first.
     * @param resource Jena Resource to update
     * @param property Jena Property to update
     * @param urls List of URLs to update with
     * @return This resource to allow cascading calls
     */
    public static Resource updateUrlObjects(Resource resource, Property property, List<URL> urls) {
        Objects.requireNonNull(resource, "Cannot update a null resource");
        Objects.requireNonNull(property, "Cannot update a resource by passing a null property");
        Objects.requireNonNull(urls, "Cannot update a resource by passing a null list");
        resource.removeAll(property);
        for (URL url : urls) {
            Node node = NodeFactory.createURI(url.toString());
            resource.addProperty(property, resource.getModel().asRDFNode(node));
        }
        return resource;
    }

    /**
     * Updates the provided Jena Resource <code>resource</code> for the specified
     * <code>property</code> with the list of Strings provided via <code>bool</code>. This will remove
     * all existing statements of <code>property</code> in <code>resource</code> first.
     * @param resource Jena Resource to update
     * @param property Jena Property to update
     * @param strings List of Strings to update with
     * @return This resource to allow cascading calls
     */
    public static Resource updateStringObjects(Resource resource, Property property, List<String> strings) {
        Objects.requireNonNull(resource, "Cannot update a null resource");
        Objects.requireNonNull(property, "Cannot update a resource by passing a null property");
        Objects.requireNonNull(strings, "Cannot update a resource by passing a null list");
        resource.removeAll(property);
        for (String string : strings) {
            Node node = NodeFactory.createLiteral(string);
            resource.addProperty(property, resource.getModel().asRDFNode(node));
        }
        return resource;
    }

    /**
     * Convert an RDFNode value to URL
     * @param node RDFNode to convert
     * @return Converted URL
     * @throws SaiException
     */
    public static URL nodeToUrl(RDFNode node) throws SaiException {
        Objects.requireNonNull(node, "Cannot convert a null node to URL");
        if (!node.isResource()) { throw new SaiException("Cannot convert literal node to URL"); }
        try {
            return new URL(node.asResource().getURI());
        } catch (MalformedURLException ex) {
            throw new SaiException("Failed to convert node to URL - " + node.asResource().getURI() + ": " + ex.getMessage());
        }
    }

    /**
     * Determine the Jena language (graph serialization type) based on a content type string
     * @param contentType Content type string
     * @return Serialization language
     */
    public static Lang getLangForContentType(ContentType contentType) {
        if (contentType == null) {
            return Lang.TURTLE;
        }
        switch (contentType) {
            case LD_JSON:
                return Lang.JSONLD11;
            case RDF_XML:
                return Lang.RDFXML;
            case N_TRIPLES:
                return Lang.NTRIPLES;
            default:
                return Lang.TURTLE;
        }
    }

    /**
     * Convenience function for common condition when the expected data type isn't found
     */
    private static String msgInvalidDataType(Resource resource, Property property, RDFDatatype type) {
        return "Excepted literal value of type " + type.toString() + "for " + resource.getURI() + " -- " + property.getURI();
    }

    /**
     * Convenience function for common condition when the expected data type isn't found
     */
    private static String msgNothingFound(Resource resource, Property property, RDFDatatype type) {
        return "Nothing found for " + resource.getURI() + " -- " + property.getURI() + " of type " + type.toString();
    }

    /**
     * Convenience function for common condition when the expected data type isn't found
     */
    private static String msgNothingFound(Resource resource, Property property) {
        return "Nothing found for " + resource.getURI() + " -- " + property.getURI();
    }

    /**
     * Convenience function for common condition when an object type isn't a URL resource
     */
    private static String msgNotUrlResource(Resource resource, Property property, RDFNode object) {
        return "Expected non-literal value for object at " + resource.getURI() + " -- " + property.getURI() + " -- " + object;
    }

}

package com.janeirodigital.sai.core.tests.vocabularies;

import com.janeirodigital.sai.core.vocabularies.InteropVocabulary;
import com.janeirodigital.sai.core.vocabularies.LdpVocabulary;
import com.janeirodigital.sai.core.vocabularies.RdfVocabulary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class VocabularyTests {

    @Test
    @DisplayName("Evaluate vocabulary for Interop")
    void evaluateInteropVocab() {

        final String ns = "http://www.w3.org/ns/solid/interop#";
        assertEquals(ns, InteropVocabulary.NAMESPACE.getURI());
        assertEquals(ns + "applicationName", InteropVocabulary.APPLICATION_NAME.getURI());
        assertEquals(ns + "applicationDescription", InteropVocabulary.APPLICATION_DESCRIPTION.getURI());
        assertEquals(ns + "applicationAuthor", InteropVocabulary.APPLICATION_AUTHOR.getURI());
        assertEquals(ns + "applicationThumbnail", InteropVocabulary.APPLICATION_THUMBNAIL.getURI());
        assertEquals(ns + "hasAccessNeedGroup", InteropVocabulary.HAS_ACCESS_NEED_GROUP.getURI());

    }

    @Test
    @DisplayName("Evaluate vocabulary for RDF")
    void evaluateRDFVocab() {

        final String ns = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
        assertEquals(ns, RdfVocabulary.NAMESPACE.getURI());
        assertEquals(ns + "type", RdfVocabulary.RDF_TYPE.getURI());

    }

    @Test
    @DisplayName("Evaluate vocabulary for LDP")
    void evaluateLDPVocab() {

        final String ns = "http://www.w3.org/ns/ldp#";
        assertEquals(ns, LdpVocabulary.NAMESPACE.getURI());
        assertEquals(ns + "Container", LdpVocabulary.CONTAINER.getURI());
        assertEquals(ns + "BasicContainer", LdpVocabulary.BASIC_CONTAINER.getURI());
        assertEquals(ns + "contains", LdpVocabulary.CONTAINS.getURI());

    }

}

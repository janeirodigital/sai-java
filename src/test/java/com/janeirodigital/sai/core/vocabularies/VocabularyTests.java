package com.janeirodigital.sai.core.vocabularies;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.janeirodigital.sai.core.vocabularies.SkosVocabulary.SKOS_DEFINITION;
import static com.janeirodigital.sai.core.vocabularies.SkosVocabulary.SKOS_PREF_LABEL;
import static com.janeirodigital.sai.core.vocabularies.SolidOidcVocabulary.SOLID_OIDC_REDIRECT_URIS;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VocabularyTests {

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
        assertEquals(ns + "Container", LdpVocabulary.LDP_CONTAINER.getURI());
        assertEquals(ns + "BasicContainer", LdpVocabulary.LDP_BASIC_CONTAINER.getURI());
        assertEquals(ns + "contains", LdpVocabulary.LDP_CONTAINS.getURI());

    }

    @Test
    @DisplayName("Evaluate vocabulary for Solid-OIDC")
    void evaluateSolidOidcVocab() {
        final String ns = "http://www.w3.org/ns/solid/oidc#";
        assertEquals(ns, SolidOidcVocabulary.NAMESPACE.getURI());
        assertEquals(ns + "redirect_uris", SOLID_OIDC_REDIRECT_URIS.getURI());
    }

    @Test
    @DisplayName("Evaluate vocabulary for SKOS")
    void evaluateSkosVocab() {
        final String ns = "http://www.w3.org/2004/02/skos/core#";
        assertEquals(ns, SkosVocabulary.NAMESPACE.getURI());
        assertEquals(ns + "prefLabel", SKOS_PREF_LABEL.getURI());
        assertEquals(ns + "definition", SKOS_DEFINITION.getURI());
    }

}

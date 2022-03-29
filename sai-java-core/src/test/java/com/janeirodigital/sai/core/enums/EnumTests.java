package com.janeirodigital.sai.core.enums;

import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.HttpHeader;
import com.janeirodigital.sai.httputils.HttpMethod;
import com.janeirodigital.sai.httputils.LinkRelation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

class EnumTests {

    @Test
    @DisplayName("Evaluate enums for HTTP Headers")
    void evaluateHttpHeaders() {
        assertEquals("Content-Type", HttpHeader.CONTENT_TYPE.getValue());
    }

    @Test
    @DisplayName("Evaluate enums for Link Relations")
    void evaluateLinkRelations() {
        assertEquals("type", LinkRelation.TYPE.getValue());
    }

    @Test
    @DisplayName("Evaluate enums for HTTP Content Types")
    void evaluateContentTypes() {
        assertEquals("text/turtle", ContentType.TEXT_TURTLE.getValue());
    }

    @Test
    @DisplayName("Evaluate enums for HTTP Methods")
    void evaluateHttpMethods() {
        assertEquals("GET", HttpMethod.GET.getValue());
    }

}

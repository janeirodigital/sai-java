package com.janeirodigital.sai.core.tests.enums;

import com.janeirodigital.sai.core.enums.HttpHeaders;
import com.janeirodigital.sai.core.enums.LinkRelations;
import com.janeirodigital.sai.core.exceptions.SaiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

class EnumTests {

    @Test
    @DisplayName("Evaluate enums for HTTP Headers")
    void evaluateHttpHeaders() throws SaiException {

        assertEquals("Accept", HttpHeaders.ACCEPT.getValue());
        assertEquals("Authorization", HttpHeaders.AUTHORIZATION.getValue());
        assertEquals("Content-Type", HttpHeaders.CONTENT_TYPE.getValue());
        assertEquals("Link", HttpHeaders.LINK.getValue());
        assertEquals("Location", HttpHeaders.LOCATION.getValue());
        assertEquals("Slug", HttpHeaders.SLUG.getValue());

    }

    @Test
    @DisplayName("Evaluate enums for Link Relations")
    void evaluateLinkRelations() throws SaiException {

        assertEquals("describedby", LinkRelations.DESCRIBED_BY.getValue());
        assertEquals("http://www.w3.org/ns/shapetrees#FocusNode", LinkRelations.FOCUS_NODE.getValue());
        assertEquals("http://www.w3.org/ns/shapetrees#managedBy", LinkRelations.MANAGED_BY.getValue());
        assertEquals("http://www.w3.org/ns/shapetrees#manages", LinkRelations.MANAGES.getValue());
        assertEquals("http://www.w3.org/ns/shapetrees#TargetShapeTree", LinkRelations.TARGET_SHAPETREE.getValue());
        assertEquals("type", LinkRelations.TYPE.getValue());
        assertEquals("acl", LinkRelations.ACL.getValue());


    }

}

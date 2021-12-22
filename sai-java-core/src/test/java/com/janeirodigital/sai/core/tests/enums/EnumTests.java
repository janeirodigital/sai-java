package com.janeirodigital.sai.core.tests.enums;

import com.janeirodigital.sai.core.enums.HttpHeader;
import com.janeirodigital.sai.core.enums.LinkRelation;
import com.janeirodigital.sai.core.exceptions.SaiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

class EnumTests {

    @Test
    @DisplayName("Evaluate enums for HTTP Headers")
    void evaluateHttpHeaders() throws SaiException {

        assertEquals("Accept", HttpHeader.ACCEPT.getValue());
        assertEquals("Authorization", HttpHeader.AUTHORIZATION.getValue());
        assertEquals("Content-Type", HttpHeader.CONTENT_TYPE.getValue());
        assertEquals("Link", HttpHeader.LINK.getValue());
        assertEquals("Location", HttpHeader.LOCATION.getValue());
        assertEquals("Slug", HttpHeader.SLUG.getValue());

    }

    @Test
    @DisplayName("Evaluate enums for Link Relations")
    void evaluateLinkRelations() throws SaiException {

        assertEquals("describedby", LinkRelation.DESCRIBED_BY.getValue());
        assertEquals("http://www.w3.org/ns/shapetrees#FocusNode", LinkRelation.FOCUS_NODE.getValue());
        assertEquals("http://www.w3.org/ns/shapetrees#managedBy", LinkRelation.MANAGED_BY.getValue());
        assertEquals("http://www.w3.org/ns/shapetrees#manages", LinkRelation.MANAGES.getValue());
        assertEquals("http://www.w3.org/ns/shapetrees#TargetShapeTree", LinkRelation.TARGET_SHAPETREE.getValue());
        assertEquals("type", LinkRelation.TYPE.getValue());
        assertEquals("acl", LinkRelation.ACL.getValue());


    }

}

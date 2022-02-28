package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Readable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-consent">Data Grant</a>
 * with a scope of <a href="https://solid.github.io/data-interoperability-panel/specification/#scope-fromregistry">AllFromRegistry</a>
 */
@Getter
public abstract class InheritableDataGrant extends ReadableDataGrant {

    private final List<InheritedDataGrant> inheritingGrants;

    protected InheritableDataGrant(URL url, SaiSession saiSession, Model dataset, Resource resource, ContentType contentType, URL dataOwner,
                                   URL grantee, URL registeredShapeTree, List<RDFNode> accessModes, List<RDFNode> creatorAccessModes,
                                   RDFNode scopeOfGrant, URL dataRegistration, URL accessNeed, URL delegationOf) throws SaiException {
        super(url, saiSession, dataset, resource, contentType, dataOwner, grantee, registeredShapeTree, accessModes, creatorAccessModes,
              scopeOfGrant, dataRegistration, accessNeed, delegationOf);
        this.inheritingGrants = new ArrayList<>();
    }

}
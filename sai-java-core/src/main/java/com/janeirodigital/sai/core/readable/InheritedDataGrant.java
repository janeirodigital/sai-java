package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.SCOPE_INHERITED;

/**
 * Readable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-consent">Data Grant</a>
 * with a scope of <a href="https://solid.github.io/data-interoperability-panel/specification/#scope-inherited">Inherited</a>
 */
@Getter
public class InheritedDataGrant extends ReadableDataGrant {
    private final URL inheritsFrom;  // TODO - Should this be a URL or a DataGrant?
    protected InheritedDataGrant(URL url, SaiSession saiSession, Model dataset, Resource resource, ContentType contentType, URL dataOwner,
                                 URL grantee, URL registeredShapeTree, List<RDFNode> accessModes, List<RDFNode> creatorAccessModes,
                                 URL dataRegistration, URL accessNeed, URL inheritsFrom, URL delegationOf) throws SaiException {
        super(url, saiSession, dataset, resource, contentType, dataOwner, grantee, registeredShapeTree, accessModes, creatorAccessModes,
                SCOPE_INHERITED, dataRegistration, accessNeed, delegationOf);
        this.inheritsFrom = inheritsFrom;
    }

    @Override
    public DataInstanceList getDataInstances() throws SaiException {
        try {
            ReadableDataGrant parentGrant = ReadableDataGrant.get(this.inheritsFrom, this.saiSession);
            List<URL> childInstanceUrls = new ArrayList<>();
            for (DataInstance parentInstance : parentGrant.getDataInstances()) {
                childInstanceUrls.addAll(parentInstance.getChildReferences(this.getRegisteredShapeTree()));
            }
            return new DataInstanceList(this.saiSession, this, childInstanceUrls);
        } catch (SaiNotFoundException ex) {
            throw new SaiException("Failed to load data instances from " + this.getDataRegistration());
        }
    }

    @Override
    public DataInstance newDataInstance(DataInstance parent) throws SaiException {
        return ReadableDataGrant.newDataInstance(this, parent);
    }

}

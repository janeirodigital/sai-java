package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.crud.DataRegistration;
import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.List;

import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.SCOPE_ALL_FROM_REGISTRY;

/**
 * Readable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-consent">Data Grant</a>
 * with a scope of <a href="https://solid.github.io/data-interoperability-panel/specification/#scope-fromregistry">AllFromRegistry</a>
 */
@Getter
public class AllFromRegistryDataGrant extends InheritableDataGrant {

    protected AllFromRegistryDataGrant(URL url, SaiSession saiSession, Model dataset, Resource resource, ContentType contentType, URL dataOwner,
                                       URL grantee, URL registeredShapeTree, List<RDFNode> accessModes, List<RDFNode> creatorAccessModes,
                                       URL dataRegistration, URL accessNeed, URL delegationOf) throws SaiException {
        super(url, saiSession, dataset, resource, contentType, dataOwner, grantee, registeredShapeTree, accessModes, creatorAccessModes,
              SCOPE_ALL_FROM_REGISTRY, dataRegistration, accessNeed, delegationOf);
    }

    @Override
    public DataInstanceList getDataInstances() throws SaiException {
        // Get the data registration
        try {
            DataRegistration registration = DataRegistration.get(this.getDataRegistration(), this.saiSession);
            return new DataInstanceList(saiSession, this, registration.getDataInstances());
        } catch (SaiNotFoundException ex) {
            throw new SaiException("Failed to load data instances from " + this.getDataRegistration());
        }
    }

    @Override
    public DataInstance newDataInstance(DataInstance instance) throws SaiException {
        return ReadableDataGrant.newDataInstance(this, null);
    }

}
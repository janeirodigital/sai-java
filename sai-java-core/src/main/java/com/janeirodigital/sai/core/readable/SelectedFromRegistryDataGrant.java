package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.List;

import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.SCOPE_SELECTED_FROM_REGISTRY;

/**
 * Readable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-consent">Data Grant</a>
 * with a scope of <a href="https://solid.github.io/data-interoperability-panel/specification/#scope-selected">SelectedFromRegistry</a>
 */
@Getter
public class SelectedFromRegistryDataGrant extends InheritableDataGrant {

    List<URL> dataInstances;
    protected SelectedFromRegistryDataGrant(URL url, SaiSession saiSession, Model dataset, Resource resource, ContentType contentType, URL dataOwner,
                                            URL grantee, URL registeredShapeTree, List<RDFNode> accessModes, List<RDFNode> creatorAccessModes,
                                            URL dataRegistration, List<URL> dataInstances, URL accessNeed, URL delegationOf) throws SaiException {
        super(url, saiSession, dataset, resource, contentType, dataOwner, grantee, registeredShapeTree, accessModes, creatorAccessModes,
              SCOPE_SELECTED_FROM_REGISTRY, dataRegistration, accessNeed, delegationOf);
        this.dataInstances = dataInstances;
    }

    @Override
    public DataInstanceList getDataInstances() {
        return new DataInstanceList(saiSession, this, this.dataInstances);
    }

    @Override
    public DataInstance newDataInstance(DataInstance instance) throws SaiException {
        throw new SaiException("Cannot create new data instances for a SelectedFromRegistry grant scope");
    }

}
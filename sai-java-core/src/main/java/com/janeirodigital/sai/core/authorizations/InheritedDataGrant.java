package com.janeirodigital.sai.core.authorizations;

import com.janeirodigital.sai.core.data.DataInstance;
import com.janeirodigital.sai.core.data.DataInstanceList;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import lombok.Getter;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Readable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-grant">Data Grant</a>
 * with a scope of <a href="https://solid.github.io/data-interoperability-panel/specification/#scope-inherited">Inherited</a>
 */
@Getter
public class InheritedDataGrant extends ReadableDataGrant {

    private final URI inheritsFrom;

    /**
     * Construct an {@link InheritedDataGrant} from the provided {@link ReadableDataGrant.Builder}.
     * @param builder {@link ReadableDataGrant.Builder} to construct with
     * @throws SaiException
     */
    protected InheritedDataGrant(ReadableDataGrant.Builder builder) throws SaiException {
        super(builder);
        this.inheritsFrom = builder.inheritsFrom;
    }

    /**
     * Returns a {@link DataInstanceList} that iterates over the list of {@link DataInstance}s
     * inherited from the {@link DataInstance}s associated with the parent {@link ReadableDataGrant}
     * that this grant inherits from.
     * @return {@link DataInstanceList}
     */
    @Override
    public DataInstanceList getDataInstances() throws SaiException {
        try {
            ReadableDataGrant parentGrant = ReadableDataGrant.get(this.getInheritsFrom(), this.getSaiSession());
            Map<URI, DataInstance> childInstanceUris = new HashMap<>();
            for (DataInstance parentInstance : parentGrant.getDataInstances()) {
                for (URI childReference : parentInstance.getChildReferences(this.getRegisteredShapeTree())) {
                    childInstanceUris.put(childReference, parentInstance);
                }
            }
            return new DataInstanceList(this.getSaiSession(), this, childInstanceUris);
        } catch (SaiHttpNotFoundException ex) {
            throw new SaiException("Failed to load data instances from " + this.getDataRegistration(), ex);
        }
    }

}
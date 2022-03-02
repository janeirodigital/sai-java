package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import lombok.Getter;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Readable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-consent">Data Grant</a>
 * with a scope of <a href="https://solid.github.io/data-interoperability-panel/specification/#scope-inherited">Inherited</a>
 */
@Getter
public class InheritedDataGrant extends ReadableDataGrant {

    private final URL inheritsFrom;  // TODO - Should this be a URL or a DataGrant?

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

    /**
     * Create a new {@link DataInstance} in the {@link com.janeirodigital.sai.core.crud.DataRegistration}
     * specified in this data grant, for the parent instance of this inherited grant.
     * @param parent {@link DataInstance} for a {@link ReadableDataGrant} that this grant inherits from
     * @return New {@link DataInstance}
     * @throws SaiException
     */
    @Override
    public DataInstance newDataInstance(DataInstance parent) throws SaiException {
        return ReadableDataGrant.newDataInstance(this, parent);
    }

}

package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.exceptions.SaiException;
import lombok.Getter;

import java.net.URL;
import java.util.List;

/**
 * Readable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-consent">Data Grant</a>
 * with a scope of <a href="https://solid.github.io/data-interoperability-panel/specification/#scope-selected">SelectedFromRegistry</a>
 */
@Getter
public class SelectedFromRegistryDataGrant extends InheritableDataGrant {

    List<URL> dataInstances;

    /**
     * Construct a {@link SelectedFromRegistryDataGrant} from the provided {@link ReadableDataGrant.Builder}.
     * @param builder {@link ReadableDataGrant.Builder} to construct with
     * @throws SaiException
     */
    protected SelectedFromRegistryDataGrant(ReadableDataGrant.Builder builder) throws SaiException {
        super(builder);
        this.dataInstances = builder.dataInstances;
    }

    /**
     * Returns a {@link DataInstanceList} that iterates over the list of {@link DataInstance}s
     * specifically selected as part of the SelectedFromRegistry data access scope
     * @return {@link DataInstanceList}
     */
    @Override
    public DataInstanceList getDataInstances() {
        return new DataInstanceList(saiSession, this, this.dataInstances);
    }

    /**
     * Unsupported operation since new data instances cannot be created from for a SelectedFromRegistry
     * data access scope
     * @throws SaiException
     */
    @Override
    public DataInstance newDataInstance(DataInstance instance) throws SaiException {
        throw new SaiException("Cannot create new data instances for a SelectedFromRegistry grant scope");
    }

}
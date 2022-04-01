package com.janeirodigital.sai.core.authorizations;

import com.janeirodigital.sai.core.data.DataInstance;
import com.janeirodigital.sai.core.data.DataInstanceList;
import com.janeirodigital.sai.core.exceptions.SaiException;
import lombok.Getter;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Readable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-grant">Data Grant</a>
 * with a scope of <a href="https://solid.github.io/data-interoperability-panel/specification/#scope-selected">SelectedFromRegistry</a>
 */
@Getter
public class SelectedFromRegistryDataGrant extends InheritableDataGrant {

    List<URI> dataInstances;

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
        Map<URI, DataInstance> dataInstanceUris = new HashMap<>();
        for (URI dataInstanceUri : this.dataInstances) { dataInstanceUris.put(dataInstanceUri, null); }
        return new DataInstanceList(saiSession, this, dataInstanceUris);
    }

}
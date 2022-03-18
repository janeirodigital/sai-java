package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.crud.DataRegistration;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import lombok.Getter;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Readable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-grant">Data Grant</a>
 * with a scope of <a href="https://solid.github.io/data-interoperability-panel/specification/#scope-fromregistry">AllFromRegistry</a>
 */
@Getter
public class AllFromRegistryDataGrant extends InheritableDataGrant {

    /**
     * Construct an {@link AllFromRegistryDataGrant} from the provided {@link ReadableDataGrant.Builder}.
     * @param builder {@link ReadableDataGrant.Builder} to construct with
     * @throws SaiException
     */
    protected AllFromRegistryDataGrant(ReadableDataGrant.Builder builder) throws SaiException {
        super(builder);
    }

    /**
     * Returns a {@link DataInstanceList} that iterates over all of the {@link DataInstance}s
     * for a given shape tree type in the {@link DataRegistration}
     * specified in this data grant
     * @return {@link DataInstanceList}
     */
    @Override
    public DataInstanceList getDataInstances() throws SaiException {
        try {
            DataRegistration registration = DataRegistration.get(this.getDataRegistration(), this.saiSession);
            Map<URL, DataInstance> dataInstanceUrls = new HashMap<>();
            for (URL dataInstanceUrl : registration.getDataInstances()) { dataInstanceUrls.put(dataInstanceUrl, null); }
            return new DataInstanceList(saiSession, this, dataInstanceUrls);
        } catch (SaiNotFoundException ex) {
            throw new SaiException("Failed to load data instances from " + this.getDataRegistration());
        }
    }

}
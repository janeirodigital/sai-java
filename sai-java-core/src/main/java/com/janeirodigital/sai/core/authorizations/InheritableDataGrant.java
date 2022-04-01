package com.janeirodigital.sai.core.authorizations;

import com.janeirodigital.sai.core.exceptions.SaiException;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for data grants with scopes that allow inherited children,
 * specifically {@link AllFromRegistryDataGrant} and {@link SelectedFromRegistryDataGrant}
 */
@Getter
public abstract class InheritableDataGrant extends ReadableDataGrant {

    private final List<InheritedDataGrant> inheritingGrants;

    /**
     * Construct an {@link InheritableDataGrant} from the provided {@link ReadableDataGrant.Builder}.
     * @param builder {@link ReadableDataGrant.Builder} to construct with
     * @throws SaiException
     */
    protected InheritableDataGrant(ReadableDataGrant.Builder builder) throws SaiException {
        super(builder);
        this.inheritingGrants = new ArrayList<>();
    }

}
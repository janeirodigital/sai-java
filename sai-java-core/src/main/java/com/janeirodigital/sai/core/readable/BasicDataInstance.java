package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import okhttp3.Response;

import java.net.URL;
import java.util.Objects;

import static com.janeirodigital.sai.core.helpers.HttpHelper.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.core.helpers.HttpHelper.getRdfModelFromResponse;
import static com.janeirodigital.sai.core.helpers.RdfUtils.getNewResource;

/**
 * Basic instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-instance">Data Instance</a>,
 * which extends the abstract {@link DataInstance}, and can be used when an application only wants to deal
 * with the basics of the data instance, and not extend it for domain specific purposes.
 */
@Getter
public class BasicDataInstance extends DataInstance {
    
    protected BasicDataInstance(Builder builder) throws SaiException {
        super(builder);
    }

    /**
     * Get a {@link BasicDataInstance} at the provided <code>url</code>
     * @param url URL of the {@link BasicDataInstance} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use for retrieval
     * @param parent Optional parent {@link DataInstance} to provide if known
     * @return Retrieved {@link BasicDataInstance}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static BasicDataInstance get(URL url, SaiSession saiSession, ContentType contentType, ReadableDataGrant dataGrant, DataInstance parent) throws SaiException, SaiNotFoundException {
        BasicDataInstance.Builder builder = new BasicDataInstance.Builder(url, saiSession);
        builder.setDataGrant(dataGrant);
        if (parent != null) builder.setParent(parent);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).setContentType(contentType).build();
        }
    }

    public static BasicDataInstance get(URL url, SaiSession saiSession, ReadableDataGrant dataGrant, DataInstance parent) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE, dataGrant, parent);
    }
    
    protected static class Builder extends DataInstance.Builder<Builder> {

        public Builder(URL url, SaiSession saiSession) { super(url, saiSession); }

        @Override
        public Builder getThis() { return this; }

        public BasicDataInstance build() throws SaiException {
            Objects.requireNonNull(this.dataGrant, "Must provide a data grant for the data instance builder");
            if (dataGrant instanceof InheritedDataGrant && this.parent == null) {
                // Inherited data instances need a parent to be set
                throw new SaiException("Must provide a parent for the inherited data instance");
            }
            if (this.dataset == null) {
                // Data instance is being created, initialize an empty graph resource the application code can populate.
                this.resource = getNewResource(this.url);
                this.dataset = this.resource.getModel();
            } else {
                this.exists = true;
                this.setDraft(false);
            }
            return new BasicDataInstance(this);
        }

    }

}

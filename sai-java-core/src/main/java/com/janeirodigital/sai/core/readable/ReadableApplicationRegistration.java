package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URL;

import static com.janeirodigital.sai.core.utils.HttpUtils.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.core.utils.HttpUtils.getRdfModelFromResponse;

/**
 * Readable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#application-registration">Application Registration</a>.
 */
@Getter
public class ReadableApplicationRegistration extends ReadableAgentRegistration {

    /**
     * Construct a {@link ReadableApplicationRegistration} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private ReadableApplicationRegistration(Builder builder) throws SaiException {
        super(builder);
    }

    /**
     * Get a {@link ReadableApplicationRegistration} from the provided <code>url</code>.
     * @param url URL to generate the {@link ReadableApplicationRegistration} from
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use for retrieval
     * @return {@link ReadableApplicationRegistration}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ReadableApplicationRegistration get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        ReadableApplicationRegistration.Builder builder = new ReadableApplicationRegistration.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link ReadableApplicationRegistration} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link ReadableApplicationRegistration}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ReadableApplicationRegistration get(URL url, SaiSession saiSession) throws SaiException, SaiNotFoundException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link ReadableApplicationRegistration} using the attributes of the current instance
     * @return Reloaded {@link ReadableApplicationRegistration}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public ReadableApplicationRegistration reload() throws SaiNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    /**
     * Builder for {@link ReadableApplicationRegistration} instances.
     */
    private static class Builder extends ReadableAgentRegistration.Builder<Builder> {

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link ReadableApplicationRegistration} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URL url, SaiSession saiSession) { super(url, saiSession); }

        /**
         * Ensures that we don't get an unchecked cast warning when returning from setters
         * @return {@link Builder}
         */
        @Override
        public Builder getThis() { return this; }

        /**
         * Set the Jena model and use it to populate attributes of the {@link Builder}. Assumption
         * is made that the corresponding resource exists.
         * @param dataset Jena model to populate the Builder attributes with
         * @return {@link Builder}
         * @throws SaiException
         */
        @Override
        public Builder setDataset(Model dataset) throws SaiException {
            super.setDataset(dataset);
            populateFromDataset();
            this.exists = true;
            return this;
        }

        /**
         * Build the {@link ReadableApplicationRegistration} using attributes from the Builder.
         * @return {@link ReadableApplicationRegistration}
         * @throws SaiException
         */
        public ReadableApplicationRegistration build() throws SaiException {
            return new ReadableApplicationRegistration(this);
        }
    }
}

package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.SaiHttpException;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URL;

import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.RECIPROCAL_REGISTRATION;
import static com.janeirodigital.sai.httputils.HttpUtils.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.httputils.HttpUtils.getRdfModelFromResponse;
import static com.janeirodigital.sai.rdfutils.RdfUtils.getUrlObject;

/**
 * Readable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agent-registration">Social Agent Registration</a>.
 */
@Getter
public class ReadableSocialAgentRegistration extends ReadableAgentRegistration {

    private final URL reciprocalRegistration;

    /**
     * Construct a {@link ReadableSocialAgentRegistration} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private ReadableSocialAgentRegistration(Builder builder) throws SaiException {
        super(builder);
        this.reciprocalRegistration = builder.reciprocalRegistration;
    }

    /**
     * Get a {@link ReadableSocialAgentRegistration} from the provided <code>url</code>.
     * @param url URL to generate the {@link ReadableSocialAgentRegistration} from
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use for retrieval
     * @return {@link ReadableSocialAgentRegistration}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static ReadableSocialAgentRegistration get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiHttpNotFoundException {
        ReadableSocialAgentRegistration.Builder builder = new ReadableSocialAgentRegistration.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).setContentType(contentType).build();
        } catch (SaiRdfException | SaiHttpException ex) {
            throw new SaiException("Unable to read readable social agent registration " + url, ex);
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link ReadableSocialAgentRegistration} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link ReadableSocialAgentRegistration}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static ReadableSocialAgentRegistration get(URL url, SaiSession saiSession) throws SaiException, SaiHttpNotFoundException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link ReadableSocialAgentRegistration} using the attributes of the
     * current instance
     * @return Reloaded {@link ReadableSocialAgentRegistration}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public ReadableSocialAgentRegistration reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    /**
     * Builder for {@link ReadableSocialAgentRegistration} instances.
     */
    private static class Builder extends ReadableAgentRegistration.Builder<Builder> {

        private URL reciprocalRegistration;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link ReadableSocialAgentRegistration} to build
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
         * Populates the fields of the {@link ReadableSocialAgentRegistration} based on the associated Jena resource.
         * @throws SaiException
         */
        @Override
        protected void populateFromDataset() throws SaiException {
            super.populateFromDataset();
            try {
                this.reciprocalRegistration = getUrlObject(this.resource, RECIPROCAL_REGISTRATION);
            } catch (SaiRdfException ex) {
                throw new SaiException("Failed to load readable social agent registration " + this.url, ex);
            }
        }

        /**
         * Build the {@link ReadableSocialAgentRegistration} using attributes from the Builder.
         * @return {@link ReadableSocialAgentRegistration}
         * @throws SaiException
         */
        public ReadableSocialAgentRegistration build() throws SaiException {
            return new ReadableSocialAgentRegistration(this);
        }
    }
}

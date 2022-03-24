package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.immutable.DataGrant;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import lombok.Setter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URL;
import java.util.Objects;

import static com.janeirodigital.sai.core.helpers.HttpHelper.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.core.helpers.HttpHelper.getRdfModelFromResponse;
import static com.janeirodigital.sai.core.helpers.RdfUtils.*;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.RECIPROCAL_REGISTRATION;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.SOCIAL_AGENT_REGISTRATION;

/**
 * Modifiable instantiation of a
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agent-registration">Social Agent Registration</a>.
 */
@Getter @Setter
public class SocialAgentRegistration extends AgentRegistration {

    private URL reciprocalRegistration;

    /**
     * Construct a {@link SocialAgentRegistration} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private SocialAgentRegistration(Builder builder) throws SaiException {
        super(builder);
        this.reciprocalRegistration = builder.reciprocalRegistration;
    }

    /**
     * Get a {@link SocialAgentRegistration} from the provided <code>url</code>.
     * @param url URL to generate the {@link SocialAgentRegistration} from
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use for retrieval
     * @return {@link SocialAgentRegistration}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static SocialAgentRegistration get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        SocialAgentRegistration.Builder builder = new SocialAgentRegistration.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link SocialAgentRegistration} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link SocialAgentRegistration}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public static SocialAgentRegistration get(URL url, SaiSession saiSession) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link SocialAgentRegistration} using the attributes of the current instance
     * @return Reloaded {@link SocialAgentRegistration}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public SocialAgentRegistration reload() throws SaiNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    /**
     * Builder for {@link SocialAgentRegistration} instances.
     */
    public static class Builder extends AgentRegistration.Builder<Builder> {

        private URL reciprocalRegistration;

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link SocialAgentRegistration} to build
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
         * Set the <a href="https://solid.github.io/data-interoperability-panel/specification/#social-agent-registration">Reciprocal Registration</a>
         * that <code>registeredAgent</code> maintains for the social agent that owns the agent registry.
         * @see <a href="https://solid.github.io/data-interoperability-panel/specification/#ar-registry">Agent Registry</a>
         * @param registrationUrl URL of the reciprocal social agent registration
         */
        public Builder setReciprocalRegistration(URL registrationUrl) {
            Objects.requireNonNull(registrationUrl, "Must provide a reciprocal registration for the social agent registration");
            this.reciprocalRegistration = registrationUrl;
            return this;
        }

        /**
         * Populates the fields of the {@link Builder} based on the associated Jena resource.
         * @throws SaiException
         */
        protected void populateFromDataset() throws SaiException {
            this.reciprocalRegistration = getUrlObject(this.resource, RECIPROCAL_REGISTRATION);
            super.populateFromDataset();
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        protected void populateDataset() {
            this.resource = getNewResourceForType(this.url, SOCIAL_AGENT_REGISTRATION);
            this.dataset = this.resource.getModel();
            if (this.reciprocalRegistration != null) { updateObject(this.resource, RECIPROCAL_REGISTRATION, this.reciprocalRegistration); }
            super.populateDataset();
        }

        /**
         * Build the {@link SocialAgentRegistration} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}.
         * @return {@link DataGrant}
         * @throws SaiException
         */
        public SocialAgentRegistration build() throws SaiException {
            Objects.requireNonNull(this.registeredBy, "Must provide the social agent who registered the agent registration");
            Objects.requireNonNull(this.registeredWith, "Must provide the application used to register the agent registration");
            Objects.requireNonNull(this.registeredAgent, "Must provide the agent to register");
            if (this.dataset == null) { populateDataset(); }
            return new SocialAgentRegistration(this);
        }

    }

}

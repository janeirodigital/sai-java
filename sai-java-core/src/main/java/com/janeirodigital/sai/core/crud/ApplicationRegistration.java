package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URL;
import java.util.Objects;

import static com.janeirodigital.sai.core.helpers.HttpHelper.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.core.helpers.HttpHelper.getRdfModelFromResponse;
import static com.janeirodigital.sai.core.helpers.RdfHelper.getNewResourceForType;
import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.APPLICATION_REGISTRATION;

/**
 * Modifiable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#application-registration">Application Registration</a>.
 */
@Getter
public class ApplicationRegistration extends AgentRegistration {

    /**
     * Construct an {@link ApplicationRegistration} instance from the provided {@link Builder}.
     * @param builder {@link Builder} to construct with
     * @throws SaiException
     */
    private ApplicationRegistration(Builder builder) throws SaiException {
        super(builder);
    }

    /**
     * Get a {@link ApplicationRegistration} at the provided <code>url</code>
     * @param url URL of the {@link ApplicationRegistration} to get
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use for retrieval
     * @return Retrieved {@link ApplicationRegistration}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static ApplicationRegistration get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        ApplicationRegistration.Builder builder = new ApplicationRegistration.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).setContentType(contentType).build();
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link ApplicationRegistration} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link ApplicationRegistration}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public static ApplicationRegistration get(URL url, SaiSession saiSession) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link ApplicationRegistration} using the attributes of the current instance
     * @return Reloaded {@link ApplicationRegistration}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public ApplicationRegistration reload() throws SaiNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    /**
     * Builder for {@link ApplicationRegistration} instances.
     */
     public static class Builder extends AgentRegistration.Builder<Builder> {

        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link ApplicationRegistration} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URL url, SaiSession saiSession) { super(url, saiSession); }

        /**
         * Ensures that we don't get an unchecked cast warning when returning from setters
         * @return {@link ApplicationRegistration.Builder}
         */
        @Override
        public ApplicationRegistration.Builder getThis() { return this; }

        /**
         * Set the Jena model and use it to populate attributes of the {@link ApplicationRegistration.Builder}. Assumption
         * is made that the corresponding resource exists.
         * @param dataset Jena model to populate the Builder attributes with
         * @return {@link ApplicationRegistration.Builder}
         * @throws SaiException
         */
        @Override
        public ApplicationRegistration.Builder setDataset(Model dataset) throws SaiException {
            super.setDataset(dataset);
            populateFromDataset();
            this.exists = true;
            return this;
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        protected void populateDataset() {
            this.resource = getNewResourceForType(this.url, APPLICATION_REGISTRATION);
            this.dataset = this.resource.getModel();
            super.populateDataset();
        }

        /**
         * Build the {@link ApplicationRegistration} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}.
         * @return {@link ApplicationRegistration}
         * @throws SaiException
         */
        public ApplicationRegistration build() throws SaiException {
            Objects.requireNonNull(this.registeredBy, "Must provide the social agent who registered the agent registration");
            Objects.requireNonNull(this.registeredWith, "Must provide the application used to register the agent registration");
            Objects.requireNonNull(this.registeredAgent, "Must provide the agent to register");
            if (this.dataset == null) { populateDataset(); }
            return new ApplicationRegistration(this);
        }

    }

}
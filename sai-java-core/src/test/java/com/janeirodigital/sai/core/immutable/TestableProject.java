package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.crud.SocialAgentRegistration;
import com.janeirodigital.sai.core.enums.ContentType;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiNotFoundException;
import com.janeirodigital.sai.core.readable.DataInstance;
import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.Getter;
import lombok.Setter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URL;
import java.util.Objects;

import static com.janeirodigital.sai.core.TestableVocabulary.TESTABLE_NAME;
import static com.janeirodigital.sai.core.TestableVocabulary.TESTABLE_PROJECT;
import static com.janeirodigital.sai.core.helpers.HttpHelper.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.core.helpers.HttpHelper.getRdfModelFromResponse;
import static com.janeirodigital.sai.core.helpers.RdfHelper.*;

@Getter @Setter
public class TestableProject extends DataInstance {
    
    private String name;
    private String description;

    public TestableProject(Builder builder) throws SaiException {
        super(builder);
        this.name = builder.name;
        this.description = builder.description;
    }

    /**
     * Get a {@link TestableProject} from the provided <code>url</code>.
     * @param url URL to generate the {@link TestableProject} from
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use for retrieval
     * @return {@link TestableProject}
     * @throws SaiException
     * @throws SaiNotFoundException
     */
    public static TestableProject get(URL url, SaiSession saiSession, ContentType contentType) throws SaiException, SaiNotFoundException {
        TestableProject.Builder builder = new TestableProject.Builder(url, saiSession);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).build();
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType)} without specifying a desired content type for retrieval
     * @param url URL of the {@link TestableProject} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link TestableProject}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public static TestableProject get(URL url, SaiSession saiSession) throws SaiNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE);
    }

    /**
     * Reload a new instance of {@link TestableProject} using the attributes of the current instance
     * @return Reloaded {@link TestableProject}
     * @throws SaiNotFoundException
     * @throws SaiException
     */
    public TestableProject reload() throws SaiNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType);
    }

    public static class Builder extends DataInstance.Builder {

        private String name;
        private String description;
        
        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link TestableProject} to build
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
         * Populates the fields of the {@link SocialAgentRegistration.Builder} based on the associated Jena resource.
         * @throws SaiException
         */
        protected void populateFromDataset() throws SaiException {
            try {
                this.name = getRequiredStringObject(this.resource, TESTABLE_NAME);
                this.description = getRequiredStringObject(this.resource, TESTABLE_NAME);
            } catch (SaiNotFoundException ex) {
                throw new SaiException("Unable to populate testable project: " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        protected void populateDataset() {
            this.resource = getNewResourceForType(this.url, TESTABLE_PROJECT);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, TESTABLE_NAME, this.name);
            updateObject(this.resource, TESTABLE_NAME, this.description);
        }

        /**
         * Build the {@link TestableProject} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}.
         * @return {@link DataGrant}
         * @throws SaiException
         */
        public TestableProject build() throws SaiException {
            Objects.requireNonNull(this.name, "Must provide the name of the project");
            Objects.requireNonNull(this.description, "Must provide a description of the project");
            if (this.dataset == null) { populateDataset(); }
            return new TestableProject(this);
        }
        
    }
    
}

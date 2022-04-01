package com.janeirodigital.sai.core.data;

import com.janeirodigital.sai.core.agents.SocialAgentRegistration;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.authorizations.DataGrant;
import com.janeirodigital.sai.core.authorizations.ReadableDataGrant;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.sai.rdfutils.SaiRdfNotFoundException;
import lombok.Getter;
import lombok.Setter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.vocabularies.TestableVocabulary.*;
import static com.janeirodigital.sai.httputils.HttpUtils.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.rdfutils.RdfUtils.*;

@Getter @Setter
public class TestableTask extends DataInstance {

    private String name;
    private String description;

    public TestableTask(Builder builder) throws SaiException {
        super(builder);
        this.name = builder.name;
        this.description = builder.description;
    }

    /**
     * Get a {@link TestableTask} from the provided <code>uri</code>.
     * @param uri URI to generate the {@link TestableTask} from
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use for retrieval
     * @return {@link TestableTask}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static TestableTask get(URI uri, SaiSession saiSession, ContentType contentType, ReadableDataGrant dataGrant, DataInstance parent) throws SaiException, SaiHttpNotFoundException {
        Objects.requireNonNull(dataGrant, "Must provide a readable data grant permitting the data instance to get");
        TestableTask.Builder builder = new TestableTask.Builder(uri, saiSession);
        if (parent != null) builder.setParent(parent);
        builder.setDataGrant(dataGrant).setDraft(false);
        try (Response response = read(uri, saiSession, contentType, false)) {
            return builder.setDataset(response).build();
        }
    }

    /**
     * Call {@link #get(URI, SaiSession, ContentType, ReadableDataGrant, DataInstance)} without specifying a desired content type for retrieval
     * @param uri URI of the {@link TestableTask} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link TestableTask}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public static TestableTask get(URI uri, SaiSession saiSession, ReadableDataGrant dataGrant, DataInstance parent) throws SaiHttpNotFoundException, SaiException {
        return get(uri, saiSession, DEFAULT_RDF_CONTENT_TYPE, dataGrant, parent);
    }

    public static List<TestableTask> getAccessible(ReadableDataGrant dataGrant, SaiSession saiSession) throws SaiHttpNotFoundException, SaiException {
        Objects.requireNonNull(dataGrant, "Must provide a data grant to get accessible data instances");
        Objects.requireNonNull(saiSession, "Must provide a sai session to get accessible data instances");
        List<TestableTask> testableTasks = new ArrayList<>();
        for (DataInstance dataInstance : dataGrant.getDataInstances()) { testableTasks.add(new TestableTask.Builder(dataInstance).build()); }
        return testableTasks;
    }

    /**
     * Reload a new instance of {@link TestableTask} using the attributes of the current instance
     * @return Reloaded {@link TestableTask}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public TestableTask reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.uri, this.saiSession, this.contentType, this.getDataGrant(), this.getParent());
    }

    public static class Builder extends DataInstance.Builder<Builder> {

        private String name;
        private String description;
        
        /**
         * Initialize builder with <code>uri</code> and <code>saiSession</code>
         * @param uri URI of the {@link TestableTask} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URI uri, SaiSession saiSession) { super(uri, saiSession); }

        public Builder(DataInstance dataInstance) throws SaiException { super(dataInstance); }

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

        public Builder setName(String name) {
            Objects.requireNonNull(name, "Must provide a name");
            this.name = name;
            return this;
        }

        public Builder setDescription(String description) {
            Objects.requireNonNull(name, "Must provide a description");
            this.description = description;
            return this;
        }

        /**
         * Populates the fields of the {@link SocialAgentRegistration.Builder} based on the associated Jena resource.
         * @throws SaiException
         */
        protected void populateFromDataset() throws SaiException {
            try {
                this.name = getRequiredStringObject(this.resource, TESTABLE_NAME);
                this.description = getRequiredStringObject(this.resource, TESTABLE_DESCRIPTION);
            } catch (SaiRdfException | SaiRdfNotFoundException ex) {
                throw new SaiException("Unable to populate testable task", ex);
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        protected void populateDataset() {
            this.resource = getNewResourceForType(this.uri, TESTABLE_TASK);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, TESTABLE_NAME, this.name);
            updateObject(this.resource, TESTABLE_DESCRIPTION, this.description);
        }

        /**
         * Build the {@link TestableTask} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}.
         * @return {@link DataGrant}
         * @throws SaiException
         */
        public TestableTask build() throws SaiException {
            Objects.requireNonNull(this.name, "Must provide the name of the task");
            Objects.requireNonNull(this.description, "Must provide a description of the task");
            if (this.dataset == null) { populateDataset(); }
            return new TestableTask(this);
        }
        
    }
    
}

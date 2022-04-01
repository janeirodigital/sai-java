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
public class TestableProject extends DataInstance {
    
    private String name;
    private String description;

    public TestableProject(Builder builder) throws SaiException {
        super(builder);
        this.name = builder.name;
        this.description = builder.description;
    }

    /**
     * Get a {@link TestableProject} from the provided <code>uri</code>.
     * @param uri URI to generate the {@link TestableProject} from
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use for retrieval
     * @return {@link TestableProject}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static TestableProject get(URI uri, SaiSession saiSession, ContentType contentType, ReadableDataGrant dataGrant, DataInstance parent) throws SaiException, SaiHttpNotFoundException {
        Objects.requireNonNull(dataGrant, "Must provide a readable data grant permitting the data instance to get");
        TestableProject.Builder builder = new TestableProject.Builder(uri, saiSession);
        if (parent != null) builder.setParent(parent);
        builder.setDataGrant(dataGrant).setDraft(false);
        try (Response response = read(uri, saiSession, contentType, false)) {
            return builder.setDataset(response).build();
        }
    }

    /**
     * Call {@link #get(URI, SaiSession, ContentType, ReadableDataGrant, DataInstance)} without specifying a desired content type for retrieval
     * @param uri URI of the {@link TestableProject} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link TestableProject}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public static TestableProject get(URI uri, SaiSession saiSession, ReadableDataGrant dataGrant, DataInstance parent) throws SaiHttpNotFoundException, SaiException {
        return get(uri, saiSession, DEFAULT_RDF_CONTENT_TYPE, dataGrant, parent);
    }

    public static List<TestableProject> getAccessible(ReadableDataGrant dataGrant, SaiSession saiSession) throws SaiHttpNotFoundException, SaiException {
        Objects.requireNonNull(dataGrant, "Must provide a data grant to get accessible data instances");
        Objects.requireNonNull(saiSession, "Must provide a sai session to get accessible data instances");
        List<TestableProject> testableProjects = new ArrayList<>();
        for (DataInstance dataInstance : dataGrant.getDataInstances()) { testableProjects.add(new TestableProject.Builder(dataInstance).build()); }
        return testableProjects;
    }

    // NOTE - In real world use it would not be necessary to pass milestoneTree, because it would be a constant,
    // globally accessible URI. In our test infrastructure we host these dynamically on a local server and the actual
    // URI is generated with each run, so it's passed here as a parameter.
    public List<TestableMilestone> getMilestones(URI milestoneTree) throws SaiException {
        List<TestableMilestone> testableMilestones = new ArrayList<>();
        for (DataInstance childInstance : this.getChildInstances(milestoneTree)) { testableMilestones.add(new TestableMilestone.Builder(childInstance).build()); }
        return testableMilestones;
    }

    public List<TestableIssue> getIssues(URI issueTree) throws SaiException {
        List<TestableIssue> testableIssues = new ArrayList<>();
        for (DataInstance childInstance : this.getChildInstances(issueTree)) { testableIssues.add(new TestableIssue.Builder(childInstance).build()); }
        return testableIssues;
    }

    public List<TestableTask> getTasks(URI taskTree) throws SaiException {
        List<TestableTask> testableTasks = new ArrayList<>();
        for (DataInstance childInstance : this.getChildInstances(taskTree)) { testableTasks.add(new TestableTask.Builder(childInstance).build()); }
        return testableTasks;
    }

    /**
     * Reload a new instance of {@link TestableProject} using the attributes of the current instance
     * @return Reloaded {@link TestableProject}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public TestableProject reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.uri, this.saiSession, this.contentType, this.getDataGrant(), this.getParent());
    }

    public static class Builder extends DataInstance.Builder<Builder> {

        private String name;
        private String description;
        
        /**
         * Initialize builder with <code>uri</code> and <code>saiSession</code>
         * @param uri URI of the {@link TestableProject} to build
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
                throw new SaiException("Unable to populate testable project: " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        protected void populateDataset() {
            this.resource = getNewResourceForType(this.uri, TESTABLE_PROJECT);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, TESTABLE_NAME, this.name);
            updateObject(this.resource, TESTABLE_DESCRIPTION, this.description);
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

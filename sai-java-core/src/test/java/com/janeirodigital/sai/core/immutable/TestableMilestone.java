package com.janeirodigital.sai.core.immutable;

import com.janeirodigital.sai.core.crud.SocialAgentRegistration;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.readable.DataInstance;
import com.janeirodigital.sai.core.readable.ReadableDataGrant;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.ContentType;
import com.janeirodigital.sai.httputils.SaiHttpException;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;
import com.janeirodigital.sai.rdfutils.SaiRdfException;
import com.janeirodigital.sai.rdfutils.SaiRdfNotFoundException;
import lombok.Getter;
import lombok.Setter;
import okhttp3.Response;
import org.apache.jena.rdf.model.Model;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.janeirodigital.sai.core.TestableVocabulary.*;
import static com.janeirodigital.sai.httputils.HttpUtils.DEFAULT_RDF_CONTENT_TYPE;
import static com.janeirodigital.sai.httputils.HttpUtils.getRdfModelFromResponse;
import static com.janeirodigital.sai.rdfutils.RdfUtils.*;

@Getter @Setter
public class TestableMilestone extends DataInstance {
    
    private String name;
    private String description;

    public TestableMilestone(Builder builder) throws SaiException {
        super(builder);
        this.name = builder.name;
        this.description = builder.description;
    }

    /**
     * Get a {@link TestableMilestone} from the provided <code>url</code>.
     * @param url URL to generate the {@link TestableMilestone} from
     * @param saiSession {@link SaiSession} to assign
     * @param contentType {@link ContentType} to use for retrieval
     * @return {@link TestableMilestone}
     * @throws SaiException
     * @throws SaiHttpNotFoundException
     */
    public static TestableMilestone get(URL url, SaiSession saiSession, ContentType contentType, ReadableDataGrant dataGrant, DataInstance parent) throws SaiException, SaiHttpNotFoundException {
        Objects.requireNonNull(dataGrant, "Must provide a readable data grant permitting the data instance to get");
        TestableMilestone.Builder builder = new TestableMilestone.Builder(url, saiSession);
        if (parent != null) builder.setParent(parent);
        builder.setDataGrant(dataGrant).setDraft(false);
        try (Response response = read(url, saiSession, contentType, false)) {
            return builder.setDataset(getRdfModelFromResponse(response)).build();
        } catch (SaiHttpException | SaiRdfException ex) {
            throw new SaiException("Unable to read testable milestone " + url, ex);
        }
    }

    /**
     * Call {@link #get(URL, SaiSession, ContentType, ReadableDataGrant, DataInstance)} without specifying a desired content type for retrieval
     * @param url URL of the {@link TestableMilestone} to get
     * @param saiSession {@link SaiSession} to assign
     * @return Retrieved {@link TestableMilestone}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public static TestableMilestone get(URL url, SaiSession saiSession, ReadableDataGrant dataGrant, DataInstance parent) throws SaiHttpNotFoundException, SaiException {
        return get(url, saiSession, DEFAULT_RDF_CONTENT_TYPE, dataGrant, parent);
    }

    public static List<TestableMilestone> getAccessible(ReadableDataGrant dataGrant, SaiSession saiSession) throws SaiException, SaiHttpNotFoundException {
        Objects.requireNonNull(dataGrant, "Must provide a data grant to get accessible data instances");
        Objects.requireNonNull(saiSession, "Must provide a sai session to get accessible data instances");
        List<TestableMilestone> testableMilestones = new ArrayList<>();
        for (DataInstance dataInstance : dataGrant.getDataInstances()) { testableMilestones.add(new TestableMilestone.Builder(dataInstance).build()); }
        return testableMilestones;
    }

    /**
     * Reload a new instance of {@link TestableMilestone} using the attributes of the current instance
     * @return Reloaded {@link TestableMilestone}
     * @throws SaiHttpNotFoundException
     * @throws SaiException
     */
    public TestableMilestone reload() throws SaiHttpNotFoundException, SaiException {
        return get(this.url, this.saiSession, this.contentType, this.getDataGrant(), this.getParent());
    }

    public static class Builder extends DataInstance.Builder<Builder> {

        private String name;
        private String description;
        
        /**
         * Initialize builder with <code>url</code> and <code>saiSession</code>
         * @param url URL of the {@link TestableMilestone} to build
         * @param saiSession {@link SaiSession} to assign
         */
        public Builder(URL url, SaiSession saiSession) { super(url, saiSession); }

        /**
         * Initialize builder with a {@DataInstance}
         * @param dataInstance {@link DataInstance} to initialize milestone from
         */
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
                throw new SaiException("Unable to populate testable milestone: " + ex.getMessage());
            }
        }

        /**
         * Populates the Jena dataset graph with the attributes from the Builder
         */
        protected void populateDataset() {
            this.resource = getNewResourceForType(this.url, TESTABLE_MILESTONE);
            this.dataset = this.resource.getModel();
            updateObject(this.resource, TESTABLE_NAME, this.name);
            updateObject(this.resource, TESTABLE_DESCRIPTION, this.description);
        }

        /**
         * Build the {@link TestableMilestone} using attributes from the Builder. If no Jena dataset has been
         * provided, then the dataset will be populated using the attributes from the Builder with
         * {@link #populateDataset()}.
         * @return {@link DataGrant}
         * @throws SaiException
         */
        public TestableMilestone build() throws SaiException {
            Objects.requireNonNull(this.name, "Must provide the name of the milestone");
            Objects.requireNonNull(this.description, "Must provide a description of the milestone");
            if (this.dataset == null) { populateDataset(); }
            return new TestableMilestone(this);
        }
        
    }
    
}

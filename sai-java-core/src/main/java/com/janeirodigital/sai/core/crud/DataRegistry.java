package com.janeirodigital.sai.core.crud;

import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.jena.rdf.model.Resource;

import java.net.URL;
import java.util.Iterator;
import java.util.List;

import static com.janeirodigital.sai.core.vocabularies.InteropVocabulary.HAS_DATA_REGISTRATION;

/**
 * Modifiable instantiation of an
 * <a href="https://solid.github.io/data-interoperability-panel/specification/#data-registry">Data Registry</a>
 */
@Getter
public class DataRegistry {

    private DataRegistrationList<DataRegistration> dataRegistrations;

    /**
     * Class for access and iteration of {@link DataRegistration}s. Most of the capability is provided
     * through extension of {@link RegistrationList}, which requires select overrides to ensure the correct
     * types are built and returned by the iterator.
     */
    public static class DataRegistrationList<T> extends RegistrationList<T> {
        public DataRegistrationList(DataFactory dataFactory, Resource resource) { super(dataFactory, resource, HAS_DATA_REGISTRATION); }

        @Override
        public T find(URL shapeTreeUrl) {
            for (T registration : this) {
                DataRegistration dataRegistration = (DataRegistration) registration;
                if (shapeTreeUrl.equals(dataRegistration.getRegisteredShapeTree())) { return (T) dataRegistration; }
            }
            return null;
        }

        @Override
        public Iterator<T> iterator() { return new DataRegistrationListIterator(this.getDataFactory(), this.getRegistrationUrls()); }

        private class DataRegistrationListIterator<T> extends RegistrationListIterator<T> {
            public DataRegistrationListIterator(DataFactory dataFactory, List<URL> registrationUrls) { super(dataFactory, registrationUrls); }
            @SneakyThrows
            @Override
            public T next() {
                URL registrationUrl = (URL) current.next();
                return (T) DataRegistration.get(registrationUrl, dataFactory);
            }
        }

    }

}

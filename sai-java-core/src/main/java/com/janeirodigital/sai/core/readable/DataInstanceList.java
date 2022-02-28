package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.factories.DataFactory;
import lombok.SneakyThrows;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class DataInstanceList implements Iterable<DataInstance> {
    
    private List<URL> dataInstanceUrls;
    protected DataFactory dataFactory;
    protected ReadableDataGrant dataGrant;

    /**
     * Construct a {@link DataInstanceList}
     * @param dataFactory {@link DataFactory} to assign
     * @param dataGrant {@link ReadableDataGrant} associated with instance access
     * @param dataInstanceUrls List of data instance URLs to iterate over and fetch
     */
    public DataInstanceList(DataFactory dataFactory, ReadableDataGrant dataGrant, List<URL> dataInstanceUrls) {
        Objects.requireNonNull(dataFactory, "Must provide a data factory for the data instance list");
        this.dataFactory = dataFactory;
        this.dataGrant = dataGrant;
        this.dataInstanceUrls = dataInstanceUrls;
    }

    public boolean isEmpty() { return this.dataInstanceUrls.isEmpty(); }

    /**
     * Return an iterator for the {@link DataInstanceList}
     * @return
     */
    public Iterator<DataInstance> iterator() {
        return new DataInstanceListIterator(this.dataFactory, this.dataGrant, this.dataInstanceUrls);
    }

    private class DataInstanceListIterator implements Iterator<DataInstance> {
        private final Iterator<URL> current;
        private final DataFactory dataFactory;
        private final ReadableDataGrant dataGrant;
        public DataInstanceListIterator(DataFactory dataFactory, ReadableDataGrant dataGrant, List<URL> dataInstanceUrls) {
            this.dataFactory = dataFactory;
            this.current = dataInstanceUrls.iterator();
            this.dataGrant = dataGrant;
        }
        public boolean hasNext() { return current.hasNext(); }
        @SneakyThrows
        public DataInstance next() {
            URL instanceUrl = current.next();
            return DataInstance.get(instanceUrl, dataFactory, dataGrant);
        }
    }

}

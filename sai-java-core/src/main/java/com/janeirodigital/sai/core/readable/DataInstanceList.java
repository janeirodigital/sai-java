package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.SneakyThrows;

import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class DataInstanceList implements Iterable<DataInstance> {
    
    private List<URL> dataInstanceUrls;
    protected SaiSession saiSession;
    protected ReadableDataGrant dataGrant;

    /**
     * Construct a {@link DataInstanceList}
     * @param saiSession {@link SaiSession} to assign
     * @param dataGrant {@link ReadableDataGrant} associated with instance access
     * @param dataInstanceUrls List of data instance URLs to iterate over and fetch
     */
    public DataInstanceList(SaiSession saiSession, ReadableDataGrant dataGrant, List<URL> dataInstanceUrls) {
        Objects.requireNonNull(saiSession, "Must provide a sai session for the data instance list");
        this.saiSession = saiSession;
        this.dataGrant = dataGrant;
        this.dataInstanceUrls = dataInstanceUrls;
    }

    public boolean isEmpty() { return this.dataInstanceUrls.isEmpty(); }

    /**
     * Return an iterator for the {@link DataInstanceList}
     * @return
     */
    public Iterator<DataInstance> iterator() {
        return new DataInstanceListIterator(this.saiSession, this.dataGrant, this.dataInstanceUrls);
    }

    private class DataInstanceListIterator implements Iterator<DataInstance> {
        private final Iterator<URL> current;
        private final SaiSession saiSession;
        private final ReadableDataGrant dataGrant;
        public DataInstanceListIterator(SaiSession saiSession, ReadableDataGrant dataGrant, List<URL> dataInstanceUrls) {
            this.saiSession = saiSession;
            this.current = dataInstanceUrls.iterator();
            this.dataGrant = dataGrant;
        }
        public boolean hasNext() { return current.hasNext(); }
        @SneakyThrows
        public DataInstance next() {
            URL instanceUrl = current.next();
            return DataInstance.get(instanceUrl, saiSession, dataGrant);
        }
    }

}

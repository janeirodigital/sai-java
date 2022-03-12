package com.janeirodigital.sai.core.readable;

import com.janeirodigital.sai.core.sessions.SaiSession;
import lombok.SneakyThrows;

import java.net.URL;
import java.util.*;

public class DataInstanceList implements Iterable<DataInstance> {
    
    protected Map<URL, DataInstance> dataInstanceUrls;
    protected SaiSession saiSession;
    protected ReadableDataGrant dataGrant;

    /**
     * Construct a {@link DataInstanceList}
     * @param saiSession {@link SaiSession} to assign
     * @param dataGrant {@link ReadableDataGrant} associated with instance access
     * @param dataInstanceUrls Map of {@link DataInstance} URLs to iterate and (optionally) their parent {@link DataInstance}s
     */
    public DataInstanceList(SaiSession saiSession, ReadableDataGrant dataGrant, Map<URL, DataInstance> dataInstanceUrls) {
        Objects.requireNonNull(saiSession, "Must provide a sai session for the data instance list");
        Objects.requireNonNull(dataGrant, "Must provide a data grant for the data instance list");
        Objects.requireNonNull(dataInstanceUrls, "Must provide a map of data instance urls and their associated parents where applicable");
        this.saiSession = saiSession;
        this.dataGrant = dataGrant;
        this.dataInstanceUrls = dataInstanceUrls;
    }

    /**
     * Indicates whether the {@link DataInstanceList} is empty
     * @return true when there are no data instances
     */
    public boolean isEmpty() { return this.dataInstanceUrls.isEmpty(); }

    /**
     * Indicates the number of {@link DataInstance}s in the list
     * @return Amount of {@link DataInstance}s in the list
     */
    public int size() { return this.dataInstanceUrls.size(); }

    /**
     * Return an iterator for the {@link DataInstanceList}
     * @return
     */
    public Iterator<DataInstance> iterator() {
        return new DataInstanceListIterator(this.saiSession, this.dataGrant, this.dataInstanceUrls);
    }

    private class DataInstanceListIterator implements Iterator<DataInstance> {
        private final Iterator<Map.Entry<URL,DataInstance>> current;
        private final SaiSession saiSession;
        private final ReadableDataGrant dataGrant;
        public DataInstanceListIterator(SaiSession saiSession, ReadableDataGrant dataGrant, Map<URL, DataInstance> dataInstanceUrls) {
            this.saiSession = saiSession;
            this.current = dataInstanceUrls.entrySet().iterator();
            this.dataGrant = dataGrant;
        }
        public boolean hasNext() { return current.hasNext(); }
        @SneakyThrows
        public DataInstance next() {
            Map.Entry pair = (Map.Entry) current.next();
            URL instanceUrl = (URL) pair.getKey();
            DataInstance parent = (DataInstance) pair.getValue();
            return DataInstance.get(instanceUrl, saiSession, false, dataGrant, parent);
        }
    }

}

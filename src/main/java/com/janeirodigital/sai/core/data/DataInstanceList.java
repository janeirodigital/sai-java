package com.janeirodigital.sai.core.data;

import com.janeirodigital.sai.core.authorizations.ReadableDataGrant;
import com.janeirodigital.sai.core.exceptions.SaiException;
import com.janeirodigital.sai.core.exceptions.SaiRuntimeException;
import com.janeirodigital.sai.core.sessions.SaiSession;
import com.janeirodigital.sai.httputils.SaiHttpNotFoundException;

import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class DataInstanceList implements Iterable<DataInstance> {
    
    protected Map<URI,DataInstance> dataInstanceUris;
    protected SaiSession saiSession;
    protected ReadableDataGrant dataGrant;

    /**
     * Construct a {@link DataInstanceList}
     * @param saiSession {@link SaiSession} to assign
     * @param dataGrant {@link ReadableDataGrant} associated with instance access
     * @param dataInstanceUris Map of {@link DataInstance} URIs to iterate and (optionally) their parent {@link DataInstance}s
     */
    public DataInstanceList(SaiSession saiSession, ReadableDataGrant dataGrant, Map<URI,DataInstance> dataInstanceUris) {
        Objects.requireNonNull(saiSession, "Must provide a sai session for the data instance list");
        Objects.requireNonNull(dataGrant, "Must provide a data grant for the data instance list");
        Objects.requireNonNull(dataInstanceUris, "Must provide a map of data instance uris and their associated parents when available");
        this.saiSession = saiSession;
        this.dataGrant = dataGrant;
        this.dataInstanceUris = dataInstanceUris;
    }

    /**
     * Indicates whether the {@link DataInstanceList} is empty
     * @return true when there are no data instances
     */
    public boolean isEmpty() { return this.dataInstanceUris.isEmpty(); }

    /**
     * Indicates the number of {@link DataInstance}s in the list
     * @return Amount of {@link DataInstance}s in the list
     */
    public int size() { return this.dataInstanceUris.size(); }

    /**
     * Return an iterator for the {@link DataInstanceList}
     * @return
     */
    public Iterator<DataInstance> iterator() {
        return new DataInstanceListIterator(this.saiSession, this.dataGrant, this.dataInstanceUris);
    }

    /**
     * Iterator that can be used to iterate over a list of {@link DataInstance} URIs,
     * returning a {@link BasicDataInstance} for each.
     */
    private static class DataInstanceListIterator implements Iterator<DataInstance> {
        private final Iterator<Map.Entry<URI, DataInstance>> current;
        private final SaiSession saiSession;
        private final ReadableDataGrant dataGrant;
        public DataInstanceListIterator(SaiSession saiSession, ReadableDataGrant dataGrant, Map<URI,DataInstance> dataInstanceUris) {
            this.saiSession = saiSession;
            this.current = dataInstanceUris.entrySet().iterator();
            this.dataGrant = dataGrant;
        }

        /**
         * Indicates whether there is another {@link DataInstance} in the list
         * @return true when there is another {@link DataInstance}
         */
        public boolean hasNext() { return current.hasNext(); }

        /**
         * Get the next {@link DataInstance} in the list. If there is an associated parent
         * {@link DataInstance}, it will be referenced in the returned {@link BasicDataInstance}
         * @return next {@link DataInstance} in the list as a {@link BasicDataInstance}
         */
        public DataInstance next() {
            try {
                Map.Entry<URI, DataInstance> pair = current.next();
                URI instanceUri = pair.getKey();
                DataInstance parent = pair.getValue();
                return BasicDataInstance.get(instanceUri, saiSession, dataGrant, parent);
            } catch (SaiException| SaiHttpNotFoundException ex) {
                throw new SaiRuntimeException("Failed to get data instance", ex);
            }
        }
    }

}

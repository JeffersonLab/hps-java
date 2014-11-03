package org.hps.monitoring.ecal.eventdisplay.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class <code>MapValueIterator</code> creates an iterator for the
 * values stored in a map.
 * 
 * @author Kyle McCarty
 * @param E The object type of the map's values.
 */
public class MapValueIterator<E> implements Iterator<E> {
    private Iterator<? extends Entry<?, E>> baseIterator;
    
    /**
     * Generates a new <code>MapValueIterator</code> from a given <code>
     * Map</code> with the appropriate parameterizations.
     */
    public MapValueIterator(Map<?, E> sourceMap) { baseIterator = sourceMap.entrySet().iterator(); }
    
    @Override
    public boolean hasNext() { return baseIterator.hasNext(); }
    
    @Override
    public E next() {
        // Get the next entry in the base iterator.
        Entry<?, E> next = baseIterator.next();
        
        // Return the value of the entry.
        return next.getValue();
    }
    
    @Override
    public void remove() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Operation \"remove\" is not supported for MapValueIterator.");
    }
}
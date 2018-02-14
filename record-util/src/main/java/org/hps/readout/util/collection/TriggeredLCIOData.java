package org.hps.readout.util.collection;

import java.util.HashSet;
import java.util.Set;

/**
 * Class <code>TriggeredLCIOData</code> represents data output from
 * a {@link org.hps.readout.ReadoutDriver ReadoutDriver}. It contains
 * both a list of the output data, corresponding to a single event,
 * and also the LCIO parameters needed to write the event.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @param <T> - The object type of the data stored by the collection.
 */
public class TriggeredLCIOData<T> extends LCIOData<T, LCIOCollection<T>, Set<T>> {
    /**
     * The collection data.
     */
    private final Set<T> data;
    
    /**
     * Creates a new <code>TriggeredLCIOData</code based on the
     * collection parameters defined by the <code>params</code>
     * object.
     * @param params - The collection parameters.
     */
    public TriggeredLCIOData(LCIOCollection<T> params) {
        super(params);
        this.data = new HashSet<T>();
    }
    
    /**
     * Creates a copy of this object with no data stored.
     * @return Returns a duplicate of this object with the same
     * parameters, but no stored data.
     */
    public TriggeredLCIOData<T> cloneEmpty() {
        return new TriggeredLCIOData<T>(getCollectionParameters());
    }
    
    @Override
    public Set<T> getData() {
        return data;
    }
}
package org.hps.readout.util.collection;

import java.util.LinkedList;

import org.hps.readout.util.TimedList;

/**
 * Class <code>ManagedLCIOData</code> represents the actual managed
 * data associated with a managed LCIO collection. It stores both the
 * collection parameters and also a list of data in the form of a
 * {@link org.hps.readout.util.TimedList TimedList}, where each entry
 * corresponds to the data present at a given time.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @param <T> - The object type of the data stored by the collection.
 */
public class ManagedLCIOData<T> {
    /**
     * The collection data. Each entry in the data list represents a
     * specific simulation time quantum, while the list itself holds
     * the collection object data.
     */
    private final LinkedList<TimedList<?>> data;
    /**
     * Stores the collection parameters for both writing the data to
     * an LCIO file and also managing it in the readout data manager.
     */
    private final ManagedLCIOCollection<T> params;
    
    /**
     * Creates a new <code>ManagedLCIOData</code> based on the
     * collection parameters defined by the <code>params</code>
     * object.
     * @param params - The collection parameters.
     */
    public ManagedLCIOData(ManagedLCIOCollection<T> params) {
        this.params = params;
        this.data = new LinkedList<TimedList<?>>();
    }
    
    /**
     * Gets the collection parameters for the data.
     * @return Returns the collection parameters as a {@link
     * org.hps.readout.util.collection.ManagedLCIOCollection
     * ManagedLCSimCollection} object.
     */
    public ManagedLCIOCollection<T> getCollectionParameters() {
        return params;
    }
    
    /**
     * Gets the collection data.
     * @return Returns the collection data. Collection data is stored
     * in a single list, which itself contains {@link
     * org.hps.readout.util.TimedList TimedList} objects. Each
     * <code>TimedList</code> object represents the collection data
     * generated at the simulation time of the list, which can be
     * obtained through the method {@link
     * org.hps.readout.util.TimedList#getTime() TimedList.getTime()}.
     */
    public LinkedList<TimedList<?>> getData() {
        return data;
    }
}
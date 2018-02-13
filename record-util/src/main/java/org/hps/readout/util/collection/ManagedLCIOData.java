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
public class ManagedLCIOData<T> extends LCIOData<T, ManagedLCIOCollection<T>, LinkedList<TimedList<?>>> {
    /**
     * The collection data. Each entry in the data list represents a
     * specific simulation time quantum, while the list itself holds
     * the collection object data.
     */
    private final LinkedList<TimedList<?>> data;
    
    /**
     * Creates a new <code>ManagedLCIOData</code> based on the
     * collection parameters defined by the <code>params</code>
     * object.
     * @param params - The collection parameters.
     */
    public ManagedLCIOData(ManagedLCIOCollection<T> params) {
        super(params);
        this.data = new LinkedList<TimedList<?>>();
    }
    
    @Override
    public LinkedList<TimedList<?>> getData() {
        return data;
    }
}
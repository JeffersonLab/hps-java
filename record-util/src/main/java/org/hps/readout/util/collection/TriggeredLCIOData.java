package org.hps.readout.util.collection;

import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>TriggeredLCIOData</code> represents data output from
 * a {@link org.hps.readout.ReadoutDriver ReadoutDriver}. It contains
 * both a list of the output data, corresponding to a single event,
 * and also the LCIO parameters needed to write the event.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @param <T> - The object type of the data stored by the collection.
 */
public class TriggeredLCIOData<T> {
    /**
     * The collection data.
     */
    private final List<T> data;
    /**
     * Stores the collection parameters for both writing the data to
     * an LCIO file and also managing it in the readout data manager.
     */
    private final LCIOCollection<T> params;
    
    /**
     * Creates a new <code>TriggeredLCIOData</code based on the
     * collection parameters defined by the <code>params</code>
     * object.
     * @param params - The collection parameters.
     */
    public TriggeredLCIOData(LCIOCollection<T> params) {
        this.params = params;
        this.data = new ArrayList<T>();
    }
    
    /**
     * Gets the collection parameters for the data.
     * @return Returns the collection parameters as a {@link
     * org.hps.readout.util.collection.LCIOCollection
     * LCSimCollection} object.
     */
    public LCIOCollection<T> getCollectionParameters() {
        return params;
    }
    
    /**
     * Gets the collection data.
     * @return Returns the collection data as a {@link java.util.List
     * List}.
     */
    public List<T> getData() {
        return data;
    }
}
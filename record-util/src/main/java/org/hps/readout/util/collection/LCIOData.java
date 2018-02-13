package org.hps.readout.util.collection;

import java.util.Collection;

/**
 * Class <code>LCIOData</code> functions as a superclass to all the
 * readout framework LCIO collection data set objects. It requires
 * that they define LCIO collection parameters, and provide access to
 * the data sets.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @param <T> - The object type of the data stored by the collection.
 * @param <E> - The type of LCIO collection parameters object used by
 * the data set.
 * @param <V> - The collection that actually stores the data.
 */
public abstract class LCIOData<T, E extends LCIOCollection<T>, V extends Collection<?>> {
    private final E params;
    
    /**
     * Instantiates the LCIO data collection.
     * @param collectionParameters
     */
    LCIOData(E collectionParameters) {
        params = collectionParameters;
    }
    
    /**
     * Gets the collection parameters for the data.
     * @return Returns the collection parameters as a {@link
     * org.hps.readout.util.collection.LCIOCollection
     * LCSimCollection} object or subclass.
     */
    public E getCollectionParameters() {
        return params;
    }
    
    /**
     * Gets the collection data.
     * @return Returns the collection data.
     */
    public abstract V getData();
}
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
public class TriggeredLCIOData<T> extends LCIOData<T, LCIOCollection<T>, List<T>> {
    /**
     * The collection data.
     */
    private final List<T> data;
    
    /**
     * Creates a new <code>TriggeredLCIOData</code based on the
     * collection parameters defined by the <code>params</code>
     * object.
     * @param params - The collection parameters.
     */
    public TriggeredLCIOData(LCIOCollection<T> params) {
        super(params);
        this.data = new ArrayList<T>();
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
    public List<T> getData() {
        return data;
    }
    
    /**
     * Merges the data set of the argument list into this list.
     * @param dataList - The data set to be merged into this list.
     * @throws IllegalArgumentException Occurs if the object type of
     * the argument list is not compatible with the data type stored
     * in this list.
     */
    @SuppressWarnings("unchecked")
    public void mergeDataList(TriggeredLCIOData<?> dataList) throws IllegalArgumentException {
        // Verify that the object types are the same.
        if(!getCollectionParameters().getObjectType().isAssignableFrom(dataList.getCollectionParameters().getObjectType())) {
            throw new IllegalArgumentException("Error: Can not merge data set of class " + dataList.getCollectionParameters().getObjectType().getSimpleName()
                    + " into data set of type " + getCollectionParameters().getObjectType().getSimpleName() + ".");
        }
        
        // Otherwise, combine the data sets. This cast is safe, since
        // we already verified that both lists contain objects of the
        // same type (or a subclass thereof).
        for(Object obj : dataList.getData()) {
            getData().add((T) obj);
        }
    }
}
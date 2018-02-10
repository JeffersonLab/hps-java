package org.hps.readout.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>LcsimSingleEventCollectionData</code> is an extension
 * of {@link org.hps.readout.util.LcsimCollection LcsimCollection}
 * which additionally stores a single set of collection data. It is
 * designed to be used to store on-trigger special data from {@link
 * org.hps.readout.ReadoutDriver ReadoutDriver} objects.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @param <T> - The object type of the data stored by the collection.
 */
public class LcsimSingleEventCollectionData<T> extends LcsimCollection<T> {
    /**
     * The collection data.
     */
    private final List<T> data;
    
    /**
     * Creates a new <code>LcsimSingleEventCollectionData</code
     * based on the collection parameters defined by the
     * <code>params</code> object.
     * @param params - The collection parameters.
     */
    public LcsimSingleEventCollectionData(LcsimCollection<T> params) {
        super(params.getCollectionName(), params.getProductionDriver(), params.getObjectType(), params.getGlobalTimeDisplacement());
        setPersistent(params.isPersistent());
        setFlags(params.getFlags());
        setReadoutName(params.getReadoutName());
        setWindowBefore(params.getWindowBefore());
        setWindowAfter(params.getWindowAfter());
        
        this.data = new ArrayList<T>();
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
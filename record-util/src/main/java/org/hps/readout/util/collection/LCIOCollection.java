package org.hps.readout.util.collection;

import org.hps.readout.ReadoutDriver;

/**
 * Class <code>LCIOCollection</code> represents a data collection
 * for use in the LCIO framework. It contains all of the information
 * needed to create and write an LCSim event to an LCIO file.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @param <T> - The object type of the data stored by the collection.
 */
public class LCIOCollection<T> implements Comparable<LCIOCollection<T>> {
    private final int flags;
    private final String readoutName;
    private final String collectionName;
    private final Class<T> objectType;
    private final ReadoutDriver productionDriver;
    
    /**
     * Instantiates a new <code>LCIOCollection</code> object with
     * the specified collection parameters.
     * @param collectionName - The collection name.
     * @param productionDriver - The driver that produces the
     * collection.
     * @param objectType - The type of object that is stored by the
     * collection.
     */
    LCIOCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType) {
        this(collectionName, productionDriver, objectType, 0, null);
    }
    
    /**
     * Instantiates a new <code>LCIOCollection</code> object with
     * the specified collection parameters.
     * @param collectionName - The collection name.
     * @param productionDriver - The driver that produces the
     * collection.
     * @param objectType - The type of object that is stored by the
     * collection.
     * @param flags - The LCIO flags for the collection.
     */
    LCIOCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, int flags) {
        this(collectionName, productionDriver, objectType, flags, null);
    }
    
    /**
     * Instantiates a new <code>LCIOCollection</code> object with
     * the specified collection parameters.
     * @param collectionName - The collection name.
     * @param productionDriver - The driver that produces the
     * collection.
     * @param objectType - The type of object that is stored by the
     * collection.
     * @param readoutName - The linked readout object name.
     */
    LCIOCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, String readoutName) {
        this(collectionName, productionDriver, objectType, 0, readoutName);
    }
    
    /**
     * Instantiates a new <code>LCIOCollection</code> object with
     * the specified collection parameters.
     * @param collectionName - The collection name.
     * @param productionDriver - The driver that produces the
     * collection.
     * @param objectType - The type of object that is stored by the
     * collection.
     * @param flags - The LCIO flags for the collection.
     * @param readoutName - The linked readout object name.
     */
    LCIOCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, int flags, String readoutName) {
        this.flags = flags;
        this.objectType = objectType;
        this.readoutName = readoutName;
        this.collectionName = collectionName;
        this.productionDriver = productionDriver;
    }
    
    /**
     * Instantiates a new <code>LCIOCollection</code> object with
     * all parameters identical to the argument object.
     * @param baseParams - The base object that should be copied by
     * this object.
     */
    LCIOCollection(LCIOCollection<T> baseParams) {
        this(baseParams.collectionName, baseParams.productionDriver, baseParams.objectType, baseParams.flags, baseParams.readoutName);
    }
    
    /**
     * Instantiates a new <code>LCIOCollection</code> object with
     * the specified collection name, and with all other parameters
     * having been drawn from the input parameters object.
     * @param collectionName - The new collection name.
     * @param baseParams - The object which should be used to define
     * all parameters other than the collection name.
     */
    LCIOCollection(String collectionName, LCIOCollection<T> baseParams) {
        this(collectionName, baseParams.productionDriver, baseParams.objectType, baseParams.flags, baseParams.readoutName);
    }
    
    /**
     * Compares two LCIO collections. They are sorted by object type,
     * collection name, and flags in this order. Note that the
     * production driver is not required to match.
     * @param otherCollection - The collection to which this object
     * will be compared.
     * @return Returns a value of <code>0</code> if the collections
     * are identical (ignoring the production driver). Otherwise, a
     * non-zero value will be returned based on the differing values.
     */
    @Override
    public int compareTo(LCIOCollection<T> otherCollection) {
        // The object types must be the same.
        if(otherCollection.getObjectType() != getObjectType()) {
            return getObjectType().getSimpleName().compareTo(otherCollection.getObjectType().getSimpleName());
        }
        
        // The collection names must be the same.
        int nameCompare = getCollectionName().compareTo(otherCollection.getCollectionName());
        if(nameCompare != 0) {
            return nameCompare;
        }
        
        // The flags must be the same.
        return Integer.compare(getFlags(), otherCollection.getFlags());
    }
    
    @Override
    public boolean equals(Object obj) {
        // The object must be of the same class type.
        if(!(obj instanceof LCIOCollection)) {
            return false;
        }
        
        // The object must have the same object type.
        LCIOCollection<?> lcioObj = (LCIOCollection<?>) obj;
        if(lcioObj.getClass() != getClass()) {
            return false;
        }
        
        // Finally, compare the object attributes. Note that the
        // preceding check requires that this object be properly
        // parameterized, so there is no risk of a case exception.
        @SuppressWarnings("unchecked")
        LCIOCollection<T> castObj = (LCIOCollection<T>) obj;
        return (compareTo(castObj) == 0);
    }
    
    /**
     * Gets the name of the LCIO collection.
     * @return Returns the collection name.
     */
    public String getCollectionName() {
        return collectionName;
    }
    
    /**
     * Gets the LCIO collection flags.
     * @return Returns the flags for the collection.
     */
    public int getFlags() {
        return flags;
    }
    
    /**
     * Gets the object type of the data stored in the collection.
     * @return Returns the object type of the collection data.
     */
    public Class<T> getObjectType() {
        return objectType;
    }
    
    /**
     * Gets the production driver that is responsible for creating the
     * collection.
     * @return Returns the driver which creates the collection.
     */
    public ReadoutDriver getProductionDriver() {
        return productionDriver;
    }
    
    /**
     * Gets the readout name for the collection. This will be
     * <code>null</code> if no readout name exists.
     * @return
     */
    public String getReadoutName() {
        return readoutName;
    }
}
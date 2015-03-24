package org.hps.record.scalars;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;

/**
 * This class encapsulates EVIO scalar data which is simply an array
 * of integer values.  The exact meaning of each of these integer
 * words is defined externally to this class.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ScalarData {
    
    // The default name for reading and writing the LCIO event collection.
    static String DEFAULT_SCALAR_DATA_COLLECTION_NAME = "ScalarData";
    
    // The scalar data values.
    int[] data;
    
    /**
     * This is the no argument constructor which is for package internal use.
     */
    ScalarData() {        
    }
    
    /**
     * Create from provided scalar data values.
     * @param data The scalar data.
     */
    public ScalarData(int[] data) {        
        this.data = new int[data.length];        
        System.arraycopy(data, 0, this.data, 0, data.length);
    }
    
    /**
     * Get the number of scalars.
     * @return The number of scalars.
     */
    public int size() {
        return data.length;
    }
    
    /**
     * Get the scalar data value at the index.
     * @param index The scalar data index.
     * @return The scalar data value.
     */
    public Integer getValue(int index) {
        return data[index];
    }
    
    /**
     * Convert this object to an lcsim {@link org.lcsim.event.GenericObject} for
     * persistency to LCIO.
     * @return The LCIO GenericObject.
     */
    GenericObject toGenericObject() {
        ScalarsGenericObject object = new ScalarsGenericObject();
        object.values = data;
        return object;
    }
    
    /**
     * Load data into this object from an {@link org.lcsim.event.GenericObject}
     * read from an LCIO event.
     * @param object The GenericObject with the scalar data.
     */
    void fromGenericObject(GenericObject object) {
        this.data = new int[object.getNInt()];
        for (int index = 0; index < object.getNInt(); index++) {
            this.data[index] = object.getIntVal(index);
        }        
    }
    
    /**
     * Write this object out to an LCIO event.
     * @param event The output LCIO event.
     */
    public void write(EventHeader event) {
        List<GenericObject> collection = new ArrayList<GenericObject>();
        collection.add(toGenericObject());
        event.put(DEFAULT_SCALAR_DATA_COLLECTION_NAME, collection, GenericObject.class, 0);
    }
    
    /**
     * Create a new object from the data in an LCIO event, using the default collection name.
     * @param event The LCIO event data.
     * @return The created ScalarData object or null if does not exist in event.
     */
    public static ScalarData read(EventHeader event) {
        ScalarData data = null;
        if (event.hasCollection(GenericObject.class, DEFAULT_SCALAR_DATA_COLLECTION_NAME)) {
            List<GenericObject> objects = event.get(GenericObject.class, DEFAULT_SCALAR_DATA_COLLECTION_NAME);
            data = new ScalarData();
            data.fromGenericObject(objects.get(0));
        }
        return data;
    }
    
    /**
     * Convert this object to a readable string, which is basically just a list of int values
     * enclosed in braces and separated by commas.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (int value : data) {
            sb.append(value + ", ");
        }
        sb.setLength(sb.length() - 2);        
        sb.append("]");
        return sb.toString();
    }    
}

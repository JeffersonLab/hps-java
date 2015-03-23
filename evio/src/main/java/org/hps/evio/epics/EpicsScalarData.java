package org.hps.evio.epics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;

/**
 * <p>
 * This is an API for reading and writing EPICS scalar data to LCIO events,
 * as well as parsing the scalar data from a CDATA section of an EVIO string
 * data bank.
 * <p>Sample data:<br/>
 * <pre>
 * 2010.350952  MBSY2C_energy
 * 0.000000  PSPECIRBCK
 * 2.190000  HPS:LS450_2:FIELD
 * -8974.000000  HPS:LS450_1:FIELD
 * 2400.000000  MTIRBCK
 * 3.882200  VCG2C21
 * 4.579233  VCG2C21A
 * 6.799115  VCG2C24A
 * 6.552529  VCG2H00A
 * 5.429465  VCG2H01A
 * 5.741360  VCG2H02A
 * -0.069630  scaler_calc1
 * 0.000000  scalerS12b
 * 0.000000  scalerS13b
 * 0.000000  scalerS14b
 * 0.000000  scalerS15b
 * 0.000000  hallb_IPM2C21A_XPOS
 * 0.000000  hallb_IPM2C21A_YPOS
 * 0.000000  hallb_IPM2C21A_CUR
 * 0.000000  hallb_IPM2C24A_XPOS
 * 0.000000  hallb_IPM2C24A_YPOS
 * 0.000000  hallb_IPM2C24A_CUR
 * 0.000000  hallb_IPM2H00_XPOS
 * 0.000000  hallb_IPM2H00_YPOS
 * 0.000000  hallb_IPM2H00_XPOS
 * 0.000000  hallb_IPM2H00_XPOS
 * 0.000000  hallb_IPM2H00_CUR
 * 0.000000  hallb_IPM2H02_YPOS
 * 0.000000  hallb_IPM2H02_XPOS
 * </pre>   
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
*/
// TODO: This API needs to be accessible to recon and analysis modules.
public final class EpicsScalarData extends LinkedHashMap<String, Double> {
    
    // Used in collection parameter map as name of the key list.
    static final String EPICS_SCALAR_NAMES = "EPICS_SCALAR_NAMES";
    
    // Default collection name in the LCIO event.
    static final String DEFAULT_COLLECTION_NAME = "EpicsScalarData";
    
    // Dummy collection parameter maps to try and make LCIO happy.
    static final Map<String, int[]> DUMMY_INT_MAP = new HashMap<String, int[]>();    
    static final Map<String, float[]> DUMMY_FLOAT_MAP = new HashMap<String, float[]>();
   
    /**
     * Convert this object to a string.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for (Entry<String, Double> entry : this.entrySet()) {
            sb.append(entry.getKey() + " " + entry.getValue() + '\n');
        }
        return sb.toString();
    }

    /**
     * Parse a raw data string from the EVIO data bank and
     * turn it into a list of keys and values within this object. 
     * @param rawData The raw data in the form of a string.
     */
    void fromString(String rawData) {
        String lines[] = rawData.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() == 0) {
                continue;
            }
            String[] data = trimmed.split("  ");
            Double value = Double.parseDouble(data[0]);
            String key = data[1];
            System.out.println("adding key, value: " + data[1] + " " + data[0]);
            put(key, value);
        }
    }

    /**
     * Convert this object into a {@link org.lcsim.event.GenericObject} 
     * that can be written into an LCIO collection.
     * @return The GenericObject representing this data.
     */
    EpicsGenericObject toGenericObject() {
        EpicsGenericObject newObject = new EpicsGenericObject();
        newObject.keys = new String[this.size()];
        newObject.values = new double[this.size()];
        int index = 0;
        for (String key : this.keySet()) {
            newObject.keys[index] = key;
            newObject.values[index] = this.get(key);
            index++;
        }
        return newObject;
    }

    /**
     * Given a list of keys, read the double values from the 
     * {@link org.lcsim.event.GenericObject} into the map.
     * @param object
     * @param keys
     */
    void fromGenericObject(GenericObject object, String[] keys) {
        for (int index = 0; index < keys.length; index++) {
            this.put(keys[index], object.getDoubleVal(index));
        }
    }

    /**
     * Write this object into an LCIO event under the given collection name.
     * @param event The LCIO event.
     * @param collectionName The name of the collection in the event.
     */
    void write(EventHeader event, String collectionName) {
        List<GenericObject> collection = new ArrayList<GenericObject>();
        EpicsGenericObject object = this.toGenericObject();
        collection.add(object);
        Map<String, String[]> stringMap = new HashMap<String, String[]>();
        stringMap.put(EPICS_SCALAR_NAMES, object.keys);
        event.put(collectionName, collection, GenericObject.class, 0, DUMMY_INT_MAP, DUMMY_FLOAT_MAP, stringMap);
    }
    
    /**
     * Write this object into an LCIO event using the default collection name.
     * @param event The LCIO event.
     */
    void write(EventHeader event) {
        write(event, DEFAULT_COLLECTION_NAME);
    }

    /**
     * Read data into this object from an LCIO event from the given collection name.
     * @param event The LCIO event.
     * @param collectionName The collection name.
     * @return The EPICS data from the LCIO event.
     */
    EpicsScalarData read(EventHeader event, String collectionName) {
        List<GenericObject> collection = event.get(GenericObject.class, collectionName);
        @SuppressWarnings("rawtypes")
        Map stringMap = event.getMetaData(collection).getStringParameters();
        String[] keys = (String[]) stringMap.get(EPICS_SCALAR_NAMES);
        EpicsScalarData data = new EpicsScalarData();
        data.fromGenericObject(collection.get(0), keys);
        return data;
    }
    
    /**
     * Read data into this object from an LCIO event using the default collection name.
     * This is the primary method for users to read the EPICS data into their Drivers
     * in the <code>process</code> method.         
     * @param event The LCIO event. 
     * @return The EPICS data from the event.
     */
    public EpicsScalarData read(EventHeader event) {
        return read(event, DEFAULT_COLLECTION_NAME);
    }
}
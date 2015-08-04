package org.hps.record.epics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;

/**
 * This is an API for reading and writing EPICS data to LCIO events, as well as parsing the data from a CDATA section
 * within an EVIO string data bank. The {@link #read(EventHeader)} method should be used to create one of these objects
 * from an LCIO event. The keys are stored in the string parameters of the collection, because
 * <code>GenericObject</code> cannot persist string data.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class EpicsData {

    /**
     * Default collection name in the LCSim events.
     */
    private static final String DEFAULT_COLLECTION_NAME = "EpicsData";

    /**
     * Dummy float parameters to make LCIO persistency work.
     */
    private static final Map<String, float[]> DUMMY_FLOAT_MAP = new HashMap<String, float[]>();

    /**
     * Dummy int parameters to make LCIO persistency work.
     */
    private static final Map<String, int[]> DUMMY_INT_MAP = new HashMap<String, int[]>();

    /**
     * Collection parameter that has the EPICS variable names.
     */
    private static final String EPICS_VARIABLE_NAMES = "EPICS_VARIABLE_NAMES";

    /**
     * This map contains the list of EPICS keys and their descriptions from the<br/>
     * <a href="https://confluence.slac.stanford.edu/display/hpsg/EVIO+Data+Format">EVIO Data Format Confluence Page</a>
     */
    private final static Map<String, String> VARIABLES = new HashMap<String, String>();

    /**
     * List of possible EPICS keys.
     */
    static {
        VARIABLES.put("MBSY2C_energy", "Beam energy according to Hall B BSY dipole string");
        VARIABLES.put("PSPECIRBCK", "Pair Spectrometer Current Readback");
        VARIABLES.put("HPS:LS450_2:FIELD", "Frascati probe field");
        VARIABLES.put("HPS:LS450_1:FIELD", "Pair Spectrometer probe field");
        VARIABLES.put("MTIRBCK", "Frascati Current Readback");
        VARIABLES.put("VCG2C21 2C21", "Vacuum gauge pressure");
        VARIABLES.put("VCG2C21A", "2C21A Vacuum gauge pressure");
        VARIABLES.put("VCG2C24A", "2C24A Vacuum gauge pressure");
        VARIABLES.put("VCG2H00A", "2H00 Vacuum gauge pressure");
        VARIABLES.put("VCG2H01A", "2H01 Vacuum gauge pressure");
        VARIABLES.put("VCG2H02A", "2H02 Vacuum gauge pressure");
        VARIABLES.put("scaler_calc1", "Faraday cup current");
        VARIABLES.put("scalerS12b", "HPS-Left beam halo count");
        VARIABLES.put("scalerS13b", "HPS-Right beam halo count");
        VARIABLES.put("scalerS14b", "HPS-Top beam halo count");
        VARIABLES.put("scalerS15b", "HPS-SC beam halo count");
        VARIABLES.put("hallb_IPM2C21A_XPOS", "Beam position X at 2C21");
        VARIABLES.put("hallb_IPM2C21A_YPOS", "Beam position Y at 2C21");
        VARIABLES.put("hallb_IPM2C21A_CUR", "Current at 2C21");
        VARIABLES.put("hallb_IPM2C24A_XPOS", "Beam position X at 2C24");
        VARIABLES.put("hallb_IPM2C24A_YPOS", "Beam position Y at 2C24");
        VARIABLES.put("hallb_IPM2C24A_CUR", "Current at 2C24");
        VARIABLES.put("hallb_IPM2H00_XPOS", "Beam position X at 2H00");
        VARIABLES.put("hallb_IPM2H00_YPOS", "Beam position Y at 2H00");
        VARIABLES.put("hallb_IPM2H00_CUR", "Current at 2H00");
        VARIABLES.put("hallb_IPM2H02_YPOS", "Beam position X at 2H02");
        VARIABLES.put("hallb_IPM2H02_XPOS", "Beam position Y at 2H02");
    }

    /**
     * Get the description of a named EPICS variable.
     *
     * @param name the name of the variable
     */
    public static String getVariableDescription(final String name) {
        return VARIABLES.get(name);
    }

    /**
     * Get the static list of all available EPICs variable names.
     * <p>
     * This could be different than the variable names which were actually written into the collection header. For this,
     * instead use the method {@link #getKeys()} method.
     *
     * @return the set of default EPICS variable names
     */
    public static Set<String> getVariableNames() {
        return VARIABLES.keySet();
    };

    /**
     * <p>
     * Read data into this object from an LCIO event using the default collection name.
     * <p>
     * This is the primary method for users to read the EPICS data into their Drivers in the
     * {@link org.lcsim.util.Driver#process(EventHeader)} method.
     *
     * @param event the LCIO event
     * @return the EPICS data from the event
     */
    public static EpicsData read(final EventHeader event) {
        if (event.hasCollection(GenericObject.class, EpicsData.DEFAULT_COLLECTION_NAME)) {
            return read(event, DEFAULT_COLLECTION_NAME);
        } else {
            return null;
        }
    }

    /**
     * Read data into this object from a collection in the LCIO event with the given collection name.
     *
     * @param event the LCIO event
     * @param collectionName the collection name
     * @return the EPICS data from the LCIO event
     */
    private static EpicsData read(final EventHeader event, final String collectionName) {
        final List<GenericObject> collection = event.get(GenericObject.class, collectionName);
        @SuppressWarnings("rawtypes")
        final Map stringMap = event.getMetaData(collection).getStringParameters();
        final String[] keys = (String[]) stringMap.get(EPICS_VARIABLE_NAMES);
        final EpicsData data = new EpicsData();
        data.fromGenericObject(collection.get(0), keys);
        return data;
    }

    /**
     * The mapping of EPICS variable names to their double values.
     */
    private final Map<String, Double> dataMap = new LinkedHashMap<String, Double>();

    /**
     * The EPICS header information.
     */
    private EpicsHeader epicsHeader;

    /**
     * Class constructor.
     */
    public EpicsData() {
    }

    /**
     * Class constructor that parses string data.
     *
     * @param data the string data
     */
    EpicsData(final String data) {
        this.fromString(data);
    }

    /**
     * Given a list of names, read the double values from the {@link org.lcsim.event.GenericObject} into the data map of
     * this object.
     *
     * @param object the <code>GenericObject</code> with the data values
     * @param names The list of names.
     */
    private void fromGenericObject(final GenericObject object, final String[] names) {

        // Read data from double array.
        for (int index = 0; index < names.length; index++) {
            this.dataMap.put(names[index], object.getDoubleVal(index));
        }

        // Read header data if set.
        if (object.getNInt() > 0) {
            final int[] headerData = new int[] {object.getIntVal(0), object.getIntVal(1), object.getIntVal(2)};
            this.epicsHeader = new EpicsHeader(headerData);
        }
    }

    /**
     * Parse a raw data string from the EVIO data bank and turn it into a list of keys and values within this object.
     *
     * @param rawData the raw EPICS data in the form of a string
     */
    void fromString(final String rawData) {
        final String lines[] = rawData.split("\\r?\\n");
        for (final String line : lines) {
            final String trimmed = line.trim();
            if (trimmed.length() == 0) {
                continue;
            }
            final String[] data = trimmed.split("  ");
            final Double value = Double.parseDouble(data[0]);
            final String key = data[1];
            this.dataMap.put(key, value);
        }
    }

    /**
     * Get the EPICS header information or <code>null</code> if not set.
     */
    public EpicsHeader getEpicsHeader() {
        return this.epicsHeader;
    }

    /**
     * Get the list of EPICS variables used by this object.
     * <p>
     * This could potentially be different than the list of default names from {@link #getVariableNames()} as not all
     * variables are included in every EPICS event.
     *
     * @return the list of used EPICS variable names
     */
    public Set<String> getKeys() {
        return this.dataMap.keySet();
    }

    /**
     * Get a double value from the key, which should be a valid EPICS variable name.
     *
     * @return the value from the key
     */
    public Double getValue(final String name) {
        return this.dataMap.get(name);
    }

    /**
     * Return <code>true</code> if the data has the given key.
     *
     * @return <code>true</code> if data has the given key
     */
    public boolean hasKey(final String key) {
        return this.getKeys().contains(key);
    }

    /**
     * Set the EPICS header information.
     *
     * @param epicsHeader the {@link EpicsHeader} object
     */
    void setEpicsHeader(final EpicsHeader epicsHeader) {
        this.epicsHeader = epicsHeader;
    }

    /**
     * Set a double value by name.
     *
     * @return the value from the key
     */
    public void setValue(final String name, final double value) {
        this.dataMap.put(name, value);
    }

    /**
     * Convert this object into a {@link org.lcsim.event.GenericObject} that can be written into an LCIO collection.
     *
     * @return the <code>GenericObject</code> representing this data
     */
    private EpicsGenericObject toGenericObject() {

        // Create new GenericObject.
        final EpicsGenericObject newObject = new EpicsGenericObject();

        newObject.setKeys(new String[this.dataMap.size()]);
        newObject.setValues(new double[this.dataMap.size()]);
        
        int index = 0;
        for (final String key : this.dataMap.keySet()) {
            newObject.setKey(index, key);
            newObject.setValue(index, this.dataMap.get(key));
            index++;
        }

        // Write header information into the object's int array.
        if (epicsHeader != null) {
            final int[] headerData = new int[] {
                    epicsHeader.getRun(), 
                    epicsHeader.getSequence(),
                    epicsHeader.getTimeStamp()};
            newObject.setHeaderData(headerData);
        }
        return newObject;
    }

    /**
     * Convert this object to a string.
     *
     * @return this object converted to a string
     */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        for (final Entry<String, Double> entry : this.dataMap.entrySet()) {
            sb.append(entry.getKey() + " " + entry.getValue() + '\n');
        }
        return sb.toString();
    }

    /**
     * Write this object's data into a <code>GenericObject</code> collection in the LCIO event using the default
     * collection name.
     *
     * @param event the LCIO event
     */
    public void write(final EventHeader event) {
        this.write(event, DEFAULT_COLLECTION_NAME);
    }

    /**
     * Write this object into an LCIO event with the given collection name.
     *
     * @param event the LCIO event
     * @param collectionName the name of the collection in the output event
     */
    private void write(final EventHeader event, final String collectionName) {

        // Create the new collection and add the GenericObject to it.
        final List<GenericObject> collection = new ArrayList<GenericObject>();
        final EpicsGenericObject object = this.toGenericObject();
        collection.add(object);

        // Write out the collection to the event, including the string parameters with the key names.
        final Map<String, String[]> stringMap = new HashMap<String, String[]>();
        stringMap.put(EPICS_VARIABLE_NAMES, object.getKeys());
        event.put(collectionName, collection, GenericObject.class, 0, DUMMY_INT_MAP, DUMMY_FLOAT_MAP, stringMap);
    }
}

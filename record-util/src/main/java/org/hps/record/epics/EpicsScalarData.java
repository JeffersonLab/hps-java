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
 * This is an API for reading and writing EPICS scalar data to LCIO events, as well as parsing the scalar data from a
 * CDATA section within an EVIO string data bank. The {@link #read(EventHeader)} method should be used to create one of
 * these objects from an LCIO event.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class EpicsScalarData {

    /**
     * Default collection name in the LCSim events.
     */
    public static final String DEFAULT_COLLECTION_NAME = "EpicsScalarData";

    /**
     * This map contains the list of EPICS key descriptions from the<br/>
     * <a href="https://confluence.slac.stanford.edu/display/hpsg/EVIO+Data+Format">EVIO Data Format Confluence Page</a>
     */
    private final static Map<String, String> DESCRIPTIONS = new HashMap<String, String>();

    /**
     * Dummy float parameters to make LCIO persistency work.
     */
    static final Map<String, float[]> DUMMY_FLOAT_MAP = new HashMap<String, float[]>();

    /**
     * Dummy int parameters to make LCIO persistency work.
     */
    static final Map<String, int[]> DUMMY_INT_MAP = new HashMap<String, int[]>();

    /**
     * Collection parameter that has the EPICS variable names.
     */
    public static final String EPICS_SCALAR_NAMES = "EPICS_SCALAR_NAMES";

    /**
     * List of descriptions.
     */
    // FIXME: Maybe this should not be listed here.
    static {
        DESCRIPTIONS.put("MBSY2C_energy", "Beam energy according to Hall B BSY dipole string");
        DESCRIPTIONS.put("PSPECIRBCK", "Pair Spectrometer Current Readback");
        DESCRIPTIONS.put("HPS:LS450_2:FIELD", "Frascati probe field");
        DESCRIPTIONS.put("HPS:LS450_1:FIELD", "Pair Spectrometer probe field");
        DESCRIPTIONS.put("MTIRBCK", "Frascati Current Readback");
        DESCRIPTIONS.put("VCG2C21 2C21", "Vacuum gauge pressure");
        DESCRIPTIONS.put("VCG2C21A", "2C21A Vacuum gauge pressure");
        DESCRIPTIONS.put("VCG2C24A", "2C24A Vacuum gauge pressure");
        DESCRIPTIONS.put("VCG2H00A", "2H00 Vacuum gauge pressure");
        DESCRIPTIONS.put("VCG2H01A", "2H01 Vacuum gauge pressure");
        DESCRIPTIONS.put("VCG2H02A", "2H02 Vacuum gauge pressure");
        DESCRIPTIONS.put("scaler_calc1", "Faraday cup current");
        DESCRIPTIONS.put("scalerS12b", "HPS-Left beam halo count");
        DESCRIPTIONS.put("scalerS13b", "HPS-Right beam halo count");
        DESCRIPTIONS.put("scalerS14b", "HPS-Top beam halo count");
        DESCRIPTIONS.put("scalerS15b", "HPS-SC beam halo count");
        DESCRIPTIONS.put("hallb_IPM2C21A_XPOS", "Beam position X at 2C21");
        DESCRIPTIONS.put("hallb_IPM2C21A_YPOS", "Beam position Y at 2C21");
        DESCRIPTIONS.put("hallb_IPM2C21A_CUR", "Current at 2C21");
        DESCRIPTIONS.put("hallb_IPM2C24A_XPOS", "Beam position X at 2C24");
        DESCRIPTIONS.put("hallb_IPM2C24A_YPOS", "Beam position Y at 2C24");
        DESCRIPTIONS.put("hallb_IPM2C24A_CUR", "Current at 2C24");
        DESCRIPTIONS.put("hallb_IPM2H00_XPOS", "Beam position X at 2H00");
        DESCRIPTIONS.put("hallb_IPM2H00_YPOS", "Beam position Y at 2H00");
        DESCRIPTIONS.put("hallb_IPM2H00_CUR", "Current at 2H00");
        DESCRIPTIONS.put("hallb_IPM2H02_YPOS", "Beam position X at 2H02");
        DESCRIPTIONS.put("hallb_IPM2H02_XPOS", "Beam position Y at 2H02");
    }

    /**
     * Get the static list of available EPICs scalar names.
     * <p>
     * This could be different than the variable names which were actually written into the collection header. For this,
     * instead use the method {@link #getUsedNames()}.
     *
     * @return the set of default EPICS scalar names
     */
    public static Set<String> getDefaultNames() {
        return DESCRIPTIONS.keySet();
    };

    /**
     * Get the description of a named EPICS variable.
     *
     * @param name the name of the scalar
     */
    public static String getDescription(final String name) {
        return DESCRIPTIONS.get(name);
    }

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
    public static EpicsScalarData read(final EventHeader event) {
        if (event.hasCollection(GenericObject.class, EpicsScalarData.DEFAULT_COLLECTION_NAME)) {
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
    static EpicsScalarData read(final EventHeader event, final String collectionName) {
        final List<GenericObject> collection = event.get(GenericObject.class, collectionName);
        @SuppressWarnings("rawtypes")
        final Map stringMap = event.getMetaData(collection).getStringParameters();
        final String[] keys = (String[]) stringMap.get(EPICS_SCALAR_NAMES);
        final EpicsScalarData data = new EpicsScalarData();
        data.fromGenericObject(collection.get(0), keys);
        return data;
    }

    /**
     * The mapping of EPICS variable names to their double values.
     */
    private final Map<String, Double> dataMap = new LinkedHashMap<String, Double>();

    /**
     * Given a list of names, read the double values from the {@link org.lcsim.event.GenericObject} into the data map of
     * this object.
     *
     * @param object the <code>GenericObject</code> with the scalar values
     * @param names The list of names.
     */
    void fromGenericObject(final GenericObject object, final String[] names) {
        for (int index = 0; index < names.length; index++) {
            this.dataMap.put(names[index], object.getDoubleVal(index));
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
     * Get the list of used EPICS scalars in this object.
     * <p>
     * This could potentially be different than the list of default names from {@link #getDefaultNames()} but it will
     * usually be the same.
     *
     * @return the list of used EPICS scalar names
     */
    public Set<String> getUsedNames() {
        return this.dataMap.keySet();
    }

    /**
     * Get a double value from the key which should be a valid EPICS variable name.
     *
     * @return the value from the key
     */
    public Double getValue(final String name) {
        return this.dataMap.get(name);
    }

    /**
     * Convert this object into a {@link org.lcsim.event.GenericObject} that can be written into an LCIO collection.
     *
     * @return the <code>GenericObject</code> representing this data
     */
    EpicsGenericObject toGenericObject() {
        final EpicsGenericObject newObject = new EpicsGenericObject();
        newObject.setKeys(new String[this.dataMap.size()]);
        newObject.setValues(new double[this.dataMap.size()]);
        int index = 0;
        for (final String key : this.dataMap.keySet()) {
            newObject.setKey(index, key);
            newObject.setValue(index, this.dataMap.get(key));
            index++;
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
        write(event, DEFAULT_COLLECTION_NAME);
    }

    /**
     * Write this object into an LCIO event with the given collection name.
     *
     * @param event the LCIO event
     * @param collectionName the name of the collection in the output event
     */
    void write(final EventHeader event, final String collectionName) {
        final List<GenericObject> collection = new ArrayList<GenericObject>();
        final EpicsGenericObject object = toGenericObject();
        collection.add(object);
        final Map<String, String[]> stringMap = new HashMap<String, String[]>();
        stringMap.put(EPICS_SCALAR_NAMES, object.getKeys());
        event.put(collectionName, collection, GenericObject.class, 0, DUMMY_INT_MAP, DUMMY_FLOAT_MAP, stringMap);
    }
}
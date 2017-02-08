package org.hps.record.epics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hps.record.evio.EventTagConstant;
import org.hps.record.evio.EvioBankTag;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;

/**
 * This is an API for reading and writing EPICS data to LCIO events, as well as parsing the data from a CDATA section
 * within an EVIO string data bank. The {@link #read(EventHeader)} method should be used to create one of these objects
 * from an LCIO event. The keys are stored in the string parameters of the collection, because
 * <code>GenericObject</code> cannot persist string data.
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
     * <p>
     * Read data into this object from an LCIO event using the default collection name.
     * <p>
     * This is the primary method for users to read the EPICS data into their Drivers in the
     * {@link org.lcsim.util.Driver#process(EventHeader)} method.
     *
     * @param event the LCIO event
     * @return the EPICS data from the event or null if none exists
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
    public void setEpicsHeader(final EpicsHeader epicsHeader) {
        this.epicsHeader = epicsHeader;
    }

    /**
     * Set a variable's value.
     *
     * @param name the name of the variable
     * @param value the new value of the variable
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
            final int[] headerData = new int[] {epicsHeader.getRun(), epicsHeader.getSequence(),
                    epicsHeader.getTimestamp()};
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
    
    /**
     * Get EPICS data from an EVIO event.
     * 
     * @param evioEvent the EVIO event
     * @return the EPICS data or <code>null</code> if it is not present in the event
     */
    // FIXME: Not currently used.
    public static EpicsData getEpicsData(EvioEvent evioEvent) {
        
        EpicsData epicsData = null;
        
        // Is this an EPICS event?
        if (EventTagConstant.EPICS.matches(evioEvent)) {

            // Find the bank with the EPICS data string.
            final BaseStructure epicsBank = EvioBankTag.EPICS_STRING.findBank(evioEvent);

            // Was EPICS data found in the event?
            if (epicsBank != null) {

                // Create EpicsData object from bank's string data.
                epicsData = new EpicsData(epicsBank.getStringData()[0]);

                // Find the header information in the event.
                final BaseStructure headerBank = EvioBankTag.EPICS_HEADER.findBank(evioEvent);

                if (headerBank != null) {
                    // Set the header object.
                    epicsData.setEpicsHeader(EpicsHeader.fromEvio(headerBank.getIntData()));
                } else {
                    throw new RuntimeException("EPICS data is missing header.");
                }

            } else {
                // This is an error because the string data bank should always be present in EPICS events.
                throw new RuntimeException("No data bank found in EPICS event.");
            }
        }
        return epicsData;
    }
    
}

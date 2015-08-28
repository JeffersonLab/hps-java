package org.hps.record.scalers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.GenericObject;

/**
 * This class encapsulates EVIO scaler data which is simply an array of integer values. The exact meaning of each of
 * these integer words is defined externally to this class.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class ScalerData {

    /**
     * Fixed array size of scaler data in the EVIO bank.
     */
    public static final int ARRAY_SIZE = 72;

    /**
     * Default name of scaler data collection in LCSim events.
     */
    private static final String DEFAULT_COLLECTION_NAME = "ScalerData";

    /**
     * Dummy float parameters to make LCIO persistency work.
     */
    private static final Map<String, float[]> DUMMY_FLOAT_MAP = new HashMap<String, float[]>();

    /**
     * Dummy int parameters to make LCIO persistency work.
     */
    private static final Map<String, String[]> DUMMY_STRING_MAP = new HashMap<String, String[]>();

    /**
     * Name of timestamp field in collection parameters.
     */
    private static final String TIMESTAMP = "TIMESTAMP";

    /**
     * Create a new <code>ScalerData</code> object from an LCIO event, using the default collection name.
     *
     * @param event the LCIO event data
     * @return the <code>ScalerData</code> object or <code>null</code> if there's no scaler data in the event
     */
    public static ScalerData read(final EventHeader event) {
        return read(event, DEFAULT_COLLECTION_NAME);
    }

    /**
     * Create a new object from the data in an LCIO event, using the default collection name.
     *
     * @param event the LCIO event data
     * @return the <code>ScalerData</code> object or <code>null</code> if does not exist in event
     */
    public static ScalerData read(final EventHeader event, final String collectionName) {
        ScalerData data = null;
        if (event.hasCollection(GenericObject.class, collectionName)) {

            // Read from generic object.
            final List<GenericObject> objects = event.get(GenericObject.class, collectionName);
            data = new ScalerData();
            data.fromGenericObject(objects.get(0));

            // Set event ID.
            data.eventId = event.getEventNumber();

            // Read timestamp.
            final LCMetaData metadata = event.getMetaData(objects);
            try {
                data.timestamp = metadata.getIntegerParameters().get(TIMESTAMP)[0];
            } catch (final Exception e) {
                throw new RuntimeException("Scaler data is missing timestamp parameter.", e);
            }
        }
        return data;
    }

    /**
     * The scaler data values.
     */
    private int[] data;

    /**
     * The event ID of the data.
     */
    private Integer eventId;

    /**
     * The timestamp of the scaler event.
     */
    private Integer timestamp;

    /**
     * This is the no argument constructor which is for package internal use only.
     */
    ScalerData() {
    }

    /**
     * Create from provided scaler data values.
     *
     * @param data the scaler data
     */
    public ScalerData(final int[] data, final int eventId, final int timestamp) {
        this.data = new int[data.length];
        System.arraycopy(data, 0, this.data, 0, data.length);
        this.eventId = eventId;
        this.timestamp = timestamp;
    }

    /**
     * Load data into this object from an {@link org.lcsim.event.GenericObject} read from an LCIO event.
     *
     * @param object the <code>GenericObject</code> with the scaler data
     */
    private void fromGenericObject(final GenericObject object) {
        this.data = new int[object.getNInt()];
        for (int index = 0; index < object.getNInt(); index++) {
            this.data[index] = object.getIntVal(index);
        }
    }

    /**
     * Get the event ID of the scaler data.
     * <p>
     * This information is not persisted to the LCIO.
     *
     * @return the event ID of the scaler data
     */
    public Integer getEventId() {
        // Null value will be returned here to indicate not set.
        return this.eventId;
    }

    /**
     * Get the scaler data's Unix timestamp.
     *
     * @return the scaler data's Unix timestamp
     */
    public int getTimestamp() {
        return this.timestamp;
    }

    /**
     * Get the scaler data value at the index.
     *
     * @param index the scaler data index
     * @return the scaler data value
     */
    public Integer getValue(final int index) {
        return this.data[index];
    }

    /**
     * Get the value using a {@link ScalerDataIndex} enum.
     *
     * @return the value at the index
     */
    public Integer getValue(final ScalerDataIndex scalarDataIndex) {
        return this.data[scalarDataIndex.index()];
    }

    /**
     * Set the event ID of the scaler data.
     *
     * @param eventId the event ID of the scaler data
     */
    void setEventId(final int eventId) {
        this.eventId = eventId;
    }

    /**
     * Get the number of scalers.
     *
     * @return the number of scalers
     */
    public int size() {
        return this.data.length;
    }

    /**
     * Convert this object to an LCSim {@link org.lcsim.event.GenericObject} for persistency to LCIO.
     *
     * @return the LCIO <code>GenericObject</code> containing scaler data
     */
    private GenericObject toGenericObject() {
        final ScalersGenericObject object = new ScalersGenericObject(this.data);
        return object;
    }

    /**
     * Convert this object to a readable string, which is a list of integer values enclosed in braces and separated by
     * commas.
     *
     * @return this object converted to a string
     */
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("[");
        for (final int value : this.data) {
            sb.append(value + ", ");
        }
        sb.setLength(sb.length() - 2);
        sb.append("]");
        return sb.toString();
    }

    /**
     * Write this object out to an LCIO event using the default collection name.
     *
     * @param event the output LCIO event
     */
    public void write(final EventHeader event) {
        this.write(event, DEFAULT_COLLECTION_NAME);
    }

    /**
     * Write this object out to an LCIO event using the given collection name.
     *
     * @param event the output LCIO event
     * @param collectionName the name of the output collection
     */
    private void write(final EventHeader event, final String collectionName) {

        // Create generic object collection.
        final List<GenericObject> collection = new ArrayList<GenericObject>();
        collection.add(this.toGenericObject());

        // Add collection parameter with timestamp.
        final Map<String, int[]> timestampParameter = new HashMap<String, int[]>();
        timestampParameter.put(TIMESTAMP, new int[] {timestamp});

        // Put collection into event.
        event.put(collectionName, collection, GenericObject.class, 0, timestampParameter, DUMMY_FLOAT_MAP,
                DUMMY_STRING_MAP);
    }
}

package org.hps.record.scalars;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;

/**
 * This class encapsulates EVIO scalar data which is simply an array of integer values. The exact meaning of each of
 * these integer words is defined externally to this class.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class ScalarData {

    /**
     * Default name of scalar data collection in LCSim events.
     */
    static String DEFAULT_SCALAR_DATA_COLLECTION_NAME = "ScalarData";

    /**
     * Create a new <code>ScalarData</code> object from an LCIO event, using the default collection name.
     *
     * @param event the LCIO event data
     * @return the <code>ScalarData</code> object or <code>null</code> if there's no scalar data in the event
     */
    public static ScalarData read(final EventHeader event) {
        return read(event, DEFAULT_SCALAR_DATA_COLLECTION_NAME);
    }

    /**
     * Create a new object from the data in an LCIO event, using the default collection name.
     *
     * @param event the LCIO event data
     * @return the <code>ScalarData</code> object or <code>null</code> if does not exist in event
     */
    public static ScalarData read(final EventHeader event, final String collectionName) {
        ScalarData data = null;
        if (event.hasCollection(GenericObject.class, collectionName)) {
            final List<GenericObject> objects = event.get(GenericObject.class, collectionName);
            data = new ScalarData();
            data.fromGenericObject(objects.get(0));
        }
        return data;
    }

    /**
     * The scalar data values.
     */
    private int[] data;

    /**
     * This is the no argument constructor which is for package internal use only.
     */
    ScalarData() {
    }

    /**
     * Create from provided scalar data values.
     *
     * @param data the scalar data
     */
    public ScalarData(final int[] data) {
        this.data = new int[data.length];
        System.arraycopy(data, 0, this.data, 0, data.length);
    }

    /**
     * Load data into this object from an {@link org.lcsim.event.GenericObject} read from an LCIO event.
     *
     * @param object the <code>GenericObject</code> with the scalar data
     */
    void fromGenericObject(final GenericObject object) {
        this.data = new int[object.getNInt()];
        for (int index = 0; index < object.getNInt(); index++) {
            this.data[index] = object.getIntVal(index);
        }
    }

    /**
     * Get the scalar data value at the index.
     *
     * @param index the scalar data index
     * @return the scalar data value
     */
    public Integer getValue(final int index) {
        return this.data[index];
    }

    /**
     * Get the number of scalars.
     *
     * @return the number of scalars
     */
    public int size() {
        return this.data.length;
    }

    /**
     * Convert this object to an LCSim {@link org.lcsim.event.GenericObject} for persistency to LCIO.
     *
     * @return the LCIO <code>GenericObject</code> containing scalar data
     */
    GenericObject toGenericObject() {
        final ScalarsGenericObject object = new ScalarsGenericObject(this.data);
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
        write(event, DEFAULT_SCALAR_DATA_COLLECTION_NAME);
    }

    /**
     * Write this object out to an LCIO event using the given collection name.
     *
     * @param event the output LCIO event
     * @param collectionName the name of the output collection
     */
    public void write(final EventHeader event, final String collectionName) {
        final List<GenericObject> collection = new ArrayList<GenericObject>();
        collection.add(toGenericObject());
        event.put(collectionName, collection, GenericObject.class, 0);
    }
}

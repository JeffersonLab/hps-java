package org.hps.readout.util;

import org.hps.readout.ReadoutDriver;

/**
 * Class <code>LcsimCollection</code> represents a data collection
 * for use in the LCIO framework. It contains all of the information
 * needed to create and write an LCSim event to an LCIO file. It also
 * stores information pertinent to the readout system, such as the
 * time displacement and readout window sizing for the collection.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @param <T> - The object type of the data stored by the collection.
 */
public class LcsimCollection<T> {
    private int flags = 0;
    private final Class<T> objectType;
    private final double timeDisplacement;
    private final String collectionName;
    private String readoutName = null;
    private boolean persistent = true;
    private final ReadoutDriver productionDriver;
    private double windowBefore = Double.NaN;
    private double windowAfter = Double.NaN;
    
    /**
     * Instantiates a new <code>LcsimCollection</code> object with
     * the specified collection parameters.
     * @param collectionName - The collection name.
     * @param productionDriver - The driver that produces the
     * collection.
     * @param objectType - The type of object that is stored by the
     * collection.
     * @param globalTimeDisplacement - The total amount of time by
     * which the objects in the collection are displaced from their
     * source truth objects.
     */
    public LcsimCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double globalTimeDisplacement) {
        this.objectType = objectType;
        this.timeDisplacement = globalTimeDisplacement;
        this.productionDriver = productionDriver;
        this.collectionName = collectionName;
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
     * Gets the amount of time by which the collection is offset due
     * to production driver behavior and buffering.
     * @return Returns the total time offset in units of nanoseconds.
     */
    public double getGlobalTimeDisplacement() {
        return timeDisplacement;
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
    
    /**
     * Specifies the amount of time after the trigger time in which
     * data from this collection will be written into readout events.
     * @return Returns the post-trigger readout period in units of
     * nanoseconds.
     */
    public double getWindowAfter() {
        return windowAfter;
    }
    
    /**
     * Specifies the amount of time before the trigger time in which
     * data from this collection will be written into readout events.
     * @return Returns the pre-trigger readout period in units of
     * nanoseconds.
     */
    public double getWindowBefore() {
        return windowBefore;
    }
    
    /**
     * Specifies whether or not this collection should be written
     * into the output readout events.
     * @return Returns <code>true</code> if the collection should be
     * output into readout, and <code>false</code> otherwise.
     */
    public boolean isPersistent() {
        return persistent;
    }
    
    /**
     * Sets the LCIO readout flags.
     * @param value - The readout flags to use when writing data.
     */
    public void setFlags(int value) {
        flags = value;
    }
    
    /**
     * Sets whether not the collection should be written to data.
     * @param state - <code>true</code> indicates the collection
     * should be output into readout, and <code>false</code>
     * otherwise.
     */
    public void setPersistent(boolean state) {
        persistent = state;
    }
    
    /**
     * Sets the readout name for the collection. A value of
     * <code>null</code> represents no readout name at all.
     * @param value - The readout name for the collection.
     */
    public void setReadoutName(String value) {
        readoutName = value;
    }
    
    /**
     * Sets the amount of time after the trigger time in which data
     * from the collection will be output to the readout event.
     * @param value - The post-trigger readout window size in units
     * of nanoseconds.
     */
    public void setWindowAfter(double value) {
        windowAfter = value;
    }
    
    /**
     * Sets the amount of time before the trigger time in which data
     * from the collection will be output to the readout event.
     * @param value - The pre-trigger readout window size in units of
     * nanoseconds.
     */
    public void setWindowBefore(double value) {
        windowBefore = value;
    }
}
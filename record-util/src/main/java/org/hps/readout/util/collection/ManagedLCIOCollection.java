package org.hps.readout.util.collection;

import org.hps.readout.ReadoutDriver;

/**
 * Class <code>ManagedLCIOCollection</code> represents a data
 * collection for use in the LCIO framework. It contains all of the
 * information needed to create and write an LCSim event to an LCIO
 * file. It additionally stores the parameters needed by the readout
 * data manager to handle readout and maintenance of the collection.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @param <T> - The object type of the data stored by the collection.
 */
public class ManagedLCIOCollection<T> extends LCIOCollection<T> {
    private boolean persistent = true;
    private final double timeDisplacement;
    private double windowAfter = Double.NaN;
    private double windowBefore = Double.NaN;
    
    /**
     * Instantiates a new <code>ManagedLCIOCollection</code> object
     * with the specified collection parameters.
     * @param collectionName - The collection name.
     * @param productionDriver - The driver that produces the
     * collection.
     * @param objectType - The type of object that is stored by the
     * collection.
     * @param timeDisplacement - The total amount of time by which
     * this collection is displaced from its originating  objects due
     * to processing needs.
     */
    ManagedLCIOCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double timeDisplacement) {
        this(collectionName, productionDriver, objectType, timeDisplacement, 0, null);
    }
    
    /**
     * Instantiates a new <code>ManagedLCIOCollection</code> object
     * with the specified collection parameters.
     * @param collectionName - The collection name.
     * @param productionDriver - The driver that produces the
     * collection.
     * @param objectType - The type of object that is stored by the
     * collection.
     * @param timeDisplacement - The total amount of time by which
     * this collection is displaced from its originating  objects due
     * to processing needs.
     * @param flags - The LCIO flags for the collection.
     */
    ManagedLCIOCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double timeDisplacement, int flags) {
        this(collectionName, productionDriver, objectType, timeDisplacement, flags, null);
    }
    
    /**
     * Instantiates a new <code>ManagedLCIOCollection</code> object
     * with the specified collection parameters.
     * @param collectionName - The collection name.
     * @param productionDriver - The driver that produces the
     * collection.
     * @param objectType - The type of object that is stored by the
     * collection.
     * @param timeDisplacement - The total amount of time by which
     * this collection is displaced from its originating  objects due
     * to processing needs.
     * @param readoutName - The linked readout object name.
     */
    ManagedLCIOCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double timeDisplacement, String readoutName) {
        this(collectionName, productionDriver, objectType, timeDisplacement, 0, readoutName);
    }
    
    /**
     * Instantiates a new <code>ManagedLCIOCollection</code> object
     * with the specified collection parameters.
     * @param collectionName - The collection name.
     * @param productionDriver - The driver that produces the
     * collection.
     * @param objectType - The type of object that is stored by the
     * collection.
     * @param timeDisplacement - The total amount of time by which
     * this collection is displaced from its originating  objects due
     * to processing needs.
     * @param flags - The LCIO flags for the collection.
     * @param readoutName - The linked readout object name.
     */
    ManagedLCIOCollection(String collectionName, ReadoutDriver productionDriver, Class<T> objectType, double timeDisplacement,
            int flags, String readoutName) {
        super(collectionName, productionDriver, objectType, flags, readoutName);
        this.timeDisplacement = timeDisplacement;
    }
    
    /**
     * Instantiates a new <code>ManagedLCIOCollection</code> object
     * with all parameters identical to the argument object.
     * @param baseParams - The base object that should be copied by
     * this object.
     * @param timeDisplacement - The total amount of time by which
     * this collection is displaced from its originating  objects due
     * to processing needs.
     */
    ManagedLCIOCollection(LCIOCollection<T> baseParams, double timeDisplacement) {
        super(baseParams);
        this.timeDisplacement = timeDisplacement;
    }
    
    /**
     * Instantiates a new <code>ManagedLCIOCollection</code> object
     * with all parameters identical to the argument object.
     * @param baseParams - The base object that should be copied by
     * this object.
     */
    ManagedLCIOCollection(ManagedLCIOCollection<T> baseParams) {
        super(baseParams);
        this.persistent = baseParams.persistent;
        this.windowAfter = baseParams.windowAfter;
        this.windowBefore = baseParams.windowBefore;
        this.timeDisplacement = baseParams.timeDisplacement;
    }
    
    /**
     * Instantiates a new <code>ManagedLCIOCollection</code> object
     * with the specified collection name, and with all other
     * parameters having been drawn from the input parameters object.
     * @param collectionName - The new collection name.
     * @param baseParams - The object which should be used to define
     * all parameters other than the collection name.
     */
    ManagedLCIOCollection(String collectionName, ManagedLCIOCollection<T> baseParams) {
        super(collectionName, baseParams);
        this.persistent = baseParams.persistent;
        this.windowAfter = baseParams.windowAfter;
        this.windowBefore = baseParams.windowBefore;
        this.timeDisplacement = baseParams.timeDisplacement;
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
     * Sets whether not the collection should be written to data.
     * @param state - <code>true</code> indicates the collection
     * should be output into readout, and <code>false</code>
     * otherwise.
     */
    void setPersistent(boolean state) {
        persistent = state;
    }
    
    /**
     * Sets the amount of time after the trigger time in which data
     * from the collection will be output to the readout event.
     * @param value - The post-trigger readout window size in units
     * of nanoseconds.
     */
    void setWindowAfter(double value) {
        windowAfter = value;
    }
    
    /**
     * Sets the amount of time before the trigger time in which data
     * from the collection will be output to the readout event.
     * @param value - The pre-trigger readout window size in units of
     * nanoseconds.
     */
    void setWindowBefore(double value) {
        windowBefore = value;
    }
}
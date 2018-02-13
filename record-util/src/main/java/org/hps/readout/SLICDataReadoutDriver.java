package org.hps.readout;

import java.util.ArrayList;
import java.util.List;

import org.hps.readout.util.collection.LCIOCollection;
import org.hps.readout.util.collection.LCIOCollectionFactory;
import org.lcsim.event.EventHeader;

/**
 * Class <code>SLICDataReadoutDriver</code> is responsible for taking
 * in SLIC objects from a source Monte Carlo file and feeding them to
 * the {@link org.hps.readout.ReadoutDataManager ReadoutDataManager}.
 * It is also responsible for performing any special actions needed
 * when a triggered event is written, if necessary.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @param <E> - The object type for the SLIC objects managed by this
 * driver.
 */
public abstract class SLICDataReadoutDriver<E> extends ReadoutDriver {
    
    // ==============================================================
    // ==== LCIO Collections ========================================
    // ==============================================================
    
    /**
     * The name of the SLIC collection that is handled by the driver.
     */
    protected String collectionName = null;
    
    // ==============================================================
    // ==== Driver Parameters =======================================
    // ==============================================================
    
    /**
     * The output flags that should be used for the handled data
     * collection when written to LCIO.
     */
    protected final int flags;
    /**
     * The object type of the handled SLIC data.
     */
    protected final Class<E> type;
    
    // ==============================================================
    // ==== Debug Output Writers ====================================
    // ==============================================================
    
    /**
     * A writer that outputs debug text to a text file. It is made
     * available for use with subclasses, but does not perform any
     * action by default.
     */
    protected TempOutputWriter writer = null;
    
    /**
     * Instantiates a default data object handler driver for the
     * given object type with no flags.
     * @param classType - The object type that is handled by this
     * driver.
     */
    protected SLICDataReadoutDriver(Class<E> classType) {
        // Instantiate the superclass.
        this(classType, 0);
    }
    
    /**
     * Instantiates a data object handler driver for the given object
     * with the specified flags.
     * @param classType - The object type that is handled by this
     * driver.
     * @param flags - The LCIO flags that should be used with the
     * driver's output.
     */
    protected SLICDataReadoutDriver(Class<E> classType, int flags) {
        // Set the object type and flags.
        type = classType;
        this.flags = flags;
    }
    
    @Override
    public void startOfData() {
        // Define the LCSim output collection parameters.
        LCIOCollectionFactory.setCollectionName(collectionName);
        LCIOCollectionFactory.setProductionDriver(this);
        LCIOCollectionFactory.setFlags(flags);
        LCIOCollection<E> mcCollectionParams = LCIOCollectionFactory.produceLCIOCollection(type);
        
        // Register the handled collection with the data management
        // driver.
        // TODO: Truth data should be handled by the SVT and ECal drivers more directly.
        ReadoutDataManager.registerCollection(mcCollectionParams, true, 8.0, 32.0);
        
        // DEBUG :: Pass the writer to the superclass writer list.
        writer = new TempOutputWriter(collectionName + ".log");
        writers.add(writer);
        
        // Run the superclass method.
        super.startOfData();
    }
    
    @Override
    public void process(EventHeader event) {
        // Get the collection from the event header. If none exists,
        // just produce an empty list.
        List<E> slicData;
        if(event.hasCollection(type, collectionName)) {
            slicData = event.get(type, collectionName);
        } else {
            slicData = new ArrayList<E>(0);
        }
        
        // DEBUG :: Output debug text, if debug text is enabled. Note
        //          that this will do nothing unless the implementing
        //          driver actually supports debug output.
        if(debug) { writeData(slicData); }
        
        // Add the SLIC data to the readout data manager.
        ReadoutDataManager.addData(collectionName, slicData, type);
    }
    
    @Override
    protected double getTimeDisplacement() {
        return 0;
    }

    @Override
    protected double getTimeNeededForLocalOutput() {
        return 0;
    }
    
    /**
     * Writes debug output data, if supported. This behavior must be
     * implemented by a subclass driver if desired, and by default
     * does nothing.
     * @param data - The list of data objects from the current event.
     */
    protected void writeData(List<E> data) { }
    
    /**
     * Sets the name of the SLIC collection that is handled by this
     * driver. Note that this must match the name of the collection
     * used by SLIC, and will also be the name of the output data
     * collection.
     * @param collection
     */
    public void setCollectionName(String collection) {
        collectionName = collection;
    }
}
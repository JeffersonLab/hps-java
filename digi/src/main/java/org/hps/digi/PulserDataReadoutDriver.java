package org.hps.digi;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.readout.ReadoutDataManager;
import org.hps.readout.ReadoutDriver;
import org.hps.readout.util.collection.LCIOCollection;
import org.hps.readout.util.collection.LCIOCollectionFactory;
import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.geometry.IDDecoder;

/**
 * Class <code>PulserDataReadoutDriver</code> is responsible for taking in
 * pulser-data objects from a source file with (MC + pulser data) overlaid
 * events and feeding them to the {@link org.hps.readout.ReadoutDataManager
 * ReadoutDataManager}. It is also responsible for performing any special
 * actions needed when a triggered event is written, if necessary.
 * 
 * @author Tongtong Cao <caot@jlab.org>
 * @param <E> - The object type for the pulser-data objects managed by this
 * driver.
 */
public abstract class PulserDataReadoutDriver<E> extends ReadoutDriver {

    // ==============================================================
    // ==== pulser data Collections ========================================
    // ==============================================================

    /**
     * The name of the pulser data collection that is handled by the driver.
     */
    protected String collectionName = null;

    /**
     * The modified name of the pulser data collection. The collection name is
     * modified to be different with names of other collections.
     */
    protected String collectionNameModified = null;

    // ==============================================================
    // ==== Driver Parameters =======================================
    // ==============================================================

    /**
     * The output flags that should be used for the handled data collection when
     * written to LCIO.
     */
    protected final int flags;
    /**
     * The object type of the handled pulse data.
     */
    protected final Class<E> type;
    /**
     * The readout name for the managed collection.
     */
    private String readoutName = null;
    /**
     * The ID decoder used to interpret cell IDs for the objects managed by this
     * driver.
     */
    protected IDDecoder decoder = null;

    /**
     * Instantiates a default data object handler driver for the given object type
     * with no flags.
     * 
     * @param classType - The object type that is handled by this driver.
     */
    protected PulserDataReadoutDriver(Class<E> classType) {
        // Instantiate the superclass.
        this(classType, 0);
    }

    /**
     * Instantiates a data object handler driver for the given object with the
     * specified flags.
     * 
     * @param classType - The object type that is handled by this driver.
     * @param flags     - The LCIO flags that should be used with the driver's
     *                  output.
     */
    protected PulserDataReadoutDriver(Class<E> classType, int flags) {
        // Set the object type and flags.
        type = classType;
        this.flags = flags;
    }

    @Override
    public void process(EventHeader event) {
        // Get the collection from the event header. If none exists,
        // just produce an empty list.
        List<E> pulserData;

        if (event.hasCollection(type, collectionName)) {
            // Get the data from the event.
            pulserData = event.get(type, collectionName);
            // Instantiate the ID decoder, if needed,
            if (decoder == null) {
                decoder = event.getMetaData(pulserData).getIDDecoder();
            }
        } else {
            pulserData = new ArrayList<E>(0);
        }

        // Check the event metadata and attempt to extract a readout
        // name for the collection. If it exists, is not null, and is
        // different than the current readout name, the data manager
        // should be updated accordingly.
        LCMetaData metaData = event.getMetaData(pulserData);
        if (metaData != null) {
            String subdetectorName = null;
            if (metaData.getIDDecoder() != null && metaData.getIDDecoder().getSubdetector() != null) {
                subdetectorName = metaData.getIDDecoder().getSubdetector().getName();
            }
            if (subdetectorName != null) {
                String readoutName = DatabaseConditionsManager.getInstance().getDetectorObject()
                        .getSubdetector(subdetectorName).getReadout().getName();
                if (readoutName != null && this.readoutName != readoutName) {
                    this.readoutName = readoutName;
                    ReadoutDataManager.updateCollectionReadoutName(collectionNameModified, type, readoutName);
                }
            }
        }

        // Add the pulser data to the readout data manager.
        ReadoutDataManager.addData(collectionNameModified, pulserData, type);
    }

    @Override
    public void startOfData() {
        collectionNameModified = "PulserData" + collectionName;

        // Define the LCSim output collection parameters.
        LCIOCollectionFactory.setCollectionName(collectionNameModified);
        LCIOCollectionFactory.setProductionDriver(this);
        LCIOCollectionFactory.setFlags(flags);
        LCIOCollection<E> mcCollectionParams = LCIOCollectionFactory.produceLCIOCollection(type);

        // Register the handled collection with the data management
        // driver.
        ReadoutDataManager.registerCollection(mcCollectionParams, isPersistent(), getReadoutWindowBefore(),
                getReadoutWindowAfter());
    }

    /**
     * Sets the name of the pulser-data collection that is handled by this driver.
     * Note that this must match the name of the collection used by pulser-data, and
     * will also be the name of the output data collection.
     * 
     * @param collection
     */
    public void setCollectionName(String collection) {
        collectionName = collection;
    }

    @Override
    protected IDDecoder getIDDecoder(String collectionName)
            throws IllegalArgumentException, UnsupportedOperationException {
        if (collectionName.compareTo(this.collectionName) == 0) {
            if (decoder == null) {
                throw new RuntimeException(
                        "IDDecoder for collection \"" + collectionName + "\" has not yet been instantiated.");
            } else {
                return decoder;
            }
        } else {
            throw new IllegalArgumentException("Collection \"" + collectionName + "\" is not managed by driver \""
                    + this.getClass().getSimpleName() + "\".");
        }
    }

    @Override
    protected double getTimeDisplacement() {
        return 0;
    }

    @Override
    protected double getTimeNeededForLocalOutput() {
        return isPersistent() ? getReadoutWindowAfter() : 0;
    }

}

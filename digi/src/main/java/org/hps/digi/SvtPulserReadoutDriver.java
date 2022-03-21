package org.hps.digi;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.readout.ReadoutDataManager;
import org.hps.readout.ReadoutDriver;
import org.hps.readout.util.collection.LCIOCollection;
import org.hps.readout.util.collection.LCIOCollectionFactory;
import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.event.RawTrackerHit;
import org.hps.readout.util.collection.TriggeredLCIOData;

/**
 * Class <code>SvtPulserReadoutDriver</code> is responsible for taking in
 * pulser-data objects from a source file with (MC + pulser data) overlaid
 * events and feeding them to the {@link org.hps.readout.ReadoutDataManager
 * ReadoutDataManager}. It is also responsible for performing any special
 * actions needed when a triggered event is written, if necessary.
 * 
 * @author Tongtong Cao <caot@jlab.org>
 * @param <E> - The object type for the pulser-data objects managed by this
 * driver.
 */
public  class SvtPulserReadoutDriver extends ReadoutDriver {

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
    protected final int flags=0x0;
    
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
     */
    public SvtPulserReadoutDriver() {       
    }

   
    @Override
    public void process(EventHeader event) {
        // Get the collection from the event header. If none exists,
        // just produce an empty list.
        List<RawTrackerHit> pulserData;

        if (event.hasCollection(RawTrackerHit.class, collectionName)) {
            // Get the data from the event.
            pulserData = event.get(RawTrackerHit.class, collectionName);
            // Instantiate the ID decoder, if needed,
            if (decoder == null) {
                decoder = event.getMetaData(pulserData).getIDDecoder();
            }
        } else {
            pulserData = new ArrayList<RawTrackerHit>(0);
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
                    ReadoutDataManager.updateCollectionReadoutName(collectionNameModified, RawTrackerHit.class, readoutName);
                }
            }
        }
        ReadoutDataManager.addData(collectionNameModified, pulserData, RawTrackerHit.class);
    }

    @Override
    public void startOfData() {
        collectionNameModified = "PulserData" + collectionName;

        // Define the LCSim output collection parameters.
        LCIOCollectionFactory.setCollectionName(collectionNameModified);
        LCIOCollectionFactory.setProductionDriver(this);
        LCIOCollectionFactory.setFlags(flags);
        LCIOCollection<RawTrackerHit> mcCollectionParams = LCIOCollectionFactory.produceLCIOCollection(RawTrackerHit.class);

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

    protected Collection<TriggeredLCIOData<?>> getOnTriggerData(double triggerTime) {
        // If hodoscope hits are not persisted, truth data doesn't
        // need to be written out.
        if (!isPersistent()) {
            return null;
        }

        // Get the truth hits in the indicated time range.
        Collection<RawTrackerHit> truthHits = ReadoutDataManager.getData(triggerTime - getReadoutWindowBefore(),
                triggerTime + getReadoutWindowAfter(), collectionNameModified, RawTrackerHit.class);

        // Create the truth MC particle collection.
        LCIOCollection<RawTrackerHit> truthRawHitCollection = ReadoutDataManager
                .getCollectionParameters(collectionNameModified, RawTrackerHit.class);
        TriggeredLCIOData<RawTrackerHit> truthRawHitData = new TriggeredLCIOData<RawTrackerHit>(truthRawHitCollection);
        truthRawHitData.getData().addAll(truthHits);

        // Create a list to store the output data.
        List<TriggeredLCIOData<?>> output = new java.util.ArrayList<TriggeredLCIOData<?>>(2);
        output.add(truthRawHitData);

        // Return the result.
        return output;
    }   

}

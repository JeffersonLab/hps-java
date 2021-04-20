package org.hps.recon.ecal.cluster;

import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;


/**
 * This is a specialized Driver to use the dual threshold cosmic clustering algorithm.
 * It really only sets up the tight hit collection name for the clustering algorithm.
 * 
 * @see ClusterDriver
 * @see DualThresholdCosmicClusterer
 */
public class DualThresholdCosmicClusterDriver extends ClusterDriver {
    
    String inputTightHitCollectionName = "TightEcalCosmicReadoutHits";
    
    DualThresholdCosmicClusterer cosmicClusterer;
    
    int minClusterSize;
    int minRows;
    
    public DualThresholdCosmicClusterDriver() {        
    }
    
    public void setMinClusterSize(int minClusterSize) {
        this.minClusterSize = minClusterSize;
    }
    
    public void setMinRows(int minRows) {
        this.minRows = minRows;
    }
        
    public void setInputTightHitCollectionName(String inputTightHitCollectionName) {
        this.inputTightHitCollectionName = inputTightHitCollectionName;
    }   
    
    public void initialize() {
        clusterer.getCuts().setValue("minClusterSize", minClusterSize);
        clusterer.getCuts().setValue("minRows", minRows);
    }
    
    /**
     * Perform job initialization.  
     */
    public void startOfData() {
        if (clusterer == null) {
            // Setup the Clusterer if it wasn't already initialized by a Driver argument.
            this.setClustererName("DualThresholdCosmicClusterer");
        } else {
            // Does the Clusterer have the right type if there was a custom initialization parameter?
            if (!(clusterer instanceof DualThresholdCosmicClusterer)) {
                // The Clusterer does not appear to have the right type for this Driver!
                throw new IllegalArgumentException("The Clusterer " + this.clusterer.getClass().getCanonicalName() + " does not have the right type.");
            }   
        }
        
        // Perform standard start of data initialization from super-class.
        super.startOfData();
        
        // Set a reference to the specific type of Clusterer.
        cosmicClusterer = getClusterer();
        cosmicClusterer.setInputTightHitCollectionName(inputTightHitCollectionName);
    }
    
    public void process(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, inputTightHitCollectionName)) {
            getLogger().info("tight hit collection " + inputTightHitCollectionName + " has " + 
                    event.get(RawTrackerHit.class, inputTightHitCollectionName).size() + " hits");
        }
        super.process(event);
    }
}

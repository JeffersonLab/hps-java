package org.hps.recon.ecal.cluster;

import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.lcio.LCIOConstants;

/**
 * This is an implementation of {@link ClusterDriver} specialized for the
 * {@link ClasInnerCalClusterer}.  It currently implements optional
 * writing of a rejected hit list to the LCSim event.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ClasInnerCalClusterDriver extends ClusterDriver {
    
    // The name of the output collection of rejected hits.
    String rejectedHitCollectionName;
    
    // Flag to persist the rejected hits to the LCIO file (off by default).
    boolean writeRejectedHitCollection = false;
    
    // Reference to the concrete Clusterer object for convenience.
    ClasInnerCalClusterer innerCalClusterer;
        
    /**
     * Perform job initialization.  
     */
    public void startOfData() {
        if (clusterer == null) {
            // Setup the Clusterer if it wasn't already initialized by a Driver argument.
            this.setClusterer("ClasInnerCalClusterer");
        } else {
            // Does the Clusterer have the right type if there was a custom inialization parameter?
            if (!(clusterer instanceof ClasInnerCalClusterer)) {
                // The Clusterer does not appear to have the right type for this Driver!
                throw new IllegalArgumentException("The Clusterer type " + this.clusterer.getClass().getCanonicalName() + " does not have the right type.");
            }   
        }
        
        // Perform standard start of data initialization from super-class.
        super.startOfData();
        
        // Set a reference to the specific type of Clusterer.
        innerCalClusterer = getClusterer();
    }
    
    /**
     * Set the name of the list of rejected hits.
     * By default, the name is null and so rejected hits will not be written
     * unless this is called.
     * @param rejectedHitCollectionName The name of the rejected hit list.
     */
    public void setRejectedHitCollectionName(String rejectedHitCollectionName) {
        this.rejectedHitCollectionName = rejectedHitCollectionName;
    }
    
    /**
     * This controls whether the rejected 
     * @param writeRejectedHitCollection
     */
    public void setWriteRejectHitCollection(boolean writeRejectedHitCollection) {
        this.writeRejectedHitCollection = writeRejectedHitCollection;
    }
    
    /**
     * Perform standard event processing, optionally writing the rejected hit list.
     */
    public void process(EventHeader event) {
        // Do standard ClusterDriver processing.
        super.process(event);        
        
        // Write rejected hit list.
        writeRejectedHitList(event);        
    }
    
    /**
     * Write the list of rejected hits to the event, according to current Driver parameter settings.
     */
    void writeRejectedHitList(EventHeader event) {
        // Should rejected hit list be written?
        if (this.rejectedHitCollectionName != null) {                   
            List<CalorimeterHit> rejectedHitList = innerCalClusterer.getRejectedHitList();
            if (rejectedHitList == null) {
                throw new RuntimeException("The rejectedHitList is null.");
            }
            int flag = 1 << LCIOConstants.CLBIT_HITS;
            this.getLogger().finer("writing rejected hit list " + rejectedHitCollectionName + " with " + rejectedHitList.size() + " hits");
            event.put(rejectedHitCollectionName, rejectedHitList, CalorimeterHit.class, flag);
            // Flag the collection as a subset, because other collection's objects are being used.
            event.getMetaData(rejectedHitList).setSubset(true);
            // Are we writing this collection to the output LCIO file?
            if (!this.writeRejectedHitCollection) {
                logger.finest("Rejected hit list is transient and will not be persisted.");
                // Flag as transient.
                event.getMetaData(rejectedHitList).setTransient(true);
            }
        }
    }             
}

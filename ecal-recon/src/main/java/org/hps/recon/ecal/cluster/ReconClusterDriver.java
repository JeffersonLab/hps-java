package org.hps.recon.ecal.cluster;

import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.lcio.LCIOConstants;

/**
 * This is an implementation of {@link ClusterDriver} specialized for the
 * {@link ReconClusterer}.  It currently implements optional
 * writing of a rejected hit list to the LCSim event.
 * 
 * @see ReconClusterer
 */
public class ReconClusterDriver extends ClusterDriver {
    
    // The name of the output collection of rejected hits.
    String rejectedHitCollectionName = "ReconClusterRejectedHits";
    
    // Flag to persist the rejected hits to the LCIO file (off by default).
    boolean writeRejectedHitCollection = false;
            
    public ReconClusterDriver() {
        // Setup the Clusterer with the correct type.
        clusterer = ClustererFactory.create("ReconClusterer");
        
        // Need this for correct seed hit position in list.
        this.sortHits = true; 
    }
    
    public void setClusterer(Clusterer clusterer) {
        throw new UnsupportedOperationException("This method should not be used on this Driver.");
    }
    
    public void setClustererName(String clustererName) {
        throw new UnsupportedOperationException("This method should not be used on this Driver.");
    }
    
    public void setHitEnergyThreshold(double hitEnergyThreshold) {
        getClusterer().getCuts().setValue("hitEnergyThreshold", hitEnergyThreshold);
    }
    
    public void setSeedEnergyThreshold(double seedEnergyThreshold) {
        getClusterer().getCuts().setValue("seedEnergyThreshold", seedEnergyThreshold);
    }
    
    public void setClusterEnergyThreshold(double clusterEnergyThreshold) {
        getClusterer().getCuts().setValue("clusterEnergyThreshold", clusterEnergyThreshold);
    }
    
    public void setMinTime(double minTime) {
        getClusterer().getCuts().setValue("minTime", minTime);
    }
    
    public void setTimeWindow(double timeWindow) {
        getClusterer().getCuts().setValue("timeWindow", timeWindow);
    }
    
    public void setUseTimeCut(boolean useTimeCut) {
        ReconClusterer clusterer = getClusterer();
        clusterer.setUseTimeCut(useTimeCut);
    }    
        
    /**
     * Perform job initialization.  
     */
    public void startOfData() {
        // Perform standard start of data initialization from super-class.
        super.startOfData();
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
     * This controls whether the rejected hit collection is written out.
     * @param writeRejectedHitCollection True to write rejected hit collection.
     */
    public void setWriteRejectedHitCollection(boolean writeRejectedHitCollection) {
        this.writeRejectedHitCollection = writeRejectedHitCollection;
    }
    
    /**
     * Perform standard event processing, optionally writing the rejected hit list.
     */
    public void process(EventHeader event) {
        // Do standard ClusterDriver processing.
        super.process(event);        
        
        // Write rejected hit list.
        if (this.writeRejectedHitCollection) {
            writeRejectedHitList(event);        
        }
    }
    
    /**
     * Write the list of rejected hits to the event, according to current Driver parameter settings.
     */
    void writeRejectedHitList(EventHeader event) {
        List<CalorimeterHit> rejectedHitList = getReconClusterer().getRejectedHitList();
        if (rejectedHitList == null) {
            throw new RuntimeException("The rejectedHitList is null.");
        }
        int flag = 1 << LCIOConstants.CLBIT_HITS;
        getLogger().finer("writing rejected hit list " + rejectedHitCollectionName + " with " + rejectedHitList.size() + " hits");
        event.put(rejectedHitCollectionName, rejectedHitList, CalorimeterHit.class, flag, ecal.getReadout().getName());
        // Flag the collection as a subset, because other collection's objects are being used.
        event.getMetaData(rejectedHitList).setSubset(true);
        // Are we writing this collection to the output LCIO file?
        if (!this.writeRejectedHitCollection) {
            getLogger().finest("Rejected hit list is transient and will not be persisted.");
            // Flag as transient.
            event.getMetaData(rejectedHitList).setTransient(true);
        }   
    }             
    
    ReconClusterer getReconClusterer() {
        return (ReconClusterer) this.clusterer;
    }
}

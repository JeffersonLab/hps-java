package org.hps.readout.trigger;

import java.util.Collection;

import org.hps.readout.ReadoutDataManager;
import org.hps.readout.TriggerDriver;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
/**
 * <code>HodoscopeStudiesTriggerReadoutDriver</code> is a simple
 * trigger that is intended for use in hodoscope trigger studies. It
 * triggers by selecting events that a cluster with an x-position
 * greater than some threshold.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see org.hps.readout.TriggerDriver
 */
public class HodoscopeStudiesTriggerReadoutDriver extends TriggerDriver {
    /**
     * Specifies the name of the LCIO collection containing the input
     * hodoscope hits that are used for triggering.
     */
    private String inputCollectionName = "EcalClustersGTP";
    
    /**
     * Defines the cluster position lower-bound cut threshold.
     */
    private double xThreshold = 80.0;
    
    /**
     * Tracks the current local time in nanoseconds for this driver.
     */
    private double localTime = 0.0;
    
    @Override
    public void process(EventHeader event) {
        // Check that clusters are available for the trigger.
        Collection<Cluster> clusters = null;
        if(ReadoutDataManager.checkCollectionStatus(inputCollectionName, localTime)) {
            clusters = ReadoutDataManager.getData(localTime, localTime + 4.0, inputCollectionName, Cluster.class);
            localTime += 4.0;
        } else { return; }
        
        // There is no need to perform the trigger cuts if the
        // trigger is in dead time, as no trigger may be issued
        // regardless of the outcome.
        if(isInDeadTime()) { return; }
        
        // If there exists a cluster in the calorimeter at a position
        // greater than 80 mm, output the event.
        for(Cluster cluster : clusters) {
            if(cluster.getPosition()[0] >= xThreshold) {
                sendTrigger();
                break;
            }
        }
    }
    
    @Override
    public void startOfData() {
        // Define the driver collection dependencies.
        addDependency(inputCollectionName);
        
        // Register the trigger.
        ReadoutDataManager.registerTrigger(this);
        
        // Run the superclass method.
        super.startOfData();
    }
    
    /**
     * Sets the name of the input cluster collection.
     * @param collection - The collection name.
     */
    public void setInputCollectionName(String collection) {
        inputCollectionName  = collection;
    }
    
    /**
     * Sets the cluster position lower bound cut threshold.
     * @param xThreshold - The threshold. Clusters with an x-position
     * below this value will not trigger.
     */
    public void setXThreshold(double xThreshold) {
        this.xThreshold = xThreshold;
    }
    
    @Override
    protected double getTimeDisplacement() {
        return 0;
    }
    
    @Override
    protected double getTimeNeededForLocalOutput() {
        return 0;
    }
}
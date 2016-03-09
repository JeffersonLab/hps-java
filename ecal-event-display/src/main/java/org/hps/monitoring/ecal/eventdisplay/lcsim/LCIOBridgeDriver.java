package org.hps.monitoring.ecal.eventdisplay.lcsim;

import java.util.List;

import org.hps.monitoring.ecal.eventdisplay.event.Cluster;
import org.hps.monitoring.ecal.eventdisplay.event.EcalHit;
import org.hps.monitoring.ecal.eventdisplay.ui.PEventViewer;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * Class <code>LCIOBridgeDriver</code> displays LCIO events on the
 * event display.
 *
 * @author Kyle McCarty
 */
public class LCIOBridgeDriver extends Driver {
    // The identification name for getting the calorimeter object.
    String ecalName;
    // The collection name for the calorimeter hits.
    String ecalCollectionName;
    // The collection name in which to store clusters.
    String clusterCollectionName = "EcalClusters";
    // Sets how many events should pass before the display updates.
    int displayInterval = 0;
    // Whether the event display is currently processing an event or not.
    private boolean updating = false;
    // How many events where processed from the last displayed event.
    private int eventsProcessed = 0;
    // The event display.
    private PEventViewer eventDisplay = new PEventViewer();
    
    /**
     * <b>process</b><br/><br/>
     * <code>public void <b>process</b>(EventHeader event)</code><br/><br/>
     * Converts an LCIO event into an event display compatible format.
     * Additionally handles updating the event display, if appropriate.
     * @param event - The LCIO event.
     */
    public void process(EventHeader event) {
        // If we are still updating the display, skip this event.
        if(updating) { return; }
        
        // Make sure that this event has calorimeter hits.
        if (event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
            // Get the list of calorimeter hits from the event.
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, ecalCollectionName);
            
            // Define a list of clusters from the event.
            List<org.lcsim.event.Cluster> clusters = event.get(org.lcsim.event.Cluster.class, clusterCollectionName);
            
            // Increment the number of events we have seen.
            eventsProcessed++;
            
            // If this is the correct place to update, do so.
            if(eventsProcessed >= displayInterval) {
                // Lock the update method for the duration of the update.
                updating = true;
                
                // Clear the event display.
                eventDisplay.resetDisplay();
                
                // Add all of the hits.
                for(CalorimeterHit hit : hits) {
                    // Get the hit's location and energy.
                    int ix = hit.getIdentifierFieldValue("ix");
                    int iy = hit.getIdentifierFieldValue("iy");
                    double energy = hit.getRawEnergy();
                    
                    // Add the hit energy to the event display.
                    eventDisplay.addHit(new EcalHit(ix, iy, energy));
                }
                
                // Add all the clusters.
                for(org.lcsim.event.Cluster cluster : clusters) {
                    // Get the seed hit.
                    CalorimeterHit seed = cluster.getCalorimeterHits().get(0);
                    int ix = seed.getIdentifierFieldValue("ix");
                    int iy = seed.getIdentifierFieldValue("iy");
                    double energy = seed.getRawEnergy(); // FIXME: Should this be getCorrectedEnergy() instead? --JM
                    
                    // Add the cluster center to the event display.
                    Cluster cc = new Cluster(ix, iy, energy);
                    eventDisplay.addCluster(cc);
                }
                
                // Update the display.
                eventDisplay.updateDisplay();
                
                // Reset the number of events we've seen since the last update.
                eventsProcessed = 0;
                
                // Unlock the update method so that more events can be processed.
                updating = false;
            }
        }
    }
    
    /**
     * <b>setClusterCollectionName</b><br/><br/>
     * <code>public void <b>setClusterCollectionName</b>(String clusterCollectionName)</code><br/><br/>
     * Sets the name of the LCIO collection that contains calorimeter
     * cluster information.
     * @param clusterCollectionName - The name of the LCIO collection. 
     */
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    /**
     * <b>setDisplayInterval</b><br/><br/>
     * <code>public void <b>setDisplayInterval</b>(String displayInterval)</code><br/><br/>
     * Sets the rate at which events are displayed. The driver will
     * render a new event when <code>displayInterval</code> events
     * have occurred since the last one. Note that e value of 0 or
     * 1 will display events as quickly as they can be displayed.
     * @param displayInterval - The number of events to skip before
     * a new event is displayed.
     */
    public void setDisplayInterval(String displayInterval) {
        // Convert the argument to an integer.
        int disp = Integer.parseInt(displayInterval);
        
        // If it is negative, make it zero.
        if(disp < 0) { disp = 0; }
        
        // Set the display interval.
        this.displayInterval = disp;
    }
    
    /**
     * <b>setEcalCollectionName</b><br/><br/>
     * <code>public void <b>setEcalCollectionName</b>(String ecalCollectionName)</code><br/><br/>
     * Sets the name of the LCIO collection that contains calorimeter
     * hit information.
     * @param ecalCollectionName - The name of the LCIO collection. 
     */
    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }
    
    /**
     * <b>setEcalName</b><br/><br/>
     * <code>public void <b>setEcalName</b>(String ecalName)</code><br/><br/>
     * Sets which detector configuration should be used.
     * @param ecalName - The name of the detector configuration.
     */
    public void setEcalName(String ecalName) { this.ecalName = ecalName; }
    
    /**
     * <b>startOfData</b><br/><br/>
     * <code>public void <b>startOfData</b>()</code><br/><br/>
     * Ensures that critical collection names are defined.
     */
    public void startOfData() {
        // Make sure that there is a cluster collection name into which clusters may be placed.
        if (ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }
        
        // Make sure that there is a calorimeter detector.
        if (ecalName == null) {
            throw new RuntimeException("The parameter ecalName was not set!");
        }
        
        // Set the events passed so that the first event will display.
        eventsProcessed = displayInterval - 1;
    }
}

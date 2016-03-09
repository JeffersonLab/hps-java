package org.hps.monitoring.ecal.eventdisplay.lcsim;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * <code>EventDisplayOutputDriver</code> writes the results from clustering
 * and hit reconstruction into a text format that can be read offline by the
 * event display.
 *
 * @author Kyle McCarty
 */
public class EventDisplayOutputDriver extends Driver {
    private FileWriter writer;
    private int eventNum = 0;
    private String ecalCollectionName = "EcalCalHits";
    private String clusterCollectionName = "EcalClusters";
    private String outputFileName = "cluster-hit.txt";
    private boolean ignoreEmptyEvents = true;
    private boolean ignoreNoClusterEvents = false;
    private boolean outputClusters = true;
    
    /**
     * Closes the output file after all events have been processed.
     */
    public void endOfData() {
        // Close the file writer.
        try { writer.close(); }
        
        // Catch any IO errors.
        catch (IOException e) {
            System.err.println("Error closing output file for event display.");
        }
    }
    
    /**
     * Writes the event to the output file if it matches the the output
     * conditions selected for the driver.
     */
    public void process(EventHeader event) {
        // Get the list of clusters.
        List<org.lcsim.event.Cluster> clusters;

        // If no cluster collection is present, then make an
        // empty list instead to avoid crashes.
        try {
            clusters = event.get(org.lcsim.event.Cluster.class, clusterCollectionName);
            if (clusters == null) { throw new RuntimeException("Missing cluster collection!"); }
        }
        catch(IllegalArgumentException e) { clusters = new ArrayList<org.lcsim.event.Cluster>(0); }
        
        // Get the list of calorimeter hits.
        List<CalorimeterHit> hits;
        
        // If no hit collection is present, then make an empty
        // list instead to avoid crashes.
        try {
            hits = event.get(CalorimeterHit.class, ecalCollectionName);
            if (hits == null) { throw new RuntimeException("Missing hit collection!"); }
        }
        catch(IllegalArgumentException e) { hits = new ArrayList<CalorimeterHit>(0); }
        
        // Check the write conditions.
        boolean hasHits = hits.size() != 0;
        boolean hasClusters = clusters.size() != 0;
        
        // Check whether the write conditions match the selected settings.
        boolean writeEvent = true;
        if(ignoreNoClusterEvents && !hasClusters) { writeEvent = false; }
        else if(ignoreEmptyEvents && !hasHits) { writeEvent = false; }
        
        try {
            if(writeEvent) {
                // Increment the event number.
                eventNum++;
                
                // Write the event header.
                writer.append("Event\t" + eventNum + "\n");
                
                // Process the calorimeter hits.
                for (CalorimeterHit hit : hits) {
                    // Get the x/y coordinates for the current hit.
                    int ix = hit.getIdentifierFieldValue("ix");
                    int iy = hit.getIdentifierFieldValue("iy");
                    double energy = hit.getCorrectedEnergy();
                    double time = hit.getTime();
                    
                    // Write the hit to the output file.
                    writer.append(String.format("EcalHit\t%d\t%d\t%f\t%f%n", ix, iy, energy, time));
                }
                
                // Only write clusters if the option is selected.
                if(outputClusters) {
                    // Process the clusters.
                    for (org.lcsim.event.Cluster cluster : clusters) {
                        // Get the seed hit for the cluster.
                        CalorimeterHit seedHit = (CalorimeterHit)cluster.getCalorimeterHits().get(0);
                        int ix = seedHit.getIdentifierFieldValue("ix");
                        int iy = seedHit.getIdentifierFieldValue("iy");
                        double time = seedHit.getTime();
                        
                        // Get the cluster's total energy.
                        double energy = cluster.getEnergy();
                        
                        // Write the seed hit to start a cluster.
                        writer.append(String.format("Cluster\t%d\t%d\t%f\t%f%n", ix, iy, energy, time));
                        
                        // Write the component hits to the cluster.
                        for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
                            // Get each component hit's x/y coordinates.
                            ix = hit.getIdentifierFieldValue("ix");
                            iy = hit.getIdentifierFieldValue("iy");
                            
                            // Write them as component hits.
                            writer.append(String.format("CompHit\t%d\t%d%n", ix, iy));
                        }
                    }
                }
                
                // Append the end of event indicator.
                writer.append("EndEvent\n");
            }
        } catch(IOException e) {
            System.err.println("Error writing to output for event display.");
        }
    }
    
    /**
     * Opens the output file and clears it if it already exists.
     */
    public void startOfData() {
        try {
            // Initialize the writer.
            writer = new FileWriter(outputFileName);
            
            // Clear the file.
            writer.write("");
        } catch(IOException e) {
            System.err.println("Error initializing output file for event display.");
        }
    }
    
    /**
     * Sets the name of the LCIO collection containing hits.
     * @param ecalCollectionName - The LCIO hit collection name.
     */
    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }
    
    /**
     * Sets the name of the LCIO collection containing clusters.
     * @param clusterCollectionName - The LCIO cluster collection name.
     */
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    /**
     * Sets whether events with no hits should be output.
     * @param ignoreEmptyEvents - <code>true</code> indicates that
     * events without hits will be skipped while <code>false</code>
     * indicates that they will be output.
     */
    public void setIgnoreEmptyEvents(boolean ignoreEmptyEvents) {
        this.ignoreEmptyEvents = ignoreEmptyEvents;
    }
    
    /**
     * Sets whether events with no clusters should be output.
     * @param ignoreNoClusterEvents - <code>true</code> indicates that
     * events without clusters will be skipped while <code>false</code>
     * indicates that they will be output.
     */
    public void setIgnoreNoClusterEvents(boolean ignoreNoClusterEvents) {
        this.ignoreNoClusterEvents = ignoreNoClusterEvents;
    }
    
    /**
     * Sets the name of the output file containing the event data.
     * @param outputFileName - The name of the output file.
     */
    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }
    
    /**
     * Sets whether clusters should be output to data file,
     * @param outputClusters - <code>true</code> indicates that clusters
     * will be written and <code>false</code> that they will not.
     */
    public void setOutputClusters(boolean outputClusters) {
        this.outputClusters = outputClusters;
    }
}

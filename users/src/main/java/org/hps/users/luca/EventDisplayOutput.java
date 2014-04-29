package org.hps.users.luca;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;


/**
 * <code>EventDisplayOutputDriver</code> writes the results from clustering
 * and hit reconstruction into a text format that can be read offline by the
 * event display.
 *
 * @author Kyle McCarty
 */
public class EventDisplayOutput extends Driver {
    private FileWriter writer;
    private FileWriter hitsWriter;
    private int eventNum = 0;
    String ecalCollectionName = "EcalCalHits";
    String clusterCollectionName = "EcalClusters";
    String outputFileName = "cluster-hit.txt";
    
    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }
    
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }
    
    public void startOfData() {
        try {
            // Initialize the writer.
            writer = new FileWriter(outputFileName);
            hitsWriter = new FileWriter("raw-hit.txt");
            
            // Clear the file.
            writer.write("");
        } catch(IOException e) {
            System.err.println("Error initializing output file for event display.");
        }
    }
    
    public void endOfData() {
        try {
            // Close the file writer.
            hitsWriter.close();
            writer.close();
        } catch (IOException e) {
            System.err.println("Error closing output file for event display.");
        }
    }
    
    public void process(EventHeader event) {
        // Get the list of clusters.
        List<Cluster> clusters = event.get(Cluster.class, clusterCollectionName);//
        if (clusters == null) {
            throw new RuntimeException("Missing cluster collection!");
        }
        
        // Get the list of calorimeter hits.
        List<CalorimeterHit> hits = event.get(CalorimeterHit.class, ecalCollectionName);
        if (hits == null) {
            throw new RuntimeException("Missing hit collection!");
        }
        
        try {
            if(clusters.size() != 0) {
                // Increment the event number.
                eventNum++;
                
                // Write the event header.
                writer.append("Event\t" + eventNum + "\n");
                hitsWriter.append("Event\t" + eventNum + "\n");
                
                // Process the calorimeter hits.
                for (CalorimeterHit hit : hits) {
                    // Get the x/y coordinates for the current hit.
                    //int ix = hit.getIdentifierFieldValue("ix");
                    //int iy = hit.getIdentifierFieldValue("iy");
                    //double energy = hit.getCorrectedEnergy();
                    
                    // Write the hit to the output file.
                    //writer.append(String.format("EcalHit\t%d\t%d\t%f%n", ix, iy, energy));
                    //hitsWriter.append(String.format("EcalHit\t%d\t%d\t%f%n", ix, iy, energy));
                }
                
                // Process the clusters.
                for (Cluster cluster : clusters) {//
                    // Get the seed hit for the cluster.
                    double seedHit = cluster.getEnergy();

        
                    // Write the seed hit to start a cluster.
                    writer.append("\n"+seedHit);
                    
                    // Write the component hits to the cluster.
                    for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
                        // Get each component hit's x/y coordinates.
                        //ix = hit.getIdentifierFieldValue("ix");
                       // iy = hit.getIdentifierFieldValue("iy");
                        
                        // Write them as component hits.
                        //writer.append(String.format("CompHit\t%d\t%d%n", ix, iy));
                    }
                }

                //writer.append("EndEvent\n");
                //hitsWriter.append("EndEvent\n");
            }
        } catch(IOException e) {
            System.err.println("Error writing to output for event display.");
        }
    }
}

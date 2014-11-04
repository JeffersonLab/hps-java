package org.hps.monitoring.ecal.eventdisplay.io;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hps.monitoring.ecal.eventdisplay.event.Cluster;
import org.hps.monitoring.ecal.eventdisplay.event.EcalHit;
import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.lcio.LCIOReader;

/**
 * Class <code>LCIOManager</code> is an implementation of <code>
 * EventManager</code> for (S)LCIO files.<br/>
 * <br/>
 * <b><span style="color:red">Warning: This class is under construction
 * and should not be used at this time!</span></b>
 * 
 * @author Kyle McCarty
 */
public class LCIOManager implements EventManager {
    // Internal variables.
    private LCIOReader reader;
    private EventHeader current;
    private int eventsRead = 0;
    private final File sourceFile;
    
    // LCIO collection names.
    private String clusterCollectionName = "EcalClusters";
    private String hitCollectionName = "EcalHits";
    
    public LCIOManager(String inputFilepath) throws IOException {
        this(new File(inputFilepath));
    }
    
    public LCIOManager(File input) throws IOException {
        // Create an LCIO reader from the file.
        reader = new LCIOReader(input);
        
        // Store the source file.
        sourceFile = input;
        
        // Read the first event.
        nextEvent();
    }
    
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    public void setHitCollectionName(String hitCollectionName) {
        this.hitCollectionName = hitCollectionName;
    }
    
    @Override
    public void close() throws IOException {
        reader.close();
    }
    
    @Override
    public int getEventNumber() {
        // If there is a currently defined event, get it's event number.
        if(current != null) { return current.getEventNumber(); }
        
        // Otherwise, return -1.
        else { return -1; }
    }
    
    @Override
    public List<Cluster> getClusters() {
        // If the current event is undefined, return an empty list.
        if(current == null) { return new ArrayList<Cluster>(); }
        
        // Otherwise, try to obtain and convert the cluster collection
        // from the LCIO event.
        else {
            // Check to see if the event has a cluster collection.
            if(current.hasCollection(HPSEcalCluster.class, clusterCollectionName)) {
                // Get the list of LCIO clusters.
                List<HPSEcalCluster> lcioList = current.get(HPSEcalCluster.class, clusterCollectionName);
                
                // Create a list to store event display clusters.
                List<Cluster> displayList = new ArrayList<Cluster>(lcioList.size());
                
                // Convert the LCIO clusters to display clusters.
                for(HPSEcalCluster lcioCluster : lcioList) {
                    displayList.add(toPanelCluster(lcioCluster));
                }
                
                // Return the converted list of clusters.
                return displayList;
            }
            
            // If it does not, return an empty list.
            else { return new ArrayList<Cluster>(); }
        }
    }
    
    @Override
    public List<EcalHit> getHits() {
        System.out.println("Event is null: " + (current == null));
        
        // If the current event is undefined, return an empty list.
        if(current == null) { return new ArrayList<EcalHit>(); }
        
        // Otherwise, try to obtain and convert the hit collection from
        // the LCIO event.
        else {
            System.out.println("Check for hits...");
            // Check to see if the event has a hit collection.
            if(current.hasCollection(CalorimeterHit.class, hitCollectionName)) {
                System.out.println("Has hits!");
                // Get the list of LCIO hits.
                List<CalorimeterHit> lcioList = current.get(CalorimeterHit.class, hitCollectionName);
                
                // Create a list to store event display hits.
                List<EcalHit> displayList = new ArrayList<EcalHit>(lcioList.size());
                
                // Convert the LCIO clusters to display clusters.
                for(CalorimeterHit lcioHit : lcioList) {
                    displayList.add(toPanelHit(lcioHit));
                }
                
                // Return the converted list of clusters.
                return displayList;
            }
            
            // If it does not, return an empty list.
            else { return new ArrayList<EcalHit>(); }
        }
    }
    
    @Override
    public boolean nextEvent() throws IOException {
        // Try to read the next event.
        try { current = reader.read(); }
        
        // If the read action fails, then there is no next event.
        catch(IOException e) {
            current = null;
            return false;
        }
        
        // Note that another event has been read.
        eventsRead++;
        
        // Otherwise, indicate that an event was read.
        return true;
    }
    
    @Override
    public boolean previousEvent() throws IOException {
        // If we are on the first event, there is no previous event.
        if(eventsRead == 0 || eventsRead == 1) { return false; }
        
        // Otherwise, reset the reader and skip to the previous event.
        else {
            // Create a new reader.
            reader = new LCIOReader(sourceFile);
            
            // Skip to immediately before the previous event.
            reader.skipEvents(eventsRead - 2);
            
            // Read the next event.
            try { current = reader.read(); }
            
            // If the read fails, return false.
            catch(IOException e) {
                current = null;
                return false;
            }
            
            // Decrement the number of events read.
            eventsRead--;
            
            // Otherwise, indicate that an event was read.
            return true;
        }
    }
    
    public static final Cluster toPanelCluster(HPSEcalCluster lcioCluster) {
        // If the argument is null, return null.
        if(lcioCluster == null) { return null; }
        
        // Otherwise, get the cluster x/y indices and energy.
        int ix = lcioCluster.getSeedHit().getIdentifierFieldValue("ix");
        int iy = lcioCluster.getSeedHit().getIdentifierFieldValue("iy");
        double energy = lcioCluster.getEnergy();
        
        // Create and return a panel cluster from the above values.
        return new Cluster(ix, iy, energy);
    }
    
    public static final EcalHit toPanelHit(CalorimeterHit lcioHit) {
        // If the argument is null, return null.
        if(lcioHit == null) { return null; }
        
        // Otherwise, get the cluster x/y indices and energy.
        int ix = lcioHit.getIdentifierFieldValue("ix");
        int iy = lcioHit.getIdentifierFieldValue("iy");
        double energy = lcioHit.getCorrectedEnergy();
        
        // Create and return a panel hit from the above values.
        return new EcalHit(ix, iy, energy);
    }
}
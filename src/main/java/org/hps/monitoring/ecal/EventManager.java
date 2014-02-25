package org.hps.monitoring.ecal;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * The class <code>EventManager</code> handles loading hits and clusters from a
 * text file to populate the calorimeter panel. Input should be of the form
 * Event
 * Indicates the start of a new event.
 * 
 * EcalHit [X] [Y] [Energy]
 * Represents a calorimeter hit at coordinates ([X], [Y]) and with energy
 * [Energy]. Coordinates should be in calorimeter form (x = [-23, 23] and
 * y = [-5, 5]) and must integers. Energy can be a decimal value. Brackets
 * should not be included in the line.
 * 
 * Cluster [X] [Y]
 * Represents the location of a cluster at coordinates ([X], [Y]). Brackets
 * should not be included in the line.
 * 
 * @author Kyle McCarty
 **/
public class EventManager {
    // File readers for reading the input.
    private FileReader fr;
    private BufferedReader reader;
    // List for storing the hits from the current event.
    private ArrayList<EcalHit> hitList = new ArrayList<EcalHit>();
    // List for storing the clusters from the current hit.
    private ArrayList<Cluster> clusterList = new ArrayList<Cluster>();
    // Whether the event manager has an open file.
    private boolean open = true;
    
    /**
     * <b>EventManager</b><br/><br/>
     * <code>public <b>EventManager</b>(String filename)</code><br/><br/>
     * Initializes an event manager that will read from the indicated file.
     * @param filename - The path to the file containing hit information.
     **/
    public EventManager(String filename) throws IOException {
        fr = new FileReader(filename);
        reader = new BufferedReader(fr);
    }
    
    /**
     * <b>readEvent</b><br/><br/>
     * <code>public boolean <b>readEvent</b>()</code><br/><br/>
     * Populates the event manager with hits and clusters from the next event.
     * @return Returns <code>true</code> if an event was read and <code>false
     * </code> if it was not.
     **/
    public boolean readEvent() throws IOException {
        // We can only read of the reader is open.
        if (!open) { return false; }
        
        // Clear the data lists.
        hitList.clear();
        clusterList.clear();
        
        // Store the current line.
        String curLine = reader.readLine();
        
        // Keep sorting until we hit a null or an event header.
        while (curLine != null && curLine.compareTo("Event") != 0) {
            curLine = reader.readLine();
        }
        
        // If we hit a null, we are at the end of the file.
        if (curLine == null) { return false; }
        
        // Otherwise, we have read an event header and must populate
        // the data lists.
        curLine = reader.readLine();
        while (curLine != null && curLine.compareTo("Event") != 0) {
            // Break apart the line.
            StringTokenizer st = new StringTokenizer(curLine);
            String name = st.nextToken();
            int ix = Integer.parseInt(st.nextToken());
            int iy = Integer.parseInt(st.nextToken());
            
            // If this is a cluster, add a new cluster object.
            if (name.compareTo("Cluster") == 0) { clusterList.add(new Cluster(ix, iy)); }
            
            // If this is a calorimeter hit, add a new calorimeter hit object.
            else if (name.compareTo("EcalHit") == 0) {
                double energy = Double.parseDouble(st.nextToken());
                hitList.add(new EcalHit(ix, iy, energy));
            }
            
            // If this is a cluster component hit, add it to the last cluster.
            else if(name.compareTo("CompHit") == 0) {
            	// There must be a last cluster to process this hit type.
            	if(clusterList.size() == 0) {
            		System.err.println("File Format Error: A cluster component hit was read, but" +
            				" no cluster has been declared. Terminating.");
            		System.exit(1);
            	}
            	else { clusterList.get(clusterList.size() - 1).addComponentHit(ix, iy); }
            }
            
            // If this is a cluster shared hit, add it to the last cluster.
            else if(name.compareTo("SharHit") == 0) {
            	// There must be a last cluster to process this hit type.
            	if(clusterList.size() == 0) {
            		System.err.println("File Format Error: A cluster shared hit was read, but" +
            				" no cluster has been declared. Terminating.");
            		System.exit(1);
            	}
            	else { clusterList.get(clusterList.size() - 1).addSharedHit(ix, iy); }
            }
            
            // Get the next line.
            curLine = reader.readLine();
        }
        
        // Indicate that an event was processed.
        return true;
    }
    
    /**
     * <b>close</b><br/><br/>
     * <code>public void <b>close</b>()</code><br/><br/>
     * Closes the event manager. Once this is performed, no additional events
     * may be read.
     * @throws IOException Occurs if there is an error closing the file stream.
     **/
    public void close() throws IOException {
        reader.close();
        fr.close();
        open = false;
    }
    
    /**
     * <b>getHits</b><br/><br/>
     * <code>public ArrayList<EcalHit> <b>getHits</b>()</code><br/><br/>
     * Allows access to the current event's list of hits.
     * @return Returns the current hits as an <code>ArrayList</code> object.
     **/
    public ArrayList<EcalHit> getHits() {
        if (!open) { return null; }
        else { return hitList; }
    }
    
    /**
     * <b>getClusters</b><br/><br/>
     * <code>public ArrayList<Cluster> <b>getClusters</b></code><br/><br/>
     * Allows access to the current event's list of clusters.
     * @return Returns the current clusters as an <code>ArrayList
     * </code> object.
     **/
    public ArrayList<Cluster> getClusters() {
        if (!open) { return null; }
        else { return clusterList; }
    }
}

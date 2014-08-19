package org.hps.monitoring.ecal.eventdisplay.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.hps.monitoring.ecal.eventdisplay.event.Cluster;
import org.hps.monitoring.ecal.eventdisplay.event.EcalHit;

/**
 * The class <code>TextManager</code> handles loading hits and clusters from a
 * text file to populate the calorimeter panel. Input should be of the form
 * Event [Number]
 * Indicates the start of a new event.
 * 
 * EcalHit [X] [Y] [Energy]
 * Represents a calorimeter hit at coordinates ([X], [Y]) and with energy
 * [Energy]. Coordinates should be in calorimeter form (x = [-23, 23] and
 * y = [-5, 5]) and must integers. Energy can be a decimal value. Brackets
 * should not be included in the line.
 * 
 * Cluster [X] [Y] [Energy]
 * Represents the location of a cluster at coordinates ([X], [Y]) with
 * [Energy] total cluster energy. Note that the [Energy] field is not
 * required. Brackets should not be included in the line.
 * 
 * CompHit [X] [Y]
 * Represents a component hit of the previously declared cluster and located
 * at coordinates ([X], [Y]). Brackets should not be included in the line.
 * 
 * SharHit [X] [Y]
 * Represents a hit that is shared between two or more clusters which is
 * located at coordinates ([X], [Y]). Brackets should not be included in
 * the line.
 * 
 * EndEvent
 * Indicates that the event has ended.
 * 
 * @author Kyle McCarty
 **/
public final class TextManager implements EventManager {
    // File reader for reading the input.
	private AdvancedReader reader;
    // List for storing the hits from the current event.
    private ArrayList<EcalHit> hitList = new ArrayList<EcalHit>();
    // List for storing the clusters from the current hit.
    private ArrayList<Cluster> clusterList = new ArrayList<Cluster>();
    // Whether the event manager has an open file.
    private boolean open = true;
    // Track the current event number.
    private int curEvent = 0;
    
    /**
     * <b>EventManager</b><br/><br/>
     * <code>public <b>EventManager</b>(String filename)</code><br/><br/>
     * Initializes an event manager that will read from the indicated file.
     * @param filename - The path to the file containing hit information.
     **/
    public TextManager(String filename) throws IOException {
    	reader = new AdvancedReader(filename);
    }
    
    public void close() throws IOException {
        reader.close();
        open = false;
    }
    
    public int getEventNumber() {
    	return curEvent;
    }
    
    public ArrayList<Cluster> getClusters() {
        if (!open) { return null; }
        else { return clusterList; }
    }
    
    public ArrayList<EcalHit> getHits() {
        if (!open) { return null; }
        else { return hitList; }
    }
    
    public boolean nextEvent() throws IOException {
        // We can only read of the reader is open.
        if (!open) { return false; }
        
        // Store the current line.
        String curLine = reader.readNextLine();
        
        // Keep sorting until we hit a null or an event header.
        while (curLine != null && !curLine.contains("Event")) {
            curLine = reader.readNextLine();
        }
        
        // If we hit a null, we are at the end of the file.
        if (curLine == null) { return false; }
        
        // Clear the data lists.
        hitList = new ArrayList<EcalHit>();
        clusterList = new ArrayList<Cluster>();
        
        // Get the current event.
        StringTokenizer et = new StringTokenizer(curLine);
        et.nextToken();
        curEvent = Integer.parseInt(et.nextToken());
        
        // Otherwise, we have read an event header and must populate
        // the data lists.
        curLine = reader.readNextLine();
        while (curLine != null && curLine.compareTo("EndEvent") != 0) {
            // Break apart the line.
            StringTokenizer st = new StringTokenizer(curLine);
            String name = st.nextToken();
            int ix = Integer.parseInt(st.nextToken());
            int iy = Integer.parseInt(st.nextToken());
            
            // If this is a cluster, add a new cluster object.
            if (name.compareTo("Cluster") == 0) {
            	// Get the cluster energy, if it is given.
            	double clusterEnergy = Double.NaN;
            	if(st.hasMoreTokens()) { clusterEnergy = Double.parseDouble(st.nextToken()); }
            	
            	// Add a new cluster.
            	clusterList.add(new Cluster(ix, iy, clusterEnergy));
            }
            
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
            curLine = reader.readNextLine();
        }
        
        // Indicate that an event was processed.
        return true;
    }
    
    public boolean previousEvent() throws IOException {
    	// If we are at the first event, do nothing. There is no
    	// previous event to display.
    	if(curEvent == 1) { return false; }
    	
    	// Otherwise, loop backward until we find the previous event header.
    	String curLine;
    	while(true) {
    		// Get the previous line.
    		curLine = reader.readPreviousLine();
    		
    		// Otherwise, if it is null, we've reached the start of the file.
    		if(curLine == null) {
    			System.err.println("Error: Unexpectedly reached SOF.");
    			System.exit(1);
    		}
    		
    		// If the previous line is an event, note it.
    		if(curLine.substring(0, 5).compareTo("Event") == 0) {
    	        // Get the event number of the current event.
    	        StringTokenizer et = new StringTokenizer(curLine);
    	        et.nextToken();
    	        int readEvent = Integer.parseInt(et.nextToken());
    	        
    	        // If the read event number is one back from the current
    	        // event, jump back a step and read the event.
    	        if(readEvent == (curEvent - 1)) {
    	        	reader.readPreviousLine();
    	        	return nextEvent();
    	        }
    		}
    	}
    }
}

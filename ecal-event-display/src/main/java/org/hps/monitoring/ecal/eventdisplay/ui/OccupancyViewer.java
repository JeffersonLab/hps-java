package org.hps.monitoring.ecal.eventdisplay.ui;

import java.awt.Dimension;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hps.monitoring.ecal.eventdisplay.event.EcalHit;
import org.hps.monitoring.ecal.eventdisplay.io.EventManager;

/**
 * Class <code>OccupancyViewer</code> is an active implementation of
 * the <code>Viewer</code> class that displays occupancies on each
 * crystal.
 * 
 * @author Kyle McCarty
 */
public class OccupancyViewer extends ActiveViewer {
	private static final long serialVersionUID = 3712604287904215617L;
	// The number of events that have been read so far.
	private long events = 0;
	// The total number of hits for each crystal position.
	private long[][] hits;
	
	/**
	 * <b>OccupancyViewer</b><br/><br/>
     * <code>public <b>OccupancyViewer</b>(EventManager em)</code><br/><br/>
     * Creates a new occupancy display that draws event data from the
     * indicated data source.
	 * @param em - The data source from which to draw events.
	 */
	public OccupancyViewer(EventManager em) {
		// Initialize the super class.
		super(em);
		
		// Set the title and scale.
		setTitle("HPS Calorimeter Occupancies");
		ecalPanel.setScaleMaximum(1.0);
		
		// Initialize the hit counts array.
		Dimension ecalSize = ecalPanel.getCrystalBounds();
		hits = new long[ecalSize.width][ecalSize.height];
	}
    
    public void displayNextEvent() throws IOException { getEvent(true); }
    
    public void displayPreviousEvent() throws IOException { getEvent(false); }
    
    /**
     * <b>resetOccupancies</b><br/><br/>
     * <code>public void <b>resetOccupancies</b>()</code><br/><br/>
     * Clears the current occupancy data.
     */
    public void resetOccupancies() {
    	// Clear the crystal hit counts.
    	for(int x = 0; x < hits.length; x++) {
    		for(int y = 0; y < hits[0].length; y++) {
    			hits[x][y] = 0;
    		}
    	}
    	
    	// Clear the number of events.
    	events = 0;
    }
    
	/**
	 * <b>displayEvent</b><br/><br/>
	 * <code>private void <b>displayEvent</b>(List<EcalHit> hitList)</code><br/><br/>
	 * Displays the given lists of hits on the calorimeter panel.
	 * @param hitList - A list of hits for the current event.
	 */
	private void displayEvent(List<EcalHit> hitList) {
		// Suppress the calorimeter panel's redrawing.
		ecalPanel.setSuppressRedraw(true);
		
        // Display the hits.
        for (EcalHit h : hitList) {
            ecalPanel.addCrystalEnergy(h.getX(), h.getY(), h.getEnergy());
        }
        
        // Stop suppressing the redraw and order the panel to update.
        ecalPanel.setSuppressRedraw(false);
        ecalPanel.repaint();
        
        // Update the status panel to account for the new event.
        updateStatusPanel();
	}
    
    /**
     * <b>getEvent</b><br/><br/>
     * <code>private void <b>getEvent</b>(boolean forward)</code><br/><br/>
     * Reads either the next or the previous event from the event manager.
     * @param forward - Whether the event data should be read forward
     * or backward.
     * @throws IOException Occurs when there is an issue with reading the data file.
     */
    private void getEvent(boolean forward) throws IOException {
        // Clear the calorimeter panel.
        ecalPanel.clearCrystals();
        
        // If there is no data source, we can not do anything.
        if (em == null) { return; }
        
        // Get the next event.
        if(forward) {
        	// Get the next event.
        	em.nextEvent();
        	
        	// Increment the event count.
        	events++;
        	
        	// For each hit, increment the hit count for the relevant
        	// crystal by one.
        	for(EcalHit hit : em.getHits()) {
        		hits[toPanelX(hit.getX())][toPanelY(hit.getY())]++;
        	}
        }
        else {
        	// Get the previous event.
        	em.previousEvent();
        	
        	// Decrement the event count.
        	events--;
        	
        	// For each hit, decrement the hit count for the relevant
        	// crystal by one.
        	for(EcalHit hit : em.getHits()) {
        		hits[toPanelX(hit.getX())][toPanelY(hit.getY())]--;
        	}
        }
        
        // Build a "hit list" from the occupancies.
        ArrayList<EcalHit> occupancyList = new ArrayList<EcalHit>();
        for(int x = 0; x < hits.length; x++) {
        	for(int y = 0; y < hits[0].length; y++) {
        		if(hits[x][y] != 0) {
        			// Define the crystal ID and "energy."
        			Point cid = new Point(x, y);
        			double occupancy = ((double) hits[x][y]) / events;
        			EcalHit occupancyHit = new EcalHit(cid, occupancy);
        			occupancyList.add(occupancyHit);
        		}
        	}
        }
        
        // Display it the occupancies.
        displayEvent(occupancyList);
    }
}

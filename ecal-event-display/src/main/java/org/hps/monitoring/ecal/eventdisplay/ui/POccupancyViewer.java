package org.hps.monitoring.ecal.eventdisplay.ui;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;

import org.hps.monitoring.ecal.eventdisplay.event.Cluster;
import org.hps.monitoring.ecal.eventdisplay.event.EcalHit;
import org.lcsim.event.CalorimeterHit;

/**
 * The class <code>POccupancyViewer</code> is an extension of the <code>
 * PassiveViewer</code> class for displaying occupancies from a stream.
 * Like all passive viewers, it is designed to receive instructions on
 * when to update its display or read data from the stream.
 * 
 * @author Kyle McCarty
 */
public class POccupancyViewer extends PassiveViewer {
    private static final long serialVersionUID = 3712604287904215617L;
    // Store the number of hits for each crystal.
    private long[][] hits;
    // Store the total number of events read.
    private long events = 0;
    // Stores hit objects.
    protected ArrayList<EcalHit> hitList = new ArrayList<EcalHit>();
    
    /**
     * Initializes a <code>Viewer</code> window that displays will
     * occupancies from a data stream.
     */
    public POccupancyViewer() {
        // Set the title and scale.
        setTitle("HPS Calorimeter Occupancies");
        ecalPanel.setScaleMaximum(1.0);
        
        // Initialize the hit counts array.
        Dimension ecalSize = ecalPanel.getCrystalBounds();
        hits = new long[ecalSize.width][ecalSize.height];
    }
    
    @Override
    public void addHit(CalorimeterHit lcioHit) {
        // Get the panel coordinates from the hit.
        int ix = toPanelX(lcioHit.getIdentifierFieldValue("ix"));
        int iy = toPanelX(lcioHit.getIdentifierFieldValue("iy"));
        
        // Increment the hit count at the indicated location.
        hits[ix][iy]++;
    }
    
    @Override
    public void addHit(EcalHit hit) {
        // Get the panel coordinates of the hit.
        int ix = toPanelX(hit.getX());
        int iy = toPanelY(hit.getY());
        
        // Increment the hit count at the indicated location.
        hits[ix][iy]++;
    }
    
    /**
     * Adds a new cluster to the display.<br/><br/>
     * <b>Note:</b> This operation is not supported for occupancies.
     */
    @Override
    public void addCluster(Cluster cluster) { }
    
    /**
     * Adds a new cluster to the display.<br/><br/>
     * <b>Note:</b> This operation is not supported for occupancies.
     */
    @Override
    public void addCluster(org.lcsim.event.Cluster cluster) { }
    
    /**
     * Removes a hit from the display.
     * @param hit - The hit to be removed.
     */
    public void removeHit(EcalHit hit) {
        // Get the panel coordinates of the hit.
        int ix = toPanelX(hit.getX());
        int iy = toPanelY(hit.getY());
        
        // Decrement the hit count at the indicated location.
        hits[ix][iy]--;
    }
    
    @Override
    public void resetDisplay() { hitList.clear(); }
    
    /**
     * Increments the number of events represented by the current data
     * set by the indicated amount. Note that this may be negative to
     * reduce the number of events.
     * @param amount - The number of events to add.
     */
    public void incrementEventCount(int amount) { events += amount; }
    
    /**
     * Displays the hits and clusters added by the <code>addHit</code>
     * and <code>addCluster</code> methods.
     */
    @Override
    public void updateDisplay() { 
        // Build a "hit list" from the occupancies.
        for(int x = 0; x < hits.length; x++) {
            for(int y = 0; y < hits[0].length; y++) {
                // Don't bother performing calculations or building
                // any objects if there are zero hits.
                if(hits[x][y] != 0) {
                    // Define the crystal ID and "energy."
                    Point cid = new Point(x, y);
                    double occupancy = ((double) hits[x][y]) / events;
                    
                    // Add a "hit" formed from these values.
                    hitList.add(new EcalHit(cid, occupancy));
                }
            }
        }
        
        // Suppress the calorimeter panel's redrawing.
        ecalPanel.setSuppressRedraw(true);
        
        // Display the hits.
        for (EcalHit h : hitList) {
            int ix = toPanelX(h.getX());
            int iy = toPanelY(h.getY());
            ecalPanel.addCrystalEnergy(ix, iy, h.getEnergy());
        }
        
        // Stop suppressing the redraw and order the panel to update.
        ecalPanel.setSuppressRedraw(false);
        ecalPanel.repaint();
        
        // Update the status panel to account for the new event.
        updateStatusPanel();
    }
}
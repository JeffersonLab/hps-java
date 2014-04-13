package org.hps.monitoring.ecal.ui;

import org.hps.monitoring.ecal.io.EventManager;

import java.awt.Point;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;

import org.hps.monitoring.ecal.event.Association;
import org.hps.monitoring.ecal.event.Cluster;
import org.hps.monitoring.ecal.event.EcalHit;

/**
 * Class <code>FileViewer</code> is an implementation of the <code>
 * Viewer</code> abstract class that reads events from a file data
 * source. Any file type can be used, so long as it has a manager
 * which implements the <code>EventManager</code> interface.
 * 
 * @author Kyle McCarty
 */
public class FileViewer extends ActiveViewer {
	private static final long serialVersionUID = 17058336873349781L;
    // Map cluster location to a cluster object.
    private HashMap<Point, Cluster> clusterMap = new HashMap<Point, Cluster>();
    // Additional status display field names for this data type.
    private static final String[] fieldNames = { "Event Number", "Shared Hits", "Component Hits", "Cluster Energy" };
    // Indices for the field values.
    private static final int EVENT_NUMBER = 0;
    private static final int SHARED_HITS = 1;
    private static final int COMPONENT_HITS = 2;
    private static final int CLUSTER_ENERGY = 3;
    
	/**
	 * <b>FileViewer</b><br/><br/>
     * <code>public <b>FileViewer</b>()</code><br/><br/>
     * Constructs a new <code>Viewer</code> for displaying data read
     * from a file.
	 * @param dataSource - The <code>EventManager</code> responsible
	 * for reading data from a file.
	 * @throws NullPointerException Occurs if the event manager is
	 * <code>null</code>.
	 */
	public FileViewer(EventManager dataSource) throws NullPointerException {
		// Pass any additional fields required by the event manager
		// to the underlying Viewer object to be added to the status
		// display panel.
		super(dataSource, fieldNames);
	}
    
    public void displayNextEvent() throws IOException { getEvent(true); }
    
    public void displayPreviousEvent() throws IOException { getEvent(false); }
    
    protected void updateStatusPanel() {
    	// Update the superclass status fields.
    	super.updateStatusPanel();
    	
		// Get the currently selected crystal.
		Point crystal = ecalPanel.getSelectedCrystal();
    	
		// If the active crystal is not null, see if it is a cluster.
		if(crystal != null) {
			// Get the cluster associated with this point.
			Cluster activeCluster = clusterMap.get(crystal);
			
			// If the cluster is null, we set everything to undefined.
			if(activeCluster == null) {
				for(String field : fieldNames) { setStatusField(field, StatusPanel.NULL_VALUE); }
			}
			
			// Otherwise, define the fields based on the cluster.
			else {
				// Get the shared and component hit counts.
				setStatusField(fieldNames[SHARED_HITS], Integer.toString(activeCluster.getSharedHitCount()));
				setStatusField(fieldNames[COMPONENT_HITS], Integer.toString(activeCluster.getComponentHitCount()));
				
				// Format the cluster energy, or account for it if it
				// doesn't exist.
				String energy;
				if(activeCluster.getClusterEnergy() != Double.NaN) {
					DecimalFormat formatter = new DecimalFormat("0.####E0");
					energy = formatter.format(activeCluster.getClusterEnergy());
				}
				else { energy = "---"; }
				setStatusField(fieldNames[CLUSTER_ENERGY], energy);
			}
		}
		// Otherwise, clear the field values.
		else { for(String field : fieldNames) { setStatusField(field, StatusPanel.NULL_VALUE); } }
    	
    	// Set the event number.
    	setStatusField(fieldNames[EVENT_NUMBER], Integer.toString(em.getEventNumber()));
    }
    
	/**
	 * <b>displayEvent</b><br/><br/>
	 * <code>private void <b>displayEvent</b>(List<EcalHit> hitList, List<Cluster> clusterList)</code><br/><br/>
	 * Displays the given lists of hits and clusters on the calorimeter
	 * panel.
	 * @param hitList - A list of hits for the current event.
	 * @param clusterList  - A list of clusters for the current event.
	 */
	private void displayEvent(List<EcalHit> hitList, List<Cluster> clusterList) {
		// Suppress the calorimeter panel's redrawing.
		ecalPanel.setSuppressRedraw(true);
		
        // Display the hits.
        for (EcalHit h : hitList) {
            int ix = toPanelX(h.getX());
            int iy = toPanelY(h.getY());
            ecalPanel.addCrystalEnergy(ix, iy, h.getEnergy());
        }
        
        // Display the clusters.
        for(Cluster cluster : clusterList) {
        	Point rawCluster = cluster.getClusterCenter();
        	Point clusterCenter = toPanelPoint(rawCluster);
            ecalPanel.setCrystalCluster(clusterCenter.x, clusterCenter.y, true);
            
        	// Add component hits to the calorimeter panel.
        	for(Point ch : cluster.getComponentHits()) {
        		ecalPanel.addAssociation(new Association(clusterCenter, toPanelPoint(ch), HIGHLIGHT_CLUSTER_COMPONENT));
        	}
        	
        	// Add shared hits to the calorimeter panel.
        	for(Point sh : cluster.getSharedHits()) {
        		ecalPanel.addAssociation(new Association(clusterCenter, toPanelPoint(sh), HIGHLIGHT_CLUSTER_SHARED));
        	}
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
        
        // Otherwise, get the next event.
        if(forward) { em.nextEvent(); }
        else { em.previousEvent(); }
        
        // Load the cluster map.
        clusterMap.clear();
        for(Cluster c : em.getClusters()) { clusterMap.put(toPanelPoint(c.getClusterCenter()), c); }
        
        // Display it.
        displayEvent(em.getHits(), em.getClusters());
    }
}

package org.hps.monitoring.ecal.eventdisplay.ui;

import org.hps.monitoring.ecal.eventdisplay.event.Cluster;
import org.hps.monitoring.ecal.eventdisplay.event.EcalHit;
import org.hps.recon.ecal.HPSEcalCluster;
import org.hps.recon.ecal.HPSEcalClusterIC;
import org.lcsim.event.CalorimeterHit;

/**
 * Abstract class <code>PassiveViewer</code> represents a <code>Viewer
 * </code> implementation which updates based on information passed to
 * it by an external source.
 * 
 * @author Kyle McCarty
 */
public abstract class PassiveViewer extends Viewer {
    private static final long serialVersionUID = -7479125553259270894L;
    
    /**
     * Adds a new hit to the display.
     * @param hit - The hit to be added.
     */
    public abstract void addHit(CalorimeterHit lcioHit);
    
    /**
     * Adds a new hit to the display.
     * @param hit - The hit to be added.
     */
    public abstract void addHit(EcalHit hit);
    
    /**
     * Adds a new cluster to the display.
     * @param cluster - The cluster to be added.
     */
    public abstract void addCluster(Cluster cluster);
    
    /**
     * Adds a new cluster to the display.
     * @param cluster - The cluster to be added.
     */
    public abstract void addCluster(HPSEcalCluster cluster);
    
    /**
     * Clears any hits or clusters that have been added to the viewer.
     * Note that this does not automatically update the displayed panel.
     * <code>updateDisplay</code> must be called separately.
     */
    public abstract void resetDisplay();
    
    /**
     * Sets the upper and lower bounds of for the calorimeter display's
     * color mapping scale.
     * @param min - The lower bound.
     * @param max - The upper bound.
     */
    public void setScale(double min, double max) {
        ecalPanel.setScaleMinimum(min);
        ecalPanel.setScaleMaximum(max);
    }
    
    /**
     * Sets the upper bound for the calorimeter display's color mapping
     * scale.
     * @param max - The upper bound.
     */
    public void setScaleMaximum(double max) { ecalPanel.setScaleMaximum(max); }
    
    /**
     * Sets the lower bound for the calorimeter display's color mapping
     * scale.
     * @param min - The lower bound.
     */
    public void setScaleMinimum(double min) { ecalPanel.setScaleMinimum(min); }
    
    /**
     * Converts an <code>HPSEcalCluster</code> object to a panel <code>
     * Cluster</code> object.
     * @param lcioCluster - The <code>HPSEcalCluster</code> object.
     * @return Returns the argument cluster as a <code>Cluster</code>
     * object that can be used with the <code>Viewer</code>.
     */
    public static final Cluster toPanelCluster(HPSEcalCluster lcioCluster) {
        // Get the cluster data from the LCIO cluster.
        int ix = lcioCluster.getSeedHit().getIdentifierFieldValue("ix");
        int iy = lcioCluster.getSeedHit().getIdentifierFieldValue("iy");
        double energy = lcioCluster.getEnergy();
        
        // Generate a new cluster.
        Cluster panelCluster = new Cluster(ix, iy, energy);
        
        // If this is an IC cluster, cast it so that shared hits can
        // be properly displayed.
        if(lcioCluster instanceof HPSEcalClusterIC) {
        	// Cast the cluster object.
        	HPSEcalClusterIC icCluster = (HPSEcalClusterIC) lcioCluster;
        	
	        // Add any component hits to the cluster.
	        for(CalorimeterHit lcioHit : icCluster.getUniqueHits()) {
	            // Get the position of the calorimeter hit.
	            int hix = lcioHit.getIdentifierFieldValue("ix");
	            int hiy = lcioHit.getIdentifierFieldValue("iy");
	            
	            // Add the hit to the cluster.
	            panelCluster.addComponentHit(hix, hiy);
	        }
	        
	        // Add any shared hits to the cluster.
	        for(CalorimeterHit lcioHit : icCluster.getSharedHits()) {
	            // Get the position of the calorimeter hit.
	            int hix = lcioHit.getIdentifierFieldValue("ix");
	            int hiy = lcioHit.getIdentifierFieldValue("iy");
	            
	            // Add the hit to the cluster.
	            panelCluster.addSharedHit(hix, hiy);
	        }
        }
        
        // Otherwise, it just has component hits.
        else {
	        // Add any component hits to the cluster.
	        for(CalorimeterHit lcioHit : lcioCluster.getCalorimeterHits()) {
	            // Get the position of the calorimeter hit.
	            int hix = lcioHit.getIdentifierFieldValue("ix");
	            int hiy = lcioHit.getIdentifierFieldValue("iy");
	            
	            // Add the hit to the cluster.
	            panelCluster.addComponentHit(hix, hiy);
	        }
        }
        
        // Return the cluster.
        return panelCluster;
    }
    
    /**
     * Converts a <code>CalorimeterHit</code> object to a panel <code>
     * EcalHit</code> object.
     * @param lcioHit - The <code>CalorimeterHit</code> object.
     * @return Returns the argument hit as an <code>EcalHit</code>
     * object that can be used with the <code>Viewer</code>.
     */
    public static final EcalHit toPanelHit(CalorimeterHit lcioHit) {
        // Get the hit information from the LCIO hit/
        int ix = lcioHit.getIdentifierFieldValue("ix");
        int iy = lcioHit.getIdentifierFieldValue("iy");
        double energy = lcioHit.getCorrectedEnergy();
        
        // Create the panel hit.
        return new EcalHit(ix, iy, energy);
    }
    
    /**
     * Displays the hits and clusters added by the <code>addHit</code>
     * and <code>addCluster</code> methods.
     */
    public abstract void updateDisplay();
}
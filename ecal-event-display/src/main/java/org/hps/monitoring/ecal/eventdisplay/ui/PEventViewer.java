package org.hps.monitoring.ecal.eventdisplay.ui;

import java.awt.Point;
import java.util.ArrayList;

import org.hps.monitoring.ecal.eventdisplay.event.Association;
import org.hps.monitoring.ecal.eventdisplay.event.Cluster;
import org.hps.monitoring.ecal.eventdisplay.event.EcalHit;

/**
 * Class <code>PEventViewer</code> represents a <code>PassiveViewer
 * </code> implementation which displays hits and clusters.
 * 
 * @author Kyle McCarty
 */
public class PEventViewer extends PassiveViewer {
    private static final long serialVersionUID = -7479125553259270894L;
    // Stores cluster objects.
    protected ArrayList<Cluster> clusterList = new ArrayList<Cluster>();
    // Stores hit objects.
    protected ArrayList<EcalHit> hitList = new ArrayList<EcalHit>();
    
    @Override
    public void addHit(EcalHit hit) { hitList.add(hit); }
    
    @Override
    public void addCluster(Cluster cluster) { clusterList.add(cluster); }
    
    /**
     * Removes all of the hit data from the viewer.
     */
    public void clearHits() { hitList.clear(); }
    
    /**
     * Removes all of the cluster data from the viewer.
     */
    public void clearClusters() { hitList.clear(); }
    
    @Override
    public void resetDisplay() {
        // Reset the hit and cluster lists.
        hitList.clear();
        clusterList.clear();
    }
    
    @Override
    public void updateDisplay() {
        // Suppress the calorimeter panel's redrawing.
        ecalPanel.setSuppressRedraw(true);
        
        // Clear the panel data.
        ecalPanel.clearCrystals();
        
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
}

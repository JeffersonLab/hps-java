package org.hps.monitoring.ecal.eventdisplay.ui;

import java.awt.Point;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

import org.hps.monitoring.ecal.eventdisplay.event.Association;
import org.hps.monitoring.ecal.eventdisplay.event.Cluster;
import org.hps.monitoring.ecal.eventdisplay.event.EcalHit;
import org.lcsim.event.CalorimeterHit;

/**
 * Class <code>PEventViewer</code> represents a <code>PassiveViewer
 * </code> implementation which displays hits and clusters.
 */
public class PEventViewer extends PassiveViewer {
    private static final long serialVersionUID = -7479125553259270894L;
    // Stores cluster objects.
    protected ArrayList<Cluster> clusterList = new ArrayList<Cluster>();
    // Stores hit objects.
    protected ArrayList<EcalHit> hitList = new ArrayList<EcalHit>();

    @Override
    public void addCluster(final Cluster cluster) {
        this.clusterList.add(cluster);
    }

    @Override
    public void addCluster(final org.lcsim.event.Cluster lcioCluster) {
        this.clusterList.add(toPanelCluster(lcioCluster));
    }

    @Override
    public void addHit(final CalorimeterHit lcioHit) {
        this.hitList.add(toPanelHit(lcioHit));
    }

    @Override
    public void addHit(final EcalHit hit) {
        this.hitList.add(hit);
    }

    /**
     * Removes all of the cluster data from the viewer.
     */
    public void clearClusters() {
        this.hitList.clear();
    }

    /**
     * Removes all of the hit data from the viewer.
     */
    public void clearHits() {
        this.hitList.clear();
    }

    @Override
    public void resetDisplay() {
        // Reset the hit and cluster lists.
        this.hitList.clear();
        this.clusterList.clear();
    }

    @Override
    public void updateDisplay() {
        // Suppress the calorimeter panel's redrawing.
        this.ecalPanel.setSuppressRedraw(true);

        // Clear the panel data.
        this.ecalPanel.clearCrystals();

        // Display the hits.
        for (final EcalHit h : this.hitList) {
            final int ix = toPanelX(h.getX());
            final int iy = toPanelY(h.getY());
            this.ecalPanel.addCrystalEnergy(ix, iy, h.getEnergy());
        }

        // Display the clusters.
        for (final Cluster cluster : this.clusterList) {
            final Point rawCluster = cluster.getClusterCenter();
            final Point clusterCenter = toPanelPoint(rawCluster);
            this.ecalPanel.setCrystalCluster(clusterCenter.x, clusterCenter.y, true);

            // Add component hits to the calorimeter panel.
            for (final Point ch : cluster.getComponentHits()) {
                this.ecalPanel.addAssociation(new Association(clusterCenter, toPanelPoint(ch), HIGHLIGHT_CLUSTER_COMPONENT));
            }

            // Add shared hits to the calorimeter panel.
            for (final Point sh : cluster.getSharedHits()) {
                this.ecalPanel.addAssociation(new Association(clusterCenter, toPanelPoint(sh), HIGHLIGHT_CLUSTER_SHARED));
            }
        }

        // Stop suppressing the redraw and order the panel to update.
        this.ecalPanel.setSuppressRedraw(false);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                PEventViewer.this.ecalPanel.repaint();
            }
        });

        // Update the status panel to account for the new event.
        this.updateStatusPanel();
    }
}

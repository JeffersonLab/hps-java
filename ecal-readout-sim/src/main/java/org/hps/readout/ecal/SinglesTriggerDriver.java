package org.hps.readout.ecal;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.util.List;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.aida.AIDA;

/**
 * Class <code>SinglesTriggerDriver</code> represents a basic single
 * cluster trigger. It triggers off of seed energy (upper and lower
 * bounds), cluster total energy (upper and lower bounds), and the
 * cluster hit count (lower bound only). All parameters may be set
 * through a steering file.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see TriggerDriver
 */
public class SinglesTriggerDriver extends TriggerDriver {
    // Cut Values
    private int hitCountLow = 2;
    private double seedEnergyLow = 0.125;
    private double seedEnergyHigh = 6.6;
    private double clusterEnergyLow = 0.200;
    private double clusterEnergyHigh = 6.6;
    
    // LCIO Collection Names
    private String clusterCollectionName = "EcalClusters";
    
    // AIDA Plots
    private AIDA aida = AIDA.defaultInstance();
    private IHistogram1D clusterSeedEnergy = aida.histogram1D("Trigger Plots :: Cluster Seed Energy Distribution", 176, 0.0, 2.2);
    private IHistogram1D clusterSeedEnergySingle = aida.histogram1D("Trigger Plots :: Cluster Seed Energy Distribution (Passed Single Cuts)", 176, 0.0, 2.2);
    private IHistogram1D clusterHitCount = aida.histogram1D("Trigger Plots :: Cluster Hit Count Distribution", 9, 1, 10);
    private IHistogram1D clusterHitCountSingle = aida.histogram1D("Trigger Plots :: Cluster Hit Count Distribution (Passed Single Cuts)", 9, 1, 10);
    private IHistogram1D clusterTotalEnergy = aida.histogram1D("Trigger Plots :: Cluster Total Energy Distribution", 176, 0.0, 2.2);
    private IHistogram1D clusterTotalEnergySingle = aida.histogram1D("Trigger Plots :: Cluster Total Energy Distribution (Passed Single Cuts)", 176, 0.0, 2.2);
    private IHistogram2D clusterDistribution = aida.histogram2D("Trigger Plots :: Cluster Seed Distribution", 46, -23, 23, 11, -5.5, 5.5);
    private IHistogram2D clusterDistributionSingle = aida.histogram2D("Trigger Plots :: Cluster Seed Distribution (Passed Single Cuts)", 46, -23, 23, 11, -5.5, 5.5);
    
    @Override
    public void process(EventHeader event) {
        // Make sure that there are clusters in the event.
        if(event.hasCollection(Cluster.class, clusterCollectionName)) {
            // Get the list of clusters.
            List<Cluster> clusterList = event.get(Cluster.class, clusterCollectionName);
            
            // Iterate over the clusters.
            for(Cluster cluster : clusterList) {
                // Get the x and y indices.
                int ix = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
                int iy = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
                ix = ix > 0 ? ix - 1 : ix;
                
                // Populate the uncut plots.
                clusterSeedEnergy.fill(cluster.getCalorimeterHits().get(0).getCorrectedEnergy(), 1);
                clusterTotalEnergy.fill(cluster.getEnergy(), 1);
                clusterHitCount.fill(cluster.getCalorimeterHits().size(), 1);
                clusterDistribution.fill(ix, iy, 1);
            }
        }
    }
    
    /**
     * Performs cluster singles cuts. These include seed energy, cluster
     * energy, and minimum hit count.
     * @return Returns <code>true</code> if the event passes the trigger
     * conditions and <code>false</code> otherwise.
     */
    @Override
    protected boolean triggerDecision(EventHeader event) {
        // Check that there is a cluster object collection.
        if(event.hasCollection(Cluster.class, clusterCollectionName)) {
            // Get the list of hits.
            List<Cluster> clusterList = event.get(Cluster.class, clusterCollectionName);
            
            // Iterate over the hits and perform the cuts.
            triggerLoop:
            for(Cluster cluster : clusterList) {
                // Perform the hit count cut.
                if(!SSPTriggerLogic.clusterHitCountCut(cluster, hitCountLow)) {
                    continue triggerLoop;
                }
                
                // Perform the seed hit cut.
                if(!SSPTriggerLogic.clusterSeedEnergyCut(cluster, seedEnergyLow, seedEnergyHigh)) {
                    continue triggerLoop;
                }
                
                // Perform the cluster energy cut.
                if(!SSPTriggerLogic.clusterTotalEnergyCut(cluster, clusterEnergyLow, clusterEnergyHigh)) {
                    continue triggerLoop;
                }
                
                // Get the x and y indices.
                int ix = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
                int iy = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
                ix = ix > 0 ? ix - 1 : ix;
                
                // Populate the cut plots.
                clusterSeedEnergySingle.fill(cluster.getCalorimeterHits().get(0).getCorrectedEnergy(), 1);
                clusterTotalEnergySingle.fill(cluster.getEnergy(), 1);
                clusterHitCountSingle.fill(cluster.getCalorimeterHits().size(), 1);
                clusterDistributionSingle.fill(ix, iy, 1);
            }
        }
        
        // If there were either no passing hits or not hits at all,
        // then there is also no trigger.
        return false;
    }
    
    /**
     * Sets the minimum hit count threshold for the trigger. This value
     * is inclusive.
     * @param hitCountThreshold - The value of the threshold.
     */
    public void setHitCountThreshold(int hitCountThreshold) {
        hitCountLow = hitCountThreshold;
    }
    
    /**
     * Sets the lower bound for the seed energy threshold on the trigger.
     * This value is inclusive.
     * @param seedEnergyLow - The value of the threshold.
     */
    public void setSeedEnergyLowThreshold(double seedEnergyLow) {
        this.seedEnergyLow = seedEnergyLow;
    }
    
    /**
     * Sets the upper bound for the seed energy threshold on the trigger.
     * This value is inclusive.
     * @param seedEnergyHigh - The value of the threshold.
     */
    public void setSeedEnergyHighThreshold(double seedEnergyHigh) {
        this.seedEnergyHigh = seedEnergyHigh;
    }
    
    /**
     * Sets the lower bound for the cluster energy threshold on the
     * trigger. This value is inclusive.
     * @param clusterEnergyLow - The value of the threshold.
     */
    public void setClusterEnergyLowThreshold(double clusterEnergyLow) {
        this.clusterEnergyLow = clusterEnergyLow;
    }
    
    /**
     * Sets the upper bound for the cluster energy threshold on the
     * trigger. This value is inclusive.
     * @param clusterEnergyHigh - The value of the threshold.
     */
    public void setClusterEnergyHighThreshold(double clusterEnergyHigh) {
        this.clusterEnergyHigh = clusterEnergyHigh;
    }
    
    /**
     * Sets the name of the LCIO collection from which clusters are drawn.
     * @param clusterCollectionName - The name of the LCIO collection.
     */
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
}
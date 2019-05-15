package org.hps.readout.trigger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.hps.readout.ReadoutDataManager;
import org.hps.readout.TriggerDriver;
import org.hps.recon.ecal.EcalUtils;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;

public class PairTriggerReadoutDriver extends TriggerDriver {
    // ==================================================================
    // ==== Trigger General Default Parameters ==========================
    // ==================================================================
    private String inputCollectionName = "EcalClustersGTP";       // Name for the LCIO cluster collection.
    private int pairCoincidence = 2;                              // Maximum allowed time difference between clusters. (4 ns clock-cycles)
    private String ecalGeometryName = "Ecal";                     // Name of the calorimeter geometry object.
    private TriggerModule triggerModule = new TriggerModule(1.0, 0.050,
            6.600, 0.010, 6.600, 0.000, 13.200, 6.600, 0.0, 360, 0.0055);
    
    // ==================================================================
    // ==== Driver Internal Variables ===================================
    // ==================================================================
    private Queue<List<Cluster>> topClusterQueue = null;           // Store clusters on the top half of the calorimeter.
    private Queue<List<Cluster>> botClusterQueue = null;           // Store clusters on the bottom half of the calorimeter.
    private double localTime = 0.0;                                // Stores the internal time clock for the driver.
    private HPSEcal3 ecal = null;                                  // The calorimeter geometry object.
    
    @Override
    public void detectorChanged(Detector detector) {
        // Get the calorimeter sub-detector.
        org.lcsim.geometry.compact.Subdetector ecalSub = detector.getSubdetector(ecalGeometryName);
        if(ecalSub instanceof HPSEcal3) {
            ecal = (HPSEcal3) ecalSub;
        } else {
            throw new IllegalStateException("Error: Unexpected calorimeter sub-detector of type \"" + ecalSub.getClass().getSimpleName() + "; expected HPSEcal3.");
        }
    }
    
    @Override
    public void startOfData() {
        // Define the driver collection dependencies.
        addDependency(inputCollectionName);
        
        // Register the trigger.
        ReadoutDataManager.registerTrigger(this);
        
        // Make sure that a valid cluster collection name has been
        // defined. If it has not, throw an exception.
        if(inputCollectionName == null) {
            throw new RuntimeException("The parameter inputCollectionName was not set!");
        }
        
        // Initialize the top and bottom cluster queues.
        topClusterQueue = new LinkedList<List<Cluster>>();
        botClusterQueue = new LinkedList<List<Cluster>>();
        
        // Populate the top cluster queue. It should be populated with
        // a number of empty lists equal to (2 * pairCoincidence + 1).
        for(int i = 0; i < 2 * pairCoincidence + 1; i++) {
            topClusterQueue.add(new ArrayList<Cluster>());
        }
        
        // Populate the bottom cluster queue. It should be populated with
        // a number of empty lists equal to (2 * pairCoincidence + 1).
        for(int i = 0; i < pairCoincidence + 1; i++) {
            botClusterQueue.add(new ArrayList<Cluster>());
        }
        
        // Run the superclass method.
        super.startOfData();
    }
    
    @Override
    public void process(EventHeader event) {
        // If there is no data ready, then nothing can be done/
        if(!ReadoutDataManager.checkCollectionStatus(inputCollectionName, localTime)) {
            return;
        }
        
        // Otherwise, get the input clusters from the present time.
        Collection<Cluster> clusters = ReadoutDataManager.getData(localTime, localTime + 4.0, inputCollectionName, Cluster.class);
        
        // Remove any clusters that do not pass the singles cuts. It
        // is more efficient to eliminate these before forming pairs.
        Collection<Cluster> goodClusters = getGoodClusters(clusters);
            
        // Put the good clusters into the cluster queue.
        updateClusterQueues(goodClusters);
        
        // Check that if a trigger exists, if the trigger is not in
        // dead time. If it is, no trigger may be issued, so this is
        // not necessary.
        if(!isInDeadTime() && testTrigger()) { sendTrigger(); }
        
        // Increment the local time.
        localTime += 4.0;
    }
    
    /**
     * Sets the name of the LCIO collection that contains the clusters.
     * @param clusterCollectionName - The cluster LCIO collection name.
     */
    public void setInputCollectionName(String clusterCollectionName) {
        inputCollectionName = clusterCollectionName;
    }
    
    /**
     * Sets the highest allowed energy a cluster may have and still
     * pass the cluster total energy single cluster cut. Value uses
     * units of GeV.
     * @param clusterEnergyHigh - The parameter value.
     */
    public void setClusterEnergyHigh(double clusterEnergyHigh) {
        triggerModule.setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH, clusterEnergyHigh * EcalUtils.GeV);
    }
    
    /**
     * Sets the lowest allowed energy a cluster may have and still
     * pass the cluster total energy single cluster cut. Value uses
     * units of GeV.
     * @param clusterEnergyLow - The parameter value.
     */
    public void setClusterEnergyLow(double clusterEnergyLow) {
        triggerModule.setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW, clusterEnergyLow * EcalUtils.GeV);
    }
    
    /**
     * Sets the maximum deviation from coplanarity that a cluster pair
     * may possess and still pass the coplanarity pair cut. Value uses
     * units of degrees.
     * @param coplanarityHigh - The parameter value.
     */
    public void setCoplanarityHigh(double coplanarityHigh) {
        triggerModule.setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH, coplanarityHigh);
    }
    
    /**
     * Sets the highest allowed energy difference a cluster pair may
     * have and still pass the cluster pair energy difference cut.
     * Value uses units of GeV.
     * @param energyDifferenceHigh - The parameter value.
     */
    public void setEnergyDifferenceHigh(double energyDifferenceHigh) {
        triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH, energyDifferenceHigh * EcalUtils.GeV);
    }
    
    /**
     * Sets the lowest allowed energy slope a cluster pair may
     * have and still pass the cluster pair energy slope cut.
     * @param energySlopeLow - The parameter value.
     */
    public void setEnergySlopeLow(double energySlopeLow) {
        triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW, energySlopeLow);
    }
    
    /**
     * Sets the highest allowed energy a cluster pair may have and
     * still pass the cluster pair energy sum cluster cut. Value uses
     * units of GeV.
     * @param energySumHigh - The parameter value.
     */
    public void setEnergySumHigh(double energySumHigh) {
        triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH, energySumHigh * EcalUtils.GeV);
    }
    
    /**
     * Sets the lowest allowed energy a cluster pair may have and
     * still pass the cluster pair energy sum cluster cut. Value uses
     * units of GeV.
     * @param energySumLow - The parameter value.
     */
    public void setEnergySumLow(double energySumLow) {
        triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW, energySumLow * EcalUtils.GeV);
    }
    
    /**
     * Sets the minimum number of hits needed for a cluster to pass
     * the hit count single cluster cut.
     * @param minHitCount - The parameter value.
     */
    public void setMinHitCount(int minHitCount) {
        triggerModule.setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW, minHitCount);
    }
    
    /**
     * Sets the time range over which cluster pairs will be formed.
     * Value uses units of clock-cycles. Note that the number of
     * clock-cycles used is calculated as (2 * pairCoincidence) + 1.
     * @param pairCoincidence - The parameter value.
     */
    public void setPairCoincidence(int pairCoincidence) {
        this.pairCoincidence = pairCoincidence;
    }
    
    /**
     * Sets the highest allowed energy a seed hit may have and still
     * pass the seed hit energy single cluster cut. Value uses units
     * of GeV.
     * @param seedEnergyHigh - The parameter value.
     */
    public void setSeedEnergyHigh(double seedEnergyHigh) {
        triggerModule.setCutValue(TriggerModule.CLUSTER_SEED_ENERGY_HIGH, seedEnergyHigh * EcalUtils.GeV);
    }
    
    /**
     * Sets the lowest allowed energy a seed hit may have and still
     * pass the seed hit energy single cluster cut. Value uses units
     * of GeV.
     * @param seedEnergyLow - The parameter value.
     */
    public void setSeedEnergyLow(double seedEnergyLow) {
        triggerModule.setCutValue(TriggerModule.CLUSTER_SEED_ENERGY_LOW, seedEnergyLow * EcalUtils.GeV);
    }
    
    /**
     * Sets the value of F in the energy slope equation <code>E_min +
     * R_min * F</code>.
     * @param f - The new energy slope parameter.
     */
    public void setEnergySlopeParamF(double f) {
        triggerModule.setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_F, f);
    }
    
    /**
     * Get a list of all unique cluster pairs in the event.
     * @return A <code>List</code> collection of <code>Cluster</code> 
     * objects containing all cluster pairs.
     */
    protected List<Cluster[]> getClusterPairsTopBot() {
        // Create a list to store cluster pairs. 
        List<Cluster[]> clusterPairs = new ArrayList<Cluster[]>();
        
        // Loop over all top-bottom pairs of clusters; higher-energy cluster goes first in the pair
        // To apply pair coincidence time, use only bottom clusters from the 
        // readout cycle pairCoincidence readout cycles ago, and top clusters 
        // from all 2*pairCoincidence+1 previous readout cycles
        for(Cluster botCluster : botClusterQueue.element()) {
            for(List<Cluster> topClusters : topClusterQueue) {
                for(Cluster topCluster : topClusters) {
                    // The first cluster in a pair should always be
                    // the higher energy cluster. If the top cluster
                    // is higher energy, it goes first.
                    if(topCluster.getEnergy() > botCluster.getEnergy()) {
                        Cluster[] clusterPair = { topCluster, botCluster };
                        clusterPairs.add(clusterPair);
                    }
                    
                    // Otherwise, the bottom cluster goes first.
                    else {
                        Cluster[] clusterPair = { botCluster, topCluster };
                        clusterPairs.add(clusterPair);
                    }
                }
            }
        }
        
        // Return the cluster pair lists.
        return clusterPairs;
    }
    
    @Override
    protected double getTimeDisplacement() {
        return (pairCoincidence + 1) * 4.0;
    }
    
    @Override
    protected double getTimeNeededForLocalOutput() {
        return 0;
    }
    
    private Collection<Cluster> getGoodClusters(Collection<Cluster> clusters) {
        // Create a list to hold clusters which pass the single
        // cluster cuts.
        Collection<Cluster> goodClusterList = new ArrayList<Cluster>();
        
        // Sort through the cluster list and add clusters that pass
        // the single cluster cuts to the good list.
        clusterLoop:
        for(Cluster cluster : clusters) {
            // ==== Seed Hit Energy Cut ====================================
            // =============================================================
            // If the cluster fails the cut, skip to the next cluster.
            if(!triggerModule.clusterSeedEnergyCut(cluster)) {
                continue clusterLoop;
            }
            
            // ==== Cluster Hit Count Cut ==================================
            // =============================================================
            // If the cluster fails the cut, skip to the next cluster.
            if(!triggerModule.clusterHitCountCut(cluster)) {
                continue clusterLoop;
            }
            
            // ==== Cluster Total Energy Cut ===============================
            // =============================================================
            // If the cluster fails the cut, skip to the next cluster.
            if(!triggerModule.clusterTotalEnergyCut(cluster)) {
                continue clusterLoop;
            }
            
            // A cluster that passes all of the single-cluster cuts
            // can be used in cluster pairs.
            goodClusterList.add(cluster);
        }
        
        // Return the good clusters.
        return goodClusterList;
    }
    
    /**
     * Tests all of the current cluster pairs for triggers.
     * @return Returns <code>true</code> if one of the cluster pairs
     * passes all of the cluster cuts and <code>false</code> otherwise.
     */
    private boolean testTrigger() {
        // Track whether a trigger has occurred.
        boolean triggered = false;
        
        // Get the list of cluster pairs.
        List<Cluster[]> clusterPairs = getClusterPairsTopBot();
        
        // Iterate over the cluster pairs and perform each of the cluster
        // pair cuts on them. A cluster pair that passes all of the
        // cuts registers as a trigger.
        pairLoop:
        for(Cluster[] clusterPair : clusterPairs) {
            // Get the x and y indices. Note that LCSim meta data is
            // not available during readout, so crystal indices must
            // be obtained directly from the calorimeter geometry.
            java.awt.Point ixy0 = ecal.getCellIndices(clusterPair[0].getCalorimeterHits().get(0).getCellID());
            java.awt.Point ixy1 = ecal.getCellIndices(clusterPair[1].getCalorimeterHits().get(0).getCellID());
            
            
            // ==== Pair Energy Sum Cut ====================================
            // =============================================================
            // If the cluster fails the cut, skip to the next pair.
            if(!triggerModule.pairEnergySumCut(clusterPair)) {
                continue pairLoop;
            }
            
            // ==== Pair Energy Difference Cut =============================
            // =============================================================
            // If the cluster fails the cut, skip to the next pair.
            if(!triggerModule.pairEnergyDifferenceCut(clusterPair)) {
                continue pairLoop;
            }
            
            // ==== Pair Energy Slope Cut ==================================
            // =============================================================
            // If the cluster fails the cut, skip to the next pair.
            if(!triggerModule.pairEnergySlopeCut(clusterPair, ixy0, ixy1)) {
                continue pairLoop;
            }
            
            // ==== Pair Coplanarity Cut ===================================
            // =============================================================
            // If the cluster fails the cut, skip to the next pair.
            if(!triggerModule.pairCoplanarityCut(clusterPair, ixy0, ixy1)) {
                continue pairLoop;
            }
            
            // Clusters that pass all of the pair cuts produce a trigger.
            triggered = true;
        }
        
        // Return whether or not a trigger was observed.
        return triggered;
    }
    
    /**
     * Adds clusters from a new event into the top and bottom cluster
     * queues so that they may be formed into pairs.
     * @param clusterList - The clusters to add to the queues.
     */
    private void updateClusterQueues(Collection<Cluster> clusterList) {
        // Create lists to store the top and bottom clusters.
        ArrayList<Cluster> topClusterList = new ArrayList<Cluster>();
        ArrayList<Cluster> botClusterList = new ArrayList<Cluster>();
        
        // Loop over the clusters in the cluster list.
        for(Cluster cluster : clusterList) {
            // Get the x and y indices. Note that LCSim meta data is
            // not available during readout, so crystal indices must
            // be obtained directly from the calorimeter geometry.
            java.awt.Point ixy = ecal.getCellIndices(cluster.getCalorimeterHits().get(0).getCellID());
            
            // If the cluster is on the top of the calorimeter, it
            // goes into the top cluster list.
            if(ixy.y > 0) { topClusterList.add(cluster); }
            
            // Otherwise, it goes into the bottom cluster list.
            else { botClusterList.add(cluster); }
        }
        
        // Add the new cluster lists to the cluster queues.
        topClusterQueue.add(topClusterList);
        botClusterQueue.add(botClusterList);
        
        // Remove the oldest cluster lists from the queues.
        topClusterQueue.remove();
        botClusterQueue.remove();
    }
}
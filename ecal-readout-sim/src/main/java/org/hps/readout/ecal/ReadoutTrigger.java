package org.hps.readout.ecal;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Creates distributions from <code>Cluster</code> objects. This is
 * intended to be used on clusters reconstructed from FADC data from
 * the hardware readout and should not be used for Monte Carlo.
 */
public class ReadoutTrigger extends Driver {
    // Define settable parameters.
    private double energySlopeParamF = 0.0055;
    private String clusterCollectionName = "EcalClusters";
    
    // Define internal variables.
    private TriggerModule trigger = new TriggerModule();
    
    // Define output plots.
    private static final int NO_CUTS  = 0;
    private static final int ALL_CUTS = 1;
    private AIDA aida = AIDA.defaultInstance();
    private IHistogram1D[] clusterSeedEnergy;
    private IHistogram1D[] clusterHitCount;
    private IHistogram1D[] clusterTotalEnergy;
    private IHistogram1D[] clusterTime;
    private IHistogram1D[] pairEnergySum;
    private IHistogram1D[] pairEnergyDifference;
    private IHistogram1D[] pairCoplanarity;
    private IHistogram1D[] pairEnergySlope;
    private IHistogram1D[] pairTime;
    private IHistogram1D[] pairCoincidence;
    private IHistogram2D[] clusterDistribution;
    private IHistogram2D[] pairEnergySum2D;
    private IHistogram2D[] pairEnergySlope2D;
    
    /**
     * Instantiates cluster plots.
     */
    @Override
    public void startOfData() {
        // Define plot type names.
        String[] plotType = new String[2];
        plotType[NO_CUTS] = "";
        plotType[ALL_CUTS] = " (Passed All Cuts)";
        
        // Define plot type directories.
        String[] plotDir = new String[2];
        plotDir[NO_CUTS] = "NoCuts/";
        plotDir[ALL_CUTS] = "PassedAll/";
        
        // Instantiate the plots.
        for(int i = 0; i < 2; i++) {
            System.out.println(plotDir[i] + "Cluster Seed Energy" + plotType[i]);
            clusterSeedEnergy[i] = aida.histogram1D(plotDir[i] + "Cluster Seed Energy" + plotType[i], 88, 0.0, 1.1);
            clusterSeedEnergy[i].annotation().addItem("xAxisLabel", "Seed Energy (GeV)");
            clusterSeedEnergy[i].annotation().addItem("yAxisLabel", "Count");
            
            clusterHitCount[i] = aida.histogram1D(plotDir[i] + "Cluster Hit Count" + plotType[i], 9, 0.5, 9.5);
            clusterHitCount[i].annotation().addItem("xAxisLabel", "Hit Count");
            clusterHitCount[i].annotation().addItem("yAxisLabel", "Count");
            
            clusterTotalEnergy[i] = aida.histogram1D(plotDir[i] + "Cluster Total Energy" + plotType[i], 88, 0.0, 1.1);
            clusterTotalEnergy[i].annotation().addItem("xAxisLabel", "Cluster Energy (GeV)");
            clusterTotalEnergy[i].annotation().addItem("yAxisLabel", "Count");
            
            clusterTime[i] = aida.histogram1D(plotDir[i] + "Cluster Time" + plotType[i], 100, 0.0, 400);
            clusterTime[i].annotation().addItem("xAxisLabel", "Cluster Time (ns)");
            clusterTime[i].annotation().addItem("yAxisLabel", "Count");
        
            pairEnergySum[i] = aida.histogram1D(plotDir[i] + "Pair Energy Sum" + plotType[i], 88, 0.0, 2.2);
            pairEnergySum[i].annotation().addItem("xAxisLabel", "Energy Sum (GeV)");
            pairEnergySum[i].annotation().addItem("yAxisLabel", "Count");
            
            pairEnergyDifference[i] = aida.histogram1D(plotDir[i] + "Pair Energy Difference" + plotType[i], 88, 0.0, 1.1);
            pairEnergyDifference[i].annotation().addItem("xAxisLabel", "Energy Difference (GeV)");
            pairEnergyDifference[i].annotation().addItem("yAxisLabel", "Count");
            
            pairCoplanarity[i] = aida.histogram1D(plotDir[i] + "Pair Coplanarity" + plotType[i], 180, 0.0, 180.0);
            pairCoplanarity[i].annotation().addItem("xAxisLabel", "Coplanarity Angle (Degrees)");
            pairCoplanarity[i].annotation().addItem("yAxisLabel", "Count");
            
            pairEnergySlope[i] = aida.histogram1D(plotDir[i] + "Pair Energy Slope" + plotType[i], 200, 0.0, 4.0);
            pairEnergySlope[i].annotation().addItem("xAxisLabel", "Energy Slope (GeV)");
            pairEnergySlope[i].annotation().addItem("yAxisLabel", "Count");
            
            pairTime[i] = aida.histogram1D(plotDir[i] + "Pair Time" + plotType[i], 100, 0.0, 400);
            pairTime[i].annotation().addItem("xAxisLabel", "Cluster Time (ns)");
            pairTime[i].annotation().addItem("yAxisLabel", "Count");
            
            pairCoincidence[i] = aida.histogram1D(plotDir[i] + "Pair Coincidence" + plotType[i], 8, 0.0, 32);
            pairCoincidence[i].annotation().addItem("xAxisLabel", "Coincidence Time (ns)");
            pairCoincidence[i].annotation().addItem("yAxisLabel", "Count");
            
            clusterDistribution[i] = aida.histogram2D(plotDir[i] + "Cluster Seed Distribution" + plotType[i], 46, -23, 23, 11, -5.5, 5.5);
            clusterDistribution[i].annotation().addItem("xAxisLabel", "x-Index");
            clusterDistribution[i].annotation().addItem("yAxisLabel", "y-Index");
            
            pairEnergySum2D[i] = aida.histogram2D(plotDir[i] + "Pair Energy Sum 2D" + plotType[i], 88, 0.0, 2.2, 88, 0.0, 2.2);
            pairEnergySum2D[i].annotation().addItem("xAxisLabel", "E1");
            pairEnergySum2D[i].annotation().addItem("yAxisLabel", "E2");
            
            pairEnergySlope2D[i] = aida.histogram2D(plotDir[i] + "Pair Energy Slope 2D" + plotType[i], 88, 0.0, 1.1, 200, 0.0, 400);
            pairEnergySlope2D[i].annotation().addItem("xAxisLabel", "E1");
            pairEnergySlope2D[i].annotation().addItem("yAxisLabel", "E2");
        }
    }
    
    /**
     * Produces both uncut and cut distributions from clusters.
     */
    @Override
    public void process(EventHeader event) {
        // Check for a collection of clusters.
        if(event.hasCollection(Cluster.class, clusterCollectionName)) {
            // Get the list of clusters.
            List<Cluster> clusters = event.get(Cluster.class, clusterCollectionName);
            
            // Track which clusters have already been plotted.
            Set<Cluster> plottedClustersUncut = new HashSet<Cluster>(clusters.size());
            Set<Cluster> plottedClustersCut = new HashSet<Cluster>(clusters.size());
            
            // Populate a list of cluster pairs.
            List<Cluster[]> pairs = getClusterPairs(clusters);
            
            // Process all cluster pairs.
            pairLoop:
            for(Cluster[] pair : pairs) {
                // Get the x and y indices for each cluster in the pair.
                int[] ix = { pair[0].getCalorimeterHits().get(0).getIdentifierFieldValue("ix"),
                        pair[1].getCalorimeterHits().get(0).getIdentifierFieldValue("ix") };
                int[] iy = { pair[0].getCalorimeterHits().get(0).getIdentifierFieldValue("iy"),
                        pair[1].getCalorimeterHits().get(0).getIdentifierFieldValue("iy") };
                
                // Iterate over the clusters in the pair and plot the
                // cluster singles distributions.
                for(int clusterIndex = 0; clusterIndex < 2; clusterIndex++) {
                    // Only plot cluster singles distributions for
                    // clusters if they have not already been plotted.
                    // Note that this is needed because the same cluster
                    // can appear across multiple pairs.
                    if(!plottedClustersUncut.contains(pair[clusterIndex])) {
                        clusterSeedEnergy[NO_CUTS].fill(TriggerModule.getValueClusterSeedEnergy(pair[clusterIndex]));
                        clusterTotalEnergy[NO_CUTS].fill(TriggerModule.getValueClusterTotalEnergy(pair[clusterIndex]));
                        clusterHitCount[NO_CUTS].fill(TriggerModule.getValueClusterHitCount(pair[clusterIndex]));
                        clusterDistribution[NO_CUTS].fill(ix[clusterIndex], iy[clusterIndex]);
                        clusterTime[NO_CUTS].fill(pair[clusterIndex].getCalorimeterHits().get(0).getTime());
                        plottedClustersUncut.add(pair[clusterIndex]);
                    }
                }
                
                // Plot the cluster pair distributions.
                pairEnergySum[NO_CUTS].fill(TriggerModule.getValueEnergySum(pair));
                pairEnergyDifference[NO_CUTS].fill(TriggerModule.getValueEnergyDifference(pair));
                pairEnergySlope[NO_CUTS].fill(TriggerModule.getValueEnergySlope(pair, energySlopeParamF));
                pairCoplanarity[NO_CUTS].fill(TriggerModule.getValueCoplanarity(pair));
                pairTime[NO_CUTS].fill(pair[1].getCalorimeterHits().get(0).getTime());
                pairCoincidence[NO_CUTS].fill(TriggerModule.getValueTimeCoincidence(pair));
                pairEnergySum2D[NO_CUTS].fill(pair[0].getEnergy(), pair[1].getEnergy());
                if(pair[0].getEnergy() < pair[1].getEnergy()) {
                    pairEnergySlope2D[NO_CUTS].fill(pair[0].getEnergy(), TriggerModule.getClusterDistance(pair[0]));
                } else {
                    pairEnergySlope2D[NO_CUTS].fill(pair[1].getEnergy(), TriggerModule.getClusterDistance(pair[1]));
                }
                
                // Perform the cluster singles cuts.
                if(!(trigger.clusterHitCountCut(pair[0]) && trigger.clusterHitCountCut(pair[1]))) {
                    continue pairLoop;
                } if(!(trigger.clusterTotalEnergyCut(pair[0]) && trigger.clusterTotalEnergyCut(pair[1]))) {
                    continue pairLoop;
                } if(!(trigger.clusterSeedEnergyCut(pair[0]) && trigger.clusterSeedEnergyCut(pair[1]))) {
                    continue pairLoop;
                }
                
                // Perform the cluster pair cuts.
                if(!trigger.pairCoplanarityCut(pair)) {
                    continue pairLoop;
                } if(!trigger.pairEnergyDifferenceCut(pair)) {
                    continue pairLoop;
                } if(!trigger.pairEnergySlopeCut(pair)) {
                    continue pairLoop;
                } if(!trigger.pairEnergySumCut(pair)) {
                    continue pairLoop;
                }
                
                // Iterate over the clusters in the pair and plot the
                // cluster singles distributions.
                for(int clusterIndex = 0; clusterIndex < 2; clusterIndex++) {
                    // Only plot cluster singles distributions for
                    // clusters if they have not already been plotted.
                    // Note that this is needed because the same cluster
                    // can appear across multiple pairs.
                    if(!plottedClustersCut.contains(pair[clusterIndex])) {
                        clusterSeedEnergy[ALL_CUTS].fill(TriggerModule.getValueClusterSeedEnergy(pair[clusterIndex]));
                        clusterTotalEnergy[ALL_CUTS].fill(TriggerModule.getValueClusterTotalEnergy(pair[clusterIndex]));
                        clusterHitCount[ALL_CUTS].fill(TriggerModule.getValueClusterHitCount(pair[clusterIndex]));
                        clusterDistribution[ALL_CUTS].fill(ix[clusterIndex], iy[clusterIndex]);
                        clusterTime[ALL_CUTS].fill(pair[clusterIndex].getCalorimeterHits().get(0).getTime());
                        plottedClustersCut.add(pair[clusterIndex]);
                    }
                }
                
                // Plot the cluster pair distributions.
                pairEnergySum[ALL_CUTS].fill(TriggerModule.getValueEnergySum(pair));
                pairEnergyDifference[ALL_CUTS].fill(TriggerModule.getValueEnergyDifference(pair));
                pairEnergySlope[ALL_CUTS].fill(TriggerModule.getValueEnergySlope(pair, energySlopeParamF));
                pairCoplanarity[ALL_CUTS].fill(TriggerModule.getValueCoplanarity(pair));
                pairTime[ALL_CUTS].fill(pair[1].getCalorimeterHits().get(0).getTime());
                pairCoincidence[ALL_CUTS].fill(TriggerModule.getValueTimeCoincidence(pair));
                pairEnergySum2D[ALL_CUTS].fill(pair[0].getEnergy(), pair[1].getEnergy());
                if(pair[0].getEnergy() < pair[1].getEnergy()) {
                    pairEnergySlope2D[ALL_CUTS].fill(pair[0].getEnergy(), TriggerModule.getClusterDistance(pair[0]));
                } else {
                    pairEnergySlope2D[ALL_CUTS].fill(pair[1].getEnergy(), TriggerModule.getClusterDistance(pair[1]));
                }
                
            }
        }
    }
    
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    public void setEnergySlopeParamF(double energySlopeParamF) {
        this.energySlopeParamF = energySlopeParamF;
        trigger.setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_F, energySlopeParamF);
    }
    
    public void setSeedEnergyLow(double value) {
        trigger.setCutValue(TriggerModule.CLUSTER_SEED_ENERGY_LOW, value);
    }
    
    public void setClusterEnergyLow(double value) {
        trigger.setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW, value);
    }
    
    public void setClusterEnergyHigh(double value) {
        trigger.setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH, value);
    }
    
    public void setHitCountLow(double value) {
        trigger.setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW, value);
    }
    
    public void setEnergySumLow(double value) {
        trigger.setCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW, value);
    }
    
    public void setEnergySumHigh(double value) {
        trigger.setCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH, value);
    }
    
    public void setEnergyDifferenceHigh(double value) {
        trigger.setCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH, value);
    }
    
    public void setEnergySlopeLow(double value) {
        trigger.setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW, value);
    }
    
    public void setCoplanarityHigh(double value) {
        trigger.setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH, value);
    }
    
    public void setTimeCoincidence(double value) {
        trigger.setCutValue(TriggerModule.PAIR_TIME_COINCIDENCE, value);
    }
    
    /**
     * Creates all top/bottom pairs from the event data.
     * @param clusters - A list of clusters from which to form pairs.
     * @return Returns a <code>List</code> collection that contains
     * <code>Cluster</code> arrays of size two.
     */
    private List<Cluster[]> getClusterPairs(List<Cluster> clusters) {
        // Separate the clusters into top nad bottom clusters.
        List<Cluster> topList = new ArrayList<Cluster>();
        List<Cluster> botList = new ArrayList<Cluster>();
        for(Cluster cluster : clusters) {
            if(cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy") > 0) {
                topList.add(cluster);
            } else {
                botList.add(cluster);
            }
        }
        
        // Create all possible top/bottom cluster pairs.
        List<Cluster[]> pairList = new ArrayList<Cluster[]>();
        for(Cluster topCluster : topList) {
            for(Cluster botCluster : botList) {
                pairList.add(new Cluster[] { topCluster, botCluster });
            }
        }
        
        // Return the pairs.
        return pairList;
    }
}

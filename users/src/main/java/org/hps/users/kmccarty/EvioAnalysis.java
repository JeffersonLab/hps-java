package org.hps.users.kmccarty;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.util.ArrayList;
import java.util.List;

import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.SSPCluster;
import org.hps.record.triggerbank.SSPData;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class EvioAnalysis extends Driver {
    // Store index reference variables.
        private static final int RECON     = 0;
        private static final int SSP       = 1;
    
    // Create histogram arrays for cut distributions.
        private AIDA aida = AIDA.defaultInstance();
        private IHistogram1D[] clusterEnergyPlot = new IHistogram1D[2];
        private IHistogram1D[] clusterHitCountPlot = new IHistogram1D[2];
        private IHistogram1D[] clusterTimePlot = new IHistogram1D[2];
    private IHistogram2D[] clusterSlopePlot = new IHistogram2D[2];
    
        private IHistogram1D[] pairClusterEnergyPlot = new IHistogram1D[2];
        private IHistogram1D[] pairHitCountPlot = new IHistogram1D[2];
        private IHistogram1D[] pairTimePlot = new IHistogram1D[2];
        private IHistogram1D[] pairSumPlot = new IHistogram1D[2];
        private IHistogram2D[] pairSumEnergiesPlot = new IHistogram2D[2];
        private IHistogram1D[] pairDiffPlot = new IHistogram1D[2];
        private IHistogram1D[] pairSlopePlot = new IHistogram1D[2];
        private IHistogram1D[] pairCoplanarityPlot = new IHistogram1D[2];
        private IHistogram1D[] pairTriggerTimePlot = new IHistogram1D[2];

    // Store programmable values.
    private String clusterCollectionName = "EcalClusters";
    private String bankCollectionName = "SSPData";
    private double energySlopeParamF = 0.0055;
    private double beamEnergy = 1.1;
    
    @Override
    public void startOfData() {
        // Store the plot source type name.
        String[] plotType = new String[2];
        plotType[RECON] = " (Recon)";
        plotType[SSP] = " (SSP)";
        
        // Set the bin sizes based on the beam energy.
        int bins = (int) beamEnergy * 100;
        
        for(int i = 0; i < 2; i++) {
            // Instantiate the single cluster distribution plots.
            clusterEnergyPlot[i] = aida.histogram1D("Raw/Cluster Energy" + plotType[i], bins, 0.0, beamEnergy);
            clusterHitCountPlot[i] = aida.histogram1D("Raw/Cluster Hit Count" + plotType[i], 9, 0.5, 9.5);
            clusterTimePlot[i] = aida.histogram1D("Raw/Cluster Time" + plotType[i], 100, 0, 400);
            clusterSlopePlot[i] = aida.histogram2D("Raw/Cluster Energy Slope" + plotType[i], 300, 0.0, 3.0, 200, 0, 400);
            
            // Instantiate the cluster pair distribution plots.
            pairSumPlot[i] = aida.histogram1D("Raw/Pair Energy Sum" + plotType[i], (int) 1.5 * bins, 0.0, 1.5 * beamEnergy);
            pairSumEnergiesPlot[i] = aida.histogram2D("Raw/Pair 2D Energy Sum" + plotType[i], (int) 1.5 * bins, 0.0, 1.5 * beamEnergy, (int) 1.5 * bins, 0.0, 1.5 * beamEnergy);
            pairDiffPlot[i] = aida.histogram1D("Raw/Pair Energy Difference" + plotType[i], bins, 0.0, beamEnergy);
            pairSlopePlot[i] = aida.histogram1D("Raw/Pair Energy Slope" + plotType[i], 100, 0.0, 4.0);
            pairCoplanarityPlot[i] = aida.histogram1D("Raw/Pair Coplanarity" + plotType[i], 180, 0.0, 180);
        }
    }
    
    @Override
    public void process(EventHeader event) {
        // Skip the event if there are no clusters.
        if(!event.hasCollection(Cluster.class, clusterCollectionName) || !event.hasCollection(GenericObject.class, bankCollectionName)) {
            return;
        }
        
        // Get the list of clusters.
        List<Cluster> clusters = event.get(Cluster.class, clusterCollectionName);
        
        // Get the SSP data bank.
        List<GenericObject> bankList = event.get(GenericObject.class, bankCollectionName);
        
        // Get the SSP bank from the generic object bank list.
        SSPData sspBank = null;
        for(GenericObject obj : bankList) {
            if(AbstractIntData.getTag(obj) == SSPData.BANK_TAG) {
                sspBank = new SSPData(obj);
            }
        }
        
        // Make sure that the SSP bank was initialized.
        if(sspBank == null) {
            return;
        }
        
        // Iterate over the reconstructed clusters and populate
        // the singles plots.
        for(Cluster cluster : clusters) {
            // Get the cluster properties.
            int hitCount = cluster.getCalorimeterHits().size();
            double x = TriggerModule.getClusterX(cluster);
            double z = TriggerModule.getClusterZ(cluster);
            double slopeParamR = Math.sqrt((x * x) + (z * z));
            
            // Populate the plots.
            clusterEnergyPlot[RECON].fill(cluster.getEnergy());
            clusterHitCountPlot[RECON].fill(cluster.getCalorimeterHits().size());
            clusterTimePlot[RECON].fill(cluster.getCalorimeterHits().get(0).getTime());
            clusterSlopePlot[RECON].fill(cluster.getEnergy(), slopeParamR);
        }
        
        // Get the list of pairs.
        List<Cluster[]> pairs = makePairs(clusters);
        
        // Iterate over the pairs and populate the pair plots.
        for(Cluster[] pair : pairs) {
            pairSumPlot[RECON].fill(TriggerModule.getValueEnergySum(pair));
            pairSumEnergiesPlot[RECON].fill(pair[0].getEnergy(), pair[1].getEnergy());
            pairDiffPlot[RECON].fill(TriggerModule.getValueEnergyDifference(pair));
            pairSlopePlot[RECON].fill(TriggerModule.getValueEnergySlope(pair, energySlopeParamF));
            pairCoplanarityPlot[RECON].fill(TriggerModule.getValueCoplanarity(pair));
        }
    }
    
    private List<Cluster[]> makePairs(List<Cluster> clusters) {
        // Create seperate lists for top and bottom clusters.
        List<Cluster> topList = new ArrayList<Cluster>();
        List<Cluster> bottomList = new ArrayList<Cluster>();
        List<Cluster[]> pairList = new ArrayList<Cluster[]>();
        
        // Sort the clusters into the appropriate list.
        for(Cluster cluster : clusters) {
            if(cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy") > 0) {
                topList.add(cluster);
            } else {
                bottomList.add(cluster);
            }
        }
        
        // Create all possible cluster pairs.
        for(Cluster topCluster : topList) {
            for(Cluster bottomCluster : bottomList) {
                Cluster[] pair = { topCluster, bottomCluster };
                pairList.add(pair);
            }
        }
        
        // Return the list of cluster pairs.
        return pairList;
    }
    
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    public void setBankCollectionName(String bankCollectionName) {
        this.bankCollectionName = bankCollectionName;
    }
    
    public void setEnergySlopeParamF(double energySlopeParamF) {
        this.energySlopeParamF = energySlopeParamF;
    }
}

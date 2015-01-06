package org.hps.readout.ecal;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class OccupancyAnalysisDriver extends Driver {
	// Internal variables.
	private double scalingFactor = 0.05;
	private double seedThreshold = 0.050;
	private double beamRatio = 1.92 / 2.2;
	private double clusterThreshold = 0.200;
    private AIDA aida = AIDA.defaultInstance();
	private boolean ignoreBeamGapRows = false;
	
    // LCIO Collection Names
    private String clusterCollectionName = "EcalClusters";
    private String hitCollectionName = "EcalCorrectedHits";
    
	// Trigger plots.
    IHistogram2D occupancyDistribution;
    IHistogram2D[] clusterDistribution = new IHistogram2D[2];
    IHistogram1D[] clusterHitDistribution = new IHistogram1D[2];
    IHistogram1D[] totalEnergyDistribution = new IHistogram1D[2];
    IHistogram1D[] clusterEnergyDistribution = new IHistogram1D[2];
    
    public void setIgnoreBeamGapRows(boolean ignoreBeamGapRows) {
    	this.ignoreBeamGapRows = ignoreBeamGapRows;
    }
    
    public void setBeamRatio(double beamRatio) {
    	this.beamRatio = beamRatio;
    }
    
    public void setScalingFactor(double scalingFactor) {
    	this.scalingFactor = scalingFactor;
    }
    
    public void setSeedThreshold(double seedThreshold) {
    	this.seedThreshold = seedThreshold;
    }
    
    public void setClusterThreshold(double clusterThreshold) {
    	this.clusterThreshold = clusterThreshold;
    }
    
    public void setClusterCollectionName(String clusterCollectionName) {
    	this.clusterCollectionName = clusterCollectionName;
    }
    
    public void setHitCollectionName(String hitCollectionName) {
    	this.hitCollectionName = hitCollectionName;
    }
    
    @Override
    public void process(EventHeader event) {
    	// If clusters are present, process them.
    	if(event.hasCollection(Cluster.class, clusterCollectionName)) {
    		// Get the list of clusters.
    		List<Cluster> clusterList = event.get(Cluster.class, clusterCollectionName);
    		
    		// Use the clusters to populate the cluster plots.
    		for(Cluster cluster : clusterList) {
    			// Get the ix and iy values for the cluster.
    			int ix = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
    			int iy = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
    			
    			// If we want to ignore the beam gap rows, make sure
    			// that iy exceeds two.
    			if(!ignoreBeamGapRows || (Math.abs(iy) > 2)) {
	        		// If the cluster passes the seed threshold, place it in
	        		// the level 1 plots.
	    			if(cluster.getCalorimeterHits().get(0).getCorrectedEnergy() >= seedThreshold) {
	    				clusterDistribution[0].fill(ix, iy, scalingFactor);
	    				clusterHitDistribution[0].fill(cluster.getCalorimeterHits().size(), scalingFactor);
	    				clusterEnergyDistribution[0].fill(cluster.getEnergy() * beamRatio, scalingFactor);
	    			}
	    			
	    			// If the cluster energy passes the cluster threshold,
	    			// populate the level 2 plots.
	    			if(cluster.getEnergy() >= clusterThreshold) {
	    				clusterDistribution[1].fill(ix, iy, scalingFactor);
	    				clusterHitDistribution[1].fill(cluster.getCalorimeterHits().size(), scalingFactor);
	    				clusterEnergyDistribution[1].fill(cluster.getEnergy() * beamRatio, scalingFactor);
	    			}
    			}
    		}
    	}
    	
    	// If the event has hits, process them.
    	if(event.hasCollection(CalorimeterHit.class, hitCollectionName)) {
    		// Get the list of hits.
    		List<CalorimeterHit> hitList = event.get(CalorimeterHit.class, hitCollectionName);
    		
    		// Track the energy in the top and bottom of the calorimeter.
    		double[] energy = { 0.0, 0.0 };
    		
    		// Iterate over the hits.
    		for(CalorimeterHit hit : hitList) {
    			// Get the ix and iy values.
    			int ix = hit.getIdentifierFieldValue("ix");
    			int iy = hit.getIdentifierFieldValue("iy");
    			
    			// If we want to ignore beam gap rows, ensure that iy
    			// is greater than 2.
    			if(!ignoreBeamGapRows || Math.abs(iy) > 2) {
	    			// Add the energy to the appropriate energy tracking
	    			// variable for the calorimeter halves.
	    			if(iy > 0) { energy[0] += hit.getCorrectedEnergy() * beamRatio; }
	    			else { energy[1] += hit.getCorrectedEnergy() * beamRatio; }
	    			
	    			// Populate the occupancy distribution.
	    			occupancyDistribution.fill(ix, iy, scalingFactor);
    			}
    		}
			
			// Populate the total calorimeter energy plot.
			totalEnergyDistribution[0].fill(energy[0], scalingFactor);
			totalEnergyDistribution[1].fill(energy[1], scalingFactor);
    	}
    }
    
    @Override
    public void startOfData() {
    	// Define the cluster distribution plots.
    	String[] clusterDistName = { String.format("Comp Plots :: Cluster Seed Distribution [Seed Threshold %.3f GeV]", seedThreshold),
    			String.format("Comp Plots :: Cluster Seed Distribution [Cluster Threshold %.3f GeV]", clusterThreshold) };
        clusterDistribution[0] = aida.histogram2D(clusterDistName[0], 46, -23, 23, 11, -5.5, 5.5);
        clusterDistribution[1] = aida.histogram2D(clusterDistName[1], 46, -23, 23, 11, -5.5, 5.5);
        
    	// Define the occupancy distribution plots.
    	String occupancyDistName = String.format("Comp Plots :: Crystal Occupancy");
    	occupancyDistribution = aida.histogram2D(occupancyDistName, 46, -23, 23, 11, -5.5, 5.5);
    	
        // Define the cluster hit count distribution.
    	String[] clusterHitDistName = { String.format("Comp Plots :: Cluster Hit Count Distribution [Seed Threshold %.3f GeV]", seedThreshold),
    			String.format("Comp Plots :: Cluster Hit Count Distribution [Cluster Threshold %.3f GeV]", clusterThreshold) };
    	clusterHitDistribution[0] = aida.histogram1D(clusterHitDistName[0], 9, 1, 10);
    	clusterHitDistribution[1] = aida.histogram1D(clusterHitDistName[1], 9, 1, 10);
    	
        // Define the cluster total energy distribution.
    	String[] clusterEnergyDistName = { String.format("Comp Plots :: Cluster Total Energy Distribution [Seed Threshold %.3f GeV]", seedThreshold),
    			String.format("Comp Plots :: Cluster Total Energy Distribution [Cluster Threshold %.3f GeV]", clusterThreshold) };
    	clusterEnergyDistribution[0] = aida.histogram1D(clusterEnergyDistName[0], 176, 0.0, 2.2);
    	clusterEnergyDistribution[1] = aida.histogram1D(clusterEnergyDistName[1], 176, 0.0, 2.2);
    	
        // Define the calorimeter total energy distribution.
    	String[] totalEnergyDistName = { String.format("Comp Plots :: Calorimeter Event Energy Distribution [Top]"),
    			String.format("Comp Plots :: Calorimeter Event Energy Distribution [Bottom]") };
    	totalEnergyDistribution[0] = aida.histogram1D(totalEnergyDistName[0], 500, 0.0, 10.0);
    	totalEnergyDistribution[1] = aida.histogram1D(totalEnergyDistName[1], 500, 0.0, 10.0);
    }
}

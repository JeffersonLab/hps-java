package org.hps.analysis.ecal;

import static org.hps.recon.ecal.ECalUtils.maxVolt;
import static org.hps.recon.ecal.ECalUtils.nBit;
import hep.aida.IHistogram2D;

import java.util.ArrayList;
import java.util.List;

import org.hps.readout.ecal.TriggerDriver;
import org.hps.recon.ecal.ECalUtils;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Diagnostic plots for HPS ECal.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: HPSEcalTriggerPlotsDriver.java,v 1.7 2013/02/25 22:39:26 meeg
 * Exp $
 */
public class HPSEcalTriggerPlotsDriver extends Driver {
	// LCSim collection names.
    String ecalCollectionName = "EcalHits";
    String clusterCollectionName = "EcalClusters";
    
    // Histogram factory.
    AIDA aida = AIDA.defaultInstance();
    
    // Energy cuts for hit histograms (in MeV).
    double energyCut[] = { 0, 1, 2, 5, 10, 20, 50, 100, 200, 500, 1000 };
    
    // Energy cut histograms.
    IHistogram2D hitXYPlot[] = new IHistogram2D[energyCut.length];
    
    // Additional histograms.
    IHistogram2D crystalDeadTime;
    IHistogram2D clusterHitXYPlot;
    IHistogram2D seedHitXYPlot;
    IHistogram2D triggerClusterHitXYPlot;
    IHistogram2D triggerSeedHitXYPlot;
    
    private double tp = 6.95;
    private double threshold = 10; //ADC counts

    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }

    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    public void startOfData() {
    	// Initialize a hit histogram for each declared energy.
    	for(int e = 0; e < energyCut.length; e++) {
    		hitXYPlot[e] = aida.histogram2D("Trigger Plots: " + ecalCollectionName +
    				" : Hits above " + energyCut[e] + " MeV", 46, -23, 23, 11, -5.5, 5.5);
    	}
    	// Initialize the remaining plots.
        crystalDeadTime = aida.histogram2D("Trigger Plots: " + ecalCollectionName +
        		" : Crystal dead time", 46, -23, 23, 11, -5.5, 5.5);
        clusterHitXYPlot = aida.histogram2D("Trigger Plots: " + clusterCollectionName +
        		" : Crystals in clusters", 47, -23.5, 23.5, 11, -5.5, 5.5);
        seedHitXYPlot = aida.histogram2D("Trigger Plots: " + clusterCollectionName +
        		" : Seed hits", 47, -23.5, 23.5, 11, -5.5, 5.5);
        triggerClusterHitXYPlot = aida.histogram2D("Trigger Plots: " + clusterCollectionName +
                " : Crystals in clusters, with trigger", 47, -23.5, 23.5, 11, -5.5, 5.5);
        triggerSeedHitXYPlot = aida.histogram2D("Trigger Plots: " + clusterCollectionName +
        		" : Seed hits, with trigger", 47, -23.5, 23.5, 11, -5.5, 5.5);
    }
    
    public void process(EventHeader event) {
    	// If the current event has the indicated hit collection,
    	// use it as the hit list.
    	List<CalorimeterHit> hits;
    	if(event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
    		hits = event.get(CalorimeterHit.class, ecalCollectionName);
    	}
    	// If it does not, then use an empty list to avoid crashing.
    	else { hits = new ArrayList<CalorimeterHit>(0); }
    	
    	// If the current event has the indicated cluster collection,
    	// use it as the cluster list.
    	List<Cluster> clusters;
    	if(event.hasCollection(Cluster.class, clusterCollectionName)) {
    		clusters = event.get(Cluster.class, clusterCollectionName);
    	}
    	// If it does not, then use an empty list to avoid crashing.
    	else { clusters = new ArrayList<Cluster>(0); }
        
        // Populate hit plots.
        for (CalorimeterHit hit : hits) {
        	// Get the hit crystal position.
            int ix = hit.getIdentifierFieldValue("ix");
            int iy = hit.getIdentifierFieldValue("iy");
            double energy = hit.getRawEnergy();
            
            // Loop through the energy plots and fill them if the hit
            // is over the current energy threshold/
            for(int e = 0; e < energyCut.length; e++) {
            	if(energy > energyCut[e] * ECalUtils.MeV) {
            		hitXYPlot[e].fill(ix - 0.5 * Math.signum(ix), iy);
            	}
            }
            
            // Generate the dead time plot.
            double deadTime = 0;
            for (int time = 0; time < 500; time++) {
                if (hit.getRawEnergy() * pulseAmplitude(time) > threshold) { deadTime += 1e-6; } // units of milliseconds
                else if (time > 2 * tp || deadTime != 0) { break; }
            }
            crystalDeadTime.fill(ix - 0.5 * Math.signum(ix), iy, deadTime);
        }
        
        // Check the trigger bit.
        boolean trigger = TriggerDriver.triggerBit();
        
        // Populate cluster based plots.
        for (Cluster cluster : clusters) {
        	// Get the cluster's seed hit position.
        	CalorimeterHit seed = cluster.getCalorimeterHits().get(0);
            int ix = seed.getIdentifierFieldValue("ix");
            int iy = seed.getIdentifierFieldValue("iy");
            
            // Fill the seed hit plot.
            seedHitXYPlot.fill(ix, iy);
            
            // If the trigger bit is set, add the seed to the trigger
            // plot histogram as well.
            if(trigger) { triggerSeedHitXYPlot.fill(ix, iy); }
            
            // Populate the component hit histogram.
            for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
            	// Get the component hit location.
                ix = hit.getIdentifierFieldValue("ix");
                iy = hit.getIdentifierFieldValue("iy");
                
                // Add it to the plot.
                clusterHitXYPlot.fill(ix, iy);
                
                // If the trigger bit is set, add the component to
                // the trigger cluster histogram too.
                if(trigger) { triggerClusterHitXYPlot.fill(ix, iy); }
            }
        }
    }
    
    private double pulseAmplitude(double time) {
        if (time <= 0.0) { return 0.0; }
        return ECalUtils.readoutGain * ((time * time / (2 * tp * tp * tp)) * Math.exp(-time / tp)) * ((Math.pow(2, nBit) - 1) / maxVolt);
    }
}

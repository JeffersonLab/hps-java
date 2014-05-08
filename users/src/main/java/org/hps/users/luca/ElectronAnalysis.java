/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.hps.users.luca;

import hep.aida.IHistogram1D;

import java.util.List;

import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * leggo le informazioni sugli e- coulombiani. speriamo bene
 * @author Luca
 */
public class ElectronAnalysis extends Driver {
    //dichiaro la classe per i grafici e quelle per la ricostruzione qui invece che nello steering
    private AIDA aida = AIDA.defaultInstance();
    IHistogram1D enePlot = aida.histogram1D("energia", 1000, 0, 3);
    IHistogram1D enePlotcorre = aida.histogram1D("energiacorretta", 1000, 0, 3);
    
	// FADCEcalReadoutDriver readoutDriver= new FADCEcalReadoutDriver();
	// EcalRawConverterDriver converterDriver = new EcalRawConverterDriver();
	// EcalClusterer ecalClusterer = new EcalClusterer();
    // da qui vanno definiti gli istogrammi
    
    
    //start of data
    @Override
    public void startOfData() {
    	// ecalClusterer.setEcalName("Ecal");
    	// ecalClusterer.setEcalCollectionName("EcalCorrectedHits");
        super.startOfData();
    }
    
    
    
    //inizio processo eventi
    @Override
    public void process(EventHeader event) {
    	// Make sure that the event contains a hit collection.
    	if(event.hasCollection(CalorimeterHit.class, "EcalHits")) {
    		// Get the hit collection.
	    	List<CalorimeterHit> hits = event.get(CalorimeterHit.class, "EcalHits");
	    	
	    	// Generate histograms from the hits which exceed 2 GeV.
	    	for(CalorimeterHit hit : hits){
	    		if(hit.getRawEnergy() > 2){
	    			enePlot.fill(hit.getRawEnergy());
	    			enePlotcorre.fill(hit.getCorrectedEnergy());
	    		 }
	    	}
    	}
    	
    	// Make sure that the event contains a cluster collection.
    	if(event.hasCollection(HPSEcalCluster.class, "EcalClusters")) {
    		// Indicate that there are clusters.
    		System.out.println("Event contains clusters.");
    		
    		// Get the cluster collection.
	    	List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, "EcalClusters");
	    	
	    	// Print the seed hits for the clusters.
	    	for(HPSEcalCluster cluster : clusters){
	    		System.out.printf("\t%d%n", cluster.getSeedHit().getCellID());
	    	}
	    	System.out.println("\n");
    	}
    	
    	// Otherwise, indicate that there are no clusters.
    	else { System.out.println("Event does not contain clusters.\n"); }
    }
}

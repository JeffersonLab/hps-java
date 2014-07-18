package org.hps.users.kmccarty;

import java.util.List;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class FADCAnalysisDriver extends Driver {
	// Analysis plots.
    AIDA aida = AIDA.defaultInstance();
	IHistogram1D rawHitEnergy;
	IHistogram1D fadcHitEnergy;
	IHistogram2D rawHitDistribution;
	IHistogram2D fadcHitDistribution;
	IHistogram2D fadcFilteredHitDistribution;
	IHistogram1D eventRawHitCount;
	IHistogram1D eventFADCHitCount;
	
	// Hit collection names.
	private String rawHitsCollectionName = "EcalHits";
	private String fadcHitsCollectionName = "EcalCorrectedHits";
	
	public void setFadcHitsCollectionName(String fadcHitsCollectionName) {
		this.fadcHitsCollectionName = fadcHitsCollectionName;
	}
	
	public void setRawHitsCollectionName(String rawHitsCollectionName) {
		this.rawHitsCollectionName = rawHitsCollectionName;
	}
	
	@Override
	public void startOfData() {
		// Initialize the histograms.
		rawHitEnergy = aida.histogram1D("FADC Plot :: Raw Hit Energy", 110, 0.00, 2.2);
		fadcHitEnergy = aida.histogram1D("FADC Plot :: FADC Hit Energy", 80, 0.00, 1.6);
		rawHitDistribution = aida.histogram2D("FADC Plot :: Raw Hit Distribution", 46, -23, 23, 11, -5.5, 5.5);
		fadcHitDistribution = aida.histogram2D("FADC Plot :: FADC Hit Distribution", 46, -23, 23, 11, -5.5, 5.5);
		fadcFilteredHitDistribution = aida.histogram2D("FADC Plot :: FADC Hit Distribution Over 100 MeV", 46, -23, 23, 11, -5.5, 5.5);
		eventRawHitCount = aida.histogram1D("FADC Plot :: Event Raw Hit Count", 159, 1, 160);
		eventFADCHitCount = aida.histogram1D("FADC Plot :: Event FADC Hit Count", 15, 1, 16);
	}
	
	public void process(EventHeader event) {
		// Check if there exists a raw hits collection.
		if(event.hasCollection(CalorimeterHit.class, rawHitsCollectionName)) {
			// Get the raw hit collection.
			List<CalorimeterHit> hitList = event.get(CalorimeterHit.class, rawHitsCollectionName);
			
			// Output the information on each hit to the histograms.
			for(CalorimeterHit hit : hitList) {
				// Get the x and y indices for the hits.
				int ix = hit.getIdentifierFieldValue("ix");
				int iy = hit.getIdentifierFieldValue("iy");
				if(ix > 0) { ix = ix - 1; }
				
				// Write to the histograms.
				rawHitEnergy.fill(hit.getCorrectedEnergy());
				rawHitDistribution.fill(ix, iy, 1.0);
				
				// If there are hits, fill the hit count histogram.
				if(hitList.size() != 0) { eventRawHitCount.fill(hitList.size()); }
			}
		}
		
		// Check if there exists an FADC hits collection.
		if(event.hasCollection(CalorimeterHit.class, fadcHitsCollectionName)) {
			// Get the raw hit collection.
			List<CalorimeterHit> hitList = event.get(CalorimeterHit.class, fadcHitsCollectionName);
			
			// Output the information on each hit to the histograms.
			for(CalorimeterHit hit : hitList) {
				// Get the x and y indices for the hits.
				int ix = hit.getIdentifierFieldValue("ix");
				int iy = hit.getIdentifierFieldValue("iy");
				if(ix > 0) { ix = ix - 1; }
				
				// Write to the histograms.
				fadcHitEnergy.fill(hit.getCorrectedEnergy());
				fadcHitDistribution.fill(ix, iy, 1.0);
				if(hit.getCorrectedEnergy() > 0.100) { fadcFilteredHitDistribution.fill(ix, iy, 1.0); }
				
				// If there are hits, fill the hit count histogram.
				if(hitList.size() != 0) { eventFADCHitCount.fill(hitList.size()); }
			}
		}
	}
}

package org.hps.users.kmccarty;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.util.List;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class ClusterAnalysisDriver extends Driver {
	// Analysis plots.
    AIDA aida = AIDA.defaultInstance();
	IHistogram1D clusterTotalEnergy;
	IHistogram1D clusterSeedEnergy;
	IHistogram1D clusterHitCount;
	IHistogram2D clusterDistribution;
	
	IHistogram1D fClusterTotalEnergy;
	IHistogram1D fClusterSeedEnergy;
	IHistogram1D fClusterHitCount;
	IHistogram2D fClusterDistribution;
	
	IHistogram1D nClusterTotalEnergy;
	IHistogram1D nClusterSeedEnergy;
	IHistogram1D nClusterHitCount;
	IHistogram2D nClusterDistribution;
	
	// Hit collection names.
	private String clusterCollectionName = "EcalClusters";
	
	public void setClusterCollectionName(String clusterCollectionName) {
		this.clusterCollectionName = clusterCollectionName;
	}
	
	@Override
	public void startOfData() {
		// Initialize the histograms.
		clusterTotalEnergy = aida.histogram1D("Cluster Plot :: Cluster Total Energy", 110, 0.00, 2.2);
		clusterSeedEnergy = aida.histogram1D("Cluster Plot :: Seed Hit Energy", 110, 0.00, 2.2);
		clusterHitCount = aida.histogram1D("Cluster Plot :: Cluster Hit Count", 8, 1, 9);
		clusterDistribution = aida.histogram2D("Cluster Plot :: Seed Hit Distribution", 46, -23, 23, 11, -5.5, 5.5);
		
		// Initialize the filtered histograms.
		fClusterTotalEnergy = aida.histogram1D("Cluster Plot :: Cluster Total Energy (Over 100 MeV)", 110, 0.00, 2.2);
		fClusterSeedEnergy = aida.histogram1D("Cluster Plot :: Seed Hit Energy (Over 100 MeV)", 110, 0.00, 2.2);
		fClusterHitCount = aida.histogram1D("Cluster Plot :: Cluster Hit Count (Over 100 MeV)", 8, 1, 9);
		fClusterDistribution = aida.histogram2D("Cluster Plot :: Seed Hit Distribution (Over 100 MeV)", 46, -23, 23, 11, -5.5, 5.5);
		
		// Initialize the more filtered histograms.
		nClusterTotalEnergy = aida.histogram1D("Cluster Plot :: Cluster Total Energy (Over 100 MeV, > 1 Hit)", 110, 0.00, 2.2);
		nClusterSeedEnergy = aida.histogram1D("Cluster Plot :: Seed Hit Energy (Over 100 MeV, > 1 Hit)", 110, 0.00, 2.2);
		nClusterHitCount = aida.histogram1D("Cluster Plot :: Cluster Hit Count (Over 100 MeV, > 1 Hit)", 8, 1, 9);
		nClusterDistribution = aida.histogram2D("Cluster Plot :: Seed Hit Distribution (Over 100 MeV, > 1 Hit)", 46, -23, 23, 11, -5.5, 5.5);
	}
	
	public void process(EventHeader event) {
		// Check if there exists a cluster collection.
		if(event.hasCollection(Cluster.class, clusterCollectionName)) {
			// Get the raw hit collection.
			List<Cluster> clusterList = event.get(Cluster.class, clusterCollectionName);
			
			// Output the information on each hit to the histograms.
			for(Cluster cluster : clusterList) {
				// Get the x and y indices for the hits.
				int ix = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
				int iy = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
				if(ix > 0) { ix = ix - 1; }
				
				// Write to the histograms.
				clusterTotalEnergy.fill(cluster.getEnergy());
				clusterSeedEnergy.fill(cluster.getCalorimeterHits().get(0).getCorrectedEnergy());
				clusterHitCount.fill(cluster.getCalorimeterHits().size());
				clusterDistribution.fill(ix, iy, 1.0);
				
				if(cluster.getCalorimeterHits().get(0).getCorrectedEnergy() > 0.100) {
					fClusterTotalEnergy.fill(cluster.getEnergy());
					fClusterSeedEnergy.fill(cluster.getCalorimeterHits().get(0).getCorrectedEnergy());
					fClusterHitCount.fill(cluster.getCalorimeterHits().size());
					fClusterDistribution.fill(ix, iy, 1.0);
					
					if(cluster.getCalorimeterHits().size() > 1) {
						nClusterTotalEnergy.fill(cluster.getEnergy());
						nClusterSeedEnergy.fill(cluster.getCalorimeterHits().get(0).getCorrectedEnergy());
						nClusterHitCount.fill(cluster.getCalorimeterHits().size());
						nClusterDistribution.fill(ix, iy, 1.0);
					}
				}
			}
		}
	}
}

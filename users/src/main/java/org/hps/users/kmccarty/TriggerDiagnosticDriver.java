package org.hps.users.kmccarty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hps.readout.ecal.TriggerModule;
import org.hps.readout.ecal.triggerbank.AbstractIntData;
import org.hps.readout.ecal.triggerbank.SSPCluster;
import org.hps.readout.ecal.triggerbank.SSPData;
import org.hps.readout.ecal.triggerbank.TIData;
import org.hps.recon.ecal.CalorimeterHitUtilities;
import org.lcsim.detector.converter.compact.EcalCrystal;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

public class TriggerDiagnosticDriver extends Driver {
	// Store the LCIO collection names for the needed objects.
	private String bankCollectionName = "TriggerBank";
	private String clusterCollectionName = "EcalClusters";
	
	// Store the lists of parsed objects.
	private TIData tiBank;
	private SSPData sspBank;
	private List<Cluster> reconClusters;
	private List<SSPCluster> sspClusters;
	private List<List<PairTrigger<Cluster>>> reconPairsTriggers = new ArrayList<List<PairTrigger<Cluster>>>(2);
	private List<List<PairTrigger<SSPCluster>>> sspPairsTriggers = new ArrayList<List<PairTrigger<SSPCluster>>>(2);
	private List<List<SinglesTrigger<Cluster>>> reconSinglesTriggers = new ArrayList<List<SinglesTrigger<Cluster>>>(2);
	private List<List<SinglesTrigger<SSPCluster>>> sspSinglesTriggers = new ArrayList<List<SinglesTrigger<SSPCluster>>>(2);
	
	// Trigger modules for performing trigger analysis.
	private TriggerModule[] singlesTrigger = new TriggerModule[2];
	private TriggerModule[] pairsTrigger = new TriggerModule[2];
	
	// Store internal variables.
	private double energyAcceptance = 0.05;
	
	/*
	@Override
	public void detectorChanged(Detector detector) {
		for(EcalCrystal crystal : detector.getSubdetector("Ecal").getDetectorElement().findDescendants(EcalCrystal.class)) {
			System.out.println(crystal.getIdentifier().getValue());
			CalorimeterHit tempHit = CalorimeterHitUtilities.create(1.000, 10.0, crystal.getIdentifier().getValue());
			
			int ix = tempHit.getIdentifierFieldValue("ix");
			int iy = tempHit.getIdentifierFieldValue("iy");
			double[] xyz = tempHit.getPosition();
			
			System.out.printf("(%3d, %3d) --> (%.2f, %.2f)%n", ix, iy, xyz[0], xyz[1]);
		}
	}
	*/
	
	/**
	 * Define the trigger modules. This should be replaced by parsing
	 * the DAQ configuration at some point.
	 */
	@Override
	public void startOfData() {
		// Define the first singles trigger.
		singlesTrigger[0] = new TriggerModule();
		singlesTrigger[0].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW, 0.500);
		singlesTrigger[0].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH, 8.191);
		singlesTrigger[0].setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW, 0);
		
		// Define the second singles trigger.
		singlesTrigger[1] = new TriggerModule();
		singlesTrigger[1].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW, 0.000);
		singlesTrigger[1].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH, 8.191);
		singlesTrigger[1].setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW, 0);
		
		// Define the first pairs trigger.
		pairsTrigger[0] = new TriggerModule();
		pairsTrigger[0].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW, 0.000);
		pairsTrigger[0].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH, 8.191);
		pairsTrigger[0].setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW, 0);
		pairsTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW, 0.000);
		pairsTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH, 8.191);
		pairsTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH, 8.191);
		pairsTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW, 0.000);
		pairsTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_F, 0.001);
		pairsTrigger[0].setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH, 180);
		
		// Define the second pairs trigger.
		pairsTrigger[1] = new TriggerModule();
		pairsTrigger[1].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW, 0.000);
		pairsTrigger[1].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH, 8.191);
		pairsTrigger[1].setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW, 0);
		pairsTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW, 0.000);
		pairsTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH, 8.191);
		pairsTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH, 8.191);
		pairsTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW, 0.000);
		pairsTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_F, 0.001);
		pairsTrigger[1].setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH, 180);
		
		// Instantiate the triggers lists.
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			reconPairsTriggers.add(new ArrayList<PairTrigger<Cluster>>());
			sspPairsTriggers.add(new ArrayList<PairTrigger<SSPCluster>>());
			reconSinglesTriggers.add(new ArrayList<SinglesTrigger<Cluster>>());
			sspSinglesTriggers.add(new ArrayList<SinglesTrigger<SSPCluster>>());
		}
	}
	
	/**
	 * Gets the banks and clusters from the event.
	 */
	@Override
	public void process(EventHeader event) {
		// Get the reconstructed clusters.
		if(event.hasCollection(Cluster.class, clusterCollectionName)) {
			reconClusters = event.get(Cluster.class, clusterCollectionName);
		}
		
		// Get the SSP clusters.
		if(event.hasCollection(GenericObject.class, bankCollectionName)) {
			// Get the bank list.
			List<GenericObject> bankList = event.get(GenericObject.class, bankCollectionName);
			
			// Search through the banks and get the SSP and TI banks.
			for(GenericObject obj : bankList) {
				// If this is an SSP bank, parse it.
				if(AbstractIntData.getTag(obj) == SSPData.BANK_TAG) {
					sspBank = new SSPData(obj);
				}
				
				// Otherwise, if this is a TI bank, parse it.
				else if(AbstractIntData.getTag(obj) == TIData.BANK_TAG) {
					tiBank = new TIData(obj);
				}
			}
			
			// If there is an SSP bank, get the list of SSP clusters.
			if(sspBank != null) {
				sspClusters = sspBank.getClusters();
			}
		}
		
		// Check that all of the collections and objects are present.
		boolean allPresent = true;
		if(sspBank == null) {
			System.out.println("SSP bank not found!");
			allPresent = false;
		} if(tiBank == null) {
			System.out.println("TI bank not found!");
			allPresent = false;
		} if(sspClusters == null) {
			System.out.println("SSP clusters not found!");
			allPresent = false;
		} if(reconClusters == null) {
			System.out.println("Reconstructed clusters not found!");
			allPresent = false;
		}
		
		// Do nothing further if an object is missing.
		if(!allPresent) { return; }
		
		// Otherwise, print out the two cluster collections.
		System.out.printf("Summary for Event %d at time %d%n", event.getEventNumber(), event.getTimeStamp());
		System.out.println("Reconstructed Clusters:");
		for(Cluster cluster : reconClusters) {
			System.out.println("\t" + reconClusterToString(cluster));
		}
		
		System.out.println("SSP Clusters:");
		for(SSPCluster cluster : sspClusters) {
			System.out.println("\t" + sspClusterToString(cluster));
		}
		
		// Perform the cluster verification step.
		verifyClusters();
		
		// Construct lists of triggers for the SSP clusters and the
		// reconstructed clusters.
		constructTriggers();
		
		System.out.println("\n\n");
	}
	
	private void verifyClusters() {
		// Track which clusters match and whether a given cluster
		// has been matched or not.
		Set<Cluster> reconClusterSet = new HashSet<Cluster>(reconClusters.size());
		Set<SSPCluster> sspClusterSet = new HashSet<SSPCluster>(sspClusters.size());
		Map<Cluster, SSPCluster> pairMap = new HashMap<Cluster, SSPCluster>(reconClusters.size());
		
		// Iterate over the reconstructed clusters and check whether
		// there is a matching SSP cluster or not.
		for(Cluster reconCluster : reconClusters) {
			// Get the cluster's x- and y- indices.
			int ix = reconCluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
			int iy = reconCluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
			
			// Look for an unmatched cluster with the same indices.
			matchLoop:
			for(SSPCluster sspCluster : sspClusters) {
				// TODO: Implement time cut on cluster matching.
				if(sspCluster.getXIndex() == ix && sspCluster.getYIndex() == iy) {
					// If this SSP cluster is already matched, it can
					// not be used again.
					if(sspClusterSet.contains(sspCluster)) {
						continue matchLoop;
					}
					
					// We also require that the SSP cluster reports an
					// energy within a certain percentage of the recon
					// cluster.
					double[] energy = { sspCluster.getEnergy() * (1 - energyAcceptance),
							sspCluster.getEnergy() * (1 + energyAcceptance) };
					
					// If the energies are within range, consider this
					// a matched cluster pair. They must also have the
					// same hit count.
					// TODO: Fix hit inconsistency bug.
					if(reconCluster.getEnergy() >= energy[0] && reconCluster.getEnergy() <= energy[1]
							&& reconCluster.getCalorimeterHits().size() == sspCluster.getHitCount()) {
						// Add the two clusters to the matched sets.
						sspClusterSet.add(sspCluster);
						reconClusterSet.add(reconCluster);
						
						// Map the two clusters together.
						pairMap.put(reconCluster, sspCluster);
						
						// Skip to the next recon cluster.
						continue matchLoop;
					}
				}
			}
		} // End matchLoop
		
		// Output the cluster matches and note which clusters failed
		// to be paired. These may suggest an error.
		System.out.println("Matched Clusters:");
		for(Entry<Cluster, SSPCluster> pair : pairMap.entrySet()) {
			System.out.printf("\t%s --> %s%n", reconClusterToString(pair.getKey()), sspClusterToString(pair.getValue()));
		}
		System.out.println("Unmatched Clusters:");
		for(SSPCluster sspCluster : sspClusters){
			if(!sspClusterSet.contains(sspCluster)) {
				System.out.printf("\tSSP   :: %s%n", sspClusterToString(sspCluster));
			}
		}
		for(Cluster reconCluster : reconClusters){
			if(!reconClusterSet.contains(reconCluster)) {
				System.out.printf("\tRecon :: %s%n", reconClusterToString(reconCluster));
			}
		}
	}
	
	private void constructTriggers() {
		// Run the SSP clusters through the singles trigger to determine
		// whether they pass it or not.
		for(SSPCluster cluster : sspClusters) {
			for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
				// For a cluster to have formed it is assumed to have passed
				// the cluster seed energy cuts. This can not be verified
				// since the SSP bank does not report individual hit. 
				boolean passSeedLow = true;
				boolean passSeedHigh = true;
				
				// The remaining cuts may be acquired from trigger module.
				boolean passClusterLow = singlesTrigger[triggerNum].clusterTotalEnergyCutLow(cluster);
				boolean passClusterHigh = singlesTrigger[triggerNum].clusterTotalEnergyCutHigh(cluster);
				boolean passHitCount = singlesTrigger[triggerNum].clusterHitCountCut(cluster);
				
				// Make a trigger to store the results.
				SinglesTrigger<SSPCluster> trigger = new SinglesTrigger<SSPCluster>(cluster);
				trigger.setStateSeedEnergyLow(passSeedLow);
				trigger.setStateSeedEnergyHigh(passSeedHigh);
				trigger.setStateClusterEnergyLow(passClusterLow);
				trigger.setStateClusterEnergyHigh(passClusterHigh);
				trigger.setStateHitCount(passHitCount);
				
				// Store the trigger.
				sspSinglesTriggers.get(triggerNum).add(trigger);
			}
		}
		
		// Run the reconstructed clusters through the singles trigger
		// to determine whether they pass it or not.
		for(Cluster cluster : reconClusters) {
			for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
				// For a cluster to have formed it is assumed to have passed
				// the cluster seed energy cuts. This can not be verified
				// since the SSP bank does not report individual hit. 
				boolean passSeedLow = true;
				boolean passSeedHigh = true;
				
				// The remaining cuts may be acquired from trigger module.
				boolean passClusterLow = singlesTrigger[triggerNum].clusterTotalEnergyCutLow(cluster);
				boolean passClusterHigh = singlesTrigger[triggerNum].clusterTotalEnergyCutHigh(cluster);
				boolean passHitCount = singlesTrigger[triggerNum].clusterHitCountCut(cluster);
				
				// Make a trigger to store the results.
				SinglesTrigger<Cluster> trigger = new SinglesTrigger<Cluster>(cluster);
				trigger.setStateSeedEnergyLow(passSeedLow);
				trigger.setStateSeedEnergyHigh(passSeedHigh);
				trigger.setStateClusterEnergyLow(passClusterLow);
				trigger.setStateClusterEnergyHigh(passClusterHigh);
				trigger.setStateHitCount(passHitCount);
				
				// Store the trigger.
				reconSinglesTriggers.get(triggerNum).add(trigger);
			}
		}
	}
	
	private static final String reconClusterToString(Cluster cluster) {
		return String.format("Cluster at (%3d, %3d) with %.3f GeV and %d hits at %4.0f ns.",
				cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix"),
				cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy"),
				cluster.getEnergy(), cluster.getCalorimeterHits().size(),
				cluster.getCalorimeterHits().get(0).getTime());
	}
	
	private static final String sspClusterToString(SSPCluster cluster) {
		return String.format("Cluster at (%3d, %3d) with %.3f GeV and %d hits at %4d ns.",
				cluster.getXIndex(), cluster.getYIndex(), cluster.getEnergy(),
				cluster.getHitCount(), cluster.getTime());
	}
}
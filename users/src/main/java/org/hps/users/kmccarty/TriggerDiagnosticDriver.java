package org.hps.users.kmccarty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;

import org.hps.readout.ecal.TriggerModule;
import org.hps.readout.ecal.triggerbank.AbstractIntData;
import org.hps.readout.ecal.triggerbank.SSPCluster;
import org.hps.readout.ecal.triggerbank.SSPData;
import org.hps.readout.ecal.triggerbank.SSPSinglesTrigger;
import org.hps.readout.ecal.triggerbank.SSPTrigger;
import org.hps.readout.ecal.triggerbank.TIData;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;
import org.lcsim.util.log.LogUtil;

public class TriggerDiagnosticDriver extends Driver {
	// Store the LCIO collection names for the needed objects.
	private String bankCollectionName = "TriggerBank";
	private String clusterCollectionName = "EcalClusters";
	
	// Store the lists of parsed objects.
	private TIData tiBank;
	private SSPData sspBank;
	private List<Cluster> reconClusters;
	private List<SSPCluster> sspClusters;
	private List<List<PairTrigger<Cluster[]>>> reconPairsTriggers = new ArrayList<List<PairTrigger<Cluster[]>>>(2);
	private List<List<PairTrigger<SSPCluster[]>>> sspPairsTriggers = new ArrayList<List<PairTrigger<SSPCluster[]>>>(2);
	private List<List<SinglesTrigger<Cluster>>> reconSinglesTriggers = new ArrayList<List<SinglesTrigger<Cluster>>>(2);
	private List<List<SinglesTrigger<SSPCluster>>> sspSinglesTriggers = new ArrayList<List<SinglesTrigger<SSPCluster>>>(2);
	
	// Trigger modules for performing trigger analysis.
	private TriggerModule[] singlesTrigger = new TriggerModule[2];
	private TriggerModule[] pairsTrigger = new TriggerModule[2];
	
	// Output text logger.
	private static Logger logger = LogUtil.create(TriggerDiagnosticDriver.class);  
	static { logger.setLevel(Level.ALL); }
	
	// Verification settings.
	private int nsa = 100;
	private int nsb = 20;
	private int windowWidth = 200;
	private int hitAcceptance = 1;
	private double energyAcceptance = 0.03;
	private boolean performClusterVerification = true;
	
	// Efficiency tracking variables.
	private int reconClustersFound = 0;
	private int reconClustersMatched = 0;
	
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
		pairsTrigger[0].setCutValue(TriggerModule.PAIR_TIME_COINCIDENCE, 8);
		
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
		pairsTrigger[1].setCutValue(TriggerModule.PAIR_TIME_COINCIDENCE, 8);
		
		// Instantiate the triggers lists.
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			reconPairsTriggers.add(new ArrayList<PairTrigger<Cluster[]>>());
			sspPairsTriggers.add(new ArrayList<PairTrigger<SSPCluster[]>>());
			reconSinglesTriggers.add(new ArrayList<SinglesTrigger<Cluster>>());
			sspSinglesTriggers.add(new ArrayList<SinglesTrigger<SSPCluster>>());
		}
		
		// Print the initial settings.
		logSettings();
	}
	
	@Override
	public void endOfData() {
		System.out.println("\n==== Efficiency Report ===============================================");
		System.out.println("======================================================================\n");
		System.out.println("==== Cluster Verification ================");
		System.out.printf("\tValid Clusters Reconstructed   :: %d%n", reconClustersFound);
		System.out.printf("\tReconstructed Clusters Matched :: %d%n", reconClustersMatched);
		System.out.printf("\tClustering Efficiency          :: %3.2f %%%n", (100.0 * reconClustersMatched / reconClustersFound));
		
	}
	
	/**
	 * Gets the banks and clusters from the event.
	 */
	@Override
	public void process(EventHeader event) {
		// ==========================================================
		// ==== Obtain Reconstructed Clusters =======================
		// ==========================================================
		
		// Get the reconstructed clusters.
		if(event.hasCollection(Cluster.class, clusterCollectionName)) {
			reconClusters = event.get(Cluster.class, clusterCollectionName);
			logger.fine(String.format("%d reconstructed clusters found.", reconClusters.size()));
		} else {
			reconClusters = new ArrayList<Cluster>(0);
			logger.warning(String.format("No reconstructed clusters were found for collection \"%s\" in this event.", clusterCollectionName));
		}
		
		
		
		// ==========================================================
		// ==== Obtain SSP and TI Banks =============================
		// ==========================================================
		
		// Get the SSP clusters.
		if(event.hasCollection(GenericObject.class, bankCollectionName)) {
			// Get the bank list.
			List<GenericObject> bankList = event.get(GenericObject.class, bankCollectionName);
			
			// Search through the banks and get the SSP and TI banks.
			for(GenericObject obj : bankList) {
				// If this is an SSP bank, parse it.
				if(AbstractIntData.getTag(obj) == SSPData.BANK_TAG) {
					sspBank = new SSPData(obj);
					logger.finer("Read SSP bank.");
				}
				
				// Otherwise, if this is a TI bank, parse it.
				else if(AbstractIntData.getTag(obj) == TIData.BANK_TAG) {
					tiBank = new TIData(obj);
					logger.finer("Read TI bank.");
				}
			}
			
			// If there is an SSP bank, get the list of SSP clusters.
			if(sspBank != null) {
				sspClusters = sspBank.getClusters();
				logger.fine(String.format("%d SSP clusters found.", sspClusters.size()));
			}
		}
		
		
		
		// ==========================================================
		// ==== Establish Event Integrity ===========================
		// ==========================================================
		
		// Check that all of the required objects are present.
		if(sspBank == null) {
			logger.warning("No SSP bank found for this event. No verification will be performed.");
			return;
		} if(tiBank == null) {
			logger.warning("No TI bank found for this event. No verification will be performed.");
			return;
		}
		
		
		
		// ==========================================================
		// ==== Perform Detailed Event Logging ======================
		// ==========================================================
		
		// Otherwise, print out the two cluster collections.
		logger.finest(String.format("Summary for Event %d at time %d", event.getEventNumber(), event.getTimeStamp()));
		logger.finest("Reconstructed Clusters:");
		for(Cluster cluster : reconClusters) {
			logger.finest("\t" + reconClusterToString(cluster));
		}
		
		logger.finest("SSP Clusters:");
		for(SSPCluster cluster : sspClusters) {
			logger.finest("\t" + sspClusterToString(cluster));
		}
		
		
		
		// ==========================================================
		// ==== Perform Event Verification ==========================
		// ==========================================================
		
		// Perform the cluster verification step.
		if(performClusterVerification) { verifyClusters(); }
		
		// Construct lists of triggers for the SSP clusters and the
		// reconstructed clusters.
		constructSinglesTriggers();
		constructPairTriggers();
		verifyTriggers();
	}
	
	/**
	 * Attempts to match all reconstructed clusters that are safely
	 * within the integration window with clusters reported by the SSP.
	 * Method also tracks the ratio of valid reconstructed clusters to
	 * matches found.<br/>
	 * <br/>
	 * Note that unmatched SSP clusters are ignored. Since these may
	 * or may not correspond to reconstructed clusters that occur in
	 * the forbidden time region, it is impossible to say whether or
	 * not these legitimately failed to match or not.
	 */
	private void verifyClusters() {
		// ==========================================================
		// ==== Cluster Verification Initialization =================
		// ==========================================================
		
		// Track which clusters match and whether a given cluster
		// has been matched or not.
		Set<Cluster> reconClusterSet = new HashSet<Cluster>(reconClusters.size());
		Set<SSPCluster> sspClusterSet = new HashSet<SSPCluster>(sspClusters.size());
		Map<Cluster, SSPCluster> pairMap = new HashMap<Cluster, SSPCluster>(reconClusters.size());
		
		// Store which clusters were rejected due to the cluster
		// window size.
		Set<Cluster> badClusters = new HashSet<Cluster>();
		
		
		
		// ==========================================================
		// ==== Cluster Matching ====================================
		// ==========================================================
		
		// Iterate over the reconstructed clusters and check whether
		// there is a matching SSP cluster or not.
		reconLoop:
		for(Cluster reconCluster : reconClusters) {
			// Get the cluster's x- and y- indices.
			int ix = reconCluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
			int iy = reconCluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
			
			// Require that the clusters are far enough way
			// from the edges of the time window to ensure
			// that they will not have the FADC pulse cut.
			for(CalorimeterHit hit : reconCluster.getCalorimeterHits()) {
				if(hit.getTime() <= nsb || hit.getTime() >= (windowWidth - nsa)) {
					badClusters.add(reconCluster);
					logger.finer(String.format("Cluster %s is out-of-time due to hit (%3d, %3d) at time %3.0f.",
							reconClusterPositionString(reconCluster),
							hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"),
							hit.getTime()));
					continue reconLoop;
				}
			}
			
			// Increment the number of reconstructed clusters found that
			// are within the allowed time window.
			reconClustersFound++;
			
			// Look for an unmatched cluster with the same indices.
			matchLoop:
			for(SSPCluster sspCluster : sspClusters) {
				if(sspCluster.getXIndex() == ix && sspCluster.getYIndex() == iy) {
					// If this SSP cluster is already matched, it can
					// not be used again.
					if(sspClusterSet.contains(sspCluster)) {
						logger.finer(String.format("Cluster %s fails to match SSP cluster %s; SSP cluster already matched.",
								reconClusterPositionString(reconCluster), sspClusterPositionString(sspCluster)));
						continue matchLoop;
					}
					
					// We also require that the SSP cluster reports an
					// energy within a certain percentage of the recon
					// cluster.
					double[] energy = { sspCluster.getEnergy() * (1 - energyAcceptance),
							sspCluster.getEnergy() * (1 + energyAcceptance) };
					
					// If the energies are within range, consider this
					// a matched cluster pair.
					if(reconCluster.getEnergy() >= energy[0] && reconCluster.getEnergy() <= energy[1]) {
						// Check that the clusters have the same hit
						// count. If the allowHitCountVariance setting
						// is enabled, this may differ by +/- 1 hit.
						int reconHits = reconCluster.getCalorimeterHits().size();
						int sspHits = sspCluster.getHitCount();
						if((reconHits + hitAcceptance >= sspHits) && (reconHits - hitAcceptance <= sspHits)) {
							// Add the two clusters to the matched sets.
							sspClusterSet.add(sspCluster);
							reconClusterSet.add(reconCluster);
							
							// Map the two clusters together.
							pairMap.put(reconCluster, sspCluster);
							
							// Log the match.
							logger.finer(String.format("Cluster %s matches SSP cluster %s",
									reconClusterPositionString(reconCluster), sspClusterPositionString(sspCluster)));
							
							// Increment the number of reconstructed
							// clusters matched.
							reconClustersMatched++;
							
							// Skip to the next recon cluster.
							continue matchLoop;
						} else {
							logger.finer(String.format("Cluster %s fails to match SSP cluster %s; hits not within threshold.",
									reconClusterPositionString(reconCluster), sspClusterPositionString(sspCluster)));
						} // End match hits check
					} else {
						logger.finer(String.format("Cluster %s fails to match SSP cluster %s; Energy not within %3.1f%%.",
								reconClusterPositionString(reconCluster), sspClusterPositionString(sspCluster),
								(energyAcceptance * 100.0)));
					} // End match energy check
				} // End match indices check
			} // End matchLoop
		} // End reconLoop
		
		
		
		// ==========================================================
		// ==== Event Summary Readout ===============================
		// ==========================================================
		
		// Output the matched clusters.
		logger.finest("Matched Clusters:");
		if(!pairMap.isEmpty()) {
			for(Entry<Cluster, SSPCluster> pair : pairMap.entrySet()) {
				logger.finest(String.format("\t%s --> %s%n", reconClusterToString(pair.getKey()), sspClusterToString(pair.getValue())));
			}
		} else { logger.finest("\tNone"); }
		
		// Output unmatched reconstructed clusters and SSP clusters.
		logger.finest("\nUnmatched Clusters:");
		if(sspClusterSet.size() != sspClusters.size() || reconClusterSet.size() != reconClusters.size()) {
			// Output the SSP clusters that were not matched.
			for(SSPCluster sspCluster : sspClusters) {
				if(!sspClusterSet.contains(sspCluster)) {
					logger.finest(String.format("\tSSP   :: %s", sspClusterToString(sspCluster)));
				}
			}
			
			// Output the recon clusters that were not matched.
			for(Cluster reconCluster : reconClusters) {
				if(!reconClusterSet.contains(reconCluster) && !badClusters.contains(reconCluster)) {
					logger.finest(String.format("\tRecon :: %s", reconClusterToString(reconCluster)));
				}
			}
		} else {  logger.finest("\tNone"); }
		
		// Output the reconstructed clusters that were out-of-time.
		logger.finest("\nOut-of-time Recon Clusters:");
		if(!badClusters.isEmpty()) {
			for(Cluster badCluster : badClusters) {
				logger.finest(String.format("\tRecon :: %s", reconClusterToString(badCluster)));
			}
		} else {  logger.finest("\tNone"); }
		
		// Output the event efficiency.
		logger.fine(String.format("Event Efficiency: %.1f", (100.0 * pairMap.size() / (reconClusters.size() - badClusters.size()))));
	}
	
	private void verifyTriggers() {
		// Get the list of triggers reported by the SSP.
		List<SSPTrigger> sspTriggers = sspBank.getTriggers();
		
		// Iterate over the triggers.
		System.out.println("SSP Bank Singles Triggers:");
		for(SSPTrigger sspTrigger : sspTriggers) {
			// If the trigger is a singles trigger, convert it.
			if(sspTrigger instanceof SSPSinglesTrigger) {
				// Cast the trigger to a singles trigger.
				SSPSinglesTrigger sspSingles = (SSPSinglesTrigger) sspTrigger;
				int triggerNum = sspSingles.isFirstTrigger() ? 0 : 1;
				boolean matchedTrigger = false;
				
				// Iterate over the SSP cluster simulated triggers and
				// look for a cluster that matches.
				matchLoop:
				for(SinglesTrigger<SSPCluster> simTrigger : sspSinglesTriggers.get(triggerNum)) {
					if(compareSSPSinglesTriggers(sspSingles, simTrigger)) {
						matchedTrigger = true;
						break matchLoop;
					}
				}
				
				System.out.printf("\tTrigger %d :: %3d :: EClusterLow: %d; EClusterHigh %d; HitCount: %d :: Matched: %5b%n",
						(triggerNum + 1), sspSingles.getTime(),
						sspSingles.passCutEnergyMin() ? 1 : 0, sspSingles.passCutEnergyMax() ? 1 : 0,
						sspSingles.passCutHitCount() ? 1 : 0, matchedTrigger);
			}
		}

		System.out.println("SSP Clusters:");
		for(SSPCluster cluster : sspClusters) {
			System.out.println("\t" + sspClusterToString(cluster));
		}
	}
	
	/**
	 * Generates and stores the singles triggers for both reconstructed
	 * and SSP clusters.
	 */
	private void constructSinglesTriggers() {
		System.out.println("SSP Cluster Singles Triggers:");
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
				
				System.out.printf("\tTrigger %d :: %s :: EClusterLow: %d; EClusterHigh %d; HitCount: %d%n",
						(triggerNum + 1), sspClusterPositionString(cluster), passClusterLow ? 1 : 0,
						passClusterHigh ? 1 : 0, passHitCount ? 1 : 0);
			}
		}
		
		// Run the reconstructed clusters through the singles trigger
		// to determine whether they pass it or not.
		System.out.println("Recon Cluster Singles Triggers:");
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
				
				System.out.printf("\tTrigger %d :: %s :: EClusterLow: %d; EClusterHigh %d; HitCount: %d%n",
						(triggerNum + 1), reconClusterPositionString(cluster), passClusterLow ? 1 : 0,
						passClusterHigh ? 1 : 0, passHitCount ? 1 : 0);
			}
		}
	}
	
	/**
	 * Generates and stores the pair triggers for both reconstructed
	 * and SSP clusters.
	 */
	private void constructPairTriggers() {
		// Store cluster pairs.
		List<Cluster> topReconClusters = new ArrayList<Cluster>();
		List<Cluster> bottomReconClusters = new ArrayList<Cluster>();
		List<Cluster[]> reconPairs = new ArrayList<Cluster[]>();
		List<SSPCluster> topSSPClusters = new ArrayList<SSPCluster>();
		List<SSPCluster> bottomSSPClusters = new ArrayList<SSPCluster>();
		List<SSPCluster[]> sspPairs = new ArrayList<SSPCluster[]>();
		
		// Split the clusters into lists of top and bottom clusters.
		for(Cluster reconCluster : reconClusters) {
			if(reconCluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy") > 0) {
				topReconClusters.add(reconCluster);
			} else {
				bottomReconClusters.add(reconCluster);
			}
		}
		for(SSPCluster sspCluster : sspClusters) {
			if(sspCluster.getYIndex() > 0) {
				topSSPClusters.add(sspCluster);
			} else {
				bottomSSPClusters.add(sspCluster);
			}
		}
		
		// Form all possible top/bottom cluster pairs.
		for(Cluster topReconCluster : topReconClusters) {
			for(Cluster bottomReconCluster : bottomReconClusters) {
				Cluster[] reconPair = new Cluster[2];
				reconPair[0] = topReconCluster;
				reconPair[1] = bottomReconCluster;
				reconPairs.add(reconPair);
			}
		}
		for(SSPCluster topSSPCluster : topSSPClusters) {
			for(SSPCluster bottomSSPCluster : bottomSSPClusters) {
				SSPCluster[] sspPair = new SSPCluster[2];
				sspPair[0] = topSSPCluster;
				sspPair[1] = bottomSSPCluster;
				sspPairs.add(sspPair);
			}
		}
		
		// Simulate the pair triggers and record the results.
		System.out.println("Recon Cluster Pairs Triggers:");
		for(Cluster[] reconPair : reconPairs) {
			for(int triggerIndex = 0; triggerIndex < 2; triggerIndex++) {
				// For a cluster to have formed it is assumed to have passed
				// the cluster seed energy cuts. This can not be verified
				// since the SSP bank does not report individual hit. 
				boolean passSeedLow = true;
				boolean passSeedHigh = true;
				
				// The remaining cuts may be acquired from trigger module.
				boolean passClusterLow = pairsTrigger[triggerIndex].clusterTotalEnergyCutLow(reconPair[0])
						&& pairsTrigger[triggerIndex].clusterTotalEnergyCutLow(reconPair[1]);
				boolean passClusterHigh = pairsTrigger[triggerIndex].clusterTotalEnergyCutHigh(reconPair[0])
						&& pairsTrigger[triggerIndex].clusterTotalEnergyCutHigh(reconPair[1]);
				boolean passHitCount = pairsTrigger[triggerIndex].clusterHitCountCut(reconPair[0])
						&& pairsTrigger[triggerIndex].clusterHitCountCut(reconPair[1]);
				boolean passPairEnergySumLow = pairsTrigger[triggerIndex].pairEnergySumCutLow(reconPair);
				boolean passPairEnergySumHigh = pairsTrigger[triggerIndex].pairEnergySumCutHigh(reconPair);
				boolean passPairEnergyDifference = pairsTrigger[triggerIndex].pairEnergyDifferenceCut(reconPair);
				boolean passPairEnergySlope = pairsTrigger[triggerIndex].pairEnergySlopeCut(reconPair);
				boolean passPairCoplanarity = pairsTrigger[triggerIndex].pairCoplanarityCut(reconPair);
				boolean passTimeCoincidence = pairsTrigger[triggerIndex].pairTimeCoincidenceCut(reconPair);
				
				// Create a trigger from the results.
				PairTrigger<Cluster[]> trigger = new PairTrigger<Cluster[]>(reconPair);
				trigger.setStateSeedEnergyLow(passSeedLow);
				trigger.setStateSeedEnergyHigh(passSeedHigh);
				trigger.setStateClusterEnergyLow(passClusterLow);
				trigger.setStateClusterEnergyHigh(passClusterHigh);
				trigger.setStateHitCount(passHitCount);
				trigger.setStateEnergySumLow(passPairEnergySumLow);
				trigger.setStateEnergySumHigh(passPairEnergySumHigh);
				trigger.setStateEnergyDifference(passPairEnergyDifference);
				trigger.setStateEnergySlope(passPairEnergySlope);
				trigger.setStateCoplanarity(passPairCoplanarity);
				trigger.setStateTimeCoincidence(passTimeCoincidence);
				
				// Add the trigger to the list.
				reconPairsTriggers.get(triggerIndex).add(trigger);
				
				System.out.printf("\tTrigger %d :: %s, %s :: EClusterLow: %d; EClusterHigh %d; HitCount: %d; ESumLow: %d, ESumHigh: %d, EDiff: %d, ESlope: %d, Coplanarity: %d, TimeDiff: %d%n",
						(triggerIndex + 1), reconClusterPositionString(reconPair[0]),
						reconClusterPositionString(reconPair[1]), passClusterLow ? 1 : 0,
						passClusterHigh ? 1 : 0, passHitCount ? 1 : 0, passPairEnergySumLow ? 1 : 0,
						passPairEnergySumHigh ? 1 : 0, passPairEnergyDifference ? 1 : 0,
						passPairEnergySlope ? 1 : 0, passPairCoplanarity ? 1 : 0,
						passTimeCoincidence ? 1 : 0);
			}
		}
		
		for(SSPCluster[] sspPair : sspPairs) {
			for(int triggerIndex = 0; triggerIndex < 2; triggerIndex++) {
				// For a cluster to have formed it is assumed to have passed
				// the cluster seed energy cuts. This can not be verified
				// since the SSP bank does not report individual hit. 
				boolean passSeedLow = true;
				boolean passSeedHigh = true;
				
				// The remaining cuts may be acquired from trigger module.
				boolean passClusterLow = pairsTrigger[triggerIndex].clusterTotalEnergyCutLow(sspPair[0])
						&& pairsTrigger[triggerIndex].clusterTotalEnergyCutLow(sspPair[1]);
				boolean passClusterHigh = pairsTrigger[triggerIndex].clusterTotalEnergyCutHigh(sspPair[0])
						&& pairsTrigger[triggerIndex].clusterTotalEnergyCutHigh(sspPair[1]);
				boolean passHitCount = pairsTrigger[triggerIndex].clusterHitCountCut(sspPair[0])
						&& pairsTrigger[triggerIndex].clusterHitCountCut(sspPair[1]);
				boolean passPairEnergySumLow = pairsTrigger[triggerIndex].pairEnergySumCutLow(sspPair);
				boolean passPairEnergySumHigh = pairsTrigger[triggerIndex].pairEnergySumCutHigh(sspPair);
				boolean passPairEnergyDifference = pairsTrigger[triggerIndex].pairEnergyDifferenceCut(sspPair);
				
				// TODO: Implement the pair energy slope cut and the and
				//       pair coplanarity cut once they are supported by
				//       TriggerModule.
				boolean passPairEnergySlope = false;
				boolean passPairCoplanarity = false;
				
				boolean passTimeCoincidence = pairsTrigger[triggerIndex].pairTimeCoincidenceCut(sspPair);
				
				// Create a trigger from the results.
				PairTrigger<SSPCluster[]> trigger = new PairTrigger<SSPCluster[]>(sspPair);
				trigger.setStateSeedEnergyLow(passSeedLow);
				trigger.setStateSeedEnergyHigh(passSeedHigh);
				trigger.setStateClusterEnergyLow(passClusterLow);
				trigger.setStateClusterEnergyHigh(passClusterHigh);
				trigger.setStateHitCount(passHitCount);
				trigger.setStateEnergySumLow(passPairEnergySumLow);
				trigger.setStateEnergySumHigh(passPairEnergySumHigh);
				trigger.setStateEnergyDifference(passPairEnergyDifference);
				trigger.setStateEnergySlope(passPairEnergySlope);
				trigger.setStateCoplanarity(passPairCoplanarity);
				trigger.setStateTimeCoincidence(passTimeCoincidence);
				
				// Add the trigger to the list.
				//sspPairsTriggers.get(triggerIndex).add(trigger);
			}
		}
	}
	
	/**
	 * Outputs all of the verification parameters currently in use by
	 * the software. A warning will be issued if the values for NSA and
	 * NSB, along with the FADC window, preclude clusters from being
	 * verified.
	 */
	private void logSettings() {
		// Output general settings.
		logger.config("Cluster Verification Settings");
		logger.config(String.format("\tEnergy Threshold       :: %1.2f", energyAcceptance));
		logger.config(String.format("\tHit Threshold          :: %1d", hitAcceptance));
		
		// Output window settings.
		logger.config("FADC Timing Window Settings");
		logger.config(String.format("\tNSB                    :: %3d ns", nsb));
		logger.config(String.format("\tNSA                    :: %3d ns", nsa));
		logger.config(String.format("\tFADC Window            :: %3d ns", windowWidth));
		
		// Calculate the valid clustering window.
		int start = nsb;
		int end = windowWidth - nsa;
		if(start < end) {
			logger.config(String.format("\tValid Cluster Window   :: [ %3d ns, %3d ns ]", start, end));
			performClusterVerification = true;
		} else {
			logger.warning("\tNSB, NSA, and FADC window preclude a valid cluster verification window.");
			logger.warning("\tCluster verification will not be performed!");
			performClusterVerification = false;
		}
		
		// Output the singles trigger settings.
		for(int i = 0; i < 2; i++) {
			logger.config(String.format("Singles Trigger %d Settings", (i + 1)));
			logger.config(String.format("\tCluster Energy Low     :: %.3f GeV", singlesTrigger[i].getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW)));
			logger.config(String.format("\tCluster Energy High    :: %.3f GeV", singlesTrigger[i].getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH)));
			logger.config(String.format("\tCluster Hit Count      :: %.0f hits", singlesTrigger[i].getCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW)));
		}
		
		// Output the pair trigger settings.
		for(int i = 0; i < 2; i++) {
			logger.config(String.format("Pairs Trigger %d Settings", (i + 1)));
			logger.config(String.format("\tCluster Energy Low     :: %.3f GeV", pairsTrigger[i].getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW)));
			logger.config(String.format("\tCluster Energy High    :: %.3f Gen", pairsTrigger[i].getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH)));
			logger.config(String.format("\tCluster Hit Count      :: %.0f hits", pairsTrigger[i].getCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW)));
			logger.config(String.format("\tPair Energy Sum Low    :: %.3f GeV", pairsTrigger[i].getCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW)));
			logger.config(String.format("\tPair Energy Sum Low    :: %.3f GeV", pairsTrigger[i].getCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH)));
			logger.config(String.format("\tPair Energy Difference :: %.3f GeV", pairsTrigger[i].getCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH)));
			logger.config(String.format("\tPair Energy Slope      :: %.3f GeV", pairsTrigger[i].getCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW)));
			logger.config(String.format("\tPair Energy Slope F    :: %.3f GeV / mm", pairsTrigger[i].getCutValue(TriggerModule.PAIR_ENERGY_SLOPE_F)));
			logger.config(String.format("\tPair Coplanarity       :: %.0f Degrees", pairsTrigger[i].getCutValue(TriggerModule.PAIR_COPLANARITY_HIGH)));
			logger.config(String.format("\tPair Time Coincidence  :: %.0f ns", pairsTrigger[i].getCutValue(TriggerModule.PAIR_TIME_COINCIDENCE)));
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
	
	private static final String reconClusterPositionString(Cluster cluster) {
		return String.format("(%3d, %3d)",
				cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix"),
				cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy"));
	}
	
	private static final String sspClusterPositionString(SSPCluster cluster) {
		return String.format("(%3d, %3d)", cluster.getXIndex(), cluster.getYIndex());
	}
	
	/**
	 * Compares a trigger from the SSP bank to a trigger simulated on
	 * an SSP cluster.
	 * @param bankTrigger - The trigger from the SSP bank.
	 * @param simTrigger - The trigger from the simulation.
	 * @return Returns <code>true</code> if the triggers match and
	 * <code>false</code> if they do not.
	 */
	private static final boolean compareSSPSinglesTriggers(SSPSinglesTrigger bankTrigger, SinglesTrigger<SSPCluster> simTrigger) {
		// The bank trigger and simulated trigger must have the same
		// time. This is equivalent to the time of the triggering cluster.
		if(bankTrigger.getTime() != simTrigger.getTriggerSource().getTime()) {
			return false;
		}
		
		// If the time stamp is the same, check that the trigger flags
		// are all the same. Start with cluster energy low.
		if(bankTrigger.passCutEnergyMin() != simTrigger.getStateClusterEnergyLow()) {
			return false;
		}
		
		// Check cluster energy high.
		if(bankTrigger.passCutEnergyMax() != simTrigger.getStateClusterEnergyHigh()) {
			return false;
		}
		
		// Check cluster hit count.
		if(bankTrigger.passCutHitCount() != simTrigger.getStateHitCount()) {
			return false;
		}
		
		// If all of the tests are successful, the triggers match.
		return true;
	}
}
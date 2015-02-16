package org.hps.users.kmccarty;

import java.awt.Point;
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
import org.hps.readout.ecal.triggerbank.SSPPairTrigger;
import org.hps.readout.ecal.triggerbank.SSPSinglesTrigger;
import org.hps.readout.ecal.triggerbank.SSPTrigger;
import org.hps.readout.ecal.triggerbank.TIData;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
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
	private List<List<PairTrigger<Cluster[]>>> reconPairsTriggers = new ArrayList<List<PairTrigger<Cluster[]>>>(2);
	private List<List<PairTrigger<SSPCluster[]>>> sspPairsTriggers = new ArrayList<List<PairTrigger<SSPCluster[]>>>(2);
	private List<List<SinglesTrigger<Cluster>>> reconSinglesTriggers = new ArrayList<List<SinglesTrigger<Cluster>>>(2);
	private List<List<SinglesTrigger<SSPCluster>>> sspSinglesTriggers = new ArrayList<List<SinglesTrigger<SSPCluster>>>(2);
	
	// Trigger modules for performing trigger analysis.
	private TriggerModule[] singlesTrigger = new TriggerModule[2];
	private TriggerModule[] pairsTrigger = new TriggerModule[2];
	
	// Verification settings.
	private int nsa = 100;
	private int nsb = 20;
	private int windowWidth = 200;
	private int hitAcceptance = 1;
	private double energyAcceptance = 0.03;
	private boolean performClusterVerification = true;
	private boolean performSinglesTriggerVerification = true;
	private boolean performPairTriggerVerification = true;
	
	// Efficiency tracking variables.
	private int globalFound = 0;
	private int globalMatched = 0;
	private int globalPosition = 0;
	private int globalEnergy = 0;
	private int globalHitCount = 0;
	
	private int singlesSSPTriggers = 0;
	private int singlesReconMatched = 0;
	private int singlesReconTriggers = 0;
	private int singlesInternalMatched = 0;
	private int singlesReportedTriggers = 0;
	
	private int pairSSPTriggers = 0;
	private int pairReconMatched = 0;
	private int pairReconTriggers = 0;
	private int pairInternalMatched = 0;
	private int pairReportedTriggers = 0;
	
	/**
	 * Define the trigger modules. This should be replaced by parsing
	 * the DAQ configuration at some point.
	 */
	@Override
	public void startOfData() {
		// Print the cluster verification header.
		System.out.println();
		System.out.println();
		System.out.println("======================================================================");
		System.out.println("=== Cluster/Trigger Verification Settings ============================");
		System.out.println("======================================================================");
		
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
	
	/**
	 * Prints the total run statistics.
	 */
	@Override
	public void endOfData() {
		// Print the cluster/trigger verification header.
		System.out.println();
		System.out.println();
		System.out.println("======================================================================");
		System.out.println("=== Cluster/Trigger Verification Results =============================");
		System.out.println("======================================================================");
		
		// Print the cluster verification data.
		System.out.println("Cluster Verification:");
		System.out.printf("\tRecon Clusters     :: %d%n", globalFound);
		System.out.printf("\tClusters Matched   :: %d%n", globalMatched);
		System.out.printf("\tFailed (Position)  :: %d%n", globalPosition);
		System.out.printf("\tFailed (Energy)    :: %d%n", globalEnergy);
		System.out.printf("\tFailed (Hit Count) :: %d%n", globalHitCount);
		if(globalFound == 0) { System.out.printf("\tCluster Efficiency :: N/A%n"); }
		else { System.out.printf("\tCluster Efficiency :: %3.0f%%%n", 100.0 * globalMatched / globalFound); }
		
		// Print the singles trigger verification data.
		System.out.println();
		System.out.println("Singles Trigger Verification:");
		System.out.printf("\tSSP Cluster Sim Triggers   :: %d%n", singlesSSPTriggers);
		System.out.printf("\tRecon Cluster Sim Triggers :: %d%n", singlesReconTriggers);
		System.out.printf("\tSSP Reported Triggers      :: %d%n", singlesReportedTriggers);

		if(singlesSSPTriggers == 0) {
			System.out.printf("\tInternal Efficiency        :: %d / %d (N/A)%n",
					singlesInternalMatched, singlesSSPTriggers);
		} else {
			System.out.printf("\tInternal Efficiency        :: %d / %d (%3.0f%%)%n",
					singlesInternalMatched, singlesSSPTriggers, (100.0 * singlesInternalMatched / singlesSSPTriggers));
		}
		if(singlesReconTriggers == 0) {
			System.out.printf("\tTrigger Efficiency         :: %d / %d (N/A)%n",
					singlesReconMatched, singlesReconTriggers);
		} else {
			System.out.printf("\tTrigger Efficiency         :: %d / %d (%3.0f%%)%n",
					singlesReconMatched, singlesReconTriggers, (100.0 * singlesReconMatched / singlesReconTriggers));
		}
		
		// Print the pair trigger verification data.
		System.out.println();
		System.out.println("Pair Trigger Verification:");
		System.out.printf("\tSSP Cluster Sim Triggers   :: %d%n", pairSSPTriggers);
		System.out.printf("\tRecon Cluster Sim Triggers :: %d%n", pairReconTriggers);
		System.out.printf("\tSSP Reported Triggers      :: %d%n", pairReportedTriggers);

		if(pairSSPTriggers == 0) {
			System.out.printf("\tInternal Efficiency        :: %d / %d (N/A)%n",
					pairInternalMatched, pairSSPTriggers);
		} else {
			System.out.printf("\tInternal Efficiency        :: %d / %d (%3.0f%%)%n",
					pairInternalMatched, pairSSPTriggers, (100.0 * pairInternalMatched / pairSSPTriggers));
		}
		if(pairReconTriggers == 0) {
			System.out.printf("\tTrigger Efficiency         :: %d / %d (N/A)%n",
					pairReconMatched, pairReconTriggers);
		} else {
			System.out.printf("\tTrigger Efficiency         :: %d / %d (%3.0f%%)%n",
					pairReconMatched, pairReconTriggers, (100.0 * pairReconMatched / pairReconTriggers));
		}
	}
	
	/**
	 * Gets the banks and clusters from the event.
	 */
	@Override
	public void process(EventHeader event) {
		// ==========================================================
		// ==== Initialize the Event ================================
		// ==========================================================
		
		System.out.println("======================================================================");
		System.out.println("==== Cluster/Trigger Verification ====================================");
		System.out.println("======================================================================");
		
		// Clear the list of triggers from previous events.
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			sspSinglesTriggers.get(triggerNum).clear();
			reconSinglesTriggers.get(triggerNum).clear();
			sspPairsTriggers.get(triggerNum).clear();
			reconPairsTriggers.get(triggerNum).clear();
		}
		
		
		
		// ==========================================================
		// ==== Obtain Reconstructed Clusters =======================
		// ==========================================================
		
		// Get the reconstructed clusters.
		if(event.hasCollection(Cluster.class, clusterCollectionName)) {
			reconClusters = event.get(Cluster.class, clusterCollectionName);
			if(reconClusters.size() == 1) {
				System.out.println("1 reconstructed cluster found.");
			} else {
				System.out.printf("%d reconstructed clusters found.%n", reconClusters.size());
			}
		} else {
			reconClusters = new ArrayList<Cluster>(0);
			System.out.printf("No reconstructed clusters were found for collection \"%s\" in this event.%n", clusterCollectionName);
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
				}
				
				// Otherwise, if this is a TI bank, parse it.
				else if(AbstractIntData.getTag(obj) == TIData.BANK_TAG) {
					tiBank = new TIData(obj);
				}
			}
			
			// If there is an SSP bank, get the list of SSP clusters.
			if(sspBank != null) {
				sspClusters = sspBank.getClusters();
				if(sspClusters.size() == 1) {
					System.out.println("1 SSP cluster found.");
				} else {
					System.out.printf("%d SSP clusters found.%n", sspClusters.size());
				}
			}
		}
		
		
		
		// ==========================================================
		// ==== Establish Event Integrity ===========================
		// ==========================================================
		
		// Check that all of the required objects are present.
		if(sspBank == null) {
			System.out.println("No SSP bank found for this event. No verification will be performed.");
			return;
		} if(tiBank == null) {
			System.out.println("No TI bank found for this event. No verification will be performed.");
			return;
		}
		
		
		
		// ==========================================================
		// ==== Perform Event Verification ==========================
		// ==========================================================
		
		// Perform the cluster verification step.
		if(performClusterVerification) { clusterVerification(); }
		
		// Construct lists of triggers for the SSP clusters and the
		// reconstructed clusters.
		if(performSinglesTriggerVerification) {
			constructSinglesTriggers();
			singlesTriggerVerification();
		}
		if(performPairTriggerVerification) {
			constructPairTriggers();
			pairTriggerVerification();
		}
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
	private void clusterVerification() {
		// ==========================================================
		// ==== Initialize Cluster Verification =====================
		// ==========================================================
		
		// Print the cluster verification header.
		System.out.println();
		System.out.println();
		System.out.println("======================================================================");
		System.out.println("=== Cluster Verification =============================================");
		System.out.println("======================================================================");
		
		// If there are no reconstructed clusters, than there is nothing
		// that can be verified.
		if(reconClusters.isEmpty()) {
			System.out.println("No reconstructed clusters are present. Skipping event...");
			return;
		}
		
		// Track reconstructed clusters that were excluded from the
		// verification process they are outside the verification time
		// range.
		Set<Cluster> unverifiedClusters = new HashSet<Cluster>();
		
		// Track the number of cluster pairs that were matched and that
		// failed by failure type.
		int eventMatched = 0;
		int eventPosition = 0;
		int eventEnergy = 0;
		int eventHitCount = 0;
		
		// Track the matched clusters.
		StringBuffer eventMatchedText = new StringBuffer();
		
		// ==========================================================
		// ==== Produce the Cluster Position Mappings ===============
		// ==========================================================
		
		// Create maps to link cluster position to the list of clusters
		// that were found at that location.
		Map<Point, ArrayList<Cluster>> reconClusterMap = new HashMap<Point, ArrayList<Cluster>>(reconClusters.size());
		Map<Point, ArrayList<SSPCluster>> sspClusterMap = new HashMap<Point, ArrayList<SSPCluster>>(reconClusters.size());
		
		// Populate the reconstructed cluster map.
		System.out.println();
		System.out.println("Testing clusters for verifiability...");
		reconMapLoop:
		for(Cluster reconCluster : reconClusters) {
			System.out.printf("\t%s", reconClusterToString(reconCluster));
			
			// Check if the cluster is within the temporal verification
			// range for cluster verification.
			for(CalorimeterHit hit : reconCluster.getCalorimeterHits()) {
				if(hit.getTime() <= nsb || hit.getTime() >= (windowWidth - nsa)) {
					// Add the cluster to the lists of clusters that
					// can not be verified due to their time position
					// and skip the cluster mapping.
					unverifiedClusters.add(reconCluster);
					System.out.println(" [ unapproved ]");
					continue reconMapLoop;
				}
			}
			
			System.out.println(" [  approved  ]");
			
			// Get the cluster position.
			Point position = new Point(
					reconCluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix"),
					reconCluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy")
			);
			
			// Get the list for this cluster position.
			ArrayList<Cluster> reconList = reconClusterMap.get(position);
			if(reconList == null) {
				reconList = new ArrayList<Cluster>();
				reconClusterMap.put(position, reconList);
			}
			
			// Add the cluster to the list.
			reconList.add(reconCluster);
		}
		
		// If all of the reconstructed clusters are invalid due to when
		// the clusters occurred, there is nothing to verify. Skip the
		// remainder of the process.
		if(unverifiedClusters.size() == reconClusters.size()) {
			System.out.println("No verifiable clusters are present. Skipping event...");
			return;
		}
		
		// Populate the SSP cluster map.
		for(SSPCluster sspCluster : sspClusters) {
			// Get the cluster position.
			Point position = new Point(sspCluster.getXIndex(), sspCluster.getYIndex());
			
			// Get the list for this cluster position.
			ArrayList<SSPCluster> sspList = sspClusterMap.get(position);
			if(sspList == null) {
				sspList = new ArrayList<SSPCluster>();
				sspClusterMap.put(position, sspList);
			}
			
			// Add the cluster to the list.
			sspList.add(sspCluster);
		}
		
		
		
		// ==========================================================
		// ==== Perform Cluster Matching ============================
		// ==========================================================
		
		// For each reconstructed cluster, attempt to match the clusters
		// with SSP clusters at the same position.
		positionLoop:
		for(Entry<Point, ArrayList<Cluster>> clusterSet : reconClusterMap.entrySet()) {
			// Get the reconstructed and SSP clusters at this position.
			ArrayList<Cluster> reconList = clusterSet.getValue();
			ArrayList<SSPCluster> sspList = sspClusterMap.get(clusterSet.getKey());
			
			System.out.println();
			System.out.printf("Considering clusters at (%3d, %3d)%n", clusterSet.getKey().x, clusterSet.getKey().y);
			
			// If there are no SSP clusters, then matching fails by
			// reason of position. The remainder of the loop may be
			// skipped, since there is nothing to match.
			if(sspList == null || sspList.isEmpty()) {
				eventPosition += reconList.size();
				continue positionLoop;
			}
			
			// Get all possible permutations of SSP clusters.
			List<List<Pair>> permutations = getPermutations(reconList, sspList);
			
			System.out.printf("\tRecon Clusters :: %d%n", reconList.size());
			System.out.printf("\tSSP Clusters   :: %d%n", sspList.size());
			System.out.printf("\tPermutations   :: %d%n", permutations.size());
			
			// Track the best results found over all permutations.
			int positionMatched = -1;
			int postionEnergy = -1;
			int postionHitCount = -1;
			StringBuffer positionMatchedText = new StringBuffer();
			
			// Iterate over the permutations and find the permutation
			// that produces the best possible result when compared to
			// the reconstructed clusters.
			int permIndex = 0;
			for(List<Pair> pairs : permutations) {
				permIndex++;
				
				// Track the results of this iteration.
				int permutationMatched = 0;
				int permutationEnergy = 0;
				int permutationHitCount = 0;
				StringBuffer permutationMatchedText = new StringBuffer();
				
				// Try to match each pair.
				for(Pair pair : pairs) {
					
					System.out.printf("\tP%d :: %s --> %s", permIndex, reconClusterToString(pair.reconCluster),
							sspClusterToString(pair.sspCluster));
					
					// Check if the reconstructed cluster has an energy
					// within the allotted threshold of the SSP cluster.
					if(pair.sspCluster.getEnergy() >= pair.reconCluster.getEnergy() * (1 - energyAcceptance) &&
							pair.sspCluster.getEnergy() <= pair.reconCluster.getEnergy() * (1 + energyAcceptance)) {
						// Check that the hit count of the reconstructed
						// is within the allotted threshold of the SSP
						// cluster.
						if(pair.sspCluster.getHitCount() >= pair.reconCluster.getCalorimeterHits().size() - hitAcceptance &&
								pair.sspCluster.getHitCount() <= pair.reconCluster.getCalorimeterHits().size() + hitAcceptance) {
							// Having passed the position, energy, and
							// hit count tests, this cluster pair is
							// designated a match.
							permutationMatched++;
							
							// Output the matched cluster to a string
							// to be printed in the event summary.
							permutationMatchedText.append(String.format("\t%s --> %s%n",
									reconClusterToString(pair.reconCluster),
									sspClusterToString(pair.sspCluster)));
							
							System.out.printf(" [ %18s ]%n", "success: matched");
						}
						
						// Otherwise, this results in an iteration-
						// level match failure by reason of hit count.
						else {
							permutationHitCount++;
							System.out.printf(" [ %18s ]%n", "failure: hit count");
						}
					}
					
					// Otherwise, this results in an iteration-level
					// match failure by reason of energy.
					else {
						permutationEnergy++;
						System.out.printf(" [ %18s ]%n", "failure: energy");
					}
				}
				
				System.out.printf("\t\tPermutation Matched   :: %d%n", permutationMatched);
				System.out.printf("\t\tPermutation Energy    :: %d%n", permutationEnergy);
				System.out.printf("\t\tPermutation Hit Count :: %d%n", permutationHitCount);
				
				// Check whether the results from this permutation
				// exceed the quality of the last best results. A
				// greater number of matches is always better.
				if(permutationMatched > positionMatched) {
					positionMatched = permutationMatched;
					postionEnergy = permutationEnergy;
					postionHitCount = permutationHitCount;
					positionMatchedText = permutationMatchedText;
				}
				
				// Otherwise, a lesser number that failed by reason
				// of energy is considered better. If the same
				// number of clusters matched and the same number
				// failed due to energy, then by definition the same
				// number failed due to hit count, so there is no
				// need to further check for a better event.
				else if(permutationHitCount < postionEnergy) {
					positionMatched = permutationMatched;
					postionEnergy = permutationEnergy;
					postionHitCount = permutationHitCount;
					positionMatchedText = permutationMatchedText;
				}
			}
			
			System.out.printf("\tPosition Matched   :: %d%n", positionMatched);
			System.out.printf("\tPosition Energy    :: %d%n", postionEnergy);
			System.out.printf("\tPosition Hit Count :: %d%n", postionHitCount);
			
			// Add the results from the best-matched permutation
			// to the event efficiency results.
			eventMatched += positionMatched;
			eventEnergy += postionEnergy;
			eventHitCount += postionHitCount;
			eventMatchedText.append(positionMatchedText.toString());
		}
		
		// Add the event results to the global results.
		globalMatched += eventMatched;
		globalPosition += eventPosition;
		globalEnergy += eventEnergy;
		globalHitCount += eventHitCount;
		globalFound += (reconClusters.size() - unverifiedClusters.size());
		
		
		
		// ==========================================================
		// ==== Output Event Summary ================================
		// ==========================================================
		
		// Print the valid reconstructed clusters.
		System.out.println();
		System.out.println("Verified Reconstructed Clusters:");
		if(unverifiedClusters.size() != reconClusters.size()) {
			for(Cluster reconCluster : reconClusters) {
				if(!unverifiedClusters.contains(reconCluster)) {
					System.out.printf("\t%s%n", reconClusterToString(reconCluster));
				}
			}
		} else { System.out.println("\tNone"); }
		
		// Print the unverified clusters.
		System.out.println("Unverified Reconstructed Clusters:");
		if(!unverifiedClusters.isEmpty()) {
			for(Cluster reconCluster : reconClusters) {
				if(unverifiedClusters.contains(reconCluster)) {
					System.out.printf("\t%s%n", reconClusterToString(reconCluster));
				}
			}
		} else { System.out.println("\tNone"); }
		
		// Print the SSP clusters.
		System.out.println("SSP Clusters:");
		if(!sspClusters.isEmpty()) {
			for(SSPCluster sspCluster : sspClusters) {
				System.out.printf("\t%s%n", sspClusterToString(sspCluster));
			}
		} else { System.out.println("\tNone"); }
		
		// Print the matched clusters.
		System.out.println("Matched Clusters:");
		if(eventMatchedText.length() != 0) {
			System.out.print(eventMatchedText.toString());
		} else { System.out.println("\tNone"); }
		
		// Print event statistics.
		System.out.println();
		System.out.println("Event Statistics:");
		System.out.printf("\tRecon Clusters     :: %d%n", (reconClusters.size() - unverifiedClusters.size()));
		System.out.printf("\tClusters Matched   :: %d%n", eventMatched);
		System.out.printf("\tFailed (Position)  :: %d%n", eventPosition);
		System.out.printf("\tFailed (Energy)    :: %d%n", eventEnergy);
		System.out.printf("\tFailed (Hit Count) :: %d%n", eventHitCount);
		System.out.printf("\tCluster Efficiency :: %3.0f%%%n", 100.0 * eventMatched / (reconClusters.size() - unverifiedClusters.size()));
	}
	
	/**
	 * Checks triggers simulated on SSP clusters against the SSP bank's
	 * reported triggers to verify that the trigger is correctly applying
	 * cuts to the clusters it sees. Additionally compares triggers
	 * simulated on reconstructed clusters to measure trigger efficiency.
	 */
	private void singlesTriggerVerification() {
		// ==========================================================
		// ==== Initialize Singles Trigger Verification =============
		// ==========================================================
		
		// Print the cluster verification header.
		System.out.println();
		System.out.println();
		System.out.println("======================================================================");
		System.out.println("=== Singles Trigger Verification =====================================");
		System.out.println("======================================================================");
		
		// Track the number of triggers seen and the number found.
		int sspReportedTriggers = 0;
		int sspInternalMatched = 0;
		int reconTriggersMatched = 0;
		
		
		
		// ==========================================================
		// ==== Output Event Summary ================================
		// ==========================================================
		
		// Get the list of triggers reported by the SSP.
		List<SSPTrigger> sspTriggers = sspBank.getTriggers();
		
		// Output the SSP cluster singles triggers.
		System.out.println();
		System.out.println("SSP Cluster Singles Triggers");
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			for(SinglesTrigger<SSPCluster> simTrigger : sspSinglesTriggers.get(triggerNum)) {
				System.out.printf("\tTrigger %d :: %s :: EClusterLow: %d; EClusterHigh %d; HitCount: %d%n",
						(triggerNum + 1), sspClusterPositionString(simTrigger.getTriggerSource()),
						simTrigger.getStateClusterEnergyLow() ? 1 : 0,
						simTrigger.getStateClusterEnergyHigh() ? 1 : 0,
						simTrigger.getStateHitCount() ? 1 : 0);
			}
		}
		if(sspSinglesTriggers.get(0).size() + sspSinglesTriggers.get(1).size() == 0) {
			System.out.println("\tNone");
		}
		
		// Output the reconstructed cluster singles triggers.
		System.out.println("Reconstructed Cluster Singles Triggers");
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			for(SinglesTrigger<Cluster> simTrigger : reconSinglesTriggers.get(triggerNum)) {
				System.out.printf("\tTrigger %d :: %s :: EClusterLow: %d; EClusterHigh %d; HitCount: %d%n",
						(triggerNum + 1), reconClusterPositionString(simTrigger.getTriggerSource()),
						simTrigger.getStateClusterEnergyLow() ? 1 : 0,
						simTrigger.getStateClusterEnergyHigh() ? 1 : 0,
						simTrigger.getStateHitCount() ? 1 : 0);
			}
		}
		if(reconSinglesTriggers.get(0).size() + reconSinglesTriggers.get(1).size() == 0) {
			System.out.println("\tNone");
		}
		
		// Output the SSP reported triggers.
		System.out.println("SSP Reported Singles Triggers");
		for(SSPTrigger sspTrigger : sspTriggers) {
			if(sspTrigger instanceof SSPSinglesTrigger) {
				// Cast the trigger to a singles trigger.
				SSPSinglesTrigger sspSingles = (SSPSinglesTrigger) sspTrigger;
				
				// Increment the number of SSP cluster singles triggers.
				sspReportedTriggers++;
				
				// Get the trigger properties.
				int triggerNum = sspSingles.isFirstTrigger() ? 1 : 2;
				
				// Print the trigger.
				System.out.printf("\tTrigger %d :: %3d ns :: EClusterLow: %d; EClusterHigh %d; HitCount: %d%n",
						triggerNum, sspSingles.getTime(), sspSingles.passCutEnergyMin() ? 1 : 0,
						sspSingles.passCutEnergyMax() ? 1 : 0, sspSingles.passCutHitCount() ? 1 : 0);
			}
		}
		if(sspReportedTriggers == 0) { System.out.println("\tNone"); }
		
		
		
		// ==========================================================
		// ==== SSP Internal Logic Verification =====================
		// ==========================================================
		
		// Track which SSP triggers have been matched to avoid matching
		// multiple reconstructed SSP cluster triggers to the same SSP
		// trigger.
		Set<SSPSinglesTrigger> sspTriggerSet = new HashSet<SSPSinglesTrigger>();
		
		// Iterate over the triggers.
		System.out.println();
		System.out.println("SSP Reported Trigger --> SSP Cluster Trigger Match Status");
		for(SSPTrigger sspTrigger : sspTriggers) {
			// If the trigger is a singles trigger, convert it.
			if(sspTrigger instanceof SSPSinglesTrigger) {
				// Cast the trigger to a singles trigger.
				SSPSinglesTrigger sspSingles = (SSPSinglesTrigger) sspTrigger;
				int triggerNum = sspSingles.isFirstTrigger() ? 0 : 1;
				boolean matchedTrigger = false;
				
				// Iterate over the SSP cluster simulated triggers and
				// look for a trigger that matches.
				matchLoop:
				for(SinglesTrigger<SSPCluster> simTrigger : sspSinglesTriggers.get(triggerNum)) {
					// If the current SSP trigger has already been
					// matched, skip it.
					if(sspTriggerSet.contains(sspSingles)) { continue matchLoop; }
					
					// Otherwise, check whether the reconstructed SSP
					// cluster trigger matches the SSP trigger.
					if(compareSSPSinglesTriggers(sspSingles, simTrigger)) {
						matchedTrigger = true;
						sspTriggerSet.add(sspSingles);
						sspInternalMatched++;
						break matchLoop;
					}
				}
				
				System.out.printf("\tTrigger %d :: %3d :: EClusterLow: %d; EClusterHigh %d; HitCount: %d :: Matched: %5b%n",
						(triggerNum + 1), sspSingles.getTime(), sspSingles.passCutEnergyMin() ? 1 : 0,
						sspSingles.passCutEnergyMax() ? 1 : 0, sspSingles.passCutHitCount() ? 1 : 0,
						matchedTrigger);
			}
		}
		
		
		
		// ==========================================================
		// ==== SSP Singles Trigger Efficiency ======================
		// ==========================================================
		
		// Reset the SSP matched trigger set.
		sspTriggerSet.clear();
		
		// Iterate over the reconstructed cluster singles triggers.
		System.out.println();
		System.out.println("Recon Cluster Trigger --> SSP Reported Trigger Match Status");
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			for(SinglesTrigger<Cluster> simTrigger : reconSinglesTriggers.get(triggerNum)) {
				// Track whether the trigger was matched.
				boolean matchedTrigger = false;
				
				// Iterate over the SSP reported triggers and compare
				// them to the reconstructed cluster simulated trigger.
				matchLoop:
				for(SSPTrigger sspTrigger : sspTriggers) {
					// Only compare singles triggers.
					if(sspTrigger instanceof SSPSinglesTrigger) {
						// Cast the trigger.
						SSPSinglesTrigger sspSingles = (SSPSinglesTrigger) sspTrigger;
						
						// Only compare the singles trigger if it was
						// not already matched to another trigger.
						if(sspTriggerSet.contains(sspSingles)) { continue matchLoop; }
						
						// Compare the triggers.
						if(compareReconSinglesTriggers(sspSingles, simTrigger)) {
							reconTriggersMatched++;
							matchedTrigger = true;
							sspTriggerSet.add(sspSingles);
							break matchLoop;
						}
					}
				}
				
				// Print the trigger matching status.
				System.out.printf("\tTrigger %d :: %s :: EClusterLow: %d; EClusterHigh %d; HitCount: %d :: Matched: %5b%n",
						(triggerNum + 1), reconClusterPositionString(simTrigger.getTriggerSource()),
						simTrigger.getStateClusterEnergyLow() ? 1 : 0,
						simTrigger.getStateClusterEnergyHigh() ? 1 : 0,
						simTrigger.getStateHitCount() ? 1 : 0, matchedTrigger);
			}
		}
		
		
		
		// ==========================================================
		// ==== Output Event Results ================================
		// ==========================================================
		
		// Get the number of SSP and reconstructed cluster simulated
		// triggers.
		int sspSimTriggers = sspSinglesTriggers.get(0).size() + sspSinglesTriggers.get(1).size();
		int reconSimTriggers = reconSinglesTriggers.get(0).size() + reconSinglesTriggers.get(1).size();
		
		// Print event statistics.
		System.out.println();
		System.out.println("Event Statistics:");
		System.out.printf("\tSSP Cluster Sim Triggers                     :: %d%n", sspSimTriggers);
		System.out.printf("\tRecon Cluster Sim Triggers                   :: %d%n", reconSimTriggers);
		System.out.printf("\tSSP Reported Triggers                        :: %d%n", sspReportedTriggers);
		if(sspSimTriggers == 0) {
			System.out.printf("\tSSP Cluster Trigger   > SSP Reported Trigger :: %d / %d (N/A)%n",
					sspInternalMatched, sspSimTriggers);
		} else {
			System.out.printf("\tSSP Cluster Trigger   > SSP Reported Trigger :: %d / %d (%3.0f%%)%n",
					sspInternalMatched, sspSimTriggers, (100.0 * sspInternalMatched / sspSimTriggers));
		}
		if(reconSimTriggers == 0) {
			System.out.printf("\tRecon Cluster Trigger > SSP Reported Trigger :: %d / %d (N/A)%n",
					reconTriggersMatched, reconSimTriggers);
		} else {
			System.out.printf("\tRecon Cluster Trigger > SSP Reported Trigger :: %d / %d (%3.0f%%)%n",
					reconTriggersMatched, reconSimTriggers, (100.0 * reconTriggersMatched / reconSimTriggers));
		}
		
		// Update the global trigger tracking variables.
		singlesSSPTriggers += sspSimTriggers;
		singlesReconMatched += reconTriggersMatched;
		singlesReconTriggers += reconSimTriggers;
		singlesInternalMatched += sspInternalMatched;
		singlesReportedTriggers += sspReportedTriggers;
	}
	
	/**
	 * Checks triggers simulated on SSP clusters against the SSP bank's
	 * reported triggers to verify that the trigger is correctly applying
	 * cuts to the clusters it sees. Additionally compares triggers
	 * simulated on reconstructed clusters to measure trigger efficiency.
	 */
	private void pairTriggerVerification() {
		// ==========================================================
		// ==== Initialize Pair Trigger Verification ===============
		// ==========================================================
		
		// Print the cluster verification header.
		System.out.println();
		System.out.println();
		System.out.println("======================================================================");
		System.out.println("=== Pair Trigger Verification ========================================");
		System.out.println("======================================================================");
		
		// Track the number of triggers seen and the number found.
		int sspReportedTriggers = 0;
		int sspInternalMatched = 0;
		int reconTriggersMatched = 0;
		
		
		
		// ==========================================================
		// ==== Output Event Summary ================================
		// ==========================================================
		
		// Get the list of triggers reported by the SSP.
		List<SSPTrigger> sspTriggers = sspBank.getTriggers();
		
		// Output the SSP cluster pair triggers.
		System.out.println();
		System.out.println("SSP Cluster Pair Triggers");
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			for(PairTrigger<SSPCluster[]> simTrigger : sspPairsTriggers.get(triggerNum)) {
				System.out.printf("\tTrigger %d :: %s, %s :: EClusterLow: %d; EClusterHigh %d; HitCount: %d; ESumLow: %d, ESumHigh: %d, EDiff: %d, ESlope: %d, Coplanarity: %d%n",
						(triggerNum + 1), sspClusterPositionString(simTrigger.getTriggerSource()[0]),
						sspClusterPositionString(simTrigger.getTriggerSource()[1]),
						simTrigger.getStateClusterEnergyLow() ? 1 : 0,
						simTrigger.getStateClusterEnergyHigh() ? 1 : 0,
						simTrigger.getStateHitCount() ? 1 : 0,
						simTrigger.getStateEnergySumLow() ? 1 : 0,
						simTrigger.getStateEnergySumHigh() ? 1 : 0,
						simTrigger.getStateEnergyDifference() ? 1 : 0,
						simTrigger.getStateEnergySlope() ? 1 : 0,
						simTrigger.getStateCoplanarity() ? 1 : 0);
			}
		}
		if(sspPairsTriggers.get(0).size() + sspPairsTriggers.get(1).size() == 0) {
			System.out.println("\tNone");
		}
		
		// Output the reconstructed cluster singles triggers.
		System.out.println("Reconstructed Cluster Pair Triggers");
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			for(PairTrigger<Cluster[]> simTrigger : reconPairsTriggers.get(triggerNum)) {
				System.out.printf("\tTrigger %d :: %s, %s :: EClusterLow: %d; EClusterHigh %d; HitCount: %d; ESumLow: %d, ESumHigh: %d, EDiff: %d, ESlope: %d, Coplanarity: %d%n",
						(triggerNum + 1), reconClusterPositionString(simTrigger.getTriggerSource()[0]),
						reconClusterPositionString(simTrigger.getTriggerSource()[1]),
						simTrigger.getStateClusterEnergyLow() ? 1 : 0,
						simTrigger.getStateClusterEnergyHigh() ? 1 : 0,
						simTrigger.getStateHitCount() ? 1 : 0,
						simTrigger.getStateEnergySumLow() ? 1 : 0,
						simTrigger.getStateEnergySumHigh() ? 1 : 0,
						simTrigger.getStateEnergyDifference() ? 1 : 0,
						simTrigger.getStateEnergySlope() ? 1 : 0,
						simTrigger.getStateCoplanarity() ? 1 : 0);
			}
		}
		if(reconPairsTriggers.get(0).size() + reconPairsTriggers.get(1).size() == 0) {
			System.out.println("\tNone");
		}
		
		// Output the SSP reported triggers.
		System.out.println("SSP Reported Pair Triggers");
		for(SSPTrigger sspTrigger : sspTriggers) {
			if(sspTrigger instanceof SSPPairTrigger) {
				// Cast the trigger to a singles trigger.
				SSPPairTrigger sspPair = (SSPPairTrigger) sspTrigger;
				
				// Increment the number of SSP cluster singles triggers.
				sspReportedTriggers++;
				
				// Get the trigger properties.
				int triggerNum = sspPair.isFirstTrigger() ? 1 : 2;
				
				// Print the trigger.
				System.out.printf("\tTrigger %d :: %3d ns :: ESum: %d, EDiff: %d, ESlope: %d, Coplanarity: %d%n",
						triggerNum, sspPair.getTime(),
						sspPair.passCutEnergySum() ? 1 : 0, sspPair.passCutEnergyDifference() ? 1 : 0,
						sspPair.passCutEnergySlope() ? 1 : 0, sspPair.passCutCoplanarity() ? 1 : 0);
			}
		}
		if(sspReportedTriggers == 0) { System.out.println("\tNone"); }
		
		
		
		// ==========================================================
		// ==== SSP Internal Logic Verification =====================
		// ==========================================================
		
		// Track which SSP triggers have been matched to avoid matching
		// multiple reconstructed SSP cluster triggers to the same SSP
		// trigger.
		Set<SSPPairTrigger> sspTriggerSet = new HashSet<SSPPairTrigger>();
		
		// Iterate over the triggers.
		System.out.println();
		System.out.println("SSP Reported Trigger --> SSP Cluster Trigger Match Status");
		for(SSPTrigger sspTrigger : sspTriggers) {
			// If the trigger is a pair trigger, convert it.
			if(sspTrigger instanceof SSPPairTrigger) {
				// Cast the trigger to a pair trigger.
				SSPPairTrigger sspPair = (SSPPairTrigger) sspTrigger;
				int triggerNum = sspPair.isFirstTrigger() ? 0 : 1;
				boolean matchedTrigger = false;
				
				// Iterate over the SSP cluster simulated triggers and
				// look for a trigger that matches.
				matchLoop:
				for(PairTrigger<SSPCluster[]> simTrigger : sspPairsTriggers.get(triggerNum)) {
					// If the current SSP trigger has already been
					// matched, skip it.
					if(sspTriggerSet.contains(sspPair)) { continue matchLoop; }
					
					// Otherwise, check whether the reconstructed SSP
					// cluster trigger matches the SSP trigger.
					if(compareSSPPairTriggers(sspPair, simTrigger)) {
						matchedTrigger = true;
						sspTriggerSet.add(sspPair);
						sspInternalMatched++;
						break matchLoop;
					}
				}
				
				System.out.printf("\tTrigger %d :: %3d ns :: ESum: %d, EDiff: %d, ESlope: %d, Coplanarity: %d :: Matched: %5b%n",
						triggerNum, sspPair.getTime(), sspPair.passCutEnergySum() ? 1 : 0,
						sspPair.passCutEnergyDifference() ? 1 : 0, sspPair.passCutEnergySlope() ? 1 : 0,
						sspPair.passCutCoplanarity() ? 1 : 0, matchedTrigger);
			}
		}
		
		
		
		// ==========================================================
		// ==== SSP Pair Trigger Efficiency =========================
		// ==========================================================
		
		// Reset the SSP matched trigger set.
		sspTriggerSet.clear();
		
		// Iterate over the reconstructed cluster pair triggers.
		System.out.println();
		System.out.println("Recon Cluster Trigger --> SSP Reported Trigger Match Status");
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			for(PairTrigger<Cluster[]> simTrigger : reconPairsTriggers.get(triggerNum)) {
				// Track whether the trigger was matched.
				boolean matchedTrigger = false;
				
				// Iterate over the SSP reported triggers and compare
				// them to the reconstructed cluster simulated trigger.
				matchLoop:
				for(SSPTrigger sspTrigger : sspTriggers) {
					// Only compare pair triggers.
					if(sspTrigger instanceof SSPPairTrigger) {
						// Cast the trigger.
						SSPPairTrigger sspPair = (SSPPairTrigger) sspTrigger;
						
						// Only compare the pair trigger if it was
						// not already matched to another trigger.
						if(sspTriggerSet.contains(sspPair)) { continue matchLoop; }
						
						// Compare the triggers.
						if(compareReconPairTriggers(sspPair, simTrigger)) {
							reconTriggersMatched++;
							matchedTrigger = true;
							sspTriggerSet.add(sspPair);
							break matchLoop;
						}
					}
				}
				
				// Print the trigger matching status.
				System.out.printf("\tTrigger %d :: %s, %s :: EClusterLow: %d; EClusterHigh %d; HitCount: %d; ESumLow: %d, ESumHigh: %d, EDiff: %d, ESlope: %d, Coplanarity: %d :: Matched: %5b%n",
						(triggerNum + 1), reconClusterPositionString(simTrigger.getTriggerSource()[0]),
						reconClusterPositionString(simTrigger.getTriggerSource()[1]),
						simTrigger.getStateClusterEnergyLow() ? 1 : 0,
						simTrigger.getStateClusterEnergyHigh() ? 1 : 0,
						simTrigger.getStateHitCount() ? 1 : 0,
						simTrigger.getStateEnergySumLow() ? 1 : 0,
						simTrigger.getStateEnergySumHigh() ? 1 : 0,
						simTrigger.getStateEnergyDifference() ? 1 : 0,
						simTrigger.getStateEnergySlope() ? 1 : 0,
						simTrigger.getStateCoplanarity() ? 1 : 0, matchedTrigger);
			}
		}
		
		
		
		// ==========================================================
		// ==== Output Event Results ================================
		// ==========================================================
		
		// Get the number of SSP and reconstructed cluster simulated
		// triggers.
		int sspSimTriggers = sspPairsTriggers.get(0).size() + sspPairsTriggers.get(1).size();
		int reconSimTriggers = reconPairsTriggers.get(0).size() + reconPairsTriggers.get(1).size();
		
		// Print event statistics.
		System.out.println();
		System.out.println("Event Statistics:");
		System.out.printf("\tSSP Cluster Sim Triggers                     :: %d%n", sspSimTriggers);
		System.out.printf("\tRecon Cluster Sim Triggers                   :: %d%n", reconSimTriggers);
		System.out.printf("\tSSP Reported Triggers                        :: %d%n", sspReportedTriggers);
		if(sspSimTriggers == 0) {
			System.out.printf("\tSSP Cluster Trigger   > SSP Reported Trigger :: %d / %d (N/A)%n",
					sspInternalMatched, sspSimTriggers);
		} else {
			System.out.printf("\tSSP Cluster Trigger   > SSP Reported Trigger :: %d / %d (%3.0f%%)%n",
					sspInternalMatched, sspSimTriggers, (100.0 * sspInternalMatched / sspSimTriggers));
		}
		if(reconSimTriggers == 0) {
			System.out.printf("\tRecon Cluster Trigger > SSP Reported Trigger :: %d / %d (N/A)%n",
					reconTriggersMatched, reconSimTriggers);
		} else {
			System.out.printf("\tRecon Cluster Trigger > SSP Reported Trigger :: %d / %d (%3.0f%%)%n",
					reconTriggersMatched, reconSimTriggers, (100.0 * reconTriggersMatched / reconSimTriggers));
		}
		
		// Update the global trigger tracking variables.
		pairSSPTriggers += sspSimTriggers;
		pairReconMatched += reconTriggersMatched;
		pairReconTriggers += reconSimTriggers;
		pairInternalMatched += sspInternalMatched;
		pairReportedTriggers += sspReportedTriggers;
	}
	
	/**
	 * Generates and stores the singles triggers for both reconstructed
	 * and SSP clusters.
	 */
	private void constructSinglesTriggers() {
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
		for(Cluster[] reconPair : reconPairs) {
			reconTriggerLoop:
			for(int triggerIndex = 0; triggerIndex < 2; triggerIndex++) {
				// Check that the pair passes the time coincidence cut.
				// If it does not, it is not a valid pair and should be
				// destroyed.
				if(!pairsTrigger[triggerIndex].pairTimeCoincidenceCut(reconPair)) {
					continue reconTriggerLoop;
				}
				
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
			}
		}
		
		for(SSPCluster[] sspPair : sspPairs) {
			pairTriggerLoop:
			for(int triggerIndex = 0; triggerIndex < 2; triggerIndex++) {
				// Check that the pair passes the time coincidence cut.
				// If it does not, it is not a valid pair and should be
				// destroyed.
				if(!pairsTrigger[triggerIndex].pairTimeCoincidenceCut(sspPair)) {
					continue pairTriggerLoop;
				}
				
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
				boolean passPairEnergySlope = pairsTrigger[triggerIndex].pairEnergySlopeCut(sspPair);
				boolean passPairCoplanarity = pairsTrigger[triggerIndex].pairCoplanarityCut(sspPair);
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
				sspPairsTriggers.get(triggerIndex).add(trigger);
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
		System.out.println("Cluster Verification Settings");
		System.out.printf("\tEnergy Threshold       :: %1.2f%n", energyAcceptance);
		System.out.printf("\tHit Threshold          :: %1d%n", hitAcceptance);
		
		// Output window settings.
		System.out.println("FADC Timing Window Settings");
		System.out.printf("\tNSB                    :: %3d ns%n", nsb);
		System.out.printf("\tNSA                    :: %3d ns%n", nsa);
		System.out.printf("\tFADC Window            :: %3d ns%n", windowWidth);
		
		// Calculate the valid clustering window.
		int start = nsb;
		int end = windowWidth - nsa;
		if(start < end) {
			System.out.printf("\tValid Cluster Window   :: [ %3d ns, %3d ns ]%n", start, end);
			performClusterVerification = true;
		} else {
			System.out.println("\tNSB, NSA, and FADC window preclude a valid cluster verification window.");
			System.out.println("\tCluster verification will not be performed!");
			performClusterVerification = false;
		}
		
		// Output the singles trigger settings.
		for(int i = 0; i < 2; i++) {
			System.out.printf("Singles Trigger %d Settings%n", (i + 1));
			System.out.printf("\tCluster Energy Low     :: %.3f GeV%n", singlesTrigger[i].getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW));
			System.out.printf("\tCluster Energy High    :: %.3f GeV%n", singlesTrigger[i].getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH));
			System.out.printf("\tCluster Hit Count      :: %.0f hits%n", singlesTrigger[i].getCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW));
		}
		
		// Output the pair trigger settings.
		for(int i = 0; i < 2; i++) {
			System.out.printf("Pairs Trigger %d Settings%n", (i + 1));
			System.out.printf("\tCluster Energy Low     :: %.3f GeV%n", pairsTrigger[i].getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW));
			System.out.printf("\tCluster Energy High    :: %.3f GeV%n", pairsTrigger[i].getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH));
			System.out.printf("\tCluster Hit Count      :: %.0f hits%n", pairsTrigger[i].getCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW));
			System.out.printf("\tPair Energy Sum Low    :: %.3f GeV%n", pairsTrigger[i].getCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW));
			System.out.printf("\tPair Energy Sum Low    :: %.3f GeV%n", pairsTrigger[i].getCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH));
			System.out.printf("\tPair Energy Difference :: %.3f GeV%n", pairsTrigger[i].getCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH));
			System.out.printf("\tPair Energy Slope      :: %.3f GeV%n", pairsTrigger[i].getCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW));
			System.out.printf("\tPair Energy Slope F    :: %.3f GeV / mm%n", pairsTrigger[i].getCutValue(TriggerModule.PAIR_ENERGY_SLOPE_F));
			System.out.printf("\tPair Coplanarity       :: %.0f Degrees%n", pairsTrigger[i].getCutValue(TriggerModule.PAIR_COPLANARITY_HIGH));
			System.out.printf("\tPair Time Coincidence  :: %.0f ns%n", pairsTrigger[i].getCutValue(TriggerModule.PAIR_TIME_COINCIDENCE));
		}
	}
	
	/**
	 * Convenience method that writes the information in a cluster to
	 * a <code>String</code>.
	 * @param cluster - The cluster.
	 * @return Returns the cluster information as a <code>String</code>.
	 */
	private static final String reconClusterToString(Cluster cluster) {
		return String.format("Cluster at (%3d, %3d) with %.3f GeV and %d hits at %4.0f ns.",
				cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix"),
				cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy"),
				cluster.getEnergy(), cluster.getCalorimeterHits().size(),
				cluster.getCalorimeterHits().get(0).getTime());
	}
	
	/**
	 * Convenience method that writes the information in a cluster to
	 * a <code>String</code>.
	 * @param cluster - The cluster.
	 * @return Returns the cluster information as a <code>String</code>.
	 */
	private static final String sspClusterToString(SSPCluster cluster) {
		return String.format("Cluster at (%3d, %3d) with %.3f GeV and %d hits at %4d ns.",
				cluster.getXIndex(), cluster.getYIndex(), cluster.getEnergy(),
				cluster.getHitCount(), cluster.getTime());
	}
	
	/**
	 * Convenience method that writes the position of a reconstructed
	 * cluster in the form (ix, iy).
	 * @param cluster - The cluster.
	 * @return Returns the cluster position as a <code>String</code>.
	 */
	private static final String reconClusterPositionString(Cluster cluster) {
		return String.format("(%3d, %3d)",
				cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix"),
				cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy"));
	}
	
	/**
	 * Convenience method that writes the position of an SSP bank
	 * cluster in the form (ix, iy).
	 * @param cluster - The cluster.
	 * @return Returns the cluster position as a <code>String</code>.
	 */
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
	
	/**
	 * Compares a trigger from the SSP bank to a trigger simulated on
	 * an SSP cluster.
	 * @param bankTrigger - The trigger from the SSP bank.
	 * @param simTrigger - The trigger from the simulation.
	 * @return Returns <code>true</code> if the triggers match and
	 * <code>false</code> if they do not.
	 */
	private static final boolean compareSSPPairTriggers(SSPPairTrigger bankTrigger, PairTrigger<SSPCluster[]> simTrigger) {
		// Get the time of the bottom cluster in the pair.
		int simTime = 0;
		if(simTrigger.getTriggerSource()[0].getYIndex() < 0) {
			simTime = simTrigger.getTriggerSource()[0].getTime();
		} else {
			simTime = simTrigger.getTriggerSource()[1].getTime();
		}
		
		// The bank trigger and simulated trigger must have the same
		// time. This is equivalent to the time of the triggering cluster.
		if(bankTrigger.getTime() != simTime) { return false; }
		
		// If the time stamp is the same, check that the trigger flags
		// are all the same. Start with energy sum.
		if(bankTrigger.passCutEnergySum() != simTrigger.getStateEnergySum()) {
			return false;
		}
		
		// Check pair energy difference.
		if(bankTrigger.passCutEnergyDifference() != simTrigger.getStateEnergyDifference()) {
			return false;
		}
		
		// Check pair energy slope.
		if(bankTrigger.passCutEnergySlope() != simTrigger.getStateEnergySlope()) {
			return false;
		}
		
		// Check pair coplanarity.
		if(bankTrigger.passCutCoplanarity() != simTrigger.getStateCoplanarity()) {
			return false;
		}
		
		// If all of the tests are successful, the triggers match.
		return true;
	}
	
	/**
	 * Compares a trigger from the SSP bank to a trigger simulated on
	 * a reconstructed cluster.
	 * @param bankTrigger - The trigger from the SSP bank.
	 * @param simTrigger - The trigger from the simulation.
	 * @return Returns <code>true</code> if the triggers match and
	 * <code>false</code> if they do not.
	 */
	private static final boolean compareReconSinglesTriggers(SSPSinglesTrigger bankTrigger, SinglesTrigger<Cluster> simTrigger) {
		// Check that the trigger flags are all the same. Start with
		// cluster energy low.
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
	
	/**
	 * Compares a trigger from the SSP bank to a trigger simulated on
	 * an reconstructed cluster.
	 * @param bankTrigger - The trigger from the SSP bank.
	 * @param simTrigger - The trigger from the simulation.
	 * @return Returns <code>true</code> if the triggers match and
	 * <code>false</code> if they do not.
	 */
	private static final boolean compareReconPairTriggers(SSPPairTrigger bankTrigger, PairTrigger<Cluster[]> simTrigger) {
		// Check that the trigger flags are all the same. Start with
		// energy sum.
		if(bankTrigger.passCutEnergySum() != simTrigger.getStateEnergySum()) {
			return false;
		}
		
		// Check pair energy difference.
		if(bankTrigger.passCutEnergyDifference() != simTrigger.getStateEnergyDifference()) {
			return false;
		}
		
		// Check pair energy slope.
		if(bankTrigger.passCutEnergySlope() != simTrigger.getStateEnergySlope()) {
			return false;
		}
		
		// Check pair coplanarity.
		if(bankTrigger.passCutCoplanarity() != simTrigger.getStateCoplanarity()) {
			return false;
		}
		
		// If all of the tests are successful, the triggers match.
		return true;
	}
	
	/**
	 * Generates a <code>List</code> collection that contains a set
	 * of <code>ArrayList</code> collections representing a unique
	 * permutation of the entries in the argument.
	 * @param values - A collection of the entries to be permuted.
	 * @return Returns a list of lists representing the permutations.
	 */
	private static final List<List<Pair>> getPermutations(ArrayList<Cluster> reconClusters, ArrayList<SSPCluster> sspClusters) {
		// Store the SSP cluster permutations.
		List<ArrayList<SSPCluster>> permList = new ArrayList<ArrayList<SSPCluster>>();
		
		// Get the SSP cluster permutations.
		permute(new ArrayList<SSPCluster>(0), sspClusters, permList);
		
		// Create pairs from the permutations.
		List<List<Pair>> pairList = new ArrayList<List<Pair>>();
		for(ArrayList<SSPCluster> permutation : permList) {
			List<Pair> pairs = new ArrayList<Pair>(reconClusters.size());
			
			for(int clusterIndex = 0; (clusterIndex < reconClusters.size() && clusterIndex < permutation.size()); clusterIndex++) {
				pairs.add(new Pair(reconClusters.get(clusterIndex), permutation.get(clusterIndex)));
			}
			
			pairList.add(pairs);
		}
		
		return pairList;
	}
	
	/**
	 * Recursive method for permuting all entries in the argument
	 * collection <code>remainingValues</code> into the argument
	 * <code>permutedValues</code> values. Completed permutations are
	 * placed in the argument <code>permList</code>.
	 * @param permutedValues - List to store entries that have already
	 * been permuted.
	 * @param remainingValues - List to store  entries that need to be
	 * permuted.
	 * @param permList - List to store completed permutations.
	 */
	private static final void permute(ArrayList<SSPCluster> permutedValues, ArrayList<SSPCluster> remainingValues, List<ArrayList<SSPCluster>> permList) {
		// If the list of entries that still need to be sorted is empty,
		// then there is nothing to sort. Just return and empty list.
		if(remainingValues.isEmpty()) { return; }
		
		// If there is only one value left in the list of entries that
		// still need to be sorted, then just add it to the permutation
		// list and return it.
		else if(remainingValues.size() <= 1) {
			// Add the last entry.
			permutedValues.add(remainingValues.get(0));
			
			// Add the permutation to the list of completed permutations.
			permList.add(permutedValues);
		}
		
		// Otherwise, continue to get all possible permutations.
		else {
			// Iterate over the entries that have not been permuted.
			for(int i = 0; i < remainingValues.size(); i++) {
				// Make new lists to contain the permutations.
				ArrayList<SSPCluster> newPermList = new ArrayList<SSPCluster>(permutedValues.size() + 1);
				ArrayList<SSPCluster> newRemainList = new ArrayList<SSPCluster>(remainingValues.size());
				
				// Copy the current permuted entries to the new list
				// and one value from the list of entries that have
				// not been permuted yet.
				newPermList.addAll(permutedValues);
				newPermList.add(remainingValues.get(i));
				
				// The new list of entries that have not been permuted
				// should be identical, except it should now be missing
				// the entry that was moved.
				for(int index = 0; index < remainingValues.size(); index++) {
					if(index != i) { newRemainList.add(remainingValues.get(index)); }
				}
				
				// Repeat the process with the new lists.
				permute(newPermList, newRemainList, permList);
			}
		}
	}
	
	/**
	 * Class <code>Pair</code> provides a convenient means of putting
	 * a reconstructed cluster and an SSP cluster in the same object
	 * for cluster matching.
	 * 
	 * @author Kyle McCarty <mccarty@jlab.org>
	 */
	private static class Pair {
		public final Cluster reconCluster;
		public final SSPCluster sspCluster;
		
		/**
		 * Instantiates a <code>Pair</code> consisting of the two
		 * cluster objects specified.
		 * @param reconCluster - A reconstructed cluster.
		 * @param sspCluster - An SSP bank cluster.
		 */
		public Pair(Cluster reconCluster, SSPCluster sspCluster) {
			this.reconCluster = reconCluster;
			this.sspCluster = sspCluster;
		}
	}
}
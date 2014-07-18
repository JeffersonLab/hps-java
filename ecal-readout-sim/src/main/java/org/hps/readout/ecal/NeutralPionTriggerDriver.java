package org.hps.readout.ecal;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.aida.AIDA;

/**
 * Class <code>NeutralPionTriggerDriver</code> simulates a pi-0 trigger.
 * It executes four cuts, two of which are single cluster cuts and two
 * of which are cluster pair cuts. The single cluster cuts are on the
 * total energy of the cluster and the energy of the seed hit of the
 * cluster. The first cluster pair cut is on the sum of the energies of
 * both clusters. The second calculates the invariant mass of the
 * particle that produced the clusters, assuming that clusters were
 * created by an electron/positron pair. The pair is then cut if the
 * invariant mass is outside the expected range for a neutral pion decay.
 * <br/><br/>
 * All incoming clusters are passed through the single cluster cuts and
 * those which survive are added to a list of clusters for their event
 * and stored in a buffer. The buffer stores a number of event lists
 * equal to coincidence window parameter. This limits the time frame
 * in which clusters can be used for a trigger. Of the clusters stored
 * in the cluster buffer, the two with the highest energies are chosen
 * and the cluster pair cuts are applied to them. If the highest energy
 * pair survives this process, the event triggers. If it does not,
 * there is no trigger for the event.
 * <br/><br/>
 * All thresholds can be set through a steering file, along with the
 * coincidence window. The driver also supports a verbose mode where
 * it will output more details with every event to help with diagnostics.
 * 
 * @author Kyle McCarty
 * @author Michel Garcon
 */
public class NeutralPionTriggerDriver extends TriggerDriver {
	
	// ==================================================================
	// ==== Trigger Algorithms ==========================================
	// ==================================================================	
	
	public void process(EventHeader event) {
		// Generate a temporary list to store the good clusters
		// in before they are added to the buffer.
		List<HPSEcalCluster> tempList = new ArrayList<HPSEcalCluster>();
		
		// If the current event has a cluster collection, get it.
		if(event.hasCollection(HPSEcalCluster.class, clusterCollectionName)) {
			// VERBOSE :: Note that a cluster collection exists for
			//            this event.
			if(verbose) { System.out.println("Cluster collection is present for event."); }
			
			// Get the cluster list from the event.
			List<HPSEcalCluster> eventList = event.get(HPSEcalCluster.class, clusterCollectionName);
			
			// VERBOSE :: Output the number of extant clusters.
			if(verbose) { System.out.printf("%d clusters in event.%n", eventList.size()); }
			
			// Add the clusters from the event into the cluster list
			// if they pass the minimum total cluster energy and seed
			// energy thresholds.
			for(HPSEcalCluster cluster : eventList) {
				// Get the cluster position indices.
				int ix = cluster.getSeedHit().getIdentifierFieldValue("ix");
				int iy = cluster.getSeedHit().getIdentifierFieldValue("iy");
				
				// VERBOSE :: Output the current cluster's properties.
				if(verbose) {
					System.out.printf("\tTesting cluster at (%d, %d) with total energy %f and seed energy %f.%n",
							ix, iy, cluster.getSeedHit().getCorrectedEnergy(), cluster.getEnergy());
				}
				
				// Add the clusters to the uncut histograms.
				clusterTotalEnergy.fill(cluster.getEnergy());
				clusterSeedEnergy.fill(cluster.getSeedHit().getCorrectedEnergy());
				clusterDistribution.fill(ix, iy, 1);
				
				// VERBOSE :: Output the single cluster trigger thresholds.
				if(verbose) {
					System.out.printf("\tCluster seed energy threshold  :: %f%n", clusterSeedEnergyThreshold);
					System.out.printf("\tCluster total energy threshold :: %f%n%n", clusterTotalEnergyThreshold);
				}
				
				// Perform the single cluster cuts.
				boolean totalEnergyCut = clusterTotalEnergyCut(cluster);
				boolean seedEnergyCut = clusterSeedEnergyCut(cluster);
				boolean hitCountCut = clusterHitCountCut(cluster);
				
				// VERBOSE :: Note whether the cluster passed the single
				//            cluster cuts.
				if(verbose) {
					System.out.printf("\tPassed seed energy cut    :: %b%n", seedEnergyCut);
					System.out.printf("\tPassed cluster energy cut :: %b%n%n", totalEnergyCut);
					System.out.printf("\tPassed hit count cut :: %b%n%n", hitCountCut);
				}
				
				// If both pass, add the cluster to the list.
				if(totalEnergyCut && seedEnergyCut && hitCountCut) {
					// Add the cluster to the cluster list.
					tempList.add(cluster);
					
					// Add the cluster information to the single cut histograms.
					pClusterTotalEnergy.fill(cluster.getEnergy());
					pClusterSeedEnergy.fill(cluster.getSeedHit().getCorrectedEnergy());
					pClusterDistribution.fill(ix, iy, 1);
				}
			}
		}
		
		// Otherwise, clear the cluster list.
		else {
			// VERBOSE :: Note that the event has no clusters.
			if(verbose) { System.out.println("No cluster collection is present for event.\n"); }
		}
		
		// If the cluster buffer has fewer than the allowed number of
		// events stored, just add the temporary list to the buffer.
		if(clusterBuffer.size() < coincidenceWindow) { clusterBuffer.addLast(tempList); }
		
		// Otherwise, remove the first element of the list (the oldest
		// buffer) and append the new list.
		else {
			clusterBuffer.removeFirst();
			clusterBuffer.addLast(tempList);
		}
		
		// Reset the highest energy pair to null.
		clusterPair[0] = null;
		clusterPair[1] = null;
		
		// Loop over all of the cluster lists in the cluster buffer.
		for(List<HPSEcalCluster> bufferList : clusterBuffer) {
			// Loop over all of the clusters in each buffer list.
			for(HPSEcalCluster cluster : bufferList) {
				// If the first cluster is null, then any cluster
				// automatically counts as the highest energy cluster.
				if(clusterPair[0] == null) { clusterPair[0] = cluster; }
				
				// If the second cluster is null and the first has
				// been populated, add the new cluster to the pair.
				else if(clusterPair[1] == null) {
					// If the new cluster exceeds the first cluster in
					// energy, it gets the first slot and the first
					// cluster is moved to the second slot.
					if(cluster.getEnergy() > clusterPair[0].getEnergy()) {
						clusterPair[1] = clusterPair[0];
						clusterPair[0] = cluster;
					}
					
					// Otherwise, the new cluster gets the second slot.
					else { clusterPair[1] = cluster; }
				}
				
				// If the current cluster has a higher energy than the
				// first cluster in the pair, it is the highest energy
				// cluster of the pair. Replace the second cluster with
				// the first and the first with the new cluster.
				else if(cluster.getEnergy() > clusterPair[0].getEnergy()) {
					clusterPair[1] = clusterPair[0];
					clusterPair[0] = cluster;
				}
				
				// Otherwise, if it has more energy than the second
				// cluster in the pair, it will replace that cluster.
				else if(cluster.getEnergy() > clusterPair[1].getEnergy()) {
					clusterPair[1] = cluster;
				}
			}
		}
		
		// Run the superclass event process.
		super.process(event);
	}
	
	public void startOfData() {
		// Initialize the cluster buffer to the size of the coincidence window.
		clusterBuffer = new LinkedList<List<HPSEcalCluster>>();
		
		// Initialize the cluster total energy diagnostic plots.
		clusterTotalEnergy = aida.histogram1D("Cluster Total Energy Distribution", 44, 0.0, 2.2);
		pClusterTotalEnergy = aida.histogram1D("Cluster Total Energy Distribution (Passed Single Cuts)", 44, 0.0, 2.2);
		aClusterTotalEnergy = aida.histogram1D("Cluster Total Energy Distribution (Passed All Cuts)", 44, 0.0, 2.2);
		
		// Initialize the cluster seed energy diagnostic plots.
		clusterSeedEnergy = aida.histogram1D("Cluster Seed Energy Distribution", 44, 0.0, 2.2);
		pClusterSeedEnergy = aida.histogram1D("Cluster Seed Energy Distribution (Passed Single Cuts)", 44, 0.0, 2.2);
		aClusterSeedEnergy = aida.histogram1D("Cluster Seed Energy Distribution (Passed All Cuts)", 44, 0.0, 2.2);
		
		// Initialize the seed distribution diagnostic plots.
		clusterDistribution = aida.histogram2D("Cluster Seed Distribution", 44, -22.0, 22.0, 10, -5, 5);
		pClusterDistribution = aida.histogram2D("Cluster Seed Distribution (Passed Single Cuts)", 44, -22.0, 22.0, 10, -5, 5);
		aClusterDistribution = aida.histogram2D("Cluster Seed Distribution (Passed All Cuts)", 44, -22.0, 22.0, 10, -5, 5);
		
		// Initialize the cluster pair energy sum diagnostic plots.
		pairEnergySum = aida.histogram1D("Pair Energy Sum Distribution", 88, 0.0, 4.4);
		pPairEnergySum = aida.histogram1D("Pair Energy Sum Distribution (Passed Pair Cuts)", 88, 0.0, 4.4);
		
		// Initialize the cluster pair hypothetical invariant mass diagnostic plots.
		invariantMass = aida.histogram1D("Hypothetical Invariant Mass Distribution", 100, 0.0, 100);
		pInvariantMass = aida.histogram1D("Hypothetical Invariant Mass Distribution (Passed Pair Cuts)", 100, 0.0, 100);
	}
	
	protected boolean triggerDecision(EventHeader event) {
		// If the active cluster pair has a null value, then there were
		// fewer than two clusters in the buffer and we can not trigger.
		if(clusterPair[0] == null || clusterPair[1] == null) {
			// VERBOSE :: Note that triggering failed due to insufficient
			// clusters. in the cluster buffer.
			if(verbose) { System.out.println("Inufficient clusters in buffer -- no trigger."); }
			
			// Return false; we can not trigger without two clusters.
			return false;
		}
		
		// Get the cluster position indices.
		int[] ix = { clusterPair[0].getSeedHit().getIdentifierFieldValue("ix"), clusterPair[1].getSeedHit().getIdentifierFieldValue("ix") };
		int[] iy = { clusterPair[0].getSeedHit().getIdentifierFieldValue("iy"), clusterPair[1].getSeedHit().getIdentifierFieldValue("iy") };
		
		// VERBOSE :: Output the clusters selected for triggering.
		if(verbose) {
			System.out.printf("\tTesting first cluster at (%d, %d) with total energy %f and seed energy %f.%n",
					ix[0], iy[0], clusterPair[0].getSeedHit().getCorrectedEnergy(), clusterPair[0].getEnergy());
			System.out.printf("\tTesting second cluster at (%d, %d) with total energy %f and seed energy %f.%n",
					ix[1], iy[1], clusterPair[1].getSeedHit().getCorrectedEnergy(), clusterPair[1].getEnergy());
		}
		
		// Fill the uncut histograms.
		pairEnergySum.fill(getEnergySumValue(clusterPair));
		invariantMass.fill(getInvariantMassValue(clusterPair));
		
		// VERBOSE :: Output the cluster pair trigger thresholds.
		if(verbose) {
			System.out.printf("\tCluster pair energy sum threshold     :: %f%n", pairEnergySumThreshold);
			System.out.printf("\tHypothetical invariant mass threshold :: [%f, %f]%n%n", invariantMassThresholdLow, invariantMassThresholdHigh);
		}
		
		// Perform the cluster pair checks.
		boolean energySumCut = pairEnergySumCut(clusterPair);
		boolean invariantMassCut = pairInvariantMassCut(clusterPair);
		
		// VERBOSE :: Note the outcome of the trigger cuts.
		if(verbose) {
			System.out.printf("\tPassed energy sum cut     :: %b%n", energySumCut);
			System.out.printf("\tPassed invariant mass cut :: %b%n%n", invariantMassCut);
		}
		
		// If the pair passes both cuts, we have a trigger.
		if(energySumCut && invariantMassCut) {
			// Fill the cut histograms.
			pPairEnergySum.fill(getEnergySumValue(clusterPair));
			pInvariantMass.fill(getInvariantMassValue(clusterPair));
			
			// Fill the all cuts histograms.
			aClusterTotalEnergy.fill(clusterPair[0].getEnergy());
			aClusterTotalEnergy.fill(clusterPair[1].getEnergy());
			aClusterSeedEnergy.fill(clusterPair[0].getSeedHit().getCorrectedEnergy());
			aClusterSeedEnergy.fill(clusterPair[1].getSeedHit().getCorrectedEnergy());
			aClusterDistribution.fill(ix[0], iy[0], 1);
			aClusterDistribution.fill(ix[1], iy[1], 1);
			
			// VERBOSE :: Note that the event has triggered.
			if(verbose) { System.out.println("Event triggers!\n\n"); }
			
			// Return the trigger.
			return true;
		}
		
		// VERBOSE :: Note that the event has failed to trigger.
		if(verbose) { System.out.println("No trigger.\n\n"); }
		
		// If one or more of the pair cuts failed, the we do not trigger.
		return false;
	}
	
	// ==================================================================
	// ==== Trigger Cut Methods =========================================
	// ==================================================================
	
	/**
	 * Checks whether the cluster passes the threshold for minimum
	 * component hits.
	 * @param cluster - The cluster to check.
	 * @return Returns <code>true</code> if the cluster passes and <code>
	 * false</code> if it does not.
	 */
	private boolean clusterHitCountCut(HPSEcalCluster cluster) {
		return cluster.getCalorimeterHits().size() >= clusterHitCountThreshold;
	}
	
	/**
	 * Checks whether the cluster passes the threshold for minimum
	 * cluster seed energy.
	 * @param cluster - The cluster to check.
	 * @return Returns <code>true</code> if the cluster passes and <code>
	 * false</code> if it does not.
	 */
	private boolean clusterSeedEnergyCut(HPSEcalCluster cluster) {
		return cluster.getSeedHit().getCorrectedEnergy() >= clusterSeedEnergyThreshold;
	}
	
	/**
	 * Checks whether the cluster passes the threshold for minimum
	 * total cluster energy.
	 * @param cluster - The cluster to check.
	 * @return Returns <code>true</code> if the cluster passes and <code>
	 * false</code> if it does not.
	 */
	private boolean clusterTotalEnergyCut(HPSEcalCluster cluster) {
		return cluster.getEnergy() >= clusterTotalEnergyThreshold;
	}
	
	/**
	 * Calculates the value used in the pair energy sum cut from a pair
	 * of two clusters.
	 * @param clusterPair - The cluster pair from which to derive the
	 * cut value.
	 * @return Returns the cut value as a <code>double</code>.
	 */
	private double getEnergySumValue(HPSEcalCluster[] clusterPair) {
		return (clusterPair[0].getEnergy() + clusterPair[1].getEnergy());
	}
	
	/**
	 * Calculates the value used in the invariant mass cut from a pair
	 * of two clusters.
	 * @param clusterPair - The cluster pair from which to derive the
	 * cut value.
	 * @return Returns the cut value as a <code>double</code>.
	 */
	private double getInvariantMassValue(HPSEcalCluster[] clusterPair) {
		double[] e = { clusterPair[0].getEnergy(), clusterPair[1].getEnergy() };
		double[] x = { clusterPair[0].getSeedHit().getIdentifierFieldValue("ix"), clusterPair[1].getSeedHit().getIdentifierFieldValue("ix") };
		double[] y = { clusterPair[0].getSeedHit().getIdentifierFieldValue("iy"), clusterPair[1].getSeedHit().getIdentifierFieldValue("iy") };
		return (e[0] * e[1] * (Math.pow(x[0] - x[1], 2) + Math.pow(y[0] - y[1], 2)) / D2);
	}
	
	/**
	 * Checks whether the cluster pair passes the threshold for the
	 * minimum pair energy sum check.
	 * @param clusterPair - An array of size two containing the cluster
	 * pair to check.
	 * @return Returns <code>true</code> if the cluster passes and <code>
	 * false</code> if it does not.
	 */
	private boolean pairEnergySumCut(HPSEcalCluster[] clusterPair) {
		// The cut will fail if this is not a cluster pair.
		if(clusterPair.length != 2) { return false; }
		
		// Otherwise, get the energy sum and compare it to the threshold.
		return getEnergySumValue(clusterPair) >= pairEnergySumThreshold;
	}
	
	/**
	 * Checks whether the cluster pair passes the threshold for the
	 * invariant mass check.
	 * @param clusterPair - An array of size two containing the cluster
	 * pair to check.
	 * @return Returns <code>true</code> if the cluster passes and <code>
	 * false</code> if it does not.
	 */
	private boolean pairInvariantMassCut(HPSEcalCluster[] clusterPair) {
		// The cut will fail if this is not a cluster pair.
		if(clusterPair.length != 2) { return false; }
		
		// Calculate the invariant mass.
		double myy2 = getInvariantMassValue(clusterPair);
		
		// Perform the cut.
		return ( (myy2 >= invariantMassThresholdLow) && (myy2 <= invariantMassThresholdHigh));
	}
	
	// ==================================================================
	// ==== Variables Mutator Methods ===================================
	// ==================================================================
	
	/**
	 * Sets the LCIO collection name where <code>HPSEcalCluster</code>
	 * objects are stored for use in the trigger.
	 * @param clusterCollectionName - The name of the LCIO collection.
	 */
	public void setClusterCollectionName(String clusterCollectionName) {
		this.clusterCollectionName = clusterCollectionName;
	}
	
	/**
	 * Sets the threshold for the number of hits in individual
	 * clusters under which the cluster will be rejected and not used
	 * for triggering.
	 * @param clusterHitCountThreshold - The cluster hit count lower
	 * bound.
	 */
	public void setClusterHitCountThreshold(int clusterHitCountThreshold) {
		this.clusterHitCountThreshold = clusterHitCountThreshold;
	}
	
	/**
	 * Sets the threshold for the cluster seed energy of individual
	 * clusters under which the cluster will be rejected and not used
	 * for triggering.
	 * @param clusterSeedEnergyThreshold - The cluster seed energy
	 * lower bound.
	 */
	public void setClusterSeedEnergyThreshold(double clusterSeedEnergyThreshold) {
		this.clusterSeedEnergyThreshold = clusterSeedEnergyThreshold;
	}
	
	/**
	 * Sets the threshold for the total cluster energy of individual
	 * clusters under which the cluster will be rejected and not used
	 * for triggering.
	 * @param clusterTotalEnergyThreshold - The cluster total energy
	 * lower bound.
	 */
	public void setClusterTotalEnergyThreshold(double clusterTotalEnergyThreshold) {
		this.clusterTotalEnergyThreshold = clusterTotalEnergyThreshold;
	}
	
	/**
	 * Sets the number of events that clusters will be retained and
	 * employed for triggering before they are cleared.
	 * @param coincidenceWindow - The number of events that clusters
	 * should be retained.
	 */
	public void setCoincidenceWindow(int coincidenceWindow) {
		this.coincidenceWindow = coincidenceWindow;
	}
	
	/**
	 * Sets the threshold for the calculated invariant mass of the
	 * generating particle (assuming that the clusters are produced
	 * by a positron/electron pair) above which the cluster pair will
	 * be rejected and not produce a trigger.
	 * @param invariantMassThresholdHigh - The invariant mass upper
	 * bound.
	 */
	public void setInvariantMassThresholdHigh(double invariantMassThresholdHigh) {
		this.invariantMassThresholdHigh = invariantMassThresholdHigh;
	}
	
	/**
	 * Sets the threshold for the calculated invariant mass of the
	 * generating particle (assuming that the clusters are produced
	 * by a positron/electron pair) under which the cluster pair will
	 * be rejected and not produce a trigger.
	 * @param invariantMassThresholdLow - The invariant mass lower
	 * bound.
	 */
	public void setInvariantMassThresholdLow(double invariantMassThresholdLow) {
		this.invariantMassThresholdLow = invariantMassThresholdLow;
	}
	
	/**
	 * Sets the threshold for the sum of the energies of a cluster pair
	 * under which the pair will be rejected and not produce a trigger.
	 * @param pairEnergySumThreshold - The cluster pair energy sum
	 * lower bound.
	 */
	public void setPairEnergySumThreshold(double pairEnergySumThreshold) {
		this.pairEnergySumThreshold = pairEnergySumThreshold;
	}
	
	/**
	 * Toggles whether the driver will output its actions to the console
	 * during run time or not.
	 * @param verbose - <code>true</code> indicates that the console
	 * will write its actions and <code>false</code> that it will not.
	 */
	public void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}
	
	// ==================================================================
	// ==== AIDA Plots ==================================================
	// ==================================================================
	IHistogram2D aClusterDistribution;
	IHistogram1D aClusterSeedEnergy;
	IHistogram1D aClusterTotalEnergy;
	IHistogram2D clusterDistribution;
	IHistogram1D clusterSeedEnergy;
	IHistogram1D clusterTotalEnergy;
	IHistogram1D invariantMass;
	IHistogram1D pairEnergySum;
	IHistogram2D pClusterDistribution;
	IHistogram1D pClusterSeedEnergy;
	IHistogram1D pClusterTotalEnergy;
	IHistogram1D pPairEnergySum;
	IHistogram1D pInvariantMass;
	
	// ==================================================================
	// ==== Variables ===================================================
	// ==================================================================
	
	/**
	 * <b>aida</b><br/><br/>
	 * <code>private AIDA <b>aida</b></code><br/><br/>
	 * Factory for generating histograms.
	 */
    private AIDA aida = AIDA.defaultInstance();
	
	/**
	 * <b>clusterBuffer</b><br/><br/>
	 * <code>private LinkedList<List<HPSEcalCluster>> <b>clusterBuffer</b></code><br/><br/>
	 * Stores the list of clusters from each event for a finite-sized
	 * buffer. The size of the buffer is determined by the coincidence
	 * window.
	 */
	private LinkedList<List<HPSEcalCluster>> clusterBuffer;
	
	/**
	 * <b>clusterCollectionName</b><br/><br/>
	 * <code>private String <b>clusterCollectionName</b></code><br/><br/>
	 * The name of the LCIO collection containing <code>HPSEcalCluster
	 * </code> objects.
	 */
	private String clusterCollectionName = "EcalClusters";
	
	/**
	 * <b>clusterHitCountThreshold</b><br/><br/>
	 * <code>private int <b>clusterHitCountThreshold</b></code><br/><br/>
	 * The minimum number of events needed for a cluster to avoid being
	 * excluded from the trigger.
	 */
	private int clusterHitCountThreshold = 2;
	
	/**
	 * <b>clusterPair</b><br/><br/>
	 * <code>private HPSEcalCluster[] <b>clusterPair</b></code><br/><br/>
	 * Stores the two highest energy clusters located in the cluster
	 * buffer. These are sorted by energy, with the highest energy
	 * cluster first in the array.
	 */
	private HPSEcalCluster[] clusterPair = new HPSEcalCluster[2];
	
	/**
	 * <b>clusterSeedEnergyThreshold</b><br/><br/>
	 * <code>private double <b>clusterSeedEnergyThreshold</b></code><br/><br/>
	 * Defines the threshold for the cluster seed energy under which
	 * a cluster will be rejected.
	 */
	private double clusterSeedEnergyThreshold = 0.05 / 0.83;
	
	/**
	 * <b>clusterTotalEnergyThreshold</b><br/><br/>
	 * <code>private double <b>clusterTotalEnergyThreshold</b></code><br/><br/>
	 * Defines the threshold for the total cluster energy under which
	 * a cluster will be rejected.
	 */
	private double clusterTotalEnergyThreshold = 0.15 / 0.83;
    
	/**
	 * <b>coincidenceWindow</b><br/><br/>
	 * <code>private int <b>coincidenceWindow</b></code><br/><br/>
	 * The number of events for which clusters will be retained and
	 * used in the trigger before they are removed.
	 */
    private int coincidenceWindow = 6;
    
	/**
	 * <b>D2</b><br/><br/>
	 * <code>private static final double <b>D2</b></code><br/><br/>
	 * The squared distance of the calorimeter from the target.
	 */
    private static final double D2 = 1.414 * 1.414; // (1414^2 mm^2)
	
	/**
	 * <b>invariantMassThresholdHigh</b><br/><br/>
	 * <code>private double <b>invariantMassThresholdHigh</b></code><br/><br/>
	 * Defines the threshold for the invariant mass of the generating
	 * particle above which the cluster pair will be rejected.
	 */
	private double invariantMassThresholdHigh = 0.00228 / 0.83 / 0.83;
	
	/**
	 * <b>invariantMassThresholdLow</b><br/><br/>
	 * <code>private double <b>invariantMassThresholdLow</b></code><br/><br/>
	 * Defines the threshold for the invariant mass of the generating
	 * particle below which the cluster pair will be rejected.
	 */
	private double invariantMassThresholdLow = 0.00137 / 0.83 / 0.83;
	
	/**
	 * <b>pairEnergySumThreshold</b><br/><br/>
	 * <code>private double <b>pairEnergySumThreshold</b></code><br/><br/>
	 * Defines the threshold for the sum of the energies of a cluster
	 * pair below which the pair will be rejected.
	 */
	private double pairEnergySumThreshold = 0.8 / 0.83;
	
	/**
	 * <b>verbose</b><br/><br/>
	 * <code>private boolean <b>verbose</b></code><br/><br/>
	 * Sets whether the driver outputs its clustering decisions to the
	 * console or not.
	 */
	private boolean verbose = true;
}

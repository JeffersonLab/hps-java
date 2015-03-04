package org.hps.users.kmccarty;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.readout.ecal.triggerbank.SSPCluster;
import org.lcsim.event.Cluster;

public class ClusterMatchStatus {
	// Track cluster statistics.
	private int sspClusters   = 0;
	private int reconClusters = 0;
	private int matches       = 0;
	private int failEnergy    = 0;
	private int failPosition  = 0;
	private int failHitCount  = 0;
	
	// Plot binning values.
	private static final int TIME_BIN = 4;
	private static final double ENERGY_BIN = 0.01;
	private static final int TIME_BIN_HALF = TIME_BIN / 2;
	private static final double ENERGY_BIN_HALF = ENERGY_BIN / 2;
	
	// Track plotting values for reconstructed and SSP clusters.
	private Map<Integer, Integer> sspHitCountBins = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> reconHitCountBins = new HashMap<Integer, Integer>();
	private Map<Point, Integer> sspPositionBins = new HashMap<Point, Integer>();
	private Map<Point, Integer> reconPositionBins = new HashMap<Point, Integer>();
	private Map<Integer, Integer> sspEnergyBins = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> reconEnergyBins = new HashMap<Integer, Integer>();
	
	// Track plotting values for cluster matching results.
	private Map<Point, Integer> failPositionBins = new HashMap<Point, Integer>();
	private Map<Point, Integer> allHenergyBins = new HashMap<Point, Integer>();
	private Map<Point, Integer> failHenergyBins = new HashMap<Point, Integer>();
	private Map<Integer, Integer> allTimeBins = new HashMap<Integer, Integer>();
	private Map<Integer, Integer> failTimeBins = new HashMap<Integer, Integer>();
	
    public void addEvent(ClusterMatchEvent event, List<Cluster> reconClusters, List<SSPCluster> sspClusters) {
    	// Update the number of reconstructed and SSP clusters
		// that have been seen so far.
		int sspCount = sspClusters == null ? 0 : sspClusters.size();
		int reconCount = reconClusters == null ? 0 : reconClusters.size();
		this.sspClusters   += sspCount;
		this.reconClusters += reconCount;
		
		// Update the pair state information.
		matches      += event.getMatches();
		failEnergy   += event.getEnergyFailures();
		failHitCount += event.getHitCountFailures();
		failPosition += event.getPositionFailures();
		
		// In the special case that there are no SSP clusters, no pairs
		// will be listed. All possible fails are known to have failed
		// due to position.
		if(sspClusters == null || sspClusters.isEmpty()) {
			failPosition += (reconClusters == null ? 0 : reconClusters.size());
		}
    	
    	// Update the plotting information for reconstructed clusters.
		for(Cluster cluster : reconClusters) {
			// Update the hit count bin data.
			Integer hitCountCount = reconHitCountBins.get(cluster.getCalorimeterHits().size());
			if(hitCountCount == null) { reconHitCountBins.put(cluster.getCalorimeterHits().size(), 1); }
			else { reconHitCountBins.put(cluster.getCalorimeterHits().size(), hitCountCount + 1); }
			
			// Update the position bin data.
			Point clusterPosition = TriggerDiagnosticUtil.getClusterPosition(cluster);
			Integer positionCount = reconPositionBins.get(clusterPosition);
			if(positionCount == null) { reconPositionBins.put(clusterPosition, 1); }
			else { reconPositionBins.put(clusterPosition, positionCount + 1); }
			
			// Update the energy bin data.
			int energyBin = (int) Math.floor(cluster.getEnergy() / ENERGY_BIN);
			Integer energyCount = reconEnergyBins.get(energyBin);
			if(energyCount == null) { reconEnergyBins.put(energyBin, 1); }
			else { reconEnergyBins.put(energyBin, energyCount + 1); }
		}
		
    	// Update the plotting information for SSP clusters.
		for(SSPCluster cluster : sspClusters) {
			// Update the hit count bin data.
			Integer hitCountCount = sspHitCountBins.get(cluster.getHitCount());
			if(hitCountCount == null) { sspHitCountBins.put(cluster.getHitCount(), 1); }
			else { sspHitCountBins.put(cluster.getHitCount(), hitCountCount + 1); }
			
			// Update the position bin data.
			Point clusterPosition = TriggerDiagnosticUtil.getClusterPosition(cluster);
			Integer positionCount = sspPositionBins.get(clusterPosition);
			if(positionCount == null) { sspPositionBins.put(clusterPosition, 1); }
			else { sspPositionBins.put(clusterPosition, positionCount + 1); }
			
			// Update the energy bin data.
			int energyBin = (int) Math.floor(cluster.getEnergy() / ENERGY_BIN);
			Integer energyCount = sspEnergyBins.get(energyBin);
			if(energyCount == null) { sspEnergyBins.put(energyBin, 1); }
			else { sspEnergyBins.put(energyBin, energyCount + 1); }
		}
		
		// Update the plotting information for SSP/reconstructed cluster
		// pairs.
		for(ClusterMatchedPair pair : event.getMatchedPairs()) {
			// If one of the pairs is null, then it is unmatched cluster
			// and may be skipped.
			if(pair.getReconstructedCluster() == null || pair.getSSPCluster() == null) {
				continue;
			}
			
			// Populate the bins for the "all" plots.
			// Update the match time plots.
			int timeBin = (int) Math.floor(TriggerDiagnosticUtil.getClusterTime(pair.getReconstructedCluster()) / TIME_BIN);
			Integer timeCount = allTimeBins.get(timeBin);
			if(timeCount == null) { allTimeBins.put(timeBin, 1); }
			else { allTimeBins.put(timeBin, timeCount + 1); }
			
			// Update the energy/hit difference plots.
			int hitBin = getHitCountDifference(pair.getSSPCluster(), pair.getReconstructedCluster());
			int energyBin = (int) Math.floor(getEnergyPercentDifference(pair.getSSPCluster(), pair.getReconstructedCluster()) / ENERGY_BIN);
			Point henergyBin = new Point(hitBin, energyBin);
			Integer henergyCount = allHenergyBins.get(henergyBin);
			if(henergyCount == null) { allHenergyBins.put(henergyBin, 1); }
			else { allHenergyBins.put(henergyBin, henergyCount + 1); }
			
			// Populate the bins for the "fail" plots.
			if(!pair.isMatch()) {
				// Update the failed cluster position bins.
				Point clusterPosition = TriggerDiagnosticUtil.getClusterPosition(pair.getReconstructedCluster());
				Integer positionCount = failPositionBins.get(clusterPosition);
				if(positionCount == null) { failPositionBins.put(clusterPosition, 1); }
				else { failPositionBins.put(clusterPosition, positionCount + 1); }
				
				// Update the failed match time plots.
				timeCount = failTimeBins.get(timeBin);
				if(timeCount == null) { failTimeBins.put(timeBin, 1); }
				else { failTimeBins.put(timeBin, timeCount + 1); }
				
				// Update the failed energy/hit difference plots.
				henergyCount = failHenergyBins.get(henergyBin);
				if(henergyCount == null) { failHenergyBins.put(henergyBin, 1); }
				else { failHenergyBins.put(henergyBin, henergyCount + 1); }
			}
		}
    }
    
	/**
	 * Clears all statistical information and resets the object ot its
	 * default, empty state.
	 */
	public void clear() {
		// Clear statistical data.
		sspClusters   = 0;
		reconClusters = 0;
		matches       = 0;
		failEnergy    = 0;
		failPosition  = 0;
		failHitCount  = 0;
		
		// Clear plot collections.
		sspHitCountBins.clear();
		reconHitCountBins.clear();
		sspPositionBins.clear();
		reconPositionBins.clear();
		sspEnergyBins.clear();
		reconEnergyBins.clear();
		failPositionBins.clear();
		allHenergyBins.clear();
		failHenergyBins.clear();
		allTimeBins.clear();
		failTimeBins.clear();
	}
	
	/**
	 * Gets the number of cluster pairs stored in this event that are
	 * marked with energy fail states.
	 * @return Returns the number of instances of this state as an
	 * <code>int</code> primitive.
	 */
	public int getEnergyFailures() {
		return failEnergy;
	}
	
	/**
	 * Gets the number of cluster pairs stored in this event that are
	 * marked with hit count fail states.
	 * @return Returns the number of instances of this state as an
	 * <code>int</code> primitive.
	 */
	public int getHitCountFailures() {
		return failHitCount;
	}
	
	/**
	 * Gets the number of cluster pairs stored in this event that are
	 * marked with position fail states.
	 * @return Returns the number of instances of this state as an
	 * <code>int</code> primitive.
	 */
	public int getMatches() {
		return matches;
	}
	
	/**
	 * Gets the number of cluster pairs stored in this event that are
	 * marked with position fail states.
	 * @return Returns the number of instances of this state as an
	 * <code>int</code> primitive.
	 */
	public int getPositionFailures() {
		return failPosition;
	}
	
	/**
	 * Gets the total number of verifiable reconstructed clusters seen.
     * @return Returns the cluster count as an <code>int</code>
     * primitive.
	 */
    public int getReconClusterCount() {
    	return reconClusters;
    }
    
    /**
     * Gets the total number of SSP bank clusters seen.
     * @return Returns the cluster count as an <code>int</code>
     * primitive.
     */
    public int getSSPClusterCount() {
    	return sspClusters;
    }
    
	/**
	 * Gets the difference in hit count between an SSP cluster and a
	 * reconstructed cluster.
	 * @param sspCluster - The SSP cluster.
	 * @param reconCluster - The reconstructed cluster.
	 * @return Returns the difference as an <code>int</code> primitive.
	 */
	private static final int getHitCountDifference(SSPCluster sspCluster, Cluster reconCluster) {
		return sspCluster.getHitCount() - TriggerDiagnosticUtil.getHitCount(reconCluster);
	}
	
	/**
	 * Solves the equation <code>|E_ssp / E_recon|</code>.
	 * @param sspCluster - The SSP cluster.
	 * @param reconCluster - The reconstructed cluster.
	 * @return Returns the solution to the equation as a <code>double
	 * </code> primitive.
	 */
	private static final double getEnergyPercentDifference(SSPCluster sspCluster, Cluster reconCluster) {
		return Math.abs((sspCluster.getEnergy() / reconCluster.getEnergy()));
	}
}
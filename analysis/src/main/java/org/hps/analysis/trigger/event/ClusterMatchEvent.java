package org.hps.analysis.trigger.event;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.ecal.triggerbank.SSPCluster;
import org.hps.analysis.trigger.util.TriggerDiagnosticUtil;
import org.lcsim.event.Cluster;

/**
 * Class <code>ClusterMatchEvent</code> tracks reconstructed/SSP cluster
 * pairs for the purpose of cluster matching. It maintains a list of
 * all pairs that have been seen as well as their match states. It can
 * additionally provide the total number of each type of match state.
 * 
 * @author Kyle McCarty
 */
public class ClusterMatchEvent {
	// Track the number of state instances.
	private int matched = 0;
	private int failPosition = 0;
	private int failEnergy = 0;
	private int failHitCount = 0;
	private int failTime = 0;
	
	// Store all of the pairs.
	private List<ClusterMatchedPair> pairList = new ArrayList<ClusterMatchedPair>();
	
	/**
	 * Fuses another <code>ClusterMatchEvent</code> with this object.
	 * The other event's cluster pairs and states will be added to those
	 * already in this event.
	 * @param event - The event to fuse.
	 */
	public void addEvent(ClusterMatchEvent event) {
		// If the event is null, do nothing.
		if(event == null) { return; }
		
		// Iterate over the new event's matched pairs and add them into
		// this event's statistics.
		for(ClusterMatchedPair cmp : event.pairList) {
			// Add the current pair to this pair list.
			pairList.add(cmp);
			
			// Increment the statistics counters based on the pair state.
			if(cmp.isMatch()) { matched++; }
			if(cmp.isPositionFailState()) { failPosition++; }
			if(cmp.isEnergyFailState()) { failEnergy++; }
			if(cmp.isHitCountFailState()) { failHitCount++; }
			if(cmp.isTimeFailState()) { failTime++; }
		}
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
	 * marked with match states.
	 * @return Returns the number of instances of this state as an
	 * <code>int</code> primitive.
	 */
	public List<ClusterMatchedPair> getMatchedPairs() {
		return pairList;
	}
	
	/**
	 * Gets the number of cluster pairs stored in this event that are
	 * marked with position fail states.
	 * @return Returns the number of instances of this state as an
	 * <code>int</code> primitive.
	 */
	public int getMatches() {
		return matched;
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
	 * Gets the number of cluster pairs stored in this event that are
	 * marked with time fail states.
	 * @return Returns the number of instances of this state as an
	 * <code>int</code> primitive.
	 */
	public int getTimeFailures() {
		return failTime;
	}
	
	/**
	 * Indicates whether at least one cluster pair in the event created
	 * a fail state.
	 * @return Returns <code>true</code> if not all clusters matched and
	 * <code>false</code> otherwise.
	 */
	public boolean isFailState() {
		return (failEnergy > 0) || (failHitCount > 0) || (failTime > 0) || (failPosition > 0);
	}
	
	/**
	 * Adds a reconstructed/SSP cluster pair and marks it as having an
	 * energy fail state.
	 * @param reconCluster - The reconstructed cluster.
	 * @param sspCluster - The SSP cluster.
	 */
	public void pairFailEnergy(Cluster reconCluster, SSPCluster sspCluster) {
		failEnergy++;
		pairList.add(new ClusterMatchedPair(reconCluster, sspCluster, TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_ENERGY));
	}
	
	/**
	 * Adds a reconstructed/SSP cluster pair and marks it as having a
	 * hit count fail state.
	 * @param reconCluster - The reconstructed cluster.
	 * @param sspCluster - The SSP cluster.
	 */
	public void pairFailHitCount(Cluster reconCluster, SSPCluster sspCluster) {
		failHitCount++;
		pairList.add(new ClusterMatchedPair(reconCluster, sspCluster, TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_HIT_COUNT));
	}
	
	/**
	 * Adds a reconstructed/SSP cluster pair and marks it as having a
	 * position fail state.
	 * @param reconCluster - The reconstructed cluster.
	 * @param sspCluster - The SSP cluster.
	 */
	public void pairFailPosition(Cluster reconCluster, SSPCluster sspCluster) {
		failPosition++;
		pairList.add(new ClusterMatchedPair(reconCluster, sspCluster, TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_POSITION));
	}
	
	/**
	 * Adds a reconstructed/SSP cluster pair and marks it as having a
	 * time fail state.
	 * @param reconCluster - The reconstructed cluster.
	 * @param sspCluster - The SSP cluster.
	 */
	public void pairFailTime(Cluster reconCluster, SSPCluster sspCluster) {
		failTime++;
		pairList.add(new ClusterMatchedPair(reconCluster, sspCluster, TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_TIME));
	}
	
	/**
	 * Adds a reconstructed/SSP cluster pair and marks it as having a
	 * match state.
	 * @param reconCluster - The reconstructed cluster.
	 * @param sspCluster - The SSP cluster.
	 */
	public void pairMatch(Cluster reconCluster, SSPCluster sspCluster) {
		matched++;
		pairList.add(new ClusterMatchedPair(reconCluster, sspCluster, TriggerDiagnosticUtil.CLUSTER_STATE_MATCHED));
	}
}

package org.hps.analysis.trigger.event;

import org.hps.analysis.trigger.util.Pair;
import org.hps.analysis.trigger.util.TriggerDiagnosticUtil;
import org.hps.readout.ecal.triggerbank.SSPCluster;
import org.lcsim.event.Cluster;

/**
 * Class <code>ClusterMatchedPair</code> stores a reconstructed cluster
 * and an SSP bank reported cluster which have been compared for the
 * purpose of cluster matching. It also tracks what the match state of
 * the two clusters is.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class ClusterMatchedPair extends Pair<Cluster, SSPCluster> {
	// CLass variables.
	private final byte state;
	
	/**
	 * Instantiates a new <code>ClusterMatchedPair</code> object from
	 * the two indicated clusters and marks their match state.
	 * @param reconCluster - The reconstructed cluster.
	 * @param sspCluster - The SSP cluster.
	 * @param state - The pair match state.
	 */
	public ClusterMatchedPair(Cluster reconCluster, SSPCluster sspCluster, byte state) {
		// Set the cluster pairs.
		super(reconCluster, sspCluster);
		
		// If the state is defined, set it. Otherwise, it is unknown.
		if(state == TriggerDiagnosticUtil.CLUSTER_STATE_MATCHED
				|| state == TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_POSITION
				|| state == TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_ENERGY
				|| state == TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_HIT_COUNT) {
			this.state = state;
		} else {
			this.state = TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_UNKNOWN;
		}
	}
	
	/**
	 * Gets the reconstructed cluster of the pair.
	 * @return Returns the reconstructed cluster a <code>Cluster</cod>
	 * object.
	 */
	public Cluster getReconstructedCluster() {
		return getFirstElement();
	}
	
	/**
	 * Gets the SSP cluster of the pair.
	 * @return Returns the SSP cluster as an <code>SSPCluster</code>
	 * object.
	 */
	public SSPCluster getSSPCluster() {
		return getSecondElement();
	}
	
	/**
	 * Gets the raw state identifier.
	 * @return Returns the state identifier as a <code>byte</code>
	 * primitive. Valid identifiers are defined in the class
	 * <code>TriggerDiagnosticUtil</code>.
	 */
	public byte getState() {
		return state;
	}
	
	/**
	 * Indicates whether the recon/SSP pair failed to not being close
	 * enough in energy.
	 * @return Returns <code>true</code> if the pair match state is an
	 * energy fail state and <code>false</code> otherwise.
	 */
	public boolean isEnergyFailState() {
		return (state == TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_ENERGY);
	}
	
	/**
	 * Indicates whether the recon/SSP pair failed to match due to not
	 * being close enough in hit count.
	 * @return Returns <code>true</code> if the pair match state is a
	 * hit count fail state and <code>false</code> otherwise.
	 */
	public boolean isHitCountFailState() {
		return (state == TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_HIT_COUNT);
	}
	
	/**
	 * Indicates whether the recon/SSP pair matched.
	 * @return Returns <code>true</code> if the pair match state is a
	 * match state and <code>false</code> otherwise.
	 */
	public boolean isMatch() {
		return (state == TriggerDiagnosticUtil.CLUSTER_STATE_MATCHED);
	}
	
	/**
	 * Indicates whether the recon/SSP pair failed to match due to the
	 * cluster positions not aligning.
	 * @return Returns <code>true</code> if the pair match state is a
	 * position fail state and <code>false</code> otherwise.
	 */
	public boolean isPositionFailState() {
		return (state == TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_POSITION);
	}
	
	/**
	 * Indicates whether the recon/SSP pair failed to match due to the
	 * cluster time-stamps not aligning.
	 * @return Returns <code>true</code> if the pair match state is a
	 * time fail state and <code>false</code> otherwise.
	 */
	public boolean isTimeFailState() {
		return (state == TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_TIME);
	}
	
	/**
	 * Indicates whether the recon/SSP pair has no known match state.
	 * @return Returns <code>true</code> if the pair match state is
	 * unknown and <code>false</code> otherwise.
	 */
	public boolean isUnknownState() {
		return (state == TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_UNKNOWN);
	}
}

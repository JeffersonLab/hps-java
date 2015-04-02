package org.hps.analysis.trigger.util;

import java.awt.Point;

import org.hps.analysis.trigger.data.TriggerStatModule;
import org.hps.recon.ecal.triggerbank.SSPCluster;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;

/**
 * Class <code>TriggerDiagnosticUtil</code> contains a series of
 * utility methods that are used at various points throughout the
 * trigger diagnostic package.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class TriggerDiagnosticUtil {
	// Cluster match state variables.
	public static final byte CLUSTER_STATE_MATCHED        = 0;
	public static final byte CLUSTER_STATE_FAIL_POSITION  = 1;
	public static final byte CLUSTER_STATE_FAIL_ENERGY    = 2;
	public static final byte CLUSTER_STATE_FAIL_HIT_COUNT = 3;
	public static final byte CLUSTER_STATE_FAIL_TIME      = 4;
	public static final byte CLUSTER_STATE_FAIL_UNKNOWN   = 5;
	
	// Trigger match cut IDs.
	public static final int SINGLES_ENERGY_MIN = 0;
	public static final int SINGLES_ENERGY_MAX = 1;
	public static final int SINGLES_HIT_COUNT = 2;
	public static final int PAIR_ENERGY_SUM = 0;
	public static final int PAIR_ENERGY_DIFF = 1;
	public static final int PAIR_ENERGY_SLOPE = 2;
	public static final int PAIR_COPLANARITY = 3;
	
	// Trigger type variables.
	public static final int TRIGGER_PULSER    = TriggerStatModule.PULSER;
	public static final int TRIGGER_COSMIC    = TriggerStatModule.COSMIC;
	public static final int TRIGGER_SINGLES_0 = TriggerStatModule.SINGLES_0;
	public static final int TRIGGER_SINGLES_1 = TriggerStatModule.SINGLES_1;
	public static final int TRIGGER_PAIR_0    = TriggerStatModule.PAIR_0;
	public static final int TRIGGER_PAIR_1    = TriggerStatModule.PAIR_1;
	public static final String[] TRIGGER_NAME = { "Singles 0", "Singles 1", "Pair 0", "Pair 1", "Pulser", "Cosmic" };
	
	/**
	 * Convenience method that writes the position of a cluster in the
	 * form (ix, iy).
	 * @param cluster - The cluster.
	 * @return Returns the cluster position as a <code>String</code>.
	 */
	public static final String clusterPositionString(Cluster cluster) {
		return String.format("(%3d, %3d)",
				cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix"),
				cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy"));
	}
	
	/**
	 * Convenience method that writes the position of a cluster in the
	 * form (ix, iy).
	 * @param cluster - The cluster.
	 * @return Returns the cluster position as a <code>String</code>.
	 */
	public static final String clusterPositionString(SSPCluster cluster) {
		return String.format("(%3d, %3d)", cluster.getXIndex(), cluster.getYIndex());
	}
	
	/**
	 * Convenience method that writes the information in a cluster to
	 * a <code>String</code>.
	 * @param cluster - The cluster.
	 * @return Returns the cluster information as a <code>String</code>.
	 */
	public static final String clusterToString(Cluster cluster) {
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
	public static final String clusterToString(SSPCluster cluster) {
		return String.format("Cluster at (%3d, %3d) with %.3f GeV and %d hits at %4d ns.",
				cluster.getXIndex(), cluster.getYIndex(), cluster.getEnergy(),
				cluster.getHitCount(), cluster.getTime());
	}
	
	/**
	 * Gets the x/y-indices of the cluster.
	 * @param cluster -  The cluster of which to obtain the indices.
	 * @return Returns the indices as a <code>Point</code> object.
	 */
	public static final Point getClusterPosition(Cluster cluster) {
		return new Point(getXIndex(cluster), getYIndex(cluster));
	}
	
	/**
	 * Gets the x/y-indices of the cluster.
	 * @param cluster -  The cluster of which to obtain the indices.
	 * @return Returns the indices as a <code>Point</code> object.
	 */
	public static final Point getClusterPosition(SSPCluster cluster) {
		return new Point(cluster.getXIndex(), cluster.getYIndex());
	}
	
	/**
	 * Gets the time stamp of the cluster in nanoseconds.
	 * @param cluster - The cluster.
	 * @return Returns the time-stamp.
	 */
	public static final double getClusterTime(Cluster cluster) {
		return cluster.getCalorimeterHits().get(0).getTime();
	}
	
	/**
	 * Gets the time stamp of the cluster in nanoseconds.
	 * @param cluster - The cluster.
	 * @return Returns the time-stamp.
	 */
	public static final int getClusterTime(SSPCluster cluster) {
		return cluster.getTime();
	}
	
	/**
	 * Gets the number of digits in the base-10 String representation
	 * of an integer primitive. Negative signs are not included in the
	 * digit count.
	 * @param value - The value of which to obtain the length.
	 * @return Returns the number of digits in the String representation
	 * of the argument value.
	 */
	public static final int getDigits(int value) {
		if(value < 0) { return Integer.toString(value).length() - 1; }
		else { return Integer.toString(value).length(); }
	}
	
	/**
	 * Gets the number of hits in a cluster.
	 * @param cluster - The cluster.
	 * @return Returns the number of hits in the cluster.
	 */
	public static final int getHitCount(Cluster cluster) {
		return cluster.getCalorimeterHits().size();
	}
	
	/**
	 * Gets the number of hits in a cluster.
	 * @param cluster - The cluster.
	 * @return Returns the number of hits in the cluster.
	 */
	public static final int getHitCount(SSPCluster cluster) {
		return cluster.getHitCount();
	}
	
	/**
	 * Gets the x-index of the cluster's seed hit.
	 * @param cluster - The cluster.
	 * @return Returns the x-index.
	 */
	public static final int getXIndex(Cluster cluster) {
		return cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
	}
	
	/**
	 * Gets the x-index of the cluster's seed hit.
	 * @param cluster - The cluster.
	 * @return Returns the x-index.
	 */
	public static final int getXIndex(SSPCluster cluster) {
		return cluster.getXIndex();
	}
	
	/**
	 * Gets the y-index of the cluster's seed hit.
	 * @param cluster - The cluster.
	 * @return Returns the y-index.
	 */
	public static final int getYIndex(Cluster cluster) {
		return cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
	}
	
	/**
	 * Gets the y-index of the cluster's seed hit.
	 * @param cluster - The cluster.
	 * @return Returns the y-index.
	 */
	public static final int getYIndex(SSPCluster cluster) {
		return cluster.getYIndex();
	}
	
	/**
	 * Checks whether all of the hits in a cluster are within the safe
	 * region of the FADC output window.
	 * @param reconCluster - The cluster to check.
	 * @return Returns <code>true</code> if the cluster is safe and
	 * returns <code>false</code> otherwise.
	 */
	public static final boolean isVerifiable(Cluster reconCluster, int nsa, int nsb, int windowWidth) {
		// Iterate over the hits in the cluster.
		for(CalorimeterHit hit : reconCluster.getCalorimeterHits()) {
			// Check that none of the hits are within the disallowed
			// region of the FADC readout window.
			if(hit.getTime() <= nsb || hit.getTime() >= (windowWidth - nsa)) {
				return false;
			}
			
			// Also check to make sure that the cluster does not have
			// any negative energy hits. These are, obviously, wrong.
			if(hit.getCorrectedEnergy() < 0.0) {
				return false;
			}
		}
		
		// If all of the cluster hits pass the time cut, the cluster
		// is valid.
		return true;
	}
}

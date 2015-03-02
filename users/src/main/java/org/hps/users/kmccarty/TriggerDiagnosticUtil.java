package org.hps.users.kmccarty;

import java.awt.Point;

import org.hps.readout.ecal.triggerbank.SSPCluster;
import org.lcsim.event.Cluster;

public class TriggerDiagnosticUtil {
	public static final byte CLUSTER_STATE_MATCHED        = 0;
	public static final byte CLUSTER_STATE_FAIL_POSITION  = 1;
	public static final byte CLUSTER_STATE_FAIL_ENERGY    = 2;
	public static final byte CLUSTER_STATE_FAIL_HIT_COUNT = 3;
	public static final byte CLUSTER_STATE_FAIL_UNKNOWN   = 4;
	
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
}

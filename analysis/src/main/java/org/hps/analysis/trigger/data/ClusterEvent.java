package org.hps.analysis.trigger.data;

/**
 * Class <code>ClusterEvent</code> tracks reconstructed/SSP cluster
 * pairs for the purpose of cluster matching. It maintains statistical
 * information related to how many of each type of cluster was found
 * as well as how many matched and failed with a given fail state.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class ClusterEvent extends ClusterStatModule {
	/**
	 * Fuses another <code>ClusterEvent</code> with this object. The
	 * other event's cluster pairs and states will be added to those
	 * already in this event.
	 * @param event - The event to fuse.
	 */
	public void addEvent(ClusterEvent event) {
		// If the event is null, do nothing.
		if(event == null) { return; }
		
		// Add the values stored in the argument event to the counters
		// in this event.
		sspClusters   += event.sspClusters;
		reconClusters += event.reconClusters;
		matches       += event.matches;
		failEnergy    += event.failEnergy;
		failPosition  += event.failPosition;
		failHitCount  += event.failHitCount;
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
	 * Notes that a reconstructed cluster and SSP cluster pair failed
	 * due to energy.
	 */
	public void pairFailEnergy() {
		failEnergy++;
	}
	
	/**
	 * Notes that a reconstructed cluster and SSP cluster pair failed
	 * due to hit count.
	 */
	public void pairFailHitCount() {
		failHitCount++;
	}
	
	/**
	 * Notes that a reconstructed cluster and SSP cluster pair failed
	 * due to position.
	 */
	public void pairFailPosition() {
		failPosition++;
	}
	
	/**
	 * Notes that one or more reconstructed cluster and SSP cluster pair
	 * failed due to position.
	 * @param count - The number of events that failed in this manner.
	 */
	public void pairFailPosition(int count) {
		// negative values are non-physical.
		if(count < 0) {
			throw new IllegalArgumentException("Cluster failure counts must be non-negative.");
		}
		
		// Increment the count.
		failPosition += count;
	}
	
	/**
	 * Notes that a reconstructed cluster and SSP cluster pair failed
	 * due to time.
	 */
	public void pairFailTime() {
		failTime++;
	}
	
	/**
	 * Notes that a reconstructed cluster and SSP cluster pair was
	 * successfully matched.
	 */
	public void pairMatch() {
		matches++;
	}
	
	/**
	 * Increments the number of reconstructed FADC clusters seen.
	 * @param count - The number of clusters seen.
	 */
	public void sawReconClusters(int count) {
		reconClusters += count;
	}
	
	/**
	 * Increments the number of SSP bank clusters seen.
	 * @param count - The number of clusters seen.
	 */
	public void sawSSPClusters(int count) {
		sspClusters += count;
	}
}

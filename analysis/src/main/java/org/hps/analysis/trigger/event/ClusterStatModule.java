package org.hps.analysis.trigger.event;

/**
 * Class <code>ClusterStatModule</code> stores the statistical data
 * for trigger diagnostic cluster matching.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class ClusterStatModule {
	// Track cluster statistics.
	protected int sspClusters   = 0;
	protected int reconClusters = 0;
	protected int matches       = 0;
	protected int failEnergy    = 0;
	protected int failPosition  = 0;
	protected int failHitCount  = 0;
	
	/**
	 * Instantiates a <code>ClusterStatModule</code> with no statistics
	 * stored.
	 */
	ClusterStatModule() {  }
	
	/**
	 * Instantiates a <code>ClusterStatModule</code> with no statistics
	 * cloned from the base object.
	 * @param base - The source for the statistical data.
	 */
	ClusterStatModule(ClusterStatModule base) {
		// Copy the statistical data into it.
		sspClusters = base.sspClusters;
		reconClusters = base.reconClusters;
		matches = base.matches;
		failEnergy = base.failEnergy;
		failPosition = base.failPosition;
		failHitCount = base.failHitCount;
	}
	
	/**
	 * Clears all statistical information and resets the object of its
	 * default, empty state.
	 */
	void clear() {
		sspClusters   = 0;
		reconClusters = 0;
		matches       = 0;
		failEnergy    = 0;
		failPosition  = 0;
		failHitCount  = 0;
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
}

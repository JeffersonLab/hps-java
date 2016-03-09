package org.hps.analysis.trigger.data;

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
    protected int failTime      = 0;
    
    /**
     * Instantiates a <code>ClusterStatModule</code> with no statistics
     * stored.
     */
    ClusterStatModule() {  }
    
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
        failTime      = 0;
    }
    
    @Override
    public ClusterStatModule clone() {
        // Create a clone.
        ClusterStatModule clone = new ClusterStatModule();
        
        // Copy the statistical values to the clone.
        clone.sspClusters   = sspClusters;
        clone.reconClusters = reconClusters;
        clone.matches       = matches;
        clone.failEnergy    = failEnergy;
        clone.failPosition  = failPosition;
        clone.failHitCount  = failHitCount;
        clone.failTime      = failTime;
        
        // Return the clone.
        return clone;
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
     * Gets the number of cluster pairs stored in this event that are
     * marked with time fail states.
     * @return Returns the number of instances of this state as an
     * <code>int</code> primitive.
     */
    public int getTimeFailures() {
        return failTime;
    }
}

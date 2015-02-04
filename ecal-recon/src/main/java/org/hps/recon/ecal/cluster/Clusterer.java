package org.hps.recon.ecal.cluster;

import java.util.List;

import org.lcsim.conditions.ConditionsListener;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCluster;

/**
 * This is an interface for creating clusters and providing cut values
 * to the clustering algorithms in a generic fashion.
 * 
 * @see org.lcsim.event.Cluster
 * @see org.lcsim.event.CalorimeterHit
 * @see org.lcsim.event.EventHeader
 * @see NumericalCuts
 */
public interface Clusterer extends ConditionsListener {

    /**
     * Create a list of output clusters from input hits.
     * The clustering algorithm may modify the list of hits as it is copied
     * from the master list in the event.
     * @param event The current LCSim event.
     * @param hits The list of hits.
     * @return The output clusters.
     */
    List<Cluster> createClusters(EventHeader event, List<CalorimeterHit> hits);
    
    /**
     * <p>
     * Perform start of job initialization on this object.
     * <p>
     * This method would typically be used to cache cluster cut values
     * from the {@link NumericalCuts} into instance variables for
     * convenience and runtime performance purposes.  If the cuts 
     * have certain constraints on their reasonable values for the
     * algorithm, then this method should throw an <code>IllegalArgumentException</code> 
     * if the parameter value is invalid.
     * <p>
     * The Detector object from LCSim is not available yet when this
     * method is typically called, so the conditions system should not
     * be used.  Instead, the inherited callback method 
     * {@link #conditionsChanged(org.lcsim.conditions.ConditionsEvent)}
     * can be used to configure the class depending on the available
     * conditions when they are available.
     */
    void initialize();
    
    /**
     * Get numerical cut settings.
     * @return The numerical cut settings.
     */
    NumericalCuts getCuts();
    
    /**
     * Get the type of <code>Cluster</code> created by this algorithm.
     * @return The type of the Cluster.
     */
    ClusterType getClusterType();
    
    /**
     * Get the integer encoding of the <code>Cluster</code> type.
     * This is a convenience method, and it is declared as final in the abstract implementation.
     * @return The integer encoding of the Cluster type.
     */
    int getClusterTypeEncoding();
    
    /**
     * Create a basic <code>Cluster</code> with the correct type for this algorithm. 
     * @return The basic Cluster.
     */
    BaseCluster createBasicCluster();
}
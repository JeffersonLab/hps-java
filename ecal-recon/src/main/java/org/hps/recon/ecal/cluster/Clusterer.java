package org.hps.recon.ecal.cluster;

import java.util.List;

import org.lcsim.conditions.ConditionsListener;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;

/**
 * This is an interface for creating clusters and providing cut values
 * to the clustering algorithms in a generic fashion.
 */
public interface Clusterer extends ConditionsListener {

    /**
     * Create a list of output clusters from input hits.
     * @param event The current LCSim event.
     * @param hits The list of hits.
     * @return The output clusters.
     */
    List<Cluster> createClusters(EventHeader event, List<CalorimeterHit> hits);
    
    /**
     * Perform start of job intialization on this object.
     */
    void initialize();
    
    /**
     * Get the list of numerical cuts.
     * @return The list of numerical cuts.
     */
    double[] getCuts();
    
    /**
     * Get the default cut values.
     * @return The default cut values.
     */
    double[] getDefaultCuts();
    
    /**
     * True if algorithm is using its default cuts.
     * @return True if using the default cuts.
     */
    boolean isDefaultCuts();
    
    /**
     * Set numerical cuts array.
     * @param cuts The numerical cuts.
     */
    void setCuts(double[] cuts);
                 
    /**
     * Get a cut value by its index.
     * @param index The index of the cut.
     * @return The cut value at index.
     */
    double getCut(int index);
    
    /**
     * Get a cut value by name.
     * @param name The name of the cut.
     * @return The named cut.
     */
    double getCut(String name);
    
    /**
     * Set a cut value by name.
     * @param name The name of the cut.
     * @param value The value of the cut.
     */
    void setCut(String name, double value);
    
    /**
     * Set a cut value by index.
     * @param index The index of the cut.
     * @param value The value of the cut.
     */
    void setCut(int index, double value);
    
    /**
     * Get the names of the cuts.
     * @return The names of the cuts.
     */
    String[] getCutNames();       
}

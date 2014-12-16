package org.hps.recon.ecal.cluster;

import org.lcsim.conditions.ConditionsListener;
import org.lcsim.conditions.ConditionsManager;

/**
 * This is a convenience class for creating different kinds of clustering algorithms.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class ClustererFactory {
    
    private ClustererFactory() {        
    }
    
    /**
     * Create a clustering algorithm with a set of cuts.
     * @param name The name of the clustering algorithm.
     * @param cuts The set of cuts (can be null).
     * @return The clustering algorithm.
     * @throw IllegalArgumentException if there is no Clusterer found with name.
     */
    public static Clusterer create(String name, double[] cuts) {
        Clusterer clusterer;
        System.out.println("simple name:" + LegacyClusterer.class.getSimpleName());
        if (LegacyClusterer.class.getSimpleName().equals(name)) {            
            clusterer = new LegacyClusterer();
        } else if (SimpleClasInnerCalClusterer.class.getSimpleName().equals(name)) {
            clusterer = new SimpleClasInnerCalClusterer();
        } else {
            throw new IllegalArgumentException("Unknown clusterer type: " + name);
        }
        if (clusterer instanceof ConditionsListener) {
            ConditionsManager.defaultInstance().addConditionsListener((ConditionsListener) clusterer);
        }
        if (cuts != null) {
            clusterer.setCuts(cuts);
        }
        return clusterer;
    }
    
    /**
     * Create a clustering algorithm with default cut values.
     * @param name The name of the clustering algorithm.
     * @return The clustering algorithm.
     */
    public static Clusterer create(String name) {
        return create(name, null);
    }
}

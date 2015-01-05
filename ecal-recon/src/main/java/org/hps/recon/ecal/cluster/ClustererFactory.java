package org.hps.recon.ecal.cluster;

import org.lcsim.conditions.ConditionsManager;

/**
 * <p>
 * This is a convenience class for creating specific clustering algorithms via their name
 * in the package <code>org.hps.recon.ecal.cluster</code>.  They must implement the {@link Clusterer} 
 * interface.
 * <p>
 * If the name does not match one in the clustering package, the factory will attempt to create
 * a new class instance, assuming that the string is a canonical class name.  It then checks if 
 * this class implements the {@link Clusterer} interface and will throw an error if it does not.
 * 
 * @see Clusterer
 * @see ReconClusterer
 * @see DualThresholdCosmicClusterer
 * @see LegacyClusterer 
 * @see NearestNeighborClusterer
 * @see SimpleReconClusterer
 * @see SimpleCosmicClusterer
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class ClustererFactory {
    
    /**
     * We don't want this class to be instantiated.
     */
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
        if (LegacyClusterer.class.getSimpleName().equals(name)) {
            clusterer = new LegacyClusterer();
        } else if (SimpleReconClusterer.class.getSimpleName().equals(name)) {
            clusterer = new SimpleReconClusterer();
        } else if (ReconClusterer.class.getSimpleName().equals(name)) {
            clusterer = new ReconClusterer();
        } else if (NearestNeighborClusterer.class.getSimpleName().equals(name)) {
            clusterer = new NearestNeighborClusterer();
        } else if (DualThresholdCosmicClusterer.class.getSimpleName().equals(name)) {
            clusterer = new DualThresholdCosmicClusterer();
        } else if (SimpleCosmicClusterer.class.getSimpleName().equals(name)) {
            clusterer = new SimpleCosmicClusterer();
        } else {
            // Try to instantiate a Clusterer object from the name argument, assuming it is a canonical class name.
            try {
                clusterer = fromCanonicalClassName(name);
                if (!clusterer.getClass().isAssignableFrom(Clusterer.class)) {
                    throw new IllegalArgumentException("The class " + name + " does not implement the Clusterer interface.");
                }
            } catch (Exception e) {
                // Okay nothing worked, so we have a problem!
                throw new IllegalArgumentException("Unknown Clusterer type " + name + " cannot be instantiated.", e);
            }
        }
        
        // Register the Clusterer for notification when conditions change.
        ConditionsManager.defaultInstance().addConditionsListener(clusterer);        
        
        // Set cuts if they were provided.
        if (cuts != null) {
            clusterer.getCuts().setValues(cuts);
        }
        return clusterer;
    }
    
    /**
     * Create a clustering algorithm with default cut values.
     * This is just a simple wrapper to {@link #create(String, double[])}.
     * @param name The name of the clustering algorithm.
     * @return The clustering algorithm.
     */
    public static Clusterer create(String name) {
        return create(name, null);
    }
    
    /**
     * Attempt to create a Clusterer object from the canonical class name.
     * @param canonicalName
     * @return The new Clusterer object.
     * @throw IllegalArgumentException if the class does not implement the Clusterer interface.
     */
    private static Clusterer fromCanonicalClassName(String canonicalName) {
        Clusterer clusterer = null;
        try {
            Class<?> clustererClass = Class.forName(canonicalName);
            Object object = clustererClass.newInstance();
            if (!(object instanceof Clusterer)) { 
                throw new IllegalArgumentException("The class " + canonicalName + " does not implement the Clusterer interface.");
            } 
            clusterer = (Clusterer) object;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return clusterer;
    }
}

package org.hps.recon.ecal.cluster;

import org.lcsim.conditions.ConditionsListener;
import org.lcsim.conditions.ConditionsManager;

/**
 * <p>
 * This is a convenience class for creating different kinds of clustering algorithms via their name.
 * <p>
 * The currently available types include:
 * <ul>
 * <li>{@link LegacyClusterer}</li>
 * <li>{@link SimpleClasInnerCalClusterer}</li>
 * <li>{@link ClasInnerCalClusterer</li>
 * </ul>
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
            // Test Run legacy clusterer.
            clusterer = new LegacyClusterer();
        } else if (SimpleClasInnerCalClusterer.class.getSimpleName().equals(name)) {
            // Simple IC clusterer.
            clusterer = new SimpleClasInnerCalClusterer();
        } else if (ClasInnerCalClusterer.class.getSimpleName().equals(name)) {
            // Full IC algorithm.
            clusterer = new ClasInnerCalClusterer();
        } else {
            // Try to instantiate the class from the name argument, assuming it is a canonical class name.
            try {
                clusterer = fromCanonicalClassName(name);
            } catch (Exception e) {
                // Okay nothing worked, so we have a problem!
                throw new IllegalArgumentException("Unknown Clusterer type " + name + " cannot be instantiated.", e);
            }
        }
        // Add the Clusterer as a conditions listener so it can set itself up via the conditions system.
        if (clusterer instanceof ConditionsListener) {
            ConditionsManager.defaultInstance().addConditionsListener((ConditionsListener) clusterer);
        }
        // Set cuts if they were provided.
        if (cuts != null) {
            clusterer.setCuts(cuts);
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
     * @return
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

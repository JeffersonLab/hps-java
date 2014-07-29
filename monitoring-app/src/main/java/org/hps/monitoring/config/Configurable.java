package org.hps.monitoring.config;

/**
 * This is an interface for object's that have a {@link Configuration}
 * which can be set, saved, loaded and reset.  The exact meaning of
 * these operations may vary by type of object.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public interface Configurable {
    
    /**
     * Load configuration into the object.
     * @param config The configuration to load.
     */
    void load(Configuration config);
    
    /**
     * Push values from object into the configuration.
     * @param config The configuration to save.
     */
    void save(Configuration config);
    
    /**
     * Get the current configuration.
     * @return The current configuration.
     */
    Configuration getConfiguration();
    
    /**
     * Save values from the object into the current configuration.
     */
    void save();
    
    /**
     * Set the current configuration.
     * @param config The current configuration.
     */
    void set(Configuration config);
    
    /**
     * Reset the object from the current configuration.
     */
    void reset();
}

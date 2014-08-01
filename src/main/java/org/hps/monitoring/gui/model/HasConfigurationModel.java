package org.hps.monitoring.gui.model;

/**
 * Mixin interface for classes that have an associated {@link ConfigurationModel}.
 */
public interface HasConfigurationModel {    
    /**
     * Set the ConfigurationModel of the object.
     * @param configurationModel The ConfigurationModel.
     */
    void setConfigurationModel(ConfigurationModel configurationModel);
    
    /**
     * Get the current ConfigurationModel of the object.
     * @return The ConfigurationModel.
     */
    ConfigurationModel getConfigurationModel();
}

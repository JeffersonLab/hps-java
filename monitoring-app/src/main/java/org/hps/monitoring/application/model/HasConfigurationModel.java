package org.hps.monitoring.application.model;

/**
 * Mix-in interface for classes that have an associated {@link ConfigurationModel}.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public interface HasConfigurationModel {

    /**
     * Get the current ConfigurationModel of the object.
     *
     * @return The ConfigurationModel.
     */
    ConfigurationModel getConfigurationModel();

    /**
     * Set the ConfigurationModel of the object.
     *
     * @param configurationModel The ConfigurationModel.
     */
    void setConfigurationModel(ConfigurationModel configurationModel);
}

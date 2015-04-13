package org.hps.monitoring.application.model;

/**
 * Mix-in interface for classes that have an associated {@link ConfigurationModel}.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public interface HasConfigurationModel {

    /**
     * Get the current {@link ConfigurationModel} of the object.
     *
     * @return the associated {@link ConfigurationModel}
     */
    ConfigurationModel getConfigurationModel();

    /**
     * Set the {@link ConfigurationModel} of the object.
     *
     * @param configurationModel the new {@link ConfigurationModel}
     */
    void setConfigurationModel(ConfigurationModel configurationModel);
}

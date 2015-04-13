package org.hps.monitoring.application.model;

/**
 * The type of steering to use for event processing.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public enum SteeringType {
    /**
     * Steering from local file on disk.
     */
    FILE,
    /**
     * Steering from resource in jar file.
     */
    RESOURCE;
}

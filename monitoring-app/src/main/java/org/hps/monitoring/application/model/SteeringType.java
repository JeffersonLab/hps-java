package org.hps.monitoring.application.model;

/**
 * The type of steering to use for event processing.
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

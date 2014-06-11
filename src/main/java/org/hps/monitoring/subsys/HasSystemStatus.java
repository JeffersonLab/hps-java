package org.hps.monitoring.subsys;

/**
 * A simple mix-in interface for objects that have {@link SystemStatus}
 * about some monitoring point on a sub-system.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public interface HasSystemStatus {
    
    /**
     * Get the system status object.
     * @return The system status object.
     */
    SystemStatus getSystemStatus();    
}

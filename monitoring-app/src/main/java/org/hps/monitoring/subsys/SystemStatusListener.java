package org.hps.monitoring.subsys;

/**
 * Interface for receiving changes to {@link SystemStatus} objects,
 * e.g. when a new code is set.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public interface SystemStatusListener {
    
    /**
     * Receive a change to the system status.
     * The implementation of this method should absolutely not
     * attempt to change the status!
     * @param status The system status.
     */
    void statusChanged(SystemStatus status);
}

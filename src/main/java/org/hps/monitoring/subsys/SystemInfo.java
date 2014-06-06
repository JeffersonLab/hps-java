package org.hps.monitoring.subsys;

/**
 * Basic interface for information about a detector sub-system.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public interface SystemInfo {

    /**
     * The name of the sub-system e.g. "SVT".
     * @return The name of the sub-system.
     */
    String getName();
    
    /**
     * The current status of the sub-system.
     * @return The sub-system status.
     */
    SystemStatus getStatus();
    
    /**
     * The set of statistics attached to the sub-system.
     * @return The sub-system's statistics.
     */
    SystemStatistics getStatistics();
}

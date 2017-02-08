package org.hps.monitoring.subsys;

/**
 * Listener that is activated from updates to a {@link SystemStatistics} object.
 */
public interface SystemStatisticsListener {
    
    void started(SystemStatistics stats);
    
    void endTick(SystemStatistics stats);
    
    void stopped(SystemStatistics stats);
}

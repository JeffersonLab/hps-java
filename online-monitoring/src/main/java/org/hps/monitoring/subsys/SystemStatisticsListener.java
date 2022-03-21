package org.hps.monitoring.subsys;

public interface SystemStatisticsListener {
    
    void started(SystemStatistics stats);
    
    void endTick(SystemStatistics stats);
    
    void stopped(SystemStatistics stats);
}

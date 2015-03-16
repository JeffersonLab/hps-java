/**
 * 
 */
package org.hps.monitoring.subsys;

/**
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public interface SystemStatisticsListener {
    
    void started(SystemStatistics stats);
    
    void endTick(SystemStatistics stats);
    
    void stopped(SystemStatistics stats);
}

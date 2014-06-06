package org.hps.monitoring.subsys;

/**
 * Implementation of {@link SystemInfo}.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SystemInfoImpl implements SystemInfo {

    String systemName = "";
    SystemStatus status = new SystemStatusImpl();
    SystemStatistics stats = new SystemStatisticsImpl();
    
    public SystemInfoImpl(String systemName) {
        this.systemName = systemName;
    }     
            
    @Override
    public String getName() {
        return systemName;
    }

    @Override
    public SystemStatus getStatus() {
        return status;
    }

    @Override
    public SystemStatistics getStatistics() {
        return stats;
    }

}

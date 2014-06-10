package org.hps.monitoring.subsys.et;

import org.hps.monitoring.record.etevent.EtEventProcessor;
import org.hps.monitoring.subsys.HasSystemInfo;
import org.hps.monitoring.subsys.SystemInfo;
import org.hps.monitoring.subsys.SystemInfoImpl;
import org.hps.monitoring.subsys.SystemStatus.StatusCode;
import org.jlab.coda.et.EtEvent;

/**
 * This is a barebones implementation of an ET system monitor.
 * It does not do much right now but accumulate statistics 
 * and set basic system statuses.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EtSystemMonitor extends EtEventProcessor implements HasSystemInfo {
        
    SystemInfo info = new SystemInfoImpl("EtSystem");    
    
    @Override
    public void startJob() {
        info.getStatistics().start();
        info.getStatus().setStatus(StatusCode.OKAY, "EtSystemMonitor set okay.");
    }
    
    @Override
    public void processEvent(EtEvent event) {
        info.getStatistics().update(event.getLength());
    }    
    
    @Override
    public void endJob() {
        info.getStatistics().stop();
        info.getStatus().setStatus(StatusCode.OFFLINE, "EtSystemMonitor set offline.");
    }

    @Override
    public SystemInfo getSystemInfo() {
        return info;
    }
}

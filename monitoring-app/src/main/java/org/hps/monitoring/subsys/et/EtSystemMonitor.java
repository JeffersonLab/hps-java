package org.hps.monitoring.subsys.et;

import org.hps.monitoring.record.etevent.EtEventProcessor;
import org.hps.monitoring.subsys.HasSystemStatus;
import org.hps.monitoring.subsys.SystemStatus;
import org.hps.monitoring.subsys.SystemStatus.StatusCode;
import org.hps.monitoring.subsys.SystemStatusImpl;
import org.jlab.coda.et.EtEvent;

/**
 * This is just a test class for a monitor of the ET system.
 * It should actually do something useful eventually!
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EtSystemMonitor extends EtEventProcessor implements HasSystemStatus {

    SystemStatus systemStatus;
    int events = 0;
    
    public EtSystemMonitor() {
        systemStatus = new SystemStatusImpl(SystemStatus.SystemName.ET.name(), "Example ET Monitor");
        systemStatus.setStatusCode(StatusCode.UNKNOWN, "System is not active yet.");
    }
    
    public void startJob() {
        systemStatus.setStatusCode(StatusCode.OKAY, "ET job started.");
    }
                   
    public void processEvent(EtEvent event) {
        //systemStatus.setStatusCode(StatusCode.WARNING, "Just a dummy warning message.");
    }
    
    public void endJob() {
        systemStatus.setStatusCode(StatusCode.OFFLINE, "ET job ended.");
    }
        
    @Override
    public SystemStatus getSystemStatus() {
        return systemStatus;
    }
}

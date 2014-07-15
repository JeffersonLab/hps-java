package org.hps.monitoring.subsys.et;

import static org.hps.monitoring.subsys.SystemStatus.SystemName.ET;

import java.util.Timer;
import java.util.TimerTask;

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
public class EtSystemMonitor extends EtEventProcessor {

    SystemStatus systemStatus;
    int events = 0;    
    long eventReceivedMillis = 0;
    long warningIntervalMillis = 1000; /* default of 1 second */
    Timer timer = new Timer("ET Event Monitor");
    
    public EtSystemMonitor() {
        systemStatus = new SystemStatusImpl(ET, "Example ET Monitor");
        systemStatus.setStatusCode(StatusCode.UNKNOWN, "System is not active yet.");
    }
    
    public void setWarningIntervalMillis(long warningIntervalMillis) {
        this.warningIntervalMillis = warningIntervalMillis;
    }
    
    public void startJob() {
        systemStatus.setStatusCode(StatusCode.OKAY, "ET job started.");
        TimerTask task = new TimerTask() {                    
            long startedMillis = 0;
            public void run() {
                if (startedMillis == 0)
                    startedMillis = System.currentTimeMillis();
                long elapsedMillis = 0;
                if (eventReceivedMillis == 0)
                    elapsedMillis = System.currentTimeMillis() - startedMillis;
                else
                    elapsedMillis = System.currentTimeMillis() - eventReceivedMillis;                
                if (elapsedMillis > warningIntervalMillis) 
                    systemStatus.setStatusCode(StatusCode.WARNING, "No ET events received for " + elapsedMillis + " millis.");
                else
                    systemStatus.setStatusCode(StatusCode.OKAY, "ET events received.");
            }            
        };
        
        timer.schedule(task, 0, 1000);
    }
                   
    public void processEvent(EtEvent event) {
        eventReceivedMillis = System.currentTimeMillis();
    }
    
    public void endJob() {
        timer.cancel();
        timer.purge();
        systemStatus.setStatusCode(StatusCode.OFFLINE, "ET job ended.");
    }        
}

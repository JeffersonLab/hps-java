package org.hps.monitoring.subsys.et;

import java.util.Timer;
import java.util.TimerTask;

import org.hps.monitoring.subsys.StatusCode;
import org.hps.monitoring.subsys.Subsystem;
import org.hps.monitoring.subsys.SystemStatus;
import org.hps.monitoring.subsys.SystemStatusImpl;
import org.hps.record.et.EtEventProcessor;
import org.jlab.coda.et.EtEvent;

/**
 * This is a class for monitoring the ET system.
 */
public final class EtSystemMonitor extends EtEventProcessor {

    SystemStatus systemStatus;
    int events = 0;    
    long eventReceivedMillis = 0;
    long warningIntervalMillis = 1000; /* default of 1 second */
    Timer timer = new Timer("ET Event Monitor");
    
    public EtSystemMonitor() {
        systemStatus = new SystemStatusImpl(Subsystem.ET, "ET System Monitor", false);
        systemStatus.setStatus(StatusCode.UNKNOWN, "System is not active yet.");
    }
    
    public void setWarningIntervalMillis(long warningIntervalMillis) {
        this.warningIntervalMillis = warningIntervalMillis;
    }
    
    public void startJob() {
        systemStatus.setStatus(StatusCode.OKAY, "ET job started.");
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
                    systemStatus.setStatus(StatusCode.WARNING, "No ET events received for " + elapsedMillis + " millis.");
                else
                    systemStatus.setStatus(StatusCode.OKAY, "ET events received.");
            }            
        };
        
        timer.schedule(task, 0, 1000);
    }
                   
    public void process(EtEvent event) {
        eventReceivedMillis = System.currentTimeMillis();
    }
    
    public void endJob() {
        timer.cancel();
        timer.purge();
        systemStatus.setStatus(StatusCode.OFFLINE, "ET job ended.");
    }        
}

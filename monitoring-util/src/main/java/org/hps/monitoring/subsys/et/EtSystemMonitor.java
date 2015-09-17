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
 * 
 * @author Jeremy McCormick, SLAC
 */
public final class EtSystemMonitor extends EtEventProcessor {

    SystemStatus systemStatus;
    int events = 0;
    long eventReceivedMillis = 0;
    long warningIntervalMillis = 10000;
    int timerInterval = 10000;
    Timer timer = new Timer("ET Event Monitor");

    public EtSystemMonitor() {
        systemStatus = new SystemStatusImpl(Subsystem.ET, "ET System Monitor", false);
        systemStatus.setStatus(StatusCode.UNKNOWN, "System is not active yet.");
    }

    public void setWarningIntervalMillis(long warningIntervalMillis) {
        this.warningIntervalMillis = warningIntervalMillis;
    }
    
    public void setTimerInterval(int timerInterval) {
        this.timerInterval = timerInterval;
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
                else if (systemStatus.getStatusCode()!=StatusCode.OKAY)
                    systemStatus.setStatus(StatusCode.OKAY, "ET events received.");
            }
        };

        timer.schedule(task, 0, timerInterval);
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

package org.hps.monitoring.subsys.svt;

import java.util.Timer;
import java.util.TimerTask;

import org.hps.monitoring.subsys.StatusCode;
import org.hps.monitoring.subsys.Subsystem;
import org.hps.monitoring.subsys.SystemStatus;
import org.hps.monitoring.subsys.SystemStatusImpl;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.util.Driver;

/**
 * This is a basic example of monitoring hits using an updateable <code>SystemStatus</code>.
 * It checks from a <code>TimerTask</code> once per second whether <code>RawTrackerHit</code> 
 * objects are being received by the {@link #process(EventHeader)} method.
 */
public class BasicHitMonitoringDriver extends Driver {

    SystemStatus status;
    long hitsReceivedMillis;
    long warningIntervalMillis = 1000;
    Timer timer;
    static final String hitsCollectionName = "SVTRawTrackerHits";
                
    public BasicHitMonitoringDriver() {
        status = new SystemStatusImpl(Subsystem.SVT, "Checks that SVT hits are received.", true);
        status.setStatus(StatusCode.UNKNOWN, "Status is unknown.");
    }
    
    public void setWarningIntervalMillis(long warningIntervalMillis) {
        this.warningIntervalMillis = warningIntervalMillis;
    }
   
    public void startOfData() {
        if (hitsCollectionName == null)
            throw new RuntimeException("The hitsCollectionName was never set.");
        status.setStatus(StatusCode.OKAY, "SVT hit monitor started.");
        timer = new Timer("SVT Hit Monitor");
        TimerTask task = new TimerTask() {
            long startedMillis = 0;
            public void run() {
                if (startedMillis == 0)
                    startedMillis = System.currentTimeMillis();
                long elapsedMillis = 0;
                if (hitsReceivedMillis == 0)
                    elapsedMillis = System.currentTimeMillis() - startedMillis;
                else
                    elapsedMillis = System.currentTimeMillis() - hitsReceivedMillis;
                if (elapsedMillis > warningIntervalMillis)
                    status.setStatus(StatusCode.WARNING, "No SVT hits received for " + elapsedMillis + " millis.");
                else
                    status.setStatus(StatusCode.OKAY, "SVT hits received.");
            }
        };
        // Task will run once per second.
        timer.schedule(task, 0, 1000);
    }
        
    public void process(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, hitsCollectionName))
            if (event.get(RawTrackerHit.class, hitsCollectionName).size() > 0)
                hitsReceivedMillis = System.currentTimeMillis();
    }   
    
    public void endOfData() {
        timer.cancel();
        timer.purge();
        status.setStatus(StatusCode.OFFLINE, "SVT hit monitor went offline.");
        status.setActive(false);
    }    
}

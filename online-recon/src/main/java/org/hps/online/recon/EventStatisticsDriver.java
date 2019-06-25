package org.hps.online.recon;

import java.util.logging.Logger;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * Driver to print simple job processing statistics.
 * 
 * @author jeremym
 */
public class EventStatisticsDriver extends Driver {

    // Class logger.
    private static final Logger LOGGER = Logger.getLogger(EventStatisticsDriver.class.getCanonicalName());
    
    private Long startTime = -1L;
    private Long endTime = -1L;
    private Integer eventsProcessed = 0;
    private Integer eventPrintInterval;
    
    private Long MILLIS_TO_SECONDS = 1000L;
    
    void setEventPrintInterval(int eventPrintInterval) {
        this.eventPrintInterval = eventPrintInterval;
    }
        
    public void startOfData() {
        startTime = System.currentTimeMillis();
    }

    public void process(EventHeader event) {
        ++eventsProcessed;
        if (eventsProcessed % eventPrintInterval == 0) {
            printStatistics();
        }
    }
    
    public void endOfData() {
        printStatistics();
    }
    
    private void printStatistics() {
        if (eventsProcessed > 0) {
            endTime = System.currentTimeMillis();
            long elapsed = endTime - startTime;
            if (elapsed > 1000) {
                LOGGER.info('\n' + "Event statistics: " + '\n' +
                        "    Events Processed: " + eventsProcessed + '\n' +
                        "    Elapsed: " + (elapsed / MILLIS_TO_SECONDS) + "s" + '\n' +
                        "    Time per event: " + (elapsed / eventsProcessed) + "ms");
            } else {
                LOGGER.warning("Not enough time elapsed for job statistics.");
            }
        } else {
            LOGGER.warning("No events were processed!");
        }
    }
}

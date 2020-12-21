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

    /** Class logger. */
    private static final Logger LOGGER = Logger.getLogger(EventStatisticsDriver.class.getCanonicalName());
    
    /** Statistics start time in milliseconds. */
    private Long startTime = -1L;
    
    /** Statistics end time in milliseconds. */
    private Long endTime = -1L;
    
    /** The number of events processed. */
    private Integer eventsProcessed = 0;
    
    /**
     * The interval for event printing.  
     * 
     * If negative no statistics will be printed during processing,
     * only at the end of the job. 
     */
    private Integer eventPrintInterval = -1;
    
    /** Conversion of milliseconds to seconds. */
    private Long MILLIS_TO_SECONDS = 1000L;
    
    /**
     * Set the event print interval.
     * @param eventPrintInterval The event print interval
     */
    void setEventPrintInterval(int eventPrintInterval) {
        this.eventPrintInterval = eventPrintInterval;
    }
        
    /**
     * Start of data hook, which sets the statistics start time.
     */
    public void startOfData() {
        startTime = System.currentTimeMillis();
    }

    /**
     * Process events, incrementing number of events processed, and printing
     * event statistics periodically.
     */
    public void process(EventHeader event) {
        ++eventsProcessed;
        if (eventPrintInterval > 0 && eventsProcessed % eventPrintInterval == 0) {
            printStatistics();
        }
    }
    
    /**
     * End of data hook, which always prints final statistics.
     */
    public void endOfData() {
        printStatistics();
    }
    
    /**
     * Print even processing statistics to the logger.
     */
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

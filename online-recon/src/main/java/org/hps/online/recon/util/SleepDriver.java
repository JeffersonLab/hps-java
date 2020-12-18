package org.hps.online.recon.util;

import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;

/**
 * Driver to insert a delay into event processing,
 * e.g. for testing online components with a limited
 * number of source events
 */
public class SleepDriver extends Driver {

    // Default is one second delay
    Long millis = 1000L;

    public SleepDriver() {
    }

    public void process(EventHeader event) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void setMillis(int millis) {
        this.millis = (long) millis;
    }
}

package org.lcsim.hps.evio;

import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;

public interface LCSimEventBuilder {

    /**
     * Read any run information out of an EVIO event (not necessarily a physics event).
     * @param evioEvent
     */
    void readEvioEvent(EvioEvent evioEvent);

    /**
     * Make the LCSim event.
     * @param evioEvent - must be a physics event
     * @return LCSim event
     */
    EventHeader makeLCSimEvent(EvioEvent evioEvent);

    boolean isPhysicsEvent(EvioEvent evioEvent);

    void setDetectorName(String detectorName);

    void setDebug(boolean debug);
}

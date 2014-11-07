package org.hps.evio;

import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.conditions.ConditionsListener;
import org.lcsim.event.EventHeader;

public interface LCSimEventBuilder extends ConditionsListener {

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

    // FIXME: Why is this needed here when checking the header tag is a static operation on the EvioEvent?  
    boolean isPhysicsEvent(EvioEvent evioEvent);

    // FIXME: This should not be a method on the API.  It should come from the conditions system.
    void setDetectorName(String detectorName);

    void setDebug(boolean debug);
}

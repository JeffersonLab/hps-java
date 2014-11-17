package org.hps.record.evio;

import static org.hps.record.evio.EvioEventConstants.END_EVENT_TAG;
import static org.hps.record.evio.EvioEventConstants.GO_EVENT_TAG;
import static org.hps.record.evio.EvioEventConstants.PAUSE_EVENT_TAG;
import static org.hps.record.evio.EvioEventConstants.PHYSICS_EVENT_TAG;
import static org.hps.record.evio.EvioEventConstants.PRESTART_EVENT_TAG;
import static org.hps.record.evio.EvioEventConstants.SYNC_EVENT_TAG;

import org.jlab.coda.jevio.EvioEvent;

/**
 * This is a set of basic static utility methods on <code>EvioEvent</code> objects.  
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class EvioEventUtilities {
    
    private EvioEventUtilities() {        
    }
        
    /**
     * Check if the EVIO event is a PRE START event indicating
     * the beginning of a run.
     * @param event The EvioEvent.
     * @return True if the event is a pre start event.
     */
    public static boolean isPreStartEvent(EvioEvent event) {
        return event.getHeader().getTag() == PRESTART_EVENT_TAG;
    }

    /**
     * Check if the EVIO event is a GO event.
     * @param event The EvioEvent.
     * @return True if the event is a go event.
     */
    public static boolean isGoEvent(EvioEvent event) {
        return event.getHeader().getTag() == GO_EVENT_TAG;
    }

    /**
     * Check if the EVIO event is a PAUSE event.
     * @param event The EvioEvent.
     * @return True if the event is a pause event.
     */
    public static boolean isPauseEvent(EvioEvent event) {
        return event.getHeader().getTag() == PAUSE_EVENT_TAG;
    }

    /**
     * Check if this event is an END event.
     * @param event The EvioEvent.
     * @return True if this event is an end event.
     */
    public static boolean isEndEvent(EvioEvent event) {
        return event.getHeader().getTag() == END_EVENT_TAG;
    }
    
    /**
     * Check if this event has physics data.
     * @param event The EvioEvent.
     * @return True if this event is a physics event.
     */
    public static boolean isPhysicsEvent(EvioEvent event) {
        return event.getHeader().getTag() == PHYSICS_EVENT_TAG;
    }
    
    /**
     * Check if this event is a SYNC event.
     * @param event The EvioEvent.
     * @return True if this event is a SYNC event.
     */
    public static boolean isSyncEvent(EvioEvent event) {
        return event.getHeader().getTag() == SYNC_EVENT_TAG;
    }
}

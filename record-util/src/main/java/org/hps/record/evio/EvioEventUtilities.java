package org.hps.record.evio;

import static org.hps.record.evio.EvioEventConstants.*;

import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;

/**
 * This is a set of basic static utility methods on <code>EvioEvent</code>
 * objects.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class EvioEventUtilities {

    private EvioEventUtilities() {
    }
    
    /**
     * Get the event tag from the header bank.
     * @param event The input EvioEvent.
     * @return The event tag from the header bank.
     */
    public static int getEventTag(EvioEvent event) {
        return event.getHeader().getTag();
    }

    /**
     * Check if the EVIO event is a PRE START event indicating the beginning of
     * a run.
     *
     * @param event The EvioEvent.
     * @return True if the event is a pre start event.
     */
    public static boolean isPreStartEvent(EvioEvent event) {
        return event.getHeader().getTag() == PRESTART_EVENT_TAG;
    }

    /**
     * Check if the EVIO event is a GO event.
     *
     * @param event The EvioEvent.
     * @return True if the event is a go event.
     */
    public static boolean isGoEvent(EvioEvent event) {
        return event.getHeader().getTag() == GO_EVENT_TAG;
    }

    /**
     * Check if the EVIO event is a PAUSE event.
     *
     * @param event The EvioEvent.
     * @return True if the event is a pause event.
     */
    public static boolean isPauseEvent(EvioEvent event) {
        return event.getHeader().getTag() == PAUSE_EVENT_TAG;
    }

    /**
     * Check if this event is an END event.
     *
     * @param event The EvioEvent.
     * @return True if this event is an end event.
     */
    public static boolean isEndEvent(EvioEvent event) {
        return event.getHeader().getTag() == END_EVENT_TAG;
    }

    /**
     * Check if this event has physics data.
     *
     * @param event The EvioEvent.
     * @return True if this event is a physics event.
     */
    public static boolean isPhysicsEvent(EvioEvent event) {
        return (event.getHeader().getTag() >= PHYSICS_START_TAG ||
                event.getHeader().getTag() < SYNC_EVENT_TAG);
        // return event.getHeader().getTag() == PHYSICS_EVENT_TAG;
    }

    /**
     * Check if this event is a SYNC event.
     *
     * @param event The EvioEvent.
     * @return True if this event is a SYNC event.
     */
    public static boolean isSyncEvent(EvioEvent event) {
        return event.getHeader().getTag() == SYNC_EVENT_TAG;
    }
    
    /**
     * Check if this event is an EPICS event containing scalar data.
     * 
     * @param event The EvioEvent.
     * @return True if this event is an EPICS event.
     */
    public static boolean isEpicsEvent(EvioEvent event) {
        return event.getHeader().getTag() == EPICS_EVENT_TAG;
    }
    
    /**
     * True if <code>event</code> is an EVIO control event.
     * @return True if event is a control event.
     */
    public static boolean isControlEvent(EvioEvent event) {
        return isPreStartEvent(event) || 
                isGoEvent(event) || 
                isPauseEvent(event) || 
                isEndEvent(event) || 
                isSyncEvent(event) ||
                isEpicsEvent(event);
    }

    /**
     * Extract the CODA run data stored in a control event.
     *
     * @param event The EvioEvent.
     * @return The int data for the control event. Null if the event is not a
     * control event, or the int bank for the control event is not found.
     */
    public static int[] getControlEventData(EvioEvent event) {
        int eventTag = event.getHeader().getTag();
        switch (eventTag) { //if the event's not a control event, stop
            case PRESTART_EVENT_TAG:
            case PAUSE_EVENT_TAG:
            case END_EVENT_TAG:
            case SYNC_EVENT_TAG:
            case GO_EVENT_TAG:
                break;
            default:
                return null;
        }

        int[] data = event.getIntData();
        if (data != null) { //found the data in the top-level bank
            return data;
        } else { //data is not in event bank; look for the data bank whose tag matches the event type
            for (BaseStructure bank : event.getChildrenList()) {
                if (bank.getHeader().getTag() == eventTag) {
                    return bank.getIntData(); //return whatever int data this bank has
                }
            }
            return null; //we didn't find the bank; give up
        }
    }
    
    /**
     * Get the head bank with event header that includes run number.
     * This is a nested bank.
     * @param evioEvent The EVIO event.
     * @return The head bank or null if does not exist in this event.
     */
    public static BaseStructure getHeadBank(EvioEvent evioEvent) {
        if (evioEvent.getChildCount() > 0) {
            for (BaseStructure topBank : evioEvent.getChildrenList()) {
                if (topBank.getChildrenList() != null) {
                    for (BaseStructure nestedBank : topBank.getChildrenList()) {
                        if (nestedBank.getHeader().getTag() == EvioEventConstants.HEAD_BANK_TAG) {
                            return nestedBank;
                        }
                    }
                }
            }
        }
        return null;        
    }
    
    /**
     * Get the run number from an EVIO event.
     * @return The run number.
     */
    public static int getRunNumber(EvioEvent event) {
        if (isControlEvent(event)) {
            return getControlEventData(event)[1];
        } else if (isPhysicsEvent(event)) {
            BaseStructure headBank = EvioEventUtilities.getHeadBank(event);
            if (headBank != null) {                                        
                return headBank.getIntData()[1];   
            } else {
                throw new IllegalArgumentException("Head bank is missing from physics event.");
            }
        } else {
            // Not sure if this would ever happen.
            throw new IllegalArgumentException("Wrong event type: " + event.getHeader().getTag());
        }
    }
}

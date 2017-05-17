package org.hps.record.evio;

import static org.hps.record.evio.EvioEventConstants.END_EVENT_TAG;
import static org.hps.record.evio.EvioEventConstants.EPICS_EVENT_TAG;
import static org.hps.record.evio.EvioEventConstants.GO_EVENT_TAG;
import static org.hps.record.evio.EvioEventConstants.PAUSE_EVENT_TAG;
import static org.hps.record.evio.EvioEventConstants.PHYSICS_START_TAG;
import static org.hps.record.evio.EvioEventConstants.PRESTART_EVENT_TAG;
import static org.hps.record.evio.EvioEventConstants.SYNC_EVENT_TAG;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.record.daqconfig.EvioDAQParser;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * This is a set of basic static utility methods for <code>EvioEvent</code> objects.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class EvioEventUtilities {

    /**
     * Class should not be instantiated.
     */
    private EvioEventUtilities() {
        throw new UnsupportedOperationException("Do not instantiate this class.");
    }
    
    /**
     * Extract the CODA run data stored in a control event.
     *
     * @param event the <code>EvioEvent</code> to handle
     * @return the <code>int</code> data array for the control event or <code>null</code> if the event is not a control
     *         event or data bank is not found
     */
    public static int[] getControlEventData(final EvioEvent event) {
        final int eventTag = event.getHeader().getTag();
        switch (eventTag) { // if the event's not a control event, stop
            case PRESTART_EVENT_TAG:
            case PAUSE_EVENT_TAG:
            case END_EVENT_TAG:
            case SYNC_EVENT_TAG:
            case GO_EVENT_TAG:
                break;
            default:
                return null;
        }

        final int[] data = event.getIntData();
        if (data != null) { // found the data in the top-level bank
            return data;
        } else { // data is not in event bank; look for the data bank whose tag matches the event type
            for (final BaseStructure bank : event.getChildrenList()) {
                if (bank.getHeader().getTag() == eventTag) {
                    return bank.getIntData(); // return whatever int data this bank has
                }
            }
            return null; // we didn't find the bank; give up
        }
    }

    /**
     * Get the event tag from the header bank.
     *
     * @param event the input <code>EvioEvent</code>
     * @return the event tag from the header bank
     */
    public static int getEventTag(final EvioEvent event) {
        return event.getHeader().getTag();
    }

    /**
     * Get the head bank with event header that includes run number.
     * <p>
     * This is a nested bank.
     *
     * @param evioEvent the <code>EvioEvent</code> with the head bank
     * @return the head bank or <code>null</code> if it does not exist
     */
    public static BaseStructure getHeadBank(final EvioEvent evioEvent) {
        if (evioEvent.getChildCount() > 0) {
            for (final BaseStructure topBank : evioEvent.getChildrenList()) {
                if (topBank.getChildrenList() != null) {
                    for (final BaseStructure nestedBank : topBank.getChildrenList()) {
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
     * Get the head bank int data.
     * 
     * @param evioEvent the EVIO event
     * @return the head bank int data
     */
    public static int[] getHeadBankData(final EvioEvent evioEvent) {
        return getHeadBank(evioEvent).getIntData();
    }

    /**
     * Get the run number from an EVIO event.
     *
     * @return the run number or <code>null</code> if not present in event
     */
    public static Integer getRunNumber(final EvioEvent event) {
        if (isControlEvent(event)) {
            return getControlEventData(event)[1];
        } else if (isPhysicsEvent(event)) {
            final BaseStructure headBank = EvioEventUtilities.getHeadBank(event);
            if (headBank != null) {
                return headBank.getIntData()[1];
            } 
        } 
        return null;
    }

    /**
     * Return <code>true</code> if <code>event</code> is a CODA control event such as a PRESTART or GO event.
     *
     * @return <code>true</code> if event is a control event
     */
    public static boolean isControlEvent(final EvioEvent event) {
        return isPreStartEvent(event) || isGoEvent(event) || isPauseEvent(event) || isEndEvent(event)
                || isSyncEvent(event) || isEpicsEvent(event);
    }

    /**
     * Check if this event is an END event.
     *
     * @param event the <code>EvioEvent</code> to check
     * @return <code>true</code> if this event is an END event.
     */
    public static boolean isEndEvent(final EvioEvent event) {
        return event.getHeader().getTag() == END_EVENT_TAG;
    }

    /**
     * Check if this event is an EPICS event.
     *
     * @param event the <code>EvioEvent</code> to check
     * @return <code>true</code> if this event is an EPICS event
     */
    public static boolean isEpicsEvent(final EvioEvent event) {
        return event.getHeader().getTag() == EPICS_EVENT_TAG;
    }

    /**
     * Check if the event is a GO event.
     *
     * @param event the <code>EvioEvent</code> to check
     * @return <code>true</code> if the event is a GO event.
     */
    public static boolean isGoEvent(final EvioEvent event) {
        return event.getHeader().getTag() == GO_EVENT_TAG;
    }

    /**
     * Check if the EVIO event is a PAUSE event.
     *
     * @param event the <code>EvioEvent</code> to check
     * @return <code>true</code> if the event is a PAUSE event.
     */
    public static boolean isPauseEvent(final EvioEvent event) {
        return event.getHeader().getTag() == PAUSE_EVENT_TAG;
    }

    /**
     * Check if this event has physics data.
     *
     * @param event the <code>EvioEvent</code> to check
     * @return <code>true</code> if this event is a physics event
     */
    public static boolean isPhysicsEvent(final EvioEvent event) {
        // This checks if the tag is outside the CODA control event range.
        return event.getHeader().getTag() >= PHYSICS_START_TAG || event.getHeader().getTag() < SYNC_EVENT_TAG;
        // return event.getHeader().getTag() == PHYSICS_EVENT_TAG;
    }

    /**
     * Check if the EVIO event is a PRESTART event indicating the beginning of a run.
     *
     * @param event the <code>EvioEvent</code> to check
     * @return <code>true</code> if the event is a PRESTART event
     */
    public static boolean isPreStartEvent(final EvioEvent event) {
        return event.getHeader().getTag() == PRESTART_EVENT_TAG;
    }

    /**
     * Check if this event is a SYNC event.
     *
     * @param event the <code>EvioEvent</code> to check
     * @return <code>true</code> if this event is a SYNC event
     */
    public static boolean isSyncEvent(final EvioEvent event) {
        return event.getHeader().getTag() == SYNC_EVENT_TAG;
    }

    /**
     * Manually set the event number on an <code>EvioEvent</code> from its "EVENT ID" bank.
     *
     * @param evioEvent the input <code>EvioEvent</code>
     */
    public static void setEventNumber(final EvioEvent evioEvent) {
        int eventNumber = -1;
        if (evioEvent.getChildrenList() != null) {
            for (final BaseStructure bank : evioEvent.getChildrenList()) {
                if (bank.getHeader().getTag() == EvioEventConstants.EVENTID_BANK_TAG) {
                    eventNumber = bank.getIntData()[0];
                    break;
                }
            }
        }
        if (eventNumber != -1) {
            evioEvent.setEventNumber(eventNumber);
        }
    }
    
    /**
     * Get integer data from the event ID bank of an EVIO event.
     * 
     * @param evioEvent the input EVIO event
     * @return the event ID integer array or <code>null</code> if not found
     */
    public static int[] getEventIdData(EvioEvent evioEvent) {
        int[] eventId = null;
        if (evioEvent.getChildCount() > 0) {
            for (final BaseStructure bank : evioEvent.getChildrenList()) {
                if (bank.getHeader().getTag() == EvioEventConstants.EVENTID_BANK_TAG) {
                    eventId = bank.getIntData();
                }
            }
        }
        return eventId;
    }
                  
    static EvioDAQParser createDAQConfig(EvioEvent evioEvent, int crate, BaseStructure subBank) {
        
        EvioDAQParser parser = new EvioDAQParser();
        
        // Get run number from EVIO event.
        int runNumber = EvioEventUtilities.getRunNumber(evioEvent);
        
        // Initialize the conditions system if necessary as the DAQ config parsing classes use it.
        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        if (!conditionsManager.isInitialized() || conditionsManager.getRun() != runNumber) {
            try {
                DatabaseConditionsManager.getInstance().setDetector("HPS-dummy-detector", runNumber);
            } catch (ConditionsNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        
        // Create the trigger config from the EVIO data.
        parser.parse(
                crate, 
                runNumber, 
                subBank.getStringData());
        
        return parser;
    }
    
    public static List<EvioDAQParser> getDAQConfig(EvioEvent evioEvent) {
        List<EvioDAQParser> triggerConfig = new ArrayList<EvioDAQParser>();
        outerLoop: for (BaseStructure bank : evioEvent.getChildrenList()) {
            if (bank.getChildCount() <= 0)
                continue;
            int crate = bank.getHeader().getTag();
            for (BaseStructure subBank : bank.getChildrenList()) {
                if (EvioBankTag.TRIGGER_CONFIG.equals(subBank)) {
                    if (subBank.getStringData() == null) {                        
                        throw new RuntimeException("Trigger config bank is missing string data.");
                    }
                    triggerConfig.add(createDAQConfig(evioEvent, crate, subBank));
                }
            }
        }
        return triggerConfig;
    }
}

package org.hps.evio;

import org.jlab.coda.jevio.EvioEvent;

public final class EventConstants {

    //event type tag
    public static final int PHYSICS_EVENT_TAG = 1;
    public static final int SYNC_EVENT_TAG = 16;
    public static final int PRESTART_EVENT_TAG = 17;
    public static final int GO_EVENT_TAG = 18;
    public static final int PAUSE_EVENT_TAG = 19;
    public static final int END_EVENT_TAG = 20;
    //event type tag
    public static final int EVENTID_BANK_TAG = 0xC000;
    public static final int EVENT_BANK_NUM = 0xCC;
    // These correspond to ROC (readout crate) IDs from the DAQ.
    public static final int ECAL_TOP_BANK_TAG = 0x1;
    public static final int ECAL_BOTTOM_BANK_TAG = 0x2;
    public static final int SVT_BANK_TAG = 0x3;
    // These values are put into the number field of the banks.
    // FIXME Bank numbers should actually be sequentially numbered and not hard-coded.
    public static final int SVT_BANK_NUMBER = 1;
    public static final int ECAL_BANK_NUMBER = 1;
    public static final int TRIGGER_BANK_NUMBER = 1;
    public static final int ECAL_WINDOW_MODE = 1;
    public static final int ECAL_PULSE_MODE = 2;
    public static final int ECAL_PULSE_INTEGRAL_MODE = 3;
    // The composite data format for window ecal data.
    public static final String ECAL_WINDOW_FORMAT = "c,i,l,N(c,Ns)";
    // The composite data format for pulse ecal data.
    public static final String ECAL_PULSE_FORMAT = "c,i,l,N(c,N(c,Ns))";
    // The composite data format for pulse integral ecal data.
    public static final String ECAL_PULSE_INTEGRAL_FORMAT = "c,i,l,N(c,N(s,i))";
    // The tag for ECal window data.
    public static final int ECAL_WINDOW_BANK_TAG = 0xe101;
    // The tag for ECal pulse data.
    public static final int ECAL_PULSE_BANK_TAG = 0xe102;
    // The tag for ECal pulse integral data.
    public static final int ECAL_PULSE_INTEGRAL_BANK_TAG = 0xe103;
    // The tag for trigger data.
    public static final int TRIGGER_BANK_TAG = 0xe106;
    public static final int SVT_TOTAL_NUMBER_FPGAS = 8;
    public static final int MC_TIME = 2019686400; //Unix time (in seconds) used for Monte Carlo data - 1/1/2034

    public static boolean isSyncEvent(EvioEvent event) {
        return event.getHeader().getTag() == EventConstants.SYNC_EVENT_TAG;
    }

    // TODO: Move these methods into an EvioEventUtil class.
    
    /**
     * Check if this event is a Pre Start Event.
     *
     * @param event
     * @return
     */
    public static boolean isPreStartEvent(EvioEvent event) {
        return event.getHeader().getTag() == EventConstants.PRESTART_EVENT_TAG;
    }

    public static boolean isGoEvent(EvioEvent event) {
        return event.getHeader().getTag() == EventConstants.GO_EVENT_TAG;
    }

    public static boolean isPauseEvent(EvioEvent event) {
        return event.getHeader().getTag() == EventConstants.PAUSE_EVENT_TAG;
    }

    /**
     * Check if this event is an End Event.
     *
     * @param event
     * @return True if this event is an End Event; false if not.
     */
    public static boolean isEndEvent(EvioEvent event) {
        return event.getHeader().getTag() == EventConstants.END_EVENT_TAG;
    }
}

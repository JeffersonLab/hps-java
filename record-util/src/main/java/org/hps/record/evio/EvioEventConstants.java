package org.hps.record.evio;

public class EvioEventConstants {
    
    // This is the old tag for physics events.
    public static final int PHYSICS_EVENT_TAG = 1;
    
    // CODA control event tags.
    public static final int SYNC_EVENT_TAG = 16;
    public static final int PRESTART_EVENT_TAG = 17;
    public static final int GO_EVENT_TAG = 18;
    public static final int PAUSE_EVENT_TAG = 19;
    public static final int END_EVENT_TAG = 20;
    
    // Special tag for events with EPICS scalars.
    public static final int EPICS_EVENT_TAG = 31;
    
    // Event tags greater than or equal to this will be physics events.
    public static final int PHYSICS_START_TAG = 32;
    
    public static final int EVENTID_BANK_TAG = 0xC000;
  
    // Bank tag for header bank with run and event numbers in physics events.
    public static final int HEAD_BANK_TAG = 0xe10F;
  
    public static final int EPICS_BANK_TAG = 57620;
    public static final int EPICS_BANK_TAG_2s = -1;
    public static final int EPICS_BANK_TAG_20s = -1;
    //public static final int EPICS_BANK_TAG_2s = -1;
    //public static final int EPICS_BANK_TAG_20s = -1;
    
}

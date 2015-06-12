package org.hps.record.evio;

/**
 * This is a set of static EVIO event constants.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class EvioEventConstants {

    /**
     * END event tag.
     */
    public static final int END_EVENT_TAG = 20;

    /**
     * EPICS bank tag.
     */
    public static final int EPICS_BANK_TAG = 57620;

    /**
     * EPICS 20 second event tag.
     */
    // FIXME: This is unused and not handled in event processing.
    public static final int EPICS_BANK_TAG_20s = -1;

    /**
     * EPICS 2 second event tag.
     */
    // FIXME: This is unused and not handled in event processing.
    public static final int EPICS_BANK_TAG_2s = -1;

    /**
     * EPICS event tag.
     */
    public static final int EPICS_EVENT_TAG = 31;

    /**
     * Event ID bank tag.
     */
    public static final int EVENTID_BANK_TAG = 0xC000;

    /**
     * GO event tag.
     */
    public static final int GO_EVENT_TAG = 18;

    /**
     * Bank tag for header bank with run and event numbers in physics events.
     */
    public static final int HEAD_BANK_TAG = 0xe10F;

    /**
     * Pause event tag.
     */
    // FIXME: Not generally handled or used in event processing.
    public static final int PAUSE_EVENT_TAG = 19;

    /**
     * This is the old tag for physics events.
     */
    public static final int PHYSICS_EVENT_TAG = 1;

    /**
     * Event tags greater than or equal to this will be physics events.
     */
    public static final int PHYSICS_START_TAG = 32;

    /**
     * PRESTART event tag.
     */
    public static final int PRESTART_EVENT_TAG = 17;

    /**
     * Tag of the scalers integer bank, which is a child of the crate bank.
     */
    public static final int SCALERS_BANK_TAG = 57621;

    /**
     * Tag of the scalers crate bank, which is a child of the top bank.
     */
    public static final int SCALERS_CRATE_TAG = 39;

    /**
     * CODA SYNC event tag.
     */
    public static final int SYNC_EVENT_TAG = 16;

    /**
     * Disallow class instantiation.
     */
    private EvioEventConstants() {
        throw new UnsupportedOperationException("Do not instantiate this class.");
    }
}

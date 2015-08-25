package org.hps.record.evio;

import org.jlab.coda.jevio.EvioEvent;

/**
 * Encapsulates event tag constants for EVIO events as described at <a
 * href="https://confluence.slac.stanford.edu/display/hpsg/EVIO+Data+Format#EVIODataFormat-EVIOEventtypes-2015DataSet"
 * >EVIO Event types</a>.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public enum EventTagConstant {

    /** CODA END event inserted at end of run. */
    END(20),
    /** EPICS event containing variable values. */
    EPICS(31),
    /** CODA GO event indicating event stream is starting. */
    GO(18),
    /** CODA PAUSE event indicating the pause button was pressed in the GUI. */
    PAUSE(19),
    /** The old physics event tag by HPS convention. */
    PHYSICS_OLD(1),
    /** CODA PRESTART event when run initializes. */
    PRESTART(17),
    /** CODA SYNC event. */
    SYNC(16);

    /**
     * The tag value.
     */
    private final int tag;

    /**
     * Constructor with tag value.
     * 
     * @param tag the tag value
     */
    private EventTagConstant(final int tag) {
        this.tag = tag;
    }

    /**
     * Return <code>true</code> if the event tag matches this one.
     * 
     * @param event the <code>EvioEvent</code> to check
     * @return <code>true</code> if the event's tag matches this one
     */
    public boolean isEventTag(final EvioEvent event) {
        return event.getHeader().getTag() == this.tag;
    }

    /**
     * Get the tag value.
     * 
     * @return the tag value
     */
    public int tag() {
        return this.tag;
    }
    
    public boolean equals(final int tag) {
        return tag == this.tag;
    }
    
    public boolean equals(final EvioEvent evioEvent) {
        return evioEvent.getHeader().getTag() == this.tag;
    }
}

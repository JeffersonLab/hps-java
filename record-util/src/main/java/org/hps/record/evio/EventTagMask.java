package org.hps.record.evio;

/**
 * Event tag mask for physics and sync events.
 */
public enum EventTagMask {
    
    /** Sync event with scalers and/or trigger config. */
    SYNC(0x40),
    /** Physics events. */
    PHYSICS(0x80);

    /**
     * Define an event tag with a mask.
     * @param mask the bit mask value
     */
    EventTagMask(int mask) {
        this.mask = mask;
    }
    
    private int mask;
    
    /**
     * Get the tag's mask value.
     * @return the tag's mask value
     */
    public int getMask() {
        return mask;
    }
    
    /**
     * Return <code>true</code> if the <code>eventTag</code> matches this mask.
     * @param eventTag the event tag value from the EVIO header
     * @return <code>true</code> if <code>eventTag</code> matches this mask
     */
    public boolean matches(int eventTag) {
        return (eventTag & mask) > 0;
    }   
}

package org.hps.record.evio;

import java.util.HashSet;
import java.util.Set;

import org.jlab.coda.jevio.EvioEvent;

/**
 * Encapsulates bit mask values for different types of physics events as described at 
 * <a href="https://confluence.slac.stanford.edu/display/hpsg/EVIO+Data+Format#EVIODataFormat-EVIOEventtypes-2015DataSet">EVIO Event types</a>.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public enum EventTagBitMask {

    /** LED or Cosmic trigger. */
    LED_COSMIC(4),
    /** Pair 0 trigger. */
    PAIRS0(2),
    /** Pair 1 trigger. */
    PAIRS1(3),
    /** Physics event. */
    PHYSICS(7), // FIXME: Doesn't work!
    /** Pulser triggered event. */
    PULSER(5),
    /** Single 0 trigger. */
    SINGLE0(0),
    /** Single 1 trigger. */
    SINGLE1(1),
    /** Physics sync event. */
    SYNC(6);

    /**
     * The bit number.
     */
    private int bit;

    /**
     * The bit mask.
     */
    private int bitMask;

    /**
     * Constructor with bit number.
     *
     * @param bit the bit number
     */
    private EventTagBitMask(final int bit) {
        this.bit = bit;
        this.bitMask = 1 >> this.bit;
    }

    /**
     * Get the bit number.
     *
     * @return the bit number
     */
    public int getBit() {
        return this.bit;
    }

    /**
     * Get the bit mask.
     *
     * @return the bit mask
     */
    public int getBitMask() {
        return this.bitMask;
    }

    /**
     * Return <code>true</code> if the event's tag matches this mask.
     *
     * @param event an <code>EvioEvent</code> with tag to check against this mask
     * @return <code>true</code> if the event's tag matches this mask
     */
    public boolean equals(final EvioEvent event) {
        return equals(event.getHeader().getTag());
    }

    /**
     * Return <code>true</code> if the tag matches this mask.
     *
     * @param eventTag the event's tag from the header bank
     * @return <code>true</code> if the tag matches this mask
     */
    public boolean equals(final int eventTag) {
        return (eventTag & this.bitMask) == 1;
    }
    
    public static Set<EventTagBitMask> getEventTagBitMasks(EvioEvent evioEvent) {
        Set<EventTagBitMask> bitMasks = new HashSet<EventTagBitMask>();
        if (LED_COSMIC.equals(evioEvent)) {
            bitMasks.add(LED_COSMIC);
        } if (PAIRS0.equals(evioEvent)) {
            bitMasks.add(PAIRS0);
        } if (PAIRS1.equals(evioEvent)) {
            bitMasks.add(PAIRS1);
        } if (PHYSICS.equals(evioEvent)) {
            bitMasks.add(PHYSICS);
        } if (PULSER.equals(evioEvent)) {
            bitMasks.add(PULSER);
        } if (SINGLE0.equals(evioEvent)) {
            bitMasks.add(SINGLE0);
        } if (SINGLE1.equals(evioEvent)) {
            bitMasks.add(SINGLE1);
        } if (SYNC.equals(evioEvent)) {
            bitMasks.add(SYNC);
        }
        return bitMasks;
    }
}
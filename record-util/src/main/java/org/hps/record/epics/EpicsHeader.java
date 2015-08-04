package org.hps.record.epics;

/**
 * Representation of EPICs header data (run, sequence, time stamp).
 *
 * @author Jeremy McCormick, SLAC
 */
public final class EpicsHeader {

    /**
     * Create an {@link EpicsHeader} from an int array in the EVIO bank.
     * <p>
     * This reads in indices 1 to 3 as 0 and 5 are unused.
     *
     * @param headerBank the header bank data
     * @return the {@link EpicsHeader} object
     */
    static EpicsHeader fromEvio(final int[] headerBank) {
        final int[] headerData = new int[] {headerBank[1], headerBank[2], headerBank[3]};
        return new EpicsHeader(headerData);
    }

    /**
     * The run number.
     */
    private final int run;

    /**
     * The sequence number.
     */
    private final int sequence;

    /**
     * The time stamp in seconds (Unix).
     */
    private final int timestamp;

    /**
     * Class constructor.
     * <p>
     * The data array should be length 3 and usually will come from the int data of a <code>GenericObject</code>.
     *
     * @param data the header data with length 3
     */
    public EpicsHeader(final int[] data) {
        if (data.length != 3) {
            throw new IllegalArgumentException("Bad array length: " + data.length);
        }
        run = data[0];
        sequence = data[1];
        timestamp = data[2];
    }

    /**
     * Get the run number.
     *
     * @return the run number
     */
    public int getRun() {
        return run;
    }

    /**
     * Get the sequence number.
     *
     * @return the sequence number
     */
    public int getSequence() {
        return sequence;
    }

    /**
     * Get the time stamp.
     *
     * @return the time stamp
     */
    public int getTimeStamp() {
        return timestamp;
    }
}

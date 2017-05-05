package org.hps.record;

/**
 * An <code>Exception</code> thrown when end of run occurs in event processing.
 */
@SuppressWarnings("serial")
public final class EndRunException extends RuntimeException {

    /**
     * The run number.
     */
    private final int runNumber;

    /**
     * Class constructor.
     *
     * @param message the message
     * @param runNumber the run number
     */
    public EndRunException(final String message, final int runNumber) {
        super(message);
        this.runNumber = runNumber;
    }

    /**
     * Get the run number.
     *
     * @return The run number.
     */
    public int getRunNumber() {
        return this.runNumber;
    }

}

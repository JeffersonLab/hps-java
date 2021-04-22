package org.hps.record;

/**
 * Exception thrown when maximum number of records is reached.
 */
// FIXME: Use loop methods instead of this for controlling number of records run.
@SuppressWarnings("serial")
public final class MaxRecordsException extends RuntimeException {

    /**
     * The maximum number of records.
     */
    private final long maxRecords;

    /**
     * Class constructor.
     *
     * @param message the message
     * @param maxRecords the maximum number of records
     */
    public MaxRecordsException(final String message, final long maxRecords) {
        super(message);
        this.maxRecords = maxRecords;
    }

    /**
     * Get the maximum number of records.
     *
     * @return the maximum number of records
     */
    public long getMaxRecords() {
        return this.maxRecords;
    }
}

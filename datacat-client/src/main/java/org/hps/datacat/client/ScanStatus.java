package org.hps.datacat.client;

/**
 * The scan status of {@link Dataset}.
 * 
 * @author Jeremy McCormick, SLAC
 */
public enum ScanStatus {
    /**
     * Scan status is not known.
     */
    UNKNOWN,
    /**
     * Dataset has not been scanned.
     */
    UNSCANNED,
    /**
     * Dataset has been scanned.
     */
    OK
}

package org.hps.datacat;

/**
 * Dataset types for HPS.
 * 
 * @author jeremym
 */
public enum DataType {
    /**
     * Data quality management plots (AIDA or ROOT).
     */
    DQM,
    /**
     * Raw data (EVIO).
     */
    RAW,
    /**
     * Reconstructed data (LCIO).
     */
    RECON,
    /**
     * Digital Summary Tape files (ROOT). 
     */
    DST,
    /**
     * Test type (don't use in production).
     */
    TEST;
}

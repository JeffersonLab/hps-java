package org.hps.crawler;

/**
 * Dataset types for HPS.
 * 
 * @author Jeremy McCormick, SLAC
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

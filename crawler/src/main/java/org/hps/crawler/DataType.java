package org.hps.crawler;

/**
 * Dataset types for HPS.
 * 
 * @author Jeremy McCormick, SLAC
 */
public enum DataType {
    /**
     * Data quality management plots.
     */
    DQM,
    /**
     * Raw data (EVIO).
     */
    RAW,
    /**
     * Reconstructed data (usually LCIO).
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

package org.hps.datacat.client;

/**
 * Dataset types for HPS.
 * 
 * @author Jeremy McCormick, SLAC
 */
public enum DatasetDataType {
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

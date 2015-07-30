package org.hps.record.scalers;

/**
 * Represents an index into a {@link ScalerData} array from EVIO data (full 72 word integer array).
 *
 * @author Jeremy McCormick, SLAC
 */
public enum ScalerDataIndex {

    /**
     * Gated clock.
     */
    CLOCK_GATED(67),
    /**
     * Ungated clock.
     */
    CLOCK_UNGATED(68),
    /**
     * Gated Faraday cup TDC threshold.
     */
    FCUP_TDC_GATED(3),
    /**
     * Ungated Faraday cup TDC threshold.
     */
    FCUP_TDC_UNGATED(35),
    /**
     * Gated Faraday cup TRG threshold.
     */
    FCUP_TRG_GATED(19),
    /**
     * Ungated Faraday cup TRG threshold.
     */
    FCUP_TRG_UNGATED(51);

    /**
     * The index value within the scaler data array.
     */
    private int index;

    /**
     * Create a scaler data index.
     *
     * @param index the index within the data array
     */
    ScalerDataIndex(final int index) {
        this.index = index;
    }

    /**
     * Get the index value.
     *
     * @return the index value
     */
    public int index() {
        return this.index;
    }
}

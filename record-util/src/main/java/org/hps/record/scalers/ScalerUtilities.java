package org.hps.record.scalers;

/**
 * Utilities methods for scaler data.
 * <p>
 * Currently this is used only for computing live time measurements from standard scaler data.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class ScalerUtilities {

    /**
     * Indices for getting live time measurements.
     */
    public enum LiveTimeIndex {
        CLOCK, /* Faraday cup TDC */
        FCUP_TDC, /* Faraday cup TRG */
        FCUP_TRG; /* clock */
    }

    /**
     * Get a specific live time measurement by index.
     *
     * @param index the enum of the index type
     * @see LiveTimeIndex
     */
    public static double getLiveTime(final ScalerData data, final LiveTimeIndex index) {
        return getLiveTimes(data)[index.ordinal()];
    }

    /**
     * Get the live time measurements from standard scaler data.
     * <p>
     * This is returned as a double array of size 3 containing:</br>
     *
     * <pre>
     * [0] = FCUP TDC measurement
     * [1] = FCUP TRG measurement
     * [2] = CLOCK measurement
     * </pre>
     *
     * This method assumes the standard scaler data structure as outlined in <a
     * href="https://jira.slac.stanford.edu/browse/HPSJAVA-470">HPSJAVA-470</a>.
     *
     * @param data the scaler data
     * @return the live time measurements
     */
    public static double[] getLiveTimes(final ScalerData data) {

        // [03] - gated faraday cup with "TDC" threshold
        final int fcupTdcGated = data.getValue(ScalerDataIndex.FCUP_TDC_GATED);

        // [19] - gated faraday cup with "TRG" threshold
        final int fcupTrgGated = data.getValue(ScalerDataIndex.FCUP_TRG_GATED);

        // [35] - ungated faraday cup with "TDC" threshold
        final int fcupTdcUngated = data.getValue(ScalerDataIndex.FCUP_TDC_UNGATED);

        // [51] - ungated faraday cup with "TRG" threshold
        final int fcupTrgUndated = data.getValue(ScalerDataIndex.FCUP_TRG_UNGATED);

        // [67] - gated clock
        final int clockGated = data.getValue(ScalerDataIndex.CLOCK_GATED);

        // [68] - ungated clock
        final int clockUngated = data.getValue(ScalerDataIndex.CLOCK_UNGATED);

        // [03]/[35] = FCUP TDC
        final double fcupTdc = (double) fcupTdcGated / (double) fcupTdcUngated;

        // [19]/[51] = FCUP TRG
        final double fcupTrg = (double) fcupTrgGated / (double) fcupTrgUndated;

        // [67]/[68] = CLOCK
        final double clock = (double) clockGated / (double) clockUngated;

        // Compute the live times.
        final double[] liveTimes = new double[3];
        liveTimes[LiveTimeIndex.FCUP_TDC.ordinal()] = fcupTdc;
        liveTimes[LiveTimeIndex.FCUP_TRG.ordinal()] = fcupTrg;
        liveTimes[LiveTimeIndex.CLOCK.ordinal()] = clock;

        return liveTimes;
    }

    /**
     * Disallow class instantiation.
     */
    private ScalerUtilities() {
        throw new UnsupportedOperationException("Do not instantiate this class.");
    }
}

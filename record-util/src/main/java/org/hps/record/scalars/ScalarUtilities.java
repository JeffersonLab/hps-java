package org.hps.record.scalars;

/**
 * Utilities methods for scalar data.
 * <p>
 * Currently this is used only for computing live time measurements from standard scalar data.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class ScalarUtilities {

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
     * @param index The enum of the index type.
     * @see LiveTimeIndex
     */
    public static double getLiveTime(final ScalarData data, final LiveTimeIndex index) {
        return getLiveTimes(data)[index.ordinal()];
    }

    /**
     * Get the live time measurements from standard scalar data.
     * <p>
     * This is returned as a double array of size 3 containing:</br>
     *
     * <pre>
     * [0] = FCUP TDC measurement
     * [1] = FCUP TRG measurement
     * [2] = CLOCK measurement
     * </pre>
     *
     * This method assumes the standard scalar data structure as outlined in <a
     * href="https://jira.slac.stanford.edu/browse/HPSJAVA-470">HPSJAVA-470</a>.
     *
     * @param data The scalar data.
     * @return The live time measurements.
     */
    public static double[] getLiveTimes(final ScalarData data) {

        // [03] - gated faraday cup with "TDC" threshold
        final int word03 = data.getValue(3);

        // [19] - gated faraday cup with "TRG" threshold
        final int word19 = data.getValue(19);

        // [35] - ungated faraday cup with "TDC" threshold
        final int word35 = data.getValue(35);

        // [51] - ungated faraday cup with "TRG" threshold
        final int word51 = data.getValue(51);

        // [67] - gated clock
        final int word67 = data.getValue(67);

        // [68] - ungated clock
        final int word68 = data.getValue(68);

        // [03]/[35] = FCUP TDC
        final double fcupTdc = (double) word03 / (double) word35;

        // [19]/[51] = FCUP TRG
        final double fcupTrg = (double) word19 / (double) word51;

        // [67]/[68] = CLOCK
        final double clock = (double) word67 / (double) word68;

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
    private ScalarUtilities() {
        throw new UnsupportedOperationException("Do not instantiate this class.");
    }
}

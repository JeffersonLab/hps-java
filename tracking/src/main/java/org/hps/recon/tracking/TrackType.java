package org.hps.recon.tracking;

/**
 * Class containing utilities used to retrieve the track type value based on the
 * tracking strategy and whether it's a GBL track or not.
 *
 * @author <a href="mailto:moreno1@ucsc.edu">Omar Moreno</a>
 */
public final class TrackType {

    /**
     * Returns the track type for the given strategy. This assumes that the
     * track is not a GBL track.
     *
     * @param strategyType The StrategyType associated with the tracking
     * tracking strategy of interest.
     * @return The track type value for this StrategyType
     */
    public static int getType(StrategyType strategyType) {
        return TrackType.encodeType(strategyType, false);
    }

    /**
     * Returns the track type for a given strategy based of whether the track is
     * a GBL track or not.
     *
     * @param strategyType The StrategyType associated with the tracking
     * tracking strategy of interest.
     * @param isGblTrack Flag indicating whether the track is a GBL track
     * @return The track type value
     */
    public static int getType(StrategyType strategyType, boolean isGblTrack) {
        return TrackType.encodeType(strategyType, isGblTrack);
    }

    /**
     * Track type encoder. The strategy (S) and GBL flag (G) are packed as
     * follows:
     *
     * Note that Z denotes zero
     *
     * ZZZZZZZZZZZZZZZZZZZZZZZZZZGSSSSS
     *
     * The types are bit-packed, according to the {@link org.hps.recon.tracking.StrategyType} enum, so:
     * bit0 = MATCHED_TRACKS
     * bit1 = S345_C2_E16
     * bit2 = S456_C3_E21
     * bit3 = S123_C4_E56
     * bit4 = S123_C5_E46
     * bit5 = GBL Track.
     * @param strategyType The StrategyType associated with the tracking
     * strategy of interest.
     * @param isGblTrack Flag indicating whether the track is a GBL track
     * @return The enoded track type value
     */
    private static int encodeType(StrategyType strategyType, boolean isGblTrack) {

        int type = 1 << (strategyType.getType() - 1);
        if (isGblTrack) {
            type = (type ^ (1 << 5));
        }

        return type;
    }

    public static int addStrategy(int type, StrategyType strategyType) {
        return type | 1 << (strategyType.getType() - 1);
    }

    public static boolean isGBL(int type) {
        return (type & (1 << 5)) != 0;
    }

    public static int setGBL(int type, boolean isGblTrack) {
        if (isGblTrack != isGBL(type)) {
            return type ^ (1 << 5);
        } else {
            return type;
        }
    }

    /**
     * Constructor
     */
    private TrackType() {
    }
}

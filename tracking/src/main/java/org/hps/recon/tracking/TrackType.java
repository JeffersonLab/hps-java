package org.hps.recon.tracking;

/**
 * Class containing utilities used to retrieve the track type value
 * based on the tracking strategy and whether it's a GBL track or not.
 * 
 * @author <a href="mailto:moreno1@ucsc.edu">Omar Moreno</a>
 */
public final class TrackType {

    /**
     * Returns the track type for the given strategy.  This assumes that the
     * track is not a GBL track.
     * 
     * @param strategyType The StrategyType associated with the tracking 
     *                     tracking strategy of interest. 
     * @return The track type value for this StrategyType
     */
    public static int getType(StrategyType strategyType) { 
        return TrackType.encodeType(strategyType, false);
    }
    
    /**
     * Returns the track type for a given strategy based of whether the
     * track is a GBL track or not.
     *
     * @param strategyType The StrategyType associated with the tracking 
     *                     tracking strategy of interest.
     * @param isGblTrack Flag indicating whether the track is a GBL track
     * @return  The track type value
     */
    public static int getType(StrategyType strategyType, boolean isGblTrack) { 
        return TrackType.encodeType(strategyType, isGblTrack);
    }
   
    /**
     * Track type encoder.  
     * 
     * @param strategyType The StrategyType associated with the tracking 
     *                     tracking strategy of interest.
     * @param isGblTrack Flag indicating whether the track is a GBL track
     * @return The enoded track type value
     */
    private static int encodeType(StrategyType strategyType, boolean isGblTrack) { 
       
        int type = strategyType.getType();
        if (isGblTrack) type = (type ^ (1 << 7));
        
        return type; 
    }
    
    /** Constructor */
    private TrackType() {}
}

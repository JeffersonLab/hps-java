package org.hps.recon.tracking;

/**
 * Enum constants for different {@link Track}s based on what tracking
 * strategy was used.  The type is defined by comparing the tracking strategy
 * name to the name of all the enum constants.
 *  
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public enum StrategyType {
   
    // NOTE: The values of these enums should never be changed.  Any additional
    //       enum constants should be simply added to the end of the list.
    
    /** Enum values corresponding to different tracking strategies */
    /** 
     * This is the default TrackType value set by the reconstruction. It  
     * represents the following track finding algorithms for the first
     * two passes:
     * 
     * pass 1 - Seed 123, confirm 4, extend 56
     * pass 2 - Seed 345, confirm 2, extend 16
     * 
     * For pass 3? and beyond, this should no longer be needed since multiple 
     * versions of the same track will no longer exist.
     */
    MATCHED_TRACKS(1),
    /** Seed 345, confirm 2, extend 16 */
    S345_C2_E16(2),
    /** Seed 456, confirm 3, extend 21 */
    S456_C3_E21(3), 
    /** Seed 123, confirm 4, extend 56 */
    S123_C4_E56(4),
    /** Seed 123, confirm 5, extend 46 */
    S123_C5_E46(5);
    
    private int type; 
   
    /** Constructor */
    StrategyType(int type) { 
        this.type = type; 
    }
   
    /**
     * Returns the enum constant of this enum type
     * 
     * @return returns the enum constant of this enum type 
     */
    public int getType() { 
        return type; 
    }
    
    /**
     * Returns the {@link StrategyType} with the specified name.  If a 
     * {@link StrategyType} with that name doesn't exist, it returns null.
     * 
     * @param name The name of the StrategyType enum constant to return
     * @return The {@link StrategyType} enum constant of the given name or
     *         null if it doesn't exist. 
     */
    public static StrategyType getType(String name) {
        for (StrategyType strategyType : values()) {
            if (strategyType.name().equals(name)) return strategyType;
        }
        return null;
    }
 
    /**
     * Returns true if the specified {@link StrategyType} is equal to this enum
     * constant.
     * 
     * @param strategyType enum constant to check
     * @return true if the specified {@link StrategyType} is equal to this enum
     *         constant
     */
    public boolean equals(StrategyType strategyType) { 
        return strategyType.getType() == getType(); 
    }
}

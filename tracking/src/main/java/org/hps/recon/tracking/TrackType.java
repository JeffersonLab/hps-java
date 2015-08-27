package org.hps.recon.tracking;

/**
 * Enum constants for different {@link Track}s based on what tracking
 * strategy was used.
 *  
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public enum TrackType {
   
    // NOTE: The values of these enums should never be changed.  Any additional
    //       enum constants should be simply added to the end of the list.
    
    /** Enum values corresponding to different tracking strategies */
    S123_C4_E56(1000), // Seed 123, confirm 4, extend 56 
    S123_C5_E46(1001), // Seed 123, confirm 5, extend 46
    S345_C2_E16(1003), // Seed 345, confirm 2, extend 16
    S456_C3_E21(1004); // Seed 456, confirm 3, extend 21
    
    private int type; 
   
    /** Constructor */
    TrackType(int type) { 
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
     * Returns true if the specified {@link TrackType} is equal to this enum
     * constant.
     * 
     * @param trackType : enum constant to check
     * @return true if the specified {@link TrackType} is equal to this enum
     *         constant
     */
    public boolean equals(TrackType trackType) { 
        return trackType.getType() == getType(); 
    }
}

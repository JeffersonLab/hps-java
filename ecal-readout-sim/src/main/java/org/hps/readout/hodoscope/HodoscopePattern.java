package org.hps.readout.hodoscope;

public class HodoscopePattern {
    /**
     * Hodoscope pattern for each layer is an array with 9 elements. Among them,
     * 5 elements denote tilt hits, and the other 4 elements denote cluster hits.
     */    
    private boolean[] pattern = {false, false, false, false, false, false, false, false, false};    
    
    /**
     * Index of pattern
     */
    public static final int HODO_LX_1 = 0; 
    public static final int HODO_LX_2 = 2;
    public static final int HODO_LX_3 = 4;
    public static final int HODO_LX_4 = 6;
    public static final int HODO_LX_5 = 8;
    
    public static final int HODO_LX_CL_12 = 1;
    public static final int HODO_LX_CL_23 = 3;
    public static final int HODO_LX_CL_34 = 5;
    public static final int HODO_LX_CL_45 = 7;
        
    /**
     * @return returns hodoscope pattern
     */
    public boolean[] getPattern() {
        return pattern;
    }
    
    /**
     * Set status for a hit
     * @param parameter: index of a hit
     * @param returns status of a hit: true or false
     * 
     */
    public void setHitStatus(int index, boolean status) {
        if(index >= pattern.length) throw new IllegalArgumentException("Error: Invalid index");
        pattern[index] = status;        
    }
    
    @Override
    public String toString() {
        String str = "pattern: ";
        for(int i = 0; i < pattern.length; i++) str += pattern[i] + " ";
        str += "\n";        
        return str;
        
    }

}

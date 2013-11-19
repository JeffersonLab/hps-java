package org.lcsim.hps.conditions.ecal;

/**
 * This class is a simplistic representation of gain values from the ECAL
 * conditions database.     
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class EcalGain {
       
    /** The gain value. */
    private double gain;
    
    /**
     * Fully qualified class constructor.
     * @param id
     * @param gain
     */
    EcalGain(double gain) {
        this.gain = gain;
    }
    
    /**
     * Get the gain value.
     * @return The gain value.
     */
    public double getGain() {
        return gain;
    }       
}
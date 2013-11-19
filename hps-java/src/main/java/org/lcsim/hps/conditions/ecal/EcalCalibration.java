package org.lcsim.hps.conditions.ecal;

/**
 * This class is a simplistic representation of ECal pedestal and noise
 * values from the conditions database.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class EcalCalibration {
       
    /** The pedestal value. */
    private double pedestal;
    
    /** The noise value. */
    private double noise;
    
    /**
     * Fully qualified class constructor.
     * @param pedestal The pedestal value.
     * @param noise The noise value.
     */
    EcalCalibration(double pedestal, double noise) {
        this.pedestal = pedestal;
        this.noise = noise;
    }
        
    /**
     * Get the pedestal value.
     * @return The gain value.
     */
    public double getPedestal() {
        return pedestal;
    }       
    
    /**
     * Get the noise value.
     * @return The noise value.
     */
    public double getNoise() {
        return noise;
    }
}
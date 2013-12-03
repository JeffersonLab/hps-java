package org.hps.conditions.svt;

/**
 * This class represents a noise and pedestal measurement for an SVT channel.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class SvtCalibration {
    
    double noise = Double.NaN;
    double pedestal = Double.NaN;
    
    /**
     * Fully qualified constructor.
     * @param noise The noise value.
     * @param pedestal The pedestal value.
     */
    SvtCalibration(double noise, double pedestal) {
        this.noise = noise;
        this.pedestal = pedestal;
    }
        
    /**
     * Get the noise value.
     * @return The noise value.
     */
    public double getNoise() {
        return noise;
    }
    
    /**
     * Get the pedestal value.
     * @return The pedestal value.
     */
    public double getPedestal() {
        return pedestal;
    }
    
    /**
     * Convert this object to a human readable string.
     * @return This object converted to a string.
     */
    public String toString() {
        return "noise: " + noise + ", pedestal: " + pedestal;
    }
}

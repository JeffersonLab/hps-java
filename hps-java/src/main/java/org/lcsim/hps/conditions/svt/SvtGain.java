package org.lcsim.hps.conditions.svt;

/**
 * This class represents gain measurements for a single SVT channel.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtGain {

    double gain = Double.NaN;
    double offset = Double.NaN;
    
    /**
     * Full qualified class constructor.
     * @param gain The gain of the channel.
     * @param offset The gain's offset.
     */
    SvtGain(double gain, double offset) {
        this.gain = gain;
        this.offset = offset;
    }
    
    /**
     * Get the gain.
     * @return The gain value.
     */
    double getGain() {
        return gain;
    }
    
    /**
     * Get the offset.
     * @return The offset value.
     */
    double getOffset() {
        return offset;
    }
    
    /**
     * Convert this object to a human-readable string.
     * @return This object converted to a string.
     */
    public String toString() {
        //return "gain: " + gain + ", offset: " + offset;
        return "" + gain + '\t' + offset;
    }
    
}

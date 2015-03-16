package org.hps.monitoring.plotting;

/**
 * Simple interface for providing values to charts (e.g. strip charts) by series. 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public interface ValueProvider {        
    
    /**
     * Get an array of float values to fill in the data set series.
     * @return The array of data set values, ordered by ascending series number.
     */
    float[] getValues();
}
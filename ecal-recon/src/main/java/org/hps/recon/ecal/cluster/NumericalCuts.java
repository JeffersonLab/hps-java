package org.hps.recon.ecal.cluster;

import java.util.Map;

/**
 * <p>
 * This is an interface for accessing the numerical cut values 
 * of a clustering algorithm by index or name.
 * <p>
 * The default units are generally GeV for energy and nanoseconds 
 * for time, though this is not in any way modeled by this class.
 */
public interface NumericalCuts {

    /**
     * Set all the cut values from an array.
     * @param values The cut values.
     */
    void setValues(double[] values);
    
    /**
     * Get the cut values.
     * @return The cut values array.
     */
    double[] getValues();
    
    /**
     * Get a cut setting by name.
     * @param name The name of the cut.
     * @return The cut value.
     */
    double getValue(String name);
    
    /**
     * Get a cut value by index.
     * @param index The index of the cut.
     * @return The cut value from the index.
     */
    double getValue(int index);
         
    /**
     * Get the names of the cuts.
     * @return The names of the cuts.
     */
    String[] getNames();
    
    /**
     * Set the value of a cut by index.
     * @param index The index of the cut.
     * @param value The value of the cut.
     */
    void setValue(int index, double value);

    /**
     * Set a cut value by name.
     * @param name The name of the cut.
     * @param value The value of the cut.
     */
    void setValue(String name, double value);
    
    /**
     * True if using the default cuts.
     * @return True if using default cuts.
     */
    boolean isDefaultValues();
    
    /**
     * Get the default cuts.
     * @return The default cuts.
     */
    double[] getDefaultValues();
    
    /**
     * Set the cut values from a map of keys to values.
     * @param valueMap The cut values as a map.
     */
    void setValues(Map<String, Double> valueMap);            
}

package org.hps.record.triggerbank;

import java.util.logging.Logger;

/**
 * Class <code>VTPCalibrationTrigger</code> Parse HPS Calibration Trigger of VTP and store information. 
 * HPS Calibration Trigger:
 * <ul>
 * <li>10bit [0 : 9] trigger time in 4 ns unit</li> 
 * <li>4bit [19: 22] calibration trigger type: cosmic (0), LED (1), hodoscope (2), pulser (3)</li> 
 * </ul>
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */

public class VTPCalibrationTrigger {
    // calibration trigger's variables.
    private int t; // in 4 ns units
    private int type;
    
    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(VTPCalibrationTrigger.class.getPackage().getName());

    /**
     * Creates a new <code>VTPCalibrationTrigger</code> object.
     * 
     * @param word - HPS Calibration Trigger in the VTP bank.
     */
    
    public VTPCalibrationTrigger(int word) {
        decodeData(word);   
    }
    
    public final void decodeData(int word){
        t = word & 0x3FF;
        type = word >> 19 & 0x0F;
        
        // Make sure that the input values are valid.
        if (t < 0) {
            LOGGER.warning("Received calibration trigger with negative time.");
        }
    }
    
    /**
     * Get calibration trigger time in ns referenced from the beginning of the readout window.
     * 
     * @return Return the calibration trigger time as a <code>long</code>.
     */
    public long getTime() {
        return (long)t * 4;
    }
    
    
    /**
     * Get type of calibration trigger.
     * 
     * @return Return type of calibration trigger: cosmic (0), LED (1), hodoscope (2), pulser (3).
    */
    public int getType() {
        return type;
    }   
}

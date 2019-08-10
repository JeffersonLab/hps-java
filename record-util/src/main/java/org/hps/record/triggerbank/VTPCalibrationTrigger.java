package org.hps.record.triggerbank;

import java.util.BitSet;
import java.util.logging.Logger;

/**
 * Class <code>VTPCalibrationTrigger</code> Parse HPS Calibration Trigger of VTP and store information. 
 * HPS Calibration Trigger:
 * <ul>
 * <li>10bit [0 : 9] trigger time in 4 ns unit</li> 
 * <li>4bit [19: 22] calibration trigger type bits: cosmic (bit19), LED (bit20), hodoscope (bit21), pulser (bit22)</li> 
 * </ul>
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */

public class VTPCalibrationTrigger {
    // calibration trigger's variables.
    private int t; // in 4 ns units
    private BitSet typeBits = new BitSet(4);
    
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
        typeBits = BitSet.valueOf(new long [] {word >> 19 & 0x0F});
        
        // Make sure that the input values are valid.
        if (t < 0) {
            LOGGER.warning("Received calibration trigger with negative time.");
        }
    }
    
    /**
     * Get calibration trigger time in ns.
     * 
     * @return Return the calibration trigger time as an <code>int</code>.
     */
    public long getTime() {
        return (long)t * 4;
    }
    
    
    /**
     * Get calibration trigger type bits.
     * 
     * @return Returns calibration trigger type bits.
    */
    public BitSet getTypeBits() {
        return typeBits;
    }
   
    /**
     * Indicates whether a cosmic trigger was registered.
     */
    public boolean isCosmicTrigger() {
        return typeBits.get(0); 
    }
   
    /**
     * Indicates whether a LED trigger was registered.
     */
    public boolean isLEDTrigger() {
        return typeBits.get(1); 
    }
   
    /**
     * Indicates whether a hodoscope trigger was registered.
     */
    public boolean isHodoscopeTrigger() {
        return typeBits.get(2); 
    }

    /**
     * Indicates whether a pulser trigger was registered.
     */
    public boolean isPulserTrigger() {
        return typeBits.get(3); 
    }
}

package org.hps.record.triggerbank;

import java.util.BitSet;
import java.util.logging.Logger;

/**
 * Class <code>VTPPairsTrigger</code> Parse HPS Pairs Trigger of VTP and store information. 
 * HPS Pairs Trigger:
 * <ul>
 * <li>10bit [0 : 9] trigger time in 4 ns unit</li> 
 * <li>4bit [10 : 13] pass bits</li>
 * <li>3bit [19 : 22] pair cluster trigger bit instance: 0 to 3</li> 
 * </ul>
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */

public class VTPPairsTrigger {
    // pairs trigger's variables.
    private int t; // in 4 ns units
    private BitSet passBits = new BitSet(4);
    private int inst;
    
    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(VTPPairsTrigger.class.getPackage().getName());

    /**
     * Creates a new <code>VTPPairsTrigger</code> object.
     * 
     * @param word - HPS Pairs Trigger in the VTP bank.
     */
    
    public VTPPairsTrigger(int word) {
        decodeData(word);   
    }
    
    public final void decodeData(int word){
        t = word & 0x3FF;
        passBits = BitSet.valueOf(new long [] {word >> 10 & 0x0F});
        inst = word >> 20 & 0x07;
        
        // Make sure that the input values are valid.
        if (inst > 3) {
            LOGGER.warning("Received out-of-bounds pairs trigger instance.");
        }
        if (t < 0) {
            LOGGER.warning("Received pairs trigger with negative time.");
        }
    }
    
    /**
     * Get pairs trigger time in ns referenced from the beginning of the readout window.
     * 
     * @return Return the pairs trigger time as a <code>long</code>.
     */
    public long getTime() {
        return (long)t * 4;
    }
    
    /**
     * Get pairs trigger instance.
     * 
     * @return Return pairs trigger instance as an <code>int</code>.
     */
    public int getTriggerInstance() {
        return inst;
    }
    
    /**
     * Get pairs trigger pass bits.
     * 
     * @return Returns pairs trigger pass bits.
    */
    public BitSet getPassBits() {
        return passBits;
    }
    
    /**
     * Check if a cut was passed.
     * 
     * @param bitIndex - index of a pass bit.
     * 
     * @return Returns <code>true</code> if the corresponding cut was passed, and
     * <code>false</code> otherwise.
     */
    public boolean checkPass(int bitIndex) {
        if(bitIndex > 3) throw new RuntimeException("Index " + bitIndex + " is out of range (0 : 3)");
        else return passBits.get(bitIndex);
    }
    
    /**
     * Indicate if pass energy sum cut.
     */
    public boolean passESum() {
        return passBits.get(0);
    }
    
    /**
     * Indicate if pass energy difference cut.
     */
    public boolean passEDiff() {
        return passBits.get(1);
    }
    
    /**
     * Indicate if pass energy slope cut.
     */
    public boolean passESlope() {
        return passBits.get(2);
    }

    /**
     * Indicate if pass coplanarity cut.
     */
    public boolean passCoplanarity() {
        return passBits.get(3);
    }    
}

package org.hps.record.triggerbank;

import java.util.BitSet;
import java.util.logging.Logger;

/**
 * Class <code>VTPSinglesTrigger</code> Parse HPS Singles Trigger of VTP and store information. 
 * HPS Singles Trigger:
 * <ul>
 * <li>10bit [0 : 9] trigger time in 4 ns unit</li> 
 * <li>9bit [10 : 18] pass bits</li>
 * <li>1bit [19 : 19] TOP or BOT</li>
 * <li>3bit [20 : 22] single cluster trigger bit instance: 0 to 3</li> 
 * </ul>
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */

public class VTPSinglesTrigger {   
    // singles trigger's variables.
    private int t; // in 4 ns units
    private BitSet passBits = new BitSet(9);
    private int tOrB; // 1 means top; 0 means bot 
    private int inst;
    
    
    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(VTPSinglesTrigger.class.getPackage().getName());

    /**
     * Creates a new <code>VTPSinglesTrigger</code> object.
     * 
     * @param word - HPS Singles Trigger in the VTP bank.
     */
    
    public VTPSinglesTrigger(int word) {
        decodeData(word);   
    }
    
    public final void decodeData(int word){
        t = word & 0x3FF;
        passBits = BitSet.valueOf(new long [] {word >> 10 & 0x01FF});
        tOrB = word >> 19 & 0x01;
        inst = word >> 20 & 0x07;
        
        // Make sure that the input values are valid.
        if (inst > 3) {
            LOGGER.warning("Received out-of-bounds singles trigger instance.");
        }
        if (t < 0) {
            LOGGER.warning("Received singles trigger with negative time.");
        }
    }
    
    /**
     * Get singles trigger time in ns referenced from the beginning of the readout window.
     * 
     * @return Return the singles trigger time as a <code>long</code>.
     */
    public long getTime() {
        return (long)t * 4;
    }
    
    /**
     * Get singles trigger instance.
     * 
     * @return Return singles trigger instance as an <code>int</code>.
     */
    public int getTriggerInstance() {
        return inst;
    }
    
    /**
     * Get singles trigger pass bits.
     * 
     * @return Returns singles trigger pass bits.
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
        if(bitIndex > 8) throw new RuntimeException("Index " + bitIndex + " is out of range (0 : 8)");
        else return passBits.get(bitIndex);
    }
    
    /**
     * Indicate if pass emin.
     */
    public boolean passEMin() {
        return passBits.get(0);
    }
    
    /**
     * Indicate if pass emax.
     */
    public boolean passEMax() {
        return passBits.get(1);
    }
    
    /**
     * Indicate if pass minimum of Ecal hit count.
     */
    public boolean passNMin() {
        return passBits.get(2);
    }

    /**
     * Indicate if pass X minimum of the positron side.
     */
    public boolean passXMin() {
        return passBits.get(3);
    }
    
    /**
     * Indicate if pass position dependent energy threshhold.
     */
    public boolean passPDET() {
        return passBits.get(4);
    }
    
    /**
     * Indicate if pass hodoscope layer 1 coincidence.
     */
    public boolean passHodo1() {
        return passBits.get(5);
    }
    
    /**
     * Indicate if pass hodoscope layer 2 coincidence.
     */
    public boolean passHodo2() {
        return passBits.get(6);
    }
    
    /**
     * Indicate if pass hodoscope layer 1 & 2 geometry match
     */
    public boolean passHodoGeo() {
        return passBits.get(7);
    }
    
    /**
     * Indicate if pass hodoscope and ECal geometry match
     */
    public boolean passHodoECal() {
        return passBits.get(8);
    }
    
    /**
     * Get value of TOP_NBOT to indicate trigger occurred at TOP or bottom ECal Half.
     * 
     * @return Return value of TOP_NBOT. 1 means occurred at top, 0 means occurred at bottom.
     */
    public int getTopNBot() {
        return tOrB;
    }       
}

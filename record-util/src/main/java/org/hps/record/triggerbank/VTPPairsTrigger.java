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
 */
public class VTPPairsTrigger {
    // pairs trigger's variables.
    private int t; // in 4 ns units
    // Bits [4 : 9] are only for pair3 trigger; Only pair3 trigger requires hodoscope and Ecal geometry matching
    private BitSet passBits = new BitSet(10);
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
        passBits = BitSet.valueOf(new long [] {word >> 10 & 0x03FF});
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
        if(bitIndex > 9) throw new RuntimeException("Index " + bitIndex + " is out of range (0 : 9)");
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
    
    /**
     * Indicate if pass both L1 and L2 coincidence for top
     * @return
     */
    public boolean passL1L2CoincidenceTop() {
        return passBits.get(4);
    }
    
    /**
     * Indicate if pass L1L2 geometry matching for top
     * @return
     */
    public boolean passHodoL1L2MatchingTop() {
        return passBits.get(5);
    }
    
    /**
     * Indicate if pass Hodo and Ecal geometry matching for top
     * @return
     */
    public boolean passHodoEcalMatchingTop() {
        return passBits.get(6);
    }
    
    /**
     * Indicate if pass both L1 and L2 coincidence for bot
     * @return
     */
    public boolean passL1L2CoincidenceBot() {
        return passBits.get(7);
    }
    
    /**
     * Indicate if pass L1L2 geometry matching for bot
     * @return
     */
    public boolean passHodoL1L2MatchingBot() {
        return passBits.get(8);
    }
    
    /**
     * Indicate if pass Hodo and Ecal geometry matching for bot
     * @return
     */
    public boolean passHodoEcalMatchingBot() {
        return passBits.get(9);
    }
}

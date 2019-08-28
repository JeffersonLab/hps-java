package org.hps.record.triggerbank;

import java.util.logging.Logger;

/**
 * Class <code>VTPMultiplicityTrigger</code> Parse HPS Cluster Multiplicity Trigger of VTP and store information. 
 * HPS Cluster Multiplicity Trigger:
 * <ul>
 * <li>10bit [0 : 9] trigger time in 4 ns unit</li> 
 * <li>4bit [10 : 13] top ECal cluster multiplicity </li>
 * <li>4bit [14 : 17] bottom ECal cluster multiplicity </li>
 * <li>4bit [18 : 21] total ECal cluster multiplicity </li>
 * <li>1bit [22 : 22] cluster multiplicity trigger bit instance: 0 or 1</li>
 * </ul>
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */

public class VTPMultiplicityTrigger {    
    // Multiplicity trigger's variables.
    private int t; // in 4 ns units
    private int multTop;
    private int multBot;
    private int multTot;
    private int bitInst;

    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(VTPMultiplicityTrigger.class.getPackage().getName());

    /**
     * Creates a new <code>VTPMultiplicityTrigger</code> object.
     * 
     * @param word - HPS Multiplicity Trigger in the VTP bank.
     */

    public VTPMultiplicityTrigger(int word) {
        decodeData(word);   
    }

    public final void decodeData(int word){
        t = word & 0x3FF;
        multTop = word >> 10 & 0x0F;
        multBot = word >> 14 & 0x0F;
        multTot = word >> 18 & 0x0F;
        bitInst = word >> 22 & 0x01;

        // Make sure that the input values are valid.
        if (t < 0) {
            LOGGER.warning("Received cluster multiplicity trigger with negative time.");
        }
    }

    /**
     * Get cluster multiplicity trigger time in ns referenced from the beginning of the readout window.
     * 
     * @return Return the cluster multiplicity trigger time as a <code>long</code>.
     */
    public long getTime() {
        return (long)t * 4;
    }

    /**
     * Get top ECal cluster multiplicity.
     * 
     * @return Return top ECal cluster multiplicity as an <code>int</code>.
     */
    public int getTopClusterMultiplicity() {
        return multTop;
    }

    /**
     * Get bottom ECal cluster multiplicity.
     * 
     * @return Return bottom ECal cluster multiplicity as an <code>int</code>.
     */
    public int getBottomClusterMultiplicity() {
        return multBot;
    }

    /**
     * Get total ECal cluster multiplicity.
     * 
     * @return Return total ECal cluster multiplicity as an <code>int</code>.
     */
    public int getTotalClusterMultiplicity() {
        return multTot;
    }

    /**
     * Get cluster multiplicity trigger bit instance.
     * 
     * @return Return cluster multiplicity trigger bit instance.
     */
    public int getBitInstance() {
        return bitInst;
    }
}

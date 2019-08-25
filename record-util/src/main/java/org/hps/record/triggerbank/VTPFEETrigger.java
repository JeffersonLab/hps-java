package org.hps.record.triggerbank;

import java.util.BitSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class <code>VTPFEETrigger</code> Parse HPS FEE Trigger of VTP and store information. 
 * HPS FEE Trigger:
 * <ul>
 * <li>10bit [0 : 9] trigger time in 4 ns unit</li> 
 * <li>7bit [10 : 16] bit mask specify which FEE regions (ECal x-ranges) triggered the system</li>
 * </ul>
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */

public class VTPFEETrigger {
    // FEE trigger's variables.
    private int t; // in 4 ns units
    private BitSet regionBits = new BitSet(7);


    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(VTPFEETrigger.class.getPackage().getName());

    /**
     * Creates a new <code>VTPFEETrigger</code> object.
     * 
     * @param word - HPS FEE Trigger in the VTP bank.
     */

    public VTPFEETrigger(int word) {
        decodeData(word);   
    }

    public final void decodeData(int word){
        t = word & 0x3FF;
        regionBits = BitSet.valueOf(new long [] {word >> 10 & 0x07F});

        // Make sure that the input values are valid.
        if (t < 0) {
            LOGGER.warning("Received FEE trigger with negative time.");
        }
    }

    /**
     * Get FEE trigger time in ns referenced from the beginning of the readout window.
     * 
     * @return Return the FEE trigger time as a <code>long</code>.
     */
    public long getTime() {
        return (long)t * 4;
    }

    /**
     * Get singles trigger pass bits.
     * 
     * @return Returns singles trigger pass bits.
     */
    public BitSet getRegionBits() {
        return regionBits;
    }

    /**
     * Check if a region triggered system.
     * 
     * @param bitIndex - index of a region bit.
     * 
     * @return Returns <code>true</code> if the corresponding region triggered system, and
     * <code>false</code> otherwise.
     */
    public boolean checkRegion(int bitIndex) {
        if(bitIndex > 6) throw new RuntimeException("Index " + bitIndex + " is out of range (0 : 6)");
        else return regionBits.get(bitIndex);
    }

    /**
     * List indices of regions triggered system.
     */
    public String listIndicesofRegions() {
        return regionBits.toString();
    }

    /**
     * Save indices of regions, which triggered the system, into an integer array.
     */
    public int[] getIndicesOfRegions() {
        String str = regionBits.toString();
        String[] items = str.replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("\\s", "").split(",");
        int[] arr = new int[items.length];
        for (int i = 0; i < items.length; i++) {
            try {
                arr[i] = Integer.parseInt(items[i]);
            } catch (NumberFormatException e) {
                LOGGER.log(Level.SEVERE, "parsing a string as a signed integer is failed", e);
            };
        }
        return arr;
    }

    /**
     * Get number of regions which triggered system.
     */
    public int getNumberOfRegions() {
        return this.getIndicesOfRegions().length;
    }

    /**
     * Indicate if region 0 triggered the system.
     */
    public boolean isRegion0() {
        return regionBits.get(0);
    }

    /**
     * Indicate if region 1 triggered the system.
     */
    public boolean isRegion1() {
        return regionBits.get(1);
    }

    /**
     * Indicate if region 2 triggered the system.
     */
    public boolean isRegion2() {
        return regionBits.get(2);
    }

    /**
     * Indicate if region 3 triggered the system.
     */
    public boolean isRegion3() {
        return regionBits.get(3);
    }

    /**
     * Indicate if region 4 triggered the system.
     */
    public boolean isRegion4() {
        return regionBits.get(4);
    }

    /**
     * Indicate if region 5 triggered the system.
     */
    public boolean isRegion5() {
        return regionBits.get(5);
    }

    /**
     * Indicate if region 6 triggered the system.
     */
    public boolean isRegion6() {
        return regionBits.get(6);
    }      
}

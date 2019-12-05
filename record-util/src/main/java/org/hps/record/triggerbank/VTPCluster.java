package org.hps.record.triggerbank;

import java.util.logging.Logger;

/**
 * Class <code>VTPCluster</code> Parse HPS Cluster of VTP and store information. 
 * HPS Cluster Trigger:
 * <ul>
 * <li>6bit signed cluster X coordinate in the first word; Range: [-22, 0] and [1, 23]</li>
 * <li>4bit signed Y coordinate in the first word; Range: [-5, -1] and [1, 5]</li>
 * <li>13bit cluster energy in MeV in the first word</li>
 * <li>10bit cluster time in 4 ns unit in the second word</li> 
 * <li>4bit number of hits in the second word</li>
 * </ul>
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */

public class VTPCluster {
    // cluster's variables.
    private int ix;
    private int iy;
    private double e; // GeV
    private int t; // in 4 ns units
    private int n;

    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(VTPCluster.class.getPackage().getName());

    /**
     * Creates a new <code>VTPCluster</code> object.
     * 
     * @param word0 - The first word for HPS cluster in the VTP bank.
     * @param word1 - The second word for HPS cluster in the VTP bank.
     */
    
    public VTPCluster(int word0, int word1) {
        decodeData(word0, word1);   
    }
    
    /**
     * Parses words of HPS Cluster
     */
    public final void decodeData(int word0, int word1){
        ix = word0 & 0x03F;
        // If the first bit of the index is 1, then it is a negative number 
        if((ix >> 5 & 0x1) == 0x1) ix = -((ix ^ 0x3F) + 1);
        iy = word0 >> 6 & 0x0F;
        // If the first bit of the index is 1, then it is a negative number 
        if((iy >> 3 & 0x1) == 0x1) iy = -((iy ^ 0xF) + 1);
        e = (double)(word0 >> 10 & 0x01FFF);
        t = word1 & 0x03FF;
        n = word1 >> 10 & 0x0F;    
        
        // Make sure that the input values are valid.
        if (ix < -22 || ix > 23) {
            LOGGER.warning(String.format("Received out-of-bounds ix value of %d.", ix));
        }
        if (iy == 0 || iy < -5 || iy > 5) {
            LOGGER.warning(String.format("Received out-of-bounds iy value of %d.", iy));
        }
        if (e < 0) {
            LOGGER.warning("Received negative energy for cluster.");
        }
        if (n <= 0) {
            LOGGER.warning("Received cluster with zero or fewer hits.");
        }
        if (t < 0) {
            LOGGER.warning("Received cluster with negative time.");
        }
    }
    
    /**
     * Get the x-coordinate of the cluster.
     * 
     * @return Return the cluster x-coordinate as an <code>int</code>.
     */
    public int getXIndex() {
        return ix;
    }

    /**
     * Get the y-coordinate of the cluster.
     * 
     * @return Return the cluster y-coordinate as an <code>int</code>.
     */
    public int getYIndex() {
        return iy;
    }

    /**
     * Gets the number of hits in the cluster.
     * 
     * @return Returns the cluster hit count as an <code>int</code>.
     */
    public int getHitCount() {
        return n;
    }

    /**
     * Gets the cluster time in ns.
     * 
     * @return Returns the cluster time as an <code>int</code>.
     */
    public long getTime() {
        return (long)t * 4;
    }

    /**
     * Gets the energy of the cluster in GeV.
     * 
     * @return Returns the cluster energy as a <code>double</code>.
     */
    public double getEnergy() {
        return e/1000.;
    }
}

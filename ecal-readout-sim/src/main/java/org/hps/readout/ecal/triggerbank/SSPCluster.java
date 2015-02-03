package org.hps.readout.ecal.triggerbank;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.util.log.BasicFormatter;
import org.lcsim.util.log.LogUtil;

/**
 * Class <code>SSPCluster</code> stores all of the information on 
 * clusters that is reported by the SSP.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public final class SSPCluster {
    // Cluster definition variables.
    private final int ix;
    private final int iy;
    private final int n;
    private final int t;
    private final double e;
    
    // Output potential errors or messages.
    private static Logger logger = LogUtil.create(SSPCluster.class, new BasicFormatter(SSPCluster.class.getSimpleName()));
    static {
        logger.setLevel(Level.WARNING);
    }
    
    /**
     * Creates a new <code>SSPCluster</code> object.
     * @param ix - The x-index of the cluster.
     * @param iy - The y-index of the cluster.
     * @param energy - The cluster energy in MeV.
     * @param hits - The cluster hit count.
     * @param time - The time at which the cluster occurred in ns.
     */
    public SSPCluster(int ix, int iy, int energy, int hits, int time) {
        // Make sure that the input values are valid.
        if(ix == 0 || ix < -23 || ix > 23) {
            logger.warning(String.format("Received out-of-bounds ix value of %d.", ix));
        } if(iy == 0 || iy < -5 || iy > 5) {
            logger.warning(String.format("Received out-of-bounds iy value of %d.", iy));
        } if(energy < 0) {
            logger.warning("Received negative energy for cluster.");
        } if(hits <= 0) {
            logger.warning("Received cluster with zero or fewer hits.");
        } if(time < 0) {
            logger.warning("Received cluster with negative time.");
        }
        
        // Define the cluster parameters.
        this.ix = ix;
        this.iy = iy;
        this.e = energy / 1000.0;
        this.t = time;
        this.n = hits;
        
        // Indicate that the cluster was made.
        logger.fine(String.format("Constructed cluster at (%3d, %3d) at time %3d ns with energy %4d MeV and %d hits.",
                ix, iy, time, energy, hits));
    }
    
    /**
     * Gets the x-index of the cluster.
     * @return Returns the cluster x-index as an <code>int</code>.
     */
    public int getXIndex() { return ix; }
    
    /**
     * Gets the y-index of the cluster.
     * @return Returns the cluster y-index as an <code>int</code>.
     */
    public int getYIndex() { return iy; }
    
    /**
     * Gets the number of hits in the cluster.
     * @return Returns the cluster hit count as an <code>int</code>.
     */
    public int getHitCount() { return n; }
    
    /**
     * Gets the cluster time in nanoseconds.
     * @return Returns the cluster time as an <code>int</code>.
     */
    public int getTime() { return t; }
    
    /**
     * Gets the energy of the cluster in GeV.
     * @return Returns the cluster energy as a <code>double</code>.
     */
    public double getEnergy() { return e; }
}
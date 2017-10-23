package org.hps.record.triggerbank;

import java.util.logging.Logger;

import org.hps.record.scalers.ScalersEvioProcessor;

/**
 * Class <code>SSPCluster</code> stores all of the information on clusters that is reported by the SSP. SSP clusters store:
 * <ul>
 * <li>Cluster center x-index</li>
 * <li>Cluster center y-index</li>
 * <li>Cluster total energy</li>
 * <li>Cluster hit count</li>
 * <li>Cluster time</li>
 * </ul>
 * <code>SSPCluster</code> does not support the ability to track individual hits that are part of a cluster.
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

    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ScalersEvioProcessor.class.getPackage().getName());

    /**
     * Creates a new <code>SSPCluster</code> object.
     * 
     * @param ix - The x-index of the cluster.
     * @param iy - The y-index of the cluster.
     * @param energy - The cluster energy in MeV.
     * @param hits - The cluster hit count.
     * @param time - The time at which the cluster occurred in ns.
     */
    public SSPCluster(int ix, int iy, int energy, int hits, int time) {
        // Make sure that the input values are valid.
        if (ix == 0 || ix < -23 || ix > 23) {
            LOGGER.warning(String.format("Received out-of-bounds ix value of %d.", ix));
        }
        if (iy == 0 || iy < -5 || iy > 5) {
            LOGGER.warning(String.format("Received out-of-bounds iy value of %d.", iy));
        }
        if (energy < 0) {
            LOGGER.warning("Received negative energy for cluster.");
        }
        if (hits <= 0) {
            LOGGER.warning("Received cluster with zero or fewer hits.");
        }
        if (time < 0) {
            LOGGER.warning("Received cluster with negative time.");
        }

        // Define the cluster parameters.
        this.ix = ix;
        this.iy = iy;
        this.e = energy / 1000.0;
        this.t = time;
        this.n = hits;

        // Indicate that the cluster was made.
        LOGGER.fine(String.format("Constructed cluster at (%3d, %3d) at time %3d ns with energy %4d MeV and %d hits.",
                ix, iy, time, energy, hits));
    }

    /**
     * Gets the x-index of the cluster.
     * 
     * @return Returns the cluster x-index as an <code>int</code>.
     */
    public int getXIndex() {
        return ix;
    }

    /**
     * Gets the y-index of the cluster.
     * 
     * @return Returns the cluster y-index as an <code>int</code>.
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
     * Gets the cluster time in nanoseconds.
     * 
     * @return Returns the cluster time as an <code>int</code>.
     */
    public int getTime() {
        return t;
    }

    /**
     * Gets the energy of the cluster in GeV.
     * 
     * @return Returns the cluster energy as a <code>double</code>.
     */
    public double getEnergy() {
        return e;
    }
}
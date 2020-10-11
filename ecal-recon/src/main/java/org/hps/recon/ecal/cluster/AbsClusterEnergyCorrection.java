package org.hps.recon.ecal.cluster;

import java.util.Random;
import org.lcsim.event.Cluster;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.geometry.subdetector.HPSEcal3;

/**
 *
 * @author baltzell
 */
abstract class AbsClusterEnergyCorrection {
   
    // Variables derived as the difference between data and mc noise in
    // ecal cluster energy resolution.
    static final double NOISE_A = -0.00000981;
    static final double NOISE_B = 0.00013725;
    static final double NOISE_C = 0.000301;

    static final double CUTOFF_OFFSET = 35.0;

    static final Random random = new Random();
    
    // Calculate the noise factor to smear the Ecal energy by
    public static double calcNoise(double energy) {
        return random.nextGaussian() *
                Math.sqrt(NOISE_A + NOISE_B * energy + NOISE_C * Math.pow(energy, 2));
    }
    
    /**
     * Calculate the corrected energy for the cluster.
     * 
     * @param cluster The input cluster.
     * @return The corrected energy.
     */
    public static final double calculateCorrectedEnergy(HPSEcal3 ecal, Cluster cluster, boolean isMC) {
        double rawE = cluster.getEnergy();
        return computeCorrectedEnergy(ecal, cluster.getParticleId(), rawE, cluster.getPosition()[0],
                cluster.getPosition()[1], isMC);
    }

    /**
     * Calculate the corrected energy for the cluster using track position at ecal.
     * 
     * @param cluster The input cluster.
     * @return The corrected energy.
     */
    public static final double calculateCorrectedEnergy(HPSEcal3 ecal, Cluster cluster, double ypos, boolean isMC) {
        double rawE = cluster.getEnergy();
        return computeCorrectedEnergy(ecal, cluster.getParticleId(), rawE, cluster.getPosition()[0], ypos, isMC);
    }

    /**
     * Calculate the corrected energy and set on the cluster.
     * 
     * @param cluster The input cluster.
     */
    public static final void setCorrectedEnergy(HPSEcal3 ecal, BaseCluster cluster, boolean isMC) {
        double correctedEnergy = calculateCorrectedEnergy(ecal, cluster, isMC);
        if (isMC) {
            correctedEnergy += calcNoise(correctedEnergy);
        }
        cluster.setEnergy(correctedEnergy);
    }

    /**
     * Calculate the corrected energy and set on the cluster.
     * 
     * @param cluster The input cluster.
     */

    public static final void setCorrectedEnergy(HPSEcal3 ecal, BaseCluster cluster, double ypos, boolean isMC) {
        double correctedEnergy = calculateCorrectedEnergy(ecal, cluster, ypos, isMC);
        if (isMC) {
            correctedEnergy += calcNoise(correctedEnergy);
        }
        cluster.setEnergy(correctedEnergy);
    }
    

    public static double computeCorrectedEnergy(HPSEcal3 ecal, int pid, double rawE, double x, double y, boolean isMC) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}

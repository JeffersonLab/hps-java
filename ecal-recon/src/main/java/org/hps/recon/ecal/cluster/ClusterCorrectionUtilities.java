package org.hps.recon.ecal.cluster;

import hep.physics.vec.Hep3Vector;
import java.util.List;
import java.util.Random;
import org.hps.detector.ecal.EcalCrystal;
import org.hps.detector.ecal.HPSEcalDetectorElement;
import org.jdom.DataConversionException;
import org.lcsim.event.Cluster;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.geometry.subdetector.HPSEcal3;

/**
 *
 * @author baltzell
 */
public final class ClusterCorrectionUtilities {

    static final double CUTOFF_OFFSET = 35.0;

    // Variables derived as the difference between data and mc noise in
    // ecal cluster energy resolution.
    static final double NOISE_A = -9.81E-6;
    static final double NOISE_B = 1.3725E-4;
    static final double NOISE_C = 3.01E-4;

    static final int N_ITERATIONS_2019 = 5;
    
    static final Random random = new Random();

    // Calculate the noise factor to smear the Ecal energy by
    public static double calcNoise(double energy) {
        return random.nextGaussian() * Math.sqrt(NOISE_A + NOISE_B * energy + NOISE_C * Math.pow(energy, 2));
    }

    /**
     * Apply HPS-specific energy and position corrections to a list of clusters
     * in place.
     *
     * @param clusters The list of clusters.
     */
    public static void applyCorrections(double beamEnergy, HPSEcal3 ecal, List<Cluster> clusters, boolean isMC) {
        // Loop over the clusters.
        for (Cluster cluster : clusters) {
            if (cluster instanceof BaseCluster) {
                BaseCluster baseCluster = (BaseCluster) cluster;
                // Apply PID based position correction, which should happen before final energy correction.
                ClusterPositionCorrection.setCorrectedPosition(baseCluster);
                // Apply PID based energy correction:
                if (beamEnergy > 4.0) {
                    ClusterEnergyCorrection2019.setCorrectedEnergy(ecal, baseCluster, isMC);
                } else {
                    ClusterEnergyCorrection.setCorrectedEnergy(ecal, baseCluster, isMC);
                }
            }
        }
    }

    /**
     * Apply HPS-specific energy and position corrections to a cluster without
     * track information. In this case, cluster is the non-corrected cluster - neither the energy correction or the position correction was applied
     *
     * @param cluster The input cluster.
     */
    public static void applyCorrections(double beamEnergy, HPSEcal3 ecal, Cluster cluster, boolean isMC) {
        if (cluster instanceof BaseCluster) {
            BaseCluster baseCluster = (BaseCluster) cluster;
       
            // Apply PID based energy correction.
            if (beamEnergy > 4.0) {
                //here we need to play a little bit. 
                //ClusterEnergyCorrection2019 requires the CORRECT position and the RAW energy
                //ClusterPositionCorrection2019 requires the CORRECT energy and the RAW position.
                //Idea: iterate from the non-corrected values     
                for (int it=0;it<N_ITERATIONS_2019;it++) {
                    ClusterEnergyCorrection2019.setCorrectedEnergy(ecal, baseCluster, isMC);
                    ClusterPositionCorrection2019.setCorrectedPosition(baseCluster);
                    
                    //absolute POS, relative in energy
                }             
            
            } else {
                // Apply PID based position correction, which should happen before final energy correction.
                ClusterPositionCorrection.setCorrectedPosition(baseCluster);
                ClusterEnergyCorrection.setCorrectedEnergy(ecal, baseCluster, isMC);
            }
        }
    }

    /**
     * Apply HPS-specific energy and position corrections to a cluster with
     * track information.
     * In this case, ypos is the y position of the cluster, determined from clustering
     * @param cluster The input cluster.
     */
    public static void applyCorrections(double beamEnergy, HPSEcal3 ecal, Cluster cluster, double ypos, boolean isMC) {
        if (cluster instanceof BaseCluster) {
            BaseCluster baseCluster = (BaseCluster) cluster;
          
              if (beamEnergy > 4.0) {
                //Apply energy correction - this depends on the non-corrected energy (from baseCluster) and ypos, from tracking.
                ClusterEnergyCorrection2019.setCorrectedEnergy(ecal, baseCluster, ypos, isMC);
                //Now the energy is correct, can use the 2019 correction. Note that the order is different from below, since 2019 corrections are based on the CORRECTED ENERGY
                ClusterPositionCorrection2019.setCorrectedPosition(baseCluster);
            } else {
                // Apply PID based position correction, which should happen before final energy correction.
                ClusterPositionCorrection.setCorrectedPosition(baseCluster);
                // Apply PID based energy correction.
                ClusterEnergyCorrection.setCorrectedEnergy(ecal, baseCluster, ypos, isMC);
            }
        }
    }

    public static final double computeYDistanceFromEdge(HPSEcal3 ecal, double xpos, double ypos) {
        // distance to beam gap edge
        double ydist;
        // Get these values from the Ecal geometry:
        HPSEcalDetectorElement detElement = (HPSEcalDetectorElement) ecal.getDetectorElement();
        double BEAMGAPTOP = 20.0;
        try {
            BEAMGAPTOP = ecal.getNode().getChild("layout").getAttribute("beamgapTop").getDoubleValue();
        } catch (DataConversionException e) {
            try {
                BEAMGAPTOP = ecal.getNode().getChild("layout").getAttribute("beamgap").getDoubleValue();
            } catch (DataConversionException ee) {
                ee.printStackTrace();
            }
        }
        double BEAMGAPBOT = -20.0;
        try {
            BEAMGAPBOT = -ecal.getNode().getChild("layout").getAttribute("beamgapBottom").getDoubleValue();
        } catch (DataConversionException e) {
            try {
                BEAMGAPBOT = -ecal.getNode().getChild("layout").getAttribute("beamgap").getDoubleValue();
            } catch (DataConversionException ee) {
                ee.printStackTrace();
            }
        }
        double BEAMGAPTOPC = BEAMGAPTOP + 13.0; // mm
        double BEAMGAPBOTC = BEAMGAPBOT - 13.0; // mm
        // x-coordinates of crystals on either side of row 1 cut out
        EcalCrystal crystalM = detElement.getCrystal(-11, 1);
        Hep3Vector posM = crystalM.getPositionFront();
        EcalCrystal crystalP = detElement.getCrystal(-1, 1);
        Hep3Vector posP = crystalP.getPositionFront();
        if ((xpos < posM.x()) || (xpos > posP.x())) {
            if (ypos > 0) {
                ydist = Math.abs(ypos - BEAMGAPTOP);
            } else {
                ydist = Math.abs(ypos - BEAMGAPBOT);
            }
        } else {
            if (ypos > 0) {
                if (ypos > (CUTOFF_OFFSET + BEAMGAPTOP)) {
                    ydist = Math.abs(ypos - BEAMGAPTOP);
                } else {
                    ydist = Math.abs(ypos - BEAMGAPTOPC);
                }
            } else {
                if (ypos > (-CUTOFF_OFFSET + BEAMGAPBOT)) {
                    ydist = Math.abs(ypos - BEAMGAPBOTC);
                } else {
                    ydist = Math.abs(ypos - BEAMGAPBOT);
                }
            }
        }
        return ydist;
    }

}

package org.hps.recon.ecal.cluster;

import hep.physics.vec.Hep3Vector;
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
        double BEAMGAPTOPC = BEAMGAPTOP + 13.0;// mm
        double BEAMGAPBOTC = BEAMGAPBOT - 13.0;// mm
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
        }
        // crystals above row 1 cut out
        else {
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

        // Eliminates corrections at outermost edges to negative cluster energies
        // 66 for positrons, 69 is safe for electrons and photons
        if (ydist > 65.5) {
            ydist = 65.5;
        }
        if (ydist < 2.5) {
            ydist = 2.5;
        }

        return ydist;
    }

    public static double computeCorrectedEnergy(HPSEcal3 ecal, int pid, double rawE, double x, double y, boolean isMC) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}

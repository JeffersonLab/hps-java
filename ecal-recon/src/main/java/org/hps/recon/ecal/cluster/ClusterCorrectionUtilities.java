package org.hps.recon.ecal.cluster;

import hep.physics.vec.Hep3Vector;
import java.util.List;
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

    
    /**
     * Apply HPS-specific energy and position corrections to a list of clusters in place.
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
                }
                else {
                    ClusterEnergyCorrection.setCorrectedEnergy(ecal, baseCluster, isMC);
                }
            }
        }
    }

    /**
     * Apply HPS-specific energy and position corrections to a cluster without track information.
     * @param cluster The input cluster.
     */
    public static void applyCorrections(double beamEnergy, HPSEcal3 ecal, Cluster cluster, boolean isMC) {
        if (cluster instanceof BaseCluster) {
            BaseCluster baseCluster = (BaseCluster) cluster;
            // Apply PID based position correction, which should happen before final energy correction.
            ClusterPositionCorrection.setCorrectedPosition(baseCluster);
            // Apply PID based energy correction.
            if (beamEnergy > 4.0) {
                ClusterEnergyCorrection2019.setCorrectedEnergy(ecal, baseCluster, isMC);
            }
            else {
                ClusterEnergyCorrection.setCorrectedEnergy(ecal, baseCluster, isMC);
            }
        }
    }

    /**
     * Apply HPS-specific energy and position corrections to a cluster with track information.
     * @param cluster The input cluster.
     */
    public static void applyCorrections(double beamEnergy, HPSEcal3 ecal, Cluster cluster, double ypos, boolean isMC) {
        if (cluster instanceof BaseCluster) {
            BaseCluster baseCluster = (BaseCluster) cluster;
            // Apply PID based position correction, which should happen before final energy correction.
            ClusterPositionCorrection.setCorrectedPosition(baseCluster);
            // Apply PID based energy correction.
            if (beamEnergy > 4.0) {
                ClusterEnergyCorrection2019.setCorrectedEnergy(ecal, baseCluster, ypos, isMC);
            }
            else {
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
                if (ypos > (AbsClusterEnergyCorrection.CUTOFF_OFFSET + BEAMGAPTOP)) {
                    ydist = Math.abs(ypos - BEAMGAPTOP);
                } else {
                    ydist = Math.abs(ypos - BEAMGAPTOPC);
                }
            } else {
                if (ypos > (-AbsClusterEnergyCorrection.CUTOFF_OFFSET + BEAMGAPBOT)) {
                    ydist = Math.abs(ypos - BEAMGAPBOTC);
                } else {
                    ydist = Math.abs(ypos - BEAMGAPBOT);
                }
            }
        }
        return ydist;
    }
    
}

package org.hps.recon.ecal.cluster;

import hep.physics.vec.Hep3Vector;

import org.hps.detector.ecal.EcalCrystal;
import org.hps.detector.ecal.HPSEcalDetectorElement;
//import org.jdom.DataConversionException;
// import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.Cluster;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.geometry.subdetector.HPSEcal3;

/**
 * This is the cluster energy correction requiring the particle id uncorrected
 * cluster energy. This is now updated to include edge corrections and sampling
 * fractions derived from data.
 * 
 * @author Holly Vance <hvanc001@odu.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class ClusterEnergyCorrection {

    // Variables for electron energy corrections.
    static final double par0_em = -0.017;
    static final double par1_em[] = { 35, -0.06738, -0.0005613, 16.42, 0.3431,
            -2.021, 74.85, -0.3626 };
    static final double par2_em[] = { 35, 0.933, 0.003234, 18.06, 0.24, 8.586,
            75.08, -0.39 };

    // Variables for positron energy corrections.
    static final double par0_ep = -0.0131;
    static final double par1_ep[] = { 35, -0.076, -0.0008183, 17.88, 0.2886,
            -1.192, 73.12, -0.3747 };
    static final double par2_ep[] = { 35, 0.94, 0.003713, 18.19, 0.24, 8.342,
            72.44, -0.39 };

    // Variables for photon energy corrections.
    static final double par0_p = -0.0113;
    static final double par1_p[] = { 35, -0.0585, -0.0008572, 16.76, 0.2784,
            -0.07232, 72.88, -0.1685 };
    static final double par2_p[] = { 35, 0.9307, 0.004, 18.05, 0.23, 3.027,
            74.93, -0.34 };

    /**
     * Calculate the corrected energy for the cluster.
     * 
     * @param cluster
     *            The input cluster.
     * @return The corrected energy.
     */
    public static double calculateCorrectedEnergy(HPSEcal3 ecal, Cluster cluster) {
        double rawE = cluster.getEnergy();
        return computeCorrectedEnergy(ecal, cluster.getParticleId(), rawE,
                cluster.getPosition()[0], cluster.getPosition()[1]);
    }

    /**
     * Calculate the corrected energy for the cluster using track position at
     * ecal.
     * 
     * @param cluster
     *            The input cluster.
     * @return The corrected energy.
     */
    public static double calculateCorrectedEnergy(HPSEcal3 ecal,
            Cluster cluster, double ypos) {
        double rawE = cluster.getEnergy();
        return computeCorrectedEnergy(ecal, cluster.getParticleId(), rawE,
                cluster.getPosition()[0], ypos);
    }

    /**
     * Calculate the corrected energy and set on the cluster.
     * 
     * @param cluster
     *            The input cluster.
     */
    public static void setCorrectedEnergy(HPSEcal3 ecal, BaseCluster cluster) {
        double correctedEnergy = calculateCorrectedEnergy(ecal, cluster);
        cluster.setEnergy(correctedEnergy);
    }

    /**
     * Calculate the corrected energy and set on the cluster.
     * 
     * @param cluster
     *            The input cluster.
     */

    public static void setCorrectedEnergy(HPSEcal3 ecal, BaseCluster cluster,
            double ypos) {
        double correctedEnergy = calculateCorrectedEnergy(ecal, cluster, ypos);
        cluster.setEnergy(correctedEnergy);
    }

    /**
     * Calculates energy correction based on cluster raw energy and particle
     * type as per <a href=
     * "https://misportal.jlab.org/mis/physics/hps_notes/index.cfm?note_year=2014"
     * >HPS Note 2014-001</a>
     * 
     * @param pdg
     *            Particle id as per PDG
     * @param rawEnergy
     *            Raw Energy of the cluster (sum of hits with shared hit
     *            distribution)
     * @return Corrected Energy
     */

    private static double computeCorrectedEnergy(HPSEcal3 ecal, int pdg,
            double rawEnergy, double xpos, double ypos) {
        // distance to beam gap edge
        double r;
        // Get these values from the Ecal geometry:
        HPSEcalDetectorElement detElement = (HPSEcalDetectorElement) ecal
                .getDetectorElement();
        // double BEAMGAPTOP =
        // 22.3;//ecal.getNode().getChild("layout").getAttribute("beamgapTop").getDoubleValue();//mm
        double BEAMGAPTOP = 20.0;
        try {
            BEAMGAPTOP = ecal.getNode().getChild("layout")
                .getAttribute("beamgapTop").getDoubleValue();
        } catch (Exception e) {
            try {
                BEAMGAPTOP = ecal.getNode().getChild("layout")
                      .getAttribute("beamgap").getDoubleValue();
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        double BEAMGAPBOT = -20.0;
        try {
            BEAMGAPBOT = -ecal.getNode().getChild("layout")
                .getAttribute("beamgapBottom").getDoubleValue();
        } catch (Exception e) {
            try {
                BEAMGAPBOT = ecal.getNode().getChild("layout")
                    .getAttribute("beamgap").getDoubleValue();
            } catch (Exception ee) {
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
                r = Math.abs(ypos - BEAMGAPTOP);
            } else {
                r = Math.abs(ypos - BEAMGAPBOT);
            }
        }
        // crystals above row 1 cut out
        else {
            if (ypos > 0) {
                if (ypos > (par1_em[0] + BEAMGAPTOP)) {
                    r = Math.abs(ypos - BEAMGAPTOP);
                } else {
                    r = Math.abs(ypos - BEAMGAPTOPC);
                }
            } else {
                if (ypos > (-par1_em[0] + BEAMGAPBOT)) {
                    r = Math.abs(ypos - BEAMGAPBOTC);
                } else {
                    r = Math.abs(ypos - BEAMGAPBOT);
                }
            }
        }
        
        //Eliminates corrections at outermost edges to negative cluster energies
        //66 for positrons, 69 is safe for electrons and photons
        if (r > 66) {r = 66;}
                
        switch (pdg) {
        case 11:
            // electron
            return computeCorrectedEnergy(r, rawEnergy, par0_em, par1_em,
                    par2_em);
        case -11:
            // positron
            return computeCorrectedEnergy(r, rawEnergy, par0_ep, par1_ep,
                    par2_ep);
        case 22:
            // photon
            return computeCorrectedEnergy(r, rawEnergy, par0_p, par1_p, par2_p);
        default:
            // unknown
            return rawEnergy;
        }
    }

    /**
     * Calculates the energy correction to a cluster given the variables from
     * the fit as per <a href=
     * "https://misportal.jlab.org/mis/physics/hps_notes/index.cfm?note_year=2014"
     * >HPS Note 2014-001</a> Note that this is correct as there is a typo in
     * the formula print in the note.
     * 
     * @param rawEnergy
     *            Raw energy of the cluster
     * @param A,B,C from fitting in note
     * @return Corrected Energy
     */
    private static double computeCorrectedEnergy(double y, double rawEnergy,
            double varA, double varB[], double varC[]) {
        int ii = y < varB[0] ? 2 : 5;
        double corrEnergy = rawEnergy/ (varA / rawEnergy+ (varB[1] - varB[ii]* Math.exp(-(y - varB[ii + 1]) * varB[ii + 2]))/ (Math.sqrt(rawEnergy)) + 
                (varC[1] - varC[ii]* Math.exp(-(y - varC[ii + 1]) * varC[ii + 2])));
        return corrEnergy;
    }
}

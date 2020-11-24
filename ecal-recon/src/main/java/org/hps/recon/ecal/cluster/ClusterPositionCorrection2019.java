package org.hps.recon.ecal.cluster;

import org.lcsim.event.Cluster;
import org.lcsim.event.base.BaseCluster;

/**
 * This uses the uncorrected cluster energy to correct the position of the cluster.
 * This should be used before the energy is corrected on the Cluster and after
 * cluster-track matching.
 * This is to be used with 2019 data
 * 
 * @author Andrea Celentano <andrea.celentano@ge.infn.it>
 * @author Luca Marsicano <luca.marsicano@ge.infn.it>
 */

/**
 * To determine these corrections, we simulated e+ e- and gamma at fixed
 * energies over the ECAL acceptance, sampled the true hit position with MC
 * scoring plane, and compared with the measured cluster position. We then
 * considered:
 * 
 * dX vs X (dX = measured - true) ---> true=measured-dX dY vs Y (dY = measured -
 * true) ---> true=measured-dY
 * 
 * We then performed a fit to these with dX = q + m*X dY = q1 + t*Y if x < 0 ; =
 * q2 + t*Y if Y > 0
 * 
 * See also A.C. Talk at Nov. 2020 collaboration meeting
 * 
 * We then checked the dependency of the q,m, q1,q2, t parameters as a function
 * of the energy
 * 
 * Electrons and Positrons: parameter(E) = p0 + p1*pow(E,p2) for all parameters
 * Photons: par(E) = p0 + p1*pow(E,p2) | par = q,m par(E) = (a + b*E + c*E*E)/(d
 * + e*E + f*E*E) | par = q1,t,q2
 * 
 */

final class ClusterPosResult {
    private final double X;
    private final double Y;

    public ClusterPosResult(double X, double Y) {
        this.X = X;
        this.Y = Y;
    }

    public double getX() {
        return X;
    }

    public double getY() {
        return Y;
    }

}

public final class ClusterPositionCorrection2019 {
    // Parameterizations tested in MC using v3-fieldmap
    // Nov 2015

    // Variables for electron position corrections.
    static final double ELECTRON_POS_Q_P0 = 0.966731;
    static final double ELECTRON_POS_Q_P1 = 6.80944;
    static final double ELECTRON_POS_Q_P2 = -0.517001;

    static final double ELECTRON_POS_M_P0 = -0.0321032;
    static final double ELECTRON_POS_M_P1 = 0.0137824;
    static final double ELECTRON_POS_M_P2 = -0.657067;

    static final double ELECTRON_POS_Q1_P0 = -3.38561;
    static final double ELECTRON_POS_Q1_P1 = 0.516986;
    static final double ELECTRON_POS_Q1_P2 = -1.58624;

    static final double ELECTRON_POS_Q2_P0 = 2.43338;
    static final double ELECTRON_POS_Q2_P1 = -0.523349;
    static final double ELECTRON_POS_Q2_P2 = -1.5865;

    static final double ELECTRON_POS_T_P0 = -0.03764;
    static final double ELECTRON_POS_T_P1 = 0.0086778;
    static final double ELECTRON_POS_T_P2 = -1.76526;

    // Variables for positron position corrections.
    static final double POSITRON_POS_Q_P0 = 5.94692;
    static final double POSITRON_POS_Q_P1 = -9.50585;
    static final double POSITRON_POS_Q_P2 = -0.520818;

    static final double POSITRON_POS_M_P0 = -0.0333753;
    static final double POSITRON_POS_M_P1 = 0.0141766;
    static final double POSITRON_POS_M_P2 = -0.590604;

    static final double POSITRON_POS_Q1_P0 = -3.33536;
    static final double POSITRON_POS_Q1_P1 = 0.367933;
    static final double POSITRON_POS_Q1_P2 = -2.50553;

    static final double POSITRON_POS_Q2_P0 = 2.38039;
    static final double POSITRON_POS_Q2_P1 = -0.335049;
    static final double POSITRON_POS_Q2_P2 = -2.6066;

    static final double POSITRON_POS_T_P0 = -0.0372583;
    static final double POSITRON_POS_T_P1 = 0.00688212;
    static final double POSITRON_POS_T_P2 = -2.45476;

    // Variables for photon position corrections.
    static final double PHOTON_POS_Q_P0 = 6.05676;
    static final double PHOTON_POS_Q_P1 = -3.35614;
    static final double PHOTON_POS_Q_P2 = -0.129487;

    static final double PHOTON_POS_M_P0 = -0.0604739;
    static final double PHOTON_POS_M_P1 = 0.0345978;
    static final double PHOTON_POS_M_P2 = -0.134836;

    static final double PHOTON_POS_Q1_A = -5.35288e-04;
    static final double PHOTON_POS_Q1_B = -2.59465e-03;
    static final double PHOTON_POS_Q1_C = -1.25344e-02;
    static final double PHOTON_POS_Q1_D = 5.38706e-04;
    static final double PHOTON_POS_Q1_E = -1.03844e-04;
    static final double PHOTON_POS_Q1_F = 3.42212e-03;

    static final double PHOTON_POS_T_A = -2.94034e-05;
    static final double PHOTON_POS_T_B = 5.01519e-05;
    static final double PHOTON_POS_T_C = -1.61436e-04;
    static final double PHOTON_POS_T_D = 9.81269e-04;
    static final double PHOTON_POS_T_E = -1.72302e-03;
    static final double PHOTON_POS_T_F = 3.44850e-03;

    static final double PHOTON_POS_Q2_A = 7.20260e-01;
    static final double PHOTON_POS_Q2_B = -7.33752e-01;
    static final double PHOTON_POS_Q2_C = 3.70987e+00;
    static final double PHOTON_POS_Q2_D = 4.20198e-01;
    static final double PHOTON_POS_Q2_E = -5.84695e-01;
    static final double PHOTON_POS_Q2_F = 1.35651e+00;

    /**
     * 
     * @param cluster The cluster to be corrected, where the energy is the already-corrected energy. THIS IS DIFFERENT FROM 2015!
     * @return The corrected cluster position
     */
    
    public static double[] calculateCorrectedPosition(BaseCluster cluster) {
        double clusterPosition[] = cluster.getPosition(); 
        ClusterPosResult correctedPosition = computeCorrectedPosition(cluster.getParticleId(), clusterPosition[0],clusterPosition[1], cluster.getEnergy());
        
        double[] position = new double[3];
        position[0] = correctedPosition.getX();
        position[1] = correctedPosition.getY();
        position[2] = clusterPosition[2];
        return position;
    }

    public static void setCorrectedPosition(BaseCluster cluster) {
        cluster.setPosition(calculateCorrectedPosition(cluster));
    }

    /**
     * Calculates position correction based on cluster corrected energy, x
     * calculated position, and particle type as per <a href=
     * "https://misportal.jlab.org/mis/physics/hps_notes/index.cfm?note_year=2014">HPS
     * Note 2014-001</a>
     * 
     * @param pdg    Particle id as per PDG
     * @param xCl    Calculated x centroid position of the cluster, uncorrected, at
     *               face
     * @param Energy Corrected energy of the cluster
     * @return the corrected x position
     */
    private static ClusterPosResult computeCorrectedPosition(int pdg, double xPos, double yPos, double Energy) {
        // double xCl = xPos / 10.0;//convert to cm
        ClusterPosResult res;
        double xCorr;
        switch (pdg) {
            case 11: // Particle is electron
                res = positionCorrectionElectron(xPos, yPos, Energy);
                break;
            case -11:// Particle is positron
                res = positionCorrectionPositron(xPos, yPos, Energy);
                break;
            case 22: // Particle is photon
                res = positionCorrectionPhoton(xPos, yPos, Energy);
                break;
            default: // Unknown
                res = new ClusterPosResult(xPos, yPos);
        }
        return res;
    }

    private static ClusterPosResult positionCorrectionElectron(double xPos, double yPos, double Energy) {
        double xCorr, yCorr;
        double deltaX, deltaY;

        double q = ELECTRON_POS_Q_P0 + ELECTRON_POS_Q_P1 * Math.pow(Energy, ELECTRON_POS_Q_P2);
        double m = ELECTRON_POS_M_P0 + ELECTRON_POS_M_P1 * Math.pow(Energy, ELECTRON_POS_M_P2);
        double q1 = ELECTRON_POS_Q1_P0 + ELECTRON_POS_Q1_P1 * Math.pow(Energy, ELECTRON_POS_Q2_P2);
        double q2 = ELECTRON_POS_Q2_P0 + ELECTRON_POS_Q2_P1 * Math.pow(Energy, ELECTRON_POS_Q2_P2);
        double t = ELECTRON_POS_T_P0 + ELECTRON_POS_T_P1 * Math.pow(Energy, ELECTRON_POS_T_P2);

        deltaX = q + m * xPos;

        if (yPos < 0) {
            deltaY = q1 + t * yPos;
        } else {
            deltaY = q2 + t * yPos;
        }

        xCorr = xPos - deltaX;
        yCorr = yPos - deltaY;

        return new ClusterPosResult(xCorr, yCorr);
    }

    private static ClusterPosResult positionCorrectionPositron(double xPos, double yPos, double Energy) {
        double xCorr, yCorr;
        double deltaX, deltaY;

        double q = POSITRON_POS_Q_P0 + POSITRON_POS_Q_P1 * Math.pow(Energy, POSITRON_POS_Q_P2);
        double m = POSITRON_POS_M_P0 + POSITRON_POS_M_P1 * Math.pow(Energy, POSITRON_POS_M_P2);
        double q1 = POSITRON_POS_Q1_P0 + POSITRON_POS_Q1_P1 * Math.pow(Energy, POSITRON_POS_Q2_P2);
        double q2 = POSITRON_POS_Q2_P0 + POSITRON_POS_Q2_P1 * Math.pow(Energy, POSITRON_POS_Q2_P2);
        double t = POSITRON_POS_T_P0 + POSITRON_POS_T_P1 * Math.pow(Energy, POSITRON_POS_T_P2);

        deltaX = q + m * xPos;

        if (yPos < 0) {
            deltaY = q1 + t * yPos;
        } else {
            deltaY = q2 + t * yPos;
        }

        xCorr = xPos - deltaX;
        yCorr = yPos - deltaY;

        return new ClusterPosResult(xCorr, yCorr);
    }

    private static ClusterPosResult positionCorrectionPhoton(double xPos, double yPos, double Energy) {
        double xCorr, yCorr;
        double deltaX, deltaY;

        double q = PHOTON_POS_Q_P0 + PHOTON_POS_Q_P1 * Math.pow(Energy, PHOTON_POS_Q_P2);
        double m = PHOTON_POS_M_P0 + PHOTON_POS_M_P1 * Math.pow(Energy, PHOTON_POS_M_P2);
        double q1 = PHOTON_POS_Q1_A + PHOTON_POS_Q1_B * Energy + PHOTON_POS_Q1_C * Energy * Energy
                / (PHOTON_POS_Q1_D + PHOTON_POS_Q1_E * Energy + PHOTON_POS_Q1_F * Energy * Energy);
        double q2 = PHOTON_POS_Q2_A + PHOTON_POS_Q2_B * Energy + PHOTON_POS_Q2_C * Energy * Energy
                / (PHOTON_POS_Q2_D + PHOTON_POS_Q2_E * Energy + PHOTON_POS_Q2_F * Energy * Energy);
        double t = PHOTON_POS_T_A + PHOTON_POS_T_B * Energy + PHOTON_POS_T_C * Energy * Energy
                / (PHOTON_POS_T_D + PHOTON_POS_T_E * Energy + PHOTON_POS_T_F * Energy * Energy);

        deltaX = q + m * xPos;

        if (yPos < 0) {
            deltaY = q1 + t * yPos;
        } else {
            deltaY = q2 + t * yPos;
        }

        xCorr = xPos - deltaX;
        yCorr = yPos - deltaY;

        return new ClusterPosResult(xCorr, yCorr);
    }

}

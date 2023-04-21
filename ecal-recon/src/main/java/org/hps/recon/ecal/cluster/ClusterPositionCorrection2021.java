package org.hps.recon.ecal.cluster;

import org.lcsim.event.base.BaseCluster;

/**
 * This uses the corrected cluster energy to correct the position of the
 * cluster. This is to be used with 2021 data
 *
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
 * See also A.C. Talk at Nov. 2022 collaboration meeting
 * 
 * We then checked the dependency of the q,m, q1,q2, t parameters as a function
 * of the energy
 * 
 * Electrons and Positrons: parameter(E) = p0 + p1*pow(E,p2) for all parameters
 * Photons: par(E) = p0 + p1*pow(E,p2) | par = q,m par(E) = (a + b*E + c*E*E)/(d
 * + e*E + f*E*E) | par = q1,t,q2
 */
final class ClusterPosResult21 {
    private final double X;
    private final double Y;

    public ClusterPosResult21(double X, double Y) {
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

public final class ClusterPositionCorrection2021 {

    // Variables for positron position corrections.
    static final double POSITRON_POS_Q_P0 = 1.35533;
    static final double POSITRON_POS_Q_P1 = 5.72332;
    static final double POSITRON_POS_Q_P2 = -0.541438;

    static final double POSITRON_POS_M_P0 = -0.0340964;
    static final double POSITRON_POS_M_P1 = 0.014045;
    static final double POSITRON_POS_M_P2 = -0.545433;

    static final double POSITRON_POS_Q1_P0 = -3.21226;
    static final double POSITRON_POS_Q1_P1 = 0.339324;
    static final double POSITRON_POS_Q1_P2 = -2.72148;

    static final double POSITRON_POS_T_P0 = -0.0362339;
    static final double POSITRON_POS_T_P1 = 0.00449926;
    static final double POSITRON_POS_T_P2 = -2.91123;

    static final double POSITRON_POS_Q2_P0 = 2.24442;
    static final double POSITRON_POS_Q2_P1 = -0.282654;
    static final double POSITRON_POS_Q2_P2 = -3.20633;

    // Variables for electron position corrections.
    static final double ELECTRON_POS_Q_P0 = 5.05789;
    static final double ELECTRON_POS_Q_P1 = -7.63708;
    static final double ELECTRON_POS_Q_P2 = -0.593751;

    static final double ELECTRON_POS_M_P0 = -0.0318827;
    static final double ELECTRON_POS_M_P1 = 0.0100568;
    static final double ELECTRON_POS_M_P2 = -0.676475;

    static final double ELECTRON_POS_Q1_P0 = -2.71442;
    static final double ELECTRON_POS_Q1_P1 = -0.456846;
    static final double ELECTRON_POS_Q1_P2 = -0.772825;

    static final double ELECTRON_POS_T_P0 = -0.0275841;
    static final double ELECTRON_POS_T_P1 = -0.00844973;
    static final double ELECTRON_POS_T_P2 = -0.628533;

    static final double ELECTRON_POS_Q2_P0 = 1.72361;
    static final double ELECTRON_POS_Q2_P1 = 0.524511;
    static final double ELECTRON_POS_Q2_P2 = -0.697755;

    // Variables for photon position corrections.
    static final double PHOTON_POS_Q_P0 = 5.53107;
    static final double PHOTON_POS_Q_P1 = -2.71633;
    static final double PHOTON_POS_Q_P2 = -0.157991;

    static final double PHOTON_POS_M_P0 = -0.0645554;
    static final double PHOTON_POS_M_P1 = 0.0376413;
    static final double PHOTON_POS_M_P2 = -0.119576;

    static final double PHOTON_POS_Q1_P0 = -3.99615;
    static final double PHOTON_POS_Q1_P1 = 0.967692;
    static final double PHOTON_POS_Q1_P2 = -0.317565;

    static final double PHOTON_POS_T_P0 = -0.0582419;
    static final double PHOTON_POS_T_P1 = 0.0247615;
    static final double PHOTON_POS_T_P2 = -0.233813;

    static final double PHOTON_POS_Q2_P0 = 3.1551;
    static final double PHOTON_POS_Q2_P1 = -1.09655;
    static final double PHOTON_POS_Q2_P2 = -0.283272;

    /**
     * 
     * @param cluster The cluster to be corrected, where the energy is the
     *                already-corrected energy. THIS IS DIFFERENT FROM 2015!
     * @return The corrected cluster position
     */

    public static double[] calculateCorrectedPosition(BaseCluster cluster) {
        double clusterPosition[] = cluster.getPosition();

        ClusterPosResult21 correctedPosition = computeCorrectedPosition(cluster.getParticleId(), clusterPosition[0],
                clusterPosition[1], cluster.getEnergy());

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
    private static ClusterPosResult21 computeCorrectedPosition(int pdg, double xPos, double yPos, double Energy) {
        // double xCl = xPos / 10.0;//convert to cm
        ClusterPosResult21 res;
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
                res = new ClusterPosResult21(xPos, yPos);
        }
        return res;
    }

    private static ClusterPosResult21 positionCorrectionElectron(double xPos, double yPos, double Energy) {
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

        return new ClusterPosResult21(xCorr, yCorr);
    }

    private static ClusterPosResult21 positionCorrectionPositron(double xPos, double yPos, double Energy) {
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

        return new ClusterPosResult21(xCorr, yCorr);
    }

    private static ClusterPosResult21 positionCorrectionPhoton(double xPos, double yPos, double Energy) {
        double xCorr, yCorr;
        double deltaX, deltaY;

        double q = PHOTON_POS_Q_P0 + PHOTON_POS_Q_P1 * Math.pow(Energy, PHOTON_POS_Q_P2);
        double m = PHOTON_POS_M_P0 + PHOTON_POS_M_P1 * Math.pow(Energy, PHOTON_POS_M_P2);
        double q1 = PHOTON_POS_Q1_P0 + PHOTON_POS_Q1_P1 * Math.pow(Energy, PHOTON_POS_Q2_P2);
        double q2 = PHOTON_POS_Q2_P0 + PHOTON_POS_Q2_P1 * Math.pow(Energy, PHOTON_POS_Q2_P2);
        double t = PHOTON_POS_T_P0 + PHOTON_POS_T_P1 * Math.pow(Energy, PHOTON_POS_T_P2);

        deltaX = q + m * xPos;

        if (yPos < 0) {
            deltaY = q1 + t * yPos;
        } else {
            deltaY = q2 + t * yPos;
        }

        xCorr = xPos - deltaX;
        yCorr = yPos - deltaY;

        return new ClusterPosResult21(xCorr, yCorr);
    }

}

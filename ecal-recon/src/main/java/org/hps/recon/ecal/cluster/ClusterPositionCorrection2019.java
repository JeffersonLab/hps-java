package org.hps.recon.ecal.cluster;

import org.lcsim.event.base.BaseCluster;

/**
 * This uses the uncorrected cluster energy to correct the position of the cluster.
 * This should be used before the energy is corrected on the Cluster and after
 * cluster-track matching.
 * This is to be used with 2019 data
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
 * See also A.C. Talk at Nov. 2020 collaboration meeting
 * 
 * We then checked the dependency of the q,m, q1,q2, t parameters as a function
 * of the energy
 * 
 * Electrons and Positrons: parameter(E) = p0 + p1*pow(E,p2) for all parameters
 * Photons: par(E) = p0 + p1*pow(E,p2) | par = q,m par(E) = (a + b*E + c*E*E)/(d
 * + e*E + f*E*E) | par = q1,t,q2
 */
final class ClusterPosResult19 {
    private final double X;
    private final double Y;

    public ClusterPosResult19(double X, double Y) {
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

    // Variables for positron position corrections.
    static final double POSITRON_POS_Q_P0 = -4.28501;
    static final double POSITRON_POS_Q_P1 = 11.7974;
    static final double POSITRON_POS_Q_P2 = -0.260121;
    
    static final double POSITRON_POS_M_P0 = -0.0255807;
    static final double POSITRON_POS_M_P1 = 0.00843705;
    static final double POSITRON_POS_M_P2 = -1.42579;
    
    static final double POSITRON_POS_Q1_P0 = -3.59856;
    static final double POSITRON_POS_Q1_P1 = 1.14647;
    static final double POSITRON_POS_Q1_P2 = -0.839847;
    
    static final double POSITRON_POS_T_P0 = -0.0443278;
    static final double POSITRON_POS_T_P1 = 0.0214648;
    static final double POSITRON_POS_T_P2 = -0.846413;
    
    static final double POSITRON_POS_Q2_P0 = 2.66236;
    static final double POSITRON_POS_Q2_P1 = -1.22704;
    static final double POSITRON_POS_Q2_P2 = -0.887525;
    
    
    // Variables for electron position corrections.
    static final double ELECTRON_POS_Q_P0 = 3902.39;
    static final double ELECTRON_POS_Q_P1 = -3905.4;
    static final double ELECTRON_POS_Q_P2 = -0.000818816;
    
    static final double ELECTRON_POS_M_P0 = -0.0268859;
    static final double ELECTRON_POS_M_P1 = 0.00877462;
    static final double ELECTRON_POS_M_P2 = -2.26482;
    
    static final double ELECTRON_POS_Q1_P0 = -3.54017;
    static final double ELECTRON_POS_Q1_P1 = 0.956679;
    static final double ELECTRON_POS_Q1_P2 = -1.04374;
    
    static final double ELECTRON_POS_T_P0 = -0.0439532;
    static final double ELECTRON_POS_T_P1 = 0.0192418;
    static final double ELECTRON_POS_T_P2 = -0.970458;
    
    static final double ELECTRON_POS_Q2_P0 = 2.61555;
    static final double ELECTRON_POS_Q2_P1 = -1.0365;
    static final double ELECTRON_POS_Q2_P2 = -1.06414;
    

    // Variables for photon position corrections.   
    static final double PHOTON_POS_Q_P0 = 8.76131;
    static final double PHOTON_POS_Q_P1 = -6.13445;
    static final double PHOTON_POS_Q_P2 = -0.0818788;
    
    static final double PHOTON_POS_M_P0 = -0.0910753;
    static final double PHOTON_POS_M_P1 = 0.0658226;
    static final double PHOTON_POS_M_P2 = -0.0806989;
    
    static final double PHOTON_POS_Q1_P0 = -3.75432;
    static final double PHOTON_POS_Q1_P1 = 0.833772;
    static final double PHOTON_POS_Q1_P2 = -0.382339;
    
    static final double PHOTON_POS_T_P0 = -0.0522062;
    static final double PHOTON_POS_T_P1 = 0.020257;
    static final double PHOTON_POS_T_P2 = -0.30663;
    
    static final double PHOTON_POS_Q2_P0 = 2.81925;
    static final double PHOTON_POS_Q2_P1 = -0.868117;
    static final double PHOTON_POS_Q2_P2 = -0.372597;
    
    

    /**
     * 
     * @param cluster The cluster to be corrected, where the energy is the
     *                already-corrected energy. THIS IS DIFFERENT FROM 2015!
     * @return The corrected cluster position
     */

    public static double[] calculateCorrectedPosition(BaseCluster cluster) {
        double clusterPosition[] = cluster.getPosition();

        ClusterPosResult19 correctedPosition = computeCorrectedPosition(cluster.getParticleId(), clusterPosition[0],
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
    private static ClusterPosResult19 computeCorrectedPosition(int pdg, double xPos, double yPos, double Energy) {
        // double xCl = xPos / 10.0;//convert to cm
        ClusterPosResult19 res;
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
                res = new ClusterPosResult19(xPos, yPos);
        }
        return res;
    }

    private static ClusterPosResult19 positionCorrectionElectron(double xPos, double yPos, double Energy) {
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

        return new ClusterPosResult19(xCorr, yCorr);
    }

    private static ClusterPosResult19 positionCorrectionPositron(double xPos, double yPos, double Energy) {
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

        return new ClusterPosResult19(xCorr, yCorr);
    }

    private static ClusterPosResult19 positionCorrectionPhoton(double xPos, double yPos, double Energy) {
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

        return new ClusterPosResult19(xCorr, yCorr);
    }

}

package org.hps.recon.ecal.cluster;

import org.lcsim.event.Cluster;
import org.lcsim.event.base.BaseCluster;

/**
 * <p>
 * Cluster position corrections taken from the <code>HPSEcalClusterIC</code> class.
 * <p>
 * This should be used before the energy is corrected on the Cluster.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class ReconClusterPositionCorrection {
           
    // Variables for electron position corrections.
    static final double ELECTRON_POS_A = 0.0066;
    static final double ELECTRON_POS_B = -0.03;
    static final double ELECTRON_POS_C = 0.028;
    static final double ELECTRON_POS_D = -0.45;
    static final double ELECTRON_POS_E = 0.465;
    
    // Variables for positron position corrections.
    static final double POSITRON_POS_A = 0.0072;
    static final double POSITRON_POS_B = -0.031;
    static final double POSITRON_POS_C = 0.007;
    static final double POSITRON_POS_D = 0.342;
    static final double POSITRON_POS_E = 0.108;
    
    // Variables for photon position corrections.
    static final double PHOTON_POS_A = 0.005;
    static final double PHOTON_POS_B = -0.032;
    static final double PHOTON_POS_C = 0.011;
    static final double PHOTON_POS_D = -0.037;
    static final double PHOTON_POS_E = 0.294;
    
    public static double[] calculateCorrectedPosition(Cluster cluster) {
        double clusterPosition[] = cluster.getPosition();                
        double correctedPosition = computeCorrectedPosition(cluster.getParticleId(), clusterPosition[0], cluster.getEnergy());
        double[] position = new double[3];
        position[0] = correctedPosition;
        position[1] = clusterPosition[1];
        position[2] = clusterPosition[2];
        return position;
    }
    
    public static void setCorrectedPosition(BaseCluster cluster) {
        cluster.setPosition(calculateCorrectedPosition(cluster));
    }
                                
    /**
     * Calculates position correction based on cluster raw energy, x calculated position, 
     * and particle type as per 
     * <a href="https://misportal.jlab.org/mis/physics/hps_notes/index.cfm?note_year=2014">HPS Note 2014-001</a>
     * @param pdg Particle id as per PDG
     * @param xCl Calculated x centroid position of the cluster, uncorrected, at face
     * @param rawEnergy Raw energy of the cluster (sum of hits with shared hit distribution)
     * @return Corrected x position
     */
    private static double computeCorrectedPosition(int pdg, double xPos, double rawEnergy) {
        double xCl = xPos / 10.0;//convert to mm
        double xCorr;
        switch(pdg) {
            case 11: //Particle is electron        
                xCorr = positionCorrection(xCl, rawEnergy, ELECTRON_POS_A, ELECTRON_POS_B, ELECTRON_POS_C, ELECTRON_POS_D, ELECTRON_POS_E);
                return xCorr * 10.0;
            case -11:// Particle is positron       
                xCorr = positionCorrection(xCl, rawEnergy, POSITRON_POS_A, POSITRON_POS_B, POSITRON_POS_C, POSITRON_POS_D, POSITRON_POS_E);
                return xCorr * 10.0;
            case 22: // Particle is photon      
                xCorr = positionCorrection(xCl, rawEnergy, PHOTON_POS_A, PHOTON_POS_B, PHOTON_POS_C, PHOTON_POS_D, PHOTON_POS_E);
                return xCorr * 10.0;
            default: //Unknown 
                xCorr = xCl;
                return xCorr * 10.0;
        }
    }
    
   /**
    * Calculates the position correction in cm using the raw energy and variables associated with the fit
    * of the particle as described in  
    * <a href="https://misportal.jlab.org/mis/physics/hps_notes/index.cfm?note_year=2014">HPS Note 2014-001</a>
    * @param xCl
    * @param rawEnergy
    * @param varA
    * @param varB
    * @param varC
    * @param varD
    * @param varE
    * @return
    */    
    private static double positionCorrection(double xCl, double rawEnergy, double varA, double varB, double varC, double varD, double varE) {
        return xCl - (varA / Math.sqrt(rawEnergy) + varB ) * xCl - (varC * rawEnergy + varD / Math.sqrt(rawEnergy) + varE);
    }
}
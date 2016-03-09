package org.hps.recon.ecal.cluster;

import org.lcsim.event.Cluster;
import org.lcsim.event.base.BaseCluster;

/**
 * This uses the uncorrected cluster energy to correct the position of the cluster.
 * This should be used before the energy is corrected on the Cluster and after
 * cluster-track matching.
 * 
 * @author Holly Vance <hvanc001@odu.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class ClusterPositionCorrection {
    //Parameterizations tested in MC using v3-fieldmap
    //Nov 2015
      
    // Variables for electron position corrections.
    static final double ELECTRON_POS_A1 = 0.004483;
    static final double ELECTRON_POS_A2 = -0.02884;
    static final double ELECTRON_POS_B1 = 0.6197;
    static final double ELECTRON_POS_B2 = -2.279;
    static final double ELECTRON_POS_B3 = 3.66;
    
    // Variables for positron position corrections.
    static final double POSITRON_POS_A1 = 0.006887;
    static final double POSITRON_POS_A2 = -0.03207;
    static final double POSITRON_POS_B1 = -0.8048;
    static final double POSITRON_POS_B2 = 0.9366;
    static final double POSITRON_POS_B3 = 2.628;
    
    // Variables for photon position corrections.
    static final double PHOTON_POS_A1 = 0.005385;
    static final double PHOTON_POS_A2 = -0.03562;
    static final double PHOTON_POS_B1 = -0.1948;
    static final double PHOTON_POS_B2 = -0.7991;
    static final double PHOTON_POS_B3 = 3.797;
    
  
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
        //double xCl = xPos / 10.0;//convert to cm
        double xCorr;
        switch(pdg) {
            case 11: //Particle is electron        
                xCorr = positionCorrection(xPos, rawEnergy, ELECTRON_POS_A1, ELECTRON_POS_A2, ELECTRON_POS_B1, ELECTRON_POS_B2, ELECTRON_POS_B3);
                return xCorr;
            case -11:// Particle is positron       
                xCorr = positionCorrection(xPos, rawEnergy, POSITRON_POS_A1, POSITRON_POS_A2, POSITRON_POS_B1, POSITRON_POS_B2, POSITRON_POS_B3);
                return xCorr;
            case 22: // Particle is photon      
                xCorr = positionCorrection(xPos, rawEnergy, PHOTON_POS_A1, PHOTON_POS_A2, PHOTON_POS_B1, PHOTON_POS_B2, PHOTON_POS_B3);
                return xCorr;
            default: //Unknown 
                xCorr = xPos;
                return xCorr;
        }
    }
    
   /**
    * Calculates the position correction in cm using the raw energy and variables associated with the fit
    * of the particle. Prodecure described in  
    * <a href="https://misportal.jlab.org/mis/physics/hps_notes/index.cfm?note_year=2014">HPS Note 2014-001</a>
    * @param xCl
    * @param rawEnergy
    * @param varA1
    * @param varA2
    * @param varA3
    * @param varB1
    * @param varB2
    * @param varB3
    * @return
    */    
    private static double positionCorrection(double xCl, double rawEnergy, double varA1, double varA2, double varB1, double varB2, double varB3) {
        //return ((xCl - (varB1 * rawEnergy + varB2 / Math.sqrt(rawEnergy) + varB3))/(varA1 / Math.sqrt(rawEnergy) + varA2 + 1));
        return ((xCl - (varB1 * rawEnergy + varB2 / Math.sqrt(rawEnergy) + varB3))/(varA1 / Math.sqrt(rawEnergy) + varA2 + 1));
    }
}

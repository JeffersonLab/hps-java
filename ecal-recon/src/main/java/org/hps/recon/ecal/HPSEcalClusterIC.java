package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.CalorimeterHit;

/**
 * Cluster with addition to include shared hits and set position
 * as calculated in full cluster code. 
 *
 * @author Holly Szumila <hvanc001@odu.edu>
 * @author Sho Uemura <meeg@slac.stanford.edu>
 */
public class HPSEcalClusterIC extends HPSEcalCluster {
    private int particleID = 0;
    private double[] rawPosition = new double[3];
    private ArrayList<CalorimeterHit> allHitList = new ArrayList<CalorimeterHit>();
    private ArrayList<CalorimeterHit> sharedHitList = new ArrayList<CalorimeterHit>();
    protected double uncorrected_energy;
    
    // Variables for electron energy corrections
    static final double ELECTRON_ENERGY_A = -0.0027;
    static final double ELECTRON_ENERGY_B = -0.06;
    static final double ELECTRON_ENERGY_C = 0.95;
    // Variables for positron energy corrections
    static final double POSITRON_ENERGY_A = -0.0096;
    static final double POSITRON_ENERGY_B = -0.042;
    static final double POSITRON_ENERGY_C = 0.94;
    // Variables for photon energy corrections
    static final double PHOTON_ENERGY_A = 0.0015;
    static final double PHOTON_ENERGY_B = -0.047;
    static final double PHOTON_ENERGY_C = 0.94;
    // Variables for electron position corrections
    static final double ELECTRON_POS_A = 0.0066;
    static final double ELECTRON_POS_B = -0.03;
    static final double ELECTRON_POS_C = 0.028;
    static final double ELECTRON_POS_D = -0.45;
    static final double ELECTRON_POS_E = 0.465;
    // Variables for positron position corrections
    static final double POSITRON_POS_A = 0.0072;
    static final double POSITRON_POS_B = -0.031;
    static final double POSITRON_POS_C = 0.007;
    static final double POSITRON_POS_D = 0.342;
    static final double POSITRON_POS_E = 0.108;
    // Variables for photon position corrections
    static final double PHOTON_POS_A = 0.005;
    static final double PHOTON_POS_B = -0.032;
    static final double PHOTON_POS_C = 0.011;
    static final double PHOTON_POS_D = -0.037;
    static final double PHOTON_POS_E = 0.294;
    
    public HPSEcalClusterIC(Long cellID) {
        super(cellID);
    }

    public HPSEcalClusterIC(CalorimeterHit seedHit) {
        super(seedHit.getCellID());
        setSeedHit(seedHit);
    }
    
    public void addHit(CalorimeterHit hit) {
        super.addHit(hit);
        allHitList.add(hit);
    }
    
    /**
     * Input shared hits between two clusters. 
     */
    public void addSharedHit(CalorimeterHit sharedHit) {
        sharedHitList.add(sharedHit);
        allHitList.add(sharedHit);
    }
    /**
     * Return shared hit list between two clusters. 
     */
    public List<CalorimeterHit> getSharedHits() {
        return sharedHitList;
    }  
    /**
     * Inputs the uncorrected x,y,z position of the cluster.
     */
    public void setRawPosition(double[] Position) {
        rawPosition = Position;
    }
    
    /**
     * Sets the uncorrected cluster energy. External calculation in clustering due to inclusion
     * of shared hit distributed energies.
     */
    public void setUncorrectedEnergy(double uncorrectedE)
    {
    	uncorrected_energy = uncorrectedE;
    }
    
    /**
     * Returns the uncorrected energy of a cluster as set by cluster.
     */
    public double getUncorrectedEnergy()
    {
    	return this.uncorrected_energy;
    }
    
    
    /**
     * Gets the cluster hits that are not shared with other clusters.
     * @return Returns the clusters as a <code>List</code> object
     * containing <code>CalorimeterHit</code> objects.
     */
    public List<CalorimeterHit> getUniqueHits() {
        return super.getCalorimeterHits();
    }
    
    /**
     * Gets all hits in the cluster.
     */
    public List<CalorimeterHit> getCalorimeterHits() {
        return allHitList;
    }
    
    /**
     * Returns the uncorrected x,y,z position of the cluster.
     */
    @Override
    public double[] getPosition() {
        return this.rawPosition;
    }
    
    
    /**
     * Inputs the corrected position of the cluster, see HPS Note 2014-001.
     */
    public void setCorrPosition(double[] position) {
        this.position = position;
    }
    
    /**
     * Returns the corrected position of the cluster. 
     */
    public double[] getCorrPosition(){
        return this.position;
    }
    
    /**
     * Calculates energy correction based on cluster raw energy and particle type as per 
     *<a href="https://misportal.jlab.org/mis/physics/hps_notes/index.cfm?note_year=2014">HPS Note 2014-001</a>
     * @param pdg Particle id as per PDG
     * @param rawEnergy Raw Energy of the cluster (sum of hits with shared hit distribution)
     * @return Corrected Energy
     */    
    private double enCorrection(int pdg, double rawEnergy) {
        switch(pdg) {
            case 11: // Particle is electron             
                return energyCorrection(rawEnergy, ELECTRON_ENERGY_A, ELECTRON_ENERGY_B, ELECTRON_ENERGY_C);
            case -11: //Particle is positron
                return energyCorrection(rawEnergy, POSITRON_ENERGY_A, POSITRON_ENERGY_B, POSITRON_ENERGY_C);
            case 22: //Particle is photon
                return energyCorrection(rawEnergy, PHOTON_ENERGY_A, PHOTON_ENERGY_B, PHOTON_ENERGY_C);
            default: //Unknown 
                return rawEnergy;
        }
    }
    
    /**
     * Calculates the energy correction to a cluster given the variables from the fit as per
     * <a href="https://misportal.jlab.org/mis/physics/hps_notes/index.cfm?note_year=2014">HPS Note 2014-001</a>
     * @param rawEnergy Raw energy of the cluster
     * @param A,B,C from fitting in note
     * @return Corrected Energy
     */   
    private double energyCorrection(double rawEnergy, double varA, double varB, double varC){
        double corrEnergy = rawEnergy / (varA * rawEnergy + varB / (Math.sqrt(rawEnergy)) + varC);
        return corrEnergy;
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
    private double posCorrection(int pdg, double xPos, double rawEnergy) {
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
    private double positionCorrection(double xCl, double rawEnergy, double varA,
            double varB, double varC, double varD, double varE) {
        double xCorr = xCl - (varA / Math.sqrt(rawEnergy) + varB ) * xCl 
                - (varC * rawEnergy + varD / Math.sqrt(rawEnergy) + varE);
        return xCorr;
    }
    
    private void recalculateForParticleID(int pid) {
        double rawE = getEnergy();
        double corrE = enCorrection(pid, rawE);
        setEnergy(corrE);
        double rawP[] = getPosition();
        double corrP = posCorrection(pid, rawP[0], rawE);
        double[] corrPosition = new double[3];
        corrPosition[0] = corrP;
        corrPosition[1] = rawP[1];
        corrPosition[2] = rawP[2];
        setCorrPosition(corrPosition);
    }
    
    public void setParticleID(int pid) {
        if(particleID!=pid) {
            particleID = pid;
            recalculateForParticleID(pid);
        }
    }
}
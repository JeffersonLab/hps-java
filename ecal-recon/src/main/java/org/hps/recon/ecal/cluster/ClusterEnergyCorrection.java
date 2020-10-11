package org.hps.recon.ecal.cluster;

import org.lcsim.geometry.subdetector.HPSEcal3;

/**
 * This is the cluster energy correction requiring the particle id uncorrected cluster energy. This is now updated to
 * include edge corrections and sampling fractions derived from data.
 * 
 * @author Holly Vance <hvanc001@odu.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class ClusterEnergyCorrection extends AbsClusterEnergyCorrection {

    // Variables for electron energy corrections.
    static final double par0_em = -0.017;
    static final double par1_em[] = {35, -0.06738, -0.0005613, 16.42, 0.3431, -2.021, 74.85, -0.3626};
    static final double par2_em[] = {35, 0.933, 0.003234, 18.06, 0.24, 8.586, 75.08, -0.39};

    // Variables for positron energy corrections.
    static final double par0_ep = -0.0131;
    static final double par1_ep[] = {35, -0.076, -0.0008183, 17.88, 0.2886, -1.192, 73.12, -0.3747};
    static final double par2_ep[] = {35, 0.94, 0.003713, 18.19, 0.24, 8.342, 72.44, -0.39};

    // Variables for photon energy corrections.
    static final double par0_p = -0.0113;
    static final double par1_p[] = {35, -0.0585, -0.0008572, 16.76, 0.2784, -0.07232, 72.88, -0.1685};
    static final double par2_p[] = {35, 0.9307, 0.004, 18.05, 0.23, 3.027, 74.93, -0.34};

    // Variables for electron energy corrections--MC.
    static final double par0MC_em = 0.009051;
    static final double par1MC_em[] = {35, -0.1322, -0.0005613, 16.42, 0.3431, -2.021, 74.85, -0.3626};
    static final double par2MC_em[] = {35, 0.9652, 0.003234, 18.06, 0.2592, 8.586, 75.08, -0.3771};

    // Variables for positron energy corrections--MC.
    static final double par0MC_ep = 0.01307;
    static final double par1MC_ep[] = {35, -0.1415, -0.0008183, 17.88, 0.2886, -1.192, 73.12, -0.3747};
    static final double par2MC_ep[] = {35, 0.9733, 0.003713, 18.19, 0.2557, 8.342, 72.44, -0.3834};

    // Variables for photon energy corrections--MC.
    static final double par0MC_p = 0.01604;
    static final double par1MC_p[] = {35, -0.1268, -0.0008572, 16.76, 0.2784, -0.07232, 72.88, -0.1685};
    static final double par2MC_p[] = {35, 0.965, 0.004, 18.05, 0.24, 3.027, 74.93, -0.3221};

    /**
     * Calculates energy correction based on cluster raw energy and particle type as per <a href=
     * "https://misportal.jlab.org/mis/physics/hps_notes/index.cfm?note_year=2014" >HPS Note 2014-001</a>
     * 
     * @param ecal
     * @param pdg Particle id as per PDG
     * @param rawEnergy Raw Energy of the cluster (sum of hits with shared hit distribution)
     * @param xpos
     * @param ypos
     * @param isMC
     * @return Corrected Energy
     */
    public static double computeCorrectedEnergy(HPSEcal3 ecal, int pdg, double rawEnergy, double xpos, double ypos,
            boolean isMC) {
        // distance to beam gap edge
        double r = ClusterCorrectionUtilities.computeYDistanceFromEdge(ecal,xpos,ypos);
        
        // Eliminates corrections at outermost edges to negative cluster energies
        // 66 for positrons, 69 is safe for electrons and photons
        if (r > 66) {
            r = 66;
        }

        if (isMC) {
            switch (pdg) {
                case 11:
                    // electron
                    return computeCorrectedEnergy(r, rawEnergy, par0MC_em, par1MC_em, par2MC_em);
                case -11:
                    // positron
                    return computeCorrectedEnergy(r, rawEnergy, par0MC_ep, par1MC_ep, par2MC_ep);
                case 22:
                    // photon
                    return computeCorrectedEnergy(r, rawEnergy, par0MC_p, par1MC_p, par2MC_p);
                default:
                    // unknown
                    return rawEnergy;
            }
        } else {
            switch (pdg) {
                case 11:
                    // electron
                    return computeCorrectedEnergy(r, rawEnergy, par0_em, par1_em, par2_em);
                case -11:
                    // positron
                    return computeCorrectedEnergy(r, rawEnergy, par0_ep, par1_ep, par2_ep);
                case 22:
                    // photon
                    return computeCorrectedEnergy(r, rawEnergy, par0_p, par1_p, par2_p);
                default:
                    // unknown
                    return rawEnergy;
            }
        }
    }

    /**
     * Calculates the energy correction to a cluster given the variables from the fit as per <a href=
     * "https://misportal.jlab.org/mis/physics/hps_notes/index.cfm?note_year=2014" >HPS Note 2014-001</a> Note that this
     * is correct as there is a typo in the formula print in the note.
     * 
     * @param rawEnergy Raw energy of the cluster
     * @param A,B,C from fitting in note
     * @return Corrected Energy
     */
    private static double computeCorrectedEnergy(double y, double rawEnergy, double varA, double varB[], double varC[]) {
        int ii = y < varB[0] ? 2 : 5;
        double corrEnergy = rawEnergy
                / (varA / rawEnergy + (varB[1] - varB[ii] * Math.exp(-(y - varB[ii + 1]) * varB[ii + 2]))
                        / (Math.sqrt(rawEnergy)) + (varC[1] - varC[ii] * Math.exp(-(y - varC[ii + 1]) * varC[ii + 2])));
        return corrEnergy;
    }
}

package org.hps.analysis.ecal;

import hep.physics.vec.Hep3Vector;

import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.Cluster;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;

/**
 * This code contains all of the calculations, as found in the 2015 data, for combining the Ecal energy and SVT
 * momentum.
 * 
 * @author Holly Szumila-Vance <hvanc001@odu.edu>
 **/
public class MassCalculator {

    // TODO: BEAMGAPTOP and BEAMGAPBOT need to come from detector definitions:
    static final double BEAMGAPTOP = 22.7;// mm
    static final double BEAMGAPBOT = -22.9;// mm
    static final double BEAMGAPTOPC = BEAMGAPTOP + 13.0;// mm
    static final double BEAMGAPBOTC = BEAMGAPBOT - 13.0;// mm
    static final double par[] = {35, 0.02871, -0.3046, -9.997, 0.1401, -0.00077, 55.09, -0.4651};

    /**
     * Calculates the y position of a cluster in the Ecal relative to the inner beam gap edge of the Ecal.
     * 
     * @param ix
     * @param y0
     * @return |yrel|
     */
    static double getY(int ix, double y0) {
        if (ix > -11 && ix < -1) {
            // if (x0<-113 || x0>49){
            if (y0 > 0) {
                if (y0 < 55) {
                    return Math.abs(y0 - BEAMGAPTOPC);
                } else {
                    return Math.abs(y0 - BEAMGAPTOP);
                }
            } else {
                if (y0 > -55) {
                    return Math.abs(y0 - BEAMGAPBOTC);
                } else {
                    return Math.abs(y0 - BEAMGAPBOT);
                }
            }
        } else {
            if (y0 > 0) {
                return Math.abs(y0 - BEAMGAPTOP);
            } else {
                return Math.abs(y0 - BEAMGAPBOT);
            }
        }
    }

    /**
     * Calculates the parameter, B, as a function of position for understanding the resolution of the energy in the
     * Ecal.
     * 
     * @param ypos
     * @return B
     */
    static double calcBpar(double ypos) {
        if (ypos > 3) {
            int ii = (ypos < par[0]) ? 2 : 5;
            return par[1] - par[ii] * Math.exp(-(ypos - par[ii + 1]) * par[ii + 2]);
        } else {
            return 0.2;
        }
    }

    /**
     * Calculates the energy resolution accounting for position relative to the edge in the Ecal/
     * 
     * @param energy
     * @param ix
     * @param ypos
     * @return sigma E
     */
    static double sigE(double energy, int ix, double ypos) {

        // return energy*(sqrt(pow(0.0165,2)/pow(energy,2)+pow(calcBpar(getY(ix,ypos)),2)/energy+pow(0.0181,2)));
        return energy
                * (Math.sqrt(Math.pow(0.0162, 2) / Math.pow(energy, 2) + Math.pow(calcBpar(getY(ix, ypos)), 2) / energy
                        + Math.pow(0.025, 2)));
    }

    /**
     * Calculates the momentum resolution. Fixed for 2015 data.
     * 
     * @param mom
     * @return sigma P
     */
    static double sigP(double mom) {

        return 0.067 * mom;

    }

    /**
     * Calculates the sigma of the E/P distribution in order to check if the cluster energy is in the peak or tail of
     * the distribution before combining.
     * 
     * @param energy
     * @param mom
     * @param ix
     * @param ypos
     * @return E/P sigma
     */
    static double sigmaFrac(double energy, double mom, int ix, double ypos) {

        return Math.sqrt(Math.pow(sigE(energy, ix, ypos) / energy, 2) + Math.pow(sigP(mom) / mom, 2));

    }

    /**
     * Calculates the cosine of the opening angle of the vertex
     * 
     * @param v0
     * @return cosine of the opening angle
     */
    static double cosTheta(ReconstructedParticle v0) {
        double px1 = v0.getStartVertex().getParameters().get("p1X");
        double py1 = v0.getStartVertex().getParameters().get("p1Y");
        double pz1 = v0.getStartVertex().getParameters().get("p1Z");
        double px2 = v0.getStartVertex().getParameters().get("p2X");
        double py2 = v0.getStartVertex().getParameters().get("p2Y");
        double pz2 = v0.getStartVertex().getParameters().get("p2Z");

        return (px1 * px2 + py1 * py2 + pz1 * pz2)
                / (Math.sqrt(Math.pow(px1, 2) + Math.pow(py1, 2) + Math.pow(pz1, 2)) * Math.sqrt(Math.pow(px2, 2)
                        + Math.pow(py2, 2) + Math.pow(pz2, 2)));
    }

    /**
     * This takes in a cluster, track, refit momentum and outputs the combined, weighted momentum if it satisfies
     * conditions.
     * 
     * @return combined momentum (GeV)
     */
    public static double combinedMomentum(Cluster cluster, Track track, double momentum) {
        double energy = cluster.getEnergy();
        TrackState trackState = track.getTrackStates().get(0);
        Hep3Vector posAtEcal = TrackUtils.getTrackPositionAtEcal(trackState);

        int ix = ClusterUtilities.findSeedHit(cluster).getIdentifierFieldValue("ix");
        double yraw = posAtEcal.y();
        double ypos = getY(ix, yraw);
        double sigmaE = sigE(energy, ix, ypos);
        double sigmaP = sigP(momentum);

        double ff = (1 - energy / momentum) / sigmaFrac(energy, momentum, ix, ypos);

        if (ff > -2 && ff < 1.8 && ypos > 8 && (energy / momentum) > 0.82 && (energy / momentum) < 1.2) {

            return (energy / (Math.pow(sigmaE, 2)) + momentum / Math.pow(sigmaP, 2))
                    / (1. / (Math.pow(sigmaE, 2)) + 1. / Math.pow(sigmaP, 2));

        } else {
            return momentum;
        }
    }

    /**
     * This takes in the clusters of two particles and the associated vertex and calculates the invariant mass. The
     * cluster order must match the track order as listed in the vertex.
     * 
     * @return weighted invariant mass (GeV)
     */
    public static double combinedMass(Cluster clusterA, Cluster clusterB, ReconstructedParticle v0) {
        double momentumA = Math.sqrt(Math.pow(v0.getStartVertex().getParameters().get("p1X"), 2)
                + Math.pow(v0.getStartVertex().getParameters().get("p1Y"), 2) + Math.pow(v0.getStartVertex().getParameters().get("p1Z"), 2));
        double momentumB = Math.sqrt(Math.pow(v0.getStartVertex().getParameters().get("p2X"), 2)
                + Math.pow(v0.getStartVertex().getParameters().get("p2Y"), 2) + Math.pow(v0.getStartVertex().getParameters().get("p2Z"), 2));
       
        double energyA = combinedMomentum(clusterA, v0.getParticles().get(0).getTracks().get(0), momentumA);
        double energyB = combinedMomentum(clusterB, v0.getParticles().get(1).getTracks().get(0), momentumB);

        double cosT = cosTheta(v0);
        double mass = Math.sqrt(2 * energyA * energyB * (1 - cosT));

        return mass;

    }
    
    /**
     * This takes in the clusters of two particles and the associated vertex and calculates the invariant mass. The
     * cluster order must match the track order as listed in the vertex.
     * 
     * Use if electron has no cluster
     * 
     * @return weighted invariant mass (GeV)
     */
    public static double combinedMass(Track trackA, Cluster clusterB, ReconstructedParticle v0) {
        double momentumA = Math.sqrt(Math.pow(v0.getStartVertex().getParameters().get("p1X"), 2)
                + Math.pow(v0.getStartVertex().getParameters().get("p1Y"), 2) + Math.pow(v0.getStartVertex().getParameters().get("p1Z"), 2));
        double momentumB = Math.sqrt(Math.pow(v0.getStartVertex().getParameters().get("p2X"), 2)
                + Math.pow(v0.getStartVertex().getParameters().get("p2Y"), 2) + Math.pow(v0.getStartVertex().getParameters().get("p2Z"), 2));
        double energyA = momentumA;
        double energyB = combinedMomentum(clusterB, v0.getParticles().get(1).getTracks().get(0), momentumB);

        double cosT = cosTheta(v0);
        double mass = Math.sqrt(2 * energyA * energyB * (1 - cosT));

        return mass;

    }
    /**
     * This takes in the clusters of two particles and the associated vertex and calculates the invariant mass. The
     * cluster order must match the track order as listed in the vertex.
     * 
     * Use if positron has no cluster
     * 
     * @return weighted invariant mass (GeV)
     */
    public static double combinedMass(Cluster clusterA, Track trackB, ReconstructedParticle v0) {
        double momentumA = Math.sqrt(Math.pow(v0.getStartVertex().getParameters().get("p1X"), 2)
                + Math.pow(v0.getStartVertex().getParameters().get("p1Y"), 2) + Math.pow(v0.getStartVertex().getParameters().get("p1Z"), 2));
        double momentumB = Math.sqrt(Math.pow(v0.getStartVertex().getParameters().get("p2X"), 2)
                + Math.pow(v0.getStartVertex().getParameters().get("p2Y"), 2) + Math.pow(v0.getStartVertex().getParameters().get("p2Z"), 2));
        double energyA = combinedMomentum(clusterA, v0.getParticles().get(0).getTracks().get(0), momentumA);
        double energyB = momentumB;

        double cosT = cosTheta(v0);
        double mass = Math.sqrt(2 * energyA * energyB * (1 - cosT));

        return mass;

    }

}

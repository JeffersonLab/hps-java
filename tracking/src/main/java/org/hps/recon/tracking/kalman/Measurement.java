package org.hps.recon.tracking.kalman;

import java.util.ArrayList;

import org.lcsim.event.MCParticle;

/**
 * Holds a single silicon-strip measurement (single-sided), to interface with the Kalman fit
 */
class Measurement { // 
    double v; // Measurement value in detector frame
    double x; // X of the center of the strip in the detector frame
    double sigma; // Measurement uncertainty
    double time;  // Time of the hit in ns
    double energy; // Energy deposited in the silicon
    double vTrue; // MC truth measurement value
    Vec rGlobal; // Global MC truth
    ArrayList<KalTrack> tracks;     // Tracks that this hit lies on
    ArrayList<Integer> tksMC;       // MC track IDs that contributed to this hit (for stand-alone test program)
    ArrayList<MCParticle> pMC;      // List of hps-java MCparticle objects that contributed to this hit

    /**
     * Constructor with no MC truth info stored
     */
    Measurement(double value, double xStrip, double resolution, double t, double E) {
        v = value;
        x = xStrip;
        sigma = resolution;
        time = t;
        energy = E;
        tracks = new ArrayList<KalTrack>();
        vTrue = 0.;
        rGlobal = null;
        tksMC = null;
        pMC = null;
    }
    
    /**
     * Full constructor, including MC truth
     * @param value            value of the measured coordinate on the detector coordinate system
     * @param xStrip           x value of the center of the strip in the detector coordinate system
     * @param resolution       uncertainty in v
     * @param t                measured time
     * @param E                measured energy deposit
     * @param rGlobal          global MC truth hit position
     * @param vTrue            MC truth measurement value
     */
    Measurement(double value, double xStrip, double resolution, double t, double E, Vec rGlobal, double vTrue) {
        v = value;
        x = xStrip;
        sigma = resolution;
        time = t;
        energy = E;
        this.rGlobal = rGlobal;
        this.vTrue = vTrue;
        tracks = new ArrayList<KalTrack>();
        tksMC = new ArrayList<Integer>();
        pMC = null;
    }
    
    /**
     * Add a MC truth track to the list
     * @param idx    index of the MC truth track
     */
    void addMC(int idx) {
        tksMC.add(idx);
    }

    /**
     * Debug printout of the measurement instance
     * @param s   Arbitrary string for the user's reference
     */
    void print(String s) {
        System.out.format("%s", toString(s));
    }
    
    /**
     * Debug printout to a string of the measurement instance
     * @param s   Arbitrary string for the user's reference
     */
    String toString(String s) {
        String str = String.format("Measurement %s: Measurement value=%10.5f+-%8.6f; xStrip=%7.2f, MC truth=%10.5f; t=%8.3f; E=%8.3f", s, v, sigma, x, vTrue, time, energy);
        if (tracks.size() == 0) {
            str = str + String.format("  Not on any track.\n");
        } else {
            str = str + String.format("  Tracks: ");
            for (KalTrack tk : tracks) str = str + String.format(" %d ", tk.ID);
            str = str + String.format("\n");
        }
        if (rGlobal != null) str = str + rGlobal.toString("global location from MC truth"); 
        if (pMC != null) {
            str = str + "\n";
            for (MCParticle mcp : pMC) {
                str = str + String.format("    MC particle type %d, E=%9.5f\n", mcp.getPDGID(),mcp.getEnergy());
            }
        }
        return str;
    }
}

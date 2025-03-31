package org.hps.recon.tracking.kalman;

import java.util.ArrayList;

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
    ArrayList<Integer> tksMC;       // MC tracks that contributed to this hit

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
    }
    
    Measurement(double value, double xStrip, double resolution, double t, double E, double terr) {
        v = value;
        x = xStrip;
        sigma = resolution;
        time = t;
        timeErr = terr;
        energy = E;
        tracks = new ArrayList<KalTrack>();
        vTrue = 0.;
        rGlobal = null;
        tksMC = null;
    }
    
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
    }
    
    void addMC(int idx) {
        tksMC.add(idx);
    }

    void print(String s) {
        System.out.format("Measurement %s: Measurement value=%10.5f+-%8.6f; xStrip=%7.2f, MC truth=%10.5f; t=%8.3f; E=%8.3f", s, v, sigma, x, vTrue, time, energy);
        if (tracks.size() == 0) {
            System.out.format("  Not on any track.\n");
        } else {
            System.out.format("  Tracks: ");
            for (KalTrack tk : tracks) System.out.format(" %d ", tk.ID);
            System.out.format("\n");
        }
        if (rGlobal != null) rGlobal.print("global location from MC truth"); 
    }
    
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
        return str;
    }
}

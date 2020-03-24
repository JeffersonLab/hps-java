package org.hps.recon.tracking.kalman;

import java.util.ArrayList;

class Measurement { // Holds a single silicon-strip measurement (single-sided), to interface with
                    // the Kalman fit
    double v; // Measurement value in detector frame
    double sigma; // Measurement uncertainty
    double time;  // Time of the hit in ns
    double vTrue; // MC truth measurement value
    Vec rGlobal; // Global MC truth
    ArrayList<KalTrack> tracks;     // Tracks that this hit lies on
    ArrayList<Integer> tksMC;       // MC tracks that contributed to this hit

    Measurement(double value, double resolution, double t) {
        v = value;
        sigma = resolution;
        time = t;
        tracks = new ArrayList<KalTrack>();
        vTrue = 0.;
        rGlobal = null;
        tksMC = null;
    }
    
    Measurement(double value, double resolution, double t, Vec rGlobal, double vTrue) {
        v = value;
        sigma = resolution;
        time = t;
        this.rGlobal = rGlobal;
        this.vTrue = vTrue;
        tracks = new ArrayList<KalTrack>();
        tksMC = new ArrayList<Integer>();
    }
    
    void addMC(int idx) {
        tksMC.add(idx);
    }

    void print(String s) {
        System.out.format("Measurement %s: Measurement value=%10.5f+-%8.6f;  MC truth=%10.5f; t=%8.3f", s, v, sigma, vTrue, time);
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
        String str = String.format("Measurement %s: Measurement value=%10.5f+-%8.6f;  MC truth=%10.5f; t=%8.3f", s, v, sigma, vTrue, time);
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

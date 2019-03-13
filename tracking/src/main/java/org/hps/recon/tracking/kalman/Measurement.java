package org.hps.recon.tracking.kalman;

import java.util.ArrayList;

class Measurement { // Holds a single silicon-strip measurement (single-sided), to interface with
                    // the Kalman fit
    double v; // Measurement value in detector frame
    double sigma; // Measurement uncertainty
    double vTrue; // MC truth measurement value
    Vec rGlobal; // Global MC truth
    ArrayList<KalTrack> tracks; // Tracks that this hit lies on

    Measurement(double value, double resolution, Vec rGlobal, double vTrue) {
        v = value;
        sigma = resolution;
        this.rGlobal = rGlobal;
        this.vTrue = vTrue;
        tracks = new ArrayList<KalTrack>();
    }

    void print(String s) {
        System.out.format("Measurement %s: Measurement value=%10.6f+-%10.6f;  MC truth=%10.6f\n", s, v, sigma, vTrue);
        rGlobal.print("global location from MC truth");
        if (tracks.size() == 0) {
            System.out.format("  This hit is not on any track.\n");
        } else {
            System.out.format("  List of tracks: ");
            for (KalTrack tk : tracks) {
                System.out.format(" %d ", tk.ID);
            }
            System.out.format("\n");
        }
    }
}

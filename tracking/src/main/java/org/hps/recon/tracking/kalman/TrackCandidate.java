package org.hps.recon.tracking.kalman;
// Used by KalmanPatRecHPS to store information for candidate tracks

import java.util.ArrayList;

public class TrackCandidate {
    ArrayList<MeasurementSite> sites;
    ArrayList<KalHit> hits;
    double chi2f, chi2s;
    int nHits;
    int nTaken;
    boolean filtered;
    boolean smoothed;

    TrackCandidate() {
        filtered = false;
        smoothed = false;
        nHits = 0;
        chi2f = 0.;
        chi2s = 0.;
        nTaken = 0;
        hits = new ArrayList<KalHit>(12);
    }

    void print(String s, boolean shrt) {
        System.out.format("TrackCandidate %s, nHits=%d, nShared=%d, chi2f=%10.6f, chi2s=%10.6f\n", s, nHits, nTaken, chi2f, chi2s);
        System.out.format("   %d Hits: ", hits.size());
        for (KalHit ht : hits) { ht.print("short"); }
        if (shrt) {
            System.out.format("\n");
        } else {
            System.out.format("   Site list:\n");
            for (MeasurementSite site : sites) {
                SiModule m = site.m;
                StateVector aF = site.aF;
                double phiF = aF.planeIntersect(m.p);
                if (Double.isNaN(phiF)) { phiF = 0.; }
                double vPred = site.h(aF, site.m, phiF);
                int cnt = sites.indexOf(site);
                double resid;
                if (site.hitID < 0) resid = 99.;
                else resid = vPred - m.hits.get(site.hitID).v;
                System.out.format("   %d Lyr %d stereo=%5.2f Hit %d chi2inc=%10.6f, resid=%10.6f, vPred=%10.6f; Hits: ", cnt, m.Layer, m.stereo,
                        site.hitID, site.chi2inc, resid, vPred);
                for (Measurement hit : m.hits) { System.out.format(" v=%10.6f #tks=%d,", hit.v, hit.tracks.size()); }
                System.out.format("\n");
            }
        }
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TrackCandidate)) return false;
        TrackCandidate o = (TrackCandidate) other;

        if (this.hits.size() != o.hits.size()) return false;

        for (int i = 0; i < hits.size(); ++i) {
            KalHit ht1 = this.hits.get(i);
            KalHit ht2 = o.hits.get(i);
            if (ht1.module.Layer != ht2.module.Layer) return false;
            if (ht1.module.detector != ht2.module.detector) return false;
            if (ht1.module.hits.indexOf(ht1.hit) != ht2.module.hits.indexOf(ht2.hit)) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return nHits;
    }
}

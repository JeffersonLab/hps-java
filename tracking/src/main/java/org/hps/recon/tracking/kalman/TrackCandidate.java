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

    boolean reFit(boolean verbose) {

        if (verbose) System.out.format("TrackCandidate.reFit: starting filtering.\n");

        StateVector sH = sites.get(0).aS;
        sH.C.scale(1000.*chi2s); // Blow up the initial covariance matrix to avoid double counting measurements
        SiModule prevMod = null;
        chi2f = 0.;
        for (int idx = 0; idx < sites.size(); idx++) { // Redo all the filter steps
            MeasurementSite currentSite = sites.get(idx);
            currentSite.predicted = false;
            currentSite.filtered = false;
            currentSite.smoothed = false;
            currentSite.chi2inc = 0.;
            currentSite.aP = null;
            currentSite.aF = null;
            currentSite.aS = null;

            if (currentSite.makePrediction(sH, prevMod, currentSite.hitID, false, false, false, verbose) < 0) {
                if (verbose) System.out.format("TrackCandidate.reFit: failed to make prediction!!\n");
                return false;
            }
            if (!currentSite.filter()) {
                if (verbose) System.out.format("TrackCandidate.reFit: failed to filter!!\n");
                return false;
            }

            // if (verbose) currentSite.print("iterating filtering");
            chi2f += Math.max(currentSite.chi2inc,0.);
            sH = currentSite.aF;
            prevMod = currentSite.m;
        }
        if (verbose) System.out.format("TrackCandidate.reFit: Fit chi^2 after filtering = %12.4e\n", chi2f);
        chi2s = 0.;
        MeasurementSite nextSite = null;
        for (int idx = sites.size() - 1; idx >= 0; idx--) {
            MeasurementSite currentSite = sites.get(idx);
            if (nextSite == null) {
                currentSite.aS = currentSite.aF.copy();
                currentSite.smoothed = true;
            } else {
                currentSite.smooth(nextSite);
            }
            chi2s += Math.max(currentSite.chi2inc,0.);

            // if (verbose) {
            // currentSite.print("iterating smoothing");
            // }
            nextSite = currentSite;
        }
        if (verbose) System.out.format("TrackCandidate.reFit: Fit chi^2 after smoothing = %12.4e\n", chi2s); 

        return true;       
    }
    
    void print(String s, boolean shrt) {
        System.out.format("TrackCandidate %s, nHits=%d, nShared=%d, chi2f=%10.6f, chi2s=%10.6f\n", s, nHits, nTaken, chi2f, chi2s);
        MeasurementSite site0 = sites.get(0);
        if (site0 != null) {
            int lyr = site0.m.Layer;
            StateVector aS = site0.aS;
            if (aS == null) aS = site0.aF;
            if (aS != null) {
                Vec p = aS.a;
                double edrho = Math.sqrt(aS.C.M[0][0]);
                double ephi0 = Math.sqrt(aS.C.M[1][1]);
                double eK = Math.sqrt(aS.C.M[2][2]);
                double eZ0 = Math.sqrt(aS.C.M[3][3]);
                double etanl = Math.sqrt(aS.C.M[4][4]);
                System.out.format("   Helix parameters at lyr %d= %10.5f+-%8.5f %10.5f+-%8.5f %10.5f+-%8.5f %10.5f+-%8.5f %10.5f+-%8.5f\n", lyr, 
                        p.v[0],edrho, p.v[1],ephi0, p.v[2],eK, p.v[3],eZ0, p.v[4],etanl);
            }
        }
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
                System.out.format("   %d Lyr %d stereo=%b Hit %d chi2inc=%10.6f, resid=%10.6f, vPred=%10.6f; Hits: ", cnt, m.Layer, m.isStereo,
                        site.hitID, site.chi2inc, resid, vPred);
                for (Measurement hit : m.hits) {
                    if (hit.vTrue == 999.) System.out.format(" (v=%10.6f #tks=%d),", hit.v, hit.tracks.size());
                    else System.out.format(" v=%10.6f #tks=%d,", hit.v, hit.tracks.size());                   
                }
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

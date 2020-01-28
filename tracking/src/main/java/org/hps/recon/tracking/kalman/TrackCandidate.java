package org.hps.recon.tracking.kalman;
// Used by KalmanPatRecHPS to store information for candidate tracks

import java.util.ArrayList;

public class TrackCandidate {
    ArrayList<MeasurementSite> sites;
    ArrayList<KalHit> hits;
    double chi2f, chi2s;
    int nHits;
    int nTaken;
    int mxShared;
    boolean filtered;
    boolean smoothed;

    TrackCandidate(int mxShared) {
        this.mxShared = mxShared;
        filtered = false;
        smoothed = false;
        nHits = 0;
        chi2f = 0.;
        chi2s = 0.;
        nTaken = 0;
        hits = new ArrayList<KalHit>(12);
    }

    void removeHit(KalHit hit) {
        MeasurementSite siteR = null;
        SiModule mod = hit.module;
        for (MeasurementSite site : sites) {
            if (site.m == mod) {
                siteR = site;
                if (chi2s > 0.) chi2s = chi2s - site.chi2inc;
                if (chi2f > 0.) chi2f = chi2f - site.chi2inc;
            }
        }
        if (siteR == null) {
            System.out.format("TrackCandidate.removeHit error: MeasurementSite is missing for layer %d\n", mod.Layer);
        } else {
            siteR.hitID = -1;
            sites.remove(siteR);
        }
        hits.remove(hit);
        nHits--;
    }
    
    boolean reFit(boolean verbose) {

        if (verbose) System.out.format("TrackCandidate.reFit: starting filtering.\n");

        StateVector sH = sites.get(0).aS;
        sH.C.scale(1000.*chi2s); // Blow up the initial covariance matrix to avoid double counting measurements
        SiModule prevMod = null;
        chi2f = 0.;
        nTaken = 0;
        nHits = 0;
        int nStereo = 0;
        filtered = false;
        smoothed = false;
        boolean failure = false;
        for (int idx = 0; idx < sites.size(); idx++) { // Redo all the filter steps
            MeasurementSite currentSite = sites.get(idx);
            currentSite.predicted = false;
            currentSite.filtered = false;
            currentSite.smoothed = false;
            currentSite.chi2inc = 0.;
            currentSite.aP = null;
            currentSite.aF = null;
            currentSite.aS = null;
            currentSite.hitID = -1;  // Allow hit reselection

            boolean allowSharing = nTaken < mxShared;
            boolean pickupHits = true;
            boolean checkBounds = false;
            if (currentSite.makePrediction(sH, prevMod, currentSite.hitID, allowSharing, pickupHits, checkBounds, verbose) < 0) {
                if (verbose) System.out.format("TrackCandidate.reFit: failed to make prediction at layer %d!!\n",currentSite.m.Layer);
                if (nStereo > 2 && nHits > 4) {
                    currentSite.hitID = 0;
                    failure = true;
                    break;
                }
                if (verbose) System.out.format("TrackCandidate.reFit: aboart refit, nHits=%d, nStereo=%d\n", nHits, nStereo);
                return false;
            }
            if (!currentSite.filter()) {
                if (verbose) System.out.format("TrackCandidate.reFit: failed to filter at layer %d!!\n",currentSite.m.Layer);
                if (nStereo > 2 && nHits > 4) {
                    currentSite.hitID = 0;
                    failure = true;
                    break;
                }
                if (verbose) System.out.format("TrackCandidate.reFit: aboart refit, nHits=%d, nStereo=%d\n", nHits, nStereo);
                return false;
            }

            // if (verbose) currentSite.print("iterating filtering");
            if (currentSite.hitID >= 0) {
                chi2f += Math.max(currentSite.chi2inc,0.);
                nHits++;
                if (currentSite.m.isStereo) nStereo++;
                if (currentSite.m.hits.get(currentSite.hitID).tracks.size() > 0) nTaken++;
                if (verbose) {
                    System.out.format("TrackCandidate.refit: adding hit %d from layer %d, detector %d, chi2inc=%10.4f\n",
                            currentSite.hitID,currentSite.m.Layer,currentSite.m.detector,currentSite.chi2inc);
                }
            }
            sH = currentSite.aF;
            prevMod = currentSite.m;
        }
        if (nStereo < 3 || nHits < 5) {
            if (verbose) System.out.format("TrackCandidate.reFit: not enough hits included in fit.  nHits=%d, nStereo=%d\n", nHits, nStereo);
            return false;
        }
        if (failure) {
            ArrayList<MeasurementSite> sitesToRemove = new ArrayList<MeasurementSite>();
            for (MeasurementSite site : sites) {
                if (site.aP == null || site.aF == null) {
                    sitesToRemove.add(site);
                }
            }
            for (MeasurementSite site : sitesToRemove) {
                if (verbose) System.out.format("TrackCandidate.refit: removing site at layer %d detector %d\n", site.m.Layer, site.m.detector);
                sites.remove(site);
            }
        }
        if (verbose) System.out.format("TrackCandidate.reFit: Fit chi^2 after filtering = %12.4e\n", chi2f);
        filtered = true;
        chi2s = 0.;
        MeasurementSite nextSite = null;
        for (int idx = sites.size() - 1; idx >= 0; idx--) {
            MeasurementSite currentSite = sites.get(idx);
            if (currentSite.aF == null || currentSite.hitID < 0) continue;
            if (verbose) System.out.format("TrackCandidate.reFit: smoothing site at layer %d detector %d\n", 
                    currentSite.m.Layer, currentSite.m.detector);
            if (nextSite == null) {
                currentSite.aS = currentSite.aF.copy();
                currentSite.smoothed = true;
            } else {
                currentSite.smooth(nextSite);
            }
            chi2s += Math.max(currentSite.chi2inc,0.);

            //if (verbose) {
            //    currentSite.print("iterating smoothing");
            //}
            nextSite = currentSite;
        }
        if (verbose) System.out.format("TrackCandidate.reFit: Fit chi^2 after smoothing = %12.4e\n", chi2s); 
        smoothed = true;
        return true;       
    }
    
    void print(String s, boolean shrt) {
        System.out.format("TrackCandidate %s, nHits=%d, nShared=%d, chi2f=%10.6f, chi2s=%10.6f\n", s, nHits, nTaken, chi2f, chi2s);
        MeasurementSite site0 = null;
        for (MeasurementSite site : sites) {
            if ((site.aS != null || site.aF != null) && site.hitID >= 0) {
                site0 = site;
                break;
            }
        }
        if (site0 == null) {
            System.out.format("    No hits or smoothed site found!\n");
            return;
        }
        int lyr = site0.m.Layer;
        StateVector aS = site0.aS;
        if (aS == null) aS = site0.aF;
        Vec p = aS.a;
        double edrho = Math.sqrt(aS.C.M[0][0]);
        double ephi0 = Math.sqrt(aS.C.M[1][1]);
        double eK = Math.sqrt(aS.C.M[2][2]);
        double eZ0 = Math.sqrt(aS.C.M[3][3]);
        double etanl = Math.sqrt(aS.C.M[4][4]);
        System.out.format("   Helix parameters at lyr %d= %10.5f+-%8.5f %10.5f+-%8.5f %10.5f+-%8.5f %10.5f+-%8.5f %10.5f+-%8.5f\n", lyr, 
                p.v[0],edrho, p.v[1],ephi0, p.v[2],eK, p.v[3],eZ0, p.v[4],etanl);
        System.out.format("   %d Hits: ", hits.size());
        for (KalHit ht : hits) { ht.print("short"); }
        if (shrt) {
            System.out.format("\n");
        } else {
            System.out.format("   Site list:\n");
            for (MeasurementSite site : sites) {
                SiModule m = site.m;
                StateVector aF = site.aF;
                if (aF == null) {
                    System.out.format("    Layer %d, detector %d, the filtered state vector is missing\n", site.m.Layer, site.m.detector);
                    site.print("messed up site");
                    continue;
                }
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

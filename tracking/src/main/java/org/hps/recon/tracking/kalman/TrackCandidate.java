package org.hps.recon.tracking.kalman;
// Used by KalmanPatRecHPS to store information for candidate tracks

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

class TrackCandidate {
    int ID;
    private Map<Measurement, KalHit> hitMap;
    ArrayList<MeasurementSite> sites;
    ArrayList<KalHit> hits;
    ArrayList<Integer> seedLyrs;
    double chi2f, chi2s;
    double tMin, tMax;
    int nTaken;
    private int mxShared;
    private double mxTdif;
    boolean filtered;
    boolean smoothed;
    boolean good;
    int kalTkrID;
    int eventNumber;

    TrackCandidate(int IDset, ArrayList<KalHit> seedHits, int mxShared, double mxTdif, Map<Measurement, KalHit> hitMap, int event) {
        ID = IDset;
        eventNumber = event;
        this.mxShared = mxShared;
        this.mxTdif = mxTdif;
        filtered = false;
        smoothed = false;
        chi2f = 0.;
        chi2s = 0.;
        nTaken = 0;
        hits = new ArrayList<KalHit>(12);
        seedLyrs = new ArrayList<Integer>(5);
        tMin = 1.e10;
        tMax = -1.e10;
        for (KalHit hit : seedHits) {
            seedLyrs.add(hit.module.Layer);
            hits.add(hit);
            if (hit.hit.tracks.size() > 0) nTaken++;
            tMin = Math.min(tMin, hit.hit.time);
            tMax = Math.max(tMax, hit.hit.time);
        }
        sites = new ArrayList<MeasurementSite>(12);
        good = true;
        this.hitMap = hitMap;
        kalTkrID = -1;
    }
    
    int numHits() {
        return hits.size();
    }
    
    int numStereo() {
        int nStereo = 0;
        for (KalHit ht : hits) {
            if (ht.module.isStereo) nStereo++;
        }
        return nStereo;
    }
    
    Vec originHelix() {
        MeasurementSite site0 = null;
        for (MeasurementSite site: sites) { // sites are assumed to be sorted
            if (site.hitID >= 0) {
                site0= site;
                break;
            }
        }
        return site0.aS.pivotTransform();
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
            System.out.format("TrackCandidate.removeHit error in event %d: MeasurementSite is missing for layer %d\n", eventNumber, mod.Layer);
        } else {
            siteR.hitID = -1;
            sites.remove(siteR);
        }
        if (kalTkrID == -1) {
            if (hit.hit.tracks.size() > 0) nTaken--;
        } else {
            if (hit.hit.tracks.size() > 1) nTaken--;
        }
        if (hit.hit.time <= tMin || hit.hit.time >= tMax) {
            tMin = tMax;
            tMax = tMin;
            for (KalHit ht : hits) {
                tMin = Math.min(tMin, ht.hit.time);
                tMax = Math.min(tMax, ht.hit.time);
            }
        }
        hits.remove(hit);
        hit.tkrCandidates.remove(this);
        int nstr = numStereo();
        int nax = numHits() - nstr;
        if (nstr < 3 || nax < 2) good = false;
    }
    
    boolean reFit(boolean verbose) {
        if (verbose) System.out.format("TrackCandidate.reFit: starting filtering for event %d.\n",eventNumber);

        boolean failure = false;
        StateVector sHstart = sites.get(0).aS;
        do {
            StateVector sH = sHstart;
            sH.C.scale(1000.*chi2s); // Blow up the initial covariance matrix to avoid double counting measurements
            
            SiModule prevMod = null;
            chi2f = 0.;
            nTaken = 0;
            int nStereo = 0;
            filtered = false;
            smoothed = false;
            failure = false;
            hits.clear();
            for (int idx = 0; idx < sites.size(); idx++) { // Redo all the filter steps
                MeasurementSite currentSite = sites.get(idx);
                currentSite.predicted = false;
                currentSite.filtered = false;
                currentSite.smoothed = false;
                currentSite.chi2inc = 0.;
                currentSite.aP = null;
                currentSite.aF = null;
                currentSite.aS = null;
                boolean pickupHits = false;
                if (!seedLyrs.contains(currentSite.m.Layer)) {  // Allow hit reselection only for non-seed layers
                    if (currentSite.hitID >= 0) {
                        KalHit oldHit = hitMap.get(currentSite.m.hits.get(currentSite.hitID));
                        oldHit.tkrCandidates.remove(this);
                        currentSite.hitID = -1;  
                    }
                    pickupHits = true;
                } 
    
                boolean allowSharing = nTaken < mxShared;
                boolean checkBounds = false;
                double [] tRange = {tMax - mxTdif, tMin + mxTdif}; 
                int rF = currentSite.makePrediction(sH, prevMod, currentSite.hitID, allowSharing, pickupHits, checkBounds, tRange, verbose);
                if (rF < 0) {
                    if (verbose) System.out.format("TrackCandidate.reFit: failed to make prediction at layer %d for event %d!\n",currentSite.m.Layer,eventNumber);
                    if (nStereo > 2 && hits.size() > 4) {
                        currentSite.hitID = 0;
                        failure = true;
                        break;
                    }
                    if (verbose) System.out.format("TrackCandidate.reFit: abort refit for event %d, nHits=%d, nStereo=%d\n", eventNumber, hits.size(), nStereo);
                    return false;
                } else if (rF == 1) {
                    if (currentSite.m.hits.get(currentSite.hitID).tracks.size() > 0) nTaken++;
                    tMin = Math.min(tMin, currentSite.m.hits.get(currentSite.hitID).time);
                    tMax = Math.max(tMax, currentSite.m.hits.get(currentSite.hitID).time);
                }
                if (!currentSite.filter(verbose)) {
                    if (verbose) System.out.format("TrackCandidate.reFit: failed to filter at layer %d in event %d!\n",currentSite.m.Layer, eventNumber);
                    if (nStereo > 2 && hits.size() > 4) {
                        currentSite.hitID = 0;
                        failure = true;
                        break;
                    }
                    if (verbose) System.out.format("TrackCandidate.reFit: abort refit for event %d, nHits=%d, nStereo=%d\n", eventNumber, hits.size(), nStereo);
                    return false;
                }
                if (currentSite.chi2inc >= chi2s) {
                    currentSite.filtered = false;
                    currentSite.aF = null;
                    currentSite.hitID = -1;
                    failure = true;
                    if (verbose) System.out.format("TrackCandidate.reFit: bad chi^2 %7.1f from filter at layer %d\n", currentSite.chi2inc, currentSite.m.Layer);
                    if (nStereo > 2 && hits.size() > 4) break;
                    continue;               
                }
    
                // if (verbose) currentSite.print("iterating filtering");
                if (currentSite.hitID >= 0) {
                    KalHit hitNew = hitMap.get(currentSite.m.hits.get(currentSite.hitID));
                    hits.add(hitNew);
                    //hitNew.tkrCandidates.add(this); don't do this; hits are marked later
                    chi2f += Math.max(currentSite.chi2inc,0.);
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
            if (nStereo < 3 || hits.size() < 5) {
                if (verbose) System.out.format("TrackCandidate.reFit event %d: not enough hits included in fit.  nHits=%d, nStereo=%d\n", eventNumber, hits.size(), nStereo);
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
                    if (site.hitID >= 0) {
                        KalHit theHit = hitMap.get(site.m.hits.get(site.hitID));
                        theHit.tkrCandidates.remove(this);
                    }
                }
            }
        } while (failure);
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
                currentSite.smooth(nextSite, verbose);
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
        if (good) {
            System.out.format("%d Good TrackCandidate %s for event %d, nHits=%d, nShared=%d, chi2f=%10.6f, chi2s=%10.6f, t=%5.1f to %5.1f\n", 
                    ID, s, eventNumber, hits.size(), nTaken, chi2f, chi2s, tMin, tMax);
        } else {
            System.out.format("%d Bad TrackCandidate %s for event %d, nHits=%d, nShared=%d, chi2f=%10.6f, chi2s=%10.6f, t=%5.1f to %5.1f\n", 
                    ID, s, eventNumber, hits.size(), nTaken, chi2f, chi2s, tMin, tMax);
        }
        MeasurementSite site0 = null;
        for (MeasurementSite site : sites) {
            if ((site.aS != null || site.aF != null) && site.hitID >= 0) {
                site0 = site;
                break;
            }
        }
        if (site0 == null) {
            System.out.format("    No hits or smoothed or filtered site found!\n");
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
        for (KalHit ht : hits) ht.print("short");
        if (shrt) {
            System.out.format("\n");
            System.out.format("   Site list: ");
            for (MeasurementSite site : sites) {
                System.out.format("(%d, %d, %d) ",site.m.Layer,site.m.detector,site.hitID);
            }
            System.out.format("\n");
        } else {
            System.out.format("   Site list:\n");
            for (MeasurementSite site : sites) {
                SiModule m = site.m;
                StateVector aF = null;
                String S = "f";
                if (site.aS == null) aF = site.aF;
                else {
                    S = "s";
                    aF = site.aS;
                }
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
                System.out.format("   %d Lyr=%d det=%d %s Hit=%d stereo=%b  chi2inc=%10.6f, resid=%10.6f, vPred=%10.6f; Hits: ", 
                        cnt, m.Layer, m.detector, S, site.hitID, m.isStereo, site.chi2inc, resid, vPred);
                for (Measurement hit : m.hits) {
                    if (hit.vTrue == 999.) System.out.format(" (v=%10.6f #tks=%d),", hit.v, hit.tracks.size());
                    else System.out.format(" v=%10.6f #tks=%d,", hit.v, hit.tracks.size());                   
                }
                System.out.format("\n");
            }
        }
    }
    
    // Comparator function for sorting track candidates by quality
    static Comparator<TrackCandidate> CandidateComparator = new Comparator<TrackCandidate>() {
        public int compare(TrackCandidate t1, TrackCandidate t2) {
            double p1 = 0.;
            if (!t1.good) p1 = 9.9e6;
            double p2 = 0.;
            if (!t2.good) p2 = 9.9e6; 
            
            double chi1 = t1.chi2s / t1.hits.size() + 10.*(1.0 - (double)t1.hits.size()/12.) + p1;
            double chi2 = t2.chi2s / t2.hits.size() + 10.*(1.0 - (double)t2.hits.size()/12.) + p2;
            if (chi1 < chi2) {
                return -1;
            } else {
                return +1;
            }
        }
    };

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TrackCandidate)) return false;
        TrackCandidate o = (TrackCandidate) other;

        if (this.hits.size() != o.hits.size()) return false;

        for (KalHit ht1 : this.hits) {
            if (!o.hits.contains(ht1)) return false;
        }
        return true;
    }

//    @Override
//    public int hashCode() {
//        return hits.size();
//    }
}

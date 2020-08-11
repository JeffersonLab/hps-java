package org.hps.recon.tracking.kalman;
// Used by KalmanPatRecHPS to store information for candidate tracks

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

class TrackCandidate {
    int ID;
    private Map<Measurement, KalHit> hitMap;
    ArrayList<MeasurementSite> sites;
    ArrayList<KalHit> hits;
    ArrayList<Integer> seedLyrs;
    double chi2f, chi2s;
    double tMin, tMax;
    int nTaken;
    private KalmanParams kPar;
    boolean filtered;
    boolean smoothed;
    boolean good;
    int kalTkrID;
    int eventNumber;
    private Logger logger;

    TrackCandidate(int IDset, ArrayList<KalHit> seedHits, KalmanParams kPar, Map<Measurement, KalHit> hitMap, int event) {
        logger = Logger.getLogger(TrackCandidate.class.getName());
        ID = IDset;
        eventNumber = event;
        this.kPar = kPar;
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
    
    boolean contains(ArrayList<KalHit> hitList) {
        return hits.containsAll(hitList);
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
        return site0.aS.helix.pivotTransform();
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
            logger.log(Level.WARNING, String.format("TrackCandidate.removeHit error in event %d: MeasurementSite is missing for layer %d\n", 
                    eventNumber, mod.Layer));
        } else {
            siteR.hitID = -1;
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
    
    boolean reFit() {
        boolean verbose = (logger.getLevel()==Level.FINER || logger.getLevel()==Level.FINEST);
        if (verbose) System.out.format("TrackCandidate.reFit: starting filtering for event %d.\n",eventNumber);

        boolean failure = false;
        MeasurementSite startSite = sites.get(0);
        StateVector sHstart = startSite.aS;
        if (sHstart == null) sHstart = startSite.aF;
        if (sHstart == null) return false;
        if (verbose) System.out.format("  Start site at layer %d detector %d, helix=%s\n", 
                                       startSite.m.Layer, startSite.m.detector, sHstart.helix.a.toString());
        do {
            StateVector sH = sHstart;
            sH.helix.C.scale(100.); // Blow up the initial covariance matrix to avoid double counting measurements
            
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
    
                boolean allowSharing = nTaken < kPar.mxShared;
                boolean checkBounds = false;
                double [] tRange = {tMax - kPar.mxTdif, tMin + kPar.mxTdif}; 
                int rF = currentSite.makePrediction(sH, prevMod, currentSite.hitID, allowSharing, pickupHits, checkBounds, tRange);
                if (rF < 0) {
                    if (verbose) System.out.format("TrackCandidate.reFit: failed to make prediction at layer %d for event %d!\n",currentSite.m.Layer,eventNumber);
                    if (nStereo > 2 && hits.size() > 4) {
                        currentSite.hitID = -1;
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
                if (verbose) System.out.format("  After prediction to layer %d detector %d, hit=%d, helix=%s\n",
                        currentSite.m.Layer, currentSite.m.detector, currentSite.hitID, currentSite.aP.helix.a.toString());
                if (!currentSite.filter()) {
                    if (verbose) System.out.format("TrackCandidate.reFit: failed to filter at layer %d in event %d!\n",currentSite.m.Layer, eventNumber);
                    if (nStereo > 2 && hits.size() > 4) {
                        currentSite.hitID = -1;
                        failure = true;
                        break;
                    }
                    if (verbose) System.out.format("TrackCandidate.reFit: abort refit for event %d, nHits=%d, nStereo=%d\n", eventNumber, hits.size(), nStereo);
                    return false;
                }
                if (verbose) System.out.format("  After filtering at layer %d detector %d, resid=%9.5f, helix=%s\n",
                        currentSite.m.Layer, currentSite.m.detector, currentSite.aF.r, currentSite.aF.helix.a.toString());
                if (currentSite.chi2inc > 5.0*kPar.mxChi2Inc && currentSite.chi2inc > chi2f) {
                    currentSite.hitID = -1;
                    if (verbose) System.out.format("TrackCandidate.reFit: reject hit with really bad chi^2 %7.1f from filter at layer %d\n", currentSite.chi2inc, currentSite.m.Layer);           
                    currentSite.filtered = false;
                    currentSite.filter();
                }   
                if (currentSite.hitID >= 0) {
                    KalHit hitNew = hitMap.get(currentSite.m.hits.get(currentSite.hitID));
                    hits.add(hitNew);
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
            if (nStereo < kPar.minStereo[1] || hits.size() < kPar.minAxial) {
                if (verbose) System.out.format("TrackCandidate.reFit event %d: not enough hits included in fit.  nHits=%d, nStereo=%d\n", eventNumber, hits.size(), nStereo);
                return false;
            }
            if (failure) {  // Attempt to recover from failed prediction or filter by chopping off part of the track
                ArrayList<MeasurementSite> sitesToRemove = new ArrayList<MeasurementSite>();
                for (int idx=sites.size()-1; idx>=0; --idx) {  // Remove sites without hits or filtering at tail end only
                    MeasurementSite site = sites.get(idx);
                    if (site.hitID < 0 || site.aF == null) {
                        sitesToRemove.add(site);
                    } else {
                        break;
                    }
                }
                for (MeasurementSite site : sitesToRemove) {
                    if (verbose) System.out.format("TrackCandidate.refit: removing site at layer %d detector %d\n", site.m.Layer, site.m.detector);
                    sites.remove(site);
                    if (site.hitID >= 0) {
                        if (verbose) System.out.format("       TrackCandidate.refit: removing hit %d\n", site.hitID);
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
            //if (currentSite.aF == null || currentSite.hitID < 0) continue;
            if (nextSite == null) {
                currentSite.aS = currentSite.aF.copy();
                currentSite.smoothed = true;
            } else {
                currentSite.smooth(nextSite);
            }
            if (verbose) System.out.format("TrackCandidate.reFit: after smoothing site at layer %d detector %d, resid=%9.6f, helix=%s\n", 
                    currentSite.m.Layer, currentSite.m.detector, currentSite.aS.r, currentSite.aS.helix.a.toString());
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
        System.out.format("%s", this.toString(s, shrt));
    }
    
    String toString(String s, boolean shrt) {
        String str;
        if (good) {
            str = String.format("%d Good TrackCandidate %s for event %d, nHits=%d, nShared=%d, chi2f=%10.6f, chi2s=%10.6f, t=%5.1f to %5.1f\n", 
                    ID, s, eventNumber, hits.size(), nTaken, chi2f, chi2s, tMin, tMax);
        } else {
            str = String.format("%d Bad TrackCandidate %s for event %d, nHits=%d, nShared=%d, chi2f=%10.6f, chi2s=%10.6f, t=%5.1f to %5.1f\n", 
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
            str = str + "    No hits or smoothed or filtered site found!\n";
            return str;
        }
        int lyr = site0.m.Layer;
        StateVector aS = site0.aS;
        if (aS == null) aS = site0.aF;
        Vec p = aS.helix.a;
        double edrho = Math.sqrt(aS.helix.C.M[0][0]);
        double ephi0 = Math.sqrt(aS.helix.C.M[1][1]);
        double eK = Math.sqrt(aS.helix.C.M[2][2]);
        double eZ0 = Math.sqrt(aS.helix.C.M[3][3]);
        double etanl = Math.sqrt(aS.helix.C.M[4][4]);
        str=str+String.format("   Helix parameters at lyr %d= %10.5f+-%8.5f %10.5f+-%8.5f %10.5f+-%8.5f %10.5f+-%8.5f %10.5f+-%8.5f\n", lyr, 
                p.v[0],edrho, p.v[1],ephi0, p.v[2],eK, p.v[3],eZ0, p.v[4],etanl);
        str=str+String.format("              for origin at %s and pivot=%s\n", aS.helix.origin.toString(), aS.helix.X0.toString());
        str=str+String.format("   %d Hits: ", hits.size());
        for (KalHit ht : hits) str = str + ht.toString("short");
        if (shrt) {
            str = str + "\n";
            str=str+String.format("   Site list: ");
            for (MeasurementSite site : sites) {
                str=str+String.format("(%d, %d, %d) ",site.m.Layer,site.m.detector,site.hitID);
            }
            str = str + "\n";
        } else {
            str=str+String.format("   Site list:\n");
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
                    str=str+String.format("    Layer %d, detector %d, the filtered state vector is missing\n", site.m.Layer, site.m.detector);
                    str = str + site.toString("messed up site");
                    continue;
                }
                double phiF = aF.helix.planeIntersect(m.p);
                if (Double.isNaN(phiF)) { phiF = 0.; }
                double vPred = site.h(aF, site.m, phiF);
                int cnt = sites.indexOf(site);
                double resid;
                if (site.hitID < 0) resid = 99.;
                else resid = vPred - m.hits.get(site.hitID).v;
                str=str+String.format("   %d Lyr=%d det=%d %s Hit=%d stereo=%b  chi2inc=%10.6f, resid=%10.6f, vPred=%10.6f; Hits: ", 
                        cnt, m.Layer, m.detector, S, site.hitID, m.isStereo, site.chi2inc, resid, vPred);
                for (Measurement hit : m.hits) {
                    if (hit.vTrue == 999.) str=str+String.format(" (v=%10.6f #tks=%d),", hit.v, hit.tracks.size());
                    else str=str+String.format(" v=%10.6f #tks=%d,", hit.v, hit.tracks.size());                   
                }
                str = str + "\n";
            }
        }
        return str;
    }
    
    // Comparator function for sorting track candidates by quality
    static Comparator<TrackCandidate> CandidateComparator = new Comparator<TrackCandidate>() {
        public int compare(TrackCandidate t1, TrackCandidate t2) {
            double p1 = 0.;
            if (!t1.good) p1 = 9.9e6;
            double p2 = 0.;
            if (!t2.good) p2 = 9.9e6; 
            
            Double chi1 = new Double(t1.chi2s / t1.hits.size() + 10.*(1.0 - (double)t1.hits.size()/12.) + p1);
            Double chi2 = new Double(t2.chi2s / t2.hits.size() + 10.*(1.0 - (double)t2.hits.size()/12.) + p2);
            
            return chi1.compareTo(chi2);
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

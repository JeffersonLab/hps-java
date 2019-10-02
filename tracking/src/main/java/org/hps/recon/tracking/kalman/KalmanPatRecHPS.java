package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.Collections;

// Tracking pattern recognition for the HPS experiment based on an extended Kalman filter
// Algorithm:
//    1. Loop over starting strategies in the downstream layers, each involving at least 3 stereo planes and 2 non-stereo planes
//    2. Fit the starting planes to a line and parabola (or circle?) assuming a uniform field (average field in the starting region)
//    3. Run a Kalman filter back to the first plane
//    4. Use the fit at the first plane to restart the filter going out to the last plane, selecting or reselecting hits on the way.
//    5. Smooth back to the first plane. Test each hit on the way and eliminate if no good. Select a better hit if appropriate.  
//    6. Run a new Kalman fit and smoothing over all assigned hits starting the filter from the first layer and working downstream 
//    7. Extrapolate the smoothed helix to the vertex and to the Ecal
//    8. Remove hits used in successful fits before going on to the next starting strategy

// Allow picking up used hits if they fit really well
// Final arbitration of hit sharing:
//     loop over shared hits after finishing all strategies
//        join the tracks if the hit is at the ends of the two tracks
//        give the hit to the closest track, unless it fits perfectly to both
//        refit tracks from which hits were removed

public class KalmanPatRecHPS {

    public ArrayList<KalTrack> TkrList; // Final good tracks
    private ArrayList<KalTrack> TkrList2; // Temporary storage of substandard tracks
    private ArrayList<ArrayList<KalHit>> lyrHits;
    private ArrayList<ArrayList<SiModule>> moduleList;
    private ArrayList<int[]> lyrList;

    private static final int nTries = 2;  // Number of iterations through the entire pattern recognition
    private int nIterations; 
    private double[] kMax; 
    private double[] tanlMax; 
    private double[] dRhoMax; 
    private double[] chi2mx1; 
    private double[] chi2mx2; 
    private int[] minHits1; 
    private int[] minHits2; 
    private double[] mxChi2Inc; 
    private double[] mxResid; 
    private int minUnique; 
    private double mxResidShare; 
    private double mxChi2double;
    private int mxShared; 
    private boolean verbose;
    private int nModules;
    private int minStereo;
    private int minAxial;

    public KalmanPatRecHPS(ArrayList<SiModule> data, int eventNumber, boolean verbose) {

        this.verbose = verbose;
        TkrList = new ArrayList<KalTrack>();
        int tkID = 0;
        nModules = data.size();

        // Make a list of hits for each tracker layer, 0 through 13
        // This is needed because one layer can have multiple SiModules
        // Also make a list of Si modules with hits in each layer
        int numLayers = 14;
        int firstLayer = 2; // (2 for pre-2019 data)
        lyrHits = new ArrayList<ArrayList<KalHit>>(numLayers);
        moduleList = new ArrayList<ArrayList<SiModule>>(numLayers);
        for (int lyr = 0; lyr < numLayers; lyr++) {
            ArrayList<KalHit> hits = new ArrayList<KalHit>();
            ArrayList<SiModule> modules = new ArrayList<SiModule>();
            lyrHits.add(hits);
            moduleList.add(modules);
        }
        for (SiModule thisSi : data) {
            for (Measurement m : thisSi.hits) {
                KalHit hit = new KalHit(thisSi, m);
                lyrHits.get(thisSi.Layer).add(hit);
            }
            if (thisSi.hits.size() > 0) { moduleList.get(thisSi.Layer).add(thisSi); }
        }

        lyrList = new ArrayList<int[]>(13); // Array of seed strategies
        // Each list should contain 3 stereo layers and 2 non-stereo layers
        int[] list0 = {6, 7, 8, 9, 10};
        int[] list1 = {4, 5, 6, 7, 8};
        int[] list2 = {5, 6, 8, 9, 10};
        int[] list3 = {5, 6, 7, 8, 10};
        int[] list4 = { 3, 6, 8, 9, 10 };
        int[] list5 = { 4, 5, 8, 9, 10 };
        int[] list6 = { 4, 6, 7, 8, 9 };
        int[] list7 = { 4, 6, 7, 9, 10 };
        int[] list8 = { 2, 5, 8, 9, 12};
        int[] list9 = { 8, 10, 11, 12, 13};
        int[] list10 = {6, 9, 10, 11, 12};
        int[] list11 = {6, 7, 9, 10, 12};
        int[] list12 = {2, 3, 4, 5, 6};
        lyrList.add(list0);
        lyrList.add(list1);
        lyrList.add(list2);
        lyrList.add(list3);
        lyrList.add(list4);
        lyrList.add(list5);
        lyrList.add(list6);
        lyrList.add(list7);
        lyrList.add(list8);
        lyrList.add(list9);
        lyrList.add(list10);
        lyrList.add(list11);
        lyrList.add(list12);

        kMax = new double[nTries];
        tanlMax = new double[nTries];
        dRhoMax = new double[nTries];
        chi2mx1 = new double[nTries];
        chi2mx2 = new double[nTries];
        minHits1 = new int[nTries];
        minHits2 = new int[nTries];
        mxChi2Inc = new double[nTries];
        mxResid = new double[nTries];
        
        // Cut and parameter values (length units are mm).
        // The index is the iteration number.
        // The second iteration generally will have looser cuts.

        nIterations = 2;    // Number of Kalman filter iterations per track
        kMax[0] = 3.0;      // Maximum curvature for seed
        kMax[1] = 6.0;      
        tanlMax[0] = 0.08;  // Maximum tan(lambda) for seed
        tanlMax[1] = 0.12;
        dRhoMax[0] = 15.;   // Maximum dRho at target plane for seed
        dRhoMax[1] = 25.;
        chi2mx1[0] = 8.0;   // Maximum chi**2/#hits for good track
        chi2mx1[1] = 16.0;  
        chi2mx2[0] = 50.0;  // Maximum chi**2/#hits for a substandard track
        chi2mx2[1] = 100.;
        minHits1[0] = 9;    // Minimum number of hits for a good track
        minHits1[1] = 6;
        minHits2[0] = 6;    // Minimum number of hits for a substandard track
        minHits2[1] = 5;    
        mxChi2Inc[0] = 10.; // Maximum increment to the chi^2 for a hit during smoothing
        mxChi2Inc[1] = 20.;
        mxResid[0] = 75.;   // Maximum residual, in units of detector resolution, for picking up a hit
        mxResid[1] = 100.;
        minUnique = 3;      // Minimum number of unused hits on a seed
        mxResidShare = 6.;  // Maximum residual, in units of detector resolution, for a hit to be shared
        mxChi2double = 6.;
        minStereo = 3;      // Minimum number of stereo hits
        minAxial = 2;       // Minimum number of axial hits
        mxShared = 2;       // Maximum number of shared hits
        
        Plane p0 = new Plane(new Vec(0., 0., 0.), new Vec(0., 1., 0.));

        if (verbose) System.out.format("Entering KalmanPatRecHPS with %d modules, for %d trials.\n", nModules, nTries);

        // Loop over seed strategies, each with 2 non-stereo layers and 3 stereo layers
        // For each strategy generate a seed track for every hit combination
        // Keep only those pointing more-or-less back to the origin and not too curved
        // Sort the list first by curvature, then by drho
        for (int trial = 0; trial < nTries; trial++) {
            if (verbose) { System.out.format("\nKalmanPatRecHPS: start of pass %d through the algorithm.\n", trial); }
            ArrayList<TrackCandidate> candidateList = new ArrayList<TrackCandidate>();
            TkrList2 = new ArrayList<KalTrack>();
            for (int[] list : lyrList) {
                int nLyrs = list.length;

                SiModule m0 = data.get(list[4]);
                double yOrigin = m0.p.X().v[1];
                Vec pivot = new Vec(0, yOrigin, 0.);
                Vec Bfield = KalmanInterface.getField(pivot, m0.Bfield);
                double Bmag = Bfield.mag();
                Vec tB = Bfield.unitVec(Bmag);
                if (verbose) {
                    System.out.format("\n\nKalmanPatRecHPS: layer list=%d %d %d %d %d\n", list[0], list[1], list[2], list[3], list[4]);
                    System.out.format("KalmanPatRecHPS: yOrigin=%10.6f\n", yOrigin);
                }
                ArrayList<SeedTrack> seedList = new ArrayList<SeedTrack>();
                int[] idx = new int[nLyrs];
                for (idx[0] = 0; idx[0] < lyrHits.get(list[0]).size(); idx[0]++) {
                    if (lyrHits.get(list[0]).get(idx[0]).hit.tracks.size() > 0) continue; // don't use hits already on tracks
                    for (idx[1] = 0; idx[1] < lyrHits.get(list[1]).size(); idx[1]++) {
                        if (lyrHits.get(list[1]).get(idx[1]).hit.tracks.size() > 0) continue;
                        for (idx[2] = 0; idx[2] < lyrHits.get(list[2]).size(); idx[2]++) {
                            if (lyrHits.get(list[2]).get(idx[2]).hit.tracks.size() > 0) continue;
                            for (idx[3] = 0; idx[3] < lyrHits.get(list[3]).size(); idx[3]++) {
                                if (lyrHits.get(list[3]).get(idx[3]).hit.tracks.size() > 0) continue;
                                for (idx[4] = 0; idx[4] < lyrHits.get(list[4]).size(); idx[4]++) {
                                    if (lyrHits.get(list[4]).get(idx[4]).hit.tracks.size() > 0) continue;
                                    ArrayList<int[]> hitList = new ArrayList<int[]>(nLyrs);
                                    for (int i = 0; i < nLyrs; i++) {
                                        int[] elm = new int[2];
                                        elm[0] = data.indexOf(lyrHits.get(list[i]).get(idx[i]).module);
                                        elm[1] = lyrHits.get(list[i]).get(idx[i]).module.hits.indexOf(lyrHits.get(list[i]).get(idx[i]).hit);
                                        hitList.add(elm);
                                    }
                                    SeedTrack seed = new SeedTrack(data, yOrigin, hitList, false);
                                    if (!seed.success) continue;
                                    // Cuts on the seed quality
                                    Vec hp = seed.helixParams();
                                    if (verbose) {
                                        System.out.format("Seed %d %d %d %d %d parameteters for cuts: K=%10.5f, tanl=%10.5f, dxz=%10.5f   ",
                                                idx[0], idx[1], idx[2], idx[3], idx[4], hp.v[2], hp.v[4], seed.planeIntersection(p0).mag());
                                    }
                                    if (Math.abs(hp.v[2]) < kMax[trial]) {
                                        if (Math.abs(hp.v[4]) < tanlMax[trial]) {
                                            Vec pInt = seed.planeIntersection(p0);
                                            if (verbose) System.out.format("intersection with target plane= %9.3f %9.3f %9.3f", pInt.v[0],
                                                    pInt.v[1], pInt.v[2]);
                                            if (pInt.mag() < dRhoMax[trial]) { seedList.add(seed); }
                                        }
                                    }
                                    if (verbose) System.out.format("\n");
                                }
                            }
                        }
                    }
                }
                // Sort all of the seeds by distance from origin in x,z plane
                Collections.sort(seedList, SeedTrack.dRhoComparator);
                if (verbose) {
                    int cnt = 0;
                    for (SeedTrack seed : seedList) {
                        System.out.format("\nSorted seed %d", cnt);
                        for (KalHit ht : seed.hits) {
                            System.out.format(" (%d,%d,%d)", ht.module.Layer, ht.module.detector, ht.module.hits.indexOf(ht.hit));
                        }
                        System.out.format("\n");
                        seed.print("sorted seeds");
                        cnt++;
                    }
                }

                // Kalman filter the sorted seeds
                seedLoop: for (SeedTrack seed : seedList) {
                    if (verbose) {
                        System.out.format("\n\nStart the filter step for seed");
                        for (KalHit ht : seed.hits) {
                            System.out.format(" (%d,%d,%d)", ht.module.Layer, ht.module.detector, ht.module.hits.indexOf(ht.hit));
                        }
                        System.out.format("\n");
                    }
                    SquareMatrix CovGuess = seed.covariance();
                    CovGuess.scale(1000.);
                    // Create an state vector from the input seed to initialize the Kalman filter
                    StateVector sI = new StateVector(-1, seed.helixParams(), CovGuess, new Vec(0., 0., 0.), Bmag, tB, pivot, false);
                    TrackCandidate candidateTrack = filterTrack(list[0], numLayers - 1, sI, seed.hits, trial);
                    if (!candidateTrack.filtered) {
                        if (verbose) {
                            System.out.format("KalmanPatRecHPS: filtering of this seed failed. chi2=%10.5f, nHits=%d\n", candidateTrack.chi2f,
                                    candidateTrack.nHits);
                        }
                        continue;
                    }

                    if (verbose) {
                        candidateTrack.print("after initial filtering", false);
                        System.out.format("Hits after initial filtering= ");
                        for (int i = 0; i < numLayers; i++) {
                            int lHit = -1;
                            for (KalHit ht : candidateTrack.hits) {
                                if (ht.module.Layer == i) lHit = ht.module.hits.indexOf(ht.hit);
                            }
                            System.out.format("%2d ", lHit);
                        }
                        System.out.format("\n");
                    }

                    if (candidateTrack.sites.size() < minHits2[trial]) {
                        if (verbose) System.out.format("KalmanPatRecHPS: Initial filtering has too few sites, n=%d<%d. Skip to the next seed.\n",
                                candidateTrack.sites.size(), minHits2[trial]);
                        continue;                        
                    }
                    if (candidateTrack.nHits < minHits2[trial]) {
                        if (verbose) System.out.format("KalmanPatRecHPS: Initial filtering has too few hits, n=%d<%d. Skip to the next seed.\n",
                                candidateTrack.nHits, minHits2[trial]);
                        continue;
                    }
                    if (candidateTrack.chi2f / (double) candidateTrack.nHits > chi2mx2[trial]) {
                        if (verbose) System.out.format("KalmanPatRecHPS: Initial filtering has too large chi^2. Skip to the next seed.\n");
                        continue;
                    }

                    // Now smooth back to the original point
                    smoothTrack(candidateTrack, false, trial, 0);
                    if (verbose) System.out.format("\nKalmanPatRecHPS: Smoothed chi2=%10.5f\n", candidateTrack.chi2s);

                    // Then filter toward the target if there are any more untried layers there
                    boolean inSuccess = false;
                    TrackCandidate candidateTrackIn = null;
                    if (candidateTrack.sites.get(0).m.Layer > firstLayer) {
                        candidateTrackIn = filterTrack(candidateTrack.sites.get(0).m.Layer - 1, firstLayer, candidateTrack.sites.get(0).aS,
                                candidateTrack.hits, trial);
                        if (candidateTrackIn.filtered && candidateTrackIn.sites.size()>0) {
                            candidateTrackIn.chi2s = candidateTrack.chi2s;  // Save smoothed chi^2 from previous fit, for later comparison
                            if (verbose) {
                                candidateTrack.print("after filtering inward", false);
                                System.out.format("Hits after filtering to layer 2: ");
                                for (int i = 0; i < numLayers; i++) {
                                    int lHit = -1;
                                    for (KalHit ht : candidateTrack.hits) { 
                                        if (ht.module.Layer == i) lHit = ht.module.hits.indexOf(ht.hit);
                                    }
                                    System.out.format("%2d ", lHit);
                                }
                                System.out.format("\n");
                            }
                            inSuccess = true;
                        }
                    }
                    if (inSuccess) {
                        candidateTrack = candidateTrackIn;
                    }
                    
                    // skip this one if it was already found before using a different seed. First, sort the hits.
                    Collections.sort(candidateTrack.hits, KalHit.HitComparator);
                    boolean match = false;
                    for (TrackCandidate oldCandidate : candidateList) {
                        if (candidateTrack.equals(oldCandidate)) {
                            if (candidateTrack.chi2s >= 0.5*oldCandidate.chi2s && candidateTrack.chi2f >= 0.5*oldCandidate.chi2f) {
                                if (verbose) System.out.format("KalmanPatRecHPS: candidate track is redundant. Skip to next seed.\n");
                                continue seedLoop;
                            }
                            match=true;
                        }
                    }
                    if (!match) candidateList.add(candidateTrack); // Save the candidate in this list for later checking for duplicates

                    if (candidateTrack.hits.size() < minHits1[trial]) {
                        if (verbose) {
                            System.out.format("KalmanPatRecHPS: Filtering to layer 0 has too few hits, %d<%d. Skip to the next seed.\n",
                                    candidateTrack.hits.size(), minHits2[trial]);
                        }
                        continue;
                    }
                    if (candidateTrack.chi2f / (double) candidateTrack.nHits > chi2mx2[trial]) {
                        if (verbose) {
                            System.out.format("KalmanPatRecHPS: Filtering to layer 0 has too large chi^2. Skip to the next seed.\n");
                        }
                        continue;
                    }

                    // Iterate the fit, starting from near the target and going toward the calorimeter
                    MeasurementSite startSite=null;
                    if (inSuccess) startSite = candidateTrack.sites.get(candidateTrack.sites.size() - 1);
                    else startSite = candidateTrack.sites.get(0);
                    startSite.aF.C.scale(10000.);
                    candidateTrack = filterTrack(firstLayer, numLayers - 1, startSite.aF, candidateTrack.hits, trial);
                    if (!candidateTrack.filtered) {
                        if (verbose) { System.out.format("KalmanPatRecHPS: failed filtering of all layers. Try next seed.\n"); }
                        continue;
                    }
                    if (verbose) {
                        candidateTrack.print("after final filtering", false);
                        System.out.format("Layer hits after final filtering: ");
                        for (int i = 0; i < numLayers; i++) {
                            int lHit = -1;
                            for (KalHit ht : candidateTrack.hits) { 
                                if (ht.module.Layer == i) lHit = ht.module.hits.indexOf(ht.hit);
                            }
                            System.out.format("%2d ", lHit);
                        }
                        System.out.format("\n");
                    }

                    int nStereo = 0;
                    for (KalHit ht : candidateTrack.hits) {
                        if (ht.isStereo()) nStereo++;
                    }
                    if (nStereo < minStereo) {
                        if (verbose) { System.out.format("KalmanPatRecHPS: filtering has too few stereo hits. Skip to the next seed.\n"); }
                        continue;
                    }
                    if (candidateTrack.nHits - nStereo < minAxial) {
                        if (verbose) { System.out.format("KalmanPatRecHPS: filtering has too few non-stereo hits. Skip to the next seed.\n"); }
                        continue;
                    }

                    // Finally smooth back to the target
                    smoothTrack(candidateTrack, true, trial, 0);

                    if (verbose) candidateTrack.print("after smoothing", false);

                    if (verbose) {
                        Vec afF = candidateTrack.sites.get(candidateTrack.sites.size() - 1).aF.a;
                        Vec afC = candidateTrack.sites.get(candidateTrack.sites.size() - 1).aF.helixErrors();
                        afF.print("KalmanPatRecHPS helix parameters at final filtered site");
                        afC.print("KalmanPatRecHPS helix parameter errors");
                        startSite = candidateTrack.sites.get(0);
                        if (startSite != null) {
                            startSite.aS.a.print("KalmanPatRecHPS helix parameters at the final smoothed site");
                            startSite.aS.helixErrors().print("KalmanPatRecHPS helix parameter errors:");
                        }
                    }
                    // If the fit looks good, save the track information and mark the hits as used
                    if (candidateTrack.smoothed) {
                        if (candidateTrack.nHits >= minHits1[trial] && candidateTrack.chi2s / (double) candidateTrack.nHits < chi2mx1[trial]) { // Good tracks
                            tkID++;
                            KalTrack tkr = new KalTrack(eventNumber, tkID, candidateTrack.nHits, candidateTrack.sites, candidateTrack.chi2s);
                            boolean redundant = false;
                            for (KalTrack oldTkr : TkrList) {
                                if (tkr.equals(oldTkr)) {
                                    redundant = true;
                                    break;
                                }
                            }
                            if (!redundant) {
                                for (KalTrack oldTkr : TkrList2) {
                                    if (tkr.equals(oldTkr)) {
                                        redundant = true;
                                        break;
                                    }
                                }
                            }
                            if (redundant) {
                                if (verbose) System.out.format("KalmanPatRecHPS: throwing away redundant track %d\n", tkID);
                                tkID--;
                            } else {
                                for (MeasurementSite site : candidateTrack.sites) {
                                    int theHit = site.hitID;
                                    if (theHit < 0) { continue; }
                                    site.m.hits.get(theHit).tracks.add(tkr); // Mark the hits as used
                                }
                                if (verbose) {
                                    System.out.format("KalmanPatRecHPS: Adding track with %d hits and smoothed chi^2=%10.5f\n",
                                            candidateTrack.nHits, candidateTrack.chi2s);
                                }
                                TkrList.add(tkr);
                            }
                        } else if (candidateTrack.nHits >= minHits2[trial]
                                && candidateTrack.chi2s / (double) candidateTrack.nHits < chi2mx2[trial]) { // Low quality tracks; don't kill hits
                                    tkID++;
                                    KalTrack tkr = new KalTrack(eventNumber, tkID, candidateTrack.nHits, candidateTrack.sites, candidateTrack.chi2s);
                                    boolean redundant = false;
                                    for (KalTrack oldTkr : TkrList2) {
                                        if (tkr.equals(oldTkr)) {
                                            redundant = true;
                                            break;
                                        }
                                    }
                                    if (!redundant) {
                                        for (KalTrack oldTkr : TkrList) {
                                            if (tkr.equals(oldTkr)) {
                                                redundant = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (redundant) {
                                        if (verbose) { 
                                            System.out.format("KalmanPatRecHPS: throwing away redundant substandard track %d\n",tkID); 
                                        }
                                        tkID--;
                                    } else {
                                        if (verbose) {
                                            System.out.format(
                                                    "KalmanPatRecHPS: Adding substandard track with %d hits and smoothed chi^2=%10.5f\n",
                                                    candidateTrack.nHits, candidateTrack.chi2s);
                                        }
                                        TkrList2.add(tkr);
                                    }
                                }
                    }
                } // Next seed in set
            } // Next set of seeds
            if (verbose) {
                System.out.format("Completed loop over seeds. %d good tracks, %d crummy tracks.\n", TkrList.size(), TkrList2.size());
            }

            // Use the substandard tracks if they are not sharing too many hits
            // First sort them by quality
            if (TkrList2.size() > 0) {
                Collections.sort(TkrList2, KalTrack.TkrComparator);
                if (verbose) { 
                    for (KalTrack tkr : TkrList2) { 
                        tkr.print("sorted substandard tracks"); 
                    } 
                }
                for (KalTrack tkr : TkrList2) {
                    int nHits = 0;
                    int nStereo = 0;
                    int nShared = 0;
                    for (MeasurementSite site : tkr.SiteList) {
                        if (site.hitID < 0) continue;
                        SiModule m = site.m;
                        if (m.hits.get(site.hitID).tracks.size() > 0) {
                            if (site.chi2inc > mxChi2double) site.removeHit(verbose);
                            else nShared++;
                        }
                        if (site.hitID >= 0) {
                            nHits++;
                            if (m.isStereo) nStereo++;
                        }
                    }
                    if (nShared < 2 && nHits >= minHits2[trial] && nStereo > 3 && nHits - nStereo > 1) {
                        for (MeasurementSite site : tkr.SiteList) {
                            if (site.hitID < 0) { continue; }
                            site.m.hits.get(site.hitID).tracks.add(tkr); // Mark the hits as used
                        }
                        if (verbose) { System.out.format("KalmanPatRecHPS: adding substandard track %d to good track list.\n", tkr.ID); }
                        TkrList.add(tkr);
                    }
                }
            }
        } // Next global iteration, using looser criteria

        // Remove shared hits unless the hit is very close to two tracks
        ArrayList<KalTrack> changedTracks = new ArrayList<KalTrack>();
        if (TkrList.size() > 0) {
            for (SiModule m : data) {
                for (Measurement hit : m.hits) {
                    if (hit.tracks.size() > 1) {
                        double minChi2inc = 999.;
                        KalTrack bestTkr = null;
                        for (KalTrack tkr : hit.tracks) {
                            int idx = tkr.whichSite(m);
                            if (idx < 0) {
                                System.out.format("KalmanPatRecHPS: bad reference from hit to track. Track %d, Layer = %d\n", tkr.ID, m.Layer);
                            } else {
                                MeasurementSite site = tkr.SiteList.get(idx);
                                if (site.chi2inc < minChi2inc) {
                                    minChi2inc = site.chi2inc;
                                    bestTkr = tkr;
                                }
                            }
                        }
                        ArrayList<KalTrack> tkrsToRemove = new ArrayList<KalTrack>();
                        for (KalTrack tkr : hit.tracks) {
                            if (tkr == bestTkr) { // Keep the hit on the best track
                                continue;
                            }
                            int idx = tkr.whichSite(m);
                            if (idx < 0) {
                                System.out.format("KalmanPatRecHPS: bad reference from hit to track. Track %d, Layer = %d\n", tkr.ID, m.Layer);
                            } else {
                                MeasurementSite site = tkr.SiteList.get(idx);
                                if (site.chi2inc > mxChi2double) {
                                    changedTracks.add(tkr);
                                    int oldID = site.hitID;
                                    if (!site.smoothed) { System.out.format("KalmanPatRecHPS: oops, why isn't this site smoothed?"); }
                                    site.removeHit(verbose);
                                    if (verbose) {
                                        System.out.format("KalmanPatRecHPS: removing a hit from Track %d, Layer %d\n", tkr.ID, m.Layer);
                                    }
                                    // Check whether there might be another hit available
                                    Measurement addedHit = site.addHit(mxChi2Inc[0], oldID);
                                    if (addedHit != null) {
                                        addedHit.tracks.add(tkr);
                                        if (verbose) {
                                            System.out.format("KalmanPatRecHPS: added a hit after removing one for Track %d, Layer %d\n",
                                                    tkr.ID, m.Layer);
                                        }
                                    }
                                    tkrsToRemove.add(tkr);
                                }
                            }
                        }
                        for (KalTrack tkr : tkrsToRemove) { hit.tracks.remove(tkr); }
                    }
                }
            }
        }

        // Refit tracks that got changed
        ArrayList<KalTrack> allTks = new ArrayList<KalTrack>(TkrList.size());
        for (KalTrack tkr : TkrList) {
            allTks.add(tkr); // (refit them all for now)
        }
        for (KalTrack tkr : allTks) {
            // check that there are enough hits in both views
            int nStereo = 0;
            int nNonStereo = 0;
            for (MeasurementSite site : tkr.SiteList) {
                if (site.hitID < 0) continue;
                SiModule m = site.m;
                if (!m.isStereo) nNonStereo++;
                else nStereo++;
            }
            if (nStereo < minStereo) {
                TkrList.remove(tkr);
                continue;
            } else if (nNonStereo < minAxial) {
                TkrList.remove(tkr);
                continue;
            } else if (nNonStereo + nStereo < minHits2[nTries - 1]) {
                TkrList.remove(tkr);
                continue;
            }
            if (verbose) System.out.format("KalmanPatRecHPS: Call the Kalman fit for track %d\n", tkr.ID);
            tkr.fit(1, verbose);
        }

        Collections.sort(TkrList, KalTrack.TkrComparator); // Sort tracks by quality
        if (verbose) {
            System.out.format("\n\n Printing the list of tracks found:\n");
            for (KalTrack tkr : TkrList) {
                tkr.originHelix();
                tkr.print(" ");
            }
        }
    }

    // Method to smooth an already filtered track candidate
    private void smoothTrack(TrackCandidate filteredTkr, boolean changeHits, int trial, int iteration) {
        MeasurementSite nextSite = null;
        for (int idxS = filteredTkr.sites.size() - 1; idxS >= 0; idxS--) {
            MeasurementSite currentSite = filteredTkr.sites.get(idxS);
            if (nextSite == null) {
                currentSite.aS = currentSite.aF.copy();
                currentSite.smoothed = true;
            } else {
                currentSite.smooth(nextSite);
            }
            filteredTkr.chi2s += currentSite.chi2inc;

            if (changeHits) {
                int nStereo = 0;
                for (KalHit ht : filteredTkr.hits) {
                    if (ht.isStereo()) {
                        nStereo++;
                        break;
                    }
                }
                int ID = currentSite.hitID;
                if (ID >= 0) {
                    if (currentSite.chi2inc > mxChi2Inc[trial] && iteration != nIterations - 1 && filteredTkr.nHits > minHits2[trial]) {
                        boolean stereo = currentSite.m.isStereo;
                        if ((stereo && nStereo > 4) || (!stereo && filteredTkr.nHits - nStereo > 2)) {
                            double chi2Now = currentSite.chi2inc;
                            if (currentSite.removeHit(verbose)) {
                                if (verbose) {
                                    System.out.format("KalmanPatRecHPS: removing hit %d with chi^2inc=%10.2f from Layer %d, stereo %6.2f\n", ID,
                                            chi2Now, currentSite.m.Layer, currentSite.m.stereo);
                                }
                                filteredTkr.nHits--;
                                if (stereo) { nStereo--; }
                                for (KalHit ht : filteredTkr.hits) {
                                    if (ht.module == currentSite.m) {
                                        if (ht.hit == currentSite.m.hits.get(ID)) {
                                            filteredTkr.hits.remove(ht);
                                            if (verbose) { System.out.format("      Successfully removed the hit from the list\n"); }
                                            filteredTkr.chi2s -= chi2Now;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (currentSite.hitID < 0 && iteration != nIterations - 1) { // Look for a better hit
                    Measurement addedHit = currentSite.addHit(mxChi2Inc[trial], ID);
                    if (addedHit != null) {
                        filteredTkr.nHits++;
                        KalHit tmpHt = new KalHit(currentSite.m, addedHit);
                        filteredTkr.hits.add(tmpHt);
                        if (verbose) {
                            System.out.format("KalmanPatRecHPS: adding hit %d with chi^2inc=%10.2f to layer %d\n", currentSite.hitID,
                                    currentSite.chi2inc, idxS);
                        }
                        filteredTkr.chi2s += Math.max(currentSite.chi2inc,0.);
                        if (nextSite == null) {
                            currentSite.aS = currentSite.aF.copy();
                            currentSite.smoothed = true;
                        } else {
                            currentSite.smooth(nextSite);
                        }
                    }
                }
            }
            nextSite = currentSite;
        }
        filteredTkr.smoothed = true;
    }

    // Execute the Kalman prediction and filter steps over a range of SVT layers
    private TrackCandidate filterTrack(int lyrBegin, // layer on which to start the filtering
            int lyrEnd, // layer on which to end the filtering
            StateVector sI, // initialization state vector
            ArrayList<KalHit> hits, // existing hit assignments
            int trial // trial level, for selecting cuts
    ) {

        TrackCandidate tmpTrack = new TrackCandidate();
        for (KalHit hit : hits) {
            if (hit.hit.tracks.size() > 0) { tmpTrack.nTaken++; }
            tmpTrack.hits.add(hit);
        }
        if (tmpTrack.hits.size() - tmpTrack.nTaken < minUnique) {
            if (verbose) {
                System.out.format("KalmanPatRecHPS:filterTrack: skipping for hits used. nLyrs=%d, nTaken=%d\n", tmpTrack.hits.size(),
                        tmpTrack.nTaken);
            }
            return tmpTrack;
        }

        if (verbose) {
            Vec hprms = sI.a;
            System.out.format("\n KalmanPatRecHPS:filterTrack: Start filtering with drho=%10.5f phi0=%10.5f k=%10.5f dz=%10.5f tanl=%10.5f \n",
                    hprms.v[0], hprms.v[1], hprms.v[2], hprms.v[3], hprms.v[4]);
            for (KalHit hit : hits) { hit.print(" existing "); }
        }

        tmpTrack.sites = new ArrayList<MeasurementSite>(nModules);
        MeasurementSite newSite = null;
        MeasurementSite prevSite = null;

        int thisSite = -1;
        // Filter from the start of the seed to the last downstream detector layer
        // loop over all layers from the seed beginning to the end of the tracker
        int direction;
        if (lyrEnd > lyrBegin) {
            direction = 1;
        } else {
            direction = -1;
        }
        for (int lyr = lyrBegin; lyr != lyrEnd + direction; lyr += direction) {
            SiModule m = null;
            // Find the correct hit number and its module if this is one of the seed layers
            int hitno = -1;
            for (KalHit ht : hits) {
                if (ht.module.Layer == lyr) {
                    hitno = ht.module.hits.indexOf(ht.hit);
                    m = ht.module;
                    break;
                }
            }

            // Loop over all of the modules in this layer
            for (int imod = 0; imod < moduleList.get(lyr).size(); ++imod) {
                SiModule thisSi = moduleList.get(lyr).get(imod);
                // Only consider the one module with the given hit for the existing specified hits
                if (m != null) {
                    if (thisSi != m) continue;
                } else {
                    m = thisSi;
                }

                newSite = new MeasurementSite(lyr, m, mxResid[trial], mxResidShare);
                int rF;
                if (prevSite == null) { // For first layer use the initializer state vector
                    rF = newSite.makePrediction(sI, null, hitno, tmpTrack.nTaken <= mxShared, true, imod < moduleList.get(lyr).size() - 1,
                            verbose);
                    if (rF > 0) {
                        if (m.hits.get(newSite.hitID).tracks.size() > 0) { tmpTrack.nTaken++; }
                    } else if (rF < 0) {
                        if (rF == -2) continue;
                        if (verbose) {
                            System.out.format("KalmanPatRecHPS:filterTrack: Failed to make initial prediction at site %d, layer=%d. Abort\n",
                                    thisSite + 1, lyr);
                        }
                        return tmpTrack;
                    }
                } else {
                    rF = newSite.makePrediction(prevSite.aF, prevSite.m, hitno, tmpTrack.nTaken <= mxShared, true,
                            imod < moduleList.get(lyr).size() - 1, verbose);
                    if (rF > 0) {
                        if (m.hits.get(newSite.hitID).tracks.size() > 0) { tmpTrack.nTaken++; }
                    } else if (rF < 0) {
                        if (rF == -2) continue;
                        if (verbose) {
                            System.out.format("KalmanPatRecHPS:filterTrack: Failed to make prediction at site %d, layer=%d.  Abort\n",
                                    thisSite + 1, lyr);
                        }
                        return tmpTrack;
                    }
                }
                thisSite++;
                tmpTrack.nHits += rF;
                if (rF == 1 && hitno < 0) {
                    KalHit htNew = new KalHit(m, m.hits.get(newSite.hitID));
                    tmpTrack.hits.add(htNew);
                    if (verbose) htNew.print("new");
                }
                if (!newSite.filter()) {
                    if (verbose)
                        System.out.format("KalmanPatRecHPS:filterTrack: Failed to filter at site %d, layer=%d.  Ignore remaining sites\n",
                                thisSite, lyr);
                    return tmpTrack;
                }

                // if (verbose) newSite.print("initial filtering");
                tmpTrack.chi2f += Math.max(newSite.chi2inc,0.);

                tmpTrack.sites.add(newSite);
                prevSite = newSite;
                break;
            }
        }

        tmpTrack.filtered = true;
        return tmpTrack;
    }

    // Quick check on where the seed track is heading
    /*
    private boolean seedNoGood(int nLyr1, int nHit1, int nLyr2, int nHit2) {
        double y1 = data.get(nLyr1).p.X().v[1];
        double y2 = data.get(nLyr2).p.X().v[1];
        double z1 = data.get(nLyr1).toGlobal(new Vec(0., data.get(nLyr1).hits.get(nHit1).v, 0.)).v[2];
        double z2 = data.get(nLyr2).toGlobal(new Vec(0., data.get(nLyr2).hits.get(nHit2).v, 0.)).v[2];
        double slope = (z2 - z1) / (y2 - y1);
        double zIntercept = z1 - slope * y1;
        if (Math.abs(zIntercept) > dRhoMax) return true;
        if (slope > tanlMax) return true;
        return false;
    }
    */
}

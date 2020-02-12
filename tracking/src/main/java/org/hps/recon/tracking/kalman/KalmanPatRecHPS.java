package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hps.util.Pair;

// Tracking pattern recognition for the HPS experiment based on an extended Kalman filter
// Algorithm:
//    1. Loop over starting strategies, each involving at least 3 stereo planes and 2 non-stereo planes
//    2. Make all possible seeds by selecting a hit from each of the 5 planes
//    3. Do a linear helix fit to the 5 hits in each seed and select helices that project near to the target origin
//    4. Sort the seeds by quality and then loop over them and use them to start the Kalman filter working outward toward the ECAL
//         Smooth back to the beginning of the seed and then filter inward toward the vertex to make a candidate track.
//         The seed hits always remain on the track candidate.
//         On other layers the track candidate picks up the best fitting hit.
//         Hits not from the seed can be dropped to try to get a good fit.
//         Only decent quality candidates are kept.
//         Don't keep candidates that are identical in hit content to candidates already found
//    5. Sort all the track candidates by quality
//    6. Remove hits from track candidates that are used by better candidates, unless the hits can be shared
//    7. Track candidates without enough hits remaining get dropped, the others get refit

public class KalmanPatRecHPS {

    public ArrayList<KalTrack> TkrList; // Final good tracks
    public int topBottom;               // 0 for bottom tracker (+z), 1 for top (-z)
    
    private ArrayList<ArrayList<KalHit>> lyrHits;
    private ArrayList<ArrayList<SiModule>> moduleList;
    private ArrayList<int[]> lyrList;
    private Map<Measurement, KalHit> hitMap;

    private int eventNumber;
    private static final int nTries = 2;  // Number of iterations through the entire pattern recognition
    private int nIterations;              // Number of Kalman-fit iterations in the final fit
    private double[] kMax; 
    private double[] tanlMax; 
    private double[] dRhoMax; 
    private double[] dzMax;
    private double[] chi2mx1; 
    private int minHits0;
    private int[] minHits1; 
    private double[] mxChi2Inc; 
    private double[] mxResid; 
    private double mxResidShare; 
    private double mxChi2double;
    private int mxShared; 
    private boolean verbose;
    private int nModules;
    private int [] minStereo;
    private int minAxial;
    private double mxTdif;

    public KalmanPatRecHPS(ArrayList<SiModule> data, int topBottom, int eventNumber, boolean verbose) {
        // topBottom = 0 for the bottom tracker (z>0); 1 for the top tracker (z<0)
        this.topBottom = topBottom;
        this.eventNumber = eventNumber;
        this.verbose = verbose;

        TkrList = new ArrayList<KalTrack>();
        nModules = data.size();
        int tkID = 1;
        
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
        hitMap = new HashMap<Measurement, KalHit>();
        for (SiModule thisSi : data) {
            for (Measurement m : thisSi.hits) {
                KalHit hit = new KalHit(thisSi, m);  // Create exactly one KalHit object for each measurement!
                lyrHits.get(thisSi.Layer).add(hit);
                hitMap.put(m, hit);
            }
            if (thisSi.hits.size() > 0) moduleList.get(thisSi.Layer).add(thisSi);
        }
        if (verbose) {
            for (ArrayList<KalHit> LL : lyrHits) {
                System.out.format("KalmanPatRecHPS: layer %d hits:", lyrHits.indexOf(LL));
                for (KalHit ht : LL) {
                    ht.print("short");
                }
                System.out.format("\n");
            }
        }

        final int[] Swap = {1,0, 3,2, 5,4, 7,6, 9,8, 11,10, 13,12};
        lyrList = new ArrayList<int[]>(15); // Array of seed strategies
        // Each list should contain 3 stereo layers and 2 non-stereo layers. These lists are for the bottom tracker
        final int[] list0 = {6, 7, 8, 9, 10};
        final int[] list1 = {4, 5, 6, 7, 8};
        final int[] list2 = {5, 6, 8, 9, 10};
        final int[] list3 = {5, 6, 7, 8, 10};
        final int[] list4 = { 3, 6, 8, 9, 10 };
        final int[] list5 = { 4, 5, 8, 9, 10 };
        final int[] list6 = { 4, 6, 7, 8, 9 };
        final int[] list7 = { 4, 6, 7, 9, 10 };
        final int[] list8 = { 2, 5, 8, 9, 12};
        final int[] list9 = { 8, 10, 11, 12, 13};
        final int[] list10 = {6, 9, 10, 11, 12};
        final int[] list11 = {6, 7, 9, 10, 12};
        final int[] list12 = {2, 3, 4, 5, 6};
        final int[] list13 = {2, 4, 5, 6, 7};
        final int[] list14 = {6, 7, 8, 10, 11};
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
        lyrList.add(list13);
        lyrList.add(list14);
        
        // Swap axial/stereo in list entries for the top tracker
        if (topBottom == 1) {
            for (int[] list: lyrList) {
                for (int i=0; i<5; ++i) {
                    list[i] = Swap[list[i]];
                }
                for (int i=0; i<4; ++i) {
                    if (list[i] > list[i+1]) { // Sorting entries. No more than one swap should be necessary.
                        int tmp = list[i];
                        list[i] = list[i+1];
                        list[i+1] = tmp;
                    }
                }
            }
        }

        kMax = new double[nTries];
        tanlMax = new double[nTries];
        dRhoMax = new double[nTries];
        dzMax = new double[nTries];
        chi2mx1 = new double[nTries];
        minHits1 = new int[nTries];
        mxChi2Inc = new double[nTries];
        mxResid = new double[nTries];
        minStereo = new int[nTries];
        
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
        dzMax[0] = 3.;      // Maximum z at target plane for seed
        dzMax[1] = 10.;
        chi2mx1[0] = 8.0;   // Maximum chi**2/#hits for good track
        chi2mx1[1] = 12.0;  
        minHits0 = 6;       // Minimum number of hits in the initial outward filtering (including 5 from the seed)
        minHits1[0] = 7;    // Minimum number of hits for a good track
        minHits1[1] = 5;
        mxChi2Inc[0] = 2.;  // Maximum increment to the chi^2 to add a hit to a completed track candidate
        mxChi2Inc[1] = 10.; // Threshold for removing a bad hit
        mxResid[0] = 150.;   // Maximum residual, in units of detector resolution, for picking up a hit
        mxResid[1] = 200.;
        mxResidShare = 10.;  // Maximum residual, in units of detector resolution, for a hit to be shared
        mxChi2double = 6.;
        minStereo[0] = 4;
        minStereo[1] = 3;    // Minimum number of stereo hits
        minAxial = 2;       // Minimum number of axial hits
        mxShared = 2;       // Maximum number of shared hits
        mxTdif = 30.;       // Maximum time difference of hits in a track
        
        Plane p0 = new Plane(new Vec(0., 0., 0.), new Vec(0., 1., 0.));

        if (verbose) {
            System.out.format("Entering KalmanPatRecHPS for event %d, top-bottom=%d with %d modules, for %d trials.\n", eventNumber, topBottom, nModules, nTries);
            System.out.format("  KalmanPatRecHPS: list of the seed strategies to be applied:\n");
            for (int[] list : lyrList) {
                for (int lyr=0; lyr<list.length; ++lyr) {
                    System.out.format(" %3d ", list[lyr]);
                }
                System.out.format("\n");
            }
            System.out.format("    Layer types: ");
            for (SiModule module : data) {
                if (module.isStereo) System.out.format(" %d=S ",module.Layer);
                else System.out.format(" %d=A ",module.Layer);
            }
            System.out.format("\n");
        }

        // Loop over seed strategies, each with 2 non-stereo layers and 3 stereo layers
        // For each strategy generate a seed track for every hit combination
        // Keep only those pointing more-or-less back to the origin and not too curved
        // Sort the list first by curvature, then by drho
        for (int trial = 0; trial < nTries; trial++) {
            if (verbose) { System.out.format("\nKalmanPatRecHPS: start of pass %d through the algorithm.\n", trial); }
            ArrayList<TrackCandidate> candidateList = new ArrayList<TrackCandidate>();
            for (int[] list : lyrList) {
                int nLyrs = list.length;
                int originLyr = 2;
                if (moduleList.get(list[originLyr]).size() == 0) continue;
                SiModule m0 = moduleList.get(list[2]).get(0);
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
                    if (lyrHits.get(list[0]).get(idx[0]).hit.tracks.size() > 0) continue; // don't use hits already on tracks, in 2nd iteration
                    for (idx[1] = 0; idx[1] < lyrHits.get(list[1]).size(); idx[1]++) {
                        if (lyrHits.get(list[1]).get(idx[1]).hit.tracks.size() > 0) continue;
                        for (idx[2] = 0; idx[2] < lyrHits.get(list[2]).size(); idx[2]++) {
                            if (lyrHits.get(list[2]).get(idx[2]).hit.tracks.size() > 0) continue;
                            for (idx[3] = 0; idx[3] < lyrHits.get(list[3]).size(); idx[3]++) {
                                if (lyrHits.get(list[3]).get(idx[3]).hit.tracks.size() > 0) continue;
                                for (idx[4] = 0; idx[4] < lyrHits.get(list[4]).size(); idx[4]++) {
                                    if (lyrHits.get(list[4]).get(idx[4]).hit.tracks.size() > 0) continue;
                                    ArrayList<KalHit> hitList = new ArrayList<KalHit>(5);
                                    for (int i = 0; i < nLyrs; i++) {
                                        hitList.add(lyrHits.get(list[i]).get(idx[i]));
                                    }
                                    // Cut on the seed timing
                                    double tmin = 1.e10;
                                    double tmax = -1.e10;
                                    for (KalHit ht : hitList) {
                                        tmin = Math.min(tmin, ht.hit.time);
                                        tmax = Math.max(tmax, ht.hit.time);
                                    }
                                    if (tmax - tmin > mxTdif) {
                                        if (verbose) {
                                            System.out.format("KalmanPatRecHPS: skipping seed with tdif=%8.2f\n Hits:  ", tmax-tmin);
                                            for (KalHit ht : hitList) ht.print("short");
                                            System.out.format("\n");
                                        }
                                        continue;
                                    }
                                    SeedTrack seed = new SeedTrack(hitList, yOrigin, false);
                                    if (!seed.success) continue;
                                    // Cuts on the seed quality
                                    Vec hp = seed.helixParams();
                                    if (verbose) {
                                        System.out.format("Seed %d %d %d %d %d parameters for cuts: K=%10.5f, tanl=%10.5f, dxz=%10.5f   ",
                                                idx[0], idx[1], idx[2], idx[3], idx[4], hp.v[2], hp.v[4], seed.planeIntersection(p0).mag());
                                    }
                                    if (Math.abs(hp.v[2]) < kMax[trial]) {
                                        if (Math.abs(hp.v[4]) < tanlMax[trial]) {
                                            Vec pInt = seed.planeIntersection(p0);
                                            if (verbose) System.out.format("intersection with target plane= %9.3f %9.3f %9.3f", pInt.v[0],
                                                    pInt.v[1], pInt.v[2]);
                                            if (pInt.mag() < dRhoMax[trial]) {
                                                if (Math.abs(pInt.v[2]) < dzMax[trial]) seedList.add(seed);
                                            }
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
                            ht.print("short");
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
                            ht.print("short");
                        }
                        System.out.format("\n");
                    }
                    // Skip seeds that are already on a good track candidate
                    for (TrackCandidate tkCand : candidateList) {
                        if (tkCand.hits.containsAll(seed.hits)) {
                            if (verbose) System.out.format("KalmanPatRecHPS: skipping seed %d that is already on candidate %d\n", 
                                    seedList.indexOf(seed),candidateList.indexOf(tkCand));
                            continue seedLoop;
                        }
                    }
                    // Skip seeds that are already on a track found and saved in this iteration
                    if (TkrList.size() > 0) {
                        for (KalHit ht : seed.hits) {
                            if (ht.hit.tracks.size()>0) {
                                if (verbose) System.out.format("KalmanPatRecHPS: skipping seed %d that is already on track %d\n", 
                                        seedList.indexOf(seed),ht.hit.tracks.get(0).ID);
                                continue seedLoop;
                            }
                        }
                    }
                    
                    SquareMatrix CovGuess = seed.covariance();
                    CovGuess.scale(1000.);
                    // Create an state vector from the input seed to initialize the Kalman filter
                    StateVector sI = new StateVector(-1, seed.helixParams(), CovGuess, new Vec(0., 0., 0.), Bmag, tB, pivot, false);
                    TrackCandidate candidateTrack = new TrackCandidate(seed.hits, mxShared, mxTdif, hitMap);
                    filterTrack(candidateTrack, list[0], numLayers - 1, sI, trial, true, true);
                    if (!candidateTrack.filtered) {
                        if (verbose) {
                            System.out.format("KalmanPatRecHPS: filtering of this seed failed. chi2=%10.5f, nHits=%d\n", candidateTrack.chi2f,
                                    candidateTrack.hits.size());
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

                    if (candidateTrack.sites.size() < minHits0) {
                        if (verbose) System.out.format("KalmanPatRecHPS: Initial filtering has too few sites, n=%d<%d. Skip to the next seed.\n",
                                candidateTrack.sites.size(), minHits0);
                        continue;                        
                    }
                    if (candidateTrack.hits.size() < minHits0) {
                        if (verbose) System.out.format("KalmanPatRecHPS: Initial filtering has too few hits, n=%d<%d. Skip to the next seed.\n",
                                candidateTrack.hits.size(), minHits0);
                        continue;
                    }
                    if (candidateTrack.chi2f / (double) candidateTrack.hits.size() > chi2mx1[trial]) {
                        if (verbose) System.out.format("KalmanPatRecHPS: Initial filtering has too large chi^2. Skip to the next seed.\n");
                        continue;
                    }

                    // Now smooth back to the original point
                    smoothTrack(candidateTrack);
                    if (verbose) {
                        candidateTrack.print("after initial smoothing", false);
                        System.out.format("\nKalmanPatRecHPS: Smoothed chi2=%10.5f\n", candidateTrack.chi2s);
                    }

                    // Then filter toward the target if there are any more untried layers there
                    if (candidateTrack.sites.get(0).m.Layer > firstLayer) {
                        filterTrack(candidateTrack, candidateTrack.sites.get(0).m.Layer - 1, firstLayer, candidateTrack.sites.get(0).aS, trial, false, true);
                        if (!candidateTrack.filtered) {
                            if (verbose) System.out.format("KalmanPatRecHPS: not successful with inward filter step\n");
                            candidateTrack.good = false;
                            continue;
                        }
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
                    }
                    
                    // skip this one if it was already found before using a different seed. First, sort the hits.
                    if (verbose) candidateTrack.print("before sorting hits", true);
                    Collections.sort(candidateTrack.hits, KalHit.HitComparator);
                    if (verbose) candidateTrack.print("after sorting hits", true);
                    for (TrackCandidate oldCandidate : candidateList) {
                        if (candidateTrack.equals(oldCandidate)) {
                            if (verbose) System.out.format("KalmanPatRecHPS: candidate match new=%8.3f old=%8.3f\n",candidateTrack.chi2s,oldCandidate.chi2s);
                            if (verbose) System.out.format("KalmanPatRecHPS: candidate track is redundant. Skip to next seed.\n");
                            continue seedLoop;
                        }
                    }
                    candidateList.add(candidateTrack); // Save the candidate in this list 

                    if (candidateTrack.hits.size() < minHits1[trial]) {
                        if (verbose) {
                            System.out.format("KalmanPatRecHPS: Filtering to layer 0 has too few hits, %d<%d. Skip to the next seed.\n",
                                    candidateTrack.hits.size(), minHits1[trial]);
                        }
                        candidateTrack.good = false;
                        continue;
                    }
                    boolean hitChanges = false;
                    if (candidateTrack.chi2f / (double) candidateTrack.hits.size() > chi2mx1[trial]) {
                        // See if the chi^2 will be okay if just one hit is removed
                        boolean removedHit = false;
                        if (candidateTrack.sites.size() > (minAxial + minStereo[trial])) {
                            MeasurementSite siteR = null;
                            for (MeasurementSite site : candidateTrack.sites) {
                                if (candidateTrack.seedLyrs.contains(site.m.Layer)) continue;  // Don't alter seed layers
                                if (site.hitID < 0) continue;
                                if ((candidateTrack.chi2f - site.chi2inc)/(double) candidateTrack.hits.size() < chi2mx1[trial]) {
                                    siteR = site;
                                    break;
                                }
                            }
                            if (siteR != null) {
                                KalHit hitR = hitMap.get(siteR.m.hits.get(siteR.hitID));
                                if (hitR != null) {
                                    //if (!candidateTrack.hits.contains(hitR)) System.out.format("Oops, missing hit!\n");
                                    candidateTrack.removeHit(hitR);           
                                    if (verbose) System.out.format("KalmanPatRecHPS event %d, removing hit from layer %d detector %d\n", eventNumber, siteR.m.Layer, siteR.m.detector);
                                    removedHit = true;
                                    //candidateTrack.print("with hit removed", true);
                                } else {
                                    System.out.format("KalmanPatRecHPS error: missing hit in layer %d detector %d\n", siteR.m.Layer, siteR.m.detector);
                                }
                            }
                        }
                        if (!removedHit) {
                            if (verbose) {
                                System.out.format("KalmanPatRecHPS: Filtering to layer 0 has too large chi^2. Skip to the next seed.\n");
                            }
                            candidateTrack.good = false;
                            continue;
                        }
                        hitChanges = true;
                    }

                    // Iterate the fit, starting from near the target and going toward the calorimeter
                    MeasurementSite lastSite = candidateTrack.sites.get(candidateTrack.sites.size() - 1);
                    MeasurementSite firstSite = candidateTrack.sites.get(0);
                    // The innermost site is either at the beginning or end of the list
                    MeasurementSite startSite=null;
                    if (lastSite.m.Layer < firstSite.m.Layer) startSite = lastSite;
                    else startSite = firstSite;
                    startSite.aF.C.scale(10000.);
                    filterTrack(candidateTrack, firstLayer, numLayers - 1, startSite.aF, trial, true, false);
                    if (!candidateTrack.filtered) {
                        if (verbose) { System.out.format("KalmanPatRecHPS: failed filtering of all layers. Try next seed.\n"); }
                        candidateTrack.good = false;
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
                    if (nStereo < minStereo[trial]) {
                        if (verbose) { System.out.format("KalmanPatRecHPS: filtering has too few stereo hits. Skip to the next seed.\n"); }
                        candidateTrack.good = false;
                        continue;
                    }
                    if (candidateTrack.hits.size() - nStereo < minAxial) {
                        if (verbose) { System.out.format("KalmanPatRecHPS: filtering has too few non-stereo hits. Skip to the next seed.\n"); }
                        candidateTrack.good = false;
                        continue;
                    }

                    // Finally smooth back to the target
                    smoothTrack(candidateTrack);

                    if (verbose) candidateTrack.print("after smoothing", false);
                    
                    // Check if the track can be improved by removing hits
                    if (removeBadHits(candidateTrack, trial)) {
                        if (verbose) System.out.format("KalmanPatRecHPS: Refit candidate track after removing a hit.\n");
                        if (candidateTrack.reFit(verbose)) {
                            if (verbose) candidateTrack.print("after refitting and smoothing", false);
                        } else {
                            candidateTrack.good = false;
                            continue;
                        }
                        hitChanges = true;
                    }
                    if (!candidateTrack.smoothed) candidateTrack.good = false;
                    if (candidateTrack.chi2s/(double)candidateTrack.numHits() > chi2mx1[trial]) candidateTrack.good = false;
                    if (verbose) {
                        MeasurementSite endSite = null;
                        for (int isx = candidateTrack.sites.size()-1; isx>=0; --isx) {
                            MeasurementSite site = candidateTrack.sites.get(isx);
                            if (site.hitID >= 0) {
                                endSite = site;
                                break;
                            }
                        }
                        if (endSite != null) {
                            Vec afF = endSite.aF.a;
                            Vec afC = endSite.aF.helixErrors();
                            afF.print("KalmanPatRecHPS helix parameters at final filtered site");
                            afC.print("KalmanPatRecHPS helix parameter errors");
                        }
                        startSite = null;
                        for (MeasurementSite site : candidateTrack.sites) {
                            if (site.aS != null) startSite = site;
                        }
                        if (startSite != null) {
                            startSite.aS.a.print("KalmanPatRecHPS helix parameters at the final smoothed site");
                            startSite.aS.helixErrors().print("KalmanPatRecHPS helix parameter errors:");
                        }
                    }
                    // If any hit assignments changed, then check again for redundant candidates
                    if (hitChanges) {
                        Collections.sort(candidateTrack.hits, KalHit.HitComparator);
                        for (TrackCandidate oldCandidate : candidateList) {
                            if (oldCandidate == candidateTrack) continue;
                            if (candidateTrack.equals(oldCandidate)) {
                                if (verbose) System.out.format("KalmanPatRecHPS: candidate match new=%8.3f old=%8.3f\n",candidateTrack.chi2s,oldCandidate.chi2s);
                                if (verbose) System.out.format("KalmanPatRecHPS: candidate track is redundant (2). Skip to next seed.\n");
                                candidateTrack.good = false;
                                continue seedLoop;
                            }
                        }                        
                    }

                    // For near perfect candidates, save them immediately as KalTracks and mark the hits as no longer available (to save time)
                    if (candidateTrack.numHits() > 9) {
                        if (candidateTrack.chi2s < 30.) {
                            if (candidateTrack.numStereo() > 4) {
                                Vec helix = candidateTrack.originHelix();
                                if (Math.abs(helix.v[0]) < dRhoMax[trial]) {
                                    if (Math.abs(helix.v[3]) < dzMax[trial]) {
                                        if (verbose) {
                                            System.out.format("KalmanPatRecHPS: keeping a near perfect track candidate\n");
                                            candidateTrack.print("the perfect one", true);
                                        }
                                        if (storeTrack(tkID, candidateTrack)) {
                                            tkID++;
                                            candidateList.remove(candidateTrack);
                                            for (KalHit ht : candidateTrack.hits) {
                                                Set<TrackCandidate> tksToRemove = new HashSet<TrackCandidate>();
                                                for (TrackCandidate otherCand : ht.tkrCandidates) {
                                                    if (otherCand.nTaken >= mxShared | !otherCand.good) {
                                                        tksToRemove.add(otherCand);
                                                        if (verbose) {
                                                            System.out.print("KalmanPatRecHPS: remove a shared hit from already found candidate:\n");
                                                            otherCand.print("the other one", true);
                                                            ht.print("the shared hit");
                                                        }
                                                    } else {
                                                        otherCand.nTaken++;
                                                        if (otherCand.nTaken > mxShared) otherCand.good = false;
                                                    }
                                                }
                                                for (TrackCandidate tkr : tksToRemove) {
                                                    tkr.removeHit(ht);  // This will also remove the reference from the hit to this candidate
                                                    if (verbose) tkr.print("after hit removal", true);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Here we have a good track candidate. Mark the hits in KalHit as used by this candidate.
                    for (KalHit hit : candidateTrack.hits) {
                        hit.tkrCandidates.add(candidateTrack);
                    } 
                } // Next seed in set
            } // Next set of seeds
            if (verbose) {
                int nGood = 0;
                for (TrackCandidate tkr : candidateList) {
                    if (tkr.good) nGood++;
                }
                System.out.format("KalmanPatRecHPS for event %d, completed loop over seeds for iteration%d. %d good track candidates.\n", 
                        eventNumber, trial, nGood);
            }
            // Eliminate from the list all track candidates that are not good
            ArrayList<TrackCandidate> tkToRemove = new ArrayList<TrackCandidate>();
            for (TrackCandidate tkr : candidateList) {
                if (!tkr.good) tkToRemove.add(tkr);
            }
            for (TrackCandidate tkr : tkToRemove) {
                if (verbose) {
                    System.out.format("KalmanPatRecHPS: removing bad track candidate %d\n", candidateList.indexOf(tkr));
                    tkr.print("being removed", true);
                }
                candidateList.remove(tkr);
                for (KalHit ht : tkr.hits) {
                    ht.tkrCandidates.remove(tkr);
                }
            }
            
            // Sort the track candidates and then remove those sharing too many hits with a better candidate            
            Collections.sort(candidateList,TrackCandidate.CandidateComparator);
            if (verbose) {
                for (TrackCandidate tkr : candidateList) {
                    System.out.format("%d ",candidateList.indexOf(tkr));
                    tkr.print("sorted", true);
                }
            }            

            // Remove shared hits that should not be shared
            if (candidateList.size() > 1) {
                // Loop over all hits in all layers
                for (ArrayList<KalHit> hits : lyrHits) {
                    for (KalHit hit : hits) {
                        if (hit.tkrCandidates.size() > 1) {  // Shared hits
                            if (verbose) hit.print("evaluate sharing");
                            TrackCandidate bestTkr = null;
                            int iBest = candidateList.size();
                            for (TrackCandidate tkr : hit.tkrCandidates) {
                                int iTK = candidateList.indexOf(tkr);
                                if (iTK < iBest) {
                                    bestTkr = tkr;
                                    iBest = iTK;
                                }
                            }
                            if (verbose) System.out.format("KalmanPatRecHPS: best candidate track for this hit is %d, chi2=%7.3f\n", iBest, bestTkr.chi2s);
                            Set<TrackCandidate> tkrToRemove = new HashSet<TrackCandidate>();
                            for (TrackCandidate tkr : hit.tkrCandidates) {
                                if (tkr == bestTkr) continue;
                                for (MeasurementSite site : tkr.sites) {
                                    if (site.m == hit.module) {
                                        if (site.chi2inc > mxChi2double || Math.abs(site.aS.r/hit.hit.sigma) > mxResidShare/2.) {
                                            tkrToRemove.add(tkr);
                                            break;
                                        }
                                    }
                                }
                            }
                            for (TrackCandidate tkr : tkrToRemove) {
                                if (verbose) System.out.format("KalmanPatRecHPS: hit %d removed from track %d on layer %d detector %d\n", 
                                            hit.module.hits.indexOf(hit.hit), candidateList.indexOf(tkr), hit.module.Layer, hit.module.detector);
                                tkr.removeHit(hit);
                            }
                        }
                    }
                }
                if (verbose) {
                    for (TrackCandidate tkr : candidateList) {
                        System.out.format("%d ",candidateList.indexOf(tkr));
                        tkr.print("shared hits removed", true);
                    }
                }
                // Keep only candidates that still have enough hits to be viable
                for (TrackCandidate tkr : candidateList) {
                    int nAxial = 0;
                    int nStereo = 0;
                    for (MeasurementSite site : tkr.sites) {
                        if (site.hitID < 0) continue;
                        if (site.m.isStereo) nStereo++;
                        else nAxial++;
                    }
                    if (nAxial < minAxial || nStereo < minStereo[trial]) {
                        tkr.good = false;
                        for (KalHit ht : tkr.hits) {
                            ht.tkrCandidates.remove(tkr);
                        }
                        if (verbose) System.out.format("KalmanPatRecHPS: eliminating track candidate %d for %d axial hits and %d stereo hits\n", 
                                candidateList.indexOf(tkr), nAxial, nStereo);                        
                    }
                }
                if (verbose) {
                    for (TrackCandidate tkr : candidateList) {
                        System.out.format("%d ",candidateList.indexOf(tkr));
                        tkr.print("bad tracks removed", true);
                    }
                }
                // Eliminate candidates with too many shared hits, starting with the worst track
                for (int idx = candidateList.size()-1; idx>=0; --idx) {
                    TrackCandidate tkr = candidateList.get(idx);
                    int nShared = 0;
                    for (KalHit ht : tkr.hits) {
                        if (ht.tkrCandidates.size()>1) {
                            nShared++;
                        }
                    }
                    if (nShared > mxShared) {
                        tkr.good = false;
                        for (KalHit ht : tkr.hits) {
                            ht.tkrCandidates.remove(tkr);
                        }
                        if (verbose) System.out.format("KalmanPatRecHPS: eliminating track candidate %d for %d shared hits\n", 
                                candidateList.indexOf(tkr), nShared);  
                    }
                }
                if (verbose) {
                    for (TrackCandidate tkr : candidateList) {
                        System.out.format("%d ",candidateList.indexOf(tkr));
                        tkr.print("shared hits removed", false);
                    }
                }
            }               
            // Store the good candidates away in the KalTrack lists
            for (TrackCandidate tkrCand : candidateList) {
                if (!tkrCand.good) continue;
                if (storeTrack(tkID, tkrCand)) tkID++;
            }
        } // Next global iteration, using looser criteria

        // Sort the tracks by quality
        Collections.sort(TkrList, KalTrack.TkrComparator);
        
        // Remove shared hits unless the hit is very close to two tracks
        ArrayList<KalTrack> changedTracks = new ArrayList<KalTrack>();
        if (TkrList.size() > 0) {
            for (SiModule module : data) {
                for (Measurement hit : module.hits) {
                    if (hit.tracks.size() > 1) {
                        int minIDX = 1000;
                        KalTrack bestTkr = null;
                        for (KalTrack tkr : hit.tracks) {
                            int idx = TkrList.indexOf(tkr);
                            if (idx < 0) {
                                System.out.format("KalmanPatRecHPS: bad reference from hit to track. Track %d, Layer = %d\n", tkr.ID, module.Layer);
                            } else {
                                if (idx < minIDX) {
                                    minIDX = idx;
                                    bestTkr = tkr;
                                }
                            }
                        }
                        Set<KalTrack> tkrsToRemove = new HashSet<KalTrack>();
                        for (KalTrack tkr : hit.tracks) {
                            if (tkr == bestTkr) { // Keep the hit on the best track
                                continue;
                            }
                            int idx = tkr.whichSite(module);
                            if (idx < 0) {
                                System.out.format("KalmanPatRecHPS: bad reference from hit to track. Track %d, Layer = %d\n", tkr.ID, module.Layer);
                            } else {
                                MeasurementSite site = tkr.SiteList.get(idx);
                                if (verbose) {
                                    System.out.format("KalmanPatRecHPS: shall we remove a hit from Track %d, Layer %d with chi2inc=%10.5f?\n", 
                                            tkr.ID, module.Layer, site.chi2inc);
                                }
                                if (site.chi2inc > mxChi2double) {
                                    changedTracks.add(tkr);
                                    int oldID = site.hitID;
                                    if (!site.smoothed) System.out.format("KalmanPatRecHPS: oops, why isn't this site smoothed?");
                                    site.removeHit(verbose);
                                    if (verbose) {
                                        System.out.format("KalmanPatRecHPS: removing a hit from Track %d, Layer %d\n", tkr.ID, module.Layer);
                                    }
                                    // Check whether there might be another hit available                                   
                                    Measurement addedHit = site.addHit(tkr, mxChi2Inc[0], mxTdif, oldID);
                                    if (addedHit != null) {
                                        addedHit.tracks.add(tkr);
                                        Measurement newHit = site.m.hits.get(site.hitID);
                                        tkr.tMin = Math.min(tkr.tMin, hit.time);
                                        tkr.tMax = Math.max(tkr.tMax, hit.time);
                                        if (verbose) {
                                            System.out.format("KalmanPatRecHPS: added a hit after removing one for Track %d, Layer %d\n",
                                                    tkr.ID, module.Layer);
                                        }
                                    } else {
                                        tkr.SiteList.remove(site);
                                    }
                                    tkrsToRemove.add(tkr);
                                }
                            }
                        }
                        for (KalTrack tkr : tkrsToRemove) {
                            if (verbose) System.out.format("KalmanPatRecHPS: remove track %d from list of hit %d\n", tkr.ID, module.hits.indexOf(hit));
                            hit.tracks.remove(tkr);
                        }
                    }
                }
            }
        }
        
        // Refit the KalTracks
        ArrayList<KalTrack> allTks = new ArrayList<KalTrack>(TkrList.size());
        for (KalTrack tkr : TkrList) {
            allTks.add(tkr); 
        }
        for (int itkr = allTks.size()-1; itkr >= 0; --itkr) {
            KalTrack tkr = allTks.get(itkr);
            // Try to add hits on layers with missing hits
            tkr.addHits(data, mxResid[1], mxChi2Inc[0], mxTdif, verbose);
            
            // check that there are enough hits in both views
            int nStereo = 0;
            int nAxial = 0;
            int nShared = 0;
            for (MeasurementSite site : tkr.SiteList) {
                if (site.hitID < 0) continue;
                SiModule m = site.m;
                if (!m.isStereo) nAxial++;
                else nStereo++;
                if (verbose) {
                    System.out.format("KalmanPatRecHPS: track %d, layer %d, detector %d, hit=", tkr.ID, site.m.Layer, site.m.detector);
                    m.hits.get(site.hitID).print("on tkr");
                    System.out.format("\n");
                }                
                if (m.hits.get(site.hitID).tracks.size()>1) nShared++;
            }
            boolean removeIt = false;
            if (nStereo < minStereo[1]) {
                if (verbose) System.out.format("KalmanPatRecHPS: removing KalTrack %d for %d stereo hits\n", tkr.ID,nStereo);
                removeIt = true;
            } else if (nAxial < minAxial) {
                if (verbose) System.out.format("KalmanPatRecHPS: removing KalTrack %d for %d axial hits\n", tkr.ID,nAxial);
                removeIt = true;
            } else if (nAxial + nStereo < minHits1[nTries - 1]) {
                if (verbose) System.out.format("KalmanPatRecHPS: removing KalTrack %d for %d hits\n", tkr.ID,nStereo+nAxial);
                removeIt = true;
            } else if (nAxial + nStereo - nShared < minHits1[nTries -1]) {
                if (verbose) System.out.format("KalmanPatRecHPS: removing KalTrack %d for %d shared hits\n", tkr.ID,nShared);
                removeIt = true;
            }
            if (removeIt) {
                TkrList.remove(tkr);
                for (MeasurementSite site : tkr.SiteList) {
                    site.m.hits.get(site.hitID).tracks.remove(tkr);
                    site.hitID = -1;
                }
                continue;
            }
            
            if (verbose) System.out.format("KalmanPatRecHPS: Call the Kalman fit for track %d\n", tkr.ID);
            if (!tkr.fit(nIterations, verbose)) {
                if (verbose) System.out.format("KalmanPatRecHPS: removing KalTrack %d for bad fit!\n", tkr.ID);
                TkrList.remove(tkr);
                for (MeasurementSite site : tkr.SiteList) {
                    site.m.hits.get(site.hitID).tracks.remove(tkr);
                    site.hitID = -1;
                }
                continue;
            }
        }

        Collections.sort(TkrList, KalTrack.TkrComparator); // Sort tracks by quality
        if (verbose) {
            System.out.format("\n\n Printing the list of tracks found for event %d, top-bottom=%d:\n", eventNumber, topBottom);
            for (KalTrack tkr : TkrList) {
                tkr.originHelix();
                tkr.print(" ");
            }
            System.out.format("KalmanPatRecHPS done with event %d for top-bottom=%d\n\n", eventNumber, topBottom);
        }
    }

    // Remove the worst hit from lousy track candidates
    private boolean removeBadHits(TrackCandidate tkr, int trial) {
        
        if (tkr.chi2s/(double) tkr.hits.size() < chi2mx1[trial]) return false;
        if (tkr.hits.size() <= minHits1[trial]) return false;
        
        double mxChi2 = 0.;
        int idxBad = -1;
        for (int idx=0; idx<tkr.sites.size(); ++idx) {
            MeasurementSite site = tkr.sites.get(idx);
            if (tkr.seedLyrs.contains(site.m.Layer)) continue;
            if (site.chi2inc > mxChi2) {
                mxChi2 = site.chi2inc;
                idxBad = idx;
            }
        }
        if (verbose) {
            MeasurementSite badSite = tkr.sites.get(idxBad);
            System.out.format("KalmanPatRecHPS.removeBadHits: the worst non-seed layer is %d with chi2inc=%7.2f.\n", badSite.m.Layer, badSite.chi2inc);
        }
        if (idxBad >= 0) {
            if (mxChi2 > mxChi2Inc[trial]) {
                MeasurementSite site = tkr.sites.get(idxBad);
                Measurement badOne = site.m.hits.get(site.hitID);
                KalHit badHit = hitMap.get(badOne);
                if (badHit != null) {
                    if (verbose) System.out.format("KalmanPatRecHPS.removeBadHits: event %d, removing bad hit in layer %d with chi2=%9.3f.\n",eventNumber, tkr.sites.get(idxBad).m.Layer,mxChi2);
                    tkr.removeHit(badHit);
                    return true;
                }
            }
        }
        return false;
    }
    
    // Method to smooth an already filtered track candidate
    private void smoothTrack(TrackCandidate filteredTkr) {
        MeasurementSite nextSite = null;
        for (int idxS = filteredTkr.sites.size() - 1; idxS >= 0; idxS--) {
            MeasurementSite currentSite = filteredTkr.sites.get(idxS);
            if (nextSite == null) {
                if (currentSite.hitID < 0) continue;
                currentSite.aS = currentSite.aF.copy();
                currentSite.smoothed = true;
            } else {
                currentSite.smooth(nextSite, verbose);
            }
            filteredTkr.chi2s += Math.max(currentSite.chi2inc, 0.);

            nextSite = currentSite;
        }
        filteredTkr.smoothed = true;
    }

    // Execute the Kalman prediction and filter steps over a range of SVT layers
    private void filterTrack(TrackCandidate tkrCandidate, int lyrBegin, // layer on which to start the filtering
            int lyrEnd, // layer on which to end the filtering
            StateVector sI, // initialization state vector
            int trial, // trial level, for selecting cuts
            boolean startNew, // Start the fit over
            boolean pickUp    // true to allow picking up new hits
    ) {

        //if (tkrCandidate.hits.size() - tkrCandidate.nTaken < minUnique) {
        //    if (verbose) {
        //        System.out.format("KalmanPatRecHPS:filterTrack: skipping for hits used. nLyrs=%d, nTaken=%d\n", tkrCandidate.hits.size(),
        //                tkrCandidate.nTaken);
        //    }
        //    return;
        //}

        if (verbose) {
            Vec hprms = sI.a;
            System.out.format("\n KalmanPatRecHPS:filterTrack: Start filtering with drho=%10.5f phi0=%10.5f k=%10.5f dz=%10.5f tanl=%10.5f \n",
                    hprms.v[0], hprms.v[1], hprms.v[2], hprms.v[3], hprms.v[4]);
            for (KalHit hit : tkrCandidate.hits) hit.print(" existing ");
        }

        if (startNew) {
            tkrCandidate.chi2f = 0.;
            tkrCandidate.chi2s = 0.;
            tkrCandidate.sites.clear();
        }
        
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
        layerLoop: for (int lyr = lyrBegin; lyr != lyrEnd + direction; lyr += direction) {
            SiModule m = null;
            // Find the correct hit number and its module if this is one of the layers with an existing hit
            int hitno = -1;
            for (KalHit ht : tkrCandidate.hits) {
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
                double [] tRange = {tkrCandidate.tMax - mxTdif, tkrCandidate.tMin + mxTdif}; 
                if (prevSite == null) { // For first layer use the initializer state vector                   
                    rF = newSite.makePrediction(sI, null, hitno, tkrCandidate.nTaken <= mxShared, pickUp, imod < moduleList.get(lyr).size() - 1,
                            tRange, verbose);
                    if (rF > 0) {
                        if (m.hits.get(newSite.hitID).tracks.size() > 0) tkrCandidate.nTaken++;
                        tkrCandidate.tMin = Math.min(tkrCandidate.tMin, m.hits.get(newSite.hitID).time);
                        tkrCandidate.tMax = Math.max(tkrCandidate.tMax, m.hits.get(newSite.hitID).time);
                    } else if (rF < 0) {
                        if (rF == -2) continue;
                        if (verbose) {
                            System.out.format("KalmanPatRecHPS:filterTrack: Failed to make initial prediction at site %d, layer=%d. Abort\n",
                                    thisSite + 1, lyr);
                        }
                        return;
                    }
                } else {
                    rF = newSite.makePrediction(prevSite.aF, prevSite.m, hitno, tkrCandidate.nTaken <= mxShared, pickUp,
                            imod < moduleList.get(lyr).size() - 1, tRange, verbose);
                    if (rF > 0) {
                        if (m.hits.get(newSite.hitID).tracks.size() > 0) tkrCandidate.nTaken++;
                        tkrCandidate.tMin = Math.min(tkrCandidate.tMin, m.hits.get(newSite.hitID).time);
                        tkrCandidate.tMax = Math.max(tkrCandidate.tMax, m.hits.get(newSite.hitID).time);
                    } else if (rF < 0) {
                        if (rF == -2) continue;
                        if (verbose) {
                            System.out.format("KalmanPatRecHPS:filterTrack: Failed to make prediction at site %d, layer=%d.  Exit layer loop\n",
                                    thisSite + 1, lyr);
                        }
                        break layerLoop;
                    }
                }
                thisSite++;
                if (rF == 1 && hitno < 0) {
                    KalHit htNew = hitMap.get(m.hits.get(newSite.hitID));
                    tkrCandidate.hits.add(htNew);
                    if (verbose) htNew.print("new");
                }
                if (!newSite.filter(verbose)) {
                    if (verbose)
                        System.out.format("KalmanPatRecHPS:filterTrack: Failed to filter at site %d, layer=%d.  Ignore remaining sites\n",
                                thisSite, lyr);
                    return;
                }

                // if (verbose) newSite.print("initial filtering");
                tkrCandidate.chi2f += Math.max(newSite.chi2inc,0.);

                tkrCandidate.sites.add(newSite);
                prevSite = newSite;
                break;
            }
        }
 
        tkrCandidate.filtered = true;
        return;
    }

    boolean storeTrack(int tkID, TrackCandidate tkrCand) {
        //System.out.format("entering storeTrack for track %d, verbose=%b\n", tkID, verbose);
        KalTrack tkr = new KalTrack(eventNumber, tkID, tkrCand.hits.size(), tkrCand.sites, tkrCand.chi2s);
        boolean redundant = false;
        for (KalTrack oldTkr : TkrList) {
            if (tkr.equals(oldTkr)) {
                redundant = true;
                break;
            }
        }
        if (redundant) {
            if (verbose) System.out.format("KalmanPatRecHPS.storeTrack: throwing away redundant track %d\n", tkID);
            return false;
        } 
        for (MeasurementSite site : tkrCand.sites) {
            int theHit = site.hitID;
            if (theHit < 0) continue;
            site.m.hits.get(theHit).tracks.add(tkr); // Mark the hits as used
        }
        if (verbose) {
            System.out.format("KalmanPatRecHPS.storeTrack: Adding track %d with %d hits and smoothed chi^2=%10.5f\n",
                    tkID, tkrCand.hits.size(), tkrCand.chi2s);
        }
        tkrCand.kalTkrID = tkID;
        TkrList.add(tkr);
        return true;
    }
    
    static Comparator<Pair<Integer, Double>> pairComparator = new Comparator<Pair<Integer, Double>>() {
        public int compare(Pair<Integer, Double> p1, Pair<Integer, Double> p2) {
            if (p1.getSecondElement() < p2.getSecondElement()) {
                return 1;
            } else {
                return -1;
            }
        }
    };
    
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

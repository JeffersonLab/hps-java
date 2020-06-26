package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

class KalmanPatRecHPS {

    ArrayList<KalTrack> TkrList; // Final good tracks
    int topBottom;               // 0 for bottom tracker (+z), 1 for top (-z)
    
    private ArrayList<ArrayList<KalHit>> lyrHits;
    private ArrayList<ArrayList<SiModule>> moduleList;
    private Map<Measurement, KalHit> hitMap;

    private int eventNumber;
    private boolean verbose;
    private int nModules;
    private KalmanParams kPar;
    
    KalmanPatRecHPS(ArrayList<SiModule> data, int topBottom, int eventNumber, KalmanParams kPar, boolean verbose) {
        // topBottom = 0 for the bottom tracker (z>0); 1 for the top tracker (z<0)
        this.topBottom = topBottom;
        this.eventNumber = eventNumber;
        this.verbose = verbose;
        this.kPar = kPar;

        TkrList = new ArrayList<KalTrack>();
        nModules = data.size();
        int tkID = 100*topBottom + 1;
        
        // Make a list of hits for each tracker layer, 0 through 13
        // This is needed because one layer can have multiple SiModules
        // Also make a list of Si modules with hits in each layer
        int numLayers = 14;
        int firstLayer = 0; // (2 for pre-2019 data)
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
        
        Plane p0 = new Plane(new Vec(0., 0., 0.), new Vec(0., 1., 0.));

        if (verbose) {
            System.out.format("Entering KalmanPatRecHPS for event %d, top-bottom=%d with %d modules, for %d trials.\n", eventNumber, topBottom, nModules, KalmanParams.nTries);
            System.out.format("  KalmanPatRecHPS: list of the seed strategies to be applied:\n");
            for (int[] list : kPar.lyrList[topBottom]) {
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
        for (int trial = 0; trial < KalmanParams.nTries; trial++) {
            int candID = topBottom*1000 + trial*100 + 1;
            if (verbose) System.out.format("\nKalmanPatRecHPS: start of pass %d through the algorithm.\n", trial);
            
            // Remove references from the hits to the track candidates of the previous iteration
            if (trial != 0) {
                for (int lyr = 0; lyr < numLayers; lyr++) {
                    for (KalHit ht : lyrHits.get(lyr)) {
                        ht.tkrCandidates.clear();
                    }
                }
            }
            ArrayList<TrackCandidate> candidateList = new ArrayList<TrackCandidate>();
            for (int[] list : kPar.lyrList[topBottom]) {
                int nLyrs = list.length;
                //PF::This value is OK for 2016, what about 2019 (probably should be 0) ?!!
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
                                    if (tmax - tmin > kPar.mxTdif) {
                                        //if (verbose) {
                                        //    System.out.format("KalmanPatRecHPS: skipping seed with tdif=%8.2f\n Hits:  ", tmax-tmin);
                                        //    for (KalHit ht : hitList) ht.print("short");
                                        //    System.out.format("\n");
                                        //}
                                        continue;
                                    }
                                    SeedTrack seed = new SeedTrack(hitList, yOrigin, false);
                                    if (!seed.success) continue;
                                    // Cuts on the seed quality
                                    Vec hp = seed.helixParams();
                                    Vec pInt = seed.planeIntersection(p0);
                                    
                                    if (verbose) {
                                        System.out.format("Seed %d %d %d %d %d parameters for cuts: K=%10.5f, tanl=%10.5f, dxz=%10.5f   ",
                                                          idx[0], idx[1], idx[2], idx[3], idx[4], hp.v[2], hp.v[4], pInt.mag());
                                    }
                                    
                                    boolean seed_passes_cuts = false;
                                    
                                    if (Math.abs(hp.v[2]) < kPar.kMax[trial]) {
                                        if (Math.abs(hp.v[4]) < kPar.tanlMax[trial]) {
                                            if (verbose) 
                                                System.out.format("intersection with target plane= %9.3f %9.3f %9.3f \n", pInt.v[0],
                                                                  pInt.v[1], pInt.v[2]);
                                            if (pInt.mag() < kPar.dRhoMax[trial]) {
                                                if (Math.abs(pInt.v[2]) < kPar.dzMax[trial]) 
                                                    seed_passes_cuts = true;
                                                //seedList.add(seed);
                                            }//Check intersection with target plane
                                        }//Check tanLambda
                                    }//Check curvature
                                    
                                    //Good seed; check for duplicates
                                    if (seed_passes_cuts) {
                                        boolean reject_seed = false;
                                        for (SeedTrack sel_seed : seedList) {
                                            reject_seed = seed.isCompatibleTo(sel_seed,kPar.seedCompThr);
                                            if (reject_seed)
                                                break;
                                        }
                                        if (!reject_seed)
                                            seedList.add(seed);
                                    }
                                    if (verbose) System.out.format("\n");
                                }
                            }
                        }
                    }
                }
                
                if (verbose) {
                    System.out.printf("KalmanPatRecHPS::SeedList size = %d \n", seedList.size());
                    //for (SeedTrack seed : seedList) {
                    //  seed.print("PF::Check Seed");
                    //}
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
                    TrackCandidate candidateTrack = new TrackCandidate(candID, seed.hits, kPar.mxShared, kPar.mxTdif, hitMap, eventNumber);
                    candID++;
                    filterTrack(candidateTrack, list[0], numLayers - 1, sI, trial, true, true);
                    if (!candidateTrack.filtered) {
                        if (verbose) {
                            System.out.format("KalmanPatRecHPS: filtering of this seed failed. chi2=%10.5f, nHits=%d\n", candidateTrack.chi2f,
                                    candidateTrack.hits.size());
                        }
                        continue seedLoop;
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

                    if (candidateTrack.sites.size() < kPar.minHits0) {
                        if (verbose) System.out.format("KalmanPatRecHPS: Initial filtering has too few sites, n=%d<%d. Skip to the next seed.\n",
                                candidateTrack.sites.size(), kPar.minHits0);
                        continue seedLoop;                        
                    }
                    if (candidateTrack.hits.size() < kPar.minHits0) {
                        if (verbose) System.out.format("KalmanPatRecHPS: Initial filtering has too few hits, n=%d<%d. Skip to the next seed.\n",
                                candidateTrack.hits.size(), kPar.minHits0);
                        continue seedLoop;
                    }
                    if (candidateTrack.chi2f / (double) candidateTrack.hits.size() > kPar.chi2mx1[trial]) {
                        if (verbose) System.out.format("KalmanPatRecHPS: Initial filtering has too large chi^2. Skip to the next seed.\n");
                        continue seedLoop;
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
                            continue seedLoop;
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

                    if (candidateTrack.hits.size() < kPar.minHits1[trial]) {
                        if (verbose) {
                            System.out.format("KalmanPatRecHPS: Filtering of %d to layer 0 has too few hits, %d<%d. Skip to the next seed.\n",
                                    candidateTrack.ID, candidateTrack.hits.size(), kPar.minHits1[trial]);
                        }
                        candidateTrack.good = false;
                        continue seedLoop;
                    }
                    boolean hitChanges = false;
                    if (candidateTrack.chi2f / (double) candidateTrack.hits.size() > kPar.chi2mx1[trial]) {
                        // See if the chi^2 will be okay if just one hit is removed
                        boolean removedHit = false;
                        if (candidateTrack.sites.size() > (kPar.minAxial + kPar.minStereo[trial])) {
                            MeasurementSite siteR = null;
                            for (MeasurementSite site : candidateTrack.sites) {
                                if (candidateTrack.seedLyrs.contains(site.m.Layer)) continue;  // Don't alter seed layers
                                if (site.hitID < 0) continue;
                                if ((candidateTrack.chi2f - site.chi2inc)/(double) candidateTrack.hits.size() < kPar.chi2mx1[trial]) {
                                    siteR = site;
                                    break;
                                }
                            }
                            if (siteR != null) {
                                KalHit hitR = hitMap.get(siteR.m.hits.get(siteR.hitID));
                                if (hitR != null) {
                                    //if (!candidateTrack.hits.contains(hitR)) System.out.format("Oops, missing hit!\n");
                                    candidateTrack.removeHit(hitR);           
                                    if (verbose) System.out.format("KalmanPatRecHPS event %d candidate %d, removing hit from layer %d detector %d\n", 
                                            eventNumber, candidateTrack.ID, siteR.m.Layer, siteR.m.detector);
                                    removedHit = true;
                                    //candidateTrack.print("with hit removed", true);
                                } else {
                                    System.out.format("KalmanPatRecHPS error: missing hit in layer %d detector %d\n", siteR.m.Layer, siteR.m.detector);
                                }
                            }
                        }
                        if (!removedHit) {
                            if (verbose) {
                                System.out.format("KalmanPatRecHPS: Filtering of %d to layer 0 has too large chi^2. Skip to the next seed.\n", candidateTrack.ID);
                            }
                            candidateTrack.good = false;
                            continue seedLoop;
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
                        if (verbose) { System.out.format("KalmanPatRecHPS: %d failed filtering of all layers. Try next seed.\n", candidateTrack.ID); }
                        candidateTrack.good = false;
                        continue seedLoop;
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
                    if (nStereo < kPar.minStereo[trial]) {
                        if (verbose) { System.out.format("KalmanPatRecHPS: filtering of %d has too few stereo hits. Skip to the next seed.\n", candidateTrack.ID); }
                        candidateTrack.good = false;
                        continue seedLoop;
                    }
                    if (candidateTrack.hits.size() - nStereo < kPar.minAxial) {
                        if (verbose) { System.out.format("KalmanPatRecHPS: filtering of %d has too few non-stereo hits. Skip to the next seed.\n", candidateTrack.ID); }
                        candidateTrack.good = false;
                        continue seedLoop;
                    }

                    // Finally smooth back to the target
                    smoothTrack(candidateTrack);
                    if (verbose) candidateTrack.print("after smoothing", false);
                    
                    // Junk highly curved candidates that don't even intersect the y=0 plane
                    StateVector aS = candidateTrack.sites.get(0).aS;
                    double phi0 = aS.planeIntersect(new Plane(new Vec(0.,0.,0.), new Vec(0.,1.,0.)));
                    if (Double.isNaN(phi0)) {
                        if (verbose) System.out.format("KalmanPatRecHPS: marking track candidate %d bad, as it does not intersect the origin plane.\n", candidateTrack.ID);
                        candidateTrack.good = false;
                        continue seedLoop;
                    }                   
                    
                    // Check if the track can be improved by removing hits
                    if (removeBadHits(candidateTrack, trial)) {
                        if (verbose) System.out.format("KalmanPatRecHPS: Refit candidate track %d after removing a hit.\n", candidateTrack.ID);
                        if (candidateTrack.reFit(verbose)) {
                            if (verbose) candidateTrack.print("after refitting and smoothing", false);
                        } else {
                            candidateTrack.good = false;
                            continue seedLoop;
                        }
                        hitChanges = true;
                    }
                    
                    if (!candidateTrack.smoothed) {
                        if (verbose) System.out.format("KalmanPatRecHPS: candidate %d smoothing failed.\n", candidateTrack.ID);
                        candidateTrack.good = false;
                        continue seedLoop;
                    }
                    if (candidateTrack.chi2s/(double)candidateTrack.numHits() > kPar.chi2mx1[trial]) {
                        if (verbose) System.out.format("KalmanPatRecHPS: candidate %d chi^2 is too large.\n", candidateTrack.ID);
                        candidateTrack.good = false;
                        continue seedLoop;
                    }
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
                                //if (verbose) System.out.format("KalmanPatRecHPS: candidate match new=%8.3f old=%8.3f\n",candidateTrack.chi2s,oldCandidate.chi2s);
                                if (verbose) System.out.format("KalmanPatRecHPS: candidate track is redundant (2). Skip to next seed.\n");
                                candidateTrack.good = false;
                                continue seedLoop;
                            }
                        }                        
                    }
                    // Sometimes bad track candidates have covariance entries that are not numbers. Junk those.
                    startSite = null;
                    for (MeasurementSite site : candidateTrack.sites) {
                        if (site.aS != null) startSite = site;
                    }
                    SquareMatrix Cov = startSite.aS.C;
                    for (int ix=0; ix<5; ++ix) {
                        for (int iy=0; iy<5; ++iy) {
                            if (Double.isNaN(Cov.M[ix][iy])) {
                                if (verbose) System.out.format("KalmanPatRecHPs: candidate %d covariance is NaN!\n", candidateTrack.ID);
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
                                if (Math.abs(helix.v[0]) < kPar.dRhoMax[trial]) {
                                    if (Math.abs(helix.v[3]) < kPar.dzMax[trial]) {
                                        if (verbose) {
                                            System.out.format("KalmanPatRecHPS: keeping a near perfect track candidate %d\n", candidateTrack.ID);
                                            candidateTrack.print("the perfect one", true);
                                        }
                                        if (storeTrack(tkID, candidateTrack)) {
                                            tkID++;
                                            candidateList.remove(candidateTrack);
                                            if (verbose) {
                                                System.out.format("KalmanPatRecHPS: current list of other track candidates:\n");
                                                for (TrackCandidate tkr : candidateList) {
                                                    System.out.format("   %d good=%b: ", tkr.ID, tkr.good);
                                                    for (MeasurementSite site : tkr.sites) {
                                                        System.out.format("(%d, %d, %d) ", site.m.Layer, site.m.detector, site.hitID);
                                                    }
                                                    System.out.format("\n");
                                                }
                                            }
                                            for (KalHit ht : candidateTrack.hits) {
                                                Set<TrackCandidate> tksToRemove = new HashSet<TrackCandidate>();
                                                for (TrackCandidate otherCand : ht.tkrCandidates) {  // Note: this candidate is not yet marked in the KalHits, see below
                                                    if (otherCand.nTaken >= kPar.mxShared | !otherCand.good) {
                                                        tksToRemove.add(otherCand);
                                                        if (verbose) {
                                                            System.out.format("KalmanPatRecHPS: remove a shared hit (%d %d %d) from already found candidate %d:\n",
                                                                    ht.module.Layer, ht.module.detector, ht.module.hits.indexOf(ht.hit), otherCand.ID);
                                                            //otherCand.print("the other one", true);
                                                            //ht.print("the shared hit");
                                                        }
                                                    } else {
                                                        otherCand.nTaken++;
                                                        if (otherCand.nTaken > kPar.mxShared) otherCand.good = false;
                                                    }
                                                }
                                                for (TrackCandidate tkr : tksToRemove) {
                                                    tkr.removeHit(ht);  // This will also remove the reference from the hit to this candidate
                                                    if (verbose) tkr.print("after hit removal", true);
                                                }
                                            }
                                        } else candidateTrack.good = false;
                                    }
                                }
                            }
                        }
                    }
                    
                    // Here we have a good track candidate. Mark the hits in KalHit as used by this candidate.
                    for (KalHit hit : candidateTrack.hits) {
                        boolean foundIt = false;
                        for (MeasurementSite site : candidateTrack.sites) {
                            if (site.m == hit.module) {
                                foundIt = true;
                                break;
                            }
                        }
                        if (!foundIt) {
                            System.out.format("KalmanPatRecHPS event %d, missing site\n", eventNumber);
                            hit.print("the hit");
                            candidateTrack.print("the candidate", true);
                            System.out.format("       Sites: ");
                            for (MeasurementSite site : candidateTrack.sites) {
                                System.out.format("(%d, %d, %d) ",site.m.Layer, site.m.detector, site.hitID);
                            }
                            System.out.format("\n");
                        }
                        hit.tkrCandidates.add(candidateTrack);
                    } 
                } // Next seed in set
            } // Next set of seeds
            if (verbose) {
                int nGood = 0;
                for (TrackCandidate tkr : candidateList) {
                    if (tkr.good) nGood++;
                }
                System.out.format("KalmanPatRecHPS for event %d, completed loop over seeds for iteration %d. %d good track candidates.\n", 
                        eventNumber, trial, nGood);
                System.out.format("KalmanPatRecHPS: list of KalTracks already stored:\n");
                for (KalTrack tkr : TkrList) { 
                    System.out.format("  KalmanPatRecHPS: list of sites on KalTrack track %d: ", tkr.ID);
                    for (MeasurementSite site : tkr.SiteList) {
                        System.out.format("(%d, %d, %d) ",site.m.Layer, site.m.detector, site.hitID);
                    }
                    System.out.format("\n");
                }
            }
            // Eliminate from the list all track candidates that are not good
            Iterator<TrackCandidate> iter = candidateList.iterator();
            while (iter.hasNext()) {
                TrackCandidate tkr = iter.next();
                if (!tkr.good) {
                    if (verbose) {
                        System.out.format("KalmanPatRecHPS: removing bad track candidate %d\n", tkr.ID);
                        tkr.print("being removed", true);
                    }
                    for (KalHit ht : tkr.hits) {
                        ht.tkrCandidates.remove(tkr);
                    }
                    iter.remove();               
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
                        Iterator<TrackCandidate> iterator = hit.tkrCandidates.iterator();
                        while (iterator.hasNext()) {
                            TrackCandidate tkr = iterator.next();   // Remove references to tracks no longer in list
                            if (!candidateList.contains(tkr)) iterator.remove();
                        }
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
                            if (verbose) System.out.format("KalmanPatRecHPS: best candidate track for this hit is %d, chi2=%7.3f, of %d candidates\n", candidateList.get(iBest).ID, bestTkr.chi2s, candidateList.size());
                            Set<TrackCandidate> tkrToRemove = new HashSet<TrackCandidate>();
                            for (TrackCandidate tkr : hit.tkrCandidates) {
                                if (tkr == bestTkr) continue;
                                for (MeasurementSite site : tkr.sites) {
                                    if (site.m == hit.module) {
                                        if (site.chi2inc > kPar.mxChi2double || Math.abs(site.aS.r/hit.hit.sigma) > kPar.mxResidShare/2.) {
                                            tkrToRemove.add(tkr);
                                            break;
                                        }
                                    }
                                }
                            }
                            for (TrackCandidate tkr : tkrToRemove) {
                                if (verbose) {
                                    System.out.format("KalmanPatRecHPS: hit %d removed from track candidate %d on layer %d detector %d\n", 
                                            hit.module.hits.indexOf(hit.hit), tkr.ID, hit.module.Layer, hit.module.detector);
                                }
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
                    if (nAxial < kPar.minAxial || nStereo < kPar.minStereo[trial]) {
                        tkr.good = false;
                        for (KalHit ht : tkr.hits) {
                            ht.tkrCandidates.remove(tkr);
                        }
                        if (verbose) System.out.format("KalmanPatRecHPS: eliminating track candidate %d for %d axial hits and %d stereo hits\n", 
                                tkr.ID, nAxial, nStereo);                        
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
                    if (nShared > kPar.mxShared) {
                        tkr.good = false;
                        for (KalHit ht : tkr.hits) {
                            ht.tkrCandidates.remove(tkr);
                        }
                        if (verbose) System.out.format("KalmanPatRecHPS: eliminating track candidate %d for %d shared hits\n", tkr.ID, nShared);  
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
                else tkrCand.good = false;
            }
            if (verbose) {
                System.out.format("KalmanPatRecHPS: list of stored tracks after iteration %d:\n", trial);
                for (KalTrack tkr : TkrList) { 
                    System.out.format("  KalmanPatRecHPS: list of sites on KalTrack track %d: ", tkr.ID);
                    for (MeasurementSite site : tkr.SiteList) {
                        System.out.format("(%d, %d, %d) ",site.m.Layer, site.m.detector, site.hitID);
                    }
                    System.out.format("\n");
                }
            }
        } // Next global iteration, using looser criteria

        // Sort the tracks by quality
        Collections.sort(TkrList, KalTrack.TkrComparator);

        if (verbose) {
            System.out.format("KalmanPatRecHPS: list of sorted KalTracks before removing shared hits:\n");
            for (KalTrack tkr : TkrList) { 
                System.out.format("  KalmanPatRecHPS: list of sites on KalTrack track %d: ", tkr.ID);
                for (MeasurementSite site : tkr.SiteList) {
                    System.out.format("(%d, %d, %d) ",site.m.Layer, site.m.detector, site.hitID);
                }
                System.out.format("\n");
            }
        }
        
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
                                if (site.chi2inc > kPar.mxChi2double) {
                                    changedTracks.add(tkr);
                                    int oldID = site.hitID;
                                    if (!site.smoothed) System.out.format("KalmanPatRecHPS: oops, why isn't this site smoothed?");
                                    site.removeHit(verbose);
                                    if (verbose) {
                                        System.out.format("KalmanPatRecHPS: removing a hit from Track %d, Layer %d\n", tkr.ID, module.Layer);
                                    }
                                    // Check whether there might be another hit available                                   
                                    Measurement addedHit = site.addHit(tkr, kPar.mxChi2Inc, kPar.mxTdif, oldID);
                                    if (addedHit != null) {
                                        addedHit.tracks.add(tkr);
                                        Measurement newHit = site.m.hits.get(site.hitID);
                                        tkr.tMin = Math.min(tkr.tMin, newHit.time);
                                        tkr.tMax = Math.max(tkr.tMax, newHit.time);
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
            if (verbose) {
                System.out.format("KalmanPatRecHPS: list of sites on KalTrack track %d: ", tkr.ID);
                for (MeasurementSite site : tkr.SiteList) {
                    System.out.format("(%d, %d, %d) ",site.m.Layer, site.m.detector, site.hitID);
                }
                System.out.format("\n");
            }
        }
        for (int itkr = allTks.size()-1; itkr >= 0; --itkr) {
            KalTrack tkr = allTks.get(itkr);
            // Try to add hits on layers with missing hits
            tkr.addHits(data, kPar.mxResid[1], kPar.mxChi2Inc, kPar.mxTdif, verbose);
            
            // check that there are enough hits in both views
            int nStereo = 0;
            int nAxial = 0;
            int nShared = 0;
            for (MeasurementSite site : tkr.SiteList) {
                if (site.hitID < 0) continue;
                SiModule m = site.m;
                if (!m.isStereo) nAxial++;
                else nStereo++;
                //if (verbose) {
                //    System.out.format("KalmanPatRecHPS: track %d, layer %d, detector %d, hit=", tkr.ID, site.m.Layer, site.m.detector);
                //    m.hits.get(site.hitID).print("on tkr");
                //    System.out.format("\n");
                //}                
                if (m.hits.get(site.hitID).tracks.size()>1) nShared++;
            }
            boolean removeIt = false;
            if (nStereo < kPar.minStereo[1]) {
                if (verbose) System.out.format("KalmanPatRecHPS: removing KalTrack %d for %d stereo hits\n", tkr.ID,nStereo);
                removeIt = true;
            } else if (nAxial < kPar.minAxial) {
                if (verbose) System.out.format("KalmanPatRecHPS: removing KalTrack %d for %d axial hits\n", tkr.ID,nAxial);
                removeIt = true;
            } else if (nAxial + nStereo < kPar.minHits1[KalmanParams.nTries - 1]) {
                if (verbose) System.out.format("KalmanPatRecHPS: removing KalTrack %d for %d hits\n", tkr.ID,nStereo+nAxial);
                removeIt = true;
            } else if (nAxial + nStereo - nShared < kPar.minHits1[KalmanParams.nTries -1]) {
                if (verbose) System.out.format("KalmanPatRecHPS: removing KalTrack %d for %d shared hits\n", tkr.ID,nShared);
                removeIt = true;
            }
            if (removeIt) {
                TkrList.remove(tkr);
                for (MeasurementSite site : tkr.SiteList) {
                    if (site.hitID!=-1) {
                        site.m.hits.get(site.hitID).tracks.remove(tkr);
                    }
                    else {
                        System.out.format("KalmanPatRecHPS: Removing track from measurement site with hitID=-1. Skipping removal.");
                    }
                    site.hitID = -1;
                }
                continue;
            }
            
            if (verbose) System.out.format("KalmanPatRecHPS: Call the Kalman fit for track %d\n", tkr.ID);
            
            boolean goodFit = tkr.fit(kPar.nIterations, verbose);
            if (goodFit) {
                StateVector aS = tkr.SiteList.get(0).aS;
                if (aS != null) {
                    double phi0 = aS.planeIntersect(new Plane(new Vec(0.,0.,0.), new Vec(0.,1.,0.)));
                    if (Double.isNaN(phi0)) {
                        if (verbose) System.out.format("KalmanPatRecHPS: track %d does not intersect the origin plane!\n", tkr.ID);
                        goodFit = false;
                    }
                } else {
                    if (verbose) System.out.format("KalmanPatRecHPS: track %d has no smoothed state vector at site 0!\n", tkr.ID);
                    goodFit = false;
                }
                if (goodFit) {
                    if (!tkr.originHelix()) {
                        if (verbose) System.out.format("KalmanPatRecHPS: propagating track %d to the origin failed!\n", tkr.ID);
                        goodFit = false;
                    };
                }
            }
            if (!goodFit) {
                if (verbose) System.out.format("KalmanPatRecHPS: removing KalTrack %d for bad fit!\n", tkr.ID);
                TkrList.remove(tkr);
                for (MeasurementSite site : tkr.SiteList) {
                    if (site.hitID!=-1) {
                        site.m.hits.get(site.hitID).tracks.remove(tkr);
                    }
                    else {
                        System.out.format("KalmanPatRecHPS: Removing track from measurement site with hitID=-1. Skipping removal.");
                    }
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
        
        if (tkr.chi2s/(double) tkr.hits.size() < kPar.chi2mx1[trial]) return false;
        if (tkr.hits.size() <= kPar.minHits1[trial]) return false;
        
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
            if (mxChi2 > kPar.minChi2IncBad) {
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
            System.out.format("\n KalmanPatRecHPS:filterTrack: Start filtering candidate %d with drho=%10.5f phi0=%10.5f k=%10.5f dz=%10.5f tanl=%10.5f \n",
                    tkrCandidate.ID, hprms.v[0], hprms.v[1], hprms.v[2], hprms.v[3], hprms.v[4]);
        }

        if (startNew) {
            tkrCandidate.filtered = false;
            tkrCandidate.chi2f = 0.;
            tkrCandidate.chi2s = 0.;
            tkrCandidate.sites.clear();
        }
        
        MeasurementSite newSite = null;
        MeasurementSite prevSite = null;
        Map<KalHit, MeasurementSite> siteMap = new HashMap<KalHit, MeasurementSite>(tkrCandidate.hits.size());
        for (KalHit hit : tkrCandidate.hits) {
            if (verbose) {
                System.out.format("    On entering filterTrack: ");
                hit.print(" existing ");
            }
            siteMap.put(hit, null);
        }

        int thisSite = -1;
        // Filter from the start of the seed to the last downstream detector layer
        // loop over all layers from the seed beginning to the end of the tracker
        int direction;
        if (lyrEnd > lyrBegin) {
            direction = 1;
        } else {
            direction = -1;
        }
        boolean needCleanup = false;
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

                if (verbose) System.out.format("KalmanPatRecHPS.filterTrack: try prediction at layer %d, detector %d, %d hits.\n",
                        m.Layer, m.detector, m.hits.size());
                newSite = new MeasurementSite(lyr, m, kPar.mxResid[trial], kPar.mxResidShare);
                int rF;
                double [] tRange = {tkrCandidate.tMax - kPar.mxTdif, tkrCandidate.tMin + kPar.mxTdif}; 
                if (prevSite == null) { // For first layer use the initializer state vector                   
                    rF = newSite.makePrediction(sI, null, hitno, tkrCandidate.nTaken <= kPar.mxShared, pickUp, imod < moduleList.get(lyr).size() - 1,
                            tRange, verbose);
                    if (rF > 0) {
                        if (m.hits.get(newSite.hitID).tracks.size() > 0) tkrCandidate.nTaken++;
                        tkrCandidate.tMin = Math.min(tkrCandidate.tMin, m.hits.get(newSite.hitID).time);
                        tkrCandidate.tMax = Math.max(tkrCandidate.tMax, m.hits.get(newSite.hitID).time);
                    } else if (rF < 0) {
                        if (rF == -2) {  // This really shouldn't happen at the initial site
                            if (verbose) System.out.format("KalmanPatRecHPS.filterTrack: not within detector boundary on layer %d detector %d\n",
                                    newSite.m.Layer, newSite.m.detector);
                            continue;
                        }
                        if (verbose) {
                            System.out.format("KalmanPatRecHPS:filterTrack: Failed to make initial prediction at site %d, layer=%d. Abort\n",
                                    thisSite + 1, lyr);
                        }
                        for (KalHit hit : tkrCandidate.hits) {
                            hit.tkrCandidates.remove(tkrCandidate);                            
                        }
                        tkrCandidate.good = false;
                        tkrCandidate.hits.clear();
                        return;
                    }
                } else {
                    rF = newSite.makePrediction(prevSite.aF, prevSite.m, hitno, tkrCandidate.nTaken <= kPar.mxShared, pickUp,
                            imod < moduleList.get(lyr).size() - 1, tRange, verbose);
                    if (rF > 0) {
                        if (m.hits.get(newSite.hitID).tracks.size() > 0) tkrCandidate.nTaken++;
                        tkrCandidate.tMin = Math.min(tkrCandidate.tMin, m.hits.get(newSite.hitID).time);
                        tkrCandidate.tMax = Math.max(tkrCandidate.tMax, m.hits.get(newSite.hitID).time);
                    } else if (rF < 0) {
                        if (rF == -2) {
                            if (verbose) System.out.format("KalmanPatRecHPS.filterTrack: not within detector boundary on layer %d detector %d\n",
                                    newSite.m.Layer, newSite.m.detector);
                            continue;
                        }
                        if (verbose) {
                            System.out.format("KalmanPatRecHPS:filterTrack: Failed to make prediction at site %d, layer=%d.  Exit layer loop\n",
                                    thisSite + 1, lyr);
                        }
                        needCleanup = true;
                        break layerLoop;
                    }
                }
                if (verbose) {
                    System.out.format("KalmanPatRecHPS.filterTrack: candidate %d, completed prediction at site (%d, %d, %d)\n",
                            tkrCandidate.ID, newSite.m.Layer, newSite.m.detector, newSite.hitID);
                }
                thisSite++;
                if (!newSite.filter(verbose)) {
                    if (verbose) System.out.format("KalmanPatRecHPS:filterTrack: Failed to filter at site %d, layer=%d.  Ignore remaining sites\n", thisSite, lyr);
                    needCleanup = true;
                    break layerLoop;
                }
                if (verbose) {
                    System.out.format("KalmanPatRecHPS.filterTrack: candidate %d, completed filter at site (%d, %d, %d), chi2-inc=%8.3f\n",
                            tkrCandidate.ID, newSite.m.Layer, newSite.m.detector, newSite.hitID, newSite.chi2inc);
                }
                KalHit theHit = null;
                if (newSite.hitID >= 0) theHit = hitMap.get(m.hits.get(newSite.hitID));
                if (rF == 1 && hitno < 0) {
                    tkrCandidate.hits.add(theHit);
                    if (verbose) theHit.print("new");
                }

                // if (verbose) newSite.print("initial filtering");
                tkrCandidate.chi2f += Math.max(newSite.chi2inc,0.);

                tkrCandidate.sites.add(newSite);
                if (theHit != null) siteMap.put(theHit, newSite);
                
                prevSite = newSite;
                break;
            }
        }
        if (needCleanup) {
            Iterator<KalHit> itr = tkrCandidate.hits.iterator();
            int nstereo = 0;
            while (itr.hasNext()) {   // Clean up the hit list
                KalHit hit = itr.next();
                MeasurementSite theSite = siteMap.get(hit);
                int hitID = -1;
                if (theSite != null) hitID = theSite.hitID;
                if (hitID < 0) {
                    if (verbose) System.out.format("    KalmanPatRecHPS.filterTrack: remove hit from candidate at layer %d\n", hit.module.Layer);
                    hit.tkrCandidates.remove(tkrCandidate);
                    itr.remove();
                } else if (hit.module.isStereo) nstereo++;
            }
            if (tkrCandidate.hits.size() < kPar.minHits1[trial]) tkrCandidate.good = false;
            if (nstereo < kPar.minStereo[trial]) tkrCandidate.good = false;
        }
        tkrCandidate.filtered = true;
        return;
    }

    boolean storeTrack(int tkID, TrackCandidate tkrCand) {
        //System.out.format("entering storeTrack for track %d, verbose=%b\n", tkID, verbose);
        MeasurementSite firstSite = null;
        for (MeasurementSite site : tkrCand.sites) {
            if (site.hitID >= 0) firstSite = site;
        }
        if (Math.abs(firstSite.aS.a.v[2]) > kPar.kMax[1]) {
            if (verbose) System.out.format("KalmanPatRecHPS.storeTrack: k=%10.4f is too large for candidate %d\n", 
                    firstSite.aS.a.v[2], tkrCand.ID);
            return false;
        }
        if (Math.abs(firstSite.aS.a.v[0]) > kPar.dRhoMax[1]) {
            if (verbose) System.out.format("KalmanPatRecHPS.storeTrack: dRho=%10.4f is too large for candidate %d\n", 
                    firstSite.aS.a.v[0], tkrCand.ID);
            return false;
        }
        if (Math.abs(firstSite.aS.a.v[3]) > kPar.dzMax[1]) {
            if (verbose) System.out.format("KalmanPatRecHPS.storeTrack: dz=%10.4f is too large for candidate %d\n", 
                    firstSite.aS.a.v[3], tkrCand.ID);
            return false;
        }
        
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
        for (MeasurementSite site : tkr.SiteList) {
            int theHit = site.hitID;
            if (theHit < 0) continue;
            site.m.hits.get(theHit).tracks.add(tkr); // Mark the hits as used
        }
        if (verbose) {
            System.out.format("KalmanPatRecHPS.storeTrack: Adding track %d with %d hits and smoothed chi^2=%10.5f\n",
                    tkID, tkrCand.hits.size(), tkrCand.chi2s);
            System.out.format(" Complete list of sites on this track: ");
            for (MeasurementSite site : tkr.SiteList) {
                System.out.format("(%d, %d, %d) ",site.m.Layer, site.m.detector, site.hitID);
            }
            System.out.format("\n");
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

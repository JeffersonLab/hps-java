package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
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
    private ArrayList<Double> yScat;
    private ArrayList<Double> XLscat;

    private int eventNumber;
    final static private boolean debug = true;
    private int nModules;
    private KalmanParams kPar;
    private Logger logger;
    private double [] zh;
    private double [] yh;
    private boolean [] ah;

    KalmanPatRecHPS(ArrayList<SiModule> data, int topBottom, int eventNumber, KalmanParams kPar) {
        // topBottom = 0 for the bottom tracker (z>0); 1 for the top tracker (z<0)
        
        logger = Logger.getLogger(KalmanPatRecHPS.class.getName());
        this.topBottom = topBottom;
        this.eventNumber = eventNumber;
        this.kPar = kPar;  

        // Some arrays for making preliminary cuts on seeds
        zh = new double[5];    // global z coordinate of axial hit
        yh = new double[5];    // global y coordinate of axial hit
        ah = new boolean[5];   // true if stereo layer, false for axial
        
        double [] vtx = {0., -10., 0.};
        double [][] vtxCov = new double[3][3];
        vtxCov[0][0] = 1.0;
        vtxCov[1][1] = 9.0;
        vtxCov[2][2] = 0.25;
        
        TkrList = new ArrayList<KalTrack>();
        nModules = data.size();
        int tkID = 100*topBottom + 1;
        
        // Make a list of hits for each tracker layer, 0 through 13
        // This is needed because one layer can have multiple SiModules
        // Also make a list of Si modules with hits in each layer
        int numLayers = 14;
        int firstLayer = kPar.firstLayer;
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
            moduleList.get(thisSi.Layer).add(thisSi);
        }

        // Arrays for covariance matrix propagation
        yScat = new ArrayList<Double>(numLayers);  // List of approx. y locations where scattering in Si occurs
        XLscat = new ArrayList<Double>(numLayers); // Thicknesses of the scattering layers, in radiation lengths
        
        if (debug) System.out.format("Entering KalmanPatRecHPS for event %d, top-bottom=%d with %d modules, for %d trials.\n", 
                                         eventNumber, topBottom, nModules, kPar.nTrials);
        
        double rho = 2.329;                   // Density of silicon in g/cm^2
        double radLen = (21.82 / rho) * 10.0; // Radiation length of silicon in millimeters
        for (int lyr = 0; lyr < numLayers; lyr++) {
            if (debug) {
                System.out.format("Layer %d modules:  ", lyr);
                for (SiModule thisSi : moduleList.get(lyr)) {
                    System.out.format("det=%d %d hits, ", thisSi.detector, thisSi.hits.size());
                }
                System.out.format("\n");
            }
            if (moduleList.get(lyr).size() > 0) {
                SiModule thisSi = moduleList.get(lyr).get(0);
                yScat.add(thisSi.p.X().v[1]);
                XLscat.add(thisSi.thickness/radLen);
            }
        }
        if (debug) {
            for (int lyr = 0; lyr < lyrHits.size(); ++lyr) {
                ArrayList<KalHit> LL = lyrHits.get(lyr);
                System.out.format("KalmanPatRecHPS: layer %d hits:", lyr);
                for (KalHit ht : LL) {
                    ht.print("short");
                }
                System.out.format("\n");
            }
        }
        
        Plane p0 = new Plane(new Vec(0., kPar.beamSpot[1], 0.), new Vec(0., 1., 0.));  // xy plane at the target position

        if (debug) {
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
        for (int trial = KalmanParams.mxTrials - kPar.nTrials; trial < KalmanParams.mxTrials; trial++) {
            int candID = topBottom*1000 + trial*100 + 1;
            if (debug) System.out.format("\nKalmanPatRecHPS: start of pass %d through the algorithm.\n", trial);
            
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
                if (trial == 0 && kPar.lyrList[topBottom].indexOf(list) > kPar.maxListIter1) continue;
                int nLyrs = list.length;
                int middleLyr = 2;
                if (moduleList.get(list[middleLyr]).size() == 0) continue;  // Skip seed if there is no hit in the middle layer
                SiModule m0 = moduleList.get(list[middleLyr]).get(0);
                double yOrigin = m0.p.X().v[1];                     // Set the local origin to be in the middle of the seed list
                Vec pivot = new Vec(0, yOrigin, 0.);
                if (debug) {
                    System.out.format("\n\nKalmanPatRecHPS: layer list=%d %d %d %d %d\n", list[0], list[1], list[2], list[3], list[4]);
                    System.out.format("KalmanPatRecHPS: yOrigin=%10.6f\n", yOrigin);
                }
                ArrayList<SeedTrack> seedList = new ArrayList<SeedTrack>();
                int[] idx = new int[nLyrs];
                for (idx[0] = 0; idx[0] < lyrHits.get(list[0]).size(); idx[0]++) {
                    KalHit kht = lyrHits.get(list[0]).get(idx[0]);
                    Measurement hit = kht.hit;
                    if (hit.tracks.size() > 0) continue; // don't use hits already on KalTrack tracks
                    SiModule mod = kht.module;
                    ah[0] = mod.isStereo;
                    if (!mod.isStereo) {
                        zh[0] = mod.toGlobal(new Vec(0.,hit.v,0.)).v[2];
                        yh[0] = mod.p.X().v[1];
                    }                 
                    for (idx[1] = 0; idx[1] < lyrHits.get(list[1]).size(); idx[1]++) {
                        kht = lyrHits.get(list[1]).get(idx[1]);
                        hit = kht.hit;
                        if (hit.tracks.size() > 0) continue;
                        mod = kht.module;
                        ah[1] = mod.isStereo;
                        if (!mod.isStereo) {
                            zh[1] = mod.toGlobal(new Vec(0.,hit.v,0.)).v[2];
                            yh[1] = mod.p.X().v[1];
                            if (seedNoGood(1, trial)) continue;
                        }
                        for (idx[2] = 0; idx[2] < lyrHits.get(list[2]).size(); idx[2]++) {
                            kht = lyrHits.get(list[2]).get(idx[2]);
                            hit = kht.hit;
                            if (hit.tracks.size() > 0) continue;
                            mod = kht.module;
                            ah[2] = mod.isStereo;
                            if (!mod.isStereo) {
                                zh[2] = mod.toGlobal(new Vec(0.,hit.v,0.)).v[2];
                                yh[2] = mod.p.X().v[1];
                                if (seedNoGood(2, trial)) continue;
                            }
                            for (idx[3] = 0; idx[3] < lyrHits.get(list[3]).size(); idx[3]++) {
                                kht = lyrHits.get(list[3]).get(idx[3]);
                                hit = kht.hit;
                                if (hit.tracks.size() > 0) continue;
                                mod = kht.module;
                                ah[3] = mod.isStereo;
                                if (!mod.isStereo) {
                                    zh[3] = mod.toGlobal(new Vec(0.,hit.v,0.)).v[2];
                                    yh[3] = mod.p.X().v[1];
                                    if (seedNoGood(3, trial)) continue;
                                }
                                for (idx[4] = 0; idx[4] < lyrHits.get(list[4]).size(); idx[4]++) {
                                    kht = lyrHits.get(list[4]).get(idx[4]);
                                    hit = kht.hit;
                                    if (hit.tracks.size() > 0) continue;
                                    mod = kht.module;
                                    ah[4] = mod.isStereo;
                                    if (!mod.isStereo) {
                                        zh[4] = mod.toGlobal(new Vec(0.,hit.v,0.)).v[2];
                                        yh[4] = mod.p.X().v[1];
                                        if (seedNoGood(4, trial)) continue;
                                    }
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
                                        //if (debug) {
                                        //    System.out.format("KalmanPatRecHPS: skipping seed with tdif=%8.2f\n Hits:  ", tmax-tmin);
                                        //    for (KalHit ht : hitList) ht.print("short");
                                        //    System.out.format("\n");
                                        //}
                                        continue;
                                    }
                                    // To avoid wasting time fitting seeds, skip seeds that are entirely contained in already found candidates
                                    boolean redundantSeed = false;
                                    for (TrackCandidate tkr : candidateList) {
                                        if (tkr.contains(hitList)) {
                                            if (debug) System.out.format("KalmanPatRecHPS: seed %d %d %d %d %d is already on candidate %d\n",
                                                    idx[0], idx[1], idx[2], idx[3], idx[4], tkr.ID);
                                            redundantSeed = true;
                                            break;
                                        }
                                    }
                                    if (redundantSeed) continue;
                                    
                                    // Fit the seed to extract helix parameters
                                    SeedTrack seed = new SeedTrack(hitList, yOrigin, kPar.beamSpot[1]);
                                    if (!seed.success) continue;
                                    
                                    // Cuts on the seed quality
                                    Vec hp = seed.helixParams();                        
                                    if (debug) {
                                        System.out.format("Seed %d %d %d %d %d parameters for cuts: K=%10.5f, tanl=%10.5f, ",
                                                          idx[0], idx[1], idx[2], idx[3], idx[4], hp.v[2], hp.v[4]);
                                    }                                    
                                    boolean seed_passes_cuts = false;                                    
                                    if (Math.abs(hp.v[2]) < kPar.kMax[trial]) {
                                        if (Math.abs(hp.v[4]) < kPar.tanlMax[trial]) {
                                            Vec pInt = seed.planeIntersection(p0); 
                                            double xzDist = Math.sqrt(pInt.v[0]*pInt.v[0] + pInt.v[2]*pInt.v[2]);
                                            if (debug) System.out.format("dxz=%10.5f, Intersection with target plane= %s\n", xzDist, pInt.toString());                                         
                                            if (xzDist < kPar.dRhoMax[trial]) {
                                                if (Math.abs(pInt.v[2]) < kPar.dzMax[trial]) seed_passes_cuts = true;
                                            } //Check intersection with target plane
                                        } //Check tanLambda
                                    } //Check curvature
                                    
                                    //Good seed; check for approximate duplicates
                                    if (seed_passes_cuts) {
                                        boolean reject_seed = false;
                                        for (SeedTrack sel_seed : seedList) {
                                            reject_seed = seed.isCompatibleTo(sel_seed,kPar.seedCompThr);
                                            if (reject_seed) break;
                                        }
                                        if (!reject_seed) seedList.add(seed);
                                    }
                                    if (debug) System.out.format("\n");
                                }
                            }
                        }
                    }
                }
                
                if (debug) {
                    System.out.printf("KalmanPatRecHPS::SeedList size = %d \n", seedList.size());
                    //for (SeedTrack seed : seedList) {
                    //  seed.print("PF::Check Seed");
                    //}
                }
                
                // Sort all of the seeds by distance from origin in x,z plane
                Collections.sort(seedList, SeedTrack.dRhoComparator);
                if (debug) {
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
                Vec Bfield = KalmanInterface.getField(pivot, m0.Bfield);
                double Bmag = Bfield.mag();
                Vec tB = Bfield.unitVec(Bmag);
                seedLoop: for (SeedTrack seed : seedList) {
                    if (debug) {
                        System.out.format("\n\nStart the filter step for seed");
                        for (KalHit ht : seed.hits) {
                            ht.print("short");
                        }
                        System.out.format("\n");
                    }
                    // Skip seeds that are already on a good track candidate
                    // This is not redundant with the above test, as we can catch here some candidates formed within this seed loop
                    for (TrackCandidate tkCand : candidateList) {
                        if (tkCand.contains(seed.hits)) {
                            if (debug) System.out.format("KalmanPatRecHPS: skipping seed %d that is already on candidate %d\n", 
                                    seedList.indexOf(seed),candidateList.indexOf(tkCand));
                            continue seedLoop;
                        }
                    }
                    // Skip seeds that are already on a track found and saved in this iteration
                    if (TkrList.size() > 0) {
                        for (KalHit ht : seed.hits) {
                            if (ht.hit.tracks.size()>0) {
                                if (debug) System.out.format("KalmanPatRecHPS: skipping seed %d that is already on track %d\n", 
                                        seedList.indexOf(seed),ht.hit.tracks.get(0).ID);
                                continue seedLoop;
                            }
                        }
                    }
                    
                    DMatrixRMaj CovGuess = seed.covariance().copy();
                    CommonOps_DDRM.scale(100., CovGuess);
                    // Create an state vector from the input seed to initialize the Kalman filter
                    StateVector sI = new StateVector(-1, seed.helixParams(), CovGuess, new Vec(0., 0., 0.), Bmag, tB, pivot);
                    TrackCandidate candidateTrack = new TrackCandidate(candID, seed.hits, kPar, hitMap, eventNumber);
                    candID++;
                    filterTrack(candidateTrack, list[0], numLayers - 1, sI, trial, true, true);
                    if (!candidateTrack.filtered) {
                        if (debug) {
                            System.out.format("KalmanPatRecHPS: filtering of this seed failed. chi2=%10.5f, nHits=%d\n", candidateTrack.chi2f,
                                    candidateTrack.hits.size());
                        }
                        continue seedLoop;
                    }
                    if (!candidateTrack.good) {
                        if (debug) {
                            System.out.format("KalmanPatRecHPS: candidate track is no good. chi2=%10.5f, nHits=%d\n", candidateTrack.chi2f,
                                    candidateTrack.hits.size());
                        }
                        continue seedLoop;
                    }

                    if (debug) {
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
                        if (debug) System.out.format("KalmanPatRecHPS: Initial filtering has too few sites, n=%d<%d. Skip to the next seed.\n",
                                candidateTrack.sites.size(), kPar.minHits0);
                        continue seedLoop;                        
                    }
                    if (candidateTrack.hits.size() < kPar.minHits0) {
                        if (debug) System.out.format("KalmanPatRecHPS: Initial filtering has too few hits, n=%d<%d. Skip to the next seed.\n",
                                candidateTrack.hits.size(), kPar.minHits0);
                        continue seedLoop;
                    }
                    if (candidateTrack.chi2f / (double) candidateTrack.hits.size() > kPar.chi2mx1[trial]) {
                        if (debug) System.out.format("KalmanPatRecHPS: Initial filtering has too large chi^2. Skip to the next seed.\n");
                        continue seedLoop;
                    }

                    // Now smooth back to the original point
                    smoothTrack(candidateTrack);
                    if (debug) {
                        candidateTrack.print("after initial smoothing", false);
                        System.out.format("\nKalmanPatRecHPS: Smoothed chi2=%10.5f\n", candidateTrack.chi2s);
                    }

                    // Then filter toward the target if there are any more untried layers there
                    if (candidateTrack.sites.get(0).m.Layer > firstLayer) {
                        filterTrack(candidateTrack, candidateTrack.sites.get(0).m.Layer - 1, firstLayer, candidateTrack.sites.get(0).aS, trial, false, true);
                        if (!candidateTrack.filtered) {
                            if (debug) System.out.format("KalmanPatRecHPS: not successful with inward filter step\n");
                            candidateTrack.good = false;
                            continue seedLoop;
                        }
                        if (debug) {
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
                    if (debug) candidateTrack.print("before sorting hits", true);
                    Collections.sort(candidateTrack.hits, KalHit.HitComparator);
                    if (debug) candidateTrack.print("after sorting hits", true);
                    for (TrackCandidate oldCandidate : candidateList) {
                        if (candidateTrack.equals(oldCandidate)) {
                            if (debug) System.out.format("KalmanPatRecHPS: candidate match new=%8.3f old=%8.3f\n",candidateTrack.chi2s,oldCandidate.chi2s);
                            if (debug) System.out.format("KalmanPatRecHPS: candidate track is redundant. Skip to next seed.\n");
                            continue seedLoop;
                        }
                    }
                    candidateList.add(candidateTrack); // Save the candidate in this list 

                    if (candidateTrack.hits.size() < kPar.minHits1[trial]) {
                        if (debug) {
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
                                    if (debug) System.out.format("KalmanPatRecHPS event %d candidate %d, removing hit from layer %d detector %d\n", 
                                            eventNumber, candidateTrack.ID, siteR.m.Layer, siteR.m.detector);
                                    removedHit = true;
                                    //candidateTrack.print("with hit removed", true);
                                } else {
                                    System.out.format("KalmanPatRecHPS error: missing hit in layer %d detector %d\n", siteR.m.Layer, siteR.m.detector);
                                }
                            }
                        }
                        if (!removedHit) {
                            if (debug) {
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
                    CommonOps_DDRM.scale(10., startSite.aF.helix.C);
                    filterTrack(candidateTrack, firstLayer, numLayers - 1, startSite.aF, trial, true, false);
                    if (!candidateTrack.filtered) {
                        if (debug) { System.out.format("KalmanPatRecHPS: %d failed filtering of all layers. Try next seed.\n", candidateTrack.ID); }
                        candidateTrack.good = false;
                        continue seedLoop;
                    }
                    if (debug) {
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
                        if (debug) { System.out.format("KalmanPatRecHPS: filtering of %d has too few stereo hits. Skip to the next seed.\n", candidateTrack.ID); }
                        candidateTrack.good = false;
                        continue seedLoop;
                    }
                    if (candidateTrack.hits.size() - nStereo < kPar.minAxial) {
                        if (debug) { System.out.format("KalmanPatRecHPS: filtering of %d has too few non-stereo hits. Skip to the next seed.\n", candidateTrack.ID); }
                        candidateTrack.good = false;
                        continue seedLoop;
                    }

                    // Finally smooth back to the target
                    smoothTrack(candidateTrack);
                    if (debug) candidateTrack.print("after smoothing", false);
                    
                    // Junk highly curved candidates that don't even intersect the y=0 plane
                    StateVector aS = candidateTrack.sites.get(0).aS;
                    double phi0 = aS.helix.planeIntersect(new Plane(new Vec(0.,0.,0.), new Vec(0.,1.,0.)));
                    if (Double.isNaN(phi0)) {
                        if (debug) System.out.format("KalmanPatRecHPS: marking track candidate %d bad, as it does not intersect the origin plane.\n", candidateTrack.ID);
                        candidateTrack.good = false;
                        continue seedLoop;
                    }                   
                    
                    // Check if the track can be improved by removing hits
                    if (removeBadHits(candidateTrack, trial)) {
                        if (debug) System.out.format("KalmanPatRecHPS: Refit candidate track %d after removing a hit.\n", candidateTrack.ID);
                        if (candidateTrack.reFit()) {
                            if (debug) candidateTrack.print("after refitting and smoothing", false);
                        } else {
                            candidateTrack.good = false;
                            continue seedLoop;
                        }
                        hitChanges = true;
                    }
                    
                    if (!candidateTrack.smoothed) {
                        if (debug) System.out.format("KalmanPatRecHPS: candidate %d smoothing failed.\n", candidateTrack.ID);
                        candidateTrack.good = false;
                        continue seedLoop;
                    }
                    if (candidateTrack.chi2s/(double)candidateTrack.numHits() > kPar.chi2mx1[trial]) {
                        if (debug) System.out.format("KalmanPatRecHPS: candidate %d chi^2 is too large.\n", candidateTrack.ID);
                        candidateTrack.good = false;
                        continue seedLoop;
                    }
                    if (debug) {
                        MeasurementSite endSite = null;
                        for (int isx = candidateTrack.sites.size()-1; isx>=0; --isx) {
                            MeasurementSite site = candidateTrack.sites.get(isx);
                            if (site.hitID >= 0) {
                                endSite = site;
                                break;
                            }
                        }
                        if (endSite != null) {
                            Vec afF = endSite.aF.helix.a;
                            Vec afC = endSite.aF.helix.helixErrors();
                            afF.print("KalmanPatRecHPS helix parameters at final filtered site");
                            afC.print("KalmanPatRecHPS helix parameter errors");
                        }
                        startSite = null;
                        for (MeasurementSite site : candidateTrack.sites) {
                            if (site.aS != null) startSite = site;
                        }
                        if (startSite != null) {
                            startSite.aS.helix.a.print("KalmanPatRecHPS helix parameters at the final smoothed site");
                            startSite.aS.helix.helixErrors().print("KalmanPatRecHPS helix parameter errors:");
                        }
                    }
                    // If any hit assignments changed, then check again for redundant candidates
                    if (hitChanges) {
                        Collections.sort(candidateTrack.hits, KalHit.HitComparator);
                        for (TrackCandidate oldCandidate : candidateList) {
                            if (oldCandidate == candidateTrack) continue;
                            if (candidateTrack.equals(oldCandidate)) {
                                //if (debug) System.out.format("KalmanPatRecHPS: candidate match new=%8.3f old=%8.3f\n",candidateTrack.chi2s,oldCandidate.chi2s);
                                if (debug) System.out.format("KalmanPatRecHPS: candidate track is redundant (2). Skip to next seed.\n");
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
                    if (MatrixFeatures_DDRM.hasNaN(startSite.aS.helix.C)) {
                        if (debug) System.out.format("KalmanPatRecHPs: candidate %d covariance is NaN!\n", candidateTrack.ID);
                        candidateTrack.good = false;
                        continue seedLoop;
                    }

                    // For near perfect candidates, save them immediately as KalTracks and mark the hits as no longer available (to save time)
                    if (candidateTrack.numHits() > 9) {
                        if (candidateTrack.chi2s < 30.) {
                            if (candidateTrack.numStereo() > 4) {
                                Vec helix = candidateTrack.originHelix();
                                if (Math.abs(helix.v[0]) < kPar.dRhoMax[trial]) {
                                    if (Math.abs(helix.v[3]) < kPar.dzMax[trial]) {
                                        if (debug) {
                                            System.out.format("KalmanPatRecHPS: keeping a near perfect track candidate %d\n", candidateTrack.ID);
                                            candidateTrack.print("the perfect one", true);
                                        }
                                        if (storeTrack(tkID, candidateTrack)) {
                                            tkID++;
                                            candidateList.remove(candidateTrack);
                                            if (debug) {
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
                                                        if (debug) {
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
                                                    if (debug) tkr.print("after hit removal", true);
                                                }
                                            }
                                        } else candidateTrack.good = false;
                                    }
                                }
                            }
                        }
                    }
                    
                    // Here we have a good track candidate. Mark the hits in KalHit as used by this candidate.
                    if (candidateList.contains(candidateTrack)) {
                        for (KalHit hit : candidateTrack.hits) {
                            boolean foundIt = false;
                            for (MeasurementSite site : candidateTrack.sites) {
                                if (site.m == hit.module) {
                                    foundIt = true;
                                    break;
                                }
                            }
                            if (!foundIt) {
                                logger.log(Level.WARNING, String.format("KalmanPatRecHPS event %d, missing site for candidate track", eventNumber));
                            }
                            //if (debug) System.out.format("KalmanPatRecHPS: marking hit on layer %d of candidate %d\n", hit.module.Layer, candidateTrack.ID);
                            hit.tkrCandidates.add(candidateTrack);
                        } 
                    }
                } // Next seed in set
            } // Next set of seeds
            if (debug) {
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
                    // Resurrect the candidate if it has enough hits and none of them is shared with a good candidate or finished track
                    boolean resurrect = true;
                    for (KalHit ht : tkr.hits) {
                        if (ht.hit.tracks.size() > 0) {
                            resurrect = false;
                            break;
                        }
                        int nGood = 0;
                        for (TrackCandidate tkr2 : ht.tkrCandidates) {
                            if (tkr2.good) nGood++;
                        }
                        if (nGood > 0) {
                            resurrect = false;
                            break;
                        }
                    }
                    if (resurrect) {
                        if (tkr.numHits() >= kPar.minHits1[trial] && tkr.numStereo() >= kPar.minStereo[trial]) {
                            int nAxial = tkr.numHits() - tkr.numStereo();
                            if (nAxial >= kPar.minAxial) {
                                StateVector aS0 = tkr.sites.get(0).aS;
                                if (aS0 != null) {
                                    if (!aS0.helix.a.isNaN()) {
                                        if (!MatrixFeatures_DDRM.hasNaN(aS0.helix.C)) {
                                            Collections.sort(tkr.sites, MeasurementSite.SiteComparatorUp); // Occasionally necessary
                                            if (tkr.sites.get(0).aS == null) {
                                                if (tkr.reFit()) {
                                                    tkr.good = true;
                                                    if (debug) {
                                                        System.out.format("KalmanPatRecHPS event %d: resurrecting refit candidate %d with chi2=%9.5f\n", 
                                                                          eventNumber, tkr.ID, tkr.chi2s);
                                                        tkr.print("resurrected", true); 
                                                    }
                                                    for (KalHit hit : tkr.hits) {
                                                        boolean foundIt = false;
                                                        for (MeasurementSite site : tkr.sites) {
                                                            if (site.m == hit.module) {
                                                                foundIt = true;
                                                                break;
                                                            }
                                                        }
                                                        if (!foundIt) {
                                                            logger.log(Level.WARNING, String.format("KalmanPatRecHPS event %d, missing site for candidate track", eventNumber));
                                                        }
                                                        if (!hit.tkrCandidates.contains(tkr)) hit.tkrCandidates.add(tkr);
                                                    }
                                                    continue;
                                                }
                                            } else {
                                                tkr.good = true;
                                                if (debug) {
                                                    System.out.format("KalmanPatRecHPS event %d: resurrecting candidate %d with chi2=%9.5f\n", 
                                                                      eventNumber, tkr.ID, tkr.chi2s);
                                                    tkr.print("resurrected", true);
                                                }
                                                continue; 
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (debug) {
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
            if (debug) {
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
                            if (debug) hit.print("evaluate sharing");
                            TrackCandidate bestTkr = null;
                            int iBest = candidateList.size();
                            for (TrackCandidate tkr : hit.tkrCandidates) {
                                int iTK = candidateList.indexOf(tkr);
                                if (iTK < iBest) {
                                    bestTkr = tkr;
                                    iBest = iTK;
                                }
                            }
                            if (debug) System.out.format("KalmanPatRecHPS: best candidate track for this hit is %d, chi2=%7.3f, of %d candidates\n", candidateList.get(iBest).ID, bestTkr.chi2s, candidateList.size());
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
                                if (debug) {
                                    System.out.format("KalmanPatRecHPS: hit %d removed from track candidate %d on layer %d detector %d\n", 
                                            hit.module.hits.indexOf(hit.hit), tkr.ID, hit.module.Layer, hit.module.detector);
                                }
                                tkr.removeHit(hit);
                            }
                        }
                    }
                }
                if (debug) {
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
                        if (debug) System.out.format("KalmanPatRecHPS: eliminating track candidate %d for %d axial hits and %d stereo hits\n", 
                                tkr.ID, nAxial, nStereo);                        
                    }
                }
                if (debug) {
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
                        if (debug) System.out.format("KalmanPatRecHPS: eliminating track candidate %d for %d shared hits\n", tkr.ID, nShared);  
                    }
                }
                if (debug) {
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
            if (debug) {
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

        if (debug) {
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
        if (TkrList.size() > 0) {
            for (SiModule module : data) {
                for (Measurement hit : module.hits) {
                    if (hit.tracks.size() > 1) {
                        int minIDX = 1000;
                        KalTrack bestTkr = null;
                        ArrayList<KalTrack> tksToPrune = new ArrayList<KalTrack>(hit.tracks.size());
                        for (KalTrack tkr : hit.tracks) {
                            tksToPrune.add(tkr); 
                            int idx = TkrList.indexOf(tkr);
                            if (idx < 0) {
                                logger.log(Level.WARNING,String.format("Bad reference from hit to track. Track %d, Layer = %d\n", tkr.ID, module.Layer));
                            } else {
                                if (idx < minIDX) {
                                    minIDX = idx;
                                    bestTkr = tkr;
                                }
                            }
                        }                       
                        for (KalTrack tkr : tksToPrune){
                            if (tkr == bestTkr) continue; // Keep the hit on the best track
                            int idx = tkr.whichSite(module);
                            if (idx < 0) {
                                logger.log(Level.WARNING,String.format("KalmanPatRecHPS: bad reference from module to site. Track %d, Layer = %d\n", tkr.ID, module.Layer));
                            } else {
                                MeasurementSite site = tkr.SiteList.get(idx);
                                if (debug) {
                                    System.out.format("KalmanPatRecHPS: shall we remove a hit from Track %d, Layer %d with chi2inc=%10.5f?\n", 
                                            tkr.ID, module.Layer, site.chi2inc);
                                }
                                if (site.chi2inc > kPar.mxChi2double) {
                                    if (!site.smoothed) logger.log(Level.WARNING,String.format("OOPS, why isn't this site smoothed at layer %d?",site.m.Layer));
                                    if (tkr.removeHit(site, kPar.mxChi2Inc, kPar.mxTdif)) {
                                        if (debug) {
                                            System.out.format("KalmanPatRecHPS: added a hit after removing one for Track %d, Layer %d\n",tkr.ID, module.Layer);
                                        }
                                    } else {
                                        if (debug) System.out.format("KalmanPatRecHPS: removing a hit from Track %d, Layer %d\n", tkr.ID, module.Layer);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        ArrayList<KalTrack> allTks = new ArrayList<KalTrack>(TkrList.size());
        for (KalTrack tkr : TkrList) {
            allTks.add(tkr);
        }
        
        // Refit the KalTracks
        Iterator<KalTrack> iter = allTks.iterator();
        while (iter.hasNext()) {
            KalTrack tkr = iter.next();
            if (debug) {
                System.out.format("KalmanPatRecHPS: list of sites on KalTrack track %d with chi2=%8.3f: ", tkr.ID, tkr.chi2);
                for (MeasurementSite site : tkr.SiteList) {
                    System.out.format("(%d, %d, %d) ",site.m.Layer, site.m.detector, site.hitID);
                }
                System.out.format("\n");
            }
            
            // Try to add hits on layers with missing hits
            tkr.addHits(data, kPar.mxResid[1], kPar.mxChi2Inc, kPar.mxTdif, debug);
            
            // check that there are enough hits in both views
            int nStereo = 0;
            int nAxial = 0;
            int nShared = 0;
            for (MeasurementSite site : tkr.SiteList) {
                if (site.hitID < 0) continue;
                SiModule m = site.m;
                if (!m.isStereo) nAxial++;
                else nStereo++;
                //if (debug) {
                //    System.out.format("KalmanPatRecHPS: track %d, layer %d, detector %d, hit=", tkr.ID, site.m.Layer, site.m.detector);
                //    m.hits.get(site.hitID).print("on tkr");
                //    System.out.format("\n");
                //}                
                if (m.hits.get(site.hitID).tracks.size()>1) nShared++;
            }
            boolean removeIt = false;
            if (nStereo < kPar.minStereo[1]) {
                if (debug) System.out.format("KalmanPatRecHPS: removing KalTrack %d for %d stereo hits\n", tkr.ID,nStereo);
                removeIt = true;
            } else if (nAxial < kPar.minAxial) {
                if (debug) System.out.format("KalmanPatRecHPS: removing KalTrack %d for %d axial hits\n", tkr.ID,nAxial);
                removeIt = true;
            } else if (nAxial + nStereo < kPar.minHits1[KalmanParams.mxTrials - 1]) {
                if (debug) System.out.format("KalmanPatRecHPS: removing KalTrack %d for %d hits\n", tkr.ID,nStereo+nAxial);
                removeIt = true;
            } else if (nAxial + nStereo - nShared < kPar.minHits0) {
                if (debug) System.out.format("KalmanPatRecHPS: removing KalTrack %d for %d shared hits out of %d\n", tkr.ID,nShared,nAxial+nStereo);
                removeIt = true;
            }
            if (removeIt) {
                TkrList.remove(tkr);
                for (MeasurementSite site : tkr.SiteList) {
                    if (site.hitID != -1) {
                        site.m.hits.get(site.hitID).tracks.remove(tkr);
                        site.removeHit();
                    }
                }
                continue;
            }
            
            if (debug) System.out.format("KalmanPatRecHPS: Call the Kalman fit for track %d\n", tkr.ID);
            
            boolean goodFit = tkr.fit(kPar.nIterations, debug);
            if (goodFit) {
                StateVector aS = tkr.SiteList.get(0).aS;
                if (aS != null) {
                    double phi0 = aS.helix.planeIntersect(new Plane(new Vec(0.,0.,0.), new Vec(0.,1.,0.)));
                    if (Double.isNaN(phi0)) {
                        if (debug) System.out.format("KalmanPatRecHPS: track %d does not intersect the origin plane!\n", tkr.ID);
                        goodFit = false;
                    }
                } else {
                    if (debug) System.out.format("KalmanPatRecHPS: track %d has no smoothed state vector at site 0!\n", tkr.ID);
                    goodFit = false;
                }
                if (goodFit) {
                    if (!tkr.originHelix()) {
                        if (debug) System.out.format("KalmanPatRecHPS: propagating track %d to the origin failed!\n", tkr.ID);
                        goodFit = false;
                    };
                }
                if (goodFit) { // For tracks with few hits, include an origin constraint
                    if (tkr.nHits == 5) {
                        HelixState constrHelix = tkr.originConstraint(vtx, vtxCov);
                        double chi2inc = tkr.chi2incOrigin();
                        if (chi2inc <= 1.0) {
                            tkr.helixAtOrigin = constrHelix;
                            tkr.chi2 += chi2inc;
                        } else {
                            goodFit = false;
                        }
                    }
                }
            }
            if (!goodFit) {
                if (debug) System.out.format("KalmanPatRecHPS: removing KalTrack %d for bad fit!\n", tkr.ID);
                TkrList.remove(tkr);
                for (MeasurementSite site : tkr.SiteList) {
                    if (site.hitID!=-1) {
                        site.m.hits.get(site.hitID).tracks.remove(tkr);
                        site.removeHit();
                    }
                }
                continue;
            }
        }
        
        Collections.sort(TkrList, KalTrack.TkrComparator); // Sort tracks by quality
        if (debug) {
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
        if (debug) {
            MeasurementSite badSite = tkr.sites.get(idxBad);
            System.out.format("KalmanPatRecHPS.removeBadHits: the worst non-seed layer is %d with chi2inc=%7.2f.\n", badSite.m.Layer, badSite.chi2inc);
        }
        if (idxBad >= 0) {
            if (mxChi2 > kPar.minChi2IncBad) {
                MeasurementSite site = tkr.sites.get(idxBad);
                Measurement badOne = site.m.hits.get(site.hitID);
                KalHit badHit = hitMap.get(badOne);
                if (badHit != null) {
                    if (debug) System.out.format("KalmanPatRecHPS.removeBadHits: event %d, removing bad hit in layer %d with chi2=%9.3f.\n",eventNumber, tkr.sites.get(idxBad).m.Layer,mxChi2);
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
            if (nextSite == null) {   // The outermost site with a hit is already smoothed by definition
                if (currentSite.hitID < 0) continue;
                currentSite.aS = currentSite.aF;
                currentSite.smoothed = true;
            } else {
                currentSite.smooth(nextSite);
            }
            filteredTkr.chi2s += Math.max(currentSite.chi2inc, 0.);

            currentSite.print("smoothing");
            nextSite = currentSite;
            if (verbose) currentSite.print("smoothed");
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
        //    if (debug) {
        //        System.out.format("KalmanPatRecHPS:filterTrack: skipping for hits used. nLyrs=%d, nTaken=%d\n", tkrCandidate.hits.size(),
        //                tkrCandidate.nTaken);
        //    }
        //    return;
        //}

        if (debug) {
            Vec hprms = sI.helix.a;
            System.out.format("\n KalmanPatRecHPS:filterTrack: Start filtering candidate %d with drho=%10.5f phi0=%10.5f k=%10.5f dz=%10.5f tanl=%10.5f \n",
                    tkrCandidate.ID, hprms.v[0], hprms.v[1], hprms.v[2], hprms.v[3], hprms.v[4]);
            System.out.format("                    origin=%s,   pivot=%s\n", sI.helix.origin.toString(), sI.helix.X0.toString());
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
            if (debug) {
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
            SiModule mExistingHit = null;
            // Find the correct hit number and its module if this is one of the layers with an existing hit
            int hitno = -1;
            for (KalHit ht : tkrCandidate.hits) {
                if (ht.module.Layer == lyr) {
                    hitno = ht.module.hits.indexOf(ht.hit);
                    mExistingHit = ht.module;
                    break;
                }
            }
            if (debug) System.out.format("KalmanPatRecHPS.filterTrack: layer %d, %d modules\n", lyr, moduleList.get(lyr).size());
            // Loop over all of the modules in this layer
            for (int imod = 0; imod < moduleList.get(lyr).size(); ++imod) {
                SiModule m = moduleList.get(lyr).get(imod);
                // Only consider the one module with the given hit for the existing specified hits
                if (mExistingHit != null) {
                    if (m != mExistingHit) {
                        if (debug) System.out.format("KalmanPatRecHPS.filterTrack: skip module at layer %d, detector %d, %d hits, given hit=%d.\n",
                                                       mExistingHit.Layer, mExistingHit.detector, mExistingHit.hits.size(), hitno);
                        continue;
                    }
                } 

                if (debug) {
                    System.out.format("KalmanPatRecHPS.filterTrack: try prediction at layer %d, detector %d, %d hits, given hit=%d.\n",
                                       m.Layer, m.detector, m.hits.size(), hitno);
                    //HelixState hx = null;
                    //if (prevSite == null) hx = sI.helix; else hx = prevSite.aF.helix;
                    //hx.print("predicting from ");
                    //Vec originB = hx.toGlobal(hx.X0);
                    //Vec newB = KalmanInterface.getField(originB, m.Bfield);
                    //System.out.format("  B=%10.4f   t=%s\n", newB.mag(), newB.unitVec().toString());
                }
                newSite = new MeasurementSite(lyr, m, kPar.mxResid[trial], kPar.mxResidShare);
                int rF;
                double [] tRange = {tkrCandidate.tMax - kPar.mxTdif, tkrCandidate.tMin + kPar.mxTdif}; 
                if (prevSite == null) { // For first layer use the initializer state vector               
                    boolean checkBounds = imod < moduleList.get(lyr).size() - 1;  // Note: boundary check is not made if hitno=-1
                    rF = newSite.makePrediction(sI, null, hitno, tkrCandidate.nTaken <= kPar.mxShared, pickUp, checkBounds, tRange);
                    if (rF > 0) {
                        if (m.hits.get(newSite.hitID).tracks.size() > 0) tkrCandidate.nTaken++;
                        tkrCandidate.tMin = Math.min(tkrCandidate.tMin, m.hits.get(newSite.hitID).time);
                        tkrCandidate.tMax = Math.max(tkrCandidate.tMax, m.hits.get(newSite.hitID).time);
                    } else if (rF < 0) {
                        if (rF == -2) {  // This really shouldn't happen at the initial site
                            if (debug) System.out.format("KalmanPatRecHPS.filterTrack: not within detector boundary on layer %d detector %d!!\n",
                                    newSite.m.Layer, newSite.m.detector);
                            continue;
                        }
                        if (debug) {
                            System.out.format("KalmanPatRecHPS:filterTrack: Failed to make prediction at initial site %d, layer=%d! Abort\n",
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
                    boolean checkBounds = imod < moduleList.get(lyr).size() - 1;  // Note: boundary check is not made if hitno=-1
                    rF = newSite.makePrediction(prevSite.aF, prevSite.m, hitno, tkrCandidate.nTaken <= kPar.mxShared, pickUp,
                            checkBounds, tRange);
                    if (rF > 0) {
                        if (m.hits.get(newSite.hitID).tracks.size() > 0) tkrCandidate.nTaken++;
                        tkrCandidate.tMin = Math.min(tkrCandidate.tMin, m.hits.get(newSite.hitID).time);
                        tkrCandidate.tMax = Math.max(tkrCandidate.tMax, m.hits.get(newSite.hitID).time);
                    } else if (rF < 0) {
                        if (rF == -2) {
                            if (debug) System.out.format("KalmanPatRecHPS.filterTrack: not within detector boundary on layer %d detector %d\n",
                                    newSite.m.Layer, newSite.m.detector);
                            continue;
                        }
                        if (debug) {
                            System.out.format("KalmanPatRecHPS:filterTrack: Failed to make prediction at site %d, layer=%d.  Exit layer loop\n",
                                    thisSite + 1, lyr);
                        }
                        needCleanup = true;
                        break layerLoop;
                    }
                }
                if (debug) {
                    System.out.format("KalmanPatRecHPS.filterTrack: candidate %d, completed prediction at site (%d, %d, %d)\n",
                            tkrCandidate.ID, newSite.m.Layer, newSite.m.detector, newSite.hitID);
                }
                thisSite++;
                if (!newSite.filter()) {
                    if (debug) System.out.format("KalmanPatRecHPS:filterTrack: Failed to filter at site %d, layer=%d.  Ignore remaining sites\n", thisSite, lyr);
                    needCleanup = true;
                    break layerLoop;
                }
                if (debug) {
                    System.out.format("KalmanPatRecHPS.filterTrack: candidate %d, completed filter at site (%d, %d, %d), chi2-inc=%8.3f\n",
                            tkrCandidate.ID, newSite.m.Layer, newSite.m.detector, newSite.hitID, newSite.chi2inc);
                }
                KalHit theHit = null;
                if (newSite.hitID >= 0) theHit = hitMap.get(m.hits.get(newSite.hitID));
                if (rF == 1 && hitno < 0) {
                    tkrCandidate.hits.add(theHit);
                    if (debug) theHit.print("new");
                }

                // if (debug) newSite.print("initial filtering");
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
                    if (debug) System.out.format("    KalmanPatRecHPS.filterTrack: remove hit from candidate at layer %d\n", hit.module.Layer);
                    hit.tkrCandidates.remove(tkrCandidate);
                    itr.remove();
                } else if (hit.module.isStereo) nstereo++;
            }
            if (tkrCandidate.hits.size() < 5) {
                if (debug) System.out.format("    KalmanPatRecHPS.filterTrack: marking candidate bad; too few hits after cleanup: %d\n",tkrCandidate.hits.size());
                tkrCandidate.good = false;
            }
            if (nstereo < 3) {
                if (debug) System.out.format("    KalmanPatRecHPS.filterTrack: marking candidate bad; too few stereo hits after cleanup: %d\n",nstereo);
                tkrCandidate.good = false;
            }
        }
        tkrCandidate.filtered = true;
        return;
    }

    boolean storeTrack(int tkID, TrackCandidate tkrCand) {
        if (debug) System.out.format("entering storeTrack for track %d, debug=%b\n", tkID, debug);

        MeasurementSite firstSite = null;
        for (int idx=0; idx<tkrCand.sites.size()-1; ++idx) {
            MeasurementSite site = tkrCand.sites.get(idx);
            if (site.hitID >= 0) firstSite = site;
            MeasurementSite nxtSite = tkrCand.sites.get(idx+1);
            if (nxtSite.m.Layer-site.m.Layer > 1) {
                logger.warning(String.format("Event %d, Track candidate %d has missing layer %d", eventNumber, tkrCand.ID, site.m.Layer+1));
            }
        }
        if (firstSite == null) tkrCand.print("firstSite null", false);
        if (firstSite.aS == null) tkrCand.print("aS null", false);
        if (Math.abs(firstSite.aS.helix.a.v[2]) > kPar.kMax[1]) {
            if (debug) System.out.format("KalmanPatRecHPS.storeTrack: k=%10.4f is too large for candidate %d\n", 
                    firstSite.aS.helix.a.v[2], tkrCand.ID);
            return false;
        }
        if (Math.abs(firstSite.aS.helix.a.v[0]) > kPar.dRhoMax[1]) {
            if (debug) System.out.format("KalmanPatRecHPS.storeTrack: dRho=%10.4f is too large for candidate %d\n", 
                    firstSite.aS.helix.a.v[0], tkrCand.ID);
            return false;
        }
        if (Math.abs(firstSite.aS.helix.a.v[3]) > kPar.dzMax[1]) {
            if (debug) System.out.format("KalmanPatRecHPS.storeTrack: dz=%10.4f is too large for candidate %d\n", 
                    firstSite.aS.helix.a.v[3], tkrCand.ID);
            return false;
        }
        
        KalTrack tkr = new KalTrack(eventNumber, tkID, tkrCand.sites, yScat, XLscat, kPar);
        boolean redundant = false;
        for (KalTrack oldTkr : TkrList) {
            if (tkr.equals(oldTkr)) {
                redundant = true;
                break;
            }
        }
        if (redundant) {
            if (debug) System.out.format("KalmanPatRecHPS.storeTrack: throwing away redundant track %d\n", tkID);
            return false;
        } 
        for (MeasurementSite site : tkr.SiteList) {
            int theHit = site.hitID;
            if (theHit >= 0) site.m.hits.get(theHit).tracks.add(tkr); // Mark the hits as used
        }
        if (debug) {
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
            Double p1_2 = new Double(p1.getSecondElement());
            Double p2_2 = new Double(p2.getSecondElement());
            return p2_2.compareTo(p1_2);
        }
    };
    
    // Quick check on where the seed track is heading, using only the two axial layers in the seed
    private boolean seedNoGood(int j, int iter) {
        // j must point to an axial layer in the seed.
        // Find the previous axial layer, if there is one. . .
        for (int i=0; i<j; ++i) {
            if (!ah[i]) {
                double slope = (zh[j] - zh[i]) / (yh[j] - yh[i]);
                double zIntercept = zh[i] - slope * zh[j];
                //System.out.format("seedNoGood: i=%d lyr=%d, j=%d, lyr=%d slope=%10.5f, zInt=%10.5f\n", i, lyrs[i], j, lyrs[j], slope, zIntercept);
                if (Math.abs(zIntercept) > 1.1*kPar.dRhoMax[iter]) return true;
                if (Math.abs(slope) > 1.1*kPar.tanlMax[iter]) return true;
                return false;
            }
        }
        return false;
    }
}

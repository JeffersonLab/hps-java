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

    public KalmanPatRecHPS(ArrayList<SiModule> data, boolean verbose) {

        TkrList = new ArrayList<KalTrack>();
        int tkID = 0;
        int nModules = data.size();
        
        // Make a list of hits for each tracker layer, 0 through 12
        // This is needed because one layer can have multiple SiModules
        // Also make a list of Si modules with hits in each layer
        int numLayers = 13;
        ArrayList<ArrayList<KalHit>> lyrHits = new ArrayList<ArrayList<KalHit>>(numLayers);
        ArrayList<ArrayList<SiModule>> moduleList = new ArrayList<ArrayList<SiModule>>(numLayers);
        for (int lyr = 0; lyr < numLayers; lyr++) {
            ArrayList<KalHit> hits = new ArrayList<KalHit>();
            ArrayList<SiModule> modules = new ArrayList<SiModule>();
            lyrHits.add(hits);
            moduleList.add(modules);
        }
        for (SiModule thisSi : data) {
            for (Measurement m : thisSi.hits) {
                KalHit hit = new KalHit(thisSi,m);
                lyrHits.get(thisSi.Layer).add(hit);
            }
            if (thisSi.hits.size()>0) {
                moduleList.get(thisSi.Layer).add(thisSi);
            }
        }

        ArrayList<int[]> lyrList = new ArrayList<int[]>(6); // Array of seed strategies
        // Each list should contain 3 stereo layers and 2 non-stereo layers
        int[] list0 = { 5, 6, 7, 8, 9 };
        int[] list1 = { 3, 4, 5, 6, 7 };
        int[] list2 = { 4, 5, 7, 8, 9 };
        int[] list3 = { 4, 5, 6, 7, 9 };
        int[] list4 = { 2, 5, 7, 8, 9 };
        int[] list5 = { 3, 4, 5, 8, 9 };
        lyrList.add(list0);
        lyrList.add(list1);
        lyrList.add(list2);
        lyrList.add(list3);
        lyrList.add(list4);
        lyrList.add(list5);

        // Cut values:
        int nTries = 2; // Number of passes through this routing to find all tracks
        int nIterations = 2; // Number of Kalman filter iterations per track
        double[] kMax = { 3., 6. }; // Maximum curvature
        double[] tanlMax = { 0.08, 0.12 }; // Maximum tan(lambda)
        double[] dRhoMax = { 15., 25. }; // Maximum dRho at target plane in mm
        double[] chi2mx1 = { 5.0, 8.0 }; // Maximum chi**2/#hits
        double[] chi2mx2 = { 10.0, 15. }; // For a substandard track
        int[] minHits1 = { 9, 7 }; // Minimum number of hits for a track
        int[] minHits2 = { 7, 6 }; // For a substandard track
        double[] mxChi2Inc = { 10., 20. }; // Maximum increment to the chi^2 for a hit during smoothing
        double[] mxResid = { 22., 22. }; // Maximum residual, in units of detector resolution, for picking up a hit
        int minUnique = 3; // Minimum number of unique hits on a seed
        double mxResidShare = 6.; // Maximum residual for a hit to be shared
        double mxChi2double = 6.;
        int mxShared = 2; // Maximum number of shared hits

        Plane p0 = new Plane(new Vec(0., 0., 0.), new Vec(0., 1., 0.));

        if (verbose)
            System.out.format("Entering KalmanPatRecHPS with %d modules, for %d trials.\n", nModules, nTries);

        // Loop over seed strategies, each with 2 non-stereo layers and 3 stereo layers
        // For each strategy generate a seed track for every hit combination
        // Keep only those pointing more-or-less back to the origin and not too curved
        // Sort the list first by curvature, then by drho
        for (int trial = 0; trial < nTries; trial++) {
            if (verbose)
                System.out.format("KalmanPatRecHPS: start of pass %d through the algorithm.\n", trial);
            for (int[] list : lyrList) {
                int nLyrs = list.length;

                SiModule m0 = data.get(list[4]);
                double yOrigin = m0.p.X().v[1];
                Vec pivot = new Vec(0, yOrigin, 0.);
                Vec Bfield = KalmanInterface.getField(pivot, m0.Bfield);
                double Bmag = Bfield.mag();
                Vec tB = Bfield.unitVec(Bmag);
                if (verbose) {
                    System.out.format("KalmanPatRecHPS: layer list=%d %d %d %d %d\n", list[0], list[1], list[2], list[3], list[4]);
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
                                    // Cuts on the seed quality
                                    Vec hp = seed.helixParams();
                                    if (verbose) {
                                        System.out.format("Seed %d %d %d %d %d parameteters for cuts: K=%10.5f, tanl=%10.5f, dxz=%10.5f   ", idx[0], idx[1], idx[2], idx[3], idx[4], hp.v[2], hp.v[4], seed.planeIntersection(p0).mag());
                                    }
                                    if (Math.abs(hp.v[2]) < kMax[trial]) {
                                        if (Math.abs(hp.v[4]) < tanlMax[trial]) {
                                            Vec pInt = seed.planeIntersection(p0);
                                            if (verbose)
                                                System.out.format("intersection with target plane= %9.3f %9.3f %9.3f", pInt.v[0], pInt.v[1], pInt.v[2]);
                                            if (pInt.mag() < dRhoMax[trial]) {
                                                seedList.add(seed);
                                            }
                                        }
                                    }
                                    if (verbose)
                                        System.out.format("\n");
                                }
                            }
                        }
                    }
                }
                // Sort all of the seeds by distance from origin in x,z plane
                Collections.sort(seedList, SeedTrack.dRhoComparator);
                if (verbose) {
                    for (SeedTrack seed : seedList) {
                        seed.print("sorted seeds");
                    }
                }

                // Kalman filter the sorted seeds
                TkrList2 = new ArrayList<KalTrack>();
                for (SeedTrack seed : seedList) {
                    // Skip if too many seed hits are already taken
                    int nTaken = 0;
                    for (KalHit hit : seed.hits) {
                        if (hit.hit.tracks.size() > 0)
                            nTaken++;
                    }
                    if (nLyrs - nTaken < minUnique) {
                        if (verbose)
                            System.out.format("Skipping for hits used. nLyrs=%d, nTaken=%d\n", nLyrs, nTaken);
                        continue;
                    }

                    // Create an state vector from the input seed to initialize the Kalman filter
                    SquareMatrix Cov = seed.covariance();
                    Cov.scale(1000.);
                    StateVector sI = new StateVector(-1, seed.helixParams(), Cov, new Vec(0., 0., 0.), Bmag, tB, pivot, false);
                    if (verbose) {
                        System.out.format("\n\n Filtering seed with hits \n");
                        for (KalHit hit : seed.hits) {
                            hit.print(" filtered ");
                        }
                        System.out.format("   nTaken = %d\n", nTaken);
                    }

                    ArrayList<MeasurementSite> sites = new ArrayList<MeasurementSite>(nModules);
                    MeasurementSite startSite = null;
                    MeasurementSite newSite = null;
                    double chi2f = 0.;
                    MeasurementSite prevSite = null;
                    int thisSite = -1;
                    boolean success = true;
                    int nHits = 0;
                    // Filter from the start of the seed to the last downstream detector layer
                    layerLoop:
                    for (int lyr = list[0]; lyr < numLayers; lyr++) {
                        SiModule m = null;
                        int hitno = -1;
                        for (int j = 0; j < nLyrs; j++) {
                            if (lyr == list[j]) {
                                for (KalHit ht : seed.hits) {
                                    if (ht.module.Layer == lyr) {
                                        hitno = ht.module.hits.indexOf(ht.hit);
                                        m = ht.module;
                                        break;
                                    }
                                }
                            }
                        }
                        
                        for (int imod=0; imod < moduleList.get(lyr).size(); ++imod) {
                            SiModule thisSi = moduleList.get(lyr).get(imod);
                            if (m != null) {
                                if (thisSi != m) continue;
                            } else {
                                m = thisSi;
                            }
                            
                            newSite = new MeasurementSite(lyr, m, mxResid[trial], mxResidShare);
                            int rF;
                            if (lyr == list[0]) {
                                rF = newSite.makePrediction(sI, null, hitno, nTaken <= mxShared, true, imod<moduleList.get(lyr).size()-1);
                                if (rF > 0) {
                                    if (m.hits.get(newSite.hitID).tracks.size() > 0) {
                                        nTaken++;
                                    }
                                } else if (rF < 0) {
                                    if (rF == -2) continue;
                                    if (verbose) {
                                        System.out.format("KalmanPatRecHPS: Failed to make initial prediction at site %d, idx=%d. Abort\n", thisSite+1, idx);
                                    }
                                    success = false;
                                    break layerLoop;
                                }
                            } else {
                                rF = newSite.makePrediction(prevSite.aF, prevSite.m, hitno, nTaken <= mxShared, true, imod<moduleList.get(lyr).size()-1);
                                if (rF > 0) {
                                    if (m.hits.get(newSite.hitID).tracks.size() > 0) {
                                        nTaken++;
                                    }
                                } else if (rF < 0) {
                                    if (rF == -2) continue;
                                    if (verbose) {
                                        System.out.format("KalmanPatRecHPS: Failed to make prediction at site %d, idx=%d.  Abort\n", thisSite+1, idx);
                                    }
                                    success = false;
                                    break layerLoop;
                                }
                            }
                            thisSite++;
                            nHits += rF;
                            if (!newSite.filter()) {
                                if (verbose)
                                    System.out.format("KalmanPatRecHPS: Failed to filter at site %d, idx=%d.  Ignore remaining sites\n", thisSite, idx);
                                success = false;
                                break;
                            }
    
                            // if (verbose) {
                            // newSite.print("initial filtering");
                            // }
                            chi2f += newSite.chi2inc;
    
                            sites.add(newSite);
                            prevSite = newSite;
                            break;
                        }
                    }
                    if (!success) continue; // Try the next seed
                    if (verbose) {
                        System.out.format("\n KalmanPatRecHPS: Fit chi^2 initial filtering = %12.4e with %d hits, %d shared\n", chi2f, nHits, nTaken);
                        for (MeasurementSite site : sites) {
                            SiModule m = site.m;
                            StateVector aF = site.aF;
                            double phiF = aF.planeIntersect(m.p);
                            if (Double.isNaN(phiF))
                                phiF = 0.;
                            double vPred = site.h(aF, site.m, phiF);
                            int cnt = 2 * m.Layer;
                            if (m.isStereo)
                                cnt++;
                            System.out.format("   %d Lyr %d stereo=%5.2f Hit %d chi2inc=%10.6f, vPred=%10.6f; Hits: ", cnt, m.Layer, m.stereo, site.hitID, site.chi2inc, vPred);
                            for (Measurement hit : m.hits) {
                                System.out.format(" v=%10.6f #tks=%d,", hit.v, hit.tracks.size());
                            }
                            System.out.format("\n");
                        }
                    }

                    if (nHits < minHits2[trial]) {
                        if (verbose)
                            System.out.format("Initial filtering has too few hits. Skip to the next seed.\n");
                        continue;
                    }
                    if (chi2f / (double) nHits > chi2mx2[trial]) {
                        if (verbose)
                            System.out.format("Initial filtering has too large chi^2. Skip to the next seed.\n");
                        continue;
                    }
                    int[] hits = new int[numLayers];
                    for (int i = 0; i < numLayers; i++) {
                        hits[i] = -1;
                    }
                    for (MeasurementSite site : sites) { // Save the hit assignments for the next step
                        SiModule m = site.m;
                        hits[m.Layer] = site.hitID;
                    }
                    if (verbose) {
                        System.out.format("Hits after initial filtering= ");
                        for (int i = 0; i < numLayers; i++) {
                            System.out.format("%2d ", hits[i]);
                        }
                        System.out.format("\n");
                    }

                    startSite = newSite; // The fit will be restarted at the last site in the previous filtering

                    // Restart the fit at the last layer and filter to the 1st layer. Iterate the fit if necessary.
                    double chi2s = 0;
                    nTaken = 0;
                    for (int iteration = 0; iteration < nIterations; iteration++) {
                        if (iteration == 0) {
                            sites.clear();
                        }
                        nHits = 0;
                        int nStereo = 0;
                        StateVector sH = null;
                        if (startSite.smoothed) {
                            sH = startSite.aS.copy();
                        } else {
                            sH = startSite.aF.copy();
                        }
                        sH.C.scale(1000.); // Blow up the initial covariance matrix to avoid double counting measurements
                        if (verbose) {
                            System.out.format("KalmanTrackFit: starting filtering for iteration %d\n", iteration);
                            // sH.print("starting state vector for iteration");
                        }

                        chi2f = 0.;
                        success = true;
                        thisSite = -1;
                        // Prediction and filter step
                        layerLoop: 
                        for (int lyr=numLayers-1; lyr >= 0; --lyr) { 
                            for (int imod=0; imod<moduleList.get(lyr).size(); ++imod) {
                                SiModule m = moduleList.get(lyr).get(imod);
                                
                                newSite = new MeasurementSite(thisSite+1, m, mxResid[trial], mxResidShare);
                                int rF;
                                int theHit = -1;
                                if (iteration > 0) {
                                    theHit = sites.get(thisSite+1).hitID;
                                } else  {
                                    theHit = hits[m.Layer];
                                }
                                // Note: for the last module in the list we don't check detector bounds. We have to use something.
                                if (thisSite < 0) {
                                    rF = newSite.makePrediction(sH, null, theHit, nTaken <= mxShared, iteration < nIterations - 1, imod < moduleList.get(lyr).size()-1);
                                    if (rF > 0) {
                                        if (m.hits.get(newSite.hitID).tracks.size() > 0) { // Check if the hit is shared
                                            nTaken++;
                                        }
                                    } else if (rF < 0) {
                                        if (rF == -2) {
                                            continue;  // Try the next module
                                        }
                                        if (verbose) {
                                            System.out.format("KalmanPatRecHPS: Failed to make initial prediction at site %d, iteration %d.  Abort\n", thisSite+1, iteration);
                                        }
                                        success = false;
                                        break layerLoop;
                                    }
                                } else {
                                    rF = newSite.makePrediction(prevSite.aF, prevSite.m, theHit, nTaken <= mxShared, iteration < nIterations - 1, imod < moduleList.get(lyr).size()-1);
                                    if (rF > 0) {
                                        if (m.hits.get(newSite.hitID).tracks.size() > 0) {  // Check if the hit is shared
                                            nTaken++;
                                        }
                                    } else if (rF < 0) {
                                        if (rF == -2) {
                                            continue; // Try the next module
                                        }
                                        if (verbose) {
                                            System.out.format("KalmanPatRecHPS: Failed to make prediction at site %d, iteration %d.  Abort seed\n", thisSite+1, iteration);
                                        }
                                        success = false;
                                        break layerLoop;
                                    }
                                }
                                thisSite++;
    
                                if (!newSite.filter()) {
                                    if (verbose)
                                        System.out.format("KalmanPatRecHPS: Failed to filter at site %d, iteration %d.  Ignore remaining sites and try next seed.\n", thisSite, iteration);
                                    success = false;
                                    break;
                                }
                                if (rF == 1) {
                                    nHits++;
                                    if (m.isStereo) {
                                        nStereo++;
                                    }
                                }
    
                                // if (verbose) {
                                // newSite.print(String.format("Iteration %d: filtering", iteration));
                                // }
                                chi2f += newSite.chi2inc;
                                if (iteration == 0) {
                                    sites.add(newSite);
                                } else {
                                    sites.set(thisSite, newSite);
                                }
                                prevSite = newSite;
                                break;  // Don't look at more modules, as we already found one to use
                            }
                        }
                        if (!success) {
                            if (verbose)
                                System.out.format("\n KalmanPatRecHPS: failed fit at iteration %d. Try next seed.\n", iteration);
                            break;
                        }
                        if (nStereo < 4) {
                            if (verbose)
                                System.out.format("KalmanPatRecHPS: iteration %d, filtering has too few stereo hits. Skip to the next seed.\n", iteration);
                            success = false;
                            break;
                        }
                        if (nHits - nStereo < 2) {
                            if (verbose)
                                System.out.format("KalmanPatRecHPS: iteration %d, filtering has too few non-stereo hits. Skip to the next seed.\n", iteration);
                            success = false;
                            break;
                        }
                        if (verbose) {
                            System.out.format("\n KalmanPatRecHPS: iteration %d, Fit chi^2 after first filtering = %12.4e for %d hits, %d stereo, %d shared\n", iteration, chi2f, nHits, nStereo, nTaken);
                            int cnt = 11;
                            for (MeasurementSite site : sites) {
                                SiModule m = site.m;
                                StateVector aF = site.aF;
                                double phiF = aF.planeIntersect(m.p);
                                if (Double.isNaN(phiF))
                                    phiF = 0.;
                                double vPred = site.h(aF, site.m, phiF);
                                System.out.format("   %d Lyr %d stereo=%5.2f Hit %d chi2inc=%10.6f, vPred=%10.6f; Hits: ", cnt, m.Layer, m.stereo, site.hitID, site.chi2inc, vPred);
                                for (Measurement hit : m.hits) {
                                    System.out.format(" v=%10.6f #tks=%d,", hit.v, hit.tracks.size());
                                }
                                System.out.format("\n");
                                cnt--;
                            }
                        }

                        chi2s = 0.;
                        int nRemoved = 0;
                        MeasurementSite nextSite = null;
                        MeasurementSite currentSite = null;
                        for (int idxS = sites.size() - 1; idxS >= 0; idxS--) {
                            currentSite = sites.get(idxS);
                            if (nextSite == null) {
                                currentSite.aS = currentSite.aF.copy();
                                currentSite.smoothed = true;
                            } else {
                                currentSite.smooth(nextSite);
                            }
                            int ID = currentSite.hitID;
                            if (ID >= 0) {
                                if (currentSite.chi2inc > mxChi2Inc[trial] && iteration != nIterations - 1 && nHits > minHits2[trial]) {
                                    boolean stereo = currentSite.m.isStereo;
                                    if ((stereo && nStereo > 4) || (!stereo && nHits - nStereo > 2)) {
                                        if (currentSite.removeHit()) {
                                            if (verbose)
                                                System.out.format("KalmanPatRecHPS: removing hit %d with chi^2inc=%10.2f from Layer %d, stereo %6.2f\n", ID, currentSite.chi2inc, currentSite.m.Layer, currentSite.m.stereo);
                                            nHits--;
                                            if (stereo)
                                                nStereo--;
                                            nRemoved++;
                                        }
                                    }
                                } else {
                                    chi2s += currentSite.chi2inc;
                                }
                            }
                            if (currentSite.hitID < 0 && iteration != nIterations - 1) { // Look for a better hit
                                Measurement addedHit = currentSite.addHit(mxChi2Inc[trial], ID);
                                if (addedHit != null) {
                                    nHits++;
                                    if (verbose)
                                        System.out.format("KalmanPatRecHPS: adding hit %d with chi^2inc=%10.2f to layer %d\n", currentSite.hitID, currentSite.chi2inc, idxS);
                                }
                            }

                            // if (verbose) {
                            // currentSite.print(String.format("Iteration %d smoothing", iteration));
                            // }
                            nextSite = currentSite;
                        }
                        if (verbose) {
                            System.out.format("\nKalmanPatRecHPS: Iteration %d, Fit chi^2 after smoothing = %12.4e for %d hits\n", iteration, chi2s, nHits);
                            int cnt = 11;
                            for (MeasurementSite site : sites) {
                                SiModule m = site.m;
                                StateVector aS = site.aS;
                                double phiS = aS.planeIntersect(m.p);
                                if (Double.isNaN(phiS))
                                    phiS = 0.;
                                double vPred = site.h(aS, site.m, phiS);
                                System.out.format("   %d Lyr %d stereo=%5.2f Hit %d chi2inc=%10.6f, vPred=%10.6f; Hits: ", cnt, m.Layer, m.stereo, site.hitID, site.chi2inc, vPred);
                                for (Measurement hit : m.hits) {
                                    System.out.format(" v=%10.6f #tks=%d,", hit.v, hit.tracks.size());
                                }
                                System.out.format("\n");
                                cnt--;
                            }
                        }
                        startSite = currentSite;
                        if (iteration == nIterations - 2 && nRemoved == 0)
                            break;
                    }

                    if (verbose) {
                        if (!success)
                            System.out.format("KalmanPatRecHPS: failed fit\n");
                        System.out.format("KalmanPatRecHPS: Final fit chi^2 after smoothing = %12.4e for %d hits\n", chi2s, nHits);
                        Vec afF = sites.get(sites.size() - 1).aF.a;
                        Vec afC = sites.get(sites.size() - 1).aF.helixErrors();
                        afF.print("KalmanPatRecHPS helix parameters at final filtered site");
                        afC.print("KalmanPatRecHPS helix parameter errors");
                        if (startSite != null) {
                            startSite.aS.a.print("KalmanPatRecHPS helix parameters at the final smoothed site");
                            startSite.aS.helixErrors().print("KalmanPatRecHPS helix parameter errors:");
                        }
                    }
                    // If the fit looks good, save the track information and mark the hits as used
                    if (success) {
                        if (nHits >= minHits1[trial] && chi2s / (double) nHits < chi2mx1[trial]) { // Good tracks
                            tkID++;
                            KalTrack tkr = new KalTrack(1, tkID, nHits, sites, chi2s);
                            for (MeasurementSite site : sites) {
                                int theHit = site.hitID;
                                if (theHit < 0)
                                    continue;
                                site.m.hits.get(theHit).tracks.add(tkr); // Mark the hits as used
                            }
                            if (verbose)
                                System.out.format("Adding track with %d hits and smoothed chi^2=%10.5f\n", nHits, chi2s);
                            TkrList.add(tkr);
                        } else if (nHits >= minHits2[trial] && chi2s / (double) nHits < chi2mx2[trial]) { // Low quality tracks; don't kill hits
                            tkID++;
                            KalTrack tkr = new KalTrack(1, tkID, nHits, sites, chi2s);
                            TkrList2.add(tkr);
                        }
                    }
                } // Next seed in set
            } // Next set of seeds

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
                        if (site.hitID < 0)
                            continue;
                        SiModule m = site.m;
                        if (m.hits.get(site.hitID).tracks.size() > 0) {
                            if (site.chi2inc > mxChi2double)
                                site.removeHit();
                            else
                                nShared++;
                        }
                        if (site.hitID >= 0) {
                            nHits++;
                            if (m.isStereo)
                                nStereo++;
                        }
                    }
                    if (nShared < 2 && nHits >= minHits2[trial] && nStereo > 3 && nHits - nStereo > 1) {
                        for (MeasurementSite site : tkr.SiteList) {
                            if (site.hitID < 0)
                                continue;
                            site.m.hits.get(site.hitID).tracks.add(tkr);
                        }
                        if (verbose)
                            System.out.format("KalmanPatRecHPS: adding substandard track %d to good track list.\n", tkr.ID);
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
                            if (tkr == bestTkr)
                                continue;
                            int idx = tkr.whichSite(m);
                            if (idx < 0) {
                                System.out.format("KalmanPatRecHPS: bad reference from hit to track. Track %d, Layer = %d\n", tkr.ID, m.Layer);
                            } else {
                                MeasurementSite site = tkr.SiteList.get(idx);
                                if (site.chi2inc > mxChi2double) {
                                    changedTracks.add(tkr);
                                    int oldID = site.hitID;
                                    if (!site.smoothed)
                                        System.out.format("KalmanPatRecHPS: oops, why isn't this site smoothed?");
                                    site.removeHit();
                                    if (verbose)
                                        System.out.format("KalmanPatRecHPS: removing a hit from Track %d, Layer %d\n", tkr.ID, m.Layer);
                                    // Check whether there might be another hit available
                                    Measurement addedHit = site.addHit(mxChi2Inc[0], oldID);
                                    if (addedHit != null) {
                                        addedHit.tracks.add(tkr);
                                        if (verbose)
                                            System.out.format("KalmanPatRecHPS: added a hit after removing one for Track %d, Layer %d\n", tkr.ID, m.Layer);
                                    }
                                    tkrsToRemove.add(tkr);
                                }
                            }
                        }
                        for (KalTrack tkr : tkrsToRemove) {
                            hit.tracks.remove(tkr);
                        }
                    }
                }
            }
        }

        // Refit tracks that got changed
        ArrayList<KalTrack> allTks = new ArrayList<KalTrack>(TkrList.size());
        for (KalTrack tkr : TkrList)
            allTks.add(tkr); // (refit them all for now)
        for (KalTrack tkr : allTks) {
            // check that there are enough hits in both views
            int nStereo = 0;
            int nNonStereo = 0;
            for (MeasurementSite site : tkr.SiteList) {
                if (site.hitID < 0)
                    continue;
                SiModule m = site.m;
                if (!m.isStereo)
                    nNonStereo++;
                else
                    nStereo++;
            }
            if (nStereo < 4)
                TkrList.remove(tkr);
            else if (nNonStereo < 2)
                TkrList.remove(tkr);
            else if (nNonStereo + nStereo < minHits2[nTries - 1])
                TkrList.remove(tkr);
            else
                tkr.sortSites(true);
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

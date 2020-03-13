package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.Iterator;

//Driver program for executing a Kalman fit.  This version starts at layer N, filters to layer 0,
//then starts over using the fit result to start filtering from layer 0 outward. Then it smooths
//back to layer 0. The code assumes that the SiModules hold all the hits to be fit and only those hits.
//No pattern recognition is done; no hits are dropped.
class KalmanTrackFit2 {

    ArrayList<MeasurementSite> sites;
    int initialSite;
    int finalSite;
    double chi2f, chi2s; // Filtered and smoothed chi squared values (just summed over the N measurement
                         // sites)
    boolean success;
    KalTrack tkr;

    KalmanTrackFit2(int evtNumb, ArrayList<SiModule> data, // List of Si modules with data points to be included in the fit
            int start, // Starting point in the list
            int nIterations, // Number of fit iterations requested
            Vec pivot, // Pivot point for the starting "guess" helix
            Vec helixParams, // 5 helix parameters for the starting "guess" helix
            SquareMatrix C, // Full covariance matrix for the starting "guess" helix
            org.lcsim.geometry.FieldMap fM, boolean verbose) {

        success = true;
        tkr = null;

        // Create an state vector from the input seed to initialize the Kalman filter
        Vec Bfield = KalmanInterface.getField(pivot, fM);
        double B = Bfield.mag();
        Vec t = Bfield.unitVec(B);
        StateVector sI = new StateVector(-1, helixParams, C, new Vec(0., 0., 0.), B, t, pivot, false);

        if (verbose) {
            System.out.format("KalmanTrackFit2: begin Kalman fit, start=%d, number iterations=%d\n", start, nIterations);
            pivot.print("KalmanTrackFit2 pivot point");
            sI.print("initial state for KalmanTrackFit");
        }

        double mxResid = 9999.;

        sites = new ArrayList<MeasurementSite>();
        initialSite = 0;
        finalSite = 0;

        int prevSite;
        int thisSite;
        MeasurementSite startSite = null;
        if (start != 0) {
            MeasurementSite newSite = null;
            chi2f = 0.;
            prevSite = -1;
            thisSite = -1;
            for (int idx = start; idx > -1; idx--) {
                finalSite = idx;
                SiModule m = data.get(idx);
                if (m.Layer >= 0 && m.hits.size() == 0) { continue; }
                thisSite++;
                newSite = new MeasurementSite(idx, m, mxResid, mxResid);
                if (idx == start) {
                    if (newSite.makePrediction(sI, 0, false, false) < 0) {
                        System.out.format("KalmanTrackFit2: Failed to make initial prediction at site %d, idx=%d.  Abort\n", thisSite, idx);
                        success = false;
                        break;
                    }
                } else {
                    if (newSite.makePrediction(sites.get(prevSite).aF, sites.get(prevSite).m, 0, false, false) < 0) {
                        System.out.format("KalmanTrackFit2: Failed to make prediction at site %d, idx=%d.  Abort\n", thisSite, idx);
                        success = false;
                        break;
                    }
                }

                if (!newSite.filter()) {
                    System.out.format("KalmanTrackFit2 72: Failed to filter at site %d, idx=%d.  Ignore remaining sites\n", thisSite, idx);
                    success = false;
                    break;
                }
                ;

                // if (verbose) {
                // newSite.print("initial filtering");
                // }
                if (m.Layer >= 0) { chi2f += newSite.chi2inc; }

                sites.add(newSite);

                prevSite = thisSite;
            }
            if (!success) { return; }
            if (verbose) {
                System.out.format("KalmanTrackFit2: Fit chi^2 after initial filtering = %12.4e;  Final site = %d\n", chi2f, finalSite);
                newSite.aF.a.print("filtered helix parameters at innermost site.");
                int cnt = 0;
                for (MeasurementSite site : sites) {
                    SiModule m = site.m;
                    StateVector aF = site.aF;
                    double phiF = aF.planeIntersect(m.p);
                    if (Double.isNaN(phiF)) phiF = 0.;
                    double vPred = site.h(aF, site.m, phiF);
                    System.out.format("   %d Lyr %d stereo=%b Hit %d chi2inc=%10.6f, vPred=%10.6f; Hits: ", cnt, m.Layer, m.isStereo,
                            site.hitID, site.chi2inc, vPred);
                    for (Measurement hit : m.hits) {
                        double resid = hit.v - vPred;
                        System.out.format(" v=%10.6f r=%10.8f #tks=%d,", hit.v, resid, hit.tracks.size());
                    }
                    System.out.format("\n");
                    cnt++;
                }
            }
            startSite = newSite;
        }

        int nHits = 0;
        // Restart the fit at the first layer and iterate the fit if requested
        for (int iteration = 0; iteration < nIterations; iteration++) {
            StateVector sH = null;
            if (startSite == null) {
                sH = sI;
            } else {
                if (startSite.smoothed) {
                    sH = startSite.aS.copy();
                } else {
                    sH = startSite.aF.copy();
                }
                sH.C.scale(1000.); // Blow up the initial covariance matrix to avoid double counting measurements
            }
            if (verbose) {
                System.out.format("KalmanTrackFit: starting filtering for iteration %d\n", iteration);
                // sH.print("starting state vector for iteration");
            }
            sites.clear();

            Iterator<SiModule> itr = data.iterator();
            chi2f = 0.;
            boolean success = true;
            MeasurementSite previousSite = null;
            thisSite = -1;
            while (itr.hasNext()) {
                SiModule m = itr.next();
                if (m.Layer >= 0 && m.hits.size() == 0) {
                    continue; //FixMe: eventually need to do prediction for every layer, even if no hit
                }
                thisSite++;
                MeasurementSite newSite = new MeasurementSite(thisSite, m, mxResid, mxResid);
                if (thisSite == 0) {
                    if (newSite.makePrediction(sH, 0, false, false) < 0) {
                        System.out.format("KalmanTrackFit2: Failed to make initial prediction at site %d.  Abort\n", thisSite);
                        success = false;
                        break;
                    }
                } else {
                    if (newSite.makePrediction(previousSite.aF, previousSite.m, 0, false, false) < 0) {
                        System.out.format("KalmanTrackFit2: Failed to make prediction at site %d.  Abort\n", thisSite);
                        success = false;
                        break;
                    }
                }

                if (!newSite.filter()) {
                    System.out.format("KalmanTrackFit2 153: Failed to filter at site %d.  Ignore remaining sites\n", thisSite);
                    success = false;
                    break;
                }
                if (verbose && previousSite != null) {
                    System.out.format("KalmanTrackFit2: filter at layer %d detector %d\n", m.Layer, m.detector);
                    previousSite.aF.a.print("old filtered helix");
                    newSite.aP.a.print("new predicted helix");
                    newSite.aF.a.print("new filtered helix");
                    previousSite.aF.C.print("old filtered covariance");
                    newSite.aP.C.print("new predicted covariance");
                    newSite.aF.C.print("new filtered covariance");
                }
                // if (verbose) {
                // newSite.print(String.format("Iteration %d: filtering", iteration));
                // }
                if (m.Layer >= 0) { chi2f += newSite.chi2inc; }

                sites.add(newSite);

                previousSite = newSite;
            }
            if (!success) { return; }
            if (verbose) {
                System.out.format("KalmanTrackFit2: Fit chi^2 after first full filtering = %12.4e\n", chi2f);
                previousSite.aF.a.print("filtered helix parameters at last site filtered");
                int cnt = 0;
                for (MeasurementSite site : sites) {
                    SiModule m = site.m;
                    StateVector aF = site.aF;
                    double phiF = aF.planeIntersect(m.p);
                    if (Double.isNaN(phiF)) phiF = 0.;
                    double vPred = site.h(aF, site.m, phiF);
                    double phiP = site.aP.planeIntersect(m.p);
                    if (Double.isNaN(phiP)) phiP = 0.;
                    double vPredP = site.h(site.aP, site.m, phiP);
                    System.out.format("   %d Lyr %d stereo=%b Hit %d chi2inc=%10.6f, vPred=%10.6f; vPredP=%10.6f Hits: ", cnt, m.Layer,
                            m.isStereo, site.hitID, site.chi2inc, vPred, vPredP);
                    for (Measurement hit : m.hits) {
                        double resid = hit.v - vPred;
                        System.out.format(" v=%10.6f r=%10.8f #tks=%d,", hit.v, resid, hit.tracks.size());
                    }
                    System.out.format("\n");
                    if (m.Layer == 11) site.print("for comparison");
                    cnt++;
                }
            }
            chi2s = 0.;
            MeasurementSite nextSite = null;
            MeasurementSite currentSite = null;
            for (int idx = sites.size() - 1; idx >= 0; idx--) {
                currentSite = sites.get(idx);
                if (currentSite.m.Layer < 0 && currentSite.m.hits.size() == 0) {
                    currentSite.aS = currentSite.aF;
                    continue;
                }

                if (nextSite == null) {
                    currentSite.aS = currentSite.aF.copy();
                    currentSite.smoothed = true;
                } else {
                    currentSite.smooth(nextSite);
                }
                chi2s += currentSite.chi2inc;
                if (iteration == nIterations - 1) nHits++;

                // if (verbose) {
                // currentSite.print(String.format("Iteration %d smoothing", iteration));
                // }
                nextSite = currentSite;
            }

            if (verbose) {
                System.out.format("KalmanTrackFit2: Iteration %d, Fit chi^2 after smoothing = %12.4e\n", iteration, chi2s);
                currentSite.aS.a.print("smoothed helix parameters at the innermost site");
                int cnt = 0;
                double chi2prime = 0.;
                for (MeasurementSite site : sites) {
                    double phiS = site.aS.planeIntersect(site.m.p);
                    if (Double.isNaN(phiS)) phiS = 0.;
                    double vpred = site.h(site.aS, site.m, phiS);
                    System.out.format("   %d Lyr %d stereo=%b Hit %d chi2inc=%10.6f r1=%10.8f err=%10.8f vPred=%10.6f; Hits: ", cnt,
                            site.m.Layer, site.m.isStereo, site.hitID, site.chi2inc, site.aS.r, Math.sqrt(site.aS.R), vpred);
                    for (Measurement hit : site.m.hits) {
                        double resid = hit.v - vpred; // verified to be the same as site.aS.R
                        System.out.format(" v=%10.6f sigma=%10.8f #tks=%d,", hit.v, hit.sigma, hit.tracks.size());
                        chi2prime += Math.pow(resid / hit.sigma, 2);
                    }
                    System.out.format("\n");
                    cnt++;
                }
                System.out.format("Kalman fit chi**2 calculated directly from residuals and hit errors: %10.4f\n", chi2prime);
            }
            startSite = currentSite;
        }

        finalSite = sites.size() - 1;

        tkr = new KalTrack(evtNumb, 0, nHits, sites, chi2s); // Store the fit information as a KalTrack object
        for (MeasurementSite site : sites) { // Mark the hits as used
            for (Measurement hit : site.m.hits) { // Should be only a single hit (this is not a pattern recognition routine)
                hit.tracks.add(tkr);
            }
        }
        tkr.originHelix(); // Propagates the track to the origin region

        if (verbose) {
            System.out.format("KalmanTrackFit2: Final fit chi^2 after smoothing = %12.4e\n", chi2s);
            double[] hp = tkr.originHelixParms();
            System.out.format("   At origin, drho=%10.7f, phi0=%10.7f, K=%10.7f, dz=%10.7f, tanl=%10.7f\n", hp[0], hp[1], hp[2], hp[3], hp[4]);
            Vec afF = sites.get(sites.size() - 1).aF.a;
            Vec afC = sites.get(sites.size() - 1).aF.helixErrors();
            afF.print("KalmanFit helix parameters at final filtered site");
            afC.print("KalmanFit helix parameter errors");
            if (startSite != null) {
                startSite.aS.a.print("KalmanFit helix parameters at the final smoothed site");
                startSite.aS.helixErrors().print("KalmanFit helix parameter errors:");
            }
        }
    }

    public StateVector fittedStateBegin() {
        return sites.get(initialSite).aS;
    }

    public StateVector fittedStateEnd() {
        return sites.get(sites.size() - 1).aS;
    }

    public void printFit(String s) {
        System.out.format("KalmanTrackFit2: dump of track information for %s\n", s);
        System.out.format("    Final fit chi^2 after filtering = %12.4e\n", chi2f);
        StateVector fS = sites.get(finalSite).aF;
        double[] a = fS.a.v;
        System.out.format("    Helix parameters at the outermost layer=%f10.7 %f10.7 %f10.7 %f10.7 %f10.7\n", a[0], a[1], a[2], a[3], a[4]);
        double Bmag = fS.B;
        double[] tB = { fS.Rot.M[2][0], fS.Rot.M[2][1], fS.Rot.M[2][2] };
        System.out.format("        B-field at the outermost layer=%10.6f,  direction=%8.6f %8.6f %8.6f\n", Bmag, tB[0], tB[1], tB[2]);
        System.out.format("    Final fit chi^2 after smoothing = %12.4e\n", chi2s);
        StateVector iS = sites.get(initialSite).aS;
        a = iS.a.v;
        System.out.format("    Helix parameters at the innermost layer=%f10.7 %f10.7 %f10.7 %f10.7 %f10.7\n", a[0], a[1], a[2], a[3], a[4]);
        Bmag = iS.B;
        double[] tB2 = { iS.Rot.M[2][0], iS.Rot.M[2][1], iS.Rot.M[2][2] };
        System.out.format("        B-field at the innermost layer=%10.6f,  direction=%8.6f %8.6f %8.6f\n", Bmag, tB2[0], tB2[1], tB2[2]);
        if (tkr != null) {
            tkr.originHelix();
            double[] aO = tkr.originHelixParms();
            System.out.format("    Helix parameters at the origin=%f10.7 %f10.7 %f10.7 %f10.7 %f10.7\n", aO[0], aO[1], aO[2], aO[3], aO[4]);
        }
    }

    public void print(String s) {
        System.out.format("KalmanTrackFit: dump of the fitted sites, %s, chi2f=%12.4e, chi2s=%12.4e\n", s, chi2f, chi2s);
        Iterator<MeasurementSite> itr = sites.iterator();
        while (itr.hasNext()) {
            MeasurementSite currentSite = itr.next();
            currentSite.print("dump of fit");
        }
    }
}

package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.Iterator;

//Driver program for executing a Kalman fit.  This version starts at layer N, filters to layer 0,
//then starts over using the fit result to start filtering from layer 0 outward. Then it smooths
//back to layer 0.
public class KalmanTrackFit2 {

    ArrayList<MeasurementSite> sites;
    int initialSite;
    int finalSite;
    double chi2f, chi2s; // Filtered and smoothed chi squared values (just summed over the N measurement sites)
    boolean success;

    public KalmanTrackFit2(ArrayList<SiModule> data, // List of Si modules with data points to be included in the fit
            int start, // Starting point in the list
            int nIterations, // Number of fit iterations requested
            Vec pivot, // Pivot point for the starting "guess" helix
            Vec helixParams, // 5 helix parameters for the starting "guess" helix
            SquareMatrix C, // Full covariance matrix for the starting "guess" helix
            FieldMap fM, boolean verbose) {

        success = true;

        // Create an state vector from the input seed to initialize the Kalman filter
        Vec Bfield = fM.getField(pivot);
        double B = Bfield.mag();
        Vec t = Bfield.unitVec(B);
        StateVector sI = new StateVector(-1, helixParams, C, new Vec(0., 0., 0.), B, t, pivot, false);

        if (verbose) {
            System.out.format("KalmanTrackFit2: begin Kalman fit, start=%d, number iterations=%d\n", start, nIterations);
            // sI.print("initial state for KalmanTrackFit");
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
                thisSite++;
                newSite = new MeasurementSite(idx, m, mxResid, mxResid);
                if (idx == start) {
                    if (newSite.makePrediction(sI, 0, false, false) < 0) {
                        System.out.format("KalmanTrackFit2: Failed to make initial prediction at site %d, idx=%d.  Abort\n", thisSite, idx);
                        success = false;
                        break;
                    }
                } else {
                    if (newSite.makePrediction(sites.get(prevSite).aF, 0, false, false) < 0) {
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

                if (verbose) {
                    newSite.print("initial filtering");
                }
                chi2f += newSite.chi2inc;

                sites.add(newSite);

                prevSite = thisSite;
            }
            if (verbose) {
                System.out.format("KalmanTrackFit2: Fit chi^2 after initial filtering = %12.4e;  Final site = %d\n", chi2f, finalSite);
                int cnt = 0;
                for (MeasurementSite site : sites) {
                    SiModule m = site.m;
                    StateVector aF = site.aF;
                    double phiF = aF.planeIntersect(m.p);
                    if (Double.isNaN(phiF))
                        phiF = 0.;
                    double vPred = site.h(aF, phiF);
                    System.out.format("   %d Lyr %d stereo=%5.2f Hit %d chi2inc=%10.6f, vPred=%10.6f; Hits: ", cnt, m.Layer, m.stereo, site.hitID, site.chi2inc, vPred);
                    for (Measurement hit : m.hits) {
                        System.out.format(" v=%10.6f #tks=%d,", hit.v, hit.tracks.size());
                    }
                    System.out.format("\n");
                    cnt++;
                }
            }
            if (!success)
                return;
            startSite = newSite;
        }

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
                thisSite++;
                MeasurementSite newSite = new MeasurementSite(thisSite, m, mxResid, mxResid);
                if (thisSite == 0) {
                    if (newSite.makePrediction(sH, 0, false, false) < 0) {
                        System.out.format("KalmanTrackFit2: Failed to make initial prediction at site %d.  Abort\n", thisSite);
                        success = false;
                        break;
                    }
                } else {
                    if (newSite.makePrediction(previousSite.aF, 0, false, false) < 0) {
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

                // if (verbose) {
                // newSite.print(String.format("Iteration %d: filtering", iteration));
                // }
                chi2f += newSite.chi2inc;

                sites.add(newSite);

                previousSite = newSite;
            }
            if (verbose) {
                System.out.format("KalmanTrackFit2: Fit chi^2 after first full filtering = %12.4e\n", chi2f);
                int cnt = 0;
                for (MeasurementSite site : sites) {
                    SiModule m = site.m;
                    StateVector aF = site.aF;
                    double phiF = aF.planeIntersect(m.p);
                    if (Double.isNaN(phiF))
                        phiF = 0.;
                    double vPred = site.h(aF, phiF);
                    System.out.format("   %d Lyr %d stereo=%5.2f Hit %d chi2inc=%10.6f, vPred=%10.6f; Hits: ", cnt, m.Layer, m.stereo, site.hitID, site.chi2inc, vPred);
                    for (Measurement hit : m.hits) {
                        System.out.format(" v=%10.6f #tks=%d,", hit.v, hit.tracks.size());
                    }
                    System.out.format("\n");
                    cnt++;
                }
            }
            if (!success) {
                return;
            }
            chi2s = 0.;
            MeasurementSite nextSite = null;
            MeasurementSite currentSite = null;
            for (int idx = sites.size() - 1; idx >= 0; idx--) {
                currentSite = sites.get(idx);

                if (nextSite == null) {
                    currentSite.aS = currentSite.aF.copy();
                    currentSite.smoothed = true;
                } else {
                    currentSite.smooth(nextSite);
                }
                chi2s += currentSite.chi2inc;

                // if (verbose) {
                // currentSite.print(String.format("Iteration %d smoothing", iteration));
                // }
                nextSite = currentSite;
            }
            if (verbose) {
                System.out.format("KalmanTrackFit2: Iteration %d, Fit chi^2 after smoothing = %12.4e\n", iteration, chi2s);
                int cnt = 0;
                for (MeasurementSite site : sites) {
                    SiModule m = site.m;
                    StateVector aS = site.aS;
                    double phiS = aS.planeIntersect(m.p);
                    if (Double.isNaN(phiS))
                        phiS = 0.;
                    double vPred = site.h(aS, phiS);
                    System.out.format("   %d Lyr %d stereo=%5.2f Hit %d chi2inc=%10.6f, vPred=%10.6f; Hits: ", cnt, m.Layer, m.stereo, site.hitID, site.chi2inc, vPred);
                    for (Measurement hit : m.hits) {
                        System.out.format(" v=%10.6f #tks=%d,", hit.v, hit.tracks.size());
                    }
                    System.out.format("\n");
                    cnt++;
                }
            }
            startSite = currentSite;
        }

        if (verbose) {
            System.out.format("KalmanTrackFit2: Final fit chi^2 after smoothing = %12.4e\n", chi2s);
            Vec afF = sites.get(sites.size() - 1).aF.a;
            Vec afC = sites.get(sites.size() - 1).aF.helixErrors();
            afF.print("KalmanFit helix parameters at final filtered site");
            afC.print("KalmanFit helix parameter errors");
            if (startSite != null) {
                startSite.aS.a.print("KalmanFit helix parameters at the final smoothed site");
                startSite.aS.helixErrors().print("KalmanFit helix parameter errors:");
            }
        }
        finalSite = sites.size() - 1;
    }

    public StateVector fittedStateBegin() {
        return sites.get(initialSite).aS;
    }

    public StateVector fittedStateEnd() {
        return sites.get(sites.size() - 1).aS;
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

package kalman;

import java.util.ArrayList;
import java.util.Iterator;

//Driver program for executing a Kalman fit.  At first it takes just the simplest case of starting
// from one end, filtering to the other end, and then smoothing.  No pattern recognition is done here.
public class KalmanTrackFit {

    ArrayList<MeasurementSite> sites;
    int initialSite;
    int finalSite;
    double chi2f, chi2s; // Filtered and smoothed chi squared values (just summed over the N measurement sites)

    private boolean stopCondition(int dir, int i, int N) {
        if (dir > 0) {
            return (i < N);
        } else {
            return (i >= 0);
        }
    }

    private int increment(int dir, int i) {
        if (dir > 0)
            return i + 1;
        else
            return i - 1;
    }

    public KalmanTrackFit(ArrayList<SiModule> data, // List of Si modules with data points to be included in the fit
                                    int start, // Starting point in the list
                                    int direction, // Proceed to larger indices in the list (1) or smaller (-1)
                                    int nIterations, // Number of fit iterations requested
                                    Vec pivot, // Pivot point for the starting "guess" helix
                                    Vec helixParams, // 5 helix parameters for the starting "guess" helix
                                    SquareMatrix C, // Full covariance matrix for the starting "guess" helix
                                    double B, // Magnetic field strength at helix beginning
                                    Vec t, // Magnetic field direction at helix beginning; defines the helix coordinate system
                                    FieldMap fM, boolean verbose) {

        if (direction > 0) {
            direction = 1;
        } else {
            direction = -1;
        }
        double mxResid = 9999.; // Residual cut set very large to ensure all hits are picked up

        // First insert some dummy layers where needed for propagation in the non-uniform field
        int nNew = 0;
        int last = data.size() - 1;
        double y0 = data.get(last - 2).p.X().v[1];
        double dy = (data.get(last - 1).p.X().v[1] - y0) / (double) (nNew + 1);
        // System.out.format("y0=%12.5e, dy=%12.5e\n", y0,dy);
        for (int i = 0; i < nNew; i++) {
            Plane p = new Plane(new Vec(0., y0 + (nNew - i) * dy, 0.), new Vec(0., 1., 0.));
            SiModule si = new SiModule(-1, p, 0., 999., 999., 0., fM);
            data.add(last - 1, si);
        }
        if (verbose) {
            Iterator<SiModule> itrSi = data.iterator();
            while (itrSi.hasNext()) {
                SiModule siTmp = itrSi.next();
                siTmp.p.X().print("of SiModule in list to be fit");
            }
        }

        // Create an state vector from the input seed to initialize the Kalman filter
        StateVector sI = new StateVector(-1, helixParams, C, new Vec(0., 0., 0.), B, t, pivot, verbose);

        if (verbose) {
            System.out.format("KalmanTrackFit: begin Kalman fit, start=%d, direction=%d, number iterations=%d\n", start, direction,
                                            nIterations);
            sI.print("initial state for KalmanTrackFit");
        }

        sites = new ArrayList<MeasurementSite>();
        initialSite = 0;
        chi2f = 0.;
        int prevSite = -1;
        int thisSite = -1;
        for (int idx = start; stopCondition(direction, idx, data.size()); idx = increment(direction, idx)) {
            finalSite = idx;
            SiModule m = data.get(idx);
            thisSite++;
            MeasurementSite newSite = new MeasurementSite(idx, m, mxResid);
            if (idx == start) {
                if (!newSite.makePrediction(sI)) {
                    System.out.format("KalmanTrackFit: Failed to make initial prediction at site %d, idx=%d.  Abort\n", thisSite, idx);
                    break;
                }
            } else {
                if (!newSite.makePrediction(sites.get(prevSite).aF)) {
                    System.out.format("KalmanTrackFit: Failed to make prediction at site %d, idx=%d.  Abort\n", thisSite, idx);
                    break;
                }
            }

            if (!newSite.filter()) {
                System.out.format("KalmanTrackFit: Failed to filter at site %d, idx=%d.  Ignore remaining sites\n", thisSite, idx);
                break;
            }
            ;

            if (verbose)
                newSite.print("initial filtering");
            chi2f += newSite.chi2inc;

            sites.add(newSite);

            prevSite = thisSite;
        }
        if (verbose)
            System.out.format("KalmanTrackFit: Fit chi^2 after initial filtering = %12.4e\n", chi2f);

        // Go back through the filtered sites in the reverse order to do the smoothing
        chi2s = 0.;
        MeasurementSite nextSite = null;
        SquareMatrix Facc = null;
        for (int idx = sites.size() - 1; idx >= 0; idx--) {
            MeasurementSite currentSite = sites.get(idx);
            if (currentSite.m.Layer < 0) {
                if (Facc == null) {
                    Facc = currentSite.aP.F;
                } else {
                    Facc = currentSite.aP.F.multiply(Facc); // Accumulate propagator from dummy steps.
                }
                continue;
            }
            if (nextSite == null) {
                currentSite.aS = currentSite.aF.copy();
                currentSite.smoothed = true;
            } else {
                currentSite.smooth(nextSite, Facc);
                Facc = null;
            }
            chi2s += currentSite.chi2inc;

            if (verbose)
                currentSite.print("initial smoothing");
            nextSite = currentSite;
        }
        if (verbose)
            System.out.format("KalmanTrackFit: Fit chi^2 after initial smoothing = %12.4e\n", chi2s);

        // Filter the remaining sites, if any
        if (start != 0 && start != data.size() - 1) {
            prevSite = 0;
            for (int idx = start - direction; stopCondition(-direction, idx, data.size()); idx = increment(-direction, idx)) {
                if (verbose)
                    System.out.format("KalmanTrackFit: filtering remaining sites, measurements %d\n", idx);
                SiModule m = data.get(idx);
                thisSite++;
                MeasurementSite newSite = new MeasurementSite(idx, m, mxResid);
                if (!newSite.makePrediction(sites.get(prevSite).aF)) {
                    System.out.format("KalmanTrackFit: Failed to make prediction at site %d, idx=%d.  Abort\n", thisSite, idx);
                    break;
                }

                if (!newSite.filter()) {
                    System.out.format("KalmanTrackFit: Failed to filter at site %d, idx=%d.  Ignore remaining sites\n", thisSite, idx);
                    break;
                }

                if (verbose)
                    newSite.print("filtering remainding sites");
                chi2f += newSite.chi2inc;

                sites.add(0, newSite);
            }
            if (verbose)
                System.out.format("KalmanTrackfit: Fit chi^2 after completing the filtering = %12.5e\n", chi2f);
        }
        /*
        for (int iteration = 1; iteration < nIterations; iteration++) {
            if (verbose)
                System.out.format("KalmanTrackFit: starting filtering for iteration %d\n", iteration);
            MeasurementSite startSite = sites.get(0);
            StateVector sH = null;
            if (startSite.smoothed)
                sH = startSite.aS.copy();
            else
                sH = startSite.aF.copy();
            if (verbose)
                sH.a.print("starting helix for iteration");
            sH.C.scale(10000.); // Blow up the initial covariance matrix to avoid double counting measurements
            Iterator<MeasurementSite> itr = sites.iterator();
            chi2f = 0.;
            boolean success = true;
            while (itr.hasNext()) { // Redo all the filter steps
                MeasurementSite currentSite = itr.next();
                currentSite.predicted = false;
                currentSite.filtered = false;
                currentSite.smoothed = false;
                if (!currentSite.makePrediction(sH)) {
                    System.out.format("KalmanTrackFit: In iteration %d failed to make prediction!!\n", iteration);
                    success = false;
                    break;
                }
        
                if (!currentSite.filter()) {
                    System.out.format("KalmanTrackFit: in iteration %d failed to filter!!\n", iteration);
                    success = false;
                    break;
                }
                ;
        
                if (verbose)
                    currentSite.print("iterating filtering");
                chi2f += currentSite.chi2inc;
                sH = currentSite.aF;
            }
            if (verbose)
                System.out.format("KalmanTrackFit: Iteration %d, Fit chi^2 after filtering = %12.4e\n", iteration, chi2f);
            if (success) {
                chi2s = 0.;
                nextSite = null;
                for (int idx = sites.size() - 1; idx >= 0; idx--) {
                    MeasurementSite currentSite = sites.get(idx);
                    if (nextSite == null) {
                        currentSite.aS = currentSite.aF.copy();
                        currentSite.smoothed = true;
                    } else {
                        currentSite.smooth(nextSite);
                    }
                    chi2s += currentSite.chi2inc;
        
                    if (verbose)
                        currentSite.print("iterating smoothing");
                    nextSite = currentSite;
                }
                if (verbose)
                    System.out.format("KalmanTrackFit: Iteration %d, Fit chi^2 after smoothing = %12.4e\n", iteration, chi2s);
            }
        }
        */
        if (verbose) {
            System.out.format("KalmanTrackFit: Final fit chi^2 after smoothing = %12.4e\n", chi2s);
            Vec afF = sites.get(sites.size() - 1).aF.a;
            Vec afC = sites.get(sites.size() - 1).aF.helixErrors();
            afF.print("KalmanFit helix parameters at final filtered site");
            afC.print("KalmanFit helix parameter errors");
            if (nextSite != null) {
                nextSite.aS.a.print("KalmanFit helix parameters at the final smoothed site");
                nextSite.aS.helixErrors().print("KalmanFit helix parameter errors:");
            }
        }
    }

    public StateVector fittedStateBegin() {
        return sites.get(initialSite).aS;
    }

    public StateVector fittedStateEnd() {
        return sites.get(finalSite).aS;
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

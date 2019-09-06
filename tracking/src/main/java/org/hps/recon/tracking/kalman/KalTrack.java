package org.hps.recon.tracking.kalman;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

// Track followed and fitted by the Kalman filter
public class KalTrack {
    public int ID;
    public int nHits;
    public double chi2;

    ArrayList<MeasurementSite> SiteList;
    public Map<MeasurementSite, Double> intercepts;
    public Map<MeasurementSite, Vec> interceptVects;
    public Map<MeasurementSite, Vec> interceptMomVects;
    public Map<Integer, MeasurementSite> lyrMap;
    int eventNumber;
    private Vec helixAtOrigin;
    private boolean propagated;
    private RotMatrix Rot;
    private SquareMatrix originCov;
    private Vec originPoint;
    private Vec originMomentum;
    public double alpha;
    private double[][] Cx;
    private double[][] Cp;
    public double Bmag;
    private Vec tB;

    KalTrack(int evtNumb, int tkID, int nHits, ArrayList<MeasurementSite> SiteList, double chi2) {
        // System.out.format("KalTrack constructor chi2=%10.6f\n", chi2);
        eventNumber = evtNumb;
        this.SiteList = SiteList;
        this.nHits = nHits;
        this.chi2 = chi2;
        ID = tkID;
        helixAtOrigin = null;
        propagated = false;
        originCov = new SquareMatrix(5);
        MeasurementSite site0 = SiteList.get(0);
        Vec B = KalmanInterface.getField(new Vec(0., 0., 0.), site0.m.Bfield);
        Bmag = B.mag();
        tB = B.unitVec(Bmag);
        Vec yhat = new Vec(0., 1.0, 0.);
        Vec uB = yhat.cross(tB).unitVec();
        Vec vB = tB.cross(uB);
        Rot = new RotMatrix(uB, vB, tB);
        originPoint = null;
        originMomentum = null;
        double c = 2.99793e8; // Speed of light in m/s
        alpha = 1.0e12 / (c * Bmag); // Convert from pt in GeV to curvature in mm
        Cx = null;
        Cp = null;
        intercepts = new HashMap<MeasurementSite, Double>();
        interceptVects = new HashMap<MeasurementSite, Vec>();
        interceptMomVects = new HashMap<MeasurementSite, Vec>();
        lyrMap = new HashMap<Integer, MeasurementSite>();
        // Fill the maps
        for (MeasurementSite site : SiteList) {
            double phiS = site.aS.planeIntersect(site.m.p);
            if (Double.isNaN(phiS)) {
                phiS = 0.;
            }
            interceptVects.put(site, site.aS.toGlobal(site.aS.atPhi(phiS)));
            interceptMomVects.put(site, site.aS.Rot.inverseRotate(site.aS.getMom(phiS)));
            intercepts.put(site, site.h(site.aS, site.m, phiS));
            lyrMap.put(site.m.Layer, site);
        }
    }

    // Find the change in smoothed helix angle in XY between one layer and the next
    public double scatX(int layer) {
        if (!lyrMap.containsKey(layer)) {
            return -999.;
        }
        int lyrNxt = layer + 1;
        while (lyrNxt <= 12 && !lyrMap.containsKey(lyrNxt)) {
            lyrNxt++;
        }
        if (lyrNxt > 12) {
            return -999.;
        }

        MeasurementSite s1 = lyrMap.get(layer);
        MeasurementSite s2 = lyrMap.get(lyrNxt);
        double phiS1 = s1.aS.planeIntersect(s2.m.p);
        if (Double.isNaN(phiS1)) {
            return -999.;
        }
        Vec p1 = s1.aS.getMom(phiS1);
        double t1 = Math.atan2(p1.v[0], p1.v[1]);
        double phiS2 = s2.aS.planeIntersect(s2.m.p);
        if (Double.isNaN(phiS2)) {
            return -999.;
        }        
        Vec p2 = s2.aS.getMom(phiS2);
        double t2 = Math.atan2(p2.v[0], p2.v[1]);
        return t1 - t2;
    }
    
    // Find the change in smoothed helix angle in ZY between one layer and the next
    public double scatZ(int layer) {
        if (!lyrMap.containsKey(layer)) {
            return -999.;
        }
        int lyrNxt = layer + 1;
        while (lyrNxt <= 12 && !lyrMap.containsKey(lyrNxt)) {
            lyrNxt++;
        }
        if (lyrNxt > 12) {
            return -999.;
        }
        MeasurementSite s1 = lyrMap.get(layer);
        MeasurementSite s2 = lyrMap.get(lyrNxt);
        double phiS1 = s1.aS.planeIntersect(s2.m.p);
        if (Double.isNaN(phiS1)) {
            return -999.;
        }
        Vec p1 = s1.aS.Rot.inverseRotate(s1.aS.getMom(phiS1));
        double t1 = Math.atan2(p1.v[2], p1.v[1]);
        double phiS2 = s2.aS.planeIntersect(s2.m.p);
        if (Double.isNaN(phiS2)) {
            return -999.;
        }       
        Vec p2 = s2.aS.Rot.inverseRotate(s2.aS.getMom(phiS2));
        double t2 = Math.atan2(p2.v[2], p2.v[1]);
        return t1 - t2;
    }
    
    public double chi2prime() { // Alternative calculation of the fit chi^2, considering only residuals divided by the hit sigma
        double c2 = 0.;
        for (MeasurementSite S : SiteList) {
            double phiS = S.aS.planeIntersect(S.m.p);
            if (Double.isNaN(phiS)) {
                phiS = 0.;
            }
            double vpred = S.h(S.aS, S.m, phiS);
            for (Measurement hit : S.m.hits) {
                for (KalTrack tkr : hit.tracks) {
                    if (tkr.equals(this)) {
                        c2 += Math.pow((vpred-hit.v)/hit.sigma, 2);
                    }                   
                }
            }
        }
        return c2;
    }
    
    public void print(String s) {
        System.out.format("\n KalTrack %s: ID=%d, %d hits, chi^2=%10.5f\n", s, ID, nHits, chi2);
        if (propagated) {
            System.out.format("    B-field at the origin=%10.6f,  direction=%8.6f %8.6f %8.6f\n", Bmag, tB.v[0],
                    tB.v[1], tB.v[2]);
            helixAtOrigin.print("helix for a pivot at the origin");
            originCov.print("covariance of helix parameters for a pivot at the origin");
            originPoint.print("point on the helix closest to the origin");
            SquareMatrix C1 = new SquareMatrix(3, Cx);
            C1.print("covariance matrix for the point");
            originMomentum.print("momentum of the particle at closest approach to the origin");
            SquareMatrix C2 = new SquareMatrix(3, Cp);
            C2.print("covariance matrix for the momentum");
        }
        for (int i = 0; i < SiteList.size(); i++) {
            MeasurementSite site = SiteList.get(i);
            SiModule m = site.m;
            int hitID = site.hitID;
            if (hitID < 0) {
                continue;
            }
            int idx = 2 * m.Layer;
            if (m.isStereo) {
                idx++;
            }
            System.out.format("%d Layer %d, stereo=%9.7f, chi^2 inc.=%10.6f, Xscat=%10.8f Zscat=%10.8f, hit=%d  \n", idx, m.Layer, m.stereo,
                    site.chi2inc, site.scatX(), site.scatZ(), hitID);
        }
        System.out.format("End of printing for KalTrack %s ID %d\n\n", s, ID);
    }

    // Method to make simple yz plots of the track and the residuals. Note that in the yz plot of the track the hits are
    // placed at the strip center, so they generally will not appear to be right on the track. Use gnuplot to display.
    public void plot(String path) {
        File file = new File(String.format("%s/Track%d_%d.gp", path, ID, eventNumber));
        file.getParentFile().mkdirs();
        PrintWriter pW = null;
        try {
            pW = new PrintWriter(file);
        } catch (FileNotFoundException e1) {
            System.out.println("Kaltrack.plot: Could not create the gnuplot output file for a track plot.");
            e1.printStackTrace();
            return;
        }
        pW.format("set terminal wxt size 1000, 800\n");
        pW.format("set multiplot title 'HPS Track %d in event %d' layout 2,1 columnsfirst scale 1.0,1.0\n", ID, eventNumber);
        pW.format("set label 777 'chi^2= %10.2f' at graph 0.07, 0.9 left font 'Verdana,12'\n", this.chi2);
        pW.format("set label 778 '# hits= %d' at graph 0.07, 0.7 left font 'Verdana,12'\n", this.nHits);
        pW.format("set nokey\n");
        pW.format("set xtics font 'Verdana,12'\n");
        pW.format("set ytics font 'Verdana,12'\n");
        pW.format("set title 'Track yz projection (hit placed at strip center)' font 'Verdana,12'\n");
        pW.format("set xlabel 'Y'\n");
        pW.format("set ylabel 'Z'\n");
        pW.format("set xrange[0. : 1000.]\n");
        pW.format("set yrange[-50. : 50.]\n");
        pW.format("$hits << EOD\n");
        for (MeasurementSite site : SiteList) {
            if (site.hitID >= 0) {
                Vec rDet = new Vec(0., site.m.hits.get(site.hitID).v,0.);
                Vec rGlob = site.m.toGlobal(rDet);
                pW.format(" %10.5f  %10.6f\n", rGlob.v[1], rGlob.v[2]);
            }
        }
        pW.format("EOD\n");
        pW.format("$trks << EOD\n");
        for (MeasurementSite site : SiteList) {
            Vec rHelix = site.aS.toGlobal(site.aS.atPhi(0.));
            pW.format(" %10.5f  %10.6f\n", rHelix.v[1], rHelix.v[2]);
        }
        pW.format("EOD\n");  
        pW.format("plot $hits with points ps 2, $trks with lines lw 2\n");
        
        pW.format("set xtics font 'Verdana,12'\n");
        pW.format("set ytics font 'Verdana,12'\n");
        pW.format("set title 'Track Residuals' font 'Verdana,12'\n");
        pW.format("set xlabel 'Y'\n");
        pW.format("set ylabel 'residual'\n");
        pW.format("set xrange[0. : 1000.]\n");
        pW.format("set yrange[-0.025 : 0.025]\n");
        pW.format("$resids << EOD\n");
        for (MeasurementSite site : SiteList) {
            if (site.m.Layer < 0) continue;
            double phiS = site.aS.planeIntersect(site.m.p);
            if (Double.isNaN(phiS)) {
                continue;
            }
            Vec rHelixG = site.aS.toGlobal(site.aS.atPhi(phiS));
            Vec rHelixL = site.m.toLocal(rHelixG);
            double residual =  site.m.hits.get(site.hitID).v - rHelixL.v[1];
            pW.format(" %10.5f  %10.6f    #  %10.6f\n", rHelixG.v[1], residual, site.aS.r);
        }
        pW.format("EOD\n");  
        pW.format("plot $resids with points pt 6 ps 2\n");
        pW.close();
    }
    
    
    
    // Runge Kutta propagation of the helix to the origin
    public void originHelix() {
        if (propagated) {
            return;
        }

        // Find the measurement site closest to the origin (target)
        MeasurementSite innerSite = null;
        double minY = 9999.;
        for (MeasurementSite site : SiteList) {
            SiModule m = site.m;
            if (m.p.X().v[1] < minY) {
                minY = m.p.X().v[1];
                innerSite = site;
            }
        }
        // This propagated helix will have its pivot at the origin but is in the origin
        // B-field frame
        //Vec pMom = innerSite.aS.Rot.inverseRotate(innerSite.aS.getMom(0.));
        //double ct = pMom.unitVec().dot(innerSite.m.p.T());

        // XL has to be set to zero below to get the correct result for the covariance,
        // as the Kalman filter has already accounted for scattering in the first layer.
        // However, if there was no hit in the first layer, then the scattering should
        // be introduced, TBD
        double XL = 0.; // innerSite.XL / Math.abs(ct);
        helixAtOrigin = innerSite.aS.propagateRungeKutta(innerSite.m.Bfield, originCov, XL);

        // Find the position and momentum of the particle near the origin, including
        // covariance
        Vec XonHelix = StateVector.atPhi(new Vec(0., 0., 0.), helixAtOrigin, 0., alpha);
        Vec PofHelix = StateVector.aTOp(helixAtOrigin);
        originMomentum = Rot.inverseRotate(PofHelix);
        originPoint = Rot.inverseRotate(XonHelix);
        double[][] Dx = DxTOa(helixAtOrigin);
        double[][] Dp = DpTOa(helixAtOrigin);
        Cx = new double[3][3];
        Cp = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Cx[i][j] = 0.;
                Cp[i][j] = 0.;
                for (int k = 0; k < 5; k++) {
                    for (int l = 0; l < 5; l++) {
                        Cx[i][j] += Dx[i][k] * originCov.M[k][l] * Dx[j][l];
                        Cp[i][j] += Dp[i][k] * originCov.M[k][l] * Dp[j][l];
                    }
                }
            }
        }
        SquareMatrix temp = new SquareMatrix(3, Cx);
        Cx = temp.inverseRotate(Rot).M;
        temp = new SquareMatrix(3, Cp);
        Cp = temp.inverseRotate(Rot).M;
        propagated = true;
    }

    public double[] originX() {
        if (!propagated) {
            originHelix();
        }
        return originPoint.v.clone();
    }

    public double[][] originXcov() {
        return Cx.clone();
    }

    public double[][] originPcov() {
        return Cp.clone();
    }

    public double[] originP() {
        if (!propagated) {
            originHelix();
        }
        return originMomentum.v.clone();
    }

    public double[][] originCovariance() {
        if (!propagated) {
            originHelix();
        }
        return originCov.M.clone();
    }

    public double[] originHelixParms() {
        return helixAtOrigin.v.clone();
    }

    public double helixErr(int i) {
        return Math.sqrt(originCov.M[i][i]);
    }

    public double[] rotateToGlobal(double[] x) {
        Vec xIn = new Vec(x[0], x[1], x[2]);
        return Rot.inverseRotate(xIn).v.clone();
    }

    public double[] rotateToLocal(double[] x) {
        Vec xIn = new Vec(x[0], x[1], x[2]);
        return Rot.rotate(xIn).v.clone();
    }

    // Figure out which measurement site on this track points to a given detector
    // module
    public int whichSite(SiModule module) {
        int Layer = module.Layer;
        int idx;
        if (!module.isStereo) {
            idx = 2 * Layer;
        } else {
            idx = 2 * Layer + 1;
        }
        int idm = idx;
        do {
            if (idx < SiteList.size() - 1) {
                idx++;
            } else {
                idx = 0;
            }
            if (SiteList.get(idx).m == module)
                return idx;
        } while (idx != idm);
        return -1;
    }

    public void sortSites(boolean ascending) {
        if (ascending)
            Collections.sort(SiteList, MeasurementSite.SiteComparatorUp);
        else
            Collections.sort(SiteList, MeasurementSite.SiteComparatorDn);
    }

    // re-fit the track using its existing hits
    public boolean fit(int nIterations, boolean verbose) {
        for (int iteration = 0; iteration < nIterations; iteration++) {
            if (verbose) {
                System.out.format("KalTrack.fit: starting filtering for iteration %d\n", iteration);
                // sH.a.print("starting helix for iteration");
            }
            StateVector sH = SiteList.get(0).aS;
            sH.C.scale(10000.); // Blow up the initial covariance matrix to avoid double counting measurements
            double chi2f = 0.;
            for (int idx = 0; idx < SiteList.size(); idx++) { // Redo all the filter steps
                MeasurementSite currentSite = SiteList.get(idx);
                currentSite.predicted = false;
                currentSite.filtered = false;
                currentSite.smoothed = false;
                if (currentSite.makePrediction(sH, currentSite.hitID, false, false) < 0) {
                    System.out.format("KalTrack.fit: In iteration %d failed to make prediction!!\n", iteration);
                    return false;
                }
                if (!currentSite.filter()) {
                    System.out.format("KalTrack.fit: in iteration %d failed to filter!!\n", iteration);
                    return false;
                }

                // if (verbose) currentSite.print("iterating filtering");
                chi2f += currentSite.chi2inc;
                sH = currentSite.aF;
            }
            if (verbose)
                System.out.format("KalTrack.fit: Iteration %d, Fit chi^2 after filtering = %12.4e\n", iteration, chi2f);

            double chi2s = 0.;
            MeasurementSite nextSite = null;
            for (int idx = SiteList.size() - 1; idx >= 0; idx--) {
                MeasurementSite currentSite = SiteList.get(idx);
                if (nextSite == null) {
                    currentSite.aS = currentSite.aF.copy();
                    currentSite.smoothed = true;
                } else {
                    currentSite.smooth(nextSite);
                }
                chi2s += currentSite.chi2inc;

                // if (verbose) {
                // currentSite.print("iterating smoothing");
                // }
                nextSite = currentSite;
            }
            if (verbose) {
                System.out.format("KalTrack.fit: Iteration %d, Fit chi^2 after smoothing = %12.4e\n", iteration, chi2s);
            }
        }
        return true;
    }

    // Transform helix parameters from one pivot to another
    Vec pivotTransform(Vec pivot, Vec a, Vec X0, double alpha) {
        double xC = X0.v[0] + (a.v[0] + alpha / a.v[2]) * Math.cos(a.v[1]); // Center of the helix circle
        double yC = X0.v[1] + (a.v[0] + alpha / a.v[2]) * Math.sin(a.v[1]);

        double[] aP = new double[5];
        aP[2] = a.v[2];
        aP[4] = a.v[4];
        if (a.v[2] > 0) {
            aP[1] = Math.atan2(yC - pivot.v[1], xC - pivot.v[0]);
        } else {
            aP[1] = Math.atan2(pivot.v[1] - yC, pivot.v[0] - xC);
        }
        aP[0] = (xC - pivot.v[0]) * Math.cos(aP[1]) + (yC - pivot.v[1]) * Math.sin(aP[1]) - alpha / a.v[2];
        aP[3] = X0.v[2] - pivot.v[2] + a.v[3] - (alpha / a.v[2]) * (aP[1] - a.v[1]) * a.v[4];

        return new Vec(5, aP);
    }

    // Derivative matrix for propagating the covariance of the helix parameters to a
    // covariance of momentum
    private double[][] DpTOa(Vec a) {
        double[][] M = new double[3][5];
        double K = Math.abs(a.v[2]);
        double sgn = Math.signum(a.v[2]);
        M[0][1] = -Math.cos(a.v[1]) / K;
        M[1][1] = -Math.sin(a.v[1]) / K;
        M[0][2] = sgn * Math.sin(a.v[1]) / (K * K);
        M[1][2] = -sgn * Math.sin(a.v[1]) / (K * K);
        M[2][4] = 1. / K;
        M[2][2] = -sgn * a.v[4] / (K * K);
        return M;
    }

    // Derivative matrix for propagating the covariance of the helix parameter to a
    // covariance of the point of
    // closest approach to the origin (i.e. at phi=0)
    private double[][] DxTOa(Vec a) {
        double[][] M = new double[3][5];
        M[0][0] = Math.cos(a.v[1]);
        M[0][1] = -a.v[0] * Math.sin(a.v[1]);
        M[1][0] = Math.sin(a.v[1]);
        M[1][1] = a.v[0] * Math.cos(a.v[1]);
        M[2][3] = 1.0;
        return M;
    }

    // Comparator function for sorting tracks by quality
    static Comparator<KalTrack> TkrComparator = new Comparator<KalTrack>() {
        public int compare(KalTrack t1, KalTrack t2) {
            double chi1 = t1.chi2 / t1.nHits;
            double chi2 = t2.chi2 / t2.nHits;
            if (Math.abs(chi1 - chi2) > 0.5 || t1.nHits == t2.nHits) {
                if (chi1 < chi2) {
                    return 1;
                } else {
                    return -1;
                }
            } else {
                if (t1.nHits > t2.nHits) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }
    };
}

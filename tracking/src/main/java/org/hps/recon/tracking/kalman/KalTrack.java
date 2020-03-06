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
    public Map<MeasurementSite, Vec> interceptVects;
    public Map<MeasurementSite, Vec> interceptMomVects;
    public Map<Integer, MeasurementSite> lyrMap;
    public int eventNumber;
    private Vec helixAtOrigin;
    private boolean propagated;
    private RotMatrix Rot;
    private SquareMatrix originCov;
    private Vec originPoint;
    private Vec originMomentum;
    public double alpha;
    private double[][] Cx;
    private double[][] Cp;
    double Bmag;
    private Vec tB;
    private double time;
    private boolean verbose;
    double tMin;
    double tMax;

    KalTrack(int evtNumb, int tkID, int nHits, ArrayList<MeasurementSite> SiteList, double chi2) {
        // System.out.format("KalTrack constructor chi2=%10.6f\n", chi2);
        verbose = false;
        eventNumber = evtNumb;
        // Make a new list of sites in case somebody modifies the one referred to on input
        this.SiteList = new ArrayList<MeasurementSite>(SiteList.size());
        for (MeasurementSite site : SiteList) { 
            if (site.hitID >= 0 && site.aS != null) this.SiteList.add(site); 
            if (site.hitID >=0 && site.aS == null) {
                System.out.format("KalTrack error event %d: site is missing smoothed state vector for layer %d detector %d\n", 
                        eventNumber, site.m.Layer, site.m.detector);
                site.print("bad site");
                continue;
            }
        }
        if (verbose) {
            if (this.SiteList.size() < 5) {
                System.out.format("KalTrack error in event %d: not enough hits on track %d: ",evtNumb,tkID);
                for (MeasurementSite site : SiteList) {
                    System.out.format("(%d, %d, %d) ",site.m.Layer,site.m.detector,site.hitID);
                }
                System.out.format("\n");
            }
        }
        
        Collections.sort(this.SiteList, MeasurementSite.SiteComparatorUp);
        this.nHits = nHits;
        this.chi2 = chi2;
        ID = tkID;
        if (this.SiteList.size() < 5) {
            System.out.format("KalTrack error: not enough hits ("+SiteList.size()+") on the candidate track (ID::"+ID+") for event "+eventNumber+" \n" );
            if (verbose) {
                for (MeasurementSite site : SiteList) site.print("in KalTrack input list");
            }
        }
        helixAtOrigin = null;
        propagated = false;
        originCov = new SquareMatrix(5);
        MeasurementSite site0 = this.SiteList.get(0);
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
        interceptVects = new HashMap<MeasurementSite, Vec>();
        interceptMomVects = new HashMap<MeasurementSite, Vec>();
        lyrMap = new HashMap<Integer, MeasurementSite>();
        // Fill the maps
        time = 0.;
        tMin = 9.9e9;
        tMax = -9.9e9;
        for (MeasurementSite site : this.SiteList) {
            StateVector sV = null;
            if (site.smoothed) sV = site.aS;
            else sV = site.aP;
            double phiS = sV.planeIntersect(site.m.p);
            if (Double.isNaN(phiS)) { phiS = 0.; }
            interceptVects.put(site, sV.toGlobal(sV.atPhi(phiS)));
            interceptMomVects.put(site, sV.Rot.inverseRotate(sV.getMom(phiS)));
            lyrMap.put(site.m.Layer, site);
            time += site.m.hits.get(site.hitID).time;
            tMin = Math.min(tMin, site.m.hits.get(site.hitID).time);
            tMax = Math.max(tMax,  site.m.hits.get(site.hitID).time);
        }
        time = time/(double)SiteList.size();
    }

    public double getTime() {
        return time;
    }
    
    // Find the change in smoothed helix angle in XY between one layer and the next
    public double scatX(int layer) {
        if (!lyrMap.containsKey(layer)) return -999.;
        int lyrNxt = layer + 1;
        while (lyrNxt <= 13 && !lyrMap.containsKey(lyrNxt)) lyrNxt++;
        if (lyrNxt > 13) return -999.; 

        MeasurementSite s1 = lyrMap.get(layer);
        MeasurementSite s2 = lyrMap.get(lyrNxt);
        if (s1.aS == null || s2.aS == null) return -999.;
        double phiS1 = s1.aS.planeIntersect(s2.m.p);
        if (Double.isNaN(phiS1)) return -999.;
        Vec p1 = s1.aS.getMom(phiS1);
        double t1 = Math.atan2(p1.v[0], p1.v[1]);
        double phiS2 = s2.aS.planeIntersect(s2.m.p);
        if (Double.isNaN(phiS2)) return -999.;
        Vec p2 = s2.aS.getMom(phiS2);
        double t2 = Math.atan2(p2.v[0], p2.v[1]);
        return t1 - t2;
    }

    // Find the change in smoothed helix angle in ZY between one layer and the next
    public double scatZ(int layer) {
        if (!lyrMap.containsKey(layer)) return -999.;
        int lyrNxt = layer + 1;
        while (lyrNxt <= 13 && !lyrMap.containsKey(lyrNxt)) lyrNxt++;
        if (lyrNxt > 13) return -999.;
        MeasurementSite s1 = lyrMap.get(layer);
        MeasurementSite s2 = lyrMap.get(lyrNxt);
        if (s1.aS == null || s2.aS == null) return -999.;
        double phiS1 = s1.aS.planeIntersect(s2.m.p);
        if (Double.isNaN(phiS1)) return -999.;
        Vec p1 = s1.aS.Rot.inverseRotate(s1.aS.getMom(phiS1));
        double t1 = Math.atan2(p1.v[2], p1.v[1]);
        double phiS2 = s2.aS.planeIntersect(s2.m.p);
        if (Double.isNaN(phiS2)) return -999.;
        Vec p2 = s2.aS.Rot.inverseRotate(s2.aS.getMom(phiS2));
        double t2 = Math.atan2(p2.v[2], p2.v[1]);
        return t1 - t2;
    }

    public double chi2prime() { // Alternative calculation of the fit chi^2, considering only residuals divided by the hit sigma
        double c2 = 0.;
        for (MeasurementSite S : SiteList) {
            if (S.aS == null) continue;
            double phiS = S.aS.planeIntersect(S.m.p);
            if (Double.isNaN(phiS)) { phiS = 0.; }
            double vpred = S.h(S.aS, S.m, phiS);
            for (Measurement hit : S.m.hits) {
                for (KalTrack tkr : hit.tracks) { 
                    if (tkr.equals(this)) c2 += Math.pow((vpred - hit.v) / hit.sigma, 2);
                }
            }
        }
        return c2;
    }

    public void print(String s) {
        System.out.format("\n KalTrack %s: Event %d, ID=%d, %d hits, chi^2=%10.5f, t=%5.1f from %5.1f to %5.1f\n", s, eventNumber, ID, nHits, chi2, time, tMin, tMax);
        if (propagated) {
            System.out.format("    B-field at the origin=%10.6f,  direction=%8.6f %8.6f %8.6f\n", Bmag, tB.v[0], tB.v[1], tB.v[2]);
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
            System.out.format("Layer %d, detector %d, stereo=%b, chi^2 inc.=%10.6f, Xscat=%10.8f Zscat=%10.8f, arc=%10.5f, hit=%d  ", m.Layer, m.detector, m.isStereo,
                    site.chi2inc, site.scatX(), site.scatZ(), site.arcLength, hitID);
            if (hitID < 0) {
                System.out.format("\n");
                continue;
            }
            System.out.format(", t=%5.1f", site.m.hits.get(site.hitID).time);
            if (m.hits.get(hitID).tksMC != null) {
                System.out.format("  MC tracks: ");
                for (int iMC : m.hits.get(hitID).tksMC) {
                    System.out.format(" %d ", iMC);
                }
                System.out.format("\n");
            }
            if (interceptVects.containsKey(site)) {
                Vec interceptVec = interceptVects.get(site);
                Vec interceptMomVec = interceptMomVects.get(site);
                System.out.format("    Intercept=%s, p=%s, measurement=%10.5f, predicted=%10.5f, error=%9.5f \n", interceptVec.string(),
                        interceptMomVec.string(), site.m.hits.get(hitID).v, site.aS.mPred, Math.sqrt(site.aS.R));
            }
        }
        System.out.format("End of printing for KalTrack %s ID %d in event %d\n\n", s, ID, eventNumber);
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
                Vec rDet = new Vec(0., site.m.hits.get(site.hitID).v, 0.);
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
            if (Double.isNaN(phiS)) { continue; }
            Vec rHelixG = site.aS.toGlobal(site.aS.atPhi(phiS));
            Vec rHelixL = site.m.toLocal(rHelixG);
            double residual = site.m.hits.get(site.hitID).v - rHelixL.v[1];
            pW.format(" %10.5f  %10.6f    #  %10.6f\n", rHelixG.v[1], residual, site.aS.r);
        }
        pW.format("EOD\n");
        pW.format("plot $resids with points pt 6 ps 2\n");
        pW.close();
    }

    // Runge Kutta propagation of the helix to the origin
    public boolean originHelix() {
        if (propagated) { return true; }

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
        if (innerSite == null) {
            System.out.format("KalTrack.originHelix: event %d inner site not found.\n", eventNumber);
            return false;
        }
        if (innerSite.aS == null) {
            System.out.format("KalTrack.originHelix: event %d inner site is not smoothed.\n", eventNumber);
            return false;
        }
        // This propagated helix will have its pivot at the origin but is in the origin B-field frame
        //Vec pMom = innerSite.aS.Rot.inverseRotate(innerSite.aS.getMom(0.));
        //double ct = pMom.unitVec().dot(innerSite.m.p.T());

        // XL has to be set to zero below to get the correct result for the covariance,
        // as the Kalman filter has already accounted for scattering in the first layer.
        // However, if there was no hit in the first layer, then the scattering should
        // be introduced, TBD
        double XL = 0.; // innerSite.XL / Math.abs(ct);
        helixAtOrigin = innerSite.aS.propagateRungeKutta(innerSite.m.Bfield, originCov, XL);
        if (Double.isNaN(originCov.M[0][0])) return false;
        SquareMatrix Cinv = originCov.invert();
        for (int i=0; i<5; ++i) {
            if (Cinv.M[i][i] == 0.0) {  // The covariance matrix was singular
                System.out.format("KalTrack.originHelix: the track %d covariance matrix is singular!\n", ID);
                originCov.print("singular");
                helixAtOrigin.print("singular");
                for (int k=0; k<5; ++k) {
                    for (int m=0; m<k; ++m) {
                        originCov.M[k][m] = 0.;
                        originCov.M[m][k] = 0.;
                    }
                }
                break;
            }
        }

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
        return true;
    }

    public double[] originX() {
        if (!propagated) { originHelix(); }
        return originPoint.v.clone();
    }

    public double[][] originXcov() {
        return Cx.clone();
    }

    public double[][] originPcov() {
        return Cp.clone();
    }

    public double[] originP() {
        if (!propagated) { originHelix(); }
        return originMomentum.v.clone();
    }

    public double[][] originCovariance() {
        if (!propagated) { originHelix(); }
        return originCov.M.clone();
    }

    public boolean covNaN() { 
        if (!propagated) {originHelix();}
        return originCov.isNaN();
    }
    
    public double[] originHelixParms() {
        if (propagated) return helixAtOrigin.v.clone();
        else return null;
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

    // Figure out which measurement site on this track points to a given detector module
    public int whichSite(SiModule module) {
        for (MeasurementSite site : SiteList) {
            if (site.m == module) {
                return SiteList.indexOf(site);
            }
        }
        return -1;
    }

    public void sortSites(boolean ascending) {
        if (ascending) Collections.sort(SiteList, MeasurementSite.SiteComparatorUp);
        else Collections.sort(SiteList, MeasurementSite.SiteComparatorDn);
    }

    // Try to add missing hits to the track
    public int addHits(ArrayList<SiModule> data, double mxResid, double mxChi2inc, double mxTdif, boolean verbose) {
        int numAdded  = 0;
        int numLayers = 14;
        if (nHits == numLayers) return numAdded;
        if (verbose) System.out.format("KalTrack.addHits: trying to add hits to track %d\n", ID);
        
        sortSites(true);

        if (verbose) {
            System.out.format("KalTrac.addHits: initial list of sites: ");
            for (MeasurementSite site : SiteList) {
                System.out.format("(%d, %d, %d) ",site.m.Layer, site.m.detector, site.hitID);
            }
            System.out.format("\n");
        }
        
        ArrayList<ArrayList<SiModule>> moduleList = new ArrayList<ArrayList<SiModule>>(numLayers);
        for (int lyr = 0; lyr < numLayers; lyr++) {
            ArrayList<SiModule> modules = new ArrayList<SiModule>();
            moduleList.add(modules);
        }
        for (SiModule thisSi : data) {
            if (thisSi.hits.size() > 0) { moduleList.get(thisSi.Layer).add(thisSi); }
        }
        
        ArrayList<MeasurementSite> newSites = new ArrayList<MeasurementSite>();
        for (int idx = 0; idx < SiteList.size()-1; idx++) { 
            MeasurementSite site = SiteList.get(idx); 
            if (site.hitID < 0) continue;
            int nxtIdx = -1;
            for (int jdx = idx+1; jdx < SiteList.size(); jdx++) {
                if (SiteList.get(jdx).hitID >= 0) {
                    nxtIdx = jdx;
                    break;
                }
            }
            if (nxtIdx < 0) continue;
            MeasurementSite nxtSite = SiteList.get(nxtIdx);
            for (int lyr=site.m.Layer+1; lyr<nxtSite.m.Layer; ++lyr) {
                if (verbose) System.out.format("KalTrack.addHits: looking for hits on layer %d\n", lyr);
                for (SiModule module : moduleList.get(lyr)) {
                    MeasurementSite newSite = new MeasurementSite(lyr, module, mxResid, 0.);
                    double [] tRange = {tMax - mxTdif, tMin + mxTdif}; 
                    int rF = newSite.makePrediction(site.aF, site.m, -1, false, true, false, tRange, verbose);
                    if (rF == 1) {
                        if (verbose) System.out.format("KalTrack.addHits: predicted chi2inc=%8.3f\n",newSite.chi2inc);
                        if (newSite.chi2inc < mxChi2inc) {
                            if (newSite.filter(verbose)) {
                                if (verbose) System.out.format("KalTrack.addHits: event %d track %d filtered chi2inc=%8.3f\n",eventNumber,ID,newSite.chi2inc);
                                if (newSite.chi2inc < mxChi2inc) {
                                    if (verbose) System.out.format("KalTrack.addHits: event %d added hit with chi2inc<%8.3f to layer %d\n",eventNumber,newSite.chi2inc,module.Layer);
                                    newSites.add(newSite);
                                    numAdded++;
                                    site = newSite;
                                    double hitTime = newSite.m.hits.get(newSite.hitID).time;
                                    if (hitTime > tMax) tMax = hitTime;
                                    else if (hitTime < tMin) tMin = hitTime;
                                    break;
                                }
                            }
                        }
                    }
                }              
            }
        }
        if (numAdded > 0) {
            for (MeasurementSite site : newSites) {
                if (verbose) System.out.format("KalTrack.addHits event %d: added hit %d on layer %d detector %d\n", eventNumber, site.hitID, site.m.Layer, site.m.detector);
                SiteList.add(site);
            }
            sortSites(true);
            if (verbose) {
                System.out.format("KalTrack.addHits: final list of sites: ");
                for (MeasurementSite site : SiteList) {
                    System.out.format("(%d, %d, %d) ",site.m.Layer, site.m.detector, site.hitID);
                }
                System.out.format("\n");
            }
        } else if (verbose) {
            System.out.format("KalTrack.addHits: no hits added in event %d to track %d\n", eventNumber, ID);
        }

        return numAdded;
    }
        
    // re-fit the track 
    public boolean fit(int nIterations, boolean verbose) {
        double chi2s = 0.;
        for (int iteration = 0; iteration < nIterations; iteration++) {
            if (verbose) {
                System.out.format("KalTrack.fit: starting filtering for iteration %d\n", iteration);
                // sH.a.print("starting helix for iteration");
            }
            StateVector sH = SiteList.get(0).aS;
            sH.C.scale(1000.*chi2); // Blow up the initial covariance matrix to avoid double counting measurements
            SiModule prevMod = null;
            double chi2f = 0.;
            for (int idx = 0; idx < SiteList.size(); idx++) { // Redo all the filter steps
                MeasurementSite currentSite = SiteList.get(idx);
                currentSite.predicted = false;
                currentSite.filtered = false;
                currentSite.smoothed = false;
                currentSite.chi2inc = 0.;
                currentSite.aP = null;
                currentSite.aF = null;
                currentSite.aS = null;
                
                boolean allowSharing = false;
                boolean pickupHits = false;
                boolean checkBounds = false;
                double [] tRange = {-999., 999.};
                if (currentSite.makePrediction(sH, prevMod, currentSite.hitID, allowSharing, pickupHits, checkBounds, tRange, verbose) < 0) {
                    if (verbose) System.out.format("KalTrack.fit: event %d, track %d in iteration %d failed to make prediction!!\n", eventNumber, ID, iteration);
                    return false;
                }
                if (!currentSite.filter(verbose)) {
                    if (verbose) System.out.format("KalTrack.fit: event %d, track %d in iteration %d failed to filter!!\n", eventNumber, ID, iteration);
                    return false;
                }

                // if (verbose) currentSite.print("iterating filtering");
                chi2f += Math.max(currentSite.chi2inc,0.);

                sH = currentSite.aF;
                prevMod = currentSite.m;
            }
            if (verbose) { System.out.format("KalTrack.fit: Iteration %d, Fit chi^2 after filtering = %12.4e\n", iteration, chi2f); }
            
            chi2s = 0.;
            MeasurementSite nextSite = null;
            for (int idx = SiteList.size() - 1; idx >= 0; idx--) {
                MeasurementSite currentSite = SiteList.get(idx);
                if (nextSite == null) {
                    currentSite.aS = currentSite.aF.copy();
                    currentSite.smoothed = true;
                } else {
                    currentSite.smooth(nextSite, verbose);
                }
                chi2s += Math.max(currentSite.chi2inc,0.);

                // if (verbose) {
                // currentSite.print("iterating smoothing");
                // }
                nextSite = currentSite;
            }
            if (verbose) { System.out.format("KalTrack.fit: Iteration %d, Fit chi^2 after smoothing = %12.4e\n", iteration, chi2s); }
        }
        this.chi2 = chi2s;
        propagated = false;
        return true;
    }

    // Derivative matrix for propagating the covariance of the helix parameters to a covariance of momentum
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
            double chi1 = t1.chi2 / t1.nHits + 10.0*(1.0 - (double)t1.nHits/12.);
            double chi2 = t2.chi2 / t2.nHits + 10.0*(1.0 - (double)t2.nHits/12.);
            if (chi1 < chi2) {
                return -1;
            } else {
                return +1;
            }
        }
    };

    @Override
    public boolean equals(Object other) { // Consider two tracks to be equal if they have the same hits
        if (this == other) return true;
        if (!(other instanceof KalTrack)) return false;
        KalTrack o = (KalTrack) other;

        if (this.nHits != o.nHits) return false;
        if (this.SiteList.size() != o.SiteList.size()) return false;

        for (int i = 0; i < SiteList.size(); ++i) {
            MeasurementSite s1 = this.SiteList.get(i);
            MeasurementSite s2 = o.SiteList.get(i);
            if (s1.m.Layer != s2.m.Layer) return false;
            if (s1.m.detector != s2.m.detector) return false;
            if (s1.hitID != s2.hitID) return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return nHits + 100 * ID;
    }
}

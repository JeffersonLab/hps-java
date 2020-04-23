package org.hps.recon.tracking.kalman;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.util.Pair;

// Track followed and fitted by the Kalman filter
public class KalTrack {
    public int ID;
    public int nHits;
    public double chi2;

    ArrayList<MeasurementSite> SiteList;
    // call the corresponding functions to create and access the following two maps
    private Map<MeasurementSite, Vec> interceptVects;    
    private Map<MeasurementSite, Vec> interceptMomVects;
    Map<Integer, MeasurementSite> millipedeMap;
    Map<Integer, MeasurementSite> lyrMap;
    public int eventNumber;
    private Vec helixAtOrigin;
    private boolean propagated;
    private RotMatrix Rot;
    private SquareMatrix originCov;
    private Vec originPoint;
    private Vec originMomentum;
    private ArrayList<Double> yScat;
    public double alpha;
    private double[][] Cx;
    private double[][] Cp;
    double Bmag;
    private Vec tB;
    private double time;
    double tMin;
    double tMax;
    private Logger logger;
    private KalmanParams kPar;

    KalTrack(int evtNumb, int tkID, ArrayList<MeasurementSite> SiteList, ArrayList<Double> yScat, KalmanParams kPar) {
        // System.out.format("KalTrack constructor chi2=%10.6f\n", chi2);
        eventNumber = evtNumb;
        this.yScat = yScat;
        logger = Logger.getLogger(KalTrack.class.getName());
        this.kPar = kPar;
        ID = tkID;
        
        // Trim empty sites from the track ends
        Collections.sort(SiteList, MeasurementSite.SiteComparatorUp);
        int firstSite = -1;
        for (int idx=0; idx<SiteList.size(); ++idx) {
            firstSite = idx;
            if (SiteList.get(idx).hitID >= 0) break;
        }
        int lastSite = 999;
        for (int idx = SiteList.size()-1; idx >= 0; --idx) {
            lastSite = idx;
            if (SiteList.get(idx).hitID >= 0) break;
        }
        
        // Make a new list of sites, without empty sites at beginning or end
        this.SiteList = new ArrayList<MeasurementSite>(SiteList.size());
        for (int idx=firstSite; idx<=lastSite; ++idx) { 
            MeasurementSite site = SiteList.get(idx);
            if (site.aS == null) {
                logger.log(Level.WARNING, String.format("Event %d: site is missing smoothed state vector for layer %d detector %d", 
                        eventNumber, site.m.Layer, site.m.detector));
                logger.log(Level.WARNING, site.toString("bad site"));
                continue;
            }
            this.SiteList.add(site); 
        }
        
        helixAtOrigin = null;
        propagated = false;
        originCov = new SquareMatrix(5);
        MeasurementSite site0 = this.SiteList.get(0);
        Vec B = KalmanInterface.getField(new Vec(3,kPar.beamSpot), site0.m.Bfield);
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
        // Fill the maps
        time = 0.;
        tMin = 9.9e9;
        tMax = -9.9e9;
        this.chi2 = 0.;
        this.nHits = 0;
        for (MeasurementSite site : this.SiteList) {
            if (site.hitID < 0) continue;
            nHits++;
            time += site.m.hits.get(site.hitID).time;
            tMin = Math.min(tMin, site.m.hits.get(site.hitID).time);
            tMax = Math.max(tMax,  site.m.hits.get(site.hitID).time);
            this.chi2 += site.chi2inc;
        }
        time = time/(double)nHits; 
        lyrMap = null;
        millipedeMap = null;
        interceptVects = null;
        interceptMomVects = null;
        if (nHits < 5) {
            logger.log(Level.WARNING, "KalTrack error: not enough hits ("+nHits+") on the candidate track (ID::"+ID+") for event "+eventNumber);
            for (MeasurementSite site : SiteList) logger.log(Level.FINE, site.toString("in KalTrack input list"));
            logger.log(Level.FINE, String.format("KalTrack error in event %d: not enough hits on track %d: ",evtNumb,tkID));
            String str="";
            for (MeasurementSite site : SiteList) {
                str = str + String.format("(%d, %d, %d) ",site.m.Layer,site.m.detector,site.hitID);
            }
            str = str + "\n";
            logger.log(Level.FINER,str);
        }
    }

    public double getTime() {
        return time;
    }
    
    public Map<MeasurementSite, Vec> interceptVects() {
        if (interceptVects == null) {
            interceptVects = new HashMap<MeasurementSite, Vec>(nHits);
            for (MeasurementSite site : this.SiteList) {
                StateVector sV = null;
                if (site.smoothed) sV = site.aS;
                else sV = site.aP;
                double phiS = sV.helix.planeIntersect(site.m.p);
                if (Double.isNaN(phiS)) phiS = 0.;
                interceptVects.put(site, sV.helix.toGlobal(sV.helix.atPhi(phiS)));                
            }
        }
        return interceptVects;
    }
    
    public Map<MeasurementSite, Vec> interceptMomVects() {
        if (interceptMomVects == null) {
            interceptMomVects = new HashMap<MeasurementSite, Vec>();
            for (MeasurementSite site : this.SiteList) {
                StateVector sV = null;
                if (site.smoothed) sV = site.aS;
                else sV = site.aP;
                double phiS = sV.helix.planeIntersect(site.m.p);
                if (Double.isNaN(phiS)) phiS = 0.;
                interceptMomVects.put(site, sV.helix.Rot.inverseRotate(sV.helix.getMom(phiS)));
            }
        }
        return interceptMomVects;
    }
    
    private void makeLyrMap() {
        lyrMap = new HashMap<Integer, MeasurementSite>(nHits);
        for (MeasurementSite site : SiteList) {
            lyrMap.put(site.m.Layer, site);
        }
    }
    
    private void makeMillipedeMap() {
        millipedeMap = new HashMap<Integer, MeasurementSite>(nHits);
        for (MeasurementSite site : SiteList) {
            millipedeMap.put(site.m.millipedeID, site);
        }
    }
    
    // Find the change in smoothed helix angle in XY between one layer and the next
    public double scatX(int layer) {
        if (lyrMap == null) makeLyrMap();
        if (!lyrMap.containsKey(layer)) return -999.;
        int lyrNxt = layer + 1;
        while (lyrNxt <= 13 && !lyrMap.containsKey(lyrNxt)) lyrNxt++;
        if (lyrNxt > 13) return -999.; 

        MeasurementSite s1 = lyrMap.get(layer);
        MeasurementSite s2 = lyrMap.get(lyrNxt);
        if (s1.aS == null || s2.aS == null) return -999.;
        double phiS1 = s1.aS.helix.planeIntersect(s2.m.p);
        if (Double.isNaN(phiS1)) return -999.;
        Vec p1 = s1.aS.helix.getMom(phiS1);
        double t1 = Math.atan2(p1.v[0], p1.v[1]);
        double phiS2 = s2.aS.helix.planeIntersect(s2.m.p);
        if (Double.isNaN(phiS2)) return -999.;
        Vec p2 = s2.aS.helix.getMom(phiS2);
        double t2 = Math.atan2(p2.v[0], p2.v[1]);
        return t1 - t2;
    }

    // Find the change in smoothed helix angle in ZY between one layer and the next
    public double scatZ(int layer) {
        if (lyrMap == null) makeLyrMap();
        if (!lyrMap.containsKey(layer)) return -999.;
        int lyrNxt = layer + 1;
        while (lyrNxt <= 13 && !lyrMap.containsKey(lyrNxt)) lyrNxt++;
        if (lyrNxt > 13) return -999.;
        MeasurementSite s1 = lyrMap.get(layer);
        MeasurementSite s2 = lyrMap.get(lyrNxt);
        if (s1.aS == null || s2.aS == null) return -999.;
        double phiS1 = s1.aS.helix.planeIntersect(s2.m.p);
        if (Double.isNaN(phiS1)) return -999.;
        Vec p1 = s1.aS.helix.Rot.inverseRotate(s1.aS.helix.getMom(phiS1));
        double t1 = Math.atan2(p1.v[2], p1.v[1]);
        double phiS2 = s2.aS.helix.planeIntersect(s2.m.p);
        if (Double.isNaN(phiS2)) return -999.;
        Vec p2 = s2.aS.helix.Rot.inverseRotate(s2.aS.helix.getMom(phiS2));
        double t2 = Math.atan2(p2.v[2], p2.v[1]);
        return t1 - t2;
    }

    public double chi2prime() { // Alternative calculation of the fit chi^2, considering only residuals divided by the hit sigma
        double c2 = 0.;
        for (MeasurementSite S : SiteList) {
            if (S.aS == null) continue;
            double phiS = S.aS.helix.planeIntersect(S.m.p);
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

    public Pair<Double,Double> unbiasedResidualMillipede(int millipedeID) {
        if (millipedeMap == null) makeMillipedeMap();
        if (millipedeMap.containsKey(millipedeID)) {
            return unbiasedResidual(millipedeMap.get(millipedeID));
        } else {       
            return new Pair<Double,Double>(-999., -999.);
        }
    }
    
    public Pair<Double,Double> unbiasedResidual(int layer) {
        if (lyrMap == null) makeLyrMap();
        if (lyrMap.containsKey(layer)) {
            return unbiasedResidual(lyrMap.get(layer));
        } else {       
            return new Pair<Double,Double>(-999., -999.);
        }
    }   
    
    // Returns the unbiased residual for the track at a given layer, together with the variance on that residual
    public Pair<Double,Double> unbiasedResidual(MeasurementSite site) {
        double resid = -999.;
        double varResid = -999.;               
        Vec aStar = null;  
        if (site.hitID >= 0) {
            double sigma = site.m.hits.get(site.hitID).sigma;
            SquareMatrix Cstar = new SquareMatrix(5);
            aStar = site.aS.inverseFilter(site.H, sigma*sigma, Cstar);
            HelixPlaneIntersect hpi = new HelixPlaneIntersect();
            Plane pTrans = site.m.p.toLocal(site.aS.helix.Rot, site.aS.helix.origin);
            double phiInt = hpi.planeIntersect(aStar, site.aS.helix.X0, site.aS.helix.alpha, pTrans);
            if (!Double.isNaN(phiInt)) {
                Vec intPnt = HelixState.atPhi(site.aS.helix.X0, aStar, phiInt, site.aS.helix.alpha);
                Vec globalInt = site.aS.helix.toGlobal(intPnt);
                Vec localInt = site.m.toLocal(globalInt);
                resid = site.m.hits.get(site.hitID).v - localInt.v[1];
                varResid = sigma*sigma + site.H.dot(site.H.leftMultiply(Cstar));
            }
        }        
        return new Pair<Double,Double>(resid, varResid);
    }

    public Pair<Double,Double> biasedResidual(int layer) {
        if (lyrMap == null) makeLyrMap();
        if (lyrMap.containsKey(layer)) {
            return biasedResidual(lyrMap.get(layer));
        } else {       
            return new Pair<Double,Double>(-999., -999.);
        }
    }
    
    public Pair<Double,Double> biasedResidualMillipede(int millipedeID) {
        if (millipedeMap == null) makeMillipedeMap();
        if (millipedeMap.containsKey(millipedeID)) {
            return biasedResidual(millipedeMap.get(millipedeID));
        } else {       
            return new Pair<Double,Double>(-999., -999.);
        }
    }
    
    public Pair<Double,Double> biasedResidual(MeasurementSite site) {
        double resid = -999.;
        double varResid = -999.;               
        if (site.aS != null) {
            resid = site.aS.r;
            varResid = site.aS.R;
        }
        return new Pair<Double,Double>(resid, varResid);
    }
    
    public void print(String s) {
        System.out.format("%s", this.toString(s));    
    }
    
    String toString(String s) {
        String str = String.format("\n KalTrack %s: Event %d, ID=%d, %d hits, chi^2=%10.5f, t=%5.1f from %5.1f to %5.1f\n", s, eventNumber, ID, nHits, chi2, time, tMin, tMax);
        if (propagated) {
            str=str+String.format("    B-field at the origin=%10.6f,  direction=%8.6f %8.6f %8.6f\n", Bmag, tB.v[0], tB.v[1], tB.v[2]);
            str=str+helixAtOrigin.toString("helix for a pivot at the origin")+"\n";
            str=str+originCov.toString("covariance of helix parameters for a pivot at the origin");
            str=str+originPoint.toString("point on the helix closest to the origin")+"\n";
            SquareMatrix C1 = new SquareMatrix(3, Cx);
            str=str+C1.toString("covariance matrix for the point");
            str=str+originMomentum.toString("momentum of the particle at closest approach to the origin");
            SquareMatrix C2 = new SquareMatrix(3, Cp);
            str=str+C2.toString("covariance matrix for the momentum");
        }
        for (int i = 0; i < SiteList.size(); i++) {
            MeasurementSite site = SiteList.get(i);
            SiModule m = site.m;
            int hitID = site.hitID;
            str=str+String.format("Layer %d, detector %d, stereo=%b, chi^2 inc.=%10.6f, Xscat=%10.8f Zscat=%10.8f, arc=%10.5f, hit=%d  ", m.Layer, m.detector, m.isStereo,
                    site.chi2inc, site.scatX(), site.scatZ(), site.arcLength, hitID);
            if (hitID < 0) {
                str=str+"\n";
                continue;
            }
            str=str+String.format(", t=%5.1f", site.m.hits.get(site.hitID).time);
            if (m.hits.get(hitID).tksMC != null) {
                str=str+String.format("  MC tracks: ");
                for (int iMC : m.hits.get(hitID).tksMC) {
                    str=str+String.format(" %d ", iMC);
                }
                str=str+"\n";
            }
            if (interceptVects().containsKey(site)) {
                Vec interceptVec = interceptVects().get(site);
                Vec interceptMomVec = interceptMomVects().get(site);
                str=str+String.format("    Intercept=%s, p=%s, measurement=%10.5f, predicted=%10.5f, error=%9.5f \n", interceptVec.toString(),
                        interceptMomVec.toString(), site.m.hits.get(hitID).v, site.aS.mPred, Math.sqrt(site.aS.R));
            }
        }
        str=str+String.format("End of printing for KalTrack %s ID %d in event %d\n\n", s, ID, eventNumber);
        return str;
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
            Vec rHelix = site.aS.helix.toGlobal(site.aS.helix.atPhi(0.));
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
            double phiS = site.aS.helix.planeIntersect(site.m.p);
            if (Double.isNaN(phiS)) { continue; }
            Vec rHelixG = site.aS.helix.toGlobal(site.aS.helix.atPhi(phiS));
            Vec rHelixL = site.m.toLocal(rHelixG);
            double residual = -999.;
            if (site.hitID >= 0) residual = site.m.hits.get(site.hitID).v - rHelixL.v[1];
            pW.format(" %10.5f  %10.6f    #  %10.6f\n", rHelixG.v[1], residual, site.aS.r);
        }
        pW.format("EOD\n");
        pW.format("plot $resids with points pt 6 ps 2\n");
        pW.close();
    }

    // Runge Kutta propagation of the helix to the origin
    public boolean originHelix() {
        if (propagated) return true;

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
            logger.log(Level.WARNING, String.format("KalTrack.originHelix: event %d inner site not found.\n", eventNumber));
            return false;
        }
        if (innerSite.aS == null) {
            logger.log(Level.WARNING, String.format("KalTrack.originHelix: event %d inner site is not smoothed.\n", eventNumber));
            return false;
        }
        Vec beamSpot = new Vec(3, kPar.beamSpot);
        
        // This propagated helix will have its pivot at the origin but is in the origin B-field frame
        // The StateVector method propagateRungeKutta transforms the origin plane into the origin B-field frame
        double XL = innerSite.m.thickness/innerSite.radLen;
        Plane originPlane = new Plane(beamSpot, new Vec(0., 1., 0.)); 
        RotMatrix originRot = new RotMatrix();
        helixAtOrigin = innerSite.aS.helix.propagateRungeKutta(originPlane, innerSite.m.Bfield, originCov, yScat, originRot, XL);
        if (Double.isNaN(originCov.M[0][0])) return false;
        SquareMatrix Cinv = originCov.invert();
        for (int i=0; i<5; ++i) {
            if (Cinv.M[i][i] == 0.0) {  // The covariance matrix was singular
                logger.log(Level.WARNING, String.format("KalTrack.originHelix: the track %d covariance matrix is singular!\n", ID));
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

        Vec beamSpotBframe = Rot.rotate(beamSpot);
        //System.out.format("KalTrack: beamspot= %f %f %f global, %f %f %f local field\n", beamSpot.v[0], beamSpot.v[1], beamSpot.v[2],
        //        beamSpotBframe.v[0], beamSpotBframe.v[1], beamSpotBframe.v[2]);
        // Find the position and momentum of the particle near the origin, including covariance
        Vec XonHelix = HelixState.atPhi(beamSpotBframe, helixAtOrigin, 0., alpha);
        Vec PofHelix = HelixState.aTOp(helixAtOrigin);
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
        if (lyrMap != null) {
            return SiteList.indexOf(lyrMap.get(module.Layer));
        }
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

    public boolean removeHit(MeasurementSite site, double mxChi2Inc, double mxTdif) {
        boolean exchange = false;
        logger.log(Level.FINE, String.format("Event %d track %d remove hit %d on layer %d detector %d", 
                eventNumber, ID, site.hitID, site.m.Layer, site.m.detector));
        if (site.hitID < 0) {
            logger.log(Level.WARNING, String.format("Event %d track %d, trying to remove nonexistent hit on layer %d detector %d", 
                    eventNumber, ID, site.m.Layer, site.m.detector));
            return exchange;
        }
        if (site.m.hits.get(site.hitID).tracks.contains(this)) {
            site.m.hits.get(site.hitID).tracks.remove(this);
        } else {
            logger.log(Level.WARNING, String.format("track %d is missing on hit %d track list in layer %d detector %d", 
                    ID, site.hitID, site.m.Layer, site.m.detector));
        }
        chi2 -= site.chi2inc;
        nHits--;
        int oldID = site.hitID;
        site.removeHit();
        // Check whether there might be another hit available                                   
        Measurement addedHit = site.addHit(this, mxChi2Inc, mxTdif, oldID);
        if (addedHit != null) {
            addedHit.tracks.add(this);
            Measurement newHit = site.m.hits.get(site.hitID);
            tMin = Math.min(tMin, newHit.time);
            tMax = Math.max(tMax, newHit.time);
            exchange = true;
            nHits++;
            logger.log(Level.FINE, String.format("Event %d track %d added hit %d on layer %d detector %d", 
                    eventNumber, ID, site.hitID, site.m.Layer, site.m.detector));
        } else {
            SiteList.remove(site);
        }
        return exchange;
    }
    
    // Try to add missing hits to the track
    public int addHits(ArrayList<SiModule> data, double mxResid, double mxChi2inc, double mxTdif, boolean verbose) {
        int numAdded  = 0;
        int numLayers = 14;
        if (nHits == numLayers) return numAdded;
        //Level lvl = logger.getLevel();
        //logger.setLevel(Level.FINER);
        logger.log(Level.FINER, String.format("addHits: trying to add hits to track %d", ID));
        
        sortSites(true);

        if (logger.getLevel()==Level.FINER) {
            String str = String.format("KalTrac.addHits: initial list of sites: ");
            for (MeasurementSite site : SiteList) {
                str = str + String.format("(%d, %d, %d) ",site.m.Layer, site.m.detector, site.hitID);
            }
            logger.log(Level.FINER, str);
        }
        
        ArrayList<ArrayList<SiModule>> moduleList = new ArrayList<ArrayList<SiModule>>(numLayers);
        for (int lyr = 0; lyr < numLayers; lyr++) {
            ArrayList<SiModule> modules = new ArrayList<SiModule>();
            moduleList.add(modules);
        }
        for (SiModule thisSi : data) {
            if (thisSi.hits.size() > 0) moduleList.get(thisSi.Layer).add(thisSi);
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
            if (nxtIdx < 0) break;
            MeasurementSite nxtSite = SiteList.get(nxtIdx);
            MeasurementSite siteFrom = site;
            for (int lyr=site.m.Layer+1; lyr<nxtSite.m.Layer; ++lyr) { // Loop over hitless layers between two sites with hits
                logger.log(Level.FINER, String.format("KalTrack.addHits: looking for hits on layer %d", lyr));
                for (SiModule module : moduleList.get(lyr)) {
                    MeasurementSite newSite = new MeasurementSite(lyr, module, mxResid, 0.);
                    double [] tRange = {tMax - mxTdif, tMin + mxTdif}; 
                    int rF = newSite.makePrediction(siteFrom.aF, siteFrom.m, -1, false, true, false, tRange);
                    if (rF == 1) {
                        logger.log(Level.FINER, String.format("KalTrack.addHits: predicted chi2inc=%8.3f\n",newSite.chi2inc));
                        if (newSite.chi2inc < mxChi2inc) {
                            if (newSite.filter()) {
                                logger.log(Level.FINER, String.format("KalTrack.addHits: event %d track %d filtered chi2inc=%8.3f",eventNumber,ID,newSite.chi2inc));
                                if (newSite.chi2inc < mxChi2inc) {
                                    logger.log(Level.FINE, String.format("KalTrack.addHits: event %d added hit %d with chi2inc<%8.3f to layer %d",
                                            eventNumber, newSite.hitID, newSite.chi2inc, module.Layer));
                                    newSites.add(newSite);
                                    numAdded++;
                                    nHits++;
                                    siteFrom = newSite;
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
                MeasurementSite siteToDelete = null;
                for (MeasurementSite eSite : SiteList) {
                    if (eSite.m.Layer == site.m.Layer) {
                        siteToDelete = eSite;
                        break;
                    }
                }
                logger.log(Level.FINE, String.format("KalTrack.addHits event %d: added hit %d on layer %d detector %d", eventNumber, site.hitID, site.m.Layer, site.m.detector));
                if (siteToDelete != null) SiteList.remove(siteToDelete);
                SiteList.add(site);
            }
            sortSites(true);
            if (logger.getLevel()==Level.FINER) {
                String str = String.format("KalTrack.addHits: final list of sites: ");
                for (MeasurementSite site : SiteList) {
                    str = str + String.format("(%d, %d, %d) ",site.m.Layer, site.m.detector, site.hitID);
                }
                logger.log(Level.FINER, str);
            }
        } else {
            logger.log(Level.FINE, String.format("KalTrack.addHits: no hits added in event %d to track %d", eventNumber, ID));
        }

        //logger.setLevel(lvl);
        return numAdded;
    }
        
    // re-fit the track 
    public boolean fit(int nIterations, boolean verbose) {
        double chi2s = 0.;
        for (int iteration = 0; iteration < nIterations; iteration++) {
            logger.log(Level.FINER, String.format("KalTrack.fit: starting filtering for iteration %d", iteration));
            StateVector sH = SiteList.get(0).aS;
            sH.helix.C.scale(1000.*chi2); // Blow up the initial covariance matrix to avoid double counting measurements
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
                if (currentSite.makePrediction(sH, prevMod, currentSite.hitID, allowSharing, pickupHits, checkBounds, tRange) < 0) {
                    logger.log(Level.FINE, String.format("KalTrack.fit: event %d, track %d in iteration %d failed to make prediction!!", eventNumber, ID, iteration));
                    return false;
                }
                if (!currentSite.filter()) {
                    logger.log(Level.FINE, String.format("KalTrack.fit: event %d, track %d in iteration %d failed to filter!!", eventNumber, ID, iteration));
                    return false;
                }

                if (currentSite.hitID >= 0) chi2f += Math.max(currentSite.chi2inc,0.);

                sH = currentSite.aF;
                prevMod = currentSite.m;
            }
            logger.log(Level.FINER, String.format("KalTrack.fit: Iteration %d, Fit chi^2 after filtering = %12.4e", iteration, chi2f));
            
            chi2s = 0.;
            MeasurementSite nextSite = null;
            for (int idx = SiteList.size() - 1; idx >= 0; idx--) {
                MeasurementSite currentSite = SiteList.get(idx);
                if (nextSite == null) {
                    currentSite.aS = currentSite.aF.copy();
                    currentSite.smoothed = true;
                } else {
                    currentSite.smooth(nextSite);
                }
                if (currentSite.hitID >= 0) chi2s += Math.max(currentSite.chi2inc,0.);

                nextSite = currentSite;
            }
            logger.log(Level.FINER, String.format("KalTrack.fit: Iteration %d, Fit chi^2 after smoothing = %12.4e", iteration, chi2s));
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
    // covariance of the point of closest approach to the origin (i.e. at phi=0)
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

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

import org.apache.commons.math.util.FastMath;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;
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
    HelixState helixAtOrigin;
    private boolean propagated;
    private RotMatrix Rot;
    private Vec originPoint;
    private Vec originMomentum;
    private ArrayList<Double> yScat;
    private ArrayList<Double> XLscat;
    public double alpha;
    private double[][] Cx;
    private double[][] Cp;
    double Bmag;
    private Vec tB;
    private double time;
    double tMin;
    double tMax;
    final static boolean debug = false;
    private KalmanParams kPar;
    private double chi2incVtx;
    private static DMatrixRMaj tempV;
    private static DMatrixRMaj Cinv;
    private static Logger logger;
    private static boolean initialized;
    private static LinearSolverDense<DMatrixRMaj> solver;

    KalTrack(int evtNumb, int tkID, ArrayList<MeasurementSite> SiteList, ArrayList<Double> yScat, ArrayList<Double> XLscat, KalmanParams kPar) {
        // System.out.format("KalTrack constructor chi2=%10.6f\n", chi2);
        eventNumber = evtNumb;
        this.yScat = yScat;
        this.XLscat = XLscat;        
        this.kPar = kPar;
        ID = tkID;
        
        if (!initialized) {
            logger = Logger.getLogger(KalTrack.class.getName());
            tempV = new DMatrixRMaj(5,1);
            Cinv = new DMatrixRMaj(5,5);
            initialized = true;
            solver = LinearSolverFactory_DDRM.symmPosDef(5);
        }
        
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
            if (site.aS == null) {  // This should never happen
                logger.log(Level.WARNING, String.format("Event %d: site is missing smoothed state vector for layer %d detector %d", 
                        eventNumber, site.m.Layer, site.m.detector));
                logger.log(Level.WARNING, site.toString("bad site"));
                continue;
            }
            this.SiteList.add(site); 
        }
        
        helixAtOrigin = null;
        propagated = false;
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
        if (nHits < 5) {  // This should never happen
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
    
    // Calculate and return the intersection point of the Kaltrack with an SiModule.
    // Local sensor coordinates (u,v) are returned. 
    // The global intersection can be returned via rGbl if an array of length 3 is passed.
    public double [] moduleIntercept(SiModule mod, double [] rGbl) {
        HelixState hx = null;
        for (MeasurementSite site : SiteList) {
            if (site.m == mod) hx = site.aS.helix;
        }
        if (hx == null) {
            int mxLayer = -1;
            for (MeasurementSite site : SiteList) {
                if (site.m.Layer > mod.Layer) continue;
                if (site.m.Layer > mxLayer) {
                    mxLayer = site.m.Layer;
                    hx = site.aS.helix;
                }
            }
        }
        if (hx == null) hx = SiteList.get(0).aS.helix;
        double phiS = hx.planeIntersect(mod.p);
        if (Double.isNaN(phiS)) phiS = 0.;
        Vec intGlb = hx.toGlobal(hx.atPhi(phiS));
        if (rGbl != null) {
            rGbl[0] = intGlb.v[0];
            rGbl[1] = intGlb.v[1];
            rGbl[2] = intGlb.v[2];
        }
        Vec intLcl = mod.toLocal(intGlb);
        double [] rtnArray = {intLcl.v[0], intLcl.v[1]};
        return rtnArray;
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
        double t1 = FastMath.atan2(p1.v[0], p1.v[1]);
        double phiS2 = s2.aS.helix.planeIntersect(s2.m.p);
        if (Double.isNaN(phiS2)) return -999.;
        Vec p2 = s2.aS.helix.getMom(phiS2);
        double t2 = FastMath.atan2(p2.v[0], p2.v[1]);
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
        double t1 = FastMath.atan2(p1.v[2], p1.v[1]);
        double phiS2 = s2.aS.helix.planeIntersect(s2.m.p);
        if (Double.isNaN(phiS2)) return -999.;
        Vec p2 = s2.aS.helix.Rot.inverseRotate(s2.aS.helix.getMom(phiS2));
        double t2 = FastMath.atan2(p2.v[2], p2.v[1]);
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
                    if (tkr.equals(this)) c2 += FastMath.pow((vpred - hit.v) / hit.sigma, 2);
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
            DMatrixRMaj Cstar = new DMatrixRMaj(5,5);
            aStar = site.aS.inverseFilter(site.H, sigma*sigma, Cstar);
            HelixPlaneIntersect hpi = new HelixPlaneIntersect();
            Plane pTrans = site.m.p.toLocal(site.aS.helix.Rot, site.aS.helix.origin);
            double phiInt = hpi.planeIntersect(aStar, site.aS.helix.X0, site.aS.helix.alpha, pTrans);
            if (!Double.isNaN(phiInt)) {
                Vec intPnt = HelixState.atPhi(site.aS.helix.X0, aStar, phiInt, site.aS.helix.alpha);
                Vec globalInt = site.aS.helix.toGlobal(intPnt);
                Vec localInt = site.m.toLocal(globalInt);
                resid = site.m.hits.get(site.hitID).v - localInt.v[1];
                
                CommonOps_DDRM.mult(Cstar, site.H, tempV);               
                varResid = sigma*sigma + CommonOps_DDRM.dot(site.H, tempV);
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
            str=str+helixAtOrigin.toString("helix state for a pivot at the origin")+"\n";
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
            str=str+String.format("Layer %d, detector %d, stereo=%b, chi^2 inc.=%10.6f, Xscat=%10.8f Zscat=%10.8f, arc=%10.5f, hit=%d", m.Layer, m.detector, m.isStereo,
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
                double residual = site.m.hits.get(hitID).v - site.aS.mPred;
                Pair<Double,Double> unBiasedResid = unbiasedResidual(site);
                str=str+String.format("    Intercept=%s, p=%s, measurement=%10.5f, predicted=%10.5f, residual=%9.5f, unbiased=%9.5f+-%9.5f, error=%9.5f \n", interceptVec.toString(),
                        interceptMomVec.toString(), site.m.hits.get(hitID).v, site.aS.mPred, residual, unBiasedResid.getFirstElement(), unBiasedResid.getSecondElement(), FastMath.sqrt(site.aS.R));
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
        propagated = true;

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
        Plane originPlane = new Plane(beamSpot, new Vec(0., 1., 0.)); 
        helixAtOrigin = innerSite.aS.helix.propagateRungeKutta(originPlane, yScat, XLscat, innerSite.m.Bfield);
        if (covNaN()) return false;
        if (!solver.setA(helixAtOrigin.C)) {
            logger.severe("KalTrack:originHelix, cannot invert the covariance matrix");
            return false;
        }
        solver.invert(Cinv);

        // Find the position and momentum of the particle near the origin, including covariance
        Vec XonHelix = helixAtOrigin.atPhi(0.);
        Vec PofHelix = HelixState.aTOp(helixAtOrigin.a);
        originMomentum = Rot.inverseRotate(PofHelix);
        originPoint = Rot.inverseRotate(XonHelix);
        double[][] Dx = DxTOa(helixAtOrigin.a);
        double[][] Dp = DpTOa(helixAtOrigin.a);
        Cx = new double[3][3];
        Cp = new double[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Cx[i][j] = 0.;
                Cp[i][j] = 0.;
                for (int k = 0; k < 5; k++) {
                    for (int l = 0; l < 5; l++) {
                        Cx[i][j] += Dx[i][k] * helixAtOrigin.C.unsafe_get(k, l) * Dx[j][l];
                        Cp[i][j] += Dp[i][k] * helixAtOrigin.C.unsafe_get(k, l) * Dp[j][l];
                    }
                }
            }
        }
        SquareMatrix temp = new SquareMatrix(3, Cx);
        Cx = temp.inverseRotate(Rot).M;
        temp = new SquareMatrix(3, Cp);
        Cp = temp.inverseRotate(Rot).M;
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

    public double[] originPivot() {
        if (propagated) return helixAtOrigin.X0.v.clone();
        else return null;
    }
    
    public double[] originP() {
        if (!propagated) { originHelix(); }
        return originMomentum.v.clone();
    }

    public double[][] originCovariance() {
        if (!propagated) originHelix(); 
        double [][] M = new double[5][5];
        for (int i=0; i<5; ++i) {
            for (int j=0; j<5; ++j) {
                M[i][j] = helixAtOrigin.C.unsafe_get(i, j);
            }
        }
        return M;
    }

    public boolean covNaN() { 
        if (!propagated) originHelix();
        return MatrixFeatures_DDRM.hasNaN(helixAtOrigin.C);
    }
    
    public double[] originHelixParms() {
        if (propagated) return helixAtOrigin.a.v.clone();
        else return null;
    }
    
    //Update the helix parameters at the "origin" by using the target position or vertex as a constraint
    public HelixState originConstraint(double [] vtx, double [][] vtxCov) {
        final boolean verbose = false;
        if (!propagated) originHelix();
        
        // Transform the inputs in the the helix field-oriented coordinate system
        Vec v = helixAtOrigin.toLocal(new Vec(3,vtx));
        SquareMatrix Cov = helixAtOrigin.Rot.rotate(new SquareMatrix(3,vtxCov));
        Vec X0 = helixAtOrigin.X0;
        double phi = phiDOCA(helixAtOrigin.a, v, X0, alpha);
        if (verbose) {  // Test the DOCA algorithm
            Vec rDoca = HelixState.atPhi(X0, helixAtOrigin.a, phi, alpha);
            System.out.format("originConstraint: phi of DOCA=%10.5e\n", phi);
            rDoca.print("  DOCA point");
            double doca = rDoca.dif(v).mag();
            System.out.format("      Minimum doca=%10.7f\n", doca);
            for (double p=phi-0.0001; p<phi+0.0001; p += 0.000001) {
                rDoca = HelixState.atPhi(X0, helixAtOrigin.a, p, alpha);
                doca = rDoca.dif(v).mag();
                System.out.format("   phi=%10.5e, doca=%10.7f\n", p,doca);
            }
            
            double delPhi = 0.00001;
            double f = fDOCA(phi, helixAtOrigin.a, v, X0, alpha);
            double df1 = fDOCA(phi + delPhi, helixAtOrigin.a, v, X0, alpha) - f;
            double deriv = dfDOCAdPhi(phi, helixAtOrigin.a, v, X0, alpha);
            double df2 = deriv * delPhi;
            System.out.format("Test of fDOCA derivative: df exact = %11.7f; df from derivative = %11.7f\n", df1, df2);
        }
        double [][] H = buildH(helixAtOrigin.a, v, X0, phi, alpha);
        Vec pntDOCA = HelixState.atPhi(X0, helixAtOrigin.a, phi, alpha);
        if (verbose) {
            matrixPrint("H", H, 3, 5);
            
            // Derivative test
            HelixState hx = helixAtOrigin.copy();
            double daRel[] = { -0.04, 0.03, -0.16, -0.02, -0.015 };
            for (int i=0; i<5; ++i) daRel[i] = daRel[i]/100.;
            for (int i = 0; i < 5; i++) { hx.a.v[i] = hx.a.v[i] * (1.0 + daRel[i]); }
            Vec da = new Vec(hx.a.v[0] * daRel[0], hx.a.v[1] * daRel[1], hx.a.v[2] * daRel[2], hx.a.v[3] * daRel[3], hx.a.v[4] * daRel[4]);

            double phi2 = phiDOCA(hx.a, v, X0, alpha);
            Vec newX = HelixState.atPhi(X0, hx.a, phi2, alpha);
            Vec dxTrue = newX.dif(pntDOCA);
            dxTrue.print("originConstraint derivative test actual difference");
            
            Vec dx = new Vec(3);
            for (int i=0; i<3; ++i) {
                for (int j=0; j<5; ++j) {
                    dx.v[i] += H[i][j]*da.v[j];
                }
            }
            dx.print("difference from H derivative matrix");
            for (int i=0; i<3; ++i) {
                double err = 100.*(dxTrue.v[i] - dx.v[i])/dxTrue.v[i]; 
                System.out.format("      Coordiante %d: percent difference = %10.6f\n", i, err);
            }
            System.out.println("helix covariance:");
            helixAtOrigin.C.print();
        }
        SquareMatrix Ginv = new SquareMatrix(3);
        for (int i=0; i<3; ++i) {
            for (int j=0; j<3; ++j) {
                Ginv.M[i][j] = Cov.M[i][j];
                for (int k=0; k<5; ++k) {
                    for (int l=0; l<5; ++l) {
                        Ginv.M[i][j] += H[i][k] * helixAtOrigin.C.unsafe_get(k, l) * H[j][l];
                    }
                }
            }
        }
        SquareMatrix G = Ginv.fastInvert();
        double [][] K = new double[5][3];  // Kalman gain matrix
        for (int i=0; i<5; ++i) {
            for (int j=0; j<3; ++j) {
                for (int k=0; k<5; ++k) {
                    for (int l=0; l<3; ++l) {
                        K[i][j] += helixAtOrigin.C.unsafe_get(i, k) * H[l][k] * G.M[l][j];
                    }
                }
            }
        }
        if (verbose) {
            G.print("G");
            matrixPrint("K", K, 5, 3);
        }
        double [] newHelixParms = new double[5];
        double [][] newHelixCov = new double[5][5]; 
        for (int i=0; i<5; ++i) {
            newHelixParms[i] = helixAtOrigin.a.v[i];
            for (int j=0; j<3; ++j) {
                newHelixParms[i] += K[i][j] * (vtx[j] - pntDOCA.v[j]);
            }
            for (int j=0; j<5; ++j) {
                newHelixCov[i][j] = helixAtOrigin.C.unsafe_get(i, j);
                for (int k=0; k<3; ++k) {
                    for (int l=0; l<5; ++l) {
                        newHelixCov[i][j] -= K[i][k] * H[k][l] * helixAtOrigin.C.unsafe_get(l, j);
                    }
                }
            }
        }
        // Calculate the chi-squared contribution
        Vec newHelix = new Vec(5,newHelixParms);
        phi = phiDOCA(newHelix, v, X0, alpha);
        SquareMatrix CovInv = Cov.invert();
        pntDOCA = HelixState.atPhi(X0, newHelix, phi, alpha);
        Vec err = pntDOCA.dif(v);
        chi2incVtx = err.dot(err.leftMultiply(CovInv));
        if (verbose) {
            // Test alternative formulation
            SquareMatrix Vinv = Cov.invert();
            solver.setA(helixAtOrigin.C);
            solver.invert(Cinv);
            SquareMatrix CinvS = mToS(Cinv);
            for (int i=0; i<5; ++i) {
                for (int j=0; j<5; ++j) {
                    for (int k=0; k<3; ++k) {
                        for (int l=0; l<3; ++l) {
                            CinvS.M[i][j] += H[k][i] * Vinv.M[k][l] * H[l][j];
                        }
                    }
                }
            }
            SquareMatrix Calt = CinvS.invert();
            Calt.print("Alternative filtered covariance");
            double [][] Kp = new double[5][3];
            for (int i=0; i<5; ++i) {
                for (int j=0; j<3; ++j) {
                    for (int k=0; k<5; ++k) {
                        for (int l=0; l<3; ++l) {
                            Kp[i][j] += Calt.M[i][k] * H[l][k] * Vinv.M[l][j];
                        }
                    }
                }
            }
            matrixPrint("alternative K", Kp, 5, 3);
        }
        return new HelixState(newHelix, X0, helixAtOrigin.origin, new DMatrixRMaj(newHelixCov), helixAtOrigin.B, helixAtOrigin.tB);
    }
    
    public double chi2incOrigin() {
        return chi2incVtx;
    }
    
    private static void matrixPrint(String s, double [][] A, int M, int N) {
        System.out.format("Dump of %d by %d matrix %s:\n", M, N, s);
        for (int i=0; i<M; ++i) {
            for (int j=0; j<N; ++j) {
                System.out.format(" %10.5f", A[i][j]);
            }
            System.out.format("\n");
        }
    }
    
    //Find the helix turning alpha phi for the point on the helix 'a' closest to the point 'v'
    private static double phiDOCA(Vec a, Vec v, Vec X0, double alpha) {
        
        Plane p = new Plane(v, new Vec(0.,1.,0.));
        HelixPlaneIntersect hpa = new HelixPlaneIntersect();
        double phiInt = hpa.planeIntersect(a, X0, alpha, p);
        if (Double.isNaN(phiInt)) {
            phiInt = 0.;
        } 
        double dphi = 0.1;
        double accuracy = 0.0000001;
        double phiDoca = rtSafe(phiInt, phiInt-dphi, phiInt+dphi, accuracy, a, v, X0, alpha);

        return phiDoca;
    }

    // Safe Newton-Raphson zero finding from Numerical Recipes in C, used here to solve for the point of closest approach.
    private static double rtSafe(double xGuess, double x1, double x2, double xacc, Vec a, Vec v, Vec X0, double alpha) {
        // Here xGuess is a starting guess for the phi angle of the helix DOCA point
        // x1 and x2 give a range for the value of the solution
        // xacc specifies the accuracy needed
        // The output is an accurate result for the phi of the DOCA point
        double df, dx, dxold, f, fh, fl;
        double temp, xh, xl, rts;
        int MAXIT = 100;

        if (xGuess <= x1 || xGuess >= x2) {
            System.out.format("KalTrack.rtsafe: initial guess needs to be bracketed.");
            return xGuess;
        }
        fl = fDOCA(x1, a, v, X0, alpha);
        fh = fDOCA(x2, a, v, X0, alpha);
        int nTry = 0;
        while (fl*fh > 0.0) {
            if (nTry == 5) {
                System.out.format("KalTrack.rtsafe: Root is not bracketed in zero finding, fl=%12.5e, fh=%12.5e, alpha=%10.6f, x1=%12.5f x2=%12.5f xGuess=%12.5f", 
                        fl, fh, alpha, x1, x2, xGuess);
                return xGuess;
            }
            x1 -= 0.01;
            x2 += 0.01;
            fl = fDOCA(x1, a, v, X0, alpha);
            fh = fDOCA(x2, a, v, X0, alpha);
            nTry++;
        }
        //if (nTry > 0) System.out.format("KalTrack.rtsafe: %d tries needed to bracket solution.\n", nTry);
        if (fl == 0.) return x1;
        if (fh == 0.) return x2;
        if (fl < 0.0) {
            xl = x1;
            xh = x2;
        } else {
            xh = x1;
            xl = x2;
        }
        rts = xGuess;
        dxold = Math.abs(x2 - x1);
        dx = dxold;
        f = fDOCA(rts, a, v, X0, alpha);
        df = dfDOCAdPhi(rts,a, v, X0, alpha);
        for (int j = 1; j <= MAXIT; j++) {
            if ((((rts - xh) * df - f) * ((rts - xl) * df - f) > 0.0) || (Math.abs(2.0 * f) > Math.abs(dxold * df))) {
                dxold = dx;
                dx = 0.5 * (xh - xl); // Use bisection if the Newton-Raphson method is going bonkers
                rts = xl + dx;
                if (xl == rts) return rts;
            } else {
                dxold = dx;
                dx = f / df; // Newton-Raphson method
                temp = rts;
                rts -= dx;
                if (temp == rts) return rts;
            }
            if (Math.abs(dx) < xacc) {
                // System.out.format("KalTrack.rtSafe: solution converged in %d iterations.\n",
                // j);
                return rts;
            }
            f = fDOCA(rts, a, v, X0, alpha);
            df = dfDOCAdPhi(rts,a, v, X0, alpha);
            if (f < 0.0) {
                xl = rts;
            } else {
                xh = rts;
            }
        }
        System.out.format("KalTrack.rtsafe: maximum number of iterations exceeded.");
        return rts;
    }
    
    // Function that is zero when the helix turning angle phi is at the point of closest approach to v
    private static double fDOCA(double phi, Vec a, Vec v, Vec X0, double alpha) {
        Vec t = tangentVec(a, phi, alpha);
        Vec x = HelixState.atPhi(X0, a, phi, alpha);
        return (v.dif(x)).dot(t);
    }
    
    // derivative of the fDOCA function with respect to phi, for the zero-finding algorithm
    private static double dfDOCAdPhi(double phi, Vec a, Vec v, Vec X0, double alpha) {
        Vec x = HelixState.atPhi(X0, a, phi, alpha);
        Vec dxdphi = dXdPhi(a, phi, alpha);
        Vec t = tangentVec(a, phi, alpha);
        Vec dtdphi = dTangentVecDphi(a, phi, alpha);
        Vec dfdt = v.dif(x);
        double dfdphi = -t.dot(dxdphi) + dfdt.dot(dtdphi);
        return dfdphi;
    }
    
    // Derivatives of position along a helix with respect to the turning angle phi
    private static Vec dXdPhi(Vec a, double phi, double alpha) {
        return new Vec((alpha / a.v[2]) * FastMath.sin(a.v[1] + phi), -(alpha / a.v[2]) * FastMath.cos(a.v[1] + phi),
                -(alpha / a.v[2]) * a.v[4]);
    }
    
    // A vector tangent to the helix 'a' at the alpha phi
    private static Vec tangentVec(Vec a, double phi, double alpha) {
        return new Vec((alpha/a.v[2])*FastMath.sin(a.v[1]+phi), -(alpha/a.v[2])*FastMath.cos(a.v[1]+phi), -(alpha/a.v[2])*a.v[4]);
    }
    
    private static Vec dTangentVecDphi(Vec a, double phi, double alpha) {
        return new Vec((alpha/a.v[2])*FastMath.cos(a.v[1]+phi), (alpha/a.v[2])*FastMath.sin(a.v[2]+phi), 0.);
    }
    
    //Derivative matrix for the helix 'a' point of closet approach to point 'v'
    private static double [][] buildH(Vec a, Vec v, Vec X0, double phi, double alpha) {
        // a = helix parameters
        // X0 = pivot point of helix
        // phi = angle along helix to the point of closet approach
        // v = 3D point for which we are finding the DOCA (the "measurement" point)
        // alpha = constant to convert from curvature to 1/pt
        
        Vec x = HelixState.atPhi(X0, a, phi, alpha);
        Vec dxdphi = dXdPhi(a, phi, alpha);
        Vec t = tangentVec(a, phi, alpha);
        Vec dtdphi = dTangentVecDphi(a, phi, alpha);
        Vec dfdt = v.dif(x);
        double dfdphi = -t.dot(dxdphi) + dfdt.dot(dtdphi);
        double [][] dtda = new double[3][5];
        dtda[0][1] = (alpha/a.v[2])*FastMath.cos(a.v[1]+phi);
        dtda[0][2] = (-alpha/(a.v[2]*a.v[2]))*FastMath.sin(a.v[1]+phi);
        dtda[1][1] = (alpha/a.v[2])*FastMath.sin(a.v[1]+phi);
        dtda[1][2] = (alpha/(a.v[2]*a.v[2]))*FastMath.cos(a.v[1]+phi);
        dtda[2][2] = (alpha/(a.v[2]*a.v[2]))*a.v[4];
        dtda[2][4] = -alpha/a.v[2];
        
        double [][] dxda = new double[3][5];
        dxda[0][0] = FastMath.cos(a.v[1]);
        dxda[1][0] = FastMath.sin(a.v[1]);
        dxda[0][1] = -(a.v[0] + alpha / a.v[2]) * FastMath.sin(a.v[1]) + (alpha / a.v[2]) * FastMath.sin(a.v[1] + phi);
        dxda[1][1] = (a.v[0] + alpha / a.v[2]) * FastMath.cos(a.v[1]) - (alpha / a.v[2]) * FastMath.cos(a.v[1] + phi);
        dxda[0][2] = -(alpha / (a.v[2] * a.v[2])) * (FastMath.cos(a.v[1]) - FastMath.cos(a.v[1] + phi));
        dxda[1][2] = -(alpha / (a.v[2] * a.v[2])) * (FastMath.sin(a.v[1]) - FastMath.sin(a.v[1] + phi));
        dxda[2][2] = (alpha / (a.v[2] * a.v[2])) * a.v[4] * phi;
        dxda[2][3] = 1.0;
        dxda[2][4] = -(alpha / a.v[2]) * phi;
        
        Vec dfda = new Vec(5);
        Vec dphida = new Vec(5);
        for (int i=0; i<5; ++i) {
            for (int j=0; j<3; ++j) {
                dfda.v[i] += -t.v[j]*dxda[j][i] + dfdt.v[j]*dtda[j][i];
            }
            dphida.v[i] = -dfda.v[i]/dfdphi;
        }
        
        double [][] H = new double[3][5];
        for (int i=0; i<3; ++i) {
            for (int j=0; j<5; ++j) {
                H[i][j] = dxdphi.v[i]*dphida.v[j] + dxda[i][j];
            }           
        }
        
        return H;
    }

    public double helixErr(int i) {
        return FastMath.sqrt(helixAtOrigin.C.unsafe_get(i, i));
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
        if (debug) System.out.format("Event %d track %d remove hit %d on layer %d detector %d\n", 
                eventNumber, ID, site.hitID, site.m.Layer, site.m.detector);
        if (site.hitID < 0) { // This should never happen
            logger.log(Level.WARNING, String.format("Event %d track %d, trying to remove nonexistent hit on layer %d detector %d", 
                    eventNumber, ID, site.m.Layer, site.m.detector));
            return exchange;
        }
        if (site.m.hits.get(site.hitID).tracks.contains(this)) {
            site.m.hits.get(site.hitID).tracks.remove(this);
        } else { // This should never happen
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
            if (debug) System.out.format("Event %d track %d added hit %d on layer %d detector %d\n", 
                    eventNumber, ID, site.hitID, site.m.Layer, site.m.detector);
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
        
        if (verbose) logger.setLevel(Level.FINER);
        if (debug) System.out.format("addHits, event %d: trying to add hits to track %d\n", eventNumber, ID);
        
        sortSites(true);

        if (debug) {
            String str = String.format("KalTrac.addHits: initial list of sites: ");
            for (MeasurementSite site : SiteList) {
                str = str + String.format("(%d, %d, %d) ",site.m.Layer, site.m.detector, site.hitID);
            }
            System.out.format("%s\n", str);
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
                if (debug) System.out.format("KalTrack.addHits: looking for hits on layer %d\n", lyr);
                for (SiModule module : moduleList.get(lyr)) {
                    MeasurementSite newSite = new MeasurementSite(lyr, module, mxResid, 0.);
                    double [] tRange = {tMax - mxTdif, tMin + mxTdif}; 
                    int rF = newSite.makePrediction(siteFrom.aF, siteFrom.m, -1, false, true, false, tRange, verbose);
                    if (rF == 1) {
                        if (debug) System.out.format("KalTrack.addHits: predicted chi2inc=%8.3f\n",newSite.chi2inc);
                        if (newSite.chi2inc < mxChi2inc) {
                            if (newSite.filter()) {
                                if (debug) System.out.format("KalTrack.addHits: event %d track %d filtered chi2inc=%8.3f\n",eventNumber,ID,newSite.chi2inc);
                                if (newSite.chi2inc < mxChi2inc) {
                                    if (debug) System.out.format("KalTrack.addHits: event %d added hit %d with chi2inc<%8.3f to layer %d\n",
                                            eventNumber, newSite.hitID, newSite.chi2inc, module.Layer);
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
                if (debug) System.out.format("KalTrack.addHits event %d: added hit %d on layer %d detector %d\n", eventNumber, site.hitID, site.m.Layer, site.m.detector);
                if (siteToDelete != null) SiteList.remove(siteToDelete);
                SiteList.add(site);
            }
            sortSites(true);
            if (debug) {
                String str = String.format("KalTrack.addHits: final list of sites: ");
                for (MeasurementSite site : SiteList) {
                    str = str + String.format("(%d, %d, %d) ",site.m.Layer, site.m.detector, site.hitID);
                }
                System.out.format("%s\n", str);
            }
        } else {
            if (debug) System.out.format("KalTrack.addHits: no hits added in event %d to track %d\n", eventNumber, ID);
        }

        return numAdded;
    }
        
    // re-fit the track 
    public boolean fit(int nIterations, boolean verbose) {
        double chi2s = 0.;
        for (int iteration = 0; iteration < nIterations; iteration++) {
            if (debug) System.out.format("KalTrack.fit: starting filtering for iteration %d\n", iteration);
            StateVector sH = SiteList.get(0).aS;
            CommonOps_DDRM.scale(100., sH.helix.C);  // Blow up the initial covariance matrix to avoid double counting measurements 
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
                    if (debug) System.out.format("KalTrack.fit: event %d, track %d in iteration %d failed to make prediction!!\n", eventNumber, ID, iteration);
                    return false;
                }
                if (!currentSite.filter()) {
                    if (debug) System.out.format("KalTrack.fit: event %d, track %d in iteration %d failed to filter!!\n", eventNumber, ID, iteration);
                    return false;
                }

                if (currentSite.hitID >= 0) chi2f += Math.max(currentSite.chi2inc,0.);

                sH = currentSite.aF;
                prevMod = currentSite.m;
            }
            if (debug) System.out.format("KalTrack.fit: Iteration %d, Fit chi^2 after filtering = %12.4e\n", iteration, chi2f);
            
            chi2s = 0.;
            MeasurementSite nextSite = null;
            for (int idx = SiteList.size() - 1; idx >= 0; idx--) {
                MeasurementSite currentSite = SiteList.get(idx);
                if (nextSite == null) {
                    currentSite.aS = currentSite.aF;
                    currentSite.smoothed = true;
                } else {
                    currentSite.smooth(nextSite);
                }
                if (currentSite.hitID >= 0) chi2s += Math.max(currentSite.chi2inc,0.);

                nextSite = currentSite;
            }
            if (debug) System.out.format("KalTrack.fit: Iteration %d, Fit chi^2 after smoothing = %12.4e\n", iteration, chi2s);
        }
        this.chi2 = chi2s;
        propagated = false;
        return true;
    }

    // Derivative matrix for propagating the covariance of the helix parameters to a covariance of momentum
    static double[][] DpTOa(Vec a) {
        double[][] M = new double[3][5];
        double K = Math.abs(a.v[2]);
        double sgn = Math.signum(a.v[2]);
        M[0][1] = -FastMath.cos(a.v[1]) / K;
        M[1][1] = -FastMath.sin(a.v[1]) / K;
        M[0][2] = sgn * FastMath.sin(a.v[1]) / (K * K);
        M[1][2] = -sgn * FastMath.sin(a.v[1]) / (K * K);
        M[2][4] = 1. / K;
        M[2][2] = -sgn * a.v[4] / (K * K);
        return M;
    }

    // Derivative matrix for propagating the covariance of the helix parameter to a
    // covariance of the point of closest approach to the origin (i.e. at phi=0)
    static double[][] DxTOa(Vec a) {
        double[][] M = new double[3][5];
        M[0][0] = FastMath.cos(a.v[1]);
        M[0][1] = -a.v[0] * FastMath.sin(a.v[1]);
        M[1][0] = FastMath.sin(a.v[1]);
        M[1][1] = a.v[0] * FastMath.cos(a.v[1]);
        M[2][3] = 1.0;
        return M;
    }

    // Comparator function for sorting tracks by quality
    static Comparator<KalTrack> TkrComparator = new Comparator<KalTrack>() {
        public int compare(KalTrack t1, KalTrack t2) {
            Double chi1 = new Double(t1.chi2 / t1.nHits + 10.0*(1.0 - (double)t1.nHits/12.));
            Double chi2 = new Double(t2.chi2 / t2.nHits + 10.0*(1.0 - (double)t2.nHits/12.));
            return chi1.compareTo(chi2);
        }
    };

    static SquareMatrix mToS(DMatrixRMaj M) {
        SquareMatrix S = new SquareMatrix(M.numRows);
        if (M.numCols != M.numRows) return null;
        for (int i=0; i<M.numRows; ++i) {
            for (int j=0; j<M.numRows; ++j) {
                S.M[i][j] = M.unsafe_get(i, j);
            }
        }
        return S;
    }
    
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

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

import org.lcsim.event.TrackerHit;
import java.lang.Math;
/**
 * Track followed and fitted by the Kalman filter
 */
public class KalTrack {

    public int ID;
    public int nHits;
    public double chi2;
    private double reducedChi2;

    ArrayList<MeasurementSite> SiteList;
    // call the corresponding functions to create and access the following two maps
    private Map<MeasurementSite, Vec> interceptVects;
    private Map<MeasurementSite, Vec> interceptMomVects;
    Map<Integer, MeasurementSite> millipedeMap;
    Map<Integer, MeasurementSite> lyrMap;
    Map<Integer, Integer> laymilli;
    public int eventNumber;
    public boolean bad;
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
    static final boolean debug = false;
    private KalmanParams kPar;
    private double chi2incVtx;
    private static DMatrixRMaj tempV; 
    private static DMatrixRMaj Cinv;
    private static Logger logger;
    private static boolean initialized;
    private double[] arcLength;
    private static LinearSolverDense<DMatrixRMaj> solver;
    static int[] nBadCov = {0, 0};
    private boolean trimSites = false;

    KalTrack(int evtNumb, int tkID, ArrayList<MeasurementSite> SiteList, ArrayList<Double> yScat, ArrayList<Double> XLscat, KalmanParams kPar) {
        // System.out.format("KalTrack constructor chi2=%10.6f\n", chi2);
        eventNumber = evtNumb;
        bad = false;
        this.yScat = yScat;
        this.XLscat = XLscat;
        this.kPar = kPar;
        ID = tkID;
        arcLength = null;
        //debug = (evtNumb == 217481);
        
        if (!initialized) {
            logger = Logger.getLogger(KalTrack.class.getName());
            tempV = new DMatrixRMaj(5, 1);
            Cinv = new DMatrixRMaj(5, 5);
            initialized = true;
            solver = LinearSolverFactory_DDRM.symmPosDef(5);
        }

        // Trim empty sites from the track ends
        Collections.sort(SiteList, MeasurementSite.SiteComparatorUp);
        int firstSite = -1;
        int lastSite = 999;
	    if (trimSites) {
            for (int idx = 0; idx < SiteList.size(); ++idx) {
                firstSite = idx;
                if (SiteList.get(idx).hitID >= 0) {
                    break;
                }
	        }
	        for (int idx = SiteList.size() - 1; idx >= 0; --idx) {
		        lastSite = idx;
		        if (SiteList.get(idx).hitID >= 0) {
		            break;
		        }
	        }
	    } else {
            firstSite = 0;
            lastSite = SiteList.size() - 1;
	    }
        if (debug) System.out.println("SiteList size for this track is :  " + SiteList.size());
        this.SiteList = new ArrayList<MeasurementSite>(SiteList.size());
        for (int idx = firstSite; idx <= lastSite; ++idx) {
            MeasurementSite site = SiteList.get(idx);
            if(debug) System.out.println("site number = " + idx + "  Layer Number = " + site.m.Layer + " has hit? " + site.hitID);
            if (site.aS == null && debug) {  // This will happen pretty often when keeping sites with no hits at ends of track
                logger.log(Level.SEVERE, String.format("Event %d: site of track %d is missing smoothed state vector for layer %d detector %d", eventNumber, ID, site.m.Layer, site.m.detector));
                logger.log(Level.WARNING, site.toString("bad site"));
                System.out.println("site.aS == null for site number "+idx+" layer number = "+site.m.Layer); 
                site.print("bad one"); 
                bad = true;
                continue;
            }
            this.SiteList.add(site);
        }
        helixAtOrigin = null;
        propagated = false;
        MeasurementSite site0 = this.SiteList.get(0);
        Vec B = KalmanInterface.getField(new Vec(3, kPar.beamSpot), site0.m.Bfield);
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
	double terr = 0.0;
        tMin = 9.9e9;
        tMax = -9.9e9;
        this.chi2 = 0.;
        this.nHits = 0;
        if (debug) {
            System.out.format("KalTrack: event %d, creating track %d\n", evtNumb, ID);
        }
        for (MeasurementSite site : this.SiteList) {
            if (site.hitID < 0) {
                continue;
            }
            nHits++;
	    double hTV = site.m.hits.get(site.hitID).timeErr;
            time += site.m.hits.get(site.hitID).time/hTV;
	    terr += 1.0/hTV;
            if(site.m.Layer>kPar.maxInnerLayer){
                tMin = Math.min(tMin, site.m.hits.get(site.hitID).time);
                tMax = Math.max(tMax, site.m.hits.get(site.hitID).time);
            }
            this.chi2 += site.chi2inc;
            if (debug) {
                System.out.format("  Layer %d, chi^2 increment=%10.5f, a=%s\n", site.m.Layer, site.chi2inc, site.aS.helix.a.toString());
            }
        }
        time = time / terr;
        reducedChi2 = chi2 / (double) nHits;
        lyrMap = null;
        millipedeMap = null;
        interceptVects = null;
        interceptMomVects = null;
        if (nHits < 5) {  // This should never happen
            logger.log(Level.WARNING, "KalTrack error: not enough hits (" + nHits + ") on the candidate track (ID::" + ID + ") for event " + eventNumber);
            bad = true;
            // TODO can this be removed?
            //for (MeasurementSite site : SiteList) logger.log(Level.FINE, site.toString("in KalTrack input list"));
            //logger.log(Level.FINE, String.format("KalTrack error in event %d: not enough hits on track %d: ",evtNumb,tkID));
            //String str="";
            //for (MeasurementSite site : SiteList) {
            //    str = str + String.format("(%d, %d, %d) ",site.m.Layer,site.m.detector,site.hitID);
            //}
            //str = str + "\n";
            //logger.log(Level.FINER,str);
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
                if (site.smoothed) {
                    sV = site.aS;
                } else {
                    sV = site.aP;
                }
                double phiS = sV.helix.planeIntersect(site.m.p);
                if (Double.isNaN(phiS)) {
                    phiS = 0.;
                }
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
                if (site.smoothed) {
                    sV = site.aS;
                } else {
                    sV = site.aP;
                }
                double phiS = sV.helix.planeIntersect(site.m.p);
                if (Double.isNaN(phiS)) {
                    phiS = 0.;
                }
                interceptMomVects.put(site, sV.helix.Rot.inverseRotate(sV.helix.getMom(phiS)));
            }
        }
        return interceptMomVects;
    }

    // Calculate and return the intersection point of the Kaltrack with an SiModule.
    // Local sensor coordinates (u,v) are returned. 
    // The global intersection can be returned via rGbl if an array of length 3 is passed.
    public double[] moduleIntercept(SiModule mod, double[] rGbl) {
        HelixState hx = null;
        for (MeasurementSite site : SiteList) {
            if (site.m == mod) {
                hx = site.aS.helix;
            }
        }
        if (hx == null) {
            int mxLayer = -1;
            for (MeasurementSite site : SiteList) {
                if (site.m.Layer > mod.Layer) {
                    continue;
                }
                if (site.m.Layer > mxLayer) {
                    mxLayer = site.m.Layer;
                    hx = site.aS.helix;
                }
            }
        }
        if (hx == null) {
            hx = SiteList.get(0).aS.helix;
        }
        double phiS = hx.planeIntersect(mod.p);
        if (Double.isNaN(phiS)) {
            phiS = 0.;
        }
        Vec intGlb = hx.toGlobal(hx.atPhi(phiS));
        if (rGbl != null) {
            rGbl[0] = intGlb.v[0];
            rGbl[1] = intGlb.v[1];
            rGbl[2] = intGlb.v[2];
        }
        Vec intLcl = mod.toLocal(intGlb);
        double[] rtnArray = {intLcl.v[0], intLcl.v[1]};
        return rtnArray;
    }

    // Return an unbiased intersection point of a track with a sensor
    // If "local" is true, then the point is returned in the sensor system, otherwise global
    public Pair<Double[], Double> unbiasedIntersect(MeasurementSite site, boolean local) {
        if (lyrMap == null) {
            makeLyrMap();
        }
        Vec aStar = null;
        DMatrixRMaj Cstar = new DMatrixRMaj(5, 5);    

        if (site.hitID >= 0 && site.m.hits.size()>0) {  // Use the inverse filter to remove the bias from the local hit
            double sigma = site.m.hits.get(site.hitID).sigma;
            aStar = site.aS.inverseFilter(site.H, sigma * sigma, Cstar);
        } else {                // Just use the local helix if there is no local hit
            aStar = site.aS.helix.a;
            Cstar = site.aS.helix.C;
        }
        HelixPlaneIntersect hpi = new HelixPlaneIntersect();
        // Transform the detector plane into the local B-field system, since that is the system
        // in which the helix parameters are defined
        Plane pTrans = site.m.p.toLocal(site.aS.helix.Rot, site.aS.helix.origin);
        // Find the phi turning angle to the intersection point of the helix and detector plane
        double phiInt = hpi.planeIntersect(aStar, site.aS.helix.X0, site.aS.helix.alpha, pTrans);
        if (!Double.isNaN(phiInt)) {
            // Get the intersection point of the helix and plane in the local field coordinates
            Vec intPnt = HelixState.atPhi(site.aS.helix.X0, aStar, phiInt, site.aS.helix.alpha);
            // Transform the intersection back into the global coordinates
            Vec globalInt = site.aS.helix.toGlobal(intPnt);
            if (!local) {
                new Pair<>(new Double[]{globalInt.v[0], globalInt.v[1], globalInt.v[2]}, 999); //I didn't include global variance since I'm lazy
            }            // Transform the intersection point to the local sensor system
            Vec localInt = site.m.toLocal(globalInt);
            CommonOps_DDRM.mult(Cstar, site.H, tempV);
            Double varUmeas = CommonOps_DDRM.dot(site.H, tempV);
            return new Pair<>(new Double[]{localInt.v[0], localInt.v[1], localInt.v[2]}, varUmeas);
        } else {
            if(debug)System.out.println("no phi-interect with this layer found!");
        }

        return new Pair<>(new Double[]{666., 666., 666.}, 666.);
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

    private void makeLayMilli(){
    	laymilli = new HashMap<Integer, Integer>(14);
	for (MeasurementSite site : SiteList){
	    laymilli.put(site.m.millipedeID,site.m.Layer);
	}
    }

    public int millToLay(int millipedeID){
    	if (laymilli == null) {
            makeLayMilli();
        }
        if (millipedeMap.containsKey(millipedeID)) {
            return laymilli.get(millipedeID);
        } else {
            return -1;
        } 
    }

    // Does the scatX for Millipede
    public double scatXMillipede(int millipedeID){
    	if (laymilli == null) {
            makeLayMilli();
        }
        if (laymilli.containsKey(millipedeID)) {
            return scatX(laymilli.get(millipedeID));
        } else {
            return -1000.0;
        }
    }

    // Does the scatZ for Millipede
    public double scatZMillipede(int millipedeID){
    	if (laymilli == null) {
            makeLayMilli();
        }
        if (laymilli.containsKey(millipedeID)) {
            return scatZ(laymilli.get(millipedeID));
        } else {
            return -1000.0;
        }
    }

    // Find the change in smoothed helix angle in XY between one layer and the next
    public double scatX(int layer) {
        if (lyrMap == null) {
            makeLyrMap();
        }
        if (!lyrMap.containsKey(layer)) {
            return -999.;
        }
        int lyrNxt = layer + 1;
        while (lyrNxt <= 13 && !lyrMap.containsKey(lyrNxt)) {
            lyrNxt++;
        }
        if (lyrNxt > 13) {
            return -999.;
        }

        MeasurementSite s1 = lyrMap.get(layer);
        MeasurementSite s2 = lyrMap.get(lyrNxt);
        if (s1.aS == null || s2.aS == null) {
            return -999.;
        }
        double phiS1 = s1.aS.helix.planeIntersect(s2.m.p);
        if (Double.isNaN(phiS1)) {
            return -999.;
        }
        Vec p1 = s1.aS.helix.getMom(phiS1);
        double t1 = FastMath.atan2(p1.v[0], p1.v[1]);
        double phiS2 = s2.aS.helix.planeIntersect(s2.m.p);
        if (Double.isNaN(phiS2)) {
            return -999.;
        }
        Vec p2 = s2.aS.helix.getMom(phiS2);
        double t2 = FastMath.atan2(p2.v[0], p2.v[1]);
        return t1 - t2;
    }

    // Find the change in smoothed helix angle in ZY between one layer and the next
    public double scatZ(int layer) {
        if (lyrMap == null) {
            makeLyrMap();
        }
        if (!lyrMap.containsKey(layer)) {
            return -999.;
        }
        int lyrNxt = layer + 1;
        while (lyrNxt <= 13 && !lyrMap.containsKey(lyrNxt)) {
            lyrNxt++;
        }
        if (lyrNxt > 13) {
            return -999.;
        }
        MeasurementSite s1 = lyrMap.get(layer);
        MeasurementSite s2 = lyrMap.get(lyrNxt);
        if (s1.aS == null || s2.aS == null) {
            return -999.;
        }
        double phiS1 = s1.aS.helix.planeIntersect(s2.m.p);
        if (Double.isNaN(phiS1)) {
            return -999.;
        }
        Vec p1 = s1.aS.helix.Rot.inverseRotate(s1.aS.helix.getMom(phiS1));
        double t1 = FastMath.atan2(p1.v[2], p1.v[1]);
        double phiS2 = s2.aS.helix.planeIntersect(s2.m.p);
        if (Double.isNaN(phiS2)) {
            return -999.;
        }
        Vec p2 = s2.aS.helix.Rot.inverseRotate(s2.aS.helix.getMom(phiS2));
        double t2 = FastMath.atan2(p2.v[2], p2.v[1]);
        return t1 - t2;
    }

    public double chi2prime() { // Alternative calculation of the fit chi^2, considering only residuals divided by the hit sigma
        double c2 = 0.;
        for (MeasurementSite S : SiteList) {
            if (S.aS == null) {
                continue;
            }
            double phiS = S.aS.helix.planeIntersect(S.m.p);
            if (Double.isNaN(phiS)) {
                phiS = 0.;
            }
            double vpred = S.h(S.aS, S.m, phiS);
            for (Measurement hit : S.m.hits) {
                for (KalTrack tkr : hit.tracks) {
                    if (tkr.equals(this)) {
                        c2 += FastMath.pow((vpred - hit.v) / hit.sigma, 2);
                    }
                }
            }
        }
        return c2;
    }

    public Pair<Double, Double> unbiasedResidualMillipede(int millipedeID) {
        if (millipedeMap == null) {
            makeMillipedeMap();
        }
        if (millipedeMap.containsKey(millipedeID)) {
            return unbiasedResidual(millipedeMap.get(millipedeID));
        } else {
            return new Pair<Double, Double>(-999., -999.);
        }
    }

    public Pair<Double, Double> unbiasedResidual(int layer) {
        if (lyrMap == null) {
            makeLyrMap();
        }
        if (lyrMap.containsKey(layer)) {
            return unbiasedResidual(lyrMap.get(layer));
        } else {
            return new Pair<Double, Double>(-999., -999.);
        }
    }

    public Pair<Double[], Double> unbiasedIntersect(int layer, boolean local) {
        if (lyrMap == null) {
            makeLyrMap();
        }
        if (lyrMap.containsKey(layer)) {
            return unbiasedIntersect(lyrMap.get(layer),local);
        } else {
            if(debug)System.out.println("KalTrack::unbiasedIntersect  lyrMap does not contain layer = " + layer + " lyrMap.size() = " + lyrMap.size());
            return new Pair<Double[], Double>(new Double[]{-999., -999., -999.}, -999.);
        }
    }

    // Returns the unbiased residual for the track at a given layer, together with the variance on that residual
    public Pair<Double, Double> unbiasedResidual(MeasurementSite site) {
        double resid = -999.;
        double varResid = -999.;
        Vec aStar = null;
        if (site.hitID >= 0) {
            double sigma = site.m.hits.get(site.hitID).sigma;
            DMatrixRMaj Cstar = new DMatrixRMaj(5, 5);
            aStar = site.aS.inverseFilter(site.H, sigma * sigma, Cstar);
            HelixPlaneIntersect hpi = new HelixPlaneIntersect();
            Plane pTrans = site.m.p.toLocal(site.aS.helix.Rot, site.aS.helix.origin);
            double phiInt = hpi.planeIntersect(aStar, site.aS.helix.X0, site.aS.helix.alpha, pTrans);
            if (!Double.isNaN(phiInt)) {
                Vec intPnt = HelixState.atPhi(site.aS.helix.X0, aStar, phiInt, site.aS.helix.alpha);
                Vec globalInt = site.aS.helix.toGlobal(intPnt);
                Vec localInt = site.m.toLocal(globalInt);
                resid = site.m.hits.get(site.hitID).v - localInt.v[1];

                CommonOps_DDRM.mult(Cstar, site.H, tempV);
                varResid = sigma * sigma + CommonOps_DDRM.dot(site.H, tempV);
            }
        }
        return new Pair<Double, Double>(resid, varResid);
    }

    public Pair<Double, Double> biasedResidual(int layer) {
        if (lyrMap == null) {
            makeLyrMap();
        }
        if (lyrMap.containsKey(layer)) {
            return biasedResidual(lyrMap.get(layer));
        } else {
            return new Pair<Double, Double>(-999., -999.);
        }
    }

    public Pair<Double, Double> biasedResidualMillipede(int millipedeID) {
        if (millipedeMap == null) {
            makeMillipedeMap();
        }
        if (millipedeMap.containsKey(millipedeID)) {
            return biasedResidual(millipedeMap.get(millipedeID));
        } else {
            return new Pair<Double, Double>(-999., -999.);
        }
    }

    public Pair<Double, Double> biasedResidual(MeasurementSite site) {
        double resid = -999.;
        double varResid = -999.;
        if (site.aS != null) {
            resid = site.aS.r;
            varResid = site.aS.R;
        }
        return new Pair<Double, Double>(resid, varResid);
    }

    public void print(String s) {
        System.out.format("\nKalTrack %s: Event %d, ID=%d, %d hits, chi^2=%10.5f, t=%5.1f from %5.1f to %5.1f, bad=%b\n", s, eventNumber, ID, nHits, chi2, time, tMin, tMax, bad);
        if (propagated) {
            System.out.format("    Helix parameters at origin = %s\n", helixAtOrigin.a.toString());
        }
        MeasurementSite site0 = this.SiteList.get(0);
        if (site0.aS != null) {
            System.out.format("    Helix at layer %d: %s, pivot=%s\n", site0.m.Layer, site0.aS.helix.a.toString(), site0.aS.helix.X0.toString());
        }
        for (int i = 0; i < SiteList.size(); i++) {
            MeasurementSite site = SiteList.get(i);
            SiModule m = site.m;
            int hitID = site.hitID;
            System.out.format("    Layer %d, detector %d, stereo=%b, chi^2 inc.=%10.6f ", m.Layer, m.detector, m.isStereo,
                    site.chi2inc);
            if (hitID >= 0) {
                System.out.format(" t=%5.1f ", site.m.hits.get(site.hitID).time);
                double residual = site.m.hits.get(hitID).v - site.aS.mPred;
                Pair<Double, Double> unBiasedResid = unbiasedResidual(site);
                double[] lclint = moduleIntercept(m, null);
                System.out.format("    measurement=%10.5f, predicted=%10.5f, residual=%9.5f, x=%9.5f limit=%9.5f \n", site.m.hits.get(hitID).v, site.aS.mPred,
                        residual, lclint[0], m.xExtent[1]);
            } else {
                System.out.format("\n");
            }
        }
    }

    public void printLong(String s) {
        System.out.format("%s", this.toString(s));
    }

    String toString(String s) {
        String str = String.format("\n KalTrack %s: Event %d, ID=%d, %d hits, chi^2=%10.5f, t=%5.1f from %5.1f to %5.1f, bad=%b\n", s, eventNumber, ID, nHits, chi2, time, tMin, tMax, bad);
        if (propagated) {
            str = str + String.format("    B-field at the origin=%10.6f,  direction=%8.6f %8.6f %8.6f\n", Bmag, tB.v[0], tB.v[1], tB.v[2]);
            str = str + helixAtOrigin.toString("helix state for a pivot at the origin") + "\n";
            str = str + originPoint.toString("point on the helix closest to the origin") + "\n";
            str = str + String.format("   arc length from the origin to the first measurement=%9.4f\n", arcLength[0]);
            SquareMatrix C1 = new SquareMatrix(3, Cx);
            str = str + C1.toString("covariance matrix for the point");
            str = str + originMomentum.toString("momentum of the particle at closest approach to the origin\n");
            SquareMatrix C2 = new SquareMatrix(3, Cp);
            str = str + C2.toString("covariance matrix for the momentum");
        }
        MeasurementSite site0 = this.SiteList.get(0);
        if (site0.aS != null) {
            str = str + String.format("    Helix at layer %d: %s\n", site0.m.Layer, site0.aS.helix.a.toString());
        }
        for (int i = 0; i < SiteList.size(); i++) {
            MeasurementSite site = SiteList.get(i);
            SiModule m = site.m;
            int hitID = site.hitID;
            str = str + String.format("Layer %d, detector %d, stereo=%b, chi^2 inc.=%10.6f, Xscat=%10.8f Zscat=%10.8f, arc=%10.5f, hit=%d", m.Layer, m.detector, m.isStereo,
                    site.chi2inc, site.scatX(), site.scatZ(), site.arcLength, hitID);
            if (hitID < 0) {
                str = str + "\n";
                continue;
            }
            str = str + String.format(", t=%5.1f", site.m.hits.get(site.hitID).time);
            if (m.hits.get(hitID).tksMC != null) {
                str = str + String.format("  MC tracks: ");
                for (int iMC : m.hits.get(hitID).tksMC) {
                    str = str + String.format(" %d ", iMC);
                }
                str = str + "\n";
            }
            if (interceptVects().containsKey(site)) {
                Vec interceptVec = interceptVects().get(site);
                Vec interceptMomVec = interceptMomVects().get(site);
                double residual = site.m.hits.get(hitID).v - site.aS.mPred;
                Pair<Double, Double> unBiasedResid = unbiasedResidual(site);
                str = str + String.format("    Intercept=%s, p=%s, measurement=%10.5f, predicted=%10.5f, residual=%9.5f, unbiased=%9.5f+-%9.5f, error=%9.5f \n", interceptVec.toString(),
                        interceptMomVec.toString(), site.m.hits.get(hitID).v, site.aS.mPred, residual, unBiasedResid.getFirstElement(), unBiasedResid.getSecondElement(), FastMath.sqrt(site.aS.R));
            }
        }
        str = str + String.format("End of printing for KalTrack %s ID %d in event %d\n\n", s, ID, eventNumber);
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
            if (site.m.Layer < 0) {
                continue;
            }
            double phiS = site.aS.helix.planeIntersect(site.m.p);
            if (Double.isNaN(phiS)) {
                continue;
            }
            Vec rHelixG = site.aS.helix.toGlobal(site.aS.helix.atPhi(phiS));
            Vec rHelixL = site.m.toLocal(rHelixG);
            double residual = -999.;
            if (site.hitID >= 0) {
                residual = site.m.hits.get(site.hitID).v - rHelixL.v[1];
            }
            pW.format(" %10.5f  %10.6f    #  %10.6f\n", rHelixG.v[1], residual, site.aS.r);
        }
        pW.format("EOD\n");
        pW.format("plot $resids with points pt 6 ps 2\n");
        pW.close();
    }

    // Arc length along track from the origin to the first measurement
    public double originArcLength() {
        if (!propagated || arcLength == null) {
            originHelix();
        }
        if (arcLength == null) {
            return 0.;
        }
        return arcLength[0];
    }
  
    public HelixState getHelixAtPlane(Plane xPlane, boolean pivotAtIntersect){
	HelixState helixAtPlane = null; 
	MeasurementSite closeSite = null;
	double delY = 66666.; // y-kalman == z-global
	double planeY = xPlane.X().v[1];
        for (MeasurementSite site : SiteList) {
            SiModule m = site.m;
	    // we want closest site to the requested xPlane
	    if (Math.abs(planeY-m.p.X().v[1])<delY){
		delY =Math.abs(planeY- m.p.X().v[1]);
		closeSite = site;
	    }
        }

	if(closeSite==null){
	    System.out.println(this.getClass().getName()+"::getHelixAtPlane:  Couldn't find a site???");
	}else{
	    if(debug){
		System.out.println(this.getClass().getName()+"::getHelixAtPlane:");
		closeSite.print("closest measurement site");
	    }		    
	}
	double[] arcL = new double[1];
	helixAtPlane = closeSite.aS.helix.propagateRungeKutta(xPlane, yScat, XLscat, closeSite.m.Bfield, arcL, pivotAtIntersect);
        if (debug) {
            System.out.format("KalTrack::getHelixAtPlane: arc length to the first measurement = %9.4f\n", arcL[0]);
        }
	/*        if (covNaN()) {
            return false;
	    }*/	
	return helixAtPlane; 
    }

    // Runge Kutta propagation of the helix to the origin
    public boolean originHelix() {
        if (propagated) {
            return true;
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
        arcLength = new double[1];
        helixAtOrigin = innerSite.aS.helix.propagateRungeKutta(originPlane, yScat, XLscat, innerSite.m.Bfield, arcLength);
        if (debug) {
            System.out.format("KalTrack::originHelix: arc length to the first measurement = %9.4f\n", arcLength[0]);
        }
        if (covNaN()) {
            return false;
        }
        if (!solver.setA(helixAtOrigin.C.copy())) {
            logger.fine("KalTrack:originHelix, cannot invert the covariance matrix");
            for (int i = 0; i < 5; ++i) {      // Fill the matrix and inverse with something not too crazy and continue . . .
                for (int j = 0; j < 5; ++j) {
                    if (i == j) {
                        Cinv.unsafe_set(i, j, 1.0 / Math.abs(helixAtOrigin.C.unsafe_get(i, j)));
                        helixAtOrigin.C.unsafe_set(i, j, Math.abs(helixAtOrigin.C.unsafe_get(i, j)));
                    } else {
                        Cinv.unsafe_set(i, j, 0.);
                        helixAtOrigin.C.unsafe_set(i, j, 0.);
                    }
                }
            }
        } else {
            solver.invert(Cinv);
        }

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
        propagated = true;
        return true;
    }

    public double[] originX() {
        if (!propagated) {
            originHelix();
        }
        if (!propagated) {
            return new double[]{0., 0., 0.};
        }
        return originPoint.v.clone();
    }

    public double[][] originXcov() {
        return Cx.clone();
    }

    public double[][] originPcov() {
        return Cp.clone();
    }

    public double[] originPivot() {
        if (propagated) {
            return helixAtOrigin.X0.v.clone();
        } else {
            return null;
        }
    }

    public double[] originP() {
        if (!propagated) {
            originHelix();
        }
        if (!propagated) {
            return new double[]{0., 0., 0.};
        }
        return originMomentum.v.clone();
    }

    public double[][] originCovariance() {
        if (!propagated) {
            originHelix();
        }
        double[][] M = new double[5][5];
        for (int i = 0; i < 5; ++i) {
            for (int j = 0; j < 5; ++j) {
                if (propagated) {
                    M[i][j] = helixAtOrigin.C.unsafe_get(i, j);
                } else {
                    M[i][j] = SiteList.get(0).aS.helix.C.unsafe_get(i, j);
                }
            }
        }
        return M;
    }

    public boolean covNaN() {
        if (helixAtOrigin.C == null) {
            return true;
        }
        return MatrixFeatures_DDRM.hasNaN(helixAtOrigin.C);
    }

    public double[] originHelixParms() {
        if (propagated) {
            return helixAtOrigin.a.v.clone();
        } else {
            return null;
        }
    }

    //Update the helix parameters at the "origin" by using the target position or vertex as a constraint
    public HelixState originConstraint(double[] vtx, double[][] vtxCov) {
        if (!propagated) {
            originHelix();
        }
        if (!propagated) {
            return null;
        }

        // Transform the inputs in the the helix field-oriented coordinate system
        Vec v = helixAtOrigin.toLocal(new Vec(3, vtx));
        SquareMatrix Cov = helixAtOrigin.Rot.rotate(new SquareMatrix(3, vtxCov));
        Vec X0 = helixAtOrigin.X0;
        double phi = phiDOCA(helixAtOrigin.a, v, X0, alpha);
        /*        if (debug) {  // Test the DOCA algorithm
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
        }*/
        double[][] H = buildH(helixAtOrigin.a, v, X0, phi, alpha);
        Vec pntDOCA = HelixState.atPhi(X0, helixAtOrigin.a, phi, alpha);
        if (debug) {
            matrixPrint("H", H, 3, 5);

            // Derivative test
            HelixState hx = helixAtOrigin.copy();
            double daRel[] = {-0.04, 0.03, -0.16, -0.02, -0.015};
            for (int i = 0; i < 5; ++i) {
                daRel[i] = daRel[i] / 100.;
            }
            for (int i = 0; i < 5; i++) {
                hx.a.v[i] = hx.a.v[i] * (1.0 + daRel[i]);
            }
            Vec da = new Vec(hx.a.v[0] * daRel[0], hx.a.v[1] * daRel[1], hx.a.v[2] * daRel[2], hx.a.v[3] * daRel[3], hx.a.v[4] * daRel[4]);

            double phi2 = phiDOCA(hx.a, v, X0, alpha);
            Vec newX = HelixState.atPhi(X0, hx.a, phi2, alpha);
            Vec dxTrue = newX.dif(pntDOCA);
            dxTrue.print("originConstraint derivative test actual difference");

            Vec dx = new Vec(3);
            for (int i = 0; i < 3; ++i) {
                for (int j = 0; j < 5; ++j) {
                    dx.v[i] += H[i][j] * da.v[j];
                }
            }
            dx.print("difference from H derivative matrix");
            for (int i = 0; i < 3; ++i) {
                double err = 100. * (dxTrue.v[i] - dx.v[i]) / dxTrue.v[i];
                System.out.format("      Coordiante %d: percent difference = %10.6f\n", i, err);
            }
            System.out.println("helix covariance:");
            helixAtOrigin.C.print();
        }
        SquareMatrix Ginv = new SquareMatrix(3);
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                Ginv.M[i][j] = Cov.M[i][j];
                for (int k = 0; k < 5; ++k) {
                    for (int l = 0; l < 5; ++l) {
                        Ginv.M[i][j] += H[i][k] * helixAtOrigin.C.unsafe_get(k, l) * H[j][l];
                    }
                }
            }
        }
        SquareMatrix G = Ginv.fastInvert();
        double[][] K = new double[5][3];  // Kalman gain matrix
        for (int i = 0; i < 5; ++i) {
            for (int j = 0; j < 3; ++j) {
                for (int k = 0; k < 5; ++k) {
                    for (int l = 0; l < 3; ++l) {
                        K[i][j] += helixAtOrigin.C.unsafe_get(i, k) * H[l][k] * G.M[l][j];
                    }
                }
            }
        }
        if (debug) {
            G.print("G");
            matrixPrint("K", K, 5, 3);
        }
        double[] newHelixParms = new double[5];
        double[][] newHelixCov = new double[5][5];
        for (int i = 0; i < 5; ++i) {
            newHelixParms[i] = helixAtOrigin.a.v[i];
            for (int j = 0; j < 3; ++j) {
                newHelixParms[i] += K[i][j] * (vtx[j] - pntDOCA.v[j]);
            }
            for (int j = 0; j < 5; ++j) {
                newHelixCov[i][j] = helixAtOrigin.C.unsafe_get(i, j);
                for (int k = 0; k < 3; ++k) {
                    for (int l = 0; l < 5; ++l) {
                        newHelixCov[i][j] -= K[i][k] * H[k][l] * helixAtOrigin.C.unsafe_get(l, j);
                    }
                }
            }
        }
        // Calculate the chi-squared contribution
        Vec newHelix = new Vec(5, newHelixParms);
        phi = phiDOCA(newHelix, v, X0, alpha);
        SquareMatrix CovInv = Cov.invert();
        pntDOCA = HelixState.atPhi(X0, newHelix, phi, alpha);
        Vec err = pntDOCA.dif(v);
        chi2incVtx = err.dot(err.leftMultiply(CovInv));
        if (debug) {
            // Test alternative formulation
            SquareMatrix Vinv = Cov.invert();
            solver.setA(helixAtOrigin.C);
            solver.invert(Cinv);
            SquareMatrix CinvS = mToS(Cinv);
            for (int i = 0; i < 5; ++i) {
                for (int j = 0; j < 5; ++j) {
                    for (int k = 0; k < 3; ++k) {
                        for (int l = 0; l < 3; ++l) {
                            CinvS.M[i][j] += H[k][i] * Vinv.M[k][l] * H[l][j];
                        }
                    }
                }
            }
            SquareMatrix Calt = CinvS.invert();
            Calt.print("Alternative filtered covariance");
            double[][] Kp = new double[5][3];
            for (int i = 0; i < 5; ++i) {
                for (int j = 0; j < 3; ++j) {
                    for (int k = 0; k < 5; ++k) {
                        for (int l = 0; l < 3; ++l) {
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

    private static void matrixPrint(String s, double[][] A, int M, int N) {
        System.out.format("Dump of %d by %d matrix %s:\n", M, N, s);
        for (int i = 0; i < M; ++i) {
            for (int j = 0; j < N; ++j) {
                System.out.format(" %10.5f", A[i][j]);
            }
            System.out.format("\n");
        }
    }

    //Find the helix turning alpha phi for the point on the helix 'a' closest to the point 'v'
    private static double phiDOCA(Vec a, Vec v, Vec X0, double alpha) {

        Plane p = new Plane(v, new Vec(0., 1., 0.));
        HelixPlaneIntersect hpa = new HelixPlaneIntersect();
        double phiInt = hpa.planeIntersect(a, X0, alpha, p);
        if (Double.isNaN(phiInt)) {
            phiInt = 0.;
        }
        double dphi = 0.1;
        double accuracy = 0.0000001;
        double phiDoca = rtSafe(phiInt, phiInt - dphi, phiInt + dphi, accuracy, a, v, X0, alpha);

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
            Logger.getLogger(KalTrack.class.getName()).log(Level.WARNING, "rtsafe: initial guess needs to be bracketed.");
            return xGuess;
        }
        fl = fDOCA(x1, a, v, X0, alpha);
        fh = fDOCA(x2, a, v, X0, alpha);
        int nTry = 0;
        while (fl * fh > 0.0) {
            if (nTry == 5) {
                Logger.getLogger(KalTrack.class.getName()).log(Level.FINE, String.format("Root is not bracketed in zero finding, fl=%12.5e, fh=%12.5e, alpha=%10.6f, x1=%12.5f x2=%12.5f xGuess=%12.5f",
                        fl, fh, alpha, x1, x2, xGuess));
                return xGuess;
            }
            x1 -= 0.01;
            x2 += 0.01;
            fl = fDOCA(x1, a, v, X0, alpha);
            fh = fDOCA(x2, a, v, X0, alpha);
            nTry++;
        }
        //if (nTry > 0) System.out.format("KalTrack.rtsafe: %d tries needed to bracket solution.\n", nTry);
        if (fl == 0.) {
            return x1;
        }
        if (fh == 0.) {
            return x2;
        }
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
        df = dfDOCAdPhi(rts, a, v, X0, alpha);
        for (int j = 1; j <= MAXIT; j++) {
            if ((((rts - xh) * df - f) * ((rts - xl) * df - f) > 0.0) || (Math.abs(2.0 * f) > Math.abs(dxold * df))) {
                dxold = dx;
                dx = 0.5 * (xh - xl); // Use bisection if the Newton-Raphson method is going bonkers
                rts = xl + dx;
                if (xl == rts) {
                    return rts;
                }
            } else {
                dxold = dx;
                dx = f / df; // Newton-Raphson method
                temp = rts;
                rts -= dx;
                if (temp == rts) {
                    return rts;
                }
            }
            if (Math.abs(dx) < xacc) {
                // System.out.format("KalTrack.rtSafe: solution converged in %d iterations.\n",
                // j);
                return rts;
            }
            f = fDOCA(rts, a, v, X0, alpha);
            df = dfDOCAdPhi(rts, a, v, X0, alpha);
            if (f < 0.0) {
                xl = rts;
            } else {
                xh = rts;
            }
        }
        Logger.getLogger(KalTrack.class.getName()).log(Level.WARNING, "rtsafe: maximum number of iterations exceeded.");
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
        return new Vec((alpha / a.v[2]) * FastMath.sin(a.v[1] + phi), -(alpha / a.v[2]) * FastMath.cos(a.v[1] + phi), -(alpha / a.v[2]) * a.v[4]);
    }

    private static Vec dTangentVecDphi(Vec a, double phi, double alpha) {
        return new Vec((alpha / a.v[2]) * FastMath.cos(a.v[1] + phi), (alpha / a.v[2]) * FastMath.sin(a.v[2] + phi), 0.);
    }

    //Derivative matrix for the helix 'a' point of closet approach to point 'v'
    private static double[][] buildH(Vec a, Vec v, Vec X0, double phi, double alpha) {
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
        double[][] dtda = new double[3][5];
        dtda[0][1] = (alpha / a.v[2]) * FastMath.cos(a.v[1] + phi);
        dtda[0][2] = (-alpha / (a.v[2] * a.v[2])) * FastMath.sin(a.v[1] + phi);
        dtda[1][1] = (alpha / a.v[2]) * FastMath.sin(a.v[1] + phi);
        dtda[1][2] = (alpha / (a.v[2] * a.v[2])) * FastMath.cos(a.v[1] + phi);
        dtda[2][2] = (alpha / (a.v[2] * a.v[2])) * a.v[4];
        dtda[2][4] = -alpha / a.v[2];

        double[][] dxda = new double[3][5];
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
        for (int i = 0; i < 5; ++i) {
            for (int j = 0; j < 3; ++j) {
                dfda.v[i] += -t.v[j] * dxda[j][i] + dfdt.v[j] * dtda[j][i];
            }
            dphida.v[i] = -dfda.v[i] / dfdphi;
        }

        double[][] H = new double[3][5];
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 5; ++j) {
                H[i][j] = dxdphi.v[i] * dphida.v[j] + dxda[i][j];
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
        if (ascending) {
            Collections.sort(SiteList, MeasurementSite.SiteComparatorUp);
        } else {
            Collections.sort(SiteList, MeasurementSite.SiteComparatorDn);
        }
    }

    public boolean removeHit(MeasurementSite site, double mxChi2Inc, double mxTdif) {
        boolean exchange = false;
        if (debug) {
            System.out.format("Event %d track %d remove hit %d on layer %d detector %d\n",
                    eventNumber, ID, site.hitID, site.m.Layer, site.m.detector);
        }
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
        reducedChi2 = chi2 / (double) nHits;
        int oldID = site.hitID;
        site.removeHit();
        // Check whether there might be another hit available                                   
        Measurement addedHit = site.addHit(this, mxChi2Inc, mxTdif, oldID);
        if (addedHit != null) {
            addedHit.tracks.add(this);
            Measurement newHit = site.m.hits.get(site.hitID);
            if(site.m.Layer>kPar.maxInnerLayer){
                tMin = Math.min(tMin, newHit.time);
                tMax = Math.max(tMax, newHit.time);
            }
            exchange = true;
            nHits++;
            if (debug) {
                System.out.format("Event %d track %d added hit %d on layer %d detector %d\n",
                        eventNumber, ID, site.hitID, site.m.Layer, site.m.detector);
            }
        } else {
            //            System.out.println("Removing hit from this Kaltrack...is this necessary? Layer = "+site.m.Layer); 
            SiteList.remove(site);
        }
        return exchange;
    }

    // Try to add missing hits to the track
    public int addHits(ArrayList<SiModule> data, double mxResid, double mxChi2inc, double mxTdif, boolean verbose) {
        int numAdded = 0;
        int numLayers = 14;
        if (nHits == numLayers) {
            return numAdded;
        }

        if (verbose) {
            logger.setLevel(Level.FINER);
        }
        if (debug) {
            System.out.format("addHits, event %d: trying to add hits to track %d\n", eventNumber, ID);
        }

        sortSites(true);

        if (debug) {
            String str = String.format("KalTrac.addHits: initial list of sites: ");
            for (MeasurementSite site : SiteList) {
                str = str + String.format("(%d, %d, %d) ", site.m.Layer, site.m.detector, site.hitID);
            }
            System.out.format("%s\n", str);
        }

        ArrayList<ArrayList<SiModule>> moduleList = new ArrayList<ArrayList<SiModule>>(numLayers);
        for (int lyr = 0; lyr < numLayers; lyr++) {
            ArrayList<SiModule> modules = new ArrayList<SiModule>();
            moduleList.add(modules);
        }
        for (SiModule thisSi : data) {
            if (thisSi.hits.size() > 0) {
                moduleList.get(thisSi.Layer).add(thisSi);
            }
        }

        ArrayList<MeasurementSite> newSites = new ArrayList<MeasurementSite>();
        for (int idx = 0; idx < SiteList.size() - 1; idx++) {
            MeasurementSite site = SiteList.get(idx);
            if (site.hitID < 0) {
                continue;
            }
            int nxtIdx = -1;
            for (int jdx = idx + 1; jdx < SiteList.size(); jdx++) {
                if (SiteList.get(jdx).hitID >= 0) {
                    nxtIdx = jdx;
                    break;
                }
            }
            if (nxtIdx < 0) {
                break;
            }
            MeasurementSite nxtSite = SiteList.get(nxtIdx);
            MeasurementSite siteFrom = site;
            for (int lyr = site.m.Layer + 1; lyr < nxtSite.m.Layer; ++lyr) { // Loop over hitless layers between two sites with hits
                if (debug) {
                    System.out.format("KalTrack.addHits: looking for hits on layer %d\n", lyr);
                }
                for (SiModule module : moduleList.get(lyr)) {
                    MeasurementSite newSite = new MeasurementSite(lyr, module, kPar);
                    
                    double maxDif=kPar.mxTdif;
                    if(newSite.m.Layer<=kPar.maxInnerLayer)
                        maxDif=kPar.mxTdifIn; 
                    double[] tRange = {tMax - maxDif, tMin + maxDif};
                    int rF = newSite.makePrediction(siteFrom.aF, siteFrom.m, -1, false, true, false, tRange, 0, verbose);
                    if (rF == 1) {
                        if (debug) {
                            System.out.format("KalTrack.addHits: predicted chi2inc=%8.3f\n", newSite.chi2inc);
                        }
                        if (newSite.chi2inc < mxChi2inc) {
                            if (newSite.filter()) {
                                if (debug) {
                                    System.out.format("KalTrack.addHits: event %d track %d filtered chi2inc=%8.3f\n", eventNumber, ID, newSite.chi2inc);
                                }
                                if (newSite.chi2inc < mxChi2inc) {
                                    if (debug) {
                                        System.out.format("KalTrack.addHits: event %d added hit %d with chi2inc<%8.3f to layer %d\n",
                                                eventNumber, newSite.hitID, newSite.chi2inc, module.Layer);
                                    }
                                    newSites.add(newSite);
                                    numAdded++;
                                    nHits++;
                                    siteFrom = newSite;
                                    if(newSite.m.Layer>kPar.maxInnerLayer){
                                        double hitTime = newSite.m.hits.get(newSite.hitID).time;
                                        if (hitTime > tMax) {
                                            tMax = hitTime;
                                        } else if (hitTime < tMin) {
                                            tMin = hitTime;
                                        }
                                    }
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
                if (debug) {
                    System.out.format("KalTrack.addHits event %d: added hit %d on layer %d detector %d\n", eventNumber, site.hitID, site.m.Layer, site.m.detector);
                }
                if (siteToDelete != null) {
                    //                    System.out.println("addHits::  Deleting site on layer "+siteToDelete.m.Layer);
                    SiteList.remove(siteToDelete);
                }
                //                System.out.println("addHits::  Adding site on layer "+siteToDelete.m.Layer);
                SiteList.add(site);
            }
            sortSites(true);
            if (debug) {
                String str = String.format("KalTrack.addHits: final list of sites: ");
                for (MeasurementSite site : SiteList) {
                    str = str + String.format("(%d, %d, %d) ", site.m.Layer, site.m.detector, site.hitID);
                }
                System.out.format("%s\n", str);
            }
        } else {
            if (debug) {
                System.out.format("KalTrack.addHits: no hits added in event %d to track %d\n", eventNumber, ID);
            }
        }

        return numAdded;
    }

    // re-fit the track 
    public boolean fit(boolean keep) {
        // keep = true if there might be another recursion after dropping more hits
        double chi2s = 0.;
        if (debug) {
            System.out.format("Entering KalTrack.fit for event %d, track %d\n", eventNumber, ID);
        }
        StateVector sH = SiteList.get(0).aS.copy();
        boolean badC = KalmanPatRecHPS.negativeCov(sH.helix.C);
        if (badC) {
            if (debug) {
                System.out.format("KalTrack.fit: negative starting covariance, event %d track %d\n", eventNumber, ID);
            }
            KalmanPatRecHPS.setInitCov(sH.helix.C, sH.helix.a, false);
        } else {
            CommonOps_DDRM.scale(100., sH.helix.C);  // Blow up the initial covariance matrix to avoid double counting measurements 
        }
        SiModule prevMod = null;
        double chi2f = 0.;
        ArrayList<MeasurementSite> newSiteList = new ArrayList<MeasurementSite>(SiteList.size());
        boolean badCov = false;
        for (int idx = 0; idx < SiteList.size(); idx++) { // Redo all the filter steps
            MeasurementSite currentSite = SiteList.get(idx);
            MeasurementSite newSite = new MeasurementSite(currentSite.m.Layer, currentSite.m, kPar);

            boolean allowSharing = false;
            boolean pickupHits = false;
            boolean checkBounds = false;
            double[] tRange = {-999., 999.};
            if (newSite.makePrediction(sH, prevMod, currentSite.hitID, allowSharing, pickupHits, checkBounds, tRange, 0) < 0) {
                if (debug) {
                    System.out.format("KalTrack.fit: event %d, track %d failed to make prediction at layer %d!\n", eventNumber, ID, newSite.m.Layer);
                }
                return false;
            }
            if (!newSite.filter()) {
                if (debug) {
                    System.out.format("KalTrack.fit: event %d, track %d failed to filter!\n", eventNumber, ID);
                }
                return false;
            }
            if (KalmanPatRecHPS.negativeCov(currentSite.aF.helix.C)) {
                if (debug) {
                    System.out.format("KalTrack: event %d, ID %d, negative covariance after filtering at layer %d\n",
                            eventNumber, ID, currentSite.m.Layer);
                }
                badCov = true;
                KalmanPatRecHPS.fixCov(currentSite.aF.helix.C, currentSite.aF.helix.a);
            }
            if (debug) {
                if (newSite.hitID >= 0) {
                    chi2f += Math.max(currentSite.chi2inc, 0.);
                }
            }
            newSite.hitID = currentSite.hitID;
            sH = newSite.aF;
            if (debug) {
                System.out.format("  Layer %d hit %d filter, chi^2 increment=%10.5f, a=%s\n",
                        newSite.m.Layer, newSite.hitID, newSite.chi2inc, newSite.aF.helix.a.toString());
            }
            prevMod = newSite.m;
            if (keep) {
                currentSite.chi2inc = newSite.chi2inc; // Residuals to cut out hits in next recursion
            }
            newSiteList.add(newSite);
        }
        if (badCov) {
            nBadCov[0]++;
            bad = true;
        }
        if (debug) {
            System.out.format("KalTrack.fit: Track %d, Fit chi^2 after filtering = %12.4e\n", ID, chi2f);
        }

        chi2s = 0.;
        int nNewHits = 0;
        badCov = false;
        MeasurementSite nextSite = null;
        for (int idx = newSiteList.size() - 1; idx >= 0; idx--) {
            MeasurementSite currentSite = newSiteList.get(idx);
            if (nextSite == null) {
                currentSite.aS = currentSite.aF;
                currentSite.smoothed = true;
            } else {
                currentSite.smooth(nextSite);
            }
            if (currentSite.hitID >= 0) {
                chi2s += Math.max(currentSite.chi2inc, 0.);
                nNewHits++;
            }
            if (KalmanPatRecHPS.negativeCov(currentSite.aS.helix.C)) {
                if (debug) {
                    System.out.format("KalTrack: event %d, ID %d, negative covariance after smoothing at layer %d\n",
                            eventNumber, ID, currentSite.m.Layer);
                }
                badCov = true;
                KalmanPatRecHPS.fixCov(currentSite.aS.helix.C, currentSite.aS.helix.a);
            }
            if (debug) {
                System.out.format("  Layer %d hit %d, smooth, chi^2 increment=%10.5f, a=%s\n",
                        currentSite.m.Layer, currentSite.hitID, currentSite.chi2inc, currentSite.aS.helix.a.toString());
            }
            nextSite = currentSite;
        }
        if (badCov) {
            nBadCov[1]++;
            bad = true;
        }
        if (debug) {
            System.out.format("KalTrack.fit: Track %d, Fit chi^2 after smoothing = %12.4e\n", ID, chi2s);
        }
        if (!keep) {
            if (chi2s / (double) nNewHits > reducedChi2 * 1.2) {
                if (debug) {
                    System.out.format("KalTrack.fit event %d track %d: fit chisquared=%10.5f is not an improvement. Discard new fit.\n",
                            eventNumber, ID, chi2s);
                }
                return false;
            }
        }
        SiteList = newSiteList;
        this.chi2 = chi2s;
        this.nHits = nNewHits;
        this.reducedChi2 = chi2s / (double) nNewHits;
        propagated = false;
        if (debug) {
            System.out.format("Exiting KalTrack.fit for event %d, track %d\n", eventNumber, ID);
        }
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
            double penalty1 = 1.0;
            double penalty2 = 1.0;
            if (!t1.SiteList.get(0).aS.helix.goodCov()) {
                penalty1 = 9.9e3;
            }
            if (!t2.SiteList.get(0).aS.helix.goodCov()) {
                penalty2 = 9.9e3;
            }

            Double chi1 = new Double((penalty1 * t1.chi2) / t1.nHits + 300.0 * (1.0 - (double) t1.nHits / 14.));
            Double chi2 = new Double((penalty2 * t2.chi2) / t2.nHits + 300.0 * (1.0 - (double) t2.nHits / 14.));
            return chi1.compareTo(chi2);
        }
    };

    static SquareMatrix mToS(DMatrixRMaj M) {
        SquareMatrix S = new SquareMatrix(M.numRows);
        if (M.numCols != M.numRows) {
            return null;
        }
        for (int i = 0; i < M.numRows; ++i) {
            for (int j = 0; j < M.numRows; ++j) {
                S.M[i][j] = M.unsafe_get(i, j);
            }
        }
        return S;
    }

    @Override
    public boolean equals(Object other) { // Consider two tracks to be equal if they have the same hits
        if (this == other) {
            return true;
        }
        if (!(other instanceof KalTrack)) {
            return false;
        }
        KalTrack o = (KalTrack) other;

        if (this.nHits != o.nHits) {
            return false;
        }
        if (this.SiteList.size() != o.SiteList.size()) {
            return false;
        }

        for (int i = 0; i < SiteList.size(); ++i) {
            MeasurementSite s1 = this.SiteList.get(i);
            MeasurementSite s2 = o.SiteList.get(i);
            if (s1.m.Layer != s2.m.Layer) {
                return false;
            }
            if (s1.m.detector != s2.m.detector) {
                return false;
            }
            if (s1.hitID != s2.hitID) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return nHits + 100 * ID;
    }

    public KalmanParams getKalPars(){
	return kPar; 
    }
}

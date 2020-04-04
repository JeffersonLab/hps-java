package org.hps.recon.tracking.kalman;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

// This class provides an interface between hps-java and the Kalman Filter fitting and pattern recognition code.
// It can be used to refit the hits on an existing hps track, or it can be used to drive the pattern recognition.
// However, both cannot be done at the same time. The interface must be reset between doing one and the other. 

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.gbl.GBLStripClusterData;
import org.hps.util.Pair;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.LCIOParameters.ParameterName;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;

//import static org.lcsim.constants.Constants.fieldConversion;

public class KalmanInterface {
    private Map<Measurement, TrackerHit> hitMap;
    private Map<Measurement, SimTrackerHit> simHitMap;
    private Map<SiModule, SiStripPlane> moduleMap;
    public static RotMatrix HpsSvtToKalman;
    public static RotMatrix KalmanToHpsSvt;
    public static BasicHep3Matrix HpsSvtToKalmanMatrix;
    private ArrayList<KalHit> trackHitsKalman;
    private ArrayList<SiModule> SiMlist;
    private List<Integer> SeedTrackLayers = null;
    private static boolean uniformB;
    private int _siHitsLimit = -1;
    public boolean verbose = false;
    public int verboseLevel = 0;
    double svtAngle;
    private HelixPlaneIntersect hpi;
    KalmanParams kPar;
    Random rnd;
    
    public void setSiHitsLimit(int limit) {
        _siHitsLimit = limit;
    }
    
    public int getSiHitsLimit() {
        return _siHitsLimit;
    }
    
    // Get the HPS tracker hit corresponding to a Kalman hit
    public TrackerHit getHpsHit(Measurement km) {
        return hitMap.get(km);
    }
    
    // Get the HPS sensor that corresponds to a Kalman SiModule
    public HpsSiSensor getHpsSensor(SiModule kalmanSiMod) {
        if (moduleMap == null) return null;
        else {
            SiStripPlane temp = moduleMap.get(kalmanSiMod);
            if (temp == null) return null;
            else return (HpsSiSensor) (temp.getSensor());
        }
    }
    
    // Return the entire map relating HPS sensors to Kalman SiModule objects
    public Map<SiModule, SiStripPlane> getModuleMap() {
        return moduleMap;
    }

    // The HPS field map is in the HPS global coordinate system. This routine includes the transformations
    // to return the field in the Kalman global coordinate system given a coordinate in the same system.
    static Vec getField(Vec kalPos, org.lcsim.geometry.FieldMap hpsFm) {
        // Field map for stand-alone running
        if (FieldMap.class.isInstance(hpsFm)) { return ((FieldMap) (hpsFm)).getField(kalPos); }

        // Standard field map for running in hps-java
        //System.out.format("Accessing HPS field map for position %8.3f %8.3f %8.3f\n", kalPos.v[0], kalPos.v[1], kalPos.v[2]);
        double[] hpsPos = { kalPos.v[0], -1.0 * kalPos.v[2], kalPos.v[1] };
        if (uniformB) {
            hpsPos[0] = 0.;
            hpsPos[1] = 0.;
            hpsPos[2] = 505.57;
        }
 
        double[] hpsField = hpsFm.getField(hpsPos);
        if (uniformB) return new Vec(0., 0., -1.0 * hpsField[1]);
        //if (uniformB) return new Vec(0., 0., 0.5319090951929661);
        return new Vec(hpsField[0], hpsField[2], -1.0 * hpsField[1]);
    }

    // Set the layers to be used for finding seed tracks (not used by Kalman pattern recognition)
    public void setSeedTrackLayers(List<Integer> input) {
        SeedTrackLayers = input;
    }

    public void setVerboseLevel(int input) {
        verboseLevel = input;
    }

    // Constructor with no argument defaults to verbose being turned off
    public KalmanInterface() {
        this(false, false);
    }

    public KalmanInterface(boolean verbose, boolean uniformB) {
        
        if (verbose) {
            System.out.format("Entering the KalmanInterface constructor\n");
        }
        this.verbose = verbose;
        KalmanInterface.uniformB = uniformB;
        hpi = new HelixPlaneIntersect();
        hitMap = new HashMap<Measurement, TrackerHit>();
        simHitMap = new HashMap<Measurement, SimTrackerHit>();
        moduleMap = new HashMap<SiModule, SiStripPlane>();
        trackHitsKalman = new ArrayList<KalHit>();  // Used only to refit existing GBL tracks
        SiMlist = new ArrayList<SiModule>();
        SeedTrackLayers = new ArrayList<Integer>();
        // SeedTrackLayers.add(2);
        SeedTrackLayers.add(3);
        SeedTrackLayers.add(4);
        SeedTrackLayers.add(5);
        
        if (uniformB) {
            System.out.format("KalmanInterface WARNING: the magnetic field is set to a uniform value.\n");
        }
        
        // Transformation from HPS SVT tracking coordinates to Kalman global coordinates
        double[][] HpsSvtToKalmanVals = { { 0, 1, 0 }, { 1, 0, 0 }, { 0, 0, -1 } };
        HpsSvtToKalman = new RotMatrix(HpsSvtToKalmanVals);
        HpsSvtToKalmanMatrix = new BasicHep3Matrix();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++)
                HpsSvtToKalmanMatrix.setElement(i, j, HpsSvtToKalmanVals[i][j]);
        }
        KalmanToHpsSvt = HpsSvtToKalman.invert();
        if (verbose) {
            HpsSvtToKalman.print("HPS tracking to Kalman conversion");
            KalmanToHpsSvt.print("Kalman to HPS tracking conversion");
            Vec zHPS = new Vec(0.,0.,1.);
            Vec xHPS = new Vec(1.,0.,0.);
            Vec yHPS = new Vec(0.,1.,0.);
            HpsSvtToKalman.rotate(xHPS).print("HPS tracking x axis in Kalman coordinates");
            HpsSvtToKalman.rotate(yHPS).print("HPS tracking y axis in Kalman coordinates");
            HpsSvtToKalman.rotate(zHPS).print("HPS tracking z axis in Kalman coordinates");
        }
        
        // Seed the random number generator
        long rndSeed = -3263009337738135404L;
        rnd = new Random();
        rnd.setSeed(rndSeed);
        
        kPar = new KalmanParams();
    }

    // Return the reference to the parameter setting code for the driver to use
    public KalmanParams getKalmanParams() {
        return kPar;
    }
    
    // Transformation from HPS global coordinates to Kalman global coordinates
    public static Vec vectorGlbToKalman(double[] HPSvec) { 
        Vec kalVec = new Vec(HPSvec[0], HPSvec[2], -HPSvec[1]);
        return kalVec;
    }
    
    // Transformation from Kalman global coordinates to HPS global coordinates
    public static double[] vectorKalmanToGlb(Vec KalVec) {
        double[] HPSvec = new double[3];
        HPSvec[0] = KalVec.v[0];
        HPSvec[1] = -KalVec.v[2];
        HPSvec[2] = KalVec.v[1];
        return HPSvec;
    }
    
    // Transformation from Kalman global coordinates to HPS tracking coordinates
    public static double[] vectorKalmanToTrk(Vec KalVec) {
        double[] HPSvec = new double[3];
        HPSvec[0] = KalVec.v[1];
        HPSvec[1] = KalVec.v[0];
        HPSvec[2] = -KalVec.v[2];
        return HPSvec;
    }
    
    // Transformation from HPS tracking coordinates to Kalman global coordinates
    public static Vec vectorTrkToKalman(double[] HPSvec) { 
        Vec kalVec = new Vec(HPSvec[1], HPSvec[0], -HPSvec[2]);
        return kalVec;
    }
    
    // Transformation from HPS sensor coordinates to Kalman sensor coordinates
    public static Vec localHpsToKal(double[] HPSvec) { 
        Vec kalVec = new Vec(-HPSvec[1], HPSvec[0], HPSvec[2]);
        return kalVec;
    }
    
    // Transformation from Kalman sensor coordinates to HPS sensor coordinates
    public static double[] localKalToHps(Vec KalVec) {
        double[] HPSvec = new double[3];
        HPSvec[0] = KalVec.v[1];
        HPSvec[1] = -KalVec.v[0];
        HPSvec[2] = KalVec.v[2];
        return HPSvec;
    }

    // Return the entire list of Kalman SiModule
    public ArrayList<SiModule> getSiModuleList() {
        return SiMlist;
    }

    // Return a list of all measurements for all SiModule
    public ArrayList<Measurement> getMeasurements() {
        ArrayList<Measurement> measList = new ArrayList<Measurement>();;
        for (SiModule SiM : SiMlist) {
            for (Measurement m : SiM.hits) {
                measList.add(m);
            }
        }
        return measList;
    }

    // Clear the event hit and track information without deleting the SiModule geometry information
    public void clearInterface() {
        if (verbose) System.out.println("Clearing the Kalman interface\n");
        hitMap.clear();
        simHitMap.clear();
        trackHitsKalman.clear();
        for (SiModule SiM : SiMlist) {
            SiM.hits.clear();
        }
    }

    // Create an HPS track state from a Kalman track state at the location of a particular SiModule
    public TrackState createTrackState(MeasurementSite ms, int loc, boolean useSmoothed) {
        // public BaseTrackState(double[] trackParameters, double[] covarianceMatrix, double[] position, int location)
        StateVector sv = null;
        if (useSmoothed) {
            if (!ms.smoothed) return null;
            sv = ms.aS;
        } else {
            if (!ms.filtered) return null;
            sv = ms.aF;
        }

        // Local helix params, rotated from the field frame back to the HPS global frame.
        // First pivot transform to the point of intersection of helix with SSD.  
        // Then rotate to the global frame.
        // Then pivot transform back to the origin
        double phiS = sv.planeIntersect(ms.m.p);
        if (Double.isNaN(phiS)) phiS = 0.;
        Vec newPivot = sv.atPhi(phiS);
        Vec localParams = sv.pivotTransform(newPivot);
        // Note: this rotation doesn't totally make sense, as the helix parameters are defined, strictly speaking, 
        // only in a frame in which the B field is the axis of the helix. It's probably okay, though, as long
        // as the parameters are not used to propagate the helix over a large distance.
        SquareMatrix F = sv.makeF(localParams);
        SquareMatrix fRot = new SquareMatrix(5);
        Vec rotatedParams = StateVector.rotateHelix(localParams, sv.Rot.invert(), fRot);
        Vec globalParams = StateVector.pivotTransform(sv.origin.scale(-1.0), rotatedParams, newPivot, sv.alpha, 0.);
        double phiInt3 = hpi.planeIntersect(globalParams, new Vec(0.,0.,0.), sv.alpha, ms.m.p);
        double[] newParams = getLCSimParams(globalParams.v, sv.alpha);
        SquareMatrix F2 = StateVector.makeF(globalParams, rotatedParams, sv.alpha);
        SquareMatrix localCov = sv.C;
        SquareMatrix globalCov = localCov.similarity(F2.multiply(fRot.multiply(F)));
        double[] newCov = getLCSimCov(globalCov.M, sv.alpha).asPackedArray(true);
        if (verbose) {  // The enclosed code is for testing that the transformations made some sense. . .
            System.out.format("KalmanInterface.createTrackState: transforming to the HPS global frame\n");
            sv.X0.print("helix pivot");
            sv.origin.print("origin of local field frame");
            sv.a.print("local helix parameters");
            newPivot.print("new pivot on helix");
            localParams.print("local helix parameters transformed to pivot on helix; should have drho & dz = 0");
            rotatedParams.print("rotated local helix parameters");
            globalParams.print("helix parameters for pivot at the origin");
            F.print("Jacobian of first pivot transform");
            fRot.print("Jacobian for rotation");
            F2.print("Jacobian of second pivot transform");
            localCov.print("original covariance");
            globalCov.print("transformed covariance");
            Plane pTest = new Plane(new Vec(0.,0.,0.),new Vec(0.,1.,0.));            
            double phiInt = hpi.planeIntersect(globalParams, new Vec(0.,0.,0.), sv.alpha, pTest);
            Vec rInt = StateVector.atPhi(new Vec(0.,0.,0.), globalParams, phiInt, sv.alpha);
            Plane pTran = new Plane(sv.toLocal(new Vec(0.,0.,0.)), sv.Rot.rotate(new Vec(0.,1.,0.)));
            pTran.print("y=0 plane in field system");
            double phiInt2 = sv.planeIntersect(pTest);
            Vec rInt2 = sv.atPhi(phiInt2);
            rInt2.print("intersection of the original helix with the y=0 plane in field coordinates");
            System.out.format("The following two points will not match exactly, due to the field tilt\n");
            rInt.print("intersection of the transformed helix with y=0 plane");
            sv.toGlobal(rInt2).print("intersection of the original helix with the y=0 plane in global coordinates");            
            rInt = StateVector.atPhi(new Vec(0.,0.,0.), globalParams, phiInt3, sv.alpha);
            rInt.print("intersection of the global helix with the sensor plane, in global coordinates");
            sv.toGlobal(newPivot).print("intersection of local helix wit the sensor plane, in global coordinates");
        } 

        BaseTrackState ts = new BaseTrackState(newParams, newCov, new double[]{0., 0., 0.}, loc);
        
        // Set phi to be the angle through which the helix turns to reach the SSD plane
        //PF::Do not use a different definition wrt GBL
        //ts.setPhi(phiInt3);
        // Set the reference point (normally defaulted to the origin) to be the intersection point, in HPS tracking coordinates
        ts.setReferencePoint(vectorKalmanToTrk(sv.toGlobal(newPivot)));
        //Compute and store the momentum in the track state
        double [] momtm = ts.computeMomentum(sv.B);
        
        if (verbose && ts != null) {
            System.out.format("KalmanInterface.createTrackState: location=%d layer=%d detector=%d\n", ts.getLocation(), ms.m.Layer, ms.m.detector);
            double [] refpt= ts.getReferencePoint();
            System.out.format("  p=%9.4f %9.4f %9.4f, ref=%9.4f %9.4f %9.4f\n", momtm[0], momtm[1], momtm[2], refpt[0], refpt[1], refpt[2]);
            System.out.format("  phi=%10.6f  tan(lambda)=%10.6f\n", ts.getPhi(), ts.getTanLambda());
            double [] prm = ts.getParameters();
            System.out.format("  Helix parameters=%9.4f %9.4f %9.4f %9.4f %9.4f\n", prm[0], prm[1], prm[2], prm[3], prm[4]);
        }
        return ts;
    }

    public void printGBLStripClusterData(GBLStripClusterData clstr) {
        System.out.format("\nKalmanInterface.printGBLStripClusterData: cluster ID=%d, scatterOnly=%d\n", clstr.getId(), clstr.getScatterOnly());
        System.out.format("  HPS tracking system U=%s\n", clstr.getU().toString());
        System.out.format("  HPS tracking system V=%s\n", clstr.getV().toString());
        System.out.format("  HPS tracking system W=%s\n", clstr.getW().toString());
        System.out.format("  HPS tracking system Track direction=%s\n", clstr.getTrackDirection().toString());
        System.out.format("  phi=%10.6f, lambda=%10.6f\n", clstr.getTrackPhi(), clstr.getTrackLambda());
        System.out.format("  Arc length 2D=%10.5f mm,  Arc length 3D=%10.5f mm\n", clstr.getPath(), clstr.getPath3D());
        System.out.format("  Measurement = %10.5f +- %8.5f mm\n", clstr.getMeas(), clstr.getMeasErr());
        System.out.format("  Track intercept in sensor frame = %s\n", clstr.getTrackPos().toString());
        System.out.format("  RMS projected scattering angle=%10.6f\n", clstr.getScatterAngle());
    }
    
    // Make a GBLStripClusterData object for each MeasurementSite of a Kalman track
    public List<GBLStripClusterData> createGBLStripClusterData(KalTrack kT) {
        List<GBLStripClusterData> rtnList = new ArrayList<GBLStripClusterData>(kT.SiteList.size());
        
        for (MeasurementSite site : kT.SiteList) {
            GBLStripClusterData clstr = new GBLStripClusterData(kT.SiteList.indexOf(site));
            
            // Sites without hits are "scatter-only"
            if (site.hitID < 0) clstr.setScatterOnly(1);
            else clstr.setScatterOnly(0);
            
            // Arc length along helix from the previous site
            clstr.setPath3D(site.arcLength);
            double tanL = site.aS.a.v[4];
            clstr.setPath(site.arcLength/Math.sqrt(1.+tanL*tanL));
            
            // Direction cosines of the sensor axes in the HPS tracking coordinate system
            Hep3Vector u = new BasicHep3Vector(vectorKalmanToTrk(site.m.p.V().scale(-1.0)));
            Hep3Vector v = new BasicHep3Vector(vectorKalmanToTrk(site.m.p.U()));
            Hep3Vector w = new BasicHep3Vector(vectorKalmanToTrk(site.m.p.T()));
            clstr.setU(u);
            clstr.setV(v);
            clstr.setW(w);
            
            // Direction of the track in the HPS tracking coordinate system
            // Find the momentum from the smoothed helix at the sensor location, make it a unit vector, 
            // and then transform from the B-field frame to the Kalman global tracking frame.
            Vec momentum = site.aS.getMom(0.);
            Vec pDir= site.aS.Rot.inverseRotate(momentum.unitVec());
            Hep3Vector trackDir = new BasicHep3Vector(vectorKalmanToTrk(pDir));
            clstr.setTrackDir(trackDir);
            
            // Phi and lambda of the track (assuming standard spherical polar coordinates)
            double phi = Math.atan2(trackDir.y(), trackDir.x());
            double ct = trackDir.z()/trackDir.magnitude();
            double tanLambda = ct/Math.sqrt(1-ct*ct);  // Should be very much the same as tanL above, after accounting for the field tilt
            if (verbose) {
                Vec tilted = site.aS.Rot.inverseRotate(new Vec(0.,0.,1.));
                double tiltAngle = Math.acos(tilted.v[2]);
                System.out.format("KalmanInterface.createGBLStripClusterData: layer=%d det=%d tanL=%10.6f, tanLambda=%10.6f, tilt=%10.6f, sum=%10.6f\n", 
                        site.m.Layer, site.m.detector, -tanL, tanLambda, tiltAngle, tiltAngle+tanLambda);
            }
            clstr.setTrackPhi(phi);
            clstr.setTrackLambda(Math.atan(tanLambda));
            
            // Measured value in the sensor coordinates (u-value in the HPS system)
            double uMeas, uMeasErr;
            if (site.hitID >= 0) {
                uMeas = site.m.hits.get(site.hitID).v; 
                uMeasErr = Math.sqrt(site.aS.R);
            } else {
                uMeas = -999.;
                uMeasErr = -9999.;
            }
            clstr.setMeas(uMeas);
            clstr.setMeasErr(uMeasErr);
            
            // Track position in local frame. First coordinate will be the predicted measurement.
            Vec rGlb = site.aS.toGlobal(site.aS.atPhi(0.));
            Vec rLoc = site.m.toLocal(rGlb);
            Hep3Vector rLocHps = new BasicHep3Vector(localKalToHps(rLoc));
            clstr.setTrackPos(rLocHps);
            
            // rms projected scattering angle
            double ctSensor = pDir.dot(site.m.p.T());
            double XL = Math.abs(site.radLen/ctSensor);
            clstr.setScatterAngle(StateVector.projMSangle(momentum.mag(), XL));
            
            rtnList.add(clstr);
        }
        return rtnList;
    }
    
    // Create an HPS track from a Kalman track
    public BaseTrack createTrack(KalTrack kT, boolean storeTrackStates) {
        if (kT.SiteList == null) {
            System.out.format("KalmanInterface.createTrack: Kalman track is incomplete.\n");
            return null;
        }
        if (kT.covNaN()) {
            System.out.format("KalmanInterface.createTrack: Kalman track has NaN cov matrix. \n");
            return null;
        }
        
        kT.sortSites(true);
        int prevID = 0;
        int dummyCounter = -1;
        BaseTrack newTrack = new BaseTrack();
        
        // Add trackstate at IP as first trackstate,
        // and make this trackstate's params the overall track params
        double[][] globalCov = kT.originCovariance();
        double[] globalParams = kT.originHelixParms();
        double c = 2.99793e8; // Speed of light in m/s
        double conFac = 1.0e12 / c;
        // Field at the origin => For 2016 this is ~ 0.430612 T
        //Vec Bfield = KalmanInterface.getField(new Vec(0.,0.,0.), kT.SiteList.get(0).m.Bfield); 
        //In the center of SVT => For 2016 this is ~ 0.52340 T
        Vec Bfield = KalmanInterface.getField(new Vec(0.,500.,0.), kT.SiteList.get(0).m.Bfield);
        double B = Bfield.mag();
        double alpha = conFac / B; // Convert from pt in GeV to curvature in mm
        double[] newParams = getLCSimParams(globalParams, alpha);
        double[] newCov = getLCSimCov(globalCov, alpha).asPackedArray(true);
        TrackState ts = new BaseTrackState(newParams, newCov, new double[]{0., 0., 0.}, TrackState.AtIP);
        if (ts != null) {
            newTrack.getTrackStates().add(ts);                    
            newTrack.setTrackParameters(ts.getParameters(), B);
            newTrack.setCovarianceMatrix(new SymmetricMatrix(5, ts.getCovMatrix(), true));
        }
        
        // Add the hits to the track
        for (MeasurementSite site : kT.SiteList) {
            if (site.hitID < 0) continue;
            newTrack.addHit(getHpsHit(site.m.hits.get(site.hitID)));
        }
        
        // Get the track states at each layer
        for (int i = 0; i < kT.SiteList.size(); i++) {
            MeasurementSite site = kT.SiteList.get(i);
            ts = null;
            int loc = TrackState.AtOther;

            HpsSiSensor hssd = (HpsSiSensor) moduleMap.get(site.m).getSensor();
            int lay = hssd.getMillepedeId();
            // System.out.printf("ssp id %d \n", hssd.getMillepedeId());

            if (i == 0) {
                loc = TrackState.AtFirstHit;
            } else if (i == kT.SiteList.size() - 1) 
                loc = TrackState.AtLastHit;
            
            if (storeTrackStates) {
                for (int k = 1; k < lay - prevID; k++) {
                    // uses new lcsim constructor
                    BaseTrackState dummy = new BaseTrackState(dummyCounter);
                    newTrack.getTrackStates().add(dummy);
                    dummyCounter--;
                }
                prevID = lay;
            }
                        
            if (loc == TrackState.AtFirstHit || loc == TrackState.AtLastHit || storeTrackStates) {
                ts = createTrackState(site, loc, true);
                if (ts != null) newTrack.getTrackStates().add(ts);
            }
        }
        
        //TODO Ecal extrapolation should be done here [ Currently is done in the PatRecDriver ]
        
        // other track properties
        newTrack.setChisq(kT.chi2);
        newTrack.setNDF(kT.SiteList.size() - 5);
        newTrack.setTrackType(BaseTrack.TrackType.Y_FIELD.ordinal());
        newTrack.setFitSuccess(true);
        
        return newTrack;
    }

    // Convert helix parameters from Kalman to LCSim
    static double[] getLCSimParams(double[] oldParams, double alpha) {        
        double[] params = new double[5];
        params[ParameterName.d0.ordinal()] = oldParams[0];
        params[ParameterName.phi0.ordinal()] = -1.0 * oldParams[1];
        params[ParameterName.omega.ordinal()] = oldParams[2] / alpha * -1.0;
        params[ParameterName.z0.ordinal()] = oldParams[3] * -1.0;
        params[ParameterName.tanLambda.ordinal()] = oldParams[4] * -1.0;
        // System.out.printf("d0 ordinal = %d\n", ParameterName.d0.ordinal());
        // System.out.printf("phi0 ordinal = %d\n", ParameterName.phi0.ordinal());
        // System.out.printf("omega ordinal = %d\n", ParameterName.omega.ordinal());
        // System.out.printf("z0 ordinal = %d\n", ParameterName.z0.ordinal());
        // System.out.printf("tanLambda ordinal = %d\n",
        // ParameterName.tanLambda.ordinal());

        return params;
    }

    // Convert helix parameters from LCSim to Kalman
    static double[] unGetLCSimParams(double[] oldParams, double alpha) {      
        double[] params = new double[5];
        params[0] = oldParams[ParameterName.d0.ordinal()];
        params[1] = -1.0 * oldParams[ParameterName.phi0.ordinal()];
        params[2] = oldParams[ParameterName.omega.ordinal()] * alpha * -1.0;
        params[3] = oldParams[ParameterName.z0.ordinal()] * -1.0;
        params[4] = oldParams[ParameterName.tanLambda.ordinal()] * -1.0;
        return params;
    }

    // Convert helix parameter covariance matrix from Kalman to LCSim
    static SymmetricMatrix getLCSimCov(double[][] oldCov, double alpha) {
        double[] d = { 1.0, -1.0, -1.0 / alpha, -1.0, -1.0 };
        SymmetricMatrix cov = new SymmetricMatrix(5);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                cov.setElement(i, j, d[i] * d[j] * oldCov[i][j]);
            }
        }
        /*
         * for (int i = 0; i <= 2; i++) { for (int j = 0; j <= 2; j++) {
         * cov.setElement(i, j, oldCov.M[i][j]); } } for (int i = 3; i <= 4; i++) { for
         * (int j = 0; j <= 4; j++) { cov.setElement(i, j, oldCov.M[j][i]);
         * cov.setElement(j, i, oldCov.M[i][j]); } }
         */
        return cov;
    }

    // Convert helix parameter covariance matrix from LCSim to Kalman
    static double[][] ungetLCSimCov(double[] oldCov, double alpha) {
        double[] d = { 1.0, -1.0, -1.0 * alpha, -1.0, -1.0 };
        double[][] cov = new double[5][5];
        cov[0][0] = oldCov[0] * d[0] * d[0];
        cov[1][0] = oldCov[1] * d[1] * d[0];
        cov[1][1] = oldCov[2] * d[1] * d[1];
        cov[2][0] = oldCov[3] * d[2] * d[0];
        cov[2][1] = oldCov[4] * d[2] * d[1];
        cov[2][2] = oldCov[5] * d[2] * d[2];
        cov[3][0] = oldCov[6] * d[3] * d[0];
        cov[3][1] = oldCov[7] * d[3] * d[1];
        cov[3][2] = oldCov[8] * d[3] * d[2];
        cov[3][3] = oldCov[9] * d[3] * d[3];
        cov[4][0] = oldCov[10] * d[4] * d[0];
        cov[4][1] = oldCov[11] * d[4] * d[1];
        cov[4][2] = oldCov[12] * d[4] * d[2];
        cov[4][3] = oldCov[13] * d[4] * d[3];
        cov[4][4] = oldCov[14] * d[4] * d[4];
        for (int i = 0; i < 5; ++i) {
            for (int j = i + 1; j < 5; ++j) {
                cov[i][j] = cov[j][i];
            }
        }
        return cov;
    }

    // Used only for Kalman tracks made by fitting hits on a GBL track
    private void addHitsToTrack(BaseTrack newTrack) {
        List<Measurement> measList = getMeasurements();

        for (Measurement meas : measList) {
            TrackerHit hit = hitMap.get(meas);
            if (hit != null) { 
                if (!newTrack.getTrackerHits().contains(hit)) newTrack.addHit(hit); 
            }
        }
        newTrack.setNDF(newTrack.getTrackerHits().size());
    }

    // Create an HPS track from a Kalman seed
    public BaseTrack createTrack(SeedTrack trk) {
        double[] newPivot = { 0., 0., 0. };
        double[] params = getLCSimParams(trk.pivotTransform(newPivot), trk.getAlpha());
        SymmetricMatrix cov = getLCSimCov(trk.covariance().M, trk.getAlpha());
        BaseTrack newTrack = new BaseTrack();
        newTrack.setTrackParameters(params, trk.B());
        newTrack.setCovarianceMatrix(cov);
        addHitsToTrack(newTrack);
        newTrack.setTrackType(BaseTrack.TrackType.Y_FIELD.ordinal());
        newTrack.setFitSuccess(trk.success);

        return newTrack;
    }

    // Method to create one Kalman SiModule object for each silicon-strip detector
    public void createSiModules(List<SiStripPlane> inputPlanes, org.lcsim.geometry.FieldMap fm) {
        if (verbose && verboseLevel > 1) {
            System.out.format("Entering KalmanInterface.creasteSiModules\n");
        }
        
        //2016 => 12 planes, 2019 => 14 planes
        int nPlanes = inputPlanes.size();
        //System.out.printf("PF::nPlanes::%d", nPlanes);
        
        SiMlist.clear();

        for (SiStripPlane inputPlane : inputPlanes) {

            HpsSiSensor temp = (HpsSiSensor) (inputPlane.getSensor());

            double[] uGlb = new double[3];
            double[] vGlb = new double[3];
            double[] tGlb = new double[3];
            for (int row=0; row<3; ++row) {
                uGlb[row] = inputPlane.getSensor().getGeometry().getLocalToGlobal().getRotation().getRotationMatrix().e(row,0);
                vGlb[row] = inputPlane.getSensor().getGeometry().getLocalToGlobal().getRotation().getRotationMatrix().e(row,1);
                tGlb[row] = inputPlane.getSensor().getGeometry().getLocalToGlobal().getRotation().getRotationMatrix().e(row,2);
            }
            Vec uK = (vectorGlbToKalman(vGlb).scale(-1.0));  // u and v are reversed in hps compared to kalman
            Vec vK = vectorGlbToKalman(uGlb);
            Vec tK = vectorGlbToKalman(tGlb);

            double[] pointOnPlane = inputPlane.getSensor().getGeometry().getLocalToGlobal().getTranslation().getTranslationVector().v();
            Vec pointOnPlaneTransformed = vectorGlbToKalman(pointOnPlane);

            if (verbose) {
                System.out.format("\nSiTrackerHit local to global rotation matrix for %s:\n",temp.getName());
                for (int row=0; row<3; ++row) {
                    for (int col=0; col<3; ++col) {
                        System.out.format("  %10.6f", inputPlane.getSensor().getGeometry().getLocalToGlobal().getRotation().getRotationMatrix().e(row,col));
                    }
                    System.out.format("\n");
                }
                System.out.format("SiTrackerHit local to global translation vector for %s: %s\n",temp.getName(),
                        inputPlane.getSensor().getGeometry().getLocalToGlobal().getTranslation().getTranslationVector().toString());     
                uK.print("u in Kalman coordinates");
                vK.print("v in Kalman coordinates");
                tK.print("t in Kalman coordinates");           
                pointOnPlaneTransformed.print("point on plane in Kalman coordinates");
                Vec oldBadPoint = HpsSvtToKalman.rotate(new Vec(3, inputPlane.origin().v()));
                oldBadPoint.print("old, bad point on plane in Kalman coordinates");
                pointOnPlaneTransformed.dif(oldBadPoint).print("difference good - bad");
                System.out.printf("    Building with Kalman plane: point %s normal %s \n",
                        new BasicHep3Vector(pointOnPlaneTransformed.v).toString(), new BasicHep3Vector(tK.v).toString());
            }
            Plane p =new Plane(pointOnPlaneTransformed, tK, uK, vK);
            
            //Indexing valid for 2016 detector
            int kalLayer = temp.getLayerNumber()+1;  
            
            //Indexing valid for 2019 detector -> Include new layer-0, layers go from 0 to 13!!
            if (nPlanes == 40) {
                kalLayer = temp.getLayerNumber()-1;
            }
            
            int detector = temp.getModuleNumber();
            if (kalLayer > 13) {
                System.out.format("***KalmanInterface.createSiModules Warning: Kalman layer %d , tempLayer = %d out of range.***\n", kalLayer,temp.getLayerNumber());
            }
            SiModule newMod = new SiModule(kalLayer, p, temp.isStereo(), inputPlane.getWidth(), inputPlane.getLength(),
                    inputPlane.getThickness(), fm, detector);
            moduleMap.put(newMod, inputPlane);
            SiMlist.add(newMod);
        }
        Collections.sort(SiMlist, new SortByLayer());
    }
    
    // Method to feed simulated hits into the pattern recognition, for testing
    private boolean fillAllSimHits(EventHeader event, IDDecoder decoder) {
        boolean success = false;

        if (verbose || event.getEventNumber() < 50) System.out.format("KalmanInterface.fillAllSimHits: entering for event %d\n", event.getEventNumber());
        
        // Get the collection of 1D hits
        String stripHitInputCollectionName = "TrackerHits";
        if (!event.hasCollection(TrackerHit.class, stripHitInputCollectionName))
            return false;
                    
        List<SimTrackerHit> striphits = event.get(SimTrackerHit.class, stripHitInputCollectionName);

        // Make a mapping from (Layer,Module) to hits
        Map<Pair<Integer,Integer>, ArrayList<SimTrackerHit>> hitSensorMap = new HashMap<Pair<Integer,Integer>, ArrayList<SimTrackerHit>>();
        for (SimTrackerHit hit1D : striphits) {
            //double[] tkMom = hit1D.getMomentum();
            //System.out.format("MC true hit momentum=%10.5f %10.5f %10.5f\n",tkMom[0],tkMom[1],tkMom[2]);
            decoder.setID(hit1D.getCellID());
            int Layer = decoder.getValue("layer") + 1;
            int Module = decoder.getValue("module");
            Pair<Integer,Integer> sensor = new Pair<Integer,Integer>(Layer,Module);

            ArrayList<SimTrackerHit> hitsInSensor = null;
            if (hitSensorMap.containsKey(sensor)) {
                hitsInSensor = hitSensorMap.get(sensor);
            } else {
                hitsInSensor = new ArrayList<SimTrackerHit>();
            }
            hitsInSensor.add(hit1D);
            hitSensorMap.put(sensor, hitsInSensor);
            if (verbose) System.out.format("    Adding hit for layer %d, module %d\n", Layer, Module);
        }

        int hitsFilled = 0;
        for (int modIndex = 0; modIndex < SiMlist.size(); ++modIndex) {
            SiModule module = SiMlist.get(modIndex);
            Pair<Integer,Integer> sensor = new Pair<Integer,Integer>(module.Layer,module.detector);
            if (verbose) System.out.format("   Si module in layer %d module %d, extent=%8.3f to %8.3f\n", module.Layer, module.detector, module.yExtent[0], module.yExtent[1]);

            if (!hitSensorMap.containsKey(sensor)) continue;
            ArrayList<SimTrackerHit> hitsInSensor = hitSensorMap.get(sensor);
            if (hitsInSensor == null) continue;

            for (int i = 0; i < hitsInSensor.size(); i++) {
                SimTrackerHit hit = hitsInSensor.get(i);
                Vec rGlobal = vectorGlbToKalman(hit.getPosition());
                Vec rLocal = module.toLocal(rGlobal);
                //module.Rinv.print("Inverse rot matrix");
                //module.p.print("Si module plane");

                double du = 0.006;
                double umeas = rLocal.v[1] + rnd.nextGaussian()*du;

                if (verbose) {
                    System.out.format("\nKalmanInterface:fillAllSimHits %d, the measurement uncertainty is set to %10.7f\n", i,
                            du);
                    System.out.printf("Filling SiMod Layer %d, detector %d\n", module.Layer, module.detector);
                    module.p.print("Corresponding KalmanPlane");
                    Vec globalX = module.R.rotate(new Vec(1, 0, 0));
                    Vec globalY = module.R.rotate(new Vec(0, 1, 0));
                    globalX.print("globalX");
                    globalY.print("globalY");
                    System.out.format("     Adding measurement %10.5f to layer %d, module %d\n", umeas, module.Layer, module.detector);
                }
                Measurement m = new Measurement(umeas, du, 0., rGlobal, rLocal.v[1]);
                //rGlobal.print("new global hit location");

                module.addMeasurement(m);
                simHitMap.put(m, hit);
                hitsFilled++;
            }
            if (verbose) { module.print("SiModule-filled"); }
        }
        if (hitsFilled > 0) success = true;
        if (verbose) System.out.format("KalmanInterface.fillAllMeasurements: %d hits were filled into Si Modules\n", hitsFilled);

        return success;
    }
       
    // Method to fill all Si hits into the SiModule objects, to feed to the pattern recognition.
    private boolean fillAllMeasurements(EventHeader event) {
        boolean success = false;

        // Get the collection of 1D hits
        String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
        List<TrackerHit> striphits = event.get(TrackerHit.class, stripHitInputCollectionName);
        
        if (_siHitsLimit > 0 && striphits.size() > _siHitsLimit) {
            System.out.println("KalmanInterface::Skip event with " + stripHitInputCollectionName + " > " + String.valueOf(_siHitsLimit));
            return false;
        }

        // Make a mapping from sensor to hits
        Map<HpsSiSensor, ArrayList<TrackerHit>> hitSensorMap = new HashMap<HpsSiSensor, ArrayList<TrackerHit>>();
        for (TrackerHit hit1D : striphits) {
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hit1D.getRawHits().get(0)).getDetectorElement();

            ArrayList<TrackerHit> hitsInSensor = null;
            if (hitSensorMap.containsKey(sensor)) {
                hitsInSensor = hitSensorMap.get(sensor);
            } else {
                hitsInSensor = new ArrayList<TrackerHit>();
            }
            hitsInSensor.add(hit1D);
            hitSensorMap.put(sensor, hitsInSensor);
        }

        int hitsFilled = 0;
        for (int modIndex = 0; modIndex < SiMlist.size(); ++modIndex) {
            SiModule module = SiMlist.get(modIndex);
            SiStripPlane plane = moduleMap.get(module);
            if (!hitSensorMap.containsKey(plane.getSensor())) continue;
            ArrayList<TrackerHit> hitsInSensor = hitSensorMap.get(plane.getSensor());
            if (hitsInSensor == null) continue;            

            for (int i = 0; i < hitsInSensor.size(); i++) {
                TrackerHit hit = hitsInSensor.get(i);

                SiTrackerHitStrip1D localHit = (new SiTrackerHitStrip1D(hit)).getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);

                if (verbose) {
                    System.out.format("\nFilling hits in SiModule for %s\n", plane.getName());
                    SiTrackerHitStrip1D global = (new SiTrackerHitStrip1D(hit)).getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL); 
                    Vec rGlobal = vectorGlbToKalman(global.getPosition());
                    Vec rLocal = module.toLocal(rGlobal);
                    Vec hpsLocal = new Vec(3,localHit.getPosition());
                    rLocal.print("hps global hit transformed to Kalman local frame");
                    hpsLocal.print("hps local hit");
                    
                    /*
                    System.out.format("\nTesting the hps coordinate transformation matrices and displacements for %s\n",plane.getSensor().getName());
                    Vec hitGlobal = new Vec(3,global.getPosition());
                    Vec hitLocal = new Vec(3,localHit.getPosition());
                    System.out.format("SiTrackerHit local to global rotation matrix:\n");
                    RotMatrix lTogRot = new RotMatrix();
                    RotMatrix gTolRot = new RotMatrix();
                    Vec lTogTran = new Vec(3);
                    Vec gTolTran = new Vec(3);
                    for (int row=0; row<3; ++row) {
                        lTogTran.v[row] = plane.getSensor().getGeometry().getLocalToGlobal().getTranslation().getTranslationVector().v()[row];
                        gTolTran.v[row] = plane.getSensor().getGeometry().getGlobalToLocal().getTranslation().getTranslationVector().v()[row];
                        for (int col=0; col<3; ++col) {
                            lTogRot.M[row][col] = plane.getSensor().getGeometry().getLocalToGlobal().getRotation().getRotationMatrix().e(row,col);
                            gTolRot.M[row][col] = plane.getSensor().getGeometry().getGlobalToLocal().getRotation().getRotationMatrix().e(row,col);
                            //System.out.format("  %10.6f", localHit.getLocalToGlobal().getRotation().getRotationMatrix().e(row,col));
                            System.out.format("  %10.6f", plane.getSensor().getGeometry().getLocalToGlobal().getRotation().getRotationMatrix().e(row,col));
                        }
                        System.out.format("\n");
                    }
                    System.out.format("SiTrackerHit local to global translation vector:\n");
                    //System.out.println(localHit.getLocalToGlobal().getTranslation().getTranslationVector().toString());
                    System.out.println(plane.getSensor().getGeometry().getLocalToGlobal().getTranslation().getTranslationVector().toString());
                    lTogRot.print("rotation local to global");
                    lTogTran.print("translation local to global");   
                    Vec newHitGlobal = lTogRot.rotate(hitLocal).sum(lTogTran);
                    hitLocal.print("local hps hit");
                    hitGlobal.print("global hps hit");
                    newHitGlobal.print("transformed local hit");
                    gTolRot.print("rotation global to local");
                    gTolTran.print("translation global to local");
                    hitGlobal.print("global hps hit");
                    hitLocal.print("local hps hit");
                    Vec newHitLocal = gTolRot.rotate(hitGlobal).sum(gTolTran);
                    newHitLocal.print("transformed global hit");
                    newHitLocal = lTogRot.inverseRotate(hitGlobal.dif(lTogTran));
                    newHitLocal.print("transformed global hit");
                    */
                }
                
                double umeas = localHit.getPosition()[0];
                double du = Math.sqrt(localHit.getCovarianceAsMatrix().diagonal(0));
                double time = localHit.getTime();

                // If HPS measured coordinate axis is opposite to Kalman measured coordinate axis
                // This really should not happen, as the Kalman axis is copied directly from the hps geometry.
                Hep3Vector planeMeasuredVec = VecOp.mult(HpsSvtToKalmanMatrix, plane.getMeasuredCoordinate());
                if (planeMeasuredVec.z() * module.p.V().v[2] < 0) {
                    System.out.format("*** KalmanInterface.fillAllMeasurements: flipping Kalman coordinate sign %d! ***\n", i);
                    umeas *= -1.0;
                }

                if (verbose) {
                    System.out.format("\nKalmanInterface:fillAllMeasurements Measurement %d, the measurement uncertainty is set to %10.7f\n", i,
                            du);
                    System.out.printf("Filling SiMod: %s \n", plane.getName());
                    System.out.printf("HPSplane MeasuredCoord %s UnmeasuredCoord %s Normal %s umeas %f\n",
                            plane.getMeasuredCoordinate().toString(), plane.getUnmeasuredCoordinate().toString(), plane.normal().toString(),
                            umeas);
                    System.out.printf(" converted to Kalman Coords  Measured %s Unmeasured %s umeas %f \n", planeMeasuredVec.toString(),
                            VecOp.mult(HpsSvtToKalmanMatrix, plane.getUnmeasuredCoordinate()).toString(), umeas);
                    module.p.print("Corresponding KalmanPlane");
                    Vec globalX = module.R.rotate(new Vec(1, 0, 0));
                    Vec globalY = module.R.rotate(new Vec(0, 1, 0));
                    globalX.print("globalX");
                    globalY.print("globalY");
                }
                Measurement m = new Measurement(umeas, du, time);
                module.addMeasurement(m);
                hitMap.put(m, hit);
                hitsFilled++;
            }
            if (verbose) { module.print("SiModule-filled"); }
        }
        if (hitsFilled > 0) success = true;
        if (verbose) System.out.format("KalmanInterface.fillAllMeasurements: %d hits were filled into Si Modules\n", hitsFilled);
        
        // Add MC truth information to each hit if it is available
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
    
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                    rawtomc.add(relation.getFrom(), relation.getTo());
            }
            for (SiModule mod : SiMlist) {
                for (Measurement hit : mod.hits) {
                    hit.tksMC = new ArrayList<Integer>();
                    TrackerHit hpsHit = getHpsHit(hit);
                    List<RawTrackerHit> rawHits = hpsHit.getRawHits();
                    for (RawTrackerHit rawHit : rawHits) {
                        Set<SimTrackerHit> simHits = rawtomc.allFrom(rawHit);
                        for (SimTrackerHit simHit : simHits) {
                            if (hit.rGlobal == null) hit.rGlobal = vectorGlbToKalman(simHit.getPosition());
                            MCParticle mcp = simHit.getMCParticle();
                            if (!hit.tksMC.contains(mcp.hashCode())) hit.tksMC.add(mcp.hashCode());
                        }
                    }
                }
            }
        }                               
        return success;
    }

    // Method to fill the Si hits into the SiModule objects for a given track, in order to refit the track
    private double fillMeasurements(List<TrackerHit> hits1D, int addMode) {
        double firstZ = 10000;
        Map<HpsSiSensor, ArrayList<TrackerHit>> hitsMap = new HashMap<HpsSiSensor, ArrayList<TrackerHit>>();

        for (TrackerHit hit1D : hits1D) {
            HpsSiSensor temp = ((HpsSiSensor) ((RawTrackerHit) hit1D.getRawHits().get(0)).getDetectorElement());
            int lay = temp.getLayerNumber();
            if (addMode == 0 && !SeedTrackLayers.contains((lay + 1) / 2)) continue;
            else if (addMode == 1 && SeedTrackLayers.contains((lay + 1) / 2)) continue;

            ArrayList<TrackerHit> hitsInLayer = null;
            if (hitsMap.containsKey(temp)) {
                hitsInLayer = hitsMap.get(temp);
            } else {
                hitsInLayer = new ArrayList<TrackerHit>();
            }
            hitsInLayer.add(hit1D);
            if (hit1D.getPosition()[2] < firstZ) firstZ = hit1D.getPosition()[2];
            hitsMap.put(temp, hitsInLayer);
        }

        for (SiModule mod : SiMlist) {
            SiStripPlane plane = moduleMap.get(mod);
            if (!hitsMap.containsKey(plane.getSensor())) { continue; }
            ArrayList<TrackerHit> temp = hitsMap.get(plane.getSensor());
            if (temp == null) { continue; }

            Hep3Vector planeMeasuredVec = VecOp.mult(HpsSvtToKalmanMatrix, plane.getMeasuredCoordinate());

            for (int i = 0; i < temp.size(); i++) {
                TrackerHit hit = temp.get(i);

                SiTrackerHitStrip1D local = (new SiTrackerHitStrip1D(hit)).getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
                // SiTrackerHitStrip1D global = (new
                // SiTrackerHitStrip1D(hit)).getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL);

                double umeas = local.getPosition()[0];
                double du = Math.sqrt(local.getCovarianceAsMatrix().diagonal(0));

                // if hps measured coord axis is opposite to kalman measured coord axis
                // This really should not happen, as the Kalman axis is copied directly from the hps geometry.
                if (planeMeasuredVec.z() * mod.p.V().v[2] < 0) { 
                    System.out.format("*** KalmanInterface.fillMeasurements: flipping Kalman coordinate sign %d! ***\n", i);
                    umeas *= -1.0; 
                }

                if (verbose) {
                    System.out.format("\nKalmanInterface:fillMeasurements Measurement %d, the measurement uncertainty is set to %10.7f\n", i,
                            du);
                    System.out.printf("Filling SiMod: %s \n", plane.getName());
                    System.out.printf("HPSplane MeasuredCoord %s UnmeasuredCoord %s Normal %s umeas %f\n",
                            plane.getMeasuredCoordinate().toString(), plane.getUnmeasuredCoordinate().toString(), plane.normal().toString(),
                            umeas);
                    System.out.printf(" converted to Kalman Coords  Measured %s Unmeasured %s umeas %f \n", planeMeasuredVec.toString(),
                            VecOp.mult(HpsSvtToKalmanMatrix, plane.getUnmeasuredCoordinate()).toString(), umeas);
                    mod.p.print("Corresponding KalmanPlane");
                    Vec globalX = mod.R.rotate(new Vec(1, 0, 0));
                    Vec globalY = mod.R.rotate(new Vec(0, 1, 0));
                    globalX.print("globalX");
                    globalY.print("globalY");
                }
                Measurement m = new Measurement(umeas, du, 0.);

                KalHit hitPair = new KalHit(mod,m);
                trackHitsKalman.add(hitPair);
                mod.addMeasurement(m);
                hitMap.put(m, hit);

            }
            if (verbose) { mod.print("SiModule-filled"); }
        }
        return firstZ;
    }

    // Make a linear fit to a set of hits to be used to initialize the Kalman Filter
    public SeedTrack createKalmanSeedTrack(Track track, RelationalTable hitToStrips, RelationalTable hitToRotated) {
        List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(track, hitToStrips, hitToRotated);
        double firstHitZ = fillMeasurements(hitsOnTrack, 0);
        if (verbose) System.out.printf("firstHitZ %f \n", firstHitZ);
        return new SeedTrack(trackHitsKalman, firstHitZ, verbose);
    }

    // Method to refit an existing track's hits, using the Kalman seed-track to initialize the Kalman Filter.
    public KalmanTrackFit2 createKalmanTrackFit(int evtNumb, SeedTrack seed, Track track, RelationalTable hitToStrips,
            RelationalTable hitToRotated, org.lcsim.geometry.FieldMap fm, int nIt) {
        double firstHitZ = 10000.;
        List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(track, hitToStrips, hitToRotated);
        if (verbose) { System.out.format("createKalmanTrackFit: number of hits on track = %d\n", hitsOnTrack.size()); }
        for (TrackerHit hit1D : hitsOnTrack) {
            if (hit1D.getPosition()[2] < firstHitZ) firstHitZ = hit1D.getPosition()[2];
        }

        ArrayList<SiModule> SiMoccupied = new ArrayList<SiModule>();
        int startIndex = 0;
        fillMeasurements(hitsOnTrack, 1);
        for (SiModule SiM : SiMlist) {
            if (!SiM.hits.isEmpty()) SiMoccupied.add(SiM);
        }
        Collections.sort(SiMoccupied, new SortByLayer());

        for (int i = 0; i < SiMoccupied.size(); i++) {
            SiModule SiM = SiMoccupied.get(i);
            if (SeedTrackLayers.contains((SiM.Layer + 1) / 2) && (i > startIndex)) { startIndex = i; }
            if (verbose) { SiM.print(String.format("SiMoccupied%d", i)); }
        }
        // startIndex++;

        if (verbose) { System.out.printf("createKTF: using %d SiModules, startIndex %d \n", SiMoccupied.size(), startIndex); }

        SquareMatrix cov = seed.covariance();
        cov.scale(1000.0);

        return new KalmanTrackFit2(evtNumb, SiMoccupied, startIndex, nIt, new Vec(0., seed.yOrigin, 0.), seed.helixParams(), cov, fm, verbose);
    }

    // Method to refit an existing track, using the track's helix parameters and covariance to initialize the Kalman Filter.
    public KalmanTrackFit2 createKalmanTrackFit(int evtNumb, Vec helixParams, Vec pivot, SquareMatrix cov, Track track,
            RelationalTable hitToStrips, RelationalTable hitToRotated, org.lcsim.geometry.FieldMap fm, int nIt) {
        List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(track, hitToStrips, hitToRotated);
        if (verbose) { System.out.format("createKalmanTrackFit: using GBL fit as start; number of hits on track = %d\n", hitsOnTrack.size()); }

        ArrayList<SiModule> SiMoccupied = new ArrayList<SiModule>();

        fillMeasurements(hitsOnTrack, 2);
        for (SiModule SiM : SiMlist) {
            if (!SiM.hits.isEmpty()) SiMoccupied.add(SiM);
        }
        Collections.sort(SiMoccupied, new SortByLayer());

        for (int i = 0; i < SiMoccupied.size(); i++) {
            SiModule SiM = SiMoccupied.get(i);
            if (verbose) SiM.print(String.format("SiMoccupied%d", i));
        }

        int startIndex = 0;
        if (verbose) { System.out.printf("createKTF: using %d SiModules, startIndex %d \n", SiMoccupied.size(), startIndex); }
        cov.scale(1000.0);
        return new KalmanTrackFit2(evtNumb, SiMoccupied, startIndex, nIt, pivot, helixParams, cov, fm, verbose);
    }

    // public KalTrack createKalmanTrack(KalmanTrackFit2 ktf, int trackID) {
    // return new KalTrack(trackID, ktf.sites.size(), ktf.sites, ktf.chi2s);
    // }

    class SortByLayer implements Comparator<SiModule> {

        @Override
        public int compare(SiModule o1, SiModule o2) {
            return o1.Layer - o2.Layer;
        }
    }

    // Method to drive the Kalman-Filter based pattern recognition
    public ArrayList<KalmanPatRecHPS> KalmanPatRec(EventHeader event, IDDecoder decoder) {
        if (!fillAllMeasurements(event)) {
            System.out.format("KalmanInterface.KalmanPatRec: recon SVT hits not found for event %d; try sim hits\n",event.getEventNumber());
            if (!fillAllSimHits(event, decoder)) {
                System.out.format("KalmanInterface.KalmanPatRec: failed to fill sim SVT hits for event %d.\n",event.getEventNumber());
                return null;
            }
        }

        int evtNum = event.getEventNumber();
        
        ArrayList<KalmanPatRecHPS> outList = new ArrayList<KalmanPatRecHPS>(2);
        for (int topBottom=0; topBottom<2; ++topBottom) {
            ArrayList<SiModule> SiMoccupied = new ArrayList<SiModule>();
            for (SiModule SiM : SiMlist) {
                if (topBottom == 0) {
                    if (SiM.p.X().v[2] < 0) continue;
                } else {
                    if (SiM.p.X().v[2] > 0) continue;
                }
                if (!SiM.hits.isEmpty()) SiMoccupied.add(SiM);
            }
            Collections.sort(SiMoccupied, new SortByLayer());
            
            if (verbose) {
                for (int i = 0; i < SiMoccupied.size(); i++) {
                    SiModule SiM = SiMoccupied.get(i);
                    SiM.print(String.format("SiMoccupied Number %d for topBottom=%d", i, topBottom));
                }
                System.out.format("KalmanInterface.KalmanPatRec event %d: calling KalmanPatRecHPS for topBottom=%d\n", event.getEventNumber(), topBottom);
            }
            KalmanPatRecHPS kPat = new KalmanPatRecHPS(SiMoccupied, topBottom, evtNum, kPar, verbose);
            outList.add(kPat);
        }
        return outList;
    }
    
    public void plotGBLtracks(String path, EventHeader event) {
        
        String trackCollectionName = "GBLTracks";
        if (!event.hasCollection(Track.class, trackCollectionName)) {
            System.out.format("KalmanInterface.plotGBLtracks: the track collection %s is missing. Abort.\n",trackCollectionName);
            return;
        }
        String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
        if (!event.hasCollection(TrackerHit.class, stripHitInputCollectionName)) {
            System.out.format("KalmanInterface.plotGBLtracks: the hit collection %s is missing. Abort.\n",stripHitInputCollectionName);
            return;
        }
        
        PrintWriter printWriter3 = null;
        int eventNumber = event.getEventNumber();
        String fn = String.format("%sGBLhelix_%d.gp", path, eventNumber);
        System.out.format("KalmanInterface.plotGBLtracks: Outputting single GBL event plot to file %s\n", fn);
        File file3 = new File(fn);
        file3.getParentFile().mkdirs();
        try {
            printWriter3 = new PrintWriter(file3);
        } catch (FileNotFoundException e1) {
            System.out.format("KalmanInterface.plotGBLtracks: could not create the gnuplot output file %s", fn);
            e1.printStackTrace();
            return;
        }
        // printWriter3.format("set xrange [-500.:1500]\n");
        // printWriter3.format("set yrange [-1000.:1000.]\n");
        printWriter3.format("set title 'GBL Event Number %d'\n", eventNumber);
        printWriter3.format("set xlabel 'X'\n");
        printWriter3.format("set ylabel 'Y'\n");
       

        List<Track> tracksGBL = event.get(Track.class, trackCollectionName);
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

        for (Track tkr : tracksGBL) {
            printWriter3.format("$tkp%d << EOD\n", tracksGBL.indexOf(tkr));
            List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(tkr, hitToStrips, hitToRotated);
            for (TrackerHit ht : hitsOnTrack) {                
                double [] pnt = ht.getPosition();
                printWriter3.format(" %10.6f %10.6f %10.6f\n", pnt[0], pnt[2], -pnt[1]);
            }
            printWriter3.format("EOD\n");
        }
        
        List<TrackerHit> stripHits = event.get(TrackerHit.class, stripHitInputCollectionName);
        printWriter3.format("$pnts << EOD\n");
        unUsedHits: for (TrackerHit ht : stripHits) {
            for (Track tkr : tracksGBL) {
                List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(tkr, hitToStrips, hitToRotated);
                for (TrackerHit ht2 : hitsOnTrack) {
                    if (ht2 == ht) continue unUsedHits;
                }
            }
            double [] pnt = ht.getPosition();
            printWriter3.format(" %10.6f %10.6f %10.6f\n", pnt[0], pnt[2], -pnt[1]);
        }
        printWriter3.format("EOD\n");
        
        printWriter3.format("splot $pnts u 1:2:3 with points pt 6 ps 2");
        for (Track tkr : tracksGBL) {
            printWriter3.format(", $tkp%d u 1:2:3 with points pt 7 ps 2", tracksGBL.indexOf(tkr));
        }
        printWriter3.format("\n");
        printWriter3.close();
    }
    // This method makes a Gnuplot file to display the Kalman tracks and hits in 3D.
    public void plotKalmanEvent(String path, EventHeader event, ArrayList<KalmanPatRecHPS> patRecList) {
        
        PrintWriter printWriter3 = null;
        int eventNumber = event.getEventNumber();
        String fn = String.format("%shelix3_%d.gp", path, eventNumber);
        System.out.format("KalmanInterface.plotKalmanEvent: Outputting single event plot to file %s\n", fn);
        File file3 = new File(fn);
        file3.getParentFile().mkdirs();
        try {
            printWriter3 = new PrintWriter(file3);
        } catch (FileNotFoundException e1) {
            System.out.format("KalmanInterface.plotKalmanEvent: could not create the gnuplot output file %s", fn);
            e1.printStackTrace();
            return;
        }
        // printWriter3.format("set xrange [-500.:1500]\n");
        // printWriter3.format("set yrange [-1000.:1000.]\n");
        printWriter3.format("set title 'Event Number %d'\n", eventNumber);
        printWriter3.format("set xlabel 'X'\n");
        printWriter3.format("set ylabel 'Y'\n");
        double vPos = 0.9;
        for (KalmanPatRecHPS patRec : patRecList) {
            for (KalTrack tkr : patRec.TkrList) {
                double [] a = tkr.originHelixParms();
                String s = String.format("TB %d Track %d, %d hits, chi^2=%7.1f, a=%8.3f %8.3f %8.3f %8.3f %8.3f t=%6.1f", 
                        patRec.topBottom, tkr.ID, tkr.nHits, tkr.chi2, a[0], a[1], a[2], a[3], a[4], tkr.getTime());
                printWriter3.format("set label '%s' at screen 0.1, %2.2f\n", s, vPos);
                vPos = vPos - 0.03;
            }
        }
        for (KalmanPatRecHPS patRec : patRecList) {
            for (KalTrack tkr : patRec.TkrList) {
                printWriter3.format("$tkr%d_%d << EOD\n", tkr.ID, patRec.topBottom);
                for (MeasurementSite site : tkr.SiteList) {
                    StateVector aS = site.aS;
                    SiModule module = site.m;
                    if (aS == null) {
                        System.out.println("KalmanInterface.plotKalmanEvent: missing track state pointer.");
                        site.print(" bad site ");
                        continue;
                    }
                    if (module == null) {
                        System.out.println("KalmanInterface.plotKalmanEvent: missing module pointer.");
                        site.print(" bad site ");
                        continue;
                    }
                    double phiS = aS.planeIntersect(module.p);
                    if (Double.isNaN(phiS)) continue;
                    Vec rLocal = aS.atPhi(phiS);
                    Vec rGlobal = aS.toGlobal(rLocal);
                    printWriter3.format(" %10.6f %10.6f %10.6f\n", rGlobal.v[0], rGlobal.v[1], rGlobal.v[2]);
                    // Vec rDetector = m.toLocal(rGlobal);
                    // double vPred = rDetector.v[1];
                    // if (site.hitID >= 0) {
                    // System.out.format("vPredPrime=%10.6f, vPred=%10.6f, v=%10.6f\n", vPred, aS.mPred, m.hits.get(site.hitID).v);
                    // }
                }
                printWriter3.format("EOD\n");
            }

            for (KalTrack tkr : patRec.TkrList) {
                printWriter3.format("$tkp%d_%d << EOD\n", tkr.ID, patRec.topBottom);
                for (MeasurementSite site : tkr.SiteList) {
                    SiModule module = site.m;
                    int hitID = site.hitID;
                    if (hitID < 0) continue;
                    Measurement mm = module.hits.get(hitID);
                    Vec rLoc = null;
                    if (mm.rGlobal == null) {         // If there is no MC truth, use the track intersection for x and z
                        StateVector aS = site.aS;
                        double phiS = aS.planeIntersect(module.p);
                        if (!Double.isNaN(phiS)) {
                            Vec rLocal = aS.atPhi(phiS);        // Position in the Bfield frame
                            Vec rGlobal = aS.toGlobal(rLocal);  // Position in the global frame                 
                            rLoc = module.toLocal(rGlobal);     // Position in the detector frame
                        } else {
                            rLoc = new Vec(0.,0.,0.);
                        }
                    } else {
                        rLoc = module.toLocal(mm.rGlobal); // Use MC truth for the x and z coordinates in the detector frame
                    }
                    Vec rmG = module.toGlobal(new Vec(rLoc.v[0], mm.v, rLoc.v[2]));
                    printWriter3.format(" %10.6f %10.6f %10.6f\n", rmG.v[0], rmG.v[1], rmG.v[2]);
                }
                printWriter3.format("EOD\n");
            }
        }
        printWriter3.format("$pnts << EOD\n");
        for (SiModule si : SiMlist) {
            for (Measurement mm : si.hits) {
                if (mm.tracks.size() > 0) continue;
                Vec rLoc = null;
                if (mm.rGlobal == null) {
                    rLoc = new Vec(0.,0.,0.);      // Use the center of the detector if there is no MC truth info
                } else {
                    rLoc = si.toLocal(mm.rGlobal); // Use MC truth for the x and z coordinates in the detector frame
                }
                Vec rmG = si.toGlobal(new Vec(rLoc.v[0], mm.v, rLoc.v[2]));
                printWriter3.format(" %10.6f %10.6f %10.6f\n", rmG.v[0], rmG.v[1], rmG.v[2]);
            }
        }
        printWriter3.format("EOD\n");
        printWriter3.format("splot $pnts u 1:2:3 with points pt 6 ps 2");
        for (KalmanPatRecHPS patRec : patRecList) {
            for (KalTrack tkr : patRec.TkrList) { printWriter3.format(", $tkr%d_%d u 1:2:3 with lines lw 3", tkr.ID, patRec.topBottom); }
            for (KalTrack tkr : patRec.TkrList) { printWriter3.format(", $tkp%d_%d u 1:2:3 with points pt 7 ps 2", tkr.ID, patRec.topBottom); }
        }
        printWriter3.format("\n");
        printWriter3.close();
    }
}

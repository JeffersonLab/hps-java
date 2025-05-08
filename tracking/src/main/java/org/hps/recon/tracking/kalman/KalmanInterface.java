package org.hps.recon.tracking.kalman;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import java.lang.reflect.Array;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import org.apache.commons.math.util.FastMath;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.gbl.GBLStripClusterData;
import org.apache.commons.math3.util.Pair;
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
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.ShaperFitAlgorithm;
import org.hps.recon.tracking.ShaperPileupFitAlgorithm;
import org.hps.recon.tracking.PulseShape;
import org.hps.recon.tracking.ShapeFitParameters;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtSyncStatus.SvtSyncStatusCollection;
import org.hps.conditions.svt.SvtTimingConstants;
import org.hps.readout.ecal.ReadoutTimestamp;
import org.hps.readout.svt.HPSSVTConstants;
import org.hps.recon.ecal.cluster.TriggerTime;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.recon.cat.util.Const;
import org.lcsim.util.Driver;
import org.hps.recon.tracking.BeamlineConstants; 
/**
 *  This class provides an interface between hps-java and the Kalman Filter fitting and pattern recognition code.
 *  It can be used to refit the hits on an existing hps track, or it can be used to drive the pattern recognition.
 *  However, both cannot be done at the same time. The interface must be reset between doing one and the other. 
 */
public class KalmanInterface {
    private Detector det;
    private List<HpsSiSensor> sensors;
    private Map<Measurement, TrackerHit> hitMap;
    private Map<Measurement, SimTrackerHit> simHitMap;
    private Map<SiModule, SiStripPlane> moduleMap;
    private ArrayList<KalHit> trackHitsKalman;
    private ArrayList<SiModule> SiMlist;
    private List<Integer> SeedTrackLayers = null;
    private int _siHitsLimit = -1;
    double alphaCenter;
    private List<SiStripPlane> detPlanes;
    double svtAngle;
    org.lcsim.geometry.FieldMap fM;
    KalmanParams kPar;
    private KalmanPatRecHPS kPat;
    Random rnd;
    private static Logger logger;
    public static RotMatrix HpsSvtToKalman;
    public static RotMatrix KalmanToHpsSvt;
    public static BasicHep3Matrix HpsSvtToKalmanMatrix;
    private static boolean uniformB;
    private static DMatrixRMaj tempM;
    private static DMatrixRMaj Ft;
    private int maxHits;
    private int nBigEvents;
    private int eventNumber;
    private static double target_pos = -999.9;
    private static boolean addTrackStateAtTarget = false;
    private double[] beamPosition = null;
    
    private static final boolean debug = false;    
    static final double SVTcenter = 505.57;
    private static final double c = 2.99793e8; // Speed of light in m/s
    static double conFac= 1.0e12 / c;
    private int runNumber = 14168;
    
    public void setRunNumber(int runNumber){
        this.runNumber = runNumber;
    }
    
    public void setSiHitsLimit(int limit) {
        _siHitsLimit = limit;
    }
    
    public int getSiHitsLimit() {
        return _siHitsLimit;
    }

    public void setTargetPosition(double target_pos){
        this.target_pos = target_pos;   
    }

    public void setAddTrackStateAtTarget(boolean input){
        this.addTrackStateAtTarget = input;
    }

    public void setBeamPosition(double[] beamPosition){
        this.beamPosition = beamPosition;
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
        return new Vec(3, getFielD(kalPos, hpsFm));
    }
    
    static double [] getFielD(Vec kalPos, org.lcsim.geometry.FieldMap hpsFm) {
        // Field map for stand-alone running
        if (FieldMap.class.isInstance(hpsFm)) return ((FieldMap) (hpsFm)).getField(kalPos);

        // Standard field map for running in hps-java
        //System.out.format("Accessing HPS field map for position %8.3f %8.3f %8.3f\n", kalPos.v[0], kalPos.v[1], kalPos.v[2]);
        double[] hpsPos = { kalPos.v[0], -1.0 * kalPos.v[2], kalPos.v[1] };
        if (uniformB) {
            hpsPos[0] = 0.;
            hpsPos[1] = 0.;
            hpsPos[2] = SVTcenter;
        } else {
            if (hpsPos[1] > 70.0) hpsPos[1] = 70.0;   // To avoid getting a field returned that is identically equal to zero
            if (hpsPos[1] < -70.0) hpsPos[1] = -70.0;
            if (hpsPos[0] < -225.) hpsPos[0] = -225.;
            if (hpsPos[0] > 270.) hpsPos[0] = 270.;
        }
        double[] hpsField = hpsFm.getField(hpsPos);
        if (uniformB) {
            double [] kalField = {0., 0., -1.0 * hpsField[1]};
            return kalField;
        }
        double [] kalField = {hpsField[0], hpsField[2], -1.0 * hpsField[1]};
        return kalField;
    }

    // Set the layers to be used for finding seed tracks (not used by Kalman pattern recognition)
    public void setSeedTrackLayers(List<Integer> input) {
        SeedTrackLayers = input;
    }

    // Constructor with no uniformB argument defaults to non-uniform field
    public KalmanInterface(KalmanParams kPar, org.lcsim.geometry.FieldMap fM) {
        this(false, kPar, fM);
    }

    public KalmanInterface(boolean uniformB, KalmanParams kPar, org.lcsim.geometry.FieldMap fM) {
        
        this.det = det;
        this.sensors = sensors;
        this.fM = fM;
        this.kPar = kPar;
        logger = Logger.getLogger(KalmanInterface.class.getName());
        logger.info("Entering the KalmanInterface constructor");
        maxHits = 0;
        nBigEvents = 0;
        
        tempM = new DMatrixRMaj(5,5);
        Ft = new DMatrixRMaj(5,5);
        
        KalmanInterface.uniformB = uniformB;
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
            logger.log(Level.WARNING, "KalmanInterface WARNING: the magnetic field is set to a uniform value.");
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
        if (debug) {
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
        
        kPat = new KalmanPatRecHPS(kPar);
        
        Vec centerB = KalmanInterface.getField(new Vec(0., SVTcenter, 0.), fM);
        alphaCenter = conFac/ centerB.mag();
    }
    
    public void summary() {
        System.out.format("KalmanInterface::summary: number of events with > 200 hits=%d.\n", nBigEvents);
        System.out.format("                          Maximum event size = %d strip hits.\n", maxHits);
        System.out.format("                          Events with > %d hits were not processed.\n", _siHitsLimit);
        System.out.format("                          Number of tracks with bad covariance in filterTrack= %d %d\n", KalmanPatRecHPS.nBadCov[0], KalmanPatRecHPS.nBadCov[1]);
        System.out.format("                          Number of tracks with bad covariance in KalTrack.fit=%d %d\n", KalTrack.nBadCov[0], KalTrack.nBadCov[1]);
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
        logger.fine("Clearing the Kalman interface");
        hitMap.clear();
        simHitMap.clear();
        trackHitsKalman.clear();
        for (SiModule SiM : SiMlist) {
            SiM.hits.clear();
        }
    }

    // Create an HPS TrackState from a Kalman HelixState at the location of a particular SiModule
    //    public TrackState createTrackState(MeasurementSite ms, int loc, boolean useSmoothed) {
    public TrackState createTrackState(MeasurementSite ms, int loc, boolean useSmoothed, boolean pivotAtIntercept) {
        // Note that the helix parameters that get stored in the TrackState assume a B-field exactly oriented in the
        // z direction and a pivot point at the origin (0,0,0). The referencePoint of the TrackState is set to the
        // intersection point with the detector plane.
        StateVector sv = null;
        if (useSmoothed) {
            if (!ms.smoothed) return null;
            sv = ms.aS;
        } else {   // using the filtered state is really not recommended
            if (!ms.filtered) return null;
            sv = ms.aF;
        }

	//        return sv.helix.toTrackState(alphaCenter, ms.m.p, loc);
        return sv.helix.toTrackState(alphaCenter, ms.m.p, loc,pivotAtIntercept);
    }

    public double[][] getCovarianceFromHelix(HelixState helix) {
        double[][] M = new double[5][5];
	
        for (int i = 0; i < 5; ++i) {
            for (int j = 0; j < 5; ++j) {
		M[i][j] = helix.C.unsafe_get(i, j);
            }
        }
        return M;
    }
    
    // Transform a Kalman helix to an HPS helix rotated to the global frame and with the pivot at the origin
    // Provide covHPS with 15 elements to get the covariance as well
    // Provide 3-vector position to get the location in HPS global coordinates of the original helix pivot
    static double [] toHPShelix(HelixState helixState, Plane pln, double alphaCenter, double [] covHPS, double[] position, boolean pivotAtIntercept) {
        final boolean debug = true;
        double phiInt = helixState.planeIntersect(pln);
        if (Double.isNaN(phiInt)) {
            Logger logger = Logger.getLogger(KalmanInterface.class.getName());
            logger.fine(String.format("toHPShelix: no intersection with the plane at %s",pln.toString()));
            phiInt = 0.;
        }
        // Transforms helix to a pivot point on the helix itself (so rho0 and z0 become zero)
        Vec newPivot = helixState.atPhi(phiInt);
        Vec helixParamsPivoted = helixState.pivotTransform(newPivot);
        DMatrixRMaj F = new DMatrixRMaj(5,5);
        helixState.makeF(helixParamsPivoted, F);
        if (debug) {
            System.out.format("Entering KalmanInterface.toHPShelix");
            helixState.print("provided");
            pln.print("provided");
            newPivot.print("new pivot");
            helixParamsPivoted.print("pivoted helix params");
            System.out.format("turning angle to the plane containing the helixState origin=%10.6f\n", phiInt);
            Vec intGlb = helixState.toGlobal(newPivot);
            intGlb.print("global intersection with plane");
        }
        
        // Then rotate the helix to the global system. This isn't quite kosher, since the B-field will not
        // be aligned with the global system in general, but we have to do it to fit back into the HPS TrackState
        // coordinate convention, for which the field is assumed to be uniform and aligned.
        DMatrixRMaj fRot = new DMatrixRMaj(5,5);
        Vec helixParamsRotated = HelixState.rotateHelix(helixParamsPivoted, helixState.Rot.invert(), fRot);
        CommonOps_DDRM.mult(fRot, F, Ft);             
        
        CommonOps_DDRM.multTransB(helixState.C, Ft, tempM);
        DMatrixRMaj covRotated = new DMatrixRMaj(5,5);
        CommonOps_DDRM.mult(Ft, tempM, covRotated);
        if (debug) helixParamsRotated.print("rotated helix params");
        
        // Transform the pivot to the global system. 
        Vec pivotGlobal = helixState.toGlobal(newPivot);
        if (debug) pivotGlobal.print("pivot in global system");
        
        // Pivot transform to the final pivot at the origin
        Vec finalPivot = new Vec(0.,0.,0.);
        Vec finalHelixParams = HelixState.pivotTransform(finalPivot, helixParamsRotated, pivotGlobal, alphaCenter, 0.);
        HelixState.makeF(finalHelixParams, F, helixParamsRotated, alphaCenter);
        CommonOps_DDRM.multTransB(covRotated, F, tempM);
        CommonOps_DDRM.mult(F, tempM, covRotated);
        if (debug) {
            finalPivot.print("final pivot point");
            finalHelixParams.print("final helix parameters");
            HelixPlaneIntersect hpi = new HelixPlaneIntersect();
            phiInt = hpi.planeIntersect(finalHelixParams, finalPivot, alphaCenter, pln);
            if (!Double.isNaN(phiInt)) {
                Vec rInt = HelixState.atPhi(finalPivot, finalHelixParams, phiInt, alphaCenter);
                rInt.print("final helix intersection with given plane");
            }
            System.out.format("Exiting KalmanInterface.toHPShelix\n");
        }
        if (covHPS != null) {
            double [] temp = KalmanInterface.getLCSimCov(covRotated, alphaCenter).asPackedArray(true);
            for (int i=0; i<15; ++i) covHPS[i] = temp[i];
        }
        if (position != null) {
            double [] temp = KalmanInterface.vectorKalmanToGlb(pivotGlobal);
            for (int i=0; i<3; ++i) position[i] = temp[i];
        }
        return KalmanInterface.getLCSimParams(finalHelixParams.v, alphaCenter);
    }
    
    //    static TrackState toTrackState(HelixState helixState, Plane pln, double alphaCenter, int loc) {
    static TrackState toTrackState(HelixState helixState, Plane pln, double alpha, int loc, boolean pivotAtIntercept) {
        double [] covHPS = new double[15];
        double [] position = new double[3];
        double [] helixHPS = KalmanInterface.toHPShelix(helixState, pln, alpha, covHPS, position, pivotAtIntercept);
	//position is filled in toHPShelix and used at hte reference in the track state
	//        return new BaseTrackState( helixHPS, covHPS, position, loc);
	double bLocal=conFac/alpha; 
        return new BaseTrackState( helixHPS,  position, covHPS, loc, bLocal); 
    }
    
    public void printGBLStripClusterData(GBLStripClusterData clstr) {
        System.out.format("\nKalmanInterface.printGBLStripClusterData: cluster ID=%d, scatterOnly=%d\n", clstr.getId(), clstr.getScatterOnly());
        Pair<Integer, Integer> IDdecode = TrackUtils.getLayerSide(clstr.getVolume(), clstr.getId());
        System.out.format("  Volume = %d Layer = %d Detector = %d\n", clstr.getVolume(), IDdecode.getFirst(), IDdecode.getSecond());
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

        //Arc length from origin to first plane
        
        double phi_org         = kT.originHelixParms()[1];
        double phi_1state      = kT.SiteList.get(0).aS.helix.a.v[1]; 
        double alpha           = kT.helixAtOrigin.alpha;
        double radius          = Math.abs(alpha/kT.originHelixParms()[2]);
        double arcLength2D     = radius*(phi_1state-phi_org);
        double arcLength       = arcLength2D*Math.sqrt(1.0 + kT.originHelixParms()[4] * kT.originHelixParms()[4]);

        double total_s3D = arcLength;
        double total_s2D = arcLength2D;
        
        double phiLast = 9999.;

        for (MeasurementSite site : kT.SiteList) {
            GBLStripClusterData clstr = new GBLStripClusterData(site.m.millipedeID);
            clstr.setVolume(site.m.topBottom);
            
            // Sites without hits are "scatter-only"
            if (site.hitID < 0) clstr.setScatterOnly(1);
            else clstr.setScatterOnly(0);
            
            // Store the total Arc length in the GBLStripClusterData
            total_s3D += site.arcLength;
            clstr.setPath3D(total_s3D);
            double tanL = site.aS.helix.a.v[4];
            clstr.setPath(site.arcLength/FastMath.sqrt(1.+tanL*tanL));
            
            Hep3Vector u = new BasicHep3Vector(vectorKalmanToTrk(site.m.p.V()));
            Hep3Vector v = new BasicHep3Vector(vectorKalmanToTrk(site.m.p.U().scale(-1.0)));
            Hep3Vector w = new BasicHep3Vector(vectorKalmanToTrk(site.m.p.T()));
            
            clstr.setU(u);
            clstr.setV(v);
            clstr.setW(w);
            
            // Direction of the track in the HPS tracking coordinate system
            // Find the momentum from the smoothed helix at the sensor location, make it a unit vector, 
            // and then transform from the B-field frame to the Kalman global tracking frame.
            Vec momentum = site.aS.helix.getMom(0.);
            Vec pDir= site.aS.helix.Rot.inverseRotate(momentum.unitVec());
            Hep3Vector trackDir = new BasicHep3Vector(vectorKalmanToTrk(pDir));
            clstr.setTrackDir(trackDir);
            
            // Phi and lambda of the track (assuming standard spherical polar coordinates)
            double phi = FastMath.atan2(trackDir.y(), trackDir.x());
            double ct = trackDir.z()/trackDir.magnitude();
            double tanLambda = ct/FastMath.sqrt(1-ct*ct);  // Should be very much the same as tanL above, after accounting for the field tilt
            if (debug) {
                Vec tilted = site.aS.helix.Rot.inverseRotate(new Vec(0.,0.,1.));
                double tiltAngle = FastMath.acos(tilted.v[2]);
                System.out.format("KalmanInterface.createGBLStripClusterData: layer=%d det=%d tanL=%10.6f, tanLambda=%10.6f, tilt=%10.6f, sum=%10.6f\n", 
                        site.m.Layer, site.m.detector, -tanL, tanLambda, tiltAngle, tiltAngle+tanLambda);
            }
            clstr.setTrackPhi(phi);
            if (phiLast < 10.) {
                if (Math.abs(phi - phiLast) > 1.2) System.out.format("Big phi change in event %d\n", eventNumber);
            }
            phiLast = phi;
            clstr.setTrackLambda(FastMath.atan(tanLambda));
            
            // Measured value in the sensor coordinates (u-value in the HPS system)
            double uMeas, uMeasErr;
            if (site.hitID >= 0) {
                uMeas = site.m.hits.get(site.hitID).v; 
                uMeasErr = site.m.hits.get(site.hitID).sigma;
                //This is the un-biased residual error (post-fit)
                //uMeasErr = Math.sqrt(site.aS.R);

            } else {
                uMeas = -999.;
                uMeasErr = -9999.;
            }
            clstr.setMeas(uMeas);
            clstr.setMeasErr(uMeasErr);
            
            // Track position in local frame. First coordinate will be the predicted measurement.
            Vec rGlb = site.aS.helix.toGlobal(site.aS.helix.atPhi(0.));
            Vec rLoc = site.m.toLocal(rGlb);
            Hep3Vector rLocHps = new BasicHep3Vector(localKalToHps(rLoc));
            clstr.setTrackPos(rLocHps);
            
            // rms projected scattering angle
            double ctSensor = pDir.dot(site.m.p.T());
            double XL = Math.abs((site.m.thickness / site.radLen) / ctSensor);
            clstr.setScatterAngle(HelixState.projMSangle(momentum.mag(), XL));
            
            rtnList.add(clstr);
        }
        return rtnList;
    }

    // Propagate a TrackState "stateHPS" to a plane given by "location" and "direction".
    // The PropagatedTrackState object created includes the new propagated TrackState plus information on
    // the intersection point of the track with the plane.
    public PropagatedTrackState propagateTrackState(TrackState stateHPS, double [] location, double [] direction) {
        return new PropagatedTrackState(stateHPS, location, direction, detPlanes, fM);
    }

    // Create an HPS track from a Kalman track
    public BaseTrack createTrack(KalTrack kT, boolean storeTrackStates) {
        if (kT.SiteList == null) {
            logger.log(Level.WARNING, "KalmanInterface.createTrack: Kalman track is incomplete.");
            return null;
        }
        if (kT.covNaN()) {
            logger.log(Level.FINE, "KalmanInterface.createTrack: Kalman track has NaN cov matrix.");
            return null;
        }
        
        kT.sortSites(true);
        BaseTrack newTrack = new BaseTrack();
        
        // Add trackstate at IP as first trackstate,
        // and make this trackstate's params the overall track params
        DMatrixRMaj globalCov = new DMatrixRMaj(kT.originCovariance());
        double[] globalParams = kT.originHelixParms();
	Vec origin=kT.helixAtOrigin.origin;
        // To get the LCSIM curvature parameter, we want the curvature at the center of the SVT (more-or-less the
        // center of the magnet), so we need to use the magnetic field there to convert from 1/pt to curvature.
        // Field at the origin  => For 2016 this is ~ 0.430612 T
        // In the center of SVT => For 2016 this is ~ 0.52340 T
	//        Vec Bfield = KalmanInterface.getField(new Vec(0., SVTcenter ,0.), kT.SiteList.get(0).m.Bfield);
        Vec Bfield = KalmanInterface.getField(origin , kT.SiteList.get(0).m.Bfield);
        double B = Bfield.mag();
	double alpha = conFac/ B;
	//        double[] newParams = getLCSimParams(globalParams, alphaCenter);
	double[] newParams = getLCSimParams(globalParams, alpha);
	//        double[] newCov = getLCSimCov(globalCov, alphaCenter).asPackedArray(true);
        double[] newCov = getLCSimCov(globalCov, alpha).asPackedArray(true);
	double[] refD=new double[]{origin.v[0],origin.v[2],-origin.v[1]};
        TrackState ts = new BaseTrackState(newParams, refD,newCov,  TrackState.AtIP);
        if (ts != null) {
            newTrack.getTrackStates().add(ts);                    
            newTrack.setTrackParameters(ts.getParameters(), B);
            newTrack.setCovarianceMatrix(new SymmetricMatrix(5, ts.getCovMatrix(), true));
	    System.out.println("Track state at perigee :    " +ts.toString());
        }
        // Add the hits to the track
	int firstHit_idx = -1;
	int lastHit_idx = -1;
	int idx = -1;

	int hitPattern[] = {0,0,0,0,0,0,0,0,0,0,0,0,0,0};

        for (MeasurementSite site : kT.SiteList) {
	    idx = idx + 1;
            if (site.hitID < 0) continue;
	    if (firstHit_idx < 0) firstHit_idx = idx;
	    lastHit_idx = idx;

	    TrackerHit ht = this.getHpsHit(site.m.hits.get(site.hitID));
	    List<RawTrackerHit> rawHits = ht.getRawHits();
	    for (RawTrackerHit rawHit : rawHits) {
              HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement();
	      Array.set(hitPattern,sensor.getLayerNumber()-1,1);
	    }
            newTrack.addHit(ht);
        }

        // Get the track states at each layer
        for (int i = 0; i < kT.SiteList.size(); i++) {
            MeasurementSite site = kT.SiteList.get(i);
            ts = null;
            int loc = TrackState.AtOther;

            //HpsSiSensor hssd = (HpsSiSensor) moduleMap.get(site.m).getSensor();
            //int lay = hssd.getMillepedeId();
            // System.out.printf("ssp id %d \n", hssd.getMillepedeId());

            if (i == firstHit_idx) {
                loc = TrackState.AtFirstHit;
            } else if (i == lastHit_idx) 
                loc = TrackState.AtLastHit;
            
            /*
              //DO Not att the missing layer track states yet.
            if (storeTrackStates) {
                for (int k = 1; k < lay - prevID; k++) {
                    // uses new lcsim constructor
                    BaseTrackState dummy = new BaseTrackState(dummyCounter);
                    newTrack.getTrackStates().add(dummy);
                    dummyCounter--;
                }
                prevID = lay;
            }
            */
                        
            if (loc == TrackState.AtFirstHit || loc == TrackState.AtLastHit || storeTrackStates) {
		//                ts = createTrackState(site, loc, true);
                ts = createTrackState(site, loc, true,true);
                if (ts != null) newTrack.getTrackStates().add(ts);
		System.out.println(ts.toString());
            }
        }

	double zAtEcal = BeamlineConstants.ECAL_FACE;
        if (4441 < runNumber && runNumber < 8100)
            zAtEcal = BeamlineConstants.ECAL_FACE_ENGINEERING_RUNS;
	Vec ecalFace=new Vec(0.,zAtEcal,0.); 
	Plane ecalPlane = new Plane(ecalFace, new Vec(0., 1., 0.));
	HelixState helixAtEcal=kT.getHelixAtPlane(ecalPlane);


	// this is how createTrackState (above) goes from measurement to TrackState
	// from the measurements.  It calls toHPShelix.
	// the helix state here must already be propagated
	/* helix.toTrackState(alphaCenter, ms.m.p, loc); */  

	//	DMatrixRMaj ecalCov = new DMatrixRMaj(kT.ecalCovariance());
	DMatrixRMaj ecalCov = new DMatrixRMaj(getCovarianceFromHelix(helixAtEcal));
	//	double[] ecalParams = kT.ecalHelixParms();
	double[] ecalParams = helixAtEcal.a.v.clone();
	Vec  BfieldAtEcal = KalmanInterface.getField(helixAtEcal.origin, kT.SiteList.get(0).m.Bfield);
	if(debug)System.out.println(this.getClass().getName()+":: helixAtOrigin origin position = "+helixAtEcal.origin.toString());
        double BAtEcal = BfieldAtEcal.mag();
        double alphaAtEcal = conFac/ BAtEcal;
	if(debug)System.out.println(this.getClass().getName()+":: B = "+BAtEcal+"    alpha= "+alphaAtEcal);

	TrackState ts_toTrackState=helixAtEcal.toTrackState(alphaAtEcal, ecalPlane, TrackState.AtCalorimeter, true);
	
	
        double[] ecalLCSimParams = getLCSimParams(ecalParams, alphaAtEcal);
        double[] ecalLCSimCov = getLCSimCov(ecalCov, alphaAtEcal).asPackedArray(true);
	double[] refAtEcal = {helixAtEcal.origin.v[0],-helixAtEcal.origin.v[2],helixAtEcal.origin.v[1]};
	TrackState ts_ecal_helix = new BaseTrackState(ecalLCSimParams,refAtEcal, ecalLCSimCov,  TrackState.AtCalorimeter, BAtEcal);
	if(debug)System.out.println(this.getClass().getName()+":: Uncorrected track state:  curvature = "+ts_ecal_helix.getOmega()
			   +"  bField = "+ts_ecal_helix.getBLocal()+"  momentum z = "+ts_ecal_helix.getMomentum()[0]);
	System.out.println("Helix at ECal from kT.getHelixAtPlane and by hand conversions ");
	System.out.println(ts_ecal_helix.toString());
	newTrack.getTrackStates().add(ts_ecal_helix);

	System.out.println("Helix at ECal from helix.toTrackState");
	System.out.println(ts_toTrackState.toString());
	
        // Extrapolate to the ECAL and make a new trackState there.
	//        BaseTrackState ts_ecal = new BaseTrackState();
        //ts_ecal = TrackUtils.getTrackExtrapAtEcalRK(newTrack, fM, runNumber);


        // Extrapolate to Target and make a new trackState there.
        BaseTrackState ts_target = new BaseTrackState();
        if (target_pos != -999.9 && addTrackStateAtTarget){
            ts_target = TrackUtils.getTrackExtrapAtTargetRK(newTrack, target_pos, beamPosition, fM, 0);
            if (ts_target != null){
                newTrack.getTrackStates().add(ts_target);
            }
        }
        
        // other track properties
        newTrack.setChisq(kT.chi2);
        newTrack.setNDF(newTrack.getTrackerHits().size() - 5);
        newTrack.setTrackType(BaseTrack.TrackType.Y_FIELD.ordinal());
        newTrack.setSubdetectorHitNumbers(hitPattern);	
        newTrack.setFitSuccess(true);
	
        return newTrack;
    }

    // Convert helix parameters from Kalman to LCSim
    static double[] getLCSimParams(double[] oldParams, double alpha) {  
        // Note: since HPS-java has assumed a constant field for tracking, the alpha value used here should
        // correspond to the field at the center of the SVT or magnet.   See the class variable alphaCenter.
        double[] params = new double[5];
        params[ParameterName.d0.ordinal()] = oldParams[0];
        params[ParameterName.phi0.ordinal()] = -1.0 * oldParams[1];
        params[ParameterName.omega.ordinal()] = oldParams[2] / alpha * -1.0;
        params[ParameterName.z0.ordinal()] = oldParams[3] * -1.0;
        params[ParameterName.tanLambda.ordinal()] = oldParams[4] * -1.0;

        return params;
    }

    // Convert helix parameters from LCSim to Kalman
    static double[] unGetLCSimParams(double[] oldParams, double alpha) { 
        // Note: since HPS-java has assumed a constant field for tracking, the alpha value used here should
        // correspond to the field at the center of the SVT or magnet.   See the class variable alphaCenter.
        double[] params = new double[5];
        params[0] = oldParams[ParameterName.d0.ordinal()];
        params[1] = -1.0 * oldParams[ParameterName.phi0.ordinal()];
        params[2] = oldParams[ParameterName.omega.ordinal()] * alpha * -1.0;
        params[3] = oldParams[ParameterName.z0.ordinal()] * -1.0;
        params[4] = oldParams[ParameterName.tanLambda.ordinal()] * -1.0;
        return params;
    }

    // Convert helix parameter covariance matrix from Kalman to LCSim
    static SymmetricMatrix getLCSimCov(DMatrixRMaj oldCov, double alpha) {
        // Note: since HPS-java has assumed a constant field for tracking, the alpha value used here should
        // correspond to the field at the center of the SVT or magnet.   See the class variable alphaCenter.
        double[] d = { 1.0, -1.0, -1.0 / alpha, -1.0, -1.0 };
        SymmetricMatrix cov = new SymmetricMatrix(5);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                cov.setElement(i, j, d[i] * d[j] * oldCov.unsafe_get(i, j));
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
        // Note: since HPS-java has assumed a constant field for tracking, the alpha value used here should
        // correspond to the field at the center of the SVT or magnet.   See the class variable alphaCenter.
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
        double[] params = getLCSimParams(trk.pivotTransform(newPivot), alphaCenter);
        SymmetricMatrix cov = getLCSimCov(trk.covariance(), alphaCenter);
        BaseTrack newTrack = new BaseTrack();
        newTrack.setTrackParameters(params, trk.B());
        newTrack.setCovarianceMatrix(cov);
        addHitsToTrack(newTrack);
        newTrack.setTrackType(BaseTrack.TrackType.Y_FIELD.ordinal());
        newTrack.setFitSuccess(trk.success);

        return newTrack;
    }

    // Method to create one Kalman SiModule object for each silicon-strip detector
    public void createSiModules(List<SiStripPlane> inputPlanes) {
        if (debug) {
            System.out.format("Entering KalmanInterface.creasteSiModules\n");
        }
        detPlanes = inputPlanes;  // keep this reference for use by other methods
        
        //2016 => 12 planes, 2019 => 14 planes
        int nPlanes = inputPlanes.size();
        //System.out.printf("PF::nPlanes::%d", nPlanes);
        if (nPlanes == 40) { // 2019 vs 2016 detector first layer
            kPar.setFirstLayer(0);
        } else {            
            kPar.setFirstLayer(2);
        }
        
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

            if (debug) {
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
                        
            int kalLayer;                         
            boolean split;
            if (nPlanes == 40) { //Indexing valid for 2019 detector -> Include new layer-0, layers go from 0 to 13!!
                kalLayer = temp.getLayerNumber()-1;
                split = (kalLayer < 4);
            } else {            //Indexing valid for 2016 detector
                kalLayer = temp.getLayerNumber()+1;
                split = false;
            }
            int topBottom = 1;
            if (temp.isBottomLayer()) topBottom = 0;
            int detector = temp.getModuleNumber();
            if (kalLayer > 13) {
                System.out.format("***KalmanInterface.createSiModules Warning: Kalman layer %d , tempLayer = %d out of range.***\n", kalLayer,temp.getLayerNumber());
            }           
            int millipedeID = temp.getMillepedeId();
            SiModule newMod = new SiModule(kalLayer, p, temp.isStereo(), inputPlane.getWidth(), inputPlane.getLength(),
                    split, inputPlane.getThickness(), fM, detector, millipedeID, topBottom);           
            moduleMap.put(newMod, inputPlane);
            SiMlist.add(newMod);
        }
        Collections.sort(SiMlist, new SortByLayer());
        for (SiModule sim : SiMlist) {
            logger.log(Level.INFO, sim.toString());
        }
    }
    
    // Method to feed simulated hits into the pattern recognition, for testing
    private boolean fillAllSimHits(EventHeader event, IDDecoder decoder) {
        boolean success = false;
        eventNumber = event.getEventNumber();

        if (debug || event.getEventNumber() < 50) System.out.format("KalmanInterface.fillAllSimHits: entering for event %d\n", event.getEventNumber());
        
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
            if (debug) System.out.format("    Adding hit for layer %d, module %d\n", Layer, Module);
        }

        int hitsFilled = 0;
        for (int modIndex = 0; modIndex < SiMlist.size(); ++modIndex) {
            SiModule module = SiMlist.get(modIndex);
            Pair<Integer,Integer> sensor = new Pair<Integer,Integer>(module.Layer,module.detector);
            if (debug) System.out.format("   Si module in layer %d module %d, extent=%8.3f to %8.3f\n", module.Layer, module.detector, module.yExtent[0], module.yExtent[1]);

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

                if (debug) {
                    System.out.format("\nKalmanInterface:fillAllSimHits %d, the measurement uncertainty is set to %10.7f\n", i, du);
                    System.out.printf("Filling SiMod Layer %d, detector %d\n", module.Layer, module.detector);
                    module.p.print("Corresponding KalmanPlane");
                    Vec globalX = module.R.rotate(new Vec(1, 0, 0));
                    Vec globalY = module.R.rotate(new Vec(0, 1, 0));
                    globalX.print("globalX");
                    globalY.print("globalY");
                    System.out.format("     Adding measurement %10.5f to layer %d, module %d\n", umeas, module.Layer, module.detector);
                }
                Measurement m = new Measurement(umeas, 0., du, 0., hit.getdEdx()*1000000., rGlobal, rLocal.v[1]);
                //rGlobal.print("new global hit location");

                module.addMeasurement(m);
                simHitMap.put(m, hit);
                hitsFilled++;
            }
            if (debug) { module.print("SiModule-filled"); }
        }
        if (hitsFilled > 0) success = true;
        if (debug) System.out.format("KalmanInterface.fillAllSimHits: %d hits were filled into Si Modules\n", hitsFilled);

        return success;
    }
       
    // Method to fill all Si hits into the SiModule objects, to feed to the pattern recognition.
    private boolean fillAllMeasurements(EventHeader event) {
        boolean success = false;
        eventNumber = event.getEventNumber();

        // Get the collection of 1D hits
        String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
        List<TrackerHit> striphits = event.get(TrackerHit.class, stripHitInputCollectionName);
        
        if (striphits.size() > maxHits) maxHits = striphits.size(); 
        if (striphits.size() > 200) nBigEvents++;
        if (_siHitsLimit > 0 && striphits.size() > _siHitsLimit) {
            System.out.format("KalmanInterface::Skip event %d with %s %d hits > %d\n", event.getEventNumber(), 
                    stripHitInputCollectionName, striphits.size(), _siHitsLimit);
            return false;
        } else if (striphits.size() > 500) {
            System.out.format("KalmanInterface::fillAllMeasurements: event %d has > 500 hits!\n", event.getEventNumber());
        }

        // Make a mapping from sensor to hits
        Map<HpsSiSensor, ArrayList<TrackerHit>> hitSensorMap = new HashMap<HpsSiSensor, ArrayList<TrackerHit>>();
        if (debug) {
            if (striphits.size() == 0) {
                System.out.format("KalmanInterface:fillAllMeasurements, there are no strip hits in event %d\n",event.getEventNumber());
            }
        }
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

                if (debug) {
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
                double [] lpos = localHit.getPosition();
                double umeas = lpos[0];
                double du = FastMath.sqrt(localHit.getCovarianceAsMatrix().diagonal(0));
                double time = localHit.getTime(); 
                double xStrip = -lpos[1];    // Center of strip, i.e. ~0 except in layers 0 and 1
                if (xStrip > module.xExtent[1] || xStrip < module.xExtent[0]) {
                    logger.log(Level.WARNING, String.format("Event %d Layer %d, local hit at %9.4f %9.4f, %9.4f is outside detector extents %8.3f->%8.3f %8.3f->%8.3f", 
                            event.getEventNumber(), module.Layer, lpos[0], lpos[1], lpos[2], module.yExtent[0], module.yExtent[1], module.xExtent[0], module.xExtent[1]));
                }
                if (debug) {
                    int nstrp = localHit.getRawHits().size();
                    System.out.format("%d %d u = %9.4f +- %9.4f    cov=%10.4e, %10.4e, %10.4e\n", module.Layer, nstrp, umeas, du, localHit.getCovarianceAsMatrix().e(0,0), 
                            localHit.getCovarianceAsMatrix().e(1,0), localHit.getCovarianceAsMatrix().e(1,1));
                }

                // If HPS measured coordinate axis is opposite to Kalman measured coordinate axis
                // This really should not happen, as the Kalman axis is copied directly from the hps geometry.
                Hep3Vector planeMeasuredVec = VecOp.mult(HpsSvtToKalmanMatrix, plane.getMeasuredCoordinate());
                if (planeMeasuredVec.z() * module.p.V().v[2] < 0) {
                    System.out.format("*** KalmanInterface.fillAllMeasurements: flipping Kalman coordinate sign %d! ***\n", i);
                    umeas *= -1.0;
                }

                if (debug) {
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
		List<RawTrackerHit> rawhits =  hit.getRawHits();
                ShaperFitAlgorithm fitter = new ShaperPileupFitAlgorithm(.5,1);
                PulseShape shape = new PulseShape.FourPole();
                fitter.setRunNum(event.getRunNumber());
                double Variance=0.0;
                for(RawTrackerHit rth: rawhits){
                        double Min = 10000;
                        for(ShapeFitParameters fit : fitter.fitShape(rth, shape)){
                                if(fit.getT0Err()<Min){
                                        Min=fit.getT0Err();
                                }
                        }
                        if(module.Layer>1){
                                Variance+=1/(Min*Min);
                        }
                }
                Variance=1.0/Variance;
                Measurement m = new Measurement(umeas, xStrip, du, time, localHit.getdEdx()*1000000.,Variance);
                module.addMeasurement(m);
                hitMap.put(m, hit);
                hitsFilled++;
            }
            if (debug) { module.print("SiModule-filled"); }
        }
        if (hitsFilled > 0) success = true;
        if (debug) System.out.format("KalmanInterface.fillAllMeasurements: %d hits were filled into Si Modules\n", hitsFilled);
        
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
                double xStrip = -local.getPosition()[1];
                double du = FastMath.sqrt(local.getCovarianceAsMatrix().diagonal(0));

                // if hps measured coord axis is opposite to kalman measured coord axis
                // This really should not happen, as the Kalman axis is copied directly from the hps geometry.
                if (planeMeasuredVec.z() * mod.p.V().v[2] < 0) { 
                    System.out.format("*** KalmanInterface.fillMeasurements: flipping Kalman coordinate sign %d! ***\n", i);
                    umeas *= -1.0; 
                }

                if (debug) {
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
                Measurement m = new Measurement(umeas, xStrip, du, 0., hit.getdEdx()*1000000.,1.0);

                KalHit hitPair = new KalHit(mod,m);
                trackHitsKalman.add(hitPair);
                mod.addMeasurement(m);
                hitMap.put(m, hit);

            }
            if (debug) { mod.print("SiModule-filled"); }
        }
        return firstZ;
    }

    // Make a linear fit to a set of hits to be used to initialize the Kalman Filter
    public SeedTrack createKalmanSeedTrack(Track track, RelationalTable hitToStrips, RelationalTable hitToRotated) {
        List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(track, hitToStrips, hitToRotated);
        double firstHitZ = fillMeasurements(hitsOnTrack, 0);
        if (debug) System.out.printf("firstHitZ %f \n", firstHitZ);
        return new SeedTrack(trackHitsKalman, firstHitZ, 0.);
    }

    // Method to refit an existing track's hits, using the Kalman seed-track to initialize the Kalman Filter.
    public KalmanTrackFit2 createKalmanTrackFit(int evtNumb, SeedTrack seed, Track track, RelationalTable hitToStrips,
            RelationalTable hitToRotated, int nIt) {
        double firstHitZ = 10000.;
        List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(track, hitToStrips, hitToRotated);
        if (debug) { System.out.format("createKalmanTrackFit: number of hits on track = %d\n", hitsOnTrack.size()); }
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
            if (debug) { SiM.print(String.format("SiMoccupied%d", i)); }
        }
        // startIndex++;

        if (debug) { System.out.printf("createKTF: using %d SiModules, startIndex %d \n", SiMoccupied.size(), startIndex); }

        DMatrixRMaj cov = seed.covariance().copy();
        CommonOps_DDRM.scale(10., cov);

        return new KalmanTrackFit2(evtNumb, SiMoccupied, null, startIndex, nIt, new Vec(0., seed.yOrigin, 0.), seed.helixParams(), cov, kPar, fM);
    }

    // Method to refit an existing track, using the track's helix parameters and covariance to initialize the Kalman Filter.
    public KalmanTrackFit2 createKalmanTrackFit(int evtNumb, Vec helixParams, Vec pivot, DMatrixRMaj cov, Track track,
            RelationalTable hitToStrips, RelationalTable hitToRotated, int nIt) {
        List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(track, hitToStrips, hitToRotated);
        if (debug) { System.out.format("createKalmanTrackFit: using GBL fit as start; number of hits on track = %d\n", hitsOnTrack.size()); }

        ArrayList<SiModule> SiMoccupied = new ArrayList<SiModule>();

        fillMeasurements(hitsOnTrack, 2);
        for (SiModule SiM : SiMlist) {
            if (!SiM.hits.isEmpty()) SiMoccupied.add(SiM);
        }
        Collections.sort(SiMoccupied, new SortByLayer());

        for (int i = 0; i < SiMoccupied.size(); i++) {
            SiModule SiM = SiMoccupied.get(i);
            if (debug) SiM.print(String.format("SiMoccupied%d", i));
        }

        int startIndex = 0;
        if (debug) System.out.printf("createKTF: using %d SiModules, startIndex %d \n", SiMoccupied.size(), startIndex); 
        CommonOps_DDRM.scale(10., cov);
        return new KalmanTrackFit2(evtNumb, SiMoccupied, null, startIndex, nIt, pivot, helixParams, cov, kPar, fM);
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
    public ArrayList<KalTrack>[] KalmanPatRec(EventHeader event, IDDecoder decoder) {
        if (debug) System.out.format("KalmanInterface: entering KalmanPatRec for event %d\n", event.getEventNumber());
        ArrayList<KalTrack>[] outList = new ArrayList[2];
        if (!fillAllMeasurements(event)) {
            if (debug) System.out.format("KalmanInterface.KalmanPatRec: recon SVT hits not found for event %d\n",event.getEventNumber());
            for (int topBottom=0; topBottom<2; ++topBottom) {
                outList[topBottom] = new ArrayList<KalTrack>();
            }
            return outList;  // Return empty track lists if there are no hits
        }

        int evtNum = event.getEventNumber();
        
        for (int topBottom=0; topBottom<2; ++topBottom) {
            ArrayList<SiModule> SiMoccupied = new ArrayList<SiModule>();
            for (SiModule SiM : SiMlist) {
                if (SiM.topBottom != topBottom) continue;
                //if (topBottom == 0) {
                //    if (SiM.p.X().v[2] < 0.) continue;
                //} else {
                //    if (SiM.p.X().v[2] > 0.) continue;
                //}
                SiMoccupied.add(SiM);  // Need to keep all of these even if there are no hits!!!!!!
            }
            Collections.sort(SiMoccupied, new SortByLayer());
            
            if (debug) {
                for (int i = 0; i < SiMoccupied.size(); i++) {
                    SiModule SiM = SiMoccupied.get(i);
                    SiM.print(String.format("SiMoccupied Number %d for topBottom=%d", i, topBottom));
                }
                System.out.format("KalmanInterface.KalmanPatRec event %d: calling KalmanPatRecHPS for topBottom=%d\n", event.getEventNumber(), topBottom);
            }
            outList[topBottom] = kPat.kalmanPatRec(event, hitMap, SiMoccupied, topBottom);
        }
        return outList;
    }

    // The following method is a debugging aid for comparing SeedTracker/GBL tracks to the Kalman counterparts.
    public void compareAllTracks(String trackCollectionName, EventHeader event, ArrayList<KalTrack>[] kPatList) {
        if (!event.hasCollection(Track.class, trackCollectionName)) {
            System.out.format("\nKalmanInterface.compareAllTracks: the track collection %s is missing. Abort.\n",trackCollectionName);
            return;
        }
        String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
        if (!event.hasCollection(TrackerHit.class, stripHitInputCollectionName)) {
            System.out.format("\nKalmanInterface.compareAllTracks: the hit collection %s is missing. Abort.\n",stripHitInputCollectionName);
            return;
        }    
        List<Track> tracksGBL = event.get(Track.class, trackCollectionName);
        System.out.format("\nPrinting %s tracks for event %d\n", trackCollectionName, event.getEventNumber());
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
        //System.out.format("   relation tables: %s %d  %s %d\n", hitToStrips.toString(), hitToStrips.size(), hitToRotated.toString(), hitToRotated.size());
        for (Track tkr : tracksGBL) {
            double minz = 999.;
            TrackState ts1 = null;
            for (TrackState state : tkr.getTrackStates()) {
                //System.out.format("Track state %d: location=%d\n", tkr.getTrackStates().indexOf(state), state.getLocation());
                if (state.getLocation() == TrackState.AtIP) {
                    ts1 = state;
                    break;
                }
            }
            if (ts1 == null) {
                System.out.format("Track %d, missing TrackState.\n", tracksGBL.indexOf(tkr));
                continue;
            }
            double [] a = new double[5];
            for (int i=0; i<5; ++i) {
                a[i] = ts1.getParameter(i);
            }
            double[] covHPS = ts1.getCovMatrix();
            double Q = tkr.getCharge();
            double chi2 = tkr.getChi2();
            int nHits = tkr.getTrackerHits().size();
            System.out.format("Track %d, Q=%4.1f, %d 3D hits, chi^2=%7.1f, helix=%8.3f %9.6f %9.6f %8.4f %8.4f\n", tracksGBL.indexOf(tkr),  
                    Q, nHits, chi2, a[0], a[1], a[2], a[3], a[4]);
            Vec kalParms = new Vec(5,unGetLCSimParams(a, alphaCenter));
            System.out.format("     Helix in Kalman parameterization = %s\n", kalParms.toString());
/*            for (TrackerHit hit3D : tkr.getTrackerHits()) {
                double [] hitPos3D = hit3D.getPosition();
                System.out.format("compareAllTracks: tracker 3D hit %10.6f %10.6f %10.6f\n", hitPos3D[0], hitPos3D[1], hitPos3D[2]);
                List<TrackerHit> hits = new ArrayList<TrackerHit>();
                hits.addAll(hitToStrips.allFrom(hit3D));
                System.out.format("     hits = %s, %d\n", hits.toString(), hits.size());
                for (TrackerHit ht : hits) {                
                    double [] pnt = ht.getPosition();
                    System.out.format("    Hit global position: %10.6f %10.6f %10.6f\n", pnt[0], pnt[1], pnt[2]);
                    List<RawTrackerHit> rawHits = ht.getRawHits();
                    for (RawTrackerHit rawHit : rawHits) {
                        int chan = rawHit.getIdentifierFieldValue("strip");
                        HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement();
                        int Layer = sensor.getLayerNumber();
                        System.out.format("      Raw hit in layer %d, channel %d\n", Layer, chan);
                    }
                }
            }
            */
            List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(tkr, hitToStrips, hitToRotated);
            //System.out.format("    hitsOnTrack = %s, %d\n", hitsOnTrack.toString(), hitsOnTrack.size());
            int [] chanGBL = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
            int [] chanKAL = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};
            for (TrackerHit ht : hitsOnTrack) {                
                double [] pnt = ht.getPosition();
                System.out.format("    Hit global position: %10.6f %10.6f %10.6f\n", pnt[0], pnt[1], pnt[2]);
                List<RawTrackerHit> rawHits = ht.getRawHits();
                for (RawTrackerHit rawHit : rawHits) {
                    int chan = rawHit.getIdentifierFieldValue("strip");
                    HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement();
                    int Layer = sensor.getLayerNumber();
                    int sensorID = sensor.getModuleNumber();
                    System.out.format("      Raw hit in layer %d, sensor %d, channel %d\n", Layer, sensorID, chan);
                    if (sensorID*10000 + chan > chanGBL[Layer-1]) chanGBL[Layer-1] = sensorID*10000 + chan;
                }
            }
            //double [] pnt0 = hitsOnTrack.get(0).getPosition();
            //Vec newPivot = KalmanInterface.vectorGlbToKalman(pnt0);
            //DMatrixRMaj cov = new DMatrixRMaj(KalmanInterface.ungetLCSimCov(covHPS, alphaCenter));
            //KalmanTrackFit2 ktf2 = this.createKalmanTrackFit(event.getEventNumber(), kalParms, newPivot, cov, tkr, hitToStrips, hitToRotated, 2);
            //if (ktf2 != null) {
            //    HelixState hx = ktf2.sites.get(0).aS.helix;
            //    System.out.format("    Kalman fit of hits: chi2=%9.4f, helix=%s\n", ktf2.chi2s, hx.a.toString());
            //}
            int topBottom = 0;
            int nGood = 0;
            if (kalParms.v[4] < 0.) topBottom = 1;
            KalTrack kMatch = null;
            for (KalTrack ktk : kPatList[topBottom]) {
                int nMatch = 0;
                for (MeasurementSite site : ktk.SiteList) {
                    if (site.hitID < 0) continue;
                    SiModule mod = site.m;
                    if (mod != null) {
                        TrackerHit ht = this.getHpsHit(mod.hits.get(site.hitID));
                        List<RawTrackerHit> rawHits = ht.getRawHits();
                        for (RawTrackerHit rawHit : rawHits) {
                            int chan = rawHit.getIdentifierFieldValue("strip");
                            HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement();
                            int Layer = sensor.getLayerNumber();
                            int sensorID = sensor.getModuleNumber();
                            if (chanGBL[Layer-1] == 10000*sensorID + chan) {
                                nMatch++;
                                break;
                            }
                        }
                    }
                }
                if (nMatch > nGood) {
                    kMatch = ktk;
                    nGood = nMatch;
                }
            }
            if (kMatch != null) {
                int nKalHits = 0;
                for (MeasurementSite site : kMatch.SiteList) {
                    if (site.hitID < 0) continue;
                    nKalHits++;
                    SiModule mod = site.m;
                    if (mod != null) {
                        TrackerHit ht = this.getHpsHit(mod.hits.get(site.hitID));
                        List<RawTrackerHit> rawHits = ht.getRawHits();
                        for (RawTrackerHit rawHit : rawHits) {
                            int chan = rawHit.getIdentifierFieldValue("strip");
                            HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement();
                            int Layer = sensor.getLayerNumber();
                            int sensorID = sensor.getModuleNumber();
                            if (10000*sensorID + chan > chanKAL[Layer-1]) chanKAL[Layer-1] = 10000*sensorID + chan;
                        }
                    }
                }
                System.out.format("GBL/Kalman match, ID=%d, %d hits, with %d matching layers\n", kMatch.ID, nKalHits, nGood);
                for (int lyr=0; lyr<14; ++lyr) {
                    System.out.format("    Layer %d: GBL=%d   KAL=%d\n", lyr, chanGBL[lyr], chanKAL[lyr]);
                }
                boolean refit = false;
                HelixState hx = null;
                for (MeasurementSite site : kMatch.SiteList) {
                    if (site.hitID >= 0) {
                        if (site.aS != null) {
                            refit = true;
                            hx = site.aS.helix;
                            break;
                        }
                    }
                }
                if (refit) {
                    ArrayList<SiModule> modList = new ArrayList<SiModule>(nGood);
                    ArrayList<Integer> hits = new ArrayList<Integer>(nGood);
                    for (int lyr=0; lyr<14; ++lyr) {
                        if (chanGBL[lyr] >= 0) {
                            for (SiModule mod : SiMlist) {
                                if (mod.Layer != lyr) continue;
                                int sensorID = mod.detector;
                                HitLoop: for (Measurement kalHt : mod.hits) {
                                    TrackerHit ht = this.getHpsHit(kalHt);
                                    List<RawTrackerHit> rawHits = ht.getRawHits();
                                    for (RawTrackerHit rawHit : rawHits) {
                                        if (10000*sensorID + rawHit.getIdentifierFieldValue("strip") == chanGBL[lyr]) {
                                            modList.add(mod);
                                            hits.add(mod.hits.indexOf(kalHt));
                                            break HitLoop;
                                        }
                                    }
                                }
                            }
                        }
                    } 
                    
                    // The following is for testing by refitting the existing Kalman track
                    //for (MeasurementSite site : kMatch.SiteList) {
                    //    if (site.hitID < 0) continue;
                    //    modList.add(site.m);
                    //    hits.add(site.hitID);
                    //}
                    DMatrixRMaj cov = hx.C.copy();
                    CommonOps_DDRM.scale(10., cov);
                    KalmanTrackFit2 kft2 = new KalmanTrackFit2(event.getEventNumber(), modList, hits, 0, 2, hx.X0, hx.a, cov, kPar, fM);
                    if (kft2 != null) kft2.printFit("refit with GBL hits");
                }
                kMatch.print("matching Kalman track");
            }
        }
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

        double vPos = 0.9;
        for (Track tkr : tracksGBL) {
            double [] a = new double[5];
            for (int i=0; i<5; ++i) {
                a[i] = tkr.getTrackStates().get(0).getParameter(i);
            }
            double Q = tkr.getCharge();
            double chi2 = tkr.getChi2();
            int nHits = tkr.getTrackerHits().size();
            String s = String.format("Track %d, Q=%4.1f, %d hits, chi^2=%7.1f, helix=%8.3f %8.3f %8.3f %8.3f %8.3f", tracksGBL.indexOf(tkr),  
                    Q, nHits, chi2, a[0], a[1], a[2], a[3], a[4]);
            printWriter3.format("set label '%s' at screen 0.1, %2.2f\n", s, vPos);
            vPos = vPos - 0.03;
        }
        
        for (Track tkr : tracksGBL) {
            printWriter3.format("$tkr%d << EOD\n", tracksGBL.indexOf(tkr));
            for (TrackState state : tkr.getTrackStates()) {
                int loc = state.getLocation();
                if (loc != state.AtIP && loc != state.AtCalorimeter && loc != state.AtOther && loc != state.AtVertex) {
                    double [] pnt = state.getReferencePoint();
                    printWriter3.format(" %10.6f %10.6f %10.6f\n", pnt[0], pnt[2], -pnt[1]);
                }
            }
            printWriter3.format("EOD\n");
        }       
        
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
            //printWriter3.format(", $tkr%d u 1:2:3 with lines lw 3", tracksGBL.indexOf(tkr));
        }
        printWriter3.format("\n");
        printWriter3.close();
    }
    // This method makes a Gnuplot file to display the Kalman tracks and hits in 3D.
    public void plotKalmanEvent(String path, EventHeader event, ArrayList<KalTrack>[] patRecList) {
        
        boolean debug = false;
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
        for (int topBottom=0; topBottom<2; ++topBottom) {
            for (KalTrack tkr : patRecList[topBottom]) {
                double [] a = tkr.originHelixParms();
                if (a == null) a = tkr.SiteList.get(0).aS.helix.a.v;
                String s = String.format("TB %d Track %d, %d hits, chi^2=%7.1f, a=%8.3f %8.3f %8.3f %8.3f %8.3f t=%6.1f", 
                        topBottom, tkr.ID, tkr.nHits, tkr.chi2, a[0], a[1], a[2], a[3], a[4], tkr.getTime());
                printWriter3.format("set label '%s' at screen 0.1, %2.2f\n", s, vPos);
                vPos = vPos - 0.03;
            }
        }
        int [] nTkpL = {0, 0};
        int [] nTkpS = {0, 0};
        for (int topBottom=0; topBottom<2; ++topBottom) {   // Plotting tracks as lines
            for (KalTrack tkr : patRecList[topBottom]) {
                printWriter3.format("$tkr%d_%d << EOD\n", tkr.ID, topBottom);
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
                    double phiS = aS.helix.planeIntersect(module.p);
                    if (Double.isNaN(phiS)) continue;
                    Vec rLocal = aS.helix.atPhi(phiS);
                    Vec rGlobal = aS.helix.toGlobal(rLocal);
                    printWriter3.format(" %10.6f %10.6f %10.6f\n", rGlobal.v[0], rGlobal.v[1], rGlobal.v[2]);
                    if (debug) {
                        System.out.format("plotKalmanEvent %d: tk %d lyr %d phiS=%11.6f\n", event.getEventNumber(), tkr.ID, module.Layer, phiS);
                        rLocal.print(" local point in B frame");
                        rGlobal.print(" global point");
                    }
                    // Vec rDetector = m.toLocal(rGlobal);
                    // double vPred = rDetector.v[1];
                    // if (site.hitID >= 0) {
                    // System.out.format("vPredPrime=%10.6f, vPred=%10.6f, v=%10.6f\n", vPred, aS.mPred, m.hits.get(site.hitID).v);
                    // }
                }
                printWriter3.format("EOD\n");
            }

            for (KalTrack tkr : patRecList[topBottom]) {    // Plotting hits on tracks
                printWriter3.format("$tkp%d_%d << EOD\n", tkr.ID, topBottom);
                for (MeasurementSite site : tkr.SiteList) {
                    SiModule module = site.m;
                    int hitID = site.hitID;
                    if (hitID < 0) continue;
                    Measurement mm = module.hits.get(hitID);
                    if (mm.energy < kPar.minSeedE[module.Layer]) continue;
                    if (mm.tracks.size() > 1) continue;
                    Vec rLoc = null;
                    if (mm.rGlobal == null) {         // If there is no MC truth, use the track intersection for x and z
                        StateVector aS = site.aS;
                        double phiS = aS.helix.planeIntersect(module.p);
                        if (!Double.isNaN(phiS)) {
                            Vec rLocal = aS.helix.atPhi(phiS);        // Position in the Bfield frame
                            Vec rGlobal = aS.helix.toGlobal(rLocal);  // Position in the global frame                 
                            rLoc = module.toLocal(rGlobal);     // Position in the detector frame
                            if (debug) {
                                double resid = rLoc.v[1] - mm.v;
                                System.out.format("plotKalmanEvent %d: tk %d lyr %d phiS=%11.6f resid= %11.8f vs %11.8f\n",
                                        event.getEventNumber(), tkr.ID, module.Layer, phiS, resid, site.aS.r);
                                aS.helix.a.print(" helix parameters ");
                                rLocal.print(" local position in B frame");
                                rGlobal.print(" global position ");
                                rLoc.print(" position in detector frame");
                            }
                        } else {
                            if (debug) {
                                System.out.format("plotKalmanEvent %d: tk %d lyr %d phiS is NaN.\n", event.getEventNumber(),tkr.ID, module.Layer);
                                aS.helix.a.print(" helix parameters ");
                            }
                            rLoc = new Vec(0.,0.,0.);
                        }
                    } else {
                        rLoc = module.toLocal(mm.rGlobal); // Use MC truth for the x and z coordinates in the detector frame
                    }
                    Vec rmG = module.toGlobal(new Vec(rLoc.v[0], mm.v, rLoc.v[2]));
                    if (debug) System.out.format("plotKalmanEvent %d: tk %d lyr %d rmG=%s\n", event.getEventNumber(), tkr.ID, module.Layer, rmG.toString());
                    printWriter3.format(" %10.6f %10.6f %10.6f\n", rmG.v[0], rmG.v[1], rmG.v[2]);
                }
                printWriter3.format("EOD\n");
            }
            for (KalTrack tkr : patRecList[topBottom]) {    // Plotting shared hits on tracks
                printWriter3.format("$tkpS%d_%d << EOD\n", tkr.ID, topBottom);
                for (MeasurementSite site : tkr.SiteList) {
                    SiModule module = site.m;
                    int hitID = site.hitID;
                    if (hitID < 0) continue;
                    Measurement mm = module.hits.get(hitID);
                    if (mm.energy < kPar.minSeedE[module.Layer]) continue;
                    if (mm.tracks.size() <= 1) continue;
                    Vec rLoc = null;
                    if (mm.rGlobal == null) {         // If there is no MC truth, use the track intersection for x and z
                        StateVector aS = site.aS;
                        double phiS = aS.helix.planeIntersect(module.p);
                        if (!Double.isNaN(phiS)) {
                            Vec rLocal = aS.helix.atPhi(phiS);        // Position in the Bfield frame
                            Vec rGlobal = aS.helix.toGlobal(rLocal);  // Position in the global frame                 
                            rLoc = module.toLocal(rGlobal);     // Position in the detector frame
                        } else {
                            rLoc = new Vec(0.,0.,0.);
                        }
                    } else {
                        rLoc = module.toLocal(mm.rGlobal); // Use MC truth for the x and z coordinates in the detector frame
                    }
                    Vec rmG = module.toGlobal(new Vec(rLoc.v[0], mm.v, rLoc.v[2]));
                    printWriter3.format(" %10.6f %10.6f %10.6f\n", rmG.v[0], rmG.v[1], rmG.v[2]);
                    nTkpS[topBottom]++;
                }
                printWriter3.format("EOD\n");
            }
            for (KalTrack tkr : patRecList[topBottom]) {    // Plotting low-ph hits on tracks
                printWriter3.format("$tkpL%d_%d << EOD\n", tkr.ID, topBottom);
                for (MeasurementSite site : tkr.SiteList) {
                    SiModule module = site.m;
                    int hitID = site.hitID;
                    if (hitID < 0) continue;
                    Measurement mm = module.hits.get(hitID);
                    if (mm.energy >= kPar.minSeedE[module.Layer]) continue;
                    Vec rLoc = null;
                    if (mm.rGlobal == null) {         // If there is no MC truth, use the track intersection for x and z
                        StateVector aS = site.aS;
                        double phiS = aS.helix.planeIntersect(module.p);
                        if (!Double.isNaN(phiS)) {
                            Vec rLocal = aS.helix.atPhi(phiS);        // Position in the Bfield frame
                            Vec rGlobal = aS.helix.toGlobal(rLocal);  // Position in the global frame                 
                            rLoc = module.toLocal(rGlobal);           // Position in the detector frame
                        } else {
                            rLoc = new Vec(0.,0.,0.);
                        }
                    } else {
                        rLoc = module.toLocal(mm.rGlobal); // Use MC truth for the x and z coordinates in the detector frame
                    }
                    Vec rmG = module.toGlobal(new Vec(rLoc.v[0], mm.v, rLoc.v[2]));
                    printWriter3.format(" %10.6f %10.6f %10.6f\n", rmG.v[0], rmG.v[1], rmG.v[2]);
                    nTkpL[topBottom]++;
                }
                printWriter3.format("EOD\n");
            }
        }
        printWriter3.format("$pnts << EOD\n");
        for (SiModule si : SiMlist) {
            for (Measurement mm : si.hits) {    // Plotting high-amplitude hits not on tracks
                if (mm.tracks.size() > 0) continue;
                if (mm.energy < kPar.minSeedE[si.Layer]) continue;
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
        printWriter3.format("$pntsL << EOD\n");
        for (SiModule si : SiMlist) {
            for (Measurement mm : si.hits) {    // Plotting low-amplitude hits not on tracks
                if (mm.tracks.size() > 0) continue;
                if (mm.energy >= kPar.minSeedE[si.Layer]) continue;
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
        int idx = 1;
        printWriter3.format("splot $pnts u 1:2:3 with points pt 6 ps 2 lc %d", idx);
        idx++;
        printWriter3.format(", $pntsL u 1:2:3 with points pt 4 ps 1 lc %d", idx);
        for (int topBottom=0; topBottom<2; ++topBottom) {
            for (KalTrack tkr : patRecList[topBottom]) { 
                idx++;
                printWriter3.format(", $tkr%d_%d u 1:2:3 with lines lw 3 lc %d", tkr.ID, topBottom, idx); 
                printWriter3.format(", $tkp%d_%d u 1:2:3 with points pt 7 ps 2 lc %d", tkr.ID, topBottom, idx); 
                if (nTkpL[topBottom] > 0) printWriter3.format(", $tkpL%d_%d u 1:2:3 with points pt 9 ps 2 lc %d", tkr.ID, topBottom, idx);
                if (nTkpS[topBottom] > 0) printWriter3.format(", $tkpS%d_%d u 1:2:3 with points pt 15 ps 2 lc %d", tkr.ID, topBottom, idx);
            }
        }
        printWriter3.format("\n");
        printWriter3.close();
    }
}

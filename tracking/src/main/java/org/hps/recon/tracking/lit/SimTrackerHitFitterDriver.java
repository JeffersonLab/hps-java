package org.hps.recon.tracking.lit;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MaterialSupervisor.ScatteringDetectorVolume;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import Jama.Matrix;
import hep.aida.ITree;
import hep.physics.vec.BasicHep3Vector;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.solids.GeomOp3D;
import org.lcsim.detector.solids.Line3D;
import org.lcsim.detector.solids.Point3D;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A. Graf
 */
public class SimTrackerHitFitterDriver extends Driver {

//    final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
//    MaterialSupervisor materialManager;
    private MaterialSupervisor _materialManager = null;
    Detector _det;

    Map<String, SiStripPlane> stripPlaneNameMap = new HashMap<String, SiStripPlane>();
    Map<String, NewDetectorPlane> planeMap = new HashMap<String, NewDetectorPlane>();
    private Map<String, SimTrackerHit> simTrackerHitmap = new HashMap<>();
    Map<String, Double> stereoAngleMap = new HashMap<>();
    Vector3D vX = Vector3D.PLUS_I;
    Vector3D vY = Vector3D.PLUS_J;
    Random ran = new Random();
    double sigmaU = .005;

    // TODO fix this...
    double[] SIGS = {sigmaU, 0.00};     //  detector resolutions in mm

    // tracking stuff
    private CbmLitRK4TrackExtrapolator _extrap;
    CbmLitTrackFitter _fitter;

    boolean _debug = false;

    // histograms
    AIDA aida = AIDA.defaultInstance();
    private ITree _tree = aida.tree();

    protected void detectorChanged(Detector detector) {
//        String detectorName = detector.getName();
//        //TODO fix this
//        try {
//            manager.setDetector(detectorName, 5772);
//        } catch (ConditionsManager.ConditionsNotFoundException ex) {
//            Logger.getLogger(SimTrackerHitFitterDriver.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        _det = manager.getCachedConditions(Detector.class, "compact.xml").getCachedData();
        _materialManager = new MaterialSupervisor();
        _materialManager.buildModel(detector);
        System.out.println(detector.getName());
        setup();

        //tracking stuff
        HpsMagField field = new HpsMagField(detector.getFieldMap());
        _extrap = new CbmLitRK4TrackExtrapolator(field);
        // a Kalman Filter updater...
        CbmLitTrackUpdate trackUpdate = new CbmLitKalmanFilter();
        CbmLitTrackPropagator prop = new SimpleTrackPropagator(_extrap);
        _fitter = new CbmLitTrackFitterImp(prop, trackUpdate);
    }

    protected void process(EventHeader event) {
        List<SimTrackerHit> simTrackerHitList = event.get(SimTrackerHit.class, "TrackerHits");
        List<MCParticle> mcParticles = event.get(MCParticle.class, "MCParticle");
        if (mcParticles.size() != 1) {
            return;
        }
        for (SimTrackerHit simtrackerhit : simTrackerHitList) {
            String sensorName = simtrackerhit.getDetectorElement().getName();
            simTrackerHitmap.put(sensorName, simtrackerhit);
        }
        List<Track> tracks = event.get(Track.class, "MatchedTracks");
        if (tracks.size() != 1) {
            return;
        }
        setupSensors(event);
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

        Track t = tracks.get(0);
        Map<String, Hep3Vector> trackHitGlobalPosMap = new HashMap<>();
        Map<String, Hep3Vector> trackHitLocalPosMap = new HashMap<>();

        for (TrackerHit hit : t.getTrackerHits()) {
            Set<TrackerHit> stripList = hitToStrips.allFrom(hitToRotated.from(hit));
            for (TrackerHit strip : stripList) {
                List rawHits = strip.getRawHits();
                HpsSiSensor sensor = null;
                for (Object o : rawHits) {
                    RawTrackerHit rth = (RawTrackerHit) o;
                    // TODO figure out why the following collection is always null
                    //List<SimTrackerHit> stipMCHits = rth.getSimTrackerHits();
                    sensor = (HpsSiSensor) rth.getDetectorElement();
                }
                String name = sensor.getName();
                Hep3Vector globalPos = new BasicHep3Vector(strip.getPosition());
                Hep3Vector localPos = sensor.getGeometry().getGlobalToLocal().transformed(globalPos);
//                System.out.println("track hit in " + name);
//                System.out.println("  global " + globalPos);
//                System.out.println("  local " + localPos);
                trackHitGlobalPosMap.put(name, globalPos);
                trackHitGlobalPosMap.put(name, localPos);
            }
        } // end of loop over hits...

        MCParticle mcp = mcParticles.get(0);
        double[] pos = {mcp.getOriginX(), mcp.getOriginY(), mcp.getOriginZ()};
        double[] mom = {mcp.getPX(), mcp.getPY(), mcp.getPZ()};
        double E = mcp.getEnergy();
        double q = mcp.getCharge();
        // create a physical track 
        PhysicalTrack ptrack = new PhysicalTrack(pos, mom, E, (int) q);
        // create a Lit Track
        double p = sqrt(mom[0] * mom[0] + mom[1] * mom[1] + mom[2] * mom[2]);
        CbmLitTrackParam parIn = new CbmLitTrackParam();

        double[] pars = new double[5];
        pars[0] = mcp.getOriginX(); //x
        pars[1] = mcp.getOriginY(); //y
        pars[2] = mom[0] / mom[2]; // x' (dx/dz)
        pars[3] = mom[1] / mom[2]; // y' (dy/dz)
        pars[4] = q / p; // q/p
        parIn.SetStateVector(pars);
        parIn.SetZ(mcp.getOriginZ());

        CbmLitTrackParam parOut = new CbmLitTrackParam();
        // loop over all of the SImTrackerHits associated with this MCParticle 
        // and try fitting them
        List<CbmLitStripHit> simtrackerhitZPlaneHits = new ArrayList<CbmLitStripHit>();

        for (SimTrackerHit sth : simTrackerHitList) {
            String sensorName = sth.getDetectorElement().getName();
            Hep3Vector globalPos = sth.getPositionVec();
            NewDetectorPlane dp = planeMap.get(sensorName);
            CartesianThreeVector simTrackerHitVec = new CartesianThreeVector(globalPos.x(), globalPos.y(), globalPos.z());
            Hep3Vector localPos = dp.globalToLocal(globalPos);
            double u = localPos.y() + ran.nextGaussian() * sigmaU;
            CbmLitStripHit sthhit = new CbmLitStripHit();
            double stereo = stereoAngleMap.get(sensorName);
            sthhit.SetPhi((PI / 2. - stereo)); // tracking code assumes X measurement with Phi measured from the Y axis
            //what if this u should be the GLOBAL u?
            double globalU = globalU(globalPos, dp.v());
            globalU += ran.nextGaussian() * sigmaU;
            if (_debug) {
                System.out.println("SimTrackerHit in " + sensorName);
                System.out.println("SimTrackerHit global position " + globalPos);
                System.out.println("SimTrackerHit local position " + localPos);
                System.out.println("SimTrackerHit local back to global position " + dp.localToGlobal(localPos));
                System.out.println("trackHit global position " + trackHitGlobalPosMap.get(sensorName));
                System.out.println("trackHit local position " + trackHitLocalPosMap.get(sensorName));
                System.out.println(sensorName + " stereo angle " + stereo);
                System.out.println("globalU " + globalU);
            }
            sthhit.SetU(globalU);
            sthhit.SetDu(sigmaU);
            sthhit.SetZ(globalPos.z());
            sthhit.SetDz(.0001);
            simtrackerhitZPlaneHits.add(sthhit);
        }
        //let's fit!
        CbmLitTrack simTrackerHitTrack = fitIt(simtrackerhitZPlaneHits, parIn);
//        System.out.println("simTrackerHitTrack " + simTrackerHitTrack);
        //extrapolate to zero, compare to MC, create residuals and pulls
        compare(simTrackerHitTrack, parIn, "simTrackerHitTrack");

    }

    public void setDebug(boolean b) {
        _debug = b;
    }

    private double globalU(Hep3Vector hitPos, Hep3Vector unmeasDir) {
        Line3D zAxis = new Line3D(new Point3D(0., 0., 0.), new BasicHep3Vector(0., 0., 1.));
        Line3D strip = new Line3D(new Point3D(hitPos), unmeasDir);
        return GeomOp3D.distanceBetween(zAxis, strip);
    }

    private void setup() {
        int id = 0;
        for (ScatteringDetectorVolume vol : _materialManager.getMaterialVolumes()) {
            SiStripPlane plane = (SiStripPlane) vol;
            String stripPlaneName = plane.getName();
            stripPlaneNameMap.put(stripPlaneName, plane);
            Hep3Vector unmeasDir = CoordinateTransformations.transformVectorToDetector(plane.getUnmeasuredCoordinate());
            Hep3Vector measDir = CoordinateTransformations.transformVectorToDetector(plane.getMeasuredCoordinate());
            Hep3Vector normal = CoordinateTransformations.transformVectorToDetector(plane.normal());
            //measDir points either up or down depending on orientation of the sensor. I want it always to point up, along y.
            // so let's check something here...
            if (measDir.y() < 0.) {
                measDir = VecOp.neg(measDir);
            }
            Hep3Vector tst = VecOp.cross(unmeasDir, measDir);
            // if pointing along the z axis, OK. if not, invert unmeasDir...
            if (tst.z() < 0.) {
                unmeasDir = VecOp.neg(unmeasDir);
            }
            normal = VecOp.cross(unmeasDir, measDir);
            if (_debug) {
                System.out.println(plane.getName());
                System.out.println("unmeasDir " + unmeasDir);
                System.out.println("measDir " + measDir);
                System.out.println("normal " + normal);
            }
            stereoAngleMap.put(plane.getName(), atan2(measDir.x(), measDir.y()));
            Hep3Vector origin = CoordinateTransformations.transformVectorToDetector(plane.origin());

            // extract the rotation angles...
            Vector3D vYprime = new Vector3D(unmeasDir.x(), unmeasDir.y(), unmeasDir.z());  // nominal x
            Vector3D vXprime = new Vector3D(measDir.x(), measDir.y(), measDir.z());   // nominal y
            // create a rotation matrix from this pair of vectors
            Rotation xyVecRot = new Rotation(vX, vY, vXprime, vYprime);
            double[] hpsAngles = xyVecRot.getAngles(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR);

            Matrix[] mats = new Matrix[3];
            double[][] RW = new double[3][9];
            for (int j = 0; j < 3; ++j) {
                double angl = hpsAngles[j];
                mats[j] = GEN_ROTMAT(angl, j);
                GEN_ROTMAT(angl, j, RW[j]);
//                if (_debug) {
//                    System.out.println("Rotation matrix for axis " + (j + 1) + " angle " + angl);
//
//                    mats[j].print(6, 4);
//                }
            }
            Matrix prodrot = PROD_ROT(mats[0], mats[1], mats[2]);
            // calculate zmin, zmax
            double width = plane.getUnmeasuredDimension() / 2.;
            double height = plane.getMeasuredDimension() / 2.;
            double[] bounds = findZBounds(origin, VecOp.mult(width, unmeasDir), VecOp.mult(height, measDir));
            double zmin = bounds[0];
            double zmax = bounds[1];
//            if (_debug) {
//                System.out.println(" " + stripPlaneName);
//                System.out.println("   origin: " + origin);
//                System.out.println("   normal: " + normal);
//                System.out.println("   uDir: " + measDir);
//                System.out.println("   vDir: " + unmeasDir);
//                System.out.println("   Apache commons angles: " + Arrays.toString(hpsAngles));
//                System.out.println("zmin " + zmin + " zmax " + zmax);
//            }
            NewDetectorPlane dp = new NewDetectorPlane(id++, prodrot, origin.v(), SIGS);
            dp.setName(stripPlaneName);
            dp.setUVWR(measDir, unmeasDir, normal, origin);
            dp.setDimensions(width, height, zmin, zmax);
            dp.setAngles(hpsAngles);
            if (_debug) {
                System.out.println("  " + dp);
            }
            planeMap.put(plane.getName(), dp);
        }

//        for (Tracker t : Tracker.values()) {
//            String s = t.trackerName();
//            if (_debug) {
//                System.out.println("building " + s + " : ");
//            }
//            String[] stripNames = sensorNameMap.get(s);
//            List<DetectorPlane> planes = new ArrayList<DetectorPlane>();
//            int id = 1;
//            for (String stripPlaneName : stripNames) {
//
//                SiStripPlane plane = stripPlaneNameMap.get(stripPlaneName);
//                if (_debug) {
//                    System.out.println(stripPlaneName);
//                }
//            }
//            trackerMap.put(s, planes);
//            if (_debug) {
//                System.out.println("");
//            }
//        }
        if (_debug) {
            System.out.println("Populated planemap with " + planeMap.size() + " planes");
        }
    }

    static Matrix PROD_ROT(Matrix rx, Matrix ry, Matrix rz) {
        return rz.times(ry.times(rx));
    }

    static Matrix GEN_ROTMAT(double ANG, int IAX) {
        Matrix r = Matrix.identity(3, 3);

//        System.out.println("ANG " + ANG + " IAX " + (IAX + 1));
        double C = cos(ANG);
        double S = sin(ANG);
        switch (IAX) {
            case 0:
                r.set(1, 1, C);
                r.set(1, 2, S);
                r.set(2, 1, -S);
                r.set(2, 2, C);
                break;
            case 1:
                r.set(0, 0, C);
                r.set(0, 2, -S);
                r.set(2, 0, S);
                r.set(2, 2, C);
                break;
            case 2:
                r.set(0, 0, C);
                r.set(0, 1, S);
                r.set(1, 0, -S);
                r.set(1, 1, C);
                break;
            default:
                break;
        }
//            r.print(10, 6);
        return r;
    }

    static void GEN_ROTMAT(double ANG, int IAX, double[] R) {
        double[][] r = new double[3][3];

//        System.out.println("ANG " + ANG + " IAX " + (IAX + 1));
        r[IAX][IAX] = 1;
        double C = cos(ANG);
        double S = sin(ANG);
        switch (IAX) {
            case 0:
                r[1][1] = C;
                r[1][2] = S;
                r[2][1] = -S;
                r[2][2] = C;
                break;
            case 1:
                r[0][0] = C;
                r[0][2] = -S;
                r[2][0] = S;
                r[2][2] = C;
                break;
            case 2:
                r[0][0] = C;
                r[0][1] = S;
                r[1][0] = -S;
                r[1][1] = C;
                break;
            default:
                break;
        }
        int n = 0;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                R[n++] = r[i][j];
            }
        }
    }

    public static double[] findZBounds(Hep3Vector origin, Hep3Vector width, Hep3Vector height) {
        Hep3Vector[] corners = new Hep3Vector[4];
        double zmin = 999.;
        double zmax = -999;
        // o + w*vDir + h*uDir

        Hep3Vector edge = VecOp.add(origin, width);
        corners[0] = VecOp.add(edge, height);
        corners[1] = VecOp.sub(edge, height);
        edge = VecOp.sub(origin, width);
        corners[2] = VecOp.add(edge, height);
        corners[3] = VecOp.sub(edge, height);

        for (int i = 0; i < 4; ++i) {
            //System.out.println("corner " + i + " : " + corners[i]);
            if (corners[i].z() > zmax) {
                zmax = corners[i].z();
            }
            if (corners[i].z() < zmin) {
                zmin = corners[i].z();
            }
        }
        //System.out.println("zmin " + zmin + " zmax " + zmax);
        return new double[]{zmin, zmax};
    }

    private void setupSensors(EventHeader event) {
        List<RawTrackerHit> rawTrackerHits = null;
        if (event.hasCollection(RawTrackerHit.class, "SVTRawTrackerHits")) {
            rawTrackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        }
        if (event.hasCollection(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits")) {
            rawTrackerHits = event.get(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits");
        }
        EventHeader.LCMetaData meta = event.getMetaData(rawTrackerHits);
        // Get the ID dictionary and field information.
        IIdentifierDictionary dict = meta.getIDDecoder().getSubdetector().getDetectorElement().getIdentifierHelper().getIdentifierDictionary();
        int fieldIdx = dict.getFieldIndex("side");
        int sideIdx = dict.getFieldIndex("strip");
        for (RawTrackerHit hit : rawTrackerHits) {
            // The "side" and "strip" fields needs to be stripped from the ID for sensor lookup.
            IExpandedIdentifier expId = dict.unpack(hit.getIdentifier());
            expId.setValue(fieldIdx, 0);
            expId.setValue(sideIdx, 0);
            IIdentifier strippedId = dict.pack(expId);
            // Find the sensor DetectorElement.
            List<IDetectorElement> des = DetectorElementStore.getInstance().find(strippedId);
            if (des == null || des.size() == 0) {
                throw new RuntimeException("Failed to find any DetectorElements with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            } else if (des.size() == 1) {
                hit.setDetectorElement((SiSensor) des.get(0));
            } else {
                // Use first sensor found, which should work unless there are sensors with duplicate IDs.
                for (IDetectorElement de : des) {
                    if (de instanceof SiSensor) {
                        hit.setDetectorElement((SiSensor) de);
                        break;
                    }
                }
            }
            // No sensor was found.
            if (hit.getDetectorElement() == null) {
                throw new RuntimeException("No sensor was found for hit with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            }
        }
    }

    private void compare(CbmLitTrack track, CbmLitTrackParam mcp, String folder) {
        _tree.mkdirs(folder);
        _tree.cd(folder);
        // get the upstream track parameters
        CbmLitTrackParam tp1 = track.GetParamFirst();
        // output parameters
        CbmLitTrackParam tAtOrigin = new CbmLitTrackParam();
        // find z where we should compare
        double z = mcp.GetZ();
        // extrapolate our track to this z position
        // transport matrix
        double[] F = new double[25];
        _extrap.Extrapolate(tp1, tAtOrigin, z, F);
        if (_debug) {
            System.out.println("MC parameters             : " + mcp);
            System.out.println("track parameters at origin: " + tAtOrigin);
        }
        double[] mcStateVector = mcp.GetStateVector();
        double[] tStateVector = tAtOrigin.GetStateVector();
        String[] label = {"x", "y", "tx", "ty", "qp"};
        double[] covMat = tAtOrigin.GetCovMatrix();
        int[] index = {0, 5, 9, 12, 14};
        //                  x      y     dx/dz   dy/dz    q/p
        double[] minVal = {-0.5, -0.05, -0.005, -0.0001, -0.05};// x,y,tx,ty,q/p
        double[] maxVal = {0.5, 0.05, 0.005, 0.0001, 0.05};

        double[] minValTrack = {-0.5, -0.05, -0.2, 0.01, -1.0};// x,y,tx,ty,q/p
        double[] maxValTrack = {0.5, 0.05, 0.2, 0.06, 0.0};

        for (int i = 0; i < 5; ++i) {
//            aida.cloud1D(label[i] + " MC").fill(mcStateVector[i]);
//            aida.cloud1D(label[i] + " residual").fill(tStateVector[i] - mcStateVector[i]);
//            aida.cloud1D(label[i] + " pull").fill((tStateVector[i] - mcStateVector[i]) / sqrt(covMat[index[i]]));
            aida.histogram1D(label[i] + " MC", 100, minValTrack[i], maxValTrack[i]).fill(mcStateVector[i]);
            aida.histogram1D(label[i] + " track", 100, minValTrack[i], maxValTrack[i]).fill(tStateVector[i]);
            aida.histogram1D(label[i] + " residual", 100, minVal[i], maxVal[i]).fill(tStateVector[i] - mcStateVector[i]);
            aida.histogram1D(label[i] + " pull", 100, -5., 5.).fill((tStateVector[i] - mcStateVector[i]) / sqrt(covMat[index[i]]));
        }
        double chisq = track.GetChi2();
        int ndf = track.GetNDF();
        aida.histogram1D("Chisq", 100, 0., 25.).fill(chisq);
        aida.histogram1D("Chisq Probability", 100, 0., 1.).fill(ChisqProb.gammq(ndf, chisq));
        aida.histogram1D("Momentum", 100, 2.15, 2.5).fill(abs(1. / tStateVector[4]));
        _tree.cd("/");
    }

    private CbmLitTrack fitIt(List<CbmLitStripHit> hits, CbmLitTrackParam parIn) {
        // create a track
        CbmLitTrack track = new CbmLitTrack();
        // add the hits
        for (CbmLitHit hit : hits) {
            track.AddHit(hit);
        }
        // add start and end states
        CbmLitTrackParam defaultStartParams = new CbmLitTrackParam();
        if (parIn != null) {
            defaultStartParams = new CbmLitTrackParam(parIn);
        }
        CbmLitTrackParam defaultEndParams = new CbmLitTrackParam();
        defaultEndParams.SetZ(hits.get(hits.size() - 1).GetZ());
        track.SetParamFirst(defaultStartParams);
        track.SetParamLast(defaultEndParams);
        // fit downstream
        LitStatus status = _fitter.Fit(track, true);
        //      LitStatus status = _iterFitter.Fit(track);
        if (_debug) {
            System.out.println("zPlane fit downstream: " + status);
            System.out.println(track);
            System.out.println(track.GetParamLast());
        }
        //fit upstream
        //need to reset the covariance matrix so we don't overfit...
        track.GetParamLast().SetCovMatrix(defaultStartParams.GetCovMatrix());
        status = _fitter.Fit(track, false);
        if (_debug) {
            System.out.println("zPlane fit upstream: " + status);
            System.out.println(track);
            System.out.println(track.GetParamFirst());
        }
        return track;
    }

}

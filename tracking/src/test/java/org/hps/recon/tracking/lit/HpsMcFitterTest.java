package org.hps.recon.tracking.lit;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import junit.framework.TestCase;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MultipleScattering;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
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
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class HpsMcFitterTest extends TestCase
{

    public void testIt() throws Exception
    {
        System.out.println("Running from: " + Paths.get("").toAbsolutePath());
//        String fileName = "http://www.lcsim.org/test/hps-java/singleFullEnergyElectrons_SLIC-v05-00-00_Geant4-v10-01-02_QGSP_BERT_HPS-EngRun2015-Nominal-v2-fieldmap_minINteractions_recon.slcio";
//        String fileName = "http://www.lcsim.org/test/hps-java/e-_1.056GeV_SLIC-v05-00-00_Geant4-v10-00-02_QGSP_BERT_HPS-EngRun2015-Nominal-v2-fieldmap_recon.slcio";
        String fileName = "C:/hps_data/MC/e-_1.02776GeV_SLIC-v05-00-00_Geant4-v10-00-02_QGSP_BERT_HPS-EngRun2015-Nominal-v4-4-fieldmap_50k_nomsc_recon.slcio";
        FileCache cache = new FileCache();
        int nEvents = 1;
        LCSimLoop loop = new LCSimLoop();
        HpsMcFitter d = new HpsMcFitter();
        loop.add(d);
        try {
            File inputFile = null;
            if(fileName.startsWith("http"))
            {
             inputFile = cache.getCachedFile(new URL(fileName));
            }
            else
            {
                inputFile = new File(fileName);
            }
            loop.setLCIORecordSource(inputFile);
            loop.loop(nEvents);
            // d.showPlots();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Loop processed " + loop.getTotalSupplied() + " events.");
        System.out.println("Done!");
    }
}

/*
 * 
 * This class is intended to test the mechanics of the track propagation and fitting
 * using MC particles propagated analytically, with their hits smeared with a gaussian.
 *
 */
class HpsMcFitter extends Driver
{

    private boolean _debug = true;

    private MaterialSupervisor _materialManager = null;
    private MultipleScattering _scattering = null;
    private Map<Long, Hep3Matrix> rotMap = new HashMap<Long, Hep3Matrix>();
    private Map<Long, Hep3Vector> tranMap = new HashMap<Long, Hep3Vector>();
    private Map<Long, ITransform3D> xformMap = new HashMap<Long, ITransform3D>();
    private Map<Long, String> xformNames = new HashMap<Long, String>();

    private Hep3Vector uHat = new BasicHep3Vector(1, 0, 0);
    private Hep3Vector wHat = new BasicHep3Vector(0, 0, 1);

    private HpsDetector _det;
    private HpsDetectorNavigator _navi;
    private CbmLitRK4TrackExtrapolator _extrap;
//    private Map<String, DetectorPlane> planes;

    private double _eCalZPosition = 1338.;

    private Random ran = new Random();

    protected void detectorChanged(Detector detector)
    {
        _materialManager = new MaterialSupervisor();
        _scattering = new MultipleScattering(_materialManager);
        _materialManager.buildModel(detector);
        // get the rotation and translation quantities for the detectors.
//        setupTransforms(detector);
        _det = new HpsDetector(detector);
        CbmLitField field = _det.magneticField();
        _extrap = new CbmLitRK4TrackExtrapolator(field);
        _navi = new HpsDetectorNavigator(_det);
        System.out.println(_det);
    }    
    
    protected void process(EventHeader event)
    {
        CbmLitField field = _det.magneticField();
        double[] bField = new double[3];
        
        // get the list of planes from the reconstructed track
        setupSensors(event);
        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);
        TreeSet<DetectorPlane> struckPlanes = new TreeSet<DetectorPlane>();
        List<Track> tracks = event.get(Track.class, "MatchedTracks");
        if (tracks.size() != 1) {
            return;
        }
        //
        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, "TrackerHits");
        System.out.println("found "+simHits.size()+ " SimTrackerHits");
        List<Hep3Vector> simPositionList = new ArrayList<Hep3Vector>();
        for(SimTrackerHit h : simHits)
        {
            System.out.println("simTrackerHit position "+h.getPositionVec());
            double[] pos = h.getPosition();
            field.GetFieldValue(pos[0], pos[1], pos[2],bField);
            System.out.println("Field at position: "+bField[0] + " "+bField[1]+" "+bField[2]);
            simPositionList.add(h.getPositionVec());
        }
        //
        for (Track t : tracks) {
            List<TrackerHit> hits = t.getTrackerHits();
            if (hits.size() != 6) {
                return;
            }
            for (TrackerHit h : hits) {
                Set<TrackerHit> stripList = hitToStrips.allFrom(hitToRotated.from(h));
                for (TrackerHit strip : stripList) {
                    List rawHits = strip.getRawHits();
                    HpsSiSensor sensor = null;
                    for (Object o : rawHits) {
                        RawTrackerHit rth = (RawTrackerHit) o;
                        // TODO figure out why the following collection is always null
                        //List<SimTrackerHit> stipMCHits = rth.getSimTrackerHits();
                        sensor = (HpsSiSensor) rth.getDetectorElement();
                    }
                    struckPlanes.add(_det.getPlane(sensor.getName()));
                }
            }
        }
        // now have list of names of detector planes
        System.out.println("found " + struckPlanes.size() + " planes to extrapolate to");
        // loop over the MC particles and propagate each to every detector plane
        List<MCParticle> mcParticles = event.get(MCParticle.class, "MCParticle");
        for (MCParticle mcp : mcParticles) {
            // the starting track state...
            CbmLitTrackParam trkParam = mCToTrackParam(mcp);
            System.out.println("propagating MC "+trkParam);
            // a container to hold the propagated track states
            List<CbmLitTrackParam> trackStateList = new ArrayList<CbmLitTrackParam>();
            // a container to hold the hits
            List<HpsStripHit> stripHitList = new ArrayList<HpsStripHit>();
            // loop over all the detectors in the HPS detector, extrapolating to each in turn...
            double lastZ = -999.;
            for (DetectorPlane p : struckPlanes) {
//                System.out.println("propagating to " + p);
                // the resulting track State
                CbmLitTrackParam trkParamOut = new CbmLitTrackParam();
                if (_extrap.Extrapolate(trkParam, trkParamOut, p, null) == LitStatus.kLITSUCCESS) {
                    System.out.println("intersected " + p.name() + " at " + trkParamOut);
                    double u = p.u(trkParamOut.GetX(), trkParamOut.GetY(), trkParamOut.GetZ());
                    double du = .00001;
                    // smear this projection by a gaussian
                    u += du * ran.nextGaussian();
                    stripHitList.add(new HpsStripHit(u, du, p));
                    lastZ = trkParamOut.GetZ();
                }
            }
            // should now have a list of 12 strip hits.
            System.out.println("stripHitList has " + stripHitList.size() + " hits");
            // now let's try to fit these hits...
            // make a track
            HpsLitTrack track = new HpsLitTrack();
            // add the hits
            for (HpsStripHit h : stripHitList) {
                track.AddHit(h);
            }
            //evidently never used...
            // track.SetNofHits(stripHitList.size());
            //
            // need track states at beginning and end
            // start with basic defaults

            CbmLitTrackParam defaultStartParams = new CbmLitTrackParam();
            CbmLitTrackParam defaultEndParams = new CbmLitTrackParam();
            defaultEndParams.SetZ(lastZ);
            track.SetParamFirst(defaultStartParams);
            track.SetParamLast(defaultEndParams);

            // a Kalman Filter updater...
            HpsLitKalmanFilter kf = new HpsLitKalmanFilter();
            // we have a Runge-Kutta extrapolator
            // we need a propagator...
            HpsTrackPropagator prop = new HpsTrackPropagator(_extrap);
            // a fitter...
            HpsLitTrackFitter fitter = new HpsLitTrackFitter(prop, kf);
            //fit downstream...
            fitter.Fit(track, true);
            //fit upstream...
            fitter.Fit(track, false);
            if (_debug) {
                System.out.println(" track after fit: ");
                System.out.println(track);
            }
            CbmLitFitNode[] fitNodeList = track.GetFitNodes();
            if (_debug) {
                System.out.println(" track fit nodes: ");

                for (int i = 0; i < fitNodeList.length; ++i) {
                    System.out.println(fitNodeList[i]);
                }
            }

            // and a smoother...
            //CbmLitTrackFitter smoother = new CbmLitKalmanSmoother();
        }
    }

    public CbmLitTrackParam mCToTrackParam(MCParticle mcp)
    {
        CbmLitTrackParam trkParam = new CbmLitTrackParam();
        Hep3Vector mom = mcp.getMomentum();
        Hep3Vector pos = mcp.getOrigin();
        trkParam.SetX(pos.x()); // x
        trkParam.SetY(pos.y()); // y
        trkParam.SetZ(pos.z()); // z
        trkParam.SetTx(mom.x() / mom.z()); // dx/dz
        trkParam.SetTy(mom.y() / mom.z()); //dy/dz
        trkParam.SetQp(mcp.getCharge() / mom.magnitude());
        return trkParam;
    }


//    private void setupTransforms(Detector det)
//    {
//        List<MaterialSupervisor.ScatteringDetectorVolume> stripPlanes = _materialManager.getMaterialVolumes();
//        for (MaterialSupervisor.ScatteringDetectorVolume vol : stripPlanes) {
//            MaterialSupervisor.SiStripPlane plane = (MaterialSupervisor.SiStripPlane) vol;
//
//            if (_debug) {
//                System.out.println(plane.getName());
//            }
//
//            Hep3Vector oprime = CoordinateTransformations.transformVectorToDetector(plane.origin());
//            Hep3Vector nprime = CoordinateTransformations.transformVectorToDetector(plane.normal());
//
//            if (_debug) {
//                System.out.println(" origin: " + oprime);
//            }
//
//            if (_debug) {
//                System.out.println(" normal: " + nprime);
//            }
//
//            if (_debug) {
//                System.out.println(" Plane is: " + plane.getMeasuredDimension() + " x " + plane.getUnmeasuredDimension());
//            }
//
//            HpsSiSensor sensor = (HpsSiSensor) plane.getSensor();
//            // create a DetectorPlane object
//            String name = sensor.getName();
//            //TODO fix the number of radiation lengths here
//            double x0 = .003;
//            ITransform3D l2g = sensor.getGeometry().getLocalToGlobal();
//            ITransform3D g2l = sensor.getGeometry().getGlobalToLocal();
//            
//            CartesianThreeVector pos = new CartesianThreeVector(oprime.x(), oprime.y(), oprime.z());
//            CartesianThreeVector normal = new CartesianThreeVector(nprime.x(), nprime.y(), nprime.z());
//            double x = plane.getMeasuredDimension();
//            Hep3Vector unmeasDir = CoordinateTransformations.transformVectorToDetector(plane.getMeasuredCoordinate());
//            double y = plane.getUnmeasuredDimension();
//            Hep3Vector measDir = CoordinateTransformations.transformVectorToDetector(plane.getUnmeasuredCoordinate());
//            
//            // test some things
//            Hep3Vector origin = new BasicHep3Vector(0.,0.,0.);
//            Hep3Vector l2gOrigin = l2g.transformed(origin);
//            System.out.println("origin: "+origin);
//            System.out.println("l2g origin: "+l2gOrigin);
//            System.out.println("plane origin: "+plane.origin());
//            System.out.println("oprime: "+oprime);
//            
//            System.out.println("plane measured dir "+plane.getMeasuredCoordinate());
//            System.out.println("l2g plane measured dir "+VecOp.mult(l2g.getRotation().getRotationMatrix(),plane.getMeasuredCoordinate()));
//            System.out.println("measDir: "+measDir);
//            System.out.println("sensor measDir "+sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getMeasuredCoordinate(0));
//            Hep3Vector sensorMeasDir = VecOp.mult(l2g.getRotation().getRotationMatrix(),sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getMeasuredCoordinate(0));
//            System.out.println("l2g sensor measDir "+sensorMeasDir);
//            Hep3Vector sensorUnMeasDir =VecOp.mult(l2g.getRotation().getRotationMatrix(),sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getUnmeasuredCoordinate(0));
//            System.out.println("l2g sensor unMeasDir "+sensorUnMeasDir);
//            Hep3Vector cross = VecOp.cross(sensorMeasDir, sensorUnMeasDir);
//            System.out.println("meas cross unmeas "+cross);
//            //OK, this agrees with the normal above, nprime...
//            // test some things
//            
//            DetectorPlane dPlane = new DetectorPlane(name, pos, normal, l2g, g2l, x0, unmeasDir, x, measDir, y);
//            if (_debug) {
//                System.out.println(dPlane);
//            }
//            planes.put(name, dPlane);
//            _det.addDetectorPlane(dPlane);
//            long ID = sensor.getIdentifier().getValue();
//            xformMap.put(ID, l2g);
//            rotMap.put(ID, l2g.getRotation().getRotationMatrix());
//            tranMap.put(ID, l2g.getTranslation().getTranslationVector());
//            xformNames.put(ID, sensor.getName());
//
////            if (debug) {
////                if(_debug) System.out.println(SvtUtils.getInstance().isAxial(sensor) ? "axial" : "stereo");
////            }
//            if (_debug) {
//                System.out.println("measured coordinate:    " + measDir);
//            }
//
//            if (_debug) {
//                System.out.println("unmeasured coordinate:   " + unmeasDir);
//            }
//
//            if (_debug) {
//                System.out.println("thickness: " + plane.getThickness() + " in X0: " + plane.getThicknessInRL());
//            }
//
//            if (_debug) {
//                // try calculating the phi (stereo) angle
//                Hep3Vector o = new BasicHep3Vector(0, 0, 0);
//                Hep3Vector p = new BasicHep3Vector(1, 0, 0);
//
//            }
//
//        }
//        _navi = new HpsDetectorNavigator(_det);
//    }

    private void setupSensors(EventHeader event)
    {
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
}

package org.hps.recon.tracking.ztrack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author ngraf
 */
public class HpsAnalysisDriver extends Driver {

    HpsDetector _hpsDetector;
    HpsDetectorNavigator _hpsDetectorNavigator;
    TrfField _trfField;
    boolean _debug = false;
    private AIDA aida = AIDA.defaultInstance();

    protected void detectorChanged(Detector detector) {
        _hpsDetector = new HpsDetector(detector);
        _hpsDetectorNavigator = new HpsDetectorNavigator(_hpsDetector);
        if (_debug) {
            System.out.println(_hpsDetector);
        }
        _trfField = new TrfField(detector.getFieldMap());
    }

    protected void process(EventHeader event) {
        List<SimTrackerHit> simTrackerHits = event.get(SimTrackerHit.class, "TrackerHits");
        List<SimTrackerHit> simTrackerHitsAtEcal = event.get(SimTrackerHit.class, "TrackerHitsECal");
        List<MCParticle> mcparts = event.get(MCParticle.class, "MCParticle");
        if (_debug) {
            System.out.println("found " + mcparts.size() + "MC Particles with " + simTrackerHits.size() + " SimTrackerHits");
        }

        Map<String, SimTrackerHit> simTrackerHitMap = new HashMap<String, SimTrackerHit>();
        List<String> mcTrackPlaneNames = new ArrayList<String>();
        for (SimTrackerHit sth : simTrackerHits) {
            if (_debug) {
                System.out.println(sth.getDetectorElement().getName());
            }
            simTrackerHitMap.put(sth.getDetectorElement().getName(), sth);
            mcTrackPlaneNames.add(sth.getDetectorElement().getName());
        }

        // create a ZTrackParam object for this MCParticle
        //TODO move this to the constructor maybe?
        MCParticle mcp = mcparts.get(0);

        ZTrackParam TrackParamIn = new ZTrackParam(mcp.getOrigin().v(), mcp.getMomentum().v(), (int) mcp.getCharge());
        if (_debug) {
            System.out.println("Starting ZTrackParam " + TrackParamIn);
        }

        // create a track extrapolator
        RK4TrackExtrapolator extrap = new RK4TrackExtrapolator(_hpsDetector.magneticField());

//        // get the list of detector planes to traverse...
//        List<DetectorPlane> planesToTest = new ArrayList<DetectorPlane>();
//
//        Status stat = _hpsDetectorNavigator.FindIntersections(TrackParamIn, _hpsDetector.zMax(), planesToTest);
//
//        if (_debug) {
//            System.out.println("found " + planesToTest.size() + " planes to propagate to");
//            for (DetectorPlane p : planesToTest) {
//                System.out.println(p.name());
//            }
//        }
        // since this returns a list of ALL the planes, let's wait until that gets fixed and simply
        // go from plane-to-plane.
        // let's compare to the trf propagator
//        PropZZRK prop = new PropZZRK(_trfField);
//        TrackVector vec1 = new TrackVector();
//        // NOTE trf uses cm, hps uses mm
//        vec1.set(0, TrackParamIn.GetX() / 10.);    // x
//        vec1.set(1, TrackParamIn.GetY() / 10.);    // y
//        vec1.set(2, TrackParamIn.GetTx());   // dx/dz
//        vec1.set(3, TrackParamIn.GetTy());   // dy/dz
//        vec1.set(4, TrackParamIn.GetQp());   // q/p
//        // create a VTrack at the particle's origin.
//
//        SurfZPlane zp0 = new SurfZPlane(TrackParamIn.GetZ() / 10.);
//        TrackSurfaceDirection tdir = TrackSurfaceDirection.TSD_FORWARD;
//        VTrack trv0 = new VTrack(zp0.newPureSurface(), vec1, tdir);
//
//        VTrack trv1 = new VTrack(trv0);
//        if (_debug) {
//            System.out.println(" starting: " + trv1);
//        }
//        PropDir dir = PropDir.FORWARD;
        // create a track propagator here...
        HpsTrackPropagator trackPropagator = new HpsTrackPropagator(extrap);

        // now propagate/extrapolate and compare
        ZTrackParam TrackParamOut = new ZTrackParam();
        ZTrackParam TrackParamRunning = new ZTrackParam(TrackParamIn);
        for (String planeName : mcTrackPlaneNames) {
//            Status extrapStat = extrap.Extrapolate(TrackParamIn, TrackParamOut, _hpsDetector.getPlane(planeName), null);
            Status propStat = trackPropagator.Propagate(TrackParamRunning, _hpsDetector.getPlane(planeName), null);
            if (propStat == Status.SUCCESS) {
//            PropStat pstat = prop.vecDirProp(trv1, new SurfZPlane(simTrackerHitMap.get(planeName).getPositionVec().z() / 10.), dir);
                double dx = simTrackerHitMap.get(planeName).getPositionVec().x() - TrackParamRunning.GetX();
                double dy = simTrackerHitMap.get(planeName).getPositionVec().y() - TrackParamRunning.GetY();
                double dz = simTrackerHitMap.get(planeName).getPositionVec().z() - TrackParamRunning.GetZ();
                aida.histogram1D("prop " + planeName + " dx", 100, -0.03, 0.03).fill(dx);
                aida.histogram1D("prop " + planeName + " dy", 100, -0.005, 0.005).fill(dy);
                aida.histogram1D("prop " + planeName + " dz", 100, -0.002, 0.002).fill(dz);
                aida.histogram2D("prop " + planeName + " dx vs dy", 100, -0.1, 0.1, 100, -0.01, 0.01).fill(dx, dy);
//                aida.cloud1D("prop " + planeName + " dx").fill(dx);
//                aida.cloud1D("prop " + planeName + " dy").fill(dy);
//                aida.cloud1D("prop " + planeName + " dz").fill(dz);
//                aida.cloud2D("prop " + planeName + " dx vs dy").fill(dx, dy);
            } else {
                System.out.println("error propagating to " + planeName + " in event " + event.getEventNumber());
            }
            if (_debug) {
                System.out.println("extrapolating to " + planeName);
//                System.out.println(extrapStat);
//                System.out.println("parOut " + TrackParamOut);
                System.out.println(propStat);
                System.out.println("parRun " + TrackParamRunning);
//                System.out.println("trf vecOut: " + trv1);
//                System.out.println(pstat);
                SimTrackerHit sth = simTrackerHitMap.get(planeName);
                double[] sthMom = sth.getMomentum();
                double tx = sthMom[0] / sthMom[2];
                double ty = sthMom[1] / sthMom[2];

                System.out.println(sth.getPositionVec() + " " + tx + " " + ty);
            }
        }
    }

    public void setDebug(boolean b) {
        _debug = b;
    }
}

package org.hps.recon.tracking.lit;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MultipleScattering;
import org.hps.recon.tracking.ztrack.TrfField;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.recon.tracking.trfbase.PropDir;
import org.lcsim.recon.tracking.trfbase.PropStat;
import org.lcsim.recon.tracking.trfbase.TrackSurfaceDirection;
import org.lcsim.recon.tracking.trfbase.TrackVector;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trfzp.PropZZRK;
import org.lcsim.recon.tracking.trfzp.SurfZPlane;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A. Graf
 */
public class HpsExtrapolatorDriver extends Driver {

    private boolean _debug = false;
    private boolean trackit = false;
    AIDA aida = AIDA.defaultInstance();

    private MaterialSupervisor _materialManager = null;
    private MultipleScattering _scattering = null;
    private CbmLitRK4TrackExtrapolator _extrap;
    private HpsDetector _det;
    private Map<String, DetectorPlane> detectorPlaneMap = new HashMap<String, DetectorPlane>();

    // trf stuff
    PropZZRK _trfpropZZRK;

    protected void detectorChanged(Detector detector) {
        _materialManager = new MaterialSupervisor();
        _scattering = new MultipleScattering(_materialManager);
        _materialManager.buildModel(detector);

        HpsMagField field = new HpsMagField(detector.getFieldMap());
        _extrap = new CbmLitRK4TrackExtrapolator(field);

        // a Kalman Filter updater...
        CbmLitTrackUpdate trackUpdate = new CbmLitKalmanFilter();
        CbmLitTrackPropagator prop = new SimpleTrackPropagator(_extrap);
//        _fitter = new CbmLitTrackFitterImp(prop, trackUpdate);
//        _smoother = new CbmLitKalmanSmoother();
//        _iterFitter = new CbmLitTrackFitterIter(_fitter, _smoother);

        _det = new HpsDetector(detector);
        List<DetectorPlane> planeList = _det.getPlanes();
        for (DetectorPlane p : planeList) {
            detectorPlaneMap.put(p.name(), p);
        }
        System.out.println(_det);

        //trf stuff
        //NOTE that trf uses cm as default
        _trfpropZZRK = new PropZZRK(new TrfField(detector.getFieldMap()));
    }

    protected void process(EventHeader event) {
        List<MCParticle> mcParticles = event.get(MCParticle.class, "MCParticle");
        if (mcParticles.size() != 1) {
            return;
        }
        MCParticle mcp = mcParticles.get(0);
        MCParticle.SimulatorStatus simstat = mcp.getSimulatorStatus();
        if (simstat.isDecayedInCalorimeter() || simstat.hasLeftDetector()) {
            FieldMap fieldmap = event.getDetector().getFieldMap();
            if (_debug) {
                System.out.println("Event: " + event.getEventNumber());
            }
            setupSensors(event);
            List<SimTrackerHit> simTrackerHitList = event.get(SimTrackerHit.class, "TrackerHits");
            List<SimTrackerHit> simTrackerHitsAtEcal = event.get(SimTrackerHit.class, "TrackerHitsECal");

            IDDecoder trackerDecoder = event.getMetaData(simTrackerHitList).getIDDecoder();

            int nSimTrackerHits = simTrackerHitList.size();

            Hep3Vector mcMom = mcp.getMomentum();
            Hep3Vector mcPos = mcp.getOrigin();

            double mcE = mcp.getEnergy();
            int charge = (int) mcp.getCharge();

            if (_debug) {
                System.out.println("  MCParticle: " + "position: " + mcPos + " "
                        + " momentum: " + mcMom + " mag field " + fieldmap.getField(mcPos));
            }

            // start parameters for this MCParticle
            CbmLitTrackParam mcpParams = new CbmLitTrackParam();

            double[] pars = new double[5];
            pars[0] = mcp.getOriginX(); //x
            pars[1] = mcp.getOriginY(); //y
            pars[2] = mcMom.x() / mcMom.z(); // x' (dx/dz)
            pars[3] = mcMom.y() / mcMom.z(); // y' (dy/dz)
            pars[4] = charge / mcMom.magnitude(); // q/p
            mcpParams.SetStateVector(pars);
            mcpParams.SetZ(mcp.getOriginZ());

            CbmLitTrackParam parIn = new CbmLitTrackParam(mcpParams);
            CbmLitTrackParam parOut = new CbmLitTrackParam();

            // trf stuff note that trf distance units are cm.
            TrackVector vec1 = new TrackVector();
            vec1.set(0, pars[0] / 10.);    // x
            vec1.set(1, pars[1] / 10.);    // y
            vec1.set(2, pars[2]);   // dx/dz
            vec1.set(3, pars[3]);   // dy/dz
            vec1.set(4, pars[4]);   // q/p
            // create a VTrack at the particle origin.

            SurfZPlane zp0 = new SurfZPlane(mcp.getOriginZ() / 10.);
            TrackSurfaceDirection tdir = TrackSurfaceDirection.TSD_FORWARD;
            VTrack trfv0 = new VTrack(zp0.newPureSurface(), vec1, tdir);

            VTrack trfv1 = new VTrack(trfv0);
//        System.out.println(" starting: " + trfv1);
            PropDir trfDir = PropDir.FORWARD;

            // start with same parameters as MCarticle, update later
            CbmLitTrackParam simtrackerhitParams = new CbmLitTrackParam(parIn);
            // let's create a PhysicalTrack so we can extrapolate it
            PhysicalTrack pTrack = new PhysicalTrack(mcPos.v(), mcMom.v(), mcE, charge);

            //let's create an extrapolator...
            HpsMagField field = new HpsMagField(fieldmap);
            CbmLitRK4TrackExtrapolator extrap = new CbmLitRK4TrackExtrapolator(field);

            SimTrackerHit lastsimtrackerhit = null;
            // let's investigate our SimTrackerHits...
            for (SimTrackerHit simtrackerhit : simTrackerHitList) {
                // layer number of the hit
                trackerDecoder.setID(simtrackerhit.getCellID64());
                int layer = trackerDecoder.getValue("layer");
                //Subdetector subdetector = trackerDecoder.getSubdetector();
                String sensorName = simtrackerhit.getDetectorElement().getName();
                System.out.println(sensorName);
                // position of the hit
                Hep3Vector simtrackerhitPos = simtrackerhit.getPositionVec();

                // hit momentum
                Hep3Vector simtrackerhitMom = new BasicHep3Vector(simtrackerhit.getMomentum()[0], simtrackerhit.getMomentum()[1], simtrackerhit.getMomentum()[2]);
                aida.histogram1D("layer " + layer + " MC momentum ", 200, 4.553, 4.556).fill(simtrackerhitMom.magnitude());
                Hep3Vector magField = fieldmap.getField(simtrackerhitPos);
                if (_debug) {
                    System.out.println("*** SimTrackerHit  layer: " + layer + "; "
                            + "position: " + simtrackerhitPos + "; "
                            + "momentum: " + simtrackerhitMom + " mag field " + magField);
                }
                //let's try extrapolating our track to these nominal z-positions
                CartesianThreeVector planePos = new CartesianThreeVector(0., 0., simtrackerhitPos.z());
                CartesianThreeVector planeNorm = new CartesianThreeVector(0., 0., 1.); // zPlane
                DetectorPlane dp = new DetectorPlane(("layer" + layer), planePos, planeNorm, 0., 0.);

                LitStatus stat = _extrap.Extrapolate(mcpParams, parOut, dp, null);
                if (_debug) {
                    System.out.println("Propagating MCParticle " + mcpParams);
                    System.out.println(dp.name());
                    System.out.println("MC propagated  pos " + parOut.GetX() + " " + parOut.GetY() + " " + parOut.GetZ());
                }
                aida.histogram1D("layer " + layer + " MC propagated X meas-prop", 100, -2.0, 2.0).fill(simtrackerhitPos.x() - parOut.GetX());
                aida.histogram1D("layer " + layer + " MC propagated Y meas-prop", 100, -0.02, 0.02).fill(simtrackerhitPos.y() - parOut.GetY());
                aida.histogram1D("layer " + layer + " MC propagated Z meas-prop", 100, -0.01, 0.01).fill(simtrackerhitPos.z() - parOut.GetZ());

                //Now let's check on propagation from plane to plane...
                stat = _extrap.Extrapolate(parIn, parOut, dp, null);
                if (_debug) {
                    System.out.println("Propagating MCParticle state " + parIn);
                    System.out.println(dp.name());
                    System.out.println("parIn propagated  pos " + parOut.GetX() + " " + parOut.GetY() + " " + parOut.GetZ());
                }
                CbmLitTrackParam parTemp = new CbmLitTrackParam(parIn);
                parIn = new CbmLitTrackParam(parOut);
                aida.histogram1D("layer " + layer + " propagated X meas-prop", 100, -0.04, 0.04).fill(simtrackerhitPos.x() - parOut.GetX());
                aida.histogram1D("layer " + layer + " propagated Y meas-prop", 100, -0.02, 0.02).fill(simtrackerhitPos.y() - parOut.GetY());
                aida.histogram1D("layer " + layer + " propagated Z meas-prop", 100, -0.01, 0.01).fill(simtrackerhitPos.z() - parOut.GetZ());

                // let's try propagating to the actual (tilted) HPS sensor planes and see how close we get...
                DetectorPlane hpsPlane = detectorPlaneMap.get(sensorName);
                stat = _extrap.Extrapolate(parTemp, parOut, hpsPlane, null);
                aida.histogram1D("layer " + layer + " " + sensorName + " propagated X meas-prop", 100, -0.04, 0.04).fill(simtrackerhitPos.x() - parOut.GetX());
                aida.histogram1D("layer " + layer + " " + sensorName + " propagated Y meas-prop", 100, -0.02, 0.02).fill(simtrackerhitPos.y() - parOut.GetY());
                aida.histogram1D("layer " + layer + " " + sensorName + " propagated Z meas-prop", 100, -0.01, 0.01).fill(simtrackerhitPos.z() - parOut.GetZ());

                aida.histogram1D("layer " + layer + " vs " + sensorName + " propagated X meas-prop", 100, -0.001, 0.001).fill(parIn.GetX() - parOut.GetX());
                aida.histogram1D("layer " + layer + " vs " + sensorName + " propagated Y meas-prop", 100, -0.001, 0.001).fill(parIn.GetY() - parOut.GetY());
                aida.histogram1D("layer " + layer + " vs " + sensorName + " propagated Z meas-prop", 100, -0.002, 0.002).fill(parIn.GetZ() - parOut.GetZ());

//            //Use SimTrackerHit info to start extrapolation
//            //There is energy loss at each stage that I don't take into consideration.
//            //Now let's check on propagation from plane to plane using a SimTrackerHit...
//            //TODO figure out why this doesn't work
//            stat = extrap.Extrapolate(simtrackerhitParams, parOut, dp, null);
//            if (_debug) {
//                System.out.println("Propagating SimTrackerHit state " + simtrackerhitParams);
//                System.out.println(dp.name());
//                System.out.println("sthParams propagated  pos " + parOut.GetX() + " " + parOut.GetY() + " " + parOut.GetZ());
//            }
//
//            aida.histogram1D("layer " + layer + " sth propagated X meas-prop", 100, -0.04, 0.04).fill(simtrackerhitPos.x() - parOut.GetX());
//            aida.histogram1D("layer " + layer + " sth propagated Y meas-prop", 100, -0.02, 0.02).fill(simtrackerhitPos.y() - parOut.GetY());
//            aida.histogram1D("layer " + layer + " sth propagated Z meas-prop", 100, -0.01, 0.01).fill(simtrackerhitPos.z() - parOut.GetZ());
//
//            if (_debug && (simtrackerhitPos.x() - parOut.GetX() > 0.001)) {
//                System.out.println("stop");
//            }
//            //update the parameters to reflect this SimTrackerHit for next iteration
//            double[] sthpars = new double[5];
//            sthpars[0] = simtrackerhitPos.x(); //x
//            sthpars[1] = simtrackerhitPos.y(); //y
//            sthpars[2] = simtrackerhitMom.x() / simtrackerhitMom.z(); // x' (dx/dz)
//            sthpars[3] = simtrackerhitMom.y() / simtrackerhitMom.z(); // y' (dy/dz)
//            sthpars[4] = pars[4];//charge / simtrackerhitMom.magnitude(); // q/p
//            simtrackerhitParams.SetStateVector(sthpars);
//            simtrackerhitParams.SetZ(simtrackerhitPos.z());
                //trf
                PropStat pstat = _trfpropZZRK.vecDirProp(trfv1, new SurfZPlane(simtrackerhitPos.z() / 10.), trfDir);
                SurfZPlane zp = (SurfZPlane) trfv1.surface();
                TrackVector trfTv = trfv1.vector();
                if (_debug) {
                    System.out.println("trf    extrap to z= " + 10. * zp.z() + " " + 10. * trfTv.get(0) + " " + 10. * trfTv.get(1) + " " + 10. * trfTv.get(2) + " " + 10. * trfTv.get(3) + " " + 1. / trfTv.get(4));
                }
                aida.histogram1D("layer " + layer + " trf propagated X meas-prop", 100, -0.04, 0.04).fill(simtrackerhitPos.x() - 10. * trfTv.get(0));
                aida.histogram1D("layer " + layer + " trf propagated Y meas-prop", 100, -0.02, 0.02).fill(simtrackerhitPos.y() - 10. * trfTv.get(1));
                aida.histogram1D("layer " + layer + " trf propagated Z meas-prop", 100, -0.01, 0.01).fill(simtrackerhitPos.z() - 10. * trfTv.get(2));

//          
                lastsimtrackerhit = simtrackerhit;
            } // end of loop over SimTrackerHits

            //let's propagate to the ECal scoring plane
            if (simTrackerHitsAtEcal.size() == 1) {
                SimTrackerHit simtrackerhit = simTrackerHitsAtEcal.get(0);
                Hep3Vector simtrackerhitPos = simtrackerhit.getPositionVec();

                // hit momentum
                Hep3Vector simtrackerhitMom = new BasicHep3Vector(simtrackerhit.getMomentum()[0], simtrackerhit.getMomentum()[1], simtrackerhit.getMomentum()[2]);
                aida.histogram1D("ECal Scoring Plane MC momentum ", 200, 4.50, 4.55).fill(simtrackerhitMom.magnitude());
                Hep3Vector magField = fieldmap.getField(simtrackerhitPos);
                if (_debug) {
                    System.out.println("*** SimTrackerHit at ECal scoring plane; "
                            + "position: " + simtrackerhitPos + "; "
                            + "momentum: " + simtrackerhitMom + " mag field " + magField);
                }
                //let's try extrapolating our track to these nominal z-positions
                CartesianThreeVector planePos = new CartesianThreeVector(0., 0., simtrackerhitPos.z());
                CartesianThreeVector planeNorm = new CartesianThreeVector(0., 0., 1.); // zPlane
                DetectorPlane dp = new DetectorPlane(("layer" + 17), planePos, planeNorm, 0., 0.);

                LitStatus stat = extrap.Extrapolate(parIn, parOut, dp, null);
                if (_debug) {
                    System.out.println("Propagating MCParticle " + mcpParams);
                    System.out.println(dp.name());
                    System.out.println("MC propagated  pos " + parOut.GetX() + " " + parOut.GetY() + " " + parOut.GetZ());
                }
                aida.histogram1D("ECal Scoring Plane propagated X meas-prop", 200, -0.1, 0.1).fill(simtrackerhitPos.x() - parOut.GetX());
                aida.histogram1D("ECal Scoring Plane propagated Y meas-prop", 100, -0.1, 0.1).fill(simtrackerhitPos.y() - parOut.GetY());
                aida.histogram1D("ECal Scoring Plane propagated Z meas-prop", 100, -0.01, 0.01).fill(simtrackerhitPos.z() - parOut.GetZ());
                //Hmmm, the x and y distributions are off by about 10um.
                // Where is the SVT vacuum chamber wall? Are we losing energy there?

                //trf
                System.out.println("trf state at last sensor trfv1 " + trfv1);
                TrackVector lastvec1 = new TrackVector(trfv1.vector());
                PropStat pstat = _trfpropZZRK.vecDirProp(trfv1, new SurfZPlane(simtrackerhitPos.z() / 10.), trfDir);
                SurfZPlane zp = (SurfZPlane) trfv1.surface();
                TrackVector trfTv = trfv1.vector();
                if (_debug) {
                    System.out.println("trf    extrap to z= " + 10. * zp.z() + " " + 10. * trfTv.get(0) + " " + 10. * trfTv.get(1) + " " + 10. * trfTv.get(2) + " " + 10. * trfTv.get(3) + " " + 1. / trfTv.get(4));
                }
                aida.histogram1D("ECal Scoring Plane trf propagated X meas-prop", 200, -0.1, 0.1).fill(simtrackerhitPos.x() - 10. * trfTv.get(0));
                aida.histogram1D("ECal Scoring Plane trf propagated Y meas-prop", 100, -0.02, 0.02).fill(simtrackerhitPos.y() - 10. * trfTv.get(1));
                aida.histogram1D("ECal Scoring Plane trf propagated Z meas-prop", 100, -0.01, 0.01).fill(simtrackerhitPos.z() - 10. * zp.z());

                //let's try extrapolating from the SimTrackerHit at the last SVT sensor
                Hep3Vector lastsimtrackerhitPos = lastsimtrackerhit.getPositionVec();
                // hit momentum
                Hep3Vector lastsimtrackerhitMom = new BasicHep3Vector(lastsimtrackerhit.getMomentum()[0], simtrackerhit.getMomentum()[1], simtrackerhit.getMomentum()[2]);

                // trf stuff note that trf distance units are cm.
//            TrackVector lastvec1 = new TrackVector();
//            lastvec1.set(0, lastsimtrackerhitPos.x() / 10.);    // x
//            lastvec1.set(1, lastsimtrackerhitPos.y() / 10.);    // y
//            lastvec1.set(2, lastsimtrackerhitMom.x() / lastsimtrackerhitMom.z());   // dx/dz
//            lastvec1.set(3, lastsimtrackerhitMom.y() / lastsimtrackerhitMom.z());   // dy/dz
                lastvec1.set(4, charge / lastsimtrackerhitMom.magnitude());   // q/p
                // try just updating the position, as I don't trust the momentum vector in the SimTrackerHit
                lastvec1.set(0, lastsimtrackerhitPos.x() / 10.);    // x
                lastvec1.set(1, lastsimtrackerhitPos.y() / 10.);    // y

                // create a VTrack at the last SVT sensor.
                SurfZPlane lastzp0 = new SurfZPlane(lastsimtrackerhitPos.z() / 10.);
                VTrack lasttrfv0 = new VTrack(lastzp0.newPureSurface(), lastvec1, tdir);
                System.out.println("trf state from simtrackerhit at last sensor lasttrfv0 " + lasttrfv0);
                PropStat lastpstat = _trfpropZZRK.vecDirProp(lasttrfv0, new SurfZPlane(simtrackerhitPos.z() / 10.), trfDir);
                SurfZPlane lastzp = (SurfZPlane) lasttrfv0.surface();
                TrackVector lasttrfTv = lasttrfv0.vector();
                //if (_debug) {
                System.out.println("trf    extrap to z= " + 10. * lastzp.z() + " " + 10. * lasttrfTv.get(0) + " " + 10. * lasttrfTv.get(1) + " " + 10. * lasttrfTv.get(2) + " " + 10. * lasttrfTv.get(3) + " " + 1. / lasttrfTv.get(4));
                //}
                aida.histogram1D("ECal Scoring Plane trf propagated from last sensor X meas-prop", 200, -0.1, 0.1).fill(simtrackerhitPos.x() - 10. * lasttrfTv.get(0));
                aida.histogram1D("ECal Scoring Plane trf propagated from last sensor Y meas-prop", 100, -0.02, 0.02).fill(simtrackerhitPos.y() - 10. * lasttrfTv.get(1));
                aida.histogram1D("ECal Scoring Plane trf propagated from last sensor Z meas-prop", 100, -0.01, 0.01).fill(simtrackerhitPos.z() - 10. * lastzp.z());

            }

            if (trackit) {
                // let's look at raw trackerhits...
                List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits");
                for (RawTrackerHit rawtrackerhit : rawTrackerHits) {
                    List<SimTrackerHit> simtrackerHits = rawtrackerhit.getSimTrackerHits();
                    HpsSiSensor sensor = (HpsSiSensor) rawtrackerhit.getDetectorElement();
                    System.out.println(sensor.getName());
                    Hep3Vector position = sensor.getGeometry().getPosition();
                    Hep3Vector[] directionVectors = getUnitVectors(sensor);
                    System.out.printf("%48s: %40s %40s %40s %40s\n", sensor.getName(), position.toString(), directionVectors[0], directionVectors[1], directionVectors[2]);
                }
            }
        }
    }

    private Hep3Vector[] getUnitVectors(SiSensor sensor) {

        Hep3Vector unit_vecU = new BasicHep3Vector(-99, -99, -99);
        Hep3Vector unit_vecV = new BasicHep3Vector(-99, -99, -99);
        Hep3Vector unit_vecW = new BasicHep3Vector(-99, -99, -99);

        for (ChargeCarrier carrier : ChargeCarrier.values()) {
            if (sensor.hasElectrodesOnSide(carrier)) {
                int channel = 1;
                long cell_id = sensor.makeStripId(channel, carrier.charge()).getValue();
                IIdentifier id = new Identifier(cell_id);
                SiTrackerIdentifierHelper _sid_helper = (SiTrackerIdentifierHelper) sensor.getIdentifierHelper();
                SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(carrier);
                ITransform3D local_to_global = new Transform3D();// sensor.getGeometry().getLocalToGlobal();
                ITransform3D electrodes_to_global = electrodes.getLocalToGlobal();
                ITransform3D global_to_hit = local_to_global.inverse();
                ITransform3D electrodes_to_hit = Transform3D.multiply(global_to_hit, electrodes_to_global);

                unit_vecU = electrodes_to_hit.rotated(electrodes.getMeasuredCoordinate(0));
                unit_vecV = electrodes_to_hit.rotated(electrodes.getUnmeasuredCoordinate(0));
                unit_vecW = VecOp.cross(unit_vecU, unit_vecV);
            }
        }
        return new Hep3Vector[]{unit_vecU, unit_vecV, unit_vecW};
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

    public void setDebug(boolean b) {
        _debug = b;
    }

    public void setTrackit(boolean b) {
        trackit = b;
    }
}

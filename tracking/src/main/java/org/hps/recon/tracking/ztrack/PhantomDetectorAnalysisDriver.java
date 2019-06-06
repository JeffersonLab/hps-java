package org.hps.recon.tracking.ztrack;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.RotationPassiveXYZ;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.Translation3D;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
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
public class PhantomDetectorAnalysisDriver extends Driver {

    boolean _debug = false;
    private AIDA aida = AIDA.defaultInstance();

    private static final Vector3D vX = Vector3D.PLUS_I;
    private static final Vector3D vY = Vector3D.PLUS_J;

    RK4TrackExtrapolator _extrap;

    Map<String, DetectorPlane> planemap = new HashMap<String, DetectorPlane>();

    protected void detectorChanged(Detector detector) {
        IDetectorElement detectorElement = detector.getDetectorElement();
        List<SiSensor> sensors = detectorElement.findDescendants(SiSensor.class);
        for (SiSensor sensor : sensors) {
            Hep3Vector position = sensor.getGeometry().getPosition();
            Hep3Vector u = this.getUnitVector(sensor, "measured");
            Hep3Vector v = this.getUnitVector(sensor, "unmeasured");
            Hep3Vector w = VecOp.cross(v, u);
            if (_debug) {
                System.out.println(sensor.getName());
                System.out.println(position);
                System.out.println("u " + u);
                System.out.println("v " + v);
                System.out.println("w " + w);
            }
            planemap.put(sensor.getName(), makePlane(sensor));
        }
        _extrap = new RK4TrackExtrapolator(new HpsMagField(detector.getFieldMap()));
    }

    protected void process(EventHeader event) {
        List<SimTrackerHit> simTrackerHits = event.get(SimTrackerHit.class, "TrackerHits");
        List<SimTrackerHit> simTrackerHitsAtEcal = event.get(SimTrackerHit.class, "TrackerHitsECal");
        List<MCParticle> mcparts = event.get(MCParticle.class, "MCParticle");
        if (_debug) {
            System.out.println("found " + mcparts.size() + "MC Particles with " + simTrackerHits.size() + " SimTrackerHits");
        }

        Map<String, SimTrackerHit> simTrackerHitMap = new HashMap<String, SimTrackerHit>();
        for (SimTrackerHit sth : simTrackerHits) {
            if (_debug) {
                System.out.println(sth.getDetectorElement().getName());
            }
            simTrackerHitMap.put(sth.getDetectorElement().getName(), sth);
        }

        // create a ZTrackParam object for this MCParticle
        //TODO move this to the constructor maybe?
        MCParticle mcp = mcparts.get(0);

        ZTrackParam tp = new ZTrackParam(mcp.getOrigin().v(), mcp.getMomentum().v(), (int) mcp.getCharge());
        if (_debug) {
            System.out.println("Starting ZTrackParam " + tp);
        }

        ZTrackParam parOut = new ZTrackParam();
        for (String planeName : simTrackerHitMap.keySet()) {
            Status extrapStat = _extrap.Extrapolate(tp, parOut, planemap.get(planeName), null);

            double dx = simTrackerHitMap.get(planeName).getPositionVec().x() - parOut.GetX();
            double dy = simTrackerHitMap.get(planeName).getPositionVec().y() - parOut.GetY();
            double dz = simTrackerHitMap.get(planeName).getPositionVec().z() - parOut.GetZ();
            aida.histogram1D(planeName + " dx", 200, -0.5, 0.5).fill(dx);
            aida.histogram1D(planeName + " dy", 100, -0.01, 0.01).fill(dy);
            aida.histogram1D(planeName + " dz", 100, -0.1, 0.1).fill(dz);
            aida.histogram2D(planeName + " dx vs dy", 100, -0.1, 0.1, 100, -0.01, 0.01).fill(dx, dy);
            if (_debug) {
                System.out.println("extrapolating to " + planeName);
                System.out.println(extrapStat);
                System.out.println("parOut " + parOut);
                System.out.println(simTrackerHitMap.get(planeName).getPositionVec());
            }
        }

    }

    private Hep3Vector getUnitVector(SiSensor sensor, String type) {

        Hep3Vector unit_vec = new BasicHep3Vector(-99, -99, -99);

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
                if (type == "measured") {
                    unit_vec = electrodes_to_hit.rotated(electrodes.getMeasuredCoordinate(0));
                } else if (type == "unmeasured") {
                    unit_vec = electrodes_to_hit.rotated(electrodes.getUnmeasuredCoordinate(0));
                } else {
                    throw new UnsupportedOperationException(String.format("type=\"%s\" not supported", type));
                }
            }
        }
        return unit_vec;
    }

    public DetectorPlane makePlane(SiSensor sensor) {
        Hep3Vector position = sensor.getGeometry().getPosition();
        Hep3Vector u = this.getUnitVector(sensor, "measured");
        if (u.x() < 0.) {
            u = VecOp.neg(u);
        }

        Hep3Vector v = this.getUnitVector(sensor, "unmeasured");
        if (v.x() < 0.) {
            v = VecOp.neg(v);
        }
        Hep3Vector w = VecOp.cross(v, u);

        // extract the rotation angles...
        Vector3D vXprime = new Vector3D(v.x(), v.y(), v.z());  // nominal x
        Vector3D vYprime = new Vector3D(u.x(), u.y(), u.z());   // nominal y
        // create a rotation matrix from this pair of vectors
        Rotation xyVecRot = new Rotation(vX, vY, vXprime, vYprime);
        double[] hpsAngles = xyVecRot.getAngles(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR);

        IRotation3D r1 = new RotationPassiveXYZ(hpsAngles[0], hpsAngles[1], hpsAngles[2]);
        ITranslation3D t1 = new Translation3D(position.x(), position.y(), position.z());
        if (_debug) {
            System.out.println("hpsAngles " + Arrays.toString(hpsAngles));
            System.out.println("r1 " + r1);
            System.out.println("t1 " + t1);
        }
        ITransform3D l2g2 = new Transform3D(t1, r1);
        ITransform3D g2l2 = l2g2.inverse();

        // hard-code thickness in radiation lengths for now...
        double x0 = .001;

        // hard-code dimensions for now...
        double x = 200.;
        double y = 200.;

        // could do this before calling here
        HpsSiSensor hpsSiSensor = (HpsSiSensor) sensor;
        Box sensorSolid = (Box) hpsSiSensor.getGeometry().getLogicalVolume().getSolid();
        double length = sensorSolid.getXHalfLength() * 2.0;
        double width = sensorSolid.getYHalfLength() * 2.0;

        if(_debug) System.out.println("length x width: " + length + " x " + width);

        return new DetectorPlane(sensor.getName(), position, w, l2g2, g2l2, x0, v, width, u, length);
    }
}

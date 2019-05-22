package org.hps.recon.tracking.ztrack;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MaterialSupervisor.ScatteringDetectorVolume;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.hps.recon.tracking.CoordinateTransformations;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.RotationPassiveXYZ;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.Translation3D;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.Tracker;
import org.lcsim.geometry.compact.Subdetector;

/**
 *
 * @author ngraf
 */
public class DetectorPlaneTest extends TestCase {

    public void testIt() {
        double z = 37.;
        double radLengths = 0.001;
        Hep3Vector pos = new BasicHep3Vector(0., 0., z);
        Hep3Vector Z = new BasicHep3Vector(0., 0., 1.);

        Hep3Vector X = new BasicHep3Vector(1., 0., 0.);
        double measDim = 100.;

        Hep3Vector Y = new BasicHep3Vector(0., 1., 0.);
        double unmeasDim = 20.;

        ITransform3D l2g = new Transform3D();
        ITransform3D g2l = l2g.inverse();

        DetectorPlane p = new DetectorPlane("p1", pos, Z, l2g, g2l, radLengths, X, unmeasDim, Y, measDim);

        System.out.println(p);

        boolean in = p.inBounds(15, 0, z);
        System.out.println(in);

        // now for some tweaks...
        double beamRotation = 0.0305; // 30.5 milliradians...
        IRotation3D r1 = new RotationPassiveXYZ(0., beamRotation, 0.);
        System.out.println("r1 " + r1);
        ITranslation3D t1 = new Translation3D(0., 0., 10.);
        System.out.println("t1 " + t1);

        ITransform3D l2g2 = new Transform3D(t1, r1);
        ITransform3D g2l2 = l2g2.inverse();

        System.out.println("l2g2(X) " + l2g2.transformed(X));
        System.out.println("l2g2(Y) " + l2g2.transformed(Y));

        Hep3Vector pos2 = new BasicHep3Vector(0., 0., z + 10.);
        DetectorPlane p2 = new DetectorPlane("p1", pos2, Z, l2g2, g2l2, radLengths, l2g2.transformed(X), unmeasDim, l2g2.transformed(Y), measDim);

        System.out.println(p);
    }

    public void testHpsPlane() throws Exception {
        Vector3D vX = Vector3D.PLUS_I;
        Vector3D vY = Vector3D.PLUS_J;

        Map<String, SiStripPlane> stripPlaneNameMap = new HashMap<String, SiStripPlane>();
        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        String detectorName = "HPS-EngRun2015-Nominal-v0";
        manager.setDetector(detectorName, 5772);
        Detector d = manager.getCachedConditions(Detector.class, "compact.xml").getCachedData();
        MaterialSupervisor sup = new MaterialSupervisor();
        sup.buildModel(d);
        for (ScatteringDetectorVolume vol : sup.getMaterialVolumes()) {
            SiStripPlane plane = (SiStripPlane) vol;
            stripPlaneNameMap.put(plane.getName(), plane);
        }

        String[] planes = {"module_L1t_halfmodule_axial_sensor0", "module_L5t_halfmodule_stereo_slot_sensor0", "module_L6b_halfmodule_stereo_slot_sensor0"};
        for (String pname : planes) {
            // pick a plane...
            SiStripPlane plane = stripPlaneNameMap.get(pname);
            System.out.println(plane.getName());
            boolean debug = true;
            //let's check things out here...
            Hep3Vector oprime = CoordinateTransformations.transformVectorToDetector(plane.origin());
            Hep3Vector nprime = CoordinateTransformations.transformVectorToDetector(plane.normal());
            if (debug) {
                System.out.println(" origin: " + oprime);
            }
            if (debug) {
                System.out.println(" normal: " + nprime);
            }
            if (debug) {
                System.out.println(" Plane is: " + plane.getUnmeasuredDimension() + " x " + plane.getMeasuredDimension());
            }
//            HpsSiSensor sensor = (HpsSiSensor) plane.getSensor();

            Hep3Vector unmeasDir = CoordinateTransformations.transformVectorToDetector(plane.getUnmeasuredCoordinate());
            // for some reason some detectors have the sensors flipped
            // always want unmeasDir along x
            if (unmeasDir.x() < 0.) {
                unmeasDir = VecOp.neg(unmeasDir);
            }
            Hep3Vector measDir = CoordinateTransformations.transformVectorToDetector(plane.getMeasuredCoordinate());
            //measDir points either up or down depending on orientation of the sensor. I want it always to point up, along y.
            if (measDir.y() < 0.) {
                measDir = VecOp.neg(measDir);
            }
            // calculate the new normal
            Hep3Vector tst = VecOp.cross(unmeasDir, measDir);
            Assert.assertTrue(tst.z() > 0.);
            if (debug) {
                System.out.println("unmeasured direction:   " + unmeasDir);
                System.out.println("measured direction:    " + measDir);
                System.out.println("new normal " + tst);
            }
            // extract the rotation angles...
            Vector3D vXprime = new Vector3D(unmeasDir.x(), unmeasDir.y(), unmeasDir.z());  // nominal x
            Vector3D vYprime = new Vector3D(measDir.x(), measDir.y(), measDir.z());   // nominal y
            // create a rotation matrix from this pair of vectors
            Rotation xyVecRot = new Rotation(vX, vY, vXprime, vYprime);
            double[] hpsAngles = xyVecRot.getAngles(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR);
            System.out.println("hpsAngles " + Arrays.toString(hpsAngles));

        }
    }

    public void testHpsDetector() throws Exception {
        String hpsDetectorName = "HPS-EngRun2015-Nominal-v2-fieldmap";
        DatabaseConditionsManager cm = DatabaseConditionsManager.getInstance();
        cm.setDetector(hpsDetectorName, 0);
        Detector det = cm.getDetectorObject();
        HpsDetector hpsdet = new HpsDetector(det);
        System.out.println(hpsdet);

        // let's pick a plane
        String planeName = "module_L1t_halfmodule_axial_sensor0";
        DetectorPlane dPlane = hpsdet.getPlane(planeName);
        System.out.println(dPlane);
        CartesianThreeVector o = dPlane.position();
        boolean isInBounds = dPlane.inBounds(o.x(), o.y(), o.z());
        // origin should be in bounds
        System.out.println(isInBounds);
        assertTrue(isInBounds);
        double dy = dPlane.getMeasuredDimension() / 2.;
        // slightly more than half the dimension should be out of bounds
        isInBounds = dPlane.inBounds(o.x(), o.y() + 1.01 * dy, o.z());
        System.out.println(isInBounds);
        assertFalse(isInBounds);
        // slighly less than half the dimension should be in bounds
        isInBounds = dPlane.inBounds(o.x(), o.y() + 0.99 * dy, o.z());
        System.out.println(isInBounds);
        assertTrue(isInBounds);

        String[] planes = {"module_L1t_halfmodule_axial_sensor0", "module_L5t_halfmodule_stereo_slot_sensor0", "module_L6b_halfmodule_stereo_slot_sensor0"};
        for (String pname : planes) {
            DetectorPlane dp = hpsdet.getPlane(pname);
            System.out.println(dp);
        }
    }

    public void testZDetector() throws Exception {
        String hpsDetectorName = "HPS-PhysicsRun2019-v1-4pt5";
        DatabaseConditionsManager cm = DatabaseConditionsManager.getInstance();
        cm.setDetector(hpsDetectorName, 0);
        Detector det = cm.getDetectorObject();
        List<Subdetector> subdetectors = det.getSubdetectorList();
        System.out.println("found "+subdetectors.size()+ " subdetectors");
        for (Subdetector sd : subdetectors){
            System.out.println(sd.getName());
            if(sd.isTracker())
            {
                Tracker t = (Tracker) sd;
            }
        }

    }

}

package org.hps.recon.tracking.ztrack;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MultipleScattering;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.RotationPassiveXYZ;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.Translation3D;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.trfutil.Assert;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class HpsDetector {

    // the magnetic field for this detector
//    private CbmLitField _magfield;
    // use a TreeSet to make sure there are no duplicate DetectorPlanes, 
    // and also to naturally order in z,
    private TreeSet<DetectorPlane> _detectors = new TreeSet<DetectorPlane>();
    // use a List for fast indexing
    List<DetectorPlane> detList;
    // use a map to get plane by name
    private Map<String, DetectorPlane> _planemap = new HashMap<String, DetectorPlane>();
    double zMin = 9999.;
    double zMax = -9999.;

    // temporary objects for the range determination
    DetectorPlane probe = new DetectorPlane();
    DetectorPlane response;

    private String _name = "dummy";

    boolean debug = false;

    private static final Vector3D vX = Vector3D.PLUS_I;
    private static final Vector3D vY = Vector3D.PLUS_J;

    public HpsDetector() {
    }

    public HpsDetector(Detector det) {
//        _magfield = new HpsMagField(det.getFieldMap());
        _name = det.getName();
        System.out.println(_name);
        MaterialSupervisor sup = new MaterialSupervisor();
        MultipleScattering ms = new MultipleScattering(sup);
        sup.buildModel(det);
        List<MaterialSupervisor.ScatteringDetectorVolume> stripPlanes = sup.getMaterialVolumes();
        for (MaterialSupervisor.ScatteringDetectorVolume vol : stripPlanes) {
            MaterialSupervisor.SiStripPlane plane = (MaterialSupervisor.SiStripPlane) vol;

            // origin point in the plane
            Hep3Vector oprime = CoordinateTransformations.transformVectorToDetector(plane.origin());
            if (debug) {
                System.out.println("plane.origin() " + plane.origin());
                System.out.println("oprime " + oprime);
            }
            //CartesianThreeVector pos = new CartesianThreeVector(oprime.x(), oprime.y(), oprime.z());

            // normal to the plane
            Hep3Vector nprime = CoordinateTransformations.transformVectorToDetector(plane.normal());
            //CartesianThreeVector normal = new CartesianThreeVector(nprime.x(), nprime.y(), nprime.z());

            // calculate some angles
            Hep3Vector unmeasDir = CoordinateTransformations.transformVectorToDetector(plane.getUnmeasuredCoordinate());
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

            IRotation3D r1 = new RotationPassiveXYZ(hpsAngles[0], hpsAngles[1], hpsAngles[2]);
            ITranslation3D t1 = new Translation3D(oprime.x(), oprime.y(), oprime.z());
            if (debug) {
                System.out.println("hpsAngles " + Arrays.toString(hpsAngles));
                System.out.println("r1 " + r1);
                System.out.println("t1 " + t1);

            }
            ITransform3D l2g2 = new Transform3D(t1, r1);
            ITransform3D g2l2 = l2g2.inverse();

            // get thickness in radiation lengths
            HpsSiSensor sensor = (HpsSiSensor) plane.getSensor();
            // create a DetectorPlane object
            String name = sensor.getName();
            double x0 = plane.getThicknessInRL();
            if (debug) {
                System.out.println(name + " X0= " + x0);
            }
//            ITransform3D l2g = sensor.getGeometry().getLocalToGlobal();
//            ITransform3D g2l = sensor.getGeometry().getGlobalToLocal();
            // dimensions and directions in the plane
            double y = plane.getMeasuredDimension();
            double x = plane.getUnmeasuredDimension();
//            Hep3Vector sensorMeasDir = VecOp.mult(l2g.getRotation().getRotationMatrix(), sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getMeasuredCoordinate(0));
//            Hep3Vector sensorUnMeasDir = VecOp.mult(l2g.getRotation().getRotationMatrix(), sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getUnmeasuredCoordinate(0));
//            Hep3Vector cross = VecOp.cross(sensorMeasDir, sensorUnMeasDir);
//            Polygon3D psidePoly = plane.getPsidePlane();
//            List<Point3D> psidePoints = psidePoly.getClosedVertices();
//            if (debug) {
//                System.out.println("l2g sensor measDir " + sensorMeasDir);
//                System.out.println("l2g sensor unMeasDir " + sensorUnMeasDir);
//                System.out.println("meas cross unmeas " + cross);
//                System.out.println("nside Polygon points global:");
//            }

//            System.out.println("nside Polygon points local:");
//            for(Point3D point : psidePoints)
//            {
//                System.out.println(point);
//            }
//            double zmin = 999.;
//            double zmax = -999.;
//            for (Point3D point : psidePoints) {
//                Hep3Vector g = sensor.getGeometry().transformLocalToGlobal(point);
//                if (debug) {
//                    System.out.println(g);
//                }
//                if (g.z() > zmax) {
//                    zmax = g.z();
//                }
//                if (g.z() < zmin) {
//                    zmin = g.z();
//                }
//            }
            DetectorPlane dPlane = new DetectorPlane(name, oprime, nprime, l2g2, g2l2, x0, unmeasDir, x, measDir, y);
            if (debug) {
                System.out.println(dPlane);
            }
            addDetectorPlane(dPlane);
        }
    }

    public void addDetectorPlane(DetectorPlane p) {
        if (p.GetZpos() < zMin) {
            zMin = p.GetZpos();
        }
        if (p.GetZpos() > zMax) {
            zMax = p.GetZpos();
        }
        _detectors.add(p);
        //update the detList
        detList = new ArrayList<DetectorPlane>(_detectors);
        _planemap.put(p.name(), p);
    }

//    public void setMagneticField(CbmLitField field)
//    {
//        _magfield = field;
//    }
//    
//    public CbmLitField magneticField()
//    {
//        return _magfield;
//    }
    public List<DetectorPlane> getPlanes() {
        return detList;
    }

    public DetectorPlane getPlane(String name) {
        return _planemap.get(name);
    }

    public double[] getZPositions() {
        double[] z = new double[detList.size()];
        int index = 0;
        for (DetectorPlane m : detList) {
            z[index++] = m.GetZpos();
        }
        return z;
    }

    public String name() {
        return _name;
    }

    /**
     * Returns the indices in the List of detector elements which fall within
     * the given range first index is strictly greater than that of zStart
     * second index includes zEnd
     *
     * @param zStart z position at which to start searching
     * @param zEnd z position at end of range
     * @param indices array returning first and last indices of detector
     * elements found in range
     */
    public void indicesInRange(double zStart, double zEnd, int[] indices) {
        //TODO put in some sanity checks to make sure range is within the detector.
        if (zStart < zEnd) {
            probe.SetZpos(zStart);
            response = _detectors.higher(probe);
            indices[0] = detList.indexOf(response);
            probe.SetZpos(zEnd);
            response = _detectors.floor(probe);
            indices[1] = detList.indexOf(response);
        } else {
            probe.SetZpos(zStart);
            response = _detectors.lower(probe);
            indices[0] = detList.indexOf(response);
            probe.SetZpos(zEnd);
            response = _detectors.ceiling(probe);
            indices[1] = detList.indexOf(response);
        }
    }

    public double zMin() {
        return zMin;
    }

    public double zMax() {
        return zMax;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("HpsDetector with " + _detectors.size() + " planes \n");
        for (DetectorPlane p : _detectors) {
            sb.append(p.name() + " : z= " + p.position().z() + "\n");
        }
        return sb.toString();
    }
}

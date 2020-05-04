package org.hps.recon.tracking.lit;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MultipleScattering;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.solids.Point3D;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.geometry.Detector;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class HpsDetector {

    // the magnetic field for this detector
    private CbmLitField _magfield;
    // use a TreeSet to make sure there are no duplicate DetectorPlanes, 
    // and also to naturally order in z,
    private TreeSet<DetectorPlane> _detectors = new TreeSet<DetectorPlane>();
    // use a List for fast indexing
    List<DetectorPlane> detList;
    // use a map to get plane by name
    private Map<String, DetectorPlane> _planemap = new HashMap<String, DetectorPlane>();
    // minimum z for the detector as a whole
    double zMin = 9999.;
    // maximum z for the detector as a whole
    double zMax = -9999.;

    // temporary objects for the range determination
    DetectorPlane probe = new DetectorPlane();
    DetectorPlane response;

    private String _name = "dummy";

    boolean debug = true;

    public HpsDetector() {
    }

    public HpsDetector(Detector det) {
        _magfield = new HpsMagField(det.getFieldMap());
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
            CartesianThreeVector pos = new CartesianThreeVector(oprime.x(), oprime.y(), oprime.z());

            // normal to the plane
            Hep3Vector nprime = CoordinateTransformations.transformVectorToDetector(plane.normal());
            CartesianThreeVector normal = new CartesianThreeVector(nprime.x(), nprime.y(), nprime.z());

            HpsSiSensor sensor = (HpsSiSensor) plane.getSensor();
            // create a DetectorPlane object
            String name = sensor.getName();
            double x0 = plane.getThicknessInRL();

            ITransform3D l2g = sensor.getGeometry().getLocalToGlobal();
            ITransform3D g2l = sensor.getGeometry().getGlobalToLocal();
            // dimensions and directions in the plane
            double y = plane.getMeasuredDimension();
            double x = plane.getUnmeasuredDimension();
            Hep3Vector sensorMeasDir = VecOp.mult(l2g.getRotation().getRotationMatrix(), sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getMeasuredCoordinate(0));
            Hep3Vector sensorUnMeasDir = VecOp.mult(l2g.getRotation().getRotationMatrix(), sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getUnmeasuredCoordinate(0));
            Hep3Vector cross = VecOp.cross(sensorMeasDir, sensorUnMeasDir);
            Polygon3D psidePoly = plane.getPsidePlane();
            List<Point3D> psidePoints = psidePoly.getClosedVertices();
            if (debug) {
                System.out.println(name + ":");
                System.out.println("plane.origin() " + plane.origin());
                System.out.println("oprime " + oprime);
                System.out.println("normal: " + normal);
                System.out.println("l2g sensor measDir " + sensorMeasDir);
                System.out.println("l2g sensor unMeasDir " + sensorUnMeasDir);
                System.out.println("meas cross unmeas " + cross);
                System.out.println(name + " X0= " + x0);
            }

//            System.out.println("nside Polygon points local:");
//            for(Point3D point : psidePoints)
//            {
//                System.out.println(point);
//            }
            double zmin = 999.;
            double zmax = -999.;
            for (Point3D point : psidePoints) {
                Hep3Vector g = sensor.getGeometry().transformLocalToGlobal(point);
//                if (debug) {
//                    System.out.println(g);
//                }
                if (g.z() > zmax) {
                    zmax = g.z();
                }
                if (g.z() < zmin) {
                    zmin = g.z();
                }
            }
            DetectorPlane dPlane = new DetectorPlane(name, pos, normal, l2g, g2l, x0, sensorUnMeasDir, x, sensorMeasDir, y);
            dPlane.setZmin(zmin);
            dPlane.setZmax(zmax);
            System.out.println(dPlane);

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

    public void setMagneticField(CbmLitField field) {
        _magfield = field;
    }

    public CbmLitField magneticField() {
        return _magfield;
    }

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

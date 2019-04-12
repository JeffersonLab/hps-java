package org.hps.analysis.alignment.straighttrack;

import Jama.Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import static org.hps.analysis.alignment.straighttrack.FitTracks.GEN_ROTMAT;
import static org.hps.analysis.alignment.straighttrack.FitTracks.PROD_ROT;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MaterialSupervisor.ScatteringDetectorVolume;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.geometry.Detector;

/**
 *
 * @author ngraf
 */
public class DetectorBuilder {

    Detector _det;
    Map<String, String[]> sensorNameMap = new HashMap<String, String[]>();
    Map<String, SiStripPlane> stripPlaneNameMap = new HashMap<String, SiStripPlane>();
    Map<String, List<DetectorPlane>> trackerMap = new HashMap<String, List<DetectorPlane>>();
    Map<String, DetectorPlane> planeMap = new HashMap<String, DetectorPlane>();

    // should be enums?
    public enum Tracker {
        TOPHOLE("topHole"),
        TOPSLOT("topSlot"),
        BOTTOMHOLE("bottomHole"),
        BOTTOMSLOT("bottomSlot");
        private String _name;

        Tracker(String name) {
            _name = name;
        }

        String trackerName() {
            return _name;
        }
    }

    Vector3D vX = Vector3D.PLUS_I;
    Vector3D vY = Vector3D.PLUS_J;

    // TODO fix this...
    double[] SIGS = {0.0055, 0.00};     //  detector resolutions in mm

    boolean _debug = false;

    String[] bottomSlotNames = {
        "module_L1b_halfmodule_stereo_sensor0",
        "module_L1b_halfmodule_axial_sensor0",
        "module_L2b_halfmodule_stereo_sensor0",
        "module_L2b_halfmodule_axial_sensor0",
        "module_L3b_halfmodule_stereo_sensor0",
        "module_L3b_halfmodule_axial_sensor0",
        "module_L4b_halfmodule_stereo_slot_sensor0",
        "module_L4b_halfmodule_axial_slot_sensor0",
        "module_L5b_halfmodule_stereo_slot_sensor0",
        "module_L5b_halfmodule_axial_slot_sensor0",
        "module_L6b_halfmodule_stereo_slot_sensor0",
        "module_L6b_halfmodule_axial_slot_sensor0"};
    String[] bottomHoleNames = {
        "module_L1b_halfmodule_stereo_sensor0",
        "module_L1b_halfmodule_axial_sensor0",
        "module_L2b_halfmodule_stereo_sensor0",
        "module_L2b_halfmodule_axial_sensor0",
        "module_L3b_halfmodule_stereo_sensor0",
        "module_L3b_halfmodule_axial_sensor0",
        "module_L4b_halfmodule_stereo_hole_sensor0",
        "module_L4b_halfmodule_axial_hole_sensor0",
        "module_L5b_halfmodule_stereo_hole_sensor0",
        "module_L5b_halfmodule_axial_hole_sensor0",
        "module_L6b_halfmodule_stereo_hole_sensor0",
        "module_L6b_halfmodule_axial_hole_sensor0"};

    String[] topSlotNames = {
        "module_L1t_halfmodule_axial_sensor0",
        "module_L1t_halfmodule_stereo_sensor0",
        "module_L2t_halfmodule_axial_sensor0",
        "module_L2t_halfmodule_stereo_sensor0",
        "module_L3t_halfmodule_axial_sensor0",
        "module_L3t_halfmodule_stereo_sensor0",
        "module_L4t_halfmodule_axial_slot_sensor0",
        "module_L4t_halfmodule_stereo_slot_sensor0",
        "module_L5t_halfmodule_axial_slot_sensor0",
        "module_L5t_halfmodule_stereo_slot_sensor0",
        "module_L6t_halfmodule_axial_slot_sensor0",
        "module_L6t_halfmodule_stereo_slot_sensor0"};
    String[] topHoleNames = {
        "module_L1t_halfmodule_axial_sensor0",
        "module_L1t_halfmodule_stereo_sensor0",
        "module_L2t_halfmodule_axial_sensor0",
        "module_L2t_halfmodule_stereo_sensor0",
        "module_L3t_halfmodule_axial_sensor0",
        "module_L3t_halfmodule_stereo_sensor0",
        "module_L4t_halfmodule_axial_hole_sensor0",
        "module_L4t_halfmodule_stereo_hole_sensor0",
        "module_L5t_halfmodule_axial_hole_sensor0",
        "module_L5t_halfmodule_stereo_hole_sensor0",
        "module_L6t_halfmodule_axial_hole_sensor0",
        "module_L6t_halfmodule_stereo_hole_sensor0"};

    public DetectorBuilder(String detectorName) {
        final DatabaseConditionsManager manager = new DatabaseConditionsManager();
        try {
            manager.setDetector(detectorName, 5772);
        } catch (ConditionsManager.ConditionsNotFoundException ex) {
            Logger.getLogger(DetectorBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
        _det = manager.getCachedConditions(Detector.class, "compact.xml").getCachedData();
        System.out.println(_det.getName());
        setup();
    }

    public DetectorBuilder(Detector detector) {
        _det = detector;
        System.out.println(_det.getName());
        setup();
    }

    private void setup() {
        sensorNameMap.put("topHole", topHoleNames);
        sensorNameMap.put("bottomHole", bottomHoleNames);
        sensorNameMap.put("topSlot", topSlotNames);
        sensorNameMap.put("bottomSlot", bottomSlotNames);
        MaterialSupervisor materialManager = new MaterialSupervisor();
        materialManager.buildModel(_det);

        for (ScatteringDetectorVolume vol : materialManager.getMaterialVolumes()) {
            SiStripPlane plane = (SiStripPlane) vol;
            stripPlaneNameMap.put(plane.getName(), plane);
        }

        for (Tracker t : Tracker.values()) {
            String s = t.trackerName();
            if (_debug) {
                System.out.println("building " + s + " : ");
            }
            String[] stripNames = sensorNameMap.get(s);
            List<DetectorPlane> planes = new ArrayList<DetectorPlane>();
            int id = 1;
            for (String stripPlaneName : stripNames) {

                SiStripPlane plane = stripPlaneNameMap.get(stripPlaneName);
                Hep3Vector origin = CoordinateTransformations.transformVectorToDetector(plane.origin());

                Hep3Vector normal = CoordinateTransformations.transformVectorToDetector(plane.normal());
                // orient all normals to point along +ive z
                if (normal.z() < 0.) {
                    normal = VecOp.neg(normal);
                }
                Hep3Vector vDir = CoordinateTransformations.transformVectorToDetector(plane.getUnmeasuredCoordinate());
                Hep3Vector uDir = VecOp.cross(normal, vDir);

                // extract the rotation angles...
                // Note that these are "reversed" because the code assumes the u measurement direction is along x, not y
                // so we need an additional PI/2 rotation...
                Vector3D vYprime = new Vector3D(vDir.x(), vDir.y(), vDir.z());  // nominal x
                Vector3D vXprime = new Vector3D(uDir.x(), uDir.y(), uDir.z());   // nominal y
                // create a rotation matrix from this pair of vectors
                Rotation xyVecRot = new Rotation(vX, vY, vXprime, vYprime);
                double[] hpsAngles = xyVecRot.getAngles(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR);
//                if (_debug) {
                System.out.println(" " + stripPlaneName);
                System.out.println("   origin: " + origin);
                System.out.println("   normal: " + normal);
                System.out.println("   uDir: " + uDir);
                System.out.println("   vDir: " + vDir);
                if (_debug) {
                    System.out.println("   Apache commons angles: " + Arrays.toString(hpsAngles));
                }
                Matrix[] mats = new Matrix[3];
                double[][] RW = new double[3][9];
                for (int j = 0; j < 3; ++j) {
                    double angl = hpsAngles[j];
                    mats[j] = GEN_ROTMAT(angl, j);
                    GEN_ROTMAT(angl, j, RW[j]);
                    if (_debug) {
                        System.out.println("Rotation matrix for axis " + (j + 1) + " angle " + angl);
                    }
                    if (_debug) {
                        mats[j].print(6, 4);
                    }
                }
                Matrix prodrot = PROD_ROT(mats[0], mats[1], mats[2]);
                DetectorPlane dp = new DetectorPlane(id++, prodrot, origin.v(), SIGS);
                planes.add(dp);
                planeMap.put(plane.getName(), dp);
            }
            trackerMap.put(s, planes);
            if (_debug) {
                System.out.println("");
            }
        }

    }

    public List<DetectorPlane> getTracker(String trackerName) {
        return trackerMap.get(trackerName);
    }

    public static void main(String[] args) {
        DetectorBuilder db = new DetectorBuilder("HPS-EngRun2015-Nominal-v0");

        List<DetectorPlane> planes = db.getTracker("topSlot");
        for (DetectorPlane p : planes) {
            System.out.println(p);
            System.out.println("");
        }

    }
}

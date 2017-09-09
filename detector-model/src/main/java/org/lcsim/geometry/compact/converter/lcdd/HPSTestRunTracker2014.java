package org.lcsim.geometry.compact.converter.lcdd;

import static java.lang.Math.PI;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import org.jdom.Element;
import org.jdom.JDOMException;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014GeometryDefinition;
import org.lcsim.geometry.compact.converter.HPSTestRunTracker2014LCDDBuilder;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder;
import org.lcsim.geometry.compact.converter.HPSTrackerLCDDBuilder;
import org.lcsim.geometry.compact.converter.lcdd.util.Box;
import org.lcsim.geometry.compact.converter.lcdd.util.LCDD;
import org.lcsim.geometry.compact.converter.lcdd.util.PhysVol;
import org.lcsim.geometry.compact.converter.lcdd.util.Position;
import org.lcsim.geometry.compact.converter.lcdd.util.Rotation;
import org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector;
import org.lcsim.geometry.compact.converter.lcdd.util.Volume;
import org.lcsim.geometry.util.TransformationUtils;

/**
 * Convert the HPS Test run tracker 2014 to the LCDD format.
 * 
 * @author Per Hansson <phansson@slac.stanford.edu>
 * @todo Remove example methods from this class.
 */
public class HPSTestRunTracker2014 extends HPSTracker2014Base {

    public HPSTestRunTracker2014(Element node) throws JDOMException {
        super(node);
    }

    /*
     * (non-Javadoc)
     * @see org.lcsim.geometry.compact.converter.lcdd.HPSTracker2014Base#initializeBuilder(org.lcsim.geometry.compact.converter.lcdd.util.LCDD, org.lcsim.geometry.compact.converter.lcdd.util.SensitiveDetector)
     */
    protected HPSTrackerLCDDBuilder initializeBuilder(LCDD lcdd, SensitiveDetector sens) {
        HPSTrackerLCDDBuilder b = new HPSTestRunTracker2014LCDDBuilder(_debug, node, lcdd, sens);
        return b;
    }

    /*
     * (non-Javadoc)
     * @see org.lcsim.detector.converter.compact.HPSTracker2014ConverterBase#getModuleNumber(org.lcsim.geometry.compact.converter.JavaSurveyVolume)
     */
    protected int getModuleNumber(String surveyVolume) {
        return HPSTrackerBuilder.getHalfFromName(surveyVolume).equals("top") ? 0 : 1;
    }

    private void makeExample(LCDD lcdd, SensitiveDetector sens) throws JDOMException {

        if (_debug) {
            System.out.println("--- makeExample ----");

        }

        String volName = "example";
        Box box = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 4.0);
        lcdd.add(box);
        Volume volume = new Volume(volName + "_volume", box, lcdd.getMaterial("Vacuum"));

        org.apache.commons.math3.geometry.euclidean.threed.Vector3D axisX = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                1., 0., 0.);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D axisY = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                0., 1., 0.);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D axisZ = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                0., 0., 1.);

        double alpha1 = PI / 4.;
        double alpha2 = PI / 4.;
        double alpha3 = -PI / 4.;

        // set up a rotation by alpha1 about the X axis
        org.apache.commons.math3.geometry.euclidean.threed.Rotation r1 = new org.apache.commons.math3.geometry.euclidean.threed.Rotation(
                axisX, alpha1);

        // find y' and z'
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D axisYPrime = r1.applyTo(axisY);
        // org.apache.commons.math3.geometry.euclidean.threed.Vector3D axisZPrime = r1.applyTo(axisZ);

        if (_debug)
            System.out.println("axisYPrime: " + axisYPrime);
        // if(_debug) System.out.println("axisZPrime: " + axisZPrime);

        // set up a rotation by alpha2 about the Y' axis
        org.apache.commons.math3.geometry.euclidean.threed.Rotation r2 = new org.apache.commons.math3.geometry.euclidean.threed.Rotation(
                axisYPrime, alpha2);

        // find z''
        // org.apache.commons.math3.geometry.euclidean.threed.Vector3D axisZPrimePrime = r2.applyTo(axisZPrime);
        // if(_debug) System.out.println("axisZPrimePrime: " + axisZPrimePrime);

        // set up a rotation by alpha3 about the Z'' axis
        // org.apache.commons.math3.geometry.euclidean.threed.Rotation r3 = new
        // org.apache.commons.math3.geometry.euclidean.threed.Rotation(axisZPrimePrime, alpha3);

        if (_debug)
            System.out.println("r1 (XYZ): " + r1.toString());

        org.apache.commons.math3.geometry.euclidean.threed.Rotation r12 = r2.applyTo(r1);

        // find z''
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D axisZPrimePrime = r12.applyTo(axisZ);
        if (_debug)
            System.out.println("axisZPrimePrime: " + axisZPrimePrime);
        org.apache.commons.math3.geometry.euclidean.threed.Rotation r3 = new org.apache.commons.math3.geometry.euclidean.threed.Rotation(
                axisZPrimePrime, alpha3);

        org.apache.commons.math3.geometry.euclidean.threed.Rotation r123 = r3.applyTo(r12);

        // double [] rotations = r12.getAngles(org.apache.commons.math3.geometry.euclidean.threed.RotationOrder.XYZ);
        double[] rotations = r123.getAngles(org.apache.commons.math3.geometry.euclidean.threed.RotationOrder.XYZ);

        Position pos = new Position(volName + "_position",
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width * 1.5, 0, 0);
        // Rotation rot = new Rotation(volName + "_rotation",rotations[0],rotations[1],rotations[2]);
        Rotation rot = new Rotation(volName + "_rotation", 0, 0, 0);
        lcdd.add(pos);
        lcdd.add(rot);

        PhysVol basePV = new PhysVol(volume, lcdd.pickMotherVolume(this), pos, rot);
        if (_debug) {
            System.out.println("Created physical vomume " + basePV.getName());
        }

        volName = volName + "_sub";
        Box boxSub = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 8.0);
        lcdd.add(boxSub);
        Volume volumeSub = new Volume(volName + "_volume", boxSub, lcdd.getMaterial("Vacuum"));
        Position subPos = new Position(volName + "_position", 0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0 * 2
                        - HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 8.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 16.0);
        Rotation subRot = new Rotation(volName + "_rotation", 0, 0, 0);
        lcdd.add(subPos);
        lcdd.add(subRot);
        PhysVol subBasePV = new PhysVol(volumeSub, volume, subPos, subRot);
        if (_debug) {
            System.out.println("Created physical vomume " + subBasePV.getName());
        }

        lcdd.add(volumeSub);
        volumeSub.setVisAttributes(lcdd.getVisAttributes("SensorVis"));

        lcdd.add(volume);

    }

    private void makeExample2(LCDD lcdd, SensitiveDetector sens) throws JDOMException {

        if (_debug) {
            System.out.println("--- makeExample2 ----");

        }

        String volName = "example2";
        Box box = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 4.0);
        lcdd.add(box);
        Volume volume = new Volume(volName + "_volume", box, lcdd.getMaterial("Vacuum"));

        org.apache.commons.math3.geometry.euclidean.threed.Vector3D axisX = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                1., 0., 0.);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D axisY = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                0., 1., 0.);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D axisZ = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                0., 0., 1.);

        double alpha1 = PI / 4.;
        double alpha2 = PI / 4.;
        double alpha3 = -PI / 4.;

        org.apache.commons.math3.geometry.euclidean.threed.Rotation r123 = new org.apache.commons.math3.geometry.euclidean.threed.Rotation(
                org.apache.commons.math3.geometry.euclidean.threed.RotationOrder.XYZ, alpha1, alpha2, alpha3);

        org.apache.commons.math3.geometry.euclidean.threed.Vector3D axisXPrime = r123.applyTo(axisX);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D axisYPrime = r123.applyTo(axisY);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D axisZPrime = r123.applyTo(axisZ);

        // if(_debug) System.out.println("axisYPrime: " + axisYPrime);
        // if(_debug) System.out.println("axisZPrime: " + axisZPrime);

        // double [] rotations = r123.getAngles(org.apache.commons.math3.geometry.euclidean.threed.RotationOrder.XYZ);

        // double [] rotations = r12.getAngles(org.apache.commons.math3.geometry.euclidean.threed.RotationOrder.XYZ);
        double[] rotations = r123.getAngles(org.apache.commons.math3.geometry.euclidean.threed.RotationOrder.XYZ);

        Position pos = new Position(volName + "_position",
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width * 1.5, 0, 0);
        Rotation rot = new Rotation(volName + "_rotation", rotations[0], rotations[1], rotations[2]);
        lcdd.add(pos);
        lcdd.add(rot);

        PhysVol basePV = new PhysVol(volume, lcdd.pickMotherVolume(this), pos, rot);
        if (_debug) {
            System.out.println("Created physical vomume " + basePV.getName());
        }

        volName = volName + "_sub";
        Box boxSub = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 8.0);
        lcdd.add(boxSub);
        Volume volumeSub = new Volume(volName + "_volume", boxSub, lcdd.getMaterial("Vacuum"));
        Position subPos = new Position(volName + "_position", 0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0 * 2
                        - HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 8.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 16.0);
        Rotation subRot = new Rotation(volName + "_rotation", 0, 0, 0);
        lcdd.add(subPos);
        lcdd.add(subRot);
        PhysVol subBasePV = new PhysVol(volumeSub, volume, subPos, subRot);
        if (_debug) {
            System.out.println("Created physical vomume " + subBasePV.getName());
        }

        lcdd.add(volumeSub);
        volumeSub.setVisAttributes(lcdd.getVisAttributes("SensorVis"));

        lcdd.add(volume);

    }

    private void makeExample3(LCDD lcdd, SensitiveDetector sens) throws JDOMException {

        if (_debug) {
            System.out.println("--- makeExample3 ----");

        }

        String volName = "example3";
        Box box = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 4.0);
        lcdd.add(box);
        Volume volume = new Volume(volName + "_volume", box, lcdd.getMaterial("Vacuum"));

        /*
         * TestRunModuleL13: survey positions before ref support_plate_top transform Survey pos for module_L1t: ballPos [ 25.000, 676.10, -4.3500] veePos [ 95.000, 676.10, -4.3500] flatPos [ 60.000, 670.10, -4.3500] TestRunModuleL13: Ref support_plate_top coord Coordinate system: origin [ 40.314, 142.71, 138.40] u [ 0.99955, 0.030000, 0.0000] v [ -0.030000, 0.99955, 0.0000] w [ 0.0000, -0.0000, 1.0000] TestRunModuleL13: survey positions after ref support_plate_top transform Survey pos for module_L1t: ballPos [ 45.020, 819.25, 134.05] veePos [ 114.99, 821.35, 134.05] flatPos [ 80.184, 814.31, 134.05] TestRunModuleL13: coordinate system: Coordinate system: origin [ 45.020, 819.25, 134.05] u [ 0.99955, 0.030000, 0.0000] v [ 0.030000, -0.99955, 0.0000] w [ 0.0000, 0.0000, -1.0000] TestRunModuleL13: translation: [ 45.020, 819.25, 134.05] TestRunModuleL13: rotation: [ 0.999549894704642 0.030000133265350216 0.0 0.030000133265350216 -0.999549894704642 0.0 0.0 0.0 -1.0 ] LCDDBaseGeom: set
         * position and rotation for volume module_L1t getEulerAngles: u [ 0.030000, -0.99955, 0.0000] v[ 0.0000, 0.0000, -1.0000] -> [ 0.0000, 1.0000, 0.0000] [ 0.0000, 0.0000, 1.0000] Input: u {0.03; -1; 0} v {0; 0; -1} u' {0; 1; 0} v' {0; 0; 1} rot matrix: 0.999550 0.030000 0.000000 0.030000 -0.999550 0.000000 0.000000 0.000000 -1.000000 Resulting XYZ angles [ -3.1416, 0.0000, -0.030005] LCDDBaseGeom: box_center_base_local [ 97.600, 0.0000, 29.150] LCDDBaseGeom: box_center_base [ 142.58, 822.18, 104.90] LCDDBaseGeom: mother center [ 192.50, 608.00, 81.550] LCDDBaseGeom: box_center [ -49.924, 214.18, 23.350] LCDDBaseGeom: pos [Element: <position/>] LCDDBaseGeom: euler [ -3.1416, 0.0000, -0.030005] LCDDBaseGeom: rot [Element: <rotation/>] LCDDBaseGeom: DONE constructing LCDD object module_L1t
         */

        Hep3Vector u = new BasicHep3Vector(1, 0, 0);
        Hep3Vector v = new BasicHep3Vector(0, 1, 0);
        Hep3Vector w = new BasicHep3Vector(0, 0, 1);

        Hep3Vector u_L1 = new BasicHep3Vector(0.99955, 0.030000, 0.0000);
        Hep3Vector v_L1 = new BasicHep3Vector(0.030000, -0.99955, 0.0000);
        Hep3Vector w_L1 = new BasicHep3Vector(0.0000, 0.0000, -1.0000);

        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D_L1 = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                u_L1.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D_L1 = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                v_L1.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D_L1 = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                w_L1.v());

        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                u.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                v.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                w.v());

        Hep3Vector euler_angles = TransformationUtils.getCardanAngles(v_L1, w_L1, v, w);

        // Get the generic rotation
        org.apache.commons.math3.geometry.euclidean.threed.Rotation r = new org.apache.commons.math3.geometry.euclidean.threed.Rotation(
                v_3D_L1, w_3D_L1, v_3D, w_3D);
        // Get the angles
        double rotations[] = r.getAngles(org.apache.commons.math3.geometry.euclidean.threed.RotationOrder.XYZ);

        if (_debug) {
            System.out.println("getEulerAngles gives euler_angles: " + euler_angles.toString());
            System.out.println("manual          gives euler_angles: (" + rotations[0] + "," + rotations[1] + ","
                    + rotations[2] + ")");
        }

        if ((rotations[0] - euler_angles.x()) > 0.00001 || (rotations[1] - euler_angles.y()) > 0.00001
                || (rotations[2] - euler_angles.z()) > 0.00001) {
            // System.("closing the loop in apache rotation didn't work!");
            // throw new RuntimeException("closing the loop in apache rotation didn't work!");
        }

        Position pos = new Position(volName + "_position",
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width * 1.5 * 4, 0, 0);
        Rotation rot = new Rotation(volName + "_rotation", rotations[0], rotations[1], rotations[2]);
        lcdd.add(pos);
        lcdd.add(rot);

        PhysVol basePV = new PhysVol(volume, lcdd.pickMotherVolume(this), pos, rot);
        if (_debug) {
            System.out.println("Created physical vomume " + basePV.getName());
        }

        volName = volName + "_sub";
        Box boxSub = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 8.0);
        lcdd.add(boxSub);
        Volume volumeSub = new Volume(volName + "_volume", boxSub, lcdd.getMaterial("Vacuum"));
        Position subPos = new Position(volName + "_position", 0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0 * 2
                        - HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 8.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 16.0);
        Rotation subRot = new Rotation(volName + "_rotation", 0, 0, 0);
        lcdd.add(subPos);
        lcdd.add(subRot);
        PhysVol subBasePV = new PhysVol(volumeSub, volume, subPos, subRot);
        if (_debug) {
            System.out.println("Created physical vomume " + subBasePV.getName());
        }

        lcdd.add(volumeSub);
        volumeSub.setVisAttributes(lcdd.getVisAttributes("HybridVis"));

        lcdd.add(volume);
    }

    private void makeExample4(LCDD lcdd, SensitiveDetector sens) throws JDOMException {

        if (_debug) {
            System.out.println("--- makeExample4 ----");

        }

        String volName = "example4";
        Box box = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 4.0);
        lcdd.add(box);
        Volume volume = new Volume(volName + "_volume", box, lcdd.getMaterial("Vacuum"));

        Hep3Vector u = new BasicHep3Vector(1, 0, 0);
        Hep3Vector v = new BasicHep3Vector(0, 1, 0);
        Hep3Vector w = new BasicHep3Vector(0, 0, 1);

        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                u.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                v.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                w.v());

        // set up a rotation about the X axis
        org.apache.commons.math3.geometry.euclidean.threed.Rotation r1 = new org.apache.commons.math3.geometry.euclidean.threed.Rotation(
                u_3D, -1.0 * Math.PI);

        // find y' and z'
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D_p = r1.applyTo(u_3D);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D_p = r1.applyTo(v_3D);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D_p = r1.applyTo(w_3D);

        double[] rotations = r1.getAngles(org.apache.commons.math3.geometry.euclidean.threed.RotationOrder.XYZ);

        if (_debug) {
            System.out.println("u_3D:       " + u_3D.toString());
            System.out.println("v_3D:       " + v_3D.toString());
            System.out.println("w_3D:       " + w_3D.toString());
            r1.toString();
            System.out.println("u_3D_p: " + u_3D_p.toString());
            System.out.println("v_3D_p: " + v_3D_p.toString());
            System.out.println("w_3D_p: " + w_3D_p.toString());

            System.out.println("gives euler_angles: (" + rotations[0] + "," + rotations[1] + "," + rotations[2] + ")");

        }

        // apply to unit vector

        Position pos = new Position(volName + "_position",
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width * 1.5 * 2, 0, 0);
        Rotation rot = new Rotation(volName + "_rotation", rotations[0], rotations[1], rotations[2]);
        lcdd.add(pos);
        lcdd.add(rot);

        PhysVol basePV = new PhysVol(volume, lcdd.pickMotherVolume(this), pos, rot);
        if (_debug) {
            System.out.println("Created physical vomume " + basePV.getName());
        }

        volName = volName + "_sub";
        Box boxSub = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 8.0);
        lcdd.add(boxSub);
        Volume volumeSub = new Volume(volName + "_volume", boxSub, lcdd.getMaterial("Vacuum"));
        Position subPos = new Position(volName + "_position", 0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0 * 2
                        - HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 8.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 16.0);
        Rotation subRot = new Rotation(volName + "_rotation", 0, 0, 0);
        lcdd.add(subPos);
        lcdd.add(subRot);
        PhysVol subBasePV = new PhysVol(volumeSub, volume, subPos, subRot);
        if (_debug) {
            System.out.println("Created physical vomume " + subBasePV.getName());
        }

        lcdd.add(volumeSub);
        volumeSub.setVisAttributes(lcdd.getVisAttributes("SensorVis"));

        lcdd.add(volume);

    }

    private void makeExample5(LCDD lcdd, SensitiveDetector sens) throws JDOMException {

        if (_debug) {
            System.out.println("--- makeExample5 ----");

        }

        String volName = "example5";
        Box box = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 4.0);
        lcdd.add(box);
        Volume volume = new Volume(volName + "_volume", box, lcdd.getMaterial("Vacuum"));

        Hep3Vector u = new BasicHep3Vector(1, 0, 0);
        Hep3Vector v = new BasicHep3Vector(0, 1, 0);
        Hep3Vector w = new BasicHep3Vector(0, 0, 1);

        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                u.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                v.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                w.v());

        // set up a rotation about the X axis
        org.apache.commons.math3.geometry.euclidean.threed.Rotation r1 = new org.apache.commons.math3.geometry.euclidean.threed.Rotation(
                u_3D, -1.0 * Math.PI);

        // find y' and z'
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D_p = r1.applyTo(u_3D);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D_p = r1.applyTo(v_3D);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D_p = r1.applyTo(w_3D);

        // set up a rotation about the Z axis
        org.apache.commons.math3.geometry.euclidean.threed.Rotation r3 = new org.apache.commons.math3.geometry.euclidean.threed.Rotation(
                w_3D_p, -0.03);
        org.apache.commons.math3.geometry.euclidean.threed.Rotation r13 = r3.applyTo(r1);

        // find y' and z'
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D_pp = r13.applyTo(u_3D);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D_pp = r13.applyTo(v_3D);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D_pp = r13.applyTo(w_3D);
        // org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D_pp = r13.applyTo(u_3D_p);
        // org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D_pp = r13.applyTo(v_3D_p);
        // org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D_pp = r13.applyTo(w_3D_p);

        double[] rotations = r13.getAngles(org.apache.commons.math3.geometry.euclidean.threed.RotationOrder.XYZ);

        if (_debug) {
            System.out.println("u_3D:       " + u_3D.toString());
            System.out.println("v_3D:       " + v_3D.toString());
            System.out.println("w_3D:       " + w_3D.toString());
            r1.toString();
            System.out.println("u_3D_p: " + u_3D_p.toString());
            System.out.println("v_3D_p: " + v_3D_p.toString());
            System.out.println("w_3D_p: " + w_3D_p.toString());
            r13.toString();
            System.out.println("u_3D_pp: " + u_3D_pp.toString());
            System.out.println("v_3D_pp: " + v_3D_pp.toString());
            System.out.println("w_3D_pp: " + w_3D_pp.toString());
            System.out.println("gives euler_angles: (" + rotations[0] + "," + rotations[1] + "," + rotations[2] + ")");

        }

        // apply to unit vector

        Position pos = new Position(volName + "_position",
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width * 1.5 * 3, 0, 0);
        Rotation rot = new Rotation(volName + "_rotation", rotations[0], rotations[1], rotations[2]);
        lcdd.add(pos);
        lcdd.add(rot);

        PhysVol basePV = new PhysVol(volume, lcdd.pickMotherVolume(this), pos, rot);
        if (_debug) {
            System.out.println("Created physical vomume " + basePV.getName());
        }

        volName = volName + "_sub";
        Box boxSub = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 8.0);
        lcdd.add(boxSub);
        Volume volumeSub = new Volume(volName + "_volume", boxSub, lcdd.getMaterial("Vacuum"));
        Position subPos = new Position(volName + "_position", 0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0 * 2
                        - HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 8.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 16.0);
        Rotation subRot = new Rotation(volName + "_rotation", 0, 0, 0);
        lcdd.add(subPos);
        lcdd.add(subRot);
        PhysVol subBasePV = new PhysVol(volumeSub, volume, subPos, subRot);
        if (_debug) {
            System.out.println("Created physical vomume " + subBasePV.getName());
        }

        lcdd.add(volumeSub);
        volumeSub.setVisAttributes(lcdd.getVisAttributes("SensorVis"));

        lcdd.add(volume);

    }

    private void makeExample5b(LCDD lcdd, SensitiveDetector sens) throws JDOMException {

        if (_debug) {
            System.out.println("--- makeExample5b ----");

        }

        String volName = "example5b";
        Box box = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 4.0);
        lcdd.add(box);
        Volume volume = new Volume(volName + "_volume", box, lcdd.getMaterial("Vacuum"));

        Hep3Vector u = new BasicHep3Vector(1, 0, 0);
        Hep3Vector v = new BasicHep3Vector(0, 1, 0);
        Hep3Vector w = new BasicHep3Vector(0, 0, 1);

        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                u.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                v.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                w.v());

        // set up a rotation about the Z axis
        org.apache.commons.math3.geometry.euclidean.threed.Rotation r3 = new org.apache.commons.math3.geometry.euclidean.threed.Rotation(
                w_3D, -0.03);

        // find y' and z'
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D_p = r3.applyTo(u_3D);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D_p = r3.applyTo(v_3D);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D_p = r3.applyTo(w_3D);

        double[] rotations = r3.getAngles(org.apache.commons.math3.geometry.euclidean.threed.RotationOrder.XYZ);

        if (_debug) {
            System.out.println("u_3D:       " + u_3D.toString());
            System.out.println("v_3D:       " + v_3D.toString());
            System.out.println("w_3D:       " + w_3D.toString());
            r3.toString();
            System.out.println("u_3D_p: " + u_3D_p.toString());
            System.out.println("v_3D_p: " + v_3D_p.toString());
            System.out.println("w_3D_p: " + w_3D_p.toString());
            System.out.println("gives euler_angles: (" + rotations[0] + "," + rotations[1] + "," + rotations[2] + ")");

        }

        // apply to unit vector

        Position pos = new Position(volName + "_position",
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width * 1.5 * 3,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length * -1.5, 0);
        Rotation rot = new Rotation(volName + "_rotation", rotations[0], rotations[1], rotations[2]);
        lcdd.add(pos);
        lcdd.add(rot);

        PhysVol basePV = new PhysVol(volume, lcdd.pickMotherVolume(this), pos, rot);
        if (_debug) {
            System.out.println("Created physical vomume " + basePV.getName());
        }

        volName = volName + "_sub";
        Box boxSub = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 8.0);
        lcdd.add(boxSub);
        Volume volumeSub = new Volume(volName + "_volume", boxSub, lcdd.getMaterial("Vacuum"));
        Position subPos = new Position(volName + "_position", 0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0 * 2
                        - HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 8.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 16.0);
        Rotation subRot = new Rotation(volName + "_rotation", 0, 0, 0);
        lcdd.add(subPos);
        lcdd.add(subRot);
        PhysVol subBasePV = new PhysVol(volumeSub, volume, subPos, subRot);
        if (_debug) {
            System.out.println("Created physical vomume " + subBasePV.getName());
        }

        lcdd.add(volumeSub);
        volumeSub.setVisAttributes(lcdd.getVisAttributes("SensorVis"));

        lcdd.add(volume);

    }

    private void makeExample3b(LCDD lcdd, SensitiveDetector sens) throws JDOMException {

        if (_debug) {
            System.out.println("--- makeExample3b ----");

        }

        String volName = "example3b";
        Box box = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 4.0);
        lcdd.add(box);
        Volume volume = new Volume(volName + "_volume", box, lcdd.getMaterial("Vacuum"));

        /*
         * TestRunModuleL13: survey positions Survey pos for module_L1b: ballPos [ 25.000, 661.10, 4.3500] veePos [ 95.000, 661.10, 4.3500] flatPos [ 60.000, 667.10, 4.3500] TestRunModuleL13: survey positions before ref support_plate_bottom transform Survey pos for module_L1b: ballPos [ 25.000, 661.10, 4.3500] veePos [ 95.000, 661.10, 4.3500] flatPos [ 60.000, 667.10, 4.3500] TestRunModuleL13: Ref support_plate_bottom coord Coordinate system: origin [ 40.314, 142.71, 22.700] u [ 0.99955, 0.030000, 0.0000] v [ -0.030000, 0.99955, 0.0000] w [ 0.0000, -0.0000, 1.0000] TestRunModuleL13: survey positions after ref support_plate_bottom transform Survey pos for module_L1b: ballPos [ 45.470, 804.26, 27.050] veePos [ 115.44, 806.36, 27.050] flatPos [ 80.274, 811.31, 27.050] TestRunModuleL13: coordinate system: Coordinate system: origin [ 45.470, 804.26, 27.050] u [ 0.99955, 0.030000, 0.0000] v [ -0.030000, 0.99955, 0.0000] w [ 0.0000, 0.0000, 1.0000] TestRunModuleL13: translation: [ 45.470,
         * 804.26, 27.050] TestRunModuleL13: rotation: [ 0.9995498947046422 -0.030000133265350216 0.0 0.030000133265350216 0.9995498947046422 0.0 0.0 0.0 1.0 ] LCDDBaseGeom: set position and rotation for volume module_L1b getEulerAngles: u [ -0.030000, 0.99955, 0.0000] v[ 0.0000, 0.0000, 1.0000] -> [ 0.0000, 1.0000, 0.0000] [ 0.0000, 0.0000, 1.0000] Input: u {-0.03; 1; 0} v {0; 0; 1} u' {0; 1; 0} v' {0; 0; 1} rot matrix: 0.999550 0.030000 0.000000 -0.030000 0.999550 -0.000000 -0.000000 0.000000 1.000000 Resulting XYZ angles [ 0.0000, 0.0000, -0.030005] LCDDBaseGeom: box_center_base_local [ 97.600, 0.0000, 29.150] LCDDBaseGeom: box_center_base [ 143.03, 807.19, 56.200] LCDDBaseGeom: mother center [ 192.50, 608.00, 81.550] LCDDBaseGeom: box_center [ -49.474, 199.19, -25.350] LCDDBaseGeom: pos [Element: <position/>] LCDDBaseGeom: euler [ 0.0000, 0.0000, -0.030005] LCDDBaseGeom: rot [Element: <rotation/>] LCDDBaseGeom: DONE constructing LCDD object module_L1b
         */

        Hep3Vector u = new BasicHep3Vector(1, 0, 0);
        Hep3Vector v = new BasicHep3Vector(0, 1, 0);
        Hep3Vector w = new BasicHep3Vector(0, 0, 1);

        Hep3Vector u_L1 = new BasicHep3Vector(0.99955, 0.030000, 0.0000);
        Hep3Vector v_L1 = new BasicHep3Vector(-0.030000, 0.99955, 0.0000);
        Hep3Vector w_L1 = new BasicHep3Vector(0.0000, 0.0000, 1.0000);

        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D_L1 = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                u_L1.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D_L1 = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                v_L1.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D_L1 = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                w_L1.v());

        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                u.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                v.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                w.v());

        Hep3Vector euler_angles = TransformationUtils.getCardanAngles(v_L1, w_L1, v, w);

        // Get the generic rotation
        org.apache.commons.math3.geometry.euclidean.threed.Rotation r = new org.apache.commons.math3.geometry.euclidean.threed.Rotation(
                v_3D_L1, w_3D_L1, v_3D, w_3D);
        // Get the angles
        double rotations[] = r.getAngles(org.apache.commons.math3.geometry.euclidean.threed.RotationOrder.XYZ);

        if (_debug) {
            System.out.println("getEulerAngles gives euler_angles: " + euler_angles.toString());
            System.out.println("manual          gives euler_angles: (" + rotations[0] + "," + rotations[1] + ","
                    + rotations[2] + ")");
        }

        if ((rotations[0] - euler_angles.x()) > 0.00001 || (rotations[1] - euler_angles.y()) > 0.00001
                || (rotations[2] - euler_angles.z()) > 0.00001) {
            // throw new RuntimeException("closing the loop in apache rotation didn't work!");
        }

        Position pos = new Position(volName + "_position",
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width * 1.5 * 4,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length * -1.5, 0);
        Rotation rot = new Rotation(volName + "_rotation", rotations[0], rotations[1], rotations[2]);
        lcdd.add(pos);
        lcdd.add(rot);

        PhysVol basePV = new PhysVol(volume, lcdd.pickMotherVolume(this), pos, rot);
        if (_debug) {
            System.out.println("Created physical vomume " + basePV.getName());
        }

        volName = volName + "_sub";
        Box boxSub = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 8.0);
        lcdd.add(boxSub);
        Volume volumeSub = new Volume(volName + "_volume", boxSub, lcdd.getMaterial("Vacuum"));
        Position subPos = new Position(volName + "_position", 0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0 * 2
                        - HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 8.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 16.0);
        Rotation subRot = new Rotation(volName + "_rotation", 0, 0, 0);
        lcdd.add(subPos);
        lcdd.add(subRot);
        PhysVol subBasePV = new PhysVol(volumeSub, volume, subPos, subRot);
        if (_debug) {
            System.out.println("Created physical vomume " + subBasePV.getName());
        }

        lcdd.add(volumeSub);
        volumeSub.setVisAttributes(lcdd.getVisAttributes("HybridVis"));

        lcdd.add(volume);

    }

    private void makeExample6(LCDD lcdd, SensitiveDetector sens) throws JDOMException {

        if (_debug) {
            System.out.println("--- makeExample6 ----");

        }

        String volName = "example6";
        Box box = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 4.0);
        lcdd.add(box);
        Volume volume = new Volume(volName + "_volume", box, lcdd.getMaterial("Vacuum"));

        double[] rotations = {-0.5 * Math.PI, 0, 0};

        if (_debug) {

            System.out.println("manual set lcdd angles: (" + rotations[0] + "," + rotations[1] + "," + rotations[2]
                    + ")");

        }

        // apply to unit vector

        Position pos = new Position(volName + "_position",
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width * 1.5 * -2, 0, 0);
        Rotation rot = new Rotation(volName + "_rotation", rotations[0], rotations[1], rotations[2]);
        lcdd.add(pos);
        lcdd.add(rot);

        PhysVol basePV = new PhysVol(volume, lcdd.pickMotherVolume(this), pos, rot);
        if (_debug) {
            System.out.println("Created physical vomume " + basePV.getName());
        }

        volName = volName + "_sub";
        Box boxSub = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 8.0);
        lcdd.add(boxSub);
        Volume volumeSub = new Volume(volName + "_volume", boxSub, lcdd.getMaterial("Vacuum"));
        Position subPos = new Position(volName + "_position", 0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0 * 2
                        - HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 8.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 16.0);
        Rotation subRot = new Rotation(volName + "_rotation", 0, 0, 0);
        lcdd.add(subPos);
        lcdd.add(subRot);
        PhysVol subBasePV = new PhysVol(volumeSub, volume, subPos, subRot);
        if (_debug) {
            System.out.println("Created physical vomume " + subBasePV.getName());
        }

        lcdd.add(volumeSub);
        volumeSub.setVisAttributes(lcdd.getVisAttributes("SensorVis"));

        lcdd.add(volume);

    }

    private void makeExample66(LCDD lcdd, SensitiveDetector sens) throws JDOMException {

        if (_debug) {
            System.out.println("--- makeExample66 ----");

        }

        String volName = "example66";
        Box box = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 4.0);
        lcdd.add(box);
        Volume volume = new Volume(volName + "_volume", box, lcdd.getMaterial("Vacuum"));

        Hep3Vector u = new BasicHep3Vector(1, 0, 0);
        Hep3Vector v = new BasicHep3Vector(0, 1, 0);
        Hep3Vector w = new BasicHep3Vector(0, 0, 1);

        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                u.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                v.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                w.v());

        // set up a rotation about the X axis
        org.apache.commons.math3.geometry.euclidean.threed.Rotation r1 = new org.apache.commons.math3.geometry.euclidean.threed.Rotation(
                u_3D, -0.5 * Math.PI);

        // find y' and z'
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D_p = r1.applyTo(u_3D);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D_p = r1.applyTo(v_3D);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D_p = r1.applyTo(w_3D);

        double[] rotations = r1.getAngles(org.apache.commons.math3.geometry.euclidean.threed.RotationOrder.XYZ);

        if (_debug) {
            System.out.println("u_3D:       " + u_3D.toString());
            System.out.println("v_3D:       " + v_3D.toString());
            System.out.println("w_3D:       " + w_3D.toString());
            r1.toString();
            System.out.println("u_3D_p: " + u_3D_p.toString());
            System.out.println("v_3D_p: " + v_3D_p.toString());
            System.out.println("w_3D_p: " + w_3D_p.toString());

            System.out.println("gives euler_angles: (" + rotations[0] + "," + rotations[1] + "," + rotations[2] + ")");

        }

        // apply to unit vector

        Position pos = new Position(volName + "_position",
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width * 1.5 * -4, 0, 0);
        Rotation rot = new Rotation(volName + "_rotation", rotations[0], rotations[1], rotations[2]);
        lcdd.add(pos);
        lcdd.add(rot);

        PhysVol basePV = new PhysVol(volume, lcdd.pickMotherVolume(this), pos, rot);
        if (_debug) {
            System.out.println("Created physical vomume " + basePV.getName());
        }

        volName = volName + "_sub";
        Box boxSub = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 8.0);
        lcdd.add(boxSub);
        Volume volumeSub = new Volume(volName + "_volume", boxSub, lcdd.getMaterial("Vacuum"));
        Position subPos = new Position(volName + "_position", 0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0 * 2
                        - HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 8.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 16.0);
        Rotation subRot = new Rotation(volName + "_rotation", 0, 0, 0);
        lcdd.add(subPos);
        lcdd.add(subRot);
        PhysVol subBasePV = new PhysVol(volumeSub, volume, subPos, subRot);
        if (_debug) {
            System.out.println("Created physical vomume " + subBasePV.getName());
        }

        lcdd.add(volumeSub);
        volumeSub.setVisAttributes(lcdd.getVisAttributes("SensorVis"));

        lcdd.add(volume);

    }

    private void makeExample7(LCDD lcdd, SensitiveDetector sens) throws JDOMException {

        if (_debug) {
            System.out.println("--- makeExample7 ----");

        }

        String volName = "example7";
        Box box = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 4.0);
        lcdd.add(box);
        Volume volume = new Volume(volName + "_volume", box, lcdd.getMaterial("Vacuum"));

        double[] rotations = {-0.5 * Math.PI, 0, -0.25 * Math.PI};

        if (_debug) {

            System.out.println("manual set lcdd angles: (" + rotations[0] + "," + rotations[1] + "," + rotations[2]
                    + ")");

        }

        // apply to unit vector

        Position pos = new Position(volName + "_position",
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width * 1.5 * -2,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length * -1, 0);
        Rotation rot = new Rotation(volName + "_rotation", rotations[0], rotations[1], rotations[2]);
        lcdd.add(pos);
        lcdd.add(rot);

        PhysVol basePV = new PhysVol(volume, lcdd.pickMotherVolume(this), pos, rot);
        if (_debug) {
            System.out.println("Created physical vomume " + basePV.getName());
        }

        volName = volName + "_sub";
        Box boxSub = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 8.0);
        lcdd.add(boxSub);
        Volume volumeSub = new Volume(volName + "_volume", boxSub, lcdd.getMaterial("Vacuum"));
        Position subPos = new Position(volName + "_position", 0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0 * 2
                        - HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 8.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 16.0);
        Rotation subRot = new Rotation(volName + "_rotation", 0, 0, 0);
        lcdd.add(subPos);
        lcdd.add(subRot);
        PhysVol subBasePV = new PhysVol(volumeSub, volume, subPos, subRot);
        if (_debug) {
            System.out.println("Created physical vomume " + subBasePV.getName());
        }

        lcdd.add(volumeSub);
        volumeSub.setVisAttributes(lcdd.getVisAttributes("SensorVis"));

        lcdd.add(volume);

    }

    private void makeExample77(LCDD lcdd, SensitiveDetector sens) throws JDOMException {

        if (_debug) {
            System.out.println("--- makeExample77 ----");

        }

        String volName = "example77";
        Box box = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 4.0);
        lcdd.add(box);
        Volume volume = new Volume(volName + "_volume", box, lcdd.getMaterial("Vacuum"));

        Hep3Vector u = new BasicHep3Vector(1, 0, 0);
        Hep3Vector v = new BasicHep3Vector(0, 1, 0);
        Hep3Vector w = new BasicHep3Vector(0, 0, 1);

        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                u.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                v.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                w.v());

        // set up a rotation about the X axis
        org.apache.commons.math3.geometry.euclidean.threed.Rotation r1 = new org.apache.commons.math3.geometry.euclidean.threed.Rotation(
                u_3D, -0.5 * Math.PI);

        // find y' and z'
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D_p = r1.applyTo(u_3D);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D_p = r1.applyTo(v_3D);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D_p = r1.applyTo(w_3D);

        // set up a rotation about the Z xis
        org.apache.commons.math3.geometry.euclidean.threed.Rotation r3 = new org.apache.commons.math3.geometry.euclidean.threed.Rotation(
                w_3D_p, -0.25 * Math.PI);

        org.apache.commons.math3.geometry.euclidean.threed.Rotation r13 = r3.applyTo(r1);

        // find y'' and z''
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D_pp = r13.applyTo(u_3D);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D_pp = r13.applyTo(v_3D);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D_pp = r13.applyTo(w_3D);

        // find y'' and z'' (cross-check)
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D_pp_2 = r3.applyTo(u_3D_p);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D_pp_2 = r3.applyTo(v_3D_p);
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D_pp_2 = r3.applyTo(w_3D_p);

        double[] rotations = r13.getAngles(org.apache.commons.math3.geometry.euclidean.threed.RotationOrder.XYZ);

        if (_debug) {
            System.out.println("u_3D:       " + u_3D.toString());
            System.out.println("v_3D:       " + v_3D.toString());
            System.out.println("w_3D:       " + w_3D.toString());
            r1.toString();
            System.out.println("u_3D_p: " + u_3D_p.toString());
            System.out.println("v_3D_p: " + v_3D_p.toString());
            System.out.println("w_3D_p: " + w_3D_p.toString());
            r13.toString();
            System.out.println("u_3D_pp: " + u_3D_pp.toString());
            System.out.println("v_3D_pp: " + v_3D_pp.toString());
            System.out.println("w_3D_pp: " + w_3D_pp.toString());

            System.out.println("gives euler_angles: (" + rotations[0] + "," + rotations[1] + "," + rotations[2] + ")");

            System.out.println("u_3D_pp_2: " + u_3D_pp_2.toString());
            System.out.println("v_3D_pp_2: " + v_3D_pp_2.toString());
            System.out.println("w_3D_pp_2: " + w_3D_pp_2.toString());
        }

        // apply to unit vector

        Position pos = new Position(volName + "_position",
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width * 1.5 * -4,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length * -1, 0);
        Rotation rot = new Rotation(volName + "_rotation", rotations[0], rotations[1], rotations[2]);
        lcdd.add(pos);
        lcdd.add(rot);

        PhysVol basePV = new PhysVol(volume, lcdd.pickMotherVolume(this), pos, rot);
        if (_debug) {
            System.out.println("Created physical vomume " + basePV.getName());
        }

        volName = volName + "_sub";
        Box boxSub = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 8.0);
        lcdd.add(boxSub);
        Volume volumeSub = new Volume(volName + "_volume", boxSub, lcdd.getMaterial("Vacuum"));
        Position subPos = new Position(volName + "_position", 0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0 * 2
                        - HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 8.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 16.0);
        Rotation subRot = new Rotation(volName + "_rotation", 0, 0, 0);
        lcdd.add(subPos);
        lcdd.add(subRot);
        PhysVol subBasePV = new PhysVol(volumeSub, volume, subPos, subRot);
        if (_debug) {
            System.out.println("Created physical vomume " + subBasePV.getName());
        }

        lcdd.add(volumeSub);
        volumeSub.setVisAttributes(lcdd.getVisAttributes("SensorVis"));

        lcdd.add(volume);

    }

    private void makeExample8(LCDD lcdd, SensitiveDetector sens) throws JDOMException {

        if (_debug) {
            System.out.println("--- makeExample8 ----");

        }

        String volName = "example8";
        Box box = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 4.0);
        lcdd.add(box);
        Volume volume = new Volume(volName + "_volume", box, lcdd.getMaterial("Vacuum"));

        Hep3Vector u = new BasicHep3Vector(1, 0, 0);
        Hep3Vector v = new BasicHep3Vector(0, 1, 0);
        Hep3Vector w = new BasicHep3Vector(0, 0, 1);

        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                u.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                v.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                w.v());

        Hep3Vector u_L1 = new BasicHep3Vector(1 / Math.sqrt(2), 0, 1 / Math.sqrt(2));
        Hep3Vector v_L1 = new BasicHep3Vector(-1 / Math.sqrt(2), 0, 1 / Math.sqrt(2));
        Hep3Vector w_L1 = new BasicHep3Vector(0, -1, 0);

        Hep3Vector euler_angles = TransformationUtils.getCardanAngles(u_L1, v_L1, u, v);

        if (_debug) {

            System.out.println("euler angles " + euler_angles.toString());

        }

        // apply to unit vector

        Position pos = new Position(volName + "_position",
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width * 1.5 * -1,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length * -3, 0);
        Rotation rot = new Rotation(volName + "_rotation", euler_angles.x(), euler_angles.y(), euler_angles.z());
        lcdd.add(pos);
        lcdd.add(rot);

        PhysVol basePV = new PhysVol(volume, lcdd.pickMotherVolume(this), pos, rot);
        if (_debug) {
            System.out.println("Created physical vomume " + basePV.getName());
        }

        volName = volName + "_sub";
        Box boxSub = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 8.0);
        lcdd.add(boxSub);
        Volume volumeSub = new Volume(volName + "_volume", boxSub, lcdd.getMaterial("Vacuum"));
        Position subPos = new Position(volName + "_position", 0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0 * 2
                        - HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 8.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 16.0);
        Rotation subRot = new Rotation(volName + "_rotation", 0, 0, 0);
        lcdd.add(subPos);
        lcdd.add(subRot);
        PhysVol subBasePV = new PhysVol(volumeSub, volume, subPos, subRot);
        if (_debug) {
            System.out.println("Created physical vomume " + subBasePV.getName());
        }

        lcdd.add(volumeSub);
        volumeSub.setVisAttributes(lcdd.getVisAttributes("SensorVis"));

        lcdd.add(volume);

    }

    private void makeExample9(LCDD lcdd, SensitiveDetector sens) throws JDOMException {

        if (_debug) {
            System.out.println("--- makeExample9 ----");

        }

        String volName = "example9";
        Box box = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 4.0);
        lcdd.add(box);
        Volume volume = new Volume(volName + "_volume", box, lcdd.getMaterial("Vacuum"));

        Hep3Vector u = new BasicHep3Vector(1, 0, 0);
        Hep3Vector v = new BasicHep3Vector(0, 1, 0);
        Hep3Vector w = new BasicHep3Vector(0, 0, 1);

        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                u.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                v.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                w.v());

        Hep3Vector u_L1 = new BasicHep3Vector(1, 0, 0);
        Hep3Vector v_L1 = new BasicHep3Vector(0, 0, 1);
        Hep3Vector w_L1 = new BasicHep3Vector(0, -1, 0);

        Hep3Vector euler_angles = TransformationUtils.getCardanAngles(u_L1, v_L1, u, v);

        if (_debug) {

            System.out.println("euler angles " + euler_angles.toString());

        }

        // apply to unit vector

        Position pos = new Position(volName + "_position",
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width * 1.5 * -1,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length * -2, 0);
        Rotation rot = new Rotation(volName + "_rotation", euler_angles.x(), euler_angles.y(), euler_angles.z());
        lcdd.add(pos);
        lcdd.add(rot);

        PhysVol basePV = new PhysVol(volume, lcdd.pickMotherVolume(this), pos, rot);
        if (_debug) {
            System.out.println("Created physical vomume " + basePV.getName());
        }

        volName = volName + "_sub";
        Box boxSub = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 8.0);
        lcdd.add(boxSub);
        Volume volumeSub = new Volume(volName + "_volume", boxSub, lcdd.getMaterial("Vacuum"));
        Position subPos = new Position(volName + "_position", 0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0 * 2
                        - HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 8.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 16.0);
        Rotation subRot = new Rotation(volName + "_rotation", 0, 0, 0);
        lcdd.add(subPos);
        lcdd.add(subRot);
        PhysVol subBasePV = new PhysVol(volumeSub, volume, subPos, subRot);
        if (_debug) {
            System.out.println("Created physical vomume " + subBasePV.getName());
        }

        lcdd.add(volumeSub);
        volumeSub.setVisAttributes(lcdd.getVisAttributes("SensorVis"));

        lcdd.add(volume);

    }

    private void makeExample10(LCDD lcdd, SensitiveDetector sens) throws JDOMException {

        if (_debug) {
            System.out.println("--- makeExample10 ----");

        }

        String volName = "example10";
        Box box = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 4.0);
        lcdd.add(box);
        Volume volume = new Volume(volName + "_volume", box, lcdd.getMaterial("Vacuum"));

        Hep3Vector u = new BasicHep3Vector(1, 0, 0);
        Hep3Vector v = new BasicHep3Vector(0, 1, 0);
        Hep3Vector w = new BasicHep3Vector(0, 0, 1);

        org.apache.commons.math3.geometry.euclidean.threed.Vector3D u_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                u.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D v_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                v.v());
        org.apache.commons.math3.geometry.euclidean.threed.Vector3D w_3D = new org.apache.commons.math3.geometry.euclidean.threed.Vector3D(
                w.v());

        Hep3Vector u_L1 = new BasicHep3Vector(1 / Math.sqrt(2), 1 / Math.sqrt(2), 0);
        Hep3Vector v_L1 = new BasicHep3Vector(0, 0, 1);
        Hep3Vector w_L1 = new BasicHep3Vector(1 / Math.sqrt(2), -1 / Math.sqrt(2), 0);

        Hep3Vector euler_angles = TransformationUtils.getCardanAngles(u_L1, v_L1, u, v);

        if (_debug) {

            System.out.println("euler angles " + euler_angles.toString());

        }

        // apply to unit vector

        Position pos = new Position(volName + "_position",
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width * 1.5 * -2,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length * -2, 0);
        Rotation rot = new Rotation(volName + "_rotation", euler_angles.x(), euler_angles.y(), euler_angles.z());
        lcdd.add(pos);
        lcdd.add(rot);

        PhysVol basePV = new PhysVol(volume, lcdd.pickMotherVolume(this), pos, rot);
        if (_debug) {
            System.out.println("Created physical vomume " + basePV.getName());
        }

        volName = volName + "_sub";
        Box boxSub = new Box(volName + "Box", HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_width,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 8.0);
        lcdd.add(boxSub);
        Volume volumeSub = new Volume(volName + "_volume", boxSub, lcdd.getMaterial("Vacuum"));
        Position subPos = new Position(volName + "_position", 0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 4.0 * 2
                        - HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_length / 8.0,
                HPSTestRunTracker2014GeometryDefinition.TrackerEnvelope.base_height / 16.0);
        Rotation subRot = new Rotation(volName + "_rotation", 0, 0, 0);
        lcdd.add(subPos);
        lcdd.add(subRot);
        PhysVol subBasePV = new PhysVol(volumeSub, volume, subPos, subRot);
        if (_debug) {
            System.out.println("Created physical vomume " + subBasePV.getName());
        }

        lcdd.add(volumeSub);
        volumeSub.setVisAttributes(lcdd.getVisAttributes("SensorVis"));

        lcdd.add(volume);
    }
}

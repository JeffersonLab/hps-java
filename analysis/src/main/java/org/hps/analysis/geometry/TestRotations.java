/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.analysis.geometry;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.MaterialSupervisor;
import org.lcsim.detector.ILogicalVolume;
import org.lcsim.detector.IPhysicalVolume;
import org.lcsim.detector.IPhysicalVolumeContainer;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.material.IMaterial;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiTrackerModule;
import org.lcsim.geometry.Detector;

/**
 *
 * @author ngraf
 */
public class TestRotations {

    public static void main(String[] args) throws Exception {
        boolean debug = false;
        String detectorName = "HPS-EngRun2015-Nominal-v0";
 //       String detectorName = "HPS-EngRun2015-Nominal-v6-0-fieldmap";
        final DatabaseConditionsManager manager = new DatabaseConditionsManager();
        manager.setDetector(detectorName, 5772);
        Detector detector = manager.getCachedConditions(Detector.class, "compact.xml").getCachedData();
        System.out.println(detector.getName());

        MaterialSupervisor materialManager = new MaterialSupervisor();
//        MultipleScattering scattering = new MultipleScattering(materialManager);
        materialManager.buildModel(detector);
        List<MaterialSupervisor.ScatteringDetectorVolume> stripPlanes = materialManager.getMaterialVolumes();
        //TODO replace these lists with a helper class.
        List<String> names = new ArrayList<String>();
        List<Hep3Vector> oList = new ArrayList<Hep3Vector>();
        List<Hep3Vector> uList = new ArrayList<Hep3Vector>();
        List<Hep3Vector> vList = new ArrayList<Hep3Vector>();
        List<Hep3Vector> nList = new ArrayList<Hep3Vector>();
        List<Double> measDim = new ArrayList<Double>();
        List<Double> unmeasDim = new ArrayList<Double>();
        List<Boolean> isAxial = new ArrayList<Boolean>();
        List<IPhysicalVolume> volumeList = new ArrayList<IPhysicalVolume>();

        for (MaterialSupervisor.ScatteringDetectorVolume vol : stripPlanes) {
            MaterialSupervisor.SiStripPlane plane = (MaterialSupervisor.SiStripPlane) vol;
            if (debug) {
                System.out.println(plane.getName());
            }
            names.add(plane.getName());
            Hep3Vector oprime = CoordinateTransformations.transformVectorToDetector(plane.origin());
            Hep3Vector nprime = CoordinateTransformations.transformVectorToDetector(plane.normal());
            if (debug) {
                System.out.println(" origin: " + oprime);
            }
            if (debug) {
                System.out.println(" normal: " + nprime);
            }
            if (debug) {
                System.out.println(" Plane is: " + plane.getMeasuredDimension() + " x " + plane.getUnmeasuredDimension());
            }
            HpsSiSensor sensor = (HpsSiSensor) plane.getSensor();

//            if (debug) {
//                System.out.println(SvtUtils.getInstance().isAxial(sensor) ? "axial" : "stereo");
//            }
            Hep3Vector measDir = CoordinateTransformations.transformVectorToDetector(plane.getMeasuredCoordinate());
            if (debug) {
                System.out.println("measured coordinate:    " + measDir);
            }
            Hep3Vector unmeasDir = CoordinateTransformations.transformVectorToDetector(plane.getUnmeasuredCoordinate());
            if (debug) {
                System.out.println("unmeasured coordinate:   " + unmeasDir);
            }
            if (debug) {
                System.out.println("thickness: " + plane.getThickness() + " in X0: " + plane.getThicknessInRL());
            }
            SiTrackerModule module = (SiTrackerModule) plane.getSensor().getGeometry().getDetectorElement().getParent();
            IPhysicalVolume parent = module.getGeometry().getPhysicalVolume();
            IPhysicalVolumeContainer daughters = parent.getLogicalVolume().getDaughters();
            if (debug) {
                System.out.printf(" found %d daughters to SiTrackerModule\n", daughters.size());
            }
            for (IPhysicalVolume daughter : daughters) {
                volumeList.add(daughter);
                ILogicalVolume logicalVolume = daughter.getLogicalVolume();
                IMaterial material = logicalVolume.getMaterial();
                //System.out.println(material);
                String name = material.getName();
                double X0 = 10.0 * material.getRadiationLength() / material.getDensity();
                double X0cm = material.getRadiationLengthWithDensity();
                if (debug) {
                    System.out.println("material: " + name + " with X0= " + X0 + " mm " + X0cm);
                }
                Box solid = (Box) logicalVolume.getSolid();
                if (debug) {
                    System.out.printf(" x %f y %f z %f box\n", solid.getXHalfLength(), solid.getYHalfLength(), solid.getZHalfLength());
                }
                double halfThickness = solid.getZHalfLength();
            }
            oList.add(oprime);
            nList.add(nprime);
            uList.add(measDir);
            vList.add(unmeasDir);
            measDim.add(plane.getMeasuredDimension());
            unmeasDim.add(plane.getUnmeasuredDimension());
            isAxial.add(sensor.isAxial());
        }
        DecimalFormat df = new DecimalFormat("###.######");
//        for (int i = 0; i < oList.size(); ++i) {
//            Hep3Vector o = oList.get(i);
//            Hep3Vector n = nList.get(i);
//            Hep3Vector u = uList.get(i);
//            Hep3Vector v = vList.get(i);
//            double l = unmeasDim.get(i) / 2.;
//            double h = measDim.get(i) / 2.;
////            String planeType = isAxial.get(i) ? "axial" : "stereo";
////            System.out.println("//" + planeType);
//            System.out.println(names.get(i));
//            System.out.println(df.format(o.x()) + " , " + df.format(o.y()) + " , " + df.format(o.z()) + " " + df.format(n.x()) + " , " + df.format(n.y()) + " , " + df.format(n.z()));
//
//        }

        // let's test the rotations...
        for (int i = 10; i < 12; ++i) {
            Hep3Vector X = new BasicHep3Vector(1., 0., 0.);
            Hep3Vector Y = new BasicHep3Vector(0., 1., 0.);
            Hep3Vector Z = new BasicHep3Vector(0., 0., 1.);
            Hep3Vector n = nList.get(i);
            Hep3Vector u = uList.get(i);
            Hep3Vector v = vList.get(i);

            Hep3Vector calcNormal = VecOp.cross(v,u);
            System.out.println("calculated normal v x u "+calcNormal);
            IPhysicalVolume volume = volumeList.get(i);

            Vector3D vX = Vector3D.PLUS_I;
            Vector3D vY = Vector3D.PLUS_J;
            Vector3D vZ = Vector3D.PLUS_K;

            Vector3D vXprime = new Vector3D(v.x(), v.y(), v.z());
            Vector3D vYprime = new Vector3D(u.x(), u.y(), u.z());
//            Vector3D vZprime = new Vector3D(n.x(), n.y(), n.z());
            Vector3D vZprime = new Vector3D(calcNormal.x(), calcNormal.y(), calcNormal.z());

            
            System.out.println(names.get(i));
            System.out.println("vX " + vX);
            System.out.println("vXprime " + vXprime);
            System.out.println("vY " + vY);
            System.out.println("vYprime " + vYprime);
            System.out.println("vZ " + vZ);
            System.out.println("vZprime " + vZprime);

            Rotation xyVecRot = new Rotation(vX, vY, vXprime, vYprime);
            double[][] xyVecRotMat = xyVecRot.getMatrix();
//            for (int ii = 0; ii < 3; ++ii) {
//                System.out.println(xyVecRotMat[ii][0] + " " + xyVecRotMat[ii][1] + " " + xyVecRotMat[ii][2]);
//            }
            System.out.println("");
            Rotation xzVecRot = new Rotation(vX, vZ, vXprime, vZprime);
            double[][] xzVecRotMat = xzVecRot.getMatrix();
//            for (int ii = 0; ii < 3; ++ii) {
//                System.out.println(xzVecRotMat[ii][0] + " " + xzVecRotMat[ii][1] + " " + xzVecRotMat[ii][2]);
//            }
            System.out.println("");
            Rotation yzVecRot = new Rotation(vY, vZ, vYprime, vZprime);
            double[][] yzVecRotMat = yzVecRot.getMatrix();
//            for (int ii = 0; ii < 3; ++ii) {
//                System.out.println(yzVecRotMat[ii][0] + " " + yzVecRotMat[ii][1] + " " + yzVecRotMat[ii][2]);
//            }
//            System.out.println("");
            IRotation3D volRot = volume.getRotation();
//            System.out.println(volRot);

            // so to within the signs of the rotations I am getting consistent answers.
            // Now the big question is how this relates to the usual alpha, beta, gamma rotations....
            //let's experiment
            //
            double alpha = 0.; //rotation about X axis
            double beta = 0.0305; // 30.5mr rotation about y
            double gammaAxial = 0.; // rotation about z axis for axial layer
            double gammaStereo = 0.050; // rotation about z axis for stereo layer

            double gamma = gammaAxial;
            if (names.get(i).contains("stereo")) {
                gamma = gammaStereo;
            }
            Rotation testRot = new Rotation(RotationOrder.XYX, RotationConvention.VECTOR_OPERATOR, alpha, beta, gamma);
//            System.out.println("testing...");

            double[][] testRotMat = testRot.getMatrix();
            System.out.println("testRotMat");
            for (int ii = 0; ii < 3; ++ii) {
                System.out.println(testRotMat[ii][0] + " " + testRotMat[ii][1] + " " + testRotMat[ii][2]);
            }
            System.out.println("testRotMat angles");
            double[] angles = testRot.getAngles(RotationOrder.XYX, RotationConvention.VECTOR_OPERATOR);
            System.out.println(Arrays.toString(angles));

            // so it appears that xzRot is the correct one to use.
            // test it.
            System.out.println("");
            System.out.println("xzVecRotMat");
            for (int ii = 0; ii < 3; ++ii) {
                System.out.println(xzVecRotMat[ii][0] + " " + xzVecRotMat[ii][1] + " " + xzVecRotMat[ii][2]);
            }
            System.out.println("xzVecRotMat angles");
            double[] hpsAngles = xzVecRot.getAngles(RotationOrder.XYX, RotationConvention.VECTOR_OPERATOR);
            System.out.println(Arrays.toString(hpsAngles));

            //hmmm, seems to work OK for the bottom axial, but not so much for the stereo
        }
    }

}

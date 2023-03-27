package org.hps.recon.tracking;  

import junit.framework.TestCase;

import static org.junit.Assert.*;
import org.junit.Test; 

import java.util.ArrayList;
import java.util.List;
import java.lang.Math;

import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.DoubleByReference;

import org.hps.util.RandomGaussian;
import org.hps.recon.tracking.gbl.GblPointJna; 
import org.hps.recon.tracking.gbl.GblTrajectoryJna; 
import org.hps.recon.tracking.gbl.MilleBinaryJna; 
import org.hps.recon.tracking.gbl.matrix.SymMatrix;
import org.hps.recon.tracking.gbl.matrix.Matrix; 
import org.hps.recon.tracking.gbl.matrix.Vector; 

public class GblJNA  { 

    @Test 
    public void createAndDestroyGblPoint() {
        // Create a 5 x 5 unit matrix.  This will be passed to instantiate
        Matrix jacPointToPoint = new Matrix(5, 5); 
        jacPointToPoint.UnitMatrix(); 
        
        long startTime = System.nanoTime();
        for (int i=0; i < 100; i++) {
            // Create an instance of the JNA GBL point.  
            GblPointJna gblPointJna = new GblPointJna(jacPointToPoint);  
            gblPointJna.delete();
        }
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        System.out.printf("GblPointJna Time Elapsed %f ms \n", (double)duration/1000000.);
    }

    @Test
    public void GblPoint_addMeasurement() {
        // Create a 5 x 5 unit matrix.  This will be passed to instantiate
        Matrix jacPointToPoint = new Matrix(5, 5); 
        jacPointToPoint.UnitMatrix(); 

        Matrix m = new Matrix(2,2);
        m.UnitMatrix();
        Vector v = new Vector(2);
        v.set(0,0.);
        v.set(1,1.);
        
        long startTime = System.nanoTime();
        GblPointJna gblPointJna = new GblPointJna(jacPointToPoint);  
        for (int i=0; i < 100; i++) {
            gblPointJna.addMeasurement(m,v,v);
        }
        gblPointJna.delete();
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        System.out.printf("GblPointJna measurement Time Elapsed %f ms \n", (double)duration/1000000.);
    } 

    /**
     * Non-functional test since the matrix inversion
     * on the GBL-C++ side fails since we don't actually
     * provide it any measurements. Just leaving it out
     * of the testing suite instead of trying to construct
     * a more full test (like the one below).
     */
    //@Test
    public void GblPoint_getGlobalLabelsAndDerivatives() {
        // Create a 5 x 5 unit matrix.  This will be passed to instantiate
        Matrix jacPointToPoint = new Matrix(5, 5); 
        jacPointToPoint.UnitMatrix(); 

        Matrix m = new Matrix(2,2);
        m.UnitMatrix();
        Vector v = new Vector(2);
        v.set(0,0.);
        v.set(1,1.);
        
        GblPointJna gblPointJna = new GblPointJna(jacPointToPoint);
        gblPointJna.addMeasurement(m,v,v);

        List<Integer> og_labels = new ArrayList<Integer>();
        og_labels.add(4711);
        og_labels.add(4712);
        Matrix og_ders = new Matrix(2,2);
        og_ders.UnitMatrix();
        gblPointJna.addGlobals(og_labels, og_ders);

        List<Integer> new_labels = new ArrayList<Integer>();
        Matrix new_ders = gblPointJna.getGlobalLabelsAndDerivatives(new_labels);

        assertTrue("New copy of labels from C is not the right size", new_labels.size() == og_labels.size());
        for (int i = 0; i < new_labels.size(); ++i) {
          assertTrue("New copy of labels from C do not have the right content", new_labels.get(i) == og_labels.get(i));
        }
    }

    @Test
    public void OpenCloseMilleBinary() {
        MilleBinaryJna mille = new MilleBinaryJna("hello_world.bin");
        mille.close();
    }

    @Test
    public void CreateAndDestroyGblTrajectoryNoSeed() {
        // Create a 5 x 5 unit matrix.  This will be passed to instantiate
        Matrix jacPointToPoint = new Matrix(5, 5); 
        jacPointToPoint.UnitMatrix(); 
        
        List<GblPointJna> points_on_traj = new ArrayList<GblPointJna>();
        for (int i = 0; i < 3; i++) {
            points_on_traj.add(new GblPointJna(jacPointToPoint));
        }

        GblTrajectoryJna traj = new GblTrajectoryJna(points_on_traj,1,1,1);
        System.out.printf("traj.isValid = %d\n", traj.isValid());
        traj.delete();

        // cleanup points
        for (int i = 0; i < points_on_traj.size(); ++i) {
            points_on_traj.get(i).delete();
        }
    }

    @Test
    public void CreateAndDestroyGblTrajectoryWithSeed() {
        // Create a 5 x 5 unit matrix.  This will be passed to instantiate
        Matrix jacPointToPoint = new Matrix(5, 5); 
        jacPointToPoint.UnitMatrix(); 
        
        List<GblPointJna> points_on_traj = new ArrayList<GblPointJna>();
        for (int i = 0; i < 3; i++) {
            points_on_traj.add(new GblPointJna(jacPointToPoint));
        }

        SymMatrix seedPrecision = new SymMatrix(5);
        GblTrajectoryJna traj = new GblTrajectoryJna(points_on_traj,1,seedPrecision,1,1,1);
        System.out.printf("traj.isValid = %d\n", traj.isValid());
        traj.delete();

        // cleanup points
        for (int i = 0; i < points_on_traj.size(); ++i) {
            points_on_traj.get(i).delete();
        }
    }

    // manually translated to java from C++ from GBL CPP exampleUtil source
    private Matrix gblSimpleJacobian(double ds, double cosl, double bfac) {
        Matrix jac = new Matrix(5,5);
        jac.UnitMatrix();
        jac.set(1, 0, -bfac * ds * cosl);
        jac.set(3, 0, -0.5 * bfac * ds * ds * cosl);
        jac.set(3, 1, ds);
        jac.set(4, 2, ds);
        return jac;
    }

    @Test
    public void GBLExample1_CreatePointsCreateTrajFitWrite() {
        int nTry = 3;
        int nLayer = 10;
        // track direction
        double sinLambda = 0.3;
        double cosLambda = Math.sqrt(1.0 - sinLambda * sinLambda);
        double sinPhi = 0.;
        double cosPhi = Math.sqrt(1.0 - sinPhi * sinPhi);
        // tDir = (cosLambda * cosPhi, cosLambda * sinPhi, sinLambda)
        // U = Z x T / |Z x T|, V = T x U
        Matrix uvDir = new Matrix(2, 3);
        uvDir.set(0, 0, -sinPhi);
        uvDir.set(0, 1, cosPhi);
        uvDir.set(0, 2, 0.);
        uvDir.set(1, 0, -sinLambda * cosPhi);
        uvDir.set(1, 1, -sinLambda * sinPhi);
        uvDir.set(1, 2, cosLambda);
        // measurement resolution
        Vector measErr = new Vector(2);
        measErr.set(0, 0.001);
        measErr.set(1, 0.001);
        Vector measPrec = new Vector(2); // (independent) precisions
        measPrec.set(0, 1.0 / (measErr.get(0) * measErr.get(0)));
        measPrec.set(1, 1.0 / (measErr.get(1) * measErr.get(1)));
        Matrix measInvCov = new Matrix(2, 2); // inverse covariance matrix
        //measInvCov.setZero();
        measInvCov.set(0, 0, measPrec.get(0));
        measInvCov.set(1, 1, measPrec.get(1));
        // scattering error
        Vector scatErr = new Vector(2);
        scatErr.set(0, 0.001);
        scatErr.set(1, 0.001);
        Vector scatPrec = new Vector(2); // (independent) precisions
        scatPrec.set(0, 1.0 / (scatErr.get(0) * scatErr.get(0)));
        scatPrec.set(1, 1.0 / (scatErr.get(1) * scatErr.get(1)));
        // (RMS of) CurviLinear track parameters (Q/P, slopes, offsets)
        Vector clErr = new Vector(5);
        clErr.set(0, 0.001);
        clErr.set(1, -0.1);
        clErr.set(2, 0.2);
        clErr.set(3, -0.15);
        clErr.set(4, 0.25);
        int seedLabel = 99999;
        // additional parameters
        Vector addPar = new Vector(2);
        addPar.set(0, 0.0025);
        addPar.set(1, -0.005);
        List<Integer> globalLabels = new ArrayList<Integer>();
        globalLabels.add(4711);
        globalLabels.add(4712);

        double bfac = 0.2998; // Bz*c for Bz=1
        double step = 1.5 / cosLambda; // constant steps in RPhi

        double Chi2Sum = 0.;
        int NdfSum = 0;
        double LostSum = 0.;
        int numFit = 0;

        MilleBinaryJna mille = new MilleBinaryJna("write_traj_test.bin");

        for (int iTry = 1; iTry <= nTry; ++iTry) {
            // curvilinear track parameters
            Vector clPar = new Vector(5);
            Matrix clCov = new Matrix(5,5);
            Matrix clSeed = new Matrix(5,5);
            //clCov.setZero();
            for (int i = 0; i < 5; ++i) {
                clPar.set(i, RandomGaussian.getGaussian(0.,clErr.get(i)));
                clCov.set(i, i, 1.0 * (clErr.get(i) * clErr.get(i)));
            }
            Matrix addDer = new Matrix(2,2);
            addDer.UnitMatrix();
            // arclength
            //double s = 0.;
            Matrix jacPointToPoint = new Matrix(5, 5);
            jacPointToPoint.UnitMatrix();
            // create list of points
            List<GblPointJna> listOfPoints = new ArrayList<GblPointJna>();
            for (int iLayer = 0; iLayer < nLayer; ++iLayer) {
                // measurement directions
                double sinStereo = (iLayer % 2 == 0) ? 0. : 0.1;
                double cosStereo = Math.sqrt(1.0 - sinStereo * sinStereo);
                Matrix mDirT = new Matrix(3, 2);
                //mDirT.setZero();
                mDirT.set(1, 0, cosStereo);
                mDirT.set(2, 0, sinStereo);
                mDirT.set(1, 1, -sinStereo);
                mDirT.set(2, 1, cosStereo);
                // projection measurement to local (curvilinear uv) directions (duv/dm)
                Matrix proM2l = uvDir.times(mDirT);
                // projection local (uv) to measurement directions (dm/duv)
                Matrix proL2m = proM2l.inverse();
                // point with (independent) measurements (in measurement system)
                GblPointJna pointMeas = new GblPointJna(jacPointToPoint);
                // measurement - prediction in measurement system with error
                Vector clParTail = new Vector(2);
                clParTail.set(0, clPar.get(3));
                clParTail.set(1, clPar.get(4)); 
                Vector meas = proL2m.times(clParTail);
                //MP
                meas = meas.plus(addDer.times(addPar)); // additional parameters
                for (int i = 0; i < 2; ++i) {
                    meas.set(i, RandomGaussian.getGaussian(meas.get(i), measErr.get(i)));
                }
                pointMeas.addMeasurement(proL2m, meas, measPrec);
    
                // additional local parameters?
                //pointMeas.addLocals(addDer);
                //MP
                pointMeas.addGlobals(globalLabels, addDer);
                addDer = addDer.times(-1.); // Der flips sign every measurement
                // add point to trajectory
                listOfPoints.add(pointMeas);
                int iLabel = listOfPoints.size();
                if (iLabel == seedLabel) {
                    clSeed = clCov.inverse();
                }
                // propagate to scatterer
                jacPointToPoint = gblSimpleJacobian(step, cosLambda, bfac);
                //jac2 = gblSimpleJacobian2(step, cosLambda, bfac);
                clPar = jacPointToPoint.times(clPar);
                clCov = jacPointToPoint.times(clCov).times(jacPointToPoint.transpose());
                //s += step;
                if (iLayer < nLayer - 1) {
                    Vector scat = new Vector(2);
                    //scat.Zero();
                    // point with scatterer
                    GblPointJna pointScat = new GblPointJna(jacPointToPoint);
                    pointScat.addScatterer(scat, scatPrec);
                    listOfPoints.add(pointScat);
                    iLabel = listOfPoints.size();
                    if (iLabel == seedLabel) {
                        clSeed = clCov.inverse();
                    }
                    // scatter a little
                    for (int i = 0; i < 2; ++i) {
                        clPar.set(i+1, RandomGaussian.getGaussian(clPar.get(i+1), scatErr.get(i)));
                        clCov.set(i+1, i+1, clCov.get(i+1, i+1) + scatErr.get(i)*scatErr.get(i));
                    }
                    // propagate to next measurement layer
                    clPar = jacPointToPoint.times(clPar);
                    clCov = jacPointToPoint.times(clCov).times(jacPointToPoint.transpose());
                    //s += step;
                }
            } // end of number of layers (iLayer)
            DoubleByReference Chi2 = new DoubleByReference();
            DoubleByReference lostWeight = new DoubleByReference();
            IntByReference Ndf = new IntByReference();

            // create trajectory without external seed
            GblTrajectoryJna traj = new GblTrajectoryJna(listOfPoints,1,1,1);
            // fit trajectory
            traj.fit(Chi2, Ndf, lostWeight, "");
            // write to MP binary file
            traj.milleOut(mille);
            Chi2Sum += Chi2.getValue();
            NdfSum += Ndf.getValue();
            LostSum += lostWeight.getValue();
            numFit++;
            traj.delete();

            // create trajectory with external seed
            traj = new GblTrajectoryJna(listOfPoints,seedLabel,clSeed,1,1,1);
            // fit trajectory
            traj.fit(Chi2, Ndf, lostWeight, "");
            // write to MP binary file
            traj.milleOut(mille);
            Chi2Sum += Chi2.getValue();
            NdfSum += Ndf.getValue();
            LostSum += lostWeight.getValue();
            numFit++;
            traj.delete();

            // cleanup points
            for (int i = 0; i < listOfPoints.size(); ++i) {
                listOfPoints.get(i).delete();
            }
        } // end of number of tries (iTry)

        System.out.printf("Chi2/ndf = %.2f over Nfit = %d\n", Chi2Sum / NdfSum, numFit);
        mille.close();
    }
}

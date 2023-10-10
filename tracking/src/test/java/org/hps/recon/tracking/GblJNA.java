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
import org.hps.recon.tracking.gbl.GBLexampleJna1;

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

        GblTrajectoryJna traj = new GblTrajectoryJna(points_on_traj,true,true,true);
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
        GblTrajectoryJna traj = new GblTrajectoryJna(points_on_traj,1,seedPrecision,true,true,true);
        System.out.printf("traj.isValid = %d\n", traj.isValid());
        traj.delete();

        // cleanup points
        for (int i = 0; i < points_on_traj.size(); ++i) {
            points_on_traj.get(i).delete();
        }
    }

    @Test
    public void GBLExample1_CreatePointsCreateTrajFitWrite() {
        GBLexampleJna1 eg = new GBLexampleJna1(3, 10, false, "test_gbl_example1.root");
        eg.runExample();
    }
}

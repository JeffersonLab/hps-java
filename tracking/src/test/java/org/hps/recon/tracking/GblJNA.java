package org.hps.recon.tracking;  

import org.junit.Test; 
import java.util.ArrayList;
import java.util.List;

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
    }
}

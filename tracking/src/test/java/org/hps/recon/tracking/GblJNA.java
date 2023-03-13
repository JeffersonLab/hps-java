package org.hps.recon.tracking;  

import org.junit.Test; 

import org.hps.recon.tracking.gbl.GblPointJna; 
import org.hps.recon.tracking.gbl.MilleBinaryJna; 
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
    public void CreateAndDestroyGblPoint() {
    }
}

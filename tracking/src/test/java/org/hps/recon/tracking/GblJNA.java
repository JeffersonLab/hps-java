package org.hps.recon.tracking;  

import org.junit.Test; 

import org.hps.recon.tracking.gbl.GblPointJna; 
import org.hps.recon.tracking.gbl.GblPoint; 
import org.hps.recon.tracking.gbl.matrix.Matrix; 
import org.hps.recon.tracking.gbl.matrix.Vector; 

public class GblJNA  { 

    @Test 
    public void loadGblPoint() {
        
        // Create a 5 x 5 unit matrix.  This will be passed to instantiate
        Matrix jacPointToPoint = new Matrix(5, 5); 
        jacPointToPoint.UnitMatrix(); 
        
        
        long startTime = System.nanoTime();
        for (int i=0; i < 100000; i++) {
            
            // Create an instance of the JNA GBL point.  
            GblPointJna gblPointJna = new GblPointJna(jacPointToPoint);  
        }
        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        System.out.printf("GblPointJna Time Elapsed %f ms \n", (double)duration/1000000.);
        
        Matrix m = new Matrix(2,2);
        m.UnitMatrix();
        Vector v = new Vector(2);
        v.set(0,0.);
        v.set(1,1.);
        
        
        
        startTime = System.nanoTime();
        GblPointJna gblPointJna = new GblPointJna(jacPointToPoint);  
        for (int i=0; i < 100000; i++) {
            gblPointJna.addMeasurement(m,v,v);
        }
        endTime = System.nanoTime();
        duration = endTime - startTime;
        System.out.printf("GblPointJna measurement Time Elapsed %f ms \n", (double)duration/1000000.);

        startTime = System.nanoTime();
        for (int i=0; i < 100000; i++) {
            
            // Create an instance of the JNA GBL point.  
            GblPoint gblPoint = new GblPoint(jacPointToPoint);  
        }
        endTime = System.nanoTime();
        duration = endTime - startTime;
        System.out.printf("GblPoint Time Elapsed %f ms \n", (double)duration/1000000.);
        
        startTime = System.nanoTime();
        GblPointJna gblPoint = new GblPointJna(jacPointToPoint);  
        for (int i=0; i < 100000; i++) {
            gblPoint.addMeasurement(m,v,v);
        }
        endTime = System.nanoTime();
        duration = endTime - startTime;
        System.out.printf("GblPoint measurement Time Elapsed %f ms \n", (double)duration/1000000.);


    } 
}

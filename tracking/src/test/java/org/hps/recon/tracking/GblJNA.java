package org.hps.recon.tracking;  

import org.junit.Test; 

import org.hps.recon.tracking.gbl.GblPointJna; 
import org.hps.recon.tracking.gbl.matrix.Matrix; 

public class GblJNA  { 

    @Test 
    public void loadGblPoint() {
        
        // Create a 5 x 5 unit matrix.  This will be passed to instantiate
        Matrix jacPointToPoint = new Matrix(5, 5); 
        jacPointToPoint.UnitMatrix(); 
        
        // Create an instance of the JNA GBL point.  
        GblPointJna gblPointJna = new GblPointJna(jacPointToPoint);  
        
    } 
}

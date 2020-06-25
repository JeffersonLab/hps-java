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
        
        // Create an instance of the JNA GBL point.  The points need to be 
        // created using the double[] representation.
        GblPointJna gblPointJna = new GblPointJna(jacPointToPoint.getRowPackedCopy());  

    } 
}

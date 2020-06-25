package org.hps.recon.tracking.gbl; 

import com.sun.jna.Library; 
import com.sun.jna.Native; 
import com.sun.jna.Pointer; 

public class GblPointJna { 

    public interface GblPointInterface extends Library { 
        
        GblPointInterface INSTANCE = (GblPointInterface) Native.loadLibrary("GBL", GblPointInterface.class); 
        
        
        Pointer GblPointCtor(double matrixArray[]); 
        Pointer GblPointCtor2D(double matrix[][]); 

        int GblPoint_hasMeasurement(Pointer self);
        double GblPoint_getMeasPrecMin(Pointer self);
    }
    
    private Pointer self; 

    public GblPointJna(double matrixArray[]) { 
        System.out.println("GblPointJna constructor");
        System.out.printf("%f %f %f %f %f %f \n", matrixArray[0], matrixArray[1], matrixArray[2], matrixArray[3], matrixArray[4], matrixArray[5]);
        self = GblPointInterface.INSTANCE.GblPointCtor(matrixArray); 
    }

    int hasMeasurement() {
        return GblPointInterface.INSTANCE.GblPoint_hasMeasurement(self);
    }

    double getMeasPrecMin() {
        return GblPointInterface.INSTANCE.GblPoint_getMeasPrecMin(self);
    }
        
    
    public GblPointJna(double matrix[][]) { 
        //do nothing for the moment
        //The 2d should be unpacked here.
        //self = GblPointInterface.INSTANCE.GblPointCtor(matrixArray); 
    }

    
}

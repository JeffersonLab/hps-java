package org.hps.recon.tracking.gbl; 

import com.sun.jna.Library; 
import com.sun.jna.Native; 
import com.sun.jna.Pointer; 

public class GblPointJna { 

    public interface GblPointInterface extends Library { 

        GblPointInterface INSTANCE = (GblPointInterface) Native.loadLibrary("GBL", GblPointInterface.class); 

        Pointer GblPointCtor(double matrix[][]); 
    }

    private Pointer self; 

    public GblPointJna(double matrix[][]) { 
        self = GblPointInterface.INSTANCE.GblPointCtor(matrix); 
    }
}

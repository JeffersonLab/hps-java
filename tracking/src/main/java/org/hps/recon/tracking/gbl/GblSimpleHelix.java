package org.hps.recon.tracking.gbl;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

public class GblSimpleHelix {
    
    public interface GblSimpleHelixInterface extends Library {
        
        GblSimpleHelixInterface INSTANCE = (GblSimpleHelixInterface) Native.loadLibrary("GBL", GblSimpleHelixInterface.class);

        Pointer GblSimpleHelixCtor(double aRinv, double aPhi0, double aDca, double aDzds, double aZ0);
        void GblSimpleHelix_delete(Pointer self);
        double GblSimpleHelix_getPhi(Pointer self, double aRadius);
        double GblSimpleHelix_getArcLengthR(Pointer self, double aRadius);
        double GblSimpleHelix_getArcLengthXY(Pointer self, double xPos, double yPos);
        void GblSimpleHelix_moveToXY(Pointer self, double xPos, double yPos, double[] newPhi0, double[] newDca, double[] newZ0);
        Pointer GblSimpleHelix_getPrediction(Pointer self, double [] refPos, double [] uDir, double[] vDir);
    }

    private Pointer self;

    public GblSimpleHelix(double aRinv, double aPhi0, double aDca, double aDzds, double aZ0) {
        self = GblSimpleHelixInterface.INSTANCE.GblSimpleHelixCtor(aRinv, aPhi0, aDca, aDzds, aZ0);
    }

    public void delete() {
        GblSimpleHelixInterface.INSTANCE.GblSimpleHelix_delete(self);
    }
    
    public double getPhi(double aRadius) {
        return GblSimpleHelixInterface.INSTANCE.GblSimpleHelix_getPhi(self, aRadius);
    }

    public double getArcLengthR(double aRadius) {
        return GblSimpleHelixInterface.INSTANCE.GblSimpleHelix_getArcLengthR(self, aRadius);
    }

    public double getArcLengthXY(double xPos, double yPos) {
        return GblSimpleHelixInterface.INSTANCE.GblSimpleHelix_getArcLengthXY(self, xPos, yPos);
    }
    
    public void moveToXY(double xPos, double yPos, 
                    double [] newPhi0, double [] newDca, double [] newZ0) {
        GblSimpleHelixInterface.INSTANCE.GblSimpleHelix_moveToXY(self, xPos, yPos, newPhi0, newDca, newZ0);
        
    }
    
    GblHelixPrediction getPrediction(double [] refPos, double [] uDir, double[] vDir) {

        GblHelixPrediction prediction = new GblHelixPrediction(GblSimpleHelixInterface.INSTANCE.GblSimpleHelix_getPrediction(self, refPos, uDir, vDir));
        return prediction;
    }
}

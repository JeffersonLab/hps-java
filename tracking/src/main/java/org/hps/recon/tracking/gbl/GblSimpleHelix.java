package org.hps.recon.tracking.gbl;

import com.sun.jna.Pointer;

/**
 * wrapper class around GblSimpleHelix JNA functions
 * <p>
 * Allows access to the GBL C++ simple helix class which
 * can follow a ideal helix through space allowing for
 * simulating a "perfect" track in examples and comparison
 * of our tracks to a "perfect" helix.
 */
public class GblSimpleHelix {

    private Pointer self;

    public GblSimpleHelix(double aRinv, double aPhi0, double aDca, double aDzds, double aZ0) {
        self = GblInterface.INSTANCE.GblSimpleHelixCtor(aRinv, aPhi0, aDca, aDzds, aZ0);
    }

    public void delete() {
        GblInterface.INSTANCE.GblSimpleHelix_delete(self);
    }

    public double getPhi(double aRadius) {
        return GblInterface.INSTANCE.GblSimpleHelix_getPhi(self, aRadius);
    }

    public double getArcLengthR(double aRadius) {
        return GblInterface.INSTANCE.GblSimpleHelix_getArcLengthR(self, aRadius);
    }

    public double getArcLengthXY(double xPos, double yPos) {
        return GblInterface.INSTANCE.GblSimpleHelix_getArcLengthXY(self, xPos, yPos);
    }

    public void moveToXY(double xPos, double yPos, double[] newPhi0, double[] newDca, double[] newZ0) {
        GblInterface.INSTANCE.GblSimpleHelix_moveToXY(self, xPos, yPos, newPhi0, newDca, newZ0);
    }

    GblHelixPrediction getPrediction(double[] refPos, double[] uDir, double[] vDir) {
        GblHelixPrediction prediction = new GblHelixPrediction(GblInterface.INSTANCE.GblSimpleHelix_getPrediction(self, refPos, uDir, vDir));
        return prediction;
    }
}

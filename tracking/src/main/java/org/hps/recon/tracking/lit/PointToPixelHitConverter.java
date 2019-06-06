package org.hps.recon.tracking.lit;

import java.util.Random;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class PointToPixelHitConverter {

    double _sigmaX;
    double _sigmaY;
    double _sigmaXY;
    Random _ranX = new Random();
    Random _ranY = new Random();

    public PointToPixelHitConverter(double sigmaX, double sigmaY, double sigmaXY) {
        _sigmaX = sigmaX;
        _sigmaY = sigmaY;
        _sigmaXY = sigmaXY;
        //TODO use a real multivariate gaussian generator e.g.
        //org.apache.commons.math3.distribution.MultivariateNormalDistribution;
    }

    public CbmLitPixelHit generateHit(double x, double y, double z) {
        CbmLitPixelHit hit = new CbmLitPixelHit();
        hit.SetDx(_sigmaX);
        hit.SetDxy(_sigmaXY);
        hit.SetDy(_sigmaY);
        hit.SetDz(0.0001);
        // smear x and y independently...
        hit.SetX(x + _sigmaX * _ranX.nextGaussian());
        hit.SetY(y + _sigmaY * _ranY.nextGaussian());
        hit.SetZ(z);
        return hit;
    }

    public CbmLitPixelHit createHit(double x, double y, double z) {
        CbmLitPixelHit hit = new CbmLitPixelHit();
        hit.SetDx(_sigmaX);
        hit.SetDxy(_sigmaXY);
        hit.SetDy(_sigmaY);
        hit.SetDz(0.0001);
        hit.SetX(x);
        hit.SetY(y);
        hit.SetZ(z);
        return hit;
    }
}

package org.hps.recon.tracking.lit;

import java.util.Random;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class TrackParameterSmearer {

    double _sigmaX;
    double _sigmaY;
    double _sigmaTx;
    double _sigmaTy;
    double _sigmaQp;
    //TODO use a real multivariate random generator
    //e.g.//org.apache.commons.math3.distribution.MultivariateNormalDistribution;
    Random _ran = new Random();

    public TrackParameterSmearer(double sigmaX, double sigmaY,
            double sigmaTx, double sigmaTy, double sigmaQp) {
        _sigmaX = sigmaX;
        _sigmaY = sigmaY;
        _sigmaTx = sigmaTx;
        _sigmaTy = sigmaTy;
        _sigmaQp = sigmaQp;

    }

    public void SmearTrackParameters(CbmLitTrackParam trkParam) {
        trkParam.SetX(trkParam.GetX() + _sigmaX * _ran.nextGaussian());
        trkParam.SetY(trkParam.GetY() + _sigmaY * _ran.nextGaussian());
        trkParam.SetTx(trkParam.GetTx() + _sigmaTx * _ran.nextGaussian());
        trkParam.SetTy(trkParam.GetTy() + _sigmaTy * _ran.nextGaussian());
        trkParam.SetQp(trkParam.GetQp() + _sigmaQp * _ran.nextGaussian());
        // also set the diagonal elements of the covariance matrix to a large number...
        trkParam.SetCovariance(0, 9999.);
        trkParam.SetCovariance(5, 9999.);
        trkParam.SetCovariance(9, 9999.);
        trkParam.SetCovariance(12, 9999.);
        trkParam.SetCovariance(14, 9999.);
    }
}

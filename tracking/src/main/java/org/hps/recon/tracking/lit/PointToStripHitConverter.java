package org.hps.recon.tracking.lit;

import static java.lang.Math.sin;
import static java.lang.Math.cos;
import java.util.Random;

/**
 * A class to convert 2D space points in the plane to a 1D measurement u
 * characteristic of a strip detector Note that this convention measures phi
 * from the vertical.
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class PointToStripHitConverter {

    double _phi;
    double _sinPhi;
    double _cosPhi;
    double _sigmaU;
    Random _ranU = new Random();

    public PointToStripHitConverter(double phi, double sigmaU) {
        _phi = phi;
        _sigmaU = sigmaU;
        _sinPhi = sin(_phi);
        _cosPhi = cos(_phi);
    }

    public CbmLitStripHit generateHit(double x, double y, double z) {
        CbmLitStripHit hit = new CbmLitStripHit();
        double u = _cosPhi * x + _sinPhi * y;
        double hitU = u + _sigmaU * _ranU.nextGaussian();
        hit.SetPhi(_phi);
        hit.SetU(hitU);
        hit.SetDu(_sigmaU);
        hit.SetZ(z);
        hit.SetDz(.0001);
        // TODO add system ID for this station
        return hit;
    }

    public CbmLitDetPlaneStripHit generateHit(double x, double y, DetectorPlane p) {
        CbmLitDetPlaneStripHit hit = new CbmLitDetPlaneStripHit(p);
        double u = _cosPhi * x + _sinPhi * y;
        double hitU = u + _sigmaU * _ranU.nextGaussian();
        hit.SetPhi(_phi);
        hit.SetU(hitU);
        hit.SetDu(_sigmaU);
        hit.SetDz(.0001);
        // TODO add system ID for this station
        return hit;
    }

    public double phi() {
        return _phi;
    }

    public double cosPhi() {
        return _cosPhi;
    }

    public double sinPhi() {
        return _sinPhi;
    }
}

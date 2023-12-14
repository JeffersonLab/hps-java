package org.hps.recon.tracking.gbl;

import hep.physics.vec.Hep3Vector;
import org.hps.recon.tracking.gbl.matrix.Vector;
import org.hps.recon.tracking.gbl.matrix.Matrix;

public class GBLBeamSpotPoint {

    // BeamSpot point that holds:
    // Location
    // residual
    // precision
    // Matrix m2c
    // arcLength (2d)
    // Track directions (lambda/phi) at the point

    public Hep3Vector _location;
    public Vector _aResidual;
    public Vector _aPrecision;
    public Matrix _projL2m;
    public double _arcLength; // 2D
    public double _tanLambda;
    public double _phi;

    public GBLBeamSpotPoint(Hep3Vector location, Vector aResidual, Vector aPrecision, Matrix projL2m, double arcLength, double tanLambda, double phi) {

        _location = location;
        _aResidual = aResidual;
        _aPrecision = aPrecision;
        _projL2m = projL2m;
        _arcLength = arcLength;
        _tanLambda = tanLambda;
        _phi = phi;
    }

}

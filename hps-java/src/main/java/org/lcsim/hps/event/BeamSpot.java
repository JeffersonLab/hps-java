/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lcsim.hps.event;

import hep.physics.matrix.SymmetricMatrix;

/**
 * Class describing the HPS beamspot at the target (Z=0)
 * @author mgraham
 * created on 6/27/2011
 */
public class BeamSpot {

    private static double[] _position;
    private static double[] _angle;
    private static SymmetricMatrix _beamcov;

    public BeamSpot() {
    }

    public BeamSpot(double[] pos, SymmetricMatrix cov, double[] angle) {
        _position = pos;
        _beamcov = cov;
        _angle = angle;
    }

    public BeamSpot(double sigX, double sigY) {
        //  default beamspot position
        _position[0] = 0;
        _position[1] = 0;
        _angle[0] = 0;
        _angle[1] = 0;
        _beamcov = new SymmetricMatrix(2);
        _beamcov.setElement(0, 0, sigX * sigX);
        _beamcov.setElement(1, 1, sigY * sigY);
    }

    public BeamSpot(double posX, double sigX, double posY, double sigY) {
        _position[0] = posX;
        _position[1] = posY;
        _angle[0] = 0;
        _angle[1] = 0;
        _beamcov = new SymmetricMatrix(2);
        _beamcov.setElement(0, 0, sigX * sigX);
        _beamcov.setElement(1, 1, sigY * sigY);
    }

    public double getBeamSigmaX() {
        return Math.sqrt(_beamcov.e(0, 0));
    }

    public double getBeamSigmaY() {
        return Math.sqrt(_beamcov.e(1, 1));
    }

    public double[] getBeamPosition() {
        return _position;
    }

    public double[] getBeamAngle() {
        return _angle;
    }

    public SymmetricMatrix getBeamCovariance() {
        return _beamcov;
    }  

    public void setBeamPosition(double[] pos) {
        _position = pos;
    }

    public void setBeamAngle(double[] ang) {
        _angle = ang;
    }

    public void setBeamCovariance(SymmetricMatrix cov) {
        _beamcov = cov;
    }
}

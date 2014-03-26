package org.hps.recon.tracking;

import hep.physics.matrix.SymmetricMatrix;

/**
 * Encapsulate position, direction, and covariance matrix for a straight-line track
 *
 * @author Richard Partridge
 */
public class StraightLineTrack {

    public static int y0Index = 0;
    public static int z0Index = 1;
    public static int dydxIndex = 2;
    public static int dzdxIndex = 3;
    private double _x0;
    private double _y0;
    private double _z0;
    private double _dydx;
    private double _dzdx;
    private SymmetricMatrix _cov;
    private double[] _poca = {0, 0, 0};
    private double[] _yzT = {0, 0};

    /**
     * Fully qualified constructor for the StraightLineTrack class.  A StraightLineTrack
     * is specified by providing a position and the direction derivatives dydx and dzdx used
     * in the vertex fitting.  It is assumed that the track is traveling in the +x direction
     * (i.e., from the field-free region into the magnetized region).
     *
     * @param x0 x coordinate of the reference plane
     * @param y0 y coordinate at the reference plane
     * @param z0 z coordinate at the reference plane
     * @param dydx dy/dx for the track
     * @param dzdx dz/dx for the track
     * @param cov covariance matrix for the track parameters (y0, z0, dy/dx, and dz/dx)
     */
    public StraightLineTrack(double x0, double y0, double z0, double dydx, double dzdx, SymmetricMatrix cov) {
        _x0 = x0;
        _y0 = y0;
        _z0 = z0;
        _dydx = dydx;
        _dzdx = dzdx;
        _cov = cov;
        calculatePoca();
        calculateTargetYZ();
    }

    /**
     * Return the x coordinate of the reference plane.
     *
     * @return x coordinate of the reference plane
     */
    public double x0() {
        return _x0;
    }

    /**
     * Return the y coordinate at the reference plane.
     *
     * @return y coordinate
     */
    public double y0() {
        return _y0;
    }

    /**
     * Return the z coordinate at the reference plane.
     *
     * @return z coordinate
     */
    public double z0() {
        return _z0;
    }

    /**
     * Return the direction derivative dy/dx.
     *
     * @return dy/dx
     */
    public double dydx() {
        return _dydx;
    }

    /**
     * Return the direction derivative dz/dx.
     *
     * @return dz/dx
     */
    public double dzdx() {
        return _dzdx;
    }

    /**
     * Return the xPoca .
     *
     * @return xPoca
     */
    public double xPoca() {
        return _poca[0];
    }

    /**
     * Return the yPoca .
     *
     * @return yPoca
     */
    public double yPoca() {
        return _poca[1];
    }

    /**
     * Return the zPoca .
     *
     * @return zPoca
     */
    public double zPoca() {
        return _poca[2];
    }

    /**
     * Return the Doca .
     *
     * @return Doca
     */
    public double Doca() {
        return Math.sqrt(_poca[1] * _poca[1] + _poca[2] * _poca[2]);
    }

    /**
     * Return the Poca .
     *
     * @return Poca
     */
    public double[] Poca() {
        return _poca;
    }

    /**
     * Return the Y and Z positions of the track at X=0 (target).
     *
     * @return yzT
     */
    public double[] TargetYZ() {
        return _yzT;
    }
    
    public double[] getYZAtX(double xVal){
        return calculateYZAtX(xVal);
    }   
    
    /**
     * Return the covariance matrix.
     *
     * @return covariance matrix
     */
    public SymmetricMatrix cov() {
        return _cov;
    }

    //  mg--for now just calculate the simple POCA (to the x-axis)...no errors
    private void calculatePoca() {
        _poca[0] = _x0-(_y0 * _dydx + _z0 * _dzdx) / (_dydx * _dydx + _dzdx * _dzdx);
        _poca[1] = _y0 + _dydx * (_poca[0]-_x0);
        _poca[2] = _z0 + _dzdx * (_poca[0]-_x0);
    }

    private void calculateTargetYZ() {
        _yzT[0] = _y0 - _x0 * _dydx;
        _yzT[1] = _z0 - _x0 * _dzdx;
    }
    
    private double[] calculateYZAtX(double xVal) {
        double[] yzAtX={-66,-66};
        if(xVal>_x0)   //_x0 is where the field region starts...if xVal is bigger than this, need to get position on helix
            return yzAtX;
        yzAtX[0] = _y0 + (xVal-_x0) * _dydx;
        yzAtX[1] = _z0 + (xVal-_x0) * _dzdx;
        return yzAtX;
    }
    
    public double calculateXAtZEquals0() {       
        return _x0-_z0/_dzdx;
    }
    
    public double[] calculateXYAtZ(double zVal) {       
        double[] xyAtZ = {-99999,-99999};
        xyAtZ[0] = (zVal-_z0)/(_dzdx)+_x0;
        xyAtZ[1] = this.calculateYZAtX(xyAtZ[0])[0];
        return xyAtZ;
    }
    
}

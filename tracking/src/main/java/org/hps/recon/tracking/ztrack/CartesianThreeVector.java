package org.hps.recon.tracking.ztrack;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import static java.lang.Math.sqrt;
import java.util.Arrays;

/**
 * A Cartesian vector in 3D space
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class CartesianThreeVector {

    private double[] _pos = new double[3];
    double[] _tmp = new double[3];

    public CartesianThreeVector() {
    }

    public CartesianThreeVector(Hep3Vector v) {
        _pos[0] = v.x();
        _pos[1] = v.y();
        _pos[2] = v.z();
    }

    public CartesianThreeVector(CartesianThreeVector in) {
        _pos[0] = in._pos[0];
        _pos[1] = in._pos[1];
        _pos[2] = in._pos[2];
    }

    public CartesianThreeVector(double x, double y, double z) {
        _pos[0] = x;
        _pos[1] = y;
        _pos[2] = z;
    }

    public CartesianThreeVector(double[] pos) {
        System.arraycopy(pos, 0, _pos, 0, 3);
    }

    public double[] vector() {
        return Arrays.copyOf(_pos, 3);
    }

    public double x() {
        return _pos[0];
    }

    public double y() {
        return _pos[1];
    }

    public double z() {
        return _pos[2];
    }

    // multiply by constant
    public CartesianThreeVector times(double a) {
        for (int i = 0; i < 3; ++i) {
            _tmp[i] = _pos[i] * a;
        }
        return new CartesianThreeVector(_tmp);
    }

    // multiply by constant in place
    public void timesEquals(double a) {
        for (int i = 0; i < 3; ++i) {
            _pos[i] = _pos[i] * a;
        }

    }

    // addition
    public CartesianThreeVector plus(CartesianThreeVector v) {
        for (int i = 0; i < 3; ++i) {
            _tmp[i] = _pos[i] + v._pos[i];
        }
        return new CartesianThreeVector(_tmp);
    }

    // addition in place
    public void plusEquals(CartesianThreeVector v) {
        for (int i = 0; i < 3; ++i) {
            _pos[i] = _pos[i] + v._pos[i];
        }
    }

    // subtraction
    public CartesianThreeVector minus(CartesianThreeVector v) {
        for (int i = 0; i < 3; ++i) {
            _tmp[i] = _pos[i] - v._pos[i];
        }
        return new CartesianThreeVector(_tmp);
    }

    // subtraction in place
    public void minusEquals(CartesianThreeVector v) {
        for (int i = 0; i < 3; ++i) {
            _pos[i] = _pos[i] - v._pos[i];
        }
    }

    // dot product
    public double dot(CartesianThreeVector v) {
        double dot = 0.;
        for (int i = 0; i < 3; ++i) {
            dot += _pos[i] * v._pos[i];
        }
        return dot;
    }

    // cross product
    public CartesianThreeVector cross(CartesianThreeVector v) {
        _tmp[0] = (_pos[1] * v._pos[2]) - (v._pos[1] * _pos[2]);
        _tmp[1] = (_pos[2] * v._pos[0]) - (v._pos[2] * _pos[0]);
        _tmp[2] = (_pos[0] * v._pos[1]) - (v._pos[0] * _pos[1]);
        return new CartesianThreeVector(_tmp);
    }

    public CartesianThreeVector unitVector() {
        double d = magnitude();
        for (int i = 0; i < 3; ++i) {
            _tmp[i] = _pos[i] / d;
        }
        return new CartesianThreeVector(_tmp);
    }

    public void unitize() {
        double d = magnitude();
        for (int i = 0; i < 3; ++i) {
            _pos[i] = _pos[i] / d;
        }
    }

    public double magnitude() {
        double mag = 0.;
        for (int i = 0; i < 3; ++i) {
            mag += _pos[i] * _pos[i];
        }
        return sqrt(mag);
    }

    public void setXYZ(double x, double y, double z) {
        _pos[0] = x;
        _pos[1] = y;
        _pos[2] = z;
    }

    public void setMag(double mag) {
        double factor = mag / magnitude();
        _pos[0] = _pos[0] * factor;
        _pos[1] = _pos[1] * factor;
        _pos[2] = _pos[2] * factor;

    }

    public Hep3Vector hep3Vector() {
        return new BasicHep3Vector(x(), y(), z());
    }

    public String toString() {
        return " CartesianThreeVector: " + _pos[0] + " " + _pos[1] + " " + _pos[2] + " mag: " + magnitude() + "\n";
    }

}

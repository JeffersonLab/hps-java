package org.hps.recon.tracking.lit;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import static java.lang.Math.abs;
import static java.lang.Math.atan2;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.solids.GeomOp3D;
import org.lcsim.detector.solids.Line3D;
import org.lcsim.detector.solids.Point3D;

/**
 * A class to encapsulate the behavior of a planar detector
 *
 * @author Norman Graf
 */
public class DetectorPlane implements Comparable {

    private CartesianThreeVector _position;
    private CartesianThreeVector _normalVector;
    private ITransform3D _localToGlobal;
    private ITransform3D _globalToLocal;
    private String _name;
    private double _radLengths;
    private Hep3Vector _measDir; // the direction along which measurements are made
    private double _measuredDimension = 9999.; // locally x (transverse to strips)
    Hep3Vector _unmeasDir; // the direction along the strips
    private double _unMeasuredDimension = 9999.; // locally y (along strips)
    private double _phi; // effective stereo angle in global coordinates
    private double _zmin; //minimum z of bounding box
    private double _zmax; // maximum z of bounding box

    Line3D zAxis = new Line3D(new Point3D(0., 0., 0.), new BasicHep3Vector(0., 0., 1.));

    DetectorPlane() {

    }

    public DetectorPlane(String name, CartesianThreeVector pos, CartesianThreeVector normal, double x0, double phi) {
        _name = name;
        _position = pos;
        _normalVector = normal;
        _radLengths = x0;
        _phi = phi;
        _zmin = pos.z();
        _zmax = pos.z();

        _localToGlobal = new Transform3D();
        _globalToLocal = new Transform3D();
    }

    public DetectorPlane(String name, CartesianThreeVector pos, CartesianThreeVector normal, ITransform3D l2g, ITransform3D g2l, double x0) {
        _name = name;
        _position = pos;
        _normalVector = normal;
        _globalToLocal = g2l;
        _localToGlobal = l2g;
        _radLengths = x0;
        _zmin = pos.z();
        _zmax = pos.z();
    }

    public DetectorPlane(String name, CartesianThreeVector pos, CartesianThreeVector normal, ITransform3D l2g, ITransform3D g2l, double x0, Hep3Vector unmeasDir, double x, Hep3Vector measDir, double y) {
        _name = name;
        _position = pos;
        _normalVector = normal;
        _globalToLocal = g2l;
        _localToGlobal = l2g;
        _radLengths = x0;
        _unmeasDir = unmeasDir;
        _unMeasuredDimension = x;
        _measDir = measDir;
        _measuredDimension = y;
        // Phi is measured from the vertical
        _phi = atan2(_measDir.y(), _measDir.x());
        //System.out.println("DetectorPlane " + name + " phi " + _phi);
        _zmin = pos.z();
        _zmax = pos.z();
    }

    public void setMeasuredDimension(double d) {
        _measuredDimension = d;
    }

    public double getMeasuredDimension() {
        return _measuredDimension;
    }

    public void setUnMeasuredDimension(double d) {
        _unMeasuredDimension = d;
    }

    public double getUnMeasuredDimension() {
        return _unMeasuredDimension;
    }

    public void setGlobalToLocal(ITransform3D xform) {
        _globalToLocal = xform;
    }

    public void setLocalToGlobal(ITransform3D xform) {
        _localToGlobal = xform;
    }

    public void SetZpos(double z) {
        _position = new CartesianThreeVector(0., 0., z);
    }

    public void setZmin(double z) {
        _zmin = z;
    }

    public void setZmax(double z) {
        _zmax = z;
    }

    public double getZmin() {
        return _zmin;
    }

    public double getZmax() {
        return _zmax;
    }

    public double phi() {
        return _phi;
    }

    public double radLengths() {
        return _radLengths;

    }

    public CartesianThreeVector position() {
        return _position;
    }

    public double GetZpos() {
        return _position.z();
    }

    public CartesianThreeVector normal() {
        return _normalVector;
    }

    public Hep3Vector toLocal(Hep3Vector global) {
        return _globalToLocal.transformed(global);
    }

    public boolean inBounds(double x, double y, double z) {
        //TODO resolve remaining issue with origin not being equal to localToGlobal((0.,0.,0.))
        //Note that the local coordinates have x as the measured coordinate, not y!
        Hep3Vector local = _globalToLocal.transformed(new BasicHep3Vector(x, y, z));
        //
        Hep3Vector origin = _localToGlobal.transformed(new BasicHep3Vector(0., 0., 0.));
//        System.out.println(_name + " o: " + origin + " :global " + x + " " + y + " " + z + " local:" + local);
        //
        if (abs(local.x()) > _measuredDimension / 2.) {
            return false;
        }
        if (abs(local.y()) > _unMeasuredDimension / 2.) {
            return false;
        }
// TODO check on z 
//        if(abs(local.z())>.001) return false;
        return true;
    }

    public SymmetricMatrix toLocal(SymmetricMatrix global) {
        return _globalToLocal.transformed(global);
    }

    public Hep3Vector toGlobal(Hep3Vector local) {
        return _localToGlobal.transformed(local);
    }

    public SymmetricMatrix toGlobal(SymmetricMatrix local) {
        return _globalToLocal.transformed(local);
    }

    public String name() {
        return _name;
    }

    public double u(CartesianThreeVector globalPos) {
        return u(new BasicHep3Vector(globalPos.x(), globalPos.y(), globalPos.z()));
    }

    public double u(double x, double y, double z) {
        return u(new BasicHep3Vector(x, y, z));
    }

    public double u(Hep3Vector globalPos) {
        // note that the local measurement coordinate is x!
        Hep3Vector localPos = _globalToLocal.transformed(globalPos);
        return localPos.x();
    }

    public double globalU(Hep3Vector globalPos) {
        Line3D strip = new Line3D(new Point3D(globalPos), _unmeasDir);
        return GeomOp3D.distanceBetween(zAxis, strip);
    }

    /*
     * @return String representation of the class
     */
    public String toString() {
        StringBuffer ss = new StringBuffer();
        ss.append("DetectorPlane: " + _name + " phi " + _phi + " " + _measuredDimension + " by " + _unMeasuredDimension + " z=" + position().z() + "\n");
        ss.append("               zmin " + _zmin + " zmax " + _zmax);
        return ss.toString();
    }

    public int compareTo(Object o) {
        if (o == this) {
            return 0;
        }
        DetectorPlane that = (DetectorPlane) o;
        if (this.position().z() < that.position().z()) {
            return -1;
        }
        if (this.position().z() > that.position().z()) {
            return 1;
        }
        return 0;
    }
}

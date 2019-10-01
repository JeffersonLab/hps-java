package org.hps.analysis.alignment.straighttrack;

import Jama.Matrix;
import hep.physics.vec.Hep3Vector;
import java.util.Arrays;

/**
 *
 * @author Norman Graf
 */
public class DetectorPlane {

    private int _id;
    private Matrix _rotMat; // rotation matrices local --> global
    private double[] _rot = new double[9]; // rotation matrix local -> global
    private double[] _r0 = new double[3]; //detector origin in global coordinates (mm)
    private double[] _sigs = new double[2]; //detector resolutions in v and w (mm)
    private Offset _offset; // possible offsets in position and rotation

    private Hep3Vector _u; // measured direction (~y)
    private Hep3Vector _v; // unmeasured direction (~x)
    private Hep3Vector _w; // normal to the plane (~z)
    private Hep3Vector _r; // point on the plane

    public DetectorPlane(int id, Matrix m, double[] pos, double[] s) {
        this(id, m, pos, s, null);
        addOffset();
    }

    public DetectorPlane(int id, double[] rot, double[] pos, double[] s) {
        _id = id;
        System.arraycopy(rot, 0, _rot, 0, 9);
        System.arraycopy(pos, 0, _r0, 0, 9);
        System.arraycopy(s, 0, _sigs, 0, 2);
        addOffset();
    }

    public DetectorPlane(int id, Matrix m, double[] pos, double[] s, Offset off) {
        _id = id;
        _rotMat = m;
        _r0 = pos;
        _sigs = s;
        _offset = off;
    }

    public DetectorPlane(DetectorPlane p, Offset off) {
        _id = p.id();
        _rotMat = p.rot();
        _r0 = p.r0();
        _sigs = p.sigs();
        _offset = off;
    }

    public double[] rotArray() {
        return _rotMat.getRowPackedCopy();//getColumnPackedCopy();
    }

    public Matrix rot() {
        return _rotMat;
    }

    public int id() {
        return _id;
    }

    public double[] r0() {
        return _r0;
    }

    public double[] sigs() {
        return _sigs;
    }

    public Offset offset() {
        return _offset;
    }

    public void addOffset(Offset o) {
        _offset = o;
    }

    public void setUpdatedRotation(double[] rot) {
//        System.out.println("old rotation matrix " + Arrays.toString(_rot));
        System.arraycopy(rot, 0, _rot, 0, 9);
//        System.out.println("new rotation matrix " + Arrays.toString(_rot));
//        System.out.println("old rotation matrix " + _id );
        _rotMat.print(6, 4);
        // update rotMat
//        _rotMat.set(0, 0, rot[0]);
//        _rotMat.set(1, 0, rot[1]);
//        _rotMat.set(2, 0, rot[2]);
//        _rotMat.set(0, 1, rot[3]);
//        _rotMat.set(1, 1, rot[4]);
//        _rotMat.set(2, 1, rot[5]);
//        _rotMat.set(0, 2, rot[6]);
//        _rotMat.set(1, 2, rot[7]);
//        _rotMat.set(2, 2, rot[8]);
        _rotMat.set(0, 0, rot[0]);
        _rotMat.set(0, 1, rot[1]);
        _rotMat.set(0, 2, rot[2]);
        _rotMat.set(1, 0, rot[3]);
        _rotMat.set(1, 1, rot[4]);
        _rotMat.set(1, 2, rot[5]);
        _rotMat.set(2, 0, rot[6]);
        _rotMat.set(2, 1, rot[7]);
        _rotMat.set(2, 2, rot[8]);
//        System.out.println("new rotation matrix");
//        _rotMat.print(6, 4);
    }

    public void setUpdatedPosition(double[] r0) {
//        System.out.println("updating DetectorPlane " + _id + " position");
//        System.out.println("old position " + Arrays.toString(_r0));
        System.arraycopy(r0, 0, _r0, 0, 3);
//        System.out.println("new position " + Arrays.toString(_r0));
    }

    public void setUVWR(Hep3Vector u, Hep3Vector v, Hep3Vector w, Hep3Vector V0) {
        _u = u;
        _v = v;
        _w = w;
        _r = V0;
    }

    public Hep3Vector u() {
        return _u;
    }

    public Hep3Vector v() {
        return _v;
    }

    public Hep3Vector normal() {
        return _w;
    }

    public Hep3Vector origin() {
        return _r;
    }

    private void addOffset() {
        double[] a = new double[3];
        int[] MASK = new int[6];
        // arguments are:
        // id
        // rotation matrix local --> global
        // detector origin in global coordinates (mm)
        // smeared angles (tilts)
        // smeared offsets
        // whether to try to align this parameter
        // 0-2 offset in location (x,y,z)
        // 3-5 offset in orientation (tilt) rotations around (x,y,z)
        _offset = new Offset(_id, _rotMat.getRowPackedCopy(), _r0, a, a, MASK);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer("DetectorPlane " + _id + " : ");
        sb.append("rot : " + Arrays.toString(_rotMat.getRowPackedCopy()));
        sb.append(" r0  : " + Arrays.toString(_r0));
        sb.append(" sigs: " + Arrays.toString(_sigs));
        return sb.toString();
    }
}

package org.hps.analysis.alignment.straighttrack;

import Jama.Matrix;
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

    public DetectorPlane(int id, Matrix m, double[] pos, double[] s) {
        this(id, m, pos, s, null);
    }

    public DetectorPlane(int id, double[] rot, double[] pos, double[] s) {
        _id = id;
        System.arraycopy(rot, 0, _rot, 0, 9);
        System.arraycopy(pos, 0, _r0, 0, 9);
        System.arraycopy(s, 0, _sigs, 0, 2);
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

    public String toString() {
        StringBuffer sb = new StringBuffer("DetectorPlane "+_id+" : ");
        sb.append("rot : " + Arrays.toString(_rotMat.getRowPackedCopy()));
        sb.append(" r0  : " + Arrays.toString(_r0));
        sb.append(" sigs: " + Arrays.toString(_sigs));
        return sb.toString();
    }
}

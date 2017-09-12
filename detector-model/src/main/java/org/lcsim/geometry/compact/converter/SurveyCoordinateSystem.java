package org.lcsim.geometry.compact.converter;

import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.Rotation3D;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.Translation3D;

/**
 * Class describing a simple coordinate system used to define the {@link SurveyVolume}.
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 */
public class SurveyCoordinateSystem {

    private final boolean debug = false;
    private Hep3Vector origin;
    private Hep3Vector u;
    private Hep3Vector v;
    private Hep3Vector w;

    // public SurveyCoordinateSystem(Hep3Vector org, Hep3Vector unit_x, Hep3Vector unit_y, Hep3Vector unit_z) {
    // origin = org;
    // u = unit_x;
    // v = unit_y;
    // w = unit_z;
    // }

    public SurveyCoordinateSystem(Hep3Vector ball, Hep3Vector vee, Hep3Vector flat) {
        origin = ball;
        Hep3Vector ball_to_vee = VecOp.sub(vee, ball);
        u = VecOp.unit(ball_to_vee);
        Hep3Vector ball_to_flat = VecOp.sub(flat, ball);
        w = VecOp.unit(VecOp.cross(ball_to_vee, ball_to_flat));
        v = VecOp.cross(w, u);
        check();
    }

    private void check() {
        checkUnitLength();
        checkAngles();
    }

    private void checkUnitLength() {
        if (u.magnitude() - 1 > 0.00001 || v.magnitude() - 1 > 0.00001 || v.magnitude() - 1 > 0.00001) {
            throw new RuntimeException("Error: the unit vectors of the  coordinate system is ill-defined " + toString());
        }
    }

    private void checkAngles() {
        if ((VecOp.dot(u, v) - 1) > 0.00001 || (VecOp.dot(u, w) - 1) > 0.00001 || (VecOp.dot(v, w) - 1) > 0.00001) {
            throw new RuntimeException("Error: the angles in coordinate system is ill-defined " + toString());
        }
    }

    /**
     * Transform this coordinate system to another one.
     * 
     * @param t
     */
    public void transform(Transform3D t) {
        Transform3D t_this = getTransformation();
        Hep3Vector v = t_this.getTranslation().getTranslationVector();
        Hep3Vector vrot = t.rotated(v);
        Hep3Vector vrottrans = t.translated(vrot);
        origin = vrottrans;
        rotate(t.getRotation());
        // System.out.printf("monkey transform\n");
        // System.out.printf("v %s\n",v.toString());
        // System.out.printf("vrot %s\n",vrot.toString());
        // System.out.printf("vrottrans %s\n",vrottrans.toString());
        check();
    }

    public void rotate(IRotation3D r) {
        r.rotate(u);
        r.rotate(v);
        r.rotate(w);
    }

    public void translate(Hep3Vector translation) {
        // update origin with local translation in u,v,w
        // origin = VecOp.add(origin, translation);
        translate(new Translation3D(translation));
    }

    public void translate(Translation3D t) {
        origin = t.translated(getTransformation().getTranslation().getTranslationVector());
    }

    public void rotateApache(Rotation r) {
        if (debug)
            System.out.printf("%s: apply apache rotation to this coord system\n%s\n", getClass().getSimpleName(),
                    toString());
        this.u = new BasicHep3Vector(r.applyTo(new Vector3D(u.v())).toArray());
        this.v = new BasicHep3Vector(r.applyTo(new Vector3D(v.v())).toArray());
        this.w = new BasicHep3Vector(r.applyTo(new Vector3D(w.v())).toArray());
        if (debug)
            System.out.printf("%s: new coord system after apache rotation to this coord system\n%s\n", getClass()
                    .getSimpleName(), toString());
    }

    public Hep3Vector origin() {
        return origin;
    }

    public Hep3Vector u() {
        return u;
    }

    public Hep3Vector v() {
        return v;
    }

    public Hep3Vector w() {
        return w;
    }

    public void u(Hep3Vector vec) {
        u = vec;
    }

    public void v(Hep3Vector vec) {
        v = vec;
    }

    public void w(Hep3Vector vec) {
        w = vec;
    }

    public String toString() {
        String str = "origin " + origin.toString() + "\nu " + u.toString() + "\nv " + v.toString() + "\nw "
                + w.toString();
        return str;
    }

    /**
     * Find @ITransform3D to the coordinate system defined by the input.
     * 
     * @return resulting 3D transform
     */
    public Transform3D getTransformation() {
        // Find the transform between the two frames - use transform classes here (not really needed)
        Translation3D translation = new Translation3D(origin.x(), origin.y(), origin.z());
        // RotationGeant trackingToEnvelopeRotation = new RotationGeant(0, 0, 0);
        Rotation3D rotation = new Rotation3D(new BasicHep3Matrix(u.x(), v.x(), w.x(), u.y(), v.y(), w.y(), u.z(),
                v.z(), w.z()));
        Transform3D envelopeToSupportTransform = new Transform3D(translation, rotation);
        return envelopeToSupportTransform;
    }

}
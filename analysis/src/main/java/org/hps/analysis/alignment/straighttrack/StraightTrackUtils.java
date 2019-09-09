package org.hps.analysis.alignment.straighttrack;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

/**
 *
 * @author Norman A. Graf
 */
public class StraightTrackUtils {

    /**
     * Return the distance along the line P1-P0 to the intersection with a plane
     * defined by the point V0 and normal n
     *
     * @param P0 First point on the line segment
     * @param P1 Last point on the line segment
     * @param V0 Point on the plane
     * @param n normal to the plane
     * @return distance along (P0-P1) of line-plane intersection
     */
    public static double sToPlane(Hep3Vector P0, Hep3Vector P1, Hep3Vector V0, Hep3Vector n) {
        double num = VecOp.dot(n, VecOp.sub(V0, P0));
        double denom = VecOp.dot(n, VecOp.sub(P1, P0));
        return num / denom;
    }

    /**
     * Return the intersection point of the line P1-P0 to a plane defined by the
     * point V0 and normal n
     *
     * @param P0 First point on the line segment
     * @param P1 Last point on the line segment
     * @param V0 Point on the plane
     * @param n normal to the plane
     * @return the point of intersection in global coordinates
     */
    public static Hep3Vector linePlaneIntersect(Hep3Vector P0, Hep3Vector P1, Hep3Vector V0, Hep3Vector n) {
        double num = VecOp.dot(n, VecOp.sub(V0, P0));
        Hep3Vector line = VecOp.sub(P1, P0);
        double denom = VecOp.dot(n, line);
        double s = num / denom;
        if (0 <= s && 1 > s) {
            return VecOp.add(P0, VecOp.mult(s, line));
        }
        return null;
    }

}

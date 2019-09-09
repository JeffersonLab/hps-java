package org.hps.analysis.alignment.straighttrack;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import junit.framework.TestCase;
import org.lcsim.detector.solids.Point3D;

/**
 *
 * @author Norman A. Graf
 */
public class StraightTrackUtilsTest extends TestCase {

    public void test_sToPlane() {
        Hep3Vector P0 = new BasicHep3Vector(0., 0., 0.);
        Hep3Vector P1 = new BasicHep3Vector(0., 0., 2.);
        Hep3Vector V0 = new BasicHep3Vector(0., 0., 1.);
        Hep3Vector n = new BasicHep3Vector(0., 0., 1.);

        double s = StraightTrackUtils.sToPlane(P0, P1, V0, n);
        System.out.println("s= "+s);
        
        Hep3Vector p = StraightTrackUtils.linePlaneIntersect(P0, P1, V0, n);
        System.out.println("p= "+p);
        
        Hep3Vector n1 = new BasicHep3Vector(0., 1., 1.);
        double s1 = StraightTrackUtils.sToPlane(P0, P1, V0, n1);
        System.out.println("s1= "+s1);
        
        Point3D p0 = new Point3D(0.,0.,0.);
        Point3D p1 = new Point3D(0.,0.,2.);
        Point3D v0 = new Point3D(0.,0.,1.);
        
        s = StraightTrackUtils.sToPlane(p0, p1, v0, n);
        System.out.println("Point3D s= "+s);
    }

}

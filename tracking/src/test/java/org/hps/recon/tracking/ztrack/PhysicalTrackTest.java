package org.hps.recon.tracking.ztrack;

import junit.framework.TestCase;

/**
 *
 * @author ngraf
 */
public class PhysicalTrackTest extends TestCase {

    public void testPhysicalTrack() {
        System.out.println("BFAC= " + PhysicalTrack.BFAC);
        CartesianThreeVector pos = new CartesianThreeVector();
        CartesianThreeVector mom = new CartesianThreeVector(1., 2., 3.);
        CartesianThreeVector B0 = new CartesianThreeVector();
        double E = mom.magnitude();
        int q = -1;
        PhysicalTrack track = new PhysicalTrack(pos, mom, E, q);
        System.out.println(track);

        double step = 2 * mom.magnitude();
        CartesianThreeVector posAtS = track.pointOnPath(step, B0);
        System.out.println("point @ " + step + " " + posAtS);
        CartesianThreeVector momAtS = track.momentumOnPath(step, B0);
        System.out.println("mom @ " + step + " " + momAtS);

        // test track plane intersection, still for zero field
        CartesianThreeVector momZ = new CartesianThreeVector(0., 0., 3.);
        PhysicalTrack track2 = new PhysicalTrack(pos, momZ, E, q);
        CartesianThreeVector planePos = new CartesianThreeVector(0., 0., 5.);
        CartesianThreeVector planeNormal = new CartesianThreeVector(0., 0., 1.);
        // z plane at z=5.
        double dist2Plane = track2.approximatePathLengthToPlane(planePos, planeNormal, B0);
        System.out.println(dist2Plane);

        IntersectionStatus stat = track2.planeIntersection(planePos, planeNormal, B0);
        if (stat.success()) {
            System.out.println(stat.position());
        }

        // test stepping action
        CartesianThreeVector BZ = new CartesianThreeVector(0., 0., 1.);
        CartesianThreeVector momX = new CartesianThreeVector(1., 0., 0.);
        PhysicalTrack track3 = new PhysicalTrack(pos, momX, E, q);
        System.out.println("**** Starting track: " + track3);
        int nSteps = 150;
        double ds = 0.;
        CartesianThreeVector old = new CartesianThreeVector();
        for (int i = 0; i < nSteps; ++i) {
            ds += 10;
            CartesianThreeVector vec = track3.pointOnPath(ds, BZ);
//            System.out.println("ds: "+ds+ " vec-old "+vec.minus(old).magnitude());
            System.out.println(vec.x() + " " + vec.y() + " " + vec.z());
            old = vec;
        }
        //  should not intersect a plane at x=34.
        // let's see what happens
        CartesianThreeVector misPlanePos = new CartesianThreeVector(35., 0., 0.);
        CartesianThreeVector misPlaneDir = new CartesianThreeVector(1., 0., 0.);
        IntersectionStatus misStat = track3.planeIntersection(misPlanePos, misPlaneDir, BZ);
        System.out.println(misStat.success());
        if (misStat.success()) {
            System.out.println(misStat.iterations() + " " + misStat.position());
        }

        // this should intersect at 33.27038483548055 -30.962344158835254 0.0
        CartesianThreeVector hitPlanePos = new CartesianThreeVector(33.27038483548055, 0., 0.);
        CartesianThreeVector hitPlaneDir = new CartesianThreeVector(1., 0., 0.);
        IntersectionStatus hitStat = track3.planeIntersection(hitPlanePos, hitPlaneDir, BZ);
        System.out.println(hitStat.success());
        if (hitStat.success()) {
            System.out.println(hitStat.iterations() + " " + hitStat.position());
        }

        //TODO check propagation to plane behind point
    }

}

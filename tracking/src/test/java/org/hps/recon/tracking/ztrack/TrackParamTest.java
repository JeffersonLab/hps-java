package org.hps.recon.tracking.ztrack;

import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.acos;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import java.util.Random;
import junit.framework.TestCase;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class TrackParamTest extends TestCase {

    boolean debug = true;

    public void testTrackParam() {
        ZTrackParam p = new ZTrackParam();
        double x = 1.;
        double y = 2.;
        double z = 3.;
        double dxdz = 0.1;
        double dydz = 0.2;
        double qP = .3;
        p.SetX(x);
        p.SetY(y);
        p.SetZ(z);
        p.SetTx(dxdz);
        p.SetTy(dydz);
        p.SetQp(qP);

        // upper diagonal covariance matrix
        double[] cov = {0., 1., 2., 3., 4., 5., 6., 7., 8., 9., 10., 11., 12., 13., 14.};
        p.SetCovMatrix(cov);
        if (debug) {
            System.out.println(p);
        }
        assertTrue(p.GetX() == x);
        assertTrue(p.GetY() == y);
        assertTrue(p.GetZ() == z);
        assertTrue(p.GetTx() == dxdz);
        assertTrue(p.GetTy() == dydz);
        assertTrue(p.GetQp() == qP);

        double[] cov2 = p.GetCovMatrix();
        assertTrue(cov2.length == 15);
        for (int i = 0; i < 15; ++i) {
            assertTrue(cov2[i] == cov[i]);
            assertTrue(p.GetCovariance(i) == cov[i]);
            cov2[i] = 27.;
            assertTrue(p.GetCovariance(i) == cov[i]);
        }

        //test constructor from position and momentum
        double[] pos = {x, y, z};
        double[] mom = new double[3];
        p.GetMomentum(mom);
        ZTrackParam p2 = new ZTrackParam(pos, mom, 1);

        System.out.println("p  " + p);
        System.out.println("p2 " + p2);
        // check for equality
        assertTrue(p.GetX() == p2.GetX());
        assertTrue(p.GetY() == p2.GetY());
        assertTrue(p.GetZ() == p2.GetZ());
        assertTrue(p.GetTx() == p2.GetTx());
        assertTrue(p.GetTy() == p2.GetTy());
        assertEquals(p.GetQp(), p2.GetQp(), .0000001);
    }

    /*
     * test kicking this track by a 3D angle...
     */
    public void testScatter() {
        // we are dealing with tracks predominantly along z, so the x axis is a good choice
        // as a basis for generating a coordinate system orthogonal to the track vector
        CartesianThreeVector xHat = new CartesianThreeVector(1., 0., 0.);
        double theta = 0.002; // roughly 500MeV electron in 320 microns of Si
        // a track to test
        ZTrackParam p = new ZTrackParam();

        double x = 0.;
        double y = 0.;
        double z = 1.;
        double dxdz = 0.1;
        double dydz = 0.1;
        double qP = 1. / 1.056;
        p.SetX(x);
        p.SetY(y);
        p.SetZ(z);
        p.SetTx(dxdz);
        p.SetTy(dydz);
        p.SetQp(qP);
        System.out.println(p);
        double[] mom = new double[3];
        p.GetMomentum(mom);

        double momentum = abs(1 / qP);
        // pick a random phi angle in the plane perpendicular to momentum vector
        Random ran = new Random();
        double phi = ran.nextDouble() * 2 * PI;

        // the original track
        CartesianThreeVector pIn = new CartesianThreeVector(mom);
        // its unit vector
        CartesianThreeVector pInUnit = pIn.unitVector();

        //create a vector normal to this track vector by crossing it with the xHat vector
        CartesianThreeVector u = pIn.cross(xHat).unitVector();
        // get the third normal vector of the local coordinate system
        CartesianThreeVector v = pIn.cross(u).unitVector();
        // checks...
        System.out.println("pIn " + pIn);
        System.out.println("perp1 " + u);
        System.out.println("perp2 " + v);

        System.out.println("pIn dot perp1 " + pIn.dot(u));
        System.out.println("pIn dot perp2 " + pIn.dot(v));
        System.out.println("perp1 dot perp2 " + u.dot(v));

        // now generate the scattered vector
        // xV = sinTheta*(cosPhi*u + sinPhi*v) + cosTheta*pIn
        double sinTheta = sin(theta);
        double cosTheta = cos(theta);
        double sinPhi = sin(phi);
        double cosPhi = cos(phi);
        CartesianThreeVector xcomp = u.times(sinTheta * cosPhi);
        CartesianThreeVector ycomp = v.times(sinTheta * sinPhi);
        CartesianThreeVector zcomp = pInUnit.times(cosTheta);
        CartesianThreeVector scattered = xcomp.plus(ycomp.plus(zcomp));
        scattered.setMag(momentum);

        // some checks here...
        System.out.println("pIn " + pIn);
        System.out.println("scattered " + scattered);
        //dot product of pIn and scattered should be scattering angle
        double scatAngle = acos(pIn.dot(scattered) / (pIn.magnitude() * scattered.magnitude()));
        System.out.println("theta " + theta + " scattering Angle " + scatAngle);
        assertEquals(theta, scatAngle, .000001);
    }
}

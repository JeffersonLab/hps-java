package org.hps.recon.tracking.ztrack;

import junit.framework.TestCase;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.sin;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class StripHitTest extends TestCase {

    public void testStripHit() {
        StripHit hit = new StripHit();
        double u = 1.;
        double du = .001;
        double phi = PI / 4.;
        double z = 2.;
        double dz = .0001;

        hit.SetU(u);
        hit.SetDu(du);
        hit.SetZ(z);
        hit.SetDz(dz);
        hit.SetPhi(phi);
        assertTrue(hit.GetCosPhi() == cos(phi));
        assertTrue(hit.GetSinPhi() == sin(phi));

        System.out.println(hit);
    }

}

package org.hps.analysis.alignment.straighttrack.kfparticle;

import junit.framework.TestCase;

/**
 *
 * @author ngraf
 */
public class KFTrackTest extends TestCase {

    public void testKFTrack() {
        //                   x   y   x'   y' q/p  z
        double[] t1params = {0., 2., 0., 0.2, 1., 10.};
        double[] t1cov = new double[15];
        for (int i = 0; i < 15; i++) {
            t1cov[i] = 0.;
        }
        t1cov[0] = 1.;
        t1cov[2] = 1.;
        t1cov[5] = 1.;
        t1cov[9] = 1.;
        t1cov[14] = 1.;
        double mass = 0.511;
        double chi2 = 1.;
        boolean isElectron = true;
        int NDF = 5;
        KFTrack t1 = new KFTrack(t1params, t1cov, mass, chi2, isElectron, NDF);

        System.out.println(t1);

        assertEquals(mass, t1.GetMass());
        assertEquals(chi2, t1.GetRefChi2());
        assertEquals(NDF, t1.GetRefNDF());
        assertEquals(isElectron, t1.IsElectron());
    }

}


package org.hps.recon.tracking.circlefit;

import static java.lang.Math.sqrt;
import junit.framework.TestCase;

/**
 *
 * @author Norman A. Graf
 */
public class CircleFitTest extends TestCase
{

    private boolean _debug = false;

    public void testIt()
    {
        CircleFit fit = new CircleFit(0., 0., 1.);
        if (_debug) {
            System.out.println(fit);
        }
        double[][] testPoint = {{0., 1.}, {1., 0.}, {sqrt(0.5), sqrt(0.5)}};
        double[] results = {-0., Double.NEGATIVE_INFINITY, -1.};
        for (int i = 0; i < testPoint.length; ++i) {
            double tangent = fit.tangentAtPoint(testPoint[i]);
            assertEquals(tangent, results[i]);
            if (_debug) {
                System.out.println("tangent at (" + testPoint[i][0] + ", " + testPoint[i][1] + ") is " + tangent);
            }
        }
    }
}

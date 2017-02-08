
package org.hps.recon.tracking.circlefit;

import junit.framework.TestCase;
import static org.hps.recon.tracking.circlefit.TwoPointRadiusCircleFitter.findCircles;

/**
 *
 */
public class TwoPointRadiusCircleFitterTest extends TestCase
{

    private boolean _debug = false;

    public void testIt()
    {
        int i;
        double[][] cases
                = {{0.1234, 0.9876}, {0.8765, 0.2345},
                {0.0000, 2.0000}, {0.0000, 0.0000},
                {0.1234, 0.9876}, {0.1234, 0.9876},
                {0.1234, 0.9876}, {0.8765, 0.2345},
                {0.1234, 0.9876}, {0.1234, 0.9876}
                };

        double radii[] = {2.0, 1.0, 2.0, 0.5, 0.0};

        for (i = 0; i < 5; i++) {
            if (_debug) {
                System.out.printf("\nCase %d)", i + 1);
            }
            CircleFit[] results = findCircles(cases[2 * i], cases[2 * i + 1], radii[i]);
            if (_debug) {
                if (results == null) {
                    System.out.println("found no solutions");
                } else {
                    System.out.println("\n found " + results.length + " solution" + (results.length == 1 ? "" : "s"));
                    for (int j = 0; j < results.length; ++j) {
                        System.out.println(results[j]);
                    }
                }
            }
        }
    }
}

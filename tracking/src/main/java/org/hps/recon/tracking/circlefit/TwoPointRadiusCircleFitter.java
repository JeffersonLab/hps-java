package org.hps.recon.tracking.circlefit;

import static java.lang.Math.hypot;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;

/**
 * Fits a circle to two points and a radius
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class TwoPointRadiusCircleFitter
{

    private static boolean _debug = false;

    static double distance(double[] p1, double[] p2)
    {
        return hypot((p1[0] - p2[0]), p1[1] - p2[1]);
    }

    /**
     * Returns two circles which can be fit to two points and a radius
     * @param p1 A point on the cricle 
     * @param p2 Another point on the circle
     * @param radius The radius of the circle
     * @return The two circles which can be fit, or null 
     */
    public static CircleFit[] findCircles(double[] p1, double[] p2, double radius)
    {
        CircleFit[] results = null;
        double separation = distance(p1, p2);
        double mirrorDistance;

        if (separation == 0.0) {
            if (radius == 0.0) {
                if (_debug) {
                    System.out.printf("\nNo circles can be drawn through (%.4f,%.4f)", p1[0], p1[1]);
                }
            } else {
                if (_debug) {
                    System.out.printf("\nInfinitely many circles can be drawn through (%.4f,%.4f)", p1[0], p1[1]);
                }
            }
        } else if (separation == 2 * radius) {
            results = new CircleFit[1];
            results[0] = new CircleFit((p1[0] + p2[0]) / 2, (p1[1] + p2[1]) / 2, radius);
            if (_debug) {
                System.out.printf("\nGiven points are opposite ends of a diameter of the circle with center (%.4f,%.4f) and radius %.4f", (p1[0] + p2[0]) / 2, (p1[1] + p2[1]) / 2, radius);
            }
        } else if (separation > 2 * radius) {
            if (_debug) {
                System.out.printf("\nGiven points are farther away from each other than a diameter of a circle with radius %.4f", radius);
            }
        } else {
            mirrorDistance = sqrt(pow(radius, 2) - pow(separation / 2, 2));
            results = new CircleFit[2];
            results[0] = new CircleFit((p1[0] + p2[0]) / 2 + mirrorDistance * (p1[1] - p2[1]) / separation, (p1[1] + p2[1]) / 2 + mirrorDistance * (p2[0] - p1[0]) / separation, radius);
            results[1] = new CircleFit((p1[0] + p2[0]) / 2 - mirrorDistance * (p1[1] - p2[1]) / separation, (p1[1] + p2[1]) / 2 - mirrorDistance * (p2[0] - p1[0]) / separation, radius);
            if (_debug) {
                System.out.printf("\nTwo circles are possible.");
            }
            if (_debug) {
                System.out.printf("\nCircle C1 with center (%.4f,%.4f), radius %.4f and Circle C2 with center (%.4f,%.4f), radius %.4f", (p1[0] + p2[0]) / 2 + mirrorDistance * (p1[1] - p2[1]) / separation, (p1[1] + p2[1]) / 2 + mirrorDistance * (p2[0] - p1[0]) / separation, radius, (p1[0] + p2[0]) / 2 - mirrorDistance * (p1[1] - p2[1]) / separation, (p1[1] + p2[1]) / 2 - mirrorDistance * (p2[0] - p1[0]) / separation, radius);
            }
        }
        return results;
    }

}

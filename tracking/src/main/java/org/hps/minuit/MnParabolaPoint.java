package org.hps.minuit;

/**
 *
 */
class MnParabolaPoint {

    MnParabolaPoint(double x, double y) {
        theX = x;
        theY = y;
    }

    double x() {
        return theX;
    }

    double y() {
        return theY;
    }

    private double theX;
    private double theY;
}

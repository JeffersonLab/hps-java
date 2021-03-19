package org.hps.minuit;

/**
 * A class representing a pair of double (x,y) or (lower,upper)
 *
 */
public class Point {

    public Point(double first, double second) {
        this.first = first;
        this.second = second;
    }
    public double first;
    public double second;
}

package org.hps.recon.tracking.circlefit;

/**
 * A class which encapsulates the result of a circle fit.
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class CircleFit
{

    private double _x0;
    private double _y0;
    private double _r;

    /**
     * Creates a new instance of CircleFit
     *
     * @param x0 the x coordinate of the center of the circle
     * @param y0 the y coordinate of the center of the circle
     * @param r the radius of the circle
     */
    public CircleFit(double x0, double y0, double r)
    {
        _x0 = x0;
        _y0 = y0;
        _r = r;
    }

    /**
     * The x coordinate of the center of the circle.
     *
     * @return the x coordinate of the center of the circle
     */
    public double x0()
    {
        return _x0;
    }

    /**
     * The y coordinate of the center of the circle
     *
     * @return
     */
    public double y0()
    {
        return _y0;
    }

    /**
     * The radius of the circle
     *
     * @return the radius of the circle
     */
    public double radius()
    {
        return _r;
    }

    /**
     * Return the slope of the tangent (dy/dx) at a point on this circle (does
     * not check if point is on circle)
     *
     * @param p double array assumed to be (x, y)
     * @return the slope of the tangent to this circle at the point p
     */
    public double tangentAtPoint(double[] p)
    {
        double tangent;
        tangent = -(p[0] - _x0) / (p[1] - _y0);
        return tangent;
    }

    /**
     * String representation of this object
     *
     * @return String representation of this object
     */
    public String toString()
    {
        return "CircleFit: x0= " + _x0 + " y0= " + _y0 + " r= " + _r;
    }

}

package org.hps.monitoring.ecal;

import java.awt.Point;

/**
 * The <code>Datum</code> class contains a point representing a crystal in the
 * calorimeter display panel.
 * 
 * @author Kyle McCarty
 **/
public class Datum {
    // The coordinate on the calorimeter panel.
    protected Point loc;

    /**
     * <b>Datum</b><br/>
     * <br/>
     * <code>public <b>Datum</b>()</code><br/>
     * <br/>
     * Initializes an empty <code>Datum</code>. Note that it will have an
     * invalid coordinate.
     **/
    public Datum() {
        this(-1, -1);
    }

    /**
     * <b>Datum</b><br/>
     * <br/>
     * <code>public <b>Datum</b>(int x, int y)</code><br/>
     * <br/>
     * Initializes a new <code>Datum</code> at the indicated coordinate.
     * 
     * @param x
     *            - The x-coordinate of the object.
     * @param y
     *            - The y-coordinate of the object.
     **/
    public Datum(int x, int y) {
        loc = new Point(x, y);
    }

    /**
     * <b>getX</b><br/>
     * <br/>
     * <code>public int <b>getX</b>()</code><br/>
     * <br/>
     * Indicates the x-coordinate of the object.
     * 
     * @return Returns the x-cooridinate as an <code>int</code>.
     **/
    public int getX() {
        return loc.x;
    }

    /**
     * <b>getY</b><br/>
     * <br/>
     * <code>public int <b>getY</b>()</code><br/>
     * <br/>
     * Indicates the y-coordinate of the object.
     * 
     * @return Returns the y-coordiate as an <code>int</code>.
     **/
    public int getY() {
        return loc.y;
    }

    /**
     * <b>getLocation</b><br/>
     * <br/>
     * <code>public Point <b>getLocation</b>()</code><br/>
     * <br/>
     * Indicates the location of the object.
     * 
     * @return Returns the object's location as a <code>Point
     * </code> object.
     **/
    public Point getLocation() {
        return loc;
    }

    /**
     * <b>setX</b><br/>
     * <br/>
     * <code>public void <b>setX</b>(int x)</code><br/>
     * <br/>
     * Sets the object's x-coordinate.
     * 
     * @param x
     *            - The new x-coordinate.
     **/
    public void setX(int x) {
        loc.x = x;
    }

    /**
     * <b>setY</b><br/>
     * <br/>
     * <code>public void <b>setY</b>(int y)</code><br/>
     * <br/>
     * Sets the obejct's y-coordinate.
     * 
     * @param y
     *            - The new y-coordinate.
     **/
    public void setY(int y) {
        loc.y = y;
    }
}

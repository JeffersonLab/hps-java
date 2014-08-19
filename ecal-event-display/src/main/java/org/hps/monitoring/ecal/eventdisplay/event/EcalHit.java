package org.hps.monitoring.ecal.event;

import java.awt.Point;

/**
 * The class <code>EcalHit</code> is an extension of <code>Datum</code>
 * that stores an energy. This is used for reading input from a text
 * file. <code>CalorimeterHit</code> should be used when reading from
 * an LCIO file.
 **/
public final class EcalHit {
    // The coordinate on the calorimeter panel.
    protected final Point loc;
    // The (raw) energy of this hit.
    private double energy = 0.0;
    
    /**
     * <b>EcalHit</b><br/><br/>
     * <code>public <b>EcalHit</b>(int ix, int iy, double energy)</code><br/><br/>
     * Initializes a calorimeter hit object.
     * @param ix - The x-coordinate of the hit.
     * @param iy - The y-coordinate of the hit.
     * @param energy - The raw energy of the hit.
     **/
    public EcalHit(int ix, int iy, double energy) {
    	this(new Point(ix, iy), energy);
    }
    
    /**
     * <b>EcalHit</b><br/><br/>
     * <code>public <b>EcalHit</b>(Point ixy, double energy)</code><br/><br/>
     * Initializes a calorimeter hit object.
     * @param ixy - The x/y-coordinates of the hit.
     * @param energy - The raw energy of the hit.
     **/
    public EcalHit(Point ixy, double energy) {
    	loc = ixy;
        this.energy = energy;
    }
    
    /**
     * <b>getEnergy</b><br/><br/>
     * <code>public double <b>getEnergy</b>()</code><br/><br/>
     * Indicates the raw energy of this hit.
     * @return Returns the raw energy as a <code>double</code>.
     **/
    public double getEnergy() { return energy; }
    
    /**
     * <b>getLocation</b><br/><br/>
     * <code>public Point <b>getLocation</b>()</code><br/><br/>
     * Indicates the location of the object.
     * @return Returns the object's location as a <code>Point
     * </code> object.
     **/
    public Point getLocation() { return loc; }
    
    /**
     * <b>getX</b><br/><br/>
     * <code>public int <b>getX</b>()</code><br/><br/>
     * Indicates the x-coordinate of the object.
     * @return Returns the x-coordinate as an <code>int</code>.
     **/
    public int getX() { return loc.x; }
    
    /**
     * <b>getY</b><br/><br/>
     * <code>public int <b>getY</b>()</code><br/><br/>
     * Indicates the y-coordinate of the object.
     * @return Returns the y-coordinate as an <code>int</code>.
     **/
    public int getY() { return loc.y; }
    
    /**
     * <b>setEnergy</b><br/><br/>
     * <code>public void <b>setEnergy</b>(double energy)</code><br/><br/>
     * Sets the energy of the hit to the indicated value.
     * @param energy - The new energy of the hit.
     **/
    public void setEnergy(double energy) { this.energy = energy; }
    
    /**
     * <b>setX</b><br/><br/>
     * <code>public void <b>setX</b>(int x)</code><br/><br/>
     * Sets the object's x-coordinate.
     * @param x - The new x-coordinate.
     **/
    public void setX(int x) { loc.x = x; }
    
    /**
     * <b>setY</b><br/><br/>
     * <code>public void <b>setY</b>(int y)</code><br/><br/>
     * Sets the obejct's y-coordinate.
     * @param y - The new y-coordinate.
     **/
    public void setY(int y) { loc.y = y; }
}

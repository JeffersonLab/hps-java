package org.hps.monitoring.ecal.eventdisplay.event;

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
    // The energy of this hit.
    private double energy = 0.0;
    // The time of this hit.
    private double time = 0.0;
    
    /**
     * Initializes a calorimeter hit object.
     * @param ix - The x-coordinate of the hit.
     * @param iy - The y-coordinate of the hit.
     * @param energy - The raw energy of the hit.
     **/
    public EcalHit(int ix, int iy, double energy) {
        this(new Point(ix, iy), energy);
    }
    
    /**
     * Initializes a calorimeter hit object.
     * @param ixy - The x/y-coordinates of the hit.
     * @param energy - The raw energy of the hit.
     **/
    public EcalHit(Point ixy, double energy) {
        loc = ixy;
        this.energy = energy;
    }
    
    /**
     * Initializes a calorimeter hit object.
     * @param ix - The x-coordinate of the hit.
     * @param iy - The y-coordinate of the hit.
     * @param energy - The raw energy of the hit.
     * @param time - The time-stamp for the hit.
     **/
    public EcalHit(int ix, int iy, double energy, double time) {
        this(new Point(ix, iy), energy, time);
    }
    
    /**
     * Initializes a calorimeter hit object.
     * @param ixy - The x/y-coordinates of the hit.
     * @param energy - The raw energy of the hit.
     * @param time - The time-stamp for the hit.
     **/
    public EcalHit(Point ixy, double energy, double time) {
        loc = ixy;
        this.energy = energy;
        this.time = time;
    }
    
    /**
     * Indicates the raw energy of this hit.
     * @return Returns the raw energy as a <code>double</code>.
     **/
    public double getEnergy() { return energy; }
    
    /**
     * Indicates the location of the object.
     * @return Returns the object's location as a <code>Point
     * </code> object.
     **/
    public Point getLocation() { return loc; }
    
    /**
     * Indicates the time at which the hit occurred.
     * @return Returns the hit time.
     */
    public double getTime() { return time; }
    
    /**
     * Indicates the x-coordinate of the object.
     * @return Returns the x-coordinate as an <code>int</code>.
     **/
    public int getX() { return loc.x; }
    
    /**
     * Indicates the y-coordinate of the object.
     * @return Returns the y-coordinate as an <code>int</code>.
     **/
    public int getY() { return loc.y; }
    
    /**
     * Sets the energy of the hit to the indicated value.
     * @param energy - The new energy of the hit.
     **/
    public void setEnergy(double energy) { this.energy = energy; }
    
    /**
     * Sets the object's x-coordinate.
     * @param x - The new x-coordinate.
     **/
    public void setX(int x) { loc.x = x; }
    
    /**
     * Sets the obejct's y-coordinate.
     * @param y - The new y-coordinate.
     **/
    public void setY(int y) { loc.y = y; }
}

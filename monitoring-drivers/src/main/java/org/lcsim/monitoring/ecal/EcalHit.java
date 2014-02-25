package org.hps.monitoring.ecal;

/**
 * The class <code>EcalHit</code> is an extension of <code>Datum</code>
 * that stores an energy. This is used for reading input from a text
 * file. <code>CalorimeterHit</code> should be used when reading from
 * an LCIO file.
 **/
public final class EcalHit extends Datum {
    // The (raw) energy of this hit.
    private double energy = 0.0;
    
    /**
     * <b>EcalHit</b><br/><br/>
     * <code>public <b>EcalHit</b>(int x, int y, double energy)</code><br/><br/>
     * Initializes a calorimeter hit object.
     * @param x - The x-coordinate of the hit.
     * @param y - The y-coordinate of the hit.
     * @param energy - The raw energy of the hit.
     **/
    public EcalHit(int x, int y, double energy) {
        super(x, y);
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
     * <b>setEnergy</b><br/><br/>
     * <code>public void <b>setEnergy</b>(double energy)</code><br/><br/>
     * Sets the energy of the hit to the indicated value.
     * @param energy - The new energy of the hit.
     **/
    public void setEnergy(double energy) { this.energy = energy; }
}

package org.hps.record.daqconfig2019;

/**
 * Class <code>FEEPrecaleConfig2019</code> is an implementation of the abstract
 * <code>AbstractCutConfig2019</code> for FEE prescale of a region.
 * It provides the means to access this value and, for package classes,
 * set it.
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */ 

public class FEEPrecaleConfig2019 extends AbstractConfig2019<Double>{
    private static final int PRESCALE_REGION_MIN  = 0;
    private static final int PRESCALE_REGION_MAX  = 1;
    private static final int PRESCALE  = 2;
    
    /**
     * Instantiates a new <code>FEEPrecaleConfig2019</code> object.
     */
    public FEEPrecaleConfig2019() { super(3); }
    
    /**
     * Gets minimum of prescale region.
     * @return Returns minimum of prescale region as a <code>double</code>.
     */
    public double getRegionMin() {
        return getValue(PRESCALE_REGION_MIN);
    }
    
    /**
     * Sets minimum of prescale region.
     * @param value - The minimum of prescale region.
     */
    void setRegionMin(double value) {
        setValue(PRESCALE_REGION_MIN, value);
    }
    
    /**
     * Gets maximum of prescale region.
     * @return Returns maximum of prescale region as a <code>double</code>.
     */
    public double getRegionMax() {
        return getValue(PRESCALE_REGION_MAX);
    }
    
    /**
     * Sets maximum of prescale region.
     * @param value - The maximum of prescale region.
     */
    void setRegionMax(double value) {
        setValue(PRESCALE_REGION_MAX, value);
    }
    
    /**
     * Gets FEE prescale.
     * @return Returns FEE prescale as a <code>double</code>.
     */
    public double getRegionPrescale() {
        return getValue(PRESCALE);
    }
    
    /**
     * Sets FEE prescale.
     * @param value - The FEE prescale.
     */
    void setRegionPrescale(double value) {
        setValue(PRESCALE, value);
    }
    
    
}

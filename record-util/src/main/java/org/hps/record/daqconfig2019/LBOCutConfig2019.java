package org.hps.record.daqconfig2019;

/**
 * Class <code>LBOCutConfig2019</code> is an implementation of the abstract
 * <code>AbstractCutConfig2019</code> for cuts that have only a lower bound.
 * It provides the means to access this value and, for package classes,
 * set it.
 * 
 * Code is developed referring to org.hps.record.daqconfig.LBOCutConfig by Kyle McCarty
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */
public class LBOCutConfig2019 extends AbstractConfig2019<Double> {
    private static final int LOWER_BOUND = 0;
    
    /**
     * Instantiates a new <code>LBOCutConfig</code> object.
     */
    public LBOCutConfig2019() { super(1); }
    
    /**
     * Gets the lower bound of the cut.
     * @return Returns the lower bound as a <code>double</code>.
     */
    public double getLowerBound() {
        return getValue(LOWER_BOUND);
    }
    
    /**
     * Sets the lower bound of the cut to the specified value.
     * @param value - The new lower bound for the cut.
     */
    void setLowerBound(double value) {
        setValue(LOWER_BOUND, value);
    }
}
package org.hps.record.daqconfig2019;

/**
 * Class <code>ULBCutConfig2019</code> is an implementation of the abstract
 * <code>AbstractCutConfig2019</code> for cuts that have both an upper and
 * a lower bound. It provides the means to access these values and, for
 * package classes, set them.
 * 
 * Code is developed referring to org.hps.record.daqconfig.ULBCutConfig by Kyle McCarty
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */
public class ULBCutConfig2019 extends AbstractConfig2019<Double> {
    private static final int LOWER_BOUND = 0;
    private static final int UPPER_BOUND = 1;
    
    /**
     * Instantiates a new <code>ULBCutConfig</code> object.
     */
    public ULBCutConfig2019() { super(2); }
    
    /**
     * Gets the lower bound of the cut.
     * @return Returns the lower bound as a <code>double</code>.
     */
    public double getLowerBound() {
        return getValue(LOWER_BOUND);
    }
    
    /**
     * Gets the upper bound of the cut.
     * @return Returns the upper bound as a <code>double</code>.
     */
    public double getUpperBound() {
        return getValue(UPPER_BOUND);
    }
    
    /**
     * Sets the lower bound of the cut to the specified value.
     * @param value - The new lower bound for the cut.
     */
    void setLowerBound(double value) {
        setValue(LOWER_BOUND, value);
    }
    
    /**
     * Sets the upper bound of the cut to the specified value.
     * @param value - The new upper bound for the cut.
     */
    void setUpperBound(double value) {
        setValue(UPPER_BOUND, value);
    }
}
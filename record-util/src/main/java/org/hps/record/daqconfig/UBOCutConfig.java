package org.hps.record.daqconfig;

/**
 * Class <code>UBOCutConfig</code> is an implementation of the abstract
 * <code>AbstractCutConfig</code> for cuts that have only an upper bound.
 * It provides the means to access this value and, for package classes,
 * set it.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class UBOCutConfig extends AbstractConfig<Double> {
    private static final int UPPER_BOUND = 0;
    
    /**
     * Instantiates a new <code>UBOCutConfig</code> object.
     */
    UBOCutConfig() { super(1); }
    
    /**
     * Gets the upper bound of the cut.
     * @return Returns the upper bound as a <code>double</code>.
     */
    public double getUpperBound() {
        return getValue(UPPER_BOUND);
    }
    
    /**
     * Sets the upper bound of the cut to the specified value.
     * @param value - The new upper bound for the cut.
     */
    void setUpperBound(double value) {
        setValue(UPPER_BOUND, value);
    }
}
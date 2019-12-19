package org.hps.record.daqconfig2019;

/**
 * Class <code>UBOCutConfig2019</code> is an implementation of the abstract
 * <code>AbstractCutConfig2019</code> for cuts that have only an upper bound.
 * It provides the means to access this value and, for package classes,
 * set it.
 * 
 * Code is developed referring to org.hps.record.daqconfig.UBOCutConfig by Kyle McCarty
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */
public class UBOCutConfig2019 extends AbstractConfig2019<Double> {
    private static final int UPPER_BOUND = 0;
    
    /**
     * Instantiates a new <code>UBOCutConfig</code> object.
     */
    public UBOCutConfig2019() { super(1); }
    
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
package org.hps.record.daqconfig;

/**
 * Class <code>ESBCutConfig</code> is an implementation of the abstract
 * <code>AbstractCutConfig</code> for cuts the energy slope cut. It
 * stores both the cut lower bound and the parameter F used in the slope
 * equation. These values may also be set by classes within the same
 * package.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class ESBCutConfig extends AbstractConfig<Double> {
    private static final int ENERGY_SLOPE_THRESHOLD = 0;
    private static final int PARAMETER_F = 1;
    
    /**
     * Instantiates a new <code>ESBCutConfig</code> object.
     */
    ESBCutConfig() { super(2); }
    
    /**
     * Gets the lower bound of the cut.
     * @return Returns the lower bound as a <code>double</code>.
     */
    public double getLowerBound() {
        return getValue(ENERGY_SLOPE_THRESHOLD);
    }
    
    /**
     * Gets the parameter F in the energy slope equation.
     * @return Returns the parameter as a <code>double</code>.
     */
    public double getParameterF() {
        return getValue(PARAMETER_F);
    }
    
    /**
     * Sets the lower bound of the cut to the specified value.
     * @param value - The new lower bound for the cut.
     */
    void setLowerBound(double value) {
        setValue(ENERGY_SLOPE_THRESHOLD, value);
    }
    
    /**
     * Sets the parameter F in the energy slope equation.
     * @param value - The new parameter for the cut.
     */
    void setParameterF(double value) {
        setValue(PARAMETER_F, value);
    }
}
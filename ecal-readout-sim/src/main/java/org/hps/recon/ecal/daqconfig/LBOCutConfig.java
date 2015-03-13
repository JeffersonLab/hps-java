package org.hps.recon.ecal.daqconfig;

/**
 * Class <code>LBOCutConfig</code> is an implementation of the abstract
 * <code>AbstractCutConfig</code> for cuts that have only a lower bound.
 * It provides the means to access this value and, for package classes,
 * set it.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class LBOCutConfig extends AbstractConfig<Double> {
	private static final int LOWER_BOUND = 0;
	
	/**
	 * Instantiates a new <code>LBOCutConfig</code> object.
	 */
	LBOCutConfig() { super(1); }
	
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

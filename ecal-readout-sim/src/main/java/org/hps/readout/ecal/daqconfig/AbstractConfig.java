package org.hps.readout.ecal.daqconfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract class <code>AbstractCutConfig</code> holds a given number
 * of values and allows implementing classes to access them. It also
 * stores whether the configuration object is enabled.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
abstract class AbstractConfig<E> {
	// Store the cut values.
	private final List<E> values;
	private boolean enabled = false;
	
	/**
	 * Instantiates an <code>AbstractConfig</code> with the indicated
	 * number of values.
	 * @param count - The number of values that the object should store.
	 */
	AbstractConfig(int count) {
		// A configuration object must have at least one value.
		if(count <= 0) {
			throw new IllegalArgumentException("There must be at least one value.");
		}
		
		// Instantiate the value array.
		values = new ArrayList<E>(count);
	}
	
	/**
	 * Gets the value of the cut with the associated value ID.
	 * @param valueIndex - The ID corresponding to the desired value.
	 * @return Returns the value as a parameterized <code>E</code>
	 * object.
	 */
	protected E getValue(int valueIndex) {
		validateValueIndex(valueIndex);
		return values.get(valueIndex);
	}
	
	/**
	 * Indicates whether the object is enabled or not.
	 * @return Returns <code>true</code> if the object is enabled and
	 * <code>false</code> otherwise.
	 */
	public boolean isEnabled() {
		return enabled;
	}
	
	/**
	 * Sets the value corresponding to the argument value ID to a new
	 * object.
	 * @param valueIndex - The ID corresponding to the desired value.
	 * @param value - The new value.
	 */
	protected void setValue(int valueIndex, E value) {
		validateValueIndex(valueIndex);
		values.set(valueIndex, value);
	}
	
	/**
	 * Sets whether the configuration object is enabled.
	 * @param state <code>true</code> means that the object is enabled
	 * and <code>false</code> that it is disabled.
	 */
	protected void setIsEnabled(boolean state) {
		enabled = state;
	}
	
	/**
	 * Throws an exception if the argument index does not correspond to
	 * any value.
	 * @param index - The index to check.
	 */
	private final void validateValueIndex(int index) {
		if(index < 0 || index >= values.size()) {
			throw new IndexOutOfBoundsException("Value index \"" + index + "\" is invalid.");
		}
	}
}
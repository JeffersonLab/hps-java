package org.hps.users.kmccarty;

import org.hps.readout.ecal.TriggerModule;

public class PairTrigger<E> extends SinglesTrigger<E> {
	// Define the supported trigger cuts.
	private static final String PAIR_ENERGY_SUM_LOW = TriggerModule.PAIR_ENERGY_SUM_LOW;
	private static final String PAIR_ENERGY_SUM_HIGH = TriggerModule.PAIR_ENERGY_SUM_HIGH;
	private static final String PAIR_ENERGY_DIFFERENCE_HIGH = TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH;
	private static final String PAIR_ENERGY_SLOPE_LOW = TriggerModule.PAIR_ENERGY_SLOPE_LOW;
	private static final String PAIR_COPLANARITY_HIGH = TriggerModule.PAIR_COPLANARITY_HIGH;
	
	/**
	 * Instantiates a new <code>PairTrigger</code> with all cut
	 * states set to <code>false</code> and with the trigger source
	 * defined according to the specified object.
	 * @param source - The object from which the trigger cut states
	 * are derived.
	 */
	protected PairTrigger(E source) {
		// Instantiate the superclass.
		super(source);
		
		// Add the supported cuts types.
		addValidCut(PAIR_ENERGY_SUM_LOW);
		addValidCut(PAIR_ENERGY_SUM_HIGH);
		addValidCut(PAIR_ENERGY_DIFFERENCE_HIGH);
		addValidCut(PAIR_ENERGY_SLOPE_LOW);
		addValidCut(PAIR_COPLANARITY_HIGH);
	}
	
	/**
	 * Gets whether the pair energy sum lower bound cut was met.
	 * @return Returns <code>true</code> if the cut was met and
	 * <code>false</code> otherwise.
	 */
	public boolean getStateEnergySumLow() {
		return getCutState(PAIR_ENERGY_SUM_LOW);
	}
	
	/**
	 * Gets whether the pair energy sum upper bound cut was met.
	 * @return Returns <code>true</code> if the cut was met and
	 * <code>false</code> otherwise.
	 */
	public boolean getStateEnergySumHigh() {
		return getCutState(PAIR_ENERGY_SUM_HIGH);
	}
	
	/**
	 * Gets whether both the pair energy sum upper and lower bound cuts
	 * were met.
	 * @return Returns <code>true</code> if the cuts were met and
	 * <code>false</code> otherwise.
	 */
	public boolean getStateEnergySum() {
		return getCutState(PAIR_ENERGY_SUM_HIGH);
	}
	
	/**
	 * Gets whether the pair energy difference cut was met.
	 * @return Returns <code>true</code> if the cut was met and
	 * <code>false</code> otherwise.
	 */
	public boolean getStateEnergyDifference() {
		return getCutState(PAIR_ENERGY_SUM_HIGH);
	}
	
	/**
	 * Gets whether the pair energy slope cut was met.
	 * @return Returns <code>true</code> if the cut was met and
	 * <code>false</code> otherwise.
	 */
	public boolean getStateEnergySlope() {
		return getCutState(PAIR_ENERGY_SLOPE_LOW);
	}
	
	/**
	 * Gets whether the pair coplanarity cut was met.
	 * @return Returns <code>true</code> if the cut was met and
	 * <code>false</code> otherwise.
	 */
	public boolean getStateCoplanarity() {
		return getCutState(PAIR_COPLANARITY_HIGH);
	}
	
	/**
	 * Sets whether the conditions for the pair energy sum lower bound
	 * cut were met.
	 * @param state - <code>true</code> indicates that the cut conditions
	 * were met and <code>false</code> that they were not.
	 */
	public void getStateEnergySumLow(boolean state) {
		setCutState(PAIR_ENERGY_SUM_LOW, state);
	}
	
	/**
	 * Sets whether the conditions for the pair energy sum upper bound
	 * cut were met.
	 * @param state - <code>true</code> indicates that the cut conditions
	 * were met and <code>false</code> that they were not.
	 */
	public void getStateEnergySumHigh(boolean state) {
		setCutState(PAIR_ENERGY_SUM_HIGH, state);
	}
	
	/**
	 * Sets whether the conditions for the pair energy difference cut
	 * were met.
	 * @param state - <code>true</code> indicates that the cut conditions
	 * were met and <code>false</code> that they were not.
	 */
	public void getStateEnergyDifference(boolean state) {
		setCutState(PAIR_ENERGY_DIFFERENCE_HIGH, state);
	}
	
	/**
	 * Sets whether the conditions for the pair energy slope cut were
	 * met.
	 * @param state - <code>true</code> indicates that the cut conditions
	 * were met and <code>false</code> that they were not.
	 */
	public void getStateEnergySlope(boolean state) {
		setCutState(PAIR_ENERGY_SLOPE_LOW, state);
	}
	
	/**
	 * Sets whether the conditions for the pair coplanarity cut were
	 * met.
	 * @param state - <code>true</code> indicates that the cut conditions
	 * were met and <code>false</code> that they were not.
	 */
	public void getStateCoplanarity(boolean state) {
		setCutState(PAIR_COPLANARITY_HIGH, state);
	}
}

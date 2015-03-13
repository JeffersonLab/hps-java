package org.hps.analysis.trigger.event;

import org.hps.analysis.trigger.util.Pair;
import org.hps.analysis.trigger.util.PairTrigger;
import org.hps.analysis.trigger.util.SinglesTrigger;
import org.hps.analysis.trigger.util.Trigger;
import org.hps.recon.ecal.triggerbank.SSPNumberedTrigger;
import org.hps.recon.ecal.triggerbank.SSPPairTrigger;
import org.hps.recon.ecal.triggerbank.SSPSinglesTrigger;

/**
 * Stores a pair of one simulated trigger and one SSP bank trigger that
 * have been matched, along with which cuts are the same and which are
 * different.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class TriggerMatchedPair extends Pair<Trigger<?>, SSPNumberedTrigger> {
	// Cut IDs.
	public static final int SINGLES_ENERGY_MIN = 0;
	public static final int SINGLES_ENERGY_MAX = 1;
	public static final int SINGLES_HIT_COUNT = 2;
	public static final int PAIR_ENERGY_SUM = 0;
	public static final int PAIR_ENERGY_DIFF = 1;
	public static final int PAIR_ENERGY_SLOPE = 2;
	public static final int PAIR_COPLANARITY = 3;
	
	// Store the trigger result.
	private boolean[] matchedCut = new boolean[4];
	
	/**
	 * Creates a matched trigger pair of a simulated trigger and an SSP
	 * bank trigger.
	 * @param firstElement - The simulated trigger.
	 * @param secondElement - The SSP bank trigger.
	 * @param cutsMatched - An array indicating which cuts match and
	 * which do not.
	 */
	public TriggerMatchedPair(Trigger<?> firstElement, SSPNumberedTrigger secondElement, boolean[] cutsMatched) {
		// Store the trigger objects.
		super(firstElement, secondElement);
		
		// Set the matched cuts.
		if(cutsMatched.length != 4) {
			throw new IllegalArgumentException(String.format("The matched cuts array must be of size 4. Seen size = %d.", cutsMatched.length));
		} else {
			matchedCut[0] = cutsMatched[0];
			matchedCut[1] = cutsMatched[1];
			matchedCut[2] = cutsMatched[2];
			matchedCut[3] = cutsMatched[3];
		}
	}
	
	/**
	 * Gets whether the cut of the given cut ID passed the trigger.
	 * @param cutID - The cut ID.
	 * @return Returns <code>true</code> if the cut passed and
	 * <code>false</code> otherwise.
	 */
	public boolean getCutResult(int cutID) {
		if(cutID > matchedCut.length || cutID < 0) {
			throw new IndexOutOfBoundsException(String.format("Cut ID \"%d\" is not valid.", cutID));
		} else {
			return matchedCut[cutID];
		}
	}
	
	/**
	 * Gets the simulated trigger from the pair.
	 * @return Returns the simulated trigger as an object of the generic
	 * type <code>Trigger<?></code>.
	 */
	public Trigger<?> getSimulatedTrigger() {
		return getFirstElement();
	}
	
	/**
	 * Gets the type of cluster on which the trigger was simulated.
	 * @return Returns the type of cluster as a <code>Class<?></code>
	 * object.
	 */
	public Class<?> getSimulatedTriggerType() {
		return getFirstElement().getTriggerSource().getClass();
	}
	
	/**
	 * Gets the SSP bank trigger from the pair.
	 * @return Returns the SSP bank trigger as an object of the generic
	 * type <code>SSPNumberedTrigger</code>.
	 */
	public SSPNumberedTrigger getSSPTrigger() {
		return getSecondElement();
	}
	
	/**
	 * Returns whether the triggers in this pair are from the first
	 * trigger or not.
	 * @return Returns <code>true</code> if the triggers are from the
	 * first trigger and <code>false</code> otherwise.
	 */
	public boolean isFirstTrigger() {
		return getSecondElement().isFirstTrigger();
	}
	
	/**
	 * Indicates whether this is a pair of pair triggers.
	 * @return Returns <code>true</code> if the triggers are pair
	 * triggers and <code>false</code> otherwise.
	 */
	public boolean isPairTrigger() {
		if(getFirstElement() instanceof PairTrigger && getSecondElement() instanceof SSPPairTrigger) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Returns whether the triggers in this pair are from the second
	 * trigger or not.
	 * @return Returns <code>true</code> if the triggers are from the
	 * second trigger and <code>false</code> otherwise.
	 */
	public boolean isSecondTrigger() {
		return getSecondElement().isSecondTrigger();
	}
	
	/**
	 * Indicates whether this is a pair of singles triggers.
	 * @return Returns <code>true</code> if the triggers are singles
	 * triggers and <code>false</code> otherwise.
	 */
	public boolean isSinglesTrigger() {
		if(getFirstElement() instanceof SinglesTrigger && getSecondElement() instanceof SSPSinglesTrigger) {
			return true;
		} else {
			return false;
		}
	}
}

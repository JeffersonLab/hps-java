package org.hps.users.kmccarty.triggerdiagnostics.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class <code>Trigger</code> stores a set cut states indicating whether
 * specific cut conditions associated with a trigger were met or not as
 * well as the state of the overall trigger. It is the responsibility of
 * implementing classes to specify the supported cut states and also
 * to define when the trigger conditions are met.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public abstract class Trigger<E> {
	// Track whether the trigger conditions were met.
	private boolean passTrigger = false;
	// Store the cut condition states.
	private Map<String, Boolean> passMap = new HashMap<String, Boolean>();
	// Store the cluster associated with the trigger.
	private final E source;
	// Store the trigger number.
	private final int triggerNum;
	
	/**
	 * Creates a new <code>Trigger</code> object with the argument
	 * specifying the object from whence the trigger state is derived.
	 * @param source - The trigger source object.
	 */
	protected Trigger(E source) {
		this(source, -1);
	}
	
	/**
	 * Creates a new <code>Trigger</code> object with the argument
	 * specifying the object from whence the trigger state is derived.
	 * @param source - The trigger source object.
	 * @param triggerNum - The number of the trigger.
	 */
	protected Trigger(E source, int triggerNum) {
		this.source = source;
		this.triggerNum = triggerNum;
	}
	
	/**
	 * Adds a cut to the set of cuts tracked by this trigger.
	 * @param cut - The identifier for the cut.
	 */
	protected void addValidCut(String cut) {
		passMap.put(cut, new Boolean(false));
	}
	
	/**
	 * Gets the state of the specified cut.
	 * @param cut - The identifier for the cut.
	 * @return Returns <code>true</code> if the conditions for the
	 * specified cut were met and <code>false</code> otherwise.
	 * @throws IllegalArgumentException Occurs if the specified cut
	 * is not supported by the object.
	 */
	protected boolean getCutState(String cut) throws IllegalArgumentException {
		if(passMap.containsKey(cut)) {
			return passMap.get(cut);
		} else {
			throw new IllegalArgumentException(String.format("Trigger cut \"%s\" is not a supported trigger cut.", cut));
		}
	}
	
	/**
	 * Gets the number of the trigger. If the trigger has no number,
	 * it will return <code>-1</code>.
	 * @return Returns the trigger number as an <code>int</code>.
	 */
	public int getTriggerNumber() {
		return triggerNum;
	}
	
	/**
	 * Gets the object to which the trigger cuts are applied.
	 * @return Returns the trigger source object.
	 */
	public E getTriggerSource() { return source; }
	
	/**
	 * Gets whether the conditions for the trigger were met.
	 * @return Returns <code>true</code> if the conditions for the
	 * trigger were met and <code>false</code> if they were not.
	 */
	public boolean getTriggerState() {
		return passTrigger;
	}
	
	/**
	 * Removes a cut from the set of cuts tracked by the trigger.
	 * @param cut - The identifier for the cut.
	 */
	protected void removeValidCut(String cut) {
		passMap.remove(cut);
	}
	
	/**
	 * Checks whether the all of the trigger cut conditions were met.
	 * @return Returns <code>true</code> if all of the cut conditions
	 * were met and <code>false</code> otherwise.
	 */
	private boolean isValidTrigger() {
		// Iterate over all of the cuts and look for any that have not
		// been met.
		for(Entry<String, Boolean> cut : passMap.entrySet()) {
			if(!cut.getValue()) { return false; }
		}
		
		// If there are no cut conditions that have not been met, then
		// the trigger is valid.
		return true;
	}
	
	/**
	 * Sets whether the conditions for the specified cut were met.
	 * @param cut - The identifier for the cut.
	 * @param state - <code>true</code> indicates that the conditions
	 * for the cut were met and <code>false</code> that they were not.
	 * @throws IllegalArgumentException Occurs if the specified cut
	 * is not supported by the object.
	 */
	protected void setCutState(String cut, boolean state) throws IllegalArgumentException {
		if(passMap.containsKey(cut)) {
			// Set the cut state.
			passMap.put(cut, state);
			
			// If the cut state is true, then all cut conditions may have
			// been met. Check whether this is true and, if so, set the
			// trigger state accordingly.
			if(state && isValidTrigger()) { passTrigger = true; }
			else { passTrigger = false; }
		} else {
			throw new IllegalArgumentException(String.format("Trigger cut \"%s\" is not a supported trigger cut.", cut));
		}
	}
	
	/**
	 * Indicates whether the specified cut state is tracked by this
	 * object or not.
	 * @param cut - The identifier for the cut.
	 * @return Returns <code>true</code> if the cut state is tracked
	 * by this object and <code>false</code> otherwise.
	 */
	protected boolean supportsCut(String cut) {
		return passMap.containsKey(cut);
	}
}

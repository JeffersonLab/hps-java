package org.hps.analysis.trigger.event;

import java.util.ArrayList;
import java.util.List;

import org.hps.analysis.trigger.util.Pair;
import org.hps.analysis.trigger.util.Trigger;
import org.hps.recon.ecal.triggerbank.SSPNumberedTrigger;

/**
 * Tracks trigger pairs that were matched within an event. This can also
 * track simulated SSP cluster pairs along with specifically which cuts
 * passed and which did not.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class TriggerMatchEvent {
	// Track trigger matching statistics.
	private int[] sspInternalMatched = new int[2];
	private int[] reconTriggersMatched = new int[2];
	private int[][] triggerComp = new int[4][2];
	
	// Track the matched trigger pairs.
	private List<TriggerMatchedPair> sspPairList = new ArrayList<TriggerMatchedPair>();
	private List<Pair<Trigger<?>, SSPNumberedTrigger>> reconPairList = new ArrayList<Pair<Trigger<?>, SSPNumberedTrigger>>();
	
	/**
	 * Gets the number of times a cut of the given cut ID failed when
	 * SSP simulated triggers were compared to SSP bank triggers.
	 * @param triggerNumber - The trigger for which to get the value.
	 * @param cutID - The ID of the cut.
	 * @return Returns the number of times the cut failed as an
	 * <code>int</code> primitive.
	 */
	public int getCutFailures(int triggerNumber, int cutID) {
		// Validate the arguments.
		if(triggerNumber !=0 && triggerNumber != 1) {
			throw new IndexOutOfBoundsException("Trigger number must be 0 or 1.");
		} if(cutID < 0 || cutID > 3) {
			throw new IndexOutOfBoundsException(String.format("Cut ID \"%d\" is not valid.", cutID));
		}
		
		// Return the requested cut value.
		return triggerComp[cutID][triggerNumber];
	}
	
	/**
	 * Gets the number of reconstructed cluster triggers that were
	 * matched successfully.
	 * @return Returns the value as an <code>int</code> primitive.
	 */
	public int getMatchedReconTriggers() {
		return reconTriggersMatched[0] + reconTriggersMatched[1];
	}
	
	/**
	 * Gets the number of reconstructed cluster triggers that were
	 * matched successfully for a specific trigger.
	 * @param triggerNumber - The trigger number.
	 * @return Returns the value as an <code>int</code> primitive.
	 */
	public int getMatchedReconTriggers(int triggerNumber) {
		// Validate the arguments.
		if(triggerNumber != 0 && triggerNumber != 1) {
			throw new IndexOutOfBoundsException("Trigger number must be 0 or 1.");
		}
		
		// Return the trigger count.
		return reconTriggersMatched[triggerNumber];
	}
	
	/**
	 * Gets the number of simulated SSP cluster triggers that were
	 * matched successfully.
	 * @return Returns the value as an <code>int</code> primitive.
	 */
	public int getMatchedSSPTriggers() {
		return sspInternalMatched[0] + sspInternalMatched[1];
	}
	
	/**
	 * Gets the number of simulated SSP cluster triggers that were
	 * matched successfully for a specific trigger.
	 * @param triggerNumber - The trigger number.
	 * @return Returns the value as an <code>int</code> primitive.
	 */
	public int getMatchedSSPTriggers(int triggerNumber) {
		// Validate the arguments.
		if(triggerNumber != 0 && triggerNumber != 1) {
			throw new IndexOutOfBoundsException("Trigger number must be 0 or 1.");
		}
		
		// Return the trigger count.
		return sspInternalMatched[triggerNumber];
	}
	
	/**
	 * Gets a list containing all reconstructed cluster triggers and
	 * their matched SSP bank triggers.
	 * @return Returns the trigger pairs as a <code>List</code>
	 * collection of <code>Pair</code> objects.
	 */
	public List<Pair<Trigger<?>, SSPNumberedTrigger>> getMatchedReconPairs() {
		return reconPairList;
	}
	
	/**
	 * Adds a reconstructed trigger and SSP bank trigger pair that is
	 * marked as matched for all trigger cuts.
	 * @param reconTrigger - The reconstructed cluster trigger.
	 * @param sspTrigger - The SSP bank trigger.
	 */
	public void matchedReconPair(Trigger<?> reconTrigger, SSPNumberedTrigger sspTrigger) {
		reconTriggersMatched[sspTrigger.isFirstTrigger() ? 0 : 1]++;
		reconPairList.add(new Pair<Trigger<?>, SSPNumberedTrigger>(reconTrigger, sspTrigger));
	}
	
	/**
	 * Adds a simulated SSP trigger and SSP bank trigger pair that is
	 * marked as matched for all trigger cuts.
	 * @param simTrigger - The simulated SSP cluster trigger.
	 * @param sspTrigger - The SSP bank trigger.
	 */
	public void matchedSSPPair(Trigger<?> simTrigger, SSPNumberedTrigger sspTrigger) {
		sspInternalMatched[sspTrigger.isFirstTrigger() ? 0 : 1]++;
		sspPairList.add(new TriggerMatchedPair(simTrigger, sspTrigger, new boolean[] { true, true, true, true }));
	}
	
	/**
	 * Adds a simulated SSP trigger and SSP bank trigger pair along
	 * with the cuts that matched and did not.
	 * @param simTrigger - The simulated SSP cluster trigger.
	 * @param sspTrigger - The SSP bank trigger.
	 * @param cutsMatched - An array indicating which cuts matched and
	 * which did not.
	 */
	public void matchedSSPPair(Trigger<?> simTrigger, SSPNumberedTrigger sspTrigger, boolean[] cutsMatched) {
		// The cut values must be stored in an array of size four, but
		// singles triggers use arrays of size 3. If the array is not
		// of the appropriate size, resize it.
		boolean[] cutArray;
		if(cutsMatched.length == 4) {
			cutArray = cutsMatched;
		} else {
			cutArray = new boolean[] { cutsMatched[0], cutsMatched[1], cutsMatched[2], true };
		}
		
		// Add the trigger pair to the list.
		TriggerMatchedPair triggerPair = new TriggerMatchedPair(simTrigger, sspTrigger, cutArray);
		sspPairList.add(triggerPair);
		
		// Track which cuts have failed.
		boolean isMatched = true;
		int triggerNum = triggerPair.isFirstTrigger() ? 0 : 1;
		for(int cutIndex = 0; cutIndex < cutsMatched.length; cutIndex++) {
			if(!cutsMatched[cutIndex]) {
				triggerComp[cutIndex][triggerNum]++;
				isMatched = false;
			}
		}
		
		// If all the cuts are true, then the trigger pair is a match.
		if(isMatched) { sspInternalMatched[triggerNum]++; }
	}
}

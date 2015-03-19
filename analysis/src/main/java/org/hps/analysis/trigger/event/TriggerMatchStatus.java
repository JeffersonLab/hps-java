package org.hps.analysis.trigger.event;

import java.util.List;

import org.hps.analysis.trigger.util.Trigger;
import org.hps.analysis.trigger.util.TriggerDiagnosticUtil;
import org.hps.recon.ecal.triggerbank.SSPNumberedTrigger;

/**
 * Tracks the trigger diagnostic statistics for trigger matching.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class TriggerMatchStatus extends TriggerStatModule {
	// Track the number of triggers seen.
	private int[] sspTriggersSeen = new int[2];
	private int[] reconTriggersSeen = new int[2];
	
	/**
	 * Adds the statistical data stored in a trigger comparison event
	 * into this status tracking module.
	 * @param event - The event object.
	 * @param reconTriggers - A list of reconstructed cluster triggers.
	 * @param sspSimTriggers - A list of simulated SSP cluster triggers.
	 * @param sspBankTriggers - A list of SSP bank triggers.
	 */
	public void addEvent(int eventType, TriggerMatchEvent event, List<List<? extends Trigger<?>>> reconTriggers,
			List<List<? extends Trigger<?>>> sspSimTriggers, List<? extends SSPNumberedTrigger> sspBankTriggers) {
		// Increment the event type count.
		if(eventType == TriggerDiagnosticUtil.TRIGGER_SINGLES_1 || eventType == TriggerDiagnosticUtil.TRIGGER_PAIR_1) {
			triggerTypesSeen[0]++;
		} else if(eventType == TriggerDiagnosticUtil.TRIGGER_SINGLES_2 || eventType == TriggerDiagnosticUtil.TRIGGER_PAIR_2) {
			triggerTypesSeen[1]++;
		}
		
		// Check if there are more bank triggers than there are
		// simulated SSP triggers.
		int sspTriggerDiff = sspBankTriggers.size() - sspSimTriggers.size();
		if(sspTriggerDiff > 0) {
			reportedExtras += sspTriggerDiff;
		}
		
		// Increment the number of triggers of each type that have been
		// seen so far.
		sspTriggers += sspSimTriggers.get(0).size() + sspSimTriggers.get(1).size();
		this.reconTriggers += reconTriggers.get(0).size() + reconTriggers.get(1).size();
		reportedTriggers += sspBankTriggers.size();
		
		// Fill the specific trigger counters.
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			sspTriggersSeen[triggerNum] += sspSimTriggers.get(triggerNum).size();
			reconTriggersSeen[triggerNum] += reconTriggers.get(triggerNum).size();
		}
		
		// Increment the count for each cut failure type.
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			unmatchedTriggers[triggerNum] += event.getUnmatchedTriggers(triggerNum);
			
			for(int cutIndex = 0; cutIndex < 4; cutIndex++) {
				triggerComp[cutIndex][triggerNum] += event.getCutFailures(triggerNum, cutIndex);
			}
		}
		
		// Increment the total triggers found.
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			sspInternalMatched[triggerNum] += event.getMatchedSSPTriggers(triggerNum);
			reconTriggersMatched[triggerNum] += event.getMatchedReconTriggers(triggerNum);
		}
		
		// Check if a trigger of the right time was found.
		// Get the trigger number and type.
		if(event.sawEventType()) {
			triggerTypesFound[eventType]++;
		}
	}
	
	@Override
	public void clear() {
		// Clear the statistics module.
		super.clear();
	}
	
    /**
     * Gets a copy of the statistical data stored in the object.
     * @return Returns the data in a <code>TriggerStatModule</code>
     * object.
     */
	public TriggerStatModule cloneStatModule() {
    	return new TriggerStatModule(this);
    }
	
	/**
	 * Gets the number of reconstructed triggers seen for a specific
	 * trigger number.
	 * @param triggerNum - The trigger number.
	 * @return Returns the total number of reconstructed triggers seen
	 * by the indicated trigger number. 
	 */
	public int getTotalReconTriggers(int triggerNum) {
		return getSSPTriggerCount(false, triggerNum);
	}
	
	/**
	 * Gets the number of SSP bank triggers seen for a specific trigger
	 * number.
	 * @param triggerNum - The trigger number.
	 * @return Returns the total number of SSP bank triggers seen by
	 * the indicated trigger number. 
	 */
	public int getTotalSSPTriggers(int triggerNum) {
		return getSSPTriggerCount(true, triggerNum);
	}
	
	/**
	 * Gets the total number of triggers seen for a specific trigger
	 * source type and number.
	 * @param isSSP - Whether the trigger source is SSP bank clusters.
	 * @param triggerNum - The trigger number.
	 * @return Returns the trigger count.
	 */
	private final int getSSPTriggerCount(boolean isSSP, int triggerNum) {
		// Make sure the trigger number is valid.
		validateTriggerNumber(triggerNum);
		
		// Return the triggers.
		if(isSSP) { return sspTriggersSeen[triggerNum]; }
		else { return reconTriggersSeen[triggerNum]; }
	}
	
	/**
	 * Produces an exception if the trigger number argument is invalid.
	 * @param triggerNum - The trigger number to validate.
	 */
	private static final void validateTriggerNumber(int triggerNum) {
		if(triggerNum < 0 || triggerNum > 2) {
			throw new IndexOutOfBoundsException(String.format("Trigger number \"%d\" is invalid", triggerNum));
		}
	}
}

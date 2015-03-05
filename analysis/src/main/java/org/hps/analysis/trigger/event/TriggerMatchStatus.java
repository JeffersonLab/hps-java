package org.hps.analysis.trigger.event;

import java.util.List;

import org.hps.analysis.trigger.util.Trigger;
import org.hps.readout.ecal.triggerbank.SSPNumberedTrigger;

/**
 * Tracks the trigger diagnostic statistics for trigger matching.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class TriggerMatchStatus extends TriggerStatModule {
	/**
	 * Adds the statistical data stored in a trigger comparison event
	 * into this status tracking module.
	 * @param event - The event object.
	 * @param reconTriggers - A list of reconstructed cluster triggers.
	 * @param sspSimTriggers - A list of simulated SSP cluster triggers.
	 * @param sspBankTriggers - A list of SSP bank triggers.
	 */
	public void addEvent(TriggerMatchEvent event, List<List<? extends Trigger<?>>> reconTriggers,
			List<List<? extends Trigger<?>>> sspSimTriggers, List<? extends SSPNumberedTrigger> sspBankTriggers) {
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
		
		// Increment the count for each cut failure type.
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			for(int cutIndex = 0; cutIndex < 4; cutIndex++) {
				triggerComp[cutIndex][triggerNum] += event.getCutFailures(triggerNum, cutIndex);
			}
		}
		
		// Increment the total triggers found.
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			sspInternalMatched[triggerNum] += event.getMatchedSSPTriggers(triggerNum);
			reconTriggersMatched[triggerNum] += event.getMatchedReconTriggers(triggerNum);
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
}

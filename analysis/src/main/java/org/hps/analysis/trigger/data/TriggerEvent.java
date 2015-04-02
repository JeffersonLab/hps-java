package org.hps.analysis.trigger.data;

/**
 * Class <code>TriggerEvent</code> tracks all of the statistics from
 * a single trigger event. It is an extension of the statistics class
 * <code>TriggerStatModule</code> which implements methods for altering
 * the values in the base class.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class TriggerEvent extends TriggerStatModule {
	/**
	 * Adds the statistics from one event object into this one.
	 * @param event - The event data to add.
	 */
	public void addEvent(TriggerStatModule event) {
		// Merge the values that do not depend on trigger source type.
		reportedTriggers += event.reportedTriggers;
		
		// Merge each value that depends on the trigger source type.
		for(int sourceType = 0; sourceType < 2; sourceType++) {
			simTriggers[sourceType] += event.simTriggers[sourceType];
			matchedTriggers[sourceType] += event.matchedTriggers[sourceType];
			unmatchedTriggers[sourceType] += event.unmatchedTriggers[sourceType];
			
			// Merge the number of times each cut failed.
			for(int cutID = 0; cutID < 4; cutID++) {
				failedCuts[sourceType][cutID] += event.failedCuts[sourceType][cutID];
			}
			
			// Copy the values for the TI flag trigger counters.
			for(int tiType = 0; tiType < 6; tiType++) {
				tiTriggersSeen[sourceType][tiType] += event.tiTriggersSeen[sourceType][tiType];
				tiTriggersMatched[sourceType][tiType] += event.tiTriggersMatched[sourceType][tiType];
			}
		}
	}
	
	/**
	 * Indicates that a reconstructed trigger could not be matched, even
	 * partially, to an SSP bank trigger.
	 */
	public void failedReconTrigger() {
		unmatchedTriggers[RECON]++;
	}
	
	/**
	 * Indicates that an SSP simulated trigger could not be matched, even
	 * partially, to an SSP bank trigger.
	 */
	public void failedSSPTrigger() {
		unmatchedTriggers[SSP]++;
	}

	/**
	 * Indicates that a trigger simulated from a reconstructed cluster
	 * was successfully matched to a trigger in the SSP bank.
	 * @param tiFlags - An array of size 6 indicating which TI bank
	 * flags are active and which are not.
	 * @param triggerTypeID - An identifier indicating the type of
	 * trigger that was matched.
	 */
	public void matchedReconTrigger(boolean[] tiFlags) {
		matchedTriggers(tiFlags, RECON);
	}
	
	/**
	 * Indicates that a trigger simulated from a reconstructed cluster
	 * was partially matched to a trigger in the SSP bank, and notes
	 * which cuts did and did not match.
	 * @param tiFlags - An array of size 6 indicating which TI bank
	 * flags are active and which are not.
	 * @param triggerTypeID - An identifier indicating the type of
	 * trigger that was matched.
	 * @param matchedCuts - An array of size 3 or 4 indicating which
	 * cuts did and did not align between the triggers.
	 */
	public void matchedReconTrigger(boolean[] tiFlags, boolean[] matchedCuts) {
		matchedTriggers(tiFlags, matchedCuts, RECON);
	}
	
	/**
	 * Indicates that a trigger simulated from an SSP bank cluster was
	 * successfully matched to a trigger in the SSP bank.
	 * @param tiFlags - An array of size 6 indicating which TI bank
	 * flags are active and which are not.
	 * @param triggerTypeID - An identifier indicating the type of
	 * trigger that was matched.
	 */
	public void matchedSSPTrigger(boolean[] tiFlags) {
		matchedTriggers(tiFlags, SSP);
	}
	
	/**
	 * Indicates that a trigger simulated from an SSP bank cluster was
	 * partially matched to a trigger in the SSP bank, and notes which
	 * cuts did and did not match.
	 * @param tiFlags - An array of size 6 indicating which TI bank
	 * flags are active and which are not.
	 * @param triggerTypeID - An identifier indicating the type of
	 * trigger that was matched.
	 * @param matchedCuts - An array of size 3 or 4 indicating which
	 * cuts did and did not align between the triggers.
	 */
	public void matchedSSPTrigger(boolean[] tiFlags, boolean[] matchedCuts) {
		matchedTriggers(tiFlags, matchedCuts, SSP);
	}
	
	/**
	 * Indicates that a trigger simulated from a reconstructed cluster
	 * was seen and increments the count for this type of trigger by one.
	 * @param tiFlags - Whether or not each of the TI bank flags is
	 * active or not.
	 */
	public void sawReconSimulatedTrigger(boolean[] tiFlags) {
		sawReconSimulatedTriggers(tiFlags, 1);
	}
	
	/**
	 * Indicates that a number triggers simulated from reconstructed
	 * clusters were seen and increments the count for this type of
	 * trigger by the indicated number.
	 * @param tiFlags - Whether or not each of the TI bank flags is
	 * active or not.
	 * @param count - The number of simulated triggers seen.
	 */
	public void sawReconSimulatedTriggers(boolean[] tiFlags, int count) {
		// Increment the total count.
		simTriggers[RECON] += count;
		
		// Increment the TI flag counters.
		for(int tiType = 0; tiType < 6; tiType++) {
			if(tiFlags[tiType]) {
				tiTriggersSeen[RECON][tiType] += count;
			}
		}
	}
	
	/**
	 * Indicates that a trigger from the SSP trigger bank was seen and
	 * increments the count for this type of trigger by one.
	 */
	public void sawReportedTrigger() {
		sawReportedTriggers(1);
	}
	
	/**
	 * Indicates that a number triggers from the SSP trigger bank were
	 * seen and increments the count for this type of trigger by the
	 * indicated number.
	 * @param count - The number of simulated triggers seen.
	 */
	public void sawReportedTriggers(int count) {
		reportedTriggers += count;
	}
	
	/**
	 * Indicates that a trigger simulated from an SSP bank cluster was
	 * seen and increments the count for this type of trigger by one.
	 * @param tiFlags - Whether or not each of the TI bank flags is
	 * active or not.
	 */
	public void sawSSPSimulatedTrigger(boolean[] tiFlags) {
		sawSSPSimulatedTriggers(tiFlags, 1);
	}
	
	/**
	 * Indicates that a number triggers simulated from SSP bank clusters
	 * were seen and increments the count for this type of trigger by
	 * the indicated number.
	 * @param tiFlags - Whether or not each of the TI bank flags is
	 * active or not.
	 * @param count - The number of simulated triggers seen.
	 */
	public void sawSSPSimulatedTriggers(boolean[] tiFlags, int count) {
		// Increment the total count.
		simTriggers[SSP] += count;
		
		// Increment the TI flag counters.
		for(int tiType = 0; tiType < 6; tiType++) {
			if(tiFlags[tiType]) {
				tiTriggersSeen[SSP][tiType] += count;
			}
		}
	}
	
	/**
	 * Indicates that a simulated trigger was successfully matched to
	 * an SSP bank trigger.
	 * @param tiFlags - An array of size 6 indicating which TI bank
	 * flags are active and which are not.
	 * @param sourceType - Uses <code>SSP</code> for triggers simulated
	 * from an SSP bank cluster and <code>RECON</code> for triggers that
	 * were simulated from a reconstructed cluster.
	 */
	private final void matchedTriggers(boolean[] tiFlags, int sourceType) {
		// Increment the total triggers matched.
		matchedTriggers[sourceType]++;
		
		// Increment the triggers matched for this type for each if
		// the active TI bank flags.
		for(int tiType = 0; tiType < 6; tiType++) {
			if(tiFlags[tiType]) {
				tiTriggersMatched[sourceType][tiType]++;
			}
		}
	}
	
	/**
	 * Indicates that a simulated trigger was partially matched to a
	 * trigger in the SSP bank, and notes which cuts did and did not
	 * match.
	 * @param tiFlags - An array of size 6 indicating which TI bank
	 * flags are active and which are not.
	 * @param sourceType - Uses <code>SSP</code> for triggers simulated
	 * from an SSP bank cluster and <code>RECON</code> for triggers that
	 * were simulated from a reconstructed cluster.
	 */
	private void matchedTriggers(boolean[] tiFlags, boolean[] matchedCuts, int sourceType) {
		// The matched cuts must be defined.
		if(matchedCuts == null) {
			throw new NullPointerException("The matched cuts array must be defined.");
		}
		
		// The matched cuts array must be of either size 3 or 4.
		if(matchedCuts.length != 3 && matchedCuts.length != 4) {
			throw new IllegalArgumentException("All triggers must use either three or four cuts.");
		}
		
		// Increment the counters for each cut that was no matched. Also
		// track whether or not a cut actually failed.
		boolean cutFailed = false;
		for(int cutIndex = 0; cutIndex < matchedCuts.length; cutIndex++) {
			if(!matchedCuts[cutIndex]) {
				failedCuts[sourceType][cutIndex]++;
				cutFailed = true;
			}
		}
		
		// If no cut failed, this is actually a match. Increment the
		// appropriate counters.
		if(!cutFailed) {
			matchedTriggers(tiFlags, sourceType);
		}
	}
	
	@Deprecated
	public String getPrintData() {
		StringBuffer out = new StringBuffer();

		out.append("\n");
		out.append("Trigger Result\n");
		out.append("SSP Sim Triggers    :: " + simTriggers[SSP] + "\n");
		out.append("Recon Sim Triggers  :: " + simTriggers[RECON] + "\n");
		out.append("Reported Triggers   :: " + reportedTriggers + "\n");
		out.append(String.format("Internal Efficiency :: %d / %d (%7.3f)%n", matchedTriggers[SSP], simTriggers[SSP],
				(100.0 * matchedTriggers[SSP] / simTriggers[SSP])));
		out.append(String.format("Trigger Efficiency  :: %d / %d (%7.3f)%n", matchedTriggers[RECON], simTriggers[RECON],
				(100.0 * matchedTriggers[RECON] / simTriggers[RECON])));
		
		out.append("\n");
		out.append("Individual Cut Failure Rates\n");
		out.append("Unmatched Triggers   :: " + unmatchedTriggers[SSP] + "\n");
		for(int i = 0; i < 4; i++) {
			out.append(String.format("\tCut %d :: %d / %d (%7.3f)%n", i, failedCuts[SSP][i], simTriggers[SSP],
					(100.0 * failedCuts[SSP][i] / simTriggers[SSP])));
		}
		
		return out.toString();
	}
}

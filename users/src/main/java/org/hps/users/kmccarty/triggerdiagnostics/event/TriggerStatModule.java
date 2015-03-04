package org.hps.users.kmccarty.triggerdiagnostics.event;

/**
 * Class <code>TriggerStatModule</code> stores the statistical data
 * for trigger diagnostic trigger matching.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class TriggerStatModule {
	// Track trigger verification statistical information.
	protected int sspTriggers = 0;
	protected int reconTriggers = 0;
	protected int reportedTriggers = 0;
	protected int reportedExtras = 0;
	protected int[] sspInternalMatched = new int[2];
	protected int[] reconTriggersMatched = new int[2];
	protected int[][] triggerComp = new int[4][2];
	
	/**
	 * Instantiates a <code>TriggerStatModule</code> with no statistics
	 * stored.
	 */
	TriggerStatModule() {  }
	
	/**
	 * Instantiates a <code>TriggerStatModule</code> with no statistics
	 * cloned from the base object.
	 * @param base - The source for the statistical data.
	 */
	protected TriggerStatModule(TriggerStatModule base) {
		// Copy all of the values to the clone.
		sspTriggers = base.sspTriggers;
		reconTriggers = base.reconTriggers;
		reportedTriggers = base.reportedTriggers;
		reportedExtras = base.reportedExtras;
		for(int i = 0; i < sspInternalMatched.length; i++) {
			sspInternalMatched[i] = base.sspInternalMatched[i];
		}
		for(int i = 0; i < reconTriggersMatched.length; i++) {
			reconTriggersMatched[i] = base.reconTriggersMatched[i];
		}
		for(int i = 0; i < triggerComp.length; i++) {
			for(int j = 0; j < triggerComp[i].length; j++) {
				triggerComp[i][j] = base.triggerComp[i][j];
			}
		}
	}
	
	/**
	 * Clears all the tracked values and resets them to the default
	 * empty value.
	 */
	void clear() {
		sspTriggers = 0;
		reconTriggers = 0;
		reportedTriggers = 0;
		reportedExtras = 0;
		sspInternalMatched = new int[2];
		reconTriggersMatched = new int[2];
		triggerComp = new int[4][2];
	}
	
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
	 * Gets the number of SSP bank triggers that were reported in excess
	 * of the number of simulated SSP triggers seen.
	 * @return Returns the value as an <code>int</code> primitive.
	 */
	public int getExtraSSPBankTriggers() {
		return reportedExtras;
	}
	
	/**
	 * Gets the number of reconstructed triggers that matched bank SSP
	 * triggers for both triggers.
	 * @return Returns the value as an <code>int</code> primitive.
	 */
	public int getMatchedReconTriggers() {
		return reconTriggersMatched[0] + reconTriggersMatched[1];
	}
	
	/**
	 * Gets the number of reconstructed triggers that matched bank SSP
	 * triggers.
	 * @param triggerNumber - The trigger for which to get the value.
	 * @return Returns the value as an <code>int</code> primitive.
	 */
	public int getMatchedReconTriggers(int triggerNumber) {
		// Validate the arguments.
		if(triggerNumber !=0 && triggerNumber != 1) {
			throw new IndexOutOfBoundsException("Trigger number must be 0 or 1.");
		}
		
		// Return the trigger count.
		return reconTriggersMatched[triggerNumber];
	}
	
	/**
	 * Gets the number of simulated SSP triggers that matched bank SSP
	 * triggers for both triggers.
	 * @return Returns the value as an <code>int</code> primitive.
	 */
	public int getMatchedSSPTriggers() {
		return sspInternalMatched[0] + sspInternalMatched[1];
	}
	
	/**
	 * Gets the number of simulated SSP triggers that matched bank SSP
	 * triggers.
	 * @param triggerNumber - The trigger for which to get the value.
	 * @return Returns the value as an <code>int</code> primitive.
	 */
	public int getMatchedSSPTriggers(int triggerNumber) {
		// Validate the arguments.
		if(triggerNumber !=0 && triggerNumber != 1) {
			throw new IndexOutOfBoundsException("Trigger number must be 0 or 1.");
		}
		
		// Return the trigger count.
		return sspInternalMatched[triggerNumber];
	}
	
	/**
	 * Gets the number of reconstructed cluster triggers seen.
	 * @return Returns the value as an <code>int</code> primitive.
	 */
	public int getReconTriggerCount() {
		return reconTriggers;
	}
	
	/**
	 * Gets the number of simulated SSP cluster triggers seen.
	 * @return Returns the value as an <code>int</code> primitive.
	 */
	public int getSSPSimTriggerCount() {
		return sspTriggers;
	}
	
	/**
	 * Gets the number of triggers reported by the SSP bank.
	 * @return Returns the value as an <code>int</code> primitive.
	 */
	public int getSSPBankTriggerCount() {
		return reportedTriggers;
	}
}

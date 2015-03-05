package org.hps.users.kmccarty.triggerdiagnostics.ui;

import org.hps.users.kmccarty.triggerdiagnostics.DiagSnapshot;
import org.hps.users.kmccarty.triggerdiagnostics.event.TriggerStatModule;

/**
 * Abstract class <code>AbstractTriggerTablePanel</code> creates the
 * basic framework to display trigger statistics. It is also able to
 * update itself from the diagnostic snapshot given certain information,
 * which is obtained through the implementation of its abstract methods
 * by subclasses.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public abstract class AbstractTriggerTablePanel extends AbstractTwoColumnTablePanel {
	// Static variables.
	private static final long serialVersionUID = 0L;
	
	// Internal variables.
	private final int numCuts;
	
	// Reference variables to the default table rows.
	protected static final int ROW_RECON_COUNT        = 0;
	protected static final int ROW_SSP_SIM_COUNT      = 1;
	protected static final int ROW_SSP_BANK_COUNT     = 2;
	protected static final int ROW_SSP_EFFICIENCY     = 3;
	protected static final int ROW_TRIGGER_EFFICIENCY = 4;
	protected static final int ROW_EMPTY_SPACE        = 5;
	protected static final int ROW_CUT_FAILS_TITLE    = 6;
	protected static final int ROW_FIRST_TRIGGER_CUT  = 7;
	
	/**
	 * Instantiates an <code>AbstractTriggerTablePanel</code> with the
	 * indicated cut names.
	 * @param cutNames
	 */
	public AbstractTriggerTablePanel(String[] cutNames) {
		// Instantiate the superclass.
		super(makeTitle(cutNames));
		
		// Store the number of cuts.
		numCuts = cutNames.length;
		updatePanel(null);
	}
	
	@Override
	public void updatePanel(DiagSnapshot snapshot) {
		// If the snapshot is null, all values should be "N/A."
		if(snapshot == null) {
			// Output cluster count data.
			String scalerNullValue = "---";
			setLocalRowValue(ROW_RECON_COUNT,     scalerNullValue);
			setLocalRowValue(ROW_SSP_SIM_COUNT,   scalerNullValue);
			setLocalRowValue(ROW_SSP_BANK_COUNT,  scalerNullValue);
			setGlobalRowValue(ROW_RECON_COUNT,    scalerNullValue);
			setGlobalRowValue(ROW_SSP_SIM_COUNT,  scalerNullValue);
			setGlobalRowValue(ROW_SSP_BANK_COUNT, scalerNullValue);
			
			// Output the tracked statistical data.
			String percentNullValue = "--- / --- (---%)";
			setLocalRowValue(ROW_SSP_EFFICIENCY,      percentNullValue);
			setLocalRowValue(ROW_TRIGGER_EFFICIENCY,  percentNullValue);
			setGlobalRowValue(ROW_SSP_EFFICIENCY,     percentNullValue);
			setGlobalRowValue(ROW_TRIGGER_EFFICIENCY, percentNullValue);
			
			int ROW_SECOND_TRIGGER_CUT = ROW_FIRST_TRIGGER_CUT + numCuts + 2;
			for(int cutRow = 0; cutRow < numCuts; cutRow++) {
				setLocalRowValue(cutRow + ROW_FIRST_TRIGGER_CUT,   percentNullValue);
				setLocalRowValue(cutRow + ROW_SECOND_TRIGGER_CUT,  percentNullValue);
				setGlobalRowValue(cutRow + ROW_FIRST_TRIGGER_CUT,  percentNullValue);
				setGlobalRowValue(cutRow + ROW_SECOND_TRIGGER_CUT, percentNullValue);
			}
		} else {
			// Get the local and run trigger statistics from the snapshot.
			TriggerStatModule lstat = getLocalModule(snapshot);
			TriggerStatModule rstat = getRunModule(snapshot);
			
			// Determine the most spaces needed to display the values.
			// Get the largest number of digits in any of the values.
			int mostDigits = ComponentUtils.max(lstat.getReconTriggerCount(), lstat.getSSPBankTriggerCount(),
					lstat.getSSPSimTriggerCount(), rstat.getReconTriggerCount(), rstat.getSSPBankTriggerCount(),
					rstat.getSSPSimTriggerCount());
			
			// Update the single-value counters.
			String countFormat = "%" + mostDigits + "d";
			setLocalRowValue(ROW_RECON_COUNT,     String.format(countFormat, lstat.getReconTriggerCount()));
			setLocalRowValue(ROW_SSP_SIM_COUNT,   String.format(countFormat, lstat.getSSPSimTriggerCount()));
			setLocalRowValue(ROW_SSP_BANK_COUNT,  String.format(countFormat, lstat.getSSPBankTriggerCount()));
			setGlobalRowValue(ROW_RECON_COUNT,    String.format(countFormat, rstat.getReconTriggerCount()));
			setGlobalRowValue(ROW_SSP_SIM_COUNT,  String.format(countFormat, rstat.getSSPSimTriggerCount()));
			setGlobalRowValue(ROW_SSP_BANK_COUNT, String.format(countFormat, rstat.getSSPBankTriggerCount()));
			
			// Update the percentage counters.
			String percentFormat = "%" + mostDigits + "d / %" + mostDigits + "d (%7.3f)";
			
			setLocalRowValue(ROW_SSP_EFFICIENCY, String.format(percentFormat, lstat.getMatchedSSPTriggers(),
					lstat.getSSPSimTriggerCount(), (100.0 * lstat.getMatchedSSPTriggers() / lstat.getSSPSimTriggerCount())));
			setLocalRowValue(ROW_TRIGGER_EFFICIENCY, String.format(percentFormat, lstat.getMatchedReconTriggers(),
					lstat.getReconTriggerCount(), (100.0 * lstat.getMatchedReconTriggers() / lstat.getReconTriggerCount())));
			setGlobalRowValue(ROW_SSP_EFFICIENCY, String.format(percentFormat, rstat.getMatchedSSPTriggers(),
					rstat.getSSPSimTriggerCount(), (100.0 * rstat.getMatchedSSPTriggers() / rstat.getSSPSimTriggerCount())));
			setGlobalRowValue(ROW_TRIGGER_EFFICIENCY, String.format(percentFormat, lstat.getMatchedReconTriggers(),
					rstat.getReconTriggerCount(), (100.0 * rstat.getMatchedReconTriggers() / rstat.getReconTriggerCount())));
			
			int ROW_SECOND_TRIGGER_CUT = ROW_FIRST_TRIGGER_CUT + numCuts + 2;
			int[] total = { lstat.getSSPSimTriggerCount() / 2, rstat.getSSPSimTriggerCount() / 2 };
			for(int cutRow = 0; cutRow < numCuts; cutRow++) {
				setLocalRowValue(cutRow + ROW_FIRST_TRIGGER_CUT, String.format(percentFormat, lstat.getCutFailures(0, cutRow),
						total[0], (100.0 * lstat.getCutFailures(0, cutRow) / total[0])));
				setLocalRowValue(cutRow + ROW_SECOND_TRIGGER_CUT, String.format(percentFormat, lstat.getCutFailures(1, cutRow),
						total[0], (100.0 * lstat.getCutFailures(1, cutRow) / total[0])));
				setGlobalRowValue(cutRow + ROW_FIRST_TRIGGER_CUT, String.format(percentFormat, lstat.getCutFailures(0, cutRow),
						total[1], (100.0 * lstat.getCutFailures(0, cutRow) / total[1])));
				setGlobalRowValue(cutRow + ROW_SECOND_TRIGGER_CUT, String.format(percentFormat, lstat.getCutFailures(1, cutRow),
						total[1], (100.0 * lstat.getCutFailures(1, cutRow) / total[1])));
			}
		}
	}
	
	/**
	 * Gets the statistical module from which local statistics should
	 * be drawn.
	 * @param snapshot - The snapshot containing the modules.
	 * @return Returns the module containing local statistical data.
	 */
	protected abstract TriggerStatModule getLocalModule(DiagSnapshot snapshot);
	
	/**
	 * Gets the statistical module from which run statistics should
	 * be drawn.
	 * @param snapshot - The snapshot containing the modules.
	 * @return Returns the module containing run statistical data.
	 */
	protected abstract TriggerStatModule getRunModule(DiagSnapshot snapshot);
	
	/**
	 * Creates the table appropriate table rows from the argument cut
	 * names.
	 * @param cutNames - An array containing the names of the cuts to
	 * display.
	 * @return Returns an array with the default table rows merged in
	 * with the provided cut names.
	 */
	private static final String[] makeTitle(String[] cutNames) {
		// Make a new array to hold all the text.
		String[] mergedArray = new String[cutNames.length + cutNames.length + 9];
		
		// Define the default trigger headers.
		mergedArray[0] = "Recon Triggers:";
		mergedArray[1] = "SSP Sim Triggers:";
		mergedArray[2] = "SSP Bank Triggers:";
		mergedArray[3] = "SSP Efficiency:";
		mergedArray[4] = "Trigger Efficiency:";
		mergedArray[5] = "";
		mergedArray[6] = "First Trigger Cut Failures";
		
		// Insert the cut names for the first trigger.
		for(int cutIndex = 0; cutIndex < cutNames.length; cutIndex++) {
			mergedArray[7 + cutIndex] = cutNames[cutIndex];
		}
		
		// Insert the header for the second trigger cut names.
		int startIndex = 7 + cutNames.length;
		mergedArray[startIndex]     = "";
		mergedArray[startIndex + 1] = "Second Trigger Cut Failures";
		
		// Insert the next set of cut names.
		for(int cutIndex = 0; cutIndex < cutNames.length; cutIndex++) {
			mergedArray[startIndex + 2 + cutIndex] = cutNames[cutIndex];
		}
		
		// Return the resultant array.
		return mergedArray;
	}
}

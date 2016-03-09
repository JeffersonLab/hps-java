package org.hps.monitoring.trigger;

import org.hps.analysis.trigger.data.DiagnosticSnapshot;
import org.hps.analysis.trigger.data.TriggerStatModule;
import org.hps.analysis.trigger.util.ComponentUtils;

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
    private final boolean singles;
    
    // Store reference index variables for local and run values.
    private static final int GLOBAL = 0;
    private static final int LOCAL  = 1;
    
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
    public AbstractTriggerTablePanel(String[] cutNames, boolean isSingles) {
        // Instantiate the superclass.
        super(makeTitle(cutNames));
        
        // Store the number of cuts.
        numCuts = cutNames.length;
        updatePanel(null, null);
        
        // Store whether this is a singles or pair trigger panel.
        singles = isSingles;
    }
    
    @Override
    public void updatePanel(DiagnosticSnapshot runSnapshot, DiagnosticSnapshot localSnapshot) {
        // If the snapshot is null, all values should be "N/A."
        if(runSnapshot == null || localSnapshot == null) {
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
            DiagnosticSnapshot[] stat = new DiagnosticSnapshot[2];
            stat[GLOBAL] = runSnapshot;
            stat[LOCAL] = localSnapshot;
            
            // Get the appropriate trigger statistical modules.
            TriggerStatModule[][] triggerStats = new TriggerStatModule[2][2];
            if(singles) {
                triggerStats[LOCAL][0] = stat[LOCAL].getSingles0Stats();
                triggerStats[LOCAL][1] = stat[LOCAL].getSingles1Stats();
                triggerStats[GLOBAL][0] = stat[GLOBAL].getSingles0Stats();
                triggerStats[GLOBAL][1] = stat[GLOBAL].getSingles1Stats();
            } else {
                triggerStats[LOCAL][0] = stat[LOCAL].getPair0Stats();
                triggerStats[LOCAL][1] = stat[LOCAL].getPair1Stats();
                triggerStats[GLOBAL][0] = stat[GLOBAL].getPair0Stats();
                triggerStats[GLOBAL][1] = stat[GLOBAL].getPair1Stats();
            }
            
            // Get the total number of triggers of each type.
            int[] sspSimTriggers = new int[2];
            int[] sspBankTriggers = new int[2];
            int[] reconSimTriggers = new int[2];
            int[] sspMatchedTriggers = new int[2];
            int[] reconMatchedTriggers = new int[2];
            
            for(int i = 0; i < 2; i++) {
                sspSimTriggers[i] = triggerStats[i][0].getSSPSimulatedTriggers() + triggerStats[i][1].getSSPSimulatedTriggers();
                sspBankTriggers[i] = triggerStats[i][0].getReportedTriggers() + triggerStats[i][1].getReportedTriggers();
                reconSimTriggers[i] = triggerStats[i][0].getReconSimulatedTriggers() + triggerStats[i][1].getReconSimulatedTriggers();
                sspMatchedTriggers[i] = triggerStats[i][0].getMatchedSSPSimulatedTriggers() + triggerStats[i][1].getMatchedSSPSimulatedTriggers();
                reconMatchedTriggers[i] = triggerStats[i][0].getMatchedReconSimulatedTriggers() + triggerStats[i][1].getMatchedReconSimulatedTriggers();
            }
            
            // Determine the most spaces needed to display the values.
            // Get the largest number of digits in any of the values.
            int mostDigits = ComponentUtils.max(reconSimTriggers[LOCAL], sspBankTriggers[LOCAL],
                    sspSimTriggers[LOCAL], reconSimTriggers[GLOBAL], sspBankTriggers[GLOBAL],
                    sspSimTriggers[GLOBAL]);
            int spaces = ComponentUtils.getDigits(mostDigits);
            
            // Update the single-value counters.
            String countFormat = "%" + spaces + "d";
            setLocalRowValue(ROW_RECON_COUNT,     String.format(countFormat, reconSimTriggers[LOCAL]));
            setLocalRowValue(ROW_SSP_SIM_COUNT,   String.format(countFormat, sspSimTriggers[LOCAL]));
            setLocalRowValue(ROW_SSP_BANK_COUNT,  String.format(countFormat, sspBankTriggers[LOCAL]));
            setGlobalRowValue(ROW_RECON_COUNT,    String.format(countFormat, reconSimTriggers[GLOBAL]));
            setGlobalRowValue(ROW_SSP_SIM_COUNT,  String.format(countFormat, sspSimTriggers[GLOBAL]));
            setGlobalRowValue(ROW_SSP_BANK_COUNT, String.format(countFormat, sspBankTriggers[GLOBAL]));
            
            // Update the percentage counters.
            String percentFormat = "%" + spaces + "d / %" + spaces + "d (%7.3f)";
            
            setLocalRowValue(ROW_SSP_EFFICIENCY, String.format(percentFormat, sspMatchedTriggers[LOCAL],
                    sspSimTriggers[LOCAL], (100.0 * sspMatchedTriggers[LOCAL] / sspSimTriggers[LOCAL])));
            setLocalRowValue(ROW_TRIGGER_EFFICIENCY, String.format(percentFormat, reconMatchedTriggers[LOCAL],
                    reconSimTriggers[LOCAL], (100.0 * reconMatchedTriggers[LOCAL] / reconSimTriggers[LOCAL])));
            setGlobalRowValue(ROW_SSP_EFFICIENCY, String.format(percentFormat, sspMatchedTriggers[GLOBAL],
                    sspSimTriggers[GLOBAL], (100.0 * sspMatchedTriggers[GLOBAL] / sspSimTriggers[GLOBAL])));
            setGlobalRowValue(ROW_TRIGGER_EFFICIENCY, String.format(percentFormat, reconMatchedTriggers[GLOBAL],
                    reconSimTriggers[GLOBAL], (100.0 * reconMatchedTriggers[GLOBAL] / reconSimTriggers[GLOBAL])));
            
            int ROW_SECOND_TRIGGER_CUT = ROW_FIRST_TRIGGER_CUT + numCuts + 2;
            for(int cutRow = 0; cutRow < numCuts; cutRow++) {
                setLocalRowValue(cutRow + ROW_FIRST_TRIGGER_CUT, String.format(percentFormat,
                        triggerStats[LOCAL][0].getSSPCutFailures(cutRow), triggerStats[LOCAL][0].getSSPSimulatedTriggers(),
                        (100.0 * triggerStats[LOCAL][0].getSSPCutFailures(cutRow) / triggerStats[LOCAL][0].getSSPSimulatedTriggers())));
                setLocalRowValue(cutRow + ROW_SECOND_TRIGGER_CUT, String.format(percentFormat,
                        triggerStats[LOCAL][1].getSSPCutFailures(cutRow), triggerStats[LOCAL][1].getSSPSimulatedTriggers(),
                        (100.0 * triggerStats[LOCAL][1].getSSPCutFailures(cutRow) / triggerStats[LOCAL][1].getSSPSimulatedTriggers())));
                setGlobalRowValue(cutRow + ROW_FIRST_TRIGGER_CUT, String.format(percentFormat,
                        triggerStats[GLOBAL][0].getSSPCutFailures(cutRow), triggerStats[GLOBAL][0].getSSPSimulatedTriggers(),
                        (100.0 * triggerStats[GLOBAL][0].getSSPCutFailures(cutRow) / triggerStats[GLOBAL][0].getSSPSimulatedTriggers())));
                setGlobalRowValue(cutRow + ROW_SECOND_TRIGGER_CUT, String.format(percentFormat,
                        triggerStats[GLOBAL][1].getSSPCutFailures(cutRow), triggerStats[GLOBAL][1].getSSPSimulatedTriggers(),
                        (100.0 * triggerStats[GLOBAL][1].getSSPCutFailures(cutRow) / triggerStats[GLOBAL][1].getSSPSimulatedTriggers())));
            }
        }
    }
    
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

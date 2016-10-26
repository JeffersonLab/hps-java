package org.hps.analysis.trigger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hps.analysis.trigger.util.LocalOutputLogger;
import org.hps.analysis.trigger.util.PairTrigger;
import org.hps.analysis.trigger.util.SinglesTrigger;
import org.hps.analysis.trigger.util.Trigger;
import org.hps.analysis.trigger.util.TriggerDiagnosticUtil;
import org.hps.record.daqconfig.ConfigurationManager;
import org.hps.record.daqconfig.DAQConfig;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.SSPCluster;
import org.hps.record.triggerbank.SSPData;
import org.hps.record.triggerbank.SSPNumberedTrigger;
import org.hps.record.triggerbank.SSPPairTrigger;
import org.hps.record.triggerbank.SSPSinglesTrigger;
import org.hps.record.triggerbank.TIData;
import org.hps.record.triggerbank.TriggerModule;
import org.hps.util.Pair;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Driver <code>TriggeDiagnosticDriver</code> performs a comparison
 * between the trigger results reported by the hardware and those
 * simulated by the LCSim software for a single trigger.
 * <br/><br/>
 * The driver requires simulated trigger objects, which are produced
 * separately by the driver <code>SimTriggerDriver</code>, in order
 * to function. It also requires the presence of the runtime settings
 * management driver, <code>DAQConfigurationDriver</code>.
 * <br/><br/>
 * The driver works by taking trigger objects simulated by LCSim, using
 * both hardware reported (SSP) clusters and software clusters, and
 * comparing these to the hardware's reported triggers. Reported triggers
 * include a trigger time and which cuts passed. The driver requires
 * that there exists a hardware trigger that matches in each of these
 * fields for every simulated trigger. Note that the reverse may not
 * be true; pulse-clipping can occur for simulated clusters, resulting
 * in a reduced cluster energy, that can affect whether a cluster or
 * pair passes the trigger. This does not occur at the hardware level,
 * though. As such, triggers within the "pulse-clipping" region are not
 * necessarily comparable. The driver ignores these.
 * <br/><br/>
 * Output consists primarily of text printed at the end of the run. The
 * driver outputs the overall efficiency (defined as the number of
 * simulated triggers matched versus the total number) for both cluster
 * types. It also provides a more detailed table that further separates
 * this into efficiency by cluster type and active TI bit. The other
 * output is an efficiency over time plot that shows the efficiency for
 * a programmable time frame throughout the run.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class TriggerDiagnosticDriver extends Driver {
    // === Store the LCIO collection names for the needed objects. ======================
    // ==================================================================================
    /** The LCIO collection containing FADC hits. */
    private String hitCollectionName = "EcalCalHits";
    /** The LCIO collection containing SSP clusters, TI-bits, an
     *  hardware triggers. */
    private String bankCollectionName = "TriggerBank";
    /** The LCIO collection containing all software-simulated triggers. */
    private String simTriggerCollectionName = "SimTriggers";
    
    // === Trigger modules for performing trigger analysis. =============================
    // ==================================================================================
    /** Indicates which TI-bits are active in the current event. Array
     * indices should align with <code>TriggerType.ordinal()</code>.
     * Values set by the method <code>setTIFlags(TIData)</code> and is
     * called by the <code>process(EventHeader)</code> method.*/
    private boolean[] tiFlags = new boolean[6];
    /** Stores the singles trigger settings. Array is of size 2, with
     * each array index corresponding to the trigger of the same trigger
     * number. */
    private TriggerModule[] singlesTrigger = new TriggerModule[2];
    /** Stores the pair trigger settings. Array is of size 2, with each
     * array index corresponding to the trigger of the same trigger number. */
    private TriggerModule[] pairTrigger = new TriggerModule[2];
    
    // === Trigger matching statistics. =================================================
    // ==================================================================================
    private static final int SOURCE_SIM_CLUSTER = 0;
    private static final int SOURCE_SSP_CLUSTER = 1;
    private static final int ALL_TRIGGERS = TriggerType.COSMIC.ordinal() + 1;
    private static final int LOCAL_WINDOW_TRIGGERS = ALL_TRIGGERS + 1;
    /**
     * Stores the total number of simulated triggers observed for each
     * source type. The first array index defines the source type and
     * corresponds to the variables <code>SOURCE_SIM_CLUSTER</code>
     * and <code>SOURCE_SSP_CLUSTER</code>. The second array index
     * defines one of several conditions. This includes triggers seen
     * when a certain TI bit was active, with these indices defined by
     * <code>TriggerType.ordinal()</code>, all triggers seen in general,
     * defined by <code>ALL_TRIGGERS</code>, and all triggers seen in
     * the local window, defined by <code>LOCAL_WINDOW_TRIGGERS</code>.
     */
    private int simTriggerCount[][] = new int[2][LOCAL_WINDOW_TRIGGERS + 1];
    /**
     * Stores the total number of hardware triggers observed for each
     * source type. The first array index defines the source type and
     * corresponds to the variables <code>SOURCE_SIM_CLUSTER</code>
     * and <code>SOURCE_SSP_CLUSTER</code>. The second array index
     * defines one of several conditions. This includes triggers seen
     * when a certain TI bit was active, with these indices defined by
     * <code>TriggerType.ordinal()</code>, all triggers seen in general,
     * defined by <code>ALL_TRIGGERS</code>, and all triggers seen in
     * the local window, defined by <code>LOCAL_WINDOW_TRIGGERS</code>.
     */
    private int hardwareTriggerCount[] = new int[LOCAL_WINDOW_TRIGGERS + 1];
    /**
     * Stores the total number of matched triggers observed for each
     * source type. The first array index defines the source type and
     * corresponds to the variables <code>SOURCE_SIM_CLUSTER</code>
     * and <code>SOURCE_SSP_CLUSTER</code>. The second array index
     * defines one of several conditions. This includes triggers seen
     * when a certain TI bit was active, with these indices defined by
     * <code>TriggerType.ordinal()</code>, all triggers seen in general,
     * defined by <code>ALL_TRIGGERS</code>, and all triggers seen in
     * the local window, defined by <code>LOCAL_WINDOW_TRIGGERS</code>.
     */
    private int matchedTriggerCount[][] = new int[2][LOCAL_WINDOW_TRIGGERS + 1];
    
    // === Verbose settings. ============================================================
    // ==================================================================================
    /** Indicates that all logger messages should be output. */
    private boolean verbose = false;
    /** Indicates that at least one software-cluster simulated trigger
     * failed to verify. This is set during the
     * <code>compareSimulatedToHardware</code> method. */
    private boolean softwareSimFailure = false;
    /** Indicates that at least one hardware-cluster simulated trigger
     * failed to verify. This is set during the
     * <code>compareSimulatedToHardware</code> method. */
    private boolean hardwareSimFailure = false;
    /** Indicates whether the event log should output in the event that
     * a software-cluster simulated trigger fails to verify. */
    private boolean printOnSoftwareSimFailure = false;
    /** Indicates whether the event log should output in the event that
     * a hardware-cluster simulated trigger fails to verify. */
    private boolean printOnHardwareSimFailure = false;
    /** The event logger. Is used to store detailed event readouts, and
     * if appropriate, print them to the terminal. */
    private LocalOutputLogger logger = new LocalOutputLogger();
    
    // === Verification settings. =======================================================
    // ==================================================================================
    /** The number of samples before a pulse-crossing event to integrate
     * during hit formation. Used to determine the risk of pulse-clipping. */
    private int nsb =  20;
    /** The number of samples after a pulse-crossing event to integrate
     * during hit formation. Used to determine the risk of pulse-clipping. */
    private int nsa = 100;
    /** The width of the pulse integration window used to form hits.
     * Used to determine the risk of pulse-clipping. */
    private int windowWidth = 200;
    /** The number of hits that must be present in event in order for
     * it to be ignored as a "noise event." */
    private int noiseEventThreshold = 50;
    /** Indicates the type of trigger that is being tested. */
    private TriggerType triggerType = null;
    /** Whether events with more than <code>noiseEventThreshold</code>
     * hits should be skipped. */
    private boolean skipNoiseEvents = false;
    
    // === Local window values. =========================================================
    // ==================================================================================
    /** Defines the length of time over which statistics are collected
     * in order to produce an entry into the efficiency over time plot.
     * Units are in nanoseconds. */
    private long localWindowSize = 5000000;
    /** Tracks the current time of the current event for the purpose of
     * identifying the end of a local sample. */
    private long localEndTime = Long.MIN_VALUE;
    /** Tracks the start time of the current local sample. */
    private long localStartTime = Long.MIN_VALUE;
    /** Tracks the start time of the first observed event. */
    private long firstStartTime = Long.MIN_VALUE;
    private static final int LOCAL_ALL_TRIGGERS = 0;
    private static final int LOCAL_MATCHED_TRIGGERS = 1;
    /** 
     * Stores entries for the efficiency over time plot. The first entry
     * in the pair represents the time of the local sample. This is
     * defined with respect to the start of the first observed event.
     * The second entry contains the observed and matched triggers for
     * both trigger source types. The first array index defines the
     * source type and corresponds to the index variables defined in
     * <code>SOURCE_SIM_CLUSTER</code> and <code>SOURCE_SSP_CLUSTER</code>.
     * The second array index defines to observed and matched triggers,
     * and corresponds to the indices defined by the variables
     * <code>LOCAL_ALL_TRIGGERS</code> and <code>LOCAL_MATCHED_TRIGGERS</code>.
     */
    private List<Pair<Long, int[][]>> efficiencyPlotEntries = new ArrayList<Pair<Long, int[][]>>();
    
    
    /**
     * Enumerable <code>TriggerType</code> represents the supported
     * types of trigger for the HPS experiment. It also provides a means
     * to determine the trigger number as an <code>int</code>, where
     * applicable, and a human-readable trigger name.
     * <br/><br/>
     * <code>TriggerType.ordinal()</code> is also used as an index for
     * multiple value-tracking arrays throughout the class.
     * 
     * @author Kyle McCarty <mccarty@jlab.org>
     */
    private enum TriggerType {
        // Define the trigger types.
        SINGLES0("Singles 0", 0), SINGLES1("Singles 1", 1), PAIR0("Pair 0", 0), PAIR1("Pair 1", 1), PULSER("Pulser"), COSMIC("Cosmic");
        
        // Store trigger type data.
        private String name = null;
        private int triggerNum = -1;
        
        /**
         * Instantiates a trigger type enumerable.
         * @param name - The name of the trigger.
         */
        private TriggerType(String name) { this.name = name; }
        
        /**
         * Instantiates a trigger type enumerable.
         * @param name - The name of the trigger.
         * @param triggerNum - The trigger number.
         */
        private TriggerType(String name, int triggerNum) {
            this.name = name;
            this.triggerNum = triggerNum;
        }
        
        /**
         * Indicates whether this trigger type is a singles trigger.
         * @return Returns <code>true</code> if the trigger is of type
         * <code>TriggerType.SINGLES0</code> or
         * <code>TriggerType.SINGLES1</code>. Otherwise, returns
         * <code>false</code>.
         */
        public boolean isSinglesTrigger() { return (this.equals(SINGLES0) || this.equals(SINGLES1)); }
        
        /**
         * Gets the trigger number for this trigger type.
         * @return Returns either <code>0</code> or <code>1</code> as
         * appropriate for singles and pair trigger types. For cosmic
         * and pulser trigger types, returns <code>-1</code>.
         */
        public int getTriggerNumber() { return triggerNum; }
        
        @Override
        public String toString() { return name; }
    }
    
    /**
     * Outputs the global efficiency data.
     */
    @Override
    public void endOfData() {
        // Get the number of digits in the largest value that is to be
        // displayed.
        int largestValue = max(hardwareTriggerCount[ALL_TRIGGERS], simTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS],
                simTriggerCount[SOURCE_SSP_CLUSTER][ALL_TRIGGERS], matchedTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS],
                matchedTriggerCount[SOURCE_SSP_CLUSTER][ALL_TRIGGERS]);
        int maxChars = getDigits(largestValue);
        String charDisplay = "%" + maxChars + "d";
        
        // Calculate the efficiencies and determine the display value.
        double[] efficiency = new double[2];
        String[] efficiencyDisp = new String[2];
        for(int i = SOURCE_SIM_CLUSTER; i <= SOURCE_SSP_CLUSTER; i++) {
            // Calculate the efficiency. This is defined as the number
            // triggers which were matched, versus the number that are
            // expected to have matched.
            efficiency[i] = 100.0 * matchedTriggerCount[i][ALL_TRIGGERS] / simTriggerCount[i][ALL_TRIGGERS];
            
            // If there were no triggers, then the efficiency is not
            // defined. Display "N/A."
            if(Double.isNaN(efficiency[i])) { efficiencyDisp[i] = "  N/A  "; }
            
            // Otherwise, display the value as a percentage.
            else { efficiencyDisp[i] = String.format("%5.3f%%", efficiency[i]); }
        }
        
        // Output the trigger efficiency statistics header.
        System.out.println();
        System.out.println();
        System.out.println("======================================================================");
        System.out.println("=== Trigger Efficiency - " + triggerType.toString() + " " + generateLine(44 - triggerType.toString().length()));
        System.out.println("======================================================================");
        
        // Output the global trigger statistics.
        System.out.println("Global Efficiency:");
        System.out.printf("Total Hardware Triggers       :: " + charDisplay + "%n", hardwareTriggerCount[ALL_TRIGGERS]);
        System.out.printf("Total Software Sim Triggers   :: " + charDisplay + "%n", simTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS]);
        System.out.printf("Total Hardware Sim Triggers   :: " + charDisplay + "%n", simTriggerCount[SOURCE_SSP_CLUSTER][ALL_TRIGGERS]);
        System.out.printf("Matched Software Sim Triggers :: " + charDisplay + "%n", matchedTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS]);
        System.out.printf("Matched Hardware Sim Triggers :: " + charDisplay + "%n", matchedTriggerCount[SOURCE_SSP_CLUSTER][ALL_TRIGGERS]);
        System.out.printf("Software Sim Efficiency       :: " + charDisplay + " / " + charDisplay + " (%s)%n",
                simTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS], matchedTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS],
                efficiencyDisp[SOURCE_SIM_CLUSTER]);
        System.out.printf("Hardware Sim Efficiency       :: " + charDisplay + " / " + charDisplay + " (%s)%n",
                simTriggerCount[SOURCE_SSP_CLUSTER][ALL_TRIGGERS], matchedTriggerCount[SOURCE_SSP_CLUSTER][ALL_TRIGGERS],
                efficiencyDisp[SOURCE_SSP_CLUSTER]);
        
        // Get the largest number of spaces needed to display the TI
        // bit specific values.
        int tiMaxValue = Integer.MIN_VALUE;
        for(TriggerType trigger : TriggerType.values()) {
            tiMaxValue = max(tiMaxValue, simTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()],
                    matchedTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()], simTriggerCount[SOURCE_SSP_CLUSTER][trigger.ordinal()],
                    matchedTriggerCount[SOURCE_SSP_CLUSTER][trigger.ordinal()]);
        }
        int tiMaxChars = getDigits(tiMaxValue);
        
        // Define the column width and column headers for the TI-bit
        // specific efficiencies.
        int[] columnWidth = new int[3];
        String[] header = { "TI Bit", "Software Sim Efficiency", "Hardware Sim Efficiency" };
        
        // Determine the width of the first column. This displays the
        // name of the TI bit with respect to which the efficiency is
        // displayed. It should be equal to either the longest trigger
        // name or the header width, whichever is longer.
        columnWidth[0] = header[0].length();
        for(TriggerType trigger : TriggerType.values()) {
            columnWidth[0] = max(columnWidth[0], trigger.toString().length());
        }
        
        // The second and third columns display the total number of
        // matched triggers versus the total number of expected triggers
        // as well as the percentage of two. This takes the form of the
        // string [MATCHED] / [EXPECTED] (NNN.NN%). The width needed
        // to display this will vary based on the numbers of triggers.
        // The width should be set to wither the largest size of the
        // largest of these values, or to the width of the header text.
        // whichever is larger.
        columnWidth[1] = max(header[1].length(), tiMaxChars + tiMaxChars + 14);
        columnWidth[2] = max(header[2].length(), tiMaxChars + tiMaxChars + 14);
        
        // Finally, define the column size strings and the individual
        // value size strings.
        String valueString = "%" + tiMaxChars + "d";
        String efficiencyString = valueString + " / " + valueString + "  (%7s)";
        String[] columnString = { "%-" + columnWidth[0] + "s", "%-" + columnWidth[1] + "s", "%-" + columnWidth[2] + "s" };
        
        // Output the efficiency as a function of active TI bit.
        System.out.println("\n\nTI-Bit Efficiency:");
        System.out.printf("\t" + columnString[0] + "   " + columnString[1] + "   " + columnString[2] + "%n", header[0], header[1], header[2]);
        for(TriggerType trigger : TriggerType.values()) {
            // Calculate the efficiencies for the second and third columns.
            double softwareSimEfficiency = 100.0 * matchedTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()]
                    / simTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()];
            double hardwareSimEfficiency = 100.0 * matchedTriggerCount[SOURCE_SSP_CLUSTER][trigger.ordinal()]
                    / simTriggerCount[SOURCE_SSP_CLUSTER][trigger.ordinal()];
            
            // The efficiency value strings are either the efficiency
            // rounded to two decimal points, or "N/A" if there were
            // no triggers found.
            String softwareSimString = Double.isNaN(softwareSimEfficiency) ? "  N/A  " : String.format("%6.2f%%", softwareSimEfficiency);
            String hardwareSimString = Double.isNaN(hardwareSimEfficiency) ? "  N/A  " : String.format("%6.2f%%", hardwareSimEfficiency);
            
            // The efficiency column strings take the form "[MATCHED] /
            // [EXPECTED] (NNN.NN%)".
            String softwareSimColumn = String.format(efficiencyString, matchedTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()],
                    simTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()], softwareSimString);
            String hardwareSimColumn = String.format(efficiencyString, matchedTriggerCount[SOURCE_SSP_CLUSTER][trigger.ordinal()],
                    simTriggerCount[SOURCE_SSP_CLUSTER][trigger.ordinal()], hardwareSimString);
            
            // Create the efficiency string
            System.out.printf("\t" + columnString[0] + "   " + columnString[1] + "   " + columnString[2] + "%n", trigger.toString(),
                    softwareSimColumn, hardwareSimColumn);
        }
        
        // Create and populate the efficiency over time plot.
        final String moduleHeader = "Trigger Diagnostics/Trigger Verification/" + triggerType.toString() + "/";
        AIDA.defaultInstance().cloud2D(moduleHeader + "Software Sim Trigger Efficiency", efficiencyPlotEntries.size());
        AIDA.defaultInstance().cloud2D(moduleHeader + "Hardware Sim Trigger Efficiency", efficiencyPlotEntries.size());
        for(Pair<Long, int[][]> entry : efficiencyPlotEntries) {
            // Calculate the value for each type of efficiency.
            double softwareEfficiency = 100.0 * entry.getSecondElement()[SOURCE_SIM_CLUSTER][LOCAL_MATCHED_TRIGGERS]
                    / entry.getSecondElement()[SOURCE_SIM_CLUSTER][LOCAL_ALL_TRIGGERS];
            double hardwareEfficiency = 100.0 * entry.getSecondElement()[SOURCE_SSP_CLUSTER][LOCAL_MATCHED_TRIGGERS]
                    / entry.getSecondElement()[SOURCE_SSP_CLUSTER][LOCAL_ALL_TRIGGERS];
            
            // Convert the time to units of seconds.
            long time = Math.round(entry.getFirstElement() / 1000000.0);
            
            // If the value is properly defined, add it to the plot.
            if(!Double.isNaN(softwareEfficiency)) {
                AIDA.defaultInstance().cloud2D(moduleHeader + "Software Sim Trigger Efficiency").fill(time, softwareEfficiency);
            } if(!Double.isNaN(hardwareEfficiency)) {
                AIDA.defaultInstance().cloud2D(moduleHeader + "Hardware Sim Trigger Efficiency").fill(time, hardwareEfficiency);
            }
        }
    }
    
    /**
     * Processes an event and performs trigger verification. Method
     * will only run if the <code>ConfigurationManager</code> has been
     * initialized. Handles testing for noise events and extracting
     * basic event data for analysis, and also ensuring that everything
     * necessary is present. Event trigger data is actually processed
     * separately by the method <code>triggerVerification</code>. Method
     * also handles logger output.
     * @param event - The data object containing all event data.
     */
    @Override
    public void process(EventHeader event) {
        // If the DAQ configuration manager is not yet initialized,
        // it is not possible to analyze the event.
        if(!ConfigurationManager.isInitialized()) {
            return;
        }
        
        // Clear the logger at the start of each event.
        logger.clearLog();
        
        // If this is the first event, set the starting time stamp.
        // The end time is always the time of the current event.
        localEndTime = event.getTimeStamp();
        if(localStartTime == Long.MIN_VALUE) {
            localStartTime = event.getTimeStamp();
            firstStartTime = event.getTimeStamp();
        }
        
        // Output the trigger diagnostic header.
        logger.printNewLine(2);
        logger.println("======================================================================");
        logger.println("=== Trigger Diagnostics - " + triggerType.toString() + " " + generateLine(43 - triggerType.toString().length()));
        logger.println("======================================================================");
        
        // Output basic event information.
        logger.printf("Event Number :: %d%n", event.getEventNumber());
        logger.printf("Event Time   :: %d%n", event.getTimeStamp());
        
        // ==========================================================
        // ==== Obtain calorimeter hits =============================
        // ==========================================================
        
        // Calorimeter hits are only relevant if noise events are to
        // be skipped. Otherwise, the calorimeter hits needn't be defined.
        if(skipNoiseEvents) {
            // Get the calorimeter hits.
            List<CalorimeterHit> hits = getCalorimeterHits(event, hitCollectionName);
            
            // Noise culling can not be performed if there are no hits
            // present in the event.
            if(hits == null) {
                System.err.println("TriggerDiagnostics :: Skipping event; no hit data found.");
                if(verbose) { logger.printLog(); }
                return;
            }
            
            // If noise event culling is enabled, check if this is a noise
            // event.
            if(skipNoiseEvents) {
                if(hits.size() >= noiseEventThreshold) {
                    logger.println("TriggerDiagnostics: Event exceeds the noise threshold for total number of hits and will be skipped.");
                    if(verbose) { logger.printLog(); }
                    return;
                }
            }
        }
        
        // ==========================================================
        // ==== Obtain SSP and TI Banks =============================
        // ==========================================================
        
        // If there is no bank data, this event can not be analyzed.
        if(!event.hasCollection(GenericObject.class, bankCollectionName)) {
            System.err.println("TriggerDiagnostics :: Skipping event; no bank data found.");
            if(verbose) { logger.printLog(); }
            return;
        }
        
        // Get the SSP and TI banks. The TI bank stores TI bits, which
        // are used for trigger-type efficiency values. The SSP bank
        // contains hardware triggers and hardware clusters.
        Pair<TIData, SSPData> banks = getBanks(event, bankCollectionName);
        
        // Check that all of the required objects are present.
        if(banks.getSecondElement() == null) {
            System.err.println("TriggerDiagnostics: Skipping event; no SSP bank found for this event.");
            if(verbose) { logger.printLog(); }
            return;
        } if(banks.getFirstElement() == null) {
            System.err.println("TriggerDiagnostics: Skipping event; no TI bank found for this event.");
            if(verbose) { logger.printLog(); }
            return;
        }
        
        // Extract the active TI bits and make sure that at least one
        // is set.
        boolean activeBitRead = setTIFlags(banks.getFirstElement());
        if(!activeBitRead) {
            System.err.println("TriggerDiagnostics: Skipping event; no TI trigger bits are active.");
            if(verbose) { logger.printLog(); }
            return;
        }
        
        
        
        // ==========================================================
        // ==== Obtain Simulated Triggers ===========================
        // ==========================================================
        
        // If the simulated trigger collection does not exist, analysis
        // can not be performed.
        if(!event.hasCollection(SimTriggerData.class, simTriggerCollectionName)) {
            System.err.println("TriggerDiagnostics: Skipping event; no simulated triggers found.");
            if(verbose) { logger.printLog(); }
            return;
        }
        
        // Get the simulated trigger module.
        List<SimTriggerData> stdList = event.get(SimTriggerData.class, simTriggerCollectionName);
        SimTriggerData triggerData = stdList.get(0);
        
        // Engage the trigger verification.
        triggerVerification(triggerData, banks.getSecondElement(), triggerType);
        
        // If the local time window has elapsed, update the efficiency
        // plot values and reset the counters.
        if(localEndTime - localStartTime >= localWindowSize) {
            // Values are stored in an array within a list. Each array
            // index corresponds to specific value and each array entry
            // corresponds to an individual local window.
            int[][] databank = new int[2][2];
            databank[SOURCE_SIM_CLUSTER][LOCAL_ALL_TRIGGERS] = simTriggerCount[SOURCE_SIM_CLUSTER][LOCAL_WINDOW_TRIGGERS];
            databank[SOURCE_SSP_CLUSTER][LOCAL_ALL_TRIGGERS] = simTriggerCount[SOURCE_SSP_CLUSTER][LOCAL_WINDOW_TRIGGERS];
            databank[SOURCE_SIM_CLUSTER][LOCAL_MATCHED_TRIGGERS] = matchedTriggerCount[SOURCE_SIM_CLUSTER][LOCAL_WINDOW_TRIGGERS];
            databank[SOURCE_SSP_CLUSTER][LOCAL_MATCHED_TRIGGERS] = matchedTriggerCount[SOURCE_SSP_CLUSTER][LOCAL_WINDOW_TRIGGERS];
            
            // The efficiency list also stores the time that corresponds
            // to the particular databank. This is defined as half way
            // between the start and end time of the current window.
            Long time = localEndTime - firstStartTime - ((localEndTime - localStartTime) / 2);
            
            // Store the databank in the list.
            efficiencyPlotEntries.add(new Pair<Long, int[][]>(time, databank));
            
            // Reset all of the counters.
            hardwareTriggerCount[LOCAL_WINDOW_TRIGGERS] = 0;
            simTriggerCount[SOURCE_SIM_CLUSTER][LOCAL_WINDOW_TRIGGERS] = 0;
            simTriggerCount[SOURCE_SSP_CLUSTER][LOCAL_WINDOW_TRIGGERS] = 0;
            matchedTriggerCount[SOURCE_SIM_CLUSTER][LOCAL_WINDOW_TRIGGERS] = 0;
            matchedTriggerCount[SOURCE_SSP_CLUSTER][LOCAL_WINDOW_TRIGGERS] = 0;
            
            // The new window start time is the current event time.
            localStartTime = event.getTimeStamp();
        }
        
        // If appropriate, output the logger text.
        if(verbose || (printOnSoftwareSimFailure && softwareSimFailure) || (printOnHardwareSimFailure && hardwareSimFailure)) {
            logger.printLog();
        }
        
        // Reset the event failure flags.
        softwareSimFailure = false;
        hardwareSimFailure = false;
    }
    
    /**
     * Connects the driver to the <code>ConfigurationManager</code> to
     * obtain trigger settings.
     */
    @Override
    public void startOfData() {
        // If the DAQ configuration should be read, attach a listener
        // to track when it updates.
        ConfigurationManager.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the DAQ configuration.
                DAQConfig daq = ConfigurationManager.getInstance();
                
                // Load the DAQ settings from the configuration manager.
                singlesTrigger[0].loadDAQConfiguration(daq.getSSPConfig().getSingles1Config());
                singlesTrigger[1].loadDAQConfiguration(daq.getSSPConfig().getSingles2Config());
                pairTrigger[0].loadDAQConfiguration(daq.getSSPConfig().getPair1Config());
                pairTrigger[1].loadDAQConfiguration(daq.getSSPConfig().getPair2Config());
                nsa = daq.getFADCConfig().getNSA();
                nsb = daq.getFADCConfig().getNSB();
                windowWidth = daq.getFADCConfig().getWindowWidth();
            }
        });
        
        // Define the first singles trigger.
        singlesTrigger[0] = new TriggerModule();
        singlesTrigger[0].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW, 0.500);
        singlesTrigger[0].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH, 8.191);
        singlesTrigger[0].setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW, 0);
        
        // Define the second singles trigger.
        singlesTrigger[1] = new TriggerModule();
        singlesTrigger[1].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW, 0.000);
        singlesTrigger[1].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH, 8.191);
        singlesTrigger[1].setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW, 0);
        
        // Define the first pairs trigger.
        pairTrigger[0] = new TriggerModule();
        pairTrigger[0].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW, 0.000);
        pairTrigger[0].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH, 8.191);
        pairTrigger[0].setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW, 0);
        pairTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW, 0.000);
        pairTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH, 8.191);
        pairTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH, 8.191);
        pairTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW, 0.000);
        pairTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_F, 0.001);
        pairTrigger[0].setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH, 180);
        pairTrigger[0].setCutValue(TriggerModule.PAIR_TIME_COINCIDENCE, 8);
        
        // Define the second pairs trigger.
        pairTrigger[1] = new TriggerModule();
        pairTrigger[1].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW, 0.000);
        pairTrigger[1].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH, 8.191);
        pairTrigger[1].setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW, 0);
        pairTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW, 0.000);
        pairTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH, 8.191);
        pairTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH, 8.191);
        pairTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW, 0.000);
        pairTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_F, 0.001);
        pairTrigger[1].setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH, 180);
        pairTrigger[1].setCutValue(TriggerModule.PAIR_TIME_COINCIDENCE, 8);
    }
    
    /**
     * Compares a collection of simulated triggers to a collection of
     * triggers reported by the hardware. The simulated triggers may
     * be simulated from either SSP clusters or clusters built by the
     * software.
     * @param simTriggers - A collection containing <code>Trigger</code>
     * objects. The source objects for the <code>Trigger</code> objects
     * may be either <code>Cluster</code> or <code>SSPCluster</code>, as
     * well as a size two array of either of the above.
     * @param hardwareTriggers - A collection of SSP hardware triggers.
     * These must be of type <code>SSPNumberedTrigger</code>.
     * @param clusterType - Specifies which of the four valid object
     * types is used as the source of the <code>Trigger</code> objects
     * defined in the <code>simTriggers</code> argument.
     * @return Returns the number of simulated triggers which were
     * successfully matched to hardware triggers.
     */
    private int compareSimulatedToHardware(Collection<Trigger<?>> simTriggers, Collection<SSPNumberedTrigger> hardwareTriggers, Class<?> clusterType) {
        // Print out the appropriate sub-header.
        logger.printNewLine(2);
        if(clusterType == Cluster.class || clusterType == Cluster[].class) {
            logger.println("==== Simulated Sim Trigger to Hardware Trigger Verification ==========");
        } else {
            logger.println("==== Hardware Sim Trigger to Hardware Trigger Verification ===========");
        }
        
        // Trigger matches must be one-to-one. Track which hardware
        // triggers are already matched do that they are not matched
        // twice.
        Set<SSPNumberedTrigger> matchedTriggers = new HashSet<SSPNumberedTrigger>();
        
        // Iterate over each trigger simulated from hardware clusters.
        // It is expected that each of these triggers will correspond
        // to an existing hardware trigger, since, purportedly, these
        // are the same clusters from which the hardware triggers are
        // generated.
        simLoop:
        for(Trigger<?> simTrigger : simTriggers) {
            // Since hardware triggers do not retain the triggering
            // cluster, the only way to compare triggers is by the
            // time stamp and the cut results. In order to be verified,
            // a trigger must have all of these values match. The time
            // of a singles trigger is the time of the cluster which
            // created it.
            double simTime = getTriggerTime(simTrigger);
            
            // Output the current trigger that is being matched.
            logger.printf("Matching Trigger %s%n", getTriggerText(simTrigger, true));
            
            // Iterate over the hardware triggers and look for one that
            // matches all of the simulated trigger's values.
            hardwareLoop:
            for(SSPNumberedTrigger hardwareTrigger : hardwareTriggers) {
                // Output the comparison hardware trigger.
                logger.printf("\t%s", getTriggerText(hardwareTrigger));
                
                // If the current trigger has already been matched,
                // then skip over it.
                if(matchedTriggers.contains(hardwareTrigger)) {
                    logger.printf(" [ fail; matched      ]%n");
                    continue hardwareLoop;
                }
                
                // The triggers must occur at the same time to classify
                // as a match.
                if(hardwareTrigger.getTime() != simTime) {
                    logger.printf(" [ fail; time         ]%n");
                    continue hardwareLoop;
                }
                
                // Cut comparisons are fundamentally different between
                // singles and pair triggers, so these must be handled
                // separately.
                if(isSinglesTrigger(simTrigger) && hardwareTrigger instanceof SSPSinglesTrigger) {
                    // Cast both the triggers to pair trigger objects.
                    SinglesTrigger<?> simSinglesTrigger = (SinglesTrigger<?>) simTrigger;
                    SSPSinglesTrigger hardwareSinglesTrigger = (SSPSinglesTrigger) hardwareTrigger;
                    
                    // Since there is no seed hit information for the
                    // hardware cluster, we just assume that this cut
                    // matches. Instead, move to the cluster energy
                    // lower bound cut.
                    if(hardwareSinglesTrigger.passCutEnergyMin() != simSinglesTrigger.getStateClusterEnergyLow()) {
                        logger.printf(" [ fail; energy low  ]%n");
                        continue hardwareLoop;
                    }
                    
                    // Next, check the cluster energy upper bound cut.
                    if(hardwareSinglesTrigger.passCutEnergyMax() != simSinglesTrigger.getStateClusterEnergyHigh()) {
                        logger.printf(" [ fail; energy high ]%n");
                        continue hardwareLoop;
                    }
                    
                    // Lastly, check if the cluster hit count cut matches.
                    if(hardwareSinglesTrigger.passCutHitCount() != simSinglesTrigger.getStateHitCount()) {
                        logger.printf(" [ fail; hit count   ]%n");
                        continue hardwareLoop;
                    }
                } else if(isPairTrigger(simTrigger) && hardwareTrigger instanceof SSPPairTrigger) {
                    // Cast both the triggers to pair trigger objects.
                    PairTrigger<?> simPairTrigger = (PairTrigger<?>) simTrigger;
                    SSPPairTrigger hardwarePairTrigger = (SSPPairTrigger) hardwareTrigger;
                    
                    // Since there is no singles cuts data for the
                    // hardware trigger, we just assume that these cuts
                    // match. Move to the pair energy sum cut.
                    if(hardwarePairTrigger.passCutEnergySum() != simPairTrigger.getStateEnergySum()) {
                        logger.printf(" [ fail; sum          ]%n");
                        continue hardwareLoop;
                    }
                    
                    // Next, check the energy difference cut.
                    if(hardwarePairTrigger.passCutEnergyDifference() != simPairTrigger.getStateEnergyDifference()) {
                        logger.printf(" [ fail; difference   ]%n");
                        continue hardwareLoop;
                    }
                    
                    // Next, check the energy slope cut.
                    if(hardwarePairTrigger.passCutEnergySlope() != simPairTrigger.getStateEnergySlope()) {
                        logger.printf(" [ fail; slope        ]%n");
                        continue hardwareLoop;
                    }
                    
                    // Lastly, check the coplanarity cut.
                    if(hardwarePairTrigger.passCutCoplanarity() != simPairTrigger.getStateCoplanarity()) {
                        logger.printf(" [ fail; coplanarity  ]%n");
                        continue hardwareLoop;
                    }
                } else {
                    throw new IllegalArgumentException("Trigger type is unrecongnized or simulated and " +
                            "hardware triggers are of different types.");
                }
                
                // If all three values match, then these triggers are
                // considered a match and verified.
                logger.printf(" [ trigger verified   ]%n");
                matchedTriggers.add(hardwareTrigger);
                
                // Update the verified count for each type of trigger
                // for the local and global windows.
                if(clusterType == Cluster.class || clusterType == Cluster[].class) {
                    matchedTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS]++;
                    matchedTriggerCount[SOURCE_SIM_CLUSTER][LOCAL_WINDOW_TRIGGERS]++;
                } else {
                    matchedTriggerCount[SOURCE_SSP_CLUSTER][ALL_TRIGGERS]++;
                    matchedTriggerCount[SOURCE_SSP_CLUSTER][LOCAL_WINDOW_TRIGGERS]++;
                }
                
                // Update the verified count for each active TI bit.
                for(TriggerType trigger : TriggerType.values()) {
                    if(tiFlags[trigger.ordinal()]) {
                        if(clusterType == Cluster.class || clusterType == Cluster[].class) {
                            matchedTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()]++;
                        } else {
                            matchedTriggerCount[SOURCE_SSP_CLUSTER][trigger.ordinal()]++;
                        }
                    }
                }
                
                // A trigger can only be matched once, so no further
                // matching is necessary.
                continue simLoop;
            }
            
            // If this point is reached, all possible hardware triggers
            // have been checked and failed to match. This trigger then
            // fails to verify.
            logger.println("\t\tVerification failed!");
        }
        
        // The matched trigger set is equivalent in size to the number
        // of triggers that successfully passed verification.
        return matchedTriggers.size();
    }
    
    /**
     * Generates a <code>String</code> consisting of the indicated
     * number of '=' characters.
     * @param length - The number of characters that should be present
     * in the <code>String</code>.
     * @return Returns a <code>String</code> of '=' characters.
     */
    private static final String generateLine(int length) {
        StringBuffer buffer = new StringBuffer();
        for(int i = 0; i < length; i++) {
            buffer.append('=');
        }
        return buffer.toString();
    }
    
    /**
     * Gets the TI bank and the SSP bank from an LCIO collection of
     * bank objects.
     * @param event - The event containing the LCIO collections.
     * @param bankCollectionName - The name of bank collection.
     * @return Returns a <code>Pair</code> object where the first element
     * is the TI bank object and the second is the SSP bank object.
     */
    private static Pair<TIData, SSPData> getBanks(EventHeader event, String bankCollectionName) {
        // Get the SSP and TI banks. The TI bank stores TI bits, which
        // are used for trigger-type efficiency values. The SSP bank
        // contains hardware triggers and hardware clusters.
        TIData tiBank = null;
        SSPData sspBank = null;
        
        // The TI and SSP banks are stored as generic objects. They
        // must be extracted from the list and parsed into the correct
        // format.
        List<GenericObject> bankList = event.get(GenericObject.class, bankCollectionName);
        for(GenericObject obj : bankList) {
            // If this is an SSP bank, parse it into an SSP bank object.
            if(AbstractIntData.getTag(obj) == SSPData.BANK_TAG) {
                sspBank = new SSPData(obj);
            }
            
            // Otherwise, if this is a TI bank, convert it into the
            // correct object format.
            else if(AbstractIntData.getTag(obj) == TIData.BANK_TAG) {
                tiBank = new TIData(obj);
            }
        }
        
        // Return the parsed banks.
        return new Pair<TIData, SSPData>(tiBank, sspBank);
    }
    
    /**
     * Gets the a of <code>CalorimeterHit</code> objects from the
     * argument event from the LCIO collection <code>hitCollectionName</code>.
     * @param event - The event object.
     * @param hitCollectionName - The name of the hit collection.
     * @return Returns either a list of <code>CalorimeterHit</code>
     * objects, or returns <code>null</code> if no collection was found.
     */
    private static final List<CalorimeterHit> getCalorimeterHits(EventHeader event, String hitCollectionName) {
        // Get the list of calorimeter hits from the event.
        List<CalorimeterHit> fadcHits = null;
        if(event.hasCollection(CalorimeterHit.class, hitCollectionName)) {
            fadcHits = event.get(CalorimeterHit.class, hitCollectionName);
        }
        
        // Return the FADC hit collection.
        return fadcHits;
    }
    
    /**
     * Gets the number of digits in the base-10 String representation
     * of an integer primitive. Negative signs are not included in the
     * digit count.
     * @param value - The value of which to obtain the length.
     * @return Returns the number of digits in the String representation
     * of the argument value.
     */
    private static final int getDigits(int value) {
        if(value < 0) { return Integer.toString(value).length() - 1; }
        else { return Integer.toString(value).length(); }
    }
    
    /**
     * A helper method associated with <code>getTriggerTime</code> that
     * handles pair triggers, which have either <code>Cluster[]</code>
     * or <code>SSPCluster[]</code> objects as their source type. <b>This
     * method should not be called directly.</b>
     * @param trigger - The trigger object for which to obtain the
     * trigger time.
     * @return Returns the trigger time as a <code>double</code>.
     * @throws IllegalArgumentException Occurs if the trigger source
     * object type is anything other than <code>Cluster[]</code> or
     * <code>SSPCluster[]</code>.
     */
    private static final double getPairTriggerTime(Trigger<?> trigger) throws IllegalArgumentException {
        // Get the cluster times and y positions as appropriate to the
        // cluster object type.
        int[] y = new int[2];
        double[] time = new double[2];
        if(trigger.getTriggerSource() instanceof SSPCluster[] && ((SSPCluster[]) trigger.getTriggerSource()).length == 2) {
            SSPCluster[] pair = (SSPCluster[]) trigger.getTriggerSource();
            for(int i = 0; i < 2; i++) {
                y[i] = TriggerModule.getClusterYIndex(pair[i]);
                time[i] = TriggerModule.getClusterTime(pair[i]);
            }
        } else if(trigger.getTriggerSource() instanceof Cluster[] && ((Cluster[]) trigger.getTriggerSource()).length == 2) {
            Cluster[] pair = (Cluster[]) trigger.getTriggerSource();
            for(int i = 0; i < 2; i++) {
                y[i] = TriggerModule.getClusterYIndex(pair[i]);
                time[i] = TriggerModule.getClusterTime(pair[i]);
            }
        } else {
            throw new IllegalArgumentException("Trigger source object type is not recognized.");
        }
        
        // The bottom cluster defines the trigger time.
        if(y[0] < 0 && y[1] > 0) {
            return time[0];
        } else if(y[1] < 0 && y[0] > 0) {
            return time[1];
        } else if(y[0] < 0 && y[1] < 0) {
            throw new IllegalArgumentException("Ambiguous cluster pair; both bottom clusters.");
        } else {
            throw new IllegalArgumentException("Ambiguous cluster pair; both top clusters.");
        }
    }
    
    /**
     * A helper method associated with <code>getTriggerTime</code> that
     * handles singles triggers, which have either <code>Cluster</code>
     * or <code>SSPCluster</code> objects as their source type. <b>This
     * method should not be called directly.</b>
     * @param trigger - The trigger object for which to obtain the
     * trigger time.
     * @return Returns the trigger time as a <code>double</code>.
     * @throws IllegalArgumentException Occurs if the trigger source
     * object type is anything other than <code>Cluster</code> or
     * <code>SSPCluster</code>.
     */
    private static final double getSinglesTriggerTime(Trigger<?> trigger) throws IllegalArgumentException {
        // Get the trigger time as appropriate to the source object type.
        if(trigger.getTriggerSource() instanceof SSPCluster) {
            return TriggerModule.getClusterTime((SSPCluster) trigger.getTriggerSource());
        } else if(trigger.getTriggerSource() instanceof Cluster) {
            return TriggerModule.getClusterTime((Cluster) trigger.getTriggerSource());
        } else {
            throw new IllegalArgumentException("Trigger source object type is not recognized.");
        }
    }
    
    /**
     * Gets a textual representation of the trigger source cluster(s).
     * @param trigger - The trigger for which to obtain the textual
     * representation.
     * @return Returns a <code>String</code> array containing a textual
     * representation of the trigger's source cluster(s). Each entry in
     * the array represents an individual cluster.
     * @throws IllegalArgumentException Occurs if the trigger source
     * object type is anything other than <code>Cluster</code>,
     * <code>SSPCluster</code>, or an array of either of these two
     * object types.
     */
    private static final String[] getTriggerSourceText(Trigger<?> trigger) throws IllegalArgumentException {
        if(trigger.getTriggerSource() instanceof Cluster) {
            return new String[] { TriggerDiagnosticUtil.clusterToString((Cluster) trigger.getTriggerSource()) };
        } else if(trigger.getTriggerSource() instanceof SSPCluster) {
            return new String[] { TriggerDiagnosticUtil.clusterToString((SSPCluster) trigger.getTriggerSource()) };
        } else if(trigger.getTriggerSource() instanceof Cluster[]) {
            Cluster[] source = (Cluster[]) trigger.getTriggerSource();
            String[] text = new String[source.length];
            for(int i = 0; i < source.length; i++) {
                text[i] = TriggerDiagnosticUtil.clusterToString(source[i]);
            }
            return text;
        } else if(trigger.getTriggerSource() instanceof SSPCluster[]) {
            SSPCluster[] source = (SSPCluster[]) trigger.getTriggerSource();
            String[] text = new String[source.length];
            for(int i = 0; i < source.length; i++) {
                text[i] = TriggerDiagnosticUtil.clusterToString(source[i]);
            }
            return text;
        } else {
            throw new IllegalArgumentException("Trigger source object type is not recognized.");
        }
    }
    
    /**
     * Gets a textual representation of the trigger.
     * @param trigger - The trigger for which to obtain the textual
     * representation.
     * @param includeSource - Indicates whether a textual representation
     * of the source objects for the trigger should be included. These
     * will be present on a new line, one for each cluster.
     * @return Returns a <code>String</code> object representing the
     * trigger. If <code>includeSource</code> is set to true, this will
     * be more than one line.
     * @throws IllegalArgumentException Occurs if the trigger source
     * object type is anything other than <code>Cluster</code>,
     * <code>SSPCluster</code>, or an array of either of these two
     * object types.
     */
    private static final String getTriggerText(Trigger<?> trigger, boolean includeSource) throws IllegalArgumentException {
        // Define the trigger strings.
        final String singlesString = "t = %3.0f; EMin: %5b; EMax: %5b; Hit: %5b";
        final String doubleString  = "t = %3.0f; EMin: %5b; EMax: %5b; Hit: %5b; Sum: %5b; Diff: %5b; Slope: %5b; Coplanarity: %5b; Time: %5b";
        
        // If this is singles trigger...
        if(isSinglesTrigger(trigger)) {
            SinglesTrigger<?> singlesTrigger = (SinglesTrigger<?>) trigger;
            StringBuffer triggerText = new StringBuffer(String.format(singlesString, getTriggerTime(trigger), singlesTrigger.getStateClusterEnergyLow(),
                    singlesTrigger.getStateClusterEnergyHigh(), singlesTrigger.getStateHitCount()));
            if(includeSource) {
                triggerText.append("\n\t\t" + getTriggerSourceText(trigger)[0]);
            }
            return triggerText.toString();
        }
        
        // If this is a pair trigger...
        if(isPairTrigger(trigger)) {
            PairTrigger<?> pairTrigger = (PairTrigger<?>) trigger;
            StringBuffer triggerText = new StringBuffer(String.format(doubleString, getTriggerTime(trigger), pairTrigger.getStateClusterEnergyLow(),
                    pairTrigger.getStateClusterEnergyHigh(), pairTrigger.getStateHitCount(), pairTrigger.getStateEnergySum(),
                    pairTrigger.getStateEnergyDifference(), pairTrigger.getStateEnergySlope(), pairTrigger.getStateCoplanarity(),
                    pairTrigger.getStateTimeCoincidence()));
            if(includeSource) {
                String[] sourceText = getTriggerSourceText(trigger);
                triggerText.append("\n\t\t" + sourceText[0]);
                triggerText.append("\n\t\t" + sourceText[1]);
            }
            return triggerText.toString();
        }
        
        // Otherwise, the trigger type is invalid.
        else {
            throw new IllegalArgumentException("Trigger source object type is not recognized.");
        }
    }
    
    /**
     * Gets a textual representation of the trigger.
     * @param trigger - The trigger for which to obtain the textual
     * representation.
     * @return Returns a <code>String</code> object representing the
     * trigger.
     * @throws IllegalArgumentException Occurs if the trigger subclass
     * is not either an <code>SSPSinglesTrigger</code> object or an
     * <code>SSPPairTrigger</code> object.
     */
    private static final String getTriggerText(SSPNumberedTrigger trigger) {
        // Define the trigger strings.
        final String singlesString = "t = %3d; EMin: %5b; EMax: %5b; Hit: %5b";
        final String doubleString  = "t = %3d; EMin: %5b; EMax: %5b; Hit: %5b; Sum: %5b; Diff: %5b; Slope: %5b; Coplanarity: %5b; Time: %5b";
        
        // If this is singles trigger...
        if(trigger instanceof SSPSinglesTrigger) {
            SSPSinglesTrigger singlesTrigger = (SSPSinglesTrigger) trigger;
            StringBuffer triggerText = new StringBuffer(String.format(singlesString, singlesTrigger.getTime(),
                    singlesTrigger.passCutEnergyMin(), singlesTrigger.passCutEnergyMax(),
                    singlesTrigger.passCutHitCount()));
            return triggerText.toString();
        }
        
        // If this is a pair trigger...
        else if(trigger instanceof SSPPairTrigger) {
            SSPPairTrigger pairTrigger = (SSPPairTrigger) trigger;
            StringBuffer triggerText = new StringBuffer(String.format(doubleString, trigger.getTime(),
                    true, true, true, pairTrigger.passCutEnergySum(),
                    pairTrigger.passCutEnergyDifference(), pairTrigger.passCutEnergySlope(),
                    pairTrigger.passCutEnergySlope(), pairTrigger.passCutCoplanarity()));
            return triggerText.toString();
        }
        
        // Otherwise, the trigger type is invalid.
        else { throw new IllegalArgumentException("Trigger type is not recognized."); }
    }
    
    /**
     * Gets the trigger time for an arbitrary trigger object as is
     * appropriate to its source object type.
     * @param trigger - The trigger object for which to obtain the
     * trigger time.
     * @return Returns the trigger time as a <code>double</code>.
     * @throws IllegalArgumentException Occurs if the trigger source
     * object type is anything other than <code>Cluster</code>,
     * <code>SSPCluster</code>, or a size-two array of either of the
     * previous two object types.
     */
    private static final double getTriggerTime(Trigger<?> trigger) throws IllegalArgumentException {
        // Pass the trigger to one of the sub-handlers appropriate to
        // its source type.
        if(trigger.getTriggerSource() instanceof Cluster || trigger.getTriggerSource() instanceof SSPCluster) {
            return getSinglesTriggerTime(trigger);
        } else if(trigger.getTriggerSource() instanceof Cluster[] || trigger.getTriggerSource() instanceof SSPCluster[]) {
            return getPairTriggerTime(trigger);
        } else {
            throw new IllegalArgumentException("Trigger source object type is not recognized.");
        }
    }
    
    /**
     * Indicates whether a generic trigger object is a pair trigger.
     * @param trigger - The trigger to check.
     * @return Returns <code>true</code> in the case that the source
     * object is a <code>Cluster[]</code>, or <code>SSPCluster[]</code>.
     * Otherwise, returns <code>false</code>.
     */
    private static final boolean isPairTrigger(Trigger<?> trigger) {
        // Get the size of the trigger source cluster array, if it is
        // an array of a valid type.
        int size = -1;
        if(trigger.getTriggerSource() instanceof Cluster[]) {
            size = ((Cluster[]) trigger.getTriggerSource()).length;
        } else if(trigger.getTriggerSource() instanceof SSPCluster[]) {
            size = ((SSPCluster[]) trigger.getTriggerSource()).length;
        }
        
        // If the source object is either not of the appropriate type
        // or not a size two array, it is not a pair trigger.
        return size == 2;
    }
    
    /**
     * Indicates whether a generic trigger object is a singles trigger.
     * @param trigger - The trigger to check.
     * @return Returns <code>true</code> in the case that the source
     * object is a <code>Cluster</code> or <code>SSPCluster</code>.
     * Otherwise, returns <code>false</code>.
     */
    private static final boolean isSinglesTrigger(Trigger<?> trigger) {
        return trigger.getTriggerSource() instanceof Cluster || trigger.getTriggerSource() instanceof SSPCluster;
    }
    
    /**
     * Gets the maximum value in a set of integers.
     * @param values - The values from which to find the maximum.
     * @return Returns whichever value is the highest from within the
     * set.
     * @throws IllegalArgumentException Occurs if no arguments are given.
     */
    private static final int max(int... values) throws IllegalArgumentException {
        // There must be at least one value.
        if(values.length == 0) { throw new IllegalArgumentException("Must define at least one value."); }
        
        // Determine which value is the largest.
        int maxValue = values[0];
        for(int i = 1; i < values.length; i++) {
            if(values[i] > maxValue) { maxValue = values[i]; }
        }
        
        // Return the result.
        return maxValue;
    }
    
    /**
     * Sets the TI-bit flags for each trigger type and also indicates
     * whether or not at least one TI-bit was found active.
     * @param tiBank - The TI bank from which to set the flags.
     * @return Returns <code>true</code> if at least one TI-bit was
     * found active, and <code>false</code> if no bits were active.
     */
    private boolean setTIFlags(TIData tiBank) {
        // Reset the TI flags and determine track whether some TI bit
        // can be found.
        tiFlags = new boolean[6];
        boolean activeBitRead = false;
        
        // Check each TI bit.
        if(tiBank.isPulserTrigger()) {
            activeBitRead = true;
            tiFlags[TriggerType.PULSER.ordinal()] = true;
            logger.println("Trigger type :: Pulser");
        } if(tiBank.isSingle0Trigger()) {
            activeBitRead = true;
            tiFlags[TriggerType.SINGLES0.ordinal()] = true;
            logger.println("Trigger type :: Singles 0");
        } if(tiBank.isSingle1Trigger()) {
            activeBitRead = true;
            tiFlags[TriggerType.SINGLES1.ordinal()] = true;
            logger.println("Trigger type :: Singles 1");
        } if(tiBank.isPair0Trigger()) {
            activeBitRead = true;
            tiFlags[TriggerType.PAIR0.ordinal()] = true;
            logger.println("Trigger type :: Pair 0");
        } if(tiBank.isPair1Trigger()) {
            activeBitRead = true;
            tiFlags[TriggerType.PAIR1.ordinal()] = true;
            logger.println("Trigger type :: Pair 1");
        } if(tiBank.isCalibTrigger()) {
            activeBitRead = true;
            tiFlags[TriggerType.COSMIC.ordinal()] = true;
            logger.println("Trigger type :: Cosmic");
        }
        
        // Return whether or not a TI bit was found.
        return activeBitRead;
    }
    
    /**
     * Performs trigger verification for the specified trigger.
     * @param simTriggers - A data object containing all simulated
     * triggers for all trigger types.
     * @param sspBank - The data bank containing all of the triggers
     * reported by the hardware.
     * @param triggerType - The trigger which is to be verified.
     */
    private void triggerVerification(SimTriggerData simTriggers, SSPData sspBank, TriggerType triggerType) {
        // Trigger verification can not be performed for either pulser
        // or cosmic triggers. Null trigger types are also invalid.
        if(triggerType == null) {
            throw new IllegalArgumentException("Trigger verification type is not defined.");
        } else if(triggerType == null || triggerType.equals(TriggerType.COSMIC) || triggerType.equals(TriggerType.PULSER)) {
            throw new IllegalArgumentException("Verification for trigger type \"" + triggerType.toString() + "\" is not supported.");
        } else if(!(triggerType.equals(TriggerType.SINGLES0) || triggerType.equals(TriggerType.SINGLES1)
                || triggerType.equals(TriggerType.PAIR0) || triggerType.equals(TriggerType.PAIR1))) {
            throw new IllegalArgumentException("Verification for trigger type is not a known trigger type.");
        }
        
        // Get the SSP triggers for the appropriate trigger type.
        List<SSPNumberedTrigger> hardwareTriggers = new ArrayList<SSPNumberedTrigger>();
        if(triggerType.isSinglesTrigger()) {
            for(SSPSinglesTrigger trigger : sspBank.getSinglesTriggers()) {
                if(trigger.getTriggerNumber() == triggerType.getTriggerNumber()) {
                    hardwareTriggers.add(trigger);
                }
            }
        } else {
            for(SSPPairTrigger trigger : sspBank.getPairTriggers()) {
                if(trigger.getTriggerNumber() == triggerType.getTriggerNumber()) {
                    hardwareTriggers.add(trigger);
                }
            }
        }
        
        // Get the triggers simulated from both hardware SSP clusters
        // and software clusters.
        List<Trigger<?>> hardwareSimTriggers = new ArrayList<Trigger<?>>();
        List<Trigger<?>> softwareSimTriggers = new ArrayList<Trigger<?>>();
        if(triggerType.isSinglesTrigger()) {
            // Add all of the SSP triggers.
            hardwareSimTriggers.addAll(simTriggers.getSimHardwareClusterTriggers().getSinglesTriggers(triggerType.getTriggerNumber()));
            
            // Add only the simulated triggers that were generated
            // from clusters that are not at risk of pulse-clipping.
            for(SinglesTrigger<Cluster> trigger : simTriggers.getSimSoftwareClusterTriggers().getSinglesTriggers(triggerType.getTriggerNumber())) {
                if(TriggerDiagnosticUtil.isVerifiable(trigger.getTriggerSource(), nsa, nsb, windowWidth)) {
                    softwareSimTriggers.add(trigger);
                }
            }
        } else {
            // Add all of the SSP triggers.
            hardwareSimTriggers.addAll(simTriggers.getSimHardwareClusterTriggers().getPairTriggers(triggerType.getTriggerNumber()));
            
            // Add only the simulated triggers that were generated
            // from clusters that are not at risk of pulse-clipping.
            for(PairTrigger<Cluster[]> trigger : simTriggers.getSimSoftwareClusterTriggers().getPairTriggers(triggerType.getTriggerNumber())) {
                if(TriggerDiagnosticUtil.isVerifiable(trigger.getTriggerSource()[0], nsa, nsb, windowWidth)
                        && TriggerDiagnosticUtil.isVerifiable(trigger.getTriggerSource()[1], nsa, nsb, windowWidth)) {
                    softwareSimTriggers.add(trigger);
                }
            }
        }
        
        // Output the trigger objects as text.
        logger.printNewLine(2);
        logger.println("=== Simulated Cluster Simulated Triggers =============================");
        if(!softwareSimTriggers.isEmpty()) {
            for(Trigger<?> trigger : softwareSimTriggers) {
                logger.println(getTriggerText(trigger, true));
            }
        } else { logger.println("None!"); }
        logger.printNewLine(2);
        logger.println("=== Hardware Cluster Simulated Triggers ==============================");
        if(!softwareSimTriggers.isEmpty()) {
            for(Trigger<?> trigger : hardwareSimTriggers) {
                logger.println(getTriggerText(trigger, true));
            }
        } else { logger.println("None!"); }
        logger.printNewLine(2);
        logger.println("=== Hardware Triggers ================================================");
        if(!hardwareTriggers.isEmpty()) {
            for(SSPNumberedTrigger trigger : hardwareTriggers) {
                logger.println(getTriggerText(trigger));
            }
        } else { logger.println("None!"); }
        
        // Update the total count for each type of trigger for the local
        // and global windows.
        hardwareTriggerCount[ALL_TRIGGERS] += hardwareTriggers.size();
        hardwareTriggerCount[LOCAL_WINDOW_TRIGGERS] += hardwareTriggers.size();
        simTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS] += softwareSimTriggers.size();
        simTriggerCount[SOURCE_SSP_CLUSTER][ALL_TRIGGERS] += hardwareSimTriggers.size();
        simTriggerCount[SOURCE_SIM_CLUSTER][LOCAL_WINDOW_TRIGGERS] += softwareSimTriggers.size();
        simTriggerCount[SOURCE_SSP_CLUSTER][LOCAL_WINDOW_TRIGGERS] += hardwareSimTriggers.size();
        
        // Update the total count for each active TI bit.
        for(TriggerType trigger : TriggerType.values()) {
            if(tiFlags[trigger.ordinal()]) {
                hardwareTriggerCount[trigger.ordinal()] += hardwareTriggers.size();
                simTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()] += softwareSimTriggers.size();
                simTriggerCount[SOURCE_SSP_CLUSTER][trigger.ordinal()] += hardwareSimTriggers.size();
            }
        }
        
        // Run the trigger verification for each simulated trigger type.
        logger.printNewLine(2);
        logger.println("=== Performing Trigger Verification ==================================");
        logger.println("======================================================================");
        if(!softwareSimTriggers.isEmpty() && !hardwareTriggers.isEmpty()) {
            if(triggerType.isSinglesTrigger()) {
                // Perform trigger verification for each type of trigger.
                // Also store the number of triggers that are verified.
                int softwareSimVerifiedCount = compareSimulatedToHardware(softwareSimTriggers, hardwareTriggers, Cluster.class);
                int hardwareSimVerifiedCount = compareSimulatedToHardware(hardwareSimTriggers, hardwareTriggers, SSPCluster.class);
                
                // If the number of triggers that verified are not
                // equal to the number of simulated triggers of the
                // same type, then at least one trigger failed.
                if(softwareSimVerifiedCount != softwareSimTriggers.size()) { softwareSimFailure = true; }
                if(hardwareSimVerifiedCount != hardwareSimTriggers.size()) { hardwareSimFailure = true; }
            } else {
                // Perform trigger verification for each type of trigger.
                // Also store the number of triggers that are verified.
                int softwareSimVerifiedCount = compareSimulatedToHardware(softwareSimTriggers, hardwareTriggers, Cluster[].class);
                int hardwareSimVerifiedCount = compareSimulatedToHardware(hardwareSimTriggers, hardwareTriggers, SSPCluster[].class);
                
                // If the number of triggers that verified are not
                // equal to the number of simulated triggers of the
                // same type, then at least one trigger failed.
                if(softwareSimVerifiedCount != softwareSimTriggers.size()) { softwareSimFailure = true; }
                if(hardwareSimVerifiedCount != hardwareSimTriggers.size()) { hardwareSimFailure = true; }
            }
        } else {
            if(softwareSimTriggers.isEmpty() && !hardwareTriggers.isEmpty()) {
                logger.println("No simulated triggers found; verification can not be performed.");
            } else if(!softwareSimTriggers.isEmpty() && hardwareTriggers.isEmpty()) {
                logger.println("No hardware triggers found; verification can not be performed.");
            } else {
                logger.println("No triggers found; verification can not be performed.");
            }
        }
    }
    
    /**
     * Sets the name of LCIO collection containing the trigger banks.
     * @param collection - The collection name.
     */
    public void setBankCollectionName(String collection) {
        bankCollectionName = collection;
    }
    
    /**
     * Sets the name of LCIO collection containing hits.
     * @param collection - The collection name.
     */
    public void setHitCollectionName(String collection) {
        hitCollectionName = collection;
    }
    
    /**
     * Defines the size of the local window for use in the efficiency
     * over time plot. Units are in seconds.
     * @param size - The duration of local efficiency measurements.
     */
    public void setLocalWindowSize(int size) {
        // The setter takes units of seconds, but time stamps are in
        // nanoseconds. Convert the units less computational intense
        // comparisons.
        localWindowSize = size * 1000000;
    }
    
    /**
     * Sets the total number of hits that must be present in an event
     * in order for it to be considered a noise event. This is only
     * applied if <code>skipNoiseEvents</code> is set to <code>true</code>.
     * @param threshold - The noise hit threshold.
     */
    public void setNoiseThreshold(int threshold) {
        noiseEventThreshold = threshold;
    }
    
    /**
     * Indicates whether the event log should be printed when a trigger
     * which was simulated from a hardware-reported (SSP) cluster fails
     * to verify.
     * @param state - <code>true</code> indicates that the event log
     * should be printed, and <code>false</code> that it should not.
     */
    public void setPrintOnHardwareSimFailure(boolean state) {
        printOnHardwareSimFailure = state;
    }
    
    /**
     * Indicates whether the event log should be printed when a trigger
     * which was simulated from a software-constructed cluster fails
     * to verify.
     * @param state - <code>true</code> indicates that the event log
     * should be printed, and <code>false</code> that it should not.
     */
    public void setPrintOnSoftwareSimFailure(boolean state) {
        printOnSoftwareSimFailure = state;
    }
    
    /**
     * Indicates whether events which exceed a certain number of total
     * hits, defined by <code>noiseEventThreshold</code>, should be
     * treated as noise events and skipped.
     * @param state - <code>true</code> causes noise events to be skipped
     * and <code>false</code> does not.
     */
    public void setSkipNoiseEvents(boolean state) {
        skipNoiseEvents = state;
    }
    
    /**
     * Sets the name of LCIO collection containing simulated triggers.
     * @param collection - The collection name.
     */
    public void setTriggerCollectionName(String collection) {
        simTriggerCollectionName = collection;
    }
    
    /**
     * Sets the which of the triggers this driver should verify. This
     * value must be of the following:
     * <ul>
     * <li>SINGLES0</li>
     * <li>SINGLE1</li>
     * <li>PAIR0</li>
     * <li>PAIR1</li>
     * </ul>
     * @param type - The <code>String</code> indicating which trigger
     * should be verified.
     * @throws IllegalArgumentException Occurs if any type name is
     * besides those specified above is used.
     */
    public void setTriggerType(String type) throws IllegalArgumentException {
        if(type.compareTo(TriggerType.SINGLES0.name()) == 0) {
            triggerType = TriggerType.SINGLES0;
        } else if(type.compareTo(TriggerType.SINGLES1.name()) == 0) {
            triggerType = TriggerType.SINGLES1;
        } else if(type.compareTo(TriggerType.PAIR0.name()) == 0) {
            triggerType = TriggerType.PAIR0;
        } else if(type.compareTo(TriggerType.PAIR1.name()) == 0) {
            triggerType = TriggerType.PAIR1;
        } else {
            throw new IllegalArgumentException("Trigger type \"" + type + "\" is not supported.");
        }
    }
    
    /**
     * Sets whether the full event verification summary should be printed
     * on every event or not.
     * @param state - <code>true</code> prints the event summary and
     * <code>false</code> does not.
     */
    public void setVerbose(boolean state) {
        verbose = state;
    }
}
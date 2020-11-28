package org.hps.analysis.trigger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hps.analysis.trigger.util.LocalOutputLogger;
import org.hps.analysis.trigger.util.PairTrigger2019;
import org.hps.analysis.trigger.util.SinglesTrigger2019;
import org.hps.analysis.trigger.util.Trigger;
import org.hps.analysis.trigger.util.TriggerDiagnosticUtil;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeChannel;
import org.hps.conditions.hodoscope.HodoscopeConditions;
import org.hps.conditions.hodoscope.HodoscopeChannel.HodoscopeChannelCollection;
import org.hps.record.daqconfig2019.ConfigurationManager2019;
import org.hps.record.daqconfig2019.DAQConfig2019;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.VTPCluster;
import org.hps.record.triggerbank.VTPData;
import org.hps.record.triggerbank.TriggerModule2019;
import org.hps.util.Pair;

import org.hps.readout.util.HodoscopePattern;
import org.hps.record.triggerbank.TSData2019;
import org.hps.record.triggerbank.VTPSinglesTrigger;
import org.hps.record.triggerbank.VTPPairsTrigger;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Driver <code>TriggeDiagnostic2019Driver</code> performs a comparison between
 * the trigger results reported by the hardware and those simulated by the LCSim
 * software for a single trigger. <br/>
 * <br/>
 * The driver requires simulated trigger objects, which are produced separately
 * by the driver <code>DataTriggerSim2019Driver</code>, in order to function. It
 * also requires the presence of the runtime settings management driver,
 * <code>DAQConfiguration2019Driver</code>. <br/>
 * <br/>
 * The driver works by taking trigger objects simulated by LCSim, using both
 * hardware reported (VTP) clusters and software clusters, and comparing these
 * to the hardware's reported triggers. Reported triggers include a trigger time
 * and which cuts passed. The driver requires that there exists a hardware
 * trigger that matches in each of these fields for every simulated trigger.
 * Note that the reverse may not be true; pulse-clipping can occur for simulated
 * clusters, resulting in a reduced cluster energy, that can affect whether a
 * cluster or pair passes the trigger. This does not occur at the hardware
 * level, though. As such, triggers within the "pulse-clipping" region are not
 * necessarily comparable. The driver ignores these. <br/>
 * <br/>
 * Output consists primarily of text printed at the end of the run. The driver
 * outputs the overall efficiency (defined as the number of simulated triggers
 * matched versus the total number) for both cluster types. It also provides a
 * more detailed table that further separates this into efficiency by cluster
 * type and active TS bit. The other output is an efficiency over time plot that
 * shows the efficiency for a programmable time frame throughout the run.
 * 
 * Driver <code>TriggeDiagnostic2019Driver</code> is developed based on Driver
 * <code>TriggeDiagnosticDriver</code>
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */
public class TriggerDiagnostic2019Driver extends Driver {
    // Get a copy of the calorimeter conditions for the detector.
    private HodoscopeConditions hodoConditions = null;

    /** Stores all channel for the hodoscope. */
    private Map<Long, HodoscopeChannel> channelMap = new HashMap<Long, HodoscopeChannel>();

    // === Store the LCIO collection names for the needed objects.
    // ======================
    // ==================================================================================
    /** The LCIO collection containing FADC hits. */
    private String hitEcalCollectionName = "EcalCalHits";

    /** The LCIO collection containing Hodo FADC hits. */
    private String hitHodoCollectionName = "HodoCalHits";

    /**
     * The LCIO collection containing VTP clusters and hardware triggers.
     */
    private String bankVTPCollectionName = "VTPBank";

    /**
     * The LCIO collection containing TS-bits
     */
    private String bankTSCollectionName = "TSBank";

    /** The LCIO collection containing all software-simulated triggers. */
    private String simTriggerCollectionName = "SimTriggers";

    // === Trigger modules for performing trigger analysis.
    // =============================
    // ==================================================================================
    /**
     * Indicates which TS-bits are active in the current event. Array indices should
     * align with <code>TriggerType.ordinal()</code>. Values set by the method
     * <code>setTSFlags(TSData2019)</code> and is called by the
     * <code>process(EventHeader)</code> method.
     */
    private boolean[] tsFlags = new boolean[20];

    /**
     * Indicates if TS-bits are read. For random runs, there are no TS-bits.
     */
    private boolean isActiveBitRead = false;

    /**
     * Stores the singles trigger settings. Array is of size 4, with each array
     * index corresponding to the trigger of the same trigger number.
     */
    private TriggerModule2019[] singlesTrigger = new TriggerModule2019[4];
    /**
     * Stores the pair trigger settings. Array is of size 4, with each array index
     * corresponding to the trigger of the same trigger number.
     */
    private TriggerModule2019[] pairTrigger = new TriggerModule2019[4];

    // === Plotting variables.
    // ==========================================================
    // ==================================================================================
    /**
     * Defines the basic directory structure for all plots used in the class. This
     * is instantiated in <code>startOfData</code>.
     */
    private String moduleHeader;
    /**
     * Stores the number of bins used by the efficiency plots for each conventional
     * trigger cut. Array index corresponds to the ordinal value of the
     * <code>CutType</code> enumerable for all values for which
     * <code>isSpecial()</code> is false. Note that this variable is defined as a
     * function of the variable arrays <code>xMax</code>, <code>xMin</code> and
     * <code>binSize</code> during <code>startOfData()</code>.
     */
    private int[] bins = new int[11];
    /**
     * Stores the x-axis maximum used by the efficiency plots for each conventional
     * trigger cut.
     */
    private double[] xMax = {
            // Singles
            5.00, // Cluster energy, xMax = 5 GeV
            9.5, // Hit count, xMax = 9.5 hits
            23.5, // Cluster x index, xMax = 23.5
            5.00, // PDE, xMax = 5 GeV
            // Pairs
            5.00, // Energy sum, xMax = 5. GeV
            5.00, // Energy difference, xMax = 5. GeV
            5.000, // Energy slope, xMax = 5.0 GeV
            180.0, // Coplanarity, xMax = 180 degrees
            30.0, // Time coincidence, xMax = 30 ns
            5.00, // Energy of cluster with lower energy in pairs, xMax = 5.00 GeV
            5.00 // Energy of cluster with higher energy in pairs, xMax = 5.00 GeV
    };

    /**
     * Stores the x-axis minimum used by the efficiency plots for each conventional
     * trigger cut.
     */
    private double[] xMin = {
            // Singles
            0.00, // Cluster energy, xMax = 5 GeV
            0.5, // Hit count, xMax = 9.5 hits
            -23.5, // Cluster x index, xMax = 23.5
            0.00, // PDE, xMax = 5 GeV
            // Pairs
            0.00, // Energy sum, xMax = 5. GeV
            0.00, // Energy difference, xMax = 5. GeV
            0.000, // Energy slope, xMax = 4.0 GeV
            0.0, // Coplanarity, xMax = 180 degrees
            0.0, // Time coincidence, xMax = 30 ns
            0.00, // Energy of cluster with lower energy in pairs, xMax = 5.00 GeV
            0.00 // Energy of cluster with higher energy in pairs, xMax = 5.00 GeV
    };

    /**
     * Store the size of a bin used by the efficiency plots for each conventional
     * trigger cut.
     */
    private double[] binSize = {
            // Singles
            0.050, // Cluster energy, binSize = 50 MeV
            1, // Hit count, binSize = 1 hit
            1, // Cluster x index, binSize = 1
            0.050, // PDE, binSize = 50 MeV
            // Pairs
            0.050, // Energy sum, binSize = 50 MeV
            0.050, // Energy difference, binSize = 50 MeV
            0.050, // Energy slope, binSize = 50 MeV
            5, // Coplanarity, binSize = 5 degrees
            4, // Time coincidence, binSize = 2 ns
            0.050, // Energy of cluster with lower energy in pairs, binSize = 50 MeV
            0.050 // Energy of cluster with higher energy in pairs, binSize = 50 MeV
    };

    /**
     * Stores a list of all trigger types that are used for plotting efficiency
     * plots. This is filled in <code>startOfData</code>.
     */
    private List<TriggerType> triggerTypes = new ArrayList<TriggerType>(TriggerType.values().length + 1);

    private List<CutType> cutTypes = new ArrayList<CutType>();

    // === Trigger matching statistics.
    // =================================================
    // ==================================================================================
    private static final int SOURCE_SIM_CLUSTER = 0;
    private static final int SOURCE_VTP_CLUSTER = 1;
    private static final int ALL_TRIGGERS = TriggerType.FEEBOT.ordinal() + 1;
    private static final int LOCAL_WINDOW_TRIGGERS = ALL_TRIGGERS + 1;
    /**
     * Stores the total number of simulated triggers observed for each source type.
     * The first array index defines the source type and corresponds to the
     * variables <code>SOURCE_SIM_CLUSTER</code> and
     * <code>SOURCE_VTP_CLUSTER</code>. The second array index defines one of
     * several conditions. This includes triggers seen when a certain TS bit was
     * active, with these indices defined by <code>TriggerType.ordinal()</code>, all
     * triggers seen in general, defined by <code>ALL_TRIGGERS</code>, and all
     * triggers seen in the local window, defined by
     * <code>LOCAL_WINDOW_TRIGGERS</code>.
     */
    private int simTriggerCount[][] = new int[2][LOCAL_WINDOW_TRIGGERS + 1];
    /**
     * Stores the total number of hardware triggers observed for each source type.
     * The first array index defines the source type and corresponds to the
     * variables <code>SOURCE_SIM_CLUSTER</code> and
     * <code>SOURCE_VTP_CLUSTER</code>. The second array index defines one of
     * several conditions. This includes triggers seen when a certain TS bit was
     * active, with these indices defined by <code>TriggerType.ordinal()</code>, all
     * triggers seen in general, defined by <code>ALL_TRIGGERS</code>, and all
     * triggers seen in the local window, defined by
     * <code>LOCAL_WINDOW_TRIGGERS</code>.
     */
    private int hardwareTriggerCount[] = new int[LOCAL_WINDOW_TRIGGERS + 1];
    /**
     * Stores the total number of matched triggers observed for each source type.
     * The first array index defines the source type and corresponds to the
     * variables <code>SOURCE_SIM_CLUSTER</code> and
     * <code>SOURCE_VTP_CLUSTER</code>. The second array index defines one of
     * several conditions. This includes triggers seen when a certain TS bit was
     * active, with these indices defined by <code>TriggerType.ordinal()</code>, all
     * triggers seen in general, defined by <code>ALL_TRIGGERS</code>, and all
     * triggers seen in the local window, defined by
     * <code>LOCAL_WINDOW_TRIGGERS</code>.
     */
    private int matchedTriggerCount[][] = new int[2][LOCAL_WINDOW_TRIGGERS + 1];

    // === Verbose settings.
    // ============================================================
    // ==================================================================================
    /** Indicates that debug messages should be output. */
    private boolean debug = false;

    /** Indicates that all logger messages should be output. */
    private boolean verbose = false;
    /**
     * Indicates that at least one software-cluster simulated trigger failed to
     * verify. This is set during the <code>compareSimulatedToHardware</code>
     * method.
     */
    private boolean softwareSimFailure = false;
    /**
     * Indicates that at least one hardware-cluster simulated trigger failed to
     * verify. This is set during the <code>compareSimulatedToHardware</code>
     * method.
     */
    private boolean hardwareSimFailure = false;
    /**
     * Indicates whether the event log should output in the event that a
     * software-cluster simulated trigger fails to verify.
     */
    private boolean printOnSoftwareSimFailure = false;
    /**
     * Indicates whether the event log should output in the event that a
     * hardware-cluster simulated trigger fails to verify.
     */
    private boolean printOnHardwareSimFailure = false;
    /**
     * The event logger. Is used to store detailed event readouts, and if
     * appropriate, print them to the terminal.
     */
    private LocalOutputLogger logger = new LocalOutputLogger();

    // === Verification settings.
    // =======================================================
    // ==================================================================================
    // Store Ecal hit verifiability parameters.
    private int nsbEcal = -1;
    private int nsaEcal = -1;
    private int windowWidthEcal = -1;
    private int offsetEcal = -1;

    // Store hodoscope hit verifiability parameters.
    private int nsaHodo = -1;
    private int nsbHodo = -1;
    private int windowWidthHodo = -1;
    private int offsetHodo = -1;

    // Store VTP parameters.
    private int hodoFADCHitThr = 0;
    private int hodoThr = 0;
    private int hodoDT = 0;

    /**
     * The number of hits for Ecal that must be present in event in order for it to
     * be ignored as a "noise event."
     */
    private int noiseEventThresholdEcal = 100;

    /**
     * The number of hits for Hodo that must be present in event in order for it to
     * be ignored as a "noise event."
     */
    private int noiseEventThresholdHodo = 50;

    /** Indicates the type of trigger that is being tested. */
    private TriggerType triggerType = null;
    /**
     * Whether events with more than <code>noiseEventThreshold</code> hits for Ecal
     * should be skipped.
     */
    private boolean skipNoiseEventsEcal = false;

    /**
     * Whether events with more than <code>noiseEventThreshold</code> hits for
     * hodoscope should be skipped.
     */
    private boolean skipNoiseEventsHodo = false;

    /** Defines the (inclusive) end of the trigger window range. */
    private int triggerWindowEnd = 400;
    /** Defines the (inclusive) start of the trigger window range. */
    private int triggerWindowStart = 0;

    // === Local window values.
    // =========================================================
    // ==================================================================================
    /**
     * Defines the length of time over which statistics are collected in order to
     * produce an entry into the efficiency over time plot. Units are in
     * nanoseconds.
     */
    private long localWindowSize = 5000000;
    /**
     * Tracks the current time of the current event for the purpose of identifying
     * the end of a local sample.
     */
    private long localEndTime = Long.MIN_VALUE;
    /** Tracks the start time of the current local sample. */
    private long localStartTime = Long.MIN_VALUE;
    /** Tracks the start time of the first observed event. */
    private long firstStartTime = Long.MIN_VALUE;
    private static final int LOCAL_ALL_TRIGGERS = 0;
    private static final int LOCAL_MATCHED_TRIGGERS = 1;
    /**
     * Stores entries for the efficiency over time plot. The first entry in the pair
     * represents the time of the local sample. This is defined with respect to the
     * start of the first observed event. The second entry contains the observed and
     * matched triggers for both trigger source types. The first array index defines
     * the source type and corresponds to the index variables defined in
     * <code>SOURCE_SIM_CLUSTER</code> and <code>SOURCE_VTP_CLUSTER</code>. The
     * second array index defines to observed and matched triggers, and corresponds
     * to the indices defined by the variables <code>LOCAL_ALL_TRIGGERS</code> and
     * <code>LOCAL_MATCHED_TRIGGERS</code>.
     */
    private List<Pair<Long, int[][]>> efficiencyPlotEntries = new ArrayList<Pair<Long, int[][]>>();

    /**
     * Enumerable <code>CutType</code> represents a type of cut which against which
     * trigger efficiency may be plotted. It also provides mechanisms by which a
     * human-readable name may be acquired and also whether or not the cut is a real
     * trigger cut, or a special cut used for plotting efficiency only.
     * 
     */
    private enum CutType {
        // Singles trigger
        CLUSTER_TOTAL_ENERGY("Cluster Total Energy", true, false), CLUSTER_HIT_COUNT("Cluster Hit Count", true, false),
        CLUSTER_XINDEX("Cluster X Index", true, false), CLUSTER_PDE("Cluster PDE", true, false),

        // Pairs trigger
        PAIR_ENERGY_SUM("Pair Energy Sum", false, true), PAIR_ENERGY_DIFF("Pair Energy Difference", false, true),
        PAIR_ENERGY_SLOPE("Pair Energy Slope", false, true), PAIR_COPLANARITY("Pair Coplanarity", false, true),
        PAIR_TIME_COINCIDENCE("Pair Time Coincidence", false, true),
        PAIR_LOW_ENERGY("Pair Lower Cluster Energy", false, true),
        PAIR_HIGH_ENERGY("Pair Upper Cluster Energy", false, true),

        // trigger time
        EVENT_TIME("Event Time", true, true);

        private final String name;
        private final boolean isPair;
        private final boolean isSingles;

        /**
         * Instantiates a cut. The cut is assumed to be a real trigger cut.
         * 
         * @param name      - The name of the cut in a human-readable form.
         * @param isSingles - Whether or not this is a singles cut. <code>true</code>
         *                  means that it is and <code>false</code> that it is not.
         * @param isPair    - Whether or not this is a pair cut. <code>true</code> means
         *                  that it is and <code>false</code> that it is not.
         */
        private CutType(String name, boolean isSingles, boolean isPair) {
            this.name = name;
            this.isPair = isPair;
            this.isSingles = isSingles;
        }

        /**
         * Indicates whether this is a singles cut.
         * 
         * @return Returns <code>true</code> to indicate that it is and
         *         <code>false</code> that it is not.
         */
        public boolean isSingles() {
            return isSingles;
        }

        /**
         * Indicates whether this is a pair cut.
         * 
         * @return Returns <code>true</code> to indicate that it is and
         *         <code>false</code> that it is not.
         */
        public boolean isPair() {
            return isPair;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Enumerable <code>TriggerType</code> represents the supported types of trigger
     * for the HPS experiment. It also provides a means to determine the trigger
     * number as an <code>int</code>, where applicable, and a human-readable trigger
     * name. <br/>
     * <br/>
     * <code>TriggerType.ordinal()</code> is also used as an index for multiple
     * value-tracking arrays throughout the class.
     * 
     */
    private enum TriggerType {
        // Define the trigger types.
        SINGLESTOP0("Singles top 0", 0), SINGLESTOP1("Singles top 1", 1), SINGLESTOP2("Singles top 2", 2),
        SINGLESTOP3("Singles top 3", 3), SINGLESBOT0("Singles bot 0", 0), SINGLESBOT1("Singles bot 1", 1),
        SINGLESBOT2("Singles bot 2", 2), SINGLESBOT3("Singles bot 3", 3), PAIR0("Pair 0", 0), PAIR1("Pair 1", 1),
        PAIR2("Pair 2", 2), PAIR3("Pair 3", 3), LED("LED"), COSMIC("Cosmic"), HODOSCOPE("Hodoscope"), PULSER("Pulser"),
        MULTIPLICITY0("Multiplicity0"), MULTIPLICITY1("Multiplicity1"), FEETOP("FEETop"), FEEBOT("FEEBot");

        // Store trigger type data.
        private String name = null;
        private int triggerNum = -1;

        /**
         * Instantiates a trigger type enumerable.
         * 
         * @param name - The name of the trigger.
         */
        private TriggerType(String name) {
            this.name = name;
        }

        /**
         * Instantiates a trigger type enumerable.
         * 
         * @param name       - The name of the trigger.
         * @param triggerNum - The trigger number.
         */
        private TriggerType(String name, int triggerNum) {
            this.name = name;
            this.triggerNum = triggerNum;
        }

        public int getTriggerNum() {
            return triggerNum;
        }

        /**
         * Indicates whether this trigger type is a singles trigger.
         * 
         * @return Returns <code>true</code> if the trigger is of type
         *         <code>TriggerType.SINGLESTOP0</code> or
         *         <code>TriggerType.SINGLESTOP1</code> or
         *         <code>TriggerType.SINGLESTOP2</code> or
         *         <code>TriggerType.SINGLESTOP3</code> or
         *         <code>TriggerType.SINGLESBOT0</code> or
         *         <code>TriggerType.SINGLESBOT1</code> or
         *         <code>TriggerType.SINGLESBOT2</code> or
         *         <code>TriggerType.SINGLESBOT3</code>. Otherwise, returns
         *         <code>false</code>.
         */
        public boolean isSinglesTrigger() {
            return (this.equals(SINGLESTOP0) || this.equals(SINGLESTOP1) || this.equals(SINGLESTOP2)
                    || this.equals(SINGLESTOP3) || this.equals(SINGLESBOT0) || this.equals(SINGLESBOT1)
                    || this.equals(SINGLESBOT2) || this.equals(SINGLESBOT3));
        }
        
        /**
         * Indicates whether this trigger type is a singles top trigger.
         * 
         * @return Returns <code>true</code> if the trigger is of type
         *         <code>TriggerType.SINGLESTOP0</code> or
         *         <code>TriggerType.SINGLESTOP1</code> or
         *         <code>TriggerType.SINGLESTOP2</code> or
         *         <code>TriggerType.SINGLESTOP3</code>. Otherwise, returns
         *         <code>false</code>.
         */
        public boolean isSinglesTopTrigger() {
            return (this.equals(SINGLESTOP0) || this.equals(SINGLESTOP1) || this.equals(SINGLESTOP2)
                    || this.equals(SINGLESTOP3));
        }
        
        /**
         * Indicates whether this trigger type is a singles bot trigger.
         * 
         * @return Returns <code>true</code> if the trigger is of type
         *         <code>TriggerType.SINGLESBOT0</code> or
         *         <code>TriggerType.SINGLESBOT1</code> or
         *         <code>TriggerType.SINGLESBOT2</code> or
         *         <code>TriggerType.SINGLESBOT3</code>. Otherwise, returns
         *         <code>false</code>.
         */
        public boolean isSinglesBotTrigger() {
            return (this.equals(SINGLESBOT0) || this.equals(SINGLESBOT1) || this.equals(SINGLESBOT2)
                    || this.equals(SINGLESBOT3));
        }

        /**
         * Indicates whether this trigger type is a pair trigger.
         * 
         * @return Returns <code>true</code> if the trigger is of type
         *         <code>TriggerType.PAIR0</code> or <code>TriggerType.PAIR1</code> or
         *         <code>TriggerType.PAIR2</code> or <code>TriggerType.PAIR3</code>.
         *         Otherwise, returns <code>false</code>.
         */
        public boolean isPairTrigger() {
            return (this.equals(PAIR0) || this.equals(PAIR1) || this.equals(PAIR2) || this.equals(PAIR3));
        }

        /**
         * Indicates whether this trigger type is a LED trigger.
         * 
         * @return Returns <code>true</code> if the trigger is of type
         *         <code>TriggerType.LED</code>. Otherwise, returns <code>false</code>.
         */
        public boolean isLEDTrigger() {
            return (this.equals(LED));
        }

        /**
         * Indicates whether this trigger type is a Cosmic trigger.
         * 
         * @return Returns <code>true</code> if the trigger is of type
         *         <code>TriggerType.COSMIC</code>. Otherwise, returns
         *         <code>false</code>.
         */
        public boolean isCosmicTrigger() {
            return (this.equals(COSMIC));
        }

        /**
         * Indicates whether this trigger type is a Hodoscope trigger.
         * 
         * @return Returns <code>true</code> if the trigger is of type
         *         <code>TriggerType.HODOSCOPE</code>. Otherwise, returns
         *         <code>false</code>.
         */
        public boolean isHodoscopeTrigger() {
            return (this.equals(HODOSCOPE));
        }

        /**
         * Indicates whether this trigger type is a pulse trigger.
         * 
         * @return Returns <code>true</code> if the trigger is of type
         *         <code>TriggerType.PULSE</code>. Otherwise, returns
         *         <code>false</code>.
         */
        public boolean isPulseTrigger() {
            return (this.equals(PULSER));
        }

        /**
         * Indicates whether this trigger type is a multiplicity trigger.
         * 
         * @return Returns <code>true</code> if the trigger is of type
         *         <code>TriggerType.MULTIPLICITY0</code> or
         *         <code>TriggerType.MULTIPLICITY1</code>. Otherwise, returns
         *         <code>false</code>.
         */
        public boolean isMultiplicityTrigger() {
            return (this.equals(MULTIPLICITY0) || this.equals(MULTIPLICITY1));
        }

        /**
         * Indicates whether this trigger type is a FEE trigger.
         * 
         * @return Returns <code>true</code> if the trigger is of type
         *         <code>TriggerType.FEETOP</code> or <code>TriggerType.FEEBOT</code>.
         *         Otherwise, returns <code>false</code>.
         */
        public boolean isFEErigger() {
            return (this.equals(FEETOP) || this.equals(FEEBOT));
        }

        /**
         * Get name of a trigger type
         * 
         * @return
         */
        public String getTriggerName() {
            return name;
        }

        /**
         * Gets the trigger number for this trigger type.
         * 
         * @return Returns either <code>0</code> or <code>1</code> as appropriate for
         *         singles and pair trigger types. For cosmic and pulser trigger types,
         *         returns <code>-1</code>.
         */
        public int getTriggerNumber() {
            return triggerNum;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Outputs the global efficiency data.
     */
    @Override
    public void endOfData() {
        // Get the number of digits in the largest value that is to be
        // displayed.
        int largestValue = max(hardwareTriggerCount[ALL_TRIGGERS], simTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS],
                simTriggerCount[SOURCE_VTP_CLUSTER][ALL_TRIGGERS],
                matchedTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS],
                matchedTriggerCount[SOURCE_VTP_CLUSTER][ALL_TRIGGERS]);
        int maxChars = TriggerDiagnosticUtil.getDigits(largestValue);
        String charDisplay = "%" + maxChars + "d";

        // Calculate the efficiencies and determine the display value.
        double[] efficiency = new double[2];
        String[] efficiencyDisp = new String[2];
        for (int i = SOURCE_SIM_CLUSTER; i <= SOURCE_VTP_CLUSTER; i++) {
            // Calculate the efficiency. This is defined as the number
            // triggers which were matched, versus the number that are
            // expected to have matched.
            efficiency[i] = 100.0 * matchedTriggerCount[i][ALL_TRIGGERS] / simTriggerCount[i][ALL_TRIGGERS];

            // If there were no triggers, then the efficiency is not
            // defined. Display "N/A."
            if (Double.isNaN(efficiency[i])) {
                efficiencyDisp[i] = "   N/A   ";
            }

            // Otherwise, display the value as a percentage.
            else {
                efficiencyDisp[i] = String.format("%7.3f%%", efficiency[i]);
            }
        }

        // Output the trigger efficiency statistics header.
        System.out.println();
        System.out.println();
        System.out.println("======================================================================");
        System.out.println("=== Trigger Efficiency - " + triggerType.toString() + " "
                + generateLine(44 - triggerType.toString().length()));
        System.out.println("======================================================================");

        // Output the global trigger statistics.
        System.out.println("Global Efficiency:");
        System.out.printf("Total Hardware Triggers       :: " + charDisplay + "%n", hardwareTriggerCount[ALL_TRIGGERS]);
        System.out.printf("Total Software Sim Triggers   :: " + charDisplay + "%n",
                simTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS]);
        System.out.printf("Total Hardware Sim Triggers   :: " + charDisplay + "%n",
                simTriggerCount[SOURCE_VTP_CLUSTER][ALL_TRIGGERS]);
        System.out.printf("Matched Software Sim Triggers :: " + charDisplay + "%n",
                matchedTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS]);
        System.out.printf("Matched Hardware Sim Triggers :: " + charDisplay + "%n",
                matchedTriggerCount[SOURCE_VTP_CLUSTER][ALL_TRIGGERS]);

        System.out.printf("Software Sim Efficiency       :: " + charDisplay + " / " + charDisplay,
                matchedTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS],
                simTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS]);
        if (simTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS] == 0
                || matchedTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS] == 0) {
            System.out.printf(" (%7.3f%% ± %7.3f%%)%n", 0.0, 0.0);
        } else {
            System.out.printf(" (%7.3f%% ± %7.3f%%)%n",
                    100.0 * matchedTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS]
                            / simTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS],
                    getRatioError(matchedTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS],
                            Math.sqrt(matchedTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS]),
                            simTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS],
                            Math.sqrt(simTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS])));
        }

        System.out.printf("Hardware Sim Efficiency       :: " + charDisplay + " / " + charDisplay,
                matchedTriggerCount[SOURCE_VTP_CLUSTER][ALL_TRIGGERS],
                simTriggerCount[SOURCE_VTP_CLUSTER][ALL_TRIGGERS]);
        if (simTriggerCount[SOURCE_VTP_CLUSTER][ALL_TRIGGERS] == 0
                || matchedTriggerCount[SOURCE_VTP_CLUSTER][ALL_TRIGGERS] == 0) {
            System.out.printf(" (%7.3f%% ± %7.3f%%)%n", 0.0, 0.0);
        } else {
            System.out.printf(" (%7.3f%% ± %7.3f%%)%n",
                    100.0 * matchedTriggerCount[SOURCE_VTP_CLUSTER][ALL_TRIGGERS]
                            / simTriggerCount[SOURCE_VTP_CLUSTER][ALL_TRIGGERS],
                    getRatioError(matchedTriggerCount[SOURCE_VTP_CLUSTER][ALL_TRIGGERS],
                            Math.sqrt(matchedTriggerCount[SOURCE_VTP_CLUSTER][ALL_TRIGGERS]),
                            simTriggerCount[SOURCE_VTP_CLUSTER][ALL_TRIGGERS],
                            Math.sqrt(simTriggerCount[SOURCE_VTP_CLUSTER][ALL_TRIGGERS])));
        }

        // Get the largest number of spaces needed to display the TS
        // bit specific values.
        if (isActiveBitRead) {
            int tsMaxValue = Integer.MIN_VALUE;
            for (TriggerType trigger : TriggerType.values()) {
                tsMaxValue = max(tsMaxValue, simTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()],
                        matchedTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()],
                        simTriggerCount[SOURCE_VTP_CLUSTER][trigger.ordinal()],
                        matchedTriggerCount[SOURCE_VTP_CLUSTER][trigger.ordinal()]);
            }
            int tsMaxChars = TriggerDiagnosticUtil.getDigits(tsMaxValue);

            // Define the column width and column headers for the TS-bit
            // specific efficiencies.
            int[] columnWidth = new int[3];
            String[] header = { "TS Bit", "Software Sim Efficiency", "Hardware Sim Efficiency" };

            // Determine the width of the first column. This displays the
            // name of the TS bit with respect to which the efficiency is
            // displayed. It should be equal to either the longest trigger
            // name or the header width, whichever is longer.
            columnWidth[0] = header[0].length();
            for (TriggerType trigger : TriggerType.values()) {
                columnWidth[0] = max(columnWidth[0], trigger.toString().length());
            }

            // The second and third columns display the total number of
            // matched triggers versus the total number of expected triggers
            // as well as the percentage of two. This takes the form of the
            // string [MATCHED] / [EXPECTED] (NNN.NNN% + NNN.NNN%). The width needed
            // to display this will vary based on the numbers of triggers.
            // The width should be set to wither the largest size of the
            // largest of these values, or to the width of the header text.
            // whichever is larger.
            columnWidth[1] = max(header[1].length(), tsMaxChars + tsMaxChars + 22);
            columnWidth[2] = max(header[2].length(), tsMaxChars + tsMaxChars + 22);

            // Finally, define the column size strings and the individual
            // value size strings.
            String valueString = "%" + tsMaxChars + "d";
            String efficiencyString = valueString + " / " + valueString + "  (%19s)";
            String[] columnString = { "%-" + columnWidth[0] + "s", "%-" + columnWidth[1] + "s",
                    "%-" + columnWidth[2] + "s" };

            // Output the efficiency as a function of active TS bit.
            System.out.println("\n\nTS-Bit Efficiency:");
            System.out.printf("\t" + columnString[0] + "   " + columnString[1] + "   " + columnString[2] + "%n",
                    header[0], header[1], header[2]);
            for (TriggerType trigger : TriggerType.values()) {
                // Calculate the efficiencies for the second and third columns.
                double softwareSimEfficiency = 100.0 * matchedTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()]
                        / simTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()];
                double hardwareSimEfficiency = 100.0 * matchedTriggerCount[SOURCE_VTP_CLUSTER][trigger.ordinal()]
                        / simTriggerCount[SOURCE_VTP_CLUSTER][trigger.ordinal()];

                // The efficiency value strings are either the efficiency
                // rounded to two decimal points, or "N/A" if there were
                // no triggers found.
                String softwareSimString = Double.isNaN(softwareSimEfficiency)
                        ? String.format("%7.3f%% ± %7.3f%%", 0.0, 0.0)
                        : String.format("%7.3f%% ± %7.3f%%", softwareSimEfficiency,
                                getRatioError(matchedTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()],
                                        Math.sqrt(matchedTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()]),
                                        simTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()],
                                        Math.sqrt(simTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()])));
                String hardwareSimString = Double.isNaN(hardwareSimEfficiency)
                        ? String.format("%7.3f%% ± %7.3f%%", 0.0, 0.0)
                        : String.format("%7.3f%% ± %7.3f%%", hardwareSimEfficiency,
                                getRatioError(matchedTriggerCount[SOURCE_VTP_CLUSTER][trigger.ordinal()],
                                        Math.sqrt(matchedTriggerCount[SOURCE_VTP_CLUSTER][trigger.ordinal()]),
                                        simTriggerCount[SOURCE_VTP_CLUSTER][trigger.ordinal()],
                                        Math.sqrt(simTriggerCount[SOURCE_VTP_CLUSTER][trigger.ordinal()])));

                // The efficiency column strings take the form "[MATCHED] /
                // [EXPECTED] (NNN.NN%)".
                String softwareSimColumn = String.format(efficiencyString,
                        matchedTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()],
                        simTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()], softwareSimString);
                String hardwareSimColumn = String.format(efficiencyString,
                        matchedTriggerCount[SOURCE_VTP_CLUSTER][trigger.ordinal()],
                        simTriggerCount[SOURCE_VTP_CLUSTER][trigger.ordinal()], hardwareSimString);

                // Create the efficiency string
                System.out.printf("\t" + columnString[0] + "   " + columnString[1] + "   " + columnString[2] + "%n",
                        trigger.toString(), softwareSimColumn, hardwareSimColumn);
            }
        }

        // Create and populate the efficiency over time plot.
        AIDA.defaultInstance().cloud2D(moduleHeader + "Software Sim Trigger Efficiency", efficiencyPlotEntries.size());
        AIDA.defaultInstance().cloud2D(moduleHeader + "Hardware Sim Trigger Efficiency", efficiencyPlotEntries.size());
        for (Pair<Long, int[][]> entry : efficiencyPlotEntries) {
            // Calculate the value for each type of efficiency.
            double softwareEfficiency = 100.0 * entry.getSecondElement()[SOURCE_SIM_CLUSTER][LOCAL_MATCHED_TRIGGERS]
                    / entry.getSecondElement()[SOURCE_SIM_CLUSTER][LOCAL_ALL_TRIGGERS];
            double hardwareEfficiency = 100.0 * entry.getSecondElement()[SOURCE_VTP_CLUSTER][LOCAL_MATCHED_TRIGGERS]
                    / entry.getSecondElement()[SOURCE_VTP_CLUSTER][LOCAL_ALL_TRIGGERS];

            // Convert the time to units of seconds.
            long time = Math.round(entry.getFirstElement() / 1000000.0);

            // If the value is properly defined, add it to the plot.
            if (!Double.isNaN(softwareEfficiency)) {
                AIDA.defaultInstance().cloud2D(moduleHeader + "Software Sim Trigger Efficiency").fill(time,
                        softwareEfficiency);
            }
            if (!Double.isNaN(hardwareEfficiency)) {
                AIDA.defaultInstance().cloud2D(moduleHeader + "Hardware Sim Trigger Efficiency").fill(time,
                        hardwareEfficiency);
            }
        }

        // Create the efficiency plots from the observed and verified
        // trigger plots, as appropriate.
        for (TriggerType trigger : triggerTypes) {
            if (!isActiveBitRead && !(trigger == null))
                continue;

            for (CutType cut : cutTypes) {
                // Only process plots appropriate to the trigger type.
                if ((triggerType.isSinglesTrigger() && !cut.isSingles())
                        || (triggerType.isPairTrigger() && !cut.isPair())) {
                    continue;
                }

                // Define the plot for the current TI-bit and cut.
                for (int type = SOURCE_SIM_CLUSTER; type <= SOURCE_VTP_CLUSTER; type++) {
                    AIDA.defaultInstance().histogramFactory().divide(getPlotNameEfficiency(cut, trigger, type),
                            AIDA.defaultInstance().histogram1D(getPlotNameVerified(cut, trigger, type)),
                            AIDA.defaultInstance().histogram1D(getPlotNameTotal(cut, trigger, type)));
                }
            }
        }
    }

    /**
     * Processes an event and performs trigger verification. Method will only run if
     * the <code>ConfigurationManager2019</code> has been initialized. Handles
     * testing for noise events and extracting basic event data for analysis, and
     * also ensuring that everything necessary is present. Event trigger data is
     * actually processed separately by the method <code>triggerVerification</code>.
     * Method also handles logger output.
     * 
     * @param event - The data object containing all event data.
     */
    @Override
    public void process(EventHeader event) {
        // If the DAQ configuration manager is not yet initialized,
        // it is not possible to analyze the event.
        if (!ConfigurationManager2019.isInitialized()) {
            return;
        }

        // Clear the logger at the start of each event.
        logger.clearLog();

        // If this is the first event, set the starting time stamp.
        // The end time is always the time of the current event.
        localEndTime = event.getTimeStamp();
        if (localStartTime == Long.MIN_VALUE) {
            localStartTime = event.getTimeStamp();
            firstStartTime = event.getTimeStamp();
        }

        // Output the trigger diagnostic header.
        logger.printNewLine(2);
        logger.println("======================================================================");
        logger.println("=== Trigger Diagnostics - " + triggerType.toString() + " "
                + generateLine(43 - triggerType.toString().length()));
        logger.println("======================================================================");

        // Output basic event information.
        logger.printf("Event Number :: %d%n", event.getEventNumber());
        logger.printf("Event Time   :: %d%n", event.getTimeStamp());

        // ==========================================================
        // ==== Obtain calorimeter hits =============================
        // ==========================================================

        // Calorimeter hits are only relevant if noise events are to
        // be skipped. Otherwise, the calorimeter hits needn't be defined.
        if (skipNoiseEventsEcal) {
            // Get the calorimeter hits.
            List<CalorimeterHit> hits = getCalorimeterHits(event, hitEcalCollectionName);

            // Noise culling can not be performed if there are no hits
            // present in the event.
            if (hits == null) {
                System.err.println("TriggerDiagnostics :: Skipping event; no hit data found.");
                if (verbose) {
                    logger.printLog();
                }
                return;
            }

            // If noise event culling is enabled, check if this is a noise
            // event.
            if (skipNoiseEventsEcal) {
                if (hits.size() >= noiseEventThresholdEcal) {
                    logger.println(
                            "TriggerDiagnostics: Event exceeds the noise threshold for total number of hits and will be skipped.");
                    if (verbose) {
                        logger.printLog();
                    }
                    return;
                }
            }
        }

        // Calorimeter hits are only relevant if noise events are to
        // be skipped. Otherwise, the calorimeter hits needn't be defined.
        if (triggerType.isSinglesTrigger() && skipNoiseEventsHodo) {
            // Get the calorimeter hits.
            List<CalorimeterHit> hits = getCalorimeterHits(event, hitHodoCollectionName);

            // Noise culling can not be performed if there are no hits
            // present in the event.
            if (hits == null) {
                System.err.println("TriggerDiagnostics :: Skipping event; no hit data found.");
                if (verbose) {
                    logger.printLog();
                }
                return;
            }

            // If noise event culling is enabled, check if this is a noise
            // event.
            if (skipNoiseEventsHodo) {
                if (hits.size() >= noiseEventThresholdHodo) {
                    logger.println(
                            "TriggerDiagnostics: Event exceeds the noise threshold for total number of hits and will be skipped.");
                    if (verbose) {
                        logger.printLog();
                    }
                    return;
                }
            }
        }

        // ==========================================================
        // ==== Obtain VTP and TS Banks =============================
        // ==========================================================
        // Get the TS bank. The TS bank stores TS bits, which
        // are used for trigger-type efficiency values.
        if (isActiveBitRead) {
            List<GenericObject> bankTSList = event.get(GenericObject.class, bankTSCollectionName);

            // Check that all of the required objects are present.
            if (bankTSList.size() == 0) {
                System.err.println("TriggerDiagnostics: Skipping event; no TS bank found for this event.");
                if (verbose) {
                    logger.printLog();
                }
                return;
            }

            // Extract the active TS bits and make sure that at least one
            // is set
            boolean activeBitRead = false;
            for (GenericObject data : bankTSList) {
                if (AbstractIntData.getTag(data) == TSData2019.BANK_TAG) {
                    TSData2019 tsData = new TSData2019(data);
                    activeBitRead = setTSFlags(tsData);
                }
            }

            if (!activeBitRead) {
                if (verbose) {
                    logger.printLog();
                }
                return;
            }
        }

        // If there is no bank data, this event can not be analyzed.
        if (!event.hasCollection(GenericObject.class, bankVTPCollectionName)) {
            System.err.println("TriggerDiagnostics :: Skipping event; no VTP bank data found.");
            if (verbose) {
                logger.printLog();
            }
            return;
        }

        // Get the VTP bank. The VTP bank
        // contains hardware triggers and hardware clusters.
        List<GenericObject> bankVTPList = event.get(GenericObject.class, bankVTPCollectionName);

        // Check that all of the required objects are present.
        if (bankVTPList.size() == 0) {
            System.err.println("TriggerDiagnostics: Skipping event; no VTP bank found for this event.");
            if (verbose) {
                logger.printLog();
            }
            return;
        }

        VTPData vtpData = new VTPData(bankVTPList.get(0), bankVTPList.get(1));

        if (!event.hasCollection(GenericObject.class, bankTSCollectionName)) {
            System.err.println("TriggerDiagnostics :: Skipping event; no TS bank data found.");
            if (verbose) {
                logger.printLog();
            }
            return;
        }

        // ==========================================================
        // ==== Obtain Simulated Triggers ===========================
        // ==========================================================

        // If the simulated trigger collection does not exist, analysis
        // can not be performed.
        if (!event.hasCollection(SimTriggerData2019.class, simTriggerCollectionName)) {
            System.err.println("TriggerDiagnostics: Skipping event; no simulated triggers found.");
            if (verbose) {
                logger.printLog();
            }
            return;
        }

        // Get the simulated trigger module.
        List<SimTriggerData2019> stdList = event.get(SimTriggerData2019.class, simTriggerCollectionName);
        SimTriggerData2019 triggerData = stdList.get(0);

        // Engage the trigger verification.
        triggerVerification(triggerData, vtpData, triggerType);

        // If the local time window has elapsed, update the efficiency
        // plot values and reset the counters.
        if (localEndTime - localStartTime >= localWindowSize) {
            // Values are stored in an array within a list. Each array
            // index corresponds to specific value and each array entry
            // corresponds to an individual local window.
            int[][] databank = new int[2][2];
            databank[SOURCE_SIM_CLUSTER][LOCAL_ALL_TRIGGERS] = simTriggerCount[SOURCE_SIM_CLUSTER][LOCAL_WINDOW_TRIGGERS];
            databank[SOURCE_VTP_CLUSTER][LOCAL_ALL_TRIGGERS] = simTriggerCount[SOURCE_VTP_CLUSTER][LOCAL_WINDOW_TRIGGERS];
            databank[SOURCE_SIM_CLUSTER][LOCAL_MATCHED_TRIGGERS] = matchedTriggerCount[SOURCE_SIM_CLUSTER][LOCAL_WINDOW_TRIGGERS];
            databank[SOURCE_VTP_CLUSTER][LOCAL_MATCHED_TRIGGERS] = matchedTriggerCount[SOURCE_VTP_CLUSTER][LOCAL_WINDOW_TRIGGERS];

            // The efficiency list also stores the time that corresponds
            // to the particular databank. This is defined as half way
            // between the start and end time of the current window.
            Long time = localEndTime - firstStartTime - ((localEndTime - localStartTime) / 2);

            // Store the databank in the list.
            efficiencyPlotEntries.add(new Pair<Long, int[][]>(time, databank));

            // Reset all of the counters.
            hardwareTriggerCount[LOCAL_WINDOW_TRIGGERS] = 0;
            simTriggerCount[SOURCE_SIM_CLUSTER][LOCAL_WINDOW_TRIGGERS] = 0;
            simTriggerCount[SOURCE_VTP_CLUSTER][LOCAL_WINDOW_TRIGGERS] = 0;
            matchedTriggerCount[SOURCE_SIM_CLUSTER][LOCAL_WINDOW_TRIGGERS] = 0;
            matchedTriggerCount[SOURCE_VTP_CLUSTER][LOCAL_WINDOW_TRIGGERS] = 0;

            // The new window start time is the current event time.
            localStartTime = event.getTimeStamp();
        }

        // If appropriate, output the logger text.
        if (verbose || (printOnSoftwareSimFailure && softwareSimFailure)
                || (printOnHardwareSimFailure && hardwareSimFailure)) {
            logger.printLog();
        }

        // Reset the event failure flags.
        softwareSimFailure = false;
        hardwareSimFailure = false;
    }

    @Override
    public void detectorChanged(Detector detector) {

        hodoConditions = DatabaseConditionsManager.getInstance().getHodoConditions();

        // Populate the channel ID collections.
        populateChannelCollections();
    }

    /**
     * Populates channel map
     */
    private void populateChannelCollections() {
        // Load the conditions database and get the hodoscope channel
        // collection data.
        final DatabaseConditionsManager conditions = DatabaseConditionsManager.getInstance();
        final HodoscopeChannelCollection channels = conditions
                .getCachedConditions(HodoscopeChannelCollection.class, "hodo_channels").getCachedData();

        // Store the set of all channel IDs.
        for (HodoscopeChannel channel : channels) {
            channelMap.put(Long.valueOf(channel.getChannelId().intValue()), channel);
        }
    }

    /**
     * Get hodoscope channel id
     * 
     * @param hit
     * @return
     */
    public Long getHodoChannelID(CalorimeterHit hit) {
        return Long.valueOf(hodoConditions.getChannels().findGeometric(hit.getCellID()).getChannelId().intValue());
    }

    /**
     * Connects the driver to the <code>ConfigurationManager</code> to obtain
     * trigger settings.
     */
    @Override
    public void startOfData() {
        // If the DAQ configuration should be read, attach a listener
        // to track when it updates.
        ConfigurationManager2019.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the DAQ configuration.
                DAQConfig2019 daq = ConfigurationManager2019.getInstance();

                // If the event time plots are not instantiated, do so.
                if (nsaEcal == -1) {
                    // Calculate the bin count and x-axis maximum.
                    int bins = (daq.getEcalFADCConfig().getWindowWidth() / 4) + 1;
                    int xMax = daq.getEcalFADCConfig().getWindowWidth() + 2;

                    // Instantiate the plots for each trigger bit.
                    for (TriggerType trigger : triggerTypes) {
                        if (!isActiveBitRead && !(trigger == null))
                            continue;
                        for (int type = SOURCE_SIM_CLUSTER; type <= SOURCE_VTP_CLUSTER; type++) {
                            AIDA.defaultInstance().histogram1D(getPlotNameTotal(CutType.EVENT_TIME, trigger, type),
                                    bins, -2, xMax);
                            AIDA.defaultInstance().histogram1D(getPlotNameVerified(CutType.EVENT_TIME, trigger, type),
                                    bins, -2, xMax);
                            AIDA.defaultInstance().histogram1D(getPlotNameEfficiency(CutType.EVENT_TIME, trigger, type),
                                    bins, -2, xMax);
                        }
                    }
                }

                // Load the DAQ settings from the configuration manager.
                singlesTrigger[0].loadDAQConfiguration(daq.getVTPConfig().getSingles0Config());
                singlesTrigger[1].loadDAQConfiguration(daq.getVTPConfig().getSingles1Config());
                singlesTrigger[2].loadDAQConfiguration(daq.getVTPConfig().getSingles2Config());
                singlesTrigger[3].loadDAQConfiguration(daq.getVTPConfig().getSingles3Config());
                pairTrigger[0].loadDAQConfiguration(daq.getVTPConfig().getPair0Config());
                pairTrigger[1].loadDAQConfiguration(daq.getVTPConfig().getPair1Config());
                pairTrigger[2].loadDAQConfiguration(daq.getVTPConfig().getPair2Config());
                pairTrigger[3].loadDAQConfiguration(daq.getVTPConfig().getPair3Config());

                nsaEcal = daq.getEcalFADCConfig().getNSA();
                nsbEcal = daq.getEcalFADCConfig().getNSB();
                windowWidthEcal = daq.getEcalFADCConfig().getWindowWidth();
                offsetEcal = daq.getEcalFADCConfig().getWindowOffset();

                nsaHodo = daq.getHodoFADCConfig().getNSA();
                nsbHodo = daq.getHodoFADCConfig().getNSB();
                windowWidthHodo = daq.getHodoFADCConfig().getWindowWidth();
                offsetHodo = daq.getHodoFADCConfig().getWindowOffset();

                // Get VTP parameters.
                hodoFADCHitThr = daq.getVTPConfig().getHodoFADCHitThr();
                hodoThr = daq.getVTPConfig().getHodoThr();
                hodoDT = daq.getVTPConfig().getHodoDT() + 4;
            }
        });

        for (int i = 0; i < 4; i++) {
            singlesTrigger[i] = new TriggerModule2019();
            pairTrigger[i] = new TriggerModule2019();
        }

        // Set the trigger plots module name.
        moduleHeader = "Trigger Diagnostics/Trigger Verification/" + triggerType.toString() + "/";

        // Instantiate the trigger efficiency plots. Note that the time
        // coincidence plot is instantiated in the ConfigurationManager
        // listener, as it needs to know the event readout window size.
        for (TriggerType trigger : TriggerType.values()) {
            triggerTypes.add(trigger);
        }
        triggerTypes.add(null);
        for (TriggerType trigger : triggerTypes) {
            if (!isActiveBitRead && !(trigger == null))
                continue;

            if (triggerType.isSinglesTrigger()) {
                cutTypes.add(CutType.CLUSTER_TOTAL_ENERGY);
                cutTypes.add(CutType.CLUSTER_HIT_COUNT);
                cutTypes.add(CutType.CLUSTER_XINDEX);
                cutTypes.add(CutType.CLUSTER_PDE);

                for (CutType cut : cutTypes) {
                    // Define the bin counts for each plot.
                    bins[cut.ordinal()] = (int) ((xMax[cut.ordinal()] - xMin[cut.ordinal()]) / binSize[cut.ordinal()]);

                    // Only generate plots appropriate to the trigger type.
                    if ((triggerType.isSinglesTrigger() && !cut.isSingles())
                            || (triggerType.isPairTrigger() && !cut.isPair())) {
                        continue;
                    }

                    // Define the plot for the current TS-bit and cut.
                    for (int type = SOURCE_SIM_CLUSTER; type <= SOURCE_VTP_CLUSTER; type++) {
                        AIDA.defaultInstance().histogram1D(getPlotNameTotal(cut, trigger, type), bins[cut.ordinal()],
                                xMin[cut.ordinal()], xMax[cut.ordinal()]);
                        AIDA.defaultInstance().histogram1D(getPlotNameVerified(cut, trigger, type), bins[cut.ordinal()],
                                xMin[cut.ordinal()], xMax[cut.ordinal()]);
                        AIDA.defaultInstance().histogram1D(getPlotNameEfficiency(cut, trigger, type),
                                bins[cut.ordinal()], xMin[cut.ordinal()], xMax[cut.ordinal()]);
                    }
                }
            }

            // Define the pair cluster high and low energy plots. These
            // use the same values as the cluster total energy plot.
            // These plots are only initialized for pair triggers.
            if (triggerType.isPairTrigger()) {
                cutTypes.add(CutType.PAIR_ENERGY_SUM);
                cutTypes.add(CutType.PAIR_ENERGY_DIFF);
                cutTypes.add(CutType.PAIR_ENERGY_SLOPE);
                cutTypes.add(CutType.PAIR_COPLANARITY);
                cutTypes.add(CutType.PAIR_TIME_COINCIDENCE);
                cutTypes.add(CutType.PAIR_LOW_ENERGY);
                cutTypes.add(CutType.PAIR_HIGH_ENERGY);

                for (CutType cut : cutTypes) {
                    // Define the bin counts for each plot.
                    bins[cut.ordinal()] = (int) ((xMax[cut.ordinal()] - xMin[cut.ordinal()]) / binSize[cut.ordinal()]);

                    // Only generate plots appropriate to the trigger type.
                    if ((triggerType.isSinglesTrigger() && !cut.isSingles())
                            || (triggerType.isPairTrigger() && !cut.isPair())) {
                        continue;
                    }

                    // Define the plot for the current TS-bit and cut.
                    for (int type = SOURCE_SIM_CLUSTER; type <= SOURCE_VTP_CLUSTER; type++) {
                        AIDA.defaultInstance().histogram1D(getPlotNameTotal(cut, trigger, type), bins[cut.ordinal()],
                                xMin[cut.ordinal()], xMax[cut.ordinal()]);
                        AIDA.defaultInstance().histogram1D(getPlotNameVerified(cut, trigger, type), bins[cut.ordinal()],
                                xMin[cut.ordinal()], xMax[cut.ordinal()]);
                        AIDA.defaultInstance().histogram1D(getPlotNameEfficiency(cut, trigger, type),
                                bins[cut.ordinal()], xMin[cut.ordinal()], xMax[cut.ordinal()]);
                    }
                }
            }
        }
    }

    /**
     * Compares a collection of simulated triggers to a collection of triggers
     * reported by the hardware. The simulated triggers may be simulated from either
     * VTP clusters or clusters built by the software.
     * 
     * @param simTriggers      - A collection containing <code>Trigger</code>
     *                         objects. The source objects for the
     *                         <code>Trigger</code> objects may be either
     *                         <code>Cluster</code> or <code>VTPCluster</code>, as
     *                         well as a size two array of either of the above.
     * @param hardwareTriggers - A collection of VTP hardware triggers. The source
     *                         objects of type is <code>VTPSinglesTrigger</code>.
     * @param clusterType      - Specifies which of the four valid object types is
     *                         used as the source of the <code>Trigger</code>
     *                         objects defined in the <code>simTriggers</code>
     *                         argument.
     * @return Returns the number of simulated triggers which were successfully
     *         matched to hardware triggers.
     */
    private int compareSimulatedToHardwareSingles(Collection<Trigger<?>> simTriggers,
            Collection<VTPSinglesTrigger> hardwareTriggers, Class<?> clusterType) {
        // Print out the appropriate sub-header.
        logger.printNewLine(2);
        if (clusterType == Cluster.class) {
            logger.println("==== Simulated Sim Trigger to Hardware Trigger Verification ==========");
        } else {
            logger.println("==== Hardware Sim Trigger to Hardware Trigger Verification ===========");
        }

        // Trigger matches must be one-to-one. Track which hardware
        // triggers are already matched do that they are not matched
        // twice.
        Set<VTPSinglesTrigger> matchedTriggers = new HashSet<VTPSinglesTrigger>();

        // Iterate over each trigger simulated from hardware clusters.
        // It is expected that each of these triggers will correspond
        // to an existing hardware trigger, since, purportedly, these
        // are the same clusters from which the hardware triggers are
        // generated.
        simLoop: for (Trigger<?> simTrigger : simTriggers) {
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
            hardwareLoop: for (VTPSinglesTrigger hardwareTrigger : hardwareTriggers) {
                // Output the comparison hardware trigger.
                logger.printf("\t%s", getTriggerText(hardwareTrigger));

                // If the current trigger has already been matched,
                // then skip over it.
                if (matchedTriggers.contains(hardwareTrigger)) {
                    logger.printf(" [ fail; matched      ]%n");
                    continue hardwareLoop;
                }

                // The triggers must occur at the same time to classify
                // as a match.
                if (hardwareTrigger.getTime() != simTime) {
                    logger.printf(" [ fail; time         ]%n");
                    continue hardwareLoop;
                }

                // Cut comparisons are fundamentally different between
                // singles and pair triggers, so these must be handled
                // separately.
                if (isSinglesTrigger(simTrigger) && hardwareTrigger instanceof VTPSinglesTrigger) {
                    // Cast both the triggers to pair trigger objects.
                    SinglesTrigger2019<?> simSinglesTrigger = (SinglesTrigger2019<?>) simTrigger;
                    VTPSinglesTrigger hardwareSinglesTrigger = (VTPSinglesTrigger) hardwareTrigger;

                    // Since there is no seed hit information for the
                    // hardware cluster, we just assume that this cut
                    // matches. Instead, move to the cluster energy
                    // lower bound cut.
                    if (hardwareSinglesTrigger.passEMin() != simSinglesTrigger.getStateClusterEnergyLow()) {
                        logger.printf(" [ fail; energy low  ]%n");
                        continue hardwareLoop;
                    }

                    // Next, check the cluster energy upper bound cut.
                    if (hardwareSinglesTrigger.passEMax() != simSinglesTrigger.getStateClusterEnergyHigh()) {
                        logger.printf(" [ fail; energy high ]%n");
                        continue hardwareLoop;
                    }

                    // Lastly, check if the cluster hit count cut matches.
                    if (hardwareSinglesTrigger.passNMin() != simSinglesTrigger.getStateHitCount()) {
                        logger.printf(" [ fail; hit count   ]%n");
                        continue hardwareLoop;
                    }

                    if (hardwareSinglesTrigger.passXMin() != simSinglesTrigger.getStateClusterXMin()) {
                        logger.printf(" [ fail; x min       ]%n");
                        continue hardwareLoop;
                    }

                    if (hardwareSinglesTrigger.passPDET() != simSinglesTrigger.getStateClusterPDE()) {
                        logger.printf(" [ fail; PDE         ]%n");
                        continue hardwareLoop;
                    }

                    if (hardwareSinglesTrigger.passHodo1() != simSinglesTrigger.getStateHodoL1Matching()) {
                        logger.printf(" [ fail; Hodo L1     ]%n");
                        continue hardwareLoop;
                    }

                    if (hardwareSinglesTrigger.passHodo2() != simSinglesTrigger.getStateHodoL2Matching()) {
                        logger.printf(" [ fail; Hodo L2     ]%n");
                        continue hardwareLoop;
                    }

                    if (hardwareSinglesTrigger.passHodoGeo() != simSinglesTrigger.getStateHodoL1L2Matching()) {
                        logger.printf(" [ fail; Hodo L1L2   ]%n");
                        continue hardwareLoop;
                    }

                    if (hardwareSinglesTrigger.passHodoECal() != simSinglesTrigger.getStateHodoEcalMatching()) {
                        logger.printf(" [ fail; Hodo Ecal   ]%n");
                        continue hardwareLoop;
                    }

                } else {
                    throw new IllegalArgumentException("Trigger type is unrecongnized or simulated and "
                            + "hardware triggers are of different types.");
                }

                // If all three values match, then these triggers are
                // considered a match and verified.
                logger.printf(" [ trigger verified   ]%n");
                matchedTriggers.add(hardwareTrigger);

                // Plot the trigger for the verified plots.
                plotTrigger(simTrigger, tsFlags, true);

                // Update the verified count for each type of trigger
                // for the local and global windows.
                if (getTriggerTime(simTrigger) >= triggerWindowStart
                        && getTriggerTime(simTrigger) <= triggerWindowEnd) {
                    if (clusterType == Cluster.class || clusterType == Cluster[].class) {
                        matchedTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS]++;
                        matchedTriggerCount[SOURCE_SIM_CLUSTER][LOCAL_WINDOW_TRIGGERS]++;
                    } else {
                        matchedTriggerCount[SOURCE_VTP_CLUSTER][ALL_TRIGGERS]++;
                        matchedTriggerCount[SOURCE_VTP_CLUSTER][LOCAL_WINDOW_TRIGGERS]++;
                    }

                    // Update the verified count for each active TS bit.
                    if (isActiveBitRead) {
                        for (TriggerType trigger : TriggerType.values()) {
                            if (tsFlags[trigger.ordinal()]) {
                                if (clusterType == Cluster.class || clusterType == Cluster[].class) {
                                    matchedTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()]++;
                                } else {
                                    matchedTriggerCount[SOURCE_VTP_CLUSTER][trigger.ordinal()]++;
                                }
                            }
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

            if (debug) {
                if (clusterType == Cluster.class) {
                    System.out.println("==== Simulated Sim Trigger to Hardware Trigger Verification Fail ==========");
                } else {
                    System.out.println("==== Hardware Sim Trigger to Hardware Trigger Verification Fail ===========");
                }

                SinglesTrigger2019<?> simSinglesTrigger = (SinglesTrigger2019<?>) simTrigger;
                System.out.println("software trigger: ");
                System.out.println("\ttime" + getTriggerTime(simTrigger));
                System.out.println(
                        "\tstate of Emin, state of Emax, state of hit count, state of xmin, state of PED, state of Hodo L1, state of Hodo L2, state of Hodo L1L2, state of Hodo Ecal: "
                                + simSinglesTrigger.getStateClusterEnergyLow() + ", "
                                + simSinglesTrigger.getStateClusterEnergyHigh() + ", "
                                + simSinglesTrigger.getStateHitCount() + ", " + simSinglesTrigger.getStateClusterXMin()
                                + ", " + simSinglesTrigger.getStateClusterPDE() + ", "
                                + simSinglesTrigger.getStateHodoL1Matching() + ", "
                                + simSinglesTrigger.getStateHodoL2Matching() + ", "
                                + simSinglesTrigger.getStateHodoL1L2Matching() + ", "
                                + simSinglesTrigger.getStateHodoEcalMatching());

                System.out.println("hardware triggers: ");
                for (VTPSinglesTrigger hardwareTrigger : hardwareTriggers) {
                    VTPSinglesTrigger hardwareSinglesTrigger = (VTPSinglesTrigger) hardwareTrigger;
                    System.out.println("\ttime" + hardwareSinglesTrigger.getTime());
                    System.out.println(
                            "\tstate of Emin, state of Emax, state of hit count, state of xmin, state of PED, state of Hodo L1, state of Hodo L2, state of Hodo L1L2, state of Hodo Ecal: "
                                    + hardwareSinglesTrigger.passEMin() + ", " + hardwareSinglesTrigger.passEMax()
                                    + ", " + hardwareSinglesTrigger.passNMin() + ", "
                                    + hardwareSinglesTrigger.passXMin() + ", " + hardwareSinglesTrigger.passPDET()
                                    + ", " + hardwareSinglesTrigger.passHodo1() + ", "
                                    + hardwareSinglesTrigger.passHodo2() + ", " + hardwareSinglesTrigger.passHodoGeo()
                                    + ", " + hardwareSinglesTrigger.passHodoECal());
                }
            }

        }

        // The matched trigger set is equivalent in size to the number
        // of triggers that successfully passed verification.
        return matchedTriggers.size();
    }

    /**
     * Compares a collection of simulated triggers to a collection of triggers
     * reported by the hardware. The simulated triggers may be simulated from either
     * VTP clusters or clusters built by the software.
     * 
     * @param simTriggers      - A collection containing <code>Trigger</code>
     *                         objects. The source objects for the
     *                         <code>Trigger</code> objects may be either
     *                         <code>Cluster</code> or <code>VTPCluster</code>, as
     *                         well as a size two array of either of the above.
     * @param hardwareTriggers - A collection of VTP hardware triggers. The source
     *                         objects of type is <code>VTPPairsTrigger</code>.
     * @param clusterType      - Specifies which of the four valid object types is
     *                         used as the source of the <code>Trigger</code>
     *                         objects defined in the <code>simTriggers</code>
     *                         argument.
     * @return Returns the number of simulated triggers which were successfully
     *         matched to hardware triggers.
     */
    private int compareSimulatedToHardwarePairs(Collection<Trigger<?>> simTriggers,
            Collection<VTPPairsTrigger> hardwareTriggers, Class<?> clusterType) {
        // Print out the appropriate sub-header.
        logger.printNewLine(2);
        if (clusterType == Cluster[].class) {
            logger.println("==== Simulated Sim Trigger to Hardware Trigger Verification ==========");
        } else {
            logger.println("==== Hardware Sim Trigger to Hardware Trigger Verification ===========");
        }

        // Trigger matches must be one-to-one. Track which hardware
        // triggers are already matched do that they are not matched
        // twice.
        Set<VTPPairsTrigger> matchedTriggers = new HashSet<VTPPairsTrigger>();

        // Iterate over each trigger simulated from hardware clusters.
        // It is expected that each of these triggers will correspond
        // to an existing hardware trigger, since, purportedly, these
        // are the same clusters from which the hardware triggers are
        // generated.
        simLoop: for (Trigger<?> simTrigger : simTriggers) {
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
            hardwareLoop: for (VTPPairsTrigger hardwareTrigger : hardwareTriggers) {
                // Output the comparison hardware trigger.
                logger.printf("\t%s", getTriggerText(hardwareTrigger));

                // If the current trigger has already been matched,
                // then skip over it.
                if (matchedTriggers.contains(hardwareTrigger)) {
                    logger.printf(" [ fail; matched      ]%n");
                    continue hardwareLoop;
                }

                // The triggers must occur at the same time to classify
                // as a match.
                if (hardwareTrigger.getTime() != simTime) {
                    logger.printf(" [ fail; time         ]%n");
                    continue hardwareLoop;
                }

                // Cut comparisons are fundamentally different between
                // singles and pair triggers, so these must be handled
                // separately.
                if (isPairTrigger(simTrigger) && hardwareTrigger instanceof VTPPairsTrigger) {
                    // Cast both the triggers to pair trigger objects.
                    PairTrigger2019<?> simPairTrigger = (PairTrigger2019<?>) simTrigger;
                    VTPPairsTrigger hardwarePairTrigger = (VTPPairsTrigger) hardwareTrigger;

                    // Since there is no singles cuts data for the
                    // hardware trigger, we just assume that these cuts
                    // match. Move to the pair energy sum cut.
                    if (simPairTrigger.getStateClusterEnergyLow() != true) {
                        logger.printf(" [ fail; Energy low   ]%n");
                        continue hardwareLoop;
                    }

                    if (simPairTrigger.getStateClusterEnergyHigh() != true) {
                        logger.printf(" [ fail; Energy High  ]%n");
                        continue hardwareLoop;
                    }

                    if (simPairTrigger.getStateHitCount() != true) {
                        logger.printf(" [ fail; Hit count    ]%n");
                        continue hardwareLoop;
                    }

                    if (simPairTrigger.getStateTimeCoincidence() != true) {
                        logger.printf(" [ fail; coincidence  ]%n");
                        continue hardwareLoop;
                    }

                    if (hardwarePairTrigger.passESum() != simPairTrigger.getStateEnergySum()) {
                        logger.printf(" [ fail; sum          ]%n");
                        continue hardwareLoop;
                    }

                    // Next, check the energy difference cut.
                    if (hardwarePairTrigger.passEDiff() != simPairTrigger.getStateEnergyDifference()) {
                        logger.printf(" [ fail; difference   ]%n");
                        continue hardwareLoop;
                    }

                    // Next, check the energy slope cut.
                    if (hardwarePairTrigger.passESlope() != simPairTrigger.getStateEnergySlope()) {
                        logger.printf(" [ fail; slope        ]%n");
                        continue hardwareLoop;
                    }

                    // Lastly for pair 0, 1, 2 triggers, check the coplanarity cut.
                    if (hardwarePairTrigger.passCoplanarity() != simPairTrigger.getStateCoplanarity()) {
                        logger.printf(" [ fail; coplanarity  ]%n");
                        continue hardwareLoop;
                    }
                                        
                    // Only for pair3 trigger
                    if(triggerType == TriggerType.PAIR3) {
                        if (hardwarePairTrigger.passL1L2CoincidenceTop() != simPairTrigger.getStateHodoL1L2CoincidenceTop()) {
                            logger.printf(" [ fail; Hodo L1L2 Coincidence for Top ]%n");
                            continue hardwareLoop;
                        }
                        
                        if (hardwarePairTrigger.passHodoL1L2MatchingTop() != simPairTrigger.getStateHodoL1L2MatchingTop()) {
                            logger.printf(" [ fail; Hodo L1L2 Mataching for Top   ]%n");
                            continue hardwareLoop;
                        }
                        
                        if (hardwarePairTrigger.passHodoEcalMatchingTop() != simPairTrigger.getStateHodoEcalMatchingTop()) {
                            logger.printf(" [ fail; Hodo Ecal Mataching for Top   ]%n");
                            continue hardwareLoop;
                        }
                        
                        if (hardwarePairTrigger.passL1L2CoincidenceBot() != simPairTrigger.getStateHodoL1L2CoincidenceBot()) {
                            logger.printf(" [ fail; Hodo L1L2 Coincidence for Bot ]%n");
                            continue hardwareLoop;
                        }
                        
                        if (hardwarePairTrigger.passHodoL1L2MatchingBot() != simPairTrigger.getStateHodoL1L2MatchingBot()) {
                            logger.printf(" [ fail; Hodo L1L2 Mataching for Bot   ]%n");
                            continue hardwareLoop;
                        }
                        
                        if (hardwarePairTrigger.passHodoEcalMatchingBot() != simPairTrigger.getStateHodoEcalMatchingBot()) {
                            logger.printf(" [ fail; Hodo Ecal Mataching for Bot   ]%n");
                            continue hardwareLoop;
                        }
                    }                 
                } else {
                    throw new IllegalArgumentException("Trigger type is unrecongnized or simulated and "
                            + "hardware triggers are of different types.");
                }

                // If all three values match, then these triggers are
                // considered a match and verified.
                logger.printf(" [ trigger verified   ]%n");
                matchedTriggers.add(hardwareTrigger);

                // Plot the trigger for the verified plots.
                plotTrigger(simTrigger, tsFlags, true);

                // Update the verified count for each type of trigger
                // for the local and global windows.
                if (getTriggerTime(simTrigger) >= triggerWindowStart
                        && getTriggerTime(simTrigger) <= triggerWindowEnd) {
                    if (clusterType == Cluster[].class) {
                        matchedTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS]++;
                        matchedTriggerCount[SOURCE_SIM_CLUSTER][LOCAL_WINDOW_TRIGGERS]++;
                    } else {
                        matchedTriggerCount[SOURCE_VTP_CLUSTER][ALL_TRIGGERS]++;
                        matchedTriggerCount[SOURCE_VTP_CLUSTER][LOCAL_WINDOW_TRIGGERS]++;
                    }

                    // Update the verified count for each active TS bit.
                    if (isActiveBitRead) {
                        for (TriggerType trigger : TriggerType.values()) {
                            if (tsFlags[trigger.ordinal()]) {
                                if (clusterType == Cluster[].class) {
                                    matchedTriggerCount[SOURCE_SIM_CLUSTER][trigger.ordinal()]++;
                                } else {
                                    matchedTriggerCount[SOURCE_VTP_CLUSTER][trigger.ordinal()]++;
                                }
                            }
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

            if (debug) {
                if (clusterType == Cluster[].class) {
                    System.out.println("==== Simulated Sim Trigger to Hardware Trigger Verification Fail ==========");
                } else {
                    System.out.println("==== Hardware Sim Trigger to Hardware Trigger Verification Fail ===========");
                }

                PairTrigger2019<?> simPairTrigger = (PairTrigger2019<?>) simTrigger;
                System.out.println("software trigger: ");
                System.out.println("\ttime" + getTriggerTime(simTrigger));
                System.out.println(
                        "\tstate of energy low, state energy high, state of hit count, state of time coincidence: "
                                + simPairTrigger.getStateClusterEnergyLow() + ", "
                                + simPairTrigger.getStateClusterEnergyHigh() + ", " + simPairTrigger.getStateHitCount()
                                + ", " + simPairTrigger.getStateTimeCoincidence());
                System.out.println(
                        "\tstate of energy sum, state of energy slope, state of energy slope, state of coplanarity: "
                                + simPairTrigger.getStateEnergySum() + ", " + simPairTrigger.getStateEnergyDifference()
                                + ", " + simPairTrigger.getStateEnergySlope() + ", "
                                + simPairTrigger.getStateCoplanarity());

                System.out.println("hardware triggers: ");
                for (VTPPairsTrigger hardwareTrigger : hardwareTriggers) {
                    VTPPairsTrigger hardwarePairTrigger = (VTPPairsTrigger) hardwareTrigger;
                    System.out.println("\ttime" + hardwarePairTrigger.getTime());
                    System.out.println(
                            "\tstate of energy sum, state of energy slope, state of energy slope, state of coplanarity: "
                                    + hardwarePairTrigger.passESum() + ", " + hardwarePairTrigger.passEDiff() + ", "
                                    + hardwarePairTrigger.passESlope() + ", " + hardwarePairTrigger.passCoplanarity());
                }
            }
        }

        // The matched trigger set is equivalent in size to the number
        // of triggers that successfully passed verification.
        return matchedTriggers.size();
    }

    /**
     * Generates a <code>String</code> consisting of the indicated number of '='
     * characters.
     * 
     * @param length - The number of characters that should be present in the
     *               <code>String</code>.
     * @return Returns a <code>String</code> of '=' characters.
     */
    private static final String generateLine(int length) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < length; i++) {
            buffer.append('=');
        }
        return buffer.toString();
    }

    /**
     * Gets the a of <code>CalorimeterHit</code> objects from the argument event
     * from the LCIO collection <code>hitCollectionName</code>.
     * 
     * @param event             - The event object.
     * @param hitCollectionName - The name of the hit collection.
     * @return Returns either a list of <code>CalorimeterHit</code> objects, or
     *         returns <code>null</code> if no collection was found.
     */
    private static final List<CalorimeterHit> getCalorimeterHits(EventHeader event, String hitCollectionName) {
        // Get the list of calorimeter hits from the event.
        List<CalorimeterHit> fadcHits = null;
        if (event.hasCollection(CalorimeterHit.class, hitCollectionName)) {
            fadcHits = event.get(CalorimeterHit.class, hitCollectionName);
        }

        // Return the FADC hit collection.
        return fadcHits;
    }

    /**
     * A helper method associated with <code>getTriggerTime</code> that handles pair
     * triggers, which have either <code>Cluster[]</code> or
     * <code>VTPCluster[]</code> objects as their source type. <b>This method should
     * not be called directly.</b>
     * 
     * @param trigger - The trigger object for which to obtain the trigger time.
     * @return Returns the trigger time as a <code>double</code>.
     * @throws IllegalArgumentException Occurs if the trigger source object type is
     *                                  anything other than <code>Cluster[]</code>
     *                                  or <code>VTPCluster[]</code>.
     */
    private static final double getPairTriggerTime(Trigger<?> trigger) throws IllegalArgumentException {
        // Get the cluster times and y positions as appropriate to the
        // cluster object type.
        double[] time = new double[2];
        if (trigger.getTriggerSource() instanceof VTPCluster[]
                && ((VTPCluster[]) trigger.getTriggerSource()).length == 2) {
            VTPCluster[] pair = (VTPCluster[]) trigger.getTriggerSource();
            for (int i = 0; i < 2; i++) {
                time[i] = TriggerModule2019.getClusterTime(pair[i]);
            }
        } else if (trigger.getTriggerSource() instanceof Cluster[]
                && ((Cluster[]) trigger.getTriggerSource()).length == 2) {
            Cluster[] pair = (Cluster[]) trigger.getTriggerSource();
            for (int i = 0; i < 2; i++) {
                time[i] = TriggerModule2019.getClusterTime(pair[i]);
            }
        } else {
            throw new IllegalArgumentException("Trigger source object type is not recognized.");
        }

        if (time[0] <= time[1])
            return time[0];
        else
            return time[1];
    }

    /**
     * Get a name for a plot.
     * 
     * @param footer
     * @param cut
     * @param tsBit
     * @param sourceType
     * @return
     */
    private final String getPlotName(String footer, CutType cut, TriggerType tsBit, int sourceType) {
        // Make sure that a cut was defined.
        if (cut == null) {
            throw new NullPointerException("Plot cut type was not defined.");
        }

        // Make sure a valid source type is defined.
        if (sourceType != SOURCE_SIM_CLUSTER && sourceType != SOURCE_VTP_CLUSTER) {
            throw new NullPointerException("\"" + sourceType + "\" is not a valid source type index.");
        }

        // Get the appropriate name for the TS bit.
        String tsName = getPlotTSName(tsBit);

        // Define the source type name.
        String sourceName;
        if (sourceType == SOURCE_SIM_CLUSTER) {
            sourceName = "Software Sim Distributions/";
        } else {
            sourceName = "Hardware Sim Distributions/";
        }

        // Return the name of the coplanarity plot.
        return moduleHeader + sourceName + tsName + "/" + cut.toString() + footer;
    }

    /**
     * Get a name for a plot with type of Efficiency.
     * 
     * @param cut
     * @param tsBit
     * @param sourceType
     * @return
     */
    private final String getPlotNameEfficiency(CutType cut, TriggerType tsBit, int sourceType) {
        return getPlotName(" Efficiency", cut, tsBit, sourceType);
    }

    /**
     * Get a name for a plot with type of Observed
     * 
     * @param cut
     * @param tsBit
     * @param sourceType
     * @return
     */
    private final String getPlotNameTotal(CutType cut, TriggerType tsBit, int sourceType) {
        return getPlotName(" (Observed)", cut, tsBit, sourceType);
    }

    /**
     * Get a name for a plot with type of Verified
     * 
     * @param cut
     * @param tsBit
     * @param sourceType
     * @return
     */
    private final String getPlotNameVerified(CutType cut, TriggerType tsBit, int sourceType) {
        return getPlotName(" (Verified)", cut, tsBit, sourceType);
    }

    /**
     * Returns the name of the trigger type in the argument, or "All" if a null
     * argument is given.
     * 
     * @param tiBit - The trigger type.
     * @return Returns either the name of the trigger type or "All."
     */
    private static final String getPlotTSName(TriggerType tsBit) {
        if (tsBit == null) {
            return "All";
        } else {
            return tsBit.toString();
        }
    }

    /**
     * Get error of a ratio
     * 
     * @param num
     * @param sigmaNum
     * @param den
     * @param sigmaDen
     * @return
     */
    private static final double getRatioError(double num, double sigmaNum, double den, double sigmaDen) {
        double ratio = num / den;
        return Math.abs(ratio) * Math.sqrt(Math.pow(sigmaNum / num, 2) + Math.pow(sigmaDen / den, 2));
    }

    /**
     * A helper method associated with <code>getTriggerTime</code> that handles
     * singles triggers, which have either <code>Cluster</code> or
     * <code>VTPCluster</code> objects as their source type. <b>This method should
     * not be called directly.</b>
     * 
     * @param trigger - The trigger object for which to obtain the trigger time.
     * @return Returns the trigger time as a <code>double</code>.
     * @throws IllegalArgumentException Occurs if the trigger source object type is
     *                                  anything other than <code>Cluster</code> or
     *                                  <code>VTPCluster</code>.
     */
    private static final double getSinglesTriggerTime(Trigger<?> trigger) throws IllegalArgumentException {
        // Get the trigger time as appropriate to the source object type.
        if (trigger.getTriggerSource() instanceof VTPCluster) {
            return TriggerModule2019.getClusterTime((VTPCluster) trigger.getTriggerSource());
        } else if (trigger.getTriggerSource() instanceof Cluster) {
            return TriggerModule2019.getClusterTime((Cluster) trigger.getTriggerSource());
        } else {
            throw new IllegalArgumentException("Trigger source object type is not recognized.");
        }
    }

    /**
     * Gets a textual representation of the trigger source cluster(s).
     * 
     * @param trigger - The trigger for which to obtain the textual representation.
     * @return Returns a <code>String</code> array containing a textual
     *         representation of the trigger's source cluster(s). Each entry in the
     *         array represents an individual cluster.
     * @throws IllegalArgumentException Occurs if the trigger source object type is
     *                                  anything other than <code>Cluster</code>,
     *                                  <code>VTPCluster</code>, or an array of
     *                                  either of these two object types.
     */
    private static final String[] getTriggerSourceText(Trigger<?> trigger) throws IllegalArgumentException {
        if (trigger.getTriggerSource() instanceof Cluster) {
            return new String[] { TriggerDiagnosticUtil.clusterToString((Cluster) trigger.getTriggerSource()) };
        } else if (trigger.getTriggerSource() instanceof VTPCluster) {
            return new String[] { TriggerDiagnosticUtil.clusterToString((VTPCluster) trigger.getTriggerSource()) };
        } else if (trigger.getTriggerSource() instanceof Cluster[]) {
            Cluster[] source = (Cluster[]) trigger.getTriggerSource();
            String[] text = new String[source.length];
            for (int i = 0; i < source.length; i++) {
                text[i] = TriggerDiagnosticUtil.clusterToString(source[i]);
            }
            return text;
        } else if (trigger.getTriggerSource() instanceof VTPCluster[]) {
            VTPCluster[] source = (VTPCluster[]) trigger.getTriggerSource();
            String[] text = new String[source.length];
            for (int i = 0; i < source.length; i++) {
                text[i] = TriggerDiagnosticUtil.clusterToString(source[i]);
            }
            return text;
        } else {
            throw new IllegalArgumentException("Trigger source object type is not recognized.");
        }
    }

    /**
     * Gets a textual representation of the trigger.
     * 
     * @param trigger       - The trigger for which to obtain the textual
     *                      representation.
     * @param includeSource - Indicates whether a textual representation of the
     *                      source objects for the trigger should be included. These
     *                      will be present on a new line, one for each cluster.
     * @return Returns a <code>String</code> object representing the trigger. If
     *         <code>includeSource</code> is set to true, this will be more than one
     *         line.
     * @throws IllegalArgumentException Occurs if the trigger source object type is
     *                                  anything other than <code>Cluster</code>,
     *                                  <code>VTPCluster</code>, or an array of
     *                                  either of these two object types.
     */
    private final String getTriggerText(Trigger<?> trigger, boolean includeSource) throws IllegalArgumentException {
        // Define the trigger strings.
        final String singlesString = "t = %3.0f; EMin: %5b; EMax: %5b; Hit: %5b; XMin: %5b; PDE: %5b; L1: %5b; L2: %5b; L1L2: %5b; HodoEcal: %5b";
        final String doubleString = "t = %3.0f; EMin: %5b; EMax: %5b; Hit: %5b; Sum: %5b; Diff: %5b; Slope: %5b; Coplanarity: %5b; Time: %5b";
        final String doubleStringPair3 = "t = %3.0f; EMin: %5b; EMax: %5b; Hit: %5b; Sum: %5b; Diff: %5b; Slope: %5b; Coplanarity: %5b;  Time: %5b; L1L2 Coincidence Top: %5b; L1L2 Matching Top: %5b; HodoEcal Matching Top: %5b; L1L2 Coincidence Bot: %5b; L1L2 Matching Bot: %5b; HodoEcal Matching Bot: %5b";

        // If this is singles trigger...
        if (isSinglesTrigger(trigger)) {
            SinglesTrigger2019<?> singlesTrigger = (SinglesTrigger2019<?>) trigger;
            StringBuffer triggerText = new StringBuffer(
                    String.format(singlesString, getTriggerTime(trigger), singlesTrigger.getStateClusterEnergyLow(),
                            singlesTrigger.getStateClusterEnergyHigh(), singlesTrigger.getStateHitCount(),
                            singlesTrigger.getStateClusterXMin(), singlesTrigger.getStateClusterPDE(),
                            singlesTrigger.getStateHodoL1Matching(), singlesTrigger.getStateHodoL2Matching(),
                            singlesTrigger.getStateHodoL1L2Matching(), singlesTrigger.getStateHodoEcalMatching()));

            if (includeSource) {
                triggerText.append("\n\t\t" + getTriggerSourceText(trigger)[0]);
            }

            List<CalorimeterHit> hodoHitList = singlesTrigger.getHodoHitList();
            for (CalorimeterHit hit : hodoHitList) {
                Long hodoChannelId = getHodoChannelID(hit);
                triggerText.append(String.format(
                        "\n\t\tHodo hit: y = %3d; layer = %3d; x = %3d; hole = %3d; E = %5.3f;  t = %3.0f",
                        channelMap.get(hodoChannelId).getIY(), channelMap.get(hodoChannelId).getLayer(),
                        channelMap.get(hodoChannelId).getIX(), channelMap.get(hodoChannelId).getHole(),
                        hit.getRawEnergy(), hit.getTime()));
            }
            Map<Integer, HodoscopePattern> patternMap = singlesTrigger.getHodoPatternMap();
            triggerText.append(String.format("\n\t\tLayer %d %s", SinglesTrigger2019.LAYER1,
                    patternMap.get(SinglesTrigger2019.LAYER1)));
            triggerText.append(String.format("\n\t\tLayer %d %s", SinglesTrigger2019.LAYER2,
                    patternMap.get(SinglesTrigger2019.LAYER2)));

            return triggerText.toString();
        }

        // If this is a pair trigger...
        if (isPairTrigger(trigger)) {
            if(triggerType == TriggerType.PAIR3) {
                PairTrigger2019<?> pairTrigger = (PairTrigger2019<?>) trigger;
                StringBuffer triggerText = new StringBuffer(String.format(doubleStringPair3, getTriggerTime(trigger),
                        pairTrigger.getStateClusterEnergyLow(), pairTrigger.getStateClusterEnergyHigh(),
                        pairTrigger.getStateHitCount(), pairTrigger.getStateEnergySum(),
                        pairTrigger.getStateEnergyDifference(), pairTrigger.getStateEnergySlope(),
                        pairTrigger.getStateCoplanarity(), pairTrigger.getStateTimeCoincidence(), 
                        pairTrigger.getStateHodoL1L2CoincidenceTop(), pairTrigger.getStateHodoL1L2MatchingTop(), pairTrigger.getStateHodoEcalMatchingTop(), 
                        pairTrigger.getStateHodoL1L2CoincidenceBot(), pairTrigger.getStateHodoL1L2MatchingBot(), pairTrigger.getStateHodoEcalMatchingBot()));
                if (includeSource) {
                    String[] sourceText = getTriggerSourceText(trigger);
                    triggerText.append("\n\t\t" + sourceText[0]);
                    triggerText.append("\n\t\t" + sourceText[1]);
                }
                
                List<CalorimeterHit> hodoHitList = pairTrigger.getHodoHitList();
                for (CalorimeterHit hit : hodoHitList) {
                    Long hodoChannelId = getHodoChannelID(hit);
                    triggerText.append(String.format(
                            "\n\t\tHodo hit: y = %3d; layer = %3d; x = %3d; hole = %3d; E = %5.3f;  t = %3.0f",
                            channelMap.get(hodoChannelId).getIY(), channelMap.get(hodoChannelId).getLayer(),
                            channelMap.get(hodoChannelId).getIX(), channelMap.get(hodoChannelId).getHole(),
                            hit.getRawEnergy(), hit.getTime()));
                }
                Map<Integer, HodoscopePattern> patternMap = pairTrigger.getHodoPatternMap();
                triggerText.append(String.format("\n\t\tLayer %d %s", SinglesTrigger2019.LAYER1,
                        patternMap.get(0)));
                triggerText.append(String.format("\n\t\tLayer %d %s", SinglesTrigger2019.LAYER2,
                        patternMap.get(1)));
                triggerText.append(String.format("\n\t\tLayer %d %s", SinglesTrigger2019.LAYER1,
                        patternMap.get(2)));
                triggerText.append(String.format("\n\t\tLayer %d %s", SinglesTrigger2019.LAYER2,
                        patternMap.get(3)));
                
                return triggerText.toString();
            }
            else {
                PairTrigger2019<?> pairTrigger = (PairTrigger2019<?>) trigger;
                StringBuffer triggerText = new StringBuffer(String.format(doubleString, getTriggerTime(trigger),
                        pairTrigger.getStateClusterEnergyLow(), pairTrigger.getStateClusterEnergyHigh(),
                        pairTrigger.getStateHitCount(), pairTrigger.getStateEnergySum(),
                        pairTrigger.getStateEnergyDifference(), pairTrigger.getStateEnergySlope(),
                        pairTrigger.getStateCoplanarity(), pairTrigger.getStateTimeCoincidence()));
                if (includeSource) {
                    String[] sourceText = getTriggerSourceText(trigger);
                    triggerText.append("\n\t\t" + sourceText[0]);
                    triggerText.append("\n\t\t" + sourceText[1]);
                }
                return triggerText.toString();
            }
        }

        // Otherwise, the trigger type is invalid.
        else {
            throw new IllegalArgumentException("Trigger source object type is not recognized.");
        }
    }

    /**
     * Gets a textual representation of the trigger.
     * 
     * @param trigger - The trigger for which to obtain the textual representation.
     * @return Returns a <code>String</code> object representing the trigger.
     * @throws IllegalArgumentException Occurs if the trigger subclass is not either
     *                                  an <code>VTPSinglesTrigger</code> object or
     *                                  an <code>VTPPairTrigger</code> object.
     */
    private final String getTriggerText(VTPSinglesTrigger trigger) {
        // Define the trigger string.
        final String singlesString = "t = %3d; EMin: %5b; EMax: %5b; Hit: %5b; XMin: %5b; PDE: %5b; HodoL1: %5b; HodoL2: %5b; L1L2: %5b; HodoEcal: %5b";

        StringBuffer triggerText = new StringBuffer(String.format(singlesString, trigger.getTime(), trigger.passEMin(),
                trigger.passEMax(), trigger.passNMin(), trigger.passXMin(), trigger.passPDET(), trigger.passHodo1(),
                trigger.passHodo2(), trigger.passHodoGeo(), trigger.passHodoECal()));

        return triggerText.toString();

    }

    private final String getTriggerText(VTPPairsTrigger trigger) {
        // Define the trigger string.
        final String doubleString = "t = %3d; EMin: %5b; EMax: %5b; Hit: %5b; Time: %5b; Sum: %5b; Diff: %5b; Slope: %5b; Coplanarity: %5b";
        final String doubleStringPair3 = "t = %3d; EMin: %5b; EMax: %5b; Hit: %5b; Time: %5b; Sum: %5b; Diff: %5b; Slope: %5b; Coplanarity: %5b; L1L2 Coincidence Top: %5b; L1L2 Matching Top: %5b; HodoEcal Matching Top: %5b; L1L2 Coincidence Bot: %5b; L1L2 Matching Bot: %5b; HodoEcal Matching Bot: %5b";

        StringBuffer triggerText;
        
        if(triggerType == TriggerType.PAIR3) triggerText= new StringBuffer(String.format(doubleStringPair3, trigger.getTime(), true, true, true,
                true, trigger.passESum(), trigger.passEDiff(), trigger.passESlope(), trigger.passCoplanarity(),
                trigger.passL1L2CoincidenceTop(), trigger.passHodoL1L2MatchingTop(), trigger.passHodoEcalMatchingTop(), 
                trigger.passL1L2CoincidenceBot(), trigger.passHodoL1L2MatchingBot(), trigger.passHodoEcalMatchingBot()));
        else triggerText = new StringBuffer(String.format(doubleString, trigger.getTime(), true, true, true,
                true, trigger.passESum(), trigger.passEDiff(), trigger.passESlope(), trigger.passCoplanarity()));

        return triggerText.toString();

    }

    /**
     * Gets the trigger time for an arbitrary trigger object as is appropriate to
     * its source object type.
     * 
     * @param trigger - The trigger object for which to obtain the trigger time.
     * @return Returns the trigger time as a <code>double</code>.
     * @throws IllegalArgumentException Occurs if the trigger source object type is
     *                                  anything other than <code>Cluster</code>,
     *                                  <code>VTPCluster</code>, or a size-two array
     *                                  of either of the previous two object types.
     */
    private static final double getTriggerTime(Trigger<?> trigger) throws IllegalArgumentException {
        // Pass the trigger to one of the sub-handlers appropriate to
        // its source type.
        if (trigger.getTriggerSource() instanceof Cluster || trigger.getTriggerSource() instanceof VTPCluster) {
            return getSinglesTriggerTime(trigger);
        } else if (trigger.getTriggerSource() instanceof Cluster[]
                || trigger.getTriggerSource() instanceof VTPCluster[]) {
            return getPairTriggerTime(trigger);
        } else {
            throw new IllegalArgumentException("Trigger source object type is not recognized.");
        }
    }

    /**
     * Indicates whether a generic trigger object is a pair trigger.
     * 
     * @param trigger - The trigger to check.
     * @return Returns <code>true</code> in the case that the source object is a
     *         <code>Cluster[]</code>, or <code>VTPCluster[]</code>. Otherwise,
     *         returns <code>false</code>.
     */
    private static final boolean isPairTrigger(Trigger<?> trigger) {
        // Get the size of the trigger source cluster array, if it is
        // an array of a valid type.
        int size = -1;
        if (trigger.getTriggerSource() instanceof Cluster[]) {
            size = ((Cluster[]) trigger.getTriggerSource()).length;
        } else if (trigger.getTriggerSource() instanceof VTPCluster[]) {
            size = ((VTPCluster[]) trigger.getTriggerSource()).length;
        }

        // If the source object is either not of the appropriate type
        // or not a size two array, it is not a pair trigger.
        return size == 2;
    }

    /**
     * Indicates whether a generic trigger object is a singles trigger.
     * 
     * @param trigger - The trigger to check.
     * @return Returns <code>true</code> in the case that the source object is a
     *         <code>Cluster</code> or <code>VTPCluster</code>. Otherwise, returns
     *         <code>false</code>.
     */
    private static final boolean isSinglesTrigger(Trigger<?> trigger) {
        return trigger.getTriggerSource() instanceof Cluster || trigger.getTriggerSource() instanceof VTPCluster;
    }

    /**
     * Gets the maximum value in a set of integers.
     * 
     * @param values - The values from which to find the maximum.
     * @return Returns whichever value is the highest from within the set.
     * @throws IllegalArgumentException Occurs if no arguments are given.
     */
    private static final int max(int... values) throws IllegalArgumentException {
        // There must be at least one value.
        if (values.length == 0) {
            throw new IllegalArgumentException("Must define at least one value.");
        }

        // Determine which value is the largest.
        int maxValue = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] > maxValue) {
                maxValue = values[i];
            }
        }

        // Return the result.
        return maxValue;
    }

    /**
     * Fill plots.
     * 
     * @param trigger
     * @param activeTSBits
     * @param verified
     */
    private void plotTrigger(Trigger<?> trigger, boolean[] activeTSBits, boolean verified) {
        // Which plots are to be populated depends on the type of
        // trigger. First, handle singles triggers.
        if (trigger.getTriggerSource() instanceof Cluster || trigger.getTriggerSource() instanceof VTPCluster) {
            // Define the plot values.
            int sourceType;
            double clusterEnergy;
            double hitCount;
            double xIndex;
            double pde;
            double eventTime = getTriggerTime(trigger);

            // Get the values. This will depend on the cluster type.
            if (trigger.getTriggerSource() instanceof Cluster) {
                // Fill the plot value variables.
                Cluster cluster = (Cluster) trigger.getTriggerSource();
                clusterEnergy = TriggerModule2019.getValueClusterTotalEnergy(cluster);
                hitCount = TriggerModule2019.getClusterHitCount(cluster);
                xIndex = TriggerModule2019.getClusterXIndex(cluster);
                pde = singlesTrigger[triggerType.getTriggerNum()].getClusterPDE(cluster);

                // Note that the source type is a sim cluster.
                sourceType = SOURCE_SIM_CLUSTER;
            } else if (trigger.getTriggerSource() instanceof VTPCluster) {
                // Fill the plot value variables.
                VTPCluster cluster = (VTPCluster) trigger.getTriggerSource();
                clusterEnergy = TriggerModule2019.getValueClusterTotalEnergy(cluster);
                hitCount = TriggerModule2019.getClusterHitCount(cluster);
                xIndex = TriggerModule2019.getClusterXIndex(cluster);
                pde = singlesTrigger[triggerType.getTriggerNum()].getClusterPDE(cluster);

                // Note that the source type is an VTP cluster.
                sourceType = SOURCE_VTP_CLUSTER;
            } else {
                throw new IllegalArgumentException("Trigger source "
                        + trigger.getTriggerSource().getClass().getSimpleName() + " is not recognized.");
            }

            // Populate the appropriate trigger plot.
            for (TriggerType tsBit : triggerTypes) {
                if (!isActiveBitRead && !(tsBit == null))
                    continue;

                if (tsBit == null || activeTSBits[tsBit.ordinal()]) {
                    if (verified) {
                        AIDA.defaultInstance().histogram1D(getPlotNameVerified(CutType.EVENT_TIME, tsBit, sourceType))
                                .fill(eventTime);
                        AIDA.defaultInstance()
                                .histogram1D(getPlotNameVerified(CutType.CLUSTER_HIT_COUNT, tsBit, sourceType))
                                .fill(hitCount);
                        AIDA.defaultInstance()
                                .histogram1D(getPlotNameVerified(CutType.CLUSTER_TOTAL_ENERGY, tsBit, sourceType))
                                .fill(clusterEnergy);
                        AIDA.defaultInstance()
                                .histogram1D(getPlotNameVerified(CutType.CLUSTER_XINDEX, tsBit, sourceType))
                                .fill(xIndex);
                        AIDA.defaultInstance().histogram1D(getPlotNameVerified(CutType.CLUSTER_PDE, tsBit, sourceType))
                                .fill(pde);
                    } else {
                        AIDA.defaultInstance().histogram1D(getPlotNameTotal(CutType.EVENT_TIME, tsBit, sourceType))
                                .fill(eventTime);
                        AIDA.defaultInstance()
                                .histogram1D(getPlotNameTotal(CutType.CLUSTER_HIT_COUNT, tsBit, sourceType))
                                .fill(hitCount);
                        AIDA.defaultInstance()
                                .histogram1D(getPlotNameTotal(CutType.CLUSTER_TOTAL_ENERGY, tsBit, sourceType))
                                .fill(clusterEnergy);
                        AIDA.defaultInstance().histogram1D(getPlotNameTotal(CutType.CLUSTER_XINDEX, tsBit, sourceType))
                                .fill(xIndex);
                        AIDA.defaultInstance().histogram1D(getPlotNameTotal(CutType.CLUSTER_PDE, tsBit, sourceType))
                                .fill(pde);
                    }
                }
            }
        } else if (trigger.getTriggerSource() instanceof Cluster[]
                || trigger.getTriggerSource() instanceof VTPCluster[]) {
            // Define the plot values.
            int sourceType;
            double energySum;
            double energyDiff;
            double energySlope;
            double coplanarity;
            double timeCoincidence;
            double clusterLow;
            double clusterHigh;
            double eventTime = getTriggerTime(trigger);

            // How the values are filled depends on the source cluster
            // type.
            if (trigger.getTriggerSource() instanceof Cluster[]) {
                // Fill the plot value variables.
                Cluster[] pair = (Cluster[]) trigger.getTriggerSource();
                energySum = TriggerModule2019.getValueEnergySum(pair);
                energyDiff = TriggerModule2019.getValueEnergyDifference(pair);
                energySlope = TriggerModule2019.getValueEnergySlope(pair,
                        pairTrigger[triggerType.getTriggerNumber()].getCutValue(TriggerModule2019.PAIR_ENERGY_SLOPE_F));
                coplanarity = TriggerModule2019.getValueCoplanarity(pair);
                timeCoincidence = TriggerModule2019.getValueTimeCoincidence(pair);
                clusterLow = Math.min(TriggerModule2019.getValueClusterTotalEnergy(pair[0]),
                        TriggerModule2019.getValueClusterTotalEnergy(pair[1]));
                clusterHigh = Math.max(TriggerModule2019.getValueClusterTotalEnergy(pair[0]),
                        TriggerModule2019.getValueClusterTotalEnergy(pair[1]));

                // Note that the source type is a sim cluster.
                sourceType = SOURCE_SIM_CLUSTER;
            } else if (trigger.getTriggerSource() instanceof VTPCluster[]) {
                // Fill the plot value variables.
                VTPCluster[] pair = (VTPCluster[]) trigger.getTriggerSource();
                energySum = TriggerModule2019.getValueEnergySum(pair);
                energyDiff = TriggerModule2019.getValueEnergyDifference(pair);
                energySlope = TriggerModule2019.getValueEnergySlope(pair,
                        pairTrigger[triggerType.getTriggerNumber()].getCutValue(TriggerModule2019.PAIR_ENERGY_SLOPE_F));
                coplanarity = TriggerModule2019.getValueCoplanarity(pair);
                timeCoincidence = TriggerModule2019.getValueTimeCoincidence(pair);
                clusterLow = Math.min(TriggerModule2019.getValueClusterTotalEnergy(pair[0]),
                        TriggerModule2019.getValueClusterTotalEnergy(pair[1]));
                clusterHigh = Math.max(TriggerModule2019.getValueClusterTotalEnergy(pair[0]),
                        TriggerModule2019.getValueClusterTotalEnergy(pair[1]));

                // Note that the source type is an VTP cluster.
                sourceType = SOURCE_VTP_CLUSTER;
            } else {
                throw new IllegalArgumentException("Trigger source "
                        + trigger.getTriggerSource().getClass().getSimpleName() + " is not recognized.");
            }

            // Fill the appropriate plots.
            for (TriggerType tsBit : triggerTypes) {
                if (!isActiveBitRead && !(tsBit == null))
                    continue;

                if (tsBit == null || activeTSBits[tsBit.ordinal()]) {
                    if (verified) {
                        AIDA.defaultInstance().histogram1D(getPlotNameVerified(CutType.EVENT_TIME, tsBit, sourceType))
                                .fill(eventTime);
                        AIDA.defaultInstance()
                                .histogram1D(getPlotNameVerified(CutType.PAIR_ENERGY_SUM, tsBit, sourceType))
                                .fill(energySum);
                        AIDA.defaultInstance()
                                .histogram1D(getPlotNameVerified(CutType.PAIR_ENERGY_DIFF, tsBit, sourceType))
                                .fill(energyDiff);
                        AIDA.defaultInstance()
                                .histogram1D(getPlotNameVerified(CutType.PAIR_ENERGY_SLOPE, tsBit, sourceType))
                                .fill(energySlope);
                        AIDA.defaultInstance()
                                .histogram1D(getPlotNameVerified(CutType.PAIR_COPLANARITY, tsBit, sourceType))
                                .fill(coplanarity);
                        AIDA.defaultInstance()
                                .histogram1D(getPlotNameVerified(CutType.PAIR_TIME_COINCIDENCE, tsBit, sourceType))
                                .fill(timeCoincidence);
                        AIDA.defaultInstance()
                                .histogram1D(getPlotNameVerified(CutType.PAIR_LOW_ENERGY, tsBit, sourceType))
                                .fill(clusterLow);
                        AIDA.defaultInstance()
                                .histogram1D(getPlotNameVerified(CutType.PAIR_HIGH_ENERGY, tsBit, sourceType))
                                .fill(clusterHigh);
                    } else {
                        AIDA.defaultInstance().histogram1D(getPlotNameTotal(CutType.EVENT_TIME, tsBit, sourceType))
                                .fill(eventTime);
                        AIDA.defaultInstance().histogram1D(getPlotNameTotal(CutType.PAIR_ENERGY_SUM, tsBit, sourceType))
                                .fill(energySum);
                        AIDA.defaultInstance()
                                .histogram1D(getPlotNameTotal(CutType.PAIR_ENERGY_DIFF, tsBit, sourceType))
                                .fill(energyDiff);
                        AIDA.defaultInstance()
                                .histogram1D(getPlotNameTotal(CutType.PAIR_ENERGY_SLOPE, tsBit, sourceType))
                                .fill(energySlope);
                        AIDA.defaultInstance()
                                .histogram1D(getPlotNameTotal(CutType.PAIR_COPLANARITY, tsBit, sourceType))
                                .fill(coplanarity);
                        AIDA.defaultInstance()
                                .histogram1D(getPlotNameTotal(CutType.PAIR_TIME_COINCIDENCE, tsBit, sourceType))
                                .fill(timeCoincidence);
                        AIDA.defaultInstance().histogram1D(getPlotNameTotal(CutType.PAIR_LOW_ENERGY, tsBit, sourceType))
                                .fill(clusterLow);
                        AIDA.defaultInstance()
                                .histogram1D(getPlotNameTotal(CutType.PAIR_HIGH_ENERGY, tsBit, sourceType))
                                .fill(clusterHigh);
                    }
                }
            }
        } else {
            throw new IllegalArgumentException(
                    "Trigger type " + trigger.getClass().getSimpleName() + " is not recognized.");
        }
    }

    /**
     * Sets the TS-bit flags for each trigger type and also indicates whether or not
     * at least one TI-bit was found active.
     * 
     * @param tsBank - The TS bank from which to set the flags.
     * @return Returns <code>true</code> if at least one TI-bit was found active,
     *         and <code>false</code> if no bits were active.
     */
    private boolean setTSFlags(TSData2019 tsData) {
        // Reset the TS flags and determine track whether some TS bit
        // can be found.
        tsFlags = new boolean[20];
        boolean activeBitRead = false;

        // Check each TS bit.
        if (tsData.isSingle0TopTrigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.SINGLESTOP0.ordinal()] = true;
            logger.println("Trigger type :: Singles Top 0");
        }
        if (tsData.isSingle1TopTrigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.SINGLESTOP1.ordinal()] = true;
            logger.println("Trigger type :: Singles Top 1");
        }
        if (tsData.isSingle2TopTrigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.SINGLESTOP2.ordinal()] = true;
            logger.println("Trigger type :: Singles Top 2");
        }
        if (tsData.isSingle3TopTrigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.SINGLESTOP3.ordinal()] = true;
            logger.println("Trigger type :: Singles Top 3");
        }

        if (tsData.isSingle0BotTrigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.SINGLESBOT0.ordinal()] = true;
            logger.println("Trigger type :: Singles Bot 0");
        }
        if (tsData.isSingle1BotTrigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.SINGLESBOT1.ordinal()] = true;
            logger.println("Trigger type :: Singles Bot 1");
        }
        if (tsData.isSingle2BotTrigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.SINGLESBOT2.ordinal()] = true;
            logger.println("Trigger type :: Singles Bot 2");
        }
        if (tsData.isSingle3BotTrigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.SINGLESBOT3.ordinal()] = true;
            logger.println("Trigger type :: Singles Bot 3");
        }

        if (tsData.isPair0Trigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.PAIR0.ordinal()] = true;
            logger.println("Trigger type :: Pair 0");
        }
        if (tsData.isPair1Trigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.PAIR1.ordinal()] = true;
            logger.println("Trigger type :: Pair 1");
        }
        if (tsData.isPair2Trigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.PAIR2.ordinal()] = true;
            logger.println("Trigger type :: Pair 2");
        }
        if (tsData.isPair3Trigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.PAIR3.ordinal()] = true;
            logger.println("Trigger type :: Pair 3");
        }

        if (tsData.isLEDTrigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.LED.ordinal()] = true;
            logger.println("Trigger type :: LED");
        }

        if (tsData.isCosmicTrigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.COSMIC.ordinal()] = true;
            logger.println("Trigger type :: Cosmic");
        }

        if (tsData.isHodoscopeTrigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.HODOSCOPE.ordinal()] = true;
            logger.println("Trigger type :: Hodoscope");
        }

        if (tsData.isMultiplicity0Trigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.MULTIPLICITY0.ordinal()] = true;
            logger.println("Trigger type :: Multiplicity0");
        }

        if (tsData.isMultiplicity1Trigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.MULTIPLICITY1.ordinal()] = true;
            logger.println("Trigger type :: Multiplicity1");
        }

        if (tsData.isFEETopTrigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.FEETOP.ordinal()] = true;
            logger.println("Trigger type :: FEE Top");
        }

        if (tsData.isFEEBotTrigger()) {
            activeBitRead = true;
            tsFlags[TriggerType.FEEBOT.ordinal()] = true;
            logger.println("Trigger type :: FEE Bot");
        }

        // Return whether or not a TS bit was found.
        return activeBitRead;
    }

    /**
     * Performs trigger verification for the specified trigger.
     * 
     * @param simTriggers - A data object containing all simulated triggers for all
     *                    trigger types.
     * @param vtpBank     - The data bank containing all of the triggers reported by
     *                    the hardware.
     * @param triggerType - The trigger which is to be verified.
     */
    private void triggerVerification(SimTriggerData2019 simTriggers, VTPData vtpBank, TriggerType triggerType) {
        // Trigger verification can not be performed for either pulser
        // or cosmic triggers. Null trigger types are also invalid.
        if (triggerType == null) {
            throw new IllegalArgumentException("Trigger verification type is not defined.");
        } else if (triggerType == null || triggerType.equals(TriggerType.COSMIC)
                || triggerType.equals(TriggerType.PULSER)) {
            throw new IllegalArgumentException(
                    "Verification for trigger type \"" + triggerType.toString() + "\" is not supported.");
        } else if (!(triggerType.equals(TriggerType.SINGLESTOP0) || triggerType.equals(TriggerType.SINGLESTOP1)
                || triggerType.equals(TriggerType.SINGLESTOP2) || triggerType.equals(TriggerType.SINGLESTOP3)
                || triggerType.equals(TriggerType.SINGLESBOT0) || triggerType.equals(TriggerType.SINGLESBOT1)
                || triggerType.equals(TriggerType.SINGLESBOT2) || triggerType.equals(TriggerType.SINGLESBOT3)
                || triggerType.equals(TriggerType.PAIR0) || triggerType.equals(TriggerType.PAIR1)
                || triggerType.equals(TriggerType.PAIR2) || triggerType.equals(TriggerType.PAIR3))) {
            throw new IllegalArgumentException("Verification for trigger type is not a known trigger type.");
        }

        // Get the VTP triggers for the appropriate trigger type.
        List<VTPSinglesTrigger> hardwareSinglesTriggers = new ArrayList<VTPSinglesTrigger>();
        List<VTPPairsTrigger> hardwarePairsTriggers = new ArrayList<VTPPairsTrigger>();

        if (triggerType.isSinglesTrigger()) {
            for (VTPSinglesTrigger trigger : vtpBank.getSinglesTriggers()) {
                if (trigger.getTriggerInstance() == triggerType.getTriggerNumber()) {
                    hardwareSinglesTriggers.add(trigger);
                }
            }
        } else {
            for (VTPPairsTrigger trigger : vtpBank.getPairsTriggers()) {
                if (trigger.getTriggerInstance() == triggerType.getTriggerNumber()) {
                    hardwarePairsTriggers.add(trigger);
                }
            }
        }

        // Get the triggers simulated from both hardware VTP clusters
        // and software clusters.
        List<Trigger<?>> hardwareSimTriggers = new ArrayList<Trigger<?>>();
        List<Trigger<?>> softwareSimTriggers = new ArrayList<Trigger<?>>();

        if (triggerType.isSinglesTrigger()) {
            // Add all of the VTP triggers.
            for (SinglesTrigger2019<VTPCluster> trigger : simTriggers.getSimHardwareClusterTriggers()
                    .getSinglesTriggers(triggerType.getTriggerNumber())) {
                // Extract triggers with unclipped hodo hits and trigger time < windowWidthHodo
                // + offsetEcal - offsetHodo
                // After tiem alignment with Ecal, the readout window of hodo is [0 + offsetEcal
                // - offsetHodo, windowWidthHodo + offsetEcal - offsetHodo)
                // With consideration of hodoscope hit persistence, for fair comparison, trigger
                // time must be in the range of [hodoDT + offsetEcal - offsetHodo,
                // windowWidthHodo + offsetEcal - offsetHodo)
                // To not cancel too many triggers, we do not set the lower limit
                if (TriggerDiagnosticUtil.isVerifiableHodoHits(trigger, VTPCluster.class, nsaHodo, nsbHodo,
                        windowWidthHodo) && getTriggerTime(trigger) < windowWidthHodo + offsetEcal - offsetHodo) {
                    if(triggerType.isSinglesTopTrigger() && trigger.getTopnbot() == 1) hardwareSimTriggers.add(trigger);
                    if(triggerType.isSinglesBotTrigger() && trigger.getTopnbot() == 0) hardwareSimTriggers.add(trigger);
                }
            }

            // Add only the simulated triggers that were generated
            // from clusters that are not at risk of pulse-clipping.
            for (SinglesTrigger2019<Cluster> trigger : simTriggers.getSimSoftwareClusterTriggers()
                    .getSinglesTriggers(triggerType.getTriggerNumber())) {
                // Extract triggers with unclipped cluster, unclipped hodo hits and hodoDT +
                // offsetEcal - offsetHodo <= trigger time < windowWidthHodo + offsetEcal -
                // offsetHodo
                if (TriggerDiagnosticUtil.isVerifiable(trigger.getTriggerSource(), nsaEcal, nsbEcal, windowWidthEcal)
                        && TriggerDiagnosticUtil.isVerifiableHodoHits(trigger, Cluster.class, nsaHodo, nsbHodo,
                                windowWidthHodo)
                        && getTriggerTime(trigger) < windowWidthHodo + offsetEcal - offsetHodo) {
                    if(triggerType.isSinglesTopTrigger() && trigger.getTopnbot() == 1) softwareSimTriggers.add(trigger);
                    if(triggerType.isSinglesBotTrigger() && trigger.getTopnbot() == 0) softwareSimTriggers.add(trigger);
                }
            }
        } else if (triggerType.isPairTrigger()) {
            // Add all of the VTP triggers.
            for (PairTrigger2019<VTPCluster[]> trigger : simTriggers.getSimHardwareClusterTriggers().getPairTriggers(triggerType.getTriggerNumber())) {
                if (triggerType != TriggerType.PAIR3)
                    hardwareSimTriggers.add(trigger);
                // Pair3 trigger requires geometry matching for hodoscope and ecal
                else {
                    if (TriggerDiagnosticUtil.isVerifiableHodoHits(trigger, VTPCluster.class, nsaHodo, nsbHodo,
                            windowWidthHodo) && getTriggerTime(trigger) < windowWidthHodo + offsetEcal - offsetHodo)
                        hardwareSimTriggers.add(trigger);
                }
            }
                    

            // Add only the simulated triggers that were generated
            // from clusters that are not at risk of pulse-clipping.
            for (PairTrigger2019<Cluster[]> trigger : simTriggers.getSimSoftwareClusterTriggers()
                    .getPairTriggers(triggerType.getTriggerNumber())) {
                if (TriggerDiagnosticUtil.isVerifiable(trigger.getTriggerSource()[0], nsaEcal, nsbEcal, windowWidthEcal)
                        && TriggerDiagnosticUtil.isVerifiable(trigger.getTriggerSource()[1], nsaEcal, nsbEcal,
                                windowWidthEcal)) {
                    if (triggerType != TriggerType.PAIR3)
                        softwareSimTriggers.add(trigger);
                    // Pair3 trigger requires geometry matching for hodoscope and ecal
                    else {
                        if (TriggerDiagnosticUtil.isVerifiableHodoHits(trigger, Cluster.class, nsaHodo, nsbHodo,
                                windowWidthHodo) && getTriggerTime(trigger) < windowWidthHodo + offsetEcal - offsetHodo)
                            softwareSimTriggers.add(trigger);
                    }
                    
                }
            }
        }

        // Output the trigger objects as text.
        logger.printNewLine(2);
        logger.println("=== Simulated Cluster Simulated Triggers =============================");
        if (!softwareSimTriggers.isEmpty()) {
            for (Trigger<?> trigger : softwareSimTriggers) {
                logger.println(getTriggerText(trigger, true));
            }
        } else {
            logger.println("None!");
        }
        logger.printNewLine(2);
        logger.println("=== Hardware Cluster Simulated Triggers ==============================");
        if (!hardwareSimTriggers.isEmpty()) {
            for (Trigger<?> trigger : hardwareSimTriggers) {
                logger.println(getTriggerText(trigger, true));
            }
        } else {
            logger.println("None!");
        }
        logger.printNewLine(2);
        logger.println("=== Hardware Triggers ================================================");
        if (triggerType.isSinglesTrigger()) {
            if (!hardwareSinglesTriggers.isEmpty()) {
                for (VTPSinglesTrigger trigger : hardwareSinglesTriggers) {
                    logger.println(getTriggerText(trigger));
                }
            } else {
                logger.println("None!");
            }
        }

        if (triggerType.isPairTrigger()) {
            if (!hardwarePairsTriggers.isEmpty()) {
                for (VTPPairsTrigger trigger : hardwarePairsTriggers) {
                    logger.println(getTriggerText(trigger));
                }
            } else {
                logger.println("None!");
            }
        }

        // Update the total count for each type of trigger for the local
        // and global windows.
        // Only one trigger type, so size of only one trigger list is not 0.
        hardwareTriggerCount[ALL_TRIGGERS] += hardwareSinglesTriggers.size() + hardwarePairsTriggers.size(); 
        hardwareTriggerCount[LOCAL_WINDOW_TRIGGERS] += hardwareSinglesTriggers.size() + hardwarePairsTriggers.size(); 
        
        for (TriggerType tsBit : TriggerType.values()) {
            // Only one trigger type, so size of only one trigger list is not 0.
            hardwareTriggerCount[tsBit.ordinal()] += hardwareSinglesTriggers.size() + hardwarePairsTriggers.size(); 
        }
        for (Trigger<?> trigger : softwareSimTriggers) {
            if (getTriggerTime(trigger) >= triggerWindowStart && getTriggerTime(trigger) <= triggerWindowEnd) {
                simTriggerCount[SOURCE_SIM_CLUSTER][ALL_TRIGGERS]++;
                simTriggerCount[SOURCE_SIM_CLUSTER][LOCAL_WINDOW_TRIGGERS]++;
                if (isActiveBitRead) {
                    for (TriggerType tsBit : TriggerType.values()) {
                        simTriggerCount[SOURCE_SIM_CLUSTER][tsBit.ordinal()] += tsFlags[tsBit.ordinal()] ? 1 : 0;
                    }
                }
            }
        }
        for (Trigger<?> trigger : hardwareSimTriggers) {
            if (getTriggerTime(trigger) >= triggerWindowStart && getTriggerTime(trigger) <= triggerWindowEnd) {
                simTriggerCount[SOURCE_VTP_CLUSTER][ALL_TRIGGERS]++;
                simTriggerCount[SOURCE_VTP_CLUSTER][LOCAL_WINDOW_TRIGGERS]++;
                if (isActiveBitRead) {
                    for (TriggerType tsBit : TriggerType.values()) {
                        simTriggerCount[SOURCE_VTP_CLUSTER][tsBit.ordinal()] += tsFlags[tsBit.ordinal()] ? 1 : 0;
                    }
                }
            }
        }

        // Print the observed trigger distributions.
        for (Trigger<?> trigger : softwareSimTriggers) {
            plotTrigger(trigger, tsFlags, false);
        }
        for (Trigger<?> trigger : hardwareSimTriggers) {
            plotTrigger(trigger, tsFlags, false);
        }

        // Run the trigger verification for each simulated trigger type.
        logger.printNewLine(2);
        logger.println("=== Performing Trigger Verification ==================================");
        logger.println("======================================================================");

        if (triggerType.isSinglesTrigger()) {
            // Perform trigger verification for each type of trigger.
            // Also store the number of triggers that are verified.
            int softwareSimVerifiedCount = compareSimulatedToHardwareSingles(softwareSimTriggers,
                    hardwareSinglesTriggers, Cluster.class);
            int hardwareSimVerifiedCount = compareSimulatedToHardwareSingles(hardwareSimTriggers,
                    hardwareSinglesTriggers, VTPCluster.class);

            // If the number of triggers that verified are not
            // equal to the number of simulated triggers of the
            // same type, then at least one trigger failed.
            if (softwareSimVerifiedCount != softwareSimTriggers.size()) {
                softwareSimFailure = true;
            }
            if (hardwareSimVerifiedCount != hardwareSimTriggers.size()) {
                hardwareSimFailure = true;
            }
        } else if (triggerType.isPairTrigger()) {
            // Perform trigger verification for each type of trigger.
            // Also store the number of triggers that are verified.
            int softwareSimVerifiedCount = compareSimulatedToHardwarePairs(softwareSimTriggers, hardwarePairsTriggers,
                    Cluster[].class);
            int hardwareSimVerifiedCount = compareSimulatedToHardwarePairs(hardwareSimTriggers, hardwarePairsTriggers,
                    VTPCluster[].class);

            // If the number of triggers that verified are not
            // equal to the number of simulated triggers of the
            // same type, then at least one trigger failed.
            if (softwareSimVerifiedCount != softwareSimTriggers.size()) {
                softwareSimFailure = true;
            }
            if (hardwareSimVerifiedCount != hardwareSimTriggers.size()) {
                hardwareSimFailure = true;
            }
        }
    }

    /**
     * Sets the name of LCIO collection containing the trigger banks.
     * 
     * @param collection - The collection name.
     */
    public void setBankVTPCollectionName(String collection) {
        bankVTPCollectionName = collection;
    }

    public void setBankTSCollectionName(String collection) {
        bankTSCollectionName = collection;
    }

    /**
     * Sets the name of LCIO collection containing hits.
     * 
     * @param collection - The collection name.
     */
    public void setHitEcalCollectionName(String collection) {
        hitEcalCollectionName = collection;
    }

    public void setHitHodoCollectionName(String collection) {
        hitHodoCollectionName = collection;
    }

    /**
     * Defines the size of the local window for use in the efficiency over time
     * plot. Units are in seconds.
     * 
     * @param size - The duration of local efficiency measurements.
     */
    public void setLocalWindowSize(int size) {
        // The setter takes units of seconds, but time stamps are in
        // nanoseconds. Convert the units less computational intense
        // comparisons.
        localWindowSize = size * 1000000;
    }

    /**
     * Sets the total number of hits for Ecal that must be present in an event in
     * order for it to be considered a noise event. This is only applied if
     * <code>skipNoiseEventsEcal</code> is set to <code>true</code>.
     * 
     * @param threshold - The noise hit threshold for Ecal.
     */
    public void setNoiseThresholdEcal(int threshold) {
        noiseEventThresholdEcal = threshold;
    }

    /**
     * Sets the total number of hits for hodoscope that must be present in an event
     * in order for it to be considered a noise event. This is only applied if
     * <code>skipNoiseEventsHodo</code> is set to <code>true</code>.
     * 
     * @param threshold - The noise hit threshold for hodoscope.
     */
    public void setNoiseThresholdHodo(int threshold) {
        noiseEventThresholdHodo = threshold;
    }

    /**
     * Indicates whether events which exceed a certain number of total hits for
     * Ecal, defined by <code>noiseEventThresholdEcal</code>, should be treated as
     * noise events and skipped.
     * 
     * @param state - <code>true</code> causes noise events to be skipped and
     *              <code>false</code> does not.
     */
    public void setSkipNoiseEventsEcal(boolean state) {
        skipNoiseEventsEcal = state;
    }

    /**
     * Indicates whether events which exceed a certain number of total hits for
     * hodoscope, defined by <code>noiseEventThresholdHodo</code>, should be treated
     * as noise events and skipped.
     * 
     * @param state - <code>true</code> causes noise events to be skipped and
     *              <code>false</code> does not.
     */
    public void setSkipNoiseEventsHodo(boolean state) {
        skipNoiseEventsHodo = state;
    }

    /**
     * Indicates whether the event log should be printed when a trigger which was
     * simulated from a hardware-reported (VTP) cluster fails to verify.
     * 
     * @param state - <code>true</code> indicates that the event log should be
     *              printed, and <code>false</code> that it should not.
     */
    public void setPrintOnHardwareSimFailure(boolean state) {
        printOnHardwareSimFailure = state;
    }

    /**
     * Indicates whether the event log should be printed when a trigger which was
     * simulated from a software-constructed cluster fails to verify.
     * 
     * @param state - <code>true</code> indicates that the event log should be
     *              printed, and <code>false</code> that it should not.
     */
    public void setPrintOnSoftwareSimFailure(boolean state) {
        printOnSoftwareSimFailure = state;
    }

    /**
     * Sets the name of LCIO collection containing simulated triggers.
     * 
     * @param collection - The collection name.
     */
    public void setTriggerCollectionName(String collection) {
        simTriggerCollectionName = collection;
    }

    /**
     * Sets the which of the triggers this driver should verify. This value must be
     * of the following:
     * <ul>
     * <li>SINGLESTOP0</li>
     * <li>SINGLESTOP1</li>
     * <li>SINGLESTOP2</li>
     * <li>SINGLESTOP3</li>
     * <li>SINGLESBOT0</li>
     * <li>SINGLESBOT1</li>
     * <li>SINGLESBOT2</li>
     * <li>SINGLESBOT3</li>
     * <li>PAIR0</li>
     * <li>PAIR1</li>
     * <li>PAIR2</li>
     * <li>PAIR3</li>
     * </ul>
     * 
     * @param type - The <code>String</code> indicating which trigger should be
     *             verified.
     * @throws IllegalArgumentException Occurs if any type name is besides those
     *                                  specified above is used.
     */
    public void setTriggerType(String type) throws IllegalArgumentException {
        if (type.compareTo(TriggerType.SINGLESTOP0.name()) == 0) {
            triggerType = TriggerType.SINGLESTOP0;
        } else if (type.compareTo(TriggerType.SINGLESTOP1.name()) == 0) {
            triggerType = TriggerType.SINGLESTOP1;
        } else if (type.compareTo(TriggerType.SINGLESTOP2.name()) == 0) {
            triggerType = TriggerType.SINGLESTOP2;
        } else if (type.compareTo(TriggerType.SINGLESTOP3.name()) == 0) {
            triggerType = TriggerType.SINGLESTOP3;
        } else if (type.compareTo(TriggerType.SINGLESBOT0.name()) == 0) {
            triggerType = TriggerType.SINGLESBOT0;
        } else if (type.compareTo(TriggerType.SINGLESBOT1.name()) == 0) {
            triggerType = TriggerType.SINGLESBOT1;
        } else if (type.compareTo(TriggerType.SINGLESBOT2.name()) == 0) {
            triggerType = TriggerType.SINGLESBOT2;
        } else if (type.compareTo(TriggerType.SINGLESBOT3.name()) == 0) {
            triggerType = TriggerType.SINGLESBOT3;
        } else if (type.compareTo(TriggerType.PAIR0.name()) == 0) {
            triggerType = TriggerType.PAIR0;
        } else if (type.compareTo(TriggerType.PAIR1.name()) == 0) {
            triggerType = TriggerType.PAIR1;
        } else if (type.compareTo(TriggerType.PAIR2.name()) == 0) {
            triggerType = TriggerType.PAIR2;
        } else if (type.compareTo(TriggerType.PAIR3.name()) == 0) {
            triggerType = TriggerType.PAIR3;
        } else {
            throw new IllegalArgumentException("Trigger type \"" + type + "\" is not supported.");
        }
    }

    /**
     * Sets the end of the trigger window range. This is used during
     * hardware-cluster simulated trigger verification.
     * 
     * @param value - The end of the trigger window range. This value is inclusive.
     */
    public void setTriggerWindowEnd(int value) {
        triggerWindowEnd = value;
    }

    /**
     * Sets the start of the trigger window range. This is used during
     * hardware-cluster simulated trigger verification.
     * 
     * @param value - The start of the trigger window range. This value is
     *              inclusive.
     */
    public void setTriggerWindowStart(int value) {
        triggerWindowStart = value;
    }

    /**
     * Sets whether the full event verification summary should be printed on every
     * event or not.
     * 
     * @param state - <code>true</code> prints the event summary and
     *              <code>false</code> does not.
     */
    public void setVerbose(boolean state) {
        verbose = state;
    }

    /**
     * Sets whether the debug message should be printed
     * 
     * @param debug
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Set if TS bits are read. For random runs, no TS banks.
     * 
     * @param isActiveBitRead
     */
    public void setisActiveBitRead(boolean isActiveBitRead) {
        this.isActiveBitRead = isActiveBitRead;
    }

    public void setClusterTotalEnergyXMax(double value) {
        xMax[CutType.CLUSTER_TOTAL_ENERGY.ordinal()] = value;
    }

    public void setClusterHitCountXMax(double value) {
        xMax[CutType.CLUSTER_HIT_COUNT.ordinal()] = value;
    }

    public void setPairEnergySumXMax(double value) {
        xMax[CutType.PAIR_ENERGY_SUM.ordinal()] = value;
    }

    public void setPairEnergyDiffXMax(double value) {
        xMax[CutType.PAIR_ENERGY_DIFF.ordinal()] = value;
    }

    public void setPairEnergySlopeXMax(double value) {
        xMax[CutType.PAIR_ENERGY_SLOPE.ordinal()] = value;
    }

    public void setPairCoplanarityXMax(double value) {
        xMax[CutType.PAIR_COPLANARITY.ordinal()] = value;
    }

    public void setPairTimeCoincidenceXMax(double value) {
        xMax[CutType.PAIR_TIME_COINCIDENCE.ordinal()] = value;
    }

    public void setClusterTotalEnergyBinSize(double value) {
        binSize[CutType.CLUSTER_TOTAL_ENERGY.ordinal()] = value;
    }

    public void setClusterHitCountBinSize(double value) {
        binSize[CutType.CLUSTER_HIT_COUNT.ordinal()] = value;
    }

    public void setPairEnergySumBinSize(double value) {
        binSize[CutType.PAIR_ENERGY_SUM.ordinal()] = value;
    }

    public void setPairEnergyDiffBinSize(double value) {
        binSize[CutType.PAIR_ENERGY_DIFF.ordinal()] = value;
    }

    public void setPairEnergySlopeBinSize(double value) {
        binSize[CutType.PAIR_ENERGY_SLOPE.ordinal()] = value;
    }

    public void setPairCoplanarityBinSize(double value) {
        binSize[CutType.PAIR_COPLANARITY.ordinal()] = value;
    }

    public void setPairTimeCoincidenceBinSize(double value) {
        binSize[CutType.PAIR_TIME_COINCIDENCE.ordinal()] = value;
    }
}
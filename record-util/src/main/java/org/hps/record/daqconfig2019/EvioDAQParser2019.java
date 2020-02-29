package org.hps.record.daqconfig2019;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.hodoscope.HodoscopeChannel;
import org.hps.conditions.hodoscope.HodoscopeConditions;

/**
 * Class <code>EvioDAQParser2019</code> takes 2019 DAQ configuration banks from
 * EvIO data and extracts the configuration parameters from them. These are then
 * stored within package-accessible variables within the class. <br/>
 * <br/>
 * Note that this class should not be used directly to acquire DAQ configuration
 * data. It is intended to be used internally by the EvIO parser and the class
 * <code>ConfigurationManager</code>. The latter should be used for accessing
 * this information for any other classes.
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */

public class EvioDAQParser2019 {
    /** The EvIO bank identification tag for DAQ configuration banks. */
    public static final int BANK_TAG = 0xE10E;

    // Dump everything read from the DAQ Configuration Bank, minimal interpretation:
    private Map<String, List<String>> configMap = new HashMap<String, List<String>>();

    // Class parameters.
    private int nBanks = 0;
    private boolean debug = false;

    //////////// Cluster cut configuration ////////////
    /**
     * Cluster hit timing coincidence: 0 to 16, units: +/-ns
     */
    int ecalClusterHitDT = 0;
    /**
     * Cluster seed threshold in: 1 to 8191, units MeV
     */
    int ecalClusterSeedThr = 0;
    /**
     * Hodoscope fadc hit cut: minimum acceptable FADC hit integral: 1 to 8191, units TBD
     */
    int hodoFADCHitThr = 0;
    /**
     * Hodoscope trigger hit cut: minimum acceptable integral (clustered or single tile): 1 to 8191, units TBD
     */
    int hodoThr = 0;
    /**
     * Hodoscope hit coincidence between L1,L2, and also ECAL clusters (real with is specified value +4ns): 0 to 60, units: ns
     */
    int hodoDT = 0;
    
    //////////// Triggers ////////////
    ////// Singles //////
    /**
     * Indicates whether the singles triggers are enabled or not. Uses the format
     * <code>{ Singles0_Enabled, Singles1_Enabled, Singles2_Enabled, Singles3_Enabled }</code>.
     */
    boolean[] singlesEn = { false, false, false, false };

    // Cuts Enabled:
    /**
     * Indicates whether the singles trigger cluster hit count cuts are enabled or
     * not. Uses the format
     * <code>{ Singles0_Cut_Enabled, Singles1_Cut_Enabled, Singles2_Cut_Enabled, Singles3_Cut_Enabled }</code>.
     */
    boolean[] singlesNhitsMinEn = { false, false, false, false };
    /**
     * Indicates whether the singles trigger cluster total energy lower bound cuts
     * are enabled or not. Uses the format
     * <code>{ Singles0_Cut_Enabled, Singles1_Cut_Enabled, Singles2_Cut_Enabled, Singles3_Cut_Enabled  }</code>.
     */
    boolean[] singlesEnergyMinEn = { false, false, false, false };
    /**
     * Indicates whether the singles trigger cluster total energy upper bound cuts
     * are enabled or not. Uses the format
     * <code>{ Singles0_Cut_Enabled, Singles1_Cut_Enabled, Singles2_Cut_Enabled, Singles3_Cut_Enabled  }</code>.
     */
    boolean[] singlesEnergyMaxEn = { false, false, false, false };
    /**
     * Indicates whether the singles trigger cluster x min cuts are enabled or not.
     * Uses the format
     * <code>{ Singles0_Cut_Enabled, Singles1_Cut_Enabled, Singles2_Cut_Enabled, Singles3_Cut_Enabled  }</code>.
     */
    boolean[] singlesXMinEn = { false, false, false, false };
    /**
     * Indicates whether the singles trigger cluster Position Dependent Energy (PDE)
     * cuts are enabled or not. Uses the format
     * <code>{ Singles0_Cut_Enabled, Singles1_Cut_Enabled, Singles2_Cut_Enabled, Singles3_Cut_Enabled  }</code>.
     */
    boolean[] singlesPDEEn = { false, false, false, false };
    /**
     * Indicates whether the Hodoscope layer 1 matching cuts are enabled or not.
     * Uses the format
     * <code>{ Singles0_Cut_Enabled, Singles1_Cut_Enabled, Singles2_Cut_Enabled, Singles3_Cut_Enabled  }</code>.
     */
    boolean[] singlesL1MatchingEn = { false, false, false, false };
    /**
     * Indicates whether the Hodoscope layer 2 matching cuts are enabled or not.
     * Uses the format
     * <code>{ Singles0_Cut_Enabled, Singles1_Cut_Enabled, Singles2_Cut_Enabled, Singles3_Cut_Enabled  }</code>.
     */
    boolean[] singlesL2MatchingEn = { false, false, false, false };
    /**
     * Indicates whether the Hodoscope layer 1 to layer 2 matching cuts are enabled
     * or not. Uses the format
     * <code>{ Singles0_Cut_Enabled, Singles1_Cut_Enabled, Singles2_Cut_Enabled, Singles3_Cut_Enabled  }</code>.
     */
    boolean[] singlesL1L2MatchingEn = { false, false, false, false };
    /**
     * Indicates whether the Hodoscope layer 1 to Ecal X and layer 2 to Ecal X
     * geometry matching cuts are enabled or not. Uses the format
     * <code>{ Singles0_Cut_Enabled, Singles1_Cut_Enabled, Singles2_Cut_Enabled, Singles3_Cut_Enabled  }</code>.
     */
    boolean[] singlesL1L2EcalMatchingEn = { false, false, false, false };

    // Cut Values:
    /**
     * Specifies the value of the singles trigger cluster hit count cuts. Use the
     * format, in units of hits,
     * <code>{ Singles0_Cut_Value, Singles1_Cut_Value, Singles2_Cut_Value, Singles3_Cut_Value }</code>.
     */
    int[] singlesNhitsMin = { 0, 0, 0, 0 };
    /**
     * Specifies the value of the singles trigger cluster total energy lower bound
     * cuts. Use the format, in units of MeV,
     * <code>{ Singles0_Cut_Value, Singles1_Cut_Value, Singles2_Cut_Value, Singles3_Cut_Value }</code>.
     */
    int[] singlesEnergyMin = { 0, 0, 0, 0 };
    /**
     * Specifies the value of the singles trigger cluster total energy upper bound
     * cuts. Use the format, in units of MeV,
     * <code>{ Singles0_Cut_Value, Singles1_Cut_Value, Singles2_Cut_Value, Singles3_Cut_Value }</code>.
     */
    int[] singlesEnergyMax = { 0, 0, 0, 0 };
    /**
     * Specifies the value of the singles trigger cluster x min cuts. Use the
     * format, in units of crystal index,
     * <code>{ Singles0_Cut_Value, Singles1_Cut_Value, Singles2_Cut_Value, Singles3_Cut_Value }</code>.
     */
    int[] singlesXMin = { 0, 0, 0, 0 };
    /**
     * Specifies the value of the singles trigger cluster Position Dependent Energy
     * (PDE) cuts: Energy >= C0 + C1*x + C2*x^2 + C3*x^3, where x is crystal X
     * coordinates [-22, 0] and [1, 23] Parameter C0. Use the format, in units of
     * MeV,
     * <code>{ Singles0_Cut_Value, Singles1_Cut_Value, Singles2_Cut_Value, Singles3_Cut_Value }</code>.
     */
    float[] singlesPDEC0 = { 0, 0, 0, 0 };
    /**
     * Specifies the value of the singles trigger cluster Position Dependent Energy
     * (PDE) cuts: Energy >= C0 + C1*x + C2*x^2 + C3*x^3, where x is crystal X
     * coordinates [-22, 0] and [1, 23] Parameter C1. Use the format, in units of
     * MeV,
     * <code>{ Singles0_Cut_Value, Singles1_Cut_Value, Singles2_Cut_Value, Singles3_Cut_Value }</code>.
     */
    float[] singlesPDEC1 = { 0, 0, 0, 0 };
    /**
     * Specifies the value of the singles trigger cluster Position Dependent Energy
     * (PDE) cuts: Energy >= C0 + C1*x + C2*x^2 + C3*x^3, where x is crystal X
     * coordinates [-22, 0] and [1, 23] Parameter C2. Use the format, in units of
     * MeV,
     * <code>{ Singles0_Cut_Value, Singles1_Cut_Value, Singles2_Cut_Value, Singles3_Cut_Value }</code>.
     */
    float[] singlesPDEC2 = { 0, 0, 0, 0 };
    /**
     * Specifies the value of the singles trigger cluster Position Dependent Energy
     * (PDE) cuts: Energy >= C0 + C1*x + C2*x^2 + C3*x^3, where x is crystal X
     * coordinates [-22, 0] and [1, 23] Parameter C3. Use the format, in units of
     * MeV,
     * <code>{ Singles0_Cut_Value, Singles1_Cut_Value, Singles2_Cut_Value, Singles3_Cut_Value }</code>.
     */
    float[] singlesPDEC3 = { 0, 0, 0, 0 };

    ////// Pairs //////
    /**
     * Indicates whether the pair triggers are enabled or not. Uses the format
     * <code>{ Pair0_Enabled, Pair1_Enabled, Pair2_Enabled, Pair3_Enabled }</code>.
     */
    boolean[] pairsEn = { false, false, false, false };

    // Cuts Enabled:
    /**
     * Indicates whether the pair trigger pair energy sum cuts are enabled or not.
     * Use the format,
     * <code>{ Pair0_Cut_Enabled, Pair1_Cut_Enabled, Pair2_Cut_Enabled, Pair3_Cut_Enabled }</code>.
     */
    boolean[] pairsEnergySumMaxMinEn = { false, false, false, false };
    /**
     * Indicates whether the pair trigger pair energy difference cuts are enabled or
     * not. Use the format,
     * <code>{ Pair0_Cut_Enabled, Pair1_Cut_Enabled, Pair2_Cut_Enabled, Pair3_Cut_Enabled }</code>.
     */
    boolean[] pairsEnergyDiffEn = { false, false, false, false };
    /**
     * Indicates whether the pair trigger pair coplanarity cuts are enabled or not.
     * Use the format,
     * <code>{ Pair0_Cut_Enabled, Pair1_Cut_Enabled, Pair2_Cut_Enabled, Pair3_Cut_Enabled }</code>.
     */
    boolean[] pairsCoplanarityEn = { false, false, false, false };
    /**
     * Indicates whether the pair trigger pair energy slope cuts are enabled or not.
     * Use the format,
     * <code>{ Pair0_Cut_Enabled, Pair1_Cut_Enabled, Pair2_Cut_Enabled, Pair3_Cut_Enabled }</code>.
     */
    boolean[] pairsEnergyDistEn = { false, false, false, false };

    // Cut Values:
    /**
     * Specifies the value of the pair trigger cluster hit count cuts. Use the
     * format, in units of hits,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value, Pair2_Cut_Value, Pair3_Cut_Value }</code>.
     */
    int[] pairsNhitsMin = { 0, 0, 0, 0 };
    /**
     * Specifies the value of the pair trigger cluster total energy lower bound
     * cuts. Use the format, in units of MeV,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value, Pair2_Cut_Value, Pair3_Cut_Value }</code>.
     */
    int[] pairsEnergyMin = { 0, 0, 0, 0 };
    /**
     * Specifies the value of the pair trigger cluster total energy upper bound
     * cuts. Use the format, in units of MeV,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value, Pair2_Cut_Value, Pair3_Cut_Value }</code>.
     */
    int[] pairsEnergyMax = { 0, 0, 0, 0 };
    /**
     * Specifies the value of the pair trigger pair energy sum upper bound cuts. Use
     * the format, in units of MeV,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value, Pair2_Cut_Value, Pair3_Cut_Value }</code>.
     */
    int[] pairsEnergySumMin = { 0, 0, 0, 0 };
    /**
     * Specifies the value of the pair trigger pair energy sum lower bound cuts. Use
     * the format, in units of MeV,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value, Pair2_Cut_Value, Pair3_Cut_Value }</code>.
     */
    int[] pairsEnergySumMax = { 0, 0, 0, 0 };
    /**
     * Specifies the value of the pair trigger pair energy difference cuts. Use the
     * format, in units of MeV,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value, Pair2_Cut_Value, Pair3_Cut_Value }</code>.
     */
    int[] pairsEnergyDiffMax = { 0, 0, 0, 0 };
    /**
     * Specifies the value of the pair trigger pair coplanarity cuts. Use the
     * format, in units of degrees,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value, Pair2_Cut_Value, Pair3_Cut_Value }</code>.
     */
    int[] pairsCoplanarityMax = { 0, 0, 0, 0 };
    /**
     * Specifies the value of the pair trigger pair time coincidence cuts. Use the
     * format, in units of nanoseconds,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value, Pair2_Cut_Value, Pair3_Cut_Value }</code>.
     */
    int[] pairsTimeDiffMax = { 0, 0, 0, 0 };
    /**
     * Specifies the value of the pair trigger pair energy slope cuts. parameter
     * Threshold. Use the format, in units of MeV,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value, Pair2_Cut_Value, Pair3_Cut_Value }</code>.
     */
    int[] pairsEnergyDistMin = { 0, 0, 0, 0 };
    /**
     * Specifies the value of the pair trigger pair energy slope cuts' parameter F.
     * Use the format, in units of MeV / mm,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value, Pair2_Cut_Value, Pair3_Cut_Value }</code>.
     */
    float[] pairsEnergyDistSlope = { 0, 0, 0, 0 };

    ////// Cluster Multiplicity //////
    /**
     * Indicates whether the cluster multiplicity triggers are enabled or not. Uses
     * the format <code>{ Mult0_Enabled, Mult1_Enabled }</code>.
     */
    boolean[] multEn = { false, false };
    /**
     * Specifies the value of the cluster multiplicity trigger cluster hit count
     * cuts. Use the format, in units of hits,
     * <code>{ Mult0_Enabled, Mult1_Enabled }</code>.
     */
    int[] multNhitsMin = { 0, 0 };
    /**
     * Specifies the value of the cluster multiplicity trigger cluster total energy
     * lower bound cuts. Use the format, in units of MeV,
     * <code>{ Mult0_Enabled, Mult1_Enabled }</code>.
     */
    int[] multEnergyMin = { 0, 0 };
    /**
     * Specifies the value of the cluster multiplicity trigger cluster total energy
     * upper bound cuts. Use the format, in units of MeV,
     * <code>{ Mult0_Enabled, Mult1_Enabled }</code>.
     */
    int[] multEnergyMax = { 0, 0 };
    /**
     * Specifies the value of the cluster multiplicity trigger top cluster
     * multiplicity cuts. Use the format, in units of hits,
     * <code>{ Mult0_Enabled, Mult1_Enabled }</code>.
     */
    int[] multTopMultMin = { 0, 0 };
    /**
     * Specifies the value of the cluster multiplicity trigger bottom cluster
     * multiplicity cuts. Use the format, in units of hits,
     * <code>{ Mult0_Enabled, Mult1_Enabled }</code>.
     */
    int[] multBotMultMin = { 0, 0 };
    /**
     * Specifies the value of the cluster multiplicity trigger total cluster
     * multiplicity cuts. Use the format, in units of hits,
     * <code>{ Mult0_Enabled, Mult1_Enabled }</code>.
     */
    int[] multTotMultMin = { 0, 0 };
    /**
     * Specifies the value of the cluster multiplicity trigger time window. Use the
     * format, in units of hits, <code>{ Mult0_Enabled, Mult1_Enabled }</code>.
     */
    int[] multDT = { 0, 0 };

    ////// FEE //////
    /**
     * Indicates whether the FEE trigger is enabled or not. Uses the format
     * <code> FEE_Enabled </code>.
     */
    boolean FEEEn = false;
    /**
     * Specifies the value of the FEE trigger cluster hit count cuts. Use the
     * format, in units of hits, <code> FEE_Enabled </code>.
     */
    int FEENhitsMin = 0;
    /**
     * Specifies the value of the FEE trigger cluster total energy lower bound cuts.
     * Use the format, in units of MeV, <code> FEE_Enabled </code>.
     */
    int FEEEnergyMin = 0;
    /**
     * Specifies the value of the FEE cluster total energy upper bound cuts. Use the
     * format, in units of MeV, <code> FEE_Enabled </code>.
     */
    int FEEEnergyMax = 0;      
    /**
     * Specifies minimum of each region for FEE prescale <code> FEE_Enabled </code>.
     */
    int[] FEERegionXMin = {0, 0, 0, 0, 0, 0, 0};
    /**
     * Specifies maximum of each region for FEE prescale <code> FEE_Enabled </code>.
     */
    int[] FEERegionXMax = {0, 0, 0, 0, 0, 0, 0};
    /**
     * Specifies FEE prescale for each region <code> FEE_Enabled </code>.
     */
    int[] FEERegionPrescale = {0, 0, 0, 0, 0, 0, 0};

    //////////// TS Prescale ////////////
    /**
     * Specifies TS prescale values
     * <code> { Prescale_Singles0_Top, Prescale_Singles1_Top, Prescale_Singles2_Top, Prescale_Singles3_Top,
     * Prescale_Singles0_Bot, Prescale_Singles1_Bot, Prescale_Singles2_Bot, Prescale_Singles3_Bot, 
     * Prescale_Pairs0, Prescale_Pairs1, Prescale_Pairs2, Prescale_Pairs3, LED, Cosmic, Hodoscope,
     * Pulser, Multiplicity0, Multiplicity1, FEE_Top, FEE_Bot } </code>.
     */
    int[] TSPrescale = { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };

    //////////// FADC Config ////////////

    /////// Simple FADC Config
    // Ecal
    /**
     * The length of time after a pulse-crossing event that the pulse should be
     * integrated. Uses units of clock-cycles.
     */
    int fadcNSAEcal = 0;
    /**
     * The length of time before a pulse-crossing event that the pulse should be
     * integrated. Uses units of clock-cycles.
     */
    int fadcNSBEcal = 0;
    /**
     * The maximum number of pulses that will be extracted from a single channel
     * within a readout window.
     */
    int fadcNPEAKEcal = 0;
    /**
     * The pulse-processing mode used by the FADC. This should be 1, 3, or 7.
     */
    int fadcMODEEcal = 0;
    /** The size of readout window in ns. */
    int fadcWIDTHEcal = 0;
    /** The time-offset of the readout window in ns. */
    int fadcOFFSETEcal = 0;

    // Hodoscope
    /**
     * The length of time after a pulse-crossing event that the pulse should be
     * integrated. Uses units of clock-cycles.
     */
    int fadcNSAHodo = 0;
    /**
     * The length of time before a pulse-crossing event that the pulse should be
     * integrated. Uses units of clock-cycles.
     */
    int fadcNSBHodo = 0;
    /** The size of readout window in ns. */
    int fadcWIDTHHodo = 0;
    /** The time-offset of the readout window in ns. */
    int fadcOFFSETHodo = 0;

    /**
     * Map of <code>EcalChannel</code> to the gain for that channel. Uses units of
     * ADC / MeV for the mapped value.
     */
    Map<EcalChannel, Float> GAINECAL = new HashMap<EcalChannel, Float>();
    /**
     * Map of <code>EcalChannel</code> to the pedestal for that channel. Uses units
     * of ADC for the mapped value.
     */
    Map<EcalChannel, Float> PEDESTALECAL = new HashMap<EcalChannel, Float>();
    /**
     * Map of <code>EcalChannel</code> to the threshold for that channel. Uses units
     * of ADC for the mapped value.
     */
    Map<EcalChannel, Integer> THRESHOLDECAL = new HashMap<EcalChannel, Integer>();
    /**
     * Map of <code>EcalChannel</code> to the delay for that channel. Uses units of
     * ADC for the mapped value.
     */
    Map<EcalChannel, Integer> DELAYECAL = new HashMap<EcalChannel, Integer>();

    /**
     * Map of <code>HodoscopeChannel</code> to the gain for that channel. Uses units
     * of ADC / MeV for the mapped value.
     */
    Map<HodoscopeChannel, Float> GAINHODO = new HashMap<HodoscopeChannel, Float>();
    /**
     * Map of <code>HodoscopeChannel</code> to the pedestal for that channel. Uses
     * units of ADC for the mapped value.
     */
    Map<HodoscopeChannel, Float> PEDESTALHODO = new HashMap<HodoscopeChannel, Float>();
    /**
     * Map of <code>HodoscopeChannel</code> to the threshold for that channel. Uses
     * units of ADC for the mapped value.
     */
    Map<HodoscopeChannel, Integer> THRESHOLDHODO = new HashMap<HodoscopeChannel, Integer>();
    /**
     * Map of <code>HodoscopeChannel</code> to the delay for that channel. Uses
     * units of ADC for the mapped value.
     */
    Map<HodoscopeChannel, Integer> DELAYHODO = new HashMap<HodoscopeChannel, Integer>();

    // Tracks the last FADC slot seen. This is needed for parsing FADC
    // threshold, pedestal, and gain information.
    private int thisFadcSlot = 0;

    // Cache local set of EcalChannels:
    private EcalConditions ecalConditions = null;
    private List<EcalChannel> channels = new ArrayList<EcalChannel>();

    // Cache local set of HodoscopeChannels:
    private HodoscopeConditions hodoConditions = null;
    private List<HodoscopeChannel> channelsHodo = new ArrayList<HodoscopeChannel>();

    /**
     * Instantiates the <code>EvioDAQParser2019</code>.
     */
    public EvioDAQParser2019() {
        // Create a map to map Ecal crystals to their database channel object.
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
        for (int ii = 0; ii < 442; ii++) {
            channels.add(findChannelEcal(ii + 1));
        }
        // Create a map to map hodoscope channels to their database channel object.
        hodoConditions = DatabaseConditionsManager.getInstance().getHodoConditions();

        for (int ii = 0; ii < 32; ii++) {
            channelsHodo.add(findChannelHodo(ii + 1));
        }
    }

    /**
     * Parses a set of configuration tables to obtain DAQ configuration parameters.
     * 
     * @param crate               - The crate associated with the configuration
     *                            tables.
     * @param runNumber           - The run number for the current data set.
     * @param configurationTables - Tables containing DAQ configuration parameters.
     */
    public void parse(int crate, int runNumber, String[] configurationTables) {
        // Track the number of banks that have been parsed. If the
        // parameter values have not been populated after a certain
        // number of banks, there is missing information.
        nBanks++;

        // Create a map that maps an identifier for each configuration
        // parameter (its parameter key) to any values associated
        // with it (its parameter values).
        loadConfigMap(crate, configurationTables);

        // If debugging text is enabled, print the map to the terminal.
        if (debug) {
            printMap();
        }

        // Parse the previously generated configuration map and extract
        // the DAQ configuration from it.
        parseConfigMap();

        // If the expected number of banks have been parsed and debugging
        // text is enabled, print out all of the parsed variables.
        if (nBanks > 4 && debug) {
            printVars();
        }
    }

    /**
     * Converts the textual configuration information into a map, where the first
     * column value becomes the map entry key and the remainder becomes the map
     * entry value.
     * 
     * @param crate        - The crate associated with the textual configuration
     *                     data.
     * @param configTables - An array of textual configuration tables that contain
     *                     the DAQ configuration parameters.
     */
    private void loadConfigMap(int crate, String[] configTables) {
        // Iterate over each configuration table.
        for (String configTable : configTables) {
            // Split each table into rows and iterate over the rows.
            rowLoop: for (String line : configTable.trim().split("\n")) {
                // Split the first column from the row.
                String[] cols = line.trim().split(" +", 2);

                // If there are fewer than two segments after the split,
                // then this is not a valid parameter entry.
                if (cols.length < 2)
                    continue rowLoop;

                // The row name is the value of the first column. The
                // rest are typically values.
                String key = cols[0];
                List<String> vals = new ArrayList<String>(Arrays.asList(cols[1].trim().split(" +")));

                // If no values are present, this is not a valid entry.
                if (vals.size() < 1) {
                    continue rowLoop;
                }

                // SPECIAL CASE:: Key "FADC250"
                // This entry marks parameter values for FADC channels,
                // such as pedestals, gains, and thresholds. These are
                // stored in separate lists from the other parameters.
                if (key.startsWith("FADC250")) {
                    parseFADC(crate, key.trim(), vals);
                }

                // GENERAL CASE: Basic Parameter
                // This indicates a regular parameter that does not
                // require any special parsing.
                if (vals.size() > 1 && key.startsWith("VTP")) {
                    // List the parameter by "[ROW NAME]_[KEY]" and
                    // remove the key so that only the values remain.
                    key += "_" + vals.remove(0);
                }

                // Add the parameter key and its values to the map.
                configMap.put(key, vals);
            }
        }
    }

    /**
     * Parses the configuration parameter map entries and extracts the parameter
     * values for those parameters which have the standard format of
     * <code>[PARAMETER KEY] --> { [PARAMETER VALUES] }</code>.
     */
    public void parseConfigMap() {
        //////////// Parse VTP cluster cut values ////////////   
        ecalClusterHitDT = Integer.valueOf(getConfigParameter("VTP_HPS_ECAL_CLUSTER_HIT_DT", 0));        
        ecalClusterSeedThr = Integer.valueOf(getConfigParameter("VTP_HPS_ECAL_CLUSTER_SEED_THR", 0));
        hodoFADCHitThr = Integer.valueOf(getConfigParameter("VTP_HPS_HODOSCOPE_FADCHIT_THR", 0));
        hodoThr = Integer.valueOf(getConfigParameter("VTP_HPS_HODOSCOPE_HODO_THR", 0));
        hodoDT = Integer.valueOf(getConfigParameter("VTP_HPS_HODOSCOPE_HODO_DT", 0));        
        
        
        //////////// Parse trigger data ////////////
        // Parse singles and pairs trigger data.
        for (int ii = 0; ii < 4; ii++) {
            ////// Singles //////
            singlesEn[ii] = getBoolConfigVTP(ii, "SINGLE_EN", 0);

            // Check singles trigger cuts enabled status.
            singlesNhitsMinEn[ii] = getBoolConfigVTP(ii, "SINGLE_NMIN", 1);
            singlesEnergyMinEn[ii] = getBoolConfigVTP(ii, "SINGLE_EMIN", 1);
            singlesEnergyMaxEn[ii] = getBoolConfigVTP(ii, "SINGLE_EMAX", 1);
            singlesXMinEn[ii] = getBoolConfigVTP(ii, "SINGLE_XMIN", 1);
            singlesPDEEn[ii] = getBoolConfigVTP(ii, "SINGLE_PDE", 4);
            singlesL1MatchingEn[ii] = getBoolConfigVTP(ii, "SINGLE_HODO", 0);
            singlesL2MatchingEn[ii] = getBoolConfigVTP(ii, "SINGLE_HODO", 1);
            singlesL1L2MatchingEn[ii] = getBoolConfigVTP(ii, "SINGLE_HODO", 2);
            singlesL1L2EcalMatchingEn[ii] = getBoolConfigVTP(ii, "SINGLE_HODO", 3);

            // Get the singles trigger cuts.
            singlesNhitsMin[ii] = getIntConfigVTP(ii, "SINGLE_NMIN", 0);
            singlesEnergyMin[ii] = getIntConfigVTP(ii, "SINGLE_EMIN", 0);
            singlesEnergyMax[ii] = getIntConfigVTP(ii, "SINGLE_EMAX", 0);
            singlesXMin[ii] = getIntConfigVTP(ii, "SINGLE_XMIN", 0);
            singlesPDEC0[ii] = getFloatConfigVTP(ii, "SINGLE_PDE", 0);
            singlesPDEC1[ii] = getFloatConfigVTP(ii, "SINGLE_PDE", 1);
            singlesPDEC2[ii] = getFloatConfigVTP(ii, "SINGLE_PDE", 2);
            singlesPDEC3[ii] = getFloatConfigVTP(ii, "SINGLE_PDE", 3);

            ////// Pairs //////
            pairsEn[ii] = getBoolConfigVTP(ii, "PAIR_EN", 0);

            // Check pair trigger cuts enabled status.
            pairsEnergySumMaxMinEn[ii] = getBoolConfigVTP(ii, "PAIR_SUMMAX_MIN", 2);
            pairsEnergyDiffEn[ii] = getBoolConfigVTP(ii, "PAIR_DIFFMAX", 1);
            pairsCoplanarityEn[ii] = getBoolConfigVTP(ii, "PAIR_COPLANARITY", 1);
            pairsEnergyDistEn[ii] = getBoolConfigVTP(ii, "PAIR_ENERGYDIST", 2);

            // Get the pair trigger cuts.
            pairsNhitsMin[ii] = getIntConfigVTP(ii, "PAIR_NMIN", 0);
            pairsEnergyMin[ii] = getIntConfigVTP(ii, "PAIR_EMIN", 0);
            pairsEnergyMax[ii] = getIntConfigVTP(ii, "PAIR_EMAX", 0);
            pairsEnergySumMin[ii] = getIntConfigVTP(ii, "PAIR_SUMMAX_MIN", 1);
            pairsEnergySumMax[ii] = getIntConfigVTP(ii, "PAIR_SUMMAX_MIN", 0);
            pairsEnergyDiffMax[ii] = getIntConfigVTP(ii, "PAIR_DIFFMAX", 0);
            pairsCoplanarityMax[ii] = getIntConfigVTP(ii, "PAIR_COPLANARITY", 0);
            pairsTimeDiffMax[ii] = getIntConfigVTP(ii, "PAIR_TIMECOINCIDENCE", 0);
            pairsEnergyDistSlope[ii] = getFloatConfigVTP(ii, "PAIR_ENERGYDIST", 0);
            pairsEnergyDistMin[ii] = getIntConfigVTP(ii, "PAIR_ENERGYDIST", 1);
        }

        // Parse cluster multiplicity trigger data
        for (int ii = 0; ii < 2; ii++) {
            multEn[ii] = getBoolConfigVTP(ii, "MULT_EN", 0);
            multNhitsMin[ii] = getIntConfigVTP(ii, "MULT_NMIN", 0);
            multEnergyMin[ii] = getIntConfigVTP(ii, "MULT_EMIN", 0);
            multEnergyMax[ii] = getIntConfigVTP(ii, "MULT_EMAX", 0);
            multTopMultMin[ii] = getIntConfigVTP(ii, "MULT_MIN", 0);
            multBotMultMin[ii] = getIntConfigVTP(ii, "MULT_MIN", 1);
            multTotMultMin[ii] = getIntConfigVTP(ii, "MULT_MIN", 2);
            multDT[ii] = getIntConfigVTP(ii, "MULT_DT", 0);
        }

        // Parse FEE trigger data
        FEEEn = "1".equals(getConfigParameter("VTP_HPS_FEE_EN", 0));
        FEENhitsMin = Integer.valueOf(getConfigParameter("VTP_HPS_FEE_NMIN", 0));
        FEEEnergyMin = Integer.valueOf(getConfigParameter("VTP_HPS_FEE_EMIN", 0));
        FEEEnergyMax = Integer.valueOf(getConfigParameter("VTP_HPS_FEE_EMAX", 0));
        for (int ii = 0; ii < 7; ii++) {
            FEERegionXMin[ii] = getIntConfigVTP(ii, "FEE_PRESCALE", 0);
            FEERegionXMax[ii] = getIntConfigVTP(ii, "FEE_PRESCALE", 1);
            FEERegionPrescale[ii] = getIntConfigVTP(ii, "FEE_PRESCALE", 2);
        }

        //////////// Parse TS prescale ////////////
        for (int ii = 0; ii < 20; ii++) {
            TSPrescale[ii] = getIntConfigVTP(ii, "PRESCALE", 0);
        }

    }
    
    /**
     * Gets a VTP parameter value using a shortened version of the full parameter
     * key and parses it as a <code>float</code>.
     * 
     * @param itrig - The number of the trigger for which to obtain the parameter
     *              value.
     * @param stub  - The shortened version of the parameter key. This corresponds
     *              to "VTP_HPS_[STUB]_[TRIGGER NUMBER]".
     * @param ival  - The index of the value that is to be obtained.
     * @return Returns the requested value if it exists. Otherwise, a value of
     *         <code>0</code> is returned and a message is logged.
     */
    public float getFloatConfigVTP(int itrig, String stub, int ival) {
        return Float.valueOf(getConfigVTP(itrig, stub, ival));
    }

    /**
     * Gets a VTP parameter value using a shortened version of the full parameter
     * key and parses it as a <code>int</code>.
     * 
     * @param itrig - The number of the trigger for which to obtain the parameter
     *              value.
     * @param stub  - The shortened version of the parameter key. This corresponds
     *              to "VTP_HPS_[STUB]_[TRIGGER NUMBER]".
     * @param ival  - The index of the value that is to be obtained.
     * @return Returns the requested value if it exists. Otherwise, a value of
     *         <code>0</code> is returned and a message is logged.
     */
    public int getIntConfigVTP(int itrig, String stub, int ival) {
        return Integer.valueOf(getConfigVTP(itrig, stub, ival));
    }

    /**
     * Gets a VTP parameter value using a shortened version of the full parameter
     * key and parses it as a <code>boolean</code>.
     * 
     * @param itrig - The number of the trigger for which to obtain the parameter
     *              value.
     * @param stub  - The shortened version of the parameter key. This corresponds
     *              to "VTP_HPS_[STUB]_[TRIGGER NUMBER]".
     * @param ival  - The index of the value that is to be obtained.
     * @return Returns the requested value if it exists. Otherwise, a value of
     *         <code>false</code> is returned and a message is logged.
     */
    public boolean getBoolConfigVTP(int itrig, String stub, int ival) {
        return "1".equals(getConfigVTP(itrig, stub, ival));
    }

    /**
     * Gets an VTP parameter value using a shortened version of the full parameter
     * key.
     * 
     * @param itrig - The number of the trigger for which to obtain the parameter
     *              value.
     * @param stub  - The shortened version of the parameter key. This corresponds
     *              to "VTP_HPS_[STUB]_[TRIGGER NUMBER]".
     * @param ival  - The index of the value that is to be obtained.
     * @return Returns the requested value if it exists. Otherwise, a value of
     *         <code>"0"</code> is returned and a message is logged.
     */
    public String getConfigVTP(int itrig, String stub, int ival) {
        String key = "VTP_HPS_" + stub + "_" + itrig;
        return getConfigParameter(key, ival);
    }

    /**
     * Gets a parameter value associated with a parameter key.
     * 
     * @param key  - The parameter key to which the value belongs.
     * @param ival - The index of the desired parameter value.
     * @return Returns the requested parameter value if it exists and returns
     *         <code>"0"</code> otherwise. In the event that a parameter can not be
     *         found, an error message is passed to the logger.
     */
    public String getConfigParameter(String key, int ival) {
        // Check the parameter map for the requested parameter key.
        if (configMap.containsKey(key)) {
            // Get the list of values associated with this parameter key.
            List<String> vals = configMap.get(key);

            // Check that the list of values contains a parameter for
            // the requested parameter index. If it does, return it.
            if (ival < vals.size()) {
                return configMap.get(key).get(ival);
            }

            // Otherwise, an error has occurred. Log this and return the
            // default value of zero.
            else {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE,
                        "ConfigMap TOO SHORT:   " + ival + " " + configMap.get(key));
                return "0";
            }
        }

        // If the key is not present...
        else {
            // If more than 2 banks have been read, the absence of a
            // key represents an error. Log that this has occurred.
            if (nBanks > 2 && !key.startsWith("VTP_HPS_")) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "ConfigMap MISSING KEY:   " + key);
            }

            // Return a default value of zero.
            return "0";
        }
    }

    /**
     * Parses FADC configuration parameter entries. These all have multiple lines,
     * the first of which is the parameter key and the subsequent being parameter
     * value(s) for the indicated FADC slot.
     * 
     * @param crate - The crate associated with this parameter entry.
     * @param key   - The parameter key.
     * @param vals  - value(s) corresponding to key.
     */
    private void parseFADC(int crate, String key, List<String> vals) {
        // The FADC slot is not stored on the same line as the other
        // data and must be parsed and retained, as it is necessary
        // for handling the subsequent lines. If this line is the
        // FADC slot, store it.
        if (key.equals("FADC250_SLOT")) {
            thisFadcSlot = Integer.valueOf(vals.get(0));
        }

        if (thisFadcSlot != 10 && thisFadcSlot != 13) {// Ecal
            // Parse simple FADC data for Ecal and cosmic
            if (key.equals("FADC250_NSA"))
                fadcNSAEcal = Integer.valueOf(vals.get(0));
            if (key.equals("FADC250_NSB"))
                fadcNSBEcal = Integer.valueOf(vals.get(0));
            if (key.equals("FADC250_NPEAK"))
                fadcNPEAKEcal = Integer.valueOf(vals.get(0));
            if (key.equals("FADC250_MODE"))
                fadcMODEEcal = Integer.valueOf(vals.get(0));
            if (key.equals("FADC250_W_WIDTH"))
                fadcWIDTHEcal = Integer.valueOf(vals.get(0));
            if (key.equals("FADC250_W_OFFSET"))
                fadcOFFSETEcal = Integer.valueOf(vals.get(0));

            // Parse the channel thresholds.
            if (key.equals("FADC250_ALLCH_TET")) {
                setChannelParsIntEcal(crate, thisFadcSlot, THRESHOLDECAL, vals);
            }

            // Parse the channel thresholds.
            if (key.equals("FADC250_ALLCH_DELAY")) {
                setChannelParsIntEcal(crate, thisFadcSlot, DELAYECAL, vals);
            }

            // Parse the channel pedestals.
            else if (key.equals("FADC250_ALLCH_PED")) {
                setChannelParsFloatEcal(crate, thisFadcSlot, PEDESTALECAL, vals);
            }

            // Parse the channel gains.
            else if (key.equals("FADC250_ALLCH_GAIN")) {
                setChannelParsFloatEcal(crate, thisFadcSlot, GAINECAL, vals);
            }
        } else if (thisFadcSlot == 10) { // Hodoscope
            // Parse simple FADC data.
            if (key.equals("FADC250_NSA"))
                fadcNSAHodo = Integer.valueOf(vals.get(0));
            if (key.equals("FADC250_NSB"))
                fadcNSBHodo = Integer.valueOf(vals.get(0));
            if (key.equals("FADC250_W_WIDTH"))
                fadcWIDTHHodo = Integer.valueOf(vals.get(0));
            if (key.equals("FADC250_W_OFFSET"))
                fadcOFFSETHodo = Integer.valueOf(vals.get(0));

            // Parse the channel thresholds.
            if (key.equals("FADC250_ALLCH_TET")) {
                setChannelParsIntHodo(crate, thisFadcSlot, THRESHOLDHODO, vals);
            }

            // Parse the channel thresholds.
            if (key.equals("FADC250_ALLCH_DELAY")) {
                setChannelParsIntHodo(crate, thisFadcSlot, DELAYHODO, vals);
            }

            // Parse the channel pedestals.
            else if (key.equals("FADC250_ALLCH_PED")) {
                setChannelParsFloatHodo(crate, thisFadcSlot, PEDESTALHODO, vals);
            }

            // Parse the channel gains.
            else if (key.equals("FADC250_ALLCH_GAIN")) {
                setChannelParsFloatHodo(crate, thisFadcSlot, GAINHODO, vals);
            }
        }
    }

    /**
     * Takes a list of 16 values (in argument <code>vals</code>) and maps the
     * appropriate database calorimeter channel to the list value. This assumes that
     * the <code>String</code> values should be parsed to <code>Float</code>
     * objects.
     * 
     * @param crate - The calorimeter crate associated with the values.
     * @param slot  - The FADC slot associated with the values.
     * @param map   - The map in which to place the values.
     * @param vals  - A <code>List</code> of 16 <code>String</code> objects
     *              representing the channel values. This should correspond to FADC
     *              channels 0 - 15.
     */
    private void setChannelParsFloatEcal(int crate, int slot, Map<EcalChannel, Float> map, List<String> vals) {
        // Iterate over each channel and map the database channel object
        // to the corresponding list value.
        for (int ii = 0; ii < 16; ii++) {
            map.put(findChannelEcal(crate, slot, ii), Float.valueOf(vals.get(ii)));
        }
    }

    /**
     * Takes a list of 16 values (in argument <code>vals</code>) and maps the
     * appropriate database hodoscope channel to the list value. This assumes that
     * the <code>String</code> values should be parsed to <code>Float</code>
     * objects.
     * 
     * @param crate - The hodoscope crate associated with the values.
     * @param slot  - The FADC slot associated with the values.
     * @param map   - The map in which to place the values.
     * @param vals  - A <code>List</code> of 16 <code>String</code> objects
     *              representing the channel values. This should correspond to FADC
     *              channels 0 - 15.
     */
    private void setChannelParsFloatHodo(int crate, int slot, Map<HodoscopeChannel, Float> map, List<String> vals) {
        // Iterate over each channel and map the database channel object
        // to the corresponding list value.
        for (int ii = 0; ii < 16; ii++) {
            map.put(findChannelHodo(crate, slot, ii), Float.valueOf(vals.get(ii)));
        }
    }

    /**
     * Takes a list of 16 values (in argument <code>vals</code>) and maps the
     * appropriate database calorimeter channel to the list value. This assumes that
     * the <code>String</code> values should be parsed to <code>Integer</code>
     * objects.
     * 
     * @param crate - The calorimeter crate associated with the values.
     * @param slot  - The FADC slot associated with the values.
     * @param map   - The map in which to place the values.
     * @param vals  - A <code>List</code> of 16 <code>String</code> objects
     *              representing the channel values.
     */
    private void setChannelParsIntEcal(int crate, int slot, Map<EcalChannel, Integer> map, List<String> vals) {
        // Iterate over each channel and map the database channel object
        // to the corresponding list value.
        for (int ii = 0; ii < 16; ii++) {
            map.put(findChannelEcal(crate, slot, ii), Integer.valueOf(vals.get(ii)));
        }
    }

    /**
     * Takes a list of 16 values (in argument <code>vals</code>) and maps the
     * appropriate database hodoscope channel to the list value. This assumes that
     * the <code>String</code> values should be parsed to <code>Integer</code>
     * objects.
     * 
     * @param crate - The hodoscope crate associated with the values.
     * @param slot  - The FADC slot associated with the values.
     * @param map   - The map in which to place the values.
     * @param vals  - A <code>List</code> of 16 <code>String</code> objects
     *              representing the channel values.
     */
    private void setChannelParsIntHodo(int crate, int slot, Map<HodoscopeChannel, Integer> map, List<String> vals) {
        // Iterate over each channel and map the database channel object
        // to the corresponding list value.
        for (int ii = 0; ii < 16; ii++) {
            map.put(findChannelHodo(crate, slot, ii), Integer.valueOf(vals.get(ii)));
        }
    }

    /**
     * Gets the database calorimeter channel for a channel defined by a crate
     * number, FADC slot, and FADC channel.
     * 
     * @param crate    - The crate number.
     * @param fadcSlot - The FADC slot.
     * @param fadcChan - The FADC channel.
     * @return Returns the database channel as a <code>EcalChannel</code> if it
     *         exists, and <code>null</code> if it does not.
     */
    public EcalChannel findChannelEcal(int crate, int fadcSlot, int fadcChan) {
        // Search through the database channels for a channel that
        // matches the the argument parameters.
        for (EcalChannel cc : channels) {
            // A channel matches the argument if the slot and channel
            // values are the same. Crate number must also match, but
            // note that EcalChannel follows a different convention
            // with respect to crate numbering.
            if (((cc.getCrate() - 1) * 2 == crate - 37) && (cc.getSlot() == fadcSlot)
                    && (cc.getChannel() == fadcChan)) {
                return cc;
            }
        }

        // If no matching channel is found, return null.
        return null;
    }

    /**
     * Gets the database hodoscope channel for a channel defined by a crate number,
     * FADC slot, and FADC channel.
     * 
     * @param crate    - The crate number.
     * @param fadcSlot - The FADC slot.
     * @param fadcChan - The FADC channel.
     * @return Returns the database channel as a <code>HodoscopeChannel</code> if it
     *         exists, and <code>null</code> if it does not.
     */
    public HodoscopeChannel findChannelHodo(int crate, int fadcSlot, int fadcChan) {
        // Search through the database channels for a channel that
        // matches the the argument parameters.
        for (HodoscopeChannel cc : channelsHodo) {
            // A channel matches the argument if the slot and channel
            // values are the same. Crate number must also match, but
            // note that EcalChannel follows a different convention
            // with respect to crate numbering.
            if (((cc.getCrate() - 1) * 2 == crate - 37) && (cc.getSlot() == fadcSlot)
                    && (cc.getChannel() == fadcChan)) {
                return cc;
            }
        }

        // If no matching channel is found, return null.
        return null;
    }

    /**
     * Gets the database crystal channel object based on the crystal's numerical
     * index.
     * 
     * @param channel_id - The crystal index.
     * @return Returns the channel as an <code>EcalChannel</code>.
     */
    public EcalChannel findChannelEcal(int channel_id) {
        return ecalConditions.getChannelCollection().findChannel(channel_id);
    }

    /**
     * Gets the database hodoscope channel object based on the hodoscope's numerical
     * index.
     * 
     * @param channel_id - The hodoscope index.
     * @return Returns the channel as an <code>EcalChannel</code>.
     */
    public HodoscopeChannel findChannelHodo(int channel_id) {
        return hodoConditions.getChannels().findChannel(channel_id);
    }

    /**
     * Prints the mapped parameter keys and values to the terminal.
     */
    public void printMap() {
        System.out.print("\nTriggerConfigMap::::::::::::::::::::::::::::\n");
        for (String key : configMap.keySet()) {
            System.out.printf("%s: ", key);
            for (String val : configMap.get(key)) {
                System.out.printf("%s ", val);
            }
            System.out.printf("\n");
        }
        System.out.println("::::::::::::::::::::::::::::::::::::::::::::");
    }

    /**
     * Prints the parsed parameter values to the terminal.
     */
    public void printVars() {
        System.out.println("\nTriggerConfigVars%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
        System.out.println();
        System.out.println();
        
        System.out.println(String.format("VTP_HPS_ECAL_CLUSTER_HIT_DT: %d", ecalClusterHitDT));
        System.out.println(String.format("VTP_HPS_ECAL_CLUSTER_SEED_THR: %d", ecalClusterSeedThr));
        System.out.println(String.format("VTP_HPS_HODOSCOPE_FADCHIT_THR: %d", hodoFADCHitThr));
        System.out.println(String.format("VTP_HPS_HODOSCOPE_HODO_THR: %d", hodoThr));
        System.out.println(String.format("VTP_HPS_HODOSCOPE_HODO_DT: %d", hodoDT));
        
        System.out.println(String.format("FADC250_NSA_Ecal: %d", fadcNSAEcal));
        System.out.println(String.format("FADC250_NSB_Ecal: %d", fadcNSBEcal));
        System.out.println(String.format("FADC250_NPEAK_Ecal: %d", fadcNPEAKEcal));
        System.out.println(String.format("FADC250_MODE_Ecal: %d", fadcMODEEcal));
        System.out.println(String.format("FADC250_WIDTH_Ecal: %d", fadcWIDTHEcal));
        System.out.println(String.format("FADC250_OFFSET_Ecal: %d", fadcOFFSETEcal));

        System.out.println(String.format("FADC250_NSA_Hodo: %d", fadcNSAHodo));
        System.out.println(String.format("FADC250_NSB_Hodo: %d", fadcNSBHodo));
        System.out.println(String.format("FADC250_WIDTH_Hodo: %d", fadcWIDTHHodo));
        System.out.println(String.format("FADC250_OFFSET_Hodo: %d", fadcOFFSETHodo));

        for (EcalChannel cc : ecalConditions.getChannelCollection()) {
            System.out.println("EcalChannel in Database" + cc);
            if (THRESHOLDECAL.containsKey(cc))
                System.out.println("Parsed threshold:" + THRESHOLDECAL.get(cc));
            if (DELAYECAL.containsKey(cc))
                System.out.println("Parsed delay:" + DELAYECAL.get(cc));
            if (PEDESTALECAL.containsKey(cc))
                System.out.println("Parsed pedestal:" + PEDESTALECAL.get(cc));
            if (GAINECAL.containsKey(cc))
                System.out.println("Parsed gain:" + GAINECAL.get(cc));
        }

        for (HodoscopeChannel cc : hodoConditions.getChannels()) {
            System.out.println("HodoscopeChannel in Database" + cc);
            if (THRESHOLDHODO.containsKey(cc))
                System.out.println("Parsed threshold:" + THRESHOLDHODO.get(cc));
            if (DELAYHODO.containsKey(cc))
                System.out.println("Parsed delay:" + DELAYHODO.get(cc));
            if (PEDESTALHODO.containsKey(cc))
                System.out.println("Parsed pedestal:" + PEDESTALHODO.get(cc));
            if (GAINHODO.containsKey(cc))
                System.out.println("Parsed gain:" + GAINHODO.get(cc));
        }

        System.out.println();
        for (int ii = 0; ii < 4; ii++) {
            System.out.println(String.format("SINGLES_EN %d %b ", ii, singlesEn[ii]));
            System.out.println(String.format("SINGLES_NHITS_EN %d %b", ii, singlesNhitsMinEn[ii]));
            System.out.println(String.format("SINGLES_EMIN_EN %d %b", ii, singlesEnergyMinEn[ii]));
            System.out.println(String.format("SINGLES_EMAX_EN %d %b", ii, singlesEnergyMaxEn[ii]));
            System.out.println(String.format("SINGLES_XMIN_EN %d %b", ii, singlesXMinEn[ii]));
            System.out.println(String.format("SINGLES_L1Matching_EN %d %b", ii, singlesL1MatchingEn[ii]));
            System.out.println(String.format("SINGLES_L2Matching_EN %d %b", ii, singlesL2MatchingEn[ii]));
            System.out.println(String.format("SINGLES_L1L2Matching_EN %d %b", ii, singlesL1L2MatchingEn[ii]));
            System.out.println(String.format("SINGLES_L1L2EcalMatching_EN %d %b", ii, singlesL1L2EcalMatchingEn[ii]));
            System.out.println(String.format("SINGLES_NHTIS %d %d", ii, singlesNhitsMin[ii]));
            System.out.println(String.format("SINGLES_EMIN %d %d", ii, singlesEnergyMin[ii]));
            System.out.println(String.format("SINGLES_EMAX %d %d", ii, singlesEnergyMax[ii]));
            System.out.println(String.format("SINGLES_XMIN %d %d", ii, singlesXMin[ii]));
            System.out.println(String.format("SINGLES_PDE_CO %d %f", ii, singlesPDEC0[ii]));
            System.out.println(String.format("SINGLES_PDE_C1 %d %f", ii, singlesPDEC1[ii]));
            System.out.println(String.format("SINGLES_PDE_C2 %d %f", ii, singlesPDEC2[ii]));
            System.out.println(String.format("SINGLES_PDE_C3 %d %f", ii, singlesPDEC3[ii]));

            System.out.println(String.format("PAIRS_EN %d %b ", ii, pairsEn[ii]));
            System.out.println(String.format("PAIRS_SUMMAXMIN_EN %d %b", ii, pairsEnergySumMaxMinEn[ii]));
            System.out.println(String.format("PAIRS_ENERGYDIFF_EN %d %b", ii, pairsEnergyDiffEn[ii]));
            System.out.println(String.format("PAIRS_COP_EN %d %b", ii, pairsCoplanarityEn[ii]));
            System.out.println(String.format("PAIRS_EDIST_EN %d %b", ii, pairsEnergyDistEn[ii]));
            System.out.println(String.format("PAIRS_NHITS %d %d", ii, pairsNhitsMin[ii]));
            System.out.println(String.format("PAIRS_EMIN %d %d", ii, pairsEnergyMin[ii]));
            System.out.println(String.format("PAIRS_EMAX %d %d", ii, pairsEnergyMax[ii]));
            System.out.println(String.format("PAIRS_SUMMIN %d %d", ii, pairsEnergySumMin[ii]));
            System.out.println(String.format("PAIRS_SUMMAX %d %d", ii, pairsEnergySumMax[ii]));
            System.out.println(String.format("PAIRS_ENERGYDIFF %d %d", ii, pairsEnergyDiffMax[ii]));
            System.out.println(String.format("PAIRS_COPMAX %d %d", ii, pairsCoplanarityMax[ii]));
            System.out.println(String.format("PAIRS_TDIFFMAAX %d %d", ii, pairsTimeDiffMax[ii]));
            System.out.println(String.format("PAIRS_EDISTMIN %d %d", ii, pairsEnergyDistMin[ii]));
            System.out.println(String.format("PAIRS_EDISTSLOP %d %f", ii, pairsEnergyDistSlope[ii]));
        }

        for (int ii = 0; ii < 2; ii++) {
            System.out.println(String.format("MULT_EN %d %b", ii, multEn[ii]));
            System.out.println(String.format("MULT_NHITS %d %d", ii, multNhitsMin[ii]));
            System.out.println(String.format("MULT_EMIN %d %d", ii, multEnergyMin[ii]));
            System.out.println(String.format("MULT_EMAX %d %d", ii, multEnergyMax[ii]));
            System.out.println(String.format("MULT_TOP_MULT_MIN %d %d", ii, multTopMultMin[ii]));
            System.out.println(String.format("MULT_BOT_MULT_MIN %d %d", ii, multBotMultMin[ii]));
            System.out.println(String.format("MULT_TOT_MULT_MIN %d %d", ii, multTotMultMin[ii]));
            System.out.println(String.format("MULT_DT %d %d", ii, multDT[ii]));
        }

        System.out.println(String.format("FEE_EN %b", FEEEn));
        System.out.println(String.format("FEE_NHITS %d", FEENhitsMin));
        System.out.println(String.format("FEE_EMIN %d", FEEEnergyMin));
        System.out.println(String.format("FEE_EMAX %d", FEEEnergyMax));
        
        for (int ii = 0; ii < 7; ii++) {
            System.out.println(String.format("FEE_Prescale %d\t%d\t%d\t%d", ii, FEERegionXMin[ii], FEERegionXMax[ii], FEERegionPrescale[ii]));
        }

        for (int ii = 0; ii < 20; ii++) {
            System.out.println(String.format("TS_Prescale %d %d", ii, TSPrescale[ii]));
        }
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
    }
}

package org.hps.record.daqconfig;

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

/**
 * Class <code>EvioDAQParser</code> takes DAQ configuration banks from
 * EvIO data and extracts the configuration parameters from them. These
 * are then stored within package-accessible variables within the class.
 * <br/><br/>
 * Note that this class should not be used directly to acquire DAQ
 * configuration data. It is intended to be used internally by the EvIO
 * parser and the class <code>ConfigurationManager</code>. The latter
 * should be used for accessing this information for any other classes.
 * 
 * @author Nathan Baltzell <baltzell@jlab.org>
 */
public class EvioDAQParser {
    /*
     * Read/Parse/Save the DAQ trigger configuration settings.
     * These settings arrive in multiple banks, but they *should* be in the same event.
     * 
     * Currently this is set up to read SSP and ECAL configurations,
     * which is all that is currently available in EVIO as of Feb 28, 2015.
     * 
     * GTP settings and Prescale factors will need to be added to this class when added to EVIO.
     * 
     * TODO: Error in EVIO format for Crate 39 for 2014 data requires another JEVIO workaround (realized Feb. 16).
     *       ** This was fixed in EVIO for data after run 4044.
     * 
     * TODO: Manually put in GTP settings based on run number for 2014 data.
     * TODO: Manually deal with change in format of SSP_HPS_SINGLES_NMIN (at 3312(?)).
     * 
     * TODO: Restructure, clean up...
     */
    /** The EvIO bank identification tag for DAQ configuration banks. */
    public static final int BANK_TAG = 0xE10E;
    
    // Stores the hardware codes for each trigger type.
    private static final int[] singlesIOsrc = { 20, 21 };
    private static final int[] pairsIOsrc = { 22, 23 };
    
    // Dump everything read from the DAQ Configuration Bank, minimal interpretation:
    private Map<String, List<String>> configMap = new HashMap<String, List<String>>();
    
    // Class parameters.
    private int nBanks = 0;
    private boolean debug = false;
    
    // FADC Config:
    /** The length of time after a pulse-crossing event that the pulse
     * should be integrated. Uses units of clock-cycles. */
    int fadcNSA    = 0;
    /** The length of time before a pulse-crossing event that the pulse
     * should be integrated. Uses units of clock-cycles. */
    int fadcNSB    = 0;
    /** The maximum number of pulses that will be extracted from a single
     * channel within  a readout window. */
    int fadcNPEAK  = 0;
    /** The pulse-processing mode used by the FADC. This should be 1,
     * 3, or 7. */
    int fadcMODE   = 0;
    /** The size of readout window in nanoseconds. */
    int fadcWIDTH  = 0;
    /** The time-offset of the readout window in ns. */
    int fadcOFFSET = 0;
    /** Map of <code>EcalChannel</code> to the gain for that channel.
     * Uses units of ADC / MeV for the mapped value. */
    Map<EcalChannel, Float> GAIN = new HashMap<EcalChannel, Float>();
    /** Map of <code>EcalChannel</code> to the pedestal for that channel.
     * Uses units of ADC for the mapped value. */
    Map<EcalChannel, Float> PEDESTAL = new HashMap<EcalChannel, Float>();
    /** Map of <code>EcalChannel</code> to the threshold for that channel.
     * Uses units of ADC for the mapped value. */
    Map<EcalChannel, Integer> THRESHOLD = new HashMap<EcalChannel, Integer>();
    
    // GTP Clustering Cut Values:
    /** The seed energy lower bound cut used by the clusterer. Value is
     * in units of MeV. */
    int gtpMinSeedEnergy  = 0;
    /** The length of the clustering verification/inclusion window before
     * the seed hit. Uses units of clock-cycles. */
    int gtpWindowBefore = 0;
    /** The length of the clustering verification/inclusion window after
     * the seed hit. Uses units of clock-cycles. */
    int gtpWindowAfter = 0;
    
    // Triggers Enabled:
    /** Indicates whether the singles triggers are enabled or not. Uses
     * the format <code>{ Singles0_Enabled, Singles1_Enabled }</code>. */
    boolean[] singlesEn = { false, false };
    /** Indicates whether the pair triggers are enabled or not. Uses
     * the format <code>{ Pair0_Enabled, Pair1_Enabled }</code>. */
    boolean[] pairsEn   = { false, false };
    
    // Singles Cuts Enabled:
    /** Indicates whether the singles trigger cluster hit count cuts
     * are enabled or not. Uses the format
     * <code>{ Singles0_Cut_Enabled, Singles1_Cut_Enabled }</code>. */
    boolean[] singlesNhitsEn     = { false, false };
    /** Indicates whether the singles trigger cluster total energy lower
     * bound cuts are enabled or not. Uses the format
     * <code>{ Singles0_Cut_Enabled, Singles1_Cut_Enabled }</code>. */
    boolean[] singlesEnergyMinEn = { false, false };
    /** Indicates whether the singles trigger cluster total energy upper
     * bound cuts are enabled or not. Uses the format
     * <code>{ Singles0_Cut_Enabled, Singles1_Cut_Enabled }</code>. */
    boolean[] singlesEnergyMaxEn = { false, false };
    
    // Pairs Cuts Enabled:
    /** Indicates whether the pair trigger pair energy sum cuts are
     * enabled or not. Uses the format
     * <code>{ Pair0_Cut_Enabled, Pair1_Cut_Enabled }</code>. */
    boolean[] pairsEnergySumMaxMinEn = { false, false };
    /** Indicates whether the pair trigger pair energy difference cuts
     * are enabled or not. Uses the format
     * <code>{ Pair0_Cut_Enabled, Pair1_Cut_Enabled }</code>. */
    boolean[] pairsEnergyDiffEn      = { false, false };
    /** Indicates whether the pair trigger pair coplanarity cuts are
     * enabled or not. Uses the format
     * <code>{ Pair0_Cut_Enabled, Pair1_Cut_Enabled }</code>. */
    boolean[] pairsCoplanarityEn     = { false, false };
    /** Indicates whether the pair trigger pair energy slope cuts are
     * enabled or not. Uses the format
     * <code>{ Pair0_Cut_Enabled, Pair1_Cut_Enabled }</code>. */
    boolean[] pairsEnergyDistEn      = { false, false };
    
    // Singles Cut Values:
    /** Specifies the value of the singles trigger cluster hit count
     * cuts. Use the format, in units of hits,
     * <code>{ Singles0_Cut_Value, Singles1_Cut_Value }</code>. */
    int[] singlesNhits     = { 0, 0 };
    /** Specifies the value of the singles trigger cluster total energy
     * lower bound cuts. Use the format, in units of MeV,
     * <code>{ Singles0_Cut_Value, Singles1_Cut_Value }</code>. */
    int[] singlesEnergyMin = { 0, 0 };
    /** Specifies the value of the singles trigger cluster total energy
     * upper bound cuts. Use the format, in units of MeV,
     * <code>{ Singles0_Cut_Value, Singles1_Cut_Value }</code>. */
    int[] singlesEnergyMax = { 0, 0 };
    
    // Pairs Cut Values:
    /** Specifies the value of the pair trigger cluster hit count cuts.
     * Use the format, in units of hits,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value }</code>. */
    int[] pairsNhitsMin       = { 0, 0 };
    /** Specifies the value of the pair trigger cluster total energy
     * lower bound cuts. Use the format, in units of MeV,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value }</code>. */
    int[] pairsEnergyMin      = { 0, 0 };
    /** Specifies the value of the pair trigger cluster total energy
     * upper bound cuts. Use the format, in units of MeV,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value }</code>. */
    int[] pairsEnergyMax      = { 0, 0 };
    /** Specifies the value of the pair trigger pair energy sum upper
     * bound cuts. Use the format, in units of MeV,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value }</code>. */
    int[] pairsEnergySumMin   = { 0, 0 };
    /** Specifies the value of the pair trigger pair energy sum lower
     * bound cuts. Use the format, in units of MeV,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value }</code>. */
    int[] pairsEnergySumMax   = { 0, 0 };
    /** Specifies the value of the pair trigger pair energy difference
     * cuts. Use the format, in units of MeV,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value }</code>. */
    int[] pairsEnergyDiffMax  = { 0, 0 };
    /** Specifies the value of the pair trigger pair coplanarity cuts.
     * Use the format, in units of degrees,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value }</code>. */
    int[] pairsCoplanarityMax = { 0, 0 };
    /** Specifies the value of the pair trigger pair time coincidence
     * cuts. Use the format, in units of nanoseconds,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value }</code>. */
    int[] pairsTimeDiffMax    = { 0, 0 };
    /** Specifies the value of the pair trigger pair energy slope cuts.
     * Use the format, in units of MeV,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value }</code>. */
    int[] pairsEnergyDistMin  = { 0, 0 };
    
    // Pairs Cut Parameters:
    /** Specifies the value of the pair trigger pair energy slope cuts'
     * parameter F. Use the format, in units of MeV / mm,
     * <code>{ Pair0_Cut_Value, Pair1_Cut_Value }</code>. */
    float[] pairsEnergyDistSlope = { 0, 0 };
    
    // Tracks the last FADC slot seen. This is needed for parsing FADC
    // threshold, pedestal, and gain information.
    private int thisFadcSlot = 0;
    
    // Cache local set of EcalChannels:
    private EcalConditions ecalConditions = null;
    private List<EcalChannel> channels = new ArrayList<EcalChannel>();
    
    /**
     * Instantiates the <code>EvioDAQParser</code>.
     */
    public EvioDAQParser() {
        // Create a map to map crystals to their database channel object.
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
        for (int ii = 0; ii < 442; ii++) {
            channels.add(findChannel(ii + 1));
        } 
    }
    
    /**
     * Parses a set of configuration tables to obtain DAQ configuration
     * parameters.
     * @param crate - The crate associated with the configuration tables.
     * @param runNumber - The run number for the current data set.
     * @param configurationTables - Tables containing DAQ configuration
     * parameters.
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
        if(debug) { printMap(); }
        
        // If this run is known to be missing configuration values,
        // handle the missing values.
        fixConfigMap2014Run(runNumber);
        
        // Parse the previously generated configuration map and extract
        // the DAQ configuration from it.
        parseConfigMap();
        
        // If the expected number of banks have been parsed and debugging
        // text is enabled, print out all of the parsed variables.
        if(nBanks > 2 && debug) { printVars(); }
    }
    
    /**
     * Converts the textual configuration information into a map, where
     * the first column value becomes the map entry key and the remainder
     * becomes the map entry value.
     * @param crate - The calorimeter crate associated with the textual
     * configuration data.
     * @param configTables - An array of textual configuration tables that
     * contain the DAQ configuration parameters.
     */
    private void loadConfigMap(int crate, String[] configTables) {
        // Iterate over each configuration table.
        for(String configTable : configTables) {
            // Split each table into rows and iterate over the rows.
            rowLoop:
            for(String line : configTable.trim().split("\n")) {
                // Split the first column from the row.
                String[] cols = line.trim().split(" +", 2);
                
                // If there are fewer than two segments after the split,
                // then this is not a valid parameter entry.
                if(cols.length < 2) continue rowLoop;
                
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
                
                // SPECIAL CASE: Key "SSP_HPS_SET_IO_SRC"
                // This entry indicates which triggers are enabled and
                // needs to be parsed differently than normal.
                else if(key.startsWith("SSP_HPS_SET_IO_SRC")) {
                    // The first "parameter value" is a hardware code
                    // that identifies the trigger. Obtain it.
                    int trig = Integer.valueOf(vals.get(1));
                    
                    // There are two trigger of each type, singles and
                    // pairs. Compare the hardware code to the codes
                    // for each of these triggers to determine which
                    // it this parameter entry represents and then set
                    // its value appropriately.
                    for (int ii = 0; ii < pairsIOsrc.length; ii++) {
                        if(trig == singlesIOsrc[ii]) {
                            singlesEn[ii] = true;
                        }
                        else if(trig == pairsIOsrc[ii]) {
                            pairsEn[ii] = true;
                        }
                    }
                }
                
                // GENERAL CASE: Basic Parameter
                // This indicates a regular parameter that does not
                // require any special parsing.
                if(vals.size() > 1 && key.startsWith("SSP")) {
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
     * Parses the configuration parameter map entries and extracts the
     * parameter values for those parameters which have the standard
     * format of <code>[PARAMETER KEY] --> { [PARAMETER VALUES] }</code>.
     */
    public void parseConfigMap() {
        // Parse simple FADC data.
        fadcNSA    = Integer.valueOf(getConfigParameter("FADC250_NSA",      0));
        fadcNSB    = Integer.valueOf(getConfigParameter("FADC250_NSB",      0));
        fadcNPEAK  = Integer.valueOf(getConfigParameter("FADC250_NPEAK",    0));
        fadcMODE   = Integer.valueOf(getConfigParameter("FADC250_MODE",     0));
        fadcWIDTH  = Integer.valueOf(getConfigParameter("FADC250_W_WIDTH",  0));
        fadcOFFSET = Integer.valueOf(getConfigParameter("FADC250_W_OFFSET", 0));
        
        // Parse GTP data.
        gtpMinSeedEnergy = Integer.valueOf(getConfigParameter("GTP_CLUSTER_PULSE_THRESHOLD", 0));
        gtpWindowBefore  = Integer.valueOf(getConfigParameter("GTP_CLUSTER_PULSE_COIN",      0));
        gtpWindowAfter   = Integer.valueOf(getConfigParameter("GTP_CLUSTER_PULSE_COIN",      1));
        
        // Parse trigger data.
        for(int ii = 0; ii < 2; ii++) {
            // Check singles trigger cuts enabled status.
            singlesNhitsEn[ii]         = getBoolConfigSSP(ii,  "SINGLES_NMIN",          1);
            singlesEnergyMinEn[ii]     = getBoolConfigSSP(ii,  "SINGLES_EMIN",          1);
            singlesEnergyMaxEn[ii]     = getBoolConfigSSP(ii,  "SINGLES_EMAX",          1);
            
            // Check pair trigger cuts enabled status.
            pairsEnergySumMaxMinEn[ii] = getBoolConfigSSP(ii,  "PAIRS_SUMMAX_MIN",      2);
            pairsEnergyDiffEn[ii]      = getBoolConfigSSP(ii,  "PAIRS_DIFFMAX",         1);
            pairsCoplanarityEn[ii]     = getBoolConfigSSP(ii,  "PAIRS_COPLANARITY",     1);
            pairsEnergyDistEn[ii]      = getBoolConfigSSP(ii,  "PAIRS_ENERGYDIST",      2);
            
            // Get the singles trigger cuts.
            singlesNhits[ii]           = getIntConfigSSP(ii,   "SINGLES_NMIN",          0);
            singlesEnergyMin[ii]       = getIntConfigSSP(ii,   "SINGLES_EMIN",          0);
            singlesEnergyMax[ii]       = getIntConfigSSP(ii,   "SINGLES_EMAX",          0);
            
            // Get the pair trigger cuts.
            pairsNhitsMin[ii]          = getIntConfigSSP(ii,   "PAIRS_NMIN",            0);
            pairsEnergyMin[ii]         = getIntConfigSSP(ii,   "PAIRS_EMIN",            0);
            pairsEnergyMax[ii]         = getIntConfigSSP(ii,   "PAIRS_EMAX",            0);
            pairsEnergySumMin[ii]      = getIntConfigSSP(ii,   "PAIRS_SUMMAX_MIN",      1);
            pairsEnergySumMax[ii]      = getIntConfigSSP(ii,   "PAIRS_SUMMAX_MIN",      0);
            pairsEnergyDiffMax[ii]     = getIntConfigSSP(ii,   "PAIRS_DIFFMAX",         0);
            pairsCoplanarityMax[ii]    = getIntConfigSSP(ii,   "PAIRS_COPLANARITY",     0);
            pairsTimeDiffMax[ii]       = getIntConfigSSP(ii,   "PAIRS_TIMECOINCIDENCE", 0);
            pairsEnergyDistSlope[ii]   = getFloatConfigSSP(ii, "PAIRS_ENERGYDIST",      0);
            pairsEnergyDistMin[ii]     = getIntConfigSSP(ii,   "PAIRS_ENERGYDIST",      1);
        }
    }
    
    /**
     * Method corrects parameter data for runs that either did not
     * correctly record the full configuration data or were bugged.
     * It populates missing fields with a zero value entry.
     * @param runNumber - The run number for the current run. This is
     * used to determine if the run is a "bugged" run.
     */
    private void fixConfigMap2014Run(int runNumber) {
        // If this is a good run, noting should be done. Return.
        if(runNumber > 3470 || runNumber < 3100) { return; }
        
        // Populate missing GTP entries.
        List<String> tmp = new ArrayList<String>();
        tmp.add("0");
        tmp.add("0");
        tmp.add("0");
        tmp.add("0");
        configMap.put("GTP_CLUSTER_THRESH" ,tmp);
        configMap.put("GTP_TIMEDIFF", tmp);
        
        // TODO: Port datacat/python/engrun/engrun_metadata.py
        // 1. SET GTP SETTINGS MANUALLY BASED ON RUN NUMBER
        // 2. FIX SINGLES_NMIN prior to 3312
    }
    
    /**
     * Parses FADC configuration parameter entries. These all have 17
     * lines, the first of which is the parameter key and the subsequent
     * being parameter values corresponding to the 16 FADC channels for
     * the indicated FADC slot. These entries contain thresholds, gains,
     * and pedestals.
     * @param crate - The crate associated with this parameter entry.
     * @param key - The parameter key.
     * @param vals - A list of 16 values with indices corresponding to
     * the FADC channel with which they are associated.
     */
    private void parseFADC(int crate, String key, List<String> vals) {
        // The FADC slot is not stored on the same line as the other
        // data and must be parsed and retained, as it is necessary
        // for handling the subsequent lines. If this line is the
        // FADC slot, store it.
        if(key.equals("FADC250_SLOT")) {
            thisFadcSlot = Integer.valueOf(vals.get(0));
        }
        
        // Parse the channel thresholds.
        else if(key.equals("FADC250_ALLCH_TET")) {
            setChannelParsInt(crate, thisFadcSlot, THRESHOLD, vals);
        }
        
        // Parse the channel pedestals.
        else if(key.equals("FADC250_ALLCH_PED")) {
            setChannelParsFloat(crate, thisFadcSlot, PEDESTAL, vals);    
        }
        
        // Parse the channel gains.
        else if(key.equals("FADC250_ALLCH_GAIN")) {
            setChannelParsFloat(crate, thisFadcSlot, GAIN, vals);
        }
    }
    
    /**
     * Takes a list of 16 values (in argument <code>vals</code>) and
     * maps the appropriate database calorimeter channel to the list
     * value. This assumes that the <code>String</code> values should
     * be parsed to <code>Float</code> objects.
     * @param crate - The calorimeter crate associated with the values.
     * @param slot - The FADC slot associated with the values.
     * @param map - The map in which to place the values.
     * @param vals - A <code>List</code> of 16 <code>String</code>
     * objects representing the channel values. This should correspond
     * to FADC channels 0 - 15.
     */
    private void setChannelParsFloat(int crate, int slot, Map<EcalChannel, Float> map, List<String> vals) {
        // Iterate over each channel and map the database channel object
        // to the corresponding list value.
        for(int ii = 0; ii < 16; ii++) {
            map.put(findChannel(crate, slot, ii), Float.valueOf(vals.get(ii)));
        }
    }
    
    /**
     * Takes a list of 16 values (in argument <code>vals</code>) and
     * maps the appropriate database calorimeter channel to the list
     * value. This assumes that the <code>String</code> values should
     * be parsed to <code>Integer</code> objects.
     * @param crate - The calorimeter crate associated with the values.
     * @param slot - The FADC slot associated with the values.
     * @param map - The map in which to place the values.
     * @param vals - A <code>List</code> of 16 <code>String</code>
     * objects representing the channel values.
     */
    private void setChannelParsInt(int crate, int slot, Map<EcalChannel, Integer> map, List<String> vals) {
        // Iterate over each channel and map the database channel object
        // to the corresponding list value.
        for(int ii = 0; ii < 16; ii++) {
            map.put(findChannel(crate, slot, ii), Integer.valueOf(vals.get(ii)));
        }
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
        System.out.println(String.format("GTPMINSEED: %d",     gtpMinSeedEnergy));
        System.out.println(String.format("GTPMINHITDT: %d",    gtpWindowBefore));
        System.out.println(String.format("GTPMAXHITDT: %d",    gtpWindowAfter));
        System.out.println();
        System.out.println(String.format("FADC250_NSA: %d",    fadcNSA));
        System.out.println(String.format("FADC250_NSB: %d",    fadcNSB));
        System.out.println(String.format("FADC250_NPEAK: %d",  fadcNPEAK));
        System.out.println(String.format("FADC250_MODE: %d",   fadcMODE));
        System.out.println(String.format("FADC250_WIDTH: %d",  fadcWIDTH));
        System.out.println(String.format("FADC250_OFFSET: %d", fadcOFFSET));
        for(EcalChannel cc : ecalConditions.getChannelCollection()) {
            if(!PEDESTAL.containsKey(cc)) {
                System.out.println("\nP !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
            if(!THRESHOLD.containsKey(cc)) {
                System.out.println("\nT !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
            if(!GAIN.containsKey(cc)) {
                System.out.println("\nG !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
        }
        
        System.out.println();
        for(int ii = 0; ii < 2; ii++) {
            System.out.println(String.format("SINGLES_EN %d %b ",         ii, singlesEn[ii]));
            System.out.println(String.format("PAIRS_EN %d %b ",           ii, pairsEn[ii]));
            
            System.out.println(String.format("SINGLES_NHITS_EN %d %b: ",  ii, singlesNhitsEn[ii]));
            System.out.println(String.format("SINGLES_EMIN_EN %d %b",     ii, singlesEnergyMinEn[ii]));
            System.out.println(String.format("SINGLES_EMAX_EN %d %b",     ii, singlesEnergyMaxEn[ii]));
            
            System.out.println(String.format("PAIRS_SUMMAXMIN_EN %d %b",  ii, pairsEnergySumMaxMinEn[ii]));
            System.out.println(String.format("PAIRS_ENERGYDIFF_EN %d %b", ii, pairsEnergyDiffEn[ii]));
            System.out.println(String.format("PAIRS_COP_EN %d %b",        ii, pairsCoplanarityEn[ii]));
            System.out.println(String.format("PAIRS_EDIST_EN %d %b",      ii, pairsEnergyDistEn[ii]));
            
            System.out.println(String.format("SINGLES_NHTIS %d %d",       ii, singlesNhits[ii]));
            System.out.println(String.format("SINGLES_EMIN %d %d",        ii, singlesEnergyMin[ii]));
            System.out.println(String.format("SINGLES_EMAX %d %d",        ii, singlesEnergyMax[ii]));
            
            System.out.println(String.format("PAIRS_NHITS %d %d",         ii, pairsNhitsMin[ii]));
            System.out.println(String.format("PAIRS_SUMMIN %d %d",        ii, pairsEnergySumMin[ii]));
            System.out.println(String.format("PAIRS_SUMMAX %d %d",        ii, pairsEnergySumMax[ii]));
            System.out.println(String.format("PAIRS_ENERGYDIFF %d %d",    ii, pairsEnergyDiffMax[ii]));
            System.out.println(String.format("PAIRS_COPMAX %d %d",        ii, pairsCoplanarityMax[ii]));
            System.out.println(String.format("PAIRS_TDIFFMAAX %d %d",     ii, pairsTimeDiffMax[ii]));
            System.out.println(String.format("PAIRS_EDISTMIN %d %d",      ii, pairsEnergyDistMin[ii]));
            System.out.println(String.format("PAIRS_EDISTSLOP %d %f",     ii, pairsEnergyDistSlope[ii]));
        }
        
        System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
    }
    
    /**
     * Gets an SSP parameter value using a shortened version of the
     * full parameter key and parses it as a <code>float</code>.
     * @param itrig - The number of the trigger for which to obtain
     * the parameter value.
     * @param stub - The shortened version of the parameter key. This
     * corresponds to "SSP_HPS_[STUB]_[TRIGGER NUMBER]".
     * @param ival - The index of the value that is to be obtained.
     * @return Returns the requested value if it exists. Otherwise, a
     * value of <code>0</code> is returned and a message is logged.
     */
    public float getFloatConfigSSP(int itrig, String stub, int ival) {
        return Float.valueOf(getConfigSSP(itrig, stub, ival));
    }
    
    /**
     * Gets an SSP parameter value using a shortened version of the
     * full parameter key and parses it as a <code>int</code>.
     * @param itrig - The number of the trigger for which to obtain
     * the parameter value.
     * @param stub - The shortened version of the parameter key. This
     * corresponds to "SSP_HPS_[STUB]_[TRIGGER NUMBER]".
     * @param ival - The index of the value that is to be obtained.
     * @return Returns the requested value if it exists. Otherwise, a
     * value of <code>0</code> is returned and a message is logged.
     */
    public int getIntConfigSSP(int itrig, String stub, int ival) {
        return Integer.valueOf(getConfigSSP(itrig, stub, ival));
    }
    
    /**
     * Gets an SSP parameter value using a shortened version of the
     * full parameter key and parses it as a <code>boolean</code>.
     * @param itrig - The number of the trigger for which to obtain
     * the parameter value.
     * @param stub - The shortened version of the parameter key. This
     * corresponds to "SSP_HPS_[STUB]_[TRIGGER NUMBER]".
     * @param ival - The index of the value that is to be obtained.
     * @return Returns the requested value if it exists. Otherwise, a
     * value of <code>false</code> is returned and a message is logged.
     */
    public boolean getBoolConfigSSP(int itrig, String stub, int ival) {
        return "1".equals(getConfigSSP(itrig, stub, ival));
    }
    
    /**
     * Gets an SSP parameter value using a shortened version of the
     * full parameter key.
     * @param itrig - The number of the trigger for which to obtain
     * the parameter value.
     * @param stub - The shortened version of the parameter key. This
     * corresponds to "SSP_HPS_[STUB]_[TRIGGER NUMBER]".
     * @param ival - The index of the value that is to be obtained.
     * @return Returns the requested value if it exists. Otherwise, a
     * value of <code>"0"</code> is returned and a message is logged.
     */
    public String getConfigSSP(int itrig, String stub, int ival) {
        String key = "SSP_HPS_" + stub + "_" + itrig;
        return getConfigParameter(key, ival);
    }
    
    /**
     * Gets a parameter value associated with a parameter key.
     * @param key - The parameter key to which the value belongs.
     * @param ival - The index of the desired parameter value.
     * @return Returns the requested parameter value if it exists and
     * returns <code>"0"</code> otherwise. In the event that a parameter
     * can not be found, an error message is passed to the logger.
     */
    public String getConfigParameter(String key, int ival) {
        // Check the parameter map for the requested parameter key.
        if(configMap.containsKey(key)) {
            // Get the list of values associated with this parameter key.
            List<String> vals = configMap.get(key);
            
            // Check that the list of values contains a parameter for
            // the requested parameter index. If it does, return it.
            if (ival < vals.size()) { return configMap.get(key).get(ival); }
            
            // Otherwise, an error has occurred. Log this and return the
            // default value of zero.
            else {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "ConfigMap TOO SHORT:   " + ival + " " + configMap.get(key));
                return "0";
            }
        }
        
        // If the key is not present...
        else {
            // If more than 2 banks have been read, the absence of a
            // key represents an error. Log that this has occurred.
            if(nBanks > 2) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "ConfigMap MISSING KEY:   " + key);
            }
            
            // Return a default value of zero.
            return "0";
        }
    }
    
    /**
     * Gets the database calorimeter channel for a channel defined by
     * a crate number, FADC slot, and FADC channel.
     * @param crate - The crate number.
     * @param fadcSlot - The FADC slot.
     * @param fadcChan - The FADC channel.
     * @return Returns the database channel as a <code>EcalChannel</code>
     * if it exists, and <code>null</code> if it does not.
     */
    public EcalChannel findChannel(int crate, int fadcSlot, int fadcChan) {
        // Search through the database channels for a channel that
        // matches the the argument parameters.
        for (EcalChannel cc : channels) {
            // A channel matches the argument if the slot and channel
            // values are the same. Crate number must also match, but
            // note that EcalChannel follows a different convention
            // with respect to crate numbering.
            if( ((cc.getCrate() - 1) * 2 == crate - 37) && (cc.getSlot() == fadcSlot) && (cc.getChannel() == fadcChan) ) {
                return cc;
            }
        }
        
        // If no matching channel is found, return null.
        return null;
    }
    
    /**
     * Gets the database crystal channel object based on the crystal's
     * numerical index.
     * @param channel_id - The crystal index.
     * @return Returns the channel as an <code>EcalChannel</code>.
     */
    public EcalChannel findChannel(int channel_id) {
        return ecalConditions.getChannelCollection().findChannel(channel_id);
    }
}

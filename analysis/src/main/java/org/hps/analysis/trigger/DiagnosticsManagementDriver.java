package org.hps.analysis.trigger;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import org.hps.analysis.trigger.util.OutputLogger;
import org.hps.analysis.trigger.util.PairTrigger;
import org.hps.analysis.trigger.util.SinglesTrigger;
import org.hps.analysis.trigger.util.TriggerDiagnosticUtil;
import org.hps.record.daqconfig.ConfigurationManager;
import org.hps.record.daqconfig.DAQConfig;
import org.hps.record.daqconfig.PairTriggerConfig;
import org.hps.record.daqconfig.SinglesTriggerConfig;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.SSPCluster;
import org.hps.record.triggerbank.SSPData;
import org.hps.record.triggerbank.SSPPairTrigger;
import org.hps.record.triggerbank.SSPSinglesTrigger;
import org.hps.record.triggerbank.TIData;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;

public class DiagnosticsManagementDriver extends Driver {
    // Track global statistical information.
    private int events = 0;
    private int noiseEvents = 0;
    private int clusterFailEvents = 0;
    private int singlesFailEvents = 0;
    private int pairFailEvents = 0;
    private int[][] tiEvents = new int[2][6];
    
    // Track global cluster statistical information.
    private int simClusterCount = 0;
    private int hardwareClusterCount = 0;
    private int matchedClusters = 0;
    private int matchClusterFailPosition = 0;
    private int matchClusterFailHitCount = 0;
    private int matchClusterFailEnergy = 0;
    private int matchClusterFailTime = 0;
    
    // Track global trigger information.
    private int[] allHardwareTriggerCount = new int[4];
    private int[] allSimSimTriggerCount = new int[4];
    private int[] allHardwareSimTriggerCount = new int[4];
    private int[] allMatchedSimSimTriggers = new int[4];
    private int[] allMatchedHardwareSimTriggers = new int[4];
    private int[][] tiHardwareTriggerCount = new int[6][4];
    private int[][] tiSimSimTriggerCount = new int[6][4];
    private int[][] tiHardwareSimTriggerCount = new int[6][4];
    private int[][] tiMatchedSimSimTriggers = new int[6][4];
    private int[][] tiMatchedHardwareSimTriggers = new int[6][4];
    
    // Store the LCIO collection names for the needed objects.
    private String hitCollectionName = "EcalCalHits";
    private String bankCollectionName = "TriggerBank";
    private String clusterCollectionName = "EcalClusters";
    private String simTriggerCollectionName = "SimTriggers";
    
    // Trigger modules for performing trigger analysis.
    //private int activeTrigger = -1;
    private boolean[] tiFlags = new boolean[6];
    private TriggerModule[] singlesTrigger = new TriggerModule[2];
    private TriggerModule[] pairsTrigger = new TriggerModule[2];
    private boolean[][] singlesCutsEnabled = new boolean[2][3];
    private boolean[][] pairCutsEnabled = new boolean[2][7];
    private boolean[] singlesTriggerEnabled = new boolean[2];
    private boolean[] pairTriggerEnabled = new boolean[2];
    
    // Verification settings.
    private int nsa = 100;
    private int nsb = 20;
    private int windowWidth = 200;
    private int hitAcceptance = 1;
    private int noiseThreshold = 50;
    private double energyAcceptance = 0.003;
    private int localWindowThreshold = 1000000000;
    
    // Verbose settings.
    private boolean verbose = false;
    private boolean printClusterFail = true;
    private boolean printSinglesTriggerEfficiencyFail = true;
    private boolean printSinglesTriggerInternalFail = true;
    private boolean printPairTriggerEfficiencyFail = true;
    private boolean printPairTriggerInternalFail = true;
    private int     statPrintInterval = Integer.MAX_VALUE;
    
    // Cut index arrays for trigger verification.
    private static final int ENERGY_MIN   = TriggerDiagnosticUtil.SINGLES_ENERGY_MIN;
    private static final int ENERGY_MAX   = TriggerDiagnosticUtil.SINGLES_ENERGY_MAX;
    private static final int HIT_COUNT    = TriggerDiagnosticUtil.SINGLES_HIT_COUNT;
    private static final int ENERGY_SUM   = TriggerDiagnosticUtil.PAIR_ENERGY_SUM;
    private static final int ENERGY_DIFF  = TriggerDiagnosticUtil.PAIR_ENERGY_DIFF;
    private static final int ENERGY_SLOPE = TriggerDiagnosticUtil.PAIR_ENERGY_SLOPE;
    private static final int COPLANARITY  = TriggerDiagnosticUtil.PAIR_COPLANARITY;
    
    // Create trigger index labels.
    public static final int SINGLES0 = 0;
    public static final int SINGLES1 = 1;
    public static final int PAIR0    = 2;
    public static final int PAIR1    = 3;
    public static final int PULSER   = 4;
    public static final int COSMIC   = 5;
    
    // Store the TI flag type labels.
    private static final int TI_GENERAL     = 0;
    private static final int TI_HIERARCHICAL = 1;
    
    // Track the total run time.
    private long startTime = -1;
    private long endTime = -1;
    
    /**
     * Define the trigger modules. This should be replaced by parsing
     * the DAQ configuration at some point.
     */
    @Override
    public void startOfData() {
        // By default, all triggers and cuts are enabled.
        for(int i = 0; i < 2; i++) {
            // Enable the triggers.
            pairTriggerEnabled[i] = true;
            singlesTriggerEnabled[i] = true;
            
            // Enable the singles cuts.
            for(int j = 0; j < singlesCutsEnabled.length; j++) {
                singlesCutsEnabled[i][j] = true;
            }
            
            // Enable the pair cuts.
            for(int j = 0; j < pairCutsEnabled.length; j++) {
                pairCutsEnabled[i][j] = true;
            }
        }
        
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
                pairsTrigger[0].loadDAQConfiguration(daq.getSSPConfig().getPair1Config());
                pairsTrigger[1].loadDAQConfiguration(daq.getSSPConfig().getPair2Config());
                nsa = daq.getFADCConfig().getNSA();
                nsb = daq.getFADCConfig().getNSB();
                windowWidth = daq.getFADCConfig().getWindowWidth();
                
                // Get the trigger configurations from the DAQ.
                SinglesTriggerConfig[] singles = { daq.getSSPConfig().getSingles1Config(),
                        daq.getSSPConfig().getSingles2Config() };
                PairTriggerConfig[] pairs = { daq.getSSPConfig().getPair1Config(),
                        daq.getSSPConfig().getPair2Config() };
                
                // Update the enabled/disabled statuses.
                for(int i = 0; i < 2; i++) {
                    // Set the trigger enabled status.
                    pairTriggerEnabled[i] = pairs[i].isEnabled();
                    singlesTriggerEnabled[i] = singles[i].isEnabled();
                    
                    // Set the singles cut statuses.
                    singlesCutsEnabled[i][ENERGY_MIN] = singles[i].getEnergyMinCutConfig().isEnabled();
                    singlesCutsEnabled[i][ENERGY_MAX] = singles[i].getEnergyMaxCutConfig().isEnabled();
                    singlesCutsEnabled[i][HIT_COUNT] = singles[i].getHitCountCutConfig().isEnabled();
                    
                    // Set the pair cut statuses.
                    pairCutsEnabled[i][ENERGY_MIN] = pairs[i].getEnergyMinCutConfig().isEnabled();
                    pairCutsEnabled[i][ENERGY_MAX] = pairs[i].getEnergyMaxCutConfig().isEnabled();
                    pairCutsEnabled[i][HIT_COUNT] = pairs[i].getHitCountCutConfig().isEnabled();
                    pairCutsEnabled[i][3 + ENERGY_SUM] = pairs[i].getEnergySumCutConfig().isEnabled();
                    pairCutsEnabled[i][3 + ENERGY_DIFF] = pairs[i].getEnergyDifferenceCutConfig().isEnabled();
                    pairCutsEnabled[i][3 + ENERGY_SLOPE] = pairs[i].getEnergySlopeCutConfig().isEnabled();
                    pairCutsEnabled[i][3 + COPLANARITY] = pairs[i].getCoplanarityCutConfig().isEnabled();
                }
                
                // Print a DAQ configuration settings header.
                System.out.println();
                System.out.println();
                System.out.println("======================================================================");
                System.out.println("=== DAQ Configuration Settings =======================================");
                System.out.println("======================================================================");
                logSettings();
            }
        });
        
        // Print the cluster verification header.
        System.out.println();
        System.out.println();
        System.out.println("======================================================================");
        System.out.println("=== Cluster/Trigger Verification Settings ============================");
        System.out.println("======================================================================");
        
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
        pairsTrigger[0] = new TriggerModule();
        pairsTrigger[0].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW, 0.000);
        pairsTrigger[0].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH, 8.191);
        pairsTrigger[0].setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW, 0);
        pairsTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW, 0.000);
        pairsTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH, 8.191);
        pairsTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH, 8.191);
        pairsTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW, 0.000);
        pairsTrigger[0].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_F, 0.001);
        pairsTrigger[0].setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH, 180);
        pairsTrigger[0].setCutValue(TriggerModule.PAIR_TIME_COINCIDENCE, 8);
        
        // Define the second pairs trigger.
        pairsTrigger[1] = new TriggerModule();
        pairsTrigger[1].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW, 0.000);
        pairsTrigger[1].setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH, 8.191);
        pairsTrigger[1].setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW, 0);
        pairsTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW, 0.000);
        pairsTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH, 8.191);
        pairsTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH, 8.191);
        pairsTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW, 0.000);
        pairsTrigger[1].setCutValue(TriggerModule.PAIR_ENERGY_SLOPE_F, 0.001);
        pairsTrigger[1].setCutValue(TriggerModule.PAIR_COPLANARITY_HIGH, 180);
        pairsTrigger[1].setCutValue(TriggerModule.PAIR_TIME_COINCIDENCE, 8);
        
        // Print the initial settings.
        logSettings();
    }
    
    /**
     * Gets the banks and clusters from the event.
     */
    @Override
    public void process(EventHeader event) {
        // ==========================================================
        // ==== Event Pre-Initialization ============================
        // ==========================================================
        
        // If DAQ settings are to be used, check if they are initialized
        // yet. If not, skip the event.
        if(!ConfigurationManager.isInitialized()) {
            return;
        }
        
        // Print the statistics every so often during a run.
        if(events % statPrintInterval == 0) {
            logStatistics();
        }
        
        // Reset the output buffer.
        OutputLogger.clearLog();
        
        // Track the times.
        if(startTime == -1) { startTime = event.getTimeStamp(); }
        else { endTime = event.getTimeStamp(); }
        
        // Output the clustering diagnostic header.
        OutputLogger.printNewLine(2);
        OutputLogger.println("======================================================================");
        OutputLogger.println("==== Trigger Diagnostics Event Analysis ==============================");
        OutputLogger.println("======================================================================");
        
        
        
        // ==========================================================
        // ==== Obtain SSP and TI Banks =============================
        // ==========================================================
        
        // If there is no bank data, this event can not be analyzed.
        if(!event.hasCollection(GenericObject.class, bankCollectionName)) {
            System.err.println("TriggerDiagnostics :: Skipping event; no bank data found.");
            if(verbose) { OutputLogger.printLog(); }
            return;
        }
        
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
            // correct object format and also extract the active TI bits.
            else if(AbstractIntData.getTag(obj) == TIData.BANK_TAG) {
                // Parse the TI bank object.
                tiBank = new TIData(obj);
                
                // Store which TI bits are active.
                tiFlags = new boolean[6];
                boolean activeBitRead = false;
                if(tiBank.isPulserTrigger()) {
                    activeBitRead = true;
                    tiFlags[PULSER] = true;
                    OutputLogger.println("Trigger type :: Pulser");
                } if(tiBank.isSingle0Trigger()) {
                    activeBitRead = true;
                    tiFlags[SINGLES0] = true;
                    OutputLogger.println("Trigger type :: Singles 0");
                } if(tiBank.isSingle1Trigger()) {
                    activeBitRead = true;
                    tiFlags[SINGLES1] = true;
                    OutputLogger.println("Trigger type :: Singles 1");
                } if(tiBank.isPair0Trigger()) {
                    activeBitRead = true;
                    tiFlags[PAIR0] = true;
                    OutputLogger.println("Trigger type :: Pair 0");
                } if(tiBank.isPair1Trigger()) {
                    activeBitRead = true;
                    tiFlags[PAIR1] = true;
                    OutputLogger.println("Trigger type :: Pair 1");
                } if(tiBank.isCalibTrigger()) {
                    activeBitRead = true;
                    tiFlags[COSMIC] = true;
                    OutputLogger.println("Trigger type :: Cosmic");
                } if(!activeBitRead) {
                    System.err.println("TriggerDiagnostics: Skipping event; no TI trigger bits are active.");
                    if(verbose) { OutputLogger.printLog(); }
                    return;
                }
            }
        }
        
        // Check that all of the required objects are present.
        if(sspBank == null) {
            System.err.println("TriggerDiagnostics: Skipping event; no SSP bank found for this event.");
            if(verbose) { OutputLogger.printLog(); }
            return;
        } if(tiBank == null) {
            System.err.println("TriggerDiagnostics: Skipping event; no TI bank found for this event.");
            if(verbose) { OutputLogger.printLog(); }
            return;
        }
        
        // Output the event number and information.
        OutputLogger.printf("Event Number %d (%d)%n", sspBank.getEventNumber(), event.getEventNumber());
        
        // Get the hardware clusters.
        List<SSPCluster> hardwareClusters = sspBank.getClusters();
        
        
        
        // ==========================================================
        // ==== Obtain Simulated Clusters ===========================
        // ==========================================================
        
        // If the simulated cluster collection does not exist, analysis
        // can not be performed.
        if(!event.hasCollection(Cluster.class, clusterCollectionName)) {
            System.err.println("TriggerDiagnostics: Skipping event; no simulated clusters found.");
            if(verbose) { OutputLogger.printLog(); }
            return;
        }
        
        // Get the simulated clusters.
        List<Cluster> simulatedClusters = event.get(Cluster.class, clusterCollectionName);
        
        
        
        // ==========================================================
        // ==== Obtain Simulated Triggers ===========================
        // ==========================================================
        
        // If the simulated trigger collection does not exist, analysis
        // can not be performed.
        if(!event.hasCollection(SimTriggerData.class, simTriggerCollectionName)) {
            System.err.println("TriggerDiagnostics: Skipping event; no simulated triggers found.");
            if(verbose) { OutputLogger.printLog(); }
            return;
        }
        
        // Get the simulated trigger module.
        List<SimTriggerData> stdList = event.get(SimTriggerData.class, "SimTriggers");
        SimTriggerData triggerData = stdList.get(0);
        
        
        
        // ==========================================================
        // ==== Obtain Hit Collection / Check Noise Level ===========
        // ==========================================================
        
        // If the FADC hit collection does not exist, analysis can
        // not be performed.
        if(!event.hasCollection(CalorimeterHit.class, hitCollectionName)) {
            System.err.println("TriggerDiagnostics: Skipping event; no FADC hits found.");
            if(verbose) { OutputLogger.printLog(); }
            return;
        }
        
        // Get the simulated clusters.
        List<CalorimeterHit> fadcHits = event.get(CalorimeterHit.class, hitCollectionName);
        
        // Check if there are more hits than the noise threshold. If so,
        // the event quality is too low to perform meaningful analysis.
        if(fadcHits.size() >= noiseThreshold) {
            events++;
            noiseEvents++;
            System.err.println("TriggerDiagnostics: Skipping event; noise event detected.");
            if(verbose) { OutputLogger.printLog(); }
            return;
        }
        
        
        
        // ==========================================================
        // ==== Perform Event Verification ==========================
        // ==========================================================
        
        // Increment the number of events processed.
        events++;
        
        // Perform cluster verification.
        ClusterDiagnosticModule clusterVerification = new ClusterDiagnosticModule(fadcHits, hardwareClusters, simulatedClusters,
                nsa, nsb, windowWidth, hitAcceptance, energyAcceptance);
        
        // Create a list in which to store singles triggers.
        List<List<SSPSinglesTrigger>> singlesTriggers = new ArrayList<List<SSPSinglesTrigger>>();
        singlesTriggers.add(new ArrayList<SSPSinglesTrigger>());
        singlesTriggers.add(new ArrayList<SSPSinglesTrigger>());
        
        // Perform the singles trigger verification.
        SinglesTriggerDiagnosticModule[] singlesVerification = new SinglesTriggerDiagnosticModule[2];
        for(int trigNum = 0; trigNum < 2; trigNum++) {
            // Extract only the correct singles triggers for this trigger
            // number from the compiled list of hardware singles triggers.
            for(SSPSinglesTrigger trigger : sspBank.getSinglesTriggers()) {
                if(trigger.getTriggerNumber() == trigNum) { singlesTriggers.get(trigNum).add(trigger); }
            }
            
            // Get the appropriate simulated trigger objects.
            List<SinglesTrigger<Cluster>> simSimTriggers = triggerData.getSimSoftwareClusterTriggers().getSinglesTriggers(trigNum);
            List<SinglesTrigger<SSPCluster>> simHardwareTriggers = triggerData.getSimHardwareClusterTriggers().getSinglesTriggers(trigNum);
            
            // Perform singles trigger verification.
            singlesVerification[trigNum] = new SinglesTriggerDiagnosticModule(simSimTriggers, simHardwareTriggers,
                    singlesTriggers.get(trigNum), "Singles " + trigNum, nsa, nsb, windowWidth);
        }
        
        // Create a list in which to store pair triggers.
        List<List<SSPPairTrigger>> pairTriggers = new ArrayList<List<SSPPairTrigger>>();
        pairTriggers.add(new ArrayList<SSPPairTrigger>());
        pairTriggers.add(new ArrayList<SSPPairTrigger>());
        
        // Perform the singles trigger verification.
        PairTriggerDiagnosticModule[] pairVerification = new PairTriggerDiagnosticModule[2];
        for(int trigNum = 0; trigNum < 2; trigNum++) {
            // Extract only the correct pair triggers for this trigger
            // number from the compiled list of hardware pair triggers.
            for(SSPPairTrigger trigger : sspBank.getPairTriggers()) {
                if(trigger.getTriggerNumber() == trigNum) { pairTriggers.get(trigNum).add(trigger); }
            }
            
            // Get the appropriate simulated trigger objects.
            List<PairTrigger<Cluster[]>> simSimTriggers = triggerData.getSimSoftwareClusterTriggers().getPairTriggers(trigNum);
            List<PairTrigger<SSPCluster[]>> simHardwareTriggers = triggerData.getSimHardwareClusterTriggers().getPairTriggers(trigNum);
            
            // Perform singles trigger verification.
            pairVerification[trigNum] = new PairTriggerDiagnosticModule(simSimTriggers, simHardwareTriggers,
                    pairTriggers.get(trigNum), "Pair " + trigNum, nsa, nsb, windowWidth);
        }
        
        
        
        // ==========================================================
        // ==== Update Statistical Information ======================
        // ==========================================================
        
        // Update general event failure rate.
        if(clusterVerification.clusterFail) { clusterFailEvents++; }
        if(singlesVerification[0].hardwareTriggerFail || singlesVerification[1].hardwareTriggerFail
                || singlesVerification[0].simulatedTriggerFail || singlesVerification[1].simulatedTriggerFail) {
            singlesFailEvents++;
        }
        if(pairVerification[0].hardwareTriggerFail || pairVerification[1].hardwareTriggerFail
                || pairVerification[0].simulatedTriggerFail || pairVerification[1].simulatedTriggerFail) {
            pairFailEvents++;
        }
        
        // Update the TI event types. For the general count, it does
        // not matter how many TI bits are active.
        for(int i = 0; i < 6; i++) {
            if(tiFlags[i]) { tiEvents[TI_GENERAL][i]++; }
        }
        
        // For the hierarchical TI count, only the "highest" order bit
        // is considered.
        if(tiFlags[PAIR0]) { tiEvents[TI_HIERARCHICAL][PAIR0]++; }
        else if(tiFlags[PAIR1]) { tiEvents[TI_HIERARCHICAL][PAIR1]++; }
        else if(tiFlags[SINGLES0]) { tiEvents[TI_HIERARCHICAL][SINGLES0]++; }
        else if(tiFlags[SINGLES1]) { tiEvents[TI_HIERARCHICAL][SINGLES1]++; }
        else if(tiFlags[PULSER]) { tiEvents[TI_HIERARCHICAL][PULSER]++; }
        else if(tiFlags[COSMIC]) { tiEvents[TI_HIERARCHICAL][COSMIC]++; }
        
        // Update the cluster statistical information.
        simClusterCount += clusterVerification.goodSimulatedClusterCount;
        hardwareClusterCount += clusterVerification.hardwareClusterCount;
        matchedClusters += clusterVerification.matchedClusters;
        matchClusterFailPosition += clusterVerification.failedMatchPosition;
        matchClusterFailHitCount += clusterVerification.failedMatchHitCount;
        matchClusterFailEnergy += clusterVerification.failedMatchEnergy;
        matchClusterFailTime += clusterVerification.failedMatchTime;
        
        // Update trigger statistical information.
        for(int trigNum = 0; trigNum < 2; trigNum++) {
            // Store trigger information globally by trigger type.
            // Start with the singles triggers.
            allHardwareTriggerCount[trigNum == 0 ? SINGLES0 : SINGLES1] += singlesVerification[trigNum].hardwareTriggerCount;
            allSimSimTriggerCount[trigNum == 0 ? SINGLES0 : SINGLES1] += singlesVerification[trigNum].simSimTriggerCount;
            allHardwareSimTriggerCount[trigNum == 0 ? SINGLES0 : SINGLES1] += singlesVerification[trigNum].hardwareSimTriggerCount;
            allMatchedSimSimTriggers[trigNum == 0 ? SINGLES0 : SINGLES1] += singlesVerification[trigNum].matchedSimSimTriggers;
            allMatchedHardwareSimTriggers[trigNum == 0 ? SINGLES0 : SINGLES1] += singlesVerification[trigNum].matchedHardwareSimTriggers;
            
            // And repeat for the pair triggers.
            allHardwareTriggerCount[trigNum == 0 ? PAIR0 : PAIR1] += pairVerification[trigNum].hardwareTriggerCount;
            allSimSimTriggerCount[trigNum == 0 ? PAIR0 : PAIR1] += pairVerification[trigNum].simSimTriggerCount;
            allHardwareSimTriggerCount[trigNum == 0 ? PAIR0 : PAIR1] += pairVerification[trigNum].hardwareSimTriggerCount;
            allMatchedSimSimTriggers[trigNum == 0 ? PAIR0 : PAIR1] += pairVerification[trigNum].matchedSimSimTriggers;
            allMatchedHardwareSimTriggers[trigNum == 0 ? PAIR0 : PAIR1] += pairVerification[trigNum].matchedHardwareSimTriggers;
            
            // In addition to globally, trigger information is also
            // stored by TI bit.
            for(int tiBit = 0; tiBit < 6; tiBit++) {
                // Update the singles trigger statistical information.
                tiHardwareTriggerCount[tiBit][trigNum == 0 ? SINGLES0 : SINGLES1] += singlesVerification[trigNum].hardwareTriggerCount;
                tiSimSimTriggerCount[tiBit][trigNum == 0 ? SINGLES0 : SINGLES1] += singlesVerification[trigNum].simSimTriggerCount;
                tiHardwareSimTriggerCount[tiBit][trigNum == 0 ? SINGLES0 : SINGLES1] += singlesVerification[trigNum].hardwareSimTriggerCount;
                tiMatchedSimSimTriggers[tiBit][trigNum == 0 ? SINGLES0 : SINGLES1] += singlesVerification[trigNum].matchedSimSimTriggers;
                tiMatchedHardwareSimTriggers[tiBit][trigNum == 0 ? SINGLES0 : SINGLES1] += singlesVerification[trigNum].matchedHardwareSimTriggers;
                
                // Update the pair trigger statistical information.
                tiHardwareTriggerCount[tiBit][trigNum == 0 ? PAIR0 : PAIR1] += pairVerification[trigNum].hardwareTriggerCount;
                tiSimSimTriggerCount[tiBit][trigNum == 0 ? PAIR0 : PAIR1] += pairVerification[trigNum].simSimTriggerCount;
                tiHardwareSimTriggerCount[tiBit][trigNum == 0 ? PAIR0 : PAIR1] += pairVerification[trigNum].hardwareSimTriggerCount;
                tiMatchedSimSimTriggers[tiBit][trigNum == 0 ? PAIR0 : PAIR1] += pairVerification[trigNum].matchedSimSimTriggers;
                tiMatchedHardwareSimTriggers[tiBit][trigNum == 0 ? PAIR0 : PAIR1] += pairVerification[trigNum].matchedHardwareSimTriggers;
            }
        }
        
        
        
        // ==========================================================
        // ==== Perform Event Write-Out =============================
        // ==========================================================
        
        if(verbose ||(clusterVerification.clusterFail && printClusterFail) ||
                ((singlesVerification[0].hardwareTriggerFail || singlesVerification[1].hardwareTriggerFail) && printSinglesTriggerInternalFail) ||
                ((singlesVerification[0].simulatedTriggerFail || singlesVerification[1].simulatedTriggerFail) && printSinglesTriggerEfficiencyFail) ||
                ((pairVerification[0].hardwareTriggerFail || pairVerification[1].hardwareTriggerFail) && printPairTriggerInternalFail) ||
                ((pairVerification[0].simulatedTriggerFail || pairVerification[1].simulatedTriggerFail) && printPairTriggerEfficiencyFail)) {
            OutputLogger.printLog();
        }
        
        
        
        // ==========================================================
        // ==== Process Local Tracked Variables =====================
        // ==========================================================
        // TODO: Re-implement local plotting.
        /**
        if(localStats.getDuration() > localWindowThreshold) {
            // Write a snapshot of the driver to the event stream.
            List<DiagnosticSnapshot> snapshotList = new ArrayList<DiagnosticSnapshot>(2);
            snapshotList.add(localStats.getSnapshot());
            snapshotList.add(globalStats.getSnapshot());
            
            // Push the snapshot to the data stream.
            event.put(diagnosticCollectionName, snapshotList);
            
            // Store values needed to calculate efficiency.
            int[] matched = {
                    localStats.getClusterStats().getMatches(),
                    localStats.getTriggerStats().getSingles0Stats().getMatchedReconSimulatedTriggers(),
                    localStats.getTriggerStats().getSingles1Stats().getMatchedReconSimulatedTriggers(),
                    localStats.getTriggerStats().getPair0Stats().getMatchedReconSimulatedTriggers(),
                    localStats.getTriggerStats().getPair1Stats().getMatchedReconSimulatedTriggers()
            };
            int[] total = {
                    localStats.getClusterStats().getReconClusterCount(),
                    localStats.getTriggerStats().getSingles0Stats().getReconSimulatedTriggers(),
                    localStats.getTriggerStats().getSingles1Stats().getReconSimulatedTriggers(),
                    localStats.getTriggerStats().getPair0Stats().getReconSimulatedTriggers(),
                    localStats.getTriggerStats().getPair1Stats().getReconSimulatedTriggers()
            };
            
            // Calculate the efficiencies and upper/lower errors.
            double[] efficiency = new double[5];
            for(int i = 0; i < 5; i++) {
                efficiency[i] = 1.0 * matched[i] / total[i];
            }
            
            // Get the time for the current snapshot. This is the total
            // run time before the snapshot plus half of the snapshot.
            long time = globalStats.getDuration() - (localStats.getDuration() / 2);
            
            // Add them to the appropriate cloud plot.
            for(int i = 0; i < 5; i++) { efficiencyTimeHist[i].fill(time, efficiency[i]); }
            
            // Clear the local statistical data.
            localStats.clear();
        }
        **/
    }
    
    /**
     * Prints the total run statistics.
     */
    @Override
    public void endOfData() {
        // Output the statistics.
        logStatistics();
        
        /*
        // Calculate the values needed for the efficiency histogram.
        long totalTime = entryList.get(entryList.size()).time / 1000000000;
        int entries = (int) (totalTime / (localWindowThreshold / 1000000000)) + 1;
        
        // Generate a histogram containing the efficiencies.
        IHistogram1D[] efficiencyHist = new IHistogram1D[5];
        for(int i = 0; i < 5; i++) {
            efficiencyHist[i] = aida.histogram1D("Efficiency " + i, entries, 0.0, totalTime + (localWindowThreshold / 1000000000));
        }
        
        // Input the efficiencies.
        for(EfficiencyEntry entry : entryList) {
            for(int i = 0; i < 5; i++) {
                efficiencyHist[i].fill(entry.time / 1000000000, entry.efficiency[i]);
            }
        }
        */
    }
    
    /**
     * Outputs all of the verification parameters currently in use by
     * the software. A warning will be issued if the values for NSA and
     * NSB, along with the FADC window, preclude clusters from being
     * verified.
     */
    private void logSettings() {
        // Output general settings.
        System.out.println("Cluster Verification Settings");
        System.out.printf("\tHit Threshold          :: %1d hit(s)%n", hitAcceptance);
        System.out.printf("\tEnergy Threshold       :: %5.3f GeV%n",  energyAcceptance);
        System.out.println();
        
        // Output window settings.
        System.out.println("FADC Timing Window Settings");
        System.out.printf("\tNSB                    :: %3d ns%n", nsb);
        System.out.printf("\tNSA                    :: %3d ns%n", nsa);
        System.out.printf("\tFADC Window            :: %3d ns%n", windowWidth);
        
        // Calculate the valid clustering window.
        int start = nsb;
        int end = windowWidth - nsa;
        System.out.printf("\tValid Cluster Window   :: [ %3d ns, %3d ns ]%n", start, end);
        System.out.println();
        
        // Output the singles trigger settings.
        for(int i = 0; i < 2; i++) {
            // Print the settings.
            System.out.printf("Singles Trigger %d Settings%23s[%5b]%n", (i + 1), "", singlesTriggerEnabled[i]);
            System.out.printf("\tCluster Energy Low     :: %.3f GeV      [%5b]%n",
                    singlesTrigger[i].getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW), singlesCutsEnabled[i][0]);
            System.out.printf("\tCluster Energy High    :: %.3f GeV      [%5b]%n",
                    singlesTrigger[i].getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH), singlesCutsEnabled[i][1]);
            System.out.printf("\tCluster Hit Count      :: %.0f hit(s)       [%5b]%n",
                    singlesTrigger[i].getCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW), singlesCutsEnabled[i][2]);
            System.out.println();
        }
        
        // Output the pair trigger settings.
        for(int i = 0; i < 2; i++) {
            System.out.printf("Pairs Trigger %d Settings%25s[%5b]%n", (i + 1), "", pairTriggerEnabled[i]);
            System.out.printf("\tCluster Energy Low     :: %.3f GeV      [%5b]%n",
                    pairsTrigger[i].getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW), pairCutsEnabled[i][0]);
            System.out.printf("\tCluster Energy High    :: %.3f GeV      [%5b]%n",
                    pairsTrigger[i].getCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH), pairCutsEnabled[i][1]);
            System.out.printf("\tCluster Hit Count      :: %.0f hit(s)       [%5b]%n",
                    pairsTrigger[i].getCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW), pairCutsEnabled[i][2]);
            System.out.printf("\tPair Energy Sum Low    :: %.3f GeV      [%5b]%n",
                    pairsTrigger[i].getCutValue(TriggerModule.PAIR_ENERGY_SUM_LOW), pairCutsEnabled[i][3]);
            System.out.printf("\tPair Energy Sum High   :: %.3f GeV      [%5b]%n",
                    pairsTrigger[i].getCutValue(TriggerModule.PAIR_ENERGY_SUM_HIGH), pairCutsEnabled[i][3]);
            System.out.printf("\tPair Energy Difference :: %.3f GeV      [%5b]%n",
                    pairsTrigger[i].getCutValue(TriggerModule.PAIR_ENERGY_DIFFERENCE_HIGH), pairCutsEnabled[i][4]);
            System.out.printf("\tPair Energy Slope      :: %.3f GeV      [%5b]%n",
                    pairsTrigger[i].getCutValue(TriggerModule.PAIR_ENERGY_SLOPE_LOW), pairCutsEnabled[i][5]);
            System.out.printf("\tPair Energy Slope F    :: %.4f GeV / mm%n",
                    pairsTrigger[i].getCutValue(TriggerModule.PAIR_ENERGY_SLOPE_F));
            System.out.printf("\tPair Coplanarity       :: %3.0f Degrees    [%5b]%n",
                    pairsTrigger[i].getCutValue(TriggerModule.PAIR_COPLANARITY_HIGH), pairCutsEnabled[i][6]);
            System.out.printf("\tPair Time Coincidence  :: %2.0f ns          [%5b]%n",
                    pairsTrigger[i].getCutValue(TriggerModule.PAIR_TIME_COINCIDENCE), true);
            System.out.println();
        }
    }
    
    /**
     * Summarizes the global run statistics in a log to the terminal.
     */
    private void logStatistics() {
        // Print the cluster/trigger verification header.
        System.out.println();
        System.out.println();
        System.out.println("======================================================================");
        System.out.println("=== Cluster/Trigger Verification Results =============================");
        System.out.println("======================================================================");
        
        // Print the general event failure rate.
        int headSpaces = getPrintSpaces(events);
        System.out.println("General Event Statistics:");
        System.out.printf("\tEvent Start Time      :: %.3f s%n", (startTime / Math.pow(10, 9)));
        System.out.printf("\tEvent End Time        :: %.3f%n", (endTime / Math.pow(10, 9)));
        System.out.printf("\tEvent Run Time        :: %.3f%n", ((endTime - startTime) / Math.pow(10, 9)));
        System.out.printf("\tNoise Events          :: %" + headSpaces + "d / %" + headSpaces + "d (%7.3f%%)%n",
                noiseEvents, events, (100.0 * noiseEvents / events));
        System.out.printf("\tCluster Events Failed :: %" + headSpaces + "d / %" + headSpaces + "d (%7.3f%%)%n",
                clusterFailEvents, events, (100.0 * clusterFailEvents / events));
        System.out.printf("\tSingles Events Failed :: %" + headSpaces + "d / %" + headSpaces + "d (%7.3f%%)%n",
                singlesFailEvents, events, (100.0 * singlesFailEvents / events));
        System.out.printf("\tPair Events Failed    :: %" + headSpaces + "d / %" + headSpaces + "d (%7.3f%%)%n",
                pairFailEvents, events, (100.0 * pairFailEvents / events));
        
        // Print out how many events reported a given TI type, both in
        // total and hierarchically.
        System.out.println();
        System.out.println("Event Triggering Type Verification:");
        System.out.printf("\t%15s\t%15s\t%15s%n", "Trigger", "Total", "Hierarchical");
        System.out.printf("\t%15s\t%15s\t%15s%n", "Pulser",    tiEvents[TI_GENERAL][PULSER],   tiEvents[TI_HIERARCHICAL][PULSER]);
        System.out.printf("\t%15s\t%15s\t%15s%n", "Cosmic",    tiEvents[TI_GENERAL][COSMIC],   tiEvents[TI_HIERARCHICAL][COSMIC]);
        System.out.printf("\t%15s\t%15s\t%15s%n", "Singles 0", tiEvents[TI_GENERAL][SINGLES0], tiEvents[TI_HIERARCHICAL][SINGLES0]);
        System.out.printf("\t%15s\t%15s\t%15s%n", "Singles 1", tiEvents[TI_GENERAL][SINGLES1], tiEvents[TI_HIERARCHICAL][SINGLES1]);
        System.out.printf("\t%15s\t%15s\t%15s%n", "Pair 0",    tiEvents[TI_GENERAL][PAIR0],    tiEvents[TI_HIERARCHICAL][PAIR0]);
        System.out.printf("\t%15s\t%15s\t%15s%n", "Pair 1",    tiEvents[TI_GENERAL][PAIR1],    tiEvents[TI_HIERARCHICAL][PAIR1]);
        
        // Print the cluster verification data.
        System.out.println();
        System.out.println("Cluster Verification:");
        System.out.printf("\tSimulated Clusters    :: %d%n", simClusterCount);
        System.out.printf("\tHardware Clusters     :: %d%n", hardwareClusterCount);
        System.out.printf("\tClusters Matched      :: %d%n", matchedClusters);
        System.out.printf("\tFailed (Position)     :: %d%n", matchClusterFailPosition);
        System.out.printf("\tFailed (Time)         :: %d%n", matchClusterFailTime);
        System.out.printf("\tFailed (Energy)       :: %d%n", matchClusterFailEnergy);
        System.out.printf("\tFailed (Hit Count)    :: %d%n", matchClusterFailHitCount);
        if(simClusterCount == 0) {
            System.out.printf("\tCluster Efficiency    :: N/A%n");
        } else {
            System.out.printf("\tCluster Efficiency    :: %7.3f%%%n",
                    100.0 * matchedClusters / simClusterCount);
        }
        
        // Print the trigger verification data.
        int[][] trigRef = {
                { SINGLES0, SINGLES1 },
                { PAIR0, PAIR1 }
        };
        for(int triggerType = 0; triggerType < 2; triggerType++) {
            // Get the basic trigger data.
            int sspSimTriggers = allHardwareSimTriggerCount[trigRef[triggerType][0]] + allHardwareSimTriggerCount[trigRef[triggerType][1]];
            int reconSimTriggers = allSimSimTriggerCount[trigRef[triggerType][0]] + allSimSimTriggerCount[trigRef[triggerType][1]];
            int sspReportedTriggers  = allHardwareTriggerCount[trigRef[triggerType][0]] + allHardwareTriggerCount[trigRef[triggerType][1]];
            int sspMatchedTriggers   = allMatchedHardwareSimTriggers[trigRef[triggerType][0]] + allMatchedHardwareSimTriggers[trigRef[triggerType][1]];
            int reconMatchedTriggers = allMatchedSimSimTriggers[trigRef[triggerType][0]] + allMatchedSimSimTriggers[trigRef[triggerType][1]];
            
            // Print the basic trigger statistics.
            int spaces = getPrintSpaces(sspSimTriggers, reconSimTriggers, sspReportedTriggers);
            System.out.println();
            if(triggerType == 0) { System.out.println("Singles Trigger Verification:"); }
            else { System.out.println("Pair Trigger Verification:"); }
            System.out.printf("\tHardware Cluster Sim Triggers  :: %" + spaces + "d%n", sspSimTriggers);
            System.out.printf("\tSimulated Cluster Sim Triggers :: %" + spaces + "d%n", reconSimTriggers);
            System.out.printf("\tHardware Reported Triggers     :: %" + spaces + "d%n", sspReportedTriggers);
            
            System.out.printf("\tInternal Efficiency        :: %" + spaces + "d / %" + spaces + "d ", sspMatchedTriggers, sspSimTriggers);
            if(sspSimTriggers == 0) { System.out.printf("(N/A)%n"); }
            else { System.out.printf("(%7.3f%%)%n", (100.0 * sspMatchedTriggers / sspSimTriggers)); }
            
            System.out.printf("\tTrigger Efficiency         :: %" + spaces + "d / %" + spaces + "d ", reconMatchedTriggers, reconSimTriggers);
            if(reconSimTriggers == 0) { System.out.printf("(N/A)%n"); }
            else { System.out.printf("(%7.3f%%)%n" , (100.0 * reconMatchedTriggers / reconSimTriggers)); }
        }
        
        // Print out the trigger efficiency table.
        System.out.println();
        // TODO: Re-implement efficiency table.
        //globalStats.getTriggerStats().printEfficiencyTable();
    }
    
    /**
     * Determines the number of spaces needed to render the longest of
     * a series of integers as a string.
     * @param vals - The series of integers.
     * @return Returns the number of spaces needed to render the longest
     * integer as a base-10 string.
     */
    private static final int getPrintSpaces(int... vals) {
        // Track the largest value.
        int largest = 0;
        
        // Iterate over the arguments and find the largest.
        for(int val : vals) {
            // Get the length of the string.
            int length = TriggerDiagnosticUtil.getDigits(val);
            
            // If it is larger, track it.
            if(length > largest) { largest = length; }
        }
        
        // Return the longer one.
        return largest;
    }

    public void setPrintResultsEveryNEvents(int n) {
        statPrintInterval = n;
    }
    
    public void setPrintOnClusterFailure(boolean state) {
        printClusterFail = state;
    }
    
    public void setPrintOnSinglesEfficiencyFailure(boolean state) {
        printSinglesTriggerEfficiencyFail = state;
    }
    
    public void setPrintOnSinglesSSPFailure(boolean state) {
        printSinglesTriggerInternalFail = state;
    }
    
    public void setPrintOnPairEfficiencyFailure(boolean state) {
        printPairTriggerEfficiencyFail = state;
    }
    
    public void setPrintOnPairSSPFailure(boolean state) {
        printPairTriggerInternalFail = state;
    }
    
    public void setVerbose(boolean state) {
        verbose = state;
    }
    
    public void setHitCollectionName(String hitCollectionName) {
        this.hitCollectionName = hitCollectionName;
    }
    
    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    public void setBankCollectionName(String bankCollectionName) {
        this.bankCollectionName = bankCollectionName;
    }
    
    public void setNoiseThresholdCount(int noiseHits) {
        noiseThreshold = noiseHits;
    }
    
    public void setHitAcceptanceWindow(int window) {
        hitAcceptance = window;
    }
    
    public void setEnergyAcceptanceWindow(double window) {
        energyAcceptance = window;
    }
    
    public void setLocalWindowThresholdMilliseconds(int localWindowThreshold) {
        this.localWindowThreshold = localWindowThreshold;
    }
}
package org.hps.analysis.trigger;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hps.analysis.trigger.data.ClusterMatchedPair;
import org.hps.analysis.trigger.data.DetailedClusterEvent;
import org.hps.analysis.trigger.data.DiagnosticSnapshot;
import org.hps.analysis.trigger.data.RunDiagStats;
import org.hps.analysis.trigger.data.TriggerDiagStats;
import org.hps.analysis.trigger.data.TriggerEvent;
import org.hps.analysis.trigger.event.TriggerPlotsModule;
import org.hps.analysis.trigger.util.OutputLogger;
import org.hps.analysis.trigger.util.Pair;
import org.hps.analysis.trigger.util.PairTrigger;
import org.hps.analysis.trigger.util.SinglesTrigger;
import org.hps.analysis.trigger.util.Trigger;
import org.hps.analysis.trigger.util.TriggerDiagnosticUtil;
import org.hps.recon.ecal.daqconfig.ConfigurationManager;
import org.hps.recon.ecal.daqconfig.DAQConfig;
import org.hps.recon.ecal.daqconfig.PairTriggerConfig;
import org.hps.recon.ecal.daqconfig.SinglesTriggerConfig;
import org.hps.recon.ecal.triggerbank.AbstractIntData;
import org.hps.recon.ecal.triggerbank.SSPCluster;
import org.hps.recon.ecal.triggerbank.SSPData;
import org.hps.recon.ecal.triggerbank.SSPNumberedTrigger;
import org.hps.recon.ecal.triggerbank.SSPPairTrigger;
import org.hps.recon.ecal.triggerbank.SSPSinglesTrigger;
import org.hps.recon.ecal.triggerbank.SSPTrigger;
import org.hps.recon.ecal.triggerbank.TIData;
import org.hps.recon.ecal.triggerbank.TriggerModule;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class TriggerDiagnosticDriver extends Driver {
	// Store the LCIO collection names for the needed objects.
	private String hitCollectionName = "EcalCalHits";
	private String bankCollectionName = "TriggerBank";
	private String clusterCollectionName = "EcalClusters";
	private String diagnosticCollectionName = "DiagnosticSnapshot";
	
	// Store the lists of parsed objects.
	private TIData tiBank;
	private SSPData sspBank;
	private List<Cluster> reconClusters = new ArrayList<Cluster>();
	private List<SSPCluster> sspClusters;
	private List<List<PairTrigger<Cluster[]>>> reconPairsTriggers = new ArrayList<List<PairTrigger<Cluster[]>>>(2);
	private List<List<PairTrigger<SSPCluster[]>>> sspPairsTriggers = new ArrayList<List<PairTrigger<SSPCluster[]>>>(2);
	private List<List<SinglesTrigger<Cluster>>> reconSinglesTriggers = new ArrayList<List<SinglesTrigger<Cluster>>>(2);
	private List<List<SinglesTrigger<SSPCluster>>> sspSinglesTriggers = new ArrayList<List<SinglesTrigger<SSPCluster>>>(2);
	
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
	private boolean readDAQConfig = false;
	private int localWindowThreshold = 1000000000;
	private boolean performClusterVerification = true;
	private boolean performSinglesTriggerVerification = true;
	private boolean performPairTriggerVerification = true;
	private boolean enforceTimeCompliance = false;
	
	// Efficiency tracking variables.
	private RunDiagStats localStats = new RunDiagStats();
	private RunDiagStats globalStats = new RunDiagStats();
    
    // Verbose settings.
    private boolean clusterFail = false;
    private boolean singlesEfficiencyFail = false;
    private boolean singlesInternalFail = false;
    private boolean pairEfficiencyFail = false;
    private boolean pairInternalFail = false;
    private boolean verbose = false;
    private boolean printClusterFail = true;
    private boolean printSinglesTriggerEfficiencyFail = true;
    private boolean printSinglesTriggerInternalFail = true;
    private boolean printPairTriggerEfficiencyFail = true;
    private boolean printPairTriggerInternalFail = true;
    private int     statPrintInterval = 100000;

    // Cut index arrays for trigger verification.
	private static final int ENERGY_MIN   = TriggerDiagnosticUtil.SINGLES_ENERGY_MIN;
	private static final int ENERGY_MAX   = TriggerDiagnosticUtil.SINGLES_ENERGY_MAX;
	private static final int HIT_COUNT    = TriggerDiagnosticUtil.SINGLES_HIT_COUNT;
	private static final int ENERGY_SUM   = TriggerDiagnosticUtil.PAIR_ENERGY_SUM;
	private static final int ENERGY_DIFF  = TriggerDiagnosticUtil.PAIR_ENERGY_DIFF;
	private static final int ENERGY_SLOPE = TriggerDiagnosticUtil.PAIR_ENERGY_SLOPE;
	private static final int COPLANARITY  = TriggerDiagnosticUtil.PAIR_COPLANARITY;
    
	// Track the total run time.
	private long startTime = -1;
	private long endTime = -1;
	
	// Cut names for logging.
	private static final String[][] cutNames = {
			{ "E_min", "E_max", "hit count", "null" },
			{ "E_sum", "E_diff", "E_slope", "coplanar" }
	};
	
	// Temporary AIDA Plots
	private TriggerPlotsModule globalTriggerPlots = new TriggerPlotsModule(0, 0);
	private static final int RECON   = 0;
	private static final int SSP     = 1;
	private static final int ALL     = 0;
	private static final int MATCHED = 1;
	private static final int FAILED  = 2;
	private AIDA aida = AIDA.defaultInstance();
	private IHistogram1D[][] clusterHitPlot = {
			{
				aida.histogram1D("cluster/Recon Cluster Hit Count (All)",     9, 0.5, 9.5),
				aida.histogram1D("cluster/Recon Cluster Hit Count (Matched)", 9, 0.5, 9.5),
				aida.histogram1D("cluster/Recon Cluster Hit Count (Failed)",  9, 0.5, 9.5)
			},
			{
				aida.histogram1D("cluster/SSP Cluster Hit Count (All)",     9, 0.5, 9.5),
				aida.histogram1D("cluster/SSP Cluster Hit Count (Matched)", 9, 0.5, 9.5),
				aida.histogram1D("cluster/SSP Cluster Hit Count (Failed)",  9, 0.5, 9.5)
			}
	};
	private IHistogram1D[][] clusterEnergyPlot = {
			{
				aida.histogram1D("cluster/Recon Cluster Energy (All)",     300, 0.0, 3.0),
				aida.histogram1D("cluster/Recon Cluster Energy (Matched)", 300, 0.0, 3.0),
				aida.histogram1D("cluster/Recon Cluster Energy (Failed)",  300, 0.0, 3.0)
			},
			{
				aida.histogram1D("cluster/SSP Cluster Energy (All)",     300, 0.0, 3.0),
				aida.histogram1D("cluster/SSP Cluster Energy (Matched)", 300, 0.0, 3.0),
				aida.histogram1D("cluster/SSP Cluster Energy (Failed)",  300, 0.0, 3.0)
			}
	};
	private IHistogram1D[][] clusterTimePlot = {
			{
				aida.histogram1D("cluster/Recon Cluster Time (All)",     115, 0, 460),
				aida.histogram1D("cluster/Recon Cluster Time (Matched)", 115, 0, 460),
				aida.histogram1D("cluster/Recon Cluster Time (Failed)",  115, 0, 460)
			},
			{
				aida.histogram1D("cluster/SSP Cluster Time (All)",     115, 0, 460),
				aida.histogram1D("cluster/SSP Cluster Time (Matched)", 115, 0, 460),
				aida.histogram1D("cluster/SSP Cluster Time (Failed)",  115, 0, 460)
			}
	};
	private IHistogram2D[][] clusterPositionPlot = {
			{
				aida.histogram2D("cluster/Recon Cluster Position (All)",     47, -23.5, 23.5, 11, -5.5, 5.5),
				aida.histogram2D("cluster/Recon Cluster Position (Matched)", 47, -23.5, 23.5, 11, -5.5, 5.5),
				aida.histogram2D("cluster/Recon Cluster Position (Failed)",  47, -23.5, 23.5, 11, -5.5, 5.5)
			},
			{
				aida.histogram2D("cluster/SSP Cluster Position (All)",     47, -23.5, 23.5, 11, -5.5, 5.5),
				aida.histogram2D("cluster/SSP Cluster Position (Matched)", 47, -23.5, 23.5, 11, -5.5, 5.5),
				aida.histogram2D("cluster/SSP Cluster Position (Failed)",  47, -23.5, 23.5, 11, -5.5, 5.5)
			}
	};
	private IHistogram2D[] energyhitDiffPlot = {
		aida.histogram2D("cluster/Recon-SSP Energy-Hit Difference (All)",     21, -0.010, 0.010, 6, -3, 3),
		aida.histogram2D("cluster/Recon-SSP Energy-Hit Difference (Matched)", 21, -0.010, 0.010, 6, -3, 3),
		aida.histogram2D("cluster/Recon-SSP Energy-Hit Difference (Failed)",  21, -0.010, 0.010, 6, -3, 3)
	};
	
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
		if(readDAQConfig) {
			ConfigurationManager.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					// Get the DAQ configuration.
					DAQConfig daq = ConfigurationManager.getInstance();
					
					// Update the plotting energy slope values.
					globalTriggerPlots.setEnergySlopeParamF(0, daq.getSSPConfig().getPair1Config().getEnergySlopeCutConfig().getParameterF());
					globalTriggerPlots.setEnergySlopeParamF(1, daq.getSSPConfig().getPair2Config().getEnergySlopeCutConfig().getParameterF());
					
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
		}
		
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
		
		// Instantiate the triggers lists.
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			reconPairsTriggers.add(new ArrayList<PairTrigger<Cluster[]>>());
			sspPairsTriggers.add(new ArrayList<PairTrigger<SSPCluster[]>>());
			reconSinglesTriggers.add(new ArrayList<SinglesTrigger<Cluster>>());
			sspSinglesTriggers.add(new ArrayList<SinglesTrigger<SSPCluster>>());
		}
		
		// Print the initial settings.
		logSettings();
	}
	
	/**
	 * Prints the total run statistics.
	 */
	@Override
	public void endOfData() { logStatistics(); }
	
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
		if(readDAQConfig) {
			if(!ConfigurationManager.isInitialized()) {
				return;
			}
		}
		
		// Increment the total event count.
		localStats.sawEvent(event.getTimeStamp());
		globalStats.sawEvent(event.getTimeStamp());
		
		// Print the statistics every so often during a run.
		if(globalStats.getEventCount() % statPrintInterval == 0) {
			logStatistics();
		}
		
		// Reset the output buffer and print flags.
		clusterFail = false;
		singlesInternalFail = false;
		singlesEfficiencyFail = false;
		pairInternalFail = false;
		pairEfficiencyFail = false;
		OutputLogger.clearLog();
		
		// Track the times.
		if(startTime == -1) { startTime = event.getTimeStamp(); }
		else { endTime = event.getTimeStamp(); }
		
		
		
		// ==========================================================
		// ==== Output GTP Information ==============================
		// ==========================================================
		
        // Print the verification header.
		OutputLogger.printNewLine(2);
		OutputLogger.println("======================================================================");
		OutputLogger.println("==== FADC/GTP Readout ================================================");
		OutputLogger.println("======================================================================");
		
		OutputLogger.println("FADC Hits:");
		for(CalorimeterHit hit : event.get(CalorimeterHit.class, "EcalCalHits")) {
			int ix = hit.getIdentifierFieldValue("ix");
			int iy = hit.getIdentifierFieldValue("iy");
			OutputLogger.printf("\tHit at (%3d, %3d) with %7.3f GeV at time %3.0f ns%n", ix, iy, hit.getCorrectedEnergy(), hit.getTime());
		}
		OutputLogger.printNewLine(2);
		OutputLogger.println("GTP Clusters:");
		for(Cluster cluster : event.get(Cluster.class, clusterCollectionName)) {
			OutputLogger.printf("\t%s%n", TriggerDiagnosticUtil.clusterToString(cluster));
			for(CalorimeterHit hit : cluster.getCalorimeterHits()) {
				int ix = hit.getIdentifierFieldValue("ix");
				int iy = hit.getIdentifierFieldValue("iy");
				OutputLogger.printf("\t\t> (%3d, %3d) :: %7.3f GeV%n", ix, iy, hit.getCorrectedEnergy());
			}
		}
		
		
		
		// ==========================================================
		// ==== Initialize the Event ================================
		// ==========================================================
		
        // Print the verification header.
		OutputLogger.printNewLine(2);
		OutputLogger.println("======================================================================");
		OutputLogger.println("==== Cluster/Trigger Verification ====================================");
		OutputLogger.println("======================================================================");
		
		
		
		// ==========================================================
		// ==== Obtain SSP and TI Banks =============================
		// ==========================================================
		
		// Output the event number and information.
		OutputLogger.printf("Event Number %d (%d)%n", globalStats.getEventCount(), event.getEventNumber());
		
		// Get the SSP clusters.
		if(event.hasCollection(GenericObject.class, bankCollectionName)) {
			// Get the bank list.
			List<GenericObject> bankList = event.get(GenericObject.class, bankCollectionName);
			
			// Search through the banks and get the SSP and TI banks.
			for(GenericObject obj : bankList) {
				// If this is an SSP bank, parse it.
				if(AbstractIntData.getTag(obj) == SSPData.BANK_TAG) {
					sspBank = new SSPData(obj);
				}
				
				// Otherwise, if this is a TI bank, parse it.
				else if(AbstractIntData.getTag(obj) == TIData.BANK_TAG) {
					tiBank = new TIData(obj);
					
					tiFlags = new boolean[6];
					if(tiBank.isPulserTrigger()) {
						OutputLogger.println("Trigger type :: Pulser");
						tiFlags[TriggerDiagStats.PULSER] = true;
					} else if(tiBank.isSingle0Trigger()) {
						OutputLogger.println("Trigger type :: Singles 1");
						tiFlags[TriggerDiagStats.SINGLES0] = true;
					} else if(tiBank.isSingle1Trigger()) {
						OutputLogger.println("Trigger type :: Singles 2");
						tiFlags[TriggerDiagStats.SINGLES1] = true;
					} else if(tiBank.isPair0Trigger()) {
						OutputLogger.println("Trigger type :: Pair 1");
						tiFlags[TriggerDiagStats.PAIR0] = true;
					} else if(tiBank.isPair1Trigger()) {
						OutputLogger.println("Trigger type :: Pair 2");
						tiFlags[TriggerDiagStats.PAIR1] = true;
					} else if(tiBank.isCalibTrigger()) {
						OutputLogger.println("Trigger type :: Cosmic");
						tiFlags[TriggerDiagStats.COSMIC] = true;
					} else {
						System.err.println("TriggerDiagnosticDriver: Skipping event; no TI trigger source found.");
						return;
					}
					
					// Pass the TI triggers to the run statistical data
					// manager object.
					localStats.getTriggerStats().sawTITriggers(tiFlags);
					globalStats.getTriggerStats().sawTITriggers(tiFlags);
				}
			}
			
			// If there is an SSP bank, get the list of SSP clusters.
			if(sspBank != null) {
				sspClusters = sspBank.getClusters();
				if(sspClusters.size() == 1) {
					OutputLogger.println("1 SSP cluster found.");
				} else {
					OutputLogger.printf("%d SSP clusters found.%n", sspClusters.size());
				}
			}
		}
		
		// Make sure that both an SSP bank and a TI bank were found.
		if(tiBank == null || sspBank == null) {
			System.err.println("TriggerDiagnosticDriver :: SEVERE WARNING :: TI bank or SSP bank missing from event!");
			return;
		}
		
		
		
		// ==========================================================
		// ==== Establish Event Integrity ===========================
		// ==========================================================
		
		// Check that all of the required objects are present.
		if(sspBank == null) {
			OutputLogger.println("No SSP bank found for this event. No verification will be performed.");
			if(verbose) { OutputLogger.printLog(); }
			return;
		} if(tiBank == null) {
			OutputLogger.println("No TI bank found for this event. No verification will be performed.");
			if(verbose) { OutputLogger.printLog(); }
			return;
		}
		
		
		
		// ==========================================================
		// ==== Check the Noise Level ===============================
		// ==========================================================
		
		// Check if there are hits.
		if(event.hasCollection(CalorimeterHit.class, hitCollectionName)) {
			// Check if there are more hits than the noise threshold.
			if(event.get(CalorimeterHit.class, hitCollectionName).size() >= noiseThreshold) {
				localStats.sawNoiseEvent();
				globalStats.sawNoiseEvent();
				OutputLogger.println("Noise event detected. Skipping event...");
				if(verbose) { OutputLogger.printLog(); }
				return;
			}
		}
        
        
        
		// ==========================================================
		// ==== Obtain Reconstructed Clusters =======================
		// ==========================================================
		
		// Clear the list of triggers from previous events.
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			sspSinglesTriggers.get(triggerNum).clear();
			reconSinglesTriggers.get(triggerNum).clear();
			sspPairsTriggers.get(triggerNum).clear();
			reconPairsTriggers.get(triggerNum).clear();
		}
		
		// Get the reconstructed clusters.
		if(event.hasCollection(Cluster.class, clusterCollectionName)) {
			// Get the reconstructed clusters.
			List<Cluster> allClusters = event.get(Cluster.class, clusterCollectionName);
			
			// Keep only the clusters that can be verified.
			OutputLogger.println();
			OutputLogger.println("Process cluster for verifiability:");
			reconClusters.clear();
			for(Cluster reconCluster : allClusters) {
				// Check that the cluster is within the safe region of the
				// FADC readout window. If it is not, it will likely have
				// inaccurate energy or hit values and may not produce the
				// expected results.
				OutputLogger.printf("\t%s", TriggerDiagnosticUtil.clusterToString(reconCluster));
				if(isVerifiable(reconCluster)) {
					reconClusters.add(reconCluster);
					OutputLogger.println(" [  verifiable  ]");
				} else { OutputLogger.println(" [ unverifiable ]"); }
			}
			
			// Output the number of verifiable clusters found.
			if(reconClusters.size() == 1) { OutputLogger.println("1 verifiable reconstructed cluster found."); }
			else { OutputLogger.printf("%d verifiable reconstructed clusters found.%n", reconClusters.size()); }
			
			// Output the number of unverifiable clusters found.
			int unverifiableClusters = allClusters.size() - reconClusters.size();
			if(unverifiableClusters == 1) { OutputLogger.println("1 unverifiable reconstructed cluster found."); }
			else { OutputLogger.printf("%d unverifiable reconstructed clusters found.%n", unverifiableClusters); }
		} else {
			reconClusters = new ArrayList<Cluster>(0);
			OutputLogger.printf("No reconstructed clusters were found for collection \"%s\" in this event.%n", clusterCollectionName);
		}
		
		
		
		// ==========================================================
		// ==== Perform Event Verification ==========================
		// ==========================================================
		
		// Perform the cluster verification step.
		if(performClusterVerification) { clusterVerification(); }
		
		// Construct lists of triggers for the SSP clusters and the
		// reconstructed clusters.
		if(performSinglesTriggerVerification) {
			constructSinglesTriggers();
			singlesTriggerVerification();
		}
		if(performPairTriggerVerification) {
			constructPairTriggers();
			pairTriggerVerification();
		}
		
		// Track how many events failed due to each type of verification.
		if(clusterFail) {
			localStats.failedClusterEvent();
			globalStats.failedClusterEvent();
		} if(pairInternalFail || pairEfficiencyFail) {
			localStats.failedPairEvent();
			globalStats.failedPairEvent();
		} if(singlesInternalFail || singlesEfficiencyFail) {
			localStats.failedSinglesEvent();
			globalStats.failedSinglesEvent();
		}
		
		
		
		// ==========================================================
		// ==== Perform Event Write-Out =============================
		// ==========================================================
		
		if(verbose ||(clusterFail && printClusterFail) ||
				(singlesInternalFail && printSinglesTriggerInternalFail) ||
				(singlesEfficiencyFail && printSinglesTriggerEfficiencyFail) ||
				(pairInternalFail && printPairTriggerInternalFail) ||
				(pairEfficiencyFail && printPairTriggerEfficiencyFail)) {
			OutputLogger.printLog();
		}	
		
		
				
		// ==========================================================
		// ==== Process Local Tracked Variables =====================
		// ==========================================================
		if(localStats.getDuration() > localWindowThreshold) {
			// Write a snapshot of the driver to the event stream.
			List<DiagnosticSnapshot> snapshotList = new ArrayList<DiagnosticSnapshot>(2);
			snapshotList.add(localStats.getSnapshot());
			snapshotList.add(globalStats.getSnapshot());
			
			// Push the snapshot to the data stream.
			event.put(diagnosticCollectionName, snapshotList);
			
			// Clear the local statistical data.
			localStats.clear();
		}
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
	
	public void setEnforceStrictTimeCompliance(boolean state) {
		enforceTimeCompliance = state;
	}
	
	public void setReadDAQConfig(boolean state) {
		readDAQConfig = state;
	}
	
	public void setLocalWindowThresholdMilliseconds(int localWindowThreshold) {
	    this.localWindowThreshold = localWindowThreshold;
	}
	
	/**
	 * Attempts to match all reconstructed clusters that are safely
	 * within the integration window with clusters reported by the SSP.
	 * Method also tracks the ratio of valid reconstructed clusters to
	 * matches found.<br/>
	 * <br/>
	 * Note that unmatched SSP clusters are ignored. Since these may
	 * or may not correspond to reconstructed clusters that occur in
	 * the forbidden time region, it is impossible to say whether or
	 * not these legitimately failed to match or not.
	 */
	private void clusterVerification() {
		// ==========================================================
		// ==== Initialize Cluster Verification =====================
		// ==========================================================
		
		// Print the cluster verification header.
		OutputLogger.printNewLine(2);
		OutputLogger.println("======================================================================");
		OutputLogger.println("=== Cluster Verification =============================================");
		OutputLogger.println("======================================================================");
		
		
		
		// ==========================================================
		// ==== Perform Cluster Matching ============================
		// ==========================================================
		
		// Track the number of cluster pairs that were matched and that
		// failed by failure type.
		DetailedClusterEvent event;
		
		if(enforceTimeCompliance) {
			event = matchClustersTimeCompliant(reconClusters, sspClusters, energyAcceptance, hitAcceptance);
		} else {
			event = matchClusters(reconClusters, sspClusters, energyAcceptance, hitAcceptance);
		}
		
		// Add the event results to the global results.
		localStats.getClusterStats().addEvent(event);
		globalStats.getClusterStats().addEvent(event);
		localStats.getClusterStats().sawSSPClusters(sspClusters.size());
		globalStats.getClusterStats().sawSSPClusters(sspClusters.size());
		localStats.getClusterStats().sawReconClusters(reconClusters.size());
		globalStats.getClusterStats().sawReconClusters(reconClusters.size());
		
		
		
		// ==========================================================
		// ==== Output Event Summary ================================
		// ==========================================================
		
		// Print the valid reconstructed clusters and populate their
		// distribution graphs.
		OutputLogger.println();
		OutputLogger.println("Verified Reconstructed Clusters:");
		if(!reconClusters.isEmpty()) {
			for(Cluster reconCluster : reconClusters) {
				OutputLogger.printf("\t%s%n", TriggerDiagnosticUtil.clusterToString(reconCluster));
			}
		} else { OutputLogger.println("\tNone"); }
		
		// Print the SSP clusters and populate their distribution graphs.
		OutputLogger.println("SSP Clusters:");
		if(!sspClusters.isEmpty()) {
			for(SSPCluster sspCluster : sspClusters) {
				OutputLogger.printf("\t%s%n", TriggerDiagnosticUtil.clusterToString(sspCluster));
			}
		} else { OutputLogger.println("\tNone"); }
		
		// Print the matched clusters.
		OutputLogger.println("Matched Clusters:");
		if(event.getMatches() != 0) {
			// Iterate over the matched pairs.
			for(ClusterMatchedPair pair : event.getClusterPairs()) {
				// If the pair is a match, print it out.
				if(pair.isMatch()) {
					OutputLogger.printf("\t%s --> %s%n",
							TriggerDiagnosticUtil.clusterToString(pair.getReconstructedCluster()),
							TriggerDiagnosticUtil.clusterToString(pair.getSSPCluster()));
				}
			}
		}
		 else { OutputLogger.println("\tNone"); }
		
		// Print event statistics.
		OutputLogger.println();
		OutputLogger.println("Event Statistics:");
		OutputLogger.printf("\tRecon Clusters     :: %d%n", reconClusters.size());
		OutputLogger.printf("\tClusters Matched   :: %d%n", event.getMatches());
		OutputLogger.printf("\tFailed (Position)  :: %d%n", event.getPositionFailures());
		OutputLogger.printf("\tFailed (Time)      :: %d%n", event.getTimeFailures());
		OutputLogger.printf("\tFailed (Energy)    :: %d%n", event.getEnergyFailures());
		OutputLogger.printf("\tFailed (Hit Count) :: %d%n", event.getHitCountFailures());
		OutputLogger.printf("\tCluster Efficiency :: %3.0f%%%n", 100.0 * event.getMatches() / reconClusters.size());
		
		// Note whether there was a cluster match failure.
		if(event.isFailState() || event.getMatches() - reconClusters.size() != 0) {
			clusterFail = true;
		}
		
		
		
		// TEMP :: Populate the cluster diagnostic plots.
		
		// Populate the ALL cluster plots.
		for(Cluster cluster : reconClusters) {
			clusterHitPlot[RECON][ALL].fill(cluster.getCalorimeterHits().size());
			clusterEnergyPlot[RECON][ALL].fill(cluster.getEnergy());
			clusterTimePlot[RECON][ALL].fill(cluster.getCalorimeterHits().get(0).getTime());
			Point position = TriggerDiagnosticUtil.getClusterPosition(cluster);
			clusterPositionPlot[RECON][ALL].fill(position.x, position.y);
		}
		for(SSPCluster cluster : sspClusters) {
			clusterHitPlot[SSP][ALL].fill(cluster.getHitCount());
			clusterEnergyPlot[SSP][ALL].fill(cluster.getEnergy());
			clusterTimePlot[SSP][ALL].fill(cluster.getTime());
			clusterPositionPlot[SSP][ALL].fill(cluster.getXIndex(), cluster.getYIndex());
		}
		
		// Populate the matched and failed plots.
		for(ClusterMatchedPair pair : event.getClusterPairs()) {
			 if(pair.getFirstElement() != null && pair.getSecondElement() != null) {
				double energyDiff = pair.getSecondElement().getEnergy() - pair.getFirstElement().getEnergy();
				int hitDiff = pair.getSecondElement().getHitCount() - pair.getFirstElement().getCalorimeterHits().size();
				energyhitDiffPlot[ALL].fill(energyDiff, hitDiff);
			 }
			
			if(pair.isMatch()) {
				if(pair.getFirstElement() != null) {
					clusterHitPlot[RECON][MATCHED].fill(pair.getFirstElement().getCalorimeterHits().size());
					clusterEnergyPlot[RECON][MATCHED].fill(pair.getFirstElement().getEnergy());
					clusterTimePlot[RECON][MATCHED].fill(pair.getFirstElement().getCalorimeterHits().get(0).getTime());
					Point position = TriggerDiagnosticUtil.getClusterPosition(pair.getFirstElement());
					clusterPositionPlot[RECON][MATCHED].fill(position.x, position.y);
				} if(pair.getSecondElement() != null) {
					clusterHitPlot[SSP][MATCHED].fill(pair.getSecondElement().getHitCount());
					clusterEnergyPlot[SSP][MATCHED].fill(pair.getSecondElement().getEnergy());
					clusterTimePlot[SSP][MATCHED].fill(pair.getSecondElement().getTime());
					clusterPositionPlot[SSP][MATCHED].fill(pair.getSecondElement().getXIndex(), pair.getSecondElement().getYIndex());
				} if(pair.getFirstElement() != null && pair.getSecondElement() != null) {
					double energyDiff = pair.getSecondElement().getEnergy() - pair.getFirstElement().getEnergy();
					int hitDiff = pair.getSecondElement().getHitCount() - pair.getFirstElement().getCalorimeterHits().size();
					energyhitDiffPlot[MATCHED].fill(energyDiff, hitDiff);
				}
			} else {
				if(pair.getFirstElement() != null) {
					clusterHitPlot[RECON][FAILED].fill(pair.getFirstElement().getCalorimeterHits().size());
					clusterEnergyPlot[RECON][FAILED].fill(pair.getFirstElement().getEnergy());
					clusterTimePlot[RECON][FAILED].fill(pair.getFirstElement().getCalorimeterHits().get(0).getTime());
					Point position = TriggerDiagnosticUtil.getClusterPosition(pair.getFirstElement());
					clusterPositionPlot[RECON][FAILED].fill(position.x, position.y);
				} if(pair.getSecondElement() != null) {
					clusterHitPlot[SSP][FAILED].fill(pair.getSecondElement().getHitCount());
					clusterEnergyPlot[SSP][FAILED].fill(pair.getSecondElement().getEnergy());
					clusterTimePlot[SSP][FAILED].fill(pair.getSecondElement().getTime());
					clusterPositionPlot[SSP][FAILED].fill(pair.getSecondElement().getXIndex(), pair.getSecondElement().getYIndex());
				} if(pair.getFirstElement() != null && pair.getSecondElement() != null) {
					double energyDiff = pair.getSecondElement().getEnergy() - pair.getFirstElement().getEnergy();
					int hitDiff = pair.getSecondElement().getHitCount() - pair.getFirstElement().getCalorimeterHits().size();
					energyhitDiffPlot[FAILED].fill(energyDiff, hitDiff);
				}
			}
		}
	}
	
	/**
     * Performs cluster matching between a collection of reconstructed
	 * clusters and a collection of SSP clusters with an algorithm that
	 * ignores the times reported for each cluster.
	 * @param reconClusters - A collection of reconstructed clusters.
	 * @param sspClusters - A collection of SSP clusters.
	 * @param energyWindow - The window of allowed deviation between
	 * the reconstructed cluster and SSP cluster energies.
	 * @param hitWindow - The window of allowed deviation between
	 * the reconstructed cluster and SSP cluster hit counts.
	 * @return Returns the cluster matching results stored inside a
	 * <code>clusterMatchEvent</code> object.
	 */
	private static final DetailedClusterEvent matchClusters(Collection<Cluster> reconClusters,
			Collection<SSPCluster> sspClusters, double energyWindow, int hitWindow) {
		// Track the number of cluster pairs that were matched and that
		// failed by failure type.
		DetailedClusterEvent event = new DetailedClusterEvent();
		
		// Create maps to link cluster position to the list of clusters
		// that were found at that location.
		Map<Point, List<Cluster>> reconClusterMap = new HashMap<Point, List<Cluster>>(reconClusters.size());
		Map<Point, List<SSPCluster>> sspClusterMap = new HashMap<Point, List<SSPCluster>>(reconClusters.size());
		
		// Populate the reconstructed cluster map.
		for(Cluster reconCluster : reconClusters) {
			// Get the cluster position.
			Point position = new Point(TriggerDiagnosticUtil.getXIndex(reconCluster),
					TriggerDiagnosticUtil.getYIndex(reconCluster));
			
			// Get the list for this cluster position.
			List<Cluster> reconList = reconClusterMap.get(position);
			if(reconList == null) {
				reconList = new ArrayList<Cluster>();
				reconClusterMap.put(position, reconList);
			}
			
			// Add the cluster to the list.
			reconList.add(reconCluster);
		}
		
		// Populate the SSP cluster map.
		for(SSPCluster sspCluster : sspClusters) {
			// Get the cluster position.
			Point position = new Point(sspCluster.getXIndex(), sspCluster.getYIndex());
			
			// Get the list for this cluster position.
			List<SSPCluster> sspList = sspClusterMap.get(position);
			if(sspList == null) {
				sspList = new ArrayList<SSPCluster>();
				sspClusterMap.put(position, sspList);
			}
			
			// Add the cluster to the list.
			sspList.add(sspCluster);
		}
		
		// For each reconstructed cluster, attempt to match the clusters
		// with SSP clusters at the same position.
		positionLoop:
		for(Entry<Point, List<Cluster>> clusterSet : reconClusterMap.entrySet()) {
			// Get the reconstructed and SSP clusters at this position.
			List<Cluster> reconList = clusterSet.getValue();
			List<SSPCluster> sspList = sspClusterMap.get(clusterSet.getKey());
			
			// Print the crystal position header.
			OutputLogger.println();
			OutputLogger.printf("Considering clusters at (%3d, %3d)%n", clusterSet.getKey().x, clusterSet.getKey().y);
			
			// If there are no SSP clusters, then matching fails by
			// reason of position. The remainder of the loop may be
			// skipped, since there is nothing to check.
			if(sspList == null || sspList.isEmpty()) {
				event.pairFailPosition(reconList.size());
				continue positionLoop;
			}
			
			// Get all possible permutations of SSP clusters.
			List<List<Pair<Cluster, SSPCluster>>> permutations = getPermutations(reconList, sspList);
			
			// Print the information for this crystal position.
			OutputLogger.printf("\tRecon Clusters :: %d%n", reconList.size());
			OutputLogger.printf("\tSSP Clusters   :: %d%n", sspList.size());
			OutputLogger.printf("\tPermutations   :: %d%n", permutations.size());
			
			// Track the plotted values for the current best permutation.
			DetailedClusterEvent bestPerm = null;
			
			// Iterate over the permutations and find the permutation
			// that produces the best possible result when compared to
			// the reconstructed clusters.
			int permIndex = 0;
			for(List<Pair<Cluster, SSPCluster>> pairs : permutations) {
				// Update the current permutation number.
				permIndex++;
				
				// Track the plot values for this permutation.
				DetailedClusterEvent perm = new DetailedClusterEvent();
				
				// Try to match each pair.
				pairLoop:
				for(Pair<Cluster, SSPCluster> pair : pairs) {
					// Print the current reconstructed/SSP cluster pair.
					OutputLogger.printf("\tP%d :: %s --> %s", permIndex,
							pair.getFirstElement() == null ? "None" : TriggerDiagnosticUtil.clusterToString(pair.getFirstElement()),
							pair.getSecondElement() == null ? "None" : TriggerDiagnosticUtil.clusterToString(pair.getSecondElement()));
					
					// If either cluster in the pair is null, there
					// are not enough clusters to perform this match.
					if(pair.getFirstElement() == null || pair.getSecondElement() == null) {
						// Log the result.
						OutputLogger.printf(" [ %18s ]%n", "failure: unpaired");
						
						// An unpaired SSP cluster does not necessarily
						// represent a problem. Often, this just means
						// that the SSP cluster's matching reconstructed
						// cluster is outside the verification window.
						if(pair.getSecondElement() == null) {
							perm.pairFailPosition(pair.getFirstElement(), pair.getSecondElement());
						}
						
						// Skip the rest of the checks.
						continue pairLoop;
					}
					
					// Check if the reconstructed cluster has an energy
					// within the allotted threshold of the SSP cluster.
					if(pair.getSecondElement().getEnergy() >= pair.getFirstElement().getEnergy() - energyWindow &&
							pair.getSecondElement().getEnergy() <= pair.getFirstElement().getEnergy() + energyWindow) {
						
						// Check that the hit count of the reconstructed
						// is within the allotted threshold of the SSP
						// cluster.
						if(pair.getSecondElement().getHitCount() >= pair.getFirstElement().getCalorimeterHits().size() - hitWindow &&
								pair.getSecondElement().getHitCount() <= pair.getFirstElement().getCalorimeterHits().size() + hitWindow) {
							// Designate the pair as a match.
							perm.pairMatch(pair.getFirstElement(), pair.getSecondElement());
							OutputLogger.printf(" [ %18s ]%n", "success: matched");
						} else {
							perm.pairFailHitCount(pair.getFirstElement(), pair.getSecondElement());
							OutputLogger.printf(" [ %18s ]%n", "failure: hit count");
						} // End hit count check.
					} else {
						perm.pairFailEnergy(pair.getFirstElement(), pair.getSecondElement());
						OutputLogger.printf(" [ %18s ]%n", "failure: energy");
					} // End energy check.
				} // End Pair Loop
				
				// Print the results of the permutation.
				OutputLogger.printf("\t\tPermutation Matched   :: %d%n", perm.getMatches());
				OutputLogger.printf("\t\tPermutation Energy    :: %d%n", perm.getEnergyFailures());
				OutputLogger.printf("\t\tPermutation Hit Count :: %d%n", perm.getHitCountFailures());
				
				// Check whether the results from this permutation
				// exceed the quality of the last best results. A
				// greater number of matches is always better. If the
				// matches are the same, select the one with fewer
				// failures due to energy.
				bestPerm = getBestPermutation(bestPerm, perm);
			} // End Permutation Loop
			
			// Print the final results for the position.
			OutputLogger.printf("\tPosition Matched   :: %d%n", bestPerm.getMatches());
			OutputLogger.printf("\tPosition Energy    :: %d%n", bestPerm.getEnergyFailures());
			OutputLogger.printf("\tPosition Hit Count :: %d%n", bestPerm.getHitCountFailures());
			
			// Add the results from the best-matched permutation
			// to the event efficiency results.
			event.addEvent(bestPerm);
		} // End Crystal Position Loop
		
		// Return the cluster match summary.
		return event;
	}
	
	/**
	 * Performs cluster matching between a collection of reconstructed
	 * clusters and a collection of SSP clusters using the strictly
	 * time-compliant algorithm.
	 * @param reconClusters - A collection of reconstructed clusters.
	 * @param sspClusters - A collection of SSP clusters.
	 * @param energyWindow - The window of allowed deviation between
	 * the reconstructed cluster and SSP cluster energies.
	 * @param hitWindow - The window of allowed deviation between
	 * the reconstructed cluster and SSP cluster hit counts.
	 * @return Returns the cluster matching results stored inside a
	 * <code>clusterMatchEvent</code> object.
	 */
	private static final DetailedClusterEvent matchClustersTimeCompliant(Collection<Cluster> reconClusters,
			Collection<SSPCluster> sspClusters, double energyWindow, int hitWindow) {
		// Track the number of cluster pairs that were matched and that
		// failed by failure type.
		DetailedClusterEvent event = new DetailedClusterEvent();
		
		// Store the clusters which have been successfully paired.
		Set<SSPCluster> sspMatched = new HashSet<SSPCluster>(sspClusters.size());
		
		// Find reconstructed/SSP cluster matched pairs.
		reconLoop:
		for(Cluster reconCluster : reconClusters) {
			// Track whether a position-matched cluster was found.
			boolean matchedPosition = false;
			
			// VERBOSE :: Output the cluster being matched.
			OutputLogger.printf("Considering %s%n", TriggerDiagnosticUtil.clusterToString(reconCluster));
			
			// Search through the SSP clusters for a matching cluster.
			sspLoop:
			for(SSPCluster sspCluster : sspClusters) {
				// VERBOSE :: Output the SSP cluster being considered.
				OutputLogger.printf("\t%s ", TriggerDiagnosticUtil.clusterToString(sspCluster));
				
				// If this cluster has been paired, skip it.
				if(sspMatched.contains(sspCluster)) {
					OutputLogger.printf("[ %7s; %9s ]%n", "fail", "matched");
					continue sspLoop;
				}
				
				// Matched clusters must have the same position.
				if(TriggerDiagnosticUtil.getXIndex(reconCluster) != sspCluster.getXIndex()
						|| TriggerDiagnosticUtil.getYIndex(reconCluster) != sspCluster.getYIndex()) {
					OutputLogger.printf("[ %7s; %9s ]%n", "fail", "position");
					continue sspLoop;
				}
				
				// Note that a cluster was found at this position.
				matchedPosition = true;
				
				// Matched clusters must have the same time-stamp.
				if(reconCluster.getCalorimeterHits().get(0).getTime() != sspCluster.getTime()) {
					OutputLogger.printf("[ %7s; %9s ]%n", "fail", "time");
					continue sspLoop;
				}
				
				// Clusters that pass all of the above checks are the
				// same cluster.
				sspMatched.add(sspCluster);
				
				// Check that the clusters are sufficiently close in
				// energy to one another.
				if(sspCluster.getEnergy() >= reconCluster.getEnergy() - energyWindow
						&& sspCluster.getEnergy() <= reconCluster.getEnergy() + energyWindow) {
					// If a cluster matches in energy, check that it
					// is also sufficiently close in hit count.
					if(sspCluster.getHitCount() >= reconCluster.getCalorimeterHits().size() - hitWindow &&
							sspCluster.getHitCount() <= reconCluster.getCalorimeterHits().size() + hitWindow) {
						// The cluster is a match.
						event.pairMatch(reconCluster, sspCluster);
						OutputLogger.printf("[ %7s; %9s ]%n", "success", "matched");
						continue reconLoop;
					} else {
						event.pairFailHitCount(reconCluster, sspCluster);
						OutputLogger.printf("[ %7s; %9s ]%n", "fail", "hit count");
						continue reconLoop;
					} // End hit count check.
				} else {
					event.pairFailEnergy(reconCluster, sspCluster);
					OutputLogger.printf("[ %7s; %9s ]%n", "fail", "energy");
					continue reconLoop;
				} // End energy check.
			}// End SSP loop.
			
			// If the reconstructed cluster has not been matched, check
			// if a cluster was found at the same position. If not, then
			// the cluster fails by reason of position.
			if(!matchedPosition) {
				event.pairFailPosition(reconCluster, null);
			}
			
			// Otherwise, the cluster had a potential matched, but the
			// time-stamps were off. The cluster fails by reason of time.
			else {
				event.pairFailTime(reconCluster, null);
			}
		} // End recon loop.
		
		// Return the populated match event.
		return event;
	}
	
	/**
	 * Checks triggers simulated on SSP clusters against the SSP bank's
	 * reported triggers to verify that the trigger is correctly applying
	 * cuts to the clusters it sees. Additionally compares triggers
	 * simulated on reconstructed clusters to measure trigger efficiency.
	 */
	private void singlesTriggerVerification() {
		// Create lists of generic triggers.
		List<List<? extends Trigger<?>>> sspTriggerList = new ArrayList<List<? extends Trigger<?>>>(2);
		List<List<? extends Trigger<?>>> reconTriggerList = new ArrayList<List<? extends Trigger<?>>>(2);
		
		// Convert the simulated triggers to generic versions and add
		// them to the generic list.
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			// Get the generic trigger list.
			List<? extends Trigger<?>> sspTriggers = sspSinglesTriggers.get(triggerNum);
			List<? extends Trigger<?>> reconTriggers = reconSinglesTriggers.get(triggerNum);
			
			// Add it to the generic list.
			sspTriggerList.add(sspTriggers);
			reconTriggerList.add(reconTriggers);
		}
		
		// Run generic trigger verification.
		triggerVerification(sspTriggerList, reconTriggerList, true);
	}
	
	/**
	 * Checks triggers simulated on SSP clusters against the SSP bank's
	 * reported triggers to verify that the trigger is correctly applying
	 * cuts to the clusters it sees. Additionally compares triggers
	 * simulated on reconstructed clusters to measure trigger efficiency.
	 */
	private void pairTriggerVerification() {
		// Create lists of generic triggers.
		List<List<? extends Trigger<?>>> sspTriggerList = new ArrayList<List<? extends Trigger<?>>>(2);
		List<List<? extends Trigger<?>>> reconTriggerList = new ArrayList<List<? extends Trigger<?>>>(2);
		
		// Convert the simulated triggers to generic versions and add
		// them to the generic list.
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			// Get the generic trigger list.
			List<? extends Trigger<?>> sspTriggers = sspPairsTriggers.get(triggerNum);
			List<? extends Trigger<?>> reconTriggers = reconPairsTriggers.get(triggerNum);
			
			// Add it to the generic list.
			sspTriggerList.add(sspTriggers);
			reconTriggerList.add(reconTriggers);
		}
		
		// Run generic trigger verification.
		triggerVerification(sspTriggerList, reconTriggerList, false);
	}
	
	/**
	 * Performs trigger verification for both trigger types.
	 * @param sspTriggerList - The list of SSP triggers.
	 * @param reconTriggerList - The list of reconstructed triggers.
	 * @param isSingles - Whether or not this is a singles trigger
	 * verification.
	 */
	private void triggerVerification(List<List<? extends Trigger<?>>> sspTriggerList, 
			List<List<? extends Trigger<?>>> reconTriggerList, boolean isSingles) {
		
		// ==========================================================
		// ==== Initialize Trigger Verification =====================
		// ==========================================================
		
		// Print the cluster verification header.
		OutputLogger.println();
		OutputLogger.println();
		OutputLogger.println("======================================================================");
		if(isSingles) { OutputLogger.println("=== Singles Trigger Verification ====================================="); }
		else { OutputLogger.println("=== Pair Trigger Verification ========================================"); }
		OutputLogger.println("======================================================================");
		
		// Track the number of triggers seen and the number found.
		TriggerEvent[] triggerEvent = { new TriggerEvent(), new TriggerEvent() };
		
		// ==========================================================
		// ==== Output Event Summary ================================
		// ==========================================================
		
		// Get the list of triggers reported by the SSP.
		List<? extends SSPNumberedTrigger> sspTriggers;
		if(isSingles) { sspTriggers = sspBank.getSinglesTriggers(); }
		else { sspTriggers = sspBank.getPairTriggers(); }
		
		// Output the SSP cluster triggers.
		OutputLogger.println();
		OutputLogger.println("SSP Cluster " + (isSingles ? "Singles" : "Pair") + " Triggers");
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			for(Trigger<?> simTrigger : sspTriggerList.get(triggerNum)) {
				OutputLogger.printf("\tTrigger %d :: %s :: %3.0f :: %s%n",
						(triggerNum + 1), triggerPositionString(simTrigger),
						getTriggerTime(simTrigger), simTrigger.toString());
			}
		}
		if(sspTriggerList.get(0).size() + sspTriggerList.get(1).size() == 0) {
			OutputLogger.println("\tNone");
		}
		
		// Output the reconstructed cluster singles triggers.
		OutputLogger.println("Reconstructed Cluster " + (isSingles ? "Singles" : "Pair") + " Triggers");
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			for(Trigger<?> simTrigger : reconTriggerList.get(triggerNum)) {
				OutputLogger.printf("\tTrigger %d :: %s :: %3.0f :: %s%n",
						(triggerNum + 1), triggerPositionString(simTrigger),
						getTriggerTime(simTrigger), simTrigger.toString());
			}
		}
		if(reconTriggerList.get(0).size() + reconTriggerList.get(1).size() == 0) {
			OutputLogger.println("\tNone");
		}
		
		// Output the SSP reported triggers.
		OutputLogger.println("SSP Reported " + (isSingles ? "Singles" : "Pair") + " Triggers");
		for(SSPTrigger sspTrigger : sspTriggers) {
			OutputLogger.printf("\t%s%n", sspTrigger.toString());
		}
		if(sspTriggers.size() == 0) { OutputLogger.println("\tNone"); }
		
		// Update the trigger event with the counts for each type of
		// simulated trigger. Reported triggers are counted later when
		// already iterating over them.
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			triggerEvent[triggerNum].sawSSPSimulatedTriggers(tiFlags, sspTriggerList.get(triggerNum).size());
			triggerEvent[triggerNum].sawReconSimulatedTriggers(tiFlags, reconTriggerList.get(triggerNum).size());
		}
		
		
		
		// ==========================================================
		// ==== SSP Internal Logic Verification =====================
		// ==========================================================
		
		// Track which SSP triggers have been matched to avoid matching
		// multiple reconstructed SSP cluster triggers to the same SSP
		// trigger.
		Set<SSPNumberedTrigger> sspTriggerSet = new HashSet<SSPNumberedTrigger>();
		Set<Trigger<?>> simTriggerSet = new HashSet<Trigger<?>>();
		
		// Track the number of SSP reported triggers that are found in
		// excess of the SSP simulated triggers.
		int sspReportedExtras = sspTriggers.size() - (sspTriggerList.get(0).size() + sspTriggerList.get(1).size());
		if(sspReportedExtras > 0) {
			if(isSingles) { singlesInternalFail = true; }
			else { pairInternalFail = true; }
		} else { sspReportedExtras = 0; }
		
		// Iterate over the triggers.
		OutputLogger.println();
		OutputLogger.println("Matching SSP Reported Triggers to SSP Simulated Triggers:");
		for(SSPNumberedTrigger sspTrigger : sspTriggers) {
			// Get the trigger information.
			int triggerNum = sspTrigger.isFirstTrigger() ? 0 : 1;
			OutputLogger.printf("\t%s%n", sspTrigger.toString());
			
			// Note that a bank trigger was seen.
			triggerEvent[triggerNum].sawReportedTrigger();
			
			// Iterate over the SSP cluster simulated triggers and
			// look for a trigger that matches.
			matchLoop:
			for(Trigger<?> simTrigger : sspTriggerList.get(triggerNum)) {
				// VERBOSE :: Output the trigger being considered for
				//            matching.
				OutputLogger.printf("\t\tTrigger %d :: %s :: %3.0f :: %s ",
						(triggerNum + 1), triggerPositionString(simTrigger),
						getTriggerTime(simTrigger), simTrigger.toString());
				
				// If the current SSP trigger has already been matched,
				// skip it.
				if(simTriggerSet.contains(simTrigger)) {
					OutputLogger.printf("[ %-15s ]%n", "failed; matched");
					continue matchLoop;
				}
				
				// Check that the triggers have the same time. Triggers
				// generated from SSP bank clusters should always align
				// in time.
				if(sspTrigger.getTime() != getTriggerTime(simTrigger)) {
					OutputLogger.printf("[ %-15s ]%n", "failed; time");
					continue matchLoop;
				}
				
				// Check whether the trigger cuts match.
				boolean[] matchedCuts = triggerCutMatch(simTrigger, sspTrigger);
				for(int i = 0; i < matchedCuts.length; i++) {
					if(!matchedCuts[i]) {
						int typeIndex = isSingles ? 0 : 1;
						OutputLogger.printf("[ %-15s ]%n", String.format("failed; %s", cutNames[typeIndex][i]));
						continue matchLoop;
					}
				}
				
				// If all the cuts match, along with the time and the
				// trigger number, than these triggers are a match.
				sspTriggerSet.add(sspTrigger);
				simTriggerSet.add(simTrigger);
				triggerEvent[triggerNum].matchedSSPTrigger(tiFlags);
				OutputLogger.printf("[ %-15s ]%n", "success");
				break matchLoop;
			}
		}
		
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			for(Trigger<?> simTrigger : sspTriggerList.get(triggerNum)) {
				globalTriggerPlots.sawTrigger(simTrigger);
				if(simTriggerSet.contains(simTrigger)) {
					globalTriggerPlots.matchedTrigger(simTrigger);
				} else {
					globalTriggerPlots.failedTrigger(simTrigger);
				}
			}
		}
		
		// Iterate over the unmatched simulated triggers again and the
		// unmatched SSP reported trigger that most closely matches it.
		OutputLogger.println();
		OutputLogger.println("Matching Failed SSP Reported Triggers to Remaining SSP Simulated Triggers:");
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			simLoop:
			for(Trigger<?> simTrigger : sspTriggerList.get(triggerNum)) {
				OutputLogger.printf("\tTrigger %d :: %s :: %3.0f :: %s%n",
						(triggerNum + 1), triggerPositionString(simTrigger),
						getTriggerTime(simTrigger), simTrigger.toString());
				
				// Check whether this trigger has already been matched
				// or not. If it has been matched, skip it.
				if(simTriggerSet.contains(simTrigger)) {
					OutputLogger.println("\t\tSkipping; already matched successfully");
					continue simLoop;
				}
				
				// Get the trigger time for the simulated trigger.
				double simTime = getTriggerTime(simTrigger);
				
				// Track the match statistics for each reported trigger
				// so that the closest match may be found.
				int numMatched = -1;
				boolean[] matchedCut = null;
				SSPNumberedTrigger bestMatch = null;
				
				// Store the readout for the best match.
				String bestMatchText = null;
				
				// Iterate over the reported triggers to find a match.
				reportedLoop:
				for(SSPNumberedTrigger sspTrigger : sspTriggers) {
					OutputLogger.printf("\t\t%s ", sspTrigger.toString());
					
					// If the two triggers have different times, this
					// trigger should be skipped.
					if(sspTrigger.getTime() != simTime) {
						OutputLogger.printf("[ %-15s ]%n", "failed; time");
						continue reportedLoop;
					}
					
					// If this reported trigger has been matched then
					// it should be skipped.
					if(sspTriggerSet.contains(sspTrigger)) {
						OutputLogger.printf("[ %-15s ]%n", "failed; matched");
						continue reportedLoop;
					}
					
					// Check each of the cuts.
					boolean[] tempMatchedCut = triggerCutMatch(simTrigger, sspTrigger);
					
					// Check each cut and see if this is a closer match
					// than the previous best match.
					int tempNumMatched = 0;
					for(boolean passed : tempMatchedCut) { if(passed) { tempNumMatched++; } }
					OutputLogger.printf("[ %-15s ]%n", String.format("maybe; %d failed", tempNumMatched));
					
					// If the number of matched cuts exceeds the old
					// best result, this becomes the new best result.
					if(tempNumMatched > numMatched) {
						numMatched = tempNumMatched;
						matchedCut = tempMatchedCut;
						bestMatch = sspTrigger;
						bestMatchText = String.format("%s%n", sspTrigger.toString());
					}
				}
				
				// If there was no match found, it means that there were
				// no triggers that were both unmatched and at the same
				// time as this simulated trigger.
				if(bestMatch == null) {
					if(isSingles) { singlesInternalFail = true; }
					else { pairInternalFail = true; }
					triggerEvent[triggerNum].failedSSPTrigger();
					OutputLogger.printf("\t\tTrigger %d :: %s :: %3.0f :: %s",
							(triggerNum + 1), triggerPositionString(simTrigger),
							getTriggerTime(simTrigger), simTrigger.toString());
					OutputLogger.println(" --> No Valid Match Found");
				} else {
					triggerEvent[triggerNum].matchedSSPTrigger(tiFlags, matchedCut);
					OutputLogger.printf("\t\tTrigger %d :: %s :: %3.0f :: %s",
							(triggerNum + 1), triggerPositionString(simTrigger),
							getTriggerTime(simTrigger), simTrigger.toString());
					OutputLogger.println(" --> " + bestMatchText);
				}
			}
		}
		
		
		
		// ==========================================================
		// ==== Trigger Efficiency ==================================
		// ==========================================================
		
		// Reset the SSP matched trigger set.
		sspTriggerSet.clear();
		
		// Iterate over the reconstructed cluster singles triggers.
		OutputLogger.println();
		OutputLogger.println("Recon Cluster Trigger --> SSP Reported Trigger Match Status");
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			for(Trigger<?> simTrigger : reconTriggerList.get(triggerNum)) {
				OutputLogger.printf("\tTrigger %d :: %s :: %s%n", (triggerNum + 1),
						triggerPositionString(simTrigger), simTrigger.toString());
				
				// TEMP :: Populate the recon ALL pairs plots.
				globalTriggerPlots.sawTrigger(simTrigger);
				
				// Iterate over the SSP reported triggers and compare
				// them to the reconstructed cluster simulated trigger.
				boolean matched = false;
				matchLoop:
				for(SSPNumberedTrigger sspTrigger : sspTriggers) {
					OutputLogger.printf("\t\t\t%s", sspTrigger.toString());
					
					// Only compare triggers if they are from the
					// same trigger source.
					if((triggerNum == 0 && sspTrigger.isSecondTrigger())
							|| (triggerNum == 1 && sspTrigger.isFirstTrigger())) {
						OutputLogger.print(" [ fail; source    ]%n");
						continue matchLoop;
					}
					
					// Only compare the singles trigger if it was
					// not already matched to another trigger.
					if(sspTriggerSet.contains(sspTrigger)) {
						OutputLogger.print(" [ fail; matched   ]%n");
						continue matchLoop;
					}
					
					// Test each cut.
					int typeIndex = isSingles ? 0 : 1;
					boolean[] matchedCuts = triggerCutMatch(simTrigger, sspTrigger);
					for(int cutIndex = 0; cutIndex < matchedCuts.length; cutIndex++) {
						if(!matchedCuts[cutIndex]) {
							OutputLogger.printf(" [ fail; %-9s ]%n", cutNames[typeIndex][cutIndex]);
							continue matchLoop;
						}
					}
					
					// If all the trigger flags match, then the
					// triggers are a match.
					sspTriggerSet.add(sspTrigger);
					triggerEvent[triggerNum].matchedReconTrigger(tiFlags);
					OutputLogger.print(" [ success         ]%n");
					globalTriggerPlots.matchedTrigger(simTrigger);
					matched = true;
					break matchLoop;
				}
				
				if(!matched) { globalTriggerPlots.failedTrigger(simTrigger); }
			}
		}
		
		
		
		// ==========================================================
		// ==== Output Event Results ================================
		// ==========================================================
		
		// Get the number of SSP and reconstructed cluster simulated
		// triggers.
		int sspSimTriggers = sspTriggerList.get(0).size() + sspTriggerList.get(1).size();
		int reconSimTriggers = reconTriggerList.get(0).size() + reconTriggerList.get(1).size();
		int[] sspTriggerCount = { sspTriggerList.get(0).size(), sspTriggerList.get(1).size() };
		
		// Print event statistics.
		OutputLogger.println();
		OutputLogger.println("Event Statistics:");
		OutputLogger.printf("\tSSP Cluster Sim Triggers   :: %d%n", sspSimTriggers);
		OutputLogger.printf("\tRecon Cluster Sim Triggers :: %d%n", reconSimTriggers);
		OutputLogger.printf("\tSSP Reported Triggers      :: %d%n", sspTriggers.size());
		
		int matchedSSPTriggers = triggerEvent[0].getMatchedSSPSimulatedTriggers() + triggerEvent[1].getMatchedSSPSimulatedTriggers();
		OutputLogger.printf("\tInternal Efficiency        :: %d / %d ", matchedSSPTriggers, sspSimTriggers);
		if(sspSimTriggers == 0) { OutputLogger.printf("(N/A)%n"); }
		else { OutputLogger.printf("(%3.0f%%)%n", (100.0 * matchedSSPTriggers / sspSimTriggers)); }
		
		int matchedReconTriggers = triggerEvent[0].getMatchedReconSimulatedTriggers() + triggerEvent[1].getMatchedReconSimulatedTriggers();
		OutputLogger.printf("\tTrigger Efficiency         :: %d / %d", matchedReconTriggers, reconSimTriggers);
		if(reconSimTriggers == 0) { OutputLogger.printf("(N/A)%n"); }
		else { OutputLogger.printf("(%3.0f%%)%n", (100.0 * matchedReconTriggers / reconSimTriggers)); }
		
		// Print the individual cut performances.
		if(isSingles) {
			OutputLogger.println();
			for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
				OutputLogger.printf("Trigger %d Individual Cut Failure Rate:%n", (triggerNum + 1));
				if(sspSimTriggers == 0) {
					OutputLogger.printf("\tCluster Energy Lower Bound :: %d / %d%n", triggerEvent[triggerNum].getSSPCutFailures(ENERGY_MIN), sspTriggerCount[triggerNum]);
					OutputLogger.printf("\tCluster Energy Upper Bound :: %d / %d%n", triggerEvent[triggerNum].getSSPCutFailures(ENERGY_MAX), sspTriggerCount[triggerNum]);
					OutputLogger.printf("\tCluster Hit Count          :: %d / %d%n", triggerEvent[triggerNum].getSSPCutFailures(HIT_COUNT), sspTriggerCount[triggerNum]);
				} else {
					OutputLogger.printf("\tCluster Energy Lower Bound :: %d / %d (%3.0f%%)%n",
							triggerEvent[triggerNum].getSSPCutFailures(ENERGY_MIN), sspTriggerCount[triggerNum],
							(100.0 * triggerEvent[triggerNum].getSSPCutFailures(ENERGY_MIN) / sspTriggerCount[triggerNum]));
					OutputLogger.printf("\tCluster Energy Upper Bound :: %d / %d (%3.0f%%)%n",
							triggerEvent[triggerNum].getSSPCutFailures(ENERGY_MAX), sspTriggerCount[triggerNum],
							(100.0 * triggerEvent[triggerNum].getSSPCutFailures(ENERGY_MAX) / sspTriggerCount[triggerNum]));
					OutputLogger.printf("\tCluster Hit Count          :: %d / %d (%3.0f%%)%n",
							triggerEvent[triggerNum].getSSPCutFailures(HIT_COUNT), sspTriggerCount[triggerNum],
							(100.0 * triggerEvent[triggerNum].getSSPCutFailures(HIT_COUNT) / sspTriggerCount[triggerNum]));
				}
			}
			
			// Update the global trigger tracking variables.
			localStats.getTriggerStats().getSingles0Stats().addEvent(triggerEvent[0]);
			localStats.getTriggerStats().getSingles1Stats().addEvent(triggerEvent[1]);
			globalStats.getTriggerStats().getSingles0Stats().addEvent(triggerEvent[0]);
			globalStats.getTriggerStats().getSingles1Stats().addEvent(triggerEvent[1]);
		} else {
			for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
				OutputLogger.println();
				OutputLogger.printf("Trigger %d Individual Cut Failure Rate:%n", (triggerNum + 1));
				if(sspTriggerCount[triggerNum] == 0) {
					OutputLogger.printf("\tPair Energy Sum            :: %d / %d%n", triggerEvent[triggerNum].getSSPCutFailures(ENERGY_SUM), sspTriggerCount[triggerNum]);
					OutputLogger.printf("\tPair Energy Difference     :: %d / %d%n", triggerEvent[triggerNum].getSSPCutFailures(ENERGY_DIFF), sspTriggerCount[triggerNum]);
					OutputLogger.printf("\tPair Energy Slope          :: %d / %d%n", triggerEvent[triggerNum].getSSPCutFailures(ENERGY_SLOPE), sspTriggerCount[triggerNum]);
					OutputLogger.printf("\tPair Coplanarity           :: %d / %d%n", triggerEvent[triggerNum].getSSPCutFailures(COPLANARITY), sspTriggerCount[triggerNum]);
				} else {
					OutputLogger.printf("\tPair Energy Sum            :: %d / %d (%3.0f%%)%n",
							triggerEvent[triggerNum].getSSPCutFailures(ENERGY_SUM), sspTriggerCount[triggerNum],
							(100.0 * triggerEvent[triggerNum].getSSPCutFailures(ENERGY_SUM) / sspTriggerCount[triggerNum]));
					OutputLogger.printf("\tPair Energy Difference     :: %d / %d (%3.0f%%)%n",
							triggerEvent[triggerNum].getSSPCutFailures(ENERGY_DIFF), sspTriggerCount[triggerNum],
							(100.0 * triggerEvent[triggerNum].getSSPCutFailures(ENERGY_DIFF) / sspTriggerCount[triggerNum]));
					OutputLogger.printf("\tPair Energy Slope          :: %d / %d (%3.0f%%)%n",
							triggerEvent[triggerNum].getSSPCutFailures(ENERGY_SLOPE), sspTriggerCount[triggerNum],
							(100.0 * triggerEvent[triggerNum].getSSPCutFailures(ENERGY_SLOPE) / sspTriggerCount[triggerNum]));
					OutputLogger.printf("\tPair Coplanarity           :: %d / %d (%3.0f%%)%n",
							triggerEvent[triggerNum].getSSPCutFailures(COPLANARITY), sspTriggerCount[triggerNum],
							(100.0 * triggerEvent[triggerNum].getSSPCutFailures(COPLANARITY) / sspTriggerCount[triggerNum]));
				}
			}
			
			// Update the global trigger tracking variables.
			localStats.getTriggerStats().getPair0Stats().addEvent(triggerEvent[0]);
			localStats.getTriggerStats().getPair1Stats().addEvent(triggerEvent[1]);
			globalStats.getTriggerStats().getPair0Stats().addEvent(triggerEvent[0]);
			globalStats.getTriggerStats().getPair1Stats().addEvent(triggerEvent[1]);
		}
		
		// Note whether the was a trigger match failure.
		if(triggerEvent[0].getFailedReconSimulatedTriggers() != 0 && triggerEvent[1].getFailedReconSimulatedTriggers() != 0) {
			if(isSingles) { singlesEfficiencyFail = true; }
			else { pairEfficiencyFail = true; }
		} if(triggerEvent[0].getFailedSSPSimulatedTriggers() != 0 && triggerEvent[1].getFailedSSPSimulatedTriggers() != 0) {
			if(isSingles) { singlesInternalFail = true; }
			else { pairInternalFail = true; }
		}
	}
	
	/**
	 * Generates and stores the singles triggers for both reconstructed
	 * and SSP clusters.
	 */
	private void constructSinglesTriggers() {
		// Run the SSP clusters through the singles trigger to determine
		// whether they pass it or not.
		for(SSPCluster cluster : sspClusters) {
			triggerLoop:
			for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
				// For a cluster to have formed it is assumed to have passed
				// the cluster seed energy cuts. This can not be verified
				// since the SSP bank does not report individual hit. 
				boolean passSeedLow = true;
				boolean passSeedHigh = true;
				
				// The remaining cuts may be acquired from trigger module.
				boolean passClusterLow = singlesTrigger[triggerNum].clusterTotalEnergyCutLow(cluster);
				boolean passClusterHigh = singlesTrigger[triggerNum].clusterTotalEnergyCutHigh(cluster);
				boolean passHitCount = singlesTrigger[triggerNum].clusterHitCountCut(cluster);
				
				// Make a trigger to store the results.
				SinglesTrigger<SSPCluster> trigger = new SinglesTrigger<SSPCluster>(cluster, triggerNum);
				trigger.setStateSeedEnergyLow(passSeedLow);
				trigger.setStateSeedEnergyHigh(passSeedHigh);
				trigger.setStateClusterEnergyLow(passClusterLow);
				trigger.setStateClusterEnergyHigh(passClusterHigh);
				trigger.setStateHitCount(passHitCount);
				
				// A trigger will only be reported by the SSP if it
				// passes all of the enabled cuts for that trigger.
				// Check whether this trigger meets these conditions.
				if(singlesCutsEnabled[triggerNum][ENERGY_MIN] && !trigger.getStateClusterEnergyLow()) {
					continue triggerLoop;
				} if(singlesCutsEnabled[triggerNum][ENERGY_MAX] && !trigger.getStateClusterEnergyHigh()) {
					continue triggerLoop;
				} if(singlesCutsEnabled[triggerNum][HIT_COUNT] && !trigger.getStateHitCount()) {
					continue triggerLoop;
				}
				
				// If all the necessary checks passed, store the new
				// trigger for verification.
				sspSinglesTriggers.get(triggerNum).add(trigger);
			}
		}
		
		// Run the reconstructed clusters through the singles trigger
		// to determine whether they pass it or not.
		for(Cluster cluster : reconClusters) {
			// Simulate each of the cluster singles triggers.
			triggerLoop:
			for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
				// For a cluster to have formed it is assumed to have passed
				// the cluster seed energy cuts. This can not be verified
				// since the SSP bank does not report individual hit. 
				boolean passSeedLow = true;
				boolean passSeedHigh = true;
				
				// The remaining cuts may be acquired from trigger module.
				boolean passClusterLow = singlesTrigger[triggerNum].clusterTotalEnergyCutLow(cluster);
				boolean passClusterHigh = singlesTrigger[triggerNum].clusterTotalEnergyCutHigh(cluster);
				boolean passHitCount = singlesTrigger[triggerNum].clusterHitCountCut(cluster);
				
				// Make a trigger to store the results.
				SinglesTrigger<Cluster> trigger = new SinglesTrigger<Cluster>(cluster, triggerNum);
				trigger.setStateSeedEnergyLow(passSeedLow);
				trigger.setStateSeedEnergyHigh(passSeedHigh);
				trigger.setStateClusterEnergyLow(passClusterLow);
				trigger.setStateClusterEnergyHigh(passClusterHigh);
				trigger.setStateHitCount(passHitCount);
				
				// A trigger will only be reported by the SSP if it
				// passes all of the enabled cuts for that trigger.
				// Check whether this trigger meets these conditions.
				if(singlesCutsEnabled[triggerNum][ENERGY_MIN] && !trigger.getStateClusterEnergyLow()) {
					continue triggerLoop;
				} if(singlesCutsEnabled[triggerNum][ENERGY_MAX] && !trigger.getStateClusterEnergyHigh()) {
					continue triggerLoop;
				} if(singlesCutsEnabled[triggerNum][HIT_COUNT] && !trigger.getStateHitCount()) {
					continue triggerLoop;
				}
				
				// Store the trigger.
				reconSinglesTriggers.get(triggerNum).add(trigger);
			}
		}
	}
	
	/**
	 * Generates and stores the pair triggers for both reconstructed
	 * and SSP clusters.
	 */
	private void constructPairTriggers() {
		// Store cluster pairs.
		List<Cluster> topReconClusters = new ArrayList<Cluster>();
		List<Cluster> bottomReconClusters = new ArrayList<Cluster>();
		List<Cluster[]> reconPairs = new ArrayList<Cluster[]>();
		List<SSPCluster> topSSPClusters = new ArrayList<SSPCluster>();
		List<SSPCluster> bottomSSPClusters = new ArrayList<SSPCluster>();
		List<SSPCluster[]> sspPairs = new ArrayList<SSPCluster[]>();
		
		// Split the clusters into lists of top and bottom clusters.
		for(Cluster reconCluster : reconClusters) {
			if(reconCluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy") > 0) {
				topReconClusters.add(reconCluster);
			} else {
				bottomReconClusters.add(reconCluster);
			}
		}
		for(SSPCluster sspCluster : sspClusters) {
			if(sspCluster.getYIndex() > 0) {
				topSSPClusters.add(sspCluster);
			} else {
				bottomSSPClusters.add(sspCluster);
			}
		}
		
		// Form all possible top/bottom cluster pairs.
		for(Cluster topReconCluster : topReconClusters) {
			for(Cluster bottomReconCluster : bottomReconClusters) {
				Cluster[] reconPair = new Cluster[2];
				reconPair[0] = topReconCluster;
				reconPair[1] = bottomReconCluster;
				reconPairs.add(reconPair);
			}
		}
		for(SSPCluster topSSPCluster : topSSPClusters) {
			for(SSPCluster bottomSSPCluster : bottomSSPClusters) {
				SSPCluster[] sspPair = new SSPCluster[2];
				sspPair[0] = topSSPCluster;
				sspPair[1] = bottomSSPCluster;
				sspPairs.add(sspPair);
			}
		}
		
		// Simulate the pair triggers and record the results.
		for(Cluster[] reconPair : reconPairs) {
			// Simulate each of the cluster pair triggers.
			pairTriggerLoop:
			for(int triggerIndex = 0; triggerIndex < 2; triggerIndex++) {
				// Check that the pair passes the time coincidence cut.
				// If it does not, it is not a valid pair and should be
				// destroyed.
				if(!pairsTrigger[triggerIndex].pairTimeCoincidenceCut(reconPair)) {
					continue pairTriggerLoop;
				}
				
				// For a cluster to have formed it is assumed to have passed
				// the cluster seed energy cuts. This can not be verified
				// since the SSP bank does not report individual hit. 
				boolean passSeedLow = true;
				boolean passSeedHigh = true;
				
				// The remaining cuts may be acquired from trigger module.
				boolean passClusterLow = pairsTrigger[triggerIndex].clusterTotalEnergyCutLow(reconPair[0])
						&& pairsTrigger[triggerIndex].clusterTotalEnergyCutLow(reconPair[1]);
				boolean passClusterHigh = pairsTrigger[triggerIndex].clusterTotalEnergyCutHigh(reconPair[0])
						&& pairsTrigger[triggerIndex].clusterTotalEnergyCutHigh(reconPair[1]);
				boolean passHitCount = pairsTrigger[triggerIndex].clusterHitCountCut(reconPair[0])
						&& pairsTrigger[triggerIndex].clusterHitCountCut(reconPair[1]);
				boolean passPairEnergySumLow = pairsTrigger[triggerIndex].pairEnergySumCutLow(reconPair);
				boolean passPairEnergySumHigh = pairsTrigger[triggerIndex].pairEnergySumCutHigh(reconPair);
				boolean passPairEnergyDifference = pairsTrigger[triggerIndex].pairEnergyDifferenceCut(reconPair);
				boolean passPairEnergySlope = pairsTrigger[triggerIndex].pairEnergySlopeCut(reconPair);
				boolean passPairCoplanarity = pairsTrigger[triggerIndex].pairCoplanarityCut(reconPair);
				boolean passTimeCoincidence = pairsTrigger[triggerIndex].pairTimeCoincidenceCut(reconPair);
				
				// Create a trigger from the results.
				PairTrigger<Cluster[]> trigger = new PairTrigger<Cluster[]>(reconPair, triggerIndex);
				trigger.setStateSeedEnergyLow(passSeedLow);
				trigger.setStateSeedEnergyHigh(passSeedHigh);
				trigger.setStateClusterEnergyLow(passClusterLow);
				trigger.setStateClusterEnergyHigh(passClusterHigh);
				trigger.setStateHitCount(passHitCount);
				trigger.setStateEnergySumLow(passPairEnergySumLow);
				trigger.setStateEnergySumHigh(passPairEnergySumHigh);
				trigger.setStateEnergyDifference(passPairEnergyDifference);
				trigger.setStateEnergySlope(passPairEnergySlope);
				trigger.setStateCoplanarity(passPairCoplanarity);
				trigger.setStateTimeCoincidence(passTimeCoincidence);
				
				// A trigger will only be reported by the SSP if it
				// passes all of the enabled cuts for that trigger.
				// Check whether this trigger meets these conditions.
				if(pairCutsEnabled[triggerIndex][ENERGY_MIN] && !trigger.getStateClusterEnergyLow()) {
					continue pairTriggerLoop;
				} if(pairCutsEnabled[triggerIndex][ENERGY_MAX] && !trigger.getStateClusterEnergyHigh()) {
					continue pairTriggerLoop;
				} if(pairCutsEnabled[triggerIndex][HIT_COUNT] && !trigger.getStateHitCount()) {
					continue pairTriggerLoop;
				} if(pairCutsEnabled[triggerIndex][3 + ENERGY_SUM] && !trigger.getStateEnergySum()) {
					continue pairTriggerLoop;
				} if(pairCutsEnabled[triggerIndex][3 + ENERGY_DIFF] && !trigger.getStateEnergyDifference()) {
					continue pairTriggerLoop;
				} if(pairCutsEnabled[triggerIndex][3 + ENERGY_SLOPE] && !trigger.getStateEnergySlope()) {
					continue pairTriggerLoop;
				} if(pairCutsEnabled[triggerIndex][3 + COPLANARITY] && !trigger.getStateCoplanarity()) {
					continue pairTriggerLoop;
				}
				
				// Add the trigger to the list.
				reconPairsTriggers.get(triggerIndex).add(trigger);
			}
		}
		
		for(SSPCluster[] sspPair : sspPairs) {
			pairTriggerLoop:
			for(int triggerIndex = 0; triggerIndex < 2; triggerIndex++) {
				// Check that the pair passes the time coincidence cut.
				// If it does not, it is not a valid pair and should be
				// destroyed.
				if(!pairsTrigger[triggerIndex].pairTimeCoincidenceCut(sspPair)) {
					continue pairTriggerLoop;
				}
				
				// For a cluster to have formed it is assumed to have passed
				// the cluster seed energy cuts. This can not be verified
				// since the SSP bank does not report individual hit. 
				boolean passSeedLow = true;
				boolean passSeedHigh = true;
				
				// The remaining cuts may be acquired from trigger module.
				boolean passClusterLow = pairsTrigger[triggerIndex].clusterTotalEnergyCutLow(sspPair[0])
						&& pairsTrigger[triggerIndex].clusterTotalEnergyCutLow(sspPair[1]);
				boolean passClusterHigh = pairsTrigger[triggerIndex].clusterTotalEnergyCutHigh(sspPair[0])
						&& pairsTrigger[triggerIndex].clusterTotalEnergyCutHigh(sspPair[1]);
				boolean passHitCount = pairsTrigger[triggerIndex].clusterHitCountCut(sspPair[0])
						&& pairsTrigger[triggerIndex].clusterHitCountCut(sspPair[1]);
				boolean passPairEnergySumLow = pairsTrigger[triggerIndex].pairEnergySumCutLow(sspPair);
				boolean passPairEnergySumHigh = pairsTrigger[triggerIndex].pairEnergySumCutHigh(sspPair);
				boolean passPairEnergyDifference = pairsTrigger[triggerIndex].pairEnergyDifferenceCut(sspPair);
				boolean passPairEnergySlope = pairsTrigger[triggerIndex].pairEnergySlopeCut(sspPair);
				boolean passPairCoplanarity = pairsTrigger[triggerIndex].pairCoplanarityCut(sspPair);
				boolean passTimeCoincidence = pairsTrigger[triggerIndex].pairTimeCoincidenceCut(sspPair);
				
				// Create a trigger from the results.
				PairTrigger<SSPCluster[]> trigger = new PairTrigger<SSPCluster[]>(sspPair, triggerIndex);
				trigger.setStateSeedEnergyLow(passSeedLow);
				trigger.setStateSeedEnergyHigh(passSeedHigh);
				trigger.setStateClusterEnergyLow(passClusterLow);
				trigger.setStateClusterEnergyHigh(passClusterHigh);
				trigger.setStateHitCount(passHitCount);
				trigger.setStateEnergySumLow(passPairEnergySumLow);
				trigger.setStateEnergySumHigh(passPairEnergySumHigh);
				trigger.setStateEnergyDifference(passPairEnergyDifference);
				trigger.setStateEnergySlope(passPairEnergySlope);
				trigger.setStateCoplanarity(passPairCoplanarity);
				trigger.setStateTimeCoincidence(passTimeCoincidence);
				
				// A trigger will only be reported by the SSP if it
				// passes all of the enabled cuts for that trigger.
				// Check whether this trigger meets these conditions.
				if(pairCutsEnabled[triggerIndex][ENERGY_MIN] && !trigger.getStateClusterEnergyLow()) {
					continue pairTriggerLoop;
				} if(pairCutsEnabled[triggerIndex][ENERGY_MAX] && !trigger.getStateClusterEnergyHigh()) {
					continue pairTriggerLoop;
				} if(pairCutsEnabled[triggerIndex][HIT_COUNT] && !trigger.getStateHitCount()) {
					continue pairTriggerLoop;
				} if(pairCutsEnabled[triggerIndex][3 + ENERGY_SUM] && !trigger.getStateEnergySum()) {
					continue pairTriggerLoop;
				} if(pairCutsEnabled[triggerIndex][3 + ENERGY_DIFF] && !trigger.getStateEnergyDifference()) {
					continue pairTriggerLoop;
				} if(pairCutsEnabled[triggerIndex][3 + ENERGY_SLOPE] && !trigger.getStateEnergySlope()) {
					continue pairTriggerLoop;
				} if(pairCutsEnabled[triggerIndex][3 + COPLANARITY] && !trigger.getStateCoplanarity()) {
					continue pairTriggerLoop;
				}
				
				// Add the trigger to the list.
				sspPairsTriggers.get(triggerIndex).add(trigger);
			}
		}
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
		if(start < end) {
			System.out.printf("\tValid Cluster Window   :: [ %3d ns, %3d ns ]%n", start, end);
			performClusterVerification = true;
		} else {
			System.out.println("\tNSB, NSA, and FADC window preclude a valid cluster verification window.");
			System.out.println("\tCluster verification will not be performed!");
			performClusterVerification = false;
		}
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
		int headSpaces = getPrintSpaces(globalStats.getEventCount());
		System.out.println("General Event Statistics:");
		System.out.printf("\tEvent Start Time      :: %.3f s%n", (startTime / Math.pow(10, 9)));
		System.out.printf("\tEvent End Time        :: %.3f%n", (endTime / Math.pow(10, 9)));
		System.out.printf("\tEvent Run Time        :: %.3f%n", ((endTime - startTime) / Math.pow(10, 9)));
		System.out.printf("\tNoise Events          :: %" + headSpaces + "d / %" + headSpaces + "d (%7.3f%%)%n",
				globalStats.getNoiseEvents(), globalStats.getEventCount(), (100.0 * globalStats.getNoiseEvents() / globalStats.getEventCount()));
		System.out.printf("\tCluster Events Failed :: %" + headSpaces + "d / %" + headSpaces + "d (%7.3f%%)%n",
				globalStats.getFailedClusterEventCount(), globalStats.getEventCount(), (100.0 * globalStats.getFailedClusterEventCount() / globalStats.getEventCount()));
		System.out.printf("\tSingles Events Failed :: %" + headSpaces + "d / %" + headSpaces + "d (%7.3f%%)%n",
				globalStats.getFailedSinglesEventCount(), globalStats.getEventCount(), (100.0 * globalStats.getFailedSinglesEventCount() / globalStats.getEventCount()));
		System.out.printf("\tPair Events Failed    :: %" + headSpaces + "d / %" + headSpaces + "d (%7.3f%%)%n",
				globalStats.getFailedPairEventCount(), globalStats.getEventCount(), (100.0 * globalStats.getFailedPairEventCount() / globalStats.getEventCount()));
		
		// Print out how many events reported a given TI type, both in
		// total and hierarchically.
		System.out.println();
		System.out.println("Event Triggering Type Verification:");
		System.out.printf("\t%15s\t%15s\t%15s%n", "Trigger", "Total", "Hierarchical");
		System.out.printf("\t%15s\t%15s\t%15s%n", "Pulser", globalStats.getTriggerStats().getTITriggers(TriggerDiagStats.PULSER, false),
				globalStats.getTriggerStats().getTITriggers(TriggerDiagStats.PULSER, true));
		System.out.printf("\t%15s\t%15s\t%15s%n", "Cosmic", globalStats.getTriggerStats().getTITriggers(TriggerDiagStats.COSMIC, false),
				globalStats.getTriggerStats().getTITriggers(TriggerDiagStats.COSMIC, true));
		System.out.printf("\t%15s\t%15s\t%15s%n", "Singles 1", globalStats.getTriggerStats().getTITriggers(TriggerDiagStats.SINGLES0, false),
				globalStats.getTriggerStats().getTITriggers(TriggerDiagStats.SINGLES0, true));
		System.out.printf("\t%15s\t%15s\t%15s%n", "Singles 2", globalStats.getTriggerStats().getTITriggers(TriggerDiagStats.SINGLES1, false),
				globalStats.getTriggerStats().getTITriggers(TriggerDiagStats.SINGLES1, true));
		System.out.printf("\t%15s\t%15s\t%15s%n", "Pair 1", globalStats.getTriggerStats().getTITriggers(TriggerDiagStats.PAIR0, false),
				globalStats.getTriggerStats().getTITriggers(TriggerDiagStats.PAIR0, true));
		System.out.printf("\t%15s\t%15s\t%15s%n", "Pair 2", globalStats.getTriggerStats().getTITriggers(TriggerDiagStats.PAIR1, false),
				globalStats.getTriggerStats().getTITriggers(TriggerDiagStats.PAIR1, true));
		
		// Print the cluster verification data.
		System.out.println();
		System.out.println("Cluster Verification:");
		System.out.printf("\tRecon Clusters        :: %d%n", globalStats.getClusterStats().getReconClusterCount());
		System.out.printf("\tSSP Clusters          :: %d%n", globalStats.getClusterStats().getSSPClusterCount());
		System.out.printf("\tClusters Matched      :: %d%n", globalStats.getClusterStats().getMatches());
		System.out.printf("\tFailed (Position)     :: %d%n", globalStats.getClusterStats().getPositionFailures());
		System.out.printf("\tFailed (Energy)       :: %d%n", globalStats.getClusterStats().getEnergyFailures());
		System.out.printf("\tFailed (Hit Count)    :: %d%n", globalStats.getClusterStats().getHitCountFailures());
		if(globalStats.getClusterStats().getReconClusterCount() == 0) {
			System.out.printf("\tCluster Efficiency    :: N/A%n");
		} else {
			System.out.printf("\tCluster Efficiency    :: %7.3f%%%n",
					100.0 * globalStats.getClusterStats().getMatches() / globalStats.getClusterStats().getReconClusterCount());
		}
		
		// Print the trigger verification data.
		for(int triggerType = 0; triggerType < 2; triggerType++) {
			// Get the trigger data. Type 0 represents singles triggers.
			TriggerEvent[] triggerData = new TriggerEvent[2];
			if(triggerType == 0) {
				triggerData[0] = globalStats.getTriggerStats().getSingles0Stats();
				triggerData[1] = globalStats.getTriggerStats().getSingles1Stats();
			} else {
				triggerData[0] = globalStats.getTriggerStats().getPair0Stats();
				triggerData[1] = globalStats.getTriggerStats().getPair1Stats();
			}
			
			// Get the basic trigger data.
			int sspSimTriggers = triggerData[0].getSSPSimulatedTriggers() + triggerData[1].getSSPSimulatedTriggers();
			int reconSimTriggers = triggerData[0].getReconSimulatedTriggers() + triggerData[1].getReconSimulatedTriggers();
			int sspReportedTriggers = triggerData[0].getReportedTriggers() + triggerData[1].getReportedTriggers();
			int sspMatchedTriggers = triggerData[0].getMatchedSSPSimulatedTriggers() + triggerData[1].getMatchedSSPSimulatedTriggers();
			int reconMatchedTriggers = triggerData[0].getMatchedReconSimulatedTriggers() + triggerData[1].getMatchedReconSimulatedTriggers();
			
			// Print the basic trigger statistics.
			int spaces = getPrintSpaces(sspSimTriggers, reconSimTriggers, sspReportedTriggers);
			System.out.println();
			if(triggerType == 0) { System.out.println("Singles Trigger Verification:"); }
			else { System.out.println("Pair Trigger Verification:"); }
			System.out.printf("\tSSP Cluster Sim Triggers   :: %" + spaces + "d%n", sspSimTriggers);
			System.out.printf("\tRecon Cluster Sim Triggers :: %" + spaces + "d%n", reconSimTriggers);
			System.out.printf("\tSSP Reported Triggers      :: %" + spaces + "d%n", sspReportedTriggers);
			
			System.out.printf("\tInternal Efficiency        :: %" + spaces + "d / %" + spaces + "d ", sspMatchedTriggers, sspSimTriggers);
			if(sspSimTriggers == 0) { System.out.printf("(N/A)%n"); }
			else { System.out.printf("(%7.3f%%)%n", (100.0 * sspMatchedTriggers / sspSimTriggers)); }
			
			System.out.printf("\tTrigger Efficiency         :: %" + spaces + "d / %" + spaces + "d ", reconMatchedTriggers, reconSimTriggers);
			if(reconSimTriggers == 0) { System.out.printf("(N/A)%n"); }
			else { System.out.printf("(%7.3f%%)%n" , (100.0 * reconMatchedTriggers / reconSimTriggers)); }
			
			// Print the individual cut performances.
			if(triggerType == 0) {
				for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
					// Get the appropriate trigger statistics module.
					TriggerEvent triggerStats;
					if(triggerNum == 0) { triggerStats = globalStats.getTriggerStats().getSingles0Stats(); }
					else { triggerStats = globalStats.getTriggerStats().getSingles1Stats(); }
					
					// Get the number of SSP triggers for this trigger number.
					int sspTriggerCount = triggerStats.getSSPSimulatedTriggers();
					//int sspTriggerCount = triggerRunStats[0].getTotalSSPTriggers(triggerNum);
				
					System.out.println();
					System.out.printf("\tTrigger %d Individual Cut Failure Rate:%n", (triggerNum + 1));
					System.out.printf("\t\tUmatched Triggers          :: %" + spaces + "d%n", triggerStats.getUnmatchedSSPSimulatedTriggers());
					//System.out.printf("\t\tUmatched Triggers          :: %" + spaces + "d%n", triggerRunStats[0].getUnmatchedTriggers(triggerNum));
					if(sspTriggerCount == 0) {
						System.out.printf("\t\tCluster Energy Lower Bound :: %" + spaces + "d / %" + spaces + "d%n",
								triggerStats.getSSPCutFailures(ENERGY_MIN), sspTriggerCount);
						System.out.printf("\t\tCluster Energy Upper Bound :: %" + spaces + "d / %" + spaces + "d%n",
								triggerStats.getSSPCutFailures(ENERGY_MAX), sspTriggerCount);
						System.out.printf("\t\tCluster Hit Count          :: %" + spaces + "d / %" + spaces + "d%n",
								triggerStats.getSSPCutFailures(HIT_COUNT), sspTriggerCount);
					} else {
						System.out.printf("\t\tCluster Energy Lower Bound :: %" + spaces + "d / %" + spaces + "d (%7.3f%%)%n",
								triggerStats.getSSPCutFailures(ENERGY_MIN), sspTriggerCount,
								(100.0 * triggerStats.getSSPCutFailures(ENERGY_MIN) / sspTriggerCount));
						System.out.printf("\t\tCluster Energy Upper Bound :: %" + spaces + "d / %" + spaces + "d (%7.3f%%)%n",
								triggerStats.getSSPCutFailures(ENERGY_MAX), sspTriggerCount,
								(100.0 * triggerStats.getSSPCutFailures(ENERGY_MAX) / sspTriggerCount));
						System.out.printf("\t\tCluster Hit Count          :: %" + spaces + "d / %" + spaces + "d (%7.3f%%)%n",
								triggerStats.getSSPCutFailures(HIT_COUNT), sspTriggerCount,
								(100.0 * triggerStats.getSSPCutFailures(HIT_COUNT) / sspTriggerCount));
					}
				}
			} else {
				for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
					// Get the appropriate trigger statistics module.
					TriggerEvent triggerStats;
					if(triggerNum == 0) { triggerStats = globalStats.getTriggerStats().getPair0Stats(); }
					else { triggerStats = globalStats.getTriggerStats().getPair1Stats(); }
					
					// Get the number of SSP triggers for this trigger number.
					int sspTriggerCount = triggerStats.getSSPSimulatedTriggers();
					
					System.out.println();
					System.out.printf("\tTrigger %d Individual Cut Failure Rate:%n", (triggerNum + 1));
					System.out.printf("\t\tUmatched Triggers          :: %" + spaces + "d%n", triggerStats.getUnmatchedSSPSimulatedTriggers());
					if(sspTriggerCount == 0) {
						System.out.printf("\t\tPair Energy Sum            :: %" + spaces + "d / %" + spaces + "d%n",
								triggerStats.getSSPCutFailures(ENERGY_SUM), sspTriggerCount);
						System.out.printf("\t\tPair Energy Difference     :: %" + spaces + "d / %" + spaces + "d%n",
								triggerStats.getSSPCutFailures(ENERGY_DIFF), sspTriggerCount);
						System.out.printf("\t\tPair Energy Slope          :: %" + spaces + "d / %" + spaces + "d%n",
								triggerStats.getSSPCutFailures(ENERGY_SLOPE), sspTriggerCount);
						System.out.printf("\t\tPair Coplanarity           :: %" + spaces + "d / %" + spaces + "d%n",
								triggerStats.getSSPCutFailures(COPLANARITY), sspTriggerCount);
					} else {
						System.out.printf("\t\tPair Energy Sum            :: %" + spaces + "d / %" + spaces + "d (%7.3f%%)%n",
								triggerStats.getSSPCutFailures(ENERGY_SUM), sspTriggerCount,
								(100.0 * triggerStats.getSSPCutFailures(ENERGY_SUM) / sspTriggerCount));
						System.out.printf("\t\tPair Energy Difference     :: %" + spaces + "d / %" + spaces + "d (%7.3f%%)%n",
								triggerStats.getSSPCutFailures(ENERGY_DIFF), sspTriggerCount,
								(100.0 * triggerStats.getSSPCutFailures(ENERGY_DIFF) / sspTriggerCount));
						System.out.printf("\t\tPair Energy Slope          :: %" + spaces + "d / %" + spaces + "d (%7.3f%%)%n",
								triggerStats.getSSPCutFailures(ENERGY_SLOPE), sspTriggerCount,
								(100.0 * triggerStats.getSSPCutFailures(ENERGY_SLOPE) / sspTriggerCount));
						System.out.printf("\t\tPair Coplanarity           :: %" + spaces + "d / %" + spaces + "d (%7.3f%%)%n",
								triggerStats.getSSPCutFailures(COPLANARITY), sspTriggerCount,
								(100.0 * triggerStats.getSSPCutFailures(COPLANARITY) / sspTriggerCount));
					}
				}
			}
		}
		
		// Print out the trigger efficiency table.
		System.out.println();
		globalStats.getTriggerStats().printEfficiencyTable();
	}
	
	/**
	 * Checks whether all of the hits in a cluster are within the safe
	 * region of the FADC output window.
	 * @param reconCluster - The cluster to check.
	 * @return Returns <code>true</code> if the cluster is safe and
	 * returns <code>false</code> otherwise.
	 */
	private final boolean isVerifiable(Cluster reconCluster) {
		return TriggerDiagnosticUtil.isVerifiable(reconCluster, nsa, nsb, windowWidth);
	}
	
	/**
	 * Generates a <code>List</code> collection that contains a set
	 * of <code>ArrayList</code> collections representing a unique
	 * permutation of the entries in the argument.
	 * @param values - A collection of the entries to be permuted.
	 * @return Returns a list of lists representing the permutations.
	 */
	private static final List<List<Pair<Cluster, SSPCluster>>> getPermutations(List<Cluster> reconClusters, List<SSPCluster> sspClusters) {
		// Store the SSP cluster permutations.
		List<List<SSPCluster>> permList = new ArrayList<List<SSPCluster>>();
		
		// Make sure that the two lists are the same size.
		int reconSize = reconClusters.size();
		int sspSize = sspClusters.size();
		while(sspClusters.size() < reconClusters.size()) {
			sspClusters.add(null);
		}
		while(reconClusters.size() < sspClusters.size()) {
			reconClusters.add(null);
		}
		
		// Get the SSP cluster permutations.
		permute(new ArrayList<SSPCluster>(0), sspClusters, permList);
		
		// Create pairs from the permutations.
		List<List<Pair<Cluster, SSPCluster>>> pairList = new ArrayList<List<Pair<Cluster, SSPCluster>>>();
		for(List<SSPCluster> permutation : permList) {
			List<Pair<Cluster, SSPCluster>> pairs = new ArrayList<Pair<Cluster, SSPCluster>>(reconClusters.size());
			
			for(int clusterIndex = 0; (clusterIndex < reconClusters.size() && clusterIndex < permutation.size()); clusterIndex++) {
				pairs.add(new Pair<Cluster, SSPCluster>(reconClusters.get(clusterIndex), permutation.get(clusterIndex)));
			}
			
			pairList.add(pairs);
		}
		
		// Remove the extra values.
		for(int i = sspClusters.size() - 1; i >= sspSize; i--) { sspClusters.remove(i); }
		for(int i = reconClusters.size() - 1; i >= reconSize; i--) { reconClusters.remove(i); }
		
		// Return the pairs.
		return pairList;
	}
	
	/**
	 * Recursive method for permuting all entries in the argument
	 * collection <code>remainingValues</code> into the argument
	 * <code>permutedValues</code> values. Completed permutations are
	 * placed in the argument <code>permList</code>.
	 * @param permutedValues - List to store entries that have already
	 * been permuted.
	 * @param remainingValues - List to store  entries that need to be
	 * permuted.
	 * @param permList - List to store completed permutations.
	 */
	private static final void permute(List<SSPCluster> permutedValues, List<SSPCluster> remainingValues, List<List<SSPCluster>> permList) {
		// If the list of entries that still need to be sorted is empty,
		// then there is nothing to sort. Just return and empty list.
		if(remainingValues.isEmpty()) { return; }
		
		// If there is only one value left in the list of entries that
		// still need to be sorted, then just add it to the permutation
		// list and return it.
		else if(remainingValues.size() <= 1) {
			// Add the last entry.
			permutedValues.add(remainingValues.get(0));
			
			// Add the permutation to the list of completed permutations.
			permList.add(permutedValues);
		}
		
		// Otherwise, continue to get all possible permutations.
		else {
			// Iterate over the entries that have not been permuted.
			for(int i = 0; i < remainingValues.size(); i++) {
				// Make new lists to contain the permutations.
				List<SSPCluster> newPermList = new ArrayList<SSPCluster>(permutedValues.size() + 1);
				List<SSPCluster> newRemainList = new ArrayList<SSPCluster>(remainingValues.size());
				
				// Copy the current permuted entries to the new list
				// and one value from the list of entries that have
				// not been permuted yet.
				newPermList.addAll(permutedValues);
				newPermList.add(remainingValues.get(i));
				
				// The new list of entries that have not been permuted
				// should be identical, except it should now be missing
				// the entry that was moved.
				for(int index = 0; index < remainingValues.size(); index++) {
					if(index != i) { newRemainList.add(remainingValues.get(index)); }
				}
				
				// Repeat the process with the new lists.
				permute(newPermList, newRemainList, permList);
			}
		}
	}
	
	/**
	 * Compares two cluster matching events and finds the one that has
	 * the better results. Note that this will only return results that
	 * make sense if both of the events represent different permutations
	 * of the same set of clusters. Comparing events with different sets
	 * of clusters will produce meaningless results.
	 * @param firstEvent - The first cluster matching event,
	 * @param secondEvent - The second cluster matching event.
	 * @return Returns the cluster matching event that is better.
	 */
	private static final DetailedClusterEvent getBestPermutation(DetailedClusterEvent firstEvent, DetailedClusterEvent secondEvent) {
		// If both permutations are null, return that.
		if(firstEvent == null && secondEvent == null) {
			return null;
		}
		
		// If one permutation is null, it is not the best.
		if(firstEvent == null) { return secondEvent; }
		else if(secondEvent == null) { return firstEvent; }
		
		// A permutation is better if it has more matches.
		if(firstEvent.getMatches() > secondEvent.getMatches()) { return firstEvent; }
		else if(secondEvent.getMatches() > firstEvent.getMatches()) { return secondEvent; }
		
		// Otherwise, the permutation with the least energy failures is
		// the better permutation.
		if(firstEvent.getEnergyFailures() < secondEvent.getEnergyFailures()) { return firstEvent; }
		else if(secondEvent.getEnergyFailures() < firstEvent.getEnergyFailures()) { return secondEvent; }
		
		// If both these values are the same, then the events are identical.
		return firstEvent;
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
	
	/**
	 * Gets the position of the source of a <code>Trigger</code> object
	 * as text. This method only supports trigger sources of the types
	 * <code>SSPCluster</code>, <code>Cluster</code>, and arrays of size
	 * two of either type.
	 * @param trigger - The trigger from which to obtain the source.
	 * @return Returns the source of the trigger as a <code>String</code>
	 * object.
	 * @throws IllegalArgumentException Occurs if the source of the
	 * trigger is not any of the supported types.
	 */
	private static final String triggerPositionString(Trigger<?> trigger) throws IllegalArgumentException {
		// Get the trigger source.
		Object source = trigger.getTriggerSource();
		
		// Handle valid trigger sources.
		if(source instanceof SSPCluster) {
			return TriggerDiagnosticUtil.clusterPositionString((SSPCluster) source);
		} else if(source instanceof Cluster) {
			return TriggerDiagnosticUtil.clusterPositionString((Cluster) source);
		} else if(source instanceof SSPCluster[]) {
			SSPCluster[] sourcePair = (SSPCluster[]) source;
			if(sourcePair.length == 2) {
				return String.format("%s, %s", TriggerDiagnosticUtil.clusterPositionString(sourcePair[0]),
						TriggerDiagnosticUtil.clusterPositionString(sourcePair[1]));
			}
		} else if(source instanceof Cluster[]) {
			Cluster[] sourcePair = (Cluster[]) source;
			if(sourcePair.length == 2) {
				return String.format("%s, %s", TriggerDiagnosticUtil.clusterPositionString(sourcePair[0]),
						TriggerDiagnosticUtil.clusterPositionString(sourcePair[1]));
			}
		}
		
		// Otherwise, the source type is unrecognized. Throw an error.
		throw new IllegalArgumentException(String.format("Trigger source type \"%s\" is not supported.",
				trigger.getTriggerSource().getClass().getSimpleName()));
	}
	
	/**
	 * Gets the time of a simulated trigger object. Method supports
	 * triggers with source objects of type <code>SSPCluster</code>,
	 * <code>Cluster</code>, and arrays of size two composed of either
	 * object type.
	 * @param trigger - The trigger.
	 * @return Returns the time at which the trigger occurred.
	 * @throws IllegalArgumentException Occurs if the trigger source
	 * is not a supported type.
	 */
	private static final double getTriggerTime(Trigger<?> trigger) throws IllegalArgumentException {
		// Get the trigger source.
		Object source = trigger.getTriggerSource();
		
		// Get the trigger time for supported trigger types.
		if(source instanceof SSPCluster) {
			return ((SSPCluster) source).getTime();
		} else if(source instanceof Cluster) {
			return TriggerDiagnosticUtil.getClusterTime((Cluster) source);
		} else if(source instanceof SSPCluster[]) {
			// Get the pair.
			SSPCluster[] sourcePair = (SSPCluster[]) source;
			
			// Get the time of the bottom cluster.
			if(sourcePair.length == 2) {
				if(sourcePair[0].getYIndex() < 0) { return sourcePair[0].getTime(); }
				else if(sourcePair[1].getYIndex() < 0) { return sourcePair[1].getTime(); }
				else { throw new IllegalArgumentException("Cluster pairs must be formed of a top/bottom pair."); }
			}
			else { throw new IllegalArgumentException("Cluster pairs must be of size 2."); }
		} else if(source instanceof Cluster[]) {
			// Get the pair.
			Cluster[] sourcePair = (Cluster[]) source;
			int[] iy = {
				TriggerDiagnosticUtil.getYIndex(sourcePair[0]),
				TriggerDiagnosticUtil.getYIndex(sourcePair[1])
			};
			
			// Get the time of the bottom cluster.
			if(sourcePair.length == 2) {
				if(iy[0] < 0) { return TriggerDiagnosticUtil.getClusterTime(sourcePair[0]); }
				else if(iy[1] < 0) { return TriggerDiagnosticUtil.getClusterTime(sourcePair[1]); }
				else { throw new IllegalArgumentException("Cluster pairs must be formed of a top/bottom pair."); }
			}
			else { throw new IllegalArgumentException("Cluster pairs must be of size 2."); }
		}
		
		// If the source type is unrecognized, throw an exception.
		throw new IllegalArgumentException(String.format("Trigger source type \"%\" is not supported.",
				source.getClass().getSimpleName()));
	}
	
	/**
	 * Checks if a simulated trigger and an SSP trigger match. Note
	 * that only certain types can be compared. These are:
	 * <ul><li><code>SinglesTrigger<?> --> SSPSinglesTrigger</code></li>
	 * <li><code>PairTrigger<?> --> SSPPairTrigger</code></li></ul>
	 * @param simTrigger - The simulated trigger.
	 * @param sspTrigger - The SSP bank trigger.
	 * @return Returns an array of <code>boolean</code> primitives that
	 * indicate which cuts passed and which failed.
	 */
	private static final boolean[] triggerCutMatch(Trigger<?> simTrigger, SSPTrigger sspTrigger) {
		// Check that the cuts match for supported trigger types.
		if(simTrigger instanceof SinglesTrigger && sspTrigger instanceof SSPSinglesTrigger) {
			// Create an array to store the cut checks.
			boolean[] cutMatch = new boolean[3];
			
			// Cast the triggers.
			SinglesTrigger<?> simSingles = (SinglesTrigger<?>) simTrigger;
			SSPSinglesTrigger sspSingles = (SSPSinglesTrigger) sspTrigger;
			
			// Perform the check.
			cutMatch[ENERGY_MIN] = (simSingles.getStateClusterEnergyLow()  == sspSingles.passCutEnergyMin());
			cutMatch[ENERGY_MAX] = (simSingles.getStateClusterEnergyHigh() == sspSingles.passCutEnergyMax());
			cutMatch[HIT_COUNT] = (simSingles.getStateHitCount()          == sspSingles.passCutHitCount());
			
			// Return the match array.
			return cutMatch;
		} else if(simTrigger instanceof PairTrigger && sspTrigger instanceof SSPPairTrigger) {
			// Create an array to store the cut checks.
			boolean[] cutMatch = new boolean[4];
			
			// Cast the triggers.
			PairTrigger<?> simPair = (PairTrigger<?>) simTrigger;
			SSPPairTrigger sspPair = (SSPPairTrigger) sspTrigger;
			
			// Perform the check.
			cutMatch[ENERGY_SUM] = (simPair.getStateEnergySum()        == sspPair.passCutEnergySum());
			cutMatch[ENERGY_DIFF] = (simPair.getStateEnergyDifference() == sspPair.passCutEnergyDifference());
			cutMatch[ENERGY_SLOPE] = (simPair.getStateEnergySlope()      == sspPair.passCutEnergySlope());
			cutMatch[COPLANARITY] = (simPair.getStateCoplanarity()      == sspPair.passCutCoplanarity());
			
			// Return the match array.
			return cutMatch;
		}
		
		// If this point is reached, the triggers are not of a supported
		// type for cut comparison. Produce an exception.
		throw new IllegalArgumentException(String.format("Triggers of type \"%s\" can not be cut-matched with triggers of type \"%s\".",
				simTrigger.getClass().getSimpleName(), sspTrigger.getClass().getSimpleName()));
	}
}
package org.hps.analysis.trigger;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hps.analysis.trigger.util.ClusterMatchedPair;
import org.hps.analysis.trigger.util.LocalOutputLogger;
import org.hps.analysis.trigger.util.TriggerDiagnosticUtil;
import org.hps.record.daqconfig.ConfigurationManager;
import org.hps.record.daqconfig.DAQConfig;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.SSPCluster;
import org.hps.record.triggerbank.SSPData;
import org.hps.record.triggerbank.TriggerModule;
import org.hps.util.Pair;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * <code>ClusterDiagnosticDriver</code> performs diagnostic checks on
 * run data to verify that clustering is being performed as expected
 * and that there are no errors occurring. It takes GTP clusters created
 * by the software simulation (simulated clusters) and compares them to
 * GTP clusters reported by the hardware (hardware, or SSP, clusters).
 * <br/><br/>
 * Simulated clusters are compared to hardware clusters to find matches.
 * Clusters are considered to match if they share the same crystal
 * indices and time-stamp. Clusters are then "verified" by comparing
 * their energies and hit counts. The thresholds for verification are
 * programmable.
 * <br/><br/>
 * Statistics are produced for how well clusters matched and verified
 * along with diagnostic plots. Additionally, two levels of verbose
 * output may be enabled to provide detailed event readouts for the
 * entire matching and verification process to aid in diagnosing issues.
 * <br/><br/>
 * Certain DAQ settings for the run are required for the diagnostic
 * process. These are obtained via the <code>ConfigurationManager</code>
 * class automatically. Note that a driver to initialize this class
 * must be present in the event chain in order for the diagnostics to
 * initialize and function.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public final class ClusterDiagnosticDriver extends Driver {
    // === Defines cluster verification statistics.
    // ==============================================================
    /** Indicates the number of hardware clusters processed. */
    private int hardwareClusterCount = 0;
    /** Indicates the number of simulated clusters processed. */
    private int simulatedClusterCount = 0;
    /** Indicates the number of hardware clusters processed which were
     * safe from pulse-clipping. */
    private int goodSimulatedClusterCount = 0;
    /** Indicates the number simulated/hardware cluster pairs that were
     * successfully verified. */
    private int matchedClusters = 0;
    /** Indicates the number simulated/hardware cluster pairs that did
     * not verify because no two clusters were found with a matching
     * time-stamp. */
    private int failedMatchTime = 0;
    /** Indicates the number simulated/hardware cluster pairs that did
     * not verify because the matched clusters did not in energy within
     * the bounds set by the energy verification threshold. */
    private int failedMatchEnergy = 0;
    /** Indicates the number simulated/hardware cluster pairs that did
     * not verify because the matched clusters did not in hit count
     * within the bounds set by the hit count verification threshold. */
    private int failedMatchHitCount = 0;
    /** Indicates the number simulated/hardware cluster pairs that did
     * not verify because no two clusters were found with a matching
     * seed position. */
    private int failedMatchPosition = 0;
    /** Indicates the number of failed cluster events that have a seed
     * hit near the seed energy threshold. */
    private int failedNearSeedThreshold = 0;
    /** Indicates the number of failed cluster events that occur within
     * the dead time window of the event start and also have a t = 0
     * cluster with a hit on the same channel. */
    private int failedPositionDeadtime = 0;
    /** Indicates the number of failed cluster events that may have
     * lost a hit due to deadtime issues. */
    private int failedHitCountDeadtime = 0;
    /** Indicates the number of failed cluster events where the energies
     * were identical, but hit count differed. */
    private int failedNegativeEnergyHit = 0;
    // TODO: Add description.
    private int failedCloneBug = 0;
    
    // === Local window values. =========================================================
    // ==================================================================================
    /** Tracks the number of matched clusters in the local window. */
    private int localMatchedClusters = 0;
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
    /** Tracks the number of pulse-clipping-safe simulated clusters in
     * the local window. */
    private int localGoodSimulatedClusterCount = 0;
    /** 
     * Stores entries for the efficiency over time plot. The first entry
     * in the pair represents the time of the local sample. This is
     * defined with respect to the start of the first observed event.
     * The second entry contains the values for the total and matched
     * cluster counts. The first index corresponds to the total and the
     * second index to the matched cluster counts.
     */
    private List<Pair<Long, double[]>> efficiencyPlotEntries = new ArrayList<Pair<Long, double[]>>();
    
    // === Defines programmable driver parameters.
    // ==============================================================
    /** The number of hits required to label an event a "noise event"
     * when <code>skipNoiseEvents</code> is enabled. */
    private int noiseEventThreshold = 100;
    /** The maximum number of hits by which two matched clusters may
     * vary and still be verified. */
    private int hitVerificationThreshold = 0;
    /** The maximum energy by which two matched clusters may vary and
     * still be verified. */
    private double energyVerificationThreshold = 9;
    /** Whether every event should produce a detailed event printout. */
    private boolean verbose = false;
    /** Whether events where at least one cluster fails to verify should
     * produce a detailed event readout. */
    private boolean printOnFail = false;
    /** Whether events with more than <code>noiseEventThreshold</code>
     * hits should be skipped. */
    private boolean skipNoiseEvents = false;
    /** Defines the upper limit of the energy plot x-axes. Units of GeV. */
    private double energyXMax = 2.5;
    /** Defines the bin size used for energy plots. Units of GeV. */
    private double energyBinSize = 0.025;
    /** The LCIO collection containing FADC hits. */
    private String hitCollectionName = "EcalCalHits";
    /** The LCIO collection containing the trigger banks. */
    private String bankCollectionName = "TriggerBank";
    /** The LCIO collection containing GTP clusters. */
    private String clusterCollectionName = "EcalClusters";
    
    // === Defines internal driver variables.
    // ==============================================================
    /** The number of integration samples after a threshold-crossing
     * event used in hit pulse-processing. Used to determining if a
     * hit is at risk of pulse-clipping. */
    private int nsa = -1;
    /** The number of integration samples before a threshold-crossing
     * event used in hit pulse-processing. Used to determining if a
     * hit is at risk of pulse-clipping. */
    private int nsb = -1;
    /** The size of the event window in nanoseconds. Used for time plot
     * ranges and for determining the risk of pulse-clipping. */
    private int windowWidth = -1;
    /** Indicates whether the DAQ configuration has updated before or
     * not. Used to prevent plots from being instantiated more than once. */
    private boolean firstInstantiation = true;
    /** The event logger. Is used to store detailed event readouts, and
     * if appropriate, print them to the terminal. */
    private LocalOutputLogger logger = new LocalOutputLogger();
    
    // === Defines diagnostics plots names and paths. ===============
    // ==============================================================
    /** Directory structure for all clustering diagnostic plots. */
    private static final String MODULE_HEADER = "Trigger Diagnostics/Cluster Verification/";
    /** Plots the time distribution of safe simulated clusters. */
    private static final String softwareClustersTimePlot = MODULE_HEADER + "Software Clusters Event Time Distribution";
    /** Plots the energy distribution of safe simulated clusters. */
    private static final String softwareClustersEnergyPlot = MODULE_HEADER + "Software Clusters Energy Distribution";
    /** Plots the time distribution of matched cluster pairs. */
    private static final String matchedClustersTimePlot = MODULE_HEADER + "Matched Clusters Event Time Distribution";
    /** Plots the energy distribution of matched cluster pairs. */
    private static final String matchedClustersEnergyPlot = MODULE_HEADER + "Matched Clusters Energy Distribution";
    /** Plots the time efficiency of matched cluster pairs. Defined as
     * <code>matchedClustersTimePlot</code> / <code>softwareClustersTimePlot</code>. */
    private static final String matchedClustersTimeEfficiencyPlot = MODULE_HEADER + "Matched Clusters Event Time Efficiency";
    /** Plots the energy efficiency of matched cluster pairs. Defined as
     * <code>softwareClustersEnergyPlot</code> / <code>matchedClustersEnergyPlot</code>. */
    private static final String matchedClustersEnergyEfficiencyPlot = MODULE_HEADER + "Matched Clusters Energy Efficiency";
    /** Plots the difference in energy between matched clusters. */
    private static final String matchedClustersEnergyDiffPlot = MODULE_HEADER + "Matched Clusters Energy Difference Distribution";
    /** Plots the difference in hit count between matched clusters. */
    private static final String matchedClustersHitDiffPlot = MODULE_HEADER + "Matched Clusters Hit Count Difference Distribution";
    /** Plots the difference in energy between matched clusters which failed due to energy. */
    private static final String failedEnergyEnergyDiffPlot = MODULE_HEADER + "Energy-Match Failure Energy Difference Distribution";
    /** Plots the difference in energy between matched clusters which failed due to hit count. */
    private static final String failedHitCountEnergyDiffPlot = MODULE_HEADER + "Hit Count-Match Failure Energy Difference Distribution";
    /** Plots the difference in hit count between matched clusters which failed due to energy. */
    private static final String failedEnergyHitDiffPlot = MODULE_HEADER + "Energy-Match Failure Hit Count Difference Distribution";
    /** Plots the difference in hit count between matched clusters which failed due to hit count. */
    private static final String failedHitCountHitDiffPlot = MODULE_HEADER + "Hit Count-Match Failure Hit Count Difference Distribution";
    /** Plots the 2D difference in energy between matched clusters. */
    private static final String matchedClusters2DEnergyDiffPlot = MODULE_HEADER + "Matched Clusters 2D Energy Difference Distribution";
    /** Plots the 2D difference in hit count between matched clusters. */
    private static final String matchedClusters2DHitDiffPlot = MODULE_HEADER + "Matched Clusters 2D Hit Count Difference Distribution";
    /** Plots the efficiency over the course of the run. **/
    private static final String runtimeEfficiencyPlot = MODULE_HEADER + "Matched Clusters Run Time Efficiency";
    
    private static final Set<Point> getClusterHitIndices(Cluster cluster) {
        // Store the possible hits in a set. The seed hit is excluded.
        Set<Point> hitSet = new HashSet<Point>(8);
        
        // Iterate over the cluster hits and add any existing positions
        // to the hit set.
        for(CalorimeterHit hit : cluster.getCalorimeterHits()) {
            hitSet.add(new Point(hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy")));
        }
        
        // Return the set of constituent hits.
        return hitSet;
    }
    
    private static final Set<Point> getClusterPossibleHitIndices(Cluster cluster) {
        // Get the cluster seed position.
        int ix = TriggerModule.getClusterXIndex(cluster);
        int iy = TriggerModule.getClusterYIndex(cluster);
        
        // Store the possible hits in a set. The seed hit is excluded.
        Set<Point> hitSet = new HashSet<Point>(8);
        
        // Get all eight adjacent hits.
        xLoop:
        for(int xMod = -1; xMod <= 2; xMod++) {
            // Get the modified x position.
            int hix = ix + xMod;
            
            // Values where |x| > 23 do not exist.
            if(Math.abs(hix) > 23) { continue xLoop; }
            
            // Values of x = 0 do not exist. x = 1 and x = -1 are to
            // be treated as adjacent.
            if(hix == 0) { hix = ix == -1 ? 1 : -1; }
            
            yLoop:
            for(int yMod = -1; yMod <= 2; yMod++) {
                // Get the modified y position.
                int hiy = iy + yMod;
                
                // Values of y = 0 and |y| > 5 do not exist.
                if(hiy == 0 || Math.abs(hiy) > 5) { continue yLoop; }
                
                // Add the potential hit position to the set.
                hitSet.add(new Point(hix, hiy));
            }
        }
        
        // Return the set of possible hit positions.
        return hitSet;
    }
    
    private static final double getRatioError(double num, double sigmaNum, double den, double sigmaDen) {
        double ratio = num / den;
        return Math.abs(ratio) * Math.sqrt(Math.pow(sigmaNum / num, 2) + Math.pow(sigmaDen / den, 2));
    }
    
    /**
     * Prints out final run verification statistics and generates
     * efficiency plots.
     */
    @Override
    public void endOfData() {
        // Time and energy binned clustering efficiency plots are defined
        // by the ratio of all verified software clusters to the verified
        // software clusters that were matched.
        AIDA.defaultInstance().histogramFactory().divide(matchedClustersTimeEfficiencyPlot,
                AIDA.defaultInstance().histogram1D(matchedClustersTimePlot), AIDA.defaultInstance().histogram1D(softwareClustersTimePlot));
        AIDA.defaultInstance().histogramFactory().divide(matchedClustersEnergyEfficiencyPlot,
                AIDA.defaultInstance().histogram1D(matchedClustersEnergyPlot), AIDA.defaultInstance().histogram1D(softwareClustersEnergyPlot));
        
        // Calculate errors for efficiencies and failure rates.
        double sigmaSimCount         = Math.sqrt(goodSimulatedClusterCount);
        double sigmaMatched          = Math.sqrt(matchedClusters);
        double sigmaFailedPosition   = Math.sqrt(failedMatchPosition);
        double sigmaFailedTime       = Math.sqrt(failedMatchTime);
        double sigmaFailedHitCount   = Math.sqrt(failedMatchHitCount);
        double sigmaFailedEnergy     = Math.sqrt(failedMatchEnergy);
        double sigmaFailedNearSeed   = Math.sqrt(failedNearSeedThreshold);
        double sigmaFailedDeadtime   = Math.sqrt(failedPositionDeadtime);
        double sigmaFailedNegative   = Math.sqrt(failedNegativeEnergyHit);
        double sigmaFailedHCDeadtime = Math.sqrt(failedHitCountDeadtime);
        double sigmaFailedCloneBug   = Math.sqrt(failedCloneBug);
        
        // Get the maximum number of digits needed to display the
        // largest of the counts. This should always be either the
        // total number hardware or simulated clusters.
        String countDisp = "%" + TriggerDiagnosticUtil.getDigits(Math.max(simulatedClusterCount, goodSimulatedClusterCount)) + "d";
        
        // Print the global run statistics for cluster verification.
        System.out.println("Cluster Verification:");
        System.out.printf("\tSimulated Clusters     :: " + countDisp + "%n", simulatedClusterCount);
        System.out.printf("\tUnclipped Sim Clusters :: " + countDisp + "%n", goodSimulatedClusterCount);
        System.out.printf("\tHardware Clusters      :: " + countDisp + "%n", hardwareClusterCount);
        System.out.printf("\tClusters Matched       :: " + countDisp + "%n", matchedClusters);
        
        System.out.printf("\tFailed (Position)      :: " + countDisp, failedMatchPosition);
        if(failedMatchPosition == 0 || goodSimulatedClusterCount == 0) {
            System.out.printf("   (%7.3f%% ± %7.3f%%)%n", 0.0, 0.0);
        } else {
            System.out.printf("   (%7.3f%% ± %7.3f%%)%n", 100.0 * failedMatchPosition / goodSimulatedClusterCount,
                    getRatioError(failedMatchPosition, sigmaFailedPosition, goodSimulatedClusterCount, sigmaSimCount));
        }
        
        System.out.printf("\t> Failed (Deadtime)    :: " + countDisp, failedPositionDeadtime);
        if(failedPositionDeadtime == 0 || goodSimulatedClusterCount == 0) {
            System.out.printf("   (%7.3f%% ± %7.3f%%)%n", 0.0, 0.0);
        } else {
            System.out.printf("   (%7.3f%% ± %7.3f%%)%n", 100.0 * failedPositionDeadtime / goodSimulatedClusterCount,
                    getRatioError(failedPositionDeadtime, sigmaFailedDeadtime, goodSimulatedClusterCount, sigmaSimCount));
        }
        
        System.out.printf("\t> Failed (Clone Bug)   :: " + countDisp, failedCloneBug);
        if(failedPositionDeadtime == 0 || goodSimulatedClusterCount == 0) {
            System.out.printf("   (%7.3f%% ± %7.3f%%)%n", 0.0, 0.0);
        } else {
            System.out.printf("   (%7.3f%% ± %7.3f%%)%n", 100.0 * failedCloneBug / goodSimulatedClusterCount,
                    getRatioError(failedCloneBug, sigmaFailedCloneBug, goodSimulatedClusterCount, sigmaSimCount));
        }
        
        System.out.printf("\t> Failed (Seed Thresh) :: " + countDisp, failedNearSeedThreshold);
        if(failedNearSeedThreshold == 0 || goodSimulatedClusterCount == 0) {
            System.out.printf("   (%7.3f%% ± %7.3f%%)%n", 0.0, 0.0);
        } else {
            System.out.printf("   (%7.3f%% ± %7.3f%%)%n", 100.0 * failedNearSeedThreshold / goodSimulatedClusterCount,
                    getRatioError(failedNearSeedThreshold, sigmaFailedNearSeed, goodSimulatedClusterCount, sigmaSimCount));
        }
        
        System.out.printf("\tFailed (Time)          :: " + countDisp, failedMatchTime);
        if(failedMatchTime == 0 || goodSimulatedClusterCount == 0) {
            System.out.printf("   (%7.3f%% ± %7.3f%%)%n", 0.0, 0.0);
        } else {
            System.out.printf("   (%7.3f%% ± %7.3f%%)%n", 100.0 * failedMatchTime / goodSimulatedClusterCount,
                    getRatioError(failedMatchTime, sigmaFailedTime, goodSimulatedClusterCount, sigmaSimCount));
        }
        
        System.out.printf("\tFailed (Hit Count)     :: " + countDisp, failedMatchHitCount);
        if(failedMatchHitCount == 0 || goodSimulatedClusterCount == 0) {
            System.out.printf("   (%7.3f%% ± %7.3f%%)%n", 0.0, 0.0);
        } else {
            System.out.printf("   (%7.3f%% ± %7.3f%%)%n", 100.0 * failedMatchHitCount / goodSimulatedClusterCount,
                    getRatioError(failedMatchHitCount, sigmaFailedHitCount, goodSimulatedClusterCount, sigmaSimCount));
        }
        
        System.out.printf("\t> Failed (Deadtime)    :: " + countDisp, failedHitCountDeadtime);
        if(failedPositionDeadtime == 0 || goodSimulatedClusterCount == 0) {
            System.out.printf("   (%7.3f%% ± %7.3f%%)%n", 0.0, 0.0);
        } else {
            System.out.printf("   (%7.3f%% ± %7.3f%%)%n", 100.0 * failedHitCountDeadtime / goodSimulatedClusterCount,
                    getRatioError(failedHitCountDeadtime, sigmaFailedHCDeadtime, goodSimulatedClusterCount, sigmaSimCount));
        }
        
        System.out.printf("\t> Failed (Negative En) :: " + countDisp, failedNegativeEnergyHit);
        if(failedNegativeEnergyHit == 0 || goodSimulatedClusterCount == 0) {
            System.out.printf("   (%7.3f%% ± %7.3f%%)%n", 0.0, 0.0);
        } else {
            System.out.printf("   (%7.3f%% ± %7.3f%%)%n", 100.0 * failedNegativeEnergyHit / goodSimulatedClusterCount,
                    getRatioError(failedNegativeEnergyHit, sigmaFailedNegative, goodSimulatedClusterCount, sigmaSimCount));
        }
        
        System.out.printf("\tFailed (Energy)        :: " + countDisp, failedMatchEnergy);
        if(failedMatchEnergy == 0 || goodSimulatedClusterCount == 0) {
            System.out.printf("   (%7.3f%% ± %7.3f%%)%n", 0.0, 0.0);
        } else {
            System.out.printf("   (%7.3f%% ± %7.3f%%)%n", 100.0 * failedMatchEnergy / goodSimulatedClusterCount,
                    getRatioError(failedMatchEnergy, sigmaFailedEnergy, goodSimulatedClusterCount, sigmaSimCount));
        }
        
        if(simulatedClusterCount == 0 || goodSimulatedClusterCount == 0) {
            System.out.printf("\tCluster Efficiency     :: %7.3f%% ± %7.3f%%%n", 0.0, 0.0);
        } else {
            System.out.printf("\tCluster Efficiency     :: %7.3f%% ± %7.3f%%%n", 100.0 * matchedClusters / goodSimulatedClusterCount,
                    100.0 * getRatioError(matchedClusters, sigmaMatched, goodSimulatedClusterCount, sigmaSimCount));
        }
        
        // Create and populate the efficiency over time plot.
        AIDA.defaultInstance().cloud2D(runtimeEfficiencyPlot, efficiencyPlotEntries.size());
        for(Pair<Long, double[]> entry : efficiencyPlotEntries) {
            // Calculate the value for each type of efficiency.
            double localEfficiency = 100.0 * entry.getSecondElement()[1] / entry.getSecondElement()[0];
            
            // Convert the time to units of seconds.
            long time = Math.round(entry.getFirstElement() / 1000000.0);
            
            // If the value is properly defined, add it to the plot.
            if(!Double.isNaN(localEfficiency)) {
                AIDA.defaultInstance().cloud2D(runtimeEfficiencyPlot).fill(time, localEfficiency);
            }
        }
    }
    
    /**
     * Obtains all necessary collections, performs cluster verification,
     * and outputs event statistics. Also handles logging.
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
        
        // Output the clustering diagnostic header.
        logger.printNewLine(2);
        logger.println("======================================================================");
        logger.println("==== Clustering Diagnostics ==========================================");
        logger.println("======================================================================");
        logger.printf("Event Number :: %d%n", event.getEventNumber());
        logger.printf("Event Time   :: %d%n", event.getTimeStamp());
        
        // Get the needed object collections.
        List<CalorimeterHit> hits = getCalorimeterHits(event, hitCollectionName);
        List<SSPCluster> hardwareClusters = getHardwareClusters(event, bankCollectionName);
        List<Cluster> simulatedClusters = getSimulatedClusters(event, clusterCollectionName);
        
        // Verify that all of the collections are present.
        logger.printNewLine(2);
        logger.printf("Hit Collection Found               :: %b%n", hits != null);
        logger.printf("Hardware Cluster Collection Found  :: %b%n", hardwareClusters != null);
        logger.printf("Simulated Cluster Collection Found :: %b%n", simulatedClusters != null);
        if(hits == null || hardwareClusters == null || simulatedClusters == null) {
            logger.println("TriggerDiagnostics: One or more collections is missing from event. Verification can not proceed and event will be skipped.");
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
        
        // Output the FADC hits, hardware clusters, and simulated
        // clusters to the diagnostic logger.
        logger.printNewLine(2);
        logger.println("==== Event Summary ===================================================");
        logger.println("======================================================================");
        
        // Output the FADC hits.
        logger.println("FADC Hits:");
        for(CalorimeterHit hit : hits) { logger.println("\t" + toString(hit)); }
        if(hits.isEmpty()) { logger.println("\tNone"); }
        
        // Output the simulated clusters from the software.
        logger.printNewLine(1);
        logger.println("Software Clusters:");
        for(Cluster cluster : simulatedClusters) {
            logger.printf("\t%s%n", TriggerDiagnosticUtil.clusterToString(cluster));
            for(CalorimeterHit hit : cluster.getCalorimeterHits()) {
                logger.println("\t\t> " + toString(hit));
            }
        }
        if(simulatedClusters.isEmpty()) { logger.println("\tNone"); }
        
        // Output the reported clusters from the hardware.
        logger.printNewLine(1);
        logger.println("Hardware Clusters:");
        for(SSPCluster cluster : hardwareClusters) {
            logger.printf("\t%s%n", TriggerDiagnosticUtil.clusterToString(cluster));
        }
        if(hardwareClusters.isEmpty()) { logger.println("\tNone"); }
        
        // When hits are written to data by the FADC, the pulse height
        // is only written within the bounds of the event window. Thus,
        // if a hit is too close to the beginning or the end of the
        // event window, it will experience "pulse-clipping" where the
        // hit loses a part of its energy. Clusters containing these
        // hits will often fail verification because the reduced energy
        // despite this not indicating an actual problem. To avoid
        // this, simulated clusters that are at risk of pulse-clipping
        // are excluded from cluster verification.
        logger.printNewLine(2);
        logger.println("==== Pulse-Clipping Verification =====================================");
        logger.println("======================================================================");
        
        // Iterate through each simulated cluster and keep only the
        // clusters safe from pulse-clipping.
        List<Cluster> goodSimulatedClusters = new ArrayList<Cluster>();
        logger.println("Simulated Cluster Pulse-Clipping Check:");
        for(Cluster cluster : simulatedClusters) {
            boolean isSafe = TriggerDiagnosticUtil.isVerifiable(cluster, nsa, nsb, windowWidth);
            if(isSafe) { goodSimulatedClusters.add(cluster); }
            logger.printf("\t%s [ %7s ]%n", TriggerDiagnosticUtil.clusterToString(cluster),
                    isSafe ? "Safe" : "Clipped");
        }
        if(goodSimulatedClusters.isEmpty()) { logger.println("\tNone"); }
        
        // Print the header for cluster verification.
        logger.printNewLine(2);
        logger.println("==== Cluster Accuracy Verification ===================================");
        logger.println("======================================================================");
        
        // Generate a list of matched simulated/hardware cluster pairs
        // and the verification status of each pair.
        List<ClusterMatchedPair> matchedPairs = matchSimulatedToHardware(goodSimulatedClusters, hardwareClusters,
                energyVerificationThreshold, hitVerificationThreshold, logger);
        
        // Increment the number of clusters of each type processed.
        hardwareClusterCount += hardwareClusters.size();
        simulatedClusterCount += simulatedClusters.size();
        goodSimulatedClusterCount += goodSimulatedClusters.size();
        localGoodSimulatedClusterCount += goodSimulatedClusters.size();
        
        // Iterate over the list of pairs and extract statistical data
        // for this set of clusters.
        int matchedClusters = 0;
        int failedMatchTime = 0;
        int failedMatchEnergy = 0;
        int failedMatchHitCount = 0;
        int failedMatchPosition = 0;
        for(ClusterMatchedPair pair : matchedPairs) {
            if(pair.isMatch()) { matchedClusters++; }
            if(pair.isTimeFailState()) { failedMatchTime++; }
            if(pair.isEnergyFailState()) { failedMatchEnergy++; }
            if(pair.isHitCountFailState()) {
                // Increment the general hit count fail state count.
                failedMatchHitCount++;
                
                // If the simulated cluster is different in hit count,
                // but has the same energy, it is likely caused by a
                // near-zero energy hit that registered as negative to
                // the simulation, and was consequently ignored.
                if(Math.abs(TriggerModule.getValueClusterTotalEnergy(pair.getReconstructedCluster())
                        - TriggerModule.getValueClusterTotalEnergy(pair.getSSPCluster())) <= energyVerificationThreshold) {
                    failedNegativeEnergyHit++;
                }
                
                // Deadtime can also cause a failure in hit count. A
                // simple check of this is to see if either the cluster
                // occurred within 32 ns of the event start, or if any
                // channel that would be included by the cluster for
                // which there is no hit has a hit within this time
                // frame that is also within 32 ns of the cluster.
                else {
                    // Check for the case that the cluster itself is
                    // within the deadtime uncertainty region.
                    if(TriggerModule.getClusterTime(pair.getReconstructedCluster()) < 32) {
                        failedHitCountDeadtime++;
                    }
                    
                    // Otherwise, determine which hits are not included
                    // in the cluster and see if a hit exists which could
                    // produce a deadtime error in that channel.
                    else {
                        // Get a list of the hits that could exist in
                        // the cluster.
                        Set<Point> actualHits = getClusterHitIndices(pair.getReconstructedCluster());
                        Set<Point> possibleHits = getClusterPossibleHitIndices(pair.getReconstructedCluster());
                        
                        // Iterate over each hit and look for one that
                        // could produce a deadtime error.
                        hitLoop:
                        for(CalorimeterHit hit : hits) {
                            // The hit is required to be within the
                            // first 32 of the event.
                            if(hit.getTime() >= 32) { continue hitLoop; }
                            
                            // The hit must be within 32 ns of the
                            // cluster  time and also occur before
                            // the cluster time or simultaneously.
                            if((TriggerModule.getClusterTime(pair.getReconstructedCluster()) < hit.getTime()) 
                                    || (TriggerModule.getClusterTime(pair.getReconstructedCluster()) - hit.getTime() >= 32)) {
                                continue hitLoop;
                            }
                            
                            // The hit must occur on a channel that
                            // falls within the 3x3 cluster range, and
                            // there must not already be a hit on that
                            // channel in the cluster.
                            Point ixy = new Point(hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"));
                            if(possibleHits.contains(ixy) && !actualHits.contains(ixy)) {
                                failedHitCountDeadtime++;
                                break hitLoop;
                            }
                        }
                    }
                }
            }
            if(pair.isPositionFailState()) {
                // Increment the general position fail state count.
                failedMatchPosition++;
                
                // If the simulated cluster is within the allowed energy
                // range of the seed energy threshold, it is likely to
                // have failed because of energy differences between the
                // hardware and the simulation. Track these.
                double energyDifference = Math.abs(TriggerModule.getValueClusterTotalEnergy(pair.getReconstructedCluster())
                        - ConfigurationManager.getInstance().getGTPConfig().getSeedEnergyCutConfig().getLowerBound());
                if(energyDifference <= energyVerificationThreshold) {
                    failedNearSeedThreshold++;
                }
                
                // If the simulated cluster has a seed hit which is
                // less than 32 ns in time, it is possible that it was
                // missed by the hardware due to a dead time issue.
                // This is most likely to the case for any cluster where
                // there exists a t = 0 cluster with a hit on the same
                // channel. Check for this case and track the instances
                // where it occurred.
                else if(TriggerModule.getClusterTime(pair.getReconstructedCluster()) < 32) {
                    // Get the cluster channel.
                    int ix = TriggerModule.getClusterXIndex(pair.getReconstructedCluster());
                    int iy = TriggerModule.getClusterYIndex(pair.getReconstructedCluster());
                    
                    // Check for a t = 0 cluster with a hit on the same
                    // channel.
                    deadtimeLoop:
                    for(Cluster cluster : simulatedClusters) {
                        if(TriggerModule.getClusterTime(cluster) == 0) {
                            if(TriggerModule.getClusterXIndex(cluster) == ix && TriggerModule.getClusterYIndex(cluster) == iy) {
                                failedPositionDeadtime++;
                                break deadtimeLoop;
                            }
                        }
                    }
                }
                
                // Check for more complicated fail state causes.
                else {
                    // The "clone bug" occurs when the hardware reports
                    // a fake cluster at the same position as an existing
                    // cluster, but at the same time as a cluster which
                    // should exist, but is missed.
                    // Check if more than SSP cluster exists at the
                    // same position as another cluster, and one of them
                    // matches in time.
                    double time = TriggerModule.getClusterTime(pair.getReconstructedCluster());
                    Map<Point, Integer> countMap = new HashMap<Point, Integer>();
                    Map<Point, SSPCluster> timeMap = new HashMap<Point, SSPCluster>();
                    for(SSPCluster sspCluster : hardwareClusters) {
                        Point ixy = new Point(TriggerModule.getClusterXIndex(sspCluster), TriggerModule.getClusterYIndex(sspCluster));
                        if(countMap.containsKey(ixy)) { countMap.put(ixy, countMap.get(ixy) + 1); }
                        else { countMap.put(ixy, new Integer(1)); }
                        if(TriggerModule.getClusterTime(sspCluster) == time) { timeMap.put(ixy, sspCluster); }
                    }
                    
                    // Check each cluster position. If there are more
                    // clusters than one at that position, and one of
                    // them has the same time as the position failure
                    // cluster, the clone bug could have occurred.
                    unmatchedLoop:
                    for(Map.Entry<Point, Integer> entry : countMap.entrySet()) {
                        // Check that at least two clusters exist at
                        // this position and that one of them matched
                        // the fail state cluster in time.
                        if(entry.getValue() >= 2 && timeMap.containsKey(entry.getKey())) {
                            // Get the matching cluster.
                            SSPCluster cloneTestCluster = timeMap.get(entry.getKey());
                            
                            // The cluster that matched the fail state
                            // cluster in time must be itself unmatched.
                            for(ClusterMatchedPair match : matchedPairs) {
                                // If the cluster matching the fail state
                                // cluster in time is matched, this is
                                // not the clone bug.
                                if(match.getSSPCluster() == cloneTestCluster) {
                                    continue unmatchedLoop;
                                }
                            }
                            
                            // If not match was found for the cluster
                            // that matches the fail state cluster in
                            // time, this is probably the clone bug.
                            failedCloneBug++;
                            break unmatchedLoop;
                        }
                    }
                }
            }
        }
        
        // Increment the statistics.
        this.matchedClusters += matchedClusters;
        localMatchedClusters += matchedClusters;
        this.failedMatchTime += failedMatchTime;
        this.failedMatchEnergy += failedMatchEnergy;
        this.failedMatchHitCount += failedMatchHitCount;
        this.failedMatchPosition += failedMatchPosition;
        
        // Output the results summary header.
        logger.printNewLine(2);
        logger.println("==== Cluster Verification Summary ====================================");
        logger.println("======================================================================");
        
        // Output the cluster pairs that successfully verified.
        logger.println();
        logger.println("Verified Simulated/Hardware Cluster Pairs:");
        if(matchedClusters != 0) {
            for(ClusterMatchedPair pair : matchedPairs) {
                if(pair.isMatch()) {
                    logger.printf("\t%s --> %s%n",
                            TriggerDiagnosticUtil.clusterToString(pair.getReconstructedCluster()),
                            TriggerDiagnosticUtil.clusterToString(pair.getSSPCluster()));
                }
            }
        } else { logger.println("\tNone"); }
        
        // Output the event statistics to the diagnostics logger.
        logger.println();
        logger.println("Event Statistics:");
        logger.printf("\tSafe Clusters      :: %d%n", goodSimulatedClusters.size());
        logger.printf("\tClusters Matched   :: %d%n", matchedClusters);
        logger.printf("\tFailed (Position)  :: %d%n", failedMatchPosition);
        logger.printf("\tFailed (Time)      :: %d%n", failedMatchTime);
        logger.printf("\tFailed (Energy)    :: %d%n", failedMatchEnergy);
        logger.printf("\tFailed (Hit Count) :: %d%n", failedMatchHitCount);
        if(goodSimulatedClusters.isEmpty()) {
            logger.printf("\tCluster Efficiency :: N/A %n", 100.0 * matchedClusters / goodSimulatedClusters.size());
        } else {
            logger.printf("\tCluster Efficiency :: %3.0f%%%n", 100.0 * matchedClusters / goodSimulatedClusters.size());
        }
        
        // The verification process is considered to fail when any not
        // pulse-clipped simulated cluster fails to verify. If it is
        // appropriate to the verbosity settings, print the event log.
        if(verbose || (printOnFail && matchedPairs.size() != matchedClusters)) {
            logger.printLog();
        }
        
        // If the local time window has elapsed, update the efficiency
        // plot values and reset the counters.
        if(localEndTime - localStartTime >= localWindowSize) {
            // Values are stored in an array within a list. Each array
            // index corresponds to specific value and each array entry
            // corresponds to an individual local window.
            double[] databank = new double[2];
            databank[0] = localGoodSimulatedClusterCount;
            databank[1] = localMatchedClusters;
            
            // The efficiency list also stores the time that corresponds
            // to the particular databank. This is defined as half way
            // between the start and end time of the current window.
            Long time = localEndTime - firstStartTime - ((localEndTime - localStartTime) / 2);
            
            // Store the databank in the list.
            efficiencyPlotEntries.add(new Pair<Long, double[]>(time, databank));
            
            // Reset all of the counters.
            localMatchedClusters = 0;
            localGoodSimulatedClusterCount = 0;
            
            // The new window start time is the current event time.
            localStartTime = event.getTimeStamp();
        }
    }
    
    /**
     * Connects the driver to the DAQ configuration manager and updates
     * DAQ settings as they become available.
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
                nsa = daq.getFADCConfig().getNSA();
                nsb = daq.getFADCConfig().getNSB();
                windowWidth = daq.getFADCConfig().getWindowWidth();
                
                // If this is the first time DAQ information is received,
                // also instantiate the plots.
                if(firstInstantiation) {
                    firstInstantiation = false;
                    instantiatePlots();
                }
            }
        });
    }
    
    /**
     * Instantiates the diagnostics plots. method is run once upon DAQ
     * configuration initialization.
     */
    private void instantiatePlots() {
        // Instantiate the results event time plots. Event time is
        // defined in terms of the window width and is binned in units
        // of clock-cycles.
        AIDA.defaultInstance().histogram1D(softwareClustersTimePlot, windowWidth / 4, 0, windowWidth);
        AIDA.defaultInstance().histogram1D(matchedClustersTimePlot, windowWidth / 4, 0, windowWidth);
        AIDA.defaultInstance().histogram1D(matchedClustersTimeEfficiencyPlot, windowWidth / 4, 0, windowWidth);
        
        // Instantiate the results energy plots. The energy maximum is
        // defined by the user and is binned in units of 25 MeV.
        int bins = (int) Math.ceil(energyXMax / energyBinSize);
        double correctedEnergyMax = energyBinSize * bins;
        AIDA.defaultInstance().histogram1D(softwareClustersEnergyPlot, bins, 0, correctedEnergyMax);
        AIDA.defaultInstance().histogram1D(matchedClustersEnergyPlot, bins, 0, correctedEnergyMax);
        AIDA.defaultInstance().histogram1D(matchedClustersEnergyEfficiencyPlot, bins, 0, correctedEnergyMax);
        
        // The hit count comparison plots are binned by individual hit
        // and run from 0 to 9 hits.
        AIDA.defaultInstance().histogram1D(failedEnergyHitDiffPlot, 10, -0.5, 9.5);
        AIDA.defaultInstance().histogram1D(failedHitCountHitDiffPlot, 10, -0.5, 9.5);
        AIDA.defaultInstance().histogram1D(matchedClustersHitDiffPlot, 10, -0.5, 9.5);
        AIDA.defaultInstance().histogram2D(matchedClusters2DHitDiffPlot, 10, -0.5, 9.5, 10, -0.5, 9.5);
        
        // The energy difference plots are binned on a reduced energy
        // scale, as hits typically are close in energy.
        AIDA.defaultInstance().histogram1D(failedEnergyEnergyDiffPlot, 67, 0, 0.201);
        AIDA.defaultInstance().histogram1D(failedHitCountEnergyDiffPlot, 67, 0, 0.201);
        AIDA.defaultInstance().histogram1D(matchedClustersEnergyDiffPlot, 67, 0, 0.201);
        AIDA.defaultInstance().histogram2D(matchedClusters2DEnergyDiffPlot, 34, 0, 0.102, 34, 0, 0.102);
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
     * Gets the a of <code>SSPCluster</code> objects from the argument
     * event from the SSP bank contained in the LCIO collection
     * <code>bankCollectionName</code>.
     * @param event - The event object.
     * @param bankCollectionName - The name of the bank collection
     * containing the SSP bank, which contains the cluster list.
     * @return Returns either a list of <code>SSPCluster</code> objects,
     * or returns <code>null</code> if no bank collection or no SSP bank
     * was found.
     */
    private static final List<SSPCluster> getHardwareClusters(EventHeader event, String bankCollectionName) {
        // The SSP bank contains all SSP clusters. Check that the event
        // contains the bank collection.
        if(event.hasCollection(GenericObject.class, bankCollectionName)) {
            // If the bank collection is present, search for the SSP
            // bank specifically.
            List<GenericObject> bankList = event.get(GenericObject.class, bankCollectionName);
            for(GenericObject obj : bankList) {
                if(AbstractIntData.getTag(obj) == SSPData.BANK_TAG) {
                    return (new SSPData(obj)).getClusters();
                }
            }
        }
        
        // If either the bank collection is not present, or no SSP bank
        // was located, then there are no SSP cluster present.
        return null;
    }
    
    /**
     * Gets the a of <code>Cluster</code> objects from the argument
     * event from the LCIO collection <code>clusterCollectionName</code>.
     * @param event - The event object.
     * @param clusterCollectionName - The name of the cluster collection.
     * @return Returns either a list of <code>Cluster</code> objects, or
     * returns <code>null</code> if no collection was found.
     */
    private static final List<Cluster> getSimulatedClusters(EventHeader event, String clusterCollectionName) {
        // Get the list of simulated clusters from the event.
        List<Cluster> simulatedClusters = null;
        if(event.hasCollection(Cluster.class, clusterCollectionName)) {
            simulatedClusters = event.get(Cluster.class, clusterCollectionName);
        }
        
        // Return the simulated cluster collection.
        return simulatedClusters;
    }
    
    /**
     * Performs the verification check for cluster hit counts.
     * @param simCluster - The simulated cluster to check.
     * @param sspCluster - The hardware cluster to check.
     * @param hitWindow - The range by which hit counts are allowed to
     * differ between the clusters.
     * @return Returns <code>true</code> if the hit counts match to
     * within threshold and <code>false</code> otherwise.
     */
    private static final boolean isHitMatch(Cluster simCluster, SSPCluster sspCluster, int hitWindow) {
        // Get the hit counts for both clusters.
        double simHitCount = TriggerModule.getClusterHitCount(simCluster);
        double sspHitCount = TriggerModule.getClusterHitCount(sspCluster);
        
        // The hardware does not store cluster hit counts as higher than
        // 7, so if the software hit count is 8 or 9, we must treat it
        // as 7 instead to match this behavior.
        if(simHitCount > 7) { simHitCount = 7; }
        
        // Perform the hit count check.
        return (sspHitCount >= simHitCount - hitWindow && sspHitCount <= simHitCount + hitWindow);
    }
    
    /**
     * Performs cluster matching between a collection of simulated
     * clusters and a collection of hardware clusters using the strictly
     * time-compliant algorithm. Simulated clusters are matched with
     * a hardware cluster by comparing the x- and y-indices of the two
     * clusters, as well as their time-stamps. If all of these values
     * match, the clusters are considered to be the same. The cluster
     * then undergoes verification by requiring that both the cluster
     * energies and hit counts are within a certain programmable range
     * of one another. Matched clusters are then stored along with a
     * flag that indicates whether they were properly verified or not.
     * Simulated clusters that do not match any hardware cluster in both
     * position and time are treated as failing to have verified.
     * @param simulatedClusters - A collection of GTP clusters generated
     * by the software simulation.
     * @param hardwareClusters - A collection of GTP clusters reported
     * in the SSP bank by the hardware.
     * @param energyWindow - The window of allowed deviation between
     * the simulated cluster and hardware cluster energies. Units are
     * in GeV.
     * @param hitWindow - The window of allowed deviation between
     * the simulated cluster and hardware cluster hit counts.
     * @param logger - The logger object to which diagnostic text should
     * be written.
     * @return Returns a <code>List</code> containing all the matched
     * simulated/hardware cluster pairs as well as their verification
     * statuses.
     */
    private static final List<ClusterMatchedPair> matchSimulatedToHardware(Collection<Cluster> simulatedClusters,
            Collection<SSPCluster> hardwareClusters, double energyWindow, int hitWindow, LocalOutputLogger logger) {
        // Store the list of clusters, along with their matches (if
        // applicable) and their pair verification status.
        List<ClusterMatchedPair> pairList = new ArrayList<ClusterMatchedPair>();
        
        // Store the clusters which have been successfully paired.
        Set<SSPCluster> hardwareMatched = new HashSet<SSPCluster>(hardwareClusters.size());
        
        // Find simulated/hardware cluster matched pairs.
        softwareLoop:
        for(Cluster simCluster : simulatedClusters) {
            // Increment the plot counts for simulated clusters.
            AIDA.defaultInstance().histogram1D(softwareClustersTimePlot).fill(TriggerModule.getClusterTime(simCluster));
            AIDA.defaultInstance().histogram1D(softwareClustersEnergyPlot).fill(TriggerModule.getValueClusterTotalEnergy(simCluster));
            
            // Track whether a position-matched cluster was found.
            boolean matchedPosition = false;
            
            // VERBOSE :: Output the cluster being matched.
            logger.printf("Considering %s%n", TriggerDiagnosticUtil.clusterToString(simCluster));
            
            // Search through the hardware clusters for a match.
            hardwareLoop:
            for(SSPCluster hardwareCluster : hardwareClusters) {
                // VERBOSE :: Output the hardware cluster being considered.
                logger.printf("\t%s ", TriggerDiagnosticUtil.clusterToString(hardwareCluster));
                
                // Clusters must be matched in a one-to-one relationship,
                // so clusters that have already been matched should
                // be skipped.
                if(hardwareMatched.contains(hardwareCluster)) {
                    logger.printf("[ %7s; %9s ]%n", "fail", "matched");
                    continue hardwareLoop;
                }
                
                // If the clusters are the same, they must have the same
                // x- and y-indices. 
                if(TriggerModule.getClusterXIndex(simCluster) != TriggerModule.getClusterXIndex(hardwareCluster)
                        || TriggerModule.getClusterYIndex(simCluster) != TriggerModule.getClusterYIndex(hardwareCluster)) {
                    logger.printf("[ %7s; %9s ]%n", "fail", "position");
                    continue hardwareLoop;
                }
                
                // Note that the cluster matched another cluster in
                // position. This is used to determine why a cluster
                // failed to verify, if necessary.
                matchedPosition = true;
                
                // If the clusters are the same, they should occur at
                // the same time as well.
                if(TriggerModule.getClusterTime(simCluster) != TriggerModule.getClusterTime(hardwareCluster)) {
                    logger.printf("[ %7s; %9s ]%n", "fail", "time");
                    continue hardwareLoop;
                }
                
                // It is impossible for two clusters to exist at the
                // same place and the same time, so clusters that pass
                // both the time comparison and position comparison are
                // assumed to be the same.
                hardwareMatched.add(hardwareCluster);
                
                // Plot a comparison of the two clusters' energies and
                // hit counts.
                AIDA.defaultInstance().histogram1D(matchedClustersHitDiffPlot).fill(Math.abs(TriggerModule.getClusterHitCount(simCluster)
                        - TriggerModule.getClusterHitCount(hardwareCluster)));
                AIDA.defaultInstance().histogram2D(matchedClusters2DHitDiffPlot).fill(TriggerModule.getClusterHitCount(simCluster),
                        TriggerModule.getClusterHitCount(hardwareCluster));
                AIDA.defaultInstance().histogram1D(matchedClustersEnergyDiffPlot).fill(Math.abs(TriggerModule.getValueClusterTotalEnergy(simCluster)
                        - TriggerModule.getValueClusterTotalEnergy(hardwareCluster)));
                AIDA.defaultInstance().histogram2D(matchedClusters2DEnergyDiffPlot).fill(TriggerModule.getValueClusterTotalEnergy(simCluster),
                        TriggerModule.getValueClusterTotalEnergy(hardwareCluster));
                
                // While time and position matched clusters are considered
                // to be the same cluster, the clusters must have similar
                // energies and hit counts to be properly verified. First
                // perform the hit count check. The hardware cluster must
                // match the simulated cluster hit count to within a given
                // bound.
                //if(TriggerModule.getClusterHitCount(hardwareCluster) >= TriggerModule.getClusterHitCount(simCluster) - hitWindow &&
                //        TriggerModule.getClusterHitCount(hardwareCluster) <= TriggerModule.getClusterHitCount(simCluster) + hitWindow) {
                if(isHitMatch(simCluster, hardwareCluster, hitWindow)) {
                    // Next, check that the hardware cluster matches the
                    // simulated cluster in energy to within a given
                    // bound.
                    if(TriggerModule.getValueClusterTotalEnergy(hardwareCluster) >= TriggerModule.getValueClusterTotalEnergy(simCluster) - energyWindow
                            && TriggerModule.getValueClusterTotalEnergy(hardwareCluster) <= TriggerModule.getValueClusterTotalEnergy(simCluster) + energyWindow) {
                        // The cluster is a match.
                        pairList.add(new ClusterMatchedPair(simCluster, hardwareCluster, ClusterMatchedPair.CLUSTER_STATE_MATCHED));
                        logger.printf("[ %7s; %9s ]%n", "success", "matched");
                        
                        // Increment the matched cluster plots.
                        AIDA.defaultInstance().histogram1D(matchedClustersTimePlot).fill(TriggerModule.getClusterTime(simCluster));
                        AIDA.defaultInstance().histogram1D(matchedClustersEnergyPlot).fill(TriggerModule.getValueClusterTotalEnergy(simCluster));
                        
                        // Since the cluster is already matched, there
                        // is no need to perform further checks.
                        continue softwareLoop;
                    }
                    
                    // If the energies of the two clusters are not
                    // sufficiently close, the clusters fail to verify.
                    else {
                        pairList.add(new ClusterMatchedPair(simCluster, hardwareCluster, ClusterMatchedPair.CLUSTER_STATE_FAIL_ENERGY));
                        logger.printf("[ %7s; %9s ]%n", "fail", "energy");
                        AIDA.defaultInstance().histogram1D(failedEnergyHitDiffPlot).fill(Math.abs(TriggerModule.getClusterHitCount(simCluster)
                                - TriggerModule.getClusterHitCount(hardwareCluster)));
                        AIDA.defaultInstance().histogram1D(failedEnergyEnergyDiffPlot).fill(Math.abs(TriggerModule.getValueClusterTotalEnergy(simCluster)
                                - TriggerModule.getValueClusterTotalEnergy(hardwareCluster)));
                        continue softwareLoop;
                    } // End energy check.
                }
                
                // If the hit counts of the two clusters are not
                // sufficiently close, the clusters fail to verify.
                else {
                    pairList.add(new ClusterMatchedPair(simCluster, hardwareCluster, ClusterMatchedPair.CLUSTER_STATE_FAIL_HIT_COUNT));
                    logger.printf("[ %7s; %9s ]%n", "fail", "hit count");
                    AIDA.defaultInstance().histogram1D(failedHitCountHitDiffPlot).fill(Math.abs(TriggerModule.getClusterHitCount(simCluster)
                            - TriggerModule.getClusterHitCount(hardwareCluster)));
                    AIDA.defaultInstance().histogram1D(failedHitCountEnergyDiffPlot).fill(Math.abs(TriggerModule.getValueClusterTotalEnergy(simCluster)
                            - TriggerModule.getValueClusterTotalEnergy(hardwareCluster)));
                    continue softwareLoop;
                } // End hit count check.
            } // End hardware loop.
            
            // This point may only be reached if a cluster failed to
            // match with another cluster in either time or position.
            // Check whether a cluster at the same position was found.
            // If it was not, then the cluster fails to verify by reason
            // of position.
            if(!matchedPosition) {
                pairList.add(new ClusterMatchedPair(simCluster, null, ClusterMatchedPair.CLUSTER_STATE_FAIL_POSITION));
            }
            
            // Otherwise, the cluster had a potential match found at
            // the same position, but the time-stamps did not align.
            else {
                pairList.add(new ClusterMatchedPair(simCluster, null, ClusterMatchedPair.CLUSTER_STATE_FAIL_TIME));
            }
        } // End sim loop.
        
        // Return the list of clusters, their matches, and their
        // verification states.
        return pairList;
    }
    
    /**
     * Converts a <code>CalorimeterHit</code> to a basic descriptive
     * string.
     * @param hit - The hit to be converted.
     * @return Returns the hit object as a <code>String</code>.
     */
    private static final String toString(CalorimeterHit hit) {
        if(hit == null) { return "Invalid hit."; }
        else {
            return String.format("Hit at (%3d, %3d) with %7.3f GeV at time %3.0f ns",
                    hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"),
                    hit.getCorrectedEnergy(), hit.getTime());
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
     * Sets the name of LCIO collection containing simulated GTP clusters.
     * @param collection - The collection name.
     */
    public void setClusterCollectionName(String collection) {
        clusterCollectionName = collection;
    }
    
    /**
     * Sets the size of the bins in energy plots.
     * @param binSize - The bin width in units of GeV.
     */
    public void setEnergyAxisBinSize(double binSize) {
        energyBinSize = binSize;
    }
    
    /**
     * Sets the maximum value of the x-axis for energy plots.
     * @param xMax - The x-axis maximum, in units of GeV.
     */
    public void setEnergyAxisMaximum(double xMax) {
        energyXMax = xMax;
    }
    
    /**
     * Sets the maximum cluster energy by which a matched cluster pair
     * may vary and still verify.
     * @param thresold - The maximum difference in energy. Value is
     * inclusive and in units of GeV.
     */
    public void setEnergyVerificationThreshold(double thresold) {
        energyVerificationThreshold = thresold;
    }
    
    /**
     * Sets the name of LCIO collection containing hits.
     * @param collection - The collection name.
     */
    public void setHitCollectionName(String collection) {
        hitCollectionName = collection;
    }
    
    /**
     * Sets the maximum number of hits by which a matched cluster pair
     * may vary and still verify.
     * @param thresold - The maximum difference in hits. Value is
     * inclusive.
     */
    public void setHitVerificationThreshold(int thresold) {
        hitVerificationThreshold = thresold;
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
     * When enabled, events where at least one cluster which is not
     * pulse-clipped will print a full event summary.
     * @param state - <code>true</code> prints the event summary and
     * <code>false</code> does not.
     */
    public void setPrintOnVerificationFailure(boolean state) {
        printOnFail = state;
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
     * Sets whether the full event verification summary should be printed
     * on every event or not.
     * @param state - <code>true</code> prints the event summary and
     * <code>false</code> does not.
     */
    public void setVerbose(boolean state) {
        verbose = state;
    }
}
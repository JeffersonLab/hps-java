package org.hps.analysis.trigger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hps.analysis.trigger.data.ClusterMatchedPair;
import org.hps.analysis.trigger.util.OutputLogger;
import org.hps.analysis.trigger.util.TriggerDiagnosticUtil;
import org.hps.record.triggerbank.SSPCluster;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;

/**
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class ClusterDiagnosticModule {
    /** Indicates the number of hardware clusters processed. */
    int hardwareClusterCount = 0;
    /** Indicates the number of simulated clusters processed. */
    int simulatedClusterCount = 0;
    /** Indicates the number of hardware clusters that were not at risk
     * of pulse-clipping which were processed. */
    int goodSimulatedClusterCount = 0;
    /** Indicates the number simulated/hardware cluster pairs that were
     * successfully verified. */
    int matchedClusters = 0;
    /** Indicates the number simulated/hardware cluster pairs that did
     * not verify because no two clusters were found with a matching
     * time-stamp. */
    int failedMatchTime = 0;
    /** Indicates the number simulated/hardware cluster pairs that did
     * not verify because no two clusters were found with energies within
     * the allowed bounds of one another. */
    int failedMatchEnergy = 0;
    /** Indicates the number simulated/hardware cluster pairs that did
     * not verify because no two clusters were found with hit counts
     * within the allowed bounds of one another. */
    int failedMatchHitCount = 0;
    /** Indicates the number simulated/hardware cluster pairs that did
     * not verify because no two clusters were found with a matching
     * seed position. */
    int failedMatchPosition = 0;
    
    /** Indicates whether or not all of the simulated clusters defined
     * in the object (excluding those at risk of pulse-clipping) were
     * successfully verified or not. A value of <code>true</code> means
     * that all applicable clusters were verified, and <code>false</code>
     * that at least one was not.*/
    boolean clusterFail = false;
    
    /**
     * 
     * @param hits
     * @param hardwareClusters
     * @param simulatedClusters
     * @param nsa
     * @param nsb
     * @param windowWidth
     * @param hitVerificationThreshold
     * @param energyVerificationThreshold
     */
    ClusterDiagnosticModule(List<CalorimeterHit> hits, List<SSPCluster> hardwareClusters, List<Cluster> simulatedClusters,
            int nsa, int nsb, int windowWidth, int hitVerificationThreshold, double energyVerificationThreshold) {
        // Output the clustering diagnostic header.
        OutputLogger.printNewLine(2);
        OutputLogger.println("======================================================================");
        OutputLogger.println("==== Clustering Diagnostics ==========================================");
        OutputLogger.println("======================================================================");
        
        // Output the FADC hits, hardware clusters, and simulated
        // clusters to the diagnostic logger.
        OutputLogger.printNewLine(2);
        OutputLogger.println("==== Event Summary ===================================================");
        OutputLogger.println("======================================================================");
        
        // Output the FADC hits.
        OutputLogger.println("FADC Hits:");
        for(CalorimeterHit hit : hits) {
            int ix = hit.getIdentifierFieldValue("ix");
            int iy = hit.getIdentifierFieldValue("iy");
            OutputLogger.printf("\tHit at (%3d, %3d) with %7.3f GeV at time %3.0f ns%n", ix, iy, hit.getCorrectedEnergy(), hit.getTime());
        }
        if(hits.isEmpty()) { OutputLogger.println("\tNone"); }
        
        // Output the simulated clusters from the software.
        OutputLogger.printNewLine(2);
        OutputLogger.println("Software Clusters:");
        for(Cluster cluster : simulatedClusters) {
            OutputLogger.printf("\t%s%n", TriggerDiagnosticUtil.clusterToString(cluster));
            for(CalorimeterHit hit : cluster.getCalorimeterHits()) {
                int ix = hit.getIdentifierFieldValue("ix");
                int iy = hit.getIdentifierFieldValue("iy");
                OutputLogger.printf("\t\t> (%3d, %3d) :: %7.3f GeV%n", ix, iy, hit.getCorrectedEnergy());
            }
        }
        if(simulatedClusters.isEmpty()) { OutputLogger.println("\tNone"); }
        
        // Output the reported clusters from the hardware.
        OutputLogger.printNewLine(2);
        OutputLogger.println("Hardware Clusters:");
        for(SSPCluster cluster : hardwareClusters) {
            OutputLogger.printf("\t%s%n", TriggerDiagnosticUtil.clusterToString(cluster));
        }
        if(hardwareClusters.isEmpty()) { OutputLogger.println("\tNone"); }
        
        // When hits are written to data by the FADC, the pulse height
        // is only written within the bounds of the event window. Thus,
        // if a hit is too close to the beginning or the end of the
        // event window, it will experience "pulse-clipping" where the
        // hit loses a part of its energy. Clusters containing these
        // hits will often fail verification because the reduced energy
        // despite this not indicating an actual problem. To avoid
        // this, simulated clusters that are at risk of pulse-clipping
        // are excluded from cluster verification.
        OutputLogger.printNewLine(2);
        OutputLogger.println("==== Pulse-Clipping Verification =====================================");
        OutputLogger.println("======================================================================");
        
        // Iterate through each simulated cluster and keep only the
        // clusters safe from pulse-clipping.
        List<Cluster> goodSimulatedClusters = new ArrayList<Cluster>();
        OutputLogger.printNewLine(2);
        OutputLogger.println("Simulated Cluster Pulse-Clipping Check:");
        for(Cluster cluster : simulatedClusters) {
            boolean isSafe = TriggerDiagnosticUtil.isVerifiable(cluster, nsa, nsb, windowWidth);
            if(isSafe) { goodSimulatedClusters.add(cluster); }
            OutputLogger.printf("\t%s [ %7s ]%n", TriggerDiagnosticUtil.clusterToString(cluster),
                    isSafe ? "Safe" : "Clipped");
        }
        if(goodSimulatedClusters.isEmpty()) { OutputLogger.println("\tNone"); }
        
        // Print the header for cluster verification.
        OutputLogger.printNewLine(2);
        OutputLogger.println("==== Cluster Accuracy Verification ===================================");
        OutputLogger.println("======================================================================");
        
        // Generate a list of matched simulated/hardware cluster pairs
        // and the verification status of each pair.
        List<ClusterMatchedPair> matchedPairs = matchSimulatedToHardware(goodSimulatedClusters, hardwareClusters,
                energyVerificationThreshold, hitVerificationThreshold);
        
        // Get the number of clusters of each type processed.
        hardwareClusterCount = hardwareClusters.size();
        simulatedClusterCount = simulatedClusters.size();
        goodSimulatedClusterCount = goodSimulatedClusters.size();
        
        // Iterate over the list of pairs and extract statistical data
        // for this set of clusters.
        for(ClusterMatchedPair pair : matchedPairs) {
            if(pair.isMatch()) { matchedClusters++; }
            if(pair.isTimeFailState()) { failedMatchTime++; }
            if(pair.isEnergyFailState()) { failedMatchEnergy++; }
            if(pair.isHitCountFailState()) { failedMatchHitCount++; }
            if(pair.isPositionFailState()) { failedMatchPosition++; }
        }
        
        // The verification process is consider to fail when any not
        // pulse-clipped simulated cluster fails to verify.
        if(matchedPairs.size() != matchedClusters) { clusterFail = true; }
        
        // Output the results summary header.
        OutputLogger.printNewLine(2);
        OutputLogger.println("==== Cluster Verification Summary ====================================");
        OutputLogger.println("======================================================================");
        
        // Output the cluster pairs that successfully verified.
        OutputLogger.println();
        OutputLogger.println("Verified Simulated/Hardware Cluster Pairs:");
        if(matchedClusters != 0) {
            for(ClusterMatchedPair pair : matchedPairs) {
                if(pair.isMatch()) {
                    OutputLogger.printf("\t%s --> %s%n",
                            TriggerDiagnosticUtil.clusterToString(pair.getReconstructedCluster()),
                            TriggerDiagnosticUtil.clusterToString(pair.getSSPCluster()));
                }
            }
        } else { OutputLogger.println("\tNone"); }
        
        // Output the event statistics to the diagnostics logger.
        OutputLogger.println();
        OutputLogger.println("Event Statistics:");
        OutputLogger.printf("\tRecon Clusters     :: %d%n", simulatedClusters.size());
        OutputLogger.printf("\tClusters Matched   :: %d%n", matchedClusters);
        OutputLogger.printf("\tFailed (Position)  :: %d%n", failedMatchPosition);
        OutputLogger.printf("\tFailed (Time)      :: %d%n", failedMatchTime);
        OutputLogger.printf("\tFailed (Energy)    :: %d%n", failedMatchEnergy);
        OutputLogger.printf("\tFailed (Hit Count) :: %d%n", failedMatchHitCount);
        if(goodSimulatedClusters.isEmpty()) {
            OutputLogger.printf("\tCluster Efficiency :: N/A %n", 100.0 * matchedClusters / goodSimulatedClusters.size());
        } else {
            OutputLogger.printf("\tCluster Efficiency :: %3.0f%%%n", 100.0 * matchedClusters / goodSimulatedClusters.size());
        }
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
     * @return Returns a <code>List</code> containing all the matched
     * simulated/hardware cluster pairs as well as their verification
     * statuses.
     */
    private static final List<ClusterMatchedPair> matchSimulatedToHardware(Collection<Cluster> simulatedClusters,
            Collection<SSPCluster> hardwareClusters, double energyWindow, int hitWindow) {
        // Store the list of clusters, along with their matches (if
        // applicable) and their pair verification status.
        List<ClusterMatchedPair> pairList = new ArrayList<ClusterMatchedPair>();
        
        // Store the clusters which have been successfully paired.
        Set<SSPCluster> hardwareMatched = new HashSet<SSPCluster>(hardwareClusters.size());
        
        // Find simulated/hardware cluster matched pairs.
        simLoop:
        for(Cluster simCluster : simulatedClusters) {
            // Track whether a position-matched cluster was found.
            boolean matchedPosition = false;
            
            // VERBOSE :: Output the cluster being matched.
            OutputLogger.printf("Considering %s%n", TriggerDiagnosticUtil.clusterToString(simCluster));
            
            // Search through the hardware clusters for a match.
            hardwareLoop:
            for(SSPCluster hardwareCluster : hardwareClusters) {
                // VERBOSE :: Output the hardware cluster being considered.
                OutputLogger.printf("\t%s ", TriggerDiagnosticUtil.clusterToString(hardwareCluster));
                
                // Clusters must be matched in a one-to-one relationship,
                // so clusters that have already been matched should
                // be skipped.
                if(hardwareMatched.contains(hardwareCluster)) {
                    OutputLogger.printf("[ %7s; %9s ]%n", "fail", "matched");
                    continue hardwareLoop;
                }
                
                // If the clusters are the same, they must have the same
                // x- and y-indices.
                if(TriggerModule.getClusterXIndex(simCluster) != TriggerModule.getClusterXIndex(hardwareCluster)
                        || TriggerModule.getClusterYIndex(simCluster) != TriggerModule.getClusterYIndex(hardwareCluster)) {
                    OutputLogger.printf("[ %7s; %9s ]%n", "fail", "position");
                    continue hardwareLoop;
                }
                
                // Note that the cluster matched another cluster in
                // position. This is used to determine why a cluster
                // failed to verify, if necessary.
                matchedPosition = true;
                
                // If the clusters are the same, they should occur at
                // the same time as well.
                if(TriggerModule.getClusterTime(simCluster) != TriggerModule.getClusterTime(hardwareCluster)) {
                    OutputLogger.printf("[ %7s; %9s ]%n", "fail", "time");
                    continue hardwareLoop;
                }
                
                // It is impossible for two clusters to exist at the
                // same place and the same time, so clusters that pass
                // both the time comparison and position comparison are
                // assumed to be the same.
                hardwareMatched.add(hardwareCluster);
                
                // While time and position matched clusters are considered
                // to be the same cluster, the clusters must have similar
                // energies and hit counts to be properly verified. First
                // perform the energy check. The hardware cluster must
                // match the simulated cluster energy to within a given
                // bound.
                if(TriggerModule.getValueClusterTotalEnergy(hardwareCluster) >= TriggerModule.getValueClusterTotalEnergy(simCluster) - energyWindow
                        && TriggerModule.getValueClusterTotalEnergy(hardwareCluster) <= TriggerModule.getValueClusterTotalEnergy(simCluster) + energyWindow) {
                    // Next, check that the hardware cluster matches the
                    // simulated cluster in hit count to within a given
                    // bound.
                    if(TriggerModule.getClusterHitCount(hardwareCluster) >= TriggerModule.getClusterHitCount(simCluster) - hitWindow &&
                            TriggerModule.getClusterHitCount(hardwareCluster) <= TriggerModule.getClusterHitCount(simCluster) + hitWindow) {
                        // The cluster is a match.
                        pairList.add(new ClusterMatchedPair(simCluster, hardwareCluster, TriggerDiagnosticUtil.CLUSTER_STATE_MATCHED));
                        OutputLogger.printf("[ %7s; %9s ]%n", "success", "matched");
                        continue simLoop;
                    }
                    
                    // If the hit counts of the two clusters are not
                    // sufficiently close, the clusters fail to verify.
                    else {
                        pairList.add(new ClusterMatchedPair(simCluster, hardwareCluster, TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_HIT_COUNT));
                        OutputLogger.printf("[ %7s; %9s ]%n", "fail", "hit count");
                        continue simLoop;
                    } // End hit count check.
                }
                
                // If the energies of the two clusters are not
                // sufficiently close, the clusters fail to verify.
                else {
                    pairList.add(new ClusterMatchedPair(simCluster, hardwareCluster, TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_ENERGY));
                    OutputLogger.printf("[ %7s; %9s ]%n", "fail", "energy");
                    continue simLoop;
                } // End energy check.
            } // End hardware loop.
            
            // This point may only be reached if a cluster failed to
            // match with another cluster in either time or position.
            // Check whether a cluster at the same position was found.
            // If it was not, then the cluster fails to verify by reason
            // of position.
            if(!matchedPosition) {
                pairList.add(new ClusterMatchedPair(simCluster, null, TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_POSITION));
            }
            
            // Otherwise, the cluster had a potential match found at
            // the same position, but the time-stamps did not align.
            else {
                pairList.add(new ClusterMatchedPair(simCluster, null, TriggerDiagnosticUtil.CLUSTER_STATE_FAIL_TIME));
            }
        } // End sim loop.
        
        // Return the list of clusters, their matches, and their
        // verification states.
        return pairList;
    }
}
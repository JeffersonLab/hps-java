package org.hps.analysis.trigger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hps.analysis.trigger.util.OutputLogger;
import org.hps.analysis.trigger.util.SinglesTrigger;
import org.hps.analysis.trigger.util.TriggerDiagnosticUtil;
import org.hps.record.triggerbank.SSPCluster;
import org.hps.record.triggerbank.SSPSinglesTrigger;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;

public class SinglesTriggerDiagnosticModule {
    int hardwareTriggerCount = 0;
    int simSimTriggerCount = 0;
    int hardwareSimTriggerCount = 0;
    int matchedSimSimTriggers = 0;
    int matchedHardwareSimTriggers = 0;
    
    boolean hardwareTriggerFail = false;
    boolean simulatedTriggerFail = false;
    
    SinglesTriggerDiagnosticModule(List<SinglesTrigger<Cluster>> simClusterSimTriggers, List<SinglesTrigger<SSPCluster>> hardwareClusterSimTriggers,
            List<SSPSinglesTrigger> hardwareTriggers, String triggerName, int nsa, int nsb, int windowWidth) {
        // Output the current trigger's diagnostic header.
        OutputLogger.printNewLine(2);
        OutputLogger.println("======================================================================");
        StringBuffer nameBuffer = new StringBuffer("=== " + triggerName + " ");
        while(nameBuffer.length() < 70) { nameBuffer.append('='); }
        OutputLogger.println(nameBuffer.toString());
        OutputLogger.println("======================================================================");
        
        // Print out the event summary sub-header.
        OutputLogger.printNewLine(2);
        OutputLogger.println("==== Event Summary ===================================================");
        OutputLogger.println("======================================================================");
        
        // Only keep simulated cluster triggers which have source clusters
        // which occurred within the non-pulse-clipped region. Clusters
        // from outside this region are can not be relied upon to have
        // accurate energies, and thusly to have the correct pass/fail
        // result for most trigger cuts.
        List<SinglesTrigger<Cluster>> goodSimClusterSimTriggers = new ArrayList<SinglesTrigger<Cluster>>();
        
        // List the hardware triggers. Note that the source clusters
        // are not retained for hardware triggers. Additionally, the
        // seed energy cut is not tracked; it assumed that it passed,
        // as the trigger should not appear otherwise. If clusters are
        // appearing that should not, the error should be caught during
        // cluster verification.
        OutputLogger.printNewLine(2);
        OutputLogger.println("Hardware Triggers:");
        for(SSPSinglesTrigger trigger : hardwareTriggers) {
            OutputLogger.printf("\t%s%n", "Source Cluster Unknown");
            OutputLogger.printf("\t\tSeed Energy    :: [ %5b ]%n", true);
            OutputLogger.printf("\t\tCluster Energy :: [ %5b ]%n", (trigger.passCutEnergyMin() && trigger.passCutEnergyMax()));
            OutputLogger.printf("\t\tHit Count      :: [ %5b ]%n", trigger.passCutHitCount());
        }
        if(simClusterSimTriggers.isEmpty()) { OutputLogger.println("\tNone"); }
        
        // List the triggers simulated from simulated clusters.
        OutputLogger.printNewLine(2);
        OutputLogger.println("Simulated Triggers (Simulated Clusters):");
        for(SinglesTrigger<Cluster> trigger : simClusterSimTriggers) {
            // Output the cluster summary.
            OutputLogger.printf("\t%s%n", TriggerDiagnosticUtil.clusterToString(trigger.getTriggerSource()));
            OutputLogger.printf("\t\tSeed Energy    :: [ %5b ]%n", trigger.getStateSeedEnergy());
            OutputLogger.printf("\t\tCluster Energy :: [ %5b ]%n", trigger.getStateClusterEnergy());
            OutputLogger.printf("\t\tHit Count      :: [ %5b ]%n", trigger.getStateHitCount());
            
            // Restrict the triggers to only those that are outside
            // the pulse clipping region.
            if(TriggerDiagnosticUtil.isVerifiable(trigger.getTriggerSource(), nsa, nsb, windowWidth)) {
                goodSimClusterSimTriggers.add(trigger);
                OutputLogger.printf("\t\tVerifiable     :: [ %5b ]%n", true);
            } else { OutputLogger.printf("\t\tVerifiable     :: [ %5b ]%n", false); }
            
        }
        if(simClusterSimTriggers.isEmpty()) { OutputLogger.println("\tNone"); }
        
        // List the triggers simulated from hardware clusters.
        OutputLogger.printNewLine(2);
        OutputLogger.println("Simulated Triggers (Hardware Clusters):");
        for(SinglesTrigger<SSPCluster> trigger : hardwareClusterSimTriggers) {
            OutputLogger.printf("\t%s%n", TriggerDiagnosticUtil.clusterToString(trigger.getTriggerSource()));
            OutputLogger.printf("\t\tSeed Energy    :: [ %5b ]%n", trigger.getStateSeedEnergy());
            OutputLogger.printf("\t\tCluster Energy :: [ %5b ]%n", trigger.getStateClusterEnergy());
            OutputLogger.printf("\t\tHit Count      :: [ %5b ]%n", trigger.getStateHitCount());
        }
        if(simClusterSimTriggers.isEmpty()) { OutputLogger.println("\tNone"); }
        
        // note the number of hardware triggers.
        hardwareTriggerCount = hardwareTriggers.size();
        
        // The first verification test for the trigger is that all
        // triggers simulated from hardware clusters were actually
        // found by the hardware.
        hardwareSimTriggerCount = hardwareClusterSimTriggers.size();
        compareSimulatedToHardware(hardwareClusterSimTriggers, hardwareTriggers, SSPCluster.class);
        
        // The trigger verification can then be performed on the good
        // simulated cluster triggers.
        simSimTriggerCount = goodSimClusterSimTriggers.size();
        compareSimulatedToHardware(goodSimClusterSimTriggers, hardwareTriggers, Cluster.class);
        
        // Trigger verification is considered to have failed if at
        // least one trigger of a given type can not be verified.
        if(matchedSimSimTriggers != simSimTriggerCount) { simulatedTriggerFail = true; }
        if(matchedHardwareSimTriggers != hardwareSimTriggerCount) { hardwareTriggerFail = true; }
        
        // Print out the verification results header.
        OutputLogger.printNewLine(2);
        OutputLogger.println("==== Trigger Verification Results Summary ============================");
        OutputLogger.println("======================================================================");
        
        // Output the event statistics to the diagnostics logger.
        OutputLogger.println();
        OutputLogger.println("Event Statistics:");
        OutputLogger.printf("\tHardware Triggers              :: %d%n", hardwareTriggerCount);
        OutputLogger.printf("\tSimulated Cluster Sim Triggers :: %d%n", simSimTriggerCount);
        OutputLogger.printf("\tHardware Cluster Sim Triggers  :: %d%n", hardwareSimTriggerCount);
        if(simClusterSimTriggers.isEmpty()) {
            OutputLogger.printf("\tSimulated Cluster Sim Trigger :: N/A %n");
        } else {
            OutputLogger.printf("\tSimulated Cluster Sim Trigger Efficiency :: %3.0f%%%n", 100.0 * matchedSimSimTriggers / simSimTriggerCount);
        }
        if(hardwareClusterSimTriggers.isEmpty()) {
            OutputLogger.printf("\tHardware Cluster Sim Trigger  :: N/A %n");
        } else {
            OutputLogger.printf("\tHardware Cluster Sim Trigger Efficiency  :: %3.0f%%%n", 100.0 * matchedHardwareSimTriggers / hardwareSimTriggerCount);
        }
    }
    
    private <E> void compareSimulatedToHardware(List<SinglesTrigger<E>> hardwareSimTriggers,
            List<SSPSinglesTrigger> hardwareTriggers, Class<E> clusterType) {
        // Print out the matching sub-header.
        OutputLogger.printNewLine(2);
        if(clusterType == Cluster.class) {
            OutputLogger.println("==== Simulated Sim Trigger to Hardware Trigger Verification ==========");
        } else {
            OutputLogger.println("==== Hardware Sim Trigger to Hardware Trigger Verification ===========");
        }
        OutputLogger.println("======================================================================");
        
        // Trigger matches must be one-to-one. Track which hardware
        // triggers are already matched do that they are not matched
        // twice.
        Set<SSPSinglesTrigger> matchedTriggers = new HashSet<SSPSinglesTrigger>();
        
        // Iterate over each trigger simulated from hardware clusters.
        // It is expected that each of these triggers will correspond
        // to an existing hardware trigger, since, purportedly, these
        // are the same clusters from which the hardware triggers are
        // generated.
        simLoop:
        for(SinglesTrigger<E> simTrigger : hardwareSimTriggers) {
            // Since hardware triggers do not retain the triggering
            // cluster, the only way to compare triggers is by the
            // time stamp and the cut results. In order to be verified,
            // a trigger must have all of these values match. The time
            // of a singles trigger is the time of the cluster which
            // created it.
            double simTime;
            String clusterText;
            E cluster = simTrigger.getTriggerSource();
            if(cluster instanceof Cluster) {
                simTime = TriggerModule.getClusterTime((Cluster) cluster);
                clusterText = TriggerDiagnosticUtil.clusterToString((Cluster) cluster);
            } else {
                simTime = TriggerModule.getClusterTime((SSPCluster) cluster);
                clusterText = TriggerDiagnosticUtil.clusterToString((SSPCluster) cluster);
            }
            
            // Output the current trigger that is being matched.
            OutputLogger.printf("Matching Trigger t = %3.0f; Seed: %5b; Cluster: %5b; Hit: %5b%n\t\t%s%n",
                    simTime, simTrigger.getStateSeedEnergy(), simTrigger.getStateClusterEnergy(), simTrigger.getStateHitCount(),
                    clusterText);
            
            // Iterate over the hardware triggers and look for one that
            // matches all of the simulated trigger's values.
            hardwareLoop:
            for(SSPSinglesTrigger hardwareTrigger : hardwareTriggers) {
                // Output the comparison hardware trigger.
                OutputLogger.printf("\tHardwareTrigger t = %3d; Seed: %5b; Cluster: %5b; Hit: %5b", hardwareTrigger.getTime(),
                        true, (hardwareTrigger.passCutEnergyMin() && hardwareTrigger.passCutEnergyMax()), hardwareTrigger.passCutHitCount());
                
                // If the current trigger has already been matched,
                // then skip over it.
                if(matchedTriggers.contains(hardwareTrigger)) {
                    OutputLogger.printf(" [ fail; matched    ]%n");
                    continue hardwareLoop;
                }
                
                // The triggers must occur at the same time to classify
                // as a match.
                if(hardwareTrigger.getTime() != simTime) {
                    OutputLogger.printf(" [ fail; time       ]%n");
                    continue hardwareLoop;
                }
                
                // Since there is no seed hit information for the
                // hardware cluster, we just assume that this cut
                // matches. Instead, move to the cluster total energy
                // cut.
                if((hardwareTrigger.passCutEnergyMin() && hardwareTrigger.passCutEnergyMax()) != simTrigger.getStateClusterEnergy()) {
                    OutputLogger.printf(" [ fail; energy     ]%n");
                    continue hardwareLoop;
                }
                
                // Lastly, check if the cluster hit count cut matches.
                if(hardwareTrigger.passCutHitCount() != simTrigger.getStateHitCount()) {
                    OutputLogger.printf(" [ fail; hit count  ]%n");
                    continue hardwareLoop;
                }
                
                // If all three values match, then these triggers are
                // considered a match and verified.
                OutputLogger.printf(" [ cluster verified ]%n");
                matchedTriggers.add(hardwareTrigger);
                if(clusterType == Cluster.class) { matchedSimSimTriggers++; }
                else { matchedHardwareSimTriggers++; }
                continue simLoop;
            }
            
            // If this point is reached, all possible hardware triggers
            // have been checked and failed to match. This trigger then
            // fails to verify.
            OutputLogger.println("\t\tVerification failed!");
        }
    }
}
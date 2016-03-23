package org.hps.analysis.trigger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hps.analysis.trigger.util.OutputLogger;
import org.hps.analysis.trigger.util.PairTrigger;
import org.hps.analysis.trigger.util.TriggerDiagnosticUtil;
import org.hps.record.triggerbank.SSPCluster;
import org.hps.record.triggerbank.SSPPairTrigger;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;

public class PairTriggerDiagnosticModule {
    int hardwareTriggerCount = 0;
    int simSimTriggerCount = 0;
    int hardwareSimTriggerCount = 0;
    int matchedSimSimTriggers = 0;
    int matchedHardwareSimTriggers = 0;
    
    boolean hardwareTriggerFail = false;
    boolean simulatedTriggerFail = false;
    
    PairTriggerDiagnosticModule(List<PairTrigger<Cluster[]>> simClusterSimTriggers, List<PairTrigger<SSPCluster[]>> hardwareClusterSimTriggers,
            List<SSPPairTrigger> hardwareTriggers, String triggerName, int nsa, int nsb, int windowWidth) {
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
        
        // List the hardware triggers. Note that the source clusters
        // are not retained for hardware triggers. Additionally, the
        // singles cuts are not tracked; it assumed that they passed,
        // as the trigger should not appear otherwise.
        OutputLogger.printNewLine(2);
        OutputLogger.println("Hardware Triggers:");
        for(SSPPairTrigger trigger : hardwareTriggers) {
            OutputLogger.printf("\t%s%n", "Source Cluster Unknown");
            OutputLogger.printf("\t\tEnergy Sum        :: [ %5b ]%n", trigger.passCutEnergySum());
            OutputLogger.printf("\t\tEnergy Difference :: [ %5b ]%n", trigger.passCutEnergyDifference());
            OutputLogger.printf("\t\tEnergy Slope      :: [ %5b ]%n", trigger.passCutEnergySlope());
            OutputLogger.printf("\t\tCoplanarity       :: [ %5b ]%n", trigger.passCutCoplanarity());
        }
        if(simClusterSimTriggers.isEmpty()) { OutputLogger.println("\tNone"); }
        
        // List the triggers simulated from simulated clusters.
        OutputLogger.printNewLine(2);
        OutputLogger.println("Simulated Triggers (Simulated Clusters):");
        for(PairTrigger<Cluster[]> trigger : simClusterSimTriggers) {
            if(TriggerModule.getClusterY(trigger.getTriggerSource()[0]) < 0) {
                OutputLogger.printf("\t%s%n", TriggerDiagnosticUtil.clusterToString(trigger.getTriggerSource()[0]));
            } else {
                OutputLogger.printf("\t%s%n", TriggerDiagnosticUtil.clusterToString(trigger.getTriggerSource()[1]));
            }
            OutputLogger.printf("\t\tEnergy Sum        :: [ %5b ]%n", trigger.getStateEnergySum());
            OutputLogger.printf("\t\tEnergy Difference :: [ %5b ]%n", trigger.getStateEnergyDifference());
            OutputLogger.printf("\t\tEnergy Slope      :: [ %5b ]%n", trigger.getStateEnergySlope());
            OutputLogger.printf("\t\tCoplanarity       :: [ %5b ]%n", trigger.getStateCoplanarity());
        }
        if(simClusterSimTriggers.isEmpty()) { OutputLogger.println("\tNone"); }
        
        // List the triggers simulated from hardware clusters.
        OutputLogger.printNewLine(2);
        OutputLogger.println("Simulated Triggers (Hardware Clusters):");
        for(PairTrigger<SSPCluster[]> trigger : hardwareClusterSimTriggers) {
            if(TriggerModule.getClusterY(trigger.getTriggerSource()[0]) < 0) {
                OutputLogger.printf("\t%s%n", TriggerDiagnosticUtil.clusterToString(trigger.getTriggerSource()[0]));
            } else {
                OutputLogger.printf("\t%s%n", TriggerDiagnosticUtil.clusterToString(trigger.getTriggerSource()[1]));
            }
            OutputLogger.printf("\t\tEnergy Sum        :: [ %5b ]%n", trigger.getStateEnergySum());
            OutputLogger.printf("\t\tEnergy Difference :: [ %5b ]%n", trigger.getStateEnergyDifference());
            OutputLogger.printf("\t\tEnergy Slope      :: [ %5b ]%n", trigger.getStateEnergySlope());
            OutputLogger.printf("\t\tCoplanarity       :: [ %5b ]%n", trigger.getStateCoplanarity());
        }
        if(simClusterSimTriggers.isEmpty()) { OutputLogger.println("\tNone"); }
        
        // note the number of hardware triggers.
        hardwareTriggerCount = hardwareTriggers.size();
        
        // The first verification test for the trigger is that all
        // triggers simulated from hardware clusters were actually
        // found by the hardware.
        hardwareSimTriggerCount = hardwareClusterSimTriggers.size();
        compareSimulatedToHardware(hardwareClusterSimTriggers, hardwareTriggers, SSPCluster.class);
        
        // The next verification test is to see that all triggers
        // produced from simulated clusters are also observed by the
        // hardware. However, because of pulse-clipping, it is not a
        // meaningful test to verify every simulated cluster trigger.
        // Simulated cluster triggers with source clusters too near the
        // edge of the event window must be excluded.
        List<PairTrigger<Cluster[]>> goodSimClusterSimTriggers = new ArrayList<PairTrigger<Cluster[]>>();
        for(PairTrigger<Cluster[]> simTrigger : simClusterSimTriggers) {
            if(TriggerDiagnosticUtil.isVerifiable(simTrigger.getTriggerSource()[0], nsa, nsb, windowWidth)
                    && TriggerDiagnosticUtil.isVerifiable(simTrigger.getTriggerSource()[1], nsa, nsb, windowWidth)) {
                goodSimClusterSimTriggers.add(simTrigger);
            }
        }
        
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
    
    private <E> void compareSimulatedToHardware(List<PairTrigger<E[]>> hardwareSimTriggers,
            List<SSPPairTrigger> hardwareTriggers, Class<E> clusterType) {
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
        Set<SSPPairTrigger> matchedTriggers = new HashSet<SSPPairTrigger>();
        
        // Iterate over each trigger simulated from hardware clusters.
        // It is expected that each of these triggers will correspond
        // to an existing hardware trigger, since, purportedly, these
        // are the same clusters from which the hardware triggers are
        // generated.
        simLoop:
        for(PairTrigger<E[]> simTrigger : hardwareSimTriggers) {
            // Since hardware triggers do not retain the triggering
            // cluster, the only way to compare triggers is by the
            // time stamp and the cut results. In order to be verified,
            // a trigger must have all of these values match. The time
            // of a singles trigger is the time of the cluster which
            // created it.
            double simTime;
            String[] clusterText = new String[2];
            E[] pair = simTrigger.getTriggerSource();
            if(pair instanceof Cluster[]) {
                if(TriggerModule.getClusterYIndex(((Cluster[]) pair)[0]) < 0) {
                    simTime = TriggerModule.getClusterTime(((Cluster[]) pair)[0]);
                } else { simTime = TriggerModule.getClusterTime(((Cluster[]) pair)[1]); }
                clusterText[0] = TriggerDiagnosticUtil.clusterToString(((Cluster[]) pair)[0]);
                clusterText[1] = TriggerDiagnosticUtil.clusterToString(((Cluster[]) pair)[1]);
            } else {
                if(TriggerModule.getClusterYIndex(((SSPCluster[]) pair)[0]) < 0) {
                    simTime = TriggerModule.getClusterTime(((SSPCluster[]) pair)[0]);
                } else { simTime = TriggerModule.getClusterTime(((SSPCluster[]) pair)[1]); }
                clusterText[0] = TriggerDiagnosticUtil.clusterToString(((SSPCluster[]) pair)[0]);
                clusterText[1] = TriggerDiagnosticUtil.clusterToString(((SSPCluster[]) pair)[1]);
            }
            
            // Output the current trigger that is being matched.
            OutputLogger.printf("Matching Trigger t = %3.0f; Sum: %5b; Diff: %5b; Slope: %5b; Coplanarity: %5b%n\t\t%s%n\t\t%s%n",
                    simTime, simTrigger.getStateEnergySum(), simTrigger.getStateEnergyDifference(),
                    simTrigger.getStateEnergySlope(), simTrigger.getStateCoplanarity(), clusterText[0], clusterText[1]);
            
            // Iterate over the hardware triggers and look for one that
            // matches all of the simulated trigger's values.
            hardwareLoop:
            for(SSPPairTrigger hardwareTrigger : hardwareTriggers) {
                // Output the comparison hardware trigger.
                OutputLogger.printf("\tHardwareTrigger t = %3d; Sum: %5b; Diff: %5b; Slope: %5b; Coplanarity: %5b", hardwareTrigger.getTime(),
                        true, hardwareTrigger.passCutEnergySum(), hardwareTrigger.passCutEnergyDifference(),
                        hardwareTrigger.passCutEnergySlope(), hardwareTrigger.passCutCoplanarity());
                
                // If the current trigger has already been matched,
                // then skip over it.
                if(matchedTriggers.contains(hardwareTrigger)) {
                    OutputLogger.printf(" [ fail; matched     ]%n");
                    continue hardwareLoop;
                }
                
                // The triggers must occur at the same time to classify
                // as a match.
                if(hardwareTrigger.getTime() != simTime) {
                    OutputLogger.printf(" [ fail; time        ]%n");
                    continue hardwareLoop;
                }
                
                // Since there is no singles cuts data for the hardware
                // trigger, we just assume that these cuts match. Move
                // to the pair energy sum cut.
                if(hardwareTrigger.passCutEnergySum() != simTrigger.getStateEnergySum()) {
                    OutputLogger.printf(" [ fail; sum         ]%n");
                    continue hardwareLoop;
                }
                
                // Next, check the energy difference cut.
                if(hardwareTrigger.passCutEnergyDifference() != simTrigger.getStateEnergyDifference()) {
                    OutputLogger.printf(" [ fail; difference  ]%n");
                    continue hardwareLoop;
                }
                
                // Next, check the energy slope cut.
                if(hardwareTrigger.passCutEnergySlope() != simTrigger.getStateEnergySlope()) {
                    OutputLogger.printf(" [ fail; slope       ]%n");
                    continue hardwareLoop;
                }
                
                // Lastly, check the coplanarity cut.
                if(hardwareTrigger.passCutCoplanarity() != simTrigger.getStateCoplanarity()) {
                    OutputLogger.printf(" [ fail; coplanarity ]%n");
                    continue hardwareLoop;
                }
                
                // If all three values match, then these triggers are
                // considered a match and verified.
                OutputLogger.printf(" [ cluster verified  ]%n");
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
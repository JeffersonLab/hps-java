/**
 * 
 */
package org.hps.analysis.trigger.util;

import java.util.ArrayList;
import java.util.List;

import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.SSPCluster;
import org.hps.record.triggerbank.SSPData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;

/**
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class TriggerDecisionCalculator {

    public enum TriggerType {
        SINGLES0, SINGLES1, PAIR0, PAIR1, SINGLES1_SIM, PULSER
    }

    private List<TriggerType> passedTriggers = new ArrayList<TriggerType>();

    public TriggerDecisionCalculator(EventHeader event) {
        process(event);
    }
    
    public void add(TriggerType type) {
        passedTriggers.add(type);
    }

    public boolean passed(TriggerType type) {
        for(TriggerType passed : passedTriggers) {
            if( passed == type) 
                return true;
        }
        return false;
    }

    public void process(EventHeader event) {
        List<GenericObject> triggerBanks = event.get(GenericObject.class, "TriggerBank");
        for (GenericObject triggerBank : triggerBanks) {
            if(AbstractIntData.getTag(triggerBank) == SSPData.BANK_TAG) {
                SSPData sspBank = new SSPData(triggerBank);
                List<SSPCluster> sspClusters = sspBank.getClusters();
                List<List<SinglesTrigger<SSPCluster>>> singleTriggers = constructSinglesTriggersFromSSP(sspClusters);
                if( singleTriggers.get(1).size() > 0 )
                    passedTriggers.add(TriggerType.SINGLES1_SIM);
            } 
            else if (AbstractIntData.getTag(triggerBank) == TIData.BANK_TAG) {
                TIData tiData = new TIData(triggerBank);
                if(tiData.isSingle0Trigger())
                    passedTriggers.add(TriggerType.SINGLES0);
                else if(tiData.isSingle1Trigger())
                    passedTriggers.add(TriggerType.SINGLES1);
                else if(tiData.isPair0Trigger())
                    passedTriggers.add(TriggerType.PAIR0);
                else if(tiData.isPair1Trigger())
                    passedTriggers.add(TriggerType.PAIR1);
                else if(tiData.isPulserTrigger()) 
                    passedTriggers.add(TriggerType.PULSER);
            }
        }      
    }
    
    
    static public List<List<SinglesTrigger<SSPCluster>>> constructSinglesTriggersFromSSP(List<SSPCluster> clusters) {
        
        List<List<SinglesTrigger<SSPCluster>>> triggers = new ArrayList<List<SinglesTrigger<SSPCluster>>>(2); 
        // Instantiate the triggers lists.
        for(int triggerNum = 0; triggerNum < 2; triggerNum++) 
            triggers.add(new ArrayList<SinglesTrigger<SSPCluster>>());
        
    
        
        final double ENERGY_CUT_LOW[] = {0.5,0.0}; 
        final double ENERGY_CUT_HIGH[] = {8.191,8.191}; 
        final int HIT_COUNT_CUT_LOW[] = {0,0}; 
        final boolean singlesCutsEnabled_ENERGY_MIN[] = {true,true};
        final boolean singlesCutsEnabled_ENERGY_MAX[] = {true,true};
        final boolean singlesCutsEnabled_HIT_COUNT[] = {true,true};
       
        
        for(SSPCluster cluster : clusters) {
           
            triggerLoop:
            for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
                // For a cluster to have formed it is assumed to have
                // passed the cluster seed energy cuts. This can not
                // be verified since the SSP bank does not report
                // individual hit. 
                boolean passSeedLow = true;
                boolean passSeedHigh = true;
                
                // The remaining cuts may be acquired from trigger module.
                boolean passClusterLow = cluster.getEnergy() >= ENERGY_CUT_LOW[triggerNum] ? true : false; 
                boolean passClusterHigh = cluster.getEnergy() <= ENERGY_CUT_HIGH[triggerNum] ? true : false; 
                boolean passHitCount = cluster.getHitCount() >= HIT_COUNT_CUT_LOW[triggerNum] ? true : false; 
                
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
                // Set the singles cut statuses.
                if(singlesCutsEnabled_ENERGY_MIN[triggerNum] && !trigger.getStateClusterEnergyLow()) {
                    continue triggerLoop;
                } if(singlesCutsEnabled_ENERGY_MAX[triggerNum] && !trigger.getStateClusterEnergyHigh()) {
                    continue triggerLoop;
                } if(singlesCutsEnabled_HIT_COUNT[triggerNum] && !trigger.getStateHitCount()) {
                    continue triggerLoop;
                }
                
                // If all the necessary checks passed, store the new
                // trigger for verification.
                
                triggers.get(triggerNum).add(trigger);
            }
        }
        return triggers;
    }
    


}

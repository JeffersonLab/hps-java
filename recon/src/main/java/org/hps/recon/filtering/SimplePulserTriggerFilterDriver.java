/**
 * 
 */
package org.hps.recon.filtering;

import java.util.List;

import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;

/**
 * 
 * Skim events where the pulser fired.
 * 
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class SimplePulserTriggerFilterDriver extends EventReconFilter {

    @Override
    public void process(EventHeader event) {
        
        incrementEventProcessed();
        
        if(!event.hasCollection(GenericObject.class, "TriggerBank")) skipEvent();
        
        if(!pulserFired(event.get(GenericObject.class, "TriggerBank"))) skipEvent();
        
        incrementEventPassed();
        
    }

    private boolean pulserFired(List<GenericObject> triggerBanks) {
        boolean isRandomTriggerEvent = false;

        // Loop through the collection of banks and get the TI banks.
        for (GenericObject triggerBank : triggerBanks) {

            // If the bank contains TI data, process it
            if (AbstractIntData.getTag(triggerBank) == TIData.BANK_TAG) {

                TIData tiData = new TIData(triggerBank);

                if(tiData.isPulserTrigger())
                    isRandomTriggerEvent = true;
            
            }
        }
        
        return isRandomTriggerEvent;
    }
    
    
}


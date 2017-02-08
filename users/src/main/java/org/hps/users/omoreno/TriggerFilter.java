package org.hps.users.omoreno;

import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;

import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;

/**
 * An LCSim driver used to filter out events by trigger type. 
 */
public class TriggerFilter extends Driver {

    //----------------------//
    //   Collection Names   //
    //----------------------//
    private static final String TRIGGER_BANK_COL_NAME = "TriggerBank";
   
    //-------------------//
    //   Trigger flags   //
    //-------------------//
    
    private boolean enableTriggerFilter = false;
    private boolean filterPulserTriggers = true;
    private boolean filterSingle0Triggers = true;
    private boolean filterSingle1Triggers = true;
    private boolean filterPair0Triggers = true;
    private boolean filterPair1Triggers = true;
    
    protected boolean triggerFound = false; 
  
    /** Enable/disable filtering by trigger. */
    public void setEnableTriggerFilter(boolean enableTriggerFilter) {
        this.enableTriggerFilter = enableTriggerFilter;
    }

    /** Enable/disable filtering pulser triggers. */
    public void setFilterPulserTriggers(boolean filterPulserTriggers) {
        this.filterPulserTriggers = filterPulserTriggers;
    }

    /** Enable/disable filtering singles0 triggers. */
    public void setFilterSingle0Triggers(boolean filterSingle0Triggers) {
        this.filterSingle0Triggers = filterSingle0Triggers;
    }

    /** Enable/disable filtering of singles1 triggers. */
    public void setFilterSingle1Triggers(boolean filterSingle1Triggers) {
        this.filterSingle1Triggers = filterSingle1Triggers;
    }
    
    /** Enable/disable filtering of pair0 triggers. */
    public void setFilterPair0Triggers(boolean filterPair0Triggers) {
        this.filterPair0Triggers = filterPair0Triggers;
    }

    /** Enable/disable filtering of pair1 triggers. */
    public void setFilterPair1Triggers(boolean filterPair1Triggers) {
        this.filterPair1Triggers = filterPair1Triggers;
    }
    
    /** 
     * Method to check if the event was due to the specified trigger filter.
     * 
     * @param triggerBanks Collection containing trigger bank information.
     * @return True if event was due to specified trigger, false otherwise.
     * 
     */
    private boolean passTriggerFilter(List<GenericObject> triggerBanks) {

        // Loop through the collection of banks and get the TI banks.
        for (GenericObject triggerBank : triggerBanks) {

            // If the bank contains TI data, process it
            if (AbstractIntData.getTag(triggerBank) == TIData.BANK_TAG) {

                TIData tiData = new TIData(triggerBank);

                if (filterPulserTriggers && tiData.isPulserTrigger()) {
                    return false;
                } else if (filterSingle0Triggers && tiData.isSingle0Trigger()) {
                    return false;
                } else if (filterSingle1Triggers && tiData.isSingle1Trigger()) {
                    return false;
                } else if (filterPair0Triggers && tiData.isPair0Trigger()) {
                    return false;
                } else if (filterPair1Triggers && tiData.isPair1Trigger()) {
                    return false;
                }
            }
        }
        return true;
    }
    
    @Override
    public void process(EventHeader event) {
   
        triggerFound = false;
        //System.out.println("Event: " + event.getEventNumber());
        if (enableTriggerFilter && event.hasCollection(GenericObject.class, TRIGGER_BANK_COL_NAME)) {

            // Get the list of trigger banks from the event
            List<GenericObject> triggerBanks = event.get(GenericObject.class, TRIGGER_BANK_COL_NAME);

            // Apply the trigger filter
            if (passTriggerFilter(triggerBanks)) {
                triggerFound = true;
            }
        }
    }
}

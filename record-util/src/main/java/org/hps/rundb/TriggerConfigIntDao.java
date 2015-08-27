package org.hps.rundb;

import org.hps.record.triggerbank.TriggerConfigInt;

/**
 * Database interface to trigger config.
 * 
 * @author Jeremy McCormick, SLAC
 */
public interface TriggerConfigIntDao {
   
    /**
     * Get the trigger config by run.
     * 
     * @param run the run number
     * @return the trigger config
     */
    TriggerConfigInt getTriggerConfigInt(int run);
    
    /**
     * Insert a trigger config for a run.
     * 
     * @param run the run number
     * @param triggerConfig the trigger config
     */
    void insertTriggerConfigInt(TriggerConfigInt triggerConfig, int run);
   
    /**
     * Delete a trigger config by run number.
     * 
     * @param run the run number
     */
    void deleteTriggerConfigInt(int run);
}

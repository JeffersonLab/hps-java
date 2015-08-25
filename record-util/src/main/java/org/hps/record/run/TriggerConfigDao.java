package org.hps.record.run;

/**
 * Database interface to trigger config.
 * 
 * @author Jeremy McCormick, SLAC
 */
public interface TriggerConfigDao {
   
    /**
     * Get the trigger config by run.
     * 
     * @param run the run number
     * @return the trigger config
     */
    TriggerConfig getTriggerConfig(int run);
    
    /**
     * Insert a trigger config for a run.
     * 
     * @param run the run number
     * @param triggerConfig the trigger config
     */
    void insertTriggerConfig(int run, TriggerConfig triggerConfig);
    

    /**
     * Update a trigger config by run number.
     * 
     * @param run the run number
     * @param triggerConfig the trigger config
     */
    void updateTriggerConfig(int run, TriggerConfig triggerConfig);
    
    /**
     * Delete a trigger config by run number.
     * 
     * @param run the run number
     */
    void deleteTriggerConfig(int run);
}

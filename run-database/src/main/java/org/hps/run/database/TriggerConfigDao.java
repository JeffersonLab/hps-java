package org.hps.run.database;

/**
 * Database interface for getting raw trigger config data.
 * 
 * @author Jeremy McCormick, SLAC
 */
interface TriggerConfigDao {
    
    /**
     * Get a trigger config by run number.
     * 
     * @param run the run number
     * @return the trigger config
     */
    TriggerConfig getTriggerConfig(int run);
    
    /**
     * Insert a trigger config.
     * 
     * @param config the trigger config
     * @param run the run number
     */
    void insertTriggerConfig(TriggerConfig config, int run);
            
    /**
     * Delete a trigger config by run.
     * 
     * @param run the run number
     */
    void deleteTriggerConfig(int run);
}
